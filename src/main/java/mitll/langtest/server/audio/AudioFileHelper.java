/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.audio;

import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.result.AudioTag;
import mitll.langtest.server.*;
import mitll.langtest.server.autocrt.DecodeCorrectnessChecker;
import mitll.langtest.server.database.AnswerInfo;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.audio.EnsureAudioHelper;
import mitll.langtest.server.database.dialog.IDialogSessionDAO;
import mitll.langtest.server.database.exercise.IProject;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.project.ProjectManagement;
import mitll.langtest.server.scoring.*;
import mitll.langtest.server.trie.TextEntityValue;
import mitll.langtest.server.trie.Trie;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ModelType;
import mitll.langtest.shared.project.OOVInfo;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.DecoderOptions;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.PretestScore;
import mitll.npdata.dao.SlickDialogSession;
import mitll.npdata.dao.SlickPerfResult;
import mitll.npdata.dao.lts.HTKDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import scala.Option;
import scala.collection.Iterator;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

import static mitll.langtest.server.ScoreServlet.GetRequest.HASUSER;
import static mitll.langtest.server.ScoreServlet.HeaderValue.*;
import static mitll.langtest.server.audio.AudioConversion.LANGTEST_IMAGES_NEW_PRO_F_1_PNG;

public class AudioFileHelper implements AlignDecode {
  private static final Logger logger = LogManager.getLogger(AudioFileHelper.class);

  private static final String REG = "reg";
  private static final String SLOW = "slow";
  private static final int SUFFIX_LENGTH = ("." + AudioTag.COMPRESSED_TYPE).length();
  private static final ImageOptions DEFAULT = ImageOptions.getDefault();
  private static final ImageOptions NO_IMAGE_PLEASE = new ImageOptions(-1, -1, false, false);
  private static final String MESSAGE_NO_SESSION = "{\"message\":\"no session\"}";
  private static final String OGG = "ogg";

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_PRON = false;

  private static final double MIN_SCORE_FOR_CORRECT_ALIGN = 0.35;
  /**
   * @see #getSession
   */
  private static final String TEST_USER = "student";//"demo_";
  private static final String TEST_PASSWORD = "domino22";//"demo";
  private static final String COOKIE = "Cookie";
  private static final String SCORE_SERVLET = "scoreServlet";
  private static final String DEFAULT_EXID = "2";

  private static final boolean DEBUG_EDIT = false;
  //private static final boolean DO_KALDI_OOV = false;
  private static final String PERCENT_SYMBOL = "%";

  private final PathHelper pathHelper;
  private final ServerProperties serverProps;
  private final MP3Support mp3Support;
  private final Project project;
  private ASR asrScoring;
  private DecodeCorrectnessChecker decodeCorrectnessChecker;

  private final DatabaseServices db;
  private final LogAndNotify logAndNotify;
  private boolean checkedLTS = false;

  /**
   * TODO : why would we want this?
   */
  private Map<String, Integer> phoneToCount;

  private final AudioConversion audioConversion;
  private final Language language;

  private final EnsureAudioHelper ensureAudioHelper;

  private final boolean removeAccents;
  /**
   * @see #makeDict(String)
   */
  private long dictModified = 0L;
  private HTKDictionary htkDictionary;

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
    this.project = project;
    this.pathHelper = pathHelper;
    this.serverProps = serverProperties;
    this.db = db;
    this.logAndNotify = langTestDatabase;

    this.mp3Support = new MP3Support(pathHelper);
    ServletContext context = pathHelper.getContext();
    String realPath = context == null ? LANGTEST_IMAGES_NEW_PRO_F_1_PNG : context.getRealPath(LANGTEST_IMAGES_NEW_PRO_F_1_PNG);
    audioConversion = new AudioConversion(serverProps.shouldTrimAudio(), serverProperties.getMinDynamicRange(), realPath);

