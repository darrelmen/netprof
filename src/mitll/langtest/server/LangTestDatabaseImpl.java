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

package mitll.langtest.server;

import audio.image.ImageType;
import audio.imagewriter.SimpleImageWriter;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.client.scoring.AudioPanel;
import mitll.langtest.client.scoring.ScoringAudioPanel;
import mitll.langtest.server.amas.QuizCorrect;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.autocrt.AutoCRT;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.server.database.security.UserSecurityManager;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.server.services.MyRemoteServiceServlet;
import mitll.langtest.shared.ContextPractice;
import mitll.langtest.shared.StartupInfo;
import mitll.langtest.shared.answer.Answer;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.AVPScoreReport;
import mitll.langtest.shared.flashcard.QuizCorrectAndScore;
import mitll.langtest.shared.image.ImageResponse;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.PretestScore;
import mitll.langtest.shared.user.SlimProject;
import mitll.npdata.dao.SlickProject;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.text.CollationKey;
import java.text.Collator;
import java.util.*;

/**
 * Supports all the database interactions.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/7/12
 * Time: 5:49 PM
 */
@SuppressWarnings("serial")
public class LangTestDatabaseImpl extends MyRemoteServiceServlet implements LangTestDatabase {
  private static final Logger logger = Logger.getLogger(LangTestDatabaseImpl.class);

  public static final String DATABASE_REFERENCE = "databaseReference";

  private static final String WAV1 = "wav";
  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";
  private static final int MP3_LENGTH = MP3.length();

  /**
   */
  @Deprecated private AudioFileHelper audioFileHelper;
  private String relativeConfigDir;
  private String configDir;
  //private AudioConversion audioConversion;
  private static final boolean DEBUG = false;


  private String startupMessage = "";

  /**
   * Reco test option lets you run through and score all the reference audio -- if you want to see model performance
   */
  @Override
  public void init() {
    try {
      this.pathHelper = new PathHelper(getServletContext());
      readProperties(getServletContext());
      setInstallPath(db);
      if (serverProps.isAMAS()) {
        audioFileHelper = new AudioFileHelper(pathHelper, serverProps, db, this, null);
      }
    } catch (Exception e) {
      startupMessage = e.getMessage();
      logger.error("Got " + e, e);
    }

    try {
      db.preloadContextPractice();
      getUserListManager().setStateOnExercises();
      db.doReport(serverProps, getServletContext().getRealPath(""), getMailSupport(), pathHelper);
    } catch (Exception e) {
      logger.error("couldn't load database " + e, e);
    }

    try {
//      this.refResultDecoder = new RefResultDecoder(db, serverProps, pathHelper, getAudioFileHelper());
//      refResultDecoder.doRefDecode(getExercises(), relativeConfigDir);
      if (serverProps.isAMAS()) getAudioFileHelper().makeAutoCRT(relativeConfigDir);
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }
  }

  /**
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException
   */
  @Override
  protected void service(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
    try {
      super.service(request, response);
    } catch (ServletException | IOException e) {
      logAndNotifyServerException(e);
      throw e;
    } catch (Exception eee) {
      logAndNotifyServerException(eee);
      throw new ServletException("rethrow exception", eee);
    }
  }

  /**
   * JUST FOR AMAS
   * @param typeToSection
   * @param userID
   * @param exids
   * @return
   */
  public QuizCorrectAndScore getScoresForUser(Map<String, Collection<String>> typeToSection,
                                              int userID,
                                              Collection<Integer> exids) {
    return new QuizCorrect(db).getScoresForUser(typeToSection, userID, exids, getProjectID());
  }

  /**
   * JUST FOR AMAS
   * @param resultID
   * @param correct
   */
  @Override
  public void addStudentAnswer(long resultID, boolean correct) {
    db.getAnswerDAO().addUserScore((int) resultID, correct ? 1.0f : 0.0f);
  }

