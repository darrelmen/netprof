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
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.DecoderOptions;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.PretestScore;
import mitll.langtest.shared.user.MiniUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Created by go22670 on 3/7/17.
 */
public class JsonScoring {
  private static final Logger logger = LogManager.getLogger(JsonScoring.class);

  private static final String SCORE = "score";

  public static final String CONTENT = "content";

  private static final String IS_CORRECT = "isCorrect";
  private static final String SAID_WORD = "saidWord";

  private static final String EXID = "exid";
  private static final String VALID = "valid";
  private static final String REQID = "reqid";
  //private static final String INVALID = "invalid";

  private static final String RESULT_ID = "resultID";

  private static final ImageOptions DEFAULT = ImageOptions.getDefault();
  public static final boolean TRY_TO_DO_ALIGNMENT = false;
  //  public static final String EXERCISE_TEXT = "exerciseText";
  private static final float MIN_HYDRA_ALIGN = 0.3F;
  private static final String BAD_EXERCISE_ID = "bad_exercise_id";
  private static final String DYNAMIC_RANGE = "dynamicRange";
  private final DatabaseImpl db;
  private final ServerProperties serverProps;

  public JsonScoring(DatabaseImpl db) {
    this.db = db;
    this.serverProps = db.getServerProps();
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
    int mostRecentProjectByUser = projid == -1 ? getMostRecentProjectByUser(user) : projid;
    CommonExercise exercise = db.getCustomOrPredefExercise(mostRecentProjectByUser, exerciseID);  // allow custom items to mask out non-custom items

    JsonObject jsonForScore = new JsonObject();

    // so allow an exercise id = 0 with some actual text
    String foreignLanguage = postedWordOrPhrase;
    String transliteration = "";

    if (exercise == null && exerciseID > 1) {
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
      float hydecScore = pretestScore == null ? -1 : pretestScore.getHydecScore();
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
        jsonForScore.addProperty("isfullmatch", answer.getPretestScore().isFullMatch());
      }
      if (addStream) {
        jsonForScore.addProperty("duration", answer.getDurationInMillis());
        jsonForScore.addProperty(DYNAMIC_RANGE, answer.getDynamicRange());
        String path = answer.getPath();
        if (path.isEmpty()) logger.warn("no path?");
        jsonForScore.addProperty("path", path);
        jsonForScore.addProperty("resultID", answer.getResultID());

        if (jsonForScore.get("pretest") == null) {
          jsonForScore.add("pretest", new JsonObject());
        }
        jsonForScore.addProperty("timestamp", answer.getTimestamp());
      } else logger.warn("not adding stream info");
    }

    addValidity(exerciseID, jsonForScore,
        answer == null ? Validity.INVALID : answer.getValidity(),
        answer == null ? "1" : "" + answer.getReqid());

    return jsonForScore;
  }

  /**
   * TODO : connect this to AudioService?
   *
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
    float hydecScore = pretestScore == null ? -1 : pretestScore.getHydecScore();

    Project project = db.getProject(projid);
    //  String language = project.getLanguage();

    jsonForScore = fullJSON ?
        scoreToJSON.getJsonObject(pretestScore) :
        scoreToJSON.getJsonForScore(pretestScore, usePhoneToDisplay, serverProps, project.getLanguageEnum());
    jsonForScore.addProperty(SCORE, hydecScore);

    if (doFlashcard) {
      jsonForScore.addProperty(IS_CORRECT, answer.isCorrect());
      jsonForScore.addProperty(SAID_WORD, answer.isSaidAnswer());
      jsonForScore.addProperty(RESULT_ID, answer.getResultID());
    } else {
      jsonForScore.addProperty(IS_CORRECT, hydecScore > MIN_HYDRA_ALIGN);
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
          asrScoreForAudio.getHydecScore(),
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
        .getASRScoreForAudio(reqid, testAudioFile, sentence, transliteration, DEFAULT, "" + exerciseID,
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
    ClientExercise exercise = db.getCustomOrPredefExercise(projectID, exerciseID);  // allow custom items to mask out non-custom items

    if (exerciseID == 0) {
      exerciseID = db.getUserExerciseDAO().getUnknownExerciseID();
      // make one up
      exercise = new Exercise();
      Exercise exercise1 = (Exercise) exercise;
      exercise1.setForeignLanguage(foreignLanguage);
      exercise1.setID(exerciseID);

    }
    AudioContext audioContext =
        new AudioContext(reqid, user, projectID, getLanguage(projectID), exerciseID,
            0, options.shouldDoDecoding() ? AudioType.PRACTICE : AudioType.LEARN);
    //   logger.info("getAnswer  for " + exerciseID + " for " + user + " and file " + wavPath);
    AudioAnswer answer = getAudioFileHelper(projectID)
        .getAnswer(exercise,
            audioContext,
            wavPath, file, deviceType, device, score,
            options, pretestScore);

    ensureMP3Later(answer.getPath(), user, foreignLanguage, exercise.getEnglish(), getLanguage(projectID));

    return answer;
  }

  /**
   * @param path
   * @param user
   * @param foreignLanguage
   * @param english
   * @param language
   * @see #getAnswer
   */
  private void ensureMP3Later(final String path, final int user, final String foreignLanguage, String english, String language) {
    new Thread(() -> {
      //long then = System.currentTimeMillis();
      writeCompressedVersions(path, new TrackInfo(foreignLanguage, getUserID(user), english, language));
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

    new AudioConversion(serverProps.shouldTrimAudio(), serverProps.getMinDynamicRange())
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

  private String getLanguage(int projectid) {
    return getProject(projectid).getLanguage();
  }

  private Project getProject(int projid) {
    return db.getProject(projid);
  }
}
