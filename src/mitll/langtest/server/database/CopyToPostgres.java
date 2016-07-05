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

package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.annotation.AnnotationDAO;
import mitll.langtest.server.database.annotation.SlickAnnotationDAO;
import mitll.langtest.server.database.annotation.UserAnnotation;
import mitll.langtest.server.database.audio.SlickAudioDAO;
import mitll.langtest.server.database.custom.UserListManager;
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
import mitll.langtest.server.database.user.SlickUserDAOImpl;
import mitll.langtest.server.database.user.UserDAO;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.server.database.userexercise.UserExerciseDAO;
import mitll.langtest.server.database.userlist.*;
import mitll.langtest.server.database.word.SlickWordDAO;
import mitll.langtest.server.database.word.Word;
import mitll.langtest.server.database.word.WordDAO;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.npdata.dao.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.SQLException;
import java.util.*;

public class CopyToPostgres {
  private static final Logger logger = Logger.getLogger(CopyToPostgres.class);
  private static final boolean COPY_ANNO = true;
  private static final boolean USERS = true;
  private static final boolean AUDIO = true;
  private static final boolean EVENT = true;
  private static final boolean RESULT = true;

  private static final boolean USER_EXERCISE = true;
  private static final boolean COPY_PHONE = true;
  private static final boolean WORD = true;

  private void copyToPostgres(String config, boolean inTest) {
    getDatabaseLight(config, inTest).copyToPostgres();
  }

  private void testDrop(String config, boolean inTest) {
    getConnection(config, inTest).dropAll();
  }

  private static DBConnection getConnection(String config, boolean inTest) {
    File file = getConfigFile(config, inTest);
    String parent = file.getParentFile().getAbsolutePath();
    ServerProperties serverProps = new ServerProperties(parent, file.getName());
    return new DBConnection(serverProps.getDatabaseType(),
        serverProps.getDatabaseHost(),
        serverProps.getDatabasePort(),
        serverProps.getDatabaseName(),
        serverProps.getDatabaseUser(),
        serverProps.getDatabasePassword());
  }

  private static DatabaseImpl<CommonExercise> getDatabaseLight(String config, boolean inTest) {
    File file = getConfigFile(config, inTest);
    String parent = file.getParentFile().getAbsolutePath();
    ServerProperties serverProps = new ServerProperties(parent, file.getName());
    DatabaseImpl<CommonExercise> database = getDatabaseVeryLight(config, inTest);
    String installPath = getInstallPath(inTest);
    database.setInstallPath(installPath, parent + File.separator + database.getServerProps().getLessonPlan(),
        serverProps.getMediaDir());
    return database;
  }

  private static File getConfigFile(String config, boolean inTest) {
    return new File((inTest ? "war" + File.separator : "") + "config" + File.separator + config + File.separator + "quizlet.properties");
  }

  private static String getInstallPath(boolean inTest) {
    return inTest ? "war" : ".";
  }

  private static DatabaseImpl<CommonExercise> getDatabaseVeryLight(String config, boolean inTest) {
    File file = getConfigFile(config, inTest);
    String parent = file.getParent();
    String name = file.getName();

    parent = file.getParentFile().getAbsolutePath();

    logger.info("path is " + parent);
    ServerProperties serverProps = new ServerProperties(parent, name);
    return new DatabaseImpl<>(parent, name, serverProps.getH2Database(), serverProps,
        new PathHelper(getInstallPath(inTest)), false, null, true);
  }