  /**
   * JUST FOR AMAS
   * TODO : put this back
   *
   * @param audioContext
   * @param answer
   * @param timeSpent
   * @param typeToSection
   * @return
   * @see mitll.langtest.client.amas.TextResponse#getScoreForGuess
   */
  public Answer getScoreForAnswer(AudioContext audioContext, String answer,
                                  long timeSpent,
                                  Map<String, Collection<String>> typeToSection) {
    // AutoCRT.CRTScores scoreForAnswer1 = audioFileHelper.getScoreForAnswer(exercise, questionID, answer);
    AutoCRT.CRTScores scoreForAnswer1 = new AutoCRT.CRTScores();
    double scoreForAnswer = serverProps.useMiraClassifier() ? scoreForAnswer1.getNewScore() : scoreForAnswer1.getOldScore();

    String session = "";// getLatestSession(typeToSection, userID);
    //  logger.warn("getScoreForAnswer user " + userID + " ex " + exercise.getOldID() + " qid " +questionID + " type " +typeToSection + " session " + session);
    boolean correct = scoreForAnswer > 0.5;
    long resultID = db.getAnswerDAO().addTextAnswer(audioContext,
        answer,
        correct,
        (float) scoreForAnswer, (float) scoreForAnswer, session, timeSpent);

    Answer answer1 = new Answer(scoreForAnswer, correct, resultID);
    return answer1;
  }

  private SectionHelper<CommonExercise> getSectionHelper() {
    return db.getSectionHelper(getProjectID());
  }

  /**
   * @param byID
   * @param parentDir
   * @seex LoadTesting#getExercise
   * @seex #makeExerciseListWrapper
   */
/*  private void ensureMP3s(CommonExercise byID, String parentDir) {
    Collection<AudioAttribute> audioAttributes = byID.getAudioAttributes();
    for (AudioAttribute audioAttribute : audioAttributes) {
      if (!ensureMP3(audioAttribute.getAudioRef(), byID.getForeignLanguage(), audioAttribute.getUser().getUserID(), parentDir)) {
//        if (byID.getOldID().equals("1310")) {
//          logger.warn("ensureMP3 : can't find " + audioAttribute + " under " + parentDir + " for " + byID);
//        }
        audioAttribute.setAudioRef(AudioConversion.FILE_MISSING);
      }
    }

//    if (audioAttributes.isEmpty() && byID.getOldID().equals("1310")) {
//      logger.warn("ensureMP3s : (" + getLanguage() + ") no ref audio for " + byID);
//    }
  }*/

  private Collection<CommonExercise> getExercisesForUser() {  return db.getExercises(getProjectID());  }

  public ContextPractice getContextPractice() {
    return db.getContextPractice();
  }

  @Override
  public void reloadExercises() {
    logger.info("reloadExercises --- !");
    db.reloadExercises(getProjectID());
  }

  /**
   * @param wavFile
   * @param title
   * @param artist
   * @return true if mp3 file exists
   * @seex #ensureMP3s(CommonExercise, String)
   * @see #writeAudioFile
   */
/*  private boolean ensureMP3(String wavFile, String title, String artist) {
    return ensureMP3(wavFile, title, artist, pathHelper.getInstallPath());
  }*/
  // int spew = 0;

/*
  private boolean ensureMP3(String wavFile, String title, String artist, String parent) {
    if (wavFile != null) {
      if (!audioConversion.exists(wavFile, parent)) {
        //if (WARN_MISSING_FILE) {
        //   logger.warn("ensureMP3 : can't find " + wavFile + " under " + parent + " trying config... ");
        // }
        parent = configDir;
      }
*/
/*      if (!audioConversion.exists(wavFile, parent)) {// && wavFile.contains("1310")) {
        if (WARN_MISSING_FILE && spew++ < 10) {
          logger.error("ensureMP3 : can't find " + wavFile + " under " + parent + " for " + title + " " + artist);
        }
      }*//*


      String s = audioConversion.ensureWriteMP3(wavFile, parent, false, title, artist);
      boolean isMissing = s.equals(AudioConversion.FILE_MISSING);
*/
/*      if (isMissing && wavFile.contains("1310")) {
        logger.error("ensureMP3 : can't find " + wavFile + " under " + parent + " for " + title + " " + artist);
      }*//*

      return !isMissing;
    }
    return false;
  }
*/

