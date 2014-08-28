package mitll.langtest.server;

import audio.image.ImageType;
import audio.imagewriter.ImageWriter;
import com.google.common.io.Files;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.util.tools.shared.Md5Utils;
import com.google.gwt.util.tools.shared.StringUtils;
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
public class LangTestDatabaseImpl extends RemoteServiceServlet implements LangTestDatabase, AutoCRTScoring, LogAndNotify {
  private static final Logger logger = Logger.getLogger(LangTestDatabaseImpl.class);
  private static final String WAV = ".wav";
  private static final String MP3 = ".mp3";
  private static final String NP_SERVER = "np.ll.mit.edu";
  private static final String REPLY_TO = "admin@" + NP_SERVER;
  private static final String CONTENT_DEVELOPER_APPROVAL_EMAIL = "gordon.vidaver@ll.mit.edu";
  private static final String RP = "rp";

  private DatabaseImpl db;
  private AudioFileHelper audioFileHelper;
  private String relativeConfigDir;
  private String configDir;
  private ServerProperties serverProps;
  private PathHelper pathHelper;
  private String reqURL, path, info;

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
      reqURL = request.getRequestURI();
      path = request.getServletPath();
      info = request.getPathInfo();

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
    String message1 = e == null ? "null_ex" : e.getMessage() == null ? "null_msg":e.getMessage();
    if (!message1.contains("Broken Pipe")) {
      String message = "Server Exception : " + ExceptionUtils.getStackTrace(e);
      String prefixedMessage = "for " + pathHelper.getInstallPath() + " got " + message;
      logger.debug(prefixedMessage);
      String subject = "Server Exception on " + pathHelper.getInstallPath();
      sendEmail(subject, prefixedMessage);
    }
  }

  public void sendEmail(String subject, String prefixedMessage) {
    getMailSupport().email(serverProps.getEmailAddress(), subject, prefixedMessage);
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
   * @return
   * @see mitll.langtest.client.list.PagingExerciseList#loadExercises(String, String)
   */
  @Override
  public ExerciseListWrapper getExerciseIds(int reqID, Map<String, Collection<String>> typeToSelection, String prefix,
                                            long userListID, int userID, String role, boolean onlyUnrecordedByMe, boolean onlyExamples) {
    Collection<CommonExercise> exercises;
    logger.debug("getExerciseIds : getting exercise ids for " +
      " config " + relativeConfigDir +
      " prefix " + prefix+
      " and user list id " + userListID + " user " + userID + " role " + role + " filter " + onlyUnrecordedByMe + " only examples " + onlyExamples);

    try {
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
        exercises = filterByUnrecorded(userID, onlyUnrecordedByMe, onlyExamples, exercises);
        int i = markRecordedState(userID, role, exercises, onlyExamples);
        //logger.debug("marked " +i + " as recorded");

        // now sort : everything gets sorted the same way
        List<CommonExercise> commonExercises = new ArrayList<CommonExercise>(exercises);
        new ExerciseSorter(getTypeOrder()).getSortedByUnitThenAlpha(commonExercises, role.equals(Result.AUDIO_TYPE_RECORDER));

        return makeExerciseListWrapper(reqID, commonExercises, userID, role, onlyExamples);

      } else { // sort by unit-chapter selection
        // builds unit-lesson hierarchy if non-empty type->selection over user list
        if (userListByID != null) {
          Collection<CommonExercise> exercisesForState = getExercisesFromFiltered(typeToSelection, userListByID);
          exercisesForState = filterByUnrecorded(userID, onlyUnrecordedByMe, onlyExamples, exercisesForState);

          return getExerciseListWrapperForPrefix(reqID, prefix, exercisesForState, userID, role, onlyExamples);
        } else {
          return getExercisesForSelectionState(reqID, typeToSelection, prefix, userID, role, onlyUnrecordedByMe, onlyExamples);
        }
      }
    } catch (Exception e) {
      logger.warn("got " +e,e);
      logAndNotifyServerException(e);
      return new ExerciseListWrapper();
    }
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
      logger.debug("recorded already " + recordedForUser.size() + " checking " + exercises.size());
      // filter
      for (CommonExercise exercise : exercises) {
        if (!recordedForUser.contains(exercise.getID())) {
          copy.add(exercise);
        }
        else {
          Collection<AudioAttribute> byGender = exercise.getByGender(isMale);
          boolean hasReg = false;
          boolean hasSlow = false;
          boolean hasExample = false;
 /*         if (!byGender.isEmpty()) logger.debug("checking " + isMale + " ex " + exercise.getID()+
            " has " + byGender.size() + " recordings");*/
          for (AudioAttribute attr : byGender) {
            if (attr.getUserid() != UserDAO.DEFAULT_USER_ID) {
              hasReg = hasReg ||  attr.isRegularSpeed();
              hasSlow = hasSlow ||  attr.isSlow();
              hasExample = attr.isExampleSentence();

              if (onlyExamples) {
                if (hasExample) break;
              }
              else if (hasReg && hasSlow) {
                break;
              }
            }
          }

          if (onlyExamples) {
            if (!hasExample) {
              copy.add(exercise);
            }
          }
          else if (!hasReg || !hasSlow) {
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
   * @see #getExerciseIds(int, java.util.Map, String, long, int, String, boolean, boolean)
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
   * @param reqID
   * @param typeToSection
   * @param prefix
   * @param userID
   * @param role
   * @param onlyUnrecordedByMe
   * @param onlyExamples
   * @return
   * @see mitll.langtest.client.list.HistoryExerciseList#loadExercises(String, String)
   * @see #getExerciseIds
   */
  private ExerciseListWrapper getExercisesForSelectionState(int reqID,
                                                            Map<String, Collection<String>> typeToSection, String prefix,
                                                            long userID, String role, boolean onlyUnrecordedByMe, boolean onlyExamples) {
    Collection<CommonExercise> exercisesForState = db.getSectionHelper().getExercisesForSelectionState(typeToSection);
    exercisesForState = filterByUnrecorded(userID, onlyUnrecordedByMe, onlyExamples, exercisesForState);

    return getExerciseListWrapperForPrefix(reqID, prefix, exercisesForState, userID, role, onlyExamples);
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
   * @return
   * @see #getExerciseIds
   */
  private ExerciseListWrapper getExerciseListWrapperForPrefix(int reqID, String prefix, Collection<CommonExercise> exercisesForState, long userID, String role, boolean onlyExamples) {
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
    // why copy???
    List<CommonExercise> copy = new ArrayList<CommonExercise>(exercisesForState);

    new ExerciseSorter(getTypeOrder()).getSortedByUnitThenAlpha(copy, role.equals(Result.AUDIO_TYPE_RECORDER));

    return makeExerciseListWrapper(reqID, copy, userID, role, onlyExamples);
  }

  /**
   * Send the first exercise along so we don't have to ask for it after we get the initial list
   *
   * @param reqID
   * @param exercises
   * @param userID
   * @param role
   * @param onlyExamples
   * @return
   * @see #getExerciseIds
   * @see #getExerciseListWrapperForPrefix(int, String, java.util.Collection, long, String, boolean)
   */
  private ExerciseListWrapper makeExerciseListWrapper(int reqID, Collection<CommonExercise> exercises, long userID, String role, boolean onlyExamples) {
    CommonExercise firstExercise = exercises.isEmpty() ? null : exercises.iterator().next();
    if (firstExercise != null) {
      addAnnotationsAndAudio(userID, firstExercise);
      ensureMP3s(firstExercise);
    }
    List<CommonShell> exerciseShells = getExerciseShells(exercises);

    //   logger.debug("makeExerciseListWrapper : userID " +userID + " Role is " + role);
    if (role.equals(Result.AUDIO_TYPE_RECORDER)) {
      markRecordedState((int) userID, role, exerciseShells, onlyExamples);
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
   *
   * @param userID
   * @param firstExercise
   * @see #getExercise(String, long)
   * @see #makeExerciseListWrapper(int, java.util.Collection, long, String, boolean)
   */
  private void addAnnotationsAndAudio(long userID, CommonExercise firstExercise) {
    long then = System.currentTimeMillis();

    addAnnotations(firstExercise); // todo do this in a better way
    long now = System.currentTimeMillis();
    if (now - then > 40) {
      logger.debug("addAnnotationsAndAudio : (" + serverProps.getLanguage() + ") took " + (now - then) + " millis to add annotations to exercise " + firstExercise.getID());
    }
    then = now;
    attachAudio(firstExercise);

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

    attachScoreHistory(userID, firstExercise);

    now = System.currentTimeMillis();
    if (now - then > 40) {
      logger.debug("addAnnotationsAndAudio : (" + serverProps.getLanguage() + ") took " + (now - then) + " millis to attach score history to exercise " + firstExercise.getID());
    }
  }

  /**
   * @see #addAnnotationsAndAudio(long, mitll.langtest.shared.CommonExercise)
   * @param userID
   * @param firstExercise
   */
  private void attachScoreHistory(long userID, CommonExercise firstExercise) {
    db.getResultDAO().attachScoreHistory(userID,firstExercise);
  }

  /**
   * @param firstExercise
   * @see #addAnnotationsAndAudio(long, mitll.langtest.shared.CommonExercise)
   */
  private void attachAudio(CommonExercise firstExercise) {
    String installPath = pathHelper.getInstallPath();
    db.getAudioDAO().attachAudio(firstExercise, installPath, relativeConfigDir);
  }

  /**
   * Only add the played markings if doing QC.
   *
   * TODO : This is an expensive query - we need a smarter way of remembering when audio has been played.
   * @see #addAnnotationsAndAudio(long, mitll.langtest.shared.CommonExercise)
   * @param userID
   * @param firstExercise
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

  /**
   * Joins with annotation data when doing QC.
   *
   * @param id
   * @param userID
   * @return
   * @see mitll.langtest.client.list.ExerciseList#askServerForExercise
   */
  public CommonExercise getExercise(String id, long userID) {
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
      logger.error("getExercise : huh? couldn't find exercise with id " + id + " when examining " + exercises.size() + " items");
    } else {
      then2 = System.currentTimeMillis();

      addAnnotationsAndAudio(userID, byID);
      now = System.currentTimeMillis();
      if (now - then2 > 100) {
        logger.debug("getExercise : (" + language + ") took " + (now - then2) + " millis to add annotations to exercise " + id);
      }
      then2 = System.currentTimeMillis();

      //logger.debug("getExercise : returning " + byID);
      ensureMP3s(byID);
      now = System.currentTimeMillis();
      if (now - then2 > 100) {
        logger.debug("getExercise : (" + language + ") took " + (now - then2) + " millis to ensure there are mp3s for exercise " + id);
      }
    }
    now = System.currentTimeMillis();
    long diff = now - then;
    String message = "getExercise : (" + language + ") took " + diff + " millis to get exercise " + id;

    if (diff > 3000) {
      logger.error(message);
      sendEmail("slow exercise on " + language, "Getting ex " + id + " on " + language + " took " + diff + " millis.");
    } else if (diff > 1000) {
      logger.warn(message);
    } else if (diff > 20) {
      logger.debug(message);
    }
    return byID;
  }

  /**
   * @see #addAnnotationsAndAudio(long, mitll.langtest.shared.CommonExercise)
   * @param byID
   */
  private void addAnnotations(CommonExercise byID) {  db.getUserListManager().addAnnotations(byID);  }

  /**
   * @param byID
   * @see #getExercise(String, long)
   * @see #makeExerciseListWrapper(int, java.util.Collection, long, String, boolean)
   * @param byID
   */
  private void ensureMP3s(CommonExercise byID) {
    Collection<AudioAttribute> audioAttributes = byID.getAudioAttributes();
    for (AudioAttribute audioAttribute : audioAttributes) {
      if (!ensureMP3(audioAttribute.getAudioRef(), false)) {
        audioAttribute.setAudioRef(AudioConversion.FILE_MISSING);
      }
    }

    if (audioAttributes.isEmpty()) {
      logger.warn("ensureMP3s : no ref audio for " + byID);
    }
  }

  private boolean checkedLTS = false;
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

    checkLTS(exercises);
    long now = System.currentTimeMillis();
    if (now - then > 200) {
      logger.info("took " + (now - then) + " millis to get the predef exercise list.");
    }
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
   *
   * @param wavFile
   * @param overwrite
   * @return true if mp3 file exists
   * @see #ensureMP3s(mitll.langtest.shared.CommonExercise)
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
  public void addTextAnswer(int userID, CommonExercise exercise, int questionID, String answer, String answerType) {
    db.addAnswer(userID, exercise, questionID, answer, answerType);
  }

  // Users ---------------------

  @Override
  public synchronized int userExists(String login) {
    return db.userExists(login);
  }

  /**
   * @param login
   * @param passwordH
   * @return
   * @see mitll.langtest.client.user.UserPassLogin#gotLogin
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
   * @see mitll.langtest.client.custom.CreateListDialog#doCreate
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
   * @see mitll.langtest.client.custom.NPFExercise#populateListChoices
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
   * @see mitll.langtest.client.custom.NPFExercise#populateListChoices
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
   * @see mitll.langtest.client.custom.QCNPFExercise#markReviewed
   */
  public void markReviewed(String id, boolean isCorrect, long creatorID) {
    db.getUserListManager().markCorrectness(id, isCorrect, creatorID);
  }

  public void markState(String id, STATE state, long creatorID) {
    db.getUserListManager().markState(id, state, creatorID);
  }

  /**
   * @param ids
   * @see mitll.langtest.client.custom.MyFlashcardExercisePanelFactory.StatsPracticePanel#getRepeatButton()
   */
  @Override
  public void setAVPSkip(Collection<Long> ids) {
    db.getAnswerDAO().changeType(ids);
  }

  /**
   * @param id
   * @param state
   * @param userID
   * @see mitll.langtest.client.custom.ReviewEditableExercise#doAfterEditComplete(mitll.langtest.client.list.ListInterface, boolean)
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
   *
   * @param foreign
   * @return
   * @see mitll.langtest.client.custom.NewUserExercise#isValidForeignPhrase(mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, boolean)
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
   * @see mitll.langtest.client.custom.NewUserExercise#afterValidForeignPhrase
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
   * @see mitll.langtest.client.custom.ReviewEditableExercise#duplicateExercise
   */
  @Override
  public UserExercise duplicateExercise(UserExercise exercise) {
    return db.duplicateExercise(exercise);
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.client.custom.ReviewEditableExercise#deleteItem(String, long, mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.PagingExerciseList, mitll.langtest.client.list.PagingExerciseList)
   */
  public boolean deleteItem(String id) {
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
      logger.error("got " + e, e);
    }
  }

  public List<Event> getEvents() {
    return db.getEvents();
  }

  /**
   * @param userExercise
   * @see mitll.langtest.client.custom.EditableExercise#postEditItem
   */
  @Override
  public void editItem(UserExercise userExercise) {
    db.editItem(userExercise);
    logger.debug("editItem : now user exercise " + userExercise);
  }

  /**
   * @param audioAttribute
   * @param exid
   * @see mitll.langtest.client.custom.ReviewEditableExercise#getPanelForAudio(mitll.langtest.shared.CommonExercise, mitll.langtest.shared.AudioAttribute, mitll.langtest.client.custom.RememberTabAndContent)
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
   * @see mitll.langtest.client.custom.QCNPFExercise#getGenderGroup
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
   * @deprecated called from student dialog
   */
  @Override
  public long addUser(int age, String gender, int experience,
                      String nativeLang, String dialect, String userID, Collection<User.Permission> permissions) {
    return db.addUser(getThreadLocalRequest(), age, gender, experience, nativeLang, dialect, userID, permissions);
  }

  /**
   * @param userID
   * @param passwordH
   * @param emailH
   * @param kind
   * @param url
   * @param email
   * @param isMale
   *@param age
   * @param dialect @return null if existing user
   * @see mitll.langtest.client.user.UserPassLogin#gotSignUp(String, String, String, mitll.langtest.shared.User.Kind)
   */
  @Override
  public User addUser(String userID, String passwordH, String emailH, User.Kind kind, String url, String email,
                      boolean isMale, int age, String dialect) {
    User user = db.addUser(getThreadLocalRequest(), userID, passwordH, emailH, kind, isMale, age, dialect);
    if (user != null && !user.isEnabled()) { // user = null means existing user.
      url = trimURL(url);
      String userID1 = user.getUserID();
      String toHash = userID1 + "_" + System.currentTimeMillis();
      String hash = getHash(toHash);
      db.getUserDAO().addKey(user.getId(), false, hash);

      String message = "Hi Tamas" + ",<br/><br/>" +
          "User '" + userID1 +
          "' would like to be a content developer for " + serverProps.getLanguage() +
          "." + "<br/>" +

          "Click the link to allow them." +
          "<br/><br/>" +
          "Regards, Administrator";

      getMailSupport().sendEmail(NP_SERVER,
          url + "?cd=" + hash + "&" +
              "er" +
              "=" + rot13(email),
          serverProps.getApprovalEmailAddress(),
          "gordon.vidaver@ll.mit.edu",
          "Content Developer approval for " + userID1 + " for " + serverProps.getLanguage(),
          message,
          "Click to approve" // link text
      );
    }
    return user;
  }

  private String rot13(String val) {
    StringBuilder builder = new StringBuilder();
    for (char c : val.toCharArray()) {
      if (c >= 'a' && c <= 'm') c += 13;
      else if (c >= 'A' && c <= 'M') c += 13;
      else if (c >= 'n' && c <= 'z') c -= 13;
      else if (c >= 'N' && c <= 'Z') c -= 13;
      builder.append(c);
    }
    return builder.toString();
  }

  /**
   * @return
   * @see mitll.langtest.client.user.UserTable#showDialog(mitll.langtest.client.LangTestDatabaseAsync)
   */
  public List<User> getUsers() {
    return db.getUsers();
  }

  @Override
  public User getUserBy(long id) {
    return db.getUserDAO().getUserWhere(id);
  }

  /// TODO : replace with columns on user id table
  //Map<String,Long> keyToUser = new HashMap<String, Long>();
  // Map<String,Long> keyToEnabledUser = new HashMap<String, Long>();

  /**
   * @param user
   * @param email
   * @param url
   * @return true if there's a user with this email
   * @see mitll.langtest.client.user.UserPassLogin#getForgotPassword()
   */
  public boolean resetPassword(String user, String email, String url) {
    logger.debug("resetPassword for " + user);

    User validUserAndEmail = db.getUserDAO().isValidUserAndEmail(user, getHash(email));

    if (validUserAndEmail != null) {
      logger.debug("resetPassword for " + user + " sending reset password email.");
      //keyToUser.put(hash,validUserAndEmail.getId());
      String toHash = user + "_" + System.currentTimeMillis();
      String hash = getHash(toHash);
      db.getUserDAO().addKey(validUserAndEmail.getId(), true, hash);

      String message = "Hi " + user + ",<br/><br/>" +
          "Click the link below to change your password." +
          "<br/><br/>" +
          "Regards, Administrator";

      url = trimURL(url);
      sendEmail(url + "?" + RP + "=" + hash,
          email,
          "Password Reset",
          message,
          "Reset Password" // link text
      );

      //logger.debug("key map is " +keyToUser);
      return true;
    } else {
      logger.debug("couldn't find user " + user + " and email " + email);
      String message = "User " + user + " with email " + email + " tried to reset password - but they're not valid.";
      String prefixedMessage = "for " + pathHelper.getInstallPath() + " got " + message;
      logger.debug(prefixedMessage);
      getMailSupport().email(serverProps.getEmailAddress(), "Invalid password reset for " + serverProps.getLanguage(), prefixedMessage);
      return false;
    }
  }

  private void sendEmail(String link, String to, String subject, String message, String linkText) {
    getMailSupport().sendEmail(NP_SERVER,
        link,
        to,
        REPLY_TO,
        subject,
        message,
        linkText// link text
    );
  }

  /**
   * @param token
   * @return
   * @see mitll.langtest.client.LangTest#handleCDToken
   */
  public Long enableCDUser(String token, String emailR, String url) {
    // Long userID = keyToEnabledUser.remove(token);

    User userWhereEnabledReq = db.getUserDAO().getUserWhereEnabledReq(token);
    Long userID = null;
    if (userWhereEnabledReq == null) {
      logger.debug("user id '" + userID + "' for " + token);
      userID = null;
    } else {
      userID = userWhereEnabledReq.getId();
      logger.debug("user id '" + userID + "' for " + token + " vs " + userWhereEnabledReq.getId());
    }
    String email = rot13(emailR);

    if (userID == null) {
      return null;
    } else {
      boolean b = db.getUserDAO().enableUser(userID);
      if (b) {
        db.getUserDAO().clearKey(userID, false);

        User userWhere = db.getUserDAO().getUserWhere(userID);
        url = trimURL(url);

        logger.debug("Sending user email... link is " + url);
        String message = "Hi " + userWhere.getUserID() + ",<br/>" +
            "You have been approved to be a content developer for " + serverProps.getLanguage() + "." +
            "<br/>Click on the link below to log in." +
            "<br/><br/>" +
            "Regards, Administrator";
        sendEmail(url // baseURL
            ,
            email, // destination email
            "Account approved", // subject
            message,
            "Click here to return to the site." // link text
        );
      }
      return (b ? userID : -1);
    }
  }

  public String getHash(String toHash) {
    return StringUtils.toHexString(Md5Utils.getMd5Digest(toHash.getBytes()));
  }

  /**
   * @param token
   * @return
   * @see mitll.langtest.client.LangTest#showLogin()
   */
  @Override
  public long getUserIDForToken(String token) {
    //Long aLong = keyToUser.get(token);
    User user = db.getUserDAO().getUserWhereResetKey(token);
    if (user == null) {
      //if (aLong != null) logger.error("huh? disagree - ");
      return -1;
    } else {
      //if (aLong != user.getId()) logger.error("disagree --");
      return user.getId();
      //  return aLong;
    }
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

    url = trimURL(url);

    if (valid != null) {
      logger.debug("Sending user email...");
      String message = "Hi " + valid.getUserID() + ",<br/>" +
          "Your user name is " + valid.getUserID() + "." +
          "<br/><br/>" +
          "Regards, Administrator";
      sendEmail(url // baseURL
          ,
          email, // destination email
          "Your user name", // subject
          message,
          "Click here to return to the site." // link text
      );
    }
    return valid != null;
  }

  public String trimURL(String url) {
    if (url.contains("127.0.0.1")) return url;
    else return url.split("\\?")[0].split("\\#")[0];
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
   * @return
   * @see mitll.langtest.client.result.ResultManager#showResults()
   */
  @Override
  public int getNumResults() {
    return db.getNumResults();
  }

  /**
   * Record an answer entry in the database.<br></br>
   * Write the posted data to a wav and an mp3 file (since all the browser audio works with mp3).
   * <p/>
   * A side effect is to set the first state to UNSET if it was APPROVED
   * and to set the second state (not really used right now) to RECORDED
   * <p/>
   * Client references:
   *
   * @param base64EncodedString generated by flash on the client
   * @param plan                which set of exercises
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
   * @return AudioAnswer object with information about the audio on the server, including if audio is valid (not too short, etc.)
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#stopRecording()
   * @see mitll.langtest.client.recorder.RecordButtonPanel#stopRecording()
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
    } else {
      normalizeLevel(audioAnswer);
      ensureMP3(audioAnswer.getPath(), false);
    }
    if (!audioAnswer.isValid() && audioAnswer.getDurationInMillis() == 0) {
      logger.warn("huh? got zero length recording " + user + " " + exercise);
      logEvent("audioRecording", "writeAudioFile", exercise, "Writing audio - got zero duration!", user, "unknown");
    }
    return audioAnswer;
  }

  private void normalizeLevel(AudioAnswer audioAnswer) {
    File absoluteFile = pathHelper.getAbsoluteFile(audioAnswer.getPath());
    if (!absoluteFile.exists()) {
      logger.error("huh? can't find " + absoluteFile + " audio file just posted.?");
    } else {
      //logger.debug("norm level for " + absoluteFile);
      new AudioConversion().normalizeLevels(absoluteFile);
    }
  }

  /**
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

  /**
   * Remember this audio as reference audio for this exercise, and possibly clear the APRROVED (inspected) state
   * on the exercise indicating it needs to be inspected again (we've added new audio).
   *
   * @param user        who recorded audio
   * @param audioType   regular or slow
   * @param exercise1   for which exercise
   * @param audioAnswer holds the path of the temporary recorded file
   * @return AudioAttribute that represents the audio that has been added to the exercise
   * @see #writeAudioFile(String, String, String, int, int, int, boolean, String, boolean, boolean, boolean, boolean)
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

  void makeAutoCRT() {
    audioFileHelper.makeAutoCRT(relativeConfigDir, this);
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
   * @param userid
   * @param latestResultID
   * @return
   * @see mitll.langtest.client.custom.MyFlashcardExercisePanelFactory.StatsPracticePanel#onSetComplete()
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
      sendEmail("Javascript Exception", prefixedMessage);
    }
  }

  private MailSupport getMailSupport() {
    return new MailSupport(serverProps.isDebugEMail());
  }

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
    Object databaseReference = servletContext.getAttribute("databaseReference");
    if (databaseReference != null) {
      logger.warn("huh? found existing database reference " + databaseReference);
    } else {
      servletContext.setAttribute("databaseReference", db);
    }
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