    this.language = project.getLanguageEnum();
    removeAccents = language != Language.FRENCH;
    ensureAudioHelper = new EnsureAudioHelper(db, pathHelper);
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
    if (!transcript.isEmpty()) throw new IllegalArgumentException("for " + transcript + " = " + transliteration);
    return getPronunciationLookup().getNumPhonesFromDictionaryOrLTS(transcript, transliteration);
  }

  private IPronunciationLookup getPronunciationLookup() {
    return getASRScoring().getPronunciationLookup();
  }

  /**
   * @return
   * @see mitll.langtest.server.scoring.ASRWebserviceScoring#getCollator
   */
  public Collator getCollator() {
    return getASRScoring().getCollator();
  }

  /**
   * NOTE : has side effect of setting the number of phones!
   *
   * @param exercises
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExercises
   * @see ProjectManagement#configureProject
   */
  public int checkLTSAndCountPhones(Collection<CommonExercise> exercises) {
    if (getASRScoring().isDictEmpty()) {
      logger.info("checkLTSAndCountPhones : not checking lts on exercises for " + project.getLanguage() + " " + project.getName() +
          " since dictionary is empty " +
          "and this is probably the znetprof instance which has no dictionaries.");
      return 0;
    } else {
      synchronized (this) {
        if (!checkedLTS) {
          checkedLTS = true;
          return checkOOV(exercises, false).getChecked();
        } else {
          return 0;
        }
      }
    }
  }

  public OOVInfo checkOOV(Collection<CommonExercise> exercises, boolean includeKaldi) {
    // int count = 0;

    long now = System.currentTimeMillis();
    phoneToCount = new HashMap<>();
    Set<Integer> safe = new HashSet<>();
    Set<String> oov = new HashSet<>();
    Set<Integer> unsafe = new HashSet<>();
    logger.info("checkLTSAndCountPhones : " + language + " checking " + exercises.size() + " exercises...");

    OOVInfo checkInfo = new OOVInfo(0, 0);
    try {
      checkInfo = checkAllExercises(exercises, safe, oov, unsafe, includeKaldi);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }

    writeOOV(oov, "oov");

    long then = System.currentTimeMillis();

    if (then - now > 100) {
      logger.warn("checkLTSAndCountPhones took " + (then - now) + " millis to examine " + exercises.size() + " exercises.");
    }

    if (!safe.isEmpty() || !unsafe.isEmpty()) {
      logger.info("checkLTSAndCountPhones " + language + " marking " + safe.size() + " safe, " + unsafe.size() + " unsafe");
    }

    project.getExerciseDAO().markSafeUnsafe(safe, unsafe, dictModified);
    long now2 = System.currentTimeMillis();

    if (now2 - then > 100) {
      logger.warn("checkLTSAndCountPhones took " + (now2 - then) + " millis to mark exercises safe/unsafe to decode.");
    }

    if (checkInfo.getOovWords() > 0) {
      logger.warn("checkLTSAndCountPhones NOTE : out of " + exercises.size() + " dict and LTS fails on " + checkInfo.getOovWords());
    } else {
      logger.info("checkLTSAndCountPhones out of " + exercises.size() + " dict and LTS fails on " + checkInfo.getOovWords());
    }
    return checkInfo;
  }

  /**
   * Does context sentences too.
   *
   * @param count
   * @param exercises
   * @param safe
   * @param oov
   * @param unsafe
   * @param includeKaldi
   * @return
   */
  private OOVInfo checkAllExercises(Collection<CommonExercise> exercises,
                                    Set<Integer> safe, Set<String> oov, Set<Integer> unsafe,
                                    boolean includeKaldi) {
    int checked = 0;
    int count = 0;
    logger.info("checkAllExercises for " + exercises.size() + " ex, force " + includeKaldi);

    long then = System.currentTimeMillis();
    if (dictModified > 0 || includeKaldi) {
      for (CommonExercise exercise : exercises) {
        if (isStale(exercise) || includeKaldi) {
//          if (spew2++ < 10 || spew2 % 100 == 0)
//            logger.info("checkAllExercises for " + exercise.getForeignLanguage() + " last checked " + exercise.getLastChecked() + " vs dict modified " + dictModified);
//          return exercise.getLastChecked() != dictModified;

          boolean validForeignPhrase = isValidForeignPhrase(safe, unsafe, exercise, oov, includeKaldi);
          //  logger.info("checkLTSAndCountPhones : " + language + " oov " + oov.size());
          checked++;
          if (!validForeignPhrase) {
            if (count < 10 || count % 100 == 0) {
              logger.warn("checkLTSAndCountPhones : " + language +
                  " (" + count +
                  ") (" + oov.size() +
                  ") not a valid foreign phrase for " +
                  "\n\tex      " + exercise.getID() +
                  "\n\tenglish " + exercise.getEnglish() +
                  "\n\tfl      " + exercise.getForeignLanguage());
            }
            count++;
          }
          //else {
          //countPhones(exercise.getMutable());
          // }

          // check context sentences
          for (ClientExercise context : exercise.getDirectlyRelated()) {
            CommonExercise commonExercise = context.asCommon();
            boolean validForeignPhrase2 = isValidForeignPhrase(safe, unsafe, commonExercise, oov, includeKaldi);
            checked++;

            if (commonExercise.isSafeToDecode() != validForeignPhrase2) {
              commonExercise.getMutable().setSafeToDecode(validForeignPhrase2);
            }
          }
        }
      }
      logger.info("checkAllExercises (" + project.getName() +
          ") checked " + checked + " from " + exercises.size() + " for whether they can be decoded in " + (System.currentTimeMillis() - then) + " millis");
      return new OOVInfo(checked, oov.size());
    } else {
      logger.info("checkAllExercises skipped checking exercises since no dictionary.");
      return new OOVInfo(0, oov.size());
    }
  }

  private void writeOOV(Set<String> oov, String suffix) {
    if (!oov.isEmpty()) {
      try {
        String fileName = "Project_" + suffix + project.getLanguage() + "_" + project.getName() + ".txt";
        File file = new File("/tmp/" + fileName);
        logger.info("writeOOV writing " + oov.size() + " oov items to " + file.getAbsolutePath());
        FileWriter oovTokens = new FileWriter(file);
        ArrayList<String> strings = new ArrayList<>(oov);
        strings.sort(Comparator.naturalOrder());
        for (String token : strings) {
          try {
            oovTokens.write(token + "\n");
          } catch (IOException e) {
            break;
          }
        }
        oovTokens.close();
      } catch (IOException e) {
        logger.error("writeOOV got " + e);
      }
    } else {
      logger.info("writeOOV oov is empty for " + project);

      Set<String> oov1 = getPronunciationLookup().getOOV();
      if (!oov1.isEmpty()) {
        writeOOV(oov1, suffix);
      }
    }
  }


  /**
   * Call Kaldi all the time for now.
   * <p>
   * TODO : Consider what to do with english sentences inside an interpreter dialog...
   *
   * @param safe
   * @param unsafe
   * @param exercise
   * @param oovCumulative
   * @param includeKaldi
   * @return
   * @see #checkLTSAndCountPhones
   */
  private boolean isValidForeignPhrase(Set<Integer> safe, Set<Integer> unsafe, CommonExercise exercise, Set<String> oovCumulative, boolean includeKaldi) {
    boolean isKaldi = project.getModelType() == ModelType.KALDI;
    if (isKaldi) {
      if (includeKaldi) {
        if (!exercise.hasEnglishAttr()) {
          Collection<String> kaldiOOV = getASRScoring().getKaldiOOV(exercise.getForeignLanguage());
          oovCumulative.addAll(kaldiOOV);
          return kaldiOOV.isEmpty();
        } else {
          return true;
        }
      } else {
        return true;
      }
    } else {
      boolean validForeignPhrase = true;// = exercise.isSafeToDecode();
      // if (isStale(exercise)) {
      //  logger.info("isValidForeignPhrase STALE ex " + exercise.getProjectID()  + "  " + exercise.getID() + " " + new Date(exercise.getLastChecked()) + " vs " + new Date(dictModified));
      // int before = oov.size();
      if (!exercise.getForeignLanguage().isEmpty()) {
        Collection<String> oov = getASRScoring().getOOV(exercise.getForeignLanguage(), exercise.getTransliteration());
        oovCumulative.addAll(oov);
        validForeignPhrase = oov.isEmpty();
      }
      // validForeignPhrase = isValidForeignPhrase(exercise);
//      if (oov.size() > before) {
//        logger.info("isValidForeignPhrase " + project.getName() + " oov now " + oov.size());
//      }

      if (!validForeignPhrase && DEBUG) {
        logger.info("isValidForeignPhrase valid " + validForeignPhrase + " ex " + exercise.getForeignLanguage());
      }
      (validForeignPhrase ? safe : unsafe).add(exercise.getID());
      //  }
      return validForeignPhrase;
    }
  }

  public boolean isValidForeignPhrase(ClientExercise exercise) {
    return getASRScoring().validLTS(exercise.getForeignLanguage(), exercise.getTransliteration());
  }

  /**
   * Only check the exercise again if the dictionary we're reading from has changed.
   *
   * @param exercise
   * @return
   */
  private boolean isStale(CommonExercise exercise) {
    return exercise.getLastChecked() < dictModified;
  }

  /**
   * Why would we want to make a map of phone->childCount?
   *
   * @param exercise
   * @param <T>
   * @see #checkLTSAndCountPhones
   */