  /**
   * Get an image of desired dimensions for the audio file - only for Waveform and spectrogram.
   * Also returns the audio file duration -- so we can deal with the difference in length between mp3 and wav
   * versions of the same audio file.  (The browser soundmanager plays mp3 and reports audio offsets into
   * the mp3 file, but all the images are generated from the shorter wav file.)
   * <p>
   * TODO : Worrying about absolute vs relative path is maddening.  Must be a better way!
   *
   * @param reqid
   * @param audioFile
   * @param imageType
   * @param width
   * @param height
   * @param exerciseID
   * @return path to an image file
   * @see mitll.langtest.client.scoring.AudioPanel#getImageURLForAudio
   */
  public ImageResponse getImageForAudioFile(int reqid, String audioFile, String imageType, int width, int height,
                                            String exerciseID) {
    if (audioFile.isEmpty()) logger.error("huh? audio file is empty for req id " + reqid + " exid " + exerciseID);

    SimpleImageWriter imageWriter = new SimpleImageWriter();

    String wavAudioFile = getWavAudioFile(audioFile);
    File testFile = new File(wavAudioFile);
    if (!testFile.exists() || testFile.length() == 0) {
      if (testFile.length() == 0) logger.error("getImageForAudioFile : huh? " + wavAudioFile + " is empty???");
      return new ImageResponse();
    }
    ImageType imageType1 =
        imageType.equalsIgnoreCase(ImageType.WAVEFORM.toString()) ? ImageType.WAVEFORM :
            imageType.equalsIgnoreCase(ImageType.SPECTROGRAM.toString()) ? ImageType.SPECTROGRAM : null;
    if (imageType1 == null) {
      logger.error("getImageForAudioFile '" + imageType + "' is unknown?");
      return new ImageResponse(); // success = false!
    }
    String imageOutDir = pathHelper.getImageOutDir();

    if (DEBUG) {
      logger.debug("getImageForAudioFile : getting images (" + width + " x " + height + ") (" + reqid + ") type " + imageType +
          " for " + wavAudioFile + "");
    }

    long then = System.currentTimeMillis();

    String absolutePathToImage = imageWriter.writeImage(wavAudioFile, getAbsoluteFile(imageOutDir).getAbsolutePath(),
        width, height, imageType1, exerciseID);
    long now = System.currentTimeMillis();
    long diff = now - then;
    if (diff > 100) {
      logger.debug("getImageForAudioFile : got images (" + width + " x " + height + ") (" + reqid + ") type " + imageType +
          " for " + wavAudioFile + " took " + diff + " millis");
    }
    String installPath = pathHelper.getInstallPath();

    String relativeImagePath = absolutePathToImage;
    if (absolutePathToImage.startsWith(installPath)) {
      relativeImagePath = absolutePathToImage.substring(installPath.length());
    } else {
      logger.error("getImageForAudioFile huh? file path " + absolutePathToImage + " doesn't start with " + installPath + "?");
    }

    relativeImagePath = pathHelper.ensureForwardSlashes(relativeImagePath);
    if (relativeImagePath.startsWith("/")) {
      relativeImagePath = relativeImagePath.substring(1);
    }
    String imageURL = relativeImagePath;
    double duration = new AudioCheck(serverProps).getDurationInSeconds(wavAudioFile);
    if (duration == 0) {
      logger.error("huh? " + wavAudioFile + " has zero duration???");
    }
    /*    logger.debug("for " + wavAudioFile + " type " + imageType + " rel path is " + relativeImagePath +
        " url " + imageURL + " duration " + duration);*/

    return new ImageResponse(reqid, imageURL, duration);
  }

  private String getWavAudioFile(String audioFile) {
    if (audioFile.endsWith("." + AudioTag.COMPRESSED_TYPE) || audioFile.endsWith(MP3)) {
      String wavFile = removeSuffix(audioFile) + WAV;
      File test = getAbsoluteFile(wavFile);
      audioFile = test.exists() ? test.getAbsolutePath() : getAudioFileHelper().getWavForMP3(audioFile);
    }

    return ensureWAV(audioFile);
  }

  private String removeSuffix(String audioFile) {
    return audioFile.substring(0, audioFile.length() - MP3_LENGTH);
  }

  private String ensureWAV(String audioFile) {
    if (!audioFile.endsWith(WAV1)) {
      return audioFile.substring(0, audioFile.length() - MP3_LENGTH) + WAV;
    } else {
      return audioFile;
    }
  }

  /**
   * Get properties (first time called read properties file -- e.g. see war/config/levantine/config.properties).
   *
   * @return
   * @paramx userID
   * @see mitll.langtest.client.LangTest#onModuleLoad
   */
  @Override
  public StartupInfo getStartupInfo() {
    List<SlimProject> projectInfos = new ArrayList<>();
    if (db == null) {
      logger.info("no db yet...");
    } else {
      projectInfos = getNestedProjectInfo();
    }

    return new StartupInfo(serverProps.getProperties(), projectInfos, startupMessage);
  }

