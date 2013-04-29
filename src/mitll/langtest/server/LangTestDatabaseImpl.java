package mitll.langtest.server;

import audio.image.ImageType;
import audio.imagewriter.ImageWriter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Files;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.server.scoring.ASRScoring;
import mitll.langtest.server.scoring.AutoCRTScoring;
import mitll.langtest.server.scoring.DTWScoring;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.CountAndGradeID;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.FlashcardResponse;
import mitll.langtest.shared.Grade;
import mitll.langtest.shared.ImageResponse;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.ResultsAndGrades;
import mitll.langtest.shared.SectionNode;
import mitll.langtest.shared.Session;
import mitll.langtest.shared.Site;
import mitll.langtest.shared.User;
import mitll.langtest.shared.scoring.NetPronImageType;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
  private static Logger logger = Logger.getLogger(LangTestDatabaseImpl.class);

  private static final int MB = (1024 * 1024);
  public static final String ANSWERS = "answers";
  private static final int TIMEOUT = 30;
  private static final String IMAGE_WRITER_IMAGES = "audioimages";
  //private static final List<String> EMPTY_LIST = Collections.emptyList();
  private DatabaseImpl db;
  private ASRScoring asrScoring;
  private AutoCRT autoCRT;
  private final boolean makeFullURLs = false;
  private String relativeConfigDir;
  private String configDir;
  private final ServerProperties serverProps = new ServerProperties();

  private final Cache<String, String> userToExerciseID = CacheBuilder.newBuilder()
      .concurrencyLevel(4)
      .maximumSize(10000)
      .expireAfterWrite(TIMEOUT, TimeUnit.MINUTES).build();
  private boolean testReco = true;

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
      SiteDeployer siteDeployer = new SiteDeployer();
      Site site = siteDeployer.getSite(request, configDir);
      if (site == null) {
        super.service(request, response);
        return;
      }

      siteDeployer.doSiteResponse(db,response, siteDeployer, site);
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
   * @param id
   * @param name
   * @param language
   * @param notes
   * @return
   */
  @Override
  public boolean deploySite(long id, String name, String language, String notes) {
    SiteDeployer siteDeployer = new SiteDeployer();
    return siteDeployer.deploySite(db, getMailSupport(), getThreadLocalRequest(), configDir, getInstallPath(), id, name, language, notes);
  }

  /**
   * @see mitll.langtest.client.exercise.ExerciseList#getExercises
   * @param userID
   * @return
   */
  public List<ExerciseShell> getExerciseIds(long userID) {
    logger.debug("getting exercise ids for User id=" + userID + " config " + relativeConfigDir);
    List<Exercise> exercises = getExercises(userID);
    List<ExerciseShell> ids = getExerciseShells(exercises);
    logMemory();
    return ids;
  }

  private List<ExerciseShell> getExerciseShells(Collection<Exercise> exercises) {
    List<ExerciseShell> ids = new ArrayList<ExerciseShell>();
    for (Exercise e : exercises) {
      ids.add(e.getShell());
    }
    return ids;
  }

/*  @Override
  public Map<String, Collection<String>> getTypeToSection() { return db.getSectionHelper().getTypeToSection(); }*/
