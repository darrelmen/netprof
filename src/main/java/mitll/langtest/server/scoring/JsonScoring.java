package mitll.langtest.server.scoring;

import mitll.langtest.server.ScoreServlet;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.*;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.PretestScore;
import mitll.langtest.shared.user.MiniUser;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
  private static final String INVALID = "invalid";

  private static final String RESULT_ID = "resultID";

  private static final ImageOptions DEFAULT = ImageOptions.getDefault();
  public static final boolean TRY_TO_DO_ALIGNMENT = false;
  public static final String EXERCISE_TEXT = "exerciseText";
  private final DatabaseImpl db;
  private final ServerProperties serverProps;

  public JsonScoring(DatabaseImpl db) {
    this.db = db;
    this.serverProps = db.getServerProps();
  }


  /**
   * @param reqid      label response with req id so the client can tell if it got a stale response
   * @param projid
   * @param exerciseID for this exercise
   * @param user       by this user
   * @param request    mostly decode, could be record if doing appen corpora recording
   * @param wavPath    relative path to posted audio file
   * @param saveFile   File handle to file
   * @param deviceType iPad,iPhone, or browser
   * @param device     id for device - helpful for iPads, etc.
   * @param options
   * @param fullJSON
   * @return score json
   * @paramx allowAlternates   decode against multiple alternatives (e.g. male and female spanish words for the same english word)
   * @paramx usePhoneToDisplay should we remap the phones to different labels for display
   * @see ScoreServlet#getJsonForAudio
   */
  public JSONObject getJsonForAudioForUser(int reqid,
                                           int projid,
                                           int exerciseID,
                                           int user,
                                           ScoreServlet.PostRequest request,
                                           String wavPath,
                                           File saveFile,
                                           String deviceType,
                                           String device,
                                           DecoderOptions options,
                                           boolean fullJSON) {
    long then = System.currentTimeMillis();
    int mostRecentProjectByUser = projid == -1 ? getMostRecentProjectByUser(user) : projid;
    CommonExercise exercise = db.getCustomOrPredefExercise(mostRecentProjectByUser, exerciseID);  // allow custom items to mask out non-custom items

    JSONObject jsonForScore = new JSONObject();
    if (exercise == null) {
      jsonForScore.put(VALID, "bad_exercise_id");
      return jsonForScore;
    }
    boolean doFlashcard = request == ScoreServlet.PostRequest.DECODE;
    options.setDoFlashcard(doFlashcard);
    AudioAnswer answer = getAudioAnswer(reqid, exerciseID, user, wavPath, saveFile, deviceType, device, exercise,
        options);
    long now = System.currentTimeMillis();
    PretestScore pretestScore = answer == null ? null : answer.getPretestScore();
    float hydecScore = pretestScore == null ? -1 : pretestScore.getHydecScore();

    logger.debug("getJsonForAudioForUser flashcard " + doFlashcard +
        " exercise id " + exerciseID + " took " + (now - then) +
        " millis for " + saveFile.getName() + " = " + hydecScore);

    if (answer != null && answer.isValid() && pretestScore != null) {
      boolean usePhoneToDisplay = options.isUsePhoneToDisplay();
      ScoreToJSON scoreToJSON = new ScoreToJSON();

      String language = db.getProject(projid).getLanguage();

      jsonForScore = fullJSON ?
          scoreToJSON.getJsonObject(pretestScore) :
          scoreToJSON.getJsonForScore(pretestScore, usePhoneToDisplay, serverProps, language);
      jsonForScore.put(SCORE, pretestScore.getHydecScore());

      if (doFlashcard) {
        jsonForScore.put(IS_CORRECT, answer.isCorrect());
        jsonForScore.put(SAID_WORD, answer.isSaidAnswer());
        int decodeResultID = answer.getResultID();
        jsonForScore.put(RESULT_ID, decodeResultID);
      }
    }
    addValidity(exerciseID, jsonForScore, answer);
    return jsonForScore;
  }

  /**
   * @param id
   * @return
   * @see #getAnswer
   */
  private int getMostRecentProjectByUser(int id) {
    return db.getUserProjectDAO().mostRecentByUser(id);
  }

  /**
   * @param reqid      label response with req id so the client can tell if it got a stale response
   * @param exerciseID for this exercise - redundant
   * @param user       by this user
   * @param wavPath    path to posted audio file
   * @param saveFile
   * @param deviceType
   * @param device
   * @param exercise
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
                                     CommonExercise exercise,
                                     DecoderOptions options) {
    AudioAnswer answer;

    if (options.isDoFlashcard()) {
      options.setDoFlashcard(true);
      answer = getAnswer(reqid, exerciseID, user, wavPath, saveFile, -1, deviceType, device,
          options
      );
    } else {
      PretestScore asrScoreForAudio = getASRScoreForAudio(reqid, exerciseID, wavPath,
          exercise.getForeignLanguage(),
          exercise.getTransliteration(),
          options.isUsePhoneToDisplay(),
          exercise.getProjectID());

      options.setDoFlashcard(false);

      answer = getAnswer(reqid, exerciseID, user, wavPath, saveFile, asrScoreForAudio.getHydecScore(),
          deviceType, device,
          options
      );
      answer.setPretestScore(asrScoreForAudio);
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

    return audioFileHelper.getASRScoreForAudio(reqid, testAudioFile, sentence, transliteration, DEFAULT, "" + exerciseID,
        null,
        new DecoderOptions()
            .setDoFlashcard(false)
            .setCanUseCache(db.getServerProps().useScoreCache())
            .setUsePhoneToDisplay(usePhoneToDisplay));
  }

  private AudioFileHelper getAudioFileHelper(int projectid) {
    return getProject(projectid).getAudioFileHelper();
  }

  /**
   * Don't wait for mp3 to write to return - can take 70 millis for a short file.
   *
   * @param reqid
   * @param exerciseID
   * @param user
   * @param wavPath
   * @param file
   * @param deviceType
   * @param device
   * @return
   * @paramx doFlashcard
   * @paramx allowAlternates
   * @see #getJsonForAudioForUser
   */
  private AudioAnswer getAnswer(int reqid,
                                int exerciseID,
                                int user,
                                String wavPath,
                                File file,
                                float score,
                                String deviceType,
                                String device,
                                DecoderOptions options) {
    CommonExercise exercise = db.getCustomOrPredefExercise(getMostRecentProjectByUser(user), exerciseID);  // allow custom items to mask out non-custom items

    int projectID = exercise.getProjectID();
    AudioContext audioContext =
        new AudioContext(reqid, user, projectID, getLanguage(projectID), exerciseID,
            0, options.isDoFlashcard() ? AudioType.PRACTICE : AudioType.LEARN);

    AudioFileHelper audioFileHelper = getAudioFileHelper(projectID);
    AudioAnswer answer = audioFileHelper.getAnswer(exercise,
        audioContext,
        wavPath, file, deviceType, device, score,
        options);

    final String path = answer.getPath();
    final String foreignLanguage = exercise.getForeignLanguage();

    ensureMP3Later(path, user, foreignLanguage, exercise.getEnglish(), getLanguage(exercise.getProjectID()));

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
    }).start();
  }

  /**
   * @param wavFile
   * @param trackInfo
   * @see  #ensureMP3Later
   */
  private void writeCompressedVersions(String wavFile, TrackInfo trackInfo) {
    File absolutePathToWav = new File(wavFile);
    if (!absolutePathToWav.exists()) logger.error("no file at " + absolutePathToWav);
    String s = new AudioConversion(serverProps.shouldTrimAudio(), serverProps.getMinDynamicRange()).writeCompressedVersions(absolutePathToWav, false, trackInfo);
  }

  private String getUserID(int userid) {
    MiniUser userWhere = db.getUserDAO().getMiniUser(userid);
    if (userWhere == null) logger.error("huh? can't find user by " + userid);
    return userWhere == null ? "" + userid : userWhere.getUserID();
  }

  /**
   * @param exerciseID
   * @param jsonForScore
   * @param answer
   * @see #getJsonForAudioForUser
   */
  private void addValidity(int exerciseID, JSONObject jsonForScore, AudioAnswer answer) {
    jsonForScore.put(EXID, exerciseID);
    jsonForScore.put(VALID, answer == null ? INVALID : answer.getValidity().toString());
    jsonForScore.put(REQID, answer == null ? 1 : "" + answer.getReqid());
  }

  private String getLanguage(int projectid) {
    return getProject(projectid).getLanguage();
  }

  private Project getProject(int projid) {
    return db.getProject(projid);
  }
}