  /**
   * TODO : consider moving this into user service?
   * what if later an admin changes it while someone else is looking at it...
   *
   * @return
   */
  private List<SlimProject> getNestedProjectInfo() {
    List<SlimProject> projectInfos = new ArrayList<>();

    Map<String, List<SlickProject>> langToProject = new TreeMap<>();
    Collection<SlickProject> all = db.getProjectDAO().getAll();
//    logger.info("found " + all.size() + " projects");
    for (SlickProject project : all) {
      List<SlickProject> slimProjects = langToProject.get(project.language());
      if (slimProjects == null) langToProject.put(project.language(), slimProjects = new ArrayList<>());
      slimProjects.add(project);
    }

//    logger.info("lang->project is " + langToProject);

    for (String lang : langToProject.keySet()) {
      List<SlickProject> slickProjects = langToProject.get(lang);
      SlickProject project = slickProjects.get(0);
      SlimProject parent = getProjectInfo(project);
      projectInfos.add(parent);

      if (slickProjects.size() > 1) {
        for (SlickProject slickProject : slickProjects) {
          parent.addChild(getProjectInfo(slickProject));
          //  logger.info("\t add child to " + parent);
        }
      }
    }

    return projectInfos;
  }

  /**
   * TODOx FIX ME
   *
   * @param project
   * @return
   */
  private SlimProject getProjectInfo(SlickProject project) {
    boolean hasModel = project.getProp(ServerProperties.MODELS_DIR) != null;

    Collection<CommonExercise> exercises = db.getExercises(project.id());

    boolean isRTL = false;
    if (!exercises.isEmpty()) {
      CommonExercise next = exercises.iterator().next();
      HasDirection.Direction direction = WordCountDirectionEstimator.get().estimateDirection(next.getForeignLanguage());
      // String rtl = properties.get("rtl");
      isRTL = direction == HasDirection.Direction.RTL;
      // logger.info("examined text and found it to be " + direction);
    }

    return new SlimProject(project.id(), project.name(), project.language(),
        project.countrycode(), project.course(), project.displayorder(),
        hasModel,
        isRTL);
  }

  /**
   * @param resultID
   * @param width
   * @param height
   * @return
   * @see ScoringAudioPanel#scoreAudio(String, int, String, AudioPanel.ImageAndCheck, AudioPanel.ImageAndCheck, int, int, int)
   */
  @Override
  public PretestScore getResultASRInfo(int resultID, int width, int height) {
    PretestScore asrScoreForAudio = null;
    try {
      Result result = db.getResultDAO().getResultByID(resultID);

      int exerciseID = result.getExerciseID();

      boolean isAMAS = serverProps.isAMAS();
      CommonShell exercise;
      String sentence = "";
      if (isAMAS) {
        exercise = db.getAMASExercise(exerciseID);
        sentence = exercise.getForeignLanguage();
      } else {
        CommonExercise exercise1 = db.getExercise(getProjectID(), exerciseID);
        exercise = exercise1;

        Collection<CommonExercise> directlyRelated = exercise1.getDirectlyRelated();
        sentence =
            result.getAudioType().isContext() && !directlyRelated.isEmpty() ?
                directlyRelated.iterator().next().getForeignLanguage() :
                exercise.getForeignLanguage();
      }


      // maintain backward compatibility - so we can show old recordings of ref audio for the context sentence
//      String sentence = isAMAS ? exercise.getForeignLanguage() :
//          (result.getAudioType().isContext()) ?
//              db.getExercise(exerciseID).getDirectlyRelated().iterator().next().getForeignLanguage() :
//              exercise.getForeignLanguage();

      if (exercise == null) {
        logger.warn(getLanguage() + " can't find exercise id " + exerciseID);
        return new PretestScore();
      } else {
        String audioFilePath = result.getAnswer();

        // NOTE : actively avoid doing this -
        //ensureMP3(audioFilePath, sentence, "" + result.getUserid());
        //logger.info("resultID " +resultID+ " temp dir " + tempDir.getAbsolutePath());
        asrScoreForAudio = getAudioFileHelper().getASRScoreForAudio(1,
            audioFilePath, sentence,
            width, height,
            true,  // make transcript images with colored segments
            false, // false = do alignment
            serverProps.useScoreCache(),
            "" + exerciseID, result, serverProps.usePhoneToDisplay(), false);
      }
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }

    return asrScoreForAudio;
  }