/*  @Override
  public List<ExerciseShell> getExercisesForSection(String type, String section, long userID) {
    Collection<Exercise> exercisesForSection = db.getExercisesForSection(type, section);
    List<Exercise> exercisesBiasTowardsUnanswered = db.getExercisesBiasTowardsUnanswered(userID, exercisesForSection);
    return getExerciseShells(exercisesBiasTowardsUnanswered);
  }*/

  @Override
  public List<ExerciseShell> getExercisesForSelectionState(Map<String, Collection<String>> typeToSection, long userID) {
    Collection<Exercise> exercisesForSection = db.getSectionHelper().getExercisesForSelectionState(typeToSection);
    List<Exercise> exercisesBiasTowardsUnanswered = db.getExercisesBiasTowardsUnanswered(userID, exercisesForSection);
    return getExerciseShells(exercisesBiasTowardsUnanswered);
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

        exercises = db.getExercises();
      } else {
        logger.debug("getFullExercisesForSelectionState non-empty type->section "+ typeToSection+ " start " + start + " end " + end);

        Collection<Exercise> exercisesForSection = db.getSectionHelper().getExercisesForSelectionState(typeToSection);
        exercises = new ArrayList<Exercise>(exercisesForSection);
      }
      logger.debug("getFullExercisesForSelectionState exercises "+ exercises.size() + " start " + start + " end " + end);

      List<Exercise> resultList = exercises.subList(start, end);

      logger.debug("getFullExercisesForSelectionState sublist has "+ resultList.size());

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
    return db.getSectionHelper().getTypeOrder();
  }


  @Override
  public List<SectionNode> getSectionNodes() {
    return db.getSectionHelper().getSectionNodes();
  }

  /**
   * @see mitll.langtest.client.exercise.SectionExerciseList#setOtherListBoxes(java.util.Map)
   * @param typeToSection
   * @return
   */
  @Override
  public Map<String, Collection<String>> getTypeToSectionsForTypeAndSection(Map<String, Collection<String>> typeToSection) {
    return db.getSectionHelper().getTypeToSectionsForTypeAndSection(typeToSection);
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
   */
  public List<ExerciseShell> getExerciseIds() {
    List<Exercise> exercises = getExercises();
    return getExerciseShells(exercises);
  }

  public Exercise getExercise(String id) {
    List<Exercise> exercises = getExercises();
    for (Exercise e : exercises) {
      if (id.equals(e.getID())) {
        return e;
      }
    }
    return null;
  }

  public Exercise getExercise(String id, long userID) {
    List<Exercise> exercises = getExercises(userID);
    for (Exercise e : exercises) {
      if (id.equals(e.getID())) {
        logger.info("getExercise for user " +userID + " exid " + id + " got " + e);
        return e;
      }
    }
    return null;
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
  public List<Exercise> getExercises(long userID) {
    String lessonPlanFile = getLessonPlan();

    makeAutoCRT();

    List<Exercise> exercises = getExercisesInModeDependentOrder(userID, lessonPlanFile);

    if (serverProps.isCRTDataCollect()) {
      //logger.debug("isCRTDataCollect is true");

      setPromptAndRecordOnExercises(userID, exercises);
    }
    if (makeFullURLs) convertRefAudioURLs(exercises);
    //if (!exercises.isEmpty())
   //   logger.debug("for user #" + userID +" got " + exercises.size() + " exercises , first " + exercises.iterator().next());
          //" ref sentence = '" + exercises.iterator().next().getRefSentence() + "'");

    return exercises;
  }

  private List<Exercise> getExercisesInModeDependentOrder(long userID, String lessonPlanFile) {
    List<Exercise> exercises;
    if (serverProps.dataCollectMode) {
      // logger.debug("in data collect mode");
      if (serverProps.biasTowardsUnanswered) {
        if (serverProps.useOutsideResultCounts) {
          String outsideFileOverride = serverProps.outsideFile;
          if (lessonPlanFile.contains("farsi")) outsideFileOverride = configDir + File.separator + "farsi.txt";
          else if (lessonPlanFile.contains("urdu")) outsideFileOverride = configDir + File.separator + "urdu.txt";
          else if (lessonPlanFile.contains("sudanese"))
            outsideFileOverride = configDir + File.separator + "sudanese.txt";
          exercises = db.getExercisesBiasTowardsUnanswered(userID, outsideFileOverride);
          db.setOutsideFile(outsideFileOverride);
        } else {
          exercises = db.getExercisesBiasTowardsUnanswered(userID);
        }
      } else {
        exercises = db.getExercisesFirstNInOrder(userID, serverProps.firstNInOrder);
      }
    } else {
      exercises = serverProps.isArabicTextDataCollect() ? db.getRandomBalancedList() : db.getExercises(userID);
    }
    return exercises;
  }

  private void makeAutoCRT() {
    synchronized (this) {
      if (autoCRT == null) {
        String backgroundFile = configDir + File.separator + serverProps.getBackgroundFile();
        autoCRT = new AutoCRT(db.getExport(), this, getInstallPath(), relativeConfigDir, backgroundFile);
      }
    }
  }

  /**
   * Set the prompt in english/foreign language and text/audio answer bits.
   * <br></br>
   * Note there is no english + text response combination.
   *
   * set the text only flag if not collecting audio
   * @param userID
   * @param exercises
   */
  private void setPromptAndRecordOnExercises(long userID, List<Exercise> exercises) {
    Random rand = new Random(userID);
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
   * TODO remember exercise list between invocations ?
   * @param userID
   * @param typeToSection
   * @return
   */
  @Override
  public FlashcardResponse getNextExercise(long userID, Map<String, Collection<String>> typeToSection) {
    Collection<Exercise> exercisesForSection = db.getSectionHelper().getExercisesForSelectionState(typeToSection);
    logger.debug("req " + typeToSection + " yields " + exercisesForSection.size() + " exercises.");

    FlashcardResponse nextExercise = db.getNextExercise(new ArrayList<Exercise>(exercisesForSection),userID, serverProps.isTimedGame());
    logger.debug("\tnextExercise " + nextExercise);

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
  public List<Exercise> getExercises() {
    List<Exercise> exercises = db.getExercises();
    if (makeFullURLs) convertRefAudioURLs(exercises);
    return exercises;
  }

  private void convertRefAudioURLs(List<Exercise> exercises) {
    URLUtils urlUtils = new URLUtils(getThreadLocalRequest());
    for (Exercise e : exercises) {
      if (e.getRefAudio() != null) {
        e.setRefAudio(urlUtils.makeURL(e.getRefAudio()));
      }
    }
  }

  /**
   * Remember who is grading which exercise.  Time out reservation after 30 minutes.
   *
   * @see mitll.langtest.client.exercise.GradedExerciseList#getNextUngraded
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
      //logger.debug("getNextUngradedExercise for " + user + " current " + currentExerciseForUser + " expected " + expectedGrades);

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
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#getQuestionContent(mitll.langtest.shared.Exercise)
   *
   * @param wavFile
   */
  public void ensureMP3(String wavFile) {
    //logger.debug("ensure mp3 for " +wavFile);
    new AudioConversion().ensureWriteMP3(wavFile, getInstallPath());
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

    ImageType imageType1 =
        imageType.equalsIgnoreCase(ImageType.WAVEFORM.toString()) ? ImageType.WAVEFORM :
            imageType.equalsIgnoreCase(ImageType.SPECTROGRAM.toString()) ? ImageType.SPECTROGRAM : null;
    if (imageType1 == null) return new ImageResponse(); // success = false!
    String imageOutDir = getImageOutDir();
    String absolutePathToImage = imageWriter.writeImageSimple(wavAudioFile, getAbsoluteFile(imageOutDir).getAbsolutePath(),
        width, height, imageType1);
    String installPath = getInstallPath();
    //System.out.println("Absolute path to image is " + absolutePathToImage);

    String relativeImagePath = absolutePathToImage;
    if (absolutePathToImage.startsWith(installPath)) {
      relativeImagePath = absolutePathToImage.substring(installPath.length());
    }
    else {
      logger.error("huh? file path " + absolutePathToImage + " doesn't start with " + installPath + "?");
    }

    relativeImagePath = ensureForwardSlashes(relativeImagePath);
    if (relativeImagePath.startsWith("/")) {
      relativeImagePath = relativeImagePath.substring(1);
    }
    String imageURL = optionallyMakeURL(relativeImagePath);
    double duration = new AudioCheck().getDurationInSeconds(wavAudioFile);
    logger.debug("for " + wavAudioFile + " type " + imageType + " rel path is " + relativeImagePath +
        " url " + imageURL + " duration " + duration);

    return new ImageResponse(reqid,imageURL, duration);
  }

  private String getWavAudioFile(String audioFile) {
    if (audioFile.endsWith(".mp3")) {
      String wavFile = removeSuffix(audioFile) +".wav";
      File test = getAbsoluteFile(wavFile);
      audioFile = test.exists() ? test.getAbsolutePath() : getWavForMP3(audioFile);
    }
    else if (makeFullURLs) {
      audioFile = new URLUtils(getThreadLocalRequest()).convertURLToRelativeFile(audioFile);
    }
    return audioFile;
  }

  /**
   * Does DTW Scoring.
   *
   * @see mitll.langtest.client.scoring.DTWScoringPanel#scoreAudio(String, String, String, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, int, int, int)
   * @param reqid
   * @param audioFile
   * @param refs
   * @param width
   * @param height
   * @deprecated use {@link #getASRScoreForAudio}
   * @return
   */
  public PretestScore getScoreForAudioFile(int reqid, String audioFile, Collection<String> refs, int width, int height) {
    logger.info("getASRScoreForAudio " + audioFile + " against " + refs);
    if (refs.isEmpty()) {
      logger.error("getASRScoreForAudio no refs? ");
      PretestScore pretestScore = new PretestScore();
      pretestScore.setReqid(reqid);
      return pretestScore;
    }
    String installPath = getInstallPath();
    AudioConversion audioConversion = new AudioConversion();

    File testAudioFile = audioConversion.getProperAudioFile(audioFile, installPath);

    logger.debug("\tscoring after conversion " + testAudioFile.getAbsolutePath());
    String name = testAudioFile.getName();

    String imageOutDir = getImageOutDir();
    String testAudioDir = testAudioFile.getParent();

    logger.debug("\tscoring " + name + " in dir " +testAudioDir);

    Collection<String> names = new ArrayList<String>();
    String refAudioDir = null;

    for (String ref : refs) {
      File properAudioFile = audioConversion.getProperAudioFile(ref, installPath);
      if (refAudioDir == null) refAudioDir = properAudioFile.getParent();
      names.add(removeSuffix(properAudioFile.getName()));
    }
    //System.out.println("converted refs " + refs +" into " + names);

    if (names.isEmpty()) {
      logger.error("no valid ref files");
      return new PretestScore();
    } else {
      DTWScoring dtwScoring;
      dtwScoring = new DTWScoring(installPath);
      PretestScore pretestScore =
          dtwScoring.score(testAudioDir, removeSuffix(name), refAudioDir, names, imageOutDir, width, height);
      pretestScore.setReqid(reqid);
      //System.out.println("score " + pretestScore);
      return pretestScore;
    }
  }

  /**
   * Get properties (first time called read properties file -- e.g. see war/config/levantine/config.properties).
   * @return
   */
 public Map<String, String> getProperties() { return serverProps.getProperties();  }

  /**
   * @see mitll.langtest.client.scoring.ASRScoringAudioPanel#scoreAudio(String, String, String, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, int, int, int)
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
      return getASRScoreForAudio(reqid, testAudioFile, sentence, width, height, useScoreToColorBkg,
        false, Files.createTempDir().getAbsolutePath(), serverProps.useScoreCache());
  }

  /**
   * Get score when doing autoCRT on an audio file.
   * @see AutoCRT#getAutoCRTDecodeOutput(String, int, mitll.langtest.shared.Exercise, java.io.File, mitll.langtest.shared.AudioAnswer)
   * @see AutoCRT#getFlashcardAnswer
   * @param testAudioFile
   * @param lmSentences
   * @param background
   * @paramx vocab
   * @return PretestScore for audio
   */
  public PretestScore getASRScoreForAudio(File testAudioFile, List<String> lmSentences,
                                          List<String> background) {
    String tmpDir = Files.createTempDir().getAbsolutePath();
    List<String> both = new ArrayList<String>();
    both.addAll(lmSentences);
    String slfFile = createSLFFile(both, tmpDir);
    if (!new File(slfFile).exists()) {
      logger.error("couldn't make slf file?");
      return new PretestScore();
    } else {
      makeASRScoring();
      List<String> unk = new ArrayList<String>();
      unk.add("UNKNOWNMODEL");
      String sentence = asrScoring.getUsedTokens(both, unk);
      return getASRScoreForAudio(0, testAudioFile.getPath(), sentence, 128, 128, false, true, tmpDir, serverProps.useScoreCache());
    }
  }

/*  private String createSLFFile(List<String> lmSentences, List<String> background, String tmpDir) {
    SmallVocabDecoder svDecoderHelper = new SmallVocabDecoder();
    long then = System.currentTimeMillis();
    String slfFile = svDecoderHelper.createSLFFile(lmSentences, background, tmpDir,
      Scoring.getScoringDir(getInstallPath()), serverProps.getForegroundBlend());
    long now = System.currentTimeMillis();
    logger.debug("create slf file took " + (now - then) + " millis");
    return slfFile;
  }*/

  private String createSLFFile(List<String> lmSentences, String tmpDir) {
    SmallVocabDecoder svDecoderHelper = new SmallVocabDecoder();
    long then = System.currentTimeMillis();
    String slfFile = svDecoderHelper.createSimpleSLFFile(lmSentences, tmpDir);
    long now = System.currentTimeMillis();
    //logger.debug("simple create slf file took " + (now - then) + " millis");
    return slfFile;
  }

  /**
   * For now, we don't use a ref audio file, since we aren't comparing against a ref audio file with the DTW/sv pathway.
   *
   * @see #getASRScoreForAudio(int, String, String, int, int, boolean, boolean, String, boolean)
   * @see #getASRScoreForAudio(java.io.File, java.util.List, java.util.List)
   * @see mitll.langtest.client.scoring.ASRScoringAudioPanel#scoreAudio(String, String, String, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, int, int, int)
   * @param reqid
   * @param testAudioFile
   * @param sentence empty string when using lmSentences non empty and vice-versa
   * @param width image dim
   * @param height  image dim
   * @param useScoreToColorBkg
   * @param decode
   * @param tmpDir
   * @param useCache
   * @return PretestScore
   **/
  private PretestScore getASRScoreForAudio(int reqid, String testAudioFile, String sentence,
                                           int width, int height, boolean useScoreToColorBkg,
                                           boolean decode, String tmpDir, boolean useCache) {
    logger.info("getASRScoreForAudio scoring " + testAudioFile + " with " + sentence + " req# " + reqid);

    assert(testAudioFile != null && sentence != null);
    makeASRScoring();
    testAudioFile = dealWithMP3Audio(testAudioFile);

    String installPath = getInstallPath();

    DirAndName testDirAndName = new DirAndName(testAudioFile, installPath).invoke();
    String testAudioName = testDirAndName.getName();
    String testAudioDir = testDirAndName.getDir();

    //logger.debug("getASRScoreForAudio scoring " + testAudioName + " in dir " + testAudioDir);
    PretestScore pretestScore;
    pretestScore = asrScoring.scoreRepeat(
        testAudioDir, removeSuffix(testAudioName),
        sentence,
        getImageOutDir(), width, height, useScoreToColorBkg,decode,tmpDir, useCache);
    pretestScore.setReqid(reqid);

    if (makeFullURLs) {
      URLUtils urlUtils = new URLUtils(getThreadLocalRequest());
      Map<NetPronImageType, String> typeToURL = new HashMap<NetPronImageType, String>();
      for (Map.Entry<NetPronImageType, String> kv : pretestScore.getsTypeToImage().entrySet()) {
        typeToURL.put(kv.getKey(), urlUtils.makeURL(kv.getValue()));
      }
      pretestScore.setsTypeToImage(typeToURL);
    }
    return pretestScore;
  }

  private void makeASRScoring() {
    if (asrScoring == null) {
        asrScoring = new ASRScoring(getInstallPath(), getProperties()); // lazy eval since...?
    }
  }

  /**
   * @see #getScoreForAudioFile
   * @param testAudioFile
   * @return
   */
  private String dealWithMP3Audio(String testAudioFile) {
    if (testAudioFile.endsWith(".mp3")) {
      String noSuffix = removeSuffix(testAudioFile);
      String wavFile = noSuffix +".wav";
      File test = getAbsoluteFile(wavFile);
      if (!test.exists()) {
        logger.warn("expecting audio file with wav extension, but didn't find "  +test.getAbsolutePath());
      }
      return test.exists() ? test.getAbsolutePath() :  getWavForMP3(testAudioFile);
    }
    else {
      return testAudioFile;
    }
  }

  private String removeSuffix(String audioFile) {
    return audioFile.substring(0, audioFile.length() - ".mp3".length());
  }

  /**
   * @see #dealWithMP3Audio(String)
   * @see #getImageForAudioFile(int, String, String, int, int)
   * @param audioFile
   * @return
   */
  private String getWavForMP3(String audioFile) {
    return getWavForMP3(audioFile, getInstallPath());
  }

  /**
   * Ultimately does lame --decode from.mp3 to.wav
   *
   * Worris about converting from either a relative path to an absolute path (given the webapp install location)
   * or if audioFile is a URL, converting it to a relative path before making an absolute path.
   *
   * Gotta be a better way...
   *
   * @see #getWavForMP3(String)
   * @param audioFile to convert
   * @return
   */
  private String getWavForMP3(String audioFile, String installPath) {
    assert(audioFile.endsWith(".mp3"));
    if (makeFullURLs) audioFile = new URLUtils(getThreadLocalRequest()).convertURLToRelativeFile(audioFile);
    String absolutePath = getAbsolute(audioFile,installPath).getAbsolutePath();

    if (!new File(absolutePath).exists())
      logger.error("getWavForMP3 : expecting file at " + absolutePath);
    else {
      AudioConversion audioConversion = new AudioConversion();
      File file = audioConversion.convertMP3ToWav(absolutePath);
      if (file.exists()) {
        String orig = audioFile;
        audioFile = file.getAbsolutePath();
        logger.info("from " + orig + " wrote to " + file + " or " + audioFile);
      }
      else {
        logger.error("getImageForAudioFile : can't find " + file.getAbsolutePath());
      }
    }
    assert(audioFile.endsWith(".wav"));
    return audioFile;
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
        new AudioConversion().ensureWriteMP3(r.answer, getInstallPath());
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
   * @see mitll.langtest.client.exercise.ExercisePanel#postAnswers
   */
  public void addTextAnswer(int userID, Exercise exercise, int questionID, String answer) {
    db.addAnswer(userID, exercise, questionID, answer);
  }

  public boolean isAnswerValid(int userID, Exercise exercise, int questionID) {
    return db.isAnswerValid(userID, exercise, questionID, db);
  }

  public double getScoreForAnswer(Exercise e, int questionID, String answer) {
    return autoCRT.getScoreForExercise(e, questionID, answer);
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
   * @see mitll.langtest.client.grading.GradingResultManager#changeGrade(mitll.langtest.shared.Grade)
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

  public long addUser(int age, String gender, int experience) {
    return db.addUser(getThreadLocalRequest(), age, gender, experience);
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
   * @see mitll.langtest.client.ResultManager#showResults()
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
   * @see mitll.langtest.client.ResultManager#showResults()
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
    String wavPath = getLocalPathToAnswer(plan, exercise, questionID, user);
    logger.debug("got wave file " + wavPath);
    File file = getAbsoluteFile(wavPath);

    AudioCheck.ValidityAndDur validity = new AudioConversion().convertBase64ToAudioFiles(base64EncodedString, file);
    boolean isValid = validity.validity == AudioAnswer.Validity.OK;
    if (!isValid) {
      logger.warn("got invalid audio file (" + validity +
          ") user = " + user + " exercise " + exercise +
          " question " + questionID + " file " + file.getAbsolutePath());
    }

    String wavPathWithForwardSlashSeparators = ensureForwardSlashes(wavPath);
    String url = optionallyMakeURL(wavPathWithForwardSlashSeparators);
    // logger.info("writeAudioFile converted " + wavPathWithForwardSlashSeparators + " to url " + url);

    AudioAnswer answer = (isValid) ?
      getAudioAnswer(exercise, questionID, user, reqid, file, validity, url, doFlashcard) :
      new AudioAnswer(url, validity.validity, reqid, validity.durationInMillis);

    db.addAudioAnswer(user, plan, exercise, questionID, file.getPath(),
      isValid, flq, true, audioType, validity.durationInMillis, answer.isCorrect(), (float) answer.getScore());

    return answer;
  }

  private AudioAnswer getAudioAnswer(String exercise, int questionID, int user, int reqid,
                                     File file, AudioCheck.ValidityAndDur validity, String url, boolean doFlashcard) {
    AudioAnswer audioAnswer = new AudioAnswer(url, validity.validity, reqid, validity.durationInMillis);
    if (serverProps.isFlashcard()|| doFlashcard) {
      makeAutoCRT();
      autoCRT.getFlashcardAnswer(getExercise(exercise), getExercises(), file, audioAnswer);
     // boolean isCorrect = audioAnswer.getScore() > 0.8d;
      db.updateFlashcardState(user, exercise, audioAnswer.isCorrect());
      return audioAnswer;
    } else if (serverProps.isAutoCRT()) {
      autoCRT.getAutoCRTDecodeOutput(exercise, questionID, getExercise(exercise), file, audioAnswer);
    }
    return audioAnswer;
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
   * @return
   */
  public Map<String, List<Integer>> getResultPerExercise() {
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

  public Map<String,Number> getResultStats() {
    return db.getResultStats();
  }

  @Override
  public void logMessage(String message) {
    logger.debug("from client " + message);
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

  private String optionallyMakeURL(String wavPathWithForwardSlashSeparators) {
    return makeFullURLs ?
        new URLUtils(getThreadLocalRequest()).makeURL(wavPathWithForwardSlashSeparators) :
        wavPathWithForwardSlashSeparators;
  }

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }

  private File getAbsoluteFile(String filePath) {
    String realContextPath = getInstallPath();
    return getAbsolute(filePath, realContextPath);
  }

  private File getAbsolute(String filePath, String realContextPath) {
    return new File(realContextPath, filePath);
  }

  /**
   * Figure out from the servlet context {@link #getServletContext()} and
   * the thread local request {@link #getThreadLocalRequest()} where this instance of the webapp was
   * installed.  This is really important since we use it to convert back and forth between
   * relative and absolute paths to audio and image files.
   * @return path to webapp install location
   */
  private String getInstallPath() {
    ServletContext context = getServletContext();
    if (context == null) {
      logger.error("no servlet context.");
      return "";
    }

    String realContextPath = getServletContext().getRealPath(getServletContext().getContextPath());

    List<String> pathElements = Arrays.asList(realContextPath.split(realContextPath.contains("\\") ? "\\\\" : "/"));

    // hack to deal with the app name being duplicated in the path
    if (pathElements.size() > 1) {
      String last = pathElements.get(pathElements.size() - 1);
      String nextToLast = pathElements.get(pathElements.size() - 2);
      if (last.equals(nextToLast)) {
        String nodups = realContextPath.substring(0, realContextPath.length() - last.length() -1); // remove trailing slash
        realContextPath = nodups;
      }
    }

    return realContextPath;
  }

  /**
   * Make a place to store the audio answer, of the form:<br></br>
   *
   * "answers"/plan/exercise/question/"subject-"user/"answer_"timestamp".wav"  <br></br>
   *
   * e.g. <br></br>
   *
   * answers\repeat\nl0020_ams\0\subject--1\answer_1349987649590.wav <br></br>
   *
   * or absolute  <br></br>
   *
   * C:\Users\go22670\apache-tomcat-7.0.25\webapps\netPron2\answers\repeat\nl0020_ams\0\subject--1\answer_1349987649590.wav
   *
   * @see #writeAudioFile
   * @param plan
   * @param exercise
   * @param question
   * @param user
   * @return a path relative to the install dir
   */
  private String getLocalPathToAnswer(String plan, String exercise, int question, int user) {
    String tomcatWriteDirectory = getTomcatDir();

    String planAndTestPath = plan + File.separator + exercise + File.separator + question + File.separator + "subject-" + user;
    String currentTestDir = tomcatWriteDirectory + File.separator + planAndTestPath;
    String wavPath = currentTestDir + File.separator + "answer_" + System.currentTimeMillis() + ".wav";
    File audioFilePath = new File(currentTestDir);
    boolean mkdirs = audioFilePath.mkdirs();

    return wavPath;
  }

  private String getTomcatDir() {
    String tomcatWriteDirectory = getServletContext().getInitParameter("tomcatWriteDirectoryFullPath");
    if (tomcatWriteDirectory == null) tomcatWriteDirectory = ANSWERS;

    File test = new File(tomcatWriteDirectory);
    if (!test.exists()) test.mkdirs();
    if (!test.exists()) {
      tomcatWriteDirectory = ANSWERS;
    }
    return tomcatWriteDirectory;
  }

  /**
   * @see #getImageForAudioFile(int, String, String, int, int)
   * @return path to image output dir
   */
  private String getImageOutDir() {
    String imageOutdir = getServletContext().getInitParameter("imageOutdir");
    if (imageOutdir == null) imageOutdir = IMAGE_WRITER_IMAGES;

    File test = new File(imageOutdir);
    if (!test.exists()) {
      test = getAbsoluteFile(imageOutdir);
//      System.out.println("made image out dir at " + test.getAbsolutePath());
      test.mkdirs();
    }
    return imageOutdir;
  }

  @Override
  public void destroy() {
    super.destroy();
    db.destroy();
  }

  @Override
  public void init() {
    readProperties(getServletContext());
    setInstallPath(serverProps.getUseFile());
    if (serverProps.doRecoTest()) {
      doRecoTest();
    }
    if (serverProps.doRecoTest2()) {
      doRecoTest2();
    }
  }

  /**
   * Run through all the exercises and test them against their ref audio.
   * Ideally these should all or almost all correct.
   */
  private void doRecoTest() {
    List<Exercise> exercises = getExercises();
    makeAutoCRT();

    int incorrect = 0;
    try {
      for (Exercise exercise : exercises) {
        File audioFile = new File(getInstallPath(), exercise.getRefAudio());
        if (audioFile.exists()) {
          boolean isCorrect = isCorrect(exercise, exercises);
          if (!isCorrect) incorrect++;
        } else {
          logger.warn("for " + exercise + " can't find ref audio " + audioFile.getAbsolutePath());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    logger.info("out of " + exercises.size() + " incorrect = " + incorrect);
  }

  private void doRecoTest2() {
    List<Exercise> exercises = getExercises();
    makeAutoCRT();

    int incorrect = 0;
    int total = 0;
    try {
      for (Exercise exercise : exercises) {
        List<Exercise> others = new ArrayList<Exercise>(exercises);
        others.remove(exercise);

        for (Exercise other : others) {
          File audioFile = new File(getInstallPath(), other.getRefAudio());
          if (audioFile.exists()) {
            boolean isMatch = isMatch(exercise, exercises, audioFile);
            total++;
            if (isMatch) {
              logger.debug("for " + exercise.getID() + " falsely confused audio from " +other.getID() + " as correct match.");
              incorrect++;
            }
          } else {
            logger.warn("for " + exercise + " can't find ref audio " + audioFile.getAbsolutePath());
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    logger.info("out of " + total + " incorrect = " + incorrect + (100f * ((float) incorrect / (float) total)) + "%");
  }

  private boolean isCorrect(Exercise exercise, List<Exercise> exercises) throws Exception {
    File audioFile2 = new File(getInstallPath(), exercise.getRefAudio());

    return isMatch(exercise, exercises, audioFile2);
  }

  /**
   * Does audioFile match the text in the ref sentence(s) in the exercise, given the other exercises
   * @param exercise
   * @param exercises
   * @param audioFile2
   * @return
   * @throws Exception
   */
  private boolean isMatch(Exercise exercise, List<Exercise> exercises, File audioFile2) throws Exception {
    AudioAnswer audioAnswer = new AudioAnswer();
    autoCRT.getFlashcardAnswer(exercise, exercises, audioFile2, audioAnswer);
    if (audioAnswer.getScore() == -1) {
      logger.error("hydec bad config file, stopping...");
      throw new Exception("hydec bad config file, stopping...");
    }
   // boolean isCorrect = audioAnswer.getScore() > 0.8d;
    logger.debug("---> exercise #" + exercise.getID() + " reco " + audioAnswer.decodeOutput +
      " correct " + audioAnswer.isCorrect() + (audioAnswer.isCorrect() ? "" : " audio = " + audioFile2));
    return audioAnswer.isCorrect();
  }

  private String setInstallPath(boolean useFile) {
    String lessonPlanFile = getLessonPlan();
    if (useFile && !new File(lessonPlanFile).exists()) logger.error("couldn't find lesson plan file " + lessonPlanFile);

    //logger.debug("getExercises isurdu = " + isUrdu + " datacollect mode " + dataCollectMode);
    db.setInstallPath(getInstallPath(), lessonPlanFile, relativeConfigDir, serverProps.isUrdu, useFile,
      relativeConfigDir+File.separator+serverProps.getMediaDir());

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
    this.configDir = getInstallPath() + File.separator + relativeConfigDir;

    serverProps.readPropertiesFile(servletContext, configDir);

    String h2DatabaseFile = serverProps.getH2Database();
    boolean wordPairs = serverProps.isWordPairs();
    logger.debug("word pairs " + wordPairs);
    db = new DatabaseImpl(configDir, h2DatabaseFile, serverProps.isShowSections(), wordPairs,
      serverProps.getLanguage(), serverProps.doImages(), relativeConfigDir, serverProps.isFlashcard(), false);
  }

  private class DirAndName {
    private final String testAudioFile;
    private final String installPath;
    private String testAudioName;
    private String testAudioDir;

    public DirAndName(String testAudioFile, String installPath) {
      this.testAudioFile = testAudioFile;
      this.installPath = installPath;
    }

    public String getName() {
      return testAudioName;
    }

    public String getDir() {
      return testAudioDir;
    }

    public DirAndName invoke() {
      File testAudio = new File(testAudioFile);
      testAudioName = testAudio.getName();
      testAudioDir = testAudio.getParent().substring(installPath.length());
      return this;
    }
  }
}
