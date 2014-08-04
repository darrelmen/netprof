package mitll.langtest.server;

import audio.image.ImageType;
import audio.imagewriter.ImageWriter;
import com.google.common.io.Files;
import com.google.gwt.media.client.Audio;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.audio.PathWriter;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.SectionHelper;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.server.scoring.AutoCRTScoring;
import mitll.langtest.shared.*;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.flashcard.AVPHistoryForList;
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
import java.util.*;

/**
 * Supports all the database interactions.
 * User: GO22670
 * Date: 5/7/12
 * Time: 5:49 PM
 */
@SuppressWarnings("serial")
public class LangTestDatabaseImpl extends RemoteServiceServlet implements LangTestDatabase, AutoCRTScoring {
  private static final Logger logger = Logger.getLogger(LangTestDatabaseImpl.class);
  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";

  private DatabaseImpl db;
  private AudioFileHelper audioFileHelper;
  private String relativeConfigDir;
  private String configDir;
  private ServerProperties serverProps;
  private PathHelper pathHelper;

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
    if (!e.getMessage().contains("Broken Pipe")) {
      String message = "Server Exception : " + ExceptionUtils.getStackTrace(e);
      String prefixedMessage = "for " + pathHelper.getInstallPath() + " got " + message;
      logger.debug(prefixedMessage);
      getMailSupport().email(serverProps.getEmailAddress(), "Server Exception on " + pathHelper.getInstallPath(), prefixedMessage);
    }
  }

  private List<CommonShell> getExerciseShells(Collection<? extends CommonExercise> exercises) {
    return serverProps.getLanguage().equals("English") ? getExerciseShellsShort(exercises) : getExerciseShellsCombined(exercises);
  }

  private List<CommonShell> getExerciseShellsShort(Collection<? extends CommonExercise> exercises) {
    List<CommonShell> ids = new ArrayList<CommonShell>();
    for (CommonExercise e : exercises) {
      ids.add(e.getShell());
    }
    return ids;
  }

  private List<CommonShell> getExerciseShellsCombined(Collection<? extends CommonExercise> exercises) {
    List<CommonShell> ids = new ArrayList<CommonShell>();
    for (CommonExercise e : exercises) {
      ids.add(e.getShellCombinedTooltip());
    }
    return ids;
  }

  /**
   * Complicated.
   *
   * Get exercise ids, either from the predefined set or a user list.
   * Take the result and if there's a unit-chapter filter, use that to return only the exercises in the selected
   * units/chapters.
   * Further optionally filter by string prefix on each item's english or f.l.
   *
   * Supports lookup by id
   *
   * Marks items with state/second state given user id. User is used to mark the audio in items with whether they have
   * been played by that user.
   *
   * Uses role to determine if we're in recorder mode and marks items recorded by the user as RECORDED.
   *
   * Sorts the result by unit, then chapter, then alphabetically in chapter. If role is recorder, put the recorded
   * items at the front.
   *
   * @see mitll.langtest.client.list.PagingExerciseList#loadExercises(String, String)
   * @param reqID
   * @param typeToSelection
   * @param prefix
   * @param userListID
   * @param userID
   * @param role
   * @param onlyUnrecordedByMe
   * @return
   */
  @Override
  public ExerciseListWrapper getExerciseIds(int reqID, Map<String, Collection<String>> typeToSelection, String prefix,
                                            long userListID, int userID, String role, boolean onlyUnrecordedByMe) {
    Collection<CommonExercise> exercises;
    logger.debug("getExerciseIds : getting exercise ids for " +
      " config " + relativeConfigDir +
      " prefix " + prefix+
      " and user list id " + userListID + " user " + userID + " role " + role + " filter " + onlyUnrecordedByMe);

    UserList userListByID = userListID != -1 ? db.getUserListManager().getUserListByID(userListID, getTypeOrder()) : null;

    if (typeToSelection.isEmpty()) {   // no unit-chapter filtering
      // get initial exercise set, either from a user list or predefined
      boolean predefExercises = userListByID == null;
      exercises = predefExercises ? getExercises() : getCommonExercises(userListByID);

      // now if there's a prefix, filter by prefix match
      if (!prefix.isEmpty()) {
        // now do a trie over matches
        long then = System.currentTimeMillis();
        ExerciseTrie trie = predefExercises ? fullTrie : new ExerciseTrie(exercises, serverProps.getLanguage(), audioFileHelper.getSmallVocabDecoder());
        exercises = trie.getExercises(prefix);
        long now = System.currentTimeMillis();
        if (now-then > 300) {
          logger.debug("took " + (now-then) + " millis to do trie lookup");
        }
        if (exercises.isEmpty()) { // allow lookup by id
          CommonExercise exercise = getExercise(prefix, userID);
          if (exercise != null) exercises = Collections.singletonList(exercise);
        }
      }
      exercises = filterByUnrecorded(userID, onlyUnrecordedByMe, exercises);
      int i = markRecordedState(userID, role, exercises);
      //logger.debug("marked " +i + " as recorded");

      // now sort : everything gets sorted the same way
      List<CommonExercise> commonExercises = new ArrayList<CommonExercise>(exercises);
      new ExerciseSorter(getTypeOrder()).getSortedByUnitThenAlpha(commonExercises, role.equals(Result.AUDIO_TYPE_RECORDER));

      return makeExerciseListWrapper(reqID, commonExercises, userID, role);

    } else { // sort by unit-chapter selection
      // builds unit-lesson hierarchy if non-empty type->selection over user list
      if (userListByID != null) {
        Collection<CommonExercise> exercisesForState = getExercisesFromFiltered(typeToSelection, userListByID);
        exercisesForState = filterByUnrecorded(userID, onlyUnrecordedByMe, exercisesForState);

        return getExerciseListWrapperForPrefix(reqID, prefix, exercisesForState, userID, role);
      } else {
        return getExercisesForSelectionState(reqID, typeToSelection, prefix, userID, role, onlyUnrecordedByMe);
      }
    }
  }

  private Collection<CommonExercise> filterByUnrecorded(long userID, boolean onlyUnrecordedByMe, Collection<CommonExercise> exercises) {
    if (onlyUnrecordedByMe) {
      Set<String> recordedForUser = db.getAudioDAO().getRecordedBy(userID);
      boolean male = db.getUserDAO().getUserWhere(userID).isMale();
      List<CommonExercise> copy = new ArrayList<CommonExercise>();
      logger.debug("recorded already " + recordedForUser.size() + " checking " + exercises.size());
      // filter
      for (CommonExercise exercise : exercises) {
        if (!recordedForUser.contains(exercise.getID())) {
          copy.add(exercise);
        }
        else {
          Collection<AudioAttribute> byGender = exercise.getByGender(male);
          boolean hasReg = false;
          boolean hasSlow = false;
 /*         if (!byGender.isEmpty()) logger.debug("checking " + male + " ex " + exercise.getID()+
            " has " + byGender.size() + " recordings");*/
          for (AudioAttribute attr : byGender) {
            if (attr.getUserid() != UserDAO.DEFAULT_USER_ID) {
              hasReg = hasReg ||  attr.isRegularSpeed();
              hasSlow = hasSlow ||  attr.isSlow();
              if (hasReg && hasSlow) break;
            }
          }

          if (!hasReg || !hasSlow) {
            copy.add(exercise);
          }
        }
      }
      logger.debug("to be recorded " + copy.size() + " from " + exercises.size());

      return copy;
    }
    return exercises;
  }

  private Collection<CommonExercise> getExercisesFromFiltered(Map<String, Collection<String>> typeToSelection, UserList userListByID) {
    SectionHelper helper = new SectionHelper();
    List<CommonExercise> exercises2 = getCommonExercises(userListByID);
    long then = System.currentTimeMillis();
    for (CommonExercise commonExercise : exercises2) {
      helper.addExercise(commonExercise);
    }
    long now = System.currentTimeMillis();

    if (now - then > 100) {
      logger.debug("used " + exercises2.size() + " exercises to build a hierarchy in " + (now - then) + " millis");
    }
    helper.report();
    Collection<CommonExercise> exercisesForState = helper.getExercisesForSelectionState(typeToSelection);
    logger.debug("\tafter found " + exercisesForState.size() + " matches to " + typeToSelection);
    return exercisesForState;
  }

  /***
   * Marks each exercise - first state - with whether this user has recorded audio for this item
   * Defective audio is not included.
   * Also if just one of regular or slow is recorded it's not "recorded".
   *
   * What you want to see in the record audio tab.  One bit of info - recorded or not recorded.
   *
   * @param userID
   * @param role
   * @param exercises
   * @return
   */
  private int markRecordedState(int userID, String role, Collection<? extends CommonShell> exercises) {
    int c = 0;
    if (role.equals(Result.AUDIO_TYPE_RECORDER)) {
      Set<String> recordedForUser = db.getAudioDAO().getRecordedForUser(userID);
      //logger.debug("\tfound " + recordedForUser.size() + " recordings by " + userID);
      for (CommonShell shell : exercises) {
        if (recordedForUser.contains(shell.getID())) {
          shell.setState(STATE.RECORDED);
          c++;
        }
      }
    } else {
      //logger.debug("\tnot marking recorded for " + role + " and " + userID);
    }
    return c;
  }

  /**
   * Copies the exercises....?
   * @param userListByID
   * @return
   * @see #getExerciseIds
   * @see #getExercisesFromFiltered(java.util.Map, mitll.langtest.shared.custom.UserList)
   */
  private List<CommonExercise> getCommonExercises(UserList userListByID) {
    List<CommonExercise> exercises2 = new ArrayList<CommonExercise>();
    Collection<CommonUserExercise> exercises1 = userListByID.getExercises();
    //logger.debug("getExerciseIds size - " + exercises1.size() + " for " + userListByID);
    for (CommonExercise ue : exercises1) {
      exercises2.add(ue);
    }
    //logger.debug("getExerciseIds size - " + exercises2.size());
    return exercises2;
  }

  /**
   * @see mitll.langtest.client.list.HistoryExerciseList#loadExercises(String, String)
   * @see #getExerciseIds
   * @param reqID
   * @param typeToSection
   * @param prefix
   * @param userID
   * @param role
   * @param onlyUnrecordedByMe
   * @return
   */
  private ExerciseListWrapper getExercisesForSelectionState(int reqID,
                                                            Map<String, Collection<String>> typeToSection, String prefix,
                                                            long userID, String role, boolean onlyUnrecordedByMe) {
    Collection<CommonExercise> exercisesForState = db.getSectionHelper().getExercisesForSelectionState(typeToSection);
    exercisesForState = filterByUnrecorded(userID, onlyUnrecordedByMe, exercisesForState);

    return getExerciseListWrapperForPrefix(reqID, prefix, exercisesForState, userID, role);
  }

  /**
   * Always sort the result
   * @param reqID
   * @param prefix
   * @param exercisesForState
   * @param userID
   * @param role
   * @return
   * @see #getExerciseIds
   */
  private ExerciseListWrapper getExerciseListWrapperForPrefix(int reqID, String prefix, Collection<CommonExercise> exercisesForState, long userID, String role) {
    boolean hasPrefix = !prefix.isEmpty();
    if (hasPrefix) {
      logger.debug("getExerciseListWrapperForPrefix userID " +userID + " prefix '" + prefix+ "' role " +role);
    }

    int i = markRecordedState((int) userID, role, exercisesForState);
    //logger.debug("marked " +i + " as recorded role " +role);

    if (hasPrefix) {
      ExerciseTrie trie = new ExerciseTrie(exercisesForState, serverProps.getLanguage(), audioFileHelper.getSmallVocabDecoder());
      exercisesForState = trie.getExercises(prefix);
    }
    // why copy???
    List<CommonExercise> copy = new ArrayList<CommonExercise>(exercisesForState);

    new ExerciseSorter(getTypeOrder()).getSortedByUnitThenAlpha(copy, role.equals(Result.AUDIO_TYPE_RECORDER));

    return makeExerciseListWrapper(reqID, copy, userID, role);
  }

  /**
   * Send the first exercise along so we don't have to ask for it after we get the initial list
   * @param reqID
   * @param exercises
   * @param userID
   * @param role
   * @return
   * @see #getExerciseIds
   * @see #getExerciseListWrapperForPrefix(int, String, java.util.Collection, long, String)
   */
  private ExerciseListWrapper makeExerciseListWrapper(int reqID, Collection<CommonExercise> exercises, long userID, String role) {
    CommonExercise firstExercise = exercises.isEmpty() ? null : exercises.iterator().next();
    if (firstExercise != null) {
      addAnnotationsAndAudio(userID, firstExercise);
      ensureMP3s(firstExercise);
    }
    List<CommonShell> exerciseShells = getExerciseShells(exercises);

    //   logger.debug("makeExerciseListWrapper : userID " +userID + " Role is " + role);
    if (role.equals(Result.AUDIO_TYPE_RECORDER)) {
      markRecordedState((int) userID, role, exerciseShells);
    } else if (role.equalsIgnoreCase(User.Permission.QUALITY_CONTROL.toString()) || role.startsWith(Result.AUDIO_TYPE_REVIEW)) {
      db.getUserListManager().markState(exerciseShells);
    }

    return new ExerciseListWrapper(reqID, exerciseShells, firstExercise);
  }

  /**
   * 0) Add annotations to fields on exercise
   * 1) Attach audio recordings to exercise.
   * 2) Adds information about whether the audio has been played or not...
   * 3) Attach history info (when has the user recorded audio for the item under the learn tab and gotten a score)
   * @param userID
   * @param firstExercise
   * @see #getExercise(String, long)
   * @see #makeExerciseListWrapper(int, java.util.Collection, long, String)
   */
  private void addAnnotationsAndAudio(long userID, CommonExercise firstExercise) {
    addAnnotations(firstExercise); // todo do this in a better way
    attachAudio(firstExercise);
    addPlayedMarkings(userID, firstExercise);

    attachScoreHistory(userID, firstExercise);
  }

  private void attachScoreHistory(long userID, CommonExercise firstExercise) {
    List<Result> resultsForExercise = db.getResultDAO().getResultsForExercise(firstExercise.getID());

    int total = 0;
    float scoreTotal = 0f;
    List<ScoreAndPath> scores = new ArrayList<ScoreAndPath>();
    for (Result r : resultsForExercise) {
      float pronScore = r.getPronScore();
      if (pronScore > 0) { // overkill?
        total++;
        scoreTotal += pronScore;
        if (r.userid == userID) {
          scores.add(new ScoreAndPath(pronScore, r.answer));
        }
      }
    }
    firstExercise.setScores(scores);
    firstExercise.setAvgScore(total == 0 ? 0f : scoreTotal/total);
  }

  /**
   * @see #addAnnotationsAndAudio(long, mitll.langtest.shared.CommonExercise)
   * @param firstExercise
   */
  private void attachAudio(CommonExercise firstExercise) {
    String installPath = pathHelper.getInstallPath();
    String relativeConfigDir1 = relativeConfigDir;
    db.getAudioDAO().attachAudio(firstExercise, installPath, relativeConfigDir1);
  }

  private void addPlayedMarkings(long userID, CommonExercise firstExercise) {
    List<Event> allForUserAndExercise = db.getEventDAO().getAllForUserAndExercise(userID, firstExercise.getID());
    Map<String, AudioAttribute> audioToAttr = firstExercise.getAudioRefToAttr();
    for (Event event : allForUserAndExercise) {
      AudioAttribute audioAttribute = audioToAttr.get(event.getContext());
      if (audioAttribute == null) {
        //logger.warn("addPlayedMarkings huh? can't find " + event.getContext() + " in " + audioToAttr.keySet());
      }
      else {
        audioAttribute.setHasBeenPlayed(true);
      }
    }
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
   * @see #getStartupInfo()
   * @return
   */
  private List<SectionNode> getSectionNodes() {
    return db.getSectionHelper().getSectionNodes();
  }

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

    if (byID == null) {
      logger.error("getExercise : huh? couldn't find exercise with id " + id + " when examining " + exercises.size() + " items");
    }
    else {
      addAnnotationsAndAudio(userID, byID);
      //logger.debug("getExercise : returning " + byID);
      ensureMP3s(byID);
    }
    long now = System.currentTimeMillis();
    if (now - then > 200) {
      logger.debug("getExercise : took " + (now - then) + " millis to find " + id);
    }
    return byID;
  }

  private void addAnnotations(CommonExercise byID) {
    db.getUserListManager().addAnnotations(byID); // TODO nice not to do this when not in classroom...
  }

  /**
   * @see #getExercise(String, long)
   * @see #makeExerciseListWrapper(int, java.util.Collection, long, String)
   * @param byID
   */
  private void ensureMP3s(CommonExercise byID) {
    Collection<AudioAttribute> audioAttributes = byID.getAudioAttributes();
    for (AudioAttribute audioAttribute : audioAttributes) {
      if (!ensureMP3(audioAttribute.getAudioRef(), false)) {
        audioAttribute.setAudioRef(AudioConversion.FILE_MISSING);
      }
    }

    if (audioAttributes.isEmpty()) { logger.warn("ensureMP3s : no ref audio for " + byID); }
  }

  private boolean checkedLTS = false;
  private ExerciseTrie fullTrie;

  /**
   * Called from the client:
   * @see mitll.langtest.client.list.ListInterface#getExercises
   * @return
   */
  List<CommonExercise> getExercises() {
    List<CommonExercise> exercises = db.getExercises();
    makeAutoCRT();   // side effect of db.getExercises is to make the exercise DAO which is needed here...
    if (fullTrie == null) {
      fullTrie = new ExerciseTrie(exercises, serverProps.getLanguage(), audioFileHelper.getSmallVocabDecoder());
    }

    checkLTS(exercises);
    return exercises;
  }

  private void checkLTS(List<CommonExercise> exercises) {
    synchronized (this) {
      if (!checkedLTS) {
        checkedLTS = true;
        int count = 0;
        for (CommonExercise exercise : exercises) {
          boolean validForeignPhrase = isValidForeignPhrase(exercise.getForeignLanguage());
          if (!validForeignPhrase) {
            if (count < 10) {
              logger.error("huh? for " + exercise.getID() + " we can't parse " + exercise.getID() + " " + exercise.getEnglish() + " fl " + exercise.getForeignLanguage());
            }
            count++;
          }
        }
        if (count > 0) {
          logger.error("huh? out of " + exercises.size() + " LTS fails on " + count);
        }
      }
    }
  }

  /**
   * TODO : come back to this!!!
   * @see #ensureMP3s(mitll.langtest.shared.CommonExercise)
   * @param wavFile
   * @param overwrite
   * @return true if mp3 file exists
   */
  private boolean ensureMP3(String wavFile, boolean overwrite) {
    if (wavFile != null) {
      String parent = pathHelper.getInstallPath();
      //logger.debug("ensureMP3 : wav " + wavFile + " under " + parent);

      AudioConversion audioConversion = new AudioConversion();
      if (!audioConversion.exists(wavFile, parent)) {
        logger.warn("can't find " + wavFile + " under " + parent + " trying config... ");
        parent = configDir;
      }
      if (!audioConversion.exists(wavFile, parent)) {
        logger.error("huh? can't find " + wavFile + " under " + parent);
      }
      String s = audioConversion.ensureWriteMP3(wavFile, parent, overwrite);
      return !(s.equals(AudioConversion.FILE_MISSING));
    }
    return false;
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
    File testFile = new File(wavAudioFile);
    if (!testFile.exists() || testFile.length() == 0) {
      if (testFile.length() == 0) logger.error("huh? " +wavAudioFile + " is empty???");
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

    String relativeImagePath = absolutePathToImage;
    if (absolutePathToImage.startsWith(installPath)) {
      relativeImagePath = absolutePathToImage.substring(installPath.length());
    } else {
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

    return new ImageResponse(reqid, imageURL, duration);
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
   * @see mitll.langtest.client.scoring.ASRScoringAudioPanel#scoreAudio(String, long, String, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, int, int, int)
   * @param reqid
   * @param resultID
   * @param testAudioFile
   * @param sentence
   * @param width
   * @param height
   * @param useScoreToColorBkg
   * @param exerciseID
   * @return
   */
  public PretestScore getASRScoreForAudio(int reqid, long resultID, String testAudioFile, String sentence,
                                          int width, int height, boolean useScoreToColorBkg, String exerciseID) {
    PretestScore asrScoreForAudio = audioFileHelper.getASRScoreForAudio(reqid, testAudioFile, sentence, width, height, useScoreToColorBkg,
      false, Files.createTempDir().getAbsolutePath(), serverProps.useScoreCache(), exerciseID);
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
  }

  // Grades ---------------------

  @Override
  public synchronized int userExists(String login) { return db.userExists(login);  }

  // Users ---------------------

  /**
   * @see mitll.langtest.client.custom.CreateListDialog#doCreate
   * @param userid
   * @param name
   * @param description
   * @param dliClass
   * @param isPublic
   * @return
   */
  @Override
  public long addUserList(long userid, String name, String description, String dliClass, boolean isPublic) {
    return db.getUserListManager().addUserList(userid, name, description, dliClass, isPublic);
  }

  /**
   * @see mitll.langtest.client.custom.Navigation#setPublic(long, boolean)
   * @param userListID
   * @param isPublic
   */
  @Override
  public void setPublicOnList(long userListID, boolean isPublic) {
    db.getUserListManager().setPublicOnList(userListID, isPublic);
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

  public void markState(String id, STATE state, long creatorID) {
    db.getUserListManager().markState(id, state, creatorID);
  }

  /**
   * @see mitll.langtest.client.custom.MyFlashcardExercisePanelFactory.StatsPracticePanel#getRepeatButton()
   * @param ids
   */
  @Override
  public void setAVPSkip(Collection<Long> ids) { db.getAnswerDAO().changeType(ids); }

  /**
   * @see mitll.langtest.client.custom.ReviewEditableExercise#doAfterEditComplete(mitll.langtest.client.list.ListInterface, boolean)
   * @param id
   * @param state
   * @param userID
   */
  @Override
  public void setExerciseState(String id, STATE state, long userID) {
    db.getUserListManager().markState(id, state, userID);
  }

  /**
   * @see mitll.langtest.client.custom.Navigation#viewReview
   * @return
   */
  @Override
  public List<UserList> getReviewLists() {
    //logger.debug("asking for review lists --- ");
    List<UserList> lists = new ArrayList<UserList>();
    UserListManager userListManager = db.getUserListManager();
    UserList defectList = userListManager.getDefectList(getTypeOrder());
    lists.add(defectList);

    lists.add(userListManager.getCommentedList(getTypeOrder()));
    if (!serverProps.isNoModel()) {
      lists.add(userListManager.getAttentionList(getTypeOrder()));
    }
    return lists;
  }

  @Override
  public boolean deleteList(long id) {
    return db.getUserListManager().deleteList(id);
  }

  @Override
  public boolean deleteItemFromList(long listid, String exid) {
    return db.getUserListManager().deleteItemFromList(listid, exid, getTypeOrder());
  }

  /**
   * Can't check if it's valid if we don't have a model.
   * @see mitll.langtest.client.custom.NewUserExercise#isValidForeignPhrase(mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, boolean)
   * @param foreign
   * @return
   */
  @Override
  public boolean isValidForeignPhrase(String foreign) {
    boolean b = audioFileHelper.checkLTS(foreign);
/*    logger.debug("'" +foreign +
      "' is valid phrase = "+b);*/
    return b;
  }

  /**
   * Put the new item in the database,
   * copy the audio under bestAudio
   * assign the item to a user list
   * @see mitll.langtest.client.custom.NewUserExercise#afterValidForeignPhrase
   * @param userListID
   * @param userExercise
   */
  public UserExercise reallyCreateNewItem(long userListID, UserExercise userExercise) {
    db.getUserListManager().reallyCreateNewItem(userListID, userExercise, serverProps.getMediaDir());

    for (AudioAttribute audioAttribute : userExercise.getAudioAttributes()) {
      logger.debug("\treallyCreateNewItem : update " + audioAttribute + " to " + userExercise.getID());
      db.getAudioDAO().updateExerciseID(audioAttribute.getUniqueID(), userExercise.getID());
    }
    logger.debug("reallyCreateNewItem : made user exercise " + userExercise);

    return userExercise;
  }

  /**
   * @see mitll.langtest.client.custom.ReviewEditableExercise#duplicateExercise
   * @param exercise
   * @return
   */
  @Override
  public UserExercise duplicateExercise(UserExercise exercise) { return db.duplicateExercise(exercise);  }

  /**
   * @see mitll.langtest.client.custom.ReviewEditableExercise#deleteItem(String, long, mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.PagingExerciseList, mitll.langtest.client.list.PagingExerciseList)
   * @param id
   * @return
   */
  public boolean deleteItem(String id ) {
    boolean b = db.deleteItem(id);
    if (b) {
      fullTrie = null; // force rebuild of full trie
    }
    return b;
  }

  @Override
  public void logEvent(String id, String widgetType, String exid, String context, long userid, String hitID) {
    try {
      db.logEvent(id, widgetType, exid, context, userid, hitID);
    } catch (Exception e) {
      logger.error("got " +e,e);
    }
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
   * @see mitll.langtest.client.custom.ReviewEditableExercise#getPanelForAudio(mitll.langtest.shared.CommonExercise, mitll.langtest.shared.AudioAttribute, mitll.langtest.client.custom.RememberTabAndContent)
   * @param audioAttribute
   * @param exid
   */
  @Override
  public void markAudioDefect(AudioAttribute audioAttribute, String exid) {
    logger.debug("markAudioDefect mark audio defect for " + exid + " on " + audioAttribute);

    //CommonExercise before = db.getCustomOrPredefExercise(exid);  // allow custom items to mask out non-custom items
    //int beforeNumAudio = before.getAudioAttributes().size();
    db.markAudioDefect(audioAttribute);

    CommonExercise byID = db.getCustomOrPredefExercise(exid);  // allow custom items to mask out non-custom items

    if (!byID.removeAudio(audioAttribute)) {
      String key = audioAttribute.getKey();
      logger.warn("huh? couldn't remove key '" + key +
        "' : " + audioAttribute + " from " + exid +
        " keys were " + byID.getAudioRefToAttr().keySet() + " contains " + byID.getAudioRefToAttr().containsKey(key));
    }
   /*   int afterNumAudio = byID.getAudioAttributes().size();
    if (afterNumAudio != beforeNumAudio - 1) {
      logger.error("\thuh? before there were " + beforeNumAudio + " but after there were " + afterNumAudio);
    }*/
  }

  /**
   * @see mitll.langtest.client.custom.QCNPFExercise#getGenderGroup(mitll.langtest.client.custom.RememberTabAndContent, mitll.langtest.shared.AudioAttribute, com.github.gwtbootstrap.client.ui.Button)
   * @param attr
   * @param isMale
   */
  @Override
  public void markGender(AudioAttribute attr, boolean isMale) {
    db.getAudioDAO().addOrUpdateUser(isMale ? UserDAO.DEFAULT_MALE_ID : UserDAO.DEFAULT_FEMALE_ID, attr);
  }

  /**
   * @param age
   * @param gender
   * @param experience
   * @param nativeLang
   * @param dialect
   * @param userID
   * @param permissions
   * @return
   */
  @Override
  public long addUser(int age, String gender, int experience,
                      String nativeLang, String dialect, String userID, Collection<User.Permission> permissions) {
    //logger.debug("Adding user " + userID);
    return db.addUser(getThreadLocalRequest(),age, gender, experience, nativeLang, dialect, userID, permissions);
  }

  /**
   * @see mitll.langtest.client.user.UserTable#showDialog(mitll.langtest.client.LangTestDatabaseAsync)
   * @return
   */
  public List<User> getUsers() { return db.getUsers();  }
  @Override
  public User getUserBy(long id) {
    return db.getUserDAO().getUserWhere(id);
  }

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
   * A side effect is to set the first state to UNSET if it was APPROVED
   * and to set the second state (not really used right now) to RECORDED
   *
   * Client references:
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#stopRecording()
   * @see mitll.langtest.client.recorder.RecordButtonPanel#stopRecording()
   * @param base64EncodedString generated by flash on the client
   * @param plan which set of exercises
   * @param exercise exercise within the plan
   * @param questionID question within the exercise
   * @param user who is answering the question
   * @param reqid request id from the client, so it can potentially throw away out of order responses
   * @param flq was the prompt a foreign language query
   * @param audioType regular or fast then slow audio recording
   * @param doFlashcard true if called from practice (flashcard) and we want to do decode and not align
   * @param recordInResults if true, record in results table -- only when recording in a learn or practice tab
   * @param addToAudioTable if true, add to audio table -- only when recording reference audio for an item.
   * @param recordedWithFlash mark if we recorded it using flash recorder or webrtc
   * @return AudioAnswer object with information about the audio on the server, including if audio is valid (not too short, etc.)
   */
  @Override
  public AudioAnswer writeAudioFile(String base64EncodedString, String plan, String exercise, int questionID,
                                    int user, int reqid, boolean flq, String audioType, boolean doFlashcard,
                                    boolean recordInResults, boolean addToAudioTable, boolean recordedWithFlash) {
    CommonExercise exercise1 = getExercise(exercise, user); // NOTE : this could be null if we're posting audio against a new user exercise
    AudioAnswer audioAnswer = audioFileHelper.writeAudioFile(base64EncodedString, plan, exercise, exercise1, questionID, user, reqid,
      flq, audioType, doFlashcard, recordInResults, recordedWithFlash);

    if (addToAudioTable && audioAnswer.isValid()) {
      AudioAttribute attribute = addToAudioTable(user, audioType, exercise1, exercise, audioAnswer);
      audioAnswer.setAudioAttribute(attribute);
    }
    if (!audioAnswer.isValid() && audioAnswer.getDurationInMillis() == 0) {
      logger.warn("huh? got zero length recording " + user + " " + exercise);
      logEvent("audioRecording", "writeAudioFile", exercise, "Writing audio - got zero duration!", user, "unknown");
    }
    return audioAnswer;
  }

  @Override
  public AudioAnswer getAlignment(String base64EncodedString,
                                  String textToAlign,
                                  String identifier,
                                  int reqid) {
    AudioAnswer audioAnswer = audioFileHelper.getAlignment(base64EncodedString, textToAlign, identifier, reqid);

    if (!audioAnswer.isValid() && audioAnswer.getDurationInMillis() == 0) {
      logger.warn("huh? got zero length recording " + identifier);
      logEvent("audioRecording", "writeAudioFile", identifier, "Writing audio - got zero duration!", -1, "unknown");
    }
    return audioAnswer;
  }

  /**
   * Remember this audio as reference audio for this exercise, and possibly clear the APRROVED (inspected) state
   * on the exercise indicating it needs to be inspected again (we've added new audio).
   *
   * @param user who recorded audio
   * @param audioType regular or slow
   * @param exercise1 for which exercise
   * @param audioAnswer holds the path of the temporary recorded file
   * @return AudioAttribute that represents the audio that has been added to the exercise
   */
  private AudioAttribute addToAudioTable(int user, String audioType, CommonExercise exercise1, String exerciseID, AudioAnswer audioAnswer) {
    String exercise = exercise1 == null ? exerciseID : exercise1.getID();
    File fileRef = pathHelper.getAbsoluteFile(audioAnswer.getPath());
    String destFileName = audioType + "_" + System.currentTimeMillis() + "_by_" + user + ".wav";
    String permanentAudioPath = new PathWriter().getPermanentAudioPath(pathHelper, fileRef, destFileName, true, exercise);
    AudioAttribute audioAttribute =
      db.getAudioDAO().addOrUpdate(user, permanentAudioPath, exercise, System.currentTimeMillis(), audioType, audioAnswer.getDurationInMillis());
    //logger.debug("writeAudioFile for " + audioType + " audio answer has " + audioAttribute);

    // what state should we mark recorded audio?
    setExerciseState(exercise, user, exercise1);
    return audioAttribute;
  }

  /**
   * Only change APPROVED to UNSET.
   * @param exercise
   * @param user
   * @param exercise1
   */
  private void setExerciseState(String exercise, int user, CommonExercise exercise1) {
    if (exercise1 != null) {
      STATE currentState = db.getUserListManager().getCurrentState(exercise);
      if (currentState == STATE.APPROVED) { // clear approved on new audio -- we need to review it again
        db.getUserListManager().setState(exercise1, STATE.UNSET, user);
      }
      db.getUserListManager().setSecondState(exercise1, STATE.RECORDED, user);
    }
  }

  void makeAutoCRT() { audioFileHelper.makeAutoCRT(relativeConfigDir, this); }

  @Override
  public Map<User, Integer> getUserToResultCount() { return db.getUserToResultCount();  }
  @Override
  public Map<Integer, Integer> getResultCountToCount() { return db.getResultCountToCount();  }
  @Override
  public Map<String, Integer> getResultByDay() {  return db.getResultByDay();  }
  @Override
  public Map<String, Integer> getResultByHourOfDay() {
    return db.getResultByHourOfDay();
  }

  @Override
  public Map<String, Float> getMaleFemaleProgress() {
    List<CommonExercise> exercises = getExercises();
    Map<String, Float> report = new HashMap<String, Float>();

    float total = exercises.size();
    float male = 0;
    float female = 0;
    float maleFast = 0;
    float maleSlow = 0;

    float femaleFast = 0;
    float femaleSlow = 0;
    for (CommonExercise exercise : exercises) {
      Collection<AudioAttribute> males   = exercise.getByGender(true);
      Collection<AudioAttribute> females = exercise.getByGender(false);

      if (!males.isEmpty()) male++;
      if (!females.isEmpty()) female++;
      AudioAttribute r = null, s = null;
      for (AudioAttribute audioAttribute : males) {
        if (audioAttribute.isRegularSpeed()) r = audioAttribute;
        if (audioAttribute.isSlow()) s = audioAttribute;
      }
      if (r != null) maleFast++;
      if (s != null) maleSlow++;

      r = null;
      s = null;
      for (AudioAttribute audioAttribute : females) {
        if (audioAttribute.isRegularSpeed()) r = audioAttribute;
        if (audioAttribute.isSlow()) s = audioAttribute;
      }
      if (r != null) femaleFast++;
      if (s != null) femaleSlow++;
    }
    report.put("total", total);
    report.put("male", male);
    report.put("female", female);
    report.put("maleFast", maleFast);
    report.put("maleSlow", maleSlow);
    report.put("femaleFast", femaleFast);
    report.put("femaleSlow", femaleSlow);
    return report;

  }

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
    //logger.debug("getUserHistoryForList " + userid + " and " + ids);

    return db.getUserHistoryForList(userid, ids, latestResultID);
  }

  public void logMessage(String message) {
    String prefixedMessage = "for " + pathHelper.getInstallPath() + " from client " + message;
    logger.debug(prefixedMessage);

    if (message.startsWith("got browser exception")) {
      getMailSupport().email(serverProps.getEmailAddress(), "Javascript Exception", prefixedMessage);
    }
  }

  private MailSupport getMailSupport() {  return new MailSupport(serverProps.isDebugEMail());  }

  @Override
  public void destroy() {
    super.destroy();
    db.destroy();
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
      db.preloadExercises();
    }

    db.getUserListManager().setStateOnExercises();
  }

  /**
   * The config web.xml file.
   * As a final step, creates the DatabaseImpl!<br></br>
   *
   *  NOTE : makes the database available to other servlets via the databaseReference servlet context attribute.
   * Note that this will only ever be called once.
   * @see #init()
   * @param servletContext
   */
  private void readProperties(ServletContext servletContext) {
    String config = servletContext.getInitParameter("config");
    this.relativeConfigDir = "config" + File.separator + config;
    this.configDir = pathHelper.getInstallPath() + File.separator + relativeConfigDir;


    pathHelper.setConfigDir(configDir);

    serverProps = new ServerProperties(servletContext, configDir);
    String h2DatabaseFile = serverProps.getH2Database();

    db = makeDatabaseImpl(h2DatabaseFile);
    Object databaseReference = servletContext.getAttribute("databaseReference");
    if (databaseReference != null) {
      logger.warn("huh? found existing database reference " + databaseReference);
    } else {
      servletContext.setAttribute("databaseReference", db);
    }
  }

  private DatabaseImpl makeDatabaseImpl(String h2DatabaseFile) {
    //logger.debug("word pairs " +  serverProps.isWordPairs() + " language " + serverProps.getLanguage() + " config dir " + relativeConfigDir);
    return new DatabaseImpl(configDir, relativeConfigDir, h2DatabaseFile, serverProps, pathHelper, true);
  }

  /**
   * @see LangTestDatabaseImpl#init()
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
