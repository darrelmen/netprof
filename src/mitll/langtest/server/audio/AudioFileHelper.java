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

import corpus.HTKDictionary;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.autocrt.AutoCRT;
import mitll.langtest.server.autocrt.DecodeCorrectnessChecker;
import mitll.langtest.server.database.AnswerInfo;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.decoder.RefResultDecoder;
import mitll.langtest.server.scoring.*;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.MutableExercise;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.PretestScore;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.io.File;
import java.text.Collator;
import java.util.*;

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
  private static final Logger logger = Logger.getLogger(AudioFileHelper.class);
  private static final String POSTED_AUDIO = "postedAudio";
  //  private static final int MIN_WARN_DUR = 30;
  private static final String REG = "reg";
  private static final String SLOW = "slow";
  private static final int SUFFIX_LENGTH = ("." + AudioTag.COMPRESSED_TYPE).length();
  public static final ImageOptions DEFAULT = ImageOptions.getDefault();

  private final PathHelper pathHelper;
  private final ServerProperties serverProps;
  private final MP3Support mp3Support;
  private ASR asrScoring;
  private ASRScoring oldschoolScoring;
  private ASRWebserviceScoring webserviceScoring;
  private DecodeCorrectnessChecker decodeCorrectnessChecker;

  private AutoCRT autoCRT;

  private final DatabaseImpl db;
  private final LogAndNotify logAndNotify;
  private boolean checkedLTS = false;
  private Map<String, Integer> phoneToCount;
  private boolean useOldSchoolServiceOnly = false;
  private final AudioConversion audioConversion;

  /**
   * @param pathHelper
   * @param serverProperties
   * @param db
   * @param langTestDatabase
   * @see mitll.langtest.server.ScoreServlet#getAudioFileHelper()
   * @see LangTestDatabaseImpl#init
   */
  public AudioFileHelper(PathHelper pathHelper, ServerProperties serverProperties, DatabaseImpl db,
                         LogAndNotify langTestDatabase) {
    this.pathHelper = pathHelper;
    this.serverProps = serverProperties;
    this.db = db;
    this.logAndNotify = langTestDatabase;
    this.useOldSchoolServiceOnly = serverProperties.getOldSchoolService();
    this.mp3Support = new MP3Support(pathHelper, serverProperties);
    audioConversion = new AudioConversion(serverProps);
    makeASRScoring();
    makeDecodeCorrectnessChecker();
  }

  /**
   * @return
   * @see mitll.langtest.server.scoring.ASRScoring#getCollator
   */
  public Collator getCollator() {
    makeASRScoring();
    return asrScoring.getCollator();
  }

  /**
   * NOTE : has side effect of setting the number of phones!
   *
   * @param exercises
   * @see LangTestDatabaseImpl#getExercises()
   */
  public void checkLTSAndCountPhones(Collection<CommonExercise> exercises) {
    synchronized (this) {
      if (!checkedLTS) {
        checkedLTS = true;
        int count = 0;
        makeASRScoring();

        phoneToCount = new HashMap<String, Integer>();
        for (CommonExercise exercise : exercises) {
          boolean validForeignPhrase = isInDictOrLTS(exercise);
          if (!validForeignPhrase) {
            if (count < 10) {
              logger.error("huh? for " + exercise.getID() + " we can't parse " + exercise.getID() +
                  " " + exercise.getEnglish() + " fl '" + exercise.getForeignLanguage() + "'");
            }
            count++;
          } else {
            countPhones(exercise.getMutable());
          }
        }

        if (count > 0) {
          logger.error("huh? out of " + exercises.size() + " LTS fails on " + count);
        }
      }
    }
  }

  private boolean isInDictOrLTS(CommonShell exercise) {
    return asrScoring.validLTS(exercise.getForeignLanguage(), exercise.getTransliteration());
  }

  /**
   * @param exercise
   * @param <T>
   * @see #checkLTSAndCountPhones
   */
  private <T extends CommonShell & MutableExercise> void countPhones(T exercise) {
    ASR.PhoneInfo bagOfPhones = asrScoring.getBagOfPhones(exercise.getForeignLanguage());
    exercise.setBagOfPhones(bagOfPhones.getPhoneSet());
    exercise.setFirstPron(bagOfPhones.getFirstPron());

    for (String phone : bagOfPhones.getFirstPron()) {
      Integer integer = getPhoneToCount().get(phone);
      getPhoneToCount().put(phone, integer == null ? 1 : integer + 1);
    }
  }

  /**
   * @param foreignLanguagePhrase
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#isValidForeignPhrase
   */
  public boolean checkLTSOnForeignPhrase(String foreignLanguagePhrase, String transliteration) {
    makeASRScoring();
    return asrScoring.validLTS(foreignLanguagePhrase, transliteration);
  }

  public SmallVocabDecoder getSmallVocabDecoder() {
    makeASRScoring();
    return asrScoring.getSmallVocabDecoder();
  }

  /**
   * Record an answer entry in the database.<br></br>
   * Write the posted data to a wav and an mp3 file (since all the browser audio works with mp3).
   * <p>
   * Client references:
   *
   * @param base64EncodedString generated by flash on the client
   * @param exercise1           exerciseID within the plan
   * @return URL to audio on server and if audio is valid (not too short, etc.)
   * @paramx recordInResults     true if we should add info to the results table
   * @paramx doFlashcard         true if decoding
   * @paramx allowAlternates
   * @paramx isRefRecording
   * @paramx user                answering the question
   * @paramx exerciseID
   * @paramx questionID          question within the exerciseID
   * @paramx audioType           regular or fast then slow audio recording
   * @paramx recordedWithFlash   true if recorded with Flash, false if via webRTC
   * @paramx deviceType          browser or iPad or iPhone
   * @paramx device              browser make and version or iPad unique id
   * @see RecordButton.RecordingListener#stopRecording(long)
   * @see RecordButton.RecordingListener#stopRecording(long)
   * @see LangTestDatabaseImpl#writeAudioFile
   */
  public AudioAnswer writeAudioFile(String base64EncodedString,
                                    CommonShell exercise1,
                                    AudioContext audioContext,
                                    AnswerInfo.RecordingInfo recordingInfoInitial,

//                                    boolean recordInResults,
                                    //                                  boolean doFlashcard,
                                    //                                boolean allowAlternates,
                                    //                              boolean isRefRecording

                                    DecoderOptions options

  ) {
    String wavPath = pathHelper.getLocalPathToAnswer(audioContext);
    File file = pathHelper.getAbsoluteFile(wavPath);

    //long then = System.currentTimeMillis();
    AudioCheck.ValidityAndDur validity =
        audioConversion.convertBase64ToAudioFiles(base64EncodedString, file, options.isRefRecording(), serverProps.isQuietAudioOK());

    // logger.debug("writeAudioFile writing to " + file.getAbsolutePath() + " validity " + validity);
/*    long now = System.currentTimeMillis();
    long diff = now - then;
    if (diff > MIN_WARN_DUR) {
      logger.debug("writeAudioFile: took " + diff + " millis to write wav file " + validity.durationInMillis +
          " millis long");
    }*/

    return getAudioAnswerDecoding(exercise1,
        audioContext,
        new AnswerInfo.RecordingInfo(recordingInfoInitial, file.getPath()),

        wavPath, file,

        validity,

//        recordInResults, doFlashcard, allowAlternates,
        //      false
        options
    );
  }

  public AudioAnswer writeAMASAudioFile(String base64EncodedString,
                                        AmasExerciseImpl exercise1,
                                        AudioContext audioContext,
                                        AnswerInfo.RecordingInfo recordingInfoInitial) {
    String wavPath = pathHelper.getLocalPathToAnswer(audioContext);
    File file = pathHelper.getAbsoluteFile(wavPath);

    //long then = System.currentTimeMillis();
    AudioCheck.ValidityAndDur validity =
        audioConversion.convertBase64ToAudioFiles(base64EncodedString, file, false, serverProps.isQuietAudioOK());

    // logger.debug("writeAudioFile writing to " + file.getAbsolutePath() + " validity " + validity);
/*    long now = System.currentTimeMillis();
    long diff = now - then;
    if (diff > MIN_WARN_DUR) {
      logger.debug("writeAudioFile: took " + diff + " millis to write wav file " + validity.durationInMillis +
          " millis long");
    }*/

    return getAMASAudioAnswerDecoding(exercise1,
        audioContext,
        new AnswerInfo.RecordingInfo(recordingInfoInitial, file.getPath()),

        wavPath, file,

        validity);
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
   * @return
   * @paramx doFlashcard     determines whether we do decoding or alignment
   * @paramx allowAlternates
   * @see mitll.langtest.server.ScoreServlet#getAnswer
   */
  public AudioAnswer getAnswer(
      CommonShell exercise,
      AudioContext audioContext,

      String wavPath, File file,
      String deviceType, String device,

      float score,
//      boolean doFlashcard,
//      boolean allowAlternates,
      DecoderOptions options) {
    AudioCheck.ValidityAndDur validity = audioConversion.isValid(file, false, serverProps.isQuietAudioOK());
    AnswerInfo.RecordingInfo recordingInfo = new AnswerInfo.RecordingInfo("", file.getPath(), deviceType, device, true);

    return options.isDoFlashcard() ?
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
            score, options)
        ;
  }

  private AudioAnswer getAMASAudioAnswerDecoding(AmasExerciseImpl exercise1,

                                                 AudioContext context,
                                                 AnswerInfo.RecordingInfo recordingInfo,

                                                 String wavPath, File file,

                                                 AudioCheck.ValidityAndDur validity) {
    logValidity(context, file, validity);
    AudioAnswer answer = getAMASAudioAnswer(exercise1,
        context.getQuestionID(),
        context.getReqid(),
        wavPath, file, validity);

//    if (recordInResults) {
    recordInResults(context, recordingInfo, validity, answer);
    //  }
    return answer;
  }

  /**
   * @param exercise
   * @param wavPath
   * @param file
   * @param validity
   * @return
   * @paramx recordInResults
   * @paramx doFlashcard
   * @paramx allowAlternates
   * @paramx useOldSchool
   * @see #getAnswer
   * @see #writeAudioFile
   */
  private AudioAnswer getAudioAnswerDecoding(CommonShell exercise,

                                             AudioContext context,
                                             AnswerInfo.RecordingInfo recordingInfo,

                                             String wavPath, File file,

                                             AudioCheck.ValidityAndDur validity,

//                                             boolean recordInResults,
//                                             boolean doFlashcard,
//                                             boolean allowAlternates,
//                                             boolean useOldSchool
                                             DecoderOptions decoderOptions
  ) {
    logValidity(context, file, validity);
    AudioAnswer answer = getAudioAnswer(context.getReqid(), exercise, wavPath, file, validity,
        decoderOptions
        //    doFlashcard,
        //  true, allowAlternates, useOldSchool
    );

    if (decoderOptions.isRecordInResults()) {
      double maxMinRange = validity.getMaxMinRange();
      answer.setDynamicRange(maxMinRange);
      //  logger.info("min max " + maxMinRange + " answer " + answer);
      recordInResults(context, recordingInfo, validity, answer);
    }
    //   logger.debug("getAudioAnswerDecoding answer " + answer);
    return answer;
  }

  private void recordInResults(AudioContext context,
                               AnswerInfo.RecordingInfo recordingInfo,
                               AudioCheck.ValidityAndDur validity,
                               AudioAnswer answer) {
    int processDur = answer.getPretestScore() == null ? 0 : answer.getPretestScore().getProcessDur();

    AnswerInfo infoOrig = new AnswerInfo(
        context,
        recordingInfo,
        validity);

    AnswerInfo info = new AnswerInfo(
        infoOrig,

        new AnswerInfo.ScoreInfo(answer.isCorrect(), (float) answer.getScore(),
            new ScoreToJSON().getJsonFromAnswer(answer).toString(), processDur));

    long answerID = db.getAnswerDAO().addAnswer(db, info);
    answer.setResultID(answerID);
    db.recordWordAndPhoneInfo(answer, answerID);
  }

  /**
   * Does alignment and decoding of one audio file.
   *
   * @param exercise
   * @param attribute
   * @param doHydec
   * @see mitll.langtest.server.decoder.RefResultDecoder#doDecode
   */
  public void decodeOneAttribute(CommonExercise exercise, AudioAttribute attribute, boolean doHydec) {
    if (isInDictOrLTS(exercise)) {
      String audioRef = attribute.getAudioRef();
      if (!audioRef.contains("context=")) {
        //logger.debug("doing alignment -- ");
        // Do alignment...
        File absoluteFile = pathHelper.getAbsoluteFile(audioRef);
        String absolutePath = absoluteFile.getAbsolutePath();

        DecoderOptions options = new DecoderOptions().setUsePhoneToDisplay(serverProps.usePhoneToDisplay()).setDoFlashcard(false);

        PretestScore alignmentScore = getAlignmentScore(exercise, absolutePath,
            //serverProps.usePhoneToDisplay(), false,
            options);
        DecodeAlignOutput alignOutput = new DecodeAlignOutput(alignmentScore, false);

        // Do decoding, and record alignment info we just got in the database ...
        long durationInMillis = attribute.getDurationInMillis();
        AudioAnswer decodeAnswer = getDecodeAnswer(exercise, audioRef, absoluteFile, durationInMillis, false);
        DecodeAlignOutput decodeOutput = new DecodeAlignOutput(decodeAnswer, true);

        options.setUseOldSchool(doHydec);
        PretestScore alignmentScoreOld = doHydec ? getAlignmentScore(exercise, absolutePath,
            // serverProps.usePhoneToDisplay(), true,
            options) : new PretestScore();
        DecodeAlignOutput alignOutputOld = new DecodeAlignOutput(alignmentScoreOld, false);

        // Do decoding, and record alignment info we just got in the database ...
        AudioAnswer decodeAnswerOld = doHydec ? getDecodeAnswer(exercise, audioRef, absoluteFile, durationInMillis, true) : new AudioAnswer();
        DecodeAlignOutput decodeOutputOld = new DecodeAlignOutput(decodeAnswerOld, true);

        //logger.debug("attr dur " + attribute.getDurationInMillis());

        getRefAudioAnswerDecoding(exercise, (int) attribute.getUserid(),
            absoluteFile,
            durationInMillis,

            alignOutput,
            decodeOutput,

            alignOutputOld,
            decodeOutputOld,

            attribute.isMale(),
            attribute.isRegularSpeed() ? REG : SLOW);
      }
    } else {
      logger.warn("skipping " + exercise.getID() + " since can't do decode/align b/c of LTS errors ");
    }
  }

  /**
   * Re-write info in result table, mark it with model and model update date
   * rewrite word and phone info for result.
   *
   * @param result
   * @param exercise
   * @return
   * @see RefResultDecoder#recalcStudentAudio
   */
  public boolean recalcOne(Result result, CommonExercise exercise) {
    String audioRef = result.getAnswer();
    File absoluteFile = pathHelper.getAbsoluteFile(audioRef);

    int uniqueID = result.getUniqueID();

    if (result.getAudioType().equals("flashcard") || result.getAudioType().equals("avp")) {
      long durationInMillis = result.getDurationInMillis();
      AudioAnswer decodeAnswer = getDecodeAnswer(exercise, audioRef, absoluteFile, durationInMillis, false);
      db.getPhoneDAO().removePhones(uniqueID);
      db.getWordDAO().removeWords(uniqueID);
      db.rememberScore(uniqueID, decodeAnswer.getPretestScore(), decodeAnswer.isCorrect());
      logger.info("rememberScore for result " + uniqueID + " : decode " /* +decodeAnswer.getPretestScore()*/);
    } else {
      PretestScore alignmentScore = getEasyAlignment(exercise, absoluteFile.getAbsolutePath());
      db.getPhoneDAO().removePhones(uniqueID);
      db.getWordDAO().removeWords(uniqueID);
      db.rememberScore(uniqueID, alignmentScore, alignmentScore.getHydecScore() > 0.25);
      logger.info("rememberScore for result " + uniqueID + " : alignment " + alignmentScore);
    }
    return true;
  }

  /**
   * Really helpful - could have annotation info always at hand, so don't have to wait for it in learn tab...
   * AND we can send it along to the iPad and have it do highlighting of words and phones in time with the audio
   * I.E. Follow the bouncing ball.
   *
   * @param exercise1
   * @param user
   * @param file
   * @param duration
   * @param isMale
   * @param speed
   * @return
   * @paramx wavPath
   * @paramx numAlignPhones
   * @see #decodeOneAttribute(CommonExercise, AudioAttribute, boolean)
   */
  private void getRefAudioAnswerDecoding(CommonExercise exercise1,
                                         int user,

                                         File file, long duration,
                                         DecodeAlignOutput alignOutput,
                                         DecodeAlignOutput decodeOutput,

                                         DecodeAlignOutput alignOutputOld,
                                         DecodeAlignOutput decodeOutputOld,

                                         boolean isMale, String speed) {
    AudioCheck.ValidityAndDur validity = new AudioCheck.ValidityAndDur(duration);
    // logger.debug("validity dur " + validity.durationInMillis);

    db.addRefAnswer(user, exercise1.getID(), file.getPath(),
        validity.durationInMillis,

        decodeOutput.isCorrect(),

        alignOutput,
        decodeOutput,

        alignOutputOld,
        decodeOutputOld,

        isMale, speed);
    // TODO : add word and phone table for refs
    //	recordWordAndPhoneInfo(decodeAnswer, answerID);
    //   logger.debug("getRefAudioAnswerDecoding decodeAnswer " + decodeAnswer);
  }

  private AudioAnswer getDecodeAnswer(CommonExercise exercise1,
                                      String wavPath,
                                      File file,
                                      long duration,
                                      boolean useOldSchool) {
    return getAudioAnswer(1,
        exercise1,
        wavPath, file, new AudioCheck.ValidityAndDur(duration),

        new DecoderOptions()
            .setDoFlashcard(true)
            .setCanUseCache(false)
            .setAllowAlternates(false)
            .setUseOldSchool(useOldSchool)
//        true,  // decode!
//        false, // don't use cache
//        false, // don't allow alternates
//        useOldSchool // if should use hydec instead of hydra NPWS service
    );
  }

  private void logValidity(AudioContext context, File file, AudioCheck.ValidityAndDur validity) {
    logValidity(context.getId(), context.getQuestionID(), context.getUserid(), file, validity);
  }

  private void logValidity(String exerciseID, int questionID, int user, File file, AudioCheck.ValidityAndDur validity) {
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
   * @return
   * @paramx recordInResults
   * @paramx doFlashcard
   * @paramx useOldSchool
   * @see #getAnswer
   */
  private AudioAnswer getAudioAnswerAlignment(CommonShell exercise,

                                              AudioContext context,
                                              AnswerInfo.RecordingInfo recordingInfo,

                                              String wavPath, File file,
                                              AudioCheck.ValidityAndDur validity,

                                              float score,

                                              //          boolean recordInResults,
                                              //       boolean doFlashcard,
                                              //        boolean useOldSchool
                                              DecoderOptions options
  ) {
    logValidity(context, file, validity);
    AudioAnswer answer = getAudioAnswer(context.getReqid(), exercise, wavPath, file, validity,
        options.setCanUseCache(true).setAllowAlternates(false)
        //    doFlashcard,
        //  true, false, useOldSchool
    );

    if (options.isRecordInResults()) {
      int processDur = answer.getPretestScore() == null ? 0 : answer.getPretestScore().getProcessDur();

      AnswerInfo infoOrig = new AnswerInfo(
          context,
          recordingInfo,
          validity);

      AnswerInfo info = new AnswerInfo(infoOrig,
          new AnswerInfo.ScoreInfo(true, score, new ScoreToJSON().getJsonFromAnswer(answer).toString(), processDur));

      long answerID = db.getAnswerDAO().addAnswer(db, info);
      answer.setResultID(answerID);
    }
    logger.debug("getAudioAnswerAlignment answer " + answer);
    return answer;
  }


  /**
   * @param exercise1
   * @param reqid
   * @param wavPath
   * @param file
   * @param validity
   * @return
   * @paramxx isValid
   * @see #getAudioAnswerDecoding
   */
  private AudioAnswer getAMASAudioAnswer(AmasExerciseImpl exercise1,

                                         int questionID,
                                         int reqid,

                                         String wavPath, File file,
                                         AudioCheck.ValidityAndDur validity) {
    String url = pathHelper.ensureForwardSlashes(wavPath);

    return (validity.isValid() && !serverProps.isNoModel()) ?
        getAMASAudioAnswer(
            exercise1,
            questionID,
            reqid, file, validity, url) :
        new AudioAnswer(url, validity.getValidity(), reqid, validity.durationInMillis);
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
   */
  private AudioAnswer getAMASAudioAnswer(AmasExerciseImpl exercise,
                                         int qid, int reqid,
                                         File file, AudioCheck.ValidityAndDur validity, String url) {
    makeASRScoring();
    AudioAnswer audioAnswer = new AudioAnswer(url, validity.getValidity(), reqid, validity.durationInMillis);
    autoCRT.getAutoCRTDecodeOutput(exercise, qid, file, audioAnswer, true);
    return audioAnswer;
  }


  /**
   * Checks validity before alignment/decoding.
   *
   * @param reqid
   * @param commonShell
   * @param wavPath
   * @param file
   * @param validity    if audio isn't valid, don't do alignment or decoding
   * @return
   * @paramx doFlashcard
   * @paramx canUseCache
   * @paramx allowAlternates
   * @paramx useOldSchool
   * @see #getAudioAnswerDecoding
   */
  private AudioAnswer getAudioAnswer(int reqid,
                                     CommonShell commonShell,
                                     String wavPath, File file, AudioCheck.ValidityAndDur validity,

//                                     boolean doFlashcard,
//                                     boolean canUseCache,
//                                     boolean allowAlternates,
//                                     boolean useOldSchool

                                     DecoderOptions decoderOptions
  ) {
    String url = pathHelper.ensureForwardSlashes(wavPath);

    return (validity.isValid() && !serverProps.isNoModel()) ?
        getAudioAnswer(
            commonShell,
            reqid, file, validity, url, decoderOptions) ://doFlashcard, canUseCache, allowAlternates, useOldSchool) :
        new AudioAnswer(url, validity.getValidity(), reqid, validity.durationInMillis);
  }

  /**
   * @param base64EncodedString
   * @param textToAlign
   * @param identifier
   * @param reqid
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getAlignment
   */
  public AudioAnswer getAlignment(String base64EncodedString,
                                  String textToAlign,
                                  String transliteration,
                                  String identifier, int reqid, boolean usePhoneToDisplay) {
    File file = getPostedFileLoc();
    AudioAnswer audioAnswer = getAudioAnswer(base64EncodedString, reqid, file);

    if (audioAnswer.isValid()) {
      PretestScore asrScoreForAudio = getASRScoreForAudio(
          reqid,
          file.getAbsolutePath(),
          textToAlign,
          null,
          transliteration,
          DEFAULT,
          identifier,
          null,

          new DecoderOptions()
              .setDoFlashcard(false)
              .setCanUseCache(serverProps.useScoreCache())
              .setUsePhoneToDisplay(usePhoneToDisplay));

      audioAnswer.setPretestScore(asrScoreForAudio);
    } else {
      logger.warn("got invalid audio file (" + audioAnswer.getValidity() + ") identifier " + identifier + " file " + file.getAbsolutePath());
    }

    return audioAnswer;
  }

  private File getPostedFileLoc() {
    return getPathUnder(POSTED_AUDIO);
  }

  private File getPathUnder(String postedAudio) {
    String wavPath = pathHelper.getWavPathUnder(postedAudio);
    return pathHelper.getAbsoluteFile(wavPath);
  }

  private AudioAnswer getAudioAnswer(String base64EncodedString, int reqid, File file) {
    AudioCheck.ValidityAndDur validity =
        audioConversion.convertBase64ToAudioFiles(base64EncodedString, file, false, serverProps.isQuietAudioOK());
    //  logger.debug("getAMASAudioAnswer writing to " + file.getAbsolutePath() + " validity " + validity);
    return new AudioAnswer(pathHelper.ensureForwardSlashes(pathHelper.getWavPathUnder(POSTED_AUDIO)),
        validity.getValidity(), reqid, validity.durationInMillis);
  }

  /**
   * @param testAudioFile
   * @param lmSentences
   * @return
   * @paramx canUseCache
   * @paramx useOldSchool
   * @see AutoCRT#getScoreForAudio
   * @see DecodeCorrectnessChecker#getDecodeScore(File, Collection, AudioAnswer, DecoderOptions)
   */
/*  @Override
  public PretestScore getASRScoreForAudio(File testAudioFile, Collection<String> lmSentences, String transliteration,
                                          DecoderOptions options
  ) {
    options.setUsePhoneToDisplay(serverProps.usePhoneToDisplay());
    return getASRScoreForAudio(testAudioFile, lmSentences, transliteration,
        //    canUseCache, serverProps.usePhoneToDisplay(), useOldSchool
        options
    );
  }*/

  public PretestScore getEasyAlignment(CommonShell exercise, String testAudioPath) {
    DecoderOptions options = new DecoderOptions().setUsePhoneToDisplay(serverProps.usePhoneToDisplay());
    return getAlignmentScore(exercise, testAudioPath, options);
  }

  /**
   * @return
   * @see #decodeOneAttribute(CommonExercise, AudioAttribute, boolean)
   */
  private PretestScore getAlignmentScore(CommonShell exercise, String testAudioPath, DecoderOptions options) {
    return getASRScoreForAudio(0, testAudioPath, exercise.getForeignLanguage(), exercise.getTransliteration(),
        DEFAULT, exercise.getID(), null,
        options);
  }

  /**
   * Get score when doing autoCRT on an audio file.
   * <p>
   * TODO : why even generate images here???
   *
   * @param testAudioFile     audio file to score
   * @param lmSentences       to look for in the audio
   * @param transliteration   for languages we can't do normal LTS on (Kanji characters or similar)
   * @param options
   * @return PretestScore for audio
   * @see DecodeCorrectnessChecker#getDecodeScore
   * @see AlignDecode#getASRScoreForAudio
   */
  @Override
  public PretestScore getASRScoreForAudio(File testAudioFile,
                                           Collection<String> lmSentences,
                                           String transliteration,
                                           DecoderOptions options) {
    makeASRScoring();
    List<String> unk = new ArrayList<String>();

    if (isMacOrWin() || useOldSchoolServiceOnly || options.isUseOldSchool()) {  // i.e. NOT using cool new jcodr webservice
      unk.add(SLFFile.UNKNOWN_MODEL); // if  you don't include this dcodr will say : ERROR: word UNKNOWNMODEL is not in the dictionary!
    }

    String vocab = asrScoring.getUsedTokens(lmSentences, unk); // this is basically the transcript
    //  logger.info("from '" + lmSentences + "' to '" + vocab +"'");
    String prefix = options.isUsePhoneToDisplay() ? "phoneToDisplay" : "";
    String path = testAudioFile.getPath();

    //  logger.info("getASRScoreForAudio audio file path is " + path);
    return getASRScoreForAudio(0, path, vocab, lmSentences, transliteration,
        DEFAULT, prefix, null,
        options);
  }

  /**
   * For now, we don't use a ref audio file, since we aren't comparing against a ref audio file with the DTW/sv pathway.
   *
   * @param reqid
   * @param testAudioFile
   * @param sentence           empty string when using lmSentences non empty and vice-versa
   * @param transliteration    for languages we can't do normal LTS on (Kanji characters or similar)
   * @param prefix
   * @param precalcResult
   * @param options
   * @return PretestScore
   * @see LangTestDatabaseImpl#getPretestScore
   * @see LangTestDatabaseImpl#getResultASRInfo
   * @see AlignDecode#getASRScoreForAudio
   **/
  public PretestScore getASRScoreForAudio(int reqid,
                                          String testAudioFile,
                                          String sentence,
                                          String transliteration,

                                          ImageOptions imageOptions,

                                          String prefix,
                                          Result precalcResult,

                                          DecoderOptions options) {
    return getASRScoreForAudio(reqid,
        testAudioFile,
        sentence,
        null,
        transliteration,
        imageOptions,
        prefix,
        precalcResult,
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
   * @param transliteration    for languages we can't do normal LTS on (Kanji characters or similar)
   * @param prefix
   * @param precalcResult
   * @param options
   * @return
   * @see #getASRScoreForAudio(File, Collection, boolean, boolean, boolean)
   */
  private PretestScore getASRScoreForAudio(int reqid,
                                           String testAudioFile,
                                           String sentence,
                                           Collection<String> lmSentences,
                                           String transliteration,

                                           ImageOptions imageOptions,

                                           String prefix,
                                           Result precalcResult,

                                           DecoderOptions options) {
    logger.debug("getASRScoreForAudio (" + serverProps.getLanguage() + ")" + (options.isDoFlashcard() ? " Decoding " : " Aligning ") +
        "" + testAudioFile + " with sentence '" + sentence + "' req# " + reqid +
        (options.isCanUseCache() ? " check cache" : " NO CACHE") + " prefix " + prefix);

    makeASRScoring();
    if (testAudioFile == null) {
      logger.error("getASRScoreForAudio huh? no test audio file for " + sentence);
      return new PretestScore(); // very defensive
    }
    testAudioFile = mp3Support.dealWithMP3Audio(testAudioFile);
    if (!new File(testAudioFile).exists()) {
      String absolutePath = pathHelper.getAbsolute(pathHelper.getInstallPath(), testAudioFile).getAbsolutePath();
      if (!new File(absolutePath).exists()) {
        logger.error("getASRScoreForAudio huh? no testAudioFile for " + sentence + " at " + new File(testAudioFile).getAbsolutePath() + " or " + absolutePath);
        return new PretestScore();
      }
    }

    String installPath = pathHelper.getInstallPath();

    DirAndName testDirAndName = new DirAndName(testAudioFile, installPath).invoke();
    String testAudioName = testDirAndName.getName();
    String testAudioDir = testDirAndName.getDir();

    if (isEnglishSite()) {
      sentence = sentence.toUpperCase();  // hack for English
    }
    sentence = sentence.replaceAll(",", " ");
    sentence = getSentenceToUse(sentence);
    sentence = sentence.trim();

    ASR asrScoring = options.isUseOldSchool() || serverProps.getOldSchoolService() ? oldschoolScoring : getASRScoring();

//    logger.debug("getASRScoreForAudio : for " + testAudioName + " sentence '" + sentence + "' lm sentences '" + lmSentences + "'");

    PretestScore pretestScore = asrScoring.scoreRepeat(
        testAudioDir, removeSuffix(testAudioName),
        sentence, lmSentences, transliteration,

        pathHelper.getImageOutDir(), imageOptions,
        options.isDoFlashcard(), options.isCanUseCache(), prefix, precalcResult,
        options.isUsePhoneToDisplay());

    if (!pretestScore.isRanNormally() && isWebservice(asrScoring)) {
      logger.warn("getASRScoreForAudio Using hydec as fallback for " + (options.isDoFlashcard() ? " decoding " : " aligning ") + testAudioFile + " against '" +
          sentence +
          "'");
      pretestScore = oldschoolScoring.scoreRepeat(
          testAudioDir,
          removeSuffix(testAudioName),
          sentence,
          lmSentences,
          transliteration,

          pathHelper.getImageOutDir(),
          imageOptions,
          options.isDoFlashcard(),
          options.isCanUseCache(),
          prefix,
          precalcResult,
          options.isUsePhoneToDisplay());
    }
    pretestScore.setReqid(reqid);

    JSONObject json = new ScoreToJSON().getJsonObject(pretestScore);
    // logger.info("json for preset score " +pretestScore + " " + json);
    pretestScore.setJson(json.toString());

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
    boolean english = serverProps.getLanguage().equalsIgnoreCase("English") && sentence.equals("%") || sentence.equals("％");
    if (english) {
      //logger.info("convert " +sentence + " to percent");
    } else {
      //boolean english1 = getLanguage().equalsIgnoreCase("English");
      // boolean equals = sentence.equals("%") || sentence.equals("％");
      //logger.info("NOT convert '" +sentence + "' to percent : " +english1 + " equals " + equals);
    }
    return english ? "percent" : sentence;
  }

  private boolean isEnglishSite() {
    return serverProps.getLanguage().equalsIgnoreCase("English");
  }

  private String removeSuffix(String audioFile) {
    return audioFile.substring(0, audioFile.length() - SUFFIX_LENGTH);
  }

  public String getWavForMP3(String audioFile) {
    return mp3Support.getWavForMP3(audioFile);
  }

  /**
   * Does decoding if doFlashcard is true.
   *
   * @param reqid
   * @param file
   * @param validity
   * @param url
   * @return AudioAnswer with decode info attached, if doFlashcard is true
   * @see #getAudioAnswer(int, CommonShell, String, File, AudioCheck.ValidityAndDur, DecoderOptions)
   */
  private AudioAnswer getAudioAnswer(CommonShell exercise,
                                     int reqid,
                                     File file,
                                     AudioCheck.ValidityAndDur validity,
                                     String url,
                                     DecoderOptions decoderOptions
  ) {
    AudioAnswer audioAnswer = new AudioAnswer(url, validity.getValidity(), reqid, validity.durationInMillis);
    if (decoderOptions.isDoFlashcard()) {
      makeASRScoring();
      PretestScore flashcardAnswer = decodeCorrectnessChecker.getDecodeScore(exercise, file, audioAnswer,
          serverProps.getLanguage(),
          decoderOptions
      );
      audioAnswer.setPretestScore(flashcardAnswer);
      return audioAnswer;
    }
    return audioAnswer;
  }


  public Map<String, Integer> getPhoneToCount() {
    return phoneToCount;
  }

  /**
   * @return
   * @see #getASRScoreForAudio(int, String, String, Collection, int, int, boolean, boolean, boolean, String, Result, boolean, boolean)
   */
  private ASR getASRScoring() {
    return webserviceScoring;
  }

  private boolean isWebservice(ASR asr) {
    return asr == webserviceScoring;
  }

  private boolean isMacOrWin() {
    String property = System.getProperty("os.name").toLowerCase();
    return property.contains("mac") || property.contains("win");
  }

  // TODO: gross
  private void makeASRScoring() {
    if (webserviceScoring == null) {
      String installPath = pathHelper.getInstallPath();
      HTKDictionary htkDictionary = makeDict(installPath);
      webserviceScoring = new ASRWebserviceScoring(installPath, serverProps, logAndNotify, htkDictionary);
      oldschoolScoring = new ASRScoring(installPath, serverProps, logAndNotify, htkDictionary);
    }
    asrScoring = oldschoolScoring;
  }

  /**
   * @return
   * @see #makeASRScoring
   */
  private HTKDictionary makeDict(String installPath) {
    String dictFile = new ConfigFileCreator(serverProps.getProperties(), null, Scoring.getScoringDir(installPath)).getDictFile();
    if (dictFile != null && new File(dictFile).exists()) {
      long then = System.currentTimeMillis();
      HTKDictionary htkDictionary = new HTKDictionary(dictFile);
      long now = System.currentTimeMillis();
      int size = htkDictionary.size(); // force read from lazy val
      if (now - then > 300) {
        logger.info("for " + serverProps.getLanguage() + " read dict " + dictFile + " of size " + size + " took " + (now - then) + " millis");
      }
      return htkDictionary;
    } else {
      if (serverProps.isNoModel()) {
        logger.info("---> makeDict : Can't find dict file at " + dictFile);
      } else {
        logger.error("\n\n\n---> makeDict : Can't find dict file at " + dictFile);
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
        serverProps.useMiraClassifier());
//    autoCRT.makeClassifier();
    return autoCRT;
  }

  /**
   * @see #AudioFileHelper(PathHelper, ServerProperties, DatabaseImpl, LogAndNotify)
   */
  private void makeDecodeCorrectnessChecker() {
    decodeCorrectnessChecker = new DecodeCorrectnessChecker(this, serverProps.getMinPronScore());
  }

  /**
   * @see #getASRScoreForAudio
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
}