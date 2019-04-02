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

package mitll.langtest.server.scoring;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mitll.langtest.server.ScoreServlet;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.audio.ScoreToJSON;
import mitll.langtest.server.audio.TrackInfo;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.project.Project;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.scoring.*;
import mitll.langtest.shared.user.MiniUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static mitll.langtest.server.audio.AudioConversion.LANGTEST_IMAGES_NEW_PRO_F_1_PNG;
import static mitll.langtest.shared.answer.Validity.OK;

/**
 * Created by go22670 on 3/7/17.
 */
public class JsonScoring {
  private static final Logger logger = LogManager.getLogger(JsonScoring.class);

  private static final String SCORE = "score";

  public static final String CONTENT = "content";

  private static final String IS_CORRECT = "isCorrect";
  private static final String SAID_WORD = "saidWord";
  private static final String FULLMATCH = "fullmatch";

  private static final String EXID = "exid";
  private static final String VALID = "valid";
  private static final String REQID = "reqid";

  private static final String RESULT_ID = "resultID";

  private static final ImageOptions DEFAULT = ImageOptions.getDefault();
  public static final boolean TRY_TO_DO_ALIGNMENT = false;

  private static final float MIN_HYDRA_ALIGN = 0.3F;
  private static final String BAD_EXERCISE_ID = "bad_exercise_id";
  private static final String DYNAMIC_RANGE = "dynamicRange";
  private static final String PRETEST = "pretest";
  public static final boolean DEBUG = false;
  public static final String ISFULLMATCH = "isfullmatch";
  public static final String RESULT_ID1 = "resultID";
  public static final String DURATION = "duration";
  public static final String TIMESTAMP = "timestamp";
  public static final String PATH = "path";
  private final DatabaseImpl db;
  private final ServerProperties serverProps;
  private final int unknownExID;

  /**
   * @param db
   */
  public JsonScoring(DatabaseImpl db) {
    this.db = db;
    this.serverProps = db.getServerProps();
    unknownExID = db.getUserExerciseDAO().getUnknownExerciseID();
  }


  /**
   * @param reqid              label response with req id so the client can tell if it got a stale response
   * @param projid
   * @param exerciseID         for this exercise
   * @param postedWordOrPhrase
   * @param user               by this user
   * @param request            mostly decode, could be record if doing appen corpora recording
   * @param wavPath            relative path to posted audio file
   * @param saveFile           File handle to file
   * @param deviceType         iPad,iPhone, or browser
   * @param device             id for device - helpful for iPads, etc.
   * @param options
   * @param fullJSON
   * @return score json
   * @paramx allowAlternates   decode against multiple alternatives (e.g. male and female spanish words for the same english word)
   * @paramx usePhoneToDisplay should we remap the phones to different labels for display
   * @see ScoreServlet#getJsonForAudio
   */
  public JsonObject getJsonForAudioForUser(int reqid,
                                           int projid,
                                           int exerciseID,
                                           String postedWordOrPhrase,

                                           int user,
                                           ScoreServlet.PostRequest request,
                                           String wavPath,
                                           File saveFile,
                                           String deviceType,
                                           String device,
                                           DecoderOptions options,
                                           boolean fullJSON) {
    long start = System.currentTimeMillis();
    long then = System.currentTimeMillis();

    CommonExercise exercise = getCommonExercise(projid, exerciseID, user);

    JsonObject jsonForScore = new JsonObject();

    // so allow an exercise id = 0 with some actual text
    String foreignLanguage = postedWordOrPhrase;
    String transliteration = "";

    if (exercise == null && exerciseID != unknownExID) {
      logger.warn("getJsonForAudioForUser : can't find exercise " + exerciseID + " in " + projid + " giving up.");
      jsonForScore.add(VALID, new JsonPrimitive(BAD_EXERCISE_ID));
      return jsonForScore;
    } else if (exercise != null) {
      foreignLanguage = exercise.getForeignLanguage();
      transliteration = exercise.getTransliteration();
    }

    boolean doFlashcard = request == ScoreServlet.PostRequest.DECODE;
    options.setDoDecode(doFlashcard);

    long now = System.currentTimeMillis();

    if (now - then > 10) {
      logger.info("getJsonForAudioForUser :  prep took " + (now - then) + " millis");
    }

    then = System.currentTimeMillis();
    AudioAnswer answer = getAudioAnswer(reqid, exerciseID, user, wavPath, saveFile, deviceType, device,
        foreignLanguage,
        transliteration,
        projid,
        options);
    now = System.currentTimeMillis();

    if (logger.isInfoEnabled()) {
      PretestScore pretestScore = answer == null ? null : answer.getPretestScore();
      float hydecScore = pretestScore == null ? -1 : pretestScore.getOverallScore();
      logger.info("getJsonForAudioForUser" +
              "\n\tflashcard   " + doFlashcard +
              "\n\texercise id " + exerciseID +
              "\n\ttook        " + (now - start) + " millis " +
              "\n\tanswer      " + (now - then) + " millis " +
              "\n\tfor         " + saveFile.getName() +
              "\n\tscore       " + hydecScore
          //+
          //"\n\tpretestScore " + pretestScore
      );
    }

    then = System.currentTimeMillis();
    JsonObject jsonObject = getJsonObject(projid, exerciseID, options, fullJSON, jsonForScore, doFlashcard, answer, false);
    now = System.currentTimeMillis();
    if (now - then > 10) {
      logger.info("getJsonForAudioForUser : getting json took " + (now - then) + " millis");
    }
    return jsonObject;
  }

