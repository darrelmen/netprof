package mitll.langtest.server;

import audio.image.ImageType;
import audio.imagewriter.ImageWriter;
import com.google.common.io.Files;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.audio.PathWriter;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.mail.EmailHelper;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.server.scoring.AutoCRTScoring;
import mitll.langtest.server.trie.TextEntityValue;
import mitll.langtest.server.trie.Trie;
import mitll.langtest.shared.*;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.flashcard.AVPScoreReport;
import mitll.langtest.shared.instrumentation.Event;
import mitll.langtest.shared.monitoring.Session;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Supports all the database interactions.
 * User: GO22670
 * Date: 5/7/12
 * Time: 5:49 PM
 */
@SuppressWarnings("serial")
public class LangTestDatabaseImpl extends RemoteServiceServlet implements LangTestDatabase, AutoCRTScoring, LogAndNotify, LoadTesting {
  private static final Logger logger = Logger.getLogger(LangTestDatabaseImpl.class);
  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";
  public static final String DATABASE_REFERENCE = "databaseReference";
  private static final int SLOW_EXERCISE_EMAIL = 2000;
  public static final String ENGLISH = "English";
  public static final int MAX = 30;

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

  @Override
  public void logAndNotifyServerException(Exception e) {
    String message1 = e == null ? "null_ex" : e.getMessage() == null ? "null_msg" : e.getMessage();
    if (!message1.contains("Broken Pipe")) {
      String message = "Server Exception : " + ExceptionUtils.getStackTrace(e);
      String prefixedMessage = "for " + pathHelper.getInstallPath() + " got " + message;
      String subject = "Server Exception on " + pathHelper.getInstallPath();
      sendEmail(subject, prefixedMessage);

      logger.debug(prefixedMessage);
    }
  }

  public void sendEmail(String subject, String prefixedMessage) {
    getMailSupport().email(serverProps.getEmailAddress(), subject, prefixedMessage);
  }

