/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.audio;

import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.result.AudioTag;
import mitll.langtest.server.*;
import mitll.langtest.server.autocrt.AutoCRT;
import mitll.langtest.server.autocrt.DecodeCorrectnessChecker;
import mitll.langtest.server.database.AnswerInfo;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.audio.EnsureAudioHelper;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.project.ProjectManagement;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.server.scoring.*;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.DecoderOptions;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.PretestScore;
import mitll.npdata.dao.lts.HTKDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.*;

import static mitll.langtest.server.ScoreServlet.GetRequest.HASUSER;
import static mitll.langtest.server.ScoreServlet.HeaderValue.*;
import static mitll.langtest.server.database.exercise.Project.WEBSERVICE_HOST_DEFAULT;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 8/8/13
 * Time: 5:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class AudioFileHelper implements AlignDecode {
  private static final Logger logger = LogManager.getLogger(AudioFileHelper.class);

  private static final String REG = "reg";
  private static final String SLOW = "slow";
  private static final int SUFFIX_LENGTH = ("." + AudioTag.COMPRESSED_TYPE).length();
  private static final ImageOptions DEFAULT = ImageOptions.getDefault();
  private static final ImageOptions NO_IMAGE_PLEASE = new ImageOptions(-1, -1, false, false);
  private static final String MESSAGE_NO_SESSION = "{\"message\":\"no session\"}";
  private static final String OGG = "ogg";

  public static final boolean DEBUG = false;
  private static final String FRENCH = "french";
  private static final double MIN_SCORE_FOR_CORRECT_ALIGN = 0.35;
  /**
   * @see #getSession
   */
  private static final String TEST_USER = "demo_";
  private static final String TEST_PASSWORD = "domino22";//"demo";
  public static final long DAY = 24 * 60 * 60 * 1000L;
  private static final String COOKIE = "Cookie";

  private final PathHelper pathHelper;
  private final ServerProperties serverProps;
  private final MP3Support mp3Support;
  private final Project project;
  private ASR asrScoring;
  private ASRWebserviceScoring webserviceScoring;
  private DecodeCorrectnessChecker decodeCorrectnessChecker;

  private AutoCRT autoCRT;

  private DatabaseServices db;
  private LogAndNotify logAndNotify;
  private boolean checkedLTS = false;

  /**
   * TODO : why would we want this?
   */
  private Map<String, Integer> phoneToCount;

  private AudioConversion audioConversion;
  private boolean hasModel;
  private Language language;

  private EnsureAudioHelper ensureAudioHelper;

  private final boolean removeAccents;


  /**
   * @param pathHelper
   * @param serverProperties
   * @param db
   * @param langTestDatabase
   * @see mitll.langtest.server.ScoreServlet#getAudioFileHelper
   * @see LangTestDatabaseImpl#init
   * @see Project#Project
   */
  public AudioFileHelper(PathHelper pathHelper,
                         ServerProperties serverProperties,
                         DatabaseServices db,
                         LogAndNotify langTestDatabase,
                         Project project) {
    this.pathHelper = pathHelper;
    this.serverProps = serverProperties;
    this.db = db;
    this.logAndNotify = langTestDatabase;

    this.mp3Support = new MP3Support(pathHelper);
    audioConversion = new AudioConversion(serverProps.shouldTrimAudio(), serverProperties.getMinDynamicRange());

    this.language = project.getLanguageEnum();
    removeAccents = language != Language.FRENCH;
    hasModel = project.hasModel();
    makeASRScoring(project);
    this.project = project;
    ensureAudioHelper = new EnsureAudioHelper(db, pathHelper);

    makeDecodeCorrectnessChecker();
  }

  /**
   * TODO : Buggy.
   *
   * @param transcript
   * @param transliteration
   * @return
   * @see Project#getPronunciationsFromDictOrLTS
   */
  public String getPronunciationsFromDictOrLTS(String transcript, String transliteration) {
    return getPronunciationLookup().getPronunciationsFromDictOrLTS(transcript, transliteration, true, false, new ArrayList<>()).getDict();
  }

  public String getPronunciationsFromDictOrLTSFull(String transcript, String transliteration) {
    return getPronunciationLookup().getPronunciationsFromDictOrLTS(transcript, transliteration, false, false, new ArrayList<>()).getDict();
  }

  public int getNumPhonesFromDictionary(String transcript, String transliteration) {
    return getPronunciationLookup().getNumPhonesFromDictionaryOrLTS(transcript, transliteration);
  }

  private IPronunciationLookup getPronunciationLookup() {
    return webserviceScoring.getPronunciationLookup();
  }

  public String getLM(String fl, boolean removeAllAccents) {
    return webserviceScoring.getLM(fl, removeAllAccents);
  }

  public String getHydraTranscript(String fl) {
    return webserviceScoring.getHydraTranscriptTest(fl);
  }

  /**
   * @return
   * @see mitll.langtest.server.scoring.ASRWebserviceScoring#getCollator
   */
  public Collator getCollator() {
    return asrScoring.getCollator();
  }

  /**
   * NOTE : has side effect of setting the number of phones!
   *
   * @param exercises
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExercises
   * @see ProjectManagement#configureProject
   */
  public void checkLTSAndCountPhones(Collection<CommonExercise> exercises) {
    if (asrScoring.isDictEmpty()) {
      logger.info("checkLTSAndCountPhones : not checking lts on exercises since dictionary is empty and this is probably the znetprof instance which has no dictionaries.");
    } else {
      synchronized (this) {
        if (!checkedLTS) {
          checkedLTS = true;
          int count = 0;

          long now = System.currentTimeMillis();
          phoneToCount = new HashMap<>();
          Set<Integer> safe = new HashSet<>();
          Set<Integer> unsafe = new HashSet<>();
          logger.info("checkLTSAndCountPhones : " + language + " checking " + exercises.size() + " exercises...");

          for (CommonExercise exercise : exercises) {
            boolean validForeignPhrase = isValidForeignPhrase(now, safe, unsafe, exercise);
            if (!validForeignPhrase) {
              if (count < 10) {
                logger.warn("checkLTSAndCountPhones : not a valid foreign phrase for " +
                    "\n\tex      " + exercise.getID() +
                    "\n\tenglish " + exercise.getEnglish() +
                    "\n\tfl      " + exercise.getForeignLanguage());
              }
              count++;
            } else {
              countPhones(exercise.getMutable());
            }

            // check context sentences
            for (ClientExercise context : exercise.getDirectlyRelated()) {
              CommonExercise commonExercise = context.asCommon();
              boolean validForeignPhrase2 = isValidForeignPhrase(now, safe, unsafe, commonExercise);
              if (commonExercise.isSafeToDecode() != validForeignPhrase2) {
                commonExercise.getMutable().setSafeToDecode(validForeignPhrase2);
              }
            }
          }

          long then = System.currentTimeMillis();
          logger.warn("checkLTSAndCountPhones took " + (then - now) + " millis to examine " + exercises.size() + " exercises.");

          if (!safe.isEmpty() || !unsafe.isEmpty()) {
            logger.info("checkLTSAndCountPhones marking " + safe.size() + " safe, " + unsafe.size() + " unsafe");
          }

          project.getExerciseDAO().markSafeUnsafe(safe, unsafe);
          long now2 = System.currentTimeMillis();

          logger.warn("checkLTSAndCountPhones took " + (now2 - then) + " millis to mark exercises safe/unsafe to decode.");
          if (count > 0) {
            logger.warn("checkLTSAndCountPhones huh? out of " + exercises.size() + " LTS fails on " + count);
          }
        }
      }
    }
  }

  /**
   * @param now
   * @param safe
   * @param unsafe
   * @param exercise
   * @return
   * @see #checkLTSAndCountPhones
   */
  private boolean isValidForeignPhrase(long now, Set<Integer> safe, Set<Integer> unsafe, CommonExercise exercise) {
    boolean validForeignPhrase = exercise.isSafeToDecode();
    if (isStale(now, exercise)// || exercise.getEnglish().equalsIgnoreCase("teacher")
    ) {
      validForeignPhrase = isInDictOrLTS(exercise);
//      logger.warn("isValidForeignPhrase valid " + validForeignPhrase + " ex " +exercise);
      (validForeignPhrase ? safe : unsafe).add(exercise.getID());
    }
    return validForeignPhrase;
  }

  private boolean isStale(long now, CommonExercise exercise) {
    return now - exercise.getLastChecked() > DAY;
  }

  /**
   * Why would we want to make a map of phone->childCount?
   *
   * @param exercise
   * @param <T>
   * @see #checkLTSAndCountPhones
   */
  private <T extends CommonShell & MutableExercise> void countPhones(T exercise) {
    PhoneInfo bagOfPhones = asrScoring.getBagOfPhones(exercise.getForeignLanguage());
    List<String> firstPron = bagOfPhones.getFirstPron();
    exercise.setFirstPron(firstPron);
    for (String phone : firstPron) {
      Integer integer = getPhoneToCount().get(phone);
      getPhoneToCount().put(phone, integer == null ? 1 : integer + 1);
    }
  }

  /**
   * @param foreignLanguagePhrase
   * @return
   * @seex mitll.langtest.server.services.ScoringServiceImpl#isValidForeignPhrase
   * @see InDictFilter#isPhraseInDict
   */
  public boolean checkLTSOnForeignPhrase(String foreignLanguagePhrase, String transliteration) {
    return asrScoring.validLTS(foreignLanguagePhrase, transliteration);
  }

  private boolean isInDictOrLTS(ClientExercise exercise) {
    return asrScoring.validLTS(exercise.getForeignLanguage(), exercise.getTransliteration());
  }

  public SmallVocabDecoder getSmallVocabDecoder() {
    return asrScoring.getSmallVocabDecoder();
  }

  /**
   * Write the wav file given the posted audio in base 64.
   * <p>
   * Record an answer entry in the database.<br></br>
   * Write the posted data to a wav and an mp3 file (since all the browser audio works with mp3).
   * <p>
   * Client references:
   *
   * @param base64EncodedString generated by flash on the client
   * @param fileInstead         - optional input file
   * @param exercise1           exerciseID within the plan - could be null if we're creating a new user exercise
   * @param options
   * @return URL to audio on server and if audio is valid (not too short, etc.)
   * @see RecordButton.RecordingListener#stopRecording
   * @see RecordButton.RecordingListener#stopRecording
   * @see mitll.langtest.server.services.AudioServiceImpl#writeAudioFile
   */
  public AudioAnswer writeAudioFile(String base64EncodedString,
                                    File fileInstead,
                                    ClientExercise exercise1,
                                    AudioContext audioContext,
                                    AnswerInfo.RecordingInfo recordingInfoInitial,

                                    DecoderOptions options) {
    String wavPath = fileInstead == null ? pathHelper.getAbsoluteToAnswer(audioContext) : fileInstead.getAbsolutePath();
    String relPath = pathHelper.getRelToAnswer(wavPath);

    File wavFile = new File(wavPath);
    String absolutePath = wavFile.getAbsolutePath();

    long then = System.currentTimeMillis();

    AudioCheck.ValidityAndDur validity = fileInstead == null ?
        audioConversion.convertBase64ToAudioFiles(base64EncodedString, wavFile, options.isRefRecording(), isQuietAudioOK()) :
        audioConversion.getValidityAndDur(fileInstead, options.isRefRecording(), isQuietAudioOK(), then);

    if (logger.isDebugEnabled()) {
      logger.debug("writeAudioFile writing" +
          "\n\tfor        " + audioContext +
          "\n\trec        " + recordingInfoInitial +
          "\n\twav        " + wavPath +
          "\n\tabs        " + absolutePath +
          "\n\tvalidity   " + validity +
          "\n\ttranscript " + recordingInfoInitial.getTranscript());
    }

    if (options.isRefRecording() && validity.isValid()) {
      // make sure there's a compressed version for later review.
      new Thread(() -> ensureCompressed(exercise1, audioContext, wavPath), "ensureCompressedAudio").start();
    }

    // remember who recorded this audio wavFile.
    rememberWhoRecordedAudio(audioContext, relPath, absolutePath);

    return getAudioAnswerDecoding(exercise1,
        audioContext,
        new AnswerInfo.RecordingInfo(recordingInfoInitial, wavFile.getPath()),

        relPath,
        wavFile,

        validity,
        options
    );
  }

  /**
   * So we can check later if they have permission to hear it.
   *
   * @param audioContext
   * @param relPath
   * @param absolutePath
   */
  private void rememberWhoRecordedAudio(AudioContext audioContext, String relPath, String absolutePath) {
    int userid = audioContext.getUserid();

    int projid = audioContext.getProjid();
    boolean match = projid == project.getID();
    if (!match) {
      logger.error("huh? audio context proj id " + projid + " but this project is " + project.getID());
    }
    Project project = match ? this.project : db.getProjectManagement().getProject(projid, false);
    project.addAnswerToUser(relPath, userid);  //not needed
    project.addAnswerToUser(absolutePath, userid);
  }

  private void ensureCompressed(ClientExercise exercise1, AudioContext audioContext, String wavPath) {
    String actualPath =
        ensureAudioHelper.ensureCompressedAudio(audioContext.getUserid(), exercise1, wavPath,
            audioContext.getAudioType(), language, new HashMap<>(), true);
    logger.info("ensureCompressed wav path " + wavPath + " compressed actual " + actualPath);
  }

/*
  @Deprecated
  public AudioAnswer writeAMASAudioFile(String base64EncodedString,
                                        AmasExerciseImpl exercise1,
                                        AudioContext audioContext,
                                        AnswerInfo.RecordingInfo recordingInfoInitial) {
    String wavPath = pathHelper.getAbsoluteToAnswer(audioContext);
    File file = new File(wavPath);
    //long then = System.currentTimeMillis();
    AudioCheck.ValidityAndDur validity =
        audioConversion.convertBase64ToAudioFiles(base64EncodedString, file, false, isQuietAudioOK());

    // logger.debug("writeAudioFile writing to " + file.getAbsolutePath() + " validity " + validity);
*/
/*    long now = System.currentTimeMillis();
    long diff = now - then;
    if (diff > MIN_WARN_DUR) {
      logger.debug("writeAudioFile: took " + diff + " millis to write wav file " + validity.durationInMillis +
          " millis long");
    }*//*


    return getAMASAudioAnswerDecoding(exercise1,
        audioContext,
        new AnswerInfo.RecordingInfo(recordingInfoInitial, file.getPath()),

        wavPath, file,

        validity);
  }
*/


  /**
   * Does decoding or alignment on audio file, against the text of the exercise.
   * <p>
   * TODO : this is misleading - if doFlashcard is true, it does decoding, otherwise it does *not* do alignment
   *
   * @param exercise
   * @param wavPath
   * @param file
   * @param deviceType
   * @param device
   * @param score
   * @param options
   * @param pretestScore
   * @return
   * @see mitll.langtest.server.scoring.JsonScoring#getAnswer
   */
  public AudioAnswer getAnswer(
      ClientExercise exercise,
      AudioContext audioContext,

      String wavPath, File file,
      String deviceType, String device,

      float score,
      DecoderOptions options,
      PretestScore pretestScore) {
    AudioCheck.ValidityAndDur validity = audioConversion.getAudioCheck().isValid(file, false, isQuietAudioOK());

    AnswerInfo.RecordingInfo recordingInfo = new AnswerInfo.RecordingInfo("", file.getPath(), deviceType, device, "", "");

    return options.shouldDoDecoding() ?
        getAudioAnswerDecoding(exercise,
            audioContext,
            recordingInfo,
            wavPath, file,
            validity,
            options) :
        getAudioAnswerAlignment(exercise,
            audioContext,
            recordingInfo,
            wavPath, file,
            validity,
            score, options, pretestScore)
        ;
  }

  /**
   * @param exercise
   * @param wavPath
   * @param file
   * @param validity
   * @return
   * @see #getAnswer
   * @see #writeAudioFile
   */
  private AudioAnswer getAudioAnswerDecoding(ClientExercise exercise,

                                             AudioContext context,
                                             AnswerInfo.RecordingInfo recordingInfo,

                                             String wavPath,
                                             File file,

                                             AudioCheck.ValidityAndDur validity,
                                             DecoderOptions decoderOptions
  ) {
    logValidity(context, file, validity);

//    logger.info("getAudioAnswerDecoding wavPath " + wavPath);
//    logger.info("getAudioAnswerDecoding file " + file.getName());

    AudioAnswer answer = getAudioAnswer(context.getReqid(), exercise, wavPath, file, validity,
        decoderOptions, context.getUserid());

    if (decoderOptions.isRecordInResults()) {
      double maxMinRange = validity.getDynamicRange();
      answer.setDynamicRange(maxMinRange);
      //  logger.info("min max " + maxMinRange + " answer " + answer);
      if (exercise != null) {
        answer.setTranscript(exercise.getForeignLanguage()); // TODO : necessary?
        //answer.setNormTranscript(answer.getNormTranscript());
      }
      //  logger.info("getAudioAnswerDecoding recordInResults answer " + answer);// + " " + answer.getTranscript());
      recordInResults(context, recordingInfo, validity, answer);

    } else {
      answer.setTranscript(recordingInfo.getTranscript());
    }
    //logger.debug("getAudioAnswerDecoding answer " + answer);
    return answer;
  }

  private void recordInResults(AudioContext context,
                               AnswerInfo.RecordingInfo recordingInfo,
                               AudioCheck.ValidityAndDur validity,
                               AudioAnswer answer) {
    PretestScore pretestScore = answer.getPretestScore();
    boolean hasScore = pretestScore != null;
    int processDur = hasScore ? pretestScore.getProcessDur() : 0;

    AnswerInfo info = new AnswerInfo(
        new AnswerInfo(
            context,
            recordingInfo,
            validity,
            getModelsDir()),

        new AnswerInfo.ScoreInfo(answer.isCorrect(), (float) answer.getScore(),
            new ScoreToJSON().getJsonFromAnswer(answer).toString(), processDur), getModelsDir());

    if (hasScore) {
      info.setNormTranscript(pretestScore.getRecoSentence());
    }
    rememberAnswer(context.getProjid(), answer, info, context.getDialogSessionID());
  }

  /**
   * Does alignment and decoding of one audio file.
   *
   * @param exercise
   * @param attribute
   * @param userID
   * @param absoluteFile
   * @see mitll.langtest.server.decoder.RefResultDecoder#doDecode
   */
  public void decodeOneAttribute(CommonExercise exercise, AudioAttribute attribute, int userID, File absoluteFile) {
    if (isInDictOrLTS(exercise)) {
      //  String audioRef = attribute.getAudioRef();
      //if (!audioRef.contains("context=")) {
      decodeAndRemember(exercise, attribute, true, userID, absoluteFile);
      // }
    } else {
      logger.warn("decodeOneAttribute skipping " + exercise.getID() + " since can't do decode/align b/c of LTS errors ");
    }
  }

  /**
   * @param exercise
   * @param attribute
   * @param doDecode
   * @param userID
   * @param absoluteFile
   * @return
   * @see mitll.langtest.server.services.ScoringServiceImpl#recalcRefAudioWithHelper
   */
  public PretestScore decodeAndRemember(CommonExercise exercise, AudioAttribute attribute,
                                        boolean doDecode, int userID, File absoluteFile) {
    String audioRef = attribute.getAudioRef();

    if (DEBUG) {
      logger.info("decodeAndRemember alignment " +
          "\n\texid    " + exercise.getID() +
          "\n\tfl      " + exercise.getForeignLanguage() +
          "\n\ten      " + exercise.getEnglish() +
          "\n\tcontext " + exercise.isContext() +
          "\n\tattr    " + attribute);
    }

//    boolean doHydec = false;
    // Do alignment...
    if (absoluteFile == null) {
      absoluteFile = pathHelper.getAbsoluteBestAudioFile(audioRef, language.getLanguage());
    }
    String absolutePath = absoluteFile.getAbsolutePath();

    DecoderOptions options = new DecoderOptions().setUsePhoneToDisplay(isUsePhoneToDisplay()).setDoDecode(false);

    PrecalcScores precalcScores = null;
    String transcript = attribute.getTranscript();

    if (!transcript.equalsIgnoreCase(exercise.getForeignLanguage())) {
      logger.warn("hmm, the audio transscript " + transcript + " doesn't match the exercise " + exercise.getForeignLanguage());
    }

    try {
      precalcScores = checkForWebservice(
          exercise.getID(),
          exercise.getEnglish(),
          transcript,
          exercise.getProjectID(),
          userID,
          absoluteFile);
    } catch (Exception e) {
      logger.error("Got " + e, e);
      logger.warn(
          "exercise " + exercise +
              "\n\tattribute    " + attribute +
              "\n\tuserID       " + userID +
              "\n\tabsoluteFile " + absoluteFile);
      return null;
    }

    PretestScore alignmentScore = precalcScores == null ? getAlignmentScore(exercise, absolutePath, options) :
        getPretestScoreMaybeUseCache(-1,
            absolutePath,
            transcript,
            exercise.getTransliteration(),
            ImageOptions.getDefault(),
            exercise.getID(),
            precalcScores,
            false);
    DecodeAlignOutput alignOutput = new DecodeAlignOutput(alignmentScore, false);

    // Do decoding, and record alignment info we just got in the database ...
    long durationInMillis = attribute.getDurationInMillis();
    AudioAnswer decodeAnswer = doDecode ? getDecodeAnswer(exercise, audioRef, absoluteFile, durationInMillis) : new AudioAnswer();

    DecodeAlignOutput decodeOutput = new DecodeAlignOutput(decodeAnswer, true);

    // Do decoding, and record alignment info we just got in the database ...
    //AudioAnswer decodeAnswerOld = doHydec ? getDecodeAnswer(exercise, audioRef, absoluteFile, durationInMillis, true) : new AudioAnswer();
    DecodeAlignOutput decodeOutputOld = new DecodeAlignOutput(new AudioAnswer(), true);

    //logger.debug("attr dur " + attribute.getDurationInMillis());

    getRefAudioAnswerDecoding(exercise,
        attribute.getUserid(),
        attribute.getUniqueID(),
        durationInMillis,

        alignOutput,
        decodeOutput,

        new DecodeAlignOutput(new PretestScore(), false),
        decodeOutputOld,

        attribute.isMale(),
        attribute.isRegularSpeed() ? REG : SLOW,
        getModelsDir());
    return alignmentScore;
  }

  private PretestScore getPretestScoreMaybeUseCache(int reqid, String testAudioFile, String sentence,
                                                    String transliteration, ImageOptions imageOptions, int exerciseID,
                                                    PrecalcScores precalcScores,
                                                    boolean usePhoneToDisplay1) {
    return getASRScoreForAudio(
        reqid,
        testAudioFile,
        sentence,
        transliteration,
        imageOptions,
        "" + exerciseID,
        precalcScores,
        new DecoderOptions()
            .setDoDecode(false)
            .setCanUseCache(serverProps.useScoreCache())
            .setUsePhoneToDisplay(usePhoneToDisplay1)
    );
  }

  /**
   * TODO : finish merge
   * TODO : finish merge
   * TODO : finish merge
   * <p>
   * Re-write info in result table, mark it with model and model update date
   * rewrite word and phone info for result.
   *
   * @param result
   * @param exercise
   * @param result
   * @param exercise
   * @return
   * @seex RefResultDecoder#recalcStudentAudio
   * @seex RefResultDecoder#recalcStudentAudio
   */
  public boolean recalcOne(Result result, CommonExercise exercise) {
    String audioRef = result.getAnswer();
    File absoluteFile = pathHelper.getAbsoluteFile(audioRef);

    int uniqueID = result.getUniqueID();

    if (result.getAudioType() == AudioType.PRACTICE) {//.equals("flashcard") || result.getAudioType().equals("avp")) {
      long durationInMillis = result.getDurationInMillis();
      AudioAnswer decodeAnswer = getDecodeAnswer(exercise, audioRef, absoluteFile, durationInMillis);
/*//      db.getPhoneDAO().removePhones(uniqueID);
//      db.getWordDAO().removeWords(uniqueID);
      db.rememberScore(uniqueID, decodeAnswer.getPretestScore(), decodeAnswer.isCorrect());
      logger.info("rememberScore for result " + uniqueID + " : decode " *//* +decodeAnswer.getPretestScore()*//*);
    } else {
      String absolutePath = absoluteFile.getAbsolutePath();
      PretestScore alignmentScore = getAlignmentScore(exercise, absolutePath, serverProps.usePhoneToDisplay(), false);
//      db.getPhoneDAO().removePhones(uniqueID);
//      db.getWordDAO().removeWords(uniqueID);
      db.rememberScore(uniqueID, alignmentScore, alignmentScore.getHydecScore() > 0.25);
      logger.info("rememberScore for result " + uniqueID + " : alignment " + alignmentScore);*/
      db.getPhoneDAO().removeForResult(uniqueID);
      db.getWordDAO().removeForResult(uniqueID);
      db.rememberScore(exercise.getProjectID(), uniqueID, decodeAnswer.getPretestScore(), decodeAnswer.isCorrect());
      logger.info("rememberScore for result " + uniqueID + " : decode " /* +decodeAnswer.getPretestScore()*/);
    } else {
      PretestScore alignmentScore = getEasyAlignment(exercise, absoluteFile.getAbsolutePath());
      db.getPhoneDAO().removeForResult(uniqueID);
      db.getWordDAO().removeForResult(uniqueID);
      db.rememberScore(exercise.getProjectID(), uniqueID, alignmentScore, alignmentScore.getHydecScore() > 0.25);
      logger.info("rememberScore for result " + uniqueID + " : alignment " + alignmentScore);
    }
    return true;
  }

  private boolean isUsePhoneToDisplay() {
    return serverProps.usePhoneToDisplay(project.getLanguageEnum());
  }

  private boolean isQuietAudioOK() {
    return serverProps.isQuietAudioOK();
  }

  /**
   * Really helpful - could have annotation info always at hand, so don't have to wait for it in learn tab...
   * AND we can send it along to the iPad and have it do highlighting of words and phones in time with the audio
   * I.E. Follow the bouncing ball.
   *
   * @param exercise1
   * @param user
   * @param audioid
   * @param duration
   * @param isMale
   * @param speed
   * @param model
   * @return
   * @paramx wavPath
   * @paramx numAlignPhones
   * @seex #decodeOneAttribute(CommonExercise, AudioAttribute, int)
   * @see #decodeAndRemember(CommonExercise, AudioAttribute, boolean, int, File)
   */
  private void getRefAudioAnswerDecoding(CommonExercise exercise1,
                                         int user,
                                         int audioid,
                                         long duration,
                                         DecodeAlignOutput alignOutput,
                                         DecodeAlignOutput decodeOutput,

                                         DecodeAlignOutput alignOutputOld,
                                         DecodeAlignOutput decodeOutputOld,

                                         boolean isMale,
                                         String speed,
                                         String model) {
    AudioCheck.ValidityAndDur validity = new AudioCheck.ValidityAndDur(duration);
    // logger.debug("validity dur " + validity.durationInMillis);

    if (alignOutput.isValid()) {
      db.getRefResultDAO().addAnswer(user, exercise1.getProjectID(), exercise1.getID(),
          audioid,
          validity.durationInMillis,

          decodeOutput.isCorrect(),

          alignOutput,
          decodeOutput,

          alignOutputOld,
          decodeOutputOld,

          isMale,
          speed,
          model);
      // TODO : add word and phone table for refs
      //	recordWordAndPhoneInfo(decodeAnswer, answerID);
      //   logger.debug("getRefAudioAnswerDecoding decodeAnswer " + decodeAnswer);
    } else {
      logger.warn("not writing to db since alignment output is not valid for audio " + audioid);
    }
  }

  private AudioAnswer getDecodeAnswer(ClientExercise exercise1,
                                      String wavPath,
                                      File file,
                                      long duration) {
    return getAudioAnswer(1,
        exercise1,
        wavPath, file, new AudioCheck.ValidityAndDur(duration),

        new DecoderOptions()
            .setDoDecode(true)
            .setCanUseCache(false)
            .setAllowAlternates(false)
        ,
        db.getUserDAO().getBeforeLoginUser());
  }

  private void logValidity(AudioContext context, File file, AudioCheck.ValidityAndDur validity) {
    logValidity(context.getExid(), context.getQuestionID(), context.getUserid(), file, validity);
  }

  private void logValidity(int exerciseID, int questionID, int user, File file, AudioCheck.ValidityAndDur validity) {
    if (!validity.isValid()) {
      logger.warn("logValidity : got invalid audio file (" + validity +
          ") user = " + user + " exerciseID " + exerciseID +
          " question " + questionID + " file " + file.getAbsolutePath());
    }
  }

  /**
   * @param exercise
   * @param wavPath
   * @param file
   * @param validity
   * @param score
   * @param pretestScore
   * @return
   * @see #getAnswer
   */
  private AudioAnswer getAudioAnswerAlignment(ClientExercise exercise,

                                              AudioContext context,
                                              AnswerInfo.RecordingInfo recordingInfo,

                                              String wavPath,
                                              File file,
                                              AudioCheck.ValidityAndDur validity,

                                              float score,
                                              DecoderOptions options,
                                              PretestScore pretestScore) {
    logValidity(context, file, validity);
    AudioAnswer answer = getAudioAnswer(context.getReqid(), exercise, wavPath, file, validity,
        options
            .setCanUseCache(true)
            .setAllowAlternates(false),
        context.getUserid());

    //  logger.info("getAudioAnswerAlignment 1 answer " + answer);

    if (options.isRecordInResults()) {
      if (answer.getPretestScore() == null) {
        // logger.info("getAudioAnswerAlignment set score to " + pretestScore);
        answer.setPretestScore(pretestScore);
      }

      AnswerInfo infoOrig = new AnswerInfo(
          context,
          recordingInfo,
          validity,
          getModelsDir());

      int processDur = answer.getPretestScore() == null ? 0 : answer.getPretestScore().getProcessDur();
      AnswerInfo info = new AnswerInfo(infoOrig,
          new AnswerInfo.ScoreInfo(true, score,
              new ScoreToJSON().getJsonFromAnswer(answer).toString(), processDur), getModelsDir());

      answer.setTranscript(exercise.getForeignLanguage());
      answer.setNormTranscript(answer.getPretestScore().getRecoSentence());

      rememberAnswer(info.getProjid(), answer, info, context.getDialogSessionID());
    }
    //logger.info("getAudioAnswerAlignment 2 answer " + answer);
    return answer;
  }

  private String getModelsDir() {
    return project.getModelsDir();
  }

  private void rememberAnswer(int projid, AudioAnswer answer, AnswerInfo info, int dialogSessionID) {
    long timestamp = System.currentTimeMillis();
    int answerID = db.getAnswerDAO().addAnswer(info, timestamp);
    answer.setResultID(answerID);
    answer.setTimestamp(timestamp);

    if (dialogSessionID > 0) {
      db.getRelatedResultDAO().add(answerID, dialogSessionID);
    }

    // do this db write later
    new Thread(() -> db.recordWordAndPhoneInfo(projid, answer, answerID), "recordWordAndPhoneInfo").start();
    //   return answerID;
  }

  /**
   * @return
   * @paramx exercise1
   * @paramx reqid
   * @paramx wavPath
   * @paramx file
   * @paramx validity
   * @paramxx isValid
   * @see #getAudioAnswerDecoding
   * @deprecated - no concept of a project here - a project has a model
   */
/*
  private AudioAnswer getAMASAudioAnswer(AmasExerciseImpl exercise1,

                                         int questionID,
                                         int reqid,

                                         String wavPath, File file,
                                         AudioCheck.ValidityAndDur validity) {
    String url = pathHelper.ensureForwardSlashes(wavPath);

    return (validity.isValid() && hasModel()) ?
        getAMASAudioAnswer(
            exercise1,
            questionID,
            reqid, file, validity, url) :
        new AudioAnswer(url, validity.getValidity(), reqid, validity.durationInMillis, exercise1.getID());
  }
*/
  private boolean hasModel() {
    return hasModel;
  }

  /**
   * Does decoding if doFlashcard is true.
   *
   * @param qid
   * @param reqid
   * @param file
   * @param validity
   * @param url
   * @return AudioAnswer with decode info attached, if doFlashcard is true
   * @see #writeAudioFile
   * @deprecatedx
   */
/*  private AudioAnswer getAMASAudioAnswer(AmasExerciseImpl exercise,
                                         int qid, int reqid,
                                         File file, AudioCheck.ValidityAndDur validity, String url) {
    AudioAnswer audioAnswer = new AudioAnswer(url, validity.getValidity(), reqid, validity.durationInMillis, exid);
    autoCRT.getAutoCRTDecodeOutput(exercise, qid, file, audioAnswer, true);
    return audioAnswer;
  }*/


  /**
   * Checks validity before alignment/decoding.
   *
   * @param reqid
   * @param commonShell
   * @param wavPath
   * @param file
   * @param validity    if audio isn't valid, don't do alignment or decoding
   * @param userID
   * @return
   * @see #getAudioAnswerDecoding
   */
  private AudioAnswer getAudioAnswer(int reqid,
                                     ClientExercise commonShell,
                                     String wavPath,
                                     File file,
                                     AudioCheck.ValidityAndDur validity,
                                     DecoderOptions decoderOptions,
                                     int userID) {
    String url = pathHelper.ensureForwardSlashes(wavPath);

    return (validity.isValid() && hasModel()) ?
        getAudioAnswer(
            commonShell,
            reqid, file, validity, url, decoderOptions, userID) :
        new AudioAnswer(url, validity.getValidity(), reqid, validity.durationInMillis, commonShell.getID());
  }

  private PretestScore getEasyAlignment(ClientExercise exercise, String testAudioPath) {
    DecoderOptions options = new DecoderOptions().setUsePhoneToDisplay(isUsePhoneToDisplay());
    return getAlignmentScore(exercise, testAudioPath, options);
  }

  /**
   * @return
   * @see #decodeOneAttribute(CommonExercise, AudioAttribute, int, File)
   */

  private PretestScore getAlignmentScore(ClientExercise exercise, String testAudioPath, DecoderOptions options) {
    if (DEBUG) {
      logger.info("getAlignmentScore alignment " +
          "\n\texid    " + exercise.getID() +
          "\n\tfl      " + exercise.getForeignLanguage() +
          "\n\ten      " + exercise.getEnglish() +
          "\n\tcontext " + exercise.isContext() +
          "\n\tattr    " + testAudioPath);
    }
    return getASRScoreForAudio(0, testAudioPath,
        exercise.getForeignLanguage(),
        exercise.getTransliteration(),
        NO_IMAGE_PLEASE, "" + exercise.getID(), null,
        options);
  }

  /**
   * Get score when doing autoCRT on an audio file.
   * <p>
   * TODO : why even generate images here???
   *
   * @param reqid
   * @param testAudioFile   audio file to score
   * @param lmSentences     to look for in the audio
   * @param transliteration for languages we can't do normal LTS on (Kanji characters or similar)
   * @param options
   * @param precalcScores
   * @return PretestScore for audio
   * @see DecodeCorrectnessChecker#getDecodeScore
   * @see AlignDecode#getASRScoreForAudio
   */
  @Override
  public PretestScore getASRScoreForAudio(int reqid,
                                          File testAudioFile,
                                          Collection<String> lmSentences,
                                          String transliteration,
                                          DecoderOptions options,
                                          PrecalcScores precalcScores) {
    String prefix = options.isUsePhoneToDisplay() ? "phoneToDisplay" : "";
    //String path = testAudioFile.getPath();

    String firstSentence = lmSentences.iterator().next();
//      logger.info("getASRScoreForAudio audio file path is " + path + " " + firstSentence);
    return getASRScoreForAudio(reqid, testAudioFile.getPath(), firstSentence, lmSentences, transliteration,
        DEFAULT, prefix, precalcScores,
        options);
  }

  /**
   * GOOD for local testing!
   *
   * @param exid
   * @param english
   * @param foreignLanguage
   * @param projid
   * @param userid
   * @param theFile
   * @return scores for the file
   * @see mitll.langtest.server.services.ScoringServiceImpl#getASRScoreForAudio
   */
  public PrecalcScores checkForWebservice(int exid,
                                          String english,
                                          String foreignLanguage,
                                          int projid,
                                          int userid,
                                          File theFile) {
    boolean available = isHydraAvailable();
    String hydraHost = serverProps.getHydraHost();
    if (!available) {
      logger.info("checkForWebservice local webservice not available" +
          "\n\tfor     " + theFile.getName() +
          "\n\tproject " + projid +
          "\n\texid    " + exid +
          "\n\tenglish " + english +
          "\n\titem    " + foreignLanguage +
          "\n\tuser    " + userid +
          "\n\thost    " + hydraHost
      );
    }
    if (!available && !theFile.getName().endsWith(OGG) && serverProps.isLaptop()) {
 /*
      logger.info("checkForWebservice exid    " + exid);
      logger.info("checkForWebservice projid  " + projid);
      logger.info("checkForWebservice userid  " + userid);
      logger.info("checkForWebservice theFile " + theFile.getAbsolutePath());
      logger.info("checkForWebservice exists  " + theFile.exists());
      */
      if (theFile.exists()) {

/*
        if (language.equalsIgnoreCase("spanish")) {
          logger.info("raw before " + foreignLanguage);
          foreignLanguage = foreignLanguage
              .replaceAll("Ud.", "usted")
              .replaceAll("Uds.", "ustedes");
          logger.info("raw after   " + foreignLanguage);
        }
*/
        PrecalcScores precalcScores = getProxyScore(english, foreignLanguage, userid, theFile, hydraHost);
        return (precalcScores != null && precalcScores.isDidRunNormally()) ? precalcScores : null;
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  /**
   * This is for testing from your laptop.
   *
   * @param english
   * @param foreignLanguage
   * @param userid
   * @param theFile
   * @param hydraHost
   * @return
   * @see #checkForWebservice(int, String, String, int, int, File)
   */
  @Nullable
  private PrecalcScores getProxyScore(String english,
                                      String foreignLanguage,
                                      int userid,
                                      File theFile,
                                      String hydraHost) {
    if (session == null) {
      session = getSession(hydraHost, project.getID());
    }
    ScoreServlet.PostRequest requestToServer = ScoreServlet.PostRequest.ALIGN;

    HTTPClient httpClient = getHttpClientForNetprofServer(english, foreignLanguage, userid, hydraHost, requestToServer);

    if (session != null) {
//      logger.info("getProxyScore adding session " + session);
      httpClient.addRequestProperty(COOKIE, session);
    }

    try {
      logger.info("getProxyScore asking remote netprof (" + hydraHost + ") to " +
          requestToServer +
          "\n\teng '" + english + "'" +
          "\n\tfl  '" + foreignLanguage + "'" +
          (language == Language.JAPANESE ? "\n\tsegmented '" + getSegmented(foreignLanguage) + "'" : "")
      );

      {
        List<WordAndProns> possibleProns = new ArrayList<>();

        logger.info("getProxyScore " +
            "\n\tfile " + theFile +
            "\n\tdict " + getHydraDict(foreignLanguage, possibleProns));
        //possibleProns.forEach(p -> logger.info(foreignLanguage + " : " + p));
      }

      String json = httpClient.sendAndReceiveAndClose(theFile);
      logger.info("getProxyScore response " + json);

      return json.equals(MESSAGE_NO_SESSION) ? new PrecalcScores(serverProps, language) : new PrecalcScores(serverProps, json, language);
    } catch (IOException e) {
      logger.error("checkForWebservice got " + e);
    }
    return null;
  }

  @NotNull
  private HTTPClient getHttpClientForNetprofServer(String english,
                                                   String foreignLanguage,
                                                   int userid,
                                                   String hydraHost,
                                                   ScoreServlet.PostRequest requestToServer) {
    boolean isDefault = project.getWebserviceHost().equalsIgnoreCase(WEBSERVICE_HOST_DEFAULT);

    HTTPClient httpClient = getHttpClient(hydraHost, isDefault ? "" : project.getWebserviceHost());
    httpClient.addRequestProperty(REQUEST.toString(), requestToServer.toString());
    httpClient.addRequestProperty(ENGLISH.toString(), english);
    httpClient.addRequestProperty(EXERCISE_TEXT.toString(), new String(Base64.getEncoder().encode(foreignLanguage.getBytes())));
    httpClient.addRequestProperty(LANGUAGE.toString(), getLanguage());
    httpClient.addRequestProperty(USER.toString(), "" + userid);
    httpClient.addRequestProperty(FULL.toString(), FULL.toString());  // full json returned
    return httpClient;
  }

  private TransNormDict getHydraDict(String foreignLanguage, List<WordAndProns> possibleProns) {
    String s = getSmallVocabDecoder().cleanToken(foreignLanguage, removeAccents);
    String cleaned = getSegmented(s); // segmentation method will filter out the UNK model
    return asrScoring.getHydraDict(cleaned.trim(), "", possibleProns);
  }

  @NotNull
  private HTTPClient getHttpClient(String hydraHost, String actualHydraHost) {
    String url = hydraHost + "scoreServlet";
    if (!actualHydraHost.isEmpty()) url += File.separator + actualHydraHost;
    return new HTTPClient(url);
  }

  private String session = null;

  /**
   * For laptop dev
   * <p>
   * Get session from netprof1-dev for demo/demo or equivalent.
   * We just need some session.
   *
   * @param hydraHost
   * @param projID
   * @return
   * @see #getProxyScore
   */
  private String getSession(String hydraHost, int projID) {
    try {
      HTTPClient httpClient = getHttpClient(hydraHost, "");
      httpClient.addRequestProperty(REQUEST.toString(), HASUSER.toString());
      httpClient.addRequestProperty(PROJID.toString(), "" + projID);
      httpClient.addRequestProperty(USERID.toString(), TEST_USER);
      httpClient.addRequestProperty(PASS.toString(), TEST_PASSWORD);
      String json = httpClient.sendAndReceiveCookie("");

//      logger.info("getSession response " + json);

      return json;
    } catch (IOException e) {
      logger.warn("Got " + e, e);
    }
    return "";
  }

  /**
   * @return
   * @see mitll.langtest.server.services.ScoringServiceImpl#isHydraRunning
   */
  public boolean isHydraAvailable() {
    return webserviceScoring.isAvailable();
  }

  public boolean isHydraAvailableCheckNow() {
    return webserviceScoring.isAvailableCheckNow();
  }

  /**
   * @see mitll.langtest.server.services.ScoringServiceImpl#isHydraRunning
   */
  public void setAvailable() {
    webserviceScoring.setAvailable();
  }

  /**
   * For now, we don't use a ref audio file, since we aren't comparing against a ref audio file with the DTW/sv pathway.
   *
   * @param reqid
   * @param testAudioFile
   * @param sentence        empty string when using lmSentences non empty and vice-versa
   * @param transliteration for languages we can't do normal LTS on (Kanji characters or similar)
   * @param prefix
   * @param options
   * @return PretestScore
   * @paramx precalcResult
   * @see mitll.langtest.server.services.ScoringServiceImpl#getPretestScore
   * @see mitll.langtest.server.services.ScoringServiceImpl#getResultASRInfo
   * @see AlignDecode#getASRScoreForAudio
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#scoreAudio
   **/
  public PretestScore getASRScoreForAudio(int reqid,
                                          String testAudioFile,
                                          String sentence,
                                          String transliteration,

                                          ImageOptions imageOptions,

                                          String prefix,
                                          PrecalcScores precalcScores,

                                          DecoderOptions options) {
    return getASRScoreForAudio(reqid,
        testAudioFile,
        sentence,
        null,
        transliteration,
        imageOptions,
        prefix,
        precalcScores,
        options);
  }

  /**
   * If trying asr webservice and it doesn't work, falls back to using hydec - {@link PretestScore#isRanNormally()}
   * `
   *
   * @param reqid
   * @param testAudioFile
   * @param sentence
   * @param lmSentences
   * @param transliteration for languages we can't do normal LTS on (Kanji characters or similar)
   * @param prefix
   * @param options
   * @return
   * @see #getASRScoreForAudio(int, String, String, String, ImageOptions, String, PrecalcScores, DecoderOptions)
   */
  private PretestScore getASRScoreForAudio(int reqid,
                                           String testAudioFile,
                                           String sentence,
                                           Collection<String> lmSentences,
                                           String transliteration,

                                           ImageOptions imageOptions,

                                           String prefix,
                                           PrecalcScores precalcScores,
                                           DecoderOptions options) {
    // alignment trumps decoding
    boolean shouldDoDecoding = options.shouldDoDecoding() && !options.shouldDoAlignment();
    logger.info("getASRScoreForAudio (" + getLanguage() + ")" +
        "\n\t" + (shouldDoDecoding ? "Decoding " : "Aligning ") +
        "" + testAudioFile +
        "\n\twith sentence '" + sentence + "'" +
        "\n\treq# " + reqid +
        (options.isCanUseCache() ? " check cache" : " NO CACHE") + " prefix " + prefix);

    if (testAudioFile == null) {
      logger.warn("getASRScoreForAudio huh? " + getLanguage() + " no test audio file for '" + sentence + "'");
      return new PretestScore(); // very defensive
    }
    testAudioFile = mp3Support.dealWithMP3Audio(testAudioFile);
    File originalFile = new File(testAudioFile);

// try to fix the path for old audio files.
    if (!originalFile.exists()) {
      String absolutePath = pathHelper.getAbsoluteAudioFile(testAudioFile).getAbsolutePath();
      File file = new File(absolutePath);
      if (!file.exists()) {
        String relPrefix = db.getDatabase().getRelPrefix(language.getLanguage());

        if (!testAudioFile.startsWith(relPrefix)) {
          String webPageAudioRefWithPrefix = db.getDatabase().getWebPageAudioRefWithPrefix(relPrefix, testAudioFile);
          logger.info("getASRScoreForAudio  no testAudioFile for " +
              "\n\tsentence " + sentence +
              "\n\tat       " + originalFile.getAbsolutePath() +
              "\n\ttrying   " + webPageAudioRefWithPrefix);
          testAudioFile = webPageAudioRefWithPrefix;
          originalFile = new File(testAudioFile);
        }
      }
    }

    if (!originalFile.exists()) {
      String absolutePath = pathHelper.getAbsoluteAudioFile(testAudioFile).getAbsolutePath();
      File file = new File(absolutePath);
      if (!file.exists()) {
        logger.error("getASRScoreForAudio huh? no testAudioFile for " +
            "\n\tsentence " + sentence +
            "\n\tat       " + originalFile.getAbsolutePath() +
            "\n\tnor      " + file.getAbsolutePath());
        return new PretestScore();
      } else {
        logger.info("getASRScoreForAudio : found " + testAudioFile + " at " + absolutePath);
      }
    }

    DirAndName testDirAndName = new DirAndName(testAudioFile, serverProps.getAudioBaseDir()).invoke();
    String testAudioName = testDirAndName.getName();
    String testAudioDir = testDirAndName.getDir();

    if (isEnglish()) {
      sentence = sentence.toUpperCase();  // hack for English
    }
    sentence = sentence.replaceAll(",", " ");
    sentence = getSentenceToUse(sentence);
    sentence = sentence.trim();

//    logger.info("getASRScoreForAudio : for " + testAudioName + " sentence '" + sentence + "' lm sentences '" + lmSentences + "'");
//    logger.info("getASRScoreForAudio : precalcScore " +precalcScores);

    PretestScore pretestScore = getASRScoring().scoreRepeat(
        testAudioDir, removeSuffix(testAudioName),
        sentence, lmSentences, transliteration,

        pathHelper.getImageOutDir(language.getLanguage()), imageOptions,
        shouldDoDecoding,
        options.isCanUseCache(), prefix,
        precalcScores,
        options.isUsePhoneToDisplay());

    pretestScore.setReqid(reqid);

    String json = new ScoreToJSON().asJson(pretestScore);
    logger.info("getASRScoreForAudio : json for pretest score " + pretestScore + " " + json);
    pretestScore.setJson(json);

    return pretestScore;
  }

  /**
   * Hack for percent sign in english - must be a better way.
   * Tried adding it to dict but didn't seem to work.
   *
   * @param sentence
   * @return
   */
  private String getSentenceToUse(String sentence) {
    boolean english = isEnglish() && sentence.equals("%") || sentence.equals("％");
    return english ? "percent" : sentence;
  }

  /**
   * OK : set from project
   *
   * @return
   */
  @Deprecated
  private String getLanguage() {
    return language.getLanguage();
  }

  private boolean isEnglish() {
    return language == Language.ENGLISH;
  }

  private String removeSuffix(String audioFile) {
    return audioFile.substring(0, audioFile.length() - SUFFIX_LENGTH);
  }

  /**
   * TODO : this whole approach is WAY TOO COMPLICATED - rework so it makes sense.
   * <p>
   * Does decoding if doFlashcard is true.
   *
   * @param reqid
   * @param file
   * @param validity
   * @param url
   * @param userID
   * @return AudioAnswer with decode info attached, if doFlashcard is true
   * @see #getAudioAnswer(int, ClientExercise, String, File, AudioCheck.ValidityAndDur, DecoderOptions, int)
   */
  private AudioAnswer getAudioAnswer(ClientExercise exercise,
                                     int reqid,
                                     File file,
                                     AudioCheck.ValidityAndDur validity,
                                     String url,
                                     DecoderOptions decoderOptions,
                                     int userID) {
    AudioAnswer audioAnswer = new AudioAnswer(url, validity.getValidity(), reqid, validity.durationInMillis, exercise.getID());
    if (decoderOptions.shouldDoAlignment()) {
      PrecalcScores precalcScores =
          checkForWebservice(
              exercise.getID(),
              exercise.getEnglish(),
              exercise.getForeignLanguage(),
              project.getID(),
              userID,
              file);

      String phraseToDecode = decodeCorrectnessChecker.getPhraseToDecode(exercise.getForeignLanguage(), language);
      PretestScore asrScoreForAudio = getASRScoreForAudio(reqid,
          file,
          Collections.singleton(phraseToDecode),
          "",
          decoderOptions,
          precalcScores);

      audioAnswer.setPretestScore(asrScoreForAudio);
      audioAnswer.setCorrect(audioAnswer.getScore() > MIN_SCORE_FOR_CORRECT_ALIGN &&
          audioAnswer.getPretestScore().isFullMatch());

      //  logger.info("align : validity " + audioAnswer.getValidity());

      return audioAnswer;
    } else if (decoderOptions.shouldDoDecoding()) {
      PrecalcScores precalcScores =
          checkForWebservice(
              exercise.getID(),
              exercise.getEnglish(),
              exercise.getForeignLanguage(),
              project.getID(),
              userID,
              file);

      PretestScore flashcardAnswer = decodeCorrectnessChecker.getDecodeScore(
          exercise,
          file,
          audioAnswer,
          language,
          decoderOptions,
          precalcScores);

      audioAnswer.setPretestScore(flashcardAnswer);

      //  logger.info("decoding : validity " + audioAnswer.getValidity());
      return audioAnswer;
    }

    return audioAnswer;
  }

  /**
   * @return TODO : why would we want this?
   */
  public Map<String, Integer> getPhoneToCount() {
    return phoneToCount;
  }

  /**
   * @return
   * @see AlignDecode#getASRScoreForAudio
   */
  private ASR getASRScoring() {
    return webserviceScoring;
  }

  private boolean isMacOrWin() {
    String property = System.getProperty("os.name").toLowerCase();
    return property.contains("mac") || property.contains("win");
  }

  /**
   * @param project
   * @see #AudioFileHelper
   */
  // TODO: gross
  private void makeASRScoring(Project project) {
    if (webserviceScoring == null) {
      String installPath = pathHelper.getInstallPath();
      webserviceScoring = new ASRWebserviceScoring(installPath, serverProps, logAndNotify,
          readDictionary(project), project);
    }
    asrScoring = webserviceScoring;
  }

  /**
   * JUST FOR TESTING
   *
   * @param transcript
   */
  public void runHydra(String transcript) {
    List<String> lmSentences = new ArrayList<>();
    lmSentences.add(transcript);
    webserviceScoring.runHydra("", transcript, "", lmSentences, "", true, 1000);
  }

  /**
   * @param project
   * @return
   * @see #makeASRScoring
   */
  @Nullable
  private HTKDictionary readDictionary(Project project) {
    HTKDictionary htkDictionary = null;
    try {
      htkDictionary = makeDict(project.getModelsDir());
    } catch (Exception e) {
      logger.error("readDictionary : got " + e, e);
    }
    return htkDictionary;
  }

  /**
   * @return
   * @see #makeASRScoring
   */
  private HTKDictionary makeDict(String modelsDir) {
/*
    logger.info("makeDict :" +
        "\n\tinstall path " + installPath +
        "\n\tmodelsDir    " + modelsDir);
        */
    String scoringDir = Scoring.getScoringDir(serverProps.getDcodrBaseDir());
    String dictFile =
        new ConfigFileCreator(serverProps.getProperties(), scoringDir, modelsDir).getDictFile();
//    logger.info("makeDict :" + "\n\tdictFile    " + dictFile);

    if (dictFile != null && new File(dictFile).exists()) {
      long then = System.currentTimeMillis();
      File file = new File(dictFile);
//      logger.info("makeDict read " + file.getAbsolutePath());
      HTKDictionary htkDictionary = new HTKDictionary(dictFile);
      long now = System.currentTimeMillis();
      int size = htkDictionary.size(); // force read from lazy val
      if (now - then > 4000) {
        logger.info("makeDict for " + getLanguage() + " read" +
            "\n\tdict " + dictFile + " of size " + size + " took " + (now - then) + " millis");
      }
      return htkDictionary;
    } else {
      if (hasModel()) {
        if (new File(serverProps.getMediaDir()).exists()) {
          logger.warn("\n----->>>> makeDict : Can't find dict file at " + dictFile);
          logger.warn("\nThis is an error if you see this on hydra or hydra2 and not a problem on netprof.");
        } else {
          logger.debug("makeDict : NOTE: Can't find dict file at " + dictFile);
        }
      } else {
        logger.info("---> makeDict : Can't find dict file at " + dictFile);
      }
      return new HTKDictionary();
    }
  }

  /**
   * @param relativeConfigDir
   * @paramx studentAnswersDB
   * @paramx crtScoring
   * @see LangTestDatabaseImpl#init()
   */
  public AutoCRT makeAutoCRT(String relativeConfigDir) {
    if (autoCRT == null) {
//      logger.debug("lang " + langTestDatabase);
//      logger.debug("serverProps " + serverProps);
//      if (langTestDatabase == null) {
//        logger.warn("skipping set install path...");
//      } else {
//        langTestDatabase.setInstallPath(serverProps.getUseFile(), studentAnswersDB);
//      }
      return makeClassifier(relativeConfigDir);
    } else return autoCRT;
  }

  private AutoCRT makeClassifier(String relativeConfigDir) {
    //  Export export = studentAnswersDB.getExport();
    autoCRT = new AutoCRT(null, this, new InDictFilter(this),
        pathHelper.getInstallPath(), relativeConfigDir,
        serverProps.getMinPronScore(), serverProps.getMiraFlavor(), serverProps.getMiraClassifierURL(),
        serverProps.useMiraClassifier(), serverProps);
//    autoCRT.makeClassifier();
    return autoCRT;
  }

  /**
   * @see #AudioFileHelper
   */
  private void makeDecodeCorrectnessChecker() {
    decodeCorrectnessChecker = new DecodeCorrectnessChecker(this, serverProps.getMinPronScore(), getSmallVocabDecoder());
  }

  /**
   * If a language is in development and has no model, there will be no dictionary...?
   *
   * @return
   */
  public boolean hasDict() {
    return !asrScoring.isDictEmpty();
  }

  public ASR getASR() {
    return asrScoring;
  }

  /**
   * @see AlignDecode#getASRScoreForAudio
   */
  private static class DirAndName {
    private final String testAudioFile;
    private final String installPath;
    private String testAudioName;
    private String testAudioDir;

    DirAndName(String testAudioFile, String installPath) {
      this.testAudioFile = testAudioFile;
      this.installPath = installPath;
    }

    public String getName() {
      return testAudioName;
    }

    public String getDir() {
      return testAudioDir;
    }

    DirAndName invoke() {
      File testAudio = new File(testAudioFile);
      testAudioName = testAudio.getName();
      if (testAudio.getParent().startsWith(installPath)) {
        testAudioDir = testAudio.getParent().substring(installPath.length());
      } else {
        testAudioDir = testAudio.getParent();
      }

      return this;
    }
  }

  /**
   * JUST FOR TESTING
   *
   * @param transcript
   * @return
   */
  public String getSegmented(String transcript) {
    return asrScoring.getSegmented(transcript);
  }
}