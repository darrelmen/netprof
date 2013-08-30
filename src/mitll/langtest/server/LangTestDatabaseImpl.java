package mitll.langtest.server;

import audio.image.ImageType;
import audio.imagewriter.ImageWriter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Files;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.SectionHelper;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.server.scoring.AutoCRTScoring;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.grade.CountAndGradeID;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.flashcard.FlashcardResponse;
import mitll.langtest.shared.grade.Grade;
import mitll.langtest.shared.ImageResponse;
import mitll.langtest.shared.flashcard.Leaderboard;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.grade.ResultsAndGrades;
import mitll.langtest.shared.flashcard.ScoreInfo;
import mitll.langtest.shared.SectionNode;
import mitll.langtest.shared.monitoring.Session;
import mitll.langtest.shared.Site;
import mitll.langtest.shared.taboo.GameInfo;
import mitll.langtest.shared.taboo.PartnerState;
import mitll.langtest.shared.taboo.StimulusAnswerPair;
import mitll.langtest.shared.taboo.TabooState;
import mitll.langtest.shared.User;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Supports all the database interactions.
 * User: GO22670
 * Date: 5/7/12
 * Time: 5:49 PM
 */
@SuppressWarnings("serial")
public class LangTestDatabaseImpl extends RemoteServiceServlet implements LangTestDatabase, AutoCRTScoring {
  private static final Logger logger = Logger.getLogger(LangTestDatabaseImpl.class);

  private static final int MB = (1024 * 1024);
  private static final int TIMEOUT = 30;

  private DatabaseImpl db, studentAnswersDB;
  private AudioFileHelper audioFileHelper;
  private String relativeConfigDir;
  private String configDir;
  private final ServerProperties serverProps = new ServerProperties();
  private PathHelper pathHelper;

  private final Cache<String, String> userToExerciseID = CacheBuilder.newBuilder()
      .concurrencyLevel(4)
      .maximumSize(10000)
      .expireAfterWrite(TIMEOUT, TimeUnit.MINUTES).build();

  /**
   * This allows us to upload an exercise file and create a new {@link Site}.
   * @see mitll.langtest.client.DataCollectAdmin#makeDataCollectNewSiteForm2
   * @see SiteDeployer
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException
   */
  @Override
  protected void service(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
    boolean isMultipart = ServletFileUpload.isMultipartContent(new ServletRequestContext(request));
    if (isMultipart) {
      logger.debug("Request " + request.getQueryString() + " path "  +request.getPathInfo());
      SiteDeployer siteDeployer = new SiteDeployer();
      SiteDeployer.SiteInfo siteInfo = siteDeployer.getSite(request, configDir, db, pathHelper.getInstallPath());
      Site site = siteInfo.site;
      if (site == null) {
        super.service(request, response);
        return;
      }

      if (siteInfo.isUpdate) {
        response.setContentType("text/plain");
        response.getWriter().write("Spreadsheet updated.");
      } else {
        siteDeployer.doSiteResponse(db, response, siteDeployer, site);
      }
    } else {
      super.service(request, response);
    }
  }

  public Site getSiteByID(long id) {
    return db.getSiteByID(id);
  }

  @Override
  public List<Site> getSites() {
    return db.getDeployedSites();
  }

  /**
   * Copy template to something named by site name
   * copy exercise file from media to site/config/template
   * set fields in config.properties
   *   - apptitle
   *   - release date
   *    - lesson plan file
   *
   *    then copy to install path/../name
   *
   * @see mitll.langtest.client.DataCollectAdmin#makeDataCollectNewSiteForm2
   * @param id
   * @param name
   * @param language
   * @param notes
   * @return
   */
  @Override
  public boolean deploySite(long id, String name, String language, String notes) {
    logger.debug("deploy site id=" +id + " name " + name);
    SiteDeployer siteDeployer = new SiteDeployer();
    return siteDeployer.deploySite(db, getMailSupport(), getThreadLocalRequest(), configDir,
      pathHelper.getInstallPath(), id, name, language, notes);
  }

  /**
   * @see mitll.langtest.client.exercise.ExerciseList#getExercises
   * @param reqID
   * @param userID
   * @return
   */
  public ExerciseListWrapper getExerciseIds(int reqID, long userID) {
    logger.debug("getExerciseIds : getting exercise ids for User id=" + userID + " config " + relativeConfigDir);
    List<Exercise> exercises = getExercises(userID);
    if (serverProps.isGoodwaveMode() && !serverProps.dataCollectMode) {
      exercises = getSortedExercises(exercises);
      if (!exercises.isEmpty()) logger.debug("sorting by id -- first is " + exercises.get(0).getID());
    }
    List<ExerciseShell> ids = getExerciseShells(exercises);
    logMemory();
    return new ExerciseListWrapper(reqID,ids);
  }

  private List<ExerciseShell> getExerciseShells(Collection<Exercise> exercises) {
    List<ExerciseShell> ids = new ArrayList<ExerciseShell>();
    for (Exercise e : exercises) {
      ids.add(e.getShell());
    }
    return ids;
  }

