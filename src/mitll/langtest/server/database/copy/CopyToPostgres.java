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
import mitll.langtest.server.database.result.*;
import mitll.langtest.server.database.reviewed.ReviewedDAO;
import mitll.langtest.server.database.reviewed.SlickReviewedDAO;
import mitll.langtest.server.database.reviewed.StateCreator;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.server.database.user.IUserProjectDAO;
import mitll.langtest.server.database.user.SlickUserDAOImpl;
import mitll.langtest.server.database.user.UserDAO;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.server.database.userexercise.UserExerciseDAO;
import mitll.langtest.server.database.userlist.*;
import mitll.langtest.server.database.word.SlickWordDAO;
import mitll.langtest.server.database.word.Word;
import mitll.langtest.server.database.word.WordDAO;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class CopyToPostgres<T extends CommonShell> {
  private static final Logger logger = Logger.getLogger(CopyToPostgres.class);
  private static final int WARN_RID_MISSING_THRESHOLD = 50;
  private static final boolean COPY_EVENTS = true;
  private static final boolean DEBUG = false;

  /**
   * @param config
   * @param inTest
   * @see #main(String[])
   */
  private void copyOneConfig(String config, boolean inTest) {
    DatabaseImpl databaseLight = getDatabaseLight(config, inTest);
    new CopyToPostgres().copyOneConfig(databaseLight, getCC(config), null, 0);
    databaseLight.destroy();
  }


  /**
   * Add brazilian, serbo croatian, french, etc.
   *
   * @param config
   * @return
   */
  public String getCC(String config) {
    String cc = "";
    List<String> languages = Arrays.asList(
        "dari",
        "egyptian",
        "english",
        "farsi",
        "french",
        "german",
        "korean",
        "iraqi",
        "japanese",
        "levantine",
        "mandarin",
        "msa",
        "pashto",
        "portuguese",
        "russian",
        "spanish",
        "sudanese",
        "tagalog",
        "turkish",
        "urdu");

    List<String> flags = Arrays.asList(
        "af",
        "eg",
        "us",
        "ir",
        "fr",
        "de",
        "kr",
        "iq",
        "jp",
        "sy",
        "cn",
        "al",
        "af",
        "pt",
        "ru",
        "es",
        "ss",
        "ph",
        "tr",
        "pk");

    int i = languages.indexOf(config.toLowerCase());
    cc = flags.get(i);
    return cc;
  }

  private void testDrop(String config, boolean inTest) {
    DBConnection connection = getConnection(config, inTest);
    connection.dropAll();
  }

  private static DBConnection getConnection(String config, boolean inTest) {
    File file = getConfigFile(config, null, inTest);
    String parent = file.getParentFile().getAbsolutePath();
    ServerProperties serverProps = new ServerProperties(parent, file.getName());
/*    return new DBConnection(serverProps.getDatabaseType(),
        serverProps.getDatabaseHost(),
        serverProps.getDatabasePort(),
        serverProps.getDatabaseName(),
        serverProps.getDatabaseUser(),
        serverProps.getDatabasePassword());*/

    return new DBConnection(serverProps.getDBConfig());
  }

  private static DatabaseImpl getDatabaseLight(String config, boolean inTest) {
    File file = getConfigFile(config, null, inTest);
    String parent = file.getParentFile().getAbsolutePath();
    ServerProperties serverProps = new ServerProperties(parent, file.getName());
    serverProps.setH2(true);
    DatabaseImpl database = getDatabaseVeryLight(config, inTest);
    String installPath = getInstallPath(inTest);
    database.setInstallPath(installPath, parent + File.separator + database.getServerProps().getLessonPlan(),
        serverProps.getMediaDir());
    return database;
  }

  private static File getConfigFile(String config, String optPropsFile, boolean inTest) {
    String propsFile = "quizlet.properties";
    return new File((inTest ? "war" + File.separator : "") + "config" + File.separator + config + File.separator + propsFile);
  }

  private static String getInstallPath(boolean inTest) {
    return inTest ? "war" : ".";
  }

  private static DatabaseImpl getDatabaseVeryLight(String config, boolean inTest) {
    File file = getConfigFile(config, null, inTest);
    String name = file.getName();
    String parent = file.getParentFile().getAbsolutePath();

    logger.info("path is " + parent);
    ServerProperties serverProps = new ServerProperties(parent, name);
    serverProps.setH2(true);
    return new DatabaseImpl(parent, name, serverProps.getH2Database(), serverProps,
        new PathHelper(getInstallPath(inTest), serverProps), false, null, true);
  }

  /**
   * @param db
   * @param cc
   * @param optName null OK
   * @see #copyOneConfig(String, boolean)
   */
  public void copyOneConfig(DatabaseImpl db, String cc, String optName, int displayOrder) {
    int projectID = createProjectIfNotExists(db, cc, optName, "", displayOrder);  // TODO : course?

    logger.info("copyOneConfig project is " + projectID);

    // first add the user table
    SlickUserDAOImpl slickUserDAO = (SlickUserDAOImpl) db.getUserDAO();
    SlickResultDAO slickResultDAO = (SlickResultDAO) db.getResultDAO();
    SlickUserExerciseDAO slickUEDAO = (SlickUserExerciseDAO) db.getUserExerciseDAO();

    // check once if we've added it before
    // TODO : how to drop previous data?
    if (slickUEDAO.isProjectEmpty(projectID)) {
      ResultDAO resultDAO = new ResultDAO(db);

      Map<Integer, Integer> oldToNewUser = copyUsers(db, projectID, resultDAO);

      Map<Integer, String> idToFL = new HashMap<>();
      Map<String, Integer> exToID = copyUserAndPredefExercisesAndLists(db, projectID, oldToNewUser, idToFL);

      copyResult(db, slickResultDAO, oldToNewUser, projectID, exToID, resultDAO, idToFL);

      logger.info("oldToNewUser " + oldToNewUser.size() + " exToID " + exToID.size());

      // add the audio table
      copyAudio(db, oldToNewUser, exToID, projectID);

      // add event table
      int defectDetector = db.getUserDAO().getDefectDetector();

      if (COPY_EVENTS) {
        {
          EventDAO other = new EventDAO(db, defectDetector);
          ((SlickEventImpl) db.getEventDAO()).copyTableOnlyOnce(other, projectID, oldToNewUser, exToID);
        }
      }

      {
        Map<Integer, Integer> oldToNewResult = slickResultDAO.getOldToNew();

        if (oldToNewResult.isEmpty()) {
          logger.error("\n\n\nold to new result is EMPTY!");
        }
        logger.info("oldToNewResult " + oldToNewResult.size());
        SlickWordDAO slickWordDAO = (SlickWordDAO) db.getWordDAO();
        copyWord(db, oldToNewResult, slickWordDAO);

        Map<Integer, Integer> oldToNewWordID = slickWordDAO.getOldToNew();
        logger.info("old to new word id  " + oldToNewWordID.size());

        // phone DAO
        copyPhone(db, oldToNewResult, oldToNewWordID);
      }


      // anno DAO
      copyAnno(db, slickUserDAO, oldToNewUser, exToID);

      copyReviewed(db, oldToNewUser, exToID, true);
      copyReviewed(db, oldToNewUser, exToID, false);
      copyRefResult(db, oldToNewUser, exToID);
    } else {
      logger.info("\n\nProject " + projectID + " already has exercises in it.  Not loading again...");
    }
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
    Map<String, Integer> exToID = copyUserAndPredefExercises(db, oldToNewUser, projectID, idToFL);

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
   * TODO : what to do about pashto 1,2,3?
   *
   * @param db
   * @param countryCode
   * @param course
   * @return
   * @see #copyOneConfig
   */
  public int createProjectIfNotExists(DatabaseImpl db, String countryCode, String optName, String course, int displayOrder) {
    IProjectDAO projectDAO = db.getProjectDAO();
    String oldLanguage = getOldLanguage(db);
    String name = optName != null ? optName : oldLanguage;

    int byName = projectDAO.getByName(name);

    if (byName == -1) {
      logger.info("checking for project with name '" + name + "' opt '" + optName + "' language '" + oldLanguage +
          "' - non found");

      byName = createProject(db, projectDAO, countryCode, name, course, displayOrder);

      db.populateProjects(true);
    } else {
      logger.info("found project " + byName + " for language '" + oldLanguage + "'");
    }
    return byName;
  }

  private String getOldLanguage(DatabaseImpl db) {
    return db.getLanguage();
  }

  /**
   * Ask the database for what the type order should be, e.g. [Unit, Chapter] or [Week, Unit] (from Dari)
   *
   * @param db
   * @param projectDAO
   * @param name
   * @param course
   * @param displayOrder
   * @see #createProjectIfNotExists(DatabaseImpl, String, String, String)
   */
  private int createProject(DatabaseImpl db, IProjectDAO projectDAO, String countryCode, String name, String course, int displayOrder) {
    Iterator<String> iterator = db.getTypeOrder(-1).iterator();
    String firstType = iterator.hasNext() ? iterator.next() : "";
    String secondType = iterator.hasNext() ? iterator.next() : "";
    String language = getOldLanguage(db);

    SlickUserDAOImpl slickUserDAO = (SlickUserDAOImpl) db.getUserDAO();

    if (language.equals("msa")) language = "MSA";

    int byName = projectDAO.add(slickUserDAO.getBeforeLoginUser(), name, language, course, firstType, secondType,
        countryCode, displayOrder);

    Properties props = db.getServerProps().getProps();
    for (String prop : ServerProperties.CORE_PROPERTIES) {
      String property = props.getProperty(prop);
      if (property != null) {
        projectDAO.addProperty(byName, prop, property);
      }
    }

    logger.info("created project " + byName);
    return byName;
  }

  /**
   * What can happen:
   * <p>
   * 1) User id and password match - no action -they're already there
   * 2) User id matches, but password doesn't - go ahead and add the user id and password
   * -- could be multiple users with same userid but different password... trouble later on?
   * 3) User id is new - add a new user
   * <p>
   * Worry about how to merge two or more user tables -
   * find existing ids in postgres...
   * if some from import db aren't there, add them
   * return map of old ids -> new ids
   * <p>
   * For collisions -- must reset password -- someone loses.
   * <p>
   * ??? Also, going forward, we must store emails, since we need to be able to send the sign up message?
   *
   * @param db
   * @param projid
   * @see #copyOneConfig(DatabaseImpl, String, String)
   */
  private Map<Integer, Integer> copyUsers(DatabaseImpl db, int projid, IResultDAO oldResultDAO) {
    SlickUserDAOImpl slickUserDAO = (SlickUserDAOImpl) db.getUserDAO();

    Map<Integer, Integer> oldToNew = new HashMap<>();
    addDefaultUsers(oldToNew, slickUserDAO);

    IUserProjectDAO slickUserProjectDAO = db.getUserProjectDAO();

    UserDAO userDAO = new UserDAO(db);
    int defectDetector = userDAO.getDefectDetector();
    oldToNew.put(defectDetector, slickUserDAO.getDefectDetector());

    List<User> importUsers = userDAO.getUsers();
    logger.info("copyUsers h2 importUsers  " + importUsers.size());
    UserToCount userToNumAnswers = oldResultDAO.getUserToNumAnswers();
    Map<Integer, Integer> idToCount = userToNumAnswers.getIdToCount();
    if (DEBUG) logger.info("id->count " + idToCount.size() + " values " + idToCount.values().size());

    int collisions = 0;
    int lurker = 0;
    List<SlickUser> added = new ArrayList<>();
    for (User toImport : importUsers) {
      int importID = toImport.getId();
      if (importID != defectDetector) {
        String importUserID = toImport.getUserID();
        String passwordHash = toImport.getPasswordHash();
        if (passwordHash == null) passwordHash = "";

        if (DEBUG) logger.info("import " + toImport);
        User strictUserWithPass = slickUserDAO.getStrictUserWithPass(importUserID, passwordHash);

        if (strictUserWithPass != null) {
          // do nothing, but remember id mapping
          oldToNew.put(importID, strictUserWithPass.getId());
        } else {

          if (importUserID.isEmpty() && idToCount.get(importID) != null && idToCount.get(importID) == 0) {
            logger.info("skipping old user " + toImport + " since they have an empty user name and no recordings");
            lurker++;
          } else {
            User userByID1 = slickUserDAO.getUserByID(importUserID);

            if (userByID1 != null) {
              if (DEBUG) logger.info("found existing user " + importUserID + " : " + userByID1);
              // User "adam" already exists with a different password - what to do?
              // void current password! Force them to set it again when they log in again
              int existingID = userByID1.getId();
              if (!userByID1.getPasswordHash().isEmpty()) {
                slickUserDAO.changePassword(existingID, "");
              }
              oldToNew.put(importID, existingID);
              collisions++;
              logger.info("user collision to project " + projid + " map " + importID + "->" + existingID +
                  " : " + userByID1);
            } else {
              added.add(addUser(slickUserDAO, oldToNew, toImport));
            }
          }
        }
      }
    }

    logger.info("adding user->project for " + projid);
    List<SlickUserProject> toAdd = new ArrayList<>();
    Timestamp modified = new Timestamp(System.currentTimeMillis());
    for (SlickUser user : added) {
      toAdd.add(new SlickUserProject(-1, user.id(), projid, modified));
    }
    slickUserProjectDAO.addBulk(toAdd);
    logger.info("after, postgres importUsers " +
        "num = " + slickUserDAO.getUsers().size() +
        " added " + added.size() +
        " collisions " + collisions +
        " lurker " + lurker
    );
    return oldToNew;
  }

  private void addDefaultUsers(Map<Integer, Integer> oldToNew, SlickUserDAOImpl slickUserDAO) {
    oldToNew.put(BaseUserDAO.DEFAULT_USER_ID, slickUserDAO.getDefaultUser());
    oldToNew.put(BaseUserDAO.DEFAULT_MALE_ID, slickUserDAO.getDefaultMale());
    oldToNew.put(BaseUserDAO.DEFAULT_FEMALE_ID, slickUserDAO.getDefaultFemale());
  }

  /**
   * @param slickUserDAO
   * @param oldToNew
   * @param toImport
   * @return
   * @see #copyUsers
   */
  private SlickUser addUser(SlickUserDAOImpl slickUserDAO,
                            Map<Integer, Integer> oldToNew,
                            User toImport) {
//    logger.info("addUser " + toImport + " with " + toImport.getPermissions());
    SlickUser user = slickUserDAO.toSlick(toImport);
    SlickUser slickUser = slickUserDAO.addAndGet(user, toImport.getPermissions());
    int add = slickUser.id();
//    logger.info("addUser id  " + add + " for " + user.id() + " equal " + (user == slickUser));
    //   logger.info("addUser map " + toImport.getId() + " -> " + add);
    oldToNew.put(toImport.getId(), add);
    return slickUser;
  }

  /**
   * @param db
   * @param oldToNewUser
   * @param exToID
   * @param projid
   * @see #copyOneConfig
   */
  private void copyAudio(DatabaseImpl db,
                         Map<Integer, Integer> oldToNewUser,
                         Map<String, Integer> exToID, int projid) {
    SlickAudioDAO slickAudioDAO = (SlickAudioDAO) db.getAudioDAO();

    List<SlickAudio> bulk = new ArrayList<>();
    Collection<AudioAttribute> audioAttributes = db.getH2AudioDAO().getAudioAttributesByProject(projid);
    logger.info("h2 audio  " + audioAttributes.size());
    int missing = 0;
    int skippedMissingUser = 0;
    Set<String> missingExIDs = new TreeSet<>();
    for (AudioAttribute att : audioAttributes) {
      String oldexid = att.getOldexid();
      Integer id = exToID.get(oldexid);
      if (id != null) {
        att.setExid(id);
        SlickAudio slickAudio = slickAudioDAO.getSlickAudio(att, oldToNewUser, projid);
        if (slickAudio != null) {
          // logger.info(slickAudio.toString());
          bulk.add(slickAudio);
        } else skippedMissingUser++;
      } else {
        missingExIDs.add(oldexid);
        if (missing < 50) logger.warn("missing ex for " + att + " : " + oldexid);
        missing++;
      }
    }
    long then = System.currentTimeMillis();
    logger.debug("add bulk : " + bulk.size() + " audio... " + skippedMissingUser + " were skipped due to missing user");
    slickAudioDAO.addBulk(bulk);
    logger.debug("finished adding bulk : " + bulk.size() + " audio...");

    long now = System.currentTimeMillis();

    if (missing > 0) {
      logger.warn("had " + missing + "/" + audioAttributes.size() + " audio att due to missing ex fk : (" + missingExIDs.size() +
          ") : " +
          "" + missingExIDs);
    }

    logger.info("took " + (now - then) +
        " , postgres audio " + slickAudioDAO.getAudioAttributesByProject(projid).size());
  }

  /**
   * TODO : set the exid int field
   * TODO : set the exid int field
   * TODO : set the exid int field
   *
   * @param db
   * @param slickUserDAO
   * @param oldToNewUser
   * @param exToID
   */
  private void copyAnno(DatabaseImpl db, SlickUserDAOImpl slickUserDAO, Map<Integer, Integer> oldToNewUser,
                        Map<String, Integer> exToID) {
    SlickAnnotationDAO annotationDAO = (SlickAnnotationDAO) db.getAnnotationDAO();
    List<SlickAnnotation> bulk = new ArrayList<>();
    int missing = 0;
    Set<Long> missingUsers = new HashSet<>();
    Collection<UserAnnotation> all = new AnnotationDAO(db, slickUserDAO).getAll();
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
   * @see #copyToPostgres
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
                                        /*,
                                        int projectid*/
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
      int oldID = list.getCreator().getId();
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
   * TODO :  How to make sure we don't add duplicates?
   *
   * @param db
   * @param oldToNewUser
   * @param projectid
   * @see #copyUserAndPredefExercisesAndLists
   */
  private Map<String, Integer> copyUserAndPredefExercises(DatabaseImpl db,
                                                          Map<Integer, Integer> oldToNewUser,
                                                          int projectid,
                                                          Map<Integer, String> idToFL) {
    SlickUserExerciseDAO slickUEDAO = (SlickUserExerciseDAO) db.getUserExerciseDAO();
    Map<String, Integer> exToInt;
    {
      Collection<CommonExercise> exercises = db.getExercises(DatabaseImpl.IMPORT_PROJECT_ID);
      logger.info("copyUserAndPredefExercises found " + exercises.size() + " old exercises.");

      int importUser = ((SlickUserDAOImpl) db.getUserDAO()).getImportUser();
      addPredefExercises(projectid, slickUEDAO, importUser, exercises);
      exToInt = slickUEDAO.getOldToNew(projectid);

      idToFL.putAll(slickUEDAO.getIDToFL(projectid));

      logger.info("predef old->new for " + projectid + " : " + exercises.size() + " exercises");
      addContextExercises(projectid, slickUEDAO, exToInt, importUser, exercises);
    }

    addUserExercises(db, oldToNewUser, projectid, slickUEDAO);
    exToInt = slickUEDAO.getOldToNew(projectid);

    logger.info("finished copying exercises - found " + exToInt.size());
    return exToInt;
  }

  private void addUserExercises(DatabaseImpl db, Map<Integer, Integer> oldToNewUser, int projectid,
                                SlickUserExerciseDAO slickUEDAO) {
    List<SlickExercise> bulk = new ArrayList<>();
    try {
      int c = 0;
      UserExerciseDAO ueDAO = new UserExerciseDAO(db);
      ueDAO.setExerciseDAO(db.getExerciseDAO(projectid));
      Collection<Exercise> allUserExercises = ueDAO.getAllUserExercises();
      logger.info("copying  " + allUserExercises.size() + " user exercises");

      for (Exercise userExercise : allUserExercises) {
        Integer userID = oldToNewUser.get(userExercise.getCreator());
        if (userID == null) {
          if (c++ < 50) logger.error("user exercise : no user " + userExercise.getCreator());
        } else {
          userExercise.setCreator(userID);
          bulk.add(slickUEDAO.toSlick(userExercise, projectid));
        }
      }
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
    slickUEDAO.addBulk(bulk);
  }

  /**
   * @param projectid
   * @param slickUEDAO
   * @param exToInt
   * @param importUser
   * @param exercises
   * @see #copyUserAndPredefExercises
   */
  private void addContextExercises(int projectid, SlickUserExerciseDAO slickUEDAO, Map<String, Integer> exToInt,
                                   int importUser, Collection<CommonExercise> exercises) {
    int n = 0;
    int ct = 0;
    List<SlickRelatedExercise> pairs = new ArrayList<>();

    for (CommonExercise ex : exercises) {
      int id = exToInt.get(ex.getOldID());
      for (CommonExercise context : ex.getDirectlyRelated()) {
        context.getMutable().setOldID("c" + id);
        int contextid = slickUEDAO.insert(slickUEDAO.toSlick(context, false, projectid, true, importUser, true));
        pairs.add(new SlickRelatedExercise(-1, id, contextid, projectid));
        ct++;
        if (ct % 400 == 0) logger.debug("addContextExercises inserted " + ct + " context exercises");
      }
      n++;
    }

    slickUEDAO.addBulkRelated(pairs);

    logger.info("imported " + n + " predef exercises and " + ct + " context exercises");
  }

  private void addPredefExercises(int projectid,
                                  SlickUserExerciseDAO slickUEDAO,
                                  int importUser,
                                  Collection<CommonExercise> exercises) {
    List<SlickExercise> bulk = new ArrayList<>();
    logger.info("addPredefExercises copying   " + exercises.size() + " exercises");
    for (CommonExercise ex : exercises) {
      bulk.add(slickUEDAO.toSlick(ex, false, projectid, true, importUser, false));
    }
    logger.info("addPredefExercises add   bulk  " + bulk.size() + " exercises");
    slickUEDAO.addBulk(bulk);
    logger.info("addPredefExercises added bulk  " + bulk.size() + " exercises");

  }

  /**
   * @param db
   * @param slickResultDAO
   * @param oldToNewUser
   * @param projid
   * @param exToID
   * @param resultDAO
   * @see #copyOneConfig(DatabaseImpl, String, String)
   */
  private void copyResult(DatabaseImpl db,
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
   * @see #copyToPostgres(DatabaseImpl)
   */
  private void copyRefResult(DatabaseImpl db, Map<Integer, Integer> oldToNewUser, Map<String, Integer> exToID) {
    SlickRefResultDAO dao = (SlickRefResultDAO) db.getRefResultDAO();
    //if (dao.isEmpty()) {
    RefResultDAO originalDAO = new RefResultDAO(db, false);
    List<SlickRefResult> bulk = new ArrayList<>();
    Collection<Result> all = originalDAO.getResults();
    logger.info("copyRefResult found " + all.size());
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
          bulk.add(dao.toSlick(result));
        } else missing++;
      }
    }
    dao.addBulk(bulk);
    if (missing > 0) logger.warn("copyRefResult missing " + missing + " due to missing ex id fk");

    logger.info("copyRefResult added " + dao.getNumResults());
    // } else {
    //   logger.info("copyRefResult already has " + dao.getNumResults());

    // }
  }

  public static void main(String[] arg) {
    if (arg.length < 2) {
      logger.error("expecting either copy or drop followed by config, e.g. copy spanish");
      return;
    }
    String action = arg[0];
    String config = arg[1];
    boolean inTest = arg.length > 2;
    CopyToPostgres copyToPostgres = new CopyToPostgres();

    if (action.equals("drop")) {
      logger.info("drop " + config);
      copyToPostgres.testDrop(config, inTest);
    } else if (action.equals("copy")) {
      logger.info("copying " + config);
      copyToPostgres.copyOneConfig(config, inTest);
    }
  }
}