  @Nullable
  private CommonExercise getCommonExercise(int projid, int exerciseID, int user) {
    if (exerciseID == unknownExID) {
      return null;
    } else {
      int mostRecentProjectByUser = projid == -1 ? getMostRecentProjectByUser(user) : projid;
      return db.getCustomOrPredefExercise(mostRecentProjectByUser, exerciseID);
    }
  }

  /**
   * @param projid
   * @param exerciseID
   * @param options
   * @param fullJSON
   * @param jsonForScore
   * @param doFlashcard
   * @param answer
   * @param addStream
   * @return
   * @see #getJsonForAudioForUser(int, int, int, String, int, ScoreServlet.PostRequest, String, File, String, String, DecoderOptions, boolean)
   */
  public JsonObject getJsonObject(int projid,
                                  int exerciseID,
                                  DecoderOptions options,
                                  boolean fullJSON,
                                  JsonObject jsonForScore,
                                  boolean doFlashcard,
                                  AudioAnswer answer,
                                  boolean addStream) {
    PretestScore pretestScore = answer == null ? null : answer.getPretestScore();
    if (answer != null && answer.isValid()) {
      if (pretestScore != null) {
        jsonForScore = getJsonObject(projid, options.isUsePhoneToDisplay(), fullJSON, doFlashcard, answer, pretestScore);
        jsonForScore.addProperty(ISFULLMATCH, answer.getPretestScore().isFullMatch());
      }
      if (addStream) {
        addStreamInfo(jsonForScore, answer);
      } else {
        if (DEBUG) logger.info("getJsonObject : not adding stream info for req " + projid + " : " + exerciseID);
      }

      jsonForScore.addProperty(RESULT_ID1, answer.getResultID());
    } else if (answer != null) {
      logger.warn("getJsonObject - validity is " + answer.getValidity() +
          "\n\tduration " + answer.getDurationInMillis() +
          "\n\tfor      " + answer);
    }

    if (answer == null) {
      logger.warn("getJsonObject no answer for " + projid + " : " + exerciseID);
    }

    Validity validity = answer == null ? Validity.INVALID : answer.getValidity();

    if (validity != OK) {
      logger.warn("getJsonObject invalid " + validity + " : " + answer);
    } else if (!addStream) {
      addDurationAndDNR(jsonForScore, answer);
    }

    addValidity(exerciseID, jsonForScore,
        validity,
        answer == null ? "1" : "" + answer.getReqid());

    return jsonForScore;
  }

  private void addStreamInfo(JsonObject jsonForScore, AudioAnswer answer) {
    addDurationAndDNR(jsonForScore, answer);
    String path = answer.getPath();
    if (path.isEmpty()) logger.warn("no path?");
    jsonForScore.addProperty(PATH, path);

    if (jsonForScore.get(PRETEST) == null) {
      jsonForScore.add(PRETEST, new JsonObject());
    }

    long timestamp = answer.getTimestamp();
    //    logger.info("getJsonObject timestamp " + timestamp + " " + new Date(timestamp));
    jsonForScore.addProperty(TIMESTAMP, timestamp);
  }

  private void addDurationAndDNR(JsonObject jsonForScore, AudioAnswer answer) {
    jsonForScore.addProperty(DURATION, answer.getDurationInMillis());
    jsonForScore.addProperty(DYNAMIC_RANGE, answer.getDynamicRange());
  }