  /**
   * Don't randomize order if we're in netProF (formerly goodwave) mode.
   *
   * @see mitll.langtest.client.exercise.SectionExerciseList#loadExercises
   * @param reqID
   * @param typeToSection
   * @param userID
   * @return
   */
  @Override
  public ExerciseListWrapper getExercisesForSelectionState(int reqID, Map<String, Collection<String>> typeToSection, long userID) {
    logger.debug("getExercisesForSelectionState req " + reqID+ " for " + typeToSection + " and " +userID);
    Collection<Exercise> exercisesForSection = db.getSectionHelper().getExercisesForSelectionState(typeToSection);
    if (serverProps.sortExercises() || serverProps.isGoodwaveMode() || serverProps.isFlashcardTeacherView()) {
      logger.debug("\tsorting");

      List<Exercise> copy = getSortedExercises(exercisesForSection);
      return new ExerciseListWrapper(reqID, getExerciseShells(copy));
    } else {
      logger.debug("\t *not* sorting");

      List<Exercise> exercisesBiasTowardsUnanswered = db.getExercisesBiasTowardsUnanswered(userID, exercisesForSection, serverProps.shouldUseWeights());
      return new ExerciseListWrapper(reqID, getExerciseShells(exercisesBiasTowardsUnanswered));
    }
  }

  /**
   * @see #getExerciseIds(int, long)
   * @see #getExercisesForSelectionState(int, java.util.Map, long)
   * @see #getFullExercisesForSelectionState(java.util.Map, int, int)
   * @param exercisesForSection
   * @return
   */
  private List<Exercise> getSortedExercises(Collection<Exercise> exercisesForSection) {
    List<Exercise> copy = new ArrayList<Exercise>(exercisesForSection);
    if (serverProps.sortExercisesByID()) {
      sortByID(copy);
    }
    else {
      sortByTooltip(copy);
    }
    return copy;
  }

  private void sortByID(List<Exercise> copy) {
    boolean allInt = true;
    int size = Math.min(5, copy.size());
    for (int i = 0; i < size && allInt; i++) {
      try {
        Integer.parseInt(copy.get(i).getID());
      } catch (NumberFormatException e) {
        allInt = false;
      }
    }

    if (allInt) {
      try {
        Collections.sort(copy, new Comparator<ExerciseShell>() {
          @Override
          public int compare(ExerciseShell o1, ExerciseShell o2) {
            Integer first = Integer.parseInt(o1.getID());
            return first.compareTo(Integer.parseInt(o2.getID()));
          }
        });
      } catch (Exception e) {
        sortByIDStrings(copy);
      }
    } else {
      sortByIDStrings(copy);
    }
  }

  private void sortByIDStrings(List<Exercise> copy) {
    Collections.sort(copy, new Comparator<ExerciseShell>() {
      @Override
      public int compare(ExerciseShell o1, ExerciseShell o2) {
        String id1 = o1.getID();
        String id2 = o2.getID();
        return id1.toLowerCase().compareTo(id2.toLowerCase());
      }
    });
  }

  /**
   * I.e. by the lexicographic order of the displayed words in the word list
   * @param exerciseShells
   */
  private void sortByTooltip(List<? extends ExerciseShell> exerciseShells) {
    Collections.sort(exerciseShells, new Comparator<ExerciseShell>() {
      @Override
      public int compare(ExerciseShell o1, ExerciseShell o2) {
        String id1 = o1.getTooltip();
        String id2 = o2.getTooltip();
        return id1.toLowerCase().compareTo(id2.toLowerCase());
      }
    });
  }

  /**
   * @see mitll.langtest.client.bootstrap.TableSectionExerciseList#createProvider(java.util.Map, int, com.github.gwtbootstrap.client.ui.CellTable)
   * @param typeToSection
   * @param start
   * @param end
   * @return
   */
  @Override
  public List<Exercise> getFullExercisesForSelectionState(Map<String, Collection<String>> typeToSection, int start, int end) {
    try {
      List<Exercise> exercises;
      //logger.debug("getFullExercisesForSelectionState req = " + typeToSection);

      if (typeToSection.isEmpty()) {
       // logger.debug("getFullExercisesForSelectionState empty type->section");
        exercises = getSortedExercises(db.getExercises());
        logger.debug("getFullExercisesForSelectionState exercises "+ exercises.size() + " start " + start + " end " + end);
      } else {
        Collection<Exercise> exercisesForSection = db.getSectionHelper().getExercisesForSelectionState(typeToSection);
        exercises = getSortedExercises(exercisesForSection);
        logger.debug("getFullExercisesForSelectionState non-empty type->section "+ typeToSection+
          " start " + start + " end " + end + " yields "+ exercises.size());
      }
      List<Exercise> resultList = exercises.subList(start, end);

      return new ArrayList<Exercise>(resultList);
    } catch (Exception e) {
      logger.error("Got "+e, e);
    }
    return Collections.emptyList();
  }

  @Override
  public int getNumExercisesForSelectionState(Map<String, Collection<String>> typeToSection) {
    if (typeToSection.isEmpty()) {
      int size = db.getExercises().size();
      logger.debug("getNumExercisesForSelectionState num = " + size);
      return size;
    } else {
      logger.debug("getNumExercisesForSelectionState req = " + typeToSection);

      Collection<Exercise> exercisesForSection = db.getSectionHelper().getExercisesForSelectionState(typeToSection);
      int size = exercisesForSection.size();
      logger.debug("getNumExercisesForSelectionState req = " + typeToSection + " = " + size);
      return size;
    }
  }