  void copyToPostgres(DatabaseImpl db) {
    IProjectDAO projectDAO = db.getProjectDAO();
    int byName = projectDAO.getByName(db.getLanguage());

    // first add the user table
    SlickUserDAOImpl slickUserDAO = (SlickUserDAOImpl) db.getUserDAO();

    if (byName == -1) {
      createProject(db, projectDAO, slickUserDAO);
    }

    SlickResultDAO slickResultDAO = (SlickResultDAO) db.getResultDAO();

    Map<Integer, Integer> oldToNewUser = copyUsers(db, slickUserDAO);

    //Map<Integer, Integer> oldToNewUser = slickUserDAO.getOldToNew();
    copyResult(db, slickResultDAO, oldToNewUser, db.getLanguage());

    logger.info("oldToNewUser " + oldToNewUser.size());
    Map<Integer, Integer> oldToNewResult = slickResultDAO.getOldToNew();
    if (oldToNewResult.isEmpty()) {
      logger.error("\n\n\nold to new result is EMPTY!");
    }
    logger.info("oldToNewResult " + oldToNewResult.size());

    // add the audio table
    copyAudio(db, oldToNewUser);

    // add event table
    String language = db.getLanguage();
    if (EVENT) {
      SlickEventImpl slickEventDAO = (SlickEventImpl) db.getEventDAO();
      slickEventDAO.copyTableOnlyOnce(new EventDAO(db, db.getUserDAO().getDefectDetector()), language, oldToNewUser);
    }

    // copy user exercises
    copyUserExercises(db, oldToNewUser, language);

    SlickUserListDAO slickUserListDAO = (SlickUserListDAO) db.getUserListManager().getUserListDAO();
    copyUserExerciseList(db, oldToNewUser, slickUserListDAO);

    Map<Integer, Integer> oldToNewUserList = slickUserListDAO.getOldToNew();
    copyUserExerciseListVisitor(db, oldToNewUser, oldToNewUserList, (SlickUserListExerciseVisitorDAO) db.getUserListManager().getVisitorDAO());
    copyUserExerciseListJoin(db, oldToNewUserList);

    SlickWordDAO slickWordDAO = (SlickWordDAO) db.getWordDAO();
    // word DAO

    copyWord(db, oldToNewResult, language, slickWordDAO);

    Map<Integer, Integer> oldToNewWordID = slickWordDAO.getOldToNew();
    logger.info("old to new word id  " + oldToNewWordID.size());

    // phone DAO
    copyPhone(db, oldToNewResult, language, oldToNewWordID);

    // anno DAO
    copyAnno(db, slickUserDAO, oldToNewUser, language);

    copyReviewed(db, oldToNewUser, true);
    copyReviewed(db, oldToNewUser, false);
    copyRefResult(db, oldToNewUser);
  }

  private void createProject(DatabaseImpl db, IProjectDAO projectDAO, SlickUserDAOImpl slickUserDAO) {
    int byName;
    byName = projectDAO.add(slickUserDAO.getBeforeLoginUser(), db.getLanguage(), db.getLanguage());
    Properties props = db.getServerProps().getProps();

    for (String prop : ServerProperties.CORE_PROPERTIES) {
      String o = props.getProperty(prop);
      projectDAO.add(byName, prop, o);
    }
  }

  /**
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
   * @param slickUserDAO
   * @see
   */
  private Map<Integer, Integer> copyUsers(DatabaseImpl db, SlickUserDAOImpl slickUserDAO) {
    Map<Integer, Integer> oldToNew = new HashMap<>();
//  if (USERS) {
    //    if (slickUserDAO.isEmpty()) {
    UserDAO userDAO = new UserDAO(db);
    List<User> importUsers = userDAO.getUsers();
    logger.info("h2 importUsers  " + importUsers.size());
    int numAdded = 0;
    int unresolved = 0;
    int collisions = 0;
    for (User toImport : importUsers) {
      if (toImport.getId() != userDAO.getDefectDetector()) {
        //   int idForUserID = slickUserDAO.getIdForUserID(toImport.getUserID());
        String toImportUserID = toImport.getUserID();
        User userByID = slickUserDAO.getUserByID(toImportUserID);

        if (userByID != null) { // exists - collision?
          String passwordHash = userByID.getPasswordHash();
          String importPass = toImport.getPasswordHash();

          if (passwordHash.equals(importPass) || importPass == null) {  // same person, multiple accounts on different npf instances
            oldToNew.put(toImport.getId(), userByID.getId());
          } else { // collision.
            logger.warn("collision : user '" + toImportUserID +
                "' import '" + importPass + "' vs existing '" + passwordHash +
                "'");

            String candidate = db.getLanguage() + "_" + toImportUserID;
            User candidateUser = slickUserDAO.getUserByID(candidate);
            if (candidateUser != null) { // this should be unique
              logger.warn("----->");
              logger.warn("Collision on '" + candidate + "' ?");
              logger.warn("----->");
              unresolved++;
            } else {
              // OK, we'll have to find this person when they log in and warn them their data has been moved to a new name
              // or deal silently
              toImport.setUserID(candidate);
              addUser(slickUserDAO, oldToNew, toImport);
              collisions++;
            }
          }
        } else { // new user across all instances imported to this point
          addUser(slickUserDAO, oldToNew, toImport);
          numAdded++;
        }
      }
    }
    logger.info("after, postgres importUsers num = " + slickUserDAO.getUsers().size() +
        " added " + numAdded +
        " collisions " + collisions +
        " unresolved " + unresolved);
    //  }
    // }
    return oldToNew;
  }