  /**
   * @param projid
   * @param usePhoneToDisplay
   * @param fullJSON
   * @param doFlashcard
   * @param answer
   * @param pretestScore
   * @return
   * @see #getJsonForAudioForUser
   */
  @NotNull
  public JsonObject getJsonObject(int projid,
                                  boolean usePhoneToDisplay,
                                  boolean fullJSON,
                                  boolean doFlashcard,
                                  AudioAnswer answer,
                                  PretestScore pretestScore) {
    JsonObject jsonForScore;
    ScoreToJSON scoreToJSON = new ScoreToJSON();
    float rawOverall = pretestScore == null ? -1 : pretestScore.getRawOverallScore();
    float overallScore = pretestScore == null ? -1 : pretestScore.getOverallScore();

    float wordAvg = pretestScore == null ? -1 : pretestScore.getAvgWordScore();

    if (DEBUG && pretestScore != null) {
      List<TranscriptSegment> transcriptSegments = pretestScore.getTypeToSegments().get(NetPronImageType.WORD_TRANSCRIPT);
      logger.info("getJsonObject for " + answer.getExid() + " # word segments " + transcriptSegments.size());
      logger.info("getJsonObject for " + answer.getExid() + " word scores " + pretestScore.getWordScores());
      logger.info("getJsonObject for " + answer.getExid() + " # valid segments " + pretestScore.getNumValidSegments(transcriptSegments));
      logger.info("getJsonObject for " + answer.getExid() + " total for valid segments " + pretestScore.getValidSegmentScoreTotal(transcriptSegments));
      logger.info("getJsonObject for " + answer.getExid() + " wordAvg " + wordAvg);
    }

    float phoneAvg = pretestScore == null ? -1 : pretestScore.getAvgPhoneScore();

    Project project = db.getProject(projid);

    jsonForScore = fullJSON ?
        scoreToJSON.getJsonObject(pretestScore) :
        scoreToJSON.getJsonForScore(pretestScore, usePhoneToDisplay, serverProps, project.getLanguageEnum());
    jsonForScore.addProperty(SCORE, overallScore);
    jsonForScore.addProperty("rawOverall", rawOverall);
    jsonForScore.addProperty("wordAvg", wordAvg);
    jsonForScore.addProperty("phoneAvg", phoneAvg);

    jsonForScore.addProperty(FULLMATCH, pretestScore != null && pretestScore.isFullMatch());

    if (doFlashcard) {
      jsonForScore.addProperty(IS_CORRECT, answer.isCorrect());
      jsonForScore.addProperty(SAID_WORD, answer.isSaidAnswer());
      jsonForScore.addProperty(RESULT_ID, answer.getResultID());
    } else {
      jsonForScore.addProperty(IS_CORRECT, overallScore > MIN_HYDRA_ALIGN);
    }
    return jsonForScore;
  }

  /**
   * @param id
   * @return
   * @see #getAnswer
   */
  private int getMostRecentProjectByUser(int id) {
    return db.getUserProjectDAO().getCurrentProjectForUser(id);
  }

  /**
   * @param reqid           label response with req id so the client can tell if it got a stale response
   * @param exerciseID      for this exercise - redundant
   * @param user            by this user
   * @param wavPath         path to posted audio file
   * @param saveFile
   * @param deviceType
   * @param device
   * @param foreignLanguage
   * @param options
   * @return
   * @see #getJsonForAudioForUser
   */
  private AudioAnswer getAudioAnswer(int reqid,
                                     int exerciseID,
                                     int user,
                                     String wavPath,
                                     File saveFile,
                                     String deviceType,
                                     String device,
                                     String foreignLanguage,
                                     String transliteration,
                                     int projectID,
                                     DecoderOptions options) {
    AudioAnswer answer;

    if (options.shouldDoDecoding()) {
      options.setDoDecode(true);
      answer = getAnswer(reqid, projectID, exerciseID, foreignLanguage, user, wavPath, saveFile, -1, deviceType, device,
          options,
          null);
    } else {
      PretestScore asrScoreForAudio = getASRScoreForAudio(reqid,
          exerciseID,
          wavPath,
          foreignLanguage,
          transliteration,
          options.isUsePhoneToDisplay(),
          projectID);

      options.setDoDecode(false);

      answer = getAnswer(reqid, projectID, exerciseID, foreignLanguage, user, wavPath, saveFile,
          asrScoreForAudio.getOverallScore(),
          deviceType, device,
          options,
          asrScoreForAudio);
    }
    return answer;
  }

  /**
   * TODO : this is wacky -- have to do this for alignment but not for decoding
   *
   * @param reqid
   * @param testAudioFile
   * @param sentence
   * @param exerciseID
   * @param usePhoneToDisplay
   * @param projid
   * @return
   * @see #getAudioAnswer
   */
  private PretestScore getASRScoreForAudio(int reqid,
                                           int exerciseID,
                                           String testAudioFile,
                                           String sentence,
                                           String transliteration,
                                           boolean usePhoneToDisplay,
                                           int projid) {
    AudioFileHelper audioFileHelper = getAudioFileHelper(projid);
    return audioFileHelper
        .getASRScoreForAudio(reqid, testAudioFile, sentence, Collections.singleton(sentence), transliteration, DEFAULT, "" + exerciseID,
            null,
            new DecoderOptions()
                .setDoDecode(false)
                .setCanUseCache(db.getServerProps().useScoreCache())
                .setUsePhoneToDisplay(usePhoneToDisplay),
            audioFileHelper.isKaldi());
  }