  @Override
  public Collection<String> getTypeOrder() {
    SectionHelper sectionHelper = db.getSectionHelper();
    if (sectionHelper == null) logger.warn("no section helper for " + db);
    List<String> objects = Collections.emptyList();
    return (sectionHelper == null) ? objects : sectionHelper.getTypeOrder();
  }

  @Override
  public List<SectionNode> getSectionNodes() {
    return db.getSectionHelper().getSectionNodes();
  }

  @Override
  public Map<String, Map<String,Integer>> getTypeToSectionToCount() {
    return db.getSectionHelper().getTypeToSectionToCount();
  }

  private void logMemory() {
    Runtime rt = Runtime.getRuntime();
    long free = rt.freeMemory();
    long used = rt.totalMemory() - free;
    long max = rt.maxMemory();
    logger.debug("heap info free " + free / MB + "M used " + used / MB + "M max " + max / MB + "M");
  }

  /**
   * @see mitll.langtest.client.exercise.ExerciseList#getExercisesInOrder()
   * @return
   * @param reqID
   */
  public ExerciseListWrapper getExerciseIds(int reqID) {
    List<Exercise> exercises = getExercises();
    return new ExerciseListWrapper(reqID, getExerciseShells(exercises));
  }

  public Exercise getExercise(String id) {
    long then = System.currentTimeMillis();
    List<Exercise> exercises = getExercises();
    Exercise byID = db.getExercise(id);
    if (byID == null) {
      logger.error("huh? couldn't find exercise with id " + id + " when examining " + exercises.size() + " items");
    }
    long now = System.currentTimeMillis();
    if (now - then > 50) {
      logger.debug("getExercise : took " + (now - then) + " millis to find " + id);
    }
    return byID;
  }

  /**
   * Called from the client.<br></br>
   * Complicated? Sure.
   * <ul>
   * <li>If in data collection mode, we have the option of biasing collection towards
   * items that have not yet been answered. </li>
   * <li>And for those, we can merge the counts from another collection system
   * and bias the presentation order to try to get answers for items that have least coverage in both systems. </li>
   * <li>Alternatively, we can present the first N items in order then random after that (based on the user). </li>
   * <li>Or we can attempt to present items in an order that biases towards presenting items that were all graded "correct"  </li>
   * first and all graded "incorrect" last.
   * <li>if in crt data collect mode, we randomly choose between english or fl question, and if fl question, spoken or written response </li>
   * </ul>
   *
   *
   * @param userID
   * @return exercises for user id
   * @see mitll.langtest.client.exercise.ExerciseList#getExercises(long)
   */
  private List<Exercise> getExercises(long userID) {
    makeAutoCRT();

    List<Exercise> exercises = getExercisesInModeDependentOrder(userID);

    //logger.debug("isCRTDataCollect is " +serverProps.isCRTDataCollect());

    if (serverProps.isCRTDataCollect()) {
      //logger.debug("isCRTDataCollect is true");

      setPromptAndRecordOnExercises(userID, exercises);
    }
    //if (!exercises.isEmpty())
   //   logger.debug("for user #" + userID +" got " + exercises.size() + " exercises , first " + exercises.iterator().next());
          //" ref sentence = '" + exercises.iterator().next().getRefSentence() + "'");

    return exercises;
  }

  private List<Exercise> getExercisesInModeDependentOrder(long userID) {
    List<Exercise> exercises;
    if (serverProps.dataCollectMode) {
       //logger.debug("in data collect mode");
      if (serverProps.biasTowardsUnanswered) {
        //logger.debug("in biasTowardsUnanswered mode : user " +userID);

        if (serverProps.useOutsideResultCounts) {
          String outsideFileOverride = serverProps.outsideFile;
          String lessonPlanFile = getLessonPlan();

          if (lessonPlanFile.contains("farsi")) outsideFileOverride = configDir + File.separator + "farsi.txt";
          else if (lessonPlanFile.contains("urdu")) outsideFileOverride = configDir + File.separator + "urdu.txt";
          else if (lessonPlanFile.contains("sudanese"))
            outsideFileOverride = configDir + File.separator + "sudanese.txt";
          exercises = db.getExercisesBiasTowardsUnanswered(userID, outsideFileOverride, serverProps.shouldUseWeights());
          db.setOutsideFile(outsideFileOverride);
        } else {
          exercises = db.getExercisesBiasTowardsUnanswered(userID,serverProps.shouldUseWeights());
        }
      } else {
        exercises = db.getExercisesFirstNInOrder(userID, serverProps.firstNInOrder);
      }
    } else {
      //if (!serverProps.isArabicTextDataCollect()) logger.debug("*not* in data collect mode");

      exercises = serverProps.isArabicTextDataCollect() ? db.getRandomBalancedList(userID) : db.getUnmodExercises();
    }
    return exercises;
  }

