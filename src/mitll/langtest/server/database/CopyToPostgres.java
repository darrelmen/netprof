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
import mitll.langtest.server.database.result.Result;
import mitll.langtest.server.database.result.ResultDAO;
import mitll.langtest.server.database.result.SlickResultDAO;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CopyToPostgres {
  private static final Logger logger = Logger.getLogger(CopyToPostgres.class);
  public static final boolean COPY_ANNO = true;
  public static final boolean USERS = true;
  public static final boolean AUDIO = true;
  public static final boolean EVENT = true;
  public static final boolean RESULT = true;

  public static final boolean USER_EXERCISE = true;
  public static final boolean COPY_PHONE = true;
  public static final boolean WORD = true;

  public void copyToPostgres(String config) {
    getDatabaseLight(config).copyToPostgres();
  }

  public void testDrop(String config) {
    getConnection(config).dropAll();
  }

  protected static DBConnection getConnection(String config) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "quizlet.properties");
    String parent = file.getParent();
    String name = file.getName();

    parent = file.getParentFile().getAbsolutePath();

    logger.info("path is " + parent);
    ServerProperties serverProps = new ServerProperties(parent, name);
    return new DBConnection(serverProps.getDatabaseType(),
        serverProps.getDatabaseHost(),
        serverProps.getDatabasePort(),
        serverProps.getDatabaseName(),
        serverProps.getDatabaseUser(),
        serverProps.getDatabasePassword());
  }

  protected static DatabaseImpl<CommonExercise> getDatabaseLight(String config) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "quizlet.properties");
    String parent = file.getParent();
    String name = file.getName();

    parent = file.getParentFile().getAbsolutePath();

    logger.info("path is " + parent);
    ServerProperties serverProps = new ServerProperties(parent, name);
    DatabaseImpl<CommonExercise> database = getDatabaseVeryLight(config);
    database.setInstallPath("war", parent + File.separator + database.getServerProps().getLessonPlan(),
        serverProps.getMediaDir());
    return database;
  }

  protected static DatabaseImpl<CommonExercise> getDatabaseVeryLight(String config) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "quizlet.properties");
    String parent = file.getParent();
    String name = file.getName();

    parent = file.getParentFile().getAbsolutePath();

    logger.info("path is " + parent);
    ServerProperties serverProps = new ServerProperties(parent, name);
    DatabaseImpl<CommonExercise> database = new DatabaseImpl<>(parent, name, serverProps.getH2Database(), serverProps, new PathHelper("war"), false, null);
    return database;
  }

  public void copyToPostgres(DatabaseImpl db) {
    // first add the user table
    SlickUserDAOImpl slickUserDAO = (SlickUserDAOImpl) db.getUserDAO();
    SlickResultDAO slickResultDAO = (SlickResultDAO) db.getResultDAO();

    copyUsers(db, slickUserDAO);

    Map<Integer, Integer> oldToNewUser = slickUserDAO.getOldToNew();
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
  }

  void copyUsers(DatabaseImpl db, SlickUserDAOImpl slickUserDAO) {
    if (USERS) {
      if (slickUserDAO.isEmpty()) {
        UserDAO userDAO = new UserDAO(db);
        List<User> users = userDAO.getUsers();
        logger.info("h2 users  " + users.size());
        for (User user : users) {
          if (user.getId() != userDAO.getDefectDetector()) {
            slickUserDAO.add(slickUserDAO.toSlick(user));
          }
        }
        logger.info("after, postgres users " + slickUserDAO.getUsers().size());
      }
    }
  }

  void copyAudio(DatabaseImpl db, Map<Integer, Integer> oldToNewUser) {
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

  void copyAnno(DatabaseImpl db, SlickUserDAOImpl slickUserDAO, Map<Integer, Integer> oldToNewUser, String language) {
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

  void copyPhone(DatabaseImpl db, Map<Integer, Integer> oldToNewResult, String language, Map<Integer, Integer> oldToNewWordID) {
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

        logger.info("adding " + bulk.size());
        slickPhoneAO.addBulk(bulk);
        logger.info("added " + slickPhoneAO.getNumRows());
      } else {

      }
    }
  }

  void copyWord(DatabaseImpl db, Map<Integer, Integer> oldToNewResult, String language, SlickWordDAO slickWordDAO) {
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
  }

  void copyUserExerciseListJoin(DatabaseImpl db, Map<Integer, Integer> oldToNewUserList) {
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
          }
          else slickUserListExerciseJoinDAO.addPair(integer, join.exerciseID);
        }
      }
    }
  }

  void copyUserExerciseList(DatabaseImpl db, Map<Integer, Integer> oldToNewUser, SlickUserListDAO slickUserListDAO) {
    if (true) {
      UserListManager userListManager = db.getUserListManager();

//      IUserListDAO userListDAO = userListManager.getUserListDAO();

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

  void copyUserExercises(DatabaseImpl db, Map<Integer, Integer> oldToNewUser, String language) {
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

  void copyResult(DatabaseImpl db, SlickResultDAO slickResultDAO, Map<Integer, Integer> oldToNewUser, String language) {
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

  public static void main(String[] arg) {
    if (arg.length < 2) {
      logger.error("expecting either copy or drop followed by config, e.g. copy spanish");
      return;
    }
    String action = arg[0];
    String config = arg[1];
    if (action.equals("drop")) {
      logger.info("drop " + config);
      new CopyToPostgres().testDrop(config);
    }
    else if (action.equals("copy")) {
      logger.info("copying " + config);
      new CopyToPostgres().copyToPostgres(config);
    }
  }
}
