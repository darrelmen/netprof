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

import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.client.scoring.AudioPanel;
import mitll.langtest.client.scoring.ScoringAudioPanel;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.AudioFileHelper;
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
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.AVPScoreReport;
import mitll.langtest.shared.instrumentation.Event;
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

  /**
   */
  @Deprecated
  private AudioFileHelper audioFileHelper;
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

  protected SectionHelper<CommonExercise> getSectionHelper() {
    return super.getSectionHelper();
  }

  /**
   * @paramx byID
   * @paramx parentDir
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
  private Collection<CommonExercise> getExercisesForUser() {
    return db.getExercises(getProjectID());
  }

  public ContextPractice getContextPractice() {
    return db.getContextPractice();
  }

  @Override
  public void reloadExercises() {
    logger.info("reloadExercises --- !");
    db.reloadExercises(getProjectID());
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