  /**
   * Set the prompt in english/foreign language and text/audio answer bits.
   * <br></br>
   * Note there is no english + text response combination.
   *
   * set the text only flag if not collecting audio
   * <p></p>
   * Uses the user id as a random seed so we get repeatable behavior per user.
   *
   * @see #getExercises(long)
   * @param userID for this user
   * @param exercises to alter
   */
  private void setPromptAndRecordOnExercises(long userID, List<Exercise> exercises) {
    Random rand = new Random(userID);
    //logger.debug("setPromptAndRecordOnExercises for " + userID + " collect audio " + serverProps.isCollectOnlyAudio());
    for (Exercise e : exercises) {
      if (serverProps.isCollectOnlyAudio()) {
        e.setRecordAnswer(true);
        e.setPromptInEnglish(rand.nextBoolean());
      } else if (!serverProps.isCollectAudio()) {
        e.setTextOnly();
      } else {
        boolean inEnglish = rand.nextBoolean();
        e.setPromptInEnglish(inEnglish);
        e.setRecordAnswer(inEnglish || rand.nextBoolean());
      }
    }
  }

  /**
   * @see mitll.langtest.client.bootstrap.BootstrapFlashcardExerciseList#getExercises(long)
   * @param userID
   * @param typeToSection
   * @return
   */
  @Override
  public FlashcardResponse getNextExercise(long userID, Map<String, Collection<String>> typeToSection) {
    Collection<Exercise> exercisesForSection = db.getSectionHelper().getExercisesForSelectionState(typeToSection);
    //logger.debug("getNextExercise : req " + typeToSection + " yields " + exercisesForSection.size() + " exercises.");
    List<Exercise> copy = new ArrayList<Exercise>(exercisesForSection);
    if (serverProps.sortExercisesByID()) {
      sortByID(copy);
    }
    FlashcardResponse nextExercise = db.getNextExercise(copy,userID, serverProps.isTimedGame(), typeToSection);
    //logger.debug("\tnextExercise " + nextExercise);

    return getFlashcardResponse(userID, nextExercise);
  }

  /**
   * @see mitll.langtest.client.bootstrap.BootstrapFlashcardExerciseList#getExercises(long)
   * @param userID
   * @return
   */
  @Override
  public FlashcardResponse getNextExercise(long userID) {
    FlashcardResponse nextExercise = db.getNextExercise(userID, serverProps.isTimedGame());
    return getFlashcardResponse(userID, nextExercise);
  }

  private FlashcardResponse getFlashcardResponse(long userID, FlashcardResponse nextExercise) {
    if (nextExercise == null || nextExercise.getNextExercise() == null) {
      logger.error("huh? no next exercise for user " +userID);
      return nextExercise;
    }
    String refAudio = nextExercise.getNextExercise().getRefAudio();
    if (refAudio != null && refAudio.length() > 0) {
      getWavAudioFile(refAudio);
      ensureMP3(refAudio);
    }
    return nextExercise;
  }

  @Override
  public void resetUserState(long userID) {
    db.resetUserState(userID);
  }

  @Override
  public void clearUserState(long userID) {
    db.clearUserState(userID);
  }

  /**
   * Called from the client.
   * @see mitll.langtest.client.exercise.ExerciseList#getExercisesInOrder()
   * @return
   */
  List<Exercise> getExercises() {
    return db.getExercises();
  }

  /**
   * Remember who is grading which exercise.  Time out reservation after 30 minutes.
   *
   * @see mitll.langtest.client.grading.GradedExerciseList#getNextUngraded
   * @param user
   * @param expectedGrades
   * @param englishOnly
   * @return next ungraded exercise
   */
  public Exercise getNextUngradedExercise(String user, int expectedGrades, boolean englishOnly) {
    synchronized (this) {
      ConcurrentMap<String,String> stringStringConcurrentMap = userToExerciseID.asMap();
      Collection<String> values = stringStringConcurrentMap.values();
      String currentExerciseForUser = userToExerciseID.getIfPresent(user);
      logger.debug("getNextUngradedExercise for " + user + " current " + currentExerciseForUser + " expected " + expectedGrades);

      Collection<String> currentActiveExercises = new HashSet<String>(values);

      if (currentExerciseForUser != null) {
        currentActiveExercises.remove(currentExerciseForUser); // it's OK to include the one the user is working on now...
      }
      //logger.debug("getNextUngradedExercise current set minus " + user + " is " + currentActiveExercises);
      boolean filterForArabicTextOnly = serverProps.isArabicTextDataCollect();
      return db.getNextUngradedExercise(currentActiveExercises, expectedGrades,
          filterForArabicTextOnly, filterForArabicTextOnly, !filterForArabicTextOnly, englishOnly);
    }
  }

  public void checkoutExerciseID(String user, String id) {
    synchronized (this) {
      userToExerciseID.put(user, id);
      logger.debug("checkoutExerciseID : after adding " + user + "->" + id +
          " active exercise map now " + userToExerciseID.asMap());
    }
  }

  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#ensureMP3(mitll.langtest.shared.Exercise, String, com.google.gwt.user.client.ui.VerticalPanel)
   *
   * @param wavFile
   */
  public void ensureMP3(String wavFile) {
    if (wavFile == null) {
      logger.warn("ensureMP3 huh? wavFile is null?");
    } else {
      new AudioConversion().ensureWriteMP3(wavFile, pathHelper.getInstallPath());
    }
  }