  private void addUser(SlickUserDAOImpl slickUserDAO, Map<Integer, Integer> oldToNew, User toImport) {
    oldToNew.put(toImport.getId(), slickUserDAO.add(slickUserDAO.toSlick(toImport)));
  }

  private void copyAudio(DatabaseImpl db, Map<Integer, Integer> oldToNewUser) {
    if (AUDIO) {
      SlickAudioDAO slickAudioDAO = (SlickAudioDAO) db.getAudioDAO();
      int num = slickAudioDAO.getNumRows();
      if (num == 0) {
        logger.info("after drop slickAudioDAO " + num);

        List<SlickAudio> bulk = new ArrayList<>();
        Collection<AudioAttribute> audioAttributes = db.getH2AudioDAO().getAudioAttributes();
        logger.info("h2 audio  " + audioAttributes.size());

        for (AudioAttribute att : audioAttributes) {
          SlickAudio slickAudio = slickAudioDAO.getSlickAudio(att, oldToNewUser);
          bulk.add(slickAudio);
        }
        long then = System.currentTimeMillis();
        slickAudioDAO.addBulk(bulk);
        long now = System.currentTimeMillis();

        logger.info("took " + (now - then) +
            " , postgres audio " + slickAudioDAO.getAudioAttributes().size());
      }
    }
  }

  private void copyAnno(DatabaseImpl db, SlickUserDAOImpl slickUserDAO, Map<Integer, Integer> oldToNewUser, String language) {
    if (COPY_ANNO) {
      SlickAnnotationDAO annotationDAO = (SlickAnnotationDAO) db.getAnnotationDAO();
      if (annotationDAO.isEmpty()) {
        AnnotationDAO dao = new AnnotationDAO(db, slickUserDAO);
        List<SlickAnnotation> bulk = new ArrayList<>();
        for (UserAnnotation annotation : dao.getAll()) {
          Integer userID = oldToNewUser.get((int) annotation.getCreatorID());
          if (userID == null) {
            logger.error("no user " + annotation.getCreatorID());
          } else {
            annotation.setCreatorID(userID);
            bulk.add(annotationDAO.toSlick(annotation, language));
          }
        }
        annotationDAO.addBulk(bulk);
      }
    }
  }

  private void copyPhone(DatabaseImpl db, Map<Integer, Integer> oldToNewResult, String language, Map<Integer, Integer> oldToNewWordID) {
    if (COPY_PHONE) {
      SlickPhoneDAO slickPhoneAO = (SlickPhoneDAO) db.getPhoneDAO();
      if (slickPhoneAO.isEmpty()) {
        PhoneDAO ueDAO = new PhoneDAO(db);
        List<SlickPhone> bulk = new ArrayList<>();
        int c = 0;
        int d = 0;
        int added = 0;
        for (Phone phone : ueDAO.getAll()) {
          Integer rid = oldToNewResult.get((int) phone.getRid());
          if (rid == null) {
            if (c++ < 50) logger.error("phone : no rid " + phone.getRid());
          } else {
            Integer wid = oldToNewWordID.get((int) phone.getWid());

            if (wid == null) {
              if (d++ < 50) logger.error("phone : no word id " + phone.getWid());
            } else {
              phone.setRID(rid);
              phone.setWID(wid);
              bulk.add(slickPhoneAO.toSlick(phone, language));
            }
          }
        }
        if (c > 0) logger.warn("missing result id fks " + c);
        if (d > 0) logger.warn("missing word   id fks " + d);

        logger.info("copyPhone adding " + bulk.size());
        slickPhoneAO.addBulk(bulk);
        logger.info("copyPhone added " + slickPhoneAO.getNumRows());
      } else {
        logger.info("copyPhone not empty, still num rows = " + slickPhoneAO.getNumRows());

      }
    }
  }