  /**
   * So first we check and see if we've already done alignment for this audio (if reference audio), and if so, we grab the Result
   * object out of the result table and use it and it's json to generate the score info and transcript inmages.
   *
   * @param reqid
   * @param resultID
   * @param testAudioFile
   * @param sentence
   * @param width
   * @param height
   * @param useScoreToColorBkg
   * @param exerciseID
   * @return
   * @see ScoringAudioPanel#scoreAudio(String, int, String, AudioPanel.ImageAndCheck, AudioPanel.ImageAndCheck, int, int, int)
   */
  public PretestScore getASRScoreForAudio(int reqid, long resultID, String testAudioFile, String sentence,
                                          int width, int height, boolean useScoreToColorBkg, int exerciseID) {
    return getPretestScore(reqid, (int) resultID, testAudioFile, sentence, width, height, useScoreToColorBkg, exerciseID, false);
  }

  /**
   * Be careful - we lookup audio file by .wav extension
   *
   * @param reqid
   * @param resultID
   * @param testAudioFile
   * @param sentence
   * @param width
   * @param height
   * @param useScoreToColorBkg
   * @param exerciseID
   * @param usePhoneToDisplay
   * @return
   */
  private PretestScore getPretestScore(int reqid, int resultID, String testAudioFile, String sentence,
                                       int width, int height, boolean useScoreToColorBkg, int exerciseID,
                                       boolean usePhoneToDisplay) {
    if (testAudioFile.equals(AudioConversion.FILE_MISSING)) return new PretestScore(-1);
    long then = System.currentTimeMillis();

    String[] split = testAudioFile.split(File.separator);
    String answer = split[split.length - 1];
    String wavEndingAudio = answer.replaceAll(".mp3", ".wav").replaceAll(".ogg", ".wav");
    Result cachedResult = db.getRefResultDAO().getResult(exerciseID, wavEndingAudio);
    if (cachedResult != null) {
      if (DEBUG)
        logger.debug("getPretestScore Cache HIT  : align exercise id = " + exerciseID + " file " + answer +
            " found previous " + cachedResult.getUniqueID());
    } else {
      logger.debug("getPretestScore Cache MISS : align exercise id = " + exerciseID + " file " + answer);
    }

    boolean usePhoneToDisplay1 = usePhoneToDisplay || serverProps.usePhoneToDisplay();

    PretestScore asrScoreForAudio =
        getAudioFileHelper().getASRScoreForAudio(reqid, testAudioFile, sentence, width, height, useScoreToColorBkg,
            false, serverProps.useScoreCache(), "" + exerciseID, cachedResult, usePhoneToDisplay1, false);

    long timeToRunHydec = System.currentTimeMillis() - then;

    logger.debug("getPretestScore : scoring" +
        " file " + testAudioFile + " for " +
        " exid " + exerciseID +
        " sentence " + sentence.length() + " characters long : " +
        " score " + asrScoreForAudio.getHydecScore() +
        " took " + timeToRunHydec + " millis " +
        " usePhoneToDisplay " + usePhoneToDisplay1);

    if (resultID > -1 && cachedResult == null) { // alignment has two steps : 1) post the audio, then 2) do alignment
      db.rememberScore(resultID, asrScoreForAudio);
    }
    return asrScoreForAudio;
  }


  /**
   * @param reqid
   * @param resultID
   * @param testAudioFile
   * @param sentence
   * @param width
   * @param height
   * @param useScoreToColorBkg
   * @param exerciseID
   * @return
   * @see ScoringAudioPanel#scoreAudio(String, int, String, AudioPanel.ImageAndCheck, AudioPanel.ImageAndCheck, int, int, int)
   */
  @Override
  public PretestScore getASRScoreForAudioPhonemes(int reqid, long resultID, String testAudioFile, String sentence,
                                                  int width, int height, boolean useScoreToColorBkg, int exerciseID) {
    return getPretestScore(reqid, (int) resultID, testAudioFile, sentence, width, height, useScoreToColorBkg, exerciseID, true);
  }

  @Override
  public void addRoundTrip(int resultID, int roundTrip) {
    db.getAnswerDAO().addRoundTrip(resultID, roundTrip);
  }

  /**
   * @param exerciseID
   * @param field
   * @param status
   * @param comment
   * @param userID
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#addAnnotation(String, String, String)
   */
  @Override
  public void addAnnotation(int exerciseID, String field, String status, String comment, int userID) {
    getUserListManager().addAnnotation(exerciseID, field, status, comment, userID);
  }

  /**
   * @param id
   * @param isCorrect
   * @param creatorID
   * @see mitll.langtest.client.qc.QCNPFExercise#markReviewed
   */
  public void markReviewed(int id, boolean isCorrect, int creatorID) {
    getUserListManager().markCorrectness(id, isCorrect, creatorID);
  }