  /**
   * Get an image of desired dimensions for the audio file - only for Waveform and spectrogram.
   * Also returns the audio file duration -- so we can deal with the difference in length between mp3 and wav
   * versions of the same audio file.  (The browser soundmanager plays mp3 and reports audio offsets into
   * the mp3 file, but all the images are generated from the shorter wav file.)
   *
   * TODO : Worrying about absolute vs relative path is maddening.  Must be a better way!
   *
   * @see mitll.langtest.client.scoring.AudioPanel#getImageURLForAudio
   * @param reqid
   * @param audioFile
   * @param imageType
   * @param width
   * @param height
   * @return path to an image file
   * */
  public ImageResponse getImageForAudioFile(int reqid, String audioFile, String imageType, int width, int height) {
    ImageWriter imageWriter = new ImageWriter();

    String wavAudioFile = getWavAudioFile(audioFile);
    if (!new File(wavAudioFile).exists()) {
      return new ImageResponse();
    }
    ImageType imageType1 =
        imageType.equalsIgnoreCase(ImageType.WAVEFORM.toString()) ? ImageType.WAVEFORM :
            imageType.equalsIgnoreCase(ImageType.SPECTROGRAM.toString()) ? ImageType.SPECTROGRAM : null;
    if (imageType1 == null) return new ImageResponse(); // success = false!
    String imageOutDir = pathHelper.getImageOutDir();
    logger.debug("getting images for " + wavAudioFile + "");
    String absolutePathToImage = imageWriter.writeImageSimple(wavAudioFile, pathHelper.getAbsoluteFile(imageOutDir).getAbsolutePath(),
        width, height, imageType1);
    String installPath = pathHelper.getInstallPath();
    //System.out.println("Absolute path to image is " + absolutePathToImage);

    String relativeImagePath = absolutePathToImage;
    if (absolutePathToImage.startsWith(installPath)) {
      relativeImagePath = absolutePathToImage.substring(installPath.length());
    }
    else {
      logger.error("huh? file path " + absolutePathToImage + " doesn't start with " + installPath + "?");
    }

    relativeImagePath = pathHelper.ensureForwardSlashes(relativeImagePath);
    if (relativeImagePath.startsWith("/")) {
      relativeImagePath = relativeImagePath.substring(1);
    }
    String imageURL = relativeImagePath;
    double duration = new AudioCheck().getDurationInSeconds(wavAudioFile);
/*    logger.debug("for " + wavAudioFile + " type " + imageType + " rel path is " + relativeImagePath +
        " url " + imageURL + " duration " + duration);*/

    return new ImageResponse(reqid,imageURL, duration);
  }

  private String getWavAudioFile(String audioFile) {
    if (audioFile.endsWith(".mp3")) {
      String wavFile = removeSuffix(audioFile) +".wav";
      File test = pathHelper.getAbsoluteFile(wavFile);
      audioFile = test.exists() ? test.getAbsolutePath() : audioFileHelper.getWavForMP3(audioFile);
    }

    return audioFile;
  }

  private String removeSuffix(String audioFile) {
    return audioFile.substring(0, audioFile.length() - ".mp3".length());
  }

  /**
   * Get properties (first time called read properties file -- e.g. see war/config/levantine/config.properties).
   * @return
   */
 public Map<String, String> getProperties() { return serverProps.getProperties();  }

  /**
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#scoreAudio(String, String, String, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, int, int, int)
   * @param reqid
   * @param testAudioFile
   * @param sentence
   * @param width
   * @param height
   * @param useScoreToColorBkg
   * @return
   */
  public PretestScore getASRScoreForAudio(int reqid, String testAudioFile, String sentence,
                                          int width, int height, boolean useScoreToColorBkg) {
      return audioFileHelper.getASRScoreForAudio(reqid, testAudioFile, sentence, width, height, useScoreToColorBkg,
        false, Files.createTempDir().getAbsolutePath(), serverProps.useScoreCache());
  }

  /**
   * Get score when doing autoCRT on an audio file.
   * @see mitll.langtest.server.autocrt.AutoCRT#getAutoCRTDecodeOutput
   * @see mitll.langtest.server.autocrt.AutoCRT#getFlashcardAnswer
   * @param testAudioFile audio file to score
   * @param lmSentences to look for in the audio
   * @return PretestScore for audio
   */
  public PretestScore getASRScoreForAudio(File testAudioFile, Collection<String> lmSentences) {
    return audioFileHelper.getASRScoreForAudio(testAudioFile, lmSentences);
  }

  /**
   * @see mitll.langtest.server.autocrt.AutoCRT#getAutoCRTDecodeOutput(String, int, mitll.langtest.shared.Exercise, java.io.File, mitll.langtest.shared.AudioAnswer)
   * @param phrases
   * @return
   */
  @Override
  public Collection<String> getValidPhrases(Collection<String> phrases) {
    return audioFileHelper.getValidPhrases(phrases);
  }