  /**
   * TODO empty table checks are bogus going forward
   *
   * @param db
   * @param oldToNewResult
   * @param language
   * @param slickWordDAO
   * @see #copyToPostgres(DatabaseImpl)
   */
  private void copyWord(DatabaseImpl db, Map<Integer, Integer> oldToNewResult, String language, SlickWordDAO slickWordDAO) {
    if (WORD) {
      if (slickWordDAO.isEmpty()) {
        int c = 0;
        WordDAO ueDAO = new WordDAO(db);
        List<SlickWord> bulk = new ArrayList<>();

        for (Word word : ueDAO.getAll()) {
          Integer rid = oldToNewResult.get((int) word.getRid());
          if (rid == null) {
            if (c++ < 50) logger.error("word has no rid " + word.getRid());
          } else {
            word.setRid(rid);
            bulk.add(slickWordDAO.toSlick(word, language));
          }
        }
        if (c > 0) logger.warn("word : missing " + c + " result id fk references");

        slickWordDAO.addBulk(bulk);
      }
    }
    logger.info("copy word - complete");
  }

  private void copyUserExerciseListJoin(DatabaseImpl db, Map<Integer, Integer> oldToNewUserList) {
    if (true) {
      UserListManager userListManager = db.getUserListManager();
      IUserListExerciseJoinDAO dao = userListManager.getUserListExerciseJoinDAO();
      SlickUserListExerciseJoinDAO slickUserListExerciseJoinDAO = (SlickUserListExerciseJoinDAO) dao;
      if (slickUserListExerciseJoinDAO.isEmpty()) {
        Collection<UserListExerciseJoinDAO.Join> all = new UserListExerciseJoinDAO(db).getAll();
        for (UserListExerciseJoinDAO.Join join : all) {
          int oldID = join.userlistid;
          Integer integer = oldToNewUserList.get(oldID);
          if (integer == null) {
            logger.error("UserListManager join can't find user list " + oldID + " in " + oldToNewUserList.size());
          } else slickUserListExerciseJoinDAO.addPair(integer, join.exerciseID);
        }
      }
    }
  }

  private void copyUserExerciseList(DatabaseImpl db, Map<Integer, Integer> oldToNewUser, SlickUserListDAO slickUserListDAO) {
    if (true) {
      if (slickUserListDAO.isEmpty()) {
        UserDAO userDAO = new UserDAO(db);
        UserListDAO uelDAO = new UserListDAO(db, userDAO);
        for (UserList<CommonShell> list : uelDAO.getAll()) {
          int oldID = list.getCreator().getId();
          Integer integer = oldToNewUser.get(oldID);
          if (integer == null) logger.error("UserListManager can't find user " + oldID);
          else slickUserListDAO.addWithUser(list, integer);
        }
      }
    }
  }