  private AudioFileHelper getAudioFileHelper(int projectid) {
    return getProject(projectid).getAudioFileHelper();
  }

  /**
   * Don't wait for mp3 to write to return - can take 70 millis for a short file.
   *
   * @param projectID
   * @param reqid
   * @param exerciseID
   * @param foreignLanguage
   * @param user
   * @param wavPath
   * @param file
   * @param deviceType
   * @param device
   * @param pretestScore
   * @return
   * @see #getJsonForAudioForUser
   */
  private AudioAnswer getAnswer(int reqid,
                                int projectID,
                                int exerciseID,
                                String foreignLanguage,
                                int user,
                                String wavPath,
                                File file,
                                float score,
                                String deviceType,
                                String device,
                                DecoderOptions options,
                                PretestScore pretestScore) {
    ClientExercise exercise = exerciseID == unknownExID ? null : db.getCustomOrPredefExercise(projectID, exerciseID);  // allow custom items to mask out non-custom items

    if (exerciseID == 0 || exerciseID == unknownExID) {
      exerciseID = unknownExID;
      // make one up
      exercise = getNotionalExercise(exerciseID, foreignLanguage);
    }

    Language language = getLanguage(projectID);

    AudioContext audioContext =
        new AudioContext(reqid, user, projectID, language, exerciseID,
            0, options.shouldDoDecoding() ? AudioType.PRACTICE : AudioType.LEARN);

    //   logger.info("getAnswer  for " + exerciseID + " for " + user + " and file " + wavPath);

    AudioAnswer answer = getAudioFileHelper(projectID)
        .getAnswer(exercise,
            audioContext,
            wavPath, file, deviceType, device, score,
            options, pretestScore);

    ensureMP3Later(answer.getPath(), user, foreignLanguage, exercise.getEnglish(), language.getLanguage(), exercise);

    return answer;
  }

  @NotNull
  private ClientExercise getNotionalExercise(int exerciseID, String foreignLanguage) {
    ClientExercise exercise;
    exercise = new Exercise();
    {
      Exercise exercise1 = (Exercise) exercise;
      exercise1.setForeignLanguage(foreignLanguage);
      exercise1.setID(exerciseID);
    }
    return exercise;
  }

  /**
   * @param path
   * @param user
   * @param foreignLanguage
   * @param english
   * @param language
   * @param exercise
   * @see #getAnswer
   */
  private void ensureMP3Later(final String path, final int user, final String foreignLanguage, String english, String language, ClientExercise exercise) {
    new Thread(() -> {
      //long then = System.currentTimeMillis();
      writeCompressedVersions(path, new TrackInfo(foreignLanguage, getUserID(user), english, language, exercise.getUnitToValue()));
      // long now = System.currentTimeMillis();
      //       logger.debug("Took " + (now-then) + " millis to write mp3 version");
    }, "ensureMP3Later").start();
  }

  /**
   * @param wavFile
   * @param trackInfo
   * @see #ensureMP3Later
   */
  private void writeCompressedVersions(String wavFile, TrackInfo trackInfo) {
    File absolutePathToWav = new File(wavFile);
    if (!absolutePathToWav.exists()) {
      logger.error("no file at " + absolutePathToWav);
    }

    new AudioConversion(serverProps.shouldTrimAudio(), serverProps.getMinDynamicRange(), db.getPathHelper().getContext().getRealPath(LANGTEST_IMAGES_NEW_PRO_F_1_PNG))
        .writeCompressedVersions(absolutePathToWav, false, trackInfo, true);
  }

  private String getUserID(int userid) {
    MiniUser userWhere = db.getUserDAO().getMiniUser(userid);
    if (userWhere == null) logger.error("huh? can't find user by " + userid);
    return userWhere == null ? "" + userid : userWhere.getUserID();
  }

  /**
   * @param exerciseID
   * @param jsonForScore
   * @param validity
   * @see #getJsonForAudioForUser
   */
  public void addValidity(int exerciseID, JsonObject jsonForScore, Validity validity, String reqID) {
    jsonForScore.addProperty(EXID, exerciseID);
    jsonForScore.addProperty(VALID, validity.toString());
    jsonForScore.addProperty(REQID, reqID);
  }

  private Language getLanguage(int projectid) {
    return getProject(projectid).getLanguageEnum();
  }

  private Project getProject(int projid) {
    return db.getProject(projid);
  }
}