  private List<CommonShell> getExerciseShells(Collection<? extends CommonExercise> exercises) {
    return serverProps.getLanguage().equals(ENGLISH) ? getExerciseShellsShort(exercises) : getExerciseShellsCombined(exercises);
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
   * <p/>
   * Get exercise ids, either from the predefined set or a user list.
   * Take the result and if there's a unit-chapter filter, use that to return only the exercises in the selected
   * units/chapters.
   * Further optionally filter by string prefix on each item's english or f.l.
   * <p/>
   * Supports lookup by id
   * <p/>
   * Marks items with state/second state given user id. User is used to mark the audio in items with whether they have
   * been played by that user.
   * <p/>
   * Uses role to determine if we're in recorder mode and marks items recorded by the user as RECORDED.
   * <p/>
   * Sorts the result by unit, then chapter, then alphabetically in chapter. If role is recorder, put the recorded
   * items at the front.
   *
   * @param reqID
   * @param typeToSelection
   * @param prefix
   * @param userListID
   * @param userID
   * @param role
   * @param onlyUnrecordedByMe
   * @param onlyExamples
   * @param incorrectFirstOrder
   * @return
   * @see mitll.langtest.client.list.PagingExerciseList#loadExercises(String, String)
   */
  @Override
  public ExerciseListWrapper getExerciseIds(int reqID, Map<String, Collection<String>> typeToSelection, String prefix,
                                            long userListID, int userID, String role, boolean onlyUnrecordedByMe,
                                            boolean onlyExamples, boolean incorrectFirstOrder) {
    Collection<CommonExercise> exercises;

    logger.debug("getExerciseIds : (" + serverProps.getLanguage()+ ") " +
        "getting exercise ids for " +
        " config " + relativeConfigDir +
        " prefix '" + prefix +
        "' and user list id " + userListID + " user " + userID + " role " + role +
        " filter " + onlyUnrecordedByMe + " only examples " + onlyExamples);

    try {
      UserList userListByID = userListID != -1 ? db.getUserListByID(userListID) : null;

      if (typeToSelection.isEmpty()) {   // no unit-chapter filtering
        // get initial exercise set, either from a user list or predefined
        boolean predefExercises = userListByID == null;
        exercises = predefExercises ? getExercises() : getCommonExercises(userListByID);

        // now if there's a prefix, filter by prefix match
        if (!prefix.isEmpty()) {
          // now do a trie over matches
          exercises = getExercisesForSearch(prefix, userID, exercises, predefExercises);
        }
        exercises = filterByUnrecorded(userID, onlyUnrecordedByMe, onlyExamples, exercises);
        int i = markRecordedState(userID, role, exercises, onlyExamples);
        //logger.debug("marked " +i + " as recorded");

        // now sort : everything gets sorted the same way
        List<CommonExercise> commonExercises;
        if (incorrectFirstOrder) {
          commonExercises = db.getResultDAO().getExercisesSortedIncorrectFirst(exercises, userID);
        } else {
          commonExercises = new ArrayList<CommonExercise>(exercises);
          new ExerciseSorter(getTypeOrder()).getSortedByUnitThenAlpha(commonExercises, role.equals(Result.AUDIO_TYPE_RECORDER));
        }

        return makeExerciseListWrapper(reqID, commonExercises, userID, role, onlyExamples, incorrectFirstOrder);
      } else { // sort by unit-chapter selection
        // builds unit-lesson hierarchy if non-empty type->selection over user list
        if (userListByID != null) {
          Collection<CommonExercise> exercisesForState = getExercisesFromFiltered(typeToSelection, userListByID);
          exercisesForState = filterByUnrecorded(userID, onlyUnrecordedByMe, onlyExamples, exercisesForState);

          return getExerciseListWrapperForPrefix(reqID, prefix, exercisesForState, userID, role, onlyExamples, incorrectFirstOrder);
        } else {
          return getExercisesForSelectionState(reqID, typeToSelection, prefix, userID, role, onlyUnrecordedByMe, onlyExamples, incorrectFirstOrder);
        }
      }
    } catch (Exception e) {
      logger.warn("got " + e, e);
      logAndNotifyServerException(e);
      return new ExerciseListWrapper();
    }
  }

  protected Collection<CommonExercise> getExercisesForSearch(String prefix, int userID, Collection<CommonExercise> exercises, boolean predefExercises) {
    long then = System.currentTimeMillis();
    ExerciseTrie trie = predefExercises ? fullTrie : new ExerciseTrie(exercises, serverProps.getLanguage(), audioFileHelper.getSmallVocabDecoder());
    exercises = trie.getExercises(prefix);
    long now = System.currentTimeMillis();
    if (now - then > 300) {
      logger.debug("took " + (now - then) + " millis to do trie lookup");
    }
    if (exercises.isEmpty()) { // allow lookup by id
      CommonExercise exercise = getExercise(prefix, userID, false);
      if (exercise != null) exercises = Collections.singletonList(exercise);
    }
    return exercises;
  }

  /**
   * For all the exercises the user has not recorded, do they have the required reg and slow speed recordings by a matching gender.
   * <p/>
   * Or if looking for example audio, find ones missing examples.
   *
   * @param userID             exercise not recorded by this user and matching the user's gender
   * @param onlyUnrecordedByMe do we filter
   * @param onlyExamples       only example audio
   * @param exercises          to filter
   * @return exercises missing audio, what we want to record
   */
  private Collection<CommonExercise> filterByUnrecorded(long userID, boolean onlyUnrecordedByMe, boolean onlyExamples,
                                                        Collection<CommonExercise> exercises) {
    if (onlyUnrecordedByMe) {
      Set<String> recordedForUser = db.getAudioDAO().getRecordedBy(userID);
      boolean isMale = db.getUserDAO().isMale(userID);
      List<CommonExercise> copy = new ArrayList<CommonExercise>();
     // logger.debug("recorded already " + recordedForUser.size() + " checking " + exercises.size());
      // filter
      for (CommonExercise exercise : exercises) {
        if (!recordedForUser.contains(exercise.getID())) {
          copy.add(exercise);
        } else {
          Collection<AudioAttribute> byGender = exercise.getByGender(isMale);
          boolean hasReg = false;
          boolean hasSlow = false;
          boolean hasExample = false;
 /*         if (!byGender.isEmpty()) logger.debug("checking " + isMale + " ex " + exercise.getID()+
            " has " + byGender.size() + " recordings");*/
          for (AudioAttribute attr : byGender) {
            if (attr.getUserid() != UserDAO.DEFAULT_USER_ID) {
              hasReg = hasReg || attr.isRegularSpeed();
              hasSlow = hasSlow || attr.isSlow();
              hasExample = attr.isExampleSentence();

              if (onlyExamples) {
                if (hasExample) break;
              } else if (hasReg && hasSlow) {
                break;
              }
            }
          }

          if (onlyExamples) {
            if (!hasExample) {
              copy.add(exercise);
            }
          } else if (!hasReg || !hasSlow) {
            copy.add(exercise);
          }
        }
      }
    //  logger.debug("to be recorded " + copy.size() + " from " + exercises.size());

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
    // logger.debug("\tafter found " + exercisesForState.size() + " matches to " + typeToSelection);
    return exercisesForState;
  }

  /**
   * Marks each exercise - first state - with whether this user has recorded audio for this item
   * Defective audio is not included.
   * Also if just one of regular or slow is recorded it's not "recorded".
   * <p/>
   * What you want to see in the record audio tab.  One bit of info - recorded or not recorded.
   *
   * @param userID
   * @param role
   * @param exercises
   * @param onlyExample
   * @return
   * @see #getExerciseIds
   */
  private int markRecordedState(int userID, String role, Collection<? extends CommonShell> exercises, boolean onlyExample) {
    int c = 0;
    if (role.equals(Result.AUDIO_TYPE_RECORDER)) {
      Set<String> recordedForUser = onlyExample ? db.getAudioDAO().getRecordedExampleForUser(userID) : db.getAudioDAO().getRecordedForUser(userID);
      //logger.debug("\tfound " + recordedForUser.size() + " recordings by " + userID + " only example " + onlyExample);
      for (CommonShell shell : exercises) {
        if (recordedForUser.contains(shell.getID())) {
          shell.setState(STATE.RECORDED);
          c++;
        }
      }
    }
    //else {
    //logger.debug("\tnot marking recorded for '" + role + "' and user " + userID);
    //}
    return c;
  }

  /**
   * Copies the exercises....?
   *
   * @param userListByID
   * @return
   * @see #getExerciseIds
   * @see #getExercisesFromFiltered(java.util.Map, mitll.langtest.shared.custom.UserList)
   */
  private List<CommonExercise> getCommonExercises(UserList userListByID) {
    return new ArrayList<CommonExercise>(userListByID.getExercises());
  }

  /**
   * @param reqID
   * @param typeToSection
   * @param prefix
   * @param userID
   * @param role
   * @param onlyUnrecordedByMe
   * @param onlyExamples
   * @param incorrectFirst
   * @return
   * @see mitll.langtest.client.list.HistoryExerciseList#loadExercises(String, String)
   * @see #getExerciseIds
   */
  private ExerciseListWrapper getExercisesForSelectionState(int reqID,
                                                            Map<String, Collection<String>> typeToSection, String prefix,
                                                            long userID, String role, boolean onlyUnrecordedByMe,
                                                            boolean onlyExamples, boolean incorrectFirst) {
    Collection<CommonExercise> exercisesForState = db.getSectionHelper().getExercisesForSelectionState(typeToSection);
    exercisesForState = filterByUnrecorded(userID, onlyUnrecordedByMe, onlyExamples, exercisesForState);

    return getExerciseListWrapperForPrefix(reqID, prefix, exercisesForState, userID, role, onlyExamples, incorrectFirst);
  }

  /**
   * Always sort the result
   *
   * @param reqID
   * @param prefix
   * @param exercisesForState
   * @param userID
   * @param role
   * @param onlyExamples
   * @param incorrectFirst
   * @return
   * @see #getExerciseIds
   */
  private ExerciseListWrapper getExerciseListWrapperForPrefix(int reqID, String prefix,
                                                              Collection<CommonExercise> exercisesForState, long userID,
                                                              String role, boolean onlyExamples, boolean incorrectFirst) {
    boolean hasPrefix = !prefix.isEmpty();
    if (hasPrefix) {
      logger.debug("getExerciseListWrapperForPrefix userID " + userID + " prefix '" + prefix + "' role " + role);
    }

    int i = markRecordedState((int) userID, role, exercisesForState, onlyExamples);
    //logger.debug("marked " +i + " as recorded role " +role);

    if (hasPrefix) {
      ExerciseTrie trie = new ExerciseTrie(exercisesForState, serverProps.getLanguage(), audioFileHelper.getSmallVocabDecoder());
      exercisesForState = trie.getExercises(prefix);
    }

    if (exercisesForState.isEmpty()) { // allow lookup by id
      CommonExercise exercise = getExercise(prefix, userID, incorrectFirst);
      if (exercise != null) exercisesForState = Collections.singletonList(exercise);
    }
    // why copy???
    List<CommonExercise> copy;

    if (incorrectFirst) {
      copy = db.getResultDAO().getExercisesSortedIncorrectFirst(exercisesForState, userID);
    } else {
      copy = new ArrayList<CommonExercise>(exercisesForState);

      new ExerciseSorter(getTypeOrder()).getSortedByUnitThenAlpha(copy, role.equals(Result.AUDIO_TYPE_RECORDER));
    }

    return makeExerciseListWrapper(reqID, copy, userID, role, onlyExamples, incorrectFirst);
  }

  /**
   * Send the first exercise along so we don't have to ask for it after we get the initial list
   *
   * @param reqID
   * @param exercises
   * @param userID
   * @param role
   * @param onlyExamples
   * @param isFlashcardReq
   * @return
   * @see #getExerciseIds
   * @see #getExerciseListWrapperForPrefix(int, String, java.util.Collection, long, String, boolean, boolean)
   */
  private ExerciseListWrapper makeExerciseListWrapper(int reqID, Collection<CommonExercise> exercises, long userID,
                                                      String role, boolean onlyExamples, boolean isFlashcardReq) {
    CommonExercise firstExercise = exercises.isEmpty() ? null : exercises.iterator().next();
    if (firstExercise != null) {
      addAnnotationsAndAudio(userID, firstExercise, isFlashcardReq);
      ensureMP3s(firstExercise);
    }
    List<CommonShell> exerciseShells = getExerciseShells(exercises);

    //   logger.debug("makeExerciseListWrapper : userID " +userID + " Role is " + role);
    if (role.equals(Result.AUDIO_TYPE_RECORDER)) {
      markRecordedState((int) userID, role, exerciseShells, onlyExamples);
    } else if (role.equalsIgnoreCase(User.Permission.QUALITY_CONTROL.toString()) || role.startsWith(Result.AUDIO_TYPE_REVIEW)) {
      db.getUserListManager().markState(exerciseShells);
    }

    ExerciseListWrapper exerciseListWrapper = new ExerciseListWrapper(reqID, exerciseShells, firstExercise);
    //logger.debug("returning " + exerciseListWrapper);
    return exerciseListWrapper;
  }

  /**
   * 0) Add annotations to fields on exercise
   * 1) Attach audio recordings to exercise.
   * 2) Adds information about whether the audio has been played or not...
   * 3) Attach history info (when has the user recorded audio for the item under the learn tab and gotten a score)
   *
   * @param userID
   * @param firstExercise
   * @param isFlashcardReq
   * @see LoadTesting#getExercise(String, long, boolean)
   * @see #makeExerciseListWrapper(int, java.util.Collection, long, String, boolean, boolean)
   */
  private void addAnnotationsAndAudio(long userID, CommonExercise firstExercise, boolean isFlashcardReq) {
    long then = System.currentTimeMillis();

    addAnnotations(firstExercise); // todo do this in a better way
    long now = System.currentTimeMillis();
    if (now - then > 40) {
      logger.debug("addAnnotationsAndAudio : (" + serverProps.getLanguage() + ") took " + (now - then) + " millis to add annotations to exercise " + firstExercise.getID());
    }
    then = now;
    attachAudio(firstExercise);

    //for (AudioAttribute audioAttribute : firstExercise.getAudioAttributes()) logger.debug("\t addAnnotationsAndAudio ex " + firstExercise.getID()+ " audio " + audioAttribute);

    now = System.currentTimeMillis();
    if (now - then > 40) {
      logger.debug("addAnnotationsAndAudio : (" + serverProps.getLanguage() + ") took " + (now - then) + " millis to attach audio to exercise " + firstExercise.getID());
    }
    then = now;

    User userWhere = db.getUserDAO().getUserWhere(userID);
    if (userWhere != null && userWhere.getPermissions().contains(User.Permission.QUALITY_CONTROL)) {
      addPlayedMarkings(userID, firstExercise);
      now = System.currentTimeMillis();
      if (now - then > 40) {
        logger.debug("addAnnotationsAndAudio : (" + serverProps.getLanguage() + ") took " + (now - then) + " millis to add played markings to exercise " + firstExercise.getID());
      }
    }

    then = now;

    attachScoreHistory(userID, firstExercise, isFlashcardReq);

    now = System.currentTimeMillis();
    if (now - then > 40) {
      logger.debug("addAnnotationsAndAudio : (" + serverProps.getLanguage() + ") took " + (now - then) + " millis to attach score history to exercise " + firstExercise.getID());
    }

   // for (AudioAttribute audioAttribute : firstExercise.getAudioAttributes()) logger.debug("\t addAnnotationsAndAudio ret ex " + firstExercise.getID()+ " audio " + audioAttribute);

  }

  /**
   * @param userID
   * @param firstExercise
   * @param isFlashcardReq
   * @see #addAnnotationsAndAudio(long, mitll.langtest.shared.CommonExercise, boolean)
   */
  private void attachScoreHistory(long userID, CommonExercise firstExercise, boolean isFlashcardReq) {
    db.getResultDAO().attachScoreHistory(userID, firstExercise, isFlashcardReq);
  }

  /**
   * @param firstExercise
   * @see #addAnnotationsAndAudio(long, mitll.langtest.shared.CommonExercise, boolean)
   */
  private void attachAudio(CommonExercise firstExercise) {
    db.getAudioDAO().attachAudio(firstExercise, pathHelper.getInstallPath(), relativeConfigDir);
  }

  /**
   * Only add the played markings if doing QC.
   * <p/>
   * TODO : This is an expensive query - we need a smarter way of remembering when audio has been played.
   *
   * @param userID
   * @param firstExercise
   * @see #addAnnotationsAndAudio(long, mitll.langtest.shared.CommonExercise, boolean)
   */
  private void addPlayedMarkings(long userID, CommonExercise firstExercise) {
    db.getEventDAO().addPlayedMarkings(userID, firstExercise);
  }

  /**
   * @return
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#getTypeOrder(com.github.gwtbootstrap.client.ui.FluidContainer)
   */
  private Collection<String> getTypeOrder() {
    SectionHelper sectionHelper = db.getSectionHelper();
    if (sectionHelper == null) logger.warn("no section helper for " + db);
    List<String> objects = Collections.emptyList();
    return (sectionHelper == null) ? objects : sectionHelper.getTypeOrder();
  }

  /**
   * @return
   * @see #getStartupInfo()
   */
  private List<SectionNode> getSectionNodes() {
    return db.getSectionHelper().getSectionNodes();
  }

  @Override
  public CommonExercise getRandomExercise() {
    List<User> users = db.getUserDAO().getUsers();
    if (users.isEmpty()) return null;

    Random rand = new Random();
    User user = users.get(rand.nextInt(users.size()));

    ExerciseListWrapper exerciseIDs = getExerciseIDs((int) user.getId());

    List<CommonShell> exercises = exerciseIDs.getExercises();

    CommonShell shell = exercises.get(rand.nextInt(exercises.size()));

    return getExercise(shell.getID(), user.getId(), false);
  }

  @Override
  public CommonExercise getFirstExercise() {
    List<User> users = db.getUserDAO().getUsers();

    if (users.isEmpty()) return null;
    User user = users.get(0);

    ExerciseListWrapper exerciseIDs = getExerciseIDs((int) user.getId());

    List<CommonShell> exercises = exerciseIDs.getExercises();

    CommonShell shell = exercises.get(0);

    return getExercise(shell.getID(), user.getId(), false);
  }

  private ExerciseListWrapper getExerciseIDs(int userID) {
    Map<String, Collection<String>> objectObjectMap = Collections.emptyMap();
    return getExerciseIds(0, objectObjectMap, "", -1, userID, "", false, false, false);
  }

  /**
   * Joins with annotation data when doing QC.
   *
   * @param id
   * @param userID
   * @param isFlashcardReq
   * @return
   * @see mitll.langtest.client.list.ExerciseList#askServerForExercise
   */
  public CommonExercise getExercise(String id, long userID, boolean isFlashcardReq) {
    long then = System.currentTimeMillis();
    List<CommonExercise> exercises = getExercises();

    long then2 = System.currentTimeMillis();

    CommonExercise byID = db.getCustomOrPredefExercise(id);  // allow custom items to mask out non-custom items

    long now = System.currentTimeMillis();
    String language = serverProps.getLanguage();
    if (now - then2 > 100) {
      logger.debug("getExercise : (" + language + ") took " + (now - then2) + " millis to find exercise " + id);
    }

    if (byID == null) {
      logger.error("getExercise : huh? couldn't find exercise with id '" + id + "' when examining " + exercises.size() + " items");
    } else {
      then2 = System.currentTimeMillis();
      addAnnotationsAndAudio(userID, byID, isFlashcardReq);
      now = System.currentTimeMillis();
      if (now - then2 > 100) {
        logger.debug("getExercise : (" + language + ") took " + (now - then2) + " millis to add annotations to exercise " + id);
      }
      then2 = System.currentTimeMillis();

      //logger.debug("getExercise : returning " + byID);
      ensureMP3s(byID);

     // for (AudioAttribute audioAttribute : byID.getAudioAttributes()) logger.debug("\t addAnnotationsAndAudio after ensure mp3 ex " + byID.getID()+ " audio " + audioAttribute);

      now = System.currentTimeMillis();
      if (now - then2 > 100) {
        logger.debug("getExercise : (" + language + ") took " + (now - then2) + " millis to ensure there are mp3s for exercise " + id);
      }
    }
    checkPerformance(id, then);

    if (byID != null) {
      //logger.debug("returning (" + language + ") exercise " + byID.getID());
    }
    else {
      logger.info("couldn't find exercise with id '" +id+  "'");
    }
    return byID;
  }

  public void checkPerformance(String id, long then) {
    long now;
    now = System.currentTimeMillis();
    long diff = now - then;
    String language = serverProps.getLanguage();

    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
    String threadInfo = threadGroup.getName() + " = " + threadGroup.activeCount();
    String message = "getExercise : (" + language + ") took " + diff + " millis to get exercise " + id + " : " + threadInfo;
    if (diff > SLOW_EXERCISE_EMAIL) {
      logger.error(message + " thread count " + threadInfo);
      sendEmailWhenSlow(id, language, diff, threadInfo);
    } else if (diff > 1000) {
      logger.warn(message);
    } else if (diff > 5) {
      logger.debug(message);
    }
  }

  protected void sendEmailWhenSlow(String id, String language, long diff, String threadInfo) {
    String hostName = null;
    try {
      hostName = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      logger.error("Got " + e, e);
    }
    sendEmail("slow exercise on " + language, "Getting ex " + id + " on " + language + " took " + diff +
        " millis, threads " + threadInfo + " on " + hostName);
  }

  /**
   * @param byID
   * @see #addAnnotationsAndAudio(long, mitll.langtest.shared.CommonExercise, boolean)
   */
  private void addAnnotations(CommonExercise byID) {
    db.getUserListManager().addAnnotations(byID);
  }

  /**
   * @param byID
   * @see LoadTesting#getExercise(String, long, boolean)
   * @see #makeExerciseListWrapper(int, java.util.Collection, long, String, boolean, boolean)
   */
  private void ensureMP3s(CommonExercise byID) {
    Collection<AudioAttribute> audioAttributes = byID.getAudioAttributes();
    for (AudioAttribute audioAttribute : audioAttributes) {
      if (!ensureMP3(audioAttribute.getAudioRef())) {
        audioAttribute.setAudioRef(AudioConversion.FILE_MISSING);
      }
    }

    if (audioAttributes.isEmpty()) {
      logger.warn("ensureMP3s : (" +serverProps.getLanguage() + ") no ref audio for " + byID);
    }
  }

  private ExerciseTrie fullTrie;

  /**
   * Called from the client:
   *
   * @return
   * @see mitll.langtest.client.list.ListInterface#getExercises
   */
  List<CommonExercise> getExercises() {
    long then = System.currentTimeMillis();
    List<CommonExercise> exercises = db.getExercises();
    makeAutoCRT();   // side effect of db.getExercises is to make the exercise DAO which is needed here...
    if (fullTrie == null) {
      fullTrie = new ExerciseTrie(exercises, serverProps.getLanguage(), audioFileHelper.getSmallVocabDecoder());
    }

    audioFileHelper.checkLTS(exercises);
    long now = System.currentTimeMillis();
    if (now - then > 200) {
      logger.info("took " + (now - then) + " millis to get the predef exercise list for " + serverProps.getLanguage());
    }
    return exercises;
  }

  /**
   *
   * @param wavFile
   * @return true if mp3 file exists
   * @see #ensureMP3s(mitll.langtest.shared.CommonExercise)
   */
  private boolean ensureMP3(String wavFile) {
    if (wavFile != null) {
      String parent = pathHelper.getInstallPath();

      AudioConversion audioConversion = new AudioConversion();
      if (!audioConversion.exists(wavFile, parent)) {
        logger.warn("can't find " + wavFile + " under " + parent + " trying config... ");
        parent = configDir;
      }
      if (!audioConversion.exists(wavFile, parent)) {
        logger.error("huh? can't find " + wavFile + " under " + parent);
      }
      String s = audioConversion.ensureWriteMP3(wavFile, parent, false);
      return !(s.equals(AudioConversion.FILE_MISSING));
    }
    return false;
  }

  /**
   * Get an image of desired dimensions for the audio file - only for Waveform and spectrogram.
   * Also returns the audio file duration -- so we can deal with the difference in length between mp3 and wav
   * versions of the same audio file.  (The browser soundmanager plays mp3 and reports audio offsets into
   * the mp3 file, but all the images are generated from the shorter wav file.)
   * <p/>
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
  public ImageResponse getImageForAudioFile(int reqid, String audioFile, String imageType, int width, int height, String exerciseID) {
    ImageWriter imageWriter = new ImageWriter();

    String wavAudioFile = getWavAudioFile(audioFile);
    File testFile = new File(wavAudioFile);
    if (!testFile.exists() || testFile.length() == 0) {
      if (testFile.length() == 0) logger.error("huh? " + wavAudioFile + " is empty???");
      return new ImageResponse();
    }
    ImageType imageType1 =
        imageType.equalsIgnoreCase(ImageType.WAVEFORM.toString()) ? ImageType.WAVEFORM :
            imageType.equalsIgnoreCase(ImageType.SPECTROGRAM.toString()) ? ImageType.SPECTROGRAM : null;
    if (imageType1 == null) return new ImageResponse(); // success = false!
    String imageOutDir = pathHelper.getImageOutDir();
    logger.debug("getImageForAudioFile : getting images (" + width + " x " + height + ") (" + reqid + ") type " + imageType +
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
   *
   * @return
   * @see mitll.langtest.client.LangTest#onModuleLoad
   */
  @Override
  public StartupInfo getStartupInfo() {
    return new StartupInfo(serverProps.getProperties(), getTypeOrder(), getSectionNodes());
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
   * @see mitll.langtest.client.scoring.ASRScoringAudioPanel#scoreAudio(String, long, String, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck, int, int, int)
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
   *
   * @param testAudioFile audio file to score
   * @param lmSentences   to look for in the audio
   * @return PretestScore for audio
   * @see mitll.langtest.server.autocrt.AutoCRT#getAutoCRTDecodeOutput
   * @see mitll.langtest.server.autocrt.AutoCRT#getFlashcardAnswer
   */
  public PretestScore getASRScoreForAudio(File testAudioFile, Collection<String> lmSentences) {
    return audioFileHelper.getASRScoreForAudio(testAudioFile, lmSentences);
  }

  /**
   * @param phrases
   * @return
   * @see mitll.langtest.server.autocrt.AutoCRT#getAutoCRTDecodeOutput
   */
  @Override
  public Collection<String> getValidPhrases(Collection<String> phrases) {
    return audioFileHelper.getValidPhrases(phrases);
  }

  // Answers ---------------------

  /**
   * @param userID
   * @param exercise
   * @param questionID
   * @param answer
   * @see mitll.langtest.client.exercise.ExercisePanel#postAnswers
   */
/*  public void addTextAnswer(int userID, CommonExercise exercise, int questionID, String answer, String answerType) {
    db.addAnswer(userID, exercise, questionID, answer, answerType);
  }*/

  // Users ---------------------

  /**
   * @param login
   * @param passwordH
   * @return
   * @see mitll.langtest.client.user.UserPassLogin#gotLogin
   * @see mitll.langtest.client.user.UserPassLogin#makeSignInUserName(com.github.gwtbootstrap.client.ui.Fieldset)
   */
  public User userExists(String login, String passwordH) {
    return db.getUserDAO().getUser(login, passwordH);
  }

  /**
   * @param userid
   * @param name
   * @param description
   * @param dliClass
   * @param isPublic
   * @return
   * @see mitll.langtest.client.custom.dialog.CreateListDialog#doCreate
   */
  @Override
  public long addUserList(long userid, String name, String description, String dliClass, boolean isPublic) {
    return db.getUserListManager().addUserList(userid, name, description, dliClass, isPublic);
  }

  /**
   * @param userListID
   * @param isPublic
   * @see mitll.langtest.client.custom.Navigation#setPublic(long, boolean)
   */
  @Override
  public void setPublicOnList(long userListID, boolean isPublic) {
    db.getUserListManager().setPublicOnList(userListID, isPublic);
  }

  /**
   * @param userListID
   * @param user
   * @see mitll.langtest.client.custom.Navigation#addVisitor(mitll.langtest.shared.custom.UserList)
   */
  public void addVisitor(long userListID, long user) {
    db.getUserListManager().addVisitor(userListID, user);
  }

  /**
   * @param userid
   * @param onlyCreated
   * @param visited
   * @return
   * @see mitll.langtest.client.custom.Navigation#showInitialState()
   * @see mitll.langtest.client.custom.Navigation#viewLessons
   * @see mitll.langtest.client.custom.exercise.NPFExercise#populateListChoices
   */
  public Collection<UserList> getListsForUser(long userid, boolean onlyCreated, boolean visited) {
    if (!onlyCreated && !visited) logger.error("huh? asking for neither your lists nor  your visited lists.");
    return db.getUserListManager().getListsForUser(userid, onlyCreated, visited);
  }

  /**
   * @param search
   * @param userid
   * @return
   * @see mitll.langtest.client.custom.Navigation#showInitialState()
   * @see mitll.langtest.client.custom.Navigation#viewLessons
   */
  @Override
  public Collection<UserList> getUserListsForText(String search, long userid) {
    return db.getUserListManager().getUserListsForText(search, userid);
  }

  /**
   * @param userListID
   * @param userExercise
   * @return
   * @see mitll.langtest.client.custom.exercise.NPFExercise#populateListChoices
   */
  public void addItemToUserList(long userListID, UserExercise userExercise) {
    db.getUserListManager().addItemToUserList(userListID, userExercise);
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
  public void addAnnotation(String exerciseID, String field, String status, String comment, long userID) {
    db.getUserListManager().addAnnotation(exerciseID, field, status, comment, userID);
  }

  /**
   * @param id
   * @param isCorrect
   * @param creatorID
   * @see mitll.langtest.client.qc.QCNPFExercise#markReviewed
   */
  public void markReviewed(String id, boolean isCorrect, long creatorID) {
    db.getUserListManager().markCorrectness(id, isCorrect, creatorID);
  }

  public void markState(String id, STATE state, long creatorID) {
    db.getUserListManager().markState(id, state, creatorID);
  }

  /**
   * @param id
   * @param state
   * @param userID
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#doAfterEditComplete(mitll.langtest.client.list.ListInterface, boolean)
   */
  @Override
  public void setExerciseState(String id, STATE state, long userID) {
    db.getUserListManager().markState(id, state, userID);
  }

  /**
   * @return
   * @see mitll.langtest.client.custom.Navigation#viewReview
   */
  @Override
  public List<UserList> getReviewLists() {
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
   *
   * @param foreign
   * @return
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#isValidForeignPhrase(mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, boolean)
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
   *
   * @param userListID
   * @param userExercise
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#afterValidForeignPhrase
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
   * @param exercise
   * @return
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#duplicateExercise
   */
  @Override
  public UserExercise duplicateExercise(UserExercise exercise) {
    return db.duplicateExercise(exercise);
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#deleteItem(String, long, mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.PagingExerciseList, mitll.langtest.client.list.PagingExerciseList)
   */
  public boolean deleteItem(String id) {
    boolean b = db.deleteItem(id);
    if (b) {
      fullTrie = null; // force rebuild of full trie
    }
    return b;
  }

  /**
   * @see mitll.langtest.client.instrumentation.ButtonFactory#logEvent(String, String, String, String, long)
   * @param id
   * @param widgetType
   * @param exid
   * @param context
   * @param userid
   * @param hitID
   */
  @Override
  public void logEvent(String id, String widgetType, String exid, String context, long userid, String hitID) {
    try {
      db.logEvent(id, widgetType, exid, context, userid, hitID);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  public List<Event> getEvents() {
    return db.getEventDAO().getAll();
  }

  /**
   * @param userExercise
   * @see mitll.langtest.client.custom.dialog.EditableExercise#postEditItem
   */
  @Override
  public void editItem(UserExercise userExercise) {
    db.editItem(userExercise);
    logger.debug("editItem : now user exercise " + userExercise);
  }

  /**
   * @param audioAttribute
   * @param exid
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getPanelForAudio(mitll.langtest.shared.CommonExercise, mitll.langtest.shared.AudioAttribute, mitll.langtest.client.custom.tabs.RememberTabAndContent)
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
   * @param attr
   * @param isMale
   * @see mitll.langtest.client.qc.QCNPFExercise#getGenderGroup
   */
  @Override
  public void markGender(AudioAttribute attr, boolean isMale) {
    db.getAudioDAO().addOrUpdateUser(isMale ? UserDAO.DEFAULT_MALE_ID : UserDAO.DEFAULT_FEMALE_ID, attr);

    String exid = attr.getExid();
    CommonExercise byID = db.getCustomOrPredefExercise(exid);
    if (byID == null) {
      logger.error("couldn't find exercise " + exid);
      logAndNotifyServerException(new Exception("couldn't find exercise " + exid));
    }
    else {
      byID.getAudioAttributes().clear();
      attachAudio(byID);
     // for (AudioAttribute audioAttribute : byID.getAudioAttributes()) logger.debug("after gender change, now " + audioAttribute);
    }
    db.getSectionHelper().refreshExercise(byID);

  }

  /**
   * @param userID
   * @param passwordH
   * @param emailH
   * @param kind
   * @param url
   * @param email
   * @param isMale
   * @param age
   * @param dialect
   * @return null if existing user
   * @param isCD
   * @param device
   * @see mitll.langtest.client.user.UserPassLogin#gotSignUp(String, String, String, mitll.langtest.shared.User.Kind)
   */
  @Override
  public User addUser(String userID, String passwordH, String emailH, User.Kind kind, String url, String email,
                      boolean isMale, int age, String dialect, boolean isCD, String device) {
    User user = db.addUser(getThreadLocalRequest(), userID, passwordH, emailH, kind, isMale, age, dialect, "browser");
    if (user != null && !user.isEnabled()) { // user = null means existing user.
      logger.debug("user " + userID +"/" +user+
          " wishes to be a content developer. Asking for approval.");
      getEmailHelper().addContentDeveloper(url, email, user, getMailSupport());
    }
    else if (user == null) {
      logger.debug("no user found for id " +userID);
    }
    else {
      logger.debug("user " + userID +"/" +user+ " is enabled.");
    }
    return user;
  }

  private EmailHelper getEmailHelper() {
    return new EmailHelper(serverProps, db.getUserDAO(),getMailSupport(),pathHelper);
  }

  /**
   * @return
   * @see mitll.langtest.client.user.UserTable#showDialog(mitll.langtest.client.LangTestDatabaseAsync)
   */
  public List<User> getUsers() {
    return db.getUsers();
  }

  /**
   * @see mitll.langtest.client.user.UserManager#getPermissionsAndSetUser(int)
   * @param id
   * @return
   */
  @Override
  public User getUserBy(long id) { return db.getUserDAO().getUserWhere(id);  }

  /**
   * @param user
   * @param email
   * @param url
   * @return true if there's a user with this email
   * @see mitll.langtest.client.user.UserPassLogin#getForgotPassword()
   */
  public boolean resetPassword(String user, String email, String url) {
    logger.debug("resetPassword for " + user);
    return getEmailHelper().resetPassword(user, email, url);
  }

  /**
   * @param token
   * @param emailR - email encoded by rot13
   * @return
   * @see mitll.langtest.client.LangTest#handleCDToken
   */
  public String enableCDUser(String token, String emailR, String url) {
    logger.info("enabling token " + token + " for email " + emailR + " and url " +url);

    return getEmailHelper().enableCDUser(token, emailR, url);
  }

  /**
   * @param token
   * @return
   * @see mitll.langtest.client.LangTest#showLogin()
   */
  @Override
  public long getUserIDForToken(String token) {
    User user = db.getUserDAO().getUserWhereResetKey(token);
    long l = (user == null) ? -1 : user.getId();
    logger.info("for token " +  token + " got user id " + l);
    return l;
  }

  @Override
  public boolean changePFor(String token, String passwordH) {
    User userWhereResetKey = db.getUserDAO().getUserWhereResetKey(token);
    if (userWhereResetKey != null) {
      db.getUserDAO().clearKey(userWhereResetKey.getId(), true);

      if (!db.getUserDAO().changePassword(userWhereResetKey.getId(), passwordH)) {
        logger.error("couldn't update user password for user " + userWhereResetKey);
      }
      return true;
    } else return false;
  }

  /**
   * @param emailH
   * @param email
   * @param url
   * @return
   * @see mitll.langtest.client.user.UserPassLogin#getForgotUser()
   */
  @Override
  public boolean forgotUsername(String emailH, String email, String url) {
    User valid = db.getUserDAO().isValidEmail(emailH);
    getEmailHelper().getUserNameEmail(email, url, valid);
    return valid != null;
  }

  // Results ---------------------

  /**
   * Sometimes we type faster than we can respond, so we can throw away stale requests.
   *
   * Filter results by search criteria -- unit->value map (e.g. chapter=5), userid, and foreign language text
   * @param req - to echo back -- so that if we get an old request we can discard it
   * @param sortInfo - encoding which fields we want to sort, and ASC/DESC choice
   * @return
   * @see mitll.langtest.client.result.ResultManager#createProvider(int, com.google.gwt.user.cellview.client.CellTable)
   */
  @Override
  public ResultAndTotal getResults(int start, int end, String sortInfo, Map<String, String> unitToValue, long userid, String flText, int req) {
    List<MonitorResult> results = getResults(unitToValue, userid, flText);
    if (!results.isEmpty()) {
      Comparator<MonitorResult> comparator = results.get(0).getComparator(Arrays.asList(sortInfo.split(",")));
      try {
        Collections.sort(results, comparator);
      } catch (Exception e) {
        logger.error("Doing " + sortInfo + " " + unitToValue +" " + userid + " " + flText + " " + start +"-" + end +
            " Got " +e,e);
      }
    }
    int n = results.size();
    int min = Math.min(end, n);
    if (start > min) {
      logger.debug("original req from " + start + " to " + end);
      start = 0;
    }
    List<MonitorResult> resultList = results.subList(start, min);
    return new ResultAndTotal(new ArrayList<MonitorResult>(resultList), n, req);
  }

  @Override
  public int getNumResults() {
    return db.getResultDAO().getNumResults();
  }

  /**
   * @see #getResults(int, int, String, java.util.Map, long, String, int)
   * @param unitToValue
   * @param userid
   * @param flText
   * @return
   */
  private List<MonitorResult> getResults(Map<String, String> unitToValue, long userid, String flText) {
    //logger.debug("getResults : request " + unitToValue + " " + userid + " " + flText);
    boolean isNumber = false;
    try {
      Integer.parseInt(flText);
      isNumber = true;
    } catch (NumberFormatException e) {
    }
    if (isNumber) {
      List<MonitorResult> monitorResultsByID = db.getMonitorResultsWithText(db.getResultDAO().getMonitorResultsByID(flText));
      logger.debug("getResults : request " + unitToValue + " " + userid + " " + flText + " returning " + monitorResultsByID.size() + " results...");
      return monitorResultsByID;
    }

    Collection<MonitorResult> results = db.getMonitorResults();

    Trie<MonitorResult> trie;

    for (String type : getTypeOrder()) {
      if (unitToValue.containsKey(type)) {

       // logger.debug("getResults making trie for " + type);
        // make trie from results
        trie = new Trie<MonitorResult>();

        trie.startMakingNodes();
        for (MonitorResult result : results) {
          String s = result.getUnitToValue().get(type);
          if (s != null) {
            trie.addEntryToTrie(new ResultWrapper(s, result));
          }
        }
        trie.endMakingNodes();

        results = trie.getMatchesLC(unitToValue.get(type));
      }
    }

    if (userid > -1) { // asking for userid
      // make trie from results
//      logger.debug("making trie for userid " + userid);

      trie = new Trie<MonitorResult>();
      trie.startMakingNodes();
      for (MonitorResult result : results) {
        trie.addEntryToTrie(new ResultWrapper(Long.toString(result.getUserid()), result));
      }
      trie.endMakingNodes();

      results = trie.getMatchesLC(Long.toString(userid));
    }

    // must be asking for text
    if (flText != null && !flText.isEmpty()) { // asking for text
      trie = new Trie<MonitorResult>();
      trie.startMakingNodes();
 //     logger.debug("searching over " + results.size());
      for (MonitorResult result : results) {
        String foreignText = result.getForeignText();
        if (foreignText != null) {
          trie.addEntryToTrie(new ResultWrapper(foreignText.trim(), result));
        }
      }
      trie.endMakingNodes();

      results = trie.getMatchesLC(flText);
    }
    logger.debug("getResults : request " + unitToValue + " " + userid + " " + flText + " returning " + results.size() + " results...");
    return new ArrayList<MonitorResult>(results);
  }

  /**
   * Respond to type ahead.
   *
   * @param unitToValue
   * @param userid
   * @param flText
   * @param which
   * @return
   * @see mitll.langtest.client.result.ResultManager#populateTable(int, com.google.gwt.user.client.ui.Panel, com.google.gwt.user.client.ui.DialogBox, com.github.gwtbootstrap.client.ui.Button)
   */
  @Override
  public Collection<String> getResultAlternatives(Map<String, String> unitToValue, long userid, String flText, String which) {
    Collection<MonitorResult> results = db.getMonitorResults();

    logger.debug("getResultAlternatives request " + unitToValue + " userid=" + userid + " fl '" + flText + "' :'" + which + "'");

    Collection<String> matches = new TreeSet<String>();
    Trie<MonitorResult> trie;

    for (String type : getTypeOrder()) {
      if (unitToValue.containsKey(type)) {

    //    logger.debug("getResultAlternatives making trie for " + type);
        // make trie from results
        trie = new Trie<MonitorResult>();

        trie.startMakingNodes();
        for (MonitorResult result : results) {
          String s = result.getUnitToValue().get(type);
          if (s != null) {
            trie.addEntryToTrie(new ResultWrapper(s, result));
          }
        }
        trie.endMakingNodes();

        String s = unitToValue.get(type);
        Collection<MonitorResult> matchesLC = trie.getMatchesLC(s);

        // stop!
        if (which.equals(type)) {
  //        logger.debug("\tmatch for " + type);

          boolean allInt = true;
          for (MonitorResult result : matchesLC) {
            String e = result.getUnitToValue().get(type);
            if (allInt) {
              try {
                Integer.parseInt(e);
              } catch (NumberFormatException e1) {
                allInt = false;
              }
            }
            matches.add(e);
          }

          if (allInt) {
            List<String> sorted = new ArrayList<String>(matches);
            Collections.sort(sorted, new Comparator<String>() {
              @Override
              public int compare(String o1, String o2) {
                return compareTwoMaybeInts(o1, o2);
              }
            });
            return sorted;
          }

//          logger.debug("returning " + matches);

          return matches;
        } else {
          results = matchesLC;
        }
      }
    }

    if (userid > -1) { // asking for userid
      // make trie from results

      logger.debug("making trie for userid " + userid);

      trie = new Trie<MonitorResult>();
      trie.startMakingNodes();
      for (MonitorResult result : results) {
        trie.addEntryToTrie(new ResultWrapper(Long.toString(result.getUserid()), result));
      }
      trie.endMakingNodes();

      Set<Long> imatches = new TreeSet<Long>();
      Collection<MonitorResult> matchesLC = trie.getMatchesLC(Long.toString(userid));

      // stop!
      if (which.equals(MonitorResult.USERID)) {
        for (MonitorResult result : matchesLC) {
          imatches.add(result.getUserid());
        }
        //logger.debug("returning " + imatches);

        for (Long m : imatches) matches.add(Long.toString(m));
        matches = getLimitedSizeList(matches);
        return matches;
      } else {
        results = matchesLC;
      }
    }

    // must be asking for text
    trie = new Trie<MonitorResult>();
    trie.startMakingNodes();
    //logger.debug("text searching over " + results.size());
    for (MonitorResult result : results) {
      trie.addEntryToTrie(new ResultWrapper(result.getForeignText(), result));
      trie.addEntryToTrie(new ResultWrapper(result.getId(), result));
    }
    trie.endMakingNodes();

    Collection<MonitorResult> matchesLC = trie.getMatchesLC(flText);
    //logger.debug("matchesLC for '" +flText+  "' " + matchesLC);

    boolean isNumber = false;
    try {
      Integer.parseInt(flText);
      isNumber = true;
    } catch (NumberFormatException e) {
    }

    if (isNumber) {
      for (MonitorResult result : matchesLC) {   matches.add(result.getId().trim());    }
    }
    else {
      for (MonitorResult result : matchesLC) {   matches.add(result.getForeignText().trim());    }
    }
    //logger.debug("returning text " + matches);

    return getLimitedSizeList(matches);
  }

  private Collection<String> getLimitedSizeList(Collection<String> matches) {
    if (matches.size() > MAX) {
      List<String> matches2 = new ArrayList<String>();
      int nn = 0;
      for (String match : matches) {
        if (nn++ < MAX) {
          matches2.add(match);
        }
      }
      matches = matches2;
    }
    return matches;
  }


  protected int compareTwoMaybeInts(String id1, String id2) {
    int comp;
    try {   // this could be slow
      int i = Integer.parseInt(id1);
      int j = Integer.parseInt(id2);
      comp = i - j;
    } catch (NumberFormatException e) {
      comp = id1.compareTo(id2);
    }
    return comp;
  }

  private static class ResultWrapper implements TextEntityValue<MonitorResult> {
    private final String value;
    private final MonitorResult e;

    public ResultWrapper(String value, MonitorResult e) {
      this.value = value;
      this.e = e;
    }

    @Override
    public MonitorResult getValue() {
      return e;
    }

    @Override
    public String getNormalizedValue() {
      return value;
    }

    public String toString() {
      return "result " + e.getId() + " : " + value;
    }
  }


  /**
   * Record an answer entry in the database.<br></br>
   * Write the posted data to a wav and an mp3 file (since all the browser audio works with mp3).
   * <p/>
   * A side effect is to set the first state to UNSET if it was APPROVED
   * and to set the second state (not really used right now) to RECORDED
   * <p/>
   * <p/>
   * Wade has observed that audio normalization really messes up the ASR -- silence doesn't appear as silence after you multiply
   * the signal.  Also, the user doesn't get feedback that their mic gain is too high/too low or that they
   * are speaking too softly or too loudly
   * <p/>
   * Client references below:
   *
   * @param base64EncodedString generated by flash on the client
   * @param exercise            exercise within the plan
   * @param questionID          question within the exercise
   * @param user                who is answering the question
   * @param reqid               request id from the client, so it can potentially throw away out of order responses
   * @param flq                 was the prompt a foreign language query
   * @param audioType           regular or fast then slow audio recording
   * @param doFlashcard         true if called from practice (flashcard) and we want to do decode and not align
   * @param recordInResults     if true, record in results table -- only when recording in a learn or practice tab
   * @param addToAudioTable     if true, add to audio table -- only when recording reference audio for an item.
   * @param recordedWithFlash   mark if we recorded it using flash recorder or webrtc
   * @param deviceType
   * @param device
   * @return AudioAnswer object with information about the audio on the server, including if audio is valid (not too short, etc.)
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#stopRecording()
   * @see mitll.langtest.client.recorder.RecordButtonPanel#stopRecording()
   */
  @Override
  public AudioAnswer writeAudioFile(String base64EncodedString, String exercise, int questionID,
                                    int user, int reqid, boolean flq, String audioType, boolean doFlashcard,
                                    boolean recordInResults, boolean addToAudioTable, boolean recordedWithFlash,
                                    String deviceType, String device) {
    CommonExercise exercise1 = db.getCustomOrPredefExercise(exercise);  // allow custom items to mask out non-custom items

    AudioAnswer audioAnswer = audioFileHelper.writeAudioFile(base64EncodedString, exercise, exercise1, questionID, user, reqid,
        audioType, doFlashcard, recordInResults, recordedWithFlash, deviceType, device);

    if (addToAudioTable && audioAnswer.isValid()) {
      AudioAttribute attribute = addToAudioTable(user, audioType, exercise1, exercise, audioAnswer);
      audioAnswer.setAudioAttribute(attribute);
    } else {
      // So Wade has observed that this really messes up the ASR -- silence doesn't appear as silence after you multiply
      // the signal.  Also, the user doesn't get feedback that their mic gain is too high/too low or that they
      // are speaking too softly or too loudly.

      // normalizeLevel(audioAnswer);
      ensureMP3(audioAnswer.getPath());
    }
    if (!audioAnswer.isValid() && audioAnswer.getDurationInMillis() == 0) {
      logger.warn("huh? got zero length recording " + user + " " + exercise);
      logEvent("audioRecording", "writeAudioFile", exercise, "Writing audio - got zero duration!", user, "unknown");
    }
    return audioAnswer;
  }

/*
  private void normalizeLevel(AudioAnswer audioAnswer) {
    File absoluteFile = pathHelper.getAbsoluteFile(audioAnswer.getPath());
    if (!absoluteFile.exists()) {
      logger.error("huh? can't find " + absoluteFile + " audio file just posted.?");
    } else {
      //logger.debug("norm level for " + absoluteFile);
      new AudioConversion().normalizeLevels(absoluteFile);
    }
  }
*/

  /**
   * A low overhead way of doing alignment.
   *
   * Useful for conversational dialogs.
   *
   * @param base64EncodedString
   * @param textToAlign
   * @param identifier
   * @param reqid
   * @return
   * @see mitll.langtest.client.scoring.SimplePostAudioRecordButton#postAudioFile(String)
   */
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

/*  public AudioAnswer getAnswer(String base64EncodedString,
                               String textToAlign,
                               String identifier,
                               int reqid,
                               int user, boolean doFlashcard, float score,
                                String deviceType, String device) {
    //CommonExercise exercise1 = db.getCustomOrPredefExercise(exerciseID);  // allow custom items to mask out non-custom items

   // String wavPath = pathHelper.getWavPathUnder("postedAudio");
   // File file = pathHelper.getAbsoluteFile(wavPath);

    //AudioAnswer audioAnswer = audioFileHelper.getAlignment(base64EncodedString, textToAlign, identifier, reqid);
    File file = audioFileHelper.getPostedFileLoc();
    AudioAnswer audioAnswer = audioFileHelper.getAudioAnswer(base64EncodedString, reqid, file);
    //AudioAnswer audioAnswer = audioFileHelper.getAlignment(base64EncodedString, textToAlign, identifier, reqid);

    return audioFileHelper.getAnswer(exerciseID, exercise1, user, doFlashcard, wavPath, file, deviceType, device, score);
  }*/

  /**
   * Remember this audio as reference audio for this exercise, and possibly clear the APRROVED (inspected) state
   * on the exercise indicating it needs to be inspected again (we've added new audio).
   *
   * @param user        who recorded audio
   * @param audioType   regular or slow
   * @param exercise1   for which exercise
   * @param audioAnswer holds the path of the temporary recorded file
   * @return AudioAttribute that represents the audio that has been added to the exercise
   * @see #writeAudioFile
   */
  private AudioAttribute addToAudioTable(int user, String audioType, CommonExercise exercise1, String exerciseID, AudioAnswer audioAnswer) {
    String exercise = exercise1 == null ? exerciseID : exercise1.getID();
    File fileRef = pathHelper.getAbsoluteFile(audioAnswer.getPath());
    String destFileName = audioType + "_" + System.currentTimeMillis() + "_by_" + user + ".wav";
    String permanentAudioPath = new PathWriter().getPermanentAudioPath(pathHelper, fileRef, destFileName, true, exercise);
    AudioAttribute audioAttribute =
        db.getAudioDAO().addOrUpdate(user, permanentAudioPath, exercise, System.currentTimeMillis(), audioType, audioAnswer.getDurationInMillis());
    audioAnswer.setPath(audioAttribute.getAudioRef());
    logger.debug("addToAudioTable user " + user + " ex " + exerciseID + " for " + audioType + " audio answer has " + audioAttribute);

    // what state should we mark recorded audio?
    setExerciseState(exercise, user, exercise1);
    return audioAttribute;
  }

  /**
   * Only change APPROVED to UNSET.
   *
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

  void makeAutoCRT() {  audioFileHelper.makeAutoCRT(relativeConfigDir, this);  }

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
      Collection<AudioAttribute> males = exercise.getByGender(true);
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
   *
   * @return
   * @see mitll.langtest.client.monitoring.MonitoringManager#doResultLineQuery
   */
  public Map<String, Map<String, Integer>> getResultPerExercise() {
    return db.getResultPerExercise();
  }

  /**
   * @return
   * @see mitll.langtest.client.monitoring.MonitoringManager#doGenderQuery(com.google.gwt.user.client.ui.Panel)
   */
  @Override
  public Map<String, Map<Integer, Integer>> getResultCountsByGender() {
    return db.getResultCountsByGender();
  }

  public Map<String, Map<Integer, Map<Integer, Integer>>> getDesiredCounts() {
    return db.getDesiredCounts();
  }

  /**
   * @return
   * @see mitll.langtest.client.monitoring.MonitoringManager#doSessionQuery
   */
  public List<Session> getSessions() {
    return db.getSessions();
  }

  /**
   * @return
   * @see mitll.langtest.client.monitoring.MonitoringManager#doSessionQuery
   */
  public Map<String, Number> getResultStats() {
    return db.getResultStats();
  }

  @Override
  public Map<Integer, Map<String, Map<String, Integer>>> getGradeCountPerExercise() {
    return db.getGradeCountPerExercise();
  }

  /**
   * @param userid who's asking?
   * @param ids items the user has actually practiced/recorded audio for
   * @param latestResultID
   * @param typeToSection indicates the unit and chapter(s) we're asking about
   * @param userListID if we're asking about a list and not predef items
   * @return
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#onSetComplete()
   */
  @Override
  public AVPScoreReport getUserHistoryForList(long userid, Collection<String> ids, long latestResultID,
                                              Map<String, Collection<String>> typeToSection, long userListID) {
    //logger.debug("getUserHistoryForList " + userid + " and " + ids + " type to section " + typeToSection);

    UserList userListByID = userListID != -1 ? db.getUserListByID(userListID) : null;
    List<String> allIDs = new ArrayList<String>();
    Map<String, String> idToFL = new HashMap<String, String>();
    if (userListByID != null) {
      for (CommonExercise exercise : userListByID.getExercises()) {
        String id = exercise.getID();
        allIDs.add(id);
        idToFL.put(id, exercise.getForeignLanguage());
      }
    } else {
      Collection<CommonExercise> exercisesForState = (typeToSection == null || typeToSection.isEmpty()) ? getExercises() :
          db.getSectionHelper().getExercisesForSelectionState(typeToSection);

      for (CommonExercise exercise : exercisesForState) {
        String id = exercise.getID();
        allIDs.add(id);
        idToFL.put(id, exercise.getForeignLanguage());
      }
    }
    //logger.debug("for " + typeToSection + " found " + allIDs.size());
    return db.getUserHistoryForList(userid, ids, latestResultID, allIDs, idToFL);
  }

  public void logMessage(String message) {
    String prefixedMessage = "for " + pathHelper.getInstallPath() + " from client " + message;
    logger.debug(prefixedMessage);

    if (message.startsWith("got browser exception")) {
      sendEmail("Javascript Exception", prefixedMessage);
    }
  }

  private MailSupport getMailSupport() {
    return new MailSupport(serverProps.isDebugEMail(), serverProps.isTestEmail());
  }

  @Override
  public void destroy() {
    super.destroy();
    db.destroy(); // TODO : redundant with h2 shutdown hook?
  }

  /**
   * Reco test option lets you run through and score all the reference audio -- if you want to see model performance
   *
   */
  @Override
  public void init() {
    this.pathHelper = new PathHelper(getServletContext());
    readProperties(getServletContext());
    setInstallPath(serverProps.getUseFile(), db);
    audioFileHelper = new AudioFileHelper(pathHelper, serverProps, db, this);
    if (serverProps.doRecoTest() || serverProps.doRecoTest2()) {
      new RecoTest(this, serverProps, pathHelper, audioFileHelper);
    }
    try {
      db.preloadExercises();
      db.getUserListManager().setStateOnExercises();
      db.doReport(serverProps, getServletContext().getRealPath(""), getMailSupport(), pathHelper);
    } catch (Exception e) {
      logger.error("couldn't load database " +e,e);
    }
  }

  /**
   * The config web.xml file.
   * As a final step, creates the DatabaseImpl!<br></br>
   * <p/>
   * NOTE : makes the database available to other servlets via the databaseReference servlet context attribute.
   * Note that this will only ever be called once.
   *
   * @param servletContext
   * @see #init()
   */
  private void readProperties(ServletContext servletContext) {
    String config = servletContext.getInitParameter("config");
    this.relativeConfigDir = "config" + File.separator + config;
    this.configDir = pathHelper.getInstallPath() + File.separator + relativeConfigDir;

    pathHelper.setConfigDir(configDir);

    serverProps = new ServerProperties(servletContext, configDir);
    String h2DatabaseFile = serverProps.getH2Database();

    db = makeDatabaseImpl(h2DatabaseFile);
    shareDB(servletContext);
    shareLoadTesting(servletContext);
  }

  private void shareLoadTesting(ServletContext servletContext) {
    Object loadTesting = servletContext.getAttribute(ScoreServlet.LOAD_TESTING);
    if (loadTesting != null) {
      logger.debug("hmm... found existing load testing reference " + loadTesting);
    }
    servletContext.setAttribute(ScoreServlet.LOAD_TESTING, this);
  }

  private void shareDB(ServletContext servletContext) {
    Object databaseReference = servletContext.getAttribute(DATABASE_REFERENCE);
    if (databaseReference != null) {
      logger.debug("hmm... foundexisting database reference " + databaseReference);
    }

    servletContext.setAttribute(DATABASE_REFERENCE, db);
  }

  private DatabaseImpl makeDatabaseImpl(String h2DatabaseFile) {
    //logger.debug("word pairs " +  serverProps.isWordPairs() + " language " + serverProps.getLanguage() + " config dir " + relativeConfigDir);
    return new DatabaseImpl(configDir, relativeConfigDir, h2DatabaseFile, serverProps, pathHelper, true, this);
  }

  /**
   * @param useFile
   * @param db
   * @return
   * @see LangTestDatabaseImpl#init()
   */
  public String setInstallPath(boolean useFile, DatabaseImpl db) {
    String lessonPlanFile = getLessonPlan();
    if (useFile && !new File(lessonPlanFile).exists()) logger.error("couldn't find lesson plan file " + lessonPlanFile);

    db.setInstallPath(pathHelper.getInstallPath(), lessonPlanFile, serverProps.getLanguage(), useFile,
        relativeConfigDir + File.separator + serverProps.getMediaDir());

    return lessonPlanFile;
  }

  private String getLessonPlan() {
    return configDir + File.separator + serverProps.getLessonPlan();
  }
}
