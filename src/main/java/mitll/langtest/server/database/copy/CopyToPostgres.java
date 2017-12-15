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
import mitll.langtest.server.database.H2DatabaseImpl;
import mitll.langtest.server.database.annotation.AnnotationDAO;
import mitll.langtest.server.database.annotation.SlickAnnotationDAO;
import mitll.langtest.server.database.annotation.UserAnnotation;
import mitll.langtest.server.database.audio.SlickAudioDAO;
import mitll.langtest.server.database.custom.IUserListManager;
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
import mitll.langtest.shared.project.ProjectStatus;
import mitll.npdata.dao.*;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import static mitll.langtest.server.database.copy.CopyToPostgres.ACTION.*;
import static mitll.langtest.server.database.copy.CopyToPostgres.OPTIONS.*;

public class CopyToPostgres<T extends CommonShell> {
  private static final Logger logger = LogManager.getLogger(CopyToPostgres.class);

  private static final int WARN_RID_MISSING_THRESHOLD = 50;
  //  private static final boolean COPY_EVENTS = true;
  private static final int WARN_MISSING_THRESHOLD = 10;
  private static final String QUIZLET_PROPERTIES = "quizlet.properties";
  private static final String NETPROF_PROPERTIES = "netprof.properties";

  enum ACTION {
    COPY("c"), DROP("d"), DROPALL("a"), UNKNOWN("u");

    private String value;

    ACTION(String value) {
      this.value = value;
    }

    String getValue() {
      return value;
    }

    public String toLower() {
      return name().toLowerCase();
    }
  }

  enum OPTIONS {
    NAME("n"), OPTCONFIG("p"), EVAL("e"), ORDER("o"), SKIPREFRESULT("s");
    private String value;

    OPTIONS(String value) {
      this.value = value;
    }

    String getValue() {
      return value;
    }

    public String toLower() {
      return name().toLowerCase();
    }
  }

  public static final String OPT_NETPROF_ROOT = "/opt/netprof";
  private static final String NETPROF_PROPERTIES_FULL = OPT_NETPROF_ROOT + File.separator + "config/netprof.properties";
  private static final String OPT_NETPROF = OPT_NETPROF_ROOT + File.separator + "import";

  /**
   * @param config
   * @param optionalProperties
   * @param optionalName
   * @param displayOrder
   * @param isEval
   * @param skipRefResult
   * @see #main
   */
  private boolean copyOneConfigCommand(String config,
                                       String optionalProperties,
                                       String optionalName,
                                       int displayOrder,
                                       boolean isEval, boolean skipRefResult) {
    CopyToPostgres copyToPostgres = new CopyToPostgres();

    DatabaseImpl databaseLight = null;
    try {
      databaseLight = getDatabaseLight(config, true, false, optionalProperties, OPT_NETPROF);
      String language = databaseLight.getLanguage();
      boolean hasModel = databaseLight.getServerProps().hasModel();

      logger.info("copyOneConfigCommand :" +
          "\n\tloading  " + language +
          "\n\thasModel " + hasModel +
          "\n\tisEval   " + isEval +
          "\n\tmodel    " + databaseLight.getServerProps().getCurrentModel());

      String nameToUse = optionalName == null ? language : optionalName;

      ProjectStatus status = ProjectStatus.PRODUCTION;
      if (!hasModel) status = ProjectStatus.DEVELOPMENT;
      else if (isEval) status = ProjectStatus.EVALUATION;

      copyToPostgres.copyOneConfig(databaseLight, getCreateProject(databaseLight).getCC(language), nameToUse, displayOrder, status, skipRefResult);
      return true;
    } catch (Exception e) {
      logger.error("copyOneConfigCommand : got " + e, e);
      return false;
    } finally {
      if (databaseLight != null) {
        databaseLight.close();
      }
    }
  }