  /**
   *
   * @param exid
   * @param arabicTextDataCollect
   * @return
   * @see mitll.langtest.client.grading.GradingExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   */
  public ResultsAndGrades getResultsForExercise(String exid, boolean arabicTextDataCollect) {
    ResultsAndGrades resultsForExercise =
        arabicTextDataCollect ?
            db.getResultsForExercise(exid, true, true, false) :
            db.getResultsForExercise(exid);

    ensureMP3(resultsForExercise.results);
    return resultsForExercise;
  }

  /**
   * Make sure we have mp3 files in results.
   * @param results
   */
  private void ensureMP3(Collection<Result> results) {
    for (Result r : results) {
      if (r.answer.endsWith(".wav")) {
        new AudioConversion().ensureWriteMP3(r.answer, pathHelper.getInstallPath());
      }
    }
  }

  // Answers ---------------------

  /**
   *
   *
   * @param userID
   * @param exercise
   * @param questionID
   * @param answer
   * @see mitll.langtest.client.exercise.PostAnswerProvider#postAnswers
   */
  public void addTextAnswer(int userID, Exercise exercise, int questionID, String answer) {
    db.addAnswer(userID, exercise, questionID, answer);
  }

  public double getScoreForAnswer(Exercise e, int questionID, String answer) {
    return audioFileHelper.getScoreForAnswer(e, questionID, answer);
  }

  // Grades ---------------------

  /**
   * @see mitll.langtest.client.grading.GradingResultManager#addGrade
   * @param exerciseID
   * @return
   */
  public CountAndGradeID addGrade(String exerciseID, Grade toAdd) {
    return db.addGrade(exerciseID, toAdd);
  }

  /**
   * @see mitll.langtest.client.grading.GradingResultManager#changeGrade(mitll.langtest.shared.grade.Grade)
   * @param toChange
   */
  public void changeGrade(Grade toChange) {
    db.changeGrade(toChange);
  }

  @Override
  public synchronized int userExists(String login) {
    if (db == null) getProperties();
    return db.userExists(login);
  }

  // Users ---------------------

  /**
   * @see mitll.langtest.client.user.UserManager#displayLoginBox()
   * @param age
   * @param gender
   * @param experience
   * @param dialect
   * @return user id
   */
  public long addUser(int age, String gender, int experience, String dialect) {
    return db.addUser( getThreadLocalRequest(), age, gender, experience, dialect);
  }

  /**
   *
   * @param age
   * @param gender
   * @param experience
   * @param firstName
   * @param lastName
   * @param nativeLang
   * @param dialect
   * @param userID
   * @return
   */
  @Override
  public long addUser(int age, String gender, int experience,
                      String firstName, String lastName, String nativeLang, String dialect, String userID) {
    logger.info("Adding user " + userID + " " + firstName + " " + lastName);
    long l = db.addUser(getThreadLocalRequest(),age, gender, experience, firstName, lastName, nativeLang, dialect, userID);

    if (l != 0 && serverProps.isDataCollectAdminView) {
      new SiteDeployer().sendNewUserEmail(getMailSupport(), getThreadLocalRequest(), userID);
    }

    return l;
  }

  public List<User> getUsers() {
    return db.getUsers();
  }

  @Override
  public boolean isAdminUser(long id) { return db.isAdminUser(id); }
  @Override
  public boolean isEnabledUser(long id) { return db.isEnabledUser(id); }
  @Override
  public void setUserEnabled(long id, boolean enabled) { db.setUserEnabled(id, enabled); }

  // Results ---------------------

  /**
   * @return
   * @see mitll.langtest.client.result.ResultManager#showResults()
   */
  @Override
  public List<Result> getResults(int start, int end) {
    List<Result> results = db.getResultsWithGrades();
    Collections.sort(results, new Comparator<Result>() {
      @Override
      public int compare(Result o1, Result o2) {
        return o1.uniqueID < o2.uniqueID ? -1 : o1.uniqueID > o2.uniqueID ? +1 : 0;
      }
    });
    List<Result> resultList = results.subList(start, end);
    List<Result> copy = new ArrayList<Result>(resultList);
    return copy;
  }

  /**
   * @see mitll.langtest.client.result.ResultManager#showResults()
   * @return
   */
  @Override
  public int getNumResults() { return db.getNumResults(); }

  /**
   * Record an answer entry in the database.<br></br>
   * Write the posted data to a wav and an mp3 file (since all the browser audio works with mp3).
   *
   * Client references:
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#stopRecording()
   * @see mitll.langtest.client.recorder.RecordButtonPanel#stopRecording()
   * @param base64EncodedString generated by flash on the client
   * @param plan which set of exercises
   * @param exercise exercise within the plan
   * @param questionID question within the exercise
   * @param user answering the question
   * @param reqid request id from the client, so it can potentially throw away out of order responses
   * @param flq was the prompt a foreign language query
   * @param audioType regular or fast then slow audio recording
   * @param doFlashcard
   * @return URL to audio on server and if audio is valid (not too short, etc.)
   */

  public AudioAnswer writeAudioFile(String base64EncodedString, String plan, String exercise, int questionID,
                                    int user, int reqid, boolean flq, String audioType, boolean doFlashcard) {
    makeAutoCRT();
    return this.audioFileHelper.writeAudioFile(base64EncodedString, plan, exercise, questionID, user, reqid, flq, audioType, doFlashcard, this);
  }