/*
  private <T extends CommonShell & MutableExercise> void countPhones(T exercise) {
    PhoneInfo bagOfPhones = getASRScoring().getBagOfPhones(exercise.getForeignLanguage());
    java.util.List<String> firstPron = bagOfPhones.getFirstPron();
    exercise.setFirstPron(firstPron);
    for (String phone : firstPron) {
      Integer integer = getPhoneToCount().get(phone);
      getPhoneToCount().put(phone, integer == null ? 1 : integer + 1);
    }
  }
*/

  /**
   * @param foreignLanguagePhrase
   * @return
   * @seex mitll.langtest.server.services.ScoringServiceImpl#isValidForeignPhrase
   * @see InDictFilter#isPhraseInDict
   */
  public Collection<String> checkLTSOnForeignPhrase(String foreignLanguagePhrase, String transliteration) {
    return getASRScoring().getOOV(foreignLanguagePhrase, transliteration);
  }

  public SmallVocabDecoder getSmallVocabDecoder() {
    return getASRScoring().getSmallVocabDecoder();
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
   * @see mitll.langtest.server.services.AudioServiceImpl#getAudioAnswer
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
    String path = wavFile.getPath();

    long then = System.currentTimeMillis();

    AudioCheck.ValidityAndDur validity = fileInstead == null ?
        audioConversion.convertBase64ToAudioFiles(base64EncodedString, wavFile, options.isRefRecording(), isQuietAudioOK()) :
        audioConversion.getValidityAndDur(fileInstead, options.isRefRecording(), isQuietAudioOK(), then);

    if (logger.isInfoEnabled()) {
      logger.info("writeAudioFile writing" +
          "\n\tfor        " + audioContext +
          "\n\trec        " + recordingInfoInitial +
          "\n\twav        " + wavPath +
          "\n\trelPath    " + relPath +
//          "\n\tpath       " + path +
//          "\n\tabs        " + absolutePath +
          "\n\tvalidity   " + validity +
          "\n\ttranscript " + recordingInfoInitial.getTranscript());
    }

    if (options.isRefRecording() && validity.isValid()) {
      // make sure there's a compressed version for later review.
      new Thread(() -> ensureCompressed(exercise1, audioContext, wavPath), "ensureCompressedAudio").start();
    }

    // remember who recorded this audio wavFile.
    rememberWhoRecordedAudio(audioContext, relPath, absolutePath);

    AnswerInfo.RecordingInfo recordingInfo = new AnswerInfo.RecordingInfo(recordingInfoInitial, path);

    //  logger.info("writeAudioFile recordingInfo " + recordingInfo);

    return getAudioAnswerDecoding(exercise1,
        audioContext,
        recordingInfo,

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
    //logger.info("rememberWhoRecordedAudio " + audioContext);

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

    if (DEBUG) logger.info("ensureCompressed wav path " + wavPath + " compressed actual " + actualPath);
  }

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
    if (validity.getValidity() == Validity.MIC_DISCONNECTED) {
      logger.warn("getAnswer : got mic disconnected for " + wavPath + " with dur " + validity.getDurationInMillis() + " range " + validity.getDynamicRange());
    }
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

    if (processDur == 0) logger.warn("recordInResults proces dur = 0 for " + pretestScore);

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
    if (isValidForeignPhrase(exercise)) {
      decodeAndRemember(exercise, attribute, true, userID, absoluteFile, language);
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
   * @param projectLanguage could be we're doing english audio inside of chinese project
   * @return
   * @see mitll.langtest.server.services.ScoringServiceImpl#recalcRefAudioWithHelper
   */
  public PretestScore decodeAndRemember(CommonExercise exercise, AudioAttribute attribute,
                                        boolean doDecode, int userID, File absoluteFile,
                                        Language projectLanguage) {
    String audioRef = attribute.getAudioRef();

    if (DEBUG) {
      logger.info("decodeAndRemember alignment " +
          "\n\texid    " + exercise.getID() +
          "\n\tfl      " + exercise.getForeignLanguage() +
          "\n\ten      " + exercise.getEnglish() +
          "\n\tcontext " + exercise.isContext() +
          "\n\tattr    " + attribute);
    }

    // Do alignment...
    if (absoluteFile == null) {
      // String language = this.language.getLanguage();
      absoluteFile = pathHelper.getAbsoluteBestAudioFile(audioRef, projectLanguage);
    }
    String absolutePath = absoluteFile.getAbsolutePath();

    DecoderOptions options = new DecoderOptions().setUsePhoneToDisplay(isUsePhoneToDisplay()).setDoDecode(false);

    PrecalcScores precalcScores = null;
    String transcript = attribute.getTranscript();

    if (!transcript.equalsIgnoreCase(exercise.getForeignLanguage())) {
      logger.warn("decodeAndRemember hmm, the audio transcript " + transcript + " doesn't match the exercise " + exercise.getForeignLanguage());
    }

    try {
      Language language = getLanguage(exercise);
      precalcScores = checkForWebservice(
          exercise.getID(),
          exercise.getEnglish(),
          transcript,
          exercise.getProjectID(),
          userID,
          absoluteFile,
          language);
    } catch (Exception e) {
      logger.error("decodeAndRemember Got " + e, e);
      logger.warn(
          "decodeAndRemember exercise " + exercise +
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

    logger.info("decodeAndRemember : returning " + alignmentScore);
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

  private Language getLanguage(CommonExercise exercise) {
    boolean b = exercise.hasEnglishAttr();
/*    if (b)
      logger.info("exercise " + exercise.getID() + " " + exercise.getEnglish() + " " + exercise.getForeignLanguage() + " has english lang attr");*/
    return b ? Language.ENGLISH : this.language;
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
            .setUsePhoneToDisplay(usePhoneToDisplay1),
        isKaldi());
  }

  public boolean isKaldi() {
    return project.getModelType() == ModelType.KALDI;
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
   * @deprecated
   */
/*
  private boolean recalcOne(Result result, CommonExercise exercise) {
    String audioRef = result.getAnswer();
    File absoluteFile = pathHelper.getAbsoluteFile(audioRef);

    int uniqueID = result.getUniqueID();

    if (result.getAudioType() == AudioType.PRACTICE) {//.equals("flashcard") || result.getAudioType().equals("avp")) {
      long durationInMillis = result.getDurationInMillis();
      AudioAnswer decodeAnswer = getDecodeAnswer(exercise, audioRef, absoluteFile, durationInMillis);
*/
/*
//      db.getPhoneDAO().removePhones(uniqueID);
//      db.getWordDAO().removeWords(uniqueID);
      db.rememberScore(uniqueID, decodeAnswer.getPretestScore(), decodeAnswer.isCorrect());
      logger.info("rememberScore for result " + uniqueID + " : decode " *//*
   */
  /* +decodeAnswer.getPretestScore()*//*
   */
/*);
    } else {
      String absolutePath = absoluteFile.getAbsolutePath();
      PretestScore alignmentScore = getAlignmentScore(exercise, absolutePath, serverProps.usePhoneToDisplay(), false);
//      db.getPhoneDAO().removePhones(uniqueID);
//      db.getWordDAO().removeWords(uniqueID);
      db.rememberScore(uniqueID, alignmentScore, alignmentScore.getHydecScore() > 0.25);
      logger.info("rememberScore for result " + uniqueID + " : alignment " + alignmentScore);
      *//*

      db.getPhoneDAO().removeForResult(uniqueID);
      db.getWordDAO().removeForResult(uniqueID);
      db.rememberScore(exercise.getProjectID(), uniqueID, decodeAnswer.getPretestScore(), decodeAnswer.isCorrect());
      logger.info("rememberScore for result " + uniqueID + " : decode " */
  /* +decodeAnswer.getPretestScore()*//*
);
    } else {
      PretestScore alignmentScore = getEasyAlignment(exercise, absoluteFile.getAbsolutePath());
      db.getPhoneDAO().removeForResult(uniqueID);
      db.getWordDAO().removeForResult(uniqueID);
      db.rememberScore(exercise.getProjectID(), uniqueID, alignmentScore, alignmentScore.getHydecScore() > 0.25);
      logger.info("rememberScore for result " + uniqueID + " : alignment " + alignmentScore);
    }
    return true;
  }
*/
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
   * @see #decodeAndRemember(CommonExercise, AudioAttribute, boolean, int, File, Language)
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
          validity.getDurationInMillis(),

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
   * @see mitll.langtest.server.scoring.JsonScoring#getAnswer
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

  /**
   * @param projid
   * @param answer
   * @param info
   * @param dialogSessionID
   * @see #recordInResults
   * @see #getAudioAnswerAlignment
   */
  private void rememberAnswer(int projid, AudioAnswer answer, AnswerInfo info, int dialogSessionID) {
    long timestamp = System.currentTimeMillis();
    int answerID = db.getAnswerDAO().addAnswer(info, timestamp);
    long now = System.currentTimeMillis();

    if (now - timestamp > 10) logger.info("rememberAnswer took " + (now - timestamp));
    answer.setResultID(answerID);
//    logger.info("rememberAnswer " + new Date(timestamp) + " " + timestamp);

    answer.setTimestamp(timestamp);

    if (dialogSessionID > 0) {
      new Thread(() -> {
        db.getRelatedResultDAO().add(answerID, dialogSessionID);
        updateDialogSessionWithAnswer(dialogSessionID, timestamp);
      }, "rememberSessionInfo").start();
    }

    // do this db write later
    new Thread(() -> db.recordWordAndPhoneInfo(projid, answer, answerID), "recordWordAndPhoneInfo").start();
  }

  /**
   * TODO : speaking rate.
   *
   * @param dialogSessionID
   * @param timestamp
   */
  private void updateDialogSessionWithAnswer(int dialogSessionID, long timestamp) {
    // find all the results for this session - group by exid, take the latest one in group
    // then update the num answers, avg score, and maybe speaking rate?
    // would have to find min, max of word scores for each result id...
    java.util.List<SlickPerfResult> latestResultsForDialogSession = db.getResultDAO().getLatestResultsForDialogSession(dialogSessionID);
    int size = latestResultsForDialogSession.size();
    float num = (float) size;
    float total = 0F;
    for (SlickPerfResult perfResult : latestResultsForDialogSession) {
      total += perfResult.pronscore();
    }

    IDialogSessionDAO dialogSessionDAO = db.getDialogSessionDAO();
    SlickDialogSession slickDialogSession = dialogSessionDAO.byID(dialogSessionID);
    if (slickDialogSession == null) {
      logger.warn("\n\n\nrememberAnswer nothing with " + dialogSessionID);
    } else {
      if (DEBUG) {
        logger.info("updateDialogSessionWithAnswer : now " + dialogSessionID + " num " + size + " score " + total);
      }

      dialogSessionDAO
          .update(new SlickDialogSession(
              slickDialogSession.id(),
              slickDialogSession.userid(),
              slickDialogSession.projid(),
              slickDialogSession.dialogid(),
              slickDialogSession.modified(),
              new Timestamp(timestamp),
              slickDialogSession.view(),
              slickDialogSession.status(),
              size,
              total / num,
              0F));
    }
  }

  private boolean hasModel() {
    return project.hasModel();
  }

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
        new AudioAnswer(url, validity.getValidity(), reqid, validity.getDurationInMillis(), commonShell.getID());
  }

//  private PretestScore getEasyAlignment(ClientExercise exercise, String testAudioPath) {
//    DecoderOptions options = new DecoderOptions().setUsePhoneToDisplay(isUsePhoneToDisplay());
//    return getAlignmentScore(exercise, testAudioPath, options);
//  }

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
        options, isKaldi());
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
        options, isKaldi());
  }

  private boolean noted = false;

  /**
   * GOOD for local testing!
   *
   * @param exid
   * @param english
   * @param foreignLanguage
   * @param projid
   * @param userid
   * @param theFile
   * @param language
   * @return scores for the file
   * @see mitll.langtest.server.services.ScoringServiceImpl#getASRScoreForAudio
   */
  public PrecalcScores checkForWebservice(int exid,
                                          String english,
                                          String foreignLanguage,
                                          int projid,
                                          int userid,
                                          File theFile, Language language) {
    boolean available = isHydraAvailable();
    String hydraHost = serverProps.getHydraHost();
    if (!available && !noted) {
      noted = true;
      logger.info("checkForWebservice local webservice not available" +
          "\n\tfor     " + theFile.getName() +
          "\n\tproject " + projid +
          "\n\texid    " + exid +
          "\n\tenglish " + english +
          "\n\titem    " + foreignLanguage +
          "\n\tuser    " + userid +
          "\n\tlanguage " + language +
          "\n\thost    " + hydraHost
      );
      if (theFile.getName().endsWith("mp3")) logger.warn("don't send mp3!");
    }
    if (!available && !theFile.getName().endsWith(OGG) && serverProps.useProxy()) {
      //    logger.info("checkForWebservice exid    " + exid);
   /*   logger.info("checkForWebservice projid  " + projid);
      logger.info("checkForWebservice userid  " + userid);
      logger.info("checkForWebservice theFile " + theFile.getAbsolutePath());
      logger.info("checkForWebservice exists  " + theFile.exists());
      */
      if (theFile.exists()) {
        PrecalcScores precalcScores = getProxyScore(english, foreignLanguage, userid, theFile, hydraHost, language);
        return (precalcScores != null && precalcScores.isDidRunNormally()) ? precalcScores : null;
      } else {
//        logger.info("checkForWebservice no file at " + theFile.getAbsolutePath());
        return null;
      }
    } else {
//      logger.info("checkForWebservice available " +available + " laptop " +serverProps.useProxy() +" "+serverProps.getHostName());

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
   * @param language
   * @return
   * @see #checkForWebservice(int, String, String, int, int, File, Language)
   */
  @Nullable
  private PrecalcScores getProxyScore(String english,
                                      String foreignLanguage,
                                      int userid,
                                      File theFile,
                                      String hydraHost,
                                      Language language) {
    if (session == null || session.isEmpty()) {
      session = getSession(hydraHost, project.getID());
    }
    HTTPClient httpClient = getHttpClientForNetprofServer(english, foreignLanguage, userid, hydraHost, ScoreServlet.PostRequest.ALIGN, language);

    if (session != null) {
//      logger.info("getProxyScore adding session '" + session + "'");
      httpClient.addRequestProperty(COOKIE, session);
    }

    try {
      logger.info("getProxyScore asking remote netprof (" + hydraHost + ") to " +
          ScoreServlet.PostRequest.ALIGN +
          "\n\teng      '" + english + "'" +
          "\n\tsession  '" + session + "'" +
          "\n\tlanguage " + language +
          "\n\tfl       '" + foreignLanguage + "'" +
          (language == Language.JAPANESE ? "\n\tsegmented '" + getSegmented(foreignLanguage) + "'" : "")
      );

      if (DEBUG_PRON) {
        List<WordAndProns> possibleProns = new ArrayList<>();

        logger.info("getProxyScore " +
            "\n\tfile " + theFile +
            "\n\tdict " + getHydraDict(foreignLanguage, possibleProns));
        possibleProns.forEach(p -> logger.info(foreignLanguage + " : " + p));
      }

      long then = System.currentTimeMillis();
      String json = httpClient.sendAndReceiveAndClose(theFile);
      long now = System.currentTimeMillis();
      logger.info("getProxyScore took " + (now - then) + " to get " +
          "\n\tresponse " + json.trim());

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
                                                   ScoreServlet.PostRequest requestToServer,
                                                   Language language) {
    boolean isDefault = project.getWebserviceHost().equalsIgnoreCase(IProject.WEBSERVICE_HOST_DEFAULT);

    HTTPClient httpClient = getHttpClient(hydraHost, isDefault ? "" : project.getWebserviceHost());
    httpClient.addRequestProperty(REQUEST.toString(), requestToServer.toString());
    httpClient.addRequestProperty(ENGLISH.toString(), english);
    httpClient.addRequestProperty(EXERCISE.toString(), DEFAULT_EXID);
    httpClient.addRequestProperty(EXERCISE_TEXT.toString(), new String(Base64.getEncoder().encode(foreignLanguage.getBytes())));

    httpClient.addRequestProperty(LANGUAGE.toString(), language.name().toLowerCase());
    httpClient.addRequestProperty(USER.toString(), "" + userid);
    httpClient.addRequestProperty(FULL.toString(), FULL.toString());  // full json returned
    return httpClient;
  }

  @NotNull
  private HTTPClient getHttpClient(String hydraHost, String actualHydraHost) {
    String url = hydraHost + SCORE_SERVLET;
    if (!actualHydraHost.isEmpty()) url += File.separator + actualHydraHost;
    return new HTTPClient(url);
  }

  private TransNormDict getHydraDict(String foreignLanguage, java.util.List<WordAndProns> possibleProns) {
    // String s = getSmallVocabDecoder().lcToken(foreignLanguage, removeAccents);
    String cleaned = getSegmented(getSmallVocabDecoder().lcToken(foreignLanguage, removeAccents)); // segmentation method will filter out the UNK model
    return getASRScoring().getHydraDict(cleaned.trim(), "", possibleProns);
  }

  /**
   *
   */
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
      logger.info("getSession " +
          "\n\thost   " + hydraHost +
          "\n\tprojID " + projID +
          "\n\tuser   " + TEST_USER +
          "\n\tpass   " + TEST_PASSWORD
      );

      HTTPClient httpClient = getHttpClient(hydraHost, "");
      httpClient.addRequestProperty(REQUEST.toString(), HASUSER.toString());
      httpClient.addRequestProperty(USERID.toString(), TEST_USER);
      httpClient.addRequestProperty(PASS.toString(), TEST_PASSWORD);

      String json = httpClient.sendAndReceiveCookie("");
//      logger.info("getSession response " + json);
      return json;
    } catch (IOException e) {
      logger.warn("getSession Got " + e, e);
    }
    return "";
  }

  /**
   * @return
   * @see mitll.langtest.server.services.ScoringServiceImpl#isHydraRunning
   */
  public boolean isHydraAvailable() {
    return getASRScoring().isAvailable();
  }

  public boolean isHydraAvailableCheckNow() {
    return getASRScoring().isAvailableCheckNow();
  }

  /**
   * @see mitll.langtest.server.services.ScoringServiceImpl#isHydraRunning
   */
  public void setAvailable() {
    getASRScoring().setAvailable();
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
   * @param kaldi
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

                                          DecoderOptions options, boolean kaldi) {
    return getASRScoreForAudio(reqid,
        testAudioFile,
        sentence,
        null,
        transliteration,
        imageOptions,
        prefix,
        precalcScores,
        options, kaldi);
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
   * @param kaldi
   * @return
   * @seex #getASRScoreForAudio(int, String, String, String, ImageOptions, String, PrecalcScores, DecoderOptions)
   */
  private PretestScore getASRScoreForAudio(int reqid,
                                           String testAudioFile,
                                           String sentence,
                                           Collection<String> lmSentences,
                                           String transliteration,

                                           ImageOptions imageOptions,

                                           String prefix,
                                           PrecalcScores precalcScores,
                                           DecoderOptions options,
                                           boolean kaldi) {
    // alignment trumps decoding
    long then = System.currentTimeMillis();

    boolean shouldDoDecoding = options.shouldDoDecoding() && !options.shouldDoAlignment();
    logger.info("getASRScoreForAudio (" + getLanguage() + ")" +
            "\n\t" + (shouldDoDecoding ? "Decoding " : "Aligning ") +
            (kaldi ? "\n\tKALDI!" : "") +
            "" + testAudioFile +
            "\n\twith sentence '" + sentence + "'" +
//        "\n\treq# " + reqid +
            (prefix.isEmpty() ? "" : " prefix " + prefix)
    );


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

    long now = System.currentTimeMillis();

    if (now - then > 10) {
      logger.info("getASRScoreForAudio : prep " +
          "\n\ttook    " + (now - then));
    }

    PretestScore pretestScore = getASRScoring().scoreRepeat(
        testAudioDir, removeSuffix(testAudioName),
        sentence, lmSentences, transliteration,

        pathHelper.getImageOutDir(language.getLanguage()), imageOptions,
        shouldDoDecoding,
        options.isCanUseCache(), prefix,
        precalcScores,
        options.isUsePhoneToDisplay(), kaldi);

    pretestScore.setReqid(reqid);

    {
      String json = new ScoreToJSON().asJson(pretestScore);

      now = System.currentTimeMillis();

      if (now - then > 60) {
        logger.info("getASRScoreForAudio : for " +
            "\n\ttook    " + (now - then) +
            "\n\tpretest " + pretestScore +
            "\n\tjson    " + json);
      }

      pretestScore.setJson(json);
    }

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
    boolean english = isEnglish() && sentence.equals(PERCENT_SYMBOL) || sentence.equals("％");
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
    if (exercise == null) {
      return new AudioAnswer();
    }

    AudioAnswer audioAnswer = new AudioAnswer(url, validity.getValidity(), reqid, validity.getDurationInMillis(), exercise.getID());
    boolean b = exercise.asCommon().hasEnglishAttr();

    Language language = b ? Language.ENGLISH : this.language;
    //logger.info("getAudioAnswer Ex " + exercise.getID() + " " + exercise.getEnglish() + " " + exercise.getForeignLanguage() + " language " + language);
    if (decoderOptions.shouldDoAlignment()) {
      PrecalcScores precalcScores =
          checkForWebservice(
              exercise.getID(),
              exercise.getEnglish(),
              exercise.getForeignLanguage(),
              project.getID(),
              userID,
              file,
              language);

      String phraseToDecode = getChecker().getPhraseToDecode(exercise.getForeignLanguage(), language);

      AudioFileHelper toUse = language == Language.ENGLISH ? getEnglishAudioFileHelper(language) : this;

      PretestScore asrScoreForAudio = toUse.getASRScoreForAudio(reqid,
          file,
          Collections.singleton(phraseToDecode),
          "",
          decoderOptions,
          precalcScores);

//      logger.info("getAudioAnswer for file " + file + " asr score " + asrScoreForAudio);

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
              file,
              language);

      PretestScore flashcardAnswer = getChecker().getDecodeScore(
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

  private AudioFileHelper getEnglishAudioFileHelper(Language language) {
    List<Project> matchingProjects = db.getProjectManagement().getMatchingProjects(language, false);
    return matchingProjects.isEmpty() ? this : matchingProjects.get(0).getAudioFileHelper();
  }

  /**
   * @return TODO : why would we want this?
   * @see ScoreServlet#getExerciseSorter(int)
   * @see AudioFileHelper#countPhones(CommonShell)
   * @see mitll.langtest.server.database.DatabaseImpl#getJSONExport(int)
   */
//  public Map<String, Integer> getPhoneToCount() {
//    return phoneToCount;
//  }

  /**
   * @return
   * @see AlignDecode#getASRScoreForAudio
   */
  private ASR getASRScoring() {
    makeASRScoring(project);
    return asrScoring;
  }

  public void reloadScoring(Project project) {
    if (asrScoring != null) {
      makeASRScoring(project);
    }
  }

  /**
   * @param project
   * @see #AudioFileHelper
   */
  // TODO: gross
  private void makeASRScoring(Project project) {
    if (asrScoring == null) {
      logger.info("makeASRScoring for " + project.getName());
      String installPath = pathHelper.getInstallPath();
      HTKDictionary htkDictionary = readDictionary(project);
      asrScoring = new ASRWebserviceScoring(installPath, serverProps, logAndNotify,
          htkDictionary,
          project);
    }
  }

  public void makeOneEditMap() {
    scala.collection.Set<String> words = htkDictionary.keySet();
    //Iterable<scala.collection.immutable.List<String[]>> prons = htkDictionary.values();
    Iterator<String> iterator = words.iterator();
    java.util.Set<String> wset = new HashSet<>();
    while (iterator.hasNext()) wset.add(iterator.next());

    Map<List<String>, List<WP>> pronsToWords = new HashMap();
    wset.forEach(w -> {
      Option<scala.collection.immutable.List<String[]>> listOption = htkDictionary.get(w);
      if (listOption.isDefined()) {
        scala.collection.immutable.List<String[]> list = listOption.get();
        Iterator<String[]> iterator1 = list.iterator();
        java.util.List<List<String>> jprons = new ArrayList<>();
        while (iterator1.hasNext()) jprons.add(Arrays.asList(iterator1.next()));
        jprons.forEach(jp -> {
          List<WP> wps = pronsToWords.computeIfAbsent(jp, k -> new ArrayList<>());
          wps.add(new WP(w, jp));
        });
      }
    });
    Map<Integer, List<List<String>>> lengthToProns = pronsToWords.keySet().stream().collect(Collectors.groupingBy(List::size));


//    Set<Integer> inOrder = new TreeSet<>(lengthToProns.keySet());
    List<Integer> integers = Arrays.asList(2, 3, 4);
    integers.forEach(len -> {
      logger.info("len " + len);
      List<List<String>> pronsOfLength = lengthToProns.get(len);

      Trie<WP> trie = new Trie<>();
      trie.startMakingNodes();

      pronsOfLength.forEach(pron -> {
        List<WP> wps = pronsToWords.get(pron);
        wps.forEach(wp -> trie.addEntryToTrie(new Wrapper<WP>(wp.getEntry(), wp)));
//        WP wp = new WP(word, pron);
        // trie.addEntryToTrie(new Wrapper<WP>(wp.getEntry(), wp));
      });
      trie.endMakingNodes();

      Trie<WP> rtrie = new Trie<>();
      rtrie.startMakingNodes();

      pronsOfLength.forEach(pron -> {
        List<WP> wps = pronsToWords.get(pron);
        wps.forEach(wp1 -> {
          List<String> strings = new ArrayList<>(pron);
          // logger.info("\t" + word + " " + pron);
          Collections.reverse(strings);
          WP wp = new WP(wp1.getWord(), strings);
          rtrie.addEntryToTrie(new Wrapper<WP>(wp.getEntry(), wp));
        });


      });
      rtrie.endMakingNodes();


      if (len > 1) {
        Map<Integer, List<WP>> posToMatches = new HashMap<>();
        // List<List<String>> lists = pronsOfLength.subList(0, 5);
        pronsOfLength.forEach(pron -> {
          List<WP> wps = pronsToWords.get(pron);
          logger.info("for pron (" + len + ") " + pron + " found " + wps.size());
          wps.forEach(toFind -> {
            String word = toFind.getWord();
//            logger.info("\t" + word + " " + pron);
            for (int i = 1; i < len; i++) {
              List<String> allButSuffix = pron.subList(0, i);
              if (DEBUG_EDIT) logger.info(i + " prefix " + allButSuffix);

              List<String> after = pron.subList(i + 1, len);
              if (!after.isEmpty()) {
                if (DEBUG_EDIT) logger.info("After " + after);
              }
              WP wp = new WP(word, allButSuffix);

              List<WP> collect = matchesNotSelf(trie, wp, toFind.getPron());
              if (DEBUG_EDIT) logger.info("match (" + i + ") " + wp + " (" + collect.size() + ") " + collect);

              final int fi = i;
              List<WP> finalList = collect;
              if (!after.isEmpty()) {
                finalList = collect.stream().filter(c -> {
                  List<String> suffix = c.getPron().subList(fi + 1, len);
                  boolean match = suffix.containsAll(after);
                  if (DEBUG_EDIT && match) {
                    logger.info("suffix (" + fi + ") " + suffix + " vs " + after + " = " + match);
                  }
                  return match;
                }).collect(Collectors.toList());
              }

              if (DEBUG_EDIT)
                logger.info("for (" + i + ") (" + pron + ") : " + wp + " (" + finalList.size() + ") : " + finalList);
              List<WP> copy = new ArrayList<>();
              finalList.forEach(f -> copy.add(new WP(f.getWord(), f.getPron(), fi)));
              posToMatches.put(fi, finalList);
            }
            //logger.info("all for " + word + " " + pron + " : " + posToMatches);

            List<String> prefix = new ArrayList<>(pron.subList(1, pron.size()));

            {
              Collections.reverse(prefix);
              WP wp2 = new WP(word, prefix);
              List<WP> back = new ArrayList<>();

              List<String> pron1 = wp2.getPron(); //??? todo : correct???
              matchesNotSelf(rtrie, wp2, pron1).forEach(ww -> {
                List<String> copy = new ArrayList<>(ww.getPron());
                Collections.reverse(copy);
                back.add(new WP(ww.getWord(), copy));
              });
              if (DEBUG_EDIT) logger.info("r for " + wp2 + " (" + back.size() + ") " + back);

              List<WP> copy = new ArrayList<>();
              back.forEach(f -> copy.add(new WP(f.getWord(), f.getPron())));
              posToMatches.put(0, copy);
            }

            logger.info("for " + word + " " + pron);// + " : " + posToMatches);
            posToMatches.forEach((k, v) -> logger.info("\t" + k + " (" + v.size() + ") " + v));
          });


        });
      }
    });

  }

  @NotNull
  private List<WP> matchesNotSelf(Trie<WP> trie, WP wp, List<String> overallPron) {
    String entry = wp.getEntry();
    Collection<WP> matchesLC = trie.getMatchesLC(entry);
    //  List<String> pron = wp.getPron();
    String word = wp.getWord();
    boolean debug = (word.equalsIgnoreCase("niveau"));
    if (DEBUG_EDIT || debug) logger.info("match " + word + " " + wp);
    List<WP> collect = matchesLC.stream()
        .filter(w ->
        {
          boolean diffWord = !w.getWord().equals(word);
          boolean diffPron = !w.getPron().equals(overallPron);
          return diffWord &&
              diffPron;
        })
        .collect(Collectors.toList());
    if (DEBUG_EDIT || debug) logger.info("match for " + entry + " " + collect);
    return collect;
  }

  class WP {
    private final String word;
    private final List<String> pron;
    private int pos = -1;

    WP(String word, List<String> pron) {
      this.word = word;
      this.pron = pron;
    }

    WP(String word, List<String> pron, int pos) {
      this(word, pron);
      this.pos = pos;
    }

    String getWord() {
      return word;
    }

    List<String> getPron() {
      return pron;
    }

    String getEntry() {
      StringBuilder builder = new StringBuilder();
      pron.forEach(p -> builder.append(p).append("-"));
      return builder.toString();
    }

    public String toString() {
      String s = pos == -1 ? "" : " at " + pos;

      return getWord() + " " + getPron() + s;
    }

//    public int getPos() {
//      return pos;
//    }
//
//    public void setPos(int pos) {
//      this.pos = pos;
//    }
  }

  private static class Wrapper<T extends WP> implements TextEntityValue<T> {
    private final String value;
    private final T e;

    Wrapper(String value, T e) {
      this.value = value;
      this.e = e;
    }

    @Override
    public T getValue() {
      return e;
    }

    @Override
    public String getNormalizedValue() {
      return value;
    }

    public String toString() {
      return "e " + e.getWord() + " : " + value;
    }
  }

/*
  @NotNull
  private Trie<WP> makePronTrie(Collection<WP> results) {
    Trie<WP> trie = new Trie<>();
    trie.startMakingNodes();

    for (WP result : results) {
      trie.addEntryToTrie(new Wrapper(result.getEntry(), result));
    }
    trie.endMakingNodes();
    return trie;
  }
*/

  /**
   * JUST FOR TESTING
   *
   * @param transcript
   */
  public void runHydra(String transcript) {
    java.util.List<String> lmSentences = new ArrayList<>();
    lmSentences.add(transcript);
    getASRScoring().runHydra("", transcript, "", lmSentences, "", true, 1000);
  }

  /**
   * @param project
   * @return
   * @see #makeASRScoring
   */
  @Nullable
  private HTKDictionary readDictionary(Project project) {
    if (this.htkDictionary != null) {
      return this.htkDictionary;
    } else {
      HTKDictionary htkDictionary = null;
      try {
        htkDictionary = makeDict(project.getModelsDir());
        this.htkDictionary = htkDictionary;
      } catch (Exception e) {
        logger.error("readDictionary : got " + e, e);
      }
      return htkDictionary;
    }
  }

  /**
   * Check the file modified date to determine if should check for OOV.
   *
   * @return
   * @see #makeASRScoring
   */
  private HTKDictionary makeDict(String modelsDir) {
    // logger.info("makeDict :" +        "\n\tmodelsDir    " + modelsDir);
    String dictFile = getDictFile(modelsDir);
    //  logger.info("makeDict :" + "\n\tdictFile    " + dictFile);

    if (dictFile != null && new File(dictFile).exists()) {
      long then = System.currentTimeMillis();
      File file = new File(dictFile);
      this.dictModified = file.lastModified();

      logger.info("makeDict (" + project.getID() + " " + project.getName() + " " + project.getLanguageEnum().toDisplay() +
          ") read " + file.getAbsolutePath() + " with mod date " + new Date(dictModified));

      HTKDictionary htkDictionary = new HTKDictionary(dictFile);
      long now = System.currentTimeMillis();
      int size = htkDictionary.size(); // force read from lazy val
      if (now - then > 10) {
        logger.info("makeDict for " + getLanguage() + " read" +
            " dict " + dictFile + " of size " + size + " took " + (now - then) + " millis");
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

  private String getDictFile(String modelsDir) {
    return new ConfigFileCreator(
        serverProps.getProperties(),
        Scoring.getScoringDir(serverProps.getDcodrBaseDir()),
        modelsDir)
        .getDictFile();
  }


  private DecodeCorrectnessChecker getChecker() {
    if (decodeCorrectnessChecker == null) {
      makeDecodeCorrectnessChecker();
    }
    return decodeCorrectnessChecker;
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
    return !getASRScoring().isDictEmpty();
  }

  public ASR getASR() {
    return getASRScoring();
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

    String getName() {
      return testAudioName;
    }

    String getDir() {
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
    return getASRScoring().getSegmented(transcript);
  }
}