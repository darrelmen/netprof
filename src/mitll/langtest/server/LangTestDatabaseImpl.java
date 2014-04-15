package mitll.langtest.server;

import audio.image.ImageType;
import audio.imagewriter.ImageWriter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Files;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.audio.PathWriter;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.SectionHelper;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.server.scoring.AutoCRTScoring;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.CommonUserExercise;
import mitll.langtest.shared.DLIUser;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.ImageResponse;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.SectionNode;
import mitll.langtest.shared.StartupInfo;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.flashcard.AVPHistoryForList;
import mitll.langtest.shared.grade.CountAndGradeID;
import mitll.langtest.shared.grade.Grade;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.monitoring.Session;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.commons.lang.exception.ExceptionUtils;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
  //private static final int MB = (1024 * 1024);
  private static final int TIMEOUT = 30;
  private static final boolean SCORE_RESULTS = false;
  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";

  private DatabaseImpl db, studentAnswersDB;
  private AudioFileHelper audioFileHelper;
  private String relativeConfigDir;
  private String configDir;
  private ServerProperties serverProps;
  private PathHelper pathHelper;

  private final Cache<String, String> userToExerciseID = CacheBuilder.newBuilder()
      .concurrencyLevel(4)
      .maximumSize(10000)
      .expireAfterWrite(TIMEOUT, TimeUnit.MINUTES).build();

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
      } catch (ServletException e) {
        logAndNotifyServerException(e);
        throw e;
      } catch (IOException ee) {
        logAndNotifyServerException(ee);
        throw ee;
      } catch (Exception eee) {
        logAndNotifyServerException(eee);
        throw new ServletException("rethrow exception", eee);
      }
  }

  public void logAndNotifyServerException(Exception e) {
    String message = "Server Exception : " + ExceptionUtils.getStackTrace(e);
    String prefixedMessage = "for " + pathHelper.getInstallPath() + " got " + message;
    logger.debug(prefixedMessage);
    getMailSupport().email(serverProps.getEmailAddress(),"Server Exception on " + pathHelper.getInstallPath(), prefixedMessage);
  }

  private List<CommonShell> getExerciseShells(Collection<CommonExercise> exercises) {
    return serverProps.getLanguage().equals("English") ? getExerciseShellsShort(exercises) : getExerciseShellsCombined(exercises);
  }

  private List<CommonShell> getExerciseShellsShort(Collection<CommonExercise> exercises) {
    List<CommonShell> ids = new ArrayList<CommonShell>();
    for (CommonExercise e : exercises) {
      ids.add(e.getShell());
    }
    return ids;
  }

  private List<CommonShell> getExerciseShellsCombined(Collection<CommonExercise> exercises) {
    List<CommonShell> ids = new ArrayList<CommonShell>();
    for (CommonExercise e : exercises) {
      ids.add(e.getShellCombinedTooltip());
    }
    return ids;
  }

  /**
   * Supports lookup by id
   *
   * @see mitll.langtest.client.list.PagingExerciseList#loadExercises(String, String)
   * @param reqID
   * @param typeToSelection
   * @param prefix
   * @param userListID
   * @param userID
   * @return
   */
  @Override
  public ExerciseListWrapper getExerciseIds(int reqID, Map<String, Collection<String>> typeToSelection, String prefix, long userListID, int userID) {
    List<CommonExercise> exercises;
    logger.debug("getExerciseIds : getting exercise ids for " +
      " config " + relativeConfigDir +
      " and user list id " + userListID);

    UserList userListByID = userListID != -1 ? db.getUserListManager().getUserListByID(userListID) : null;

    if (typeToSelection.isEmpty()) {
      if (userListByID != null) { // defensive!
        List<CommonExercise> exercises2 = getCommonExercises(userListByID);

        // consider sorting list?
        exercises = getSortedExercises(exercises2);
      } else {
        exercises = getExercises();
      }
      if (!prefix.isEmpty()) {
             // now do a trie over matches
        ExerciseTrie trie = new ExerciseTrie(exercises, serverProps.getLanguage(), audioFileHelper.getSmallVocabDecoder());
        exercises = trie.getExercises(prefix);
        if (exercises.isEmpty()) { // allow lookup by id
          CommonExercise exercise = getExercise(prefix, userID);
          if (exercise != null) exercises = Collections.singletonList(exercise);
        }
      }

      return makeExerciseListWrapper(reqID, exercises, userID);

    } else {
      // TODO build unit-lesson hierarchy if non-empty type->selection over user list
      if (userListByID != null) {
        SectionHelper helper = new SectionHelper();
        List<CommonExercise> exercises2 = getCommonExercises(userListByID);
        long then = System.currentTimeMillis();
        for (CommonExercise commonExercise : exercises2) {
          for (Map.Entry<String, String> unit : commonExercise.getUnitToValue().entrySet()) {
            helper.addExerciseToLesson(commonExercise, unit.getKey(), unit.getValue());
          }
        }
        long now = System.currentTimeMillis();

        logger.debug("used " + exercises2.size() + " exercises to build a hierarchy in " + (now - then) + " millis");
        helper.report();
        Collection<CommonExercise> exercisesForState = helper.getExercisesForSelectionState(typeToSelection);
        logger.debug("\tafter found " + exercisesForState.size() + " matches to " + typeToSelection);

        return getExerciseListWrapperForPrefix(reqID, prefix, exercisesForState, userID);
      } else {
        return getExercisesForSelectionState(reqID, typeToSelection, prefix, userID);
      }
    }
  }

  protected List<CommonExercise> getCommonExercises(UserList userListByID) {
    List<CommonExercise> exercises2 = new ArrayList<CommonExercise>();
    Collection<CommonUserExercise> exercises1 = userListByID.getExercises();
    logger.debug("getExerciseIds size - " + exercises1.size() + " for " + userListByID);
    for (CommonExercise ue : exercises1) {
      exercises2.add(ue);
    }
    logger.debug("getExerciseIds size - " + exercises2.size());
    return exercises2;
  }

  /**
   * @see mitll.langtest.client.list.HistoryExerciseList#loadExercises(String, String)
   * @param reqID
   * @param typeToSection
   * @param prefix
   * @param userID
   * @return
   */
  private ExerciseListWrapper getExercisesForSelectionState(int reqID,
                                                            Map<String, Collection<String>> typeToSection, String prefix, long userID) {
    Collection<CommonExercise> exercisesForState = db.getSectionHelper().getExercisesForSelectionState(typeToSection);

    return getExerciseListWrapperForPrefix(reqID, prefix, exercisesForState, userID);
  }

  private ExerciseListWrapper getExerciseListWrapperForPrefix(int reqID, String prefix, Collection<CommonExercise> exercisesForState, long userID) {
    boolean hasPrefix = !prefix.isEmpty();
    if (hasPrefix) {
      ExerciseTrie trie = new ExerciseTrie(exercisesForState, serverProps.getLanguage(), audioFileHelper.getSmallVocabDecoder());
      exercisesForState = trie.getExercises(prefix);
    }
    else {
      exercisesForState = getSortedExercises(exercisesForState);
    }

    return makeExerciseListWrapper(reqID, exercisesForState, userID);
  }

  /**
   * Send the first exercise along so we don't have to ask for it after we get the initial list
   * @param reqID
   * @param exercises
   * @param userID
   * @return
   * @see #getExerciseIds
   * @see #getExerciseListWrapperForPrefix(int, String, java.util.Collection, long)
   */
  private ExerciseListWrapper makeExerciseListWrapper(int reqID, Collection<CommonExercise> exercises, long userID) {
    CommonExercise firstExercise = exercises.isEmpty() ? null : exercises.iterator().next();
    if (firstExercise != null) {
      ensureMP3s(firstExercise);
      addAnnotationsAndAudio(userID, firstExercise);
      logger.debug("First is " + firstExercise);
    }
    List<CommonShell> exerciseShells = getExerciseShells(exercises);
    db.getUserListManager().markState(exerciseShells);

    return new ExerciseListWrapper(reqID, exerciseShells, firstExercise);
  }

  private void addAnnotationsAndAudio(long userID, CommonExercise firstExercise) {
    addAnnotations(firstExercise); // todo do this in a better way
    addPlayedMarkings(userID, firstExercise);
    List<AudioAttribute> audioAttributes = db.getAudioDAO().getAudioAttributes(firstExercise.getID());
    for (AudioAttribute attr : audioAttributes) firstExercise.addAudio(attr);
  }

  private void addPlayedMarkings(long userID, CommonExercise firstExercise) {
    List<Event> allForUserAndExercise = db.getEventDAO().getAllForUserAndExercise(userID, firstExercise.getID());
    Map<String,AudioAttribute> audioToAttr = firstExercise.getAudioRefToAttr();
    for (Event event : allForUserAndExercise) {
      AudioAttribute audioAttribute = audioToAttr.get(event.getContext());
      if (audioAttribute == null) logger.error("huh? can't find " +event.getContext());
      else audioAttribute.setHasBeenPlayed(true);
    }
  }

  /**
   * @seex #getSortedExerciseListWrapper(int, java.util.List)
   * @seex #getExercisesForState
   * @param exercisesForSection
   * @return
   */
  private List<CommonExercise> getSortedExercises(Collection<CommonExercise> exercisesForSection) {
    List<CommonExercise> copy = new ArrayList<CommonExercise>(exercisesForSection);
    if (serverProps.sortExercisesByID()) {
      sortByID(copy);
    }
    else {
      sortByTooltip(copy);
    }
    return copy;
  }

  private <T extends CommonShell> void sortByID(List<T> copy) {
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
        Collections.sort(copy, new Comparator<T>() {
          @Override
          public int compare(T o1, T o2) {
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

  private <T extends CommonShell>  void sortByIDStrings(List<T> copy) {
    Collections.sort(copy, new Comparator<T>() {
      @Override
      public int compare(T o1, T o2) {
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
  private  <T extends CommonShell> void sortByTooltip(List<T> exerciseShells) {
    Collections.sort(exerciseShells, new Comparator<T>() {
      @Override
      public int compare(T o1, T o2) {
        String id1 = o1.getTooltip();
        String id2 = o2.getTooltip();
        return id1.toLowerCase().compareTo(id2.toLowerCase());
      }
    });
  }

  /**
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#getTypeOrder(com.github.gwtbootstrap.client.ui.FluidContainer)
   * @return
   */
  private Collection<String> getTypeOrder() {
    SectionHelper sectionHelper = db.getSectionHelper();
    if (sectionHelper == null) logger.warn("no section helper for " + db);
    List<String> objects = Collections.emptyList();
    return (sectionHelper == null) ? objects : sectionHelper.getTypeOrder();
  }

  /**
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#getTypeOrder(com.github.gwtbootstrap.client.ui.FluidContainer)
   * @return
   */
  private List<SectionNode> getSectionNodes() {
    return db.getSectionHelper().getSectionNodes();
  }

/*  private void logMemory() {
    Runtime rt = Runtime.getRuntime();
    long free = rt.freeMemory();
    long used = rt.totalMemory() - free;
    long max = rt.maxMemory();
    if (used / MB > 500) {
      logger.debug("heap info free " + free / MB + "M used " + used / MB + "M max " + max / MB + "M");
    }
  }*/

  /**
   * Joins with annotation data when doing QC.
   *
   * @see mitll.langtest.client.list.ExerciseList#askServerForExercise
   * @param id
   * @param userID
   * @return
   */
  public CommonExercise getExercise(String id, long userID) {
    long then = System.currentTimeMillis();
    List<CommonExercise> exercises = getExercises();

    CommonExercise byID = db.getCustomOrPredefExercise(id);  // allow custom items to mask out non-custom items
    addAnnotationsAndAudio(userID, byID);

    if (byID == null) {
      logger.error("getExercise : huh? couldn't find exercise with id " + id + " when examining " + exercises.size() + " items");
    }
    else {
      logger.debug("getExercise : returning " +byID);
    }
    long now = System.currentTimeMillis();
    if (now - then > 50) {
      logger.debug("getExercise : took " + (now - then) + " millis to find " + id);
    }
    if (byID != null) {
      ensureMP3s(byID);
    }
    return byID;
  }

  private void addAnnotations(CommonExercise byID) {
    db.getUserListManager().addAnnotations(byID); // TODO nice not to do this when not in classroom...
  }

  private void ensureMP3s(CommonExercise byID) {
    Collection<AudioAttribute> audioAttributes = byID.getAudioAttributes();
    for (AudioAttribute audioAttribute : audioAttributes) {
      ensureMP3(audioAttribute.getAudioRef(), false);
    }

    if (audioAttributes.isEmpty()) { logger.warn("ensureMP3s : no ref audio for " + byID); }

/*    for (String spath : byID.getSynonymAudioRefs()) {
      ensureMP3(spath, false);
    }*/
  }

  /**
   * Called from the client:
   * @see mitll.langtest.client.list.ExerciseList#getExercises
   * @return
   */
  List<CommonExercise> getExercises() {
    List<CommonExercise> exercises = db.getExercises();
    makeAutoCRT();   // side effect of db.getExercises is to make the exercise DAO which is needed here...
    return exercises;
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
  public CommonExercise getNextUngradedExercise(String user, int expectedGrades, boolean englishOnly) {
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
   * @see #ensureMP3s(mitll.langtest.shared.CommonExercise)
   * @param wavFile
   * @param overwrite
   */
  private void ensureMP3(String wavFile, boolean overwrite) {
    if (wavFile != null) {
        new AudioConversion().ensureWriteMP3(wavFile, pathHelper.getInstallPath(), overwrite);
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
   * @param exerciseID
   * @return path to an image file
   * */
  public ImageResponse getImageForAudioFile(int reqid, String audioFile, String imageType, int width, int height, String exerciseID) {
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
    logger.debug("getImageForAudioFile : getting images (" + width + " x " + height + ") (" +reqid+ ") type " + imageType+
      " for " + wavAudioFile + "");
    String absolutePathToImage = imageWriter.writeImageSimple(wavAudioFile, pathHelper.getAbsoluteFile(imageOutDir).getAbsolutePath(),
        width, height, imageType1, exerciseID);
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
    if (audioFile.endsWith("." +
        AudioTag.COMPRESSED_TYPE)) {
      String wavFile = removeSuffix(audioFile) + WAV;
      File test = pathHelper.getAbsoluteFile(wavFile);
      audioFile = test.exists() ? test.getAbsolutePath() : audioFileHelper.getWavForMP3(audioFile);
    }

    return ensureWAV(audioFile);
  }

  private String removeSuffix(String audioFile) {
    return audioFile.substring(0, audioFile.length() - MP3.length());
  }

  private String ensureWAV(String audioFile) {
    if (!audioFile.endsWith("wav")) {
      return audioFile.substring(0, audioFile.length() - MP3.length()) + WAV;
    } else {
      return audioFile;
    }
  }

  /**
   * Get properties (first time called read properties file -- e.g. see war/config/levantine/config.properties).
   * @see mitll.langtest.client.LangTest#onModuleLoad
   * @return
   */
  @Override
  public StartupInfo getStartupInfo() {
    return new StartupInfo(serverProps.getProperties(), getTypeOrder(), getSectionNodes());
  }

  /**
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#scoreAudio(String, long, String, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, int, int, int)
   * @param reqid
   * @param resultID
   * @param testAudioFile
   * @param sentence
   * @param width
   * @param height
   * @param useScoreToColorBkg
   * @return
   */
  public PretestScore getASRScoreForAudio(int reqid, long resultID, String testAudioFile, String sentence,
                                          int width, int height, boolean useScoreToColorBkg) {
    PretestScore asrScoreForAudio = audioFileHelper.getASRScoreForAudio(reqid, testAudioFile, sentence, width, height, useScoreToColorBkg,
      false, Files.createTempDir().getAbsolutePath(), serverProps.useScoreCache());
    if (resultID > -1) {
      db.getAnswerDAO().changeAnswer(resultID, asrScoreForAudio.getHydecScore());
    }
    return asrScoreForAudio;
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
   * @see mitll.langtest.server.autocrt.AutoCRT#getAutoCRTDecodeOutput
   * @param phrases
   * @return
   */
  @Override
  public Collection<String> getValidPhrases(Collection<String> phrases) {
    return audioFileHelper.getValidPhrases(phrases);
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
  public void addTextAnswer(int userID, CommonExercise exercise, int questionID, String answer, String answerType) {
    db.addAnswer(userID, exercise, questionID, answer, answerType);
    db.addCompleted(userID,exercise.getID());
  }

  // Grades ---------------------

  /**
   * @see mitll.langtest.client.grading.GradingResultManager#addGrade
   * @param exerciseID
   * @return
   */
  public CountAndGradeID addGrade(String exerciseID, Grade toAdd) {  return db.addGrade(exerciseID, toAdd);  }

  /**
   * @see mitll.langtest.client.grading.GradingResultManager#changeGrade(mitll.langtest.shared.grade.Grade)
   * @param toChange
   */
  public void changeGrade(Grade toChange) { db.changeGrade(toChange);  }

  @Override
  public synchronized int userExists(String login) { return db.userExists(login);  }

  // Users ---------------------

  public void addDLIUser(DLIUser dliUser) {
    try {
      db.addDLIUser(dliUser);
    } catch (Exception e) {
      logAndNotifyServerException(e);
    }
  }

  /**
   * @see mitll.langtest.client.custom.CreateListDialog#doCreate
   * @param userid
   * @param name
   * @param description
   * @param dliClass
   * @return
   */
  @Override
  public long addUserList(long userid, String name, String description, String dliClass) {
    return db.getUserListManager().addUserList(userid, name, description, dliClass);
  }

  /**
   * @see mitll.langtest.client.custom.Navigation#addVisitor(mitll.langtest.shared.custom.UserList)
   * @param userListID
   * @param user
   */
  public void addVisitor(long userListID, long user) { db.getUserListManager().addVisitor(userListID,user); }

  /**
   * @see mitll.langtest.client.custom.Navigation#showInitialState()
   * @see mitll.langtest.client.custom.Navigation#viewLessons
   * @see mitll.langtest.client.custom.NPFExercise#populateListChoices
   * @param userid
   * @param onlyCreated
   * @param visited
   * @return
   */
  public Collection<UserList> getListsForUser(long userid, boolean onlyCreated, boolean visited) {
    if (!onlyCreated && !visited) logger.error("huh? asking for neither your lists nor  your visited lists.");
    return db.getUserListManager().getListsForUser(userid, onlyCreated, visited);
  }

  /**
   * @see mitll.langtest.client.custom.Navigation#showInitialState()
   * @see mitll.langtest.client.custom.Navigation#viewLessons
   * @param search
   * @param userid
   * @return
   */
  @Override
  public Collection<UserList> getUserListsForText(String search, long userid) {
    return db.getUserListManager().getUserListsForText(search, userid);
  }

  /**
   * @see mitll.langtest.client.custom.NPFExercise#populateListChoices
   * @param userListID
   * @param userExercise
   * @return
   */
  public void addItemToUserList(long userListID, UserExercise userExercise) {
    db.getUserListManager().addItemToUserList(userListID, userExercise);
  }

  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#addAnnotation(String, String, String)
   * @param exerciseID
   * @param field
   * @param status
   * @param comment
   * @param userID
   */
  @Override
  public void addAnnotation(String exerciseID, String field, String status, String comment, long userID) {
    db.getUserListManager().addAnnotation(exerciseID, field, status, comment, userID);
  }

  /**
   * @see mitll.langtest.client.custom.QCNPFExercise#markReviewed
   * @param id
   * @param isCorrect
   * @param creatorID
   */
  public void markReviewed(String id, boolean isCorrect, long creatorID) {
    db.getUserListManager().markCorrectness(id, isCorrect, creatorID);
  }

  public void markState(String id, CommonShell.STATE state, long creatorID) {
    db.getUserListManager().markState(id, state, creatorID);
  }

  @Override
  public void setAVPSkip(Collection<Long> ids) { db.getAnswerDAO().changeType(ids); }

  /**
   * @see mitll.langtest.client.custom.ReviewEditableExercise#doAfterEditComplete(mitll.langtest.client.list.ListInterface, boolean)
   * @param id
   * @param state
   * @param userID
   */
  @Override
  public void setExerciseState(String id, CommonShell.STATE state, long userID) {
    db.getUserListManager().markState(id, state, userID);
  }

  /**
   * @see mitll.langtest.client.custom.Navigation#viewReview
   * @return
   */
  @Override
  public UserList getDefectList() { return db.getUserListManager().getDefectList(); }
  public UserList getAttentionList() { return db.getUserListManager().getAttentionList(); }

  @Override
  public boolean deleteList(long id) {
    return db.getUserListManager().deleteList(id);
  }

  @Override
  public boolean deleteItemFromList(long listid, String exid) {
    return db.getUserListManager().deleteItemFromList(listid, exid);
  }

  @Override
  public UserList getCommentedList() { return db.getUserListManager().getCommentedList(); }

  @Override
  public boolean isValidForeignPhrase(String foreign) {  return audioFileHelper.checkLTS(foreign); }

  /**
   * Put the new item in the database,
   * copy the audio under bestAudio
   * assign the item to a user list
   * @see mitll.langtest.client.custom.NewUserExercise#afterValidForeignPhrase
   * @param userListID
   * @param userExercise
   */
  public UserExercise reallyCreateNewItem(long userListID, UserExercise userExercise) {
    db.getUserListManager().reallyCreateNewItem(userListID, userExercise);
    logger.debug("reallyCreateNewItem : made user exercise " + userExercise);

    return userExercise;
  }

  /**
   * @see mitll.langtest.client.custom.ReviewEditableExercise#duplicateExercise()
   * @param exercise
   * @return
   */
  @Override
  public UserExercise duplicateExercise(UserExercise exercise) { return db.duplicateExercise(exercise);  }
  public boolean deleteItem(String id ) {
    return db.deleteItem(id);
  }

  @Override
  public void logEvent(String id, String widgetType, String exid, String context, long userid, String hitID) {
    db.logEvent(id,widgetType,exid,context,userid, hitID);
  }

  public List<Event> getEvents() { return db.getEvents(); }

  /**
   * @see mitll.langtest.client.custom.EditableExercise#postEditItem
   * @param userExercise
   */
  @Override
  public void editItem(UserExercise userExercise) {
    db.editItem(userExercise);
    logger.debug("editItem : now user exercise " + userExercise);
  }

  /**
   * @param age
   * @param gender
   * @param experience
   * @param nativeLang
   * @param dialect
   * @param userID
   * @return
   */
  @Override
  public long addUser(int age, String gender, int experience,
                      String nativeLang, String dialect, String userID) {
    logger.debug("Adding user " + userID);
    return db.addUser(getThreadLocalRequest(),age, gender, experience, nativeLang, dialect, userID);
  }

  /**
   * @see mitll.langtest.client.user.UserTable#showDialog(mitll.langtest.client.LangTestDatabaseAsync)
   * @return
   */
  public List<User> getUsers() { return db.getUsers();  }

  // Results ---------------------

  /**
   * @return
   * @see mitll.langtest.client.result.ResultManager#showResults()
   */
  @Override
  public List<Result> getResults(int start, int end, String sortInfo) {
    List<Result> results = db.getResultsWithGrades();
    if (!results.isEmpty()) {
      String[] columns = sortInfo.split(",");
      Comparator<Result> comparator = results.get(0).getComparator(Arrays.asList(columns));
      Collections.sort(results, comparator);
    }
    List<Result> resultList = results.subList(start, end);
    return new ArrayList<Result>(resultList);
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
   * @param recordInResults
   * @param addToAudioTable
   * @return URL to audio on server and if audio is valid (not too short, etc.)
   */
  public AudioAnswer writeAudioFile(String base64EncodedString, String plan, String exercise, int questionID,
                                    int user, int reqid, boolean flq, String audioType, boolean doFlashcard,
                                    boolean recordInResults, boolean addToAudioTable) {
    CommonExercise exercise1 = getExercise(exercise,user);
    AudioAnswer audioAnswer = audioFileHelper.writeAudioFile(base64EncodedString, plan, exercise, exercise1, questionID, user, reqid,
      flq, audioType, doFlashcard, recordInResults);

    if (addToAudioTable && audioAnswer.isValid()) {
      // TODO write or overwrite audio table entry
      File fileRef = pathHelper.getAbsoluteFile(audioAnswer.getPath());
      String destFileName = audioType + "_" + System.currentTimeMillis() + "_by_" + user + ".wav";
     // String refAudio = getRefAudioPath(userExercise.getID(), fileRef, fast, overwrite);

      String permanentAudioPath = new PathWriter().getPermanentAudioPath(pathHelper, fileRef, destFileName, true, exercise);
      db.getAudioDAO().addOrUpdate(user, permanentAudioPath, exercise, System.currentTimeMillis(), audioType, audioAnswer.getDurationInMillis());
    }
    return audioAnswer;
  }

  void makeAutoCRT() { audioFileHelper.makeAutoCRT(relativeConfigDir, this, studentAnswersDB, this); }

  @Override
  public Map<User, Integer> getUserToResultCount() { return db.getUserToResultCount();  }
  @Override
  public Map<Integer, Integer> getResultCountToCount() { return db.getResultCountToCount();  }
  @Override
  public Map<String, Integer> getResultByDay() {  return db.getResultByDay();  }
  @Override
  public Map<String, Integer> getResultByHourOfDay() {  return db.getResultByHourOfDay();  }

  /**
   * Map of overall, male, female to list of counts (ex 0 had 7, ex 1, had 5, etc.)
   * @see mitll.langtest.client.monitoring.MonitoringManager#doResultLineQuery
   * @return
   */
  public Map<String, Map<String, Integer>> getResultPerExercise() { return db.getResultPerExercise(); }

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

  /**
   * @see mitll.langtest.client.monitoring.MonitoringManager#doSessionQuery
   * @return
   */
  public List<Session> getSessions() {
    return db.getSessions();
  }

  /**
   * @see mitll.langtest.client.monitoring.MonitoringManager#doSessionQuery
   * @return
   */
  public Map<String,Number> getResultStats() {
    return db.getResultStats();
  }

  @Override
  public Map<Integer, Map<String, Map<String, Integer>>> getGradeCountPerExercise() {
    return db.getGradeCountPerExercise();
  }

  /**
   * @see mitll.langtest.client.custom.MyFlashcardExercisePanelFactory.StatsPracticePanel#onSetComplete()
   * @param userid
   * @param latestResultID
   * @return
   */
  @Override
  public List<AVPHistoryForList> getUserHistoryForList(long userid, Collection<String> ids, long latestResultID) {
    logger.debug("getUserHistoryForList " + userid + " and " + ids);

    return db.getUserHistoryForList(userid, ids, latestResultID);
  }

  public void logMessage(String message) {
    String prefixedMessage = "for " + pathHelper.getInstallPath() + " from client " + message;
    logger.debug(prefixedMessage);

    if (message.startsWith("got browser exception")) {
      getMailSupport().email(serverProps.getEmailAddress(),"Javascript Exception", prefixedMessage);
    }
  }

  private MailSupport getMailSupport() {  return new MailSupport(serverProps.isDebugEMail());  }

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
    audioFileHelper = new AudioFileHelper(pathHelper, serverProps, db, this);
    if (serverProps.doRecoTest() || serverProps.doRecoTest2()) {
      new RecoTest(this, serverProps, pathHelper, audioFileHelper);
    }
    if (!serverProps.dataCollectMode && !serverProps.isArabicTextDataCollect()) {
      db.getUnmodExercises();
    }

    db.getUserListManager().setStateOnExercises();

    if (SCORE_RESULTS) {
      List<Result> resultsThatNeedScore = db.getResultDAO().getResultsFor();
      //resultsThatNeedScore = resultsThatNeedScore.subList(0,20);
      getExercises();
      logger.debug("doing scoring on " + resultsThatNeedScore.size() + " results");
      for (Result result : resultsThatNeedScore) {
        String trim = result.id.trim();
        CommonExercise exercise = db.getExercise(trim);
        if (exercise != null) {
          logger.info("getting score for " + result);
          getASRScoreForAudio(0, result.uniqueID, pathHelper.getInstallPath() + File.separator + result.answer, exercise.getRefSentence(), 100, 100, false);
        }
      }
    }
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

    serverProps = new ServerProperties(servletContext, configDir);
    String h2DatabaseFile = serverProps.getH2Database();

    db = makeDatabaseImpl(h2DatabaseFile);
    if (serverProps.isAutoCRT()) {
      studentAnswersDB = makeDatabaseImpl(serverProps.getH2StudentAnswersDatabase());
      logger.debug("using student answers db at " + serverProps.getH2StudentAnswersDatabase());
    }
  }

  private DatabaseImpl makeDatabaseImpl(String h2DatabaseFile) {
    //logger.debug("word pairs " +  serverProps.isWordPairs() + " language " + serverProps.getLanguage() + " config dir " + relativeConfigDir);
    return new DatabaseImpl(configDir, relativeConfigDir, h2DatabaseFile, serverProps, pathHelper, true);
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
      relativeConfigDir+File.separator+serverProps.getMediaDir());

    return lessonPlanFile;
  }

  private String getLessonPlan() {
    return configDir + File.separator + serverProps.getLessonPlan();
  }
}
