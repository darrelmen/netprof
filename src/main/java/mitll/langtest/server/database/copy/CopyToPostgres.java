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

package mitll.langtest.server.database.copy;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.annotation.AnnotationDAO;
import mitll.langtest.server.database.annotation.SlickAnnotationDAO;
import mitll.langtest.server.database.annotation.UserAnnotation;
import mitll.langtest.server.database.audio.SlickAudioDAO;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.instrumentation.EventDAO;
import mitll.langtest.server.database.instrumentation.SlickEventImpl;
import mitll.langtest.server.database.phone.Phone;
import mitll.langtest.server.database.phone.PhoneDAO;
import mitll.langtest.server.database.phone.SlickPhoneDAO;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.server.database.refaudio.RefResultDAO;
import mitll.langtest.server.database.refaudio.SlickRefResultDAO;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.server.database.result.ResultDAO;
import mitll.langtest.server.database.result.SlickResultDAO;
import mitll.langtest.server.database.reviewed.ReviewedDAO;
import mitll.langtest.server.database.reviewed.SlickReviewedDAO;
import mitll.langtest.server.database.reviewed.StateCreator;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.database.user.UserDAO;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.server.database.userlist.*;
import mitll.langtest.server.database.word.SlickWordDAO;
import mitll.langtest.server.database.word.Word;
import mitll.langtest.server.database.word.WordDAO;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.npdata.dao.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class CopyToPostgres<T extends CommonShell> {
  private static final Logger logger = LogManager.getLogger(CopyToPostgres.class);
  private static final int WARN_RID_MISSING_THRESHOLD = 50;
  private static final boolean COPY_EVENTS = true;
  private static final int WARN_MISSING_THRESHOLD = 10;
  private static final String QUIZLET_PROPERTIES = "quizlet.properties";
  private static final String NETPROF_PROPERTIES = "netprof.properties";

  private static final String DROP = "drop";
  private static final String COPY = "copy";
  private static final String NETPROF_PROPERTIES_FULL = "/opt/netprof/config/netprof.properties";
  private static final String OPT_NETPROF = "/opt/netprof/import";

  /**
   * @param config
   * @param optionalProperties
   * @param optionalName
   * @param displayOrder
   * @see #main
   */
  private void copyOneConfigCommand(String config, String optionalProperties, String optionalName, int displayOrder) throws Exception {
    DatabaseImpl databaseLight = null;
    try {
      databaseLight = getDatabaseLight(config, true, false, optionalProperties, OPT_NETPROF);
      String language = databaseLight.getLanguage();
      boolean hasModel = databaseLight.getServerProps().hasModel();
      logger.info("loading " + language + " " + hasModel);
      String nameToUse = optionalName.isEmpty() ? language : optionalName;
      new CopyToPostgres().copyOneConfig(databaseLight, new CreateProject().getCC(language), nameToUse, displayOrder, !hasModel);
    } catch (Exception e) {
      logger.error("got " + e, e);
    } finally {
      if (databaseLight != null) {
        databaseLight.close();
      }
    }
  }

  private void dropOneConfig(String config) {
    DatabaseImpl database = getDatabase();
    IProjectDAO projectDAO = database.getProjectDAO();
    int byName = projectDAO.getByName(config);
    if (byName == -1) {
      logger.warn("\n\n\ncouldn't find config " + config);
    } else {
      logger.info("Dropping " + config + " please wait...");
      long then = System.currentTimeMillis();
      projectDAO.delete(byName);
      long now = System.currentTimeMillis();

      Collection<SlickProject> all = projectDAO.getAll();
      logger.info("Took " + (now - then) + " millis to drop " + config + ", now there are " + all.size() + " projects:");

      for (SlickProject project : all) {
        logger.info(" " + project);
      }

    }
    database.close();
  }

/*
  private DatabaseImpl getAndPopulate() {
    return getDatabase().setInstallPath("war", "").populateProjects();
  }
*/

  private static DatabaseImpl getDatabase() {
    File file = new File(NETPROF_PROPERTIES_FULL);
    String name = file.getName();
    String parent = file.getParentFile().getAbsolutePath();

    ServerProperties serverProps = getProps();

    DatabaseImpl database = new DatabaseImpl(parent, name, serverProps.getH2Database(), serverProps,
        new PathHelper("war", serverProps), false, null, false);
    return database;
  }

  private static ServerProperties getProps() {
    File file = new File(NETPROF_PROPERTIES_FULL);
    String name = file.getName();
    String parent = file.getParentFile().getAbsolutePath();
    return new ServerProperties(parent, name);
  }

  /**
   * @param config
   * @param useH2
   * @param optPropsFile
   * @param installPath
   * @return
   * @paramx host
   * @paramx user
   * @paramx pass
   * @see #copyOneConfigCommand
   */
  public static DatabaseImpl getDatabaseLight(String config,
                                              boolean useH2,
                                              boolean useLocal,
                                              String optPropsFile,
                                              String installPath) {
    // logger.info("getDatabaseLight db " + config + " optional props " + optPropsFile);

    String propsFile = optPropsFile != null ? optPropsFile : QUIZLET_PROPERTIES;

    logger.info("getDatabaseLight db " + config + " props " + propsFile);

    File configFile = new File(installPath + File.separator + "config" + File.separator + config + File.separator + propsFile);

    logger.info("getDatabaseLight path " + configFile.getAbsolutePath());

    ServerProperties serverProps = getServerProperties(config, propsFile, installPath);
    if (serverProps == null) {
      return null;
    }

    ServerProperties serverProps2 = getServerProperties("", NETPROF_PROPERTIES, "/opt/netprof");

    readProps(serverProps, serverProps2);

    if (useLocal) {
      serverProps.setLocalPostgres();
    }

    serverProps.setH2(useH2);

    String parent = configFile.getParentFile().getAbsolutePath();
    String name = configFile.getName();

    DatabaseImpl database = new DatabaseImpl(parent, name, serverProps.getH2Database(), serverProps,
        new PathHelper(installPath, serverProps), false, null, false);

    database.setInstallPath(installPath,
        configFile.getParentFile().getAbsolutePath() + File.separator +
            database.getServerProps().getLessonPlan()
    );

    return database;
  }

  private static void readProps(ServerProperties serverProps, ServerProperties serverProps2) {
    String configFileFullPath = serverProps2.getConfigFileFullPath();
    try {
      logger.info("readProps reading from " + configFileFullPath);
      serverProps.getProps().load(new FileInputStream(configFileFullPath));
    } catch (Exception e) {
      logger.error("can't find " + configFileFullPath);
    }
  }

  /**
   * @param config
   * @param propsFile
   * @param installPath
   * @return
   * @see #getDatabaseLight(String, boolean, boolean, String, String)
   */
  private static ServerProperties getServerProperties(String config, String propsFile, String installPath) {
    //String war = "war";
    String configDir = config.isEmpty() ? config : config + File.separator;
    File file = new File(installPath + File.separator +
        "config" + File.separator +
        configDir +
        propsFile);

    if (!file.exists()) {
      logger.error("\n\ngetServerProperties can't find config file " + file.getAbsolutePath());
      // no recovery!
      return null;
    } else {
      return new ServerProperties(file.getParentFile().getAbsolutePath(), file.getName());
    }
  }

  /**
   * @param db
   * @param cc      country code
   * @param optName null OK
   * @param isDev
   * @see #copyOneConfigCommand
   * @seex PostgresTest#testCopy
   */
  public void copyOneConfig(DatabaseImpl db, String cc, String optName, int displayOrder, boolean isDev) throws Exception {
    int projectID = createProjectIfNotExists(db, cc, optName, displayOrder, isDev);  // TODO : course?

    logger.info("copyOneConfig project is " + projectID + " type order is " + db.getTypeOrder(DatabaseImpl.IMPORT_PROJECT_ID));

    // first add the user table
    SlickUserExerciseDAO slickUEDAO = (SlickUserExerciseDAO) db.getUserExerciseDAO();

    // check once if we've added it before
    // TODO : how to drop previous data?
    if (slickUEDAO.isProjectEmpty(projectID)) {
      ResultDAO resultDAO = new ResultDAO(db);

      Map<Integer, Integer> oldToNewUser = new UserCopy().copyUsers(db, projectID, resultDAO, optName);

      Map<Integer, String> idToFL = new HashMap<>();
      Map<String, Integer> exToID = copyUserAndPredefExercisesAndLists(db, projectID, oldToNewUser, idToFL);

      SlickResultDAO slickResultDAO = (SlickResultDAO) db.getResultDAO();
      copyResult(slickResultDAO, oldToNewUser, projectID, exToID, resultDAO, idToFL);

      logger.info("oldToNewUser num = " + oldToNewUser.size() + " exToID num = " + exToID.size());

      // add the audio table
      Map<String, Integer> pathToAudioID = copyAudio(db, oldToNewUser, exToID, projectID);
      logger.info("pathToAudioID num = " + pathToAudioID.size());

      // copy ref results
      copyRefResult(db, oldToNewUser, exToID, pathToAudioID, projectID);

      // add event table
      copyEvents(db, projectID, oldToNewUser, exToID);

      // copy results, words, and phones
      {
        Map<Integer, Integer> oldToNewResult = slickResultDAO.getOldToNew();

        if (oldToNewResult.isEmpty()) {
          logger.error("\n\n\nold to new result is EMPTY!");
        }
        Map<Integer, Integer> oldToNewWordID = copyWordsAndGetIDMap(db, oldToNewResult);

        // phone DAO
        copyPhone(db, oldToNewResult, oldToNewWordID);
      }

      // anno DAO
      copyAnno(db, db.getUserDAO(), oldToNewUser, exToID);

      copyReviewed(db, oldToNewUser, exToID, true);
      copyReviewed(db, oldToNewUser, exToID, false);
    } else {
      logger.info("\n\nProject #" + projectID + " (" + optName + ") already has exercises in it.  Not loading again...\n\n");
    }
  }

  @NotNull
  private Map<Integer, Integer> copyWordsAndGetIDMap(DatabaseImpl db, Map<Integer, Integer> oldToNewResult) {
    logger.info("oldToNewResult " + oldToNewResult.size());
    SlickWordDAO slickWordDAO = (SlickWordDAO) db.getWordDAO();
    copyWord(db, oldToNewResult, slickWordDAO);

    Map<Integer, Integer> oldToNewWordID = slickWordDAO.getOldToNew();
    logger.info("old to new word id  " + oldToNewWordID.size());
    return oldToNewWordID;
  }

  private void copyEvents(DatabaseImpl db, int projectID, Map<Integer, Integer> oldToNewUser, Map<String, Integer> exToID) {
    if (COPY_EVENTS) {
      {
        int defectDetector = db.getUserDAO().getDefectDetector();
        EventDAO other = new EventDAO(db, defectDetector);
        ((SlickEventImpl) db.getEventDAO()).copyTableOnlyOnce(other, projectID, oldToNewUser, exToID);
      }
    }
  }

  private int createProjectIfNotExists(DatabaseImpl db,
                                       String cc,
                                       String optName,
                                       int displayOrder,
                                       boolean isDev) {
    CreateProject createProject = new CreateProject();
    //String cc = createProject.getCC(optName);
    return createProject.createProjectIfNotExists(db, cc, optName, "", displayOrder, isDev);
  }

  /**
   * @param db
   * @param projectID
   * @param oldToNewUser
   * @param idToFL
   * @return
   */
  private Map<String, Integer> copyUserAndPredefExercisesAndLists(DatabaseImpl db,
                                                                  int projectID,
                                                                  Map<Integer, Integer> oldToNewUser,
                                                                  Map<Integer, String> idToFL) {
    Map<String, Integer> exToID = new ExerciseCopy().copyUserAndPredefExercises(db, oldToNewUser, projectID, idToFL);

    SlickUserListDAO slickUserListDAO = (SlickUserListDAO) db.getUserListManager().getUserListDAO();
    copyUserExerciseList(db, oldToNewUser, slickUserListDAO, projectID);

    Map<Integer, Integer> oldToNewUserList = slickUserListDAO.getOldToNew(projectID);
    copyUserExerciseListVisitor(db, oldToNewUser, oldToNewUserList, (SlickUserListExerciseVisitorDAO) db.getUserListManager().getVisitorDAO());
    copyUserExListJoin(db, exToID, projectID);
    return exToID;
  }

  private void copyUserExListJoin(DatabaseImpl db, Map<String, Integer> exToInt, int projectid) {
    SlickUserListDAO slickUserListDAO = (SlickUserListDAO) db.getUserListManager().getUserListDAO();
    Map<Integer, Integer> oldToNewUserList = slickUserListDAO.getOldToNew(projectid);
    copyUserExerciseListJoin(db, oldToNewUserList, exToInt);
  }

  /**
   * @param db
   * @param oldToNewUser
   * @param exToID
   * @param projid
   * @see #copyOneConfig
   */
  private Map<String, Integer> copyAudio(DatabaseImpl db,
                                         Map<Integer, Integer> oldToNewUser,
                                         Map<String, Integer> exToID,
                                         int projid) {
    SlickAudioDAO slickAudioDAO = (SlickAudioDAO) db.getAudioDAO();

    List<SlickAudio> bulk = new ArrayList<>();
    Collection<AudioAttribute> audioAttributes = db.getH2AudioDAO().getAudioAttributesByProjectThatHaveBeenChecked(projid);
    logger.info("copyAudio h2 audio  " + audioAttributes.size());
    int missing = 0;
    int skippedMissingUser = 0;
    Set<String> missingExIDs = new TreeSet<>();
    Map<String, Integer> fileToID = new HashMap<>();
    for (AudioAttribute att : audioAttributes) {
      String oldexid = att.getOldexid();
      Integer id = exToID.get(oldexid);
      if (id != null) {
        // fileToID.put(att.getAudioRef(),att.getExid());
        att.setExid(id);
        SlickAudio slickAudio = slickAudioDAO.getSlickAudio(att, oldToNewUser, projid);
        if (slickAudio != null) {
          // logger.info(slickAudio.toString());
          bulk.add(slickAudio);
        } else skippedMissingUser++;
      } else {
        missingExIDs.add(oldexid);
        if (missing < WARN_MISSING_THRESHOLD) logger.warn("missing ex for " + att + " : " + oldexid);
        missing++;
      }
    }
    long then = System.currentTimeMillis();
    logger.debug("copyAudio add bulk : " + bulk.size() + " audio... " + skippedMissingUser + " were skipped due to missing user");
    slickAudioDAO.addBulk(bulk);
    logger.debug("copyAudio finished adding bulk : " + bulk.size() + " audio...");

    long now = System.currentTimeMillis();

    if (missing > 0) {
      logger.warn("had " + missing + "/" + audioAttributes.size() + " audio att due to missing ex fk : (" + missingExIDs.size() +
          ") : " +
          "" + missingExIDs);
    }

    Map<String, Integer> pairs = slickAudioDAO.getPairs(projid);

//    Collection<AudioAttribute> audioAttributesByProject = slickAudioDAO.getAudioAttributesByProjectThatHaveBeenChecked(projid);
    logger.info("copyAudio took " + (now - then) + " for project " + projid+
        " , postgres audio count = " + pairs.size());


    return pairs;
  }

  /**
   * TODOx : set the exid int field
   *
   * @param db
   * @param dominoUserDAO
   * @param oldToNewUser
   * @param exToID
   */
  private void copyAnno(DatabaseImpl db, IUserDAO dominoUserDAO, Map<Integer, Integer> oldToNewUser,
                        Map<String, Integer> exToID) {
    SlickAnnotationDAO annotationDAO = (SlickAnnotationDAO) db.getAnnotationDAO();
    List<SlickAnnotation> bulk = new ArrayList<>();
    int missing = 0;
    Set<Long> missingUsers = new HashSet<>();
    Collection<UserAnnotation> all = new AnnotationDAO(db, dominoUserDAO).getAll();
    for (UserAnnotation annotation : all) {
      long creatorID = annotation.getCreatorID();
      Integer userID = oldToNewUser.get((int) creatorID);
      if (userID == null) {
        boolean add = missingUsers.add(creatorID);
        if (add) logger.error("copyAnno no user " + creatorID);
      } else {
        annotation.setCreatorID(userID);
        Integer realID = exToID.get(annotation.getOldExID());
        if (realID == null) {
          missing++;
        } else {
          annotation.setExerciseID(realID);
          bulk.add(annotationDAO.toSlick(annotation));
        }
      }
    }
    if (missing > 0) logger.warn("missing " + missing + " out of " + all.size() + " users " + missingUsers);
    annotationDAO.addBulk(bulk);
  }

  private void copyPhone(DatabaseImpl db, Map<Integer, Integer> oldToNewResult, Map<Integer, Integer> oldToNewWordID) {
    SlickPhoneDAO slickPhoneAO = (SlickPhoneDAO) db.getPhoneDAO();
    List<SlickPhone> bulk = new ArrayList<>();
    int c = 0;
    int d = 0;
    int added = 0;
    Set<Long> missingrids = new TreeSet<>();
    Set<Long> missingwids = new TreeSet<>();
    for (Phone phone : new PhoneDAO(db).getAll()) {
      long rid1 = phone.getRid();
      Integer rid = oldToNewResult.get((int) rid1);
      if (rid == null) {
        if (c++ < 50 && missingrids.add(rid1)) logger.error("phone : no rid " + rid1);
      } else {
        long wid1 = phone.getWid();
        Integer wid = oldToNewWordID.get((int) wid1);

        if (wid == null) {
          if (d++ < 50 && missingwids.add(wid1)) logger.error("phone : no word id " + wid1);
        } else {
          phone.setRID(rid);
          phone.setWID(wid);
          bulk.add(slickPhoneAO.toSlick(phone));
        }
      }
    }
    if (c > 0) logger.warn("missing result id fks " + c + " : " + missingrids);
    if (d > 0) logger.warn("missing word   id fks " + d + " : " + missingwids);

    logger.info("copyPhone adding " + bulk.size());
    slickPhoneAO.addBulk(bulk);
    logger.info("copyPhone added  " + slickPhoneAO.getNumRows());
  }

  private final Set<Long> missingRIDs = new HashSet<>();

  /**
   * @param db
   * @param oldToNewResult
   * @param slickWordDAO
   * @see #copyOneConfig(DatabaseImpl, String, String, int, boolean)
   */
  private void copyWord(DatabaseImpl db, Map<Integer, Integer> oldToNewResult, SlickWordDAO slickWordDAO) {
    int c = 0;
    List<SlickWord> bulk = new ArrayList<>();
    for (Word word : new WordDAO(db).getAll()) {
      Integer rid = oldToNewResult.get((int) word.getRid());
      if (rid == null) {
        boolean add = missingRIDs.add(word.getRid());
        if (add && missingRIDs.size() < WARN_RID_MISSING_THRESHOLD)
          logger.error("copyWord word has no rid " + word.getRid());
      } else {
        word.setRid(rid);
        bulk.add(slickWordDAO.toSlick(word));
      }
    }
    if (missingRIDs.size() > 0) logger.warn("word : missing " + missingRIDs.size() + " result id fk references");

    slickWordDAO.addBulk(bulk);
    logger.info("copy word - complete");
  }

  /**
   * TODO : check for empty by project
   *
   * @param db
   * @param oldToNewUserList
   */
  private void copyUserExerciseListJoin(DatabaseImpl db,
                                        Map<Integer, Integer> oldToNewUserList,
                                        Map<String, Integer> exToInt
  ) {
    IUserListManager userListManager = db.getUserListManager();
    IUserListExerciseJoinDAO dao = userListManager.getUserListExerciseJoinDAO();
    SlickUserListExerciseJoinDAO slickUserListExerciseJoinDAO = (SlickUserListExerciseJoinDAO) dao;
    // if (slickUserListExerciseJoinDAO.isEmpty()) {
    Collection<UserListExerciseJoinDAO.Join> all = new UserListExerciseJoinDAO(db).getAll();
    logger.info("copyUserExerciseListJoin copying " + all.size() + " exercise->list joins");
    for (UserListExerciseJoinDAO.Join join : all) {
      int oldID = join.userlistid;
      Integer userListID = oldToNewUserList.get(oldID);
      if (userListID == null) {
        logger.error("UserListManager join can't find user list " + oldID + " in " + oldToNewUserList.size());
      } else {

        String exerciseID = join.exerciseID;
        Integer id = exToInt.get(exerciseID);
//        CommonExercise customOrPredefExercise = null;
        if (id == null) logger.error("Can't find " + exerciseID);
        else {
          //customOrPredefExercise = db.getCustomOrPredefExercise(projectid, id);
          slickUserListExerciseJoinDAO.addPair(userListID, id);
        }
//          logger.info("Adding user exercise join : " +join.userlistid + " adding " + exerciseID + " : " +customOrPredefExercise);
//        if (customOrPredefExercise == null) {
//          logger.error("can't find " + exerciseID + " in " + db.getExercises(projectid).size() + " exercises");
//        } else {
//          slickUserListExerciseJoinDAO.addPair(userListID, customOrPredefExercise.getID());
//        }
      }
    }
  }

/*
  private void logMemory() {
    int MB = (1024 * 1024);
    Runtime rt = Runtime.getRuntime();
    long free = rt.freeMemory();
    long used = rt.totalMemory() - free;
    long max = rt.maxMemory();

    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
    logger.debug(" current thread group " + threadGroup.getName() + " = " + threadGroup.activeCount() +
        " : # cores = " + Runtime.getRuntime().availableProcessors() + " heap info free " + free / MB + "M used " + used / MB + "M max " + max / MB + "M");
  }
*/

  /**
   * TODO : check for empty by project
   *
   * @param db
   * @param oldToNewUser
   * @param slickUserListDAO
   * @see #copyUserAndPredefExercisesAndLists
   */
  private void copyUserExerciseList(DatabaseImpl db,
                                    Map<Integer, Integer> oldToNewUser,
                                    SlickUserListDAO slickUserListDAO,
                                    int projid) {
    Collection<UserList<CommonShell>> oldUserLists = new UserListDAO(db, new UserDAO(db)).getAll();
    int count = 0;
    logger.info("copyUserExerciseList copying " + oldUserLists.size() + " user exercise lists");
    List<SlickUserExerciseList> bulk = new ArrayList<>();

    for (UserList<CommonShell> list : oldUserLists) {
      int oldID = list.getUserID();
      Integer newUserID = oldToNewUser.get(oldID);
      if (newUserID == null) {
        logger.error("UserListManager can't find user " + oldID + " in " + oldToNewUser.size());
      } else {
        SlickUserExerciseList user = slickUserListDAO.toSlick2(list, newUserID, projid);
        bulk.add(user);
        // slickUserListDAO.addWithUser(list, newUserID, projid);
        count++;
      }
    }
    slickUserListDAO.addBulk(bulk);
    logger.info("copyUserExerciseList copied  " + count + " user exercise lists");

  }

  /**
   * TODO : check for empty by project
   *
   * @param db
   * @param oldToNewUser
   * @param oldToNewUserList
   * @param visitorDAO
   */
  private void copyUserExerciseListVisitor(DatabaseImpl db,
                                           Map<Integer, Integer> oldToNewUser,
                                           Map<Integer, Integer> oldToNewUserList,
                                           SlickUserListExerciseVisitorDAO visitorDAO) {
    //if (visitorDAO.isEmpty()) {
    UserExerciseListVisitorDAO uelDAO = new UserExerciseListVisitorDAO(db);
    Collection<UserExerciseListVisitorDAO.Pair> all = uelDAO.getAll();
    logger.info("copying " + all.size() + " user exercise list visitors");

    for (UserExerciseListVisitorDAO.Pair pair : all) {
      int oldID = pair.getUser();
      Integer currentUser = oldToNewUser.get(oldID);
      if (currentUser == null) {
        logger.error("UserListManager can't find user " + oldID);
      } else {
        int olduserlist = pair.getListid();
        Integer current = oldToNewUserList.get(olduserlist);
        if (current == null) logger.error("can't find user list id " + olduserlist);
        else {
          visitorDAO.add(current, currentUser, pair.getWhen());
        }
      }
    }
    //}
  }


  /**
   * @param slickResultDAO
   * @param oldToNewUser
   * @param projid
   * @param exToID
   * @param resultDAO
   * @paramx db
   * @see #copyOneConfig
   */
  private void copyResult(
      SlickResultDAO slickResultDAO,
      Map<Integer, Integer> oldToNewUser,
      int projid,
      Map<String, Integer> exToID,
      ResultDAO resultDAO,
      Map<Integer, String> idToFL) {
    List<SlickResult> bulk = new ArrayList<>();

    List<Result> results = resultDAO.getResults();
    logger.info("copyResult " + projid + " : copying " + results.size() + " results...");

    int missing = 0;
    int missing2 = 0;

//    ExerciseDAO<CommonExercise> exerciseDAO = db.getExerciseDAO(projid);
//    Map<Integer, String> idToFL = Collections.emptyMap();
//    if (exerciseDAO == null) {
//      logger.error("huh? no project " + projid);
//    } else {
//      idToFL = exerciseDAO.getIDToFL(projid);
//    }
    //Map<Integer, String> idToFL = exerciseDAO.getIDToFL(projid);

    logger.info("id-fl has " + idToFL.size() + " items");

    Set<Integer> userids = new HashSet<>();

    for (Result result : results) {
      int userid = result.getUserid();
      Integer userID = oldToNewUser.get(userid);
      if (userID == null) {
        boolean add = userids.add(userid);
        if (add) {
          logger.error("copyResult no user " + userid);
        }
      } else {
        result.setUserID(userID);
        Integer realExID = exToID.get(result.getOldExID());

        if (realExID == null) {
          missing2++;
        } else {
          // TODO : don't - this is really slow - since every call hits exercise table with a select

          //    CommonExercise customOrPredefExercise = realExID == null ? null : db.getCustomOrPredefExercise(realExID);
          //   String transcript = customOrPredefExercise == null ? "" : customOrPredefExercise.getForeignLanguage();
          String transcript = idToFL.get(realExID);
          SlickResult e = slickResultDAO.toSlick(result, projid, exToID, transcript);
          if (e == null) {
            if (missing < 10) logger.warn("missing exid ref " + result.getOldExID());
            missing++;
          } else {
            bulk.add(e);
            if (bulk.size() % 5000 == 0) logger.debug("made " + bulk.size() + " results...");
          }
        }
      }
    }
    if (missing > 0) {
      logger.warn("skipped " + missing + "/" + results.size() +
          "  results b/c of exercise id fk missing");
    }
    if (missing2 > 0) {
      logger.warn("skipped " + missing2 + "/" + results.size() +
          "  results b/c of exercise id fk missing");
    }
    logger.debug("adding " + bulk.size() + " results...");
    slickResultDAO.addBulk(bulk);
    logger.debug("added  " + bulk.size() + " results...");

    //}
  }

  private void copyReviewed(DatabaseImpl db,
                            Map<Integer, Integer> oldToNewUser,
                            Map<String, Integer> exToID,
                            boolean isReviewed) {
    SlickReviewedDAO dao = (SlickReviewedDAO) (isReviewed ? db.getReviewedDAO() : db.getSecondStateDAO());

    String tableName = isReviewed ? ReviewedDAO.REVIEWED : ReviewedDAO.SECOND_STATE;
    ReviewedDAO originalDAO = new ReviewedDAO(db, tableName);
    List<SlickReviewed> bulk = new ArrayList<>();
    Collection<StateCreator> all = originalDAO.getAll();
    logger.info("found " + all.size() + " for " + tableName);
    int missing = 0;
    Set<Integer> missingUsers = new HashSet<>();

    for (StateCreator stateCreator : all) {
      int creatorID = (int) stateCreator.getCreatorID();
      Integer userID = oldToNewUser.get(creatorID);
      if (userID == null) {
        boolean add = missingUsers.add(creatorID);
        if (add) logger.error("copyReviewed no user " + creatorID);
      } else {
        Integer exid = exToID.get(stateCreator.getOldExID());
        if (exid != null) {
          stateCreator.setExerciseID(exid);
          stateCreator.setCreatorID(userID);
          bulk.add(dao.toSlick(stateCreator));
        } else missing++;
      }
    }
    if (missing > 0) {
      logger.warn("missing " + missing + " due to missing ex id fk");
    }
    dao.addBulk(bulk);
  }

  /**
   * @param db
   * @param oldToNewUser
   * @param exToID
   * @param projid
   * @see #copyOneConfig(DatabaseImpl, String, String, int, boolean)
   */
  private void copyRefResult(DatabaseImpl db,
                             Map<Integer, Integer> oldToNewUser,
                             Map<String, Integer> exToID,
                             Map<String, Integer> pathToAudioID,
                             int projid) {
    SlickRefResultDAO dao = (SlickRefResultDAO) db.getRefResultDAO();
    RefResultDAO originalDAO = new RefResultDAO(db, false);
    List<SlickRefResult> bulk = new ArrayList<>();
    Collection<Result> all = originalDAO.getResults();
    logger.info("copyRefResult for project " + projid + " found " + all.size() + " original ref results.");
    logger.info("copyRefResult found " + oldToNewUser.size() + " oldToNewUser entries.");
    logger.info("copyRefResult found " + exToID.size() + " ex to id entries.");
    logger.info("copyRefResult found " + pathToAudioID.size() + " path to audio id entries.");
    int missing = 0;
    Set<Integer> missingUsers = new HashSet<>();
    for (Result result : all) {
      int userid = result.getUserid();
      Integer userID = oldToNewUser.get((int) userid);
      if (userID == null) {
        boolean add = missingUsers.add(userid);
        if (add) logger.warn("copyReviewed no user " + userid);
      } else {
        result.setUserID(userID);
        Integer exid = exToID.get(result.getOldExID());
        if (exid != null) {
          result.setExid(exid);
          String answer = result.getAnswer();
//          logger.info("for " + exid+ " result id " +result.getID() +" got " + answer);
          String[] bestAudios = answer.split("bestAudio");

          Integer audioID = null;

          if (bestAudios.length == 2) {
            String bestAudio = bestAudios[1];
            bestAudio = "bestAudio"+bestAudio;
            audioID = pathToAudioID.get(bestAudio);
            if (audioID == null) logger.warn("can't find '" + bestAudio + "'");
          }

          if (audioID != null) {
            bulk.add(dao.toSlick(projid, result, audioID));
          }

        } else missing++;
      }
    }
    dao.addBulk(bulk);
    if (missing > 0) logger.warn("copyRefResult missing " + missing + " due to missing ex id fk");

    logger.info("copyRefResult added " + dao.getNumResults());
  }

  /**
   * Expects something like:
   * copy english
   * copy pashto pashto1
   *
   * @param arg
   */
  public static void main(String[] arg) {
    if (arg.length < 2) {
      logger.error("Usage : expecting either copy or drop followed by config, e.g. copy spanish");
      logger.error("Usage : optional arguments are display order and name, e.g. copy pashto2 pashtoQuizlet2.properties 1 Pashto Elementary");
      return;
    }
    String action = arg[0];
    String config = arg[1];
    String optconfig = arg.length > 2 ? arg[2] : null;
    String optDisplayOrder = arg.length > 3 ? arg[3] : null;

    StringBuilder builder = new StringBuilder();
    List<String> name = Collections.emptyList();

    if (arg.length > 3) {
      name = Arrays.asList(arg);
      name = name.subList(optconfig == null ? 2 : optDisplayOrder == null ? 3 : 4, name.size());
    }

    for (String n : name) builder.append(n).append(" ");
    String optName = builder.toString().trim();

    int displayOrder = 0;
    try {
      displayOrder = optDisplayOrder == null ? 0 : Integer.parseInt(optDisplayOrder);
    } catch (NumberFormatException e) {
      logger.error("couldn't parse display order " + optDisplayOrder);
    }

    CopyToPostgres copyToPostgres = new CopyToPostgres();

    if (action.equals(DROP)) {
      logger.info("drop " + config);
      try {
        copyToPostgres.dropOneConfig(config);
      } catch (Exception e) {
        logger.error("couldn't drop config " + config, e);
      }
    } else if (action.equals(COPY)) {
      logger.info("copying '" + config + "' '" + optconfig + "' '" + optName + "' order " + displayOrder);
      try {
        copyToPostgres.copyOneConfigCommand(config, optconfig, optName, displayOrder);
      } catch (Exception e) {
        logger.error("couldn't copy config " + config, e);
      }
    }
  }
}