  /**
   * @param exid
   * @param state
   * @param creatorID
   * @see mitll.langtest.client.qc.QCNPFExercise#markAttentionLL
   */
  public void markState(int exid, STATE state, int creatorID) {
    getUserListManager().markState(exid, state, creatorID);
  }

  /**
   * Can't check if it's valid if we don't have a model.
   *
   * @param foreign
   * @return
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#isValidForeignPhrase(mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, boolean)
   */
  @Override
  public boolean isValidForeignPhrase(String foreign) {
    return getAudioFileHelper().checkLTSOnForeignPhrase(foreign);
  }

  IUserListManager getUserListManager() {
    return db.getUserListManager();
  }

  /**
   * @param exercise
   * @return
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#duplicateExercise
   */
  //@Override
/*
  public CommonExercise duplicateExercise(CommonExercise exercise) {
    return db.duplicateExercise(exercise);
  }
*/

  /**
   * TODO : maybe fully support this
   * @param id
   * @return
   * @seex ReviewEditableExercise#confirmThenDeleteItem
   */
  public boolean deleteItem(int id) {
    boolean b = db.deleteItem(id, getProjectID());
    if (b) {
      // force rebuild of full trie
      getProject().buildExerciseTrie(db);
    }
    return b;
  }

  /**
   * @param id
   * @param widgetType
   * @param exid
   * @param context
   * @param userid
   * @param hitID
   * @param device
   * @see mitll.langtest.client.instrumentation.ButtonFactory#logEvent
   */
  @Override
  public void logEvent(String id, String widgetType, String exid, String context, int userid, String hitID, String device) {
    try {
      db.logEvent(id, widgetType, exid, context, userid, device);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * @return
   * @see mitll.langtest.client.instrumentation.EventTable#show
   */
  public List<Event> getEvents() {
    return db.getEventDAO().getAll(getProjectID());
  }

  /**
   * @param audioAttribute
   * @param exid
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getPanelForAudio
   */
  @Override
  public void markAudioDefect(AudioAttribute audioAttribute, HasID exid) {
    logger.debug("markAudioDefect mark audio defect for " + exid + " on " + audioAttribute);
    //CommonExercise before = db.getCustomOrPredefExercise(exid);  // allow custom items to mask out non-custom items
    //int beforeNumAudio = before.getAudioAttributes().size();
    db.markAudioDefect(audioAttribute);

    CommonExercise byID = db.getCustomOrPredefExercise(getProjectID(), exid.getID());  // allow custom items to mask out non-custom items

    if (!byID.getMutableAudio().removeAudio(audioAttribute)) {
      String key = audioAttribute.getKey();
      logger.warn("markAudioDefect huh? couldn't remove key '" + key +
          "' : " + audioAttribute + " from ex #" + exid +
          "\n\tkeys were " + byID.getAudioRefToAttr().keySet() + " contains " + byID.getAudioRefToAttr().containsKey(key));
    }
    /*   int afterNumAudio = byID.getAudioAttributes().size();
    if (afterNumAudio != beforeNumAudio - 1) {
      logger.error("\thuh? before there were " + beforeNumAudio + " but after there were " + afterNumAudio);
    }*/
  }

  /**
   * This supports labeling really old audio for gender.
   *
   * TODO : why think about attach audio???
   * @param attr
   * @param isMale
   * @see mitll.langtest.client.qc.QCNPFExercise#getGenderGroup
   */
  @Override
  public void markGender(AudioAttribute attr, boolean isMale) {
    CommonExercise customOrPredefExercise = db.getCustomOrPredefExercise(getProjectID(), attr.getExid());
    int projid = -1;
    if (customOrPredefExercise == null) {
      logger.error("markGender can't find exercise id " + attr.getExid() + "?");
    } else {
      projid = customOrPredefExercise.getProjectID();
    }
    db.getAudioDAO().addOrUpdateUser(isMale ? BaseUserDAO.DEFAULT_MALE_ID : BaseUserDAO.DEFAULT_FEMALE_ID, projid, attr);

    int exid = attr.getExid();
    CommonExercise byID = db.getCustomOrPredefExercise(projid, exid);
    if (byID == null) {
      logger.error(getLanguage() + " : couldn't find exercise " + exid);
      logAndNotifyServerException(new Exception("couldn't find exercise " + exid));
    } else {

      // TODO : consider putting this back???
      //   byID.getAudioAttributes().clear();
//      logger.debug("re-attach " + attr + " given isMale " + isMale);

      // TODO : consider putting this back???
      //   attachAudio(byID);
/*
      String addr = Integer.toHexString(byID.hashCode());
      for (AudioAttribute audioAttribute : byID.getAudioAttributes()) {
        logger.debug("markGender 1 after gender change, now " + audioAttribute + " : " +audioAttribute.getUserid() + " on " + addr);
      }
*/

      db.getExerciseDAO(getProjectID()).addOverlay(byID);

/*      CommonExercise customOrPredefExercise = db.getCustomOrPredefExercise(exid);
      String adrr3 = Integer.toHexString(customOrPredefExercise.hashCode());
      logger.info("markGender getting " + adrr3 + " : " + customOrPredefExercise);
      for (AudioAttribute audioAttribute : customOrPredefExercise.getAudioAttributes()) {
        logger.debug("markGender 2 after gender change, now " + audioAttribute + " : " +audioAttribute.getUserid() + " on "+ adrr3);
      }*/

    }
    getSectionHelper().refreshExercise(byID);
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.client.user.UserManager#getPermissionsAndSetUser
   */
/*
  private User getUserBy(int id) {
    return db.getUserDAO().getUserWhere(id);
  }
*/
  // Results ---------------------



  /**
   * A low overhead way of doing alignment.
   * <p>
   * Useful for conversational dialogs - Jennifer Melot's project.
   *
   * @param base64EncodedString
   * @param textToAlign
   * @param identifier
   * @param reqid
   * @param device
   * @return
   * @see mitll.langtest.client.scoring.SimplePostAudioRecordButton#postAudioFile(String)
   */
  @Override
  public AudioAnswer getAlignment(String base64EncodedString,
                                  String textToAlign,
                                  String identifier,
                                  int reqid, String device) {
    AudioAnswer audioAnswer = getAudioFileHelper().getAlignment(base64EncodedString, textToAlign, identifier, reqid,
        serverProps.usePhoneToDisplay());

    if (!audioAnswer.isValid() && audioAnswer.getDurationInMillis() == 0) {
      logger.warn("huh? got zero length recording " + identifier);
      logEvent("audioRecording", "writeAudioFile", identifier, "Writing audio - got zero duration!", -1, "unknown", device);
    }
    return audioAnswer;
  }



  private File getAbsoluteFile(String path) {
    return pathHelper.getAbsoluteFile(path);
  }


  /**
   * Filter out the default audio recordings...
   *
   * @return
   * @see mitll.langtest.client.monitoring.MonitoringManager#doMaleFemale
   */
  @Override
  public Map<String, Float> getMaleFemaleProgress() {
    return db.getMaleFemaleProgress(getProjectID());
  }

  /**
   * @param userid         who's asking?
   * @param ids            items the user has actually practiced/recorded audio for
   * @param latestResultID
   * @param typeToSection  indicates the unit and chapter(s) we're asking about
   * @param userListID     if we're asking about a list and not predef items
   * @return
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#onSetComplete()
   */
  @Override
  public AVPScoreReport getUserHistoryForList(int userid,
                                              Collection<Integer> ids,
                                              long latestResultID,
                                              Map<String, Collection<String>> typeToSection, long userListID) {
    //logger.debug("getUserHistoryForList " + userid + " and " + ids + " type to section " + typeToSection);
    UserList<CommonShell> userListByID = userListID != -1 ? db.getUserListByID(userListID, getProjectID()) : null;
    List<Integer> allIDs = new ArrayList<>();
    Map<Integer, CollationKey> idToKey = new HashMap<>();

    Collator collator = getCollator();
    if (userListByID != null) {
      for (CommonShell exercise : userListByID.getExercises()) {
        populateCollatorMap(allIDs, idToKey, collator, exercise);
      }
    } else {
      Collection<CommonExercise> exercisesForState = (typeToSection == null || typeToSection.isEmpty()) ? getExercisesForUser() :
          getSectionHelper().getExercisesForSelectionState(typeToSection);

      for (CommonExercise exercise : exercisesForState) {
        populateCollatorMap(allIDs, idToKey, collator, exercise);
      }
    }
    //logger.debug("for " + typeToSection + " found " + allIDs.size());
    return db.getUserHistoryForList(userid, ids, (int) latestResultID, allIDs, idToKey);
  }

  Collator getCollator() {
    return getAudioFileHelper().getCollator();
  }

  private void populateCollatorMap(List<Integer> allIDs, Map<Integer, CollationKey> idToKey, Collator collator,
                                   CommonShell exercise) {
    allIDs.add(exercise.getID());
    CollationKey collationKey = collator.getCollationKey(exercise.getForeignLanguage());
    idToKey.put(exercise.getID(), collationKey);
  }

  public void logMessage(String message) {
    if (message.length() > 10000) message = message.substring(0, 10000);
    String prefixedMessage = "for " + pathHelper.getInstallPath() + " from client " + message;
    logger.debug(prefixedMessage);

    if (message.startsWith("got browser exception")) {
      sendEmail("Javascript Exception", getInfo(prefixedMessage));
    }
  }

  private MailSupport getMailSupport() {
    return new MailSupport(serverProps.isDebugEMail(), serverProps.isTestEmail());
  }

  @Override
  public void destroy() {
//    refResultDecoder.setStopDecode(true);
    //stopOggCheck = true;
    super.destroy();
    if (db == null) {
      logger.error("DatabaseImpl was never made properly...");
    } else {
      db.destroy(); // TODO : redundant with h2 shutdown hook?
      db.stopDecode();
    }
  }


  private AudioFileHelper getAudioFileHelper() {
    if (serverProps.isAMAS()) {
      return audioFileHelper;
    } else {
      Project project = getProject();
      if (project == null) {
        logger.warn("getAudioFileHelper no current project???");
        return null;
      }
      return project.getAudioFileHelper();
    }
  }

  /**
   * The config web.xml file.
   * As a final step, creates the DatabaseImpl!<br></br>
   * <p>
   * NOTE : makes the database available to other servlets via the databaseReference servlet context attribute.
   * Note that this will only ever be called once.
   *
   * @param servletContext
   * @see #init()
   */
  private void readProperties(ServletContext servletContext) {
    this.relativeConfigDir = "config" + File.separator + servletContext.getInitParameter("config");
    this.configDir = pathHelper.getInstallPath() + File.separator + relativeConfigDir;
    pathHelper.setConfigDir(configDir);

    serverProps = new ServerProperties(servletContext, configDir);
 //   audioConversion = new AudioConversion(serverProps);
    db = makeDatabaseImpl(serverProps.getH2Database());
    shareDB(servletContext);
    securityManager = new UserSecurityManager(db.getUserDAO());
//    shareLoadTesting(servletContext);
  }

/*
  private void shareLoadTesting(ServletContext servletContext) {
    Object loadTesting = servletContext.getAttribute(ScoreServlet.LOAD_TESTING);
    if (loadTesting != null) {
      logger.debug("hmm... found existing load testing reference " + loadTesting);
    }
    servletContext.setAttribute(ScoreServlet.LOAD_TESTING, this);
  }
*/

  /**
   * @param servletContext
   * @see #readProperties
   */
  private void shareDB(ServletContext servletContext) {
    Object databaseReference = servletContext.getAttribute(DATABASE_REFERENCE);
    if (databaseReference != null) {
      logger.debug("hmm... found existing database reference " + databaseReference);
    }

    servletContext.setAttribute(DATABASE_REFERENCE, db);
  }

  private DatabaseImpl makeDatabaseImpl(String h2DatabaseFile) {
    //logger.debug("word pairs " +  serverProps.isWordPairs() + " language " + serverProps.getLanguage() + " config dir " + relativeConfigDir);
    return new DatabaseImpl(configDir, relativeConfigDir, h2DatabaseFile, serverProps, pathHelper, true, this, false);
  }

  /**
   * @param db
   * @return
   * @see LangTestDatabaseImpl#init()
   */
  private void setInstallPath(DatabaseImpl db) {
    String lessonPlanFile = getLessonPlan();
    if (lessonPlanFile != null &&
        !serverProps.getLessonPlan().startsWith("http") &&
        !new File(lessonPlanFile).exists()) {
      logger.error("couldn't find lesson plan file " + lessonPlanFile);
    }

    String mediaDir = "";//relativeConfigDir + File.separator + serverProps.getMediaDir();
    String installPath = pathHelper.getInstallPath();
    logger.debug("setInstallPath " + installPath + " " + lessonPlanFile + " media " + serverProps.getMediaDir() + " rel media " + mediaDir);
    db.setInstallPath(installPath,
        lessonPlanFile,
        mediaDir);
  }

  private String getLessonPlan() {
    return serverProps.getLessonPlan() == null ? null : configDir + File.separator + serverProps.getLessonPlan();
  }
}