  @NotNull
  private CreateProject getCreateProject(DatabaseImpl databaseLight) {
    return new CreateProject(databaseLight.getServerProps().getHydra2Languages());
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

  private static DatabaseImpl getDatabase() {
    ServerProperties serverProps = getProps();
    return new DatabaseImpl(getProps(), new PathHelper("war", serverProps), null, null);
  }

  public static DatabaseImpl getSimpleDatabase() {
    return new DatabaseImpl(getProps());
  }

  private static ServerProperties getProps() {
    File file = new File(NETPROF_PROPERTIES_FULL);
    if (!file.exists()) logger.error("can't find " + file.getAbsolutePath());
    String parent = file.getParentFile().getAbsolutePath();
    return new ServerProperties(parent, file.getName());
  }

  /**
   * @param config
   * @param useH2
   * @param optPropsFile
   * @param installPath
   * @return
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

    readProps(serverProps, getServerProperties("", NETPROF_PROPERTIES, OPT_NETPROF_ROOT));

    if (useLocal) {
      serverProps.setLocalPostgres();
    }

    serverProps.setH2(useH2);

    String parent = configFile.getParentFile().getAbsolutePath();
    String name = configFile.getName();

    DatabaseImpl database = new H2DatabaseImpl(parent,
        serverProps.getH2Database(),
        serverProps,
        new PathHelper(installPath, serverProps), false, null, false);

    database.setInstallPath(
        configFile.getParentFile().getAbsolutePath() + File.separator +
            database.getServerProps().getLessonPlan(),
        null);

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
   * @param db            to read from
   * @param cc            country code
   * @param optName       non-default name (not language) - null OK
   * @param status        i.e. not production
   * @param skipRefResult
   * @see #copyOneConfigCommand
   */
  public void copyOneConfig(DatabaseImpl db, String cc, String optName, int displayOrder, ProjectStatus status,
                            boolean skipRefResult) throws Exception {
    Collection<String> typeOrder = db.getTypeOrder(DatabaseImpl.IMPORT_PROJECT_ID);

    logger.info("copyOneConfig" +
        "\n\tproject is            " + optName +
        "\n\tcc is                 " + cc +
        "\n\ttype order is         " + typeOrder +
        "\n\tfor import project id " + DatabaseImpl.IMPORT_PROJECT_ID);

    int projectID = createProjectIfNotExists(db, cc, optName, displayOrder, typeOrder, status);  // TODO : course?

    logger.info("copyOneConfig" +
        "\n\tproject #" + projectID +
        "\n\ttype order is " + typeOrder +
        "\n\tfor import project id " + DatabaseImpl.IMPORT_PROJECT_ID);

    // first add the user table
    SlickUserExerciseDAO slickUEDAO = (SlickUserExerciseDAO) db.getUserExerciseDAO();

    // check once if we've added it before
    if (slickUEDAO.isProjectEmpty(projectID)) {
      ResultDAO resultDAO = new ResultDAO(db);
      Map<Integer, Integer> oldToNewUser = new UserCopy().copyUsers(db, projectID, resultDAO, optName);

      Map<Integer, String> idToFL = new HashMap<>();

      logger.info("copyOneConfig type order  " + typeOrder);

      if (typeOrder.isEmpty()) logger.error("huh? type order is empty????\\n\n\n");
      Map<String, Integer> parentExToChild = new HashMap<>();
      Map<String, Integer> exToID = copyUserAndPredefExercisesAndLists(db, projectID, oldToNewUser, idToFL, typeOrder, parentExToChild);

      SlickResultDAO slickResultDAO = (SlickResultDAO) db.getResultDAO();
      copyResult(slickResultDAO, oldToNewUser, projectID, exToID, resultDAO, idToFL, slickUEDAO.getUnknownExerciseID(), db.getUserDAO().getDefaultUser());

      logger.info("oldToNewUser num = " + oldToNewUser.size() + " exToID num = " + exToID.size());

      // add the audio table
      Map<String, Integer> pathToAudioID = copyAudio(db, oldToNewUser, exToID, parentExToChild, projectID, status == ProjectStatus.EVALUATION);
      // logger.info("pathToAudioID num = " + pathToAudioID.size());

      // copy ref results
      if (!skipRefResult) {
        copyRefResult(db, oldToNewUser, exToID, pathToAudioID, projectID);
      }

      // add event table - why events on an old UI?
      // copyEvents(db, projectID, oldToNewUser, exToID);

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
      logger.warn("\n\nProject #" + projectID + " (" + optName + ") already has exercises in it.  Not loading again...\n\n");
    }
  }

  @NotNull
  private Map<Integer, Integer> copyWordsAndGetIDMap(DatabaseImpl db, Map<Integer, Integer> oldToNewResult) {
    logger.info("copyWordsAndGetIDMap oldToNewResult " + oldToNewResult.size());
    SlickWordDAO slickWordDAO = (SlickWordDAO) db.getWordDAO();
    copyWord(db, oldToNewResult, slickWordDAO);

    Map<Integer, Integer> oldToNewWordID = slickWordDAO.getOldToNew();
    logger.info("copyWordsAndGetIDMap old to new word id  " + oldToNewWordID.size());
    return oldToNewWordID;
  }

/*  private void copyEvents(DatabaseImpl db, int projectID, Map<Integer, Integer> oldToNewUser, Map<String, Integer> exToID) {
    if (COPY_EVENTS) {
      {
        int defectDetector = db.getUserDAO().getDefectDetector();
        EventDAO other = new EventDAO(db, defectDetector);
        ((SlickEventImpl) db.getEventDAO()).copyTableOnlyOnce(other, projectID, oldToNewUser, exToID);
      }
    }
  }*/

  private int createProjectIfNotExists(DatabaseImpl db,
                                       String cc,
                                       String optName,
                                       int displayOrder,
                                       Collection<String> typeOrder,
                                       ProjectStatus status) {
    return getCreateProject(db)
        .createProjectIfNotExists(db, cc, optName, "", displayOrder, typeOrder, status);
  }

  /**
   * @param db
   * @param projectID
   * @param oldToNewUser
   * @param idToFL
   * @param typeOrder
   * @return map of parent exercise to context sentence
   * @see #copyOneConfig
   */
  private Map<String, Integer> copyUserAndPredefExercisesAndLists(DatabaseImpl db,
                                                                  int projectID,
                                                                  Map<Integer, Integer> oldToNewUser,
                                                                  Map<Integer, String> idToFL,
                                                                  Collection<String> typeOrder,
                                                                  Map<String, Integer> parentToChild) {
    Map<String, Integer> exToID = new ExerciseCopy().copyUserAndPredefExercises(db, oldToNewUser, projectID, idToFL, typeOrder, parentToChild);

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
   * TODO : replace config/language/bestAudio (E.g. farsi) with bestAudio
   *
   * @param db
   * @param oldToNewUser
   * @param exToID
   * @param projid
   * @param isEval
   * @see #copyOneConfig
   */
  private Map<String, Integer> copyAudio(DatabaseImpl db,
                                         Map<Integer, Integer> oldToNewUser,
                                         Map<String, Integer> exToID,
                                         Map<String, Integer> parentExToChild,
                                         int projid,
                                         boolean isEval) {
    SlickAudioDAO slickAudioDAO = (SlickAudioDAO) db.getAudioDAO();

    List<SlickAudio> bulk = new ArrayList<>();
    Collection<AudioAttribute> audioAttributes = db.getH2AudioDAO().getAudioAttributesByProjectThatHaveBeenChecked(projid, false);
    logger.info("copyAudio h2 audio  " + audioAttributes.size());
    int missing = 0;
    int skippedMissingUser = 0;
    Set<String> missingExIDs = new TreeSet<>();

    for (AudioAttribute att : audioAttributes) {
      String oldexid = att.getOldexid();
      Integer id = getModernIDForExercise(exToID, parentExToChild, att, oldexid);
      if (id != null) {
        att.setExid(id); // set the exercise id reference - either to a normal exercise or to a context exercise
        SlickAudio slickAudio = slickAudioDAO.getSlickAudio(att, oldToNewUser, projid);

        if (slickAudio != null) {
          bulk.add(slickAudio);
        } else {
          skippedMissingUser++;
        }
      } else {
        missingExIDs.add(oldexid);
        if (missing < WARN_MISSING_THRESHOLD) logger.warn("copyAudio missing ex for " + att + " : " + oldexid);
        missing++;
      }
    }

    long then = System.currentTimeMillis();
    logger.debug("copyAudio start    adding bulk : " + bulk.size() + " audio... " + skippedMissingUser + " were skipped due to missing user");
    slickAudioDAO.addBulk(bulk);
    logger.debug("copyAudio finished adding bulk : " + bulk.size() + " audio...");

    long now = System.currentTimeMillis();

    if (missing > 0) {
      logger.warn("copyAudio had " + missing + "/" + audioAttributes.size() + " audio att due to missing ex fk : (" + missingExIDs.size() +
          ") : " +
          "" + missingExIDs);
    }

    Map<String, Integer> pairs = slickAudioDAO.getPairs(projid);
    logger.info("copyAudio took " + (now - then) + " for project " + projid + " , postgres audio count = " + pairs.size());

    return pairs;
  }

  /**
   * @param exToID
   * @param parentExToChild
   * @param att
   * @param oldexid
   * @return
   * @see #copyAudio(DatabaseImpl, Map, Map, Map, int, boolean)
   */
  private Integer getModernIDForExercise(Map<String, Integer> exToID, Map<String, Integer> parentExToChild, AudioAttribute att, String oldexid) {
    Integer id = exToID.get(oldexid);
    if (att.isContextAudio()) {
//      logger.info("copyAudio att " + att.getUniqueID() + " for ex  " + oldexid + " is context " + att.getAudioRef());
      Integer childID = parentExToChild.get(oldexid);
      if (childID == null) {
        //logger.info("copyAudio huh? no child for " + oldexid);
      } else {
        id = childID;
      }
    }
    return id;
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
        if (add) logger.warn("copyAnno no user " + creatorID);
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
    // int added = 0;
    Set<Long> missingrids = new TreeSet<>();
    Set<Long> missingwids = new TreeSet<>();
    for (Phone phone : new PhoneDAO(db).getAll()) {
      long rid1 = phone.getRid();
      Integer rid = oldToNewResult.get((int) rid1);
      if (rid == null) {
        if (c++ < 50 && missingrids.add(rid1)) logger.warn("copyPhone phone : no rid " + rid1);
      } else {
        long wid1 = phone.getWid();
        Integer wid = oldToNewWordID.get((int) wid1);

        if (wid == null) {
          if (d++ < 50 && missingwids.add(wid1)) logger.warn("copyPhone phone : no word id " + wid1);
        } else {
          phone.setRID(rid);
          phone.setWID(wid);
          bulk.add(slickPhoneAO.toSlick(phone));
        }
      }
    }
    if (c > 0) logger.warn("missing result id fks " + c + " : " + missingrids);
    if (d > 0) logger.warn("missing word   id fks " + d + " : " + missingwids);

    long then = System.currentTimeMillis();
    logger.info("copyPhone adding " + bulk.size());
    slickPhoneAO.addBulk(bulk);
    logger.info("copyPhone added  " + slickPhoneAO.getNumRows() + " took " + (System.currentTimeMillis() - then) + " millis.");
  }

  private final Set<Long> missingRIDs = new HashSet<>();

  /**
   * @param db
   * @param oldToNewResult
   * @param slickWordDAO
   * @see #copyOneConfig
   */
  private void copyWord(DatabaseImpl db, Map<Integer, Integer> oldToNewResult, SlickWordDAO slickWordDAO) {
    int c = 0;
    List<SlickWord> bulk = new ArrayList<>();
    for (Word word : new WordDAO(db).getAll()) {
      Integer rid = oldToNewResult.get((int) word.getRid());
      if (rid == null) {
        boolean add = missingRIDs.add(word.getRid());
        if (add && missingRIDs.size() < WARN_RID_MISSING_THRESHOLD)
          logger.warn("copyWord word has no rid " + word.getRid());
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
   * @see #copyUserExListJoin
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
        logger.error("copyUserExerciseListJoin UserListManager join can't find user list " + oldID + " in " + oldToNewUserList.size());
      } else {
        String exerciseID = join.exerciseID;
        Integer id = exToInt.get(exerciseID);
//        CommonExercise customOrPredefExercise = null;
        if (id == null)
          logger.warn("copyUserExerciseListJoin Can't find exercise " + exerciseID + " in " + exToInt.size() + " ex->int map");
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
        logger.warn("UserListManager can't find user " + oldID + " in " + oldToNewUser.size());
      } else {
        SlickUserExerciseList user = slickUserListDAO.toSlick2(list, newUserID, projid, -1);
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
   * Make sure all results are copied, even when we have missing user id or exercise references.
   *
   * @param slickResultDAO
   * @param oldToNewUser
   * @param projid
   * @param exToID
   * @param resultDAO
   * @see #copyOneConfig
   */
  private void copyResult(
      SlickResultDAO slickResultDAO,
      Map<Integer, Integer> oldToNewUser,
      int projid,
      Map<String, Integer> exToID,
      ResultDAO resultDAO,
      Map<Integer, String> idToFL,
      int unknownExerciseID,
      int unknownUserID) {
    List<SlickResult> bulk = new ArrayList<>();

    List<Result> results = resultDAO.getResults();
    logger.info("copyResult " + projid + " : copying " + results.size() + " results...");

    //  int missing = 0;
    int missing2 = 0;

    logger.info("copyResult id->fl has " + idToFL.size() + " items");

    Set<Integer> missingUserIDs = new HashSet<>();

    for (Result result : results) {
      int oldUserID = result.getUserid();
      Integer userID = oldToNewUser.get(oldUserID);
      if (userID == null) {
        boolean add = missingUserIDs.add(oldUserID);
        if (add) {
          logger.error("copyResult no user " + oldUserID);
        }
        userID = unknownUserID;
      }

      result.setUserID(userID);
      Integer realExID = exToID.get(result.getOldExID());

      if (realExID == null) {
        missing2++;
        realExID = unknownExerciseID;
      }
      //else {
      String transcript = idToFL.get(realExID);

      SlickResult e = slickResultDAO.toSlick(result, projid, realExID, transcript == null ? "no transcript found" : transcript);
//        if (e == null) {
//          if (missing < 10 || true) logger.warn("missing exid ref " + result.getOldExID() + " so skipping " + result);
//          missing++;
//        } else {
      bulk.add(e);
      if (bulk.size() % 5000 == 0) logger.debug("made " + bulk.size() + " results...");
      //   }
      //}

    }
//    if (missing > 0) {
//      logger.warn("skipped " + missing + "/" + results.size() +
//          "  results b/c of exercise id fk missing");
//    }
    if (missing2 > 0) {
      logger.warn("skipped " + missing2 + "/" + results.size() +
          "  results b/c of exercise id fk missing (old->new ids)");
    }
    if (!missingUserIDs.isEmpty()) {
      logger.warn("found " + missingUserIDs.size() + " missing users " + missingUserIDs);
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
        if (add) logger.warn("copyReviewed no user " + creatorID);
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
   * @see #copyOneConfig
   */
  private void copyRefResult(DatabaseImpl db,
                             Map<Integer, Integer> oldToNewUser,
                             Map<String, Integer> exToID,
                             Map<String, Integer> pathToAudioID,
                             int projid) {
    SlickRefResultDAO dao = (SlickRefResultDAO) db.getRefResultDAO();
     List<SlickRefResult> bulk = new ArrayList<>();
    Collection<Result> toImport = new RefResultDAO(db, false).getResults();
    logger.info("copyRefResult for project " + projid + " found " + toImport.size() + " original ref results.");
    logger.info("copyRefResult found " + oldToNewUser.size() + " oldToNewUser entries.");
    logger.info("copyRefResult found " + exToID.size() + " ex to id entries.");
    logger.info("copyRefResult found " + pathToAudioID.size() + " path to audio id entries.");
    int missing = 0;
    Set<Integer> missingUsers = new HashSet<>();
    for (Result result : toImport) {
      int userid = result.getUserid();
      Integer userID = oldToNewUser.get(userid);
      if (userID == null) {
        boolean add = missingUsers.add(userid);
        if (add) logger.warn("copyRefResult no user " + userid);
      } else {
        result.setUserID(userID);
        Integer exid = exToID.get(result.getOldExID());
        if (exid != null) {
          result.setExid(exid);
          String answer = result.getAnswer();
//          logger.info("for " + exid+ " result id " +result.getID() +" got " + answer);
          Integer audioID = getAudioID(pathToAudioID, answer);

          if (audioID == null) {
            logger.warn("copyRefResult : can't find audio from audio table at '" + answer + "'");
          } else {
            bulk.add(dao.toSlick(projid, result, audioID));
          }

        } else missing++;
      }
    }
    logger.info("copyRefResult START : copying " + bulk.size() + " ref result. Currently has " + dao.getNumResults());

    long then = System.currentTimeMillis();
    dao.addBulk(bulk);
    long now = System.currentTimeMillis();
    long diff = (now - then) / 1000;
    if (missing > 0) logger.warn("copyRefResult missing " + missing + " due to missing ex id fk");

    logger.info("copyRefResult END   : added " + bulk.size() + " and now has " + dao.getNumResults() + " took " + diff + " seconds");
  }

  private Integer getAudioID(Map<String, Integer> pathToAudioID, String answer) {
    String[] bestAudios = answer.split("bestAudio");

    Integer audioID = null;

    if (bestAudios.length == 2) {
      String bestAudio = bestAudios[1];
      bestAudio = "bestAudio" + bestAudio;
      audioID = pathToAudioID.get(bestAudio);
      //   if (audioID == null) logger.warn("copyRefResult : can't find '" + bestAudio + "'");
    } else {
      audioID = pathToAudioID.get(answer);
      logger.info("path " + answer + " audio id " + audioID);
    }
    return audioID;
  }

  /**
   * Expects something like:
   * <p>
   * copy english
   * copy english -isEval optName
   * 0    1       2       3
   * copy pashto pashto1
   * drop pashto
   * dropAll destroy
   *
   * @param args
   */
  public static void main(String[] args) {
    Options options = getOptions();

    CommandLineParser parser = new GnuParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd;

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      logger.error(e.getMessage());
      formatter.printHelp("copy", options);

      System.exit(1);
      return;
    }

    ACTION action = UNKNOWN;
    String config = null;
    String dropConfirm = null;
    String optName = null;
    String optConfigValue = null;
    int displayOrderValue = 0;
    boolean isEval, skipRefResult;
    if (cmd.hasOption(COPY.toLower())) {
      action = COPY;
      config = cmd.getOptionValue(COPY.toLower());
    } else if (cmd.hasOption(DROP.toLower())) {
      action = DROP;
      config = cmd.getOptionValue(COPY.toLower());
    } else if (cmd.hasOption(DROPALL.toLower())) {
      action = DROPALL;
      dropConfirm = cmd.getOptionValue(DROPALL.toLower());
    }

    if (cmd.hasOption(NAME.toLower())) {
      optName = cmd.getOptionValue(NAME.toLower());
    }
    if (cmd.hasOption(OPTCONFIG.toLower())) {
      optConfigValue = cmd.getOptionValue(OPTCONFIG.toLower());
    }

    if (cmd.hasOption(ORDER.toLower())) {
      String optionValue = "";
      try {
        optionValue = cmd.getOptionValue(ORDER.toLower());
        displayOrderValue = Integer.parseInt(optionValue);
      } catch (NumberFormatException e) {
        logger.error("couldn't parse " + ORDER + " = " + optionValue);
        formatter.printHelp("copy", options);
        return;
      }
    }

    isEval = cmd.hasOption(EVAL.toLower());
    skipRefResult = cmd.hasOption(SKIPREFRESULT.toLower());
    logger.info("action = " + action);

    CopyToPostgres copyToPostgres = new CopyToPostgres();

    switch (action) {
      case UNKNOWN:
//        logger.error("not sure what to do with action ");
        formatter.printHelp("copy", options);
        break;
      case DROP:
        logger.info("drop " + config);
        try {
          copyToPostgres.dropOneConfig(config);
        } catch (Exception e) {
          logger.error("couldn't drop config " + config, e);
        }
        break;
      case COPY:
        logger.info("copying " +
            "\nconfig    '" + config + "' " +
            "\noptconfig '" + optConfigValue + "' " +
            "\nname      '" + optName + "'" +
            "\norder     " + displayOrderValue +
            "\neval " + isEval
        );
        try {
          boolean b = copyToPostgres.copyOneConfigCommand(config, optConfigValue, optName, displayOrderValue, isEval, skipRefResult);
          if (!b) {
            System.exit(1);
          }
        } catch (Exception e) {
          logger.error("couldn't copy config " + config, e);
        }
        break;
/*      case COPYALL:
        logger.info("copying '" + config + "' '" + optconfig + "' '" + optName + "' order " + displayOrder);
        try {
          List<String> configs = new ArrayList<>();
          for (int i =1;i<arg.length; i++) configs.add(arg[i]);
          copyToPostgres.copySeveral(configs);
        } catch (Exception e) {
          logger.error("couldn't copy config " + config, e);
        }
        break;*/
      case DROPALL:
        logger.warn("really be sure that this is only during development and not during production!");
        if (dropConfirm.equals("destroy")) {
          doDropAll();
        } else {
          logger.info("please check with Gordon or Ray or somebody like that before doing this.");
        }
        break;
      default:
        formatter.printHelp("copy", options);
    }
  }

  @NotNull
  private static Options getOptions() {
    Options options = new Options();

    Option copy = new Option(COPY.getValue(), COPY.toLower(), true, "copy this config or language into netprof");
    copy.setRequired(false);
    options.addOption(copy);

    Option drop = new Option(DROP.getValue(), DROP.toLower(), true, "drop this config from netprof database");
    drop.setRequired(false);
    options.addOption(drop);

    Option dropAll = new Option(DROPALL.getValue(), DROPALL.toLower(), true, "drop all tables in the netprof database");
    dropAll.setRequired(false);
    options.addOption(dropAll);


    Option optConfig = new Option(OPTCONFIG.getValue(), OPTCONFIG.toLower(), true, "optional properties file within config directory (e.g. for pashto)");
    optConfig.setRequired(false);
    options.addOption(optConfig);

    Option name = new Option(NAME.getValue(), NAME.toLower(), true, "optional name for the project (different from config)");
    name.setRequired(false);
    options.addOption(name);

    Option eval = new Option(EVAL.getValue(), EVAL.toLower(), false, "mark the imported project as at the eval step and use project specific audio");
    eval.setRequired(false);
    options.addOption(eval);


    {
      Option displayOrder = new Option(ORDER.getValue(), ORDER.toLower(), true, "display order among projects of the same language");
      displayOrder.setRequired(false);
      options.addOption(displayOrder);
    }
    {
      Option skip = new Option(SKIPREFRESULT.getValue(), SKIPREFRESULT.toLower(), false, "skip loading ref result table (if you want to recalculate reference audio alignment)");
      skip.setRequired(false);
      options.addOption(skip);
    }
    return options;
  }

  /**
   * Drop all doesn't require mongo connection, etc.
   */
  private static void doDropAll() {
    DatabaseImpl database = null;
    try {
      logger.warn("OK hope this is what you want.");
      database = getSimpleDatabase();
      database.dropAll();
    } catch (Exception e) {
      logger.error("couldn't drop all tables, got " + e, e);
      String concat = database.getTables().concat(",\n");
      logger.info("doDropAll now there are " + concat);
    } finally {
      try {
        if (database != null) {
          database.close();
        }
      } catch (Exception e) {
        logger.error("Got " + e, e);
      }
    }
  }
}