  void makeAutoCRT() {
    audioFileHelper.makeAutoCRT(relativeConfigDir, this, studentAnswersDB, this);
  }

  @Override
  public Map<User, Integer> getUserToResultCount() {
    return db.getUserToResultCount();
  }

  @Override
  public Map<Integer, Integer> getResultCountToCount() {
    return db.getResultCountToCount();
  }
  @Override
  public Map<String, Integer> getResultByDay() {
    return db.getResultByDay();
  }
  @Override
  public Map<String, Integer> getResultByHourOfDay() {
    return db.getResultByHourOfDay();
  }

  /**
   * Map of overall, male, female to list of counts (ex 0 had 7, ex 1, had 5, etc.)
   * @see mitll.langtest.client.monitoring.MonitoringManager#doResultLineQuery(com.google.gwt.user.client.ui.Panel)
   * @return
   */
  public Map<String, Map<String, Integer>> getResultPerExercise() {
    return db.getResultPerExercise();
  }

  /**
   * @see mitll.langtest.client.monitoring.MonitoringManager#doGenderQuery(com.google.gwt.user.client.ui.Panel)
   * @return
   */
  @Override
  public Map<String, Map<Integer, Integer>> getResultCountsByGender() {
    return db.getResultCountsByGender();
  }

  public Map<String, Map<Integer, Map<Integer, Integer>>> getDesiredCounts() {
    return db.getDesiredCounts();
  }

  public List<Session> getSessions() {
    return db.getSessions();
  }

  /**
   * @see mitll.langtest.client.monitoring.MonitoringManager#doSessionQuery(com.google.gwt.user.client.ui.Panel)
   * @return
   */
  public Map<String,Number> getResultStats() {
    return db.getResultStats();
  }

  @Override
  public Map<Integer, Map<String, Map<String, Integer>>> getGradeCountPerExercise() {
    return db.getGradeCountPerExercise();
  }

  private final Leaderboard leaderboard = new Leaderboard();

 // @Override
  //public Leaderboard getLeaderboard(Map<String, Collection<String>> typeToSection) {  return leaderboard;  }

  @Override
  public Leaderboard postTimesUp(long userid, long timeTaken, Map<String, Collection<String>> selectionState) {
    synchronized (leaderboard) {
      ScoreInfo scoreInfo = db.getScoreInfo(userid, timeTaken, selectionState);
      leaderboard.addScore(scoreInfo);
    }
    return leaderboard;
  }

  @Override
  public void userOnline(long userid, boolean isOnline) {  db.userOnline(userid, isOnline);  }

  /**
   * @see mitll.langtest.client.taboo.Taboo#checkForPartner
   * @param userid
   * @return
   */
  @Override
  public TabooState anyUsersAvailable(long userid) {  return db.getOnlineUsers().anyAvailable(userid);  }

  /**
   * @see mitll.langtest.client.taboo.Taboo
   * @param userid
   * @param isGiver
   */
  @Override
  public void registerPair(long userid, boolean isGiver) {  db.getOnlineUsers().registerPair(userid, isGiver);  }

  /**
   * @see mitll.langtest.client.taboo.GiverExerciseFactory.GiverPanel#sendStimulus
   * @param userid
   * @param exerciseID
   * @param stimulus
   * @param answers
   * @param onLastStimulus
   * @param skippedItem
   * @param numClues
   * @param isGameOver
   */
  @Override
  public int sendStimulus(long userid, String exerciseID, String stimulus, String answer, boolean onLastStimulus, boolean skippedItem, int numClues, boolean isGameOver) {
   // db.sendStimulus(userid, exerciseID, stimulus, answer);
    return db.getOnlineUsers().sendStimulus(userid, exerciseID, stimulus, answer, onLastStimulus, skippedItem, numClues, isGameOver);
  }

  /**
   * @see mitll.langtest.client.taboo.Taboo#pollForPartnerOnline
   * @param userid
   * @param isGiver
   * @return
   */
  @Override
  public PartnerState isPartnerOnline(long userid, boolean isGiver) { return db.getOnlineUsers().isPartnerOnline(userid,isGiver); }

  /**
   * @see mitll.langtest.client.taboo.TabooExerciseList#tellPartnerMyChapterSelection(mitll.langtest.client.exercise.SelectionState)
   * @param giver
   * @param selectionState
   */
  @Override
  public void registerSelectionState(long giver, Map<String, Collection<String>> selectionState) {
    Collection<Exercise> exercisesForSection = (selectionState.isEmpty()) ? getExercises(giver) : db.getSectionHelper().getExercisesForSelectionState(selectionState);
    List<ExerciseShell> exerciseShells = getExerciseShells(exercisesForSection);
    Collections.shuffle(exerciseShells);    // randomize order
    db.getOnlineUsers().registerSelectionState(giver, selectionState, exerciseShells);
  }