  private void copyUserExerciseListVisitor(DatabaseImpl db,
                                           Map<Integer, Integer> oldToNewUser,
                                           Map<Integer, Integer> oldToNewUserList,
                                           SlickUserListExerciseVisitorDAO visitorDAO) {
    if (true) {
      if (visitorDAO.isEmpty()) {
        UserExerciseListVisitorDAO uelDAO = new UserExerciseListVisitorDAO(db);
        for (UserExerciseListVisitorDAO.Pair pair : uelDAO.getAll()) {
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
      }
    }
  }

  private void copyUserExercises(DatabaseImpl db, Map<Integer, Integer> oldToNewUser, String language) {
    if (USER_EXERCISE) {
      UserExerciseDAO ueDAO = new UserExerciseDAO(db);
      ueDAO.setExerciseDAO(db.getExerciseDAO());
      SlickUserExerciseDAO slickUEDAO = (SlickUserExerciseDAO) db.getUserExerciseDAO();

      if (slickUEDAO.isEmpty()) {
        List<SlickUserExercise> bulk = new ArrayList<>();

        try {
          int c = 0;
          for (UserExercise result : ueDAO.getUserExercisesList()) {
            Integer userID = oldToNewUser.get(result.getCreator());
            if (userID == null) {
              if (c++ < 50) logger.error("user exercise : no user " + result.getCreator());
            } else {
              result.setCreator(userID);
              bulk.add(slickUEDAO.toSlick(result, language));
            }
          }
        } catch (SQLException e) {
          e.printStackTrace();
        }
        slickUEDAO.addBulk(bulk);
      }
    }
  }

  private void copyResult(DatabaseImpl db, SlickResultDAO slickResultDAO, Map<Integer, Integer> oldToNewUser, String language) {
    if (RESULT) {
      if (slickResultDAO.isEmpty()) {
        ResultDAO resultDAO = new ResultDAO(db);
        List<SlickResult> bulk = new ArrayList<>();

        for (Result result : resultDAO.getResults()) {
          Integer userID = oldToNewUser.get(result.getUserid());
          if (userID == null) {
            logger.error("no user " + result.getUserid());
          } else {
            result.setUserID(userID);
            bulk.add(slickResultDAO.toSlick(result, language));
          }
        }
        slickResultDAO.addBulk(bulk);
      }
    }
  }

  private void copyReviewed(DatabaseImpl db, Map<Integer, Integer> oldToNewUser, boolean isReviewed) {
    SlickReviewedDAO dao = (SlickReviewedDAO) (isReviewed ? db.getReviewedDAO() : db.getSecondStateDAO());
    if (dao.isEmpty()) {
      String tableName = isReviewed ? ReviewedDAO.REVIEWED : ReviewedDAO.SECOND_STATE;
      ReviewedDAO originalDAO = new ReviewedDAO(db, tableName);
      List<SlickReviewed> bulk = new ArrayList<>();
      Collection<StateCreator> all = originalDAO.getAll();
      logger.info("found " + all.size() + " for " + tableName);
      for (StateCreator stateCreator : all) {
        Integer userID = oldToNewUser.get((int) stateCreator.getCreatorID());
        if (userID == null) {
          logger.error("copyReviewed no user " + stateCreator.getCreatorID());
        } else {
          stateCreator.setCreatorID(userID);
          bulk.add(dao.toSlick(stateCreator));
        }
      }
      dao.addBulk(bulk);
    }
  }

  private void copyRefResult(DatabaseImpl db, Map<Integer, Integer> oldToNewUser) {
    SlickRefResultDAO dao = (SlickRefResultDAO) db.getRefResultDAO();
    if (dao.isEmpty()) {
      RefResultDAO originalDAO = new RefResultDAO(db, false);
      List<SlickRefResult> bulk = new ArrayList<>();
      Collection<Result> all = originalDAO.getResults();
      logger.info("copyRefResult found " + all.size());
      for (Result result : all) {
        Integer userID = oldToNewUser.get((int) result.getUserid());
        if (userID == null) {
          logger.error("copyReviewed no user " + result.getUserid());
        } else {
          result.setUserID(userID);
          bulk.add(dao.toSlick(result));
        }
      }
      dao.addBulk(bulk);
      logger.info("copyRefResult added " + dao.getNumResults());
    } else {
      logger.info("copyRefResult already has " + dao.getNumResults());

    }
  }

  public static void main(String[] arg) {
    if (arg.length < 2) {
      logger.error("expecting either copy or drop followed by config, e.g. copy spanish");
      return;
    }
    String action = arg[0];
    String config = arg[1];
    if (action.equals("drop")) {
      logger.info("drop " + config);
      new CopyToPostgres().testDrop(config, false);
    } else if (action.equals("copy")) {
      logger.info("copying " + config);
      new CopyToPostgres().copyToPostgres(config, false);
    }
  }
}