  /**
   * @see mitll.langtest.client.taboo.ReceiverExerciseFactory.ReceiverPanel#checkForStimulus(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.taboo.ReceiverExerciseFactory.ReceiverPanel)
   * @param userid
   * @return
   */
  @Override
  public StimulusAnswerPair checkForStimulus(long userid) {
    return db.getOnlineUsers().checkForStimulus(userid);
  }

  /**
   * @see mitll.langtest.client.taboo.ReceiverExerciseFactory.ReceiverPanel#registerAnswer(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.taboo.ReceiverExerciseFactory.ReceiverPanel, boolean, boolean)
   * @param userid
   * @param exerciseID
   * @param stimulus
   * @param answer
   * @param isCorrect
   */
  @Override
  public void registerAnswer(long userid, String exerciseID, String stimulus, String answer, boolean isCorrect) {
    db.registerAnswer(userid, exerciseID, stimulus, answer, isCorrect);
  }

  /**
   * @see mitll.langtest.client.taboo.GiverExerciseFactory.GiverPanel#checkForCorrect(long, String, mitll.langtest.shared.Exercise, String, mitll.langtest.client.sound.SoundFeedback)
   * @param giverUserID
   * @param stimulus
   * @return
   */
  @Override
  public int checkCorrect(long giverUserID, String stimulus) {
    return db.getOnlineUsers().checkCorrect(giverUserID, "", stimulus);
  }

  /**
   * @see mitll.langtest.client.taboo.ReceiverExerciseFactory#startGame()
   * @param userID
   * @return
   */
  public GameInfo startGame(long userID) {
    return db.getOnlineUsers().startGame(userID);
  }

  /**
   *
   * @param userID
   * @param isGiver
   * @return
   */
  public GameInfo getGame(long userID, boolean isGiver) {
    return db.getOnlineUsers().getGame(userID, isGiver);
  }

  public void logMessage(String message) {
    String prefixedMessage = "for " + pathHelper.getInstallPath() + " from client " + message;
    logger.debug(prefixedMessage);

    if (message.startsWith("got browser exception")) {
      getMailSupport().email("Javascript Exception", prefixedMessage);
    }
  }

  /**
   * @see mitll.langtest.client.mail.MailDialog.SendClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
   * @param userID
   * @param to
   * @param replyTo
   * @param subject
   * @param message
   * @param token
   */
  @Override
  public void sendEmail(int userID, String to, String replyTo, String subject, String message, String token) {
    HttpServletRequest threadLocalRequest = getThreadLocalRequest();
    getMailSupport().sendEmail(threadLocalRequest.getServerName(), new SiteDeployer().getBaseUrl(threadLocalRequest),
      to, replyTo, subject, message, token);
  }

  private MailSupport getMailSupport() {
    return new MailSupport(serverProps.isDebugEMail());
  }

  @Override
  public void destroy() {
    super.destroy();
    db.destroy();
    if (studentAnswersDB != null) {
      studentAnswersDB.destroy();
    }
  }

  @Override
  public void init() {
    this.pathHelper = new PathHelper(getServletContext());
    readProperties(getServletContext());
    setInstallPath(serverProps.getUseFile(), db);
    audioFileHelper = new AudioFileHelper(pathHelper, serverProps, db);
    new RecoTest(this,serverProps,pathHelper,audioFileHelper);
  }

  /**
   * @see AudioFileHelper#makeAutoCRT(String, mitll.langtest.server.scoring.AutoCRTScoring, mitll.langtest.server.database.DatabaseImpl, LangTestDatabaseImpl)
   * @param useFile
   * @param db
   * @return
   */
  public String setInstallPath(boolean useFile, DatabaseImpl db) {
    String lessonPlanFile = getLessonPlan();
    if (useFile && !new File(lessonPlanFile).exists()) logger.error("couldn't find lesson plan file " + lessonPlanFile);

    db.setInstallPath(pathHelper.getInstallPath(), lessonPlanFile, serverProps.getLanguage(), useFile,
      relativeConfigDir+File.separator+serverProps.getMediaDir(), serverProps.isRTL());

    return lessonPlanFile;
  }

  private String getLessonPlan() {
    return configDir + File.separator + serverProps.getLessonPlan();
  }

  /**
   * The config web.xml file.
   * As a final step, creates the DatabaseImpl!<br></br>
   *
   * Note that this will only ever be called once.
   * @see #init()
   * @param servletContext
   */
  private void readProperties(ServletContext servletContext) {
    String config = servletContext.getInitParameter("config");
    this.relativeConfigDir = "config" + File.separator + config;
    this.configDir = pathHelper.getInstallPath() + File.separator + relativeConfigDir;

    serverProps.readPropertiesFile(servletContext, configDir);
    String h2DatabaseFile = serverProps.getH2Database();

    db = makeDatabaseImpl(h2DatabaseFile);
    if (serverProps.isAutoCRT()) {
      studentAnswersDB = makeDatabaseImpl(serverProps.getH2StudentAnswersDatabase());
    }
  }

  private DatabaseImpl makeDatabaseImpl(String h2DatabaseFile) {
    logger.debug("word pairs " +  serverProps.isWordPairs() + " language " + serverProps.getLanguage() + " config dir " + relativeConfigDir);
    return new DatabaseImpl(configDir, h2DatabaseFile, relativeConfigDir,  serverProps);
  }
}
