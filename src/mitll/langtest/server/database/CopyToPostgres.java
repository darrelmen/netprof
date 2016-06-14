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

import mitll.langtest.server.database.annotation.AnnotationDAO;
import mitll.langtest.server.database.annotation.SlickAnnotationDAO;
import mitll.langtest.server.database.audio.SlickAudioDAO;
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
import mitll.langtest.server.database.word.SlickWordDAO;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.npdata.dao.SlickAudio;
import mitll.npdata.dao.SlickPhone;
import mitll.npdata.dao.SlickResult;
import mitll.npdata.dao.SlickUserExercise;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CopyToPostgres {
  private static final Logger logger = Logger.getLogger(CopyToPostgres.class);

  public void copyToPostgres(DatabaseImpl db) {
    // first add the user table
    SlickUserDAOImpl slickUserDAO = (SlickUserDAOImpl) db.getUserDAO();
    SlickAudioDAO slickAudioDAO = (SlickAudioDAO) db.getAudioDAO();
    SlickEventImpl slickEventDAO = (SlickEventImpl) db.getEventDAO();
    SlickResultDAO slickResultDAO = (SlickResultDAO) db.getResultDAO();
    SlickUserExerciseDAO slickUEDAO = (SlickUserExerciseDAO) db.getUserExerciseDAO();
    SlickPhoneDAO slickPhoneAO = (SlickPhoneDAO) db.getPhoneDAO();
    SlickWordDAO slickWordDAO = (SlickWordDAO) db.getWordDAO();
    SlickAnnotationDAO annotationDAO = (SlickAnnotationDAO) db.getAnnotationDAO();

//    try {
//      logger.info("drop tables");
//      dropTables();
//    } catch (Exception e) {
//      logger.warn("drop got " + e);
//    }
//
//    createTables();

    if (false) {
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

    Map<Integer, Integer> oldToNewUser = slickUserDAO.getOldToNew();
    logger.info("oldToNewUser " + oldToNewUser.size());
    Map<Integer, Integer> oldToNewResult = slickResultDAO.getOldToNew();

    // add the audio table
    if (false) {
      int num = slickAudioDAO.getNumRows();
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

    // add event table
    String language = db.getLanguage();
    if (false) {
      slickEventDAO.copyTableOnlyOnce(new EventDAO(db, db.getUserDAO().getDefectDetector()), language, oldToNewUser);
    }

    if (false) {
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

    // copy user exercises
    if (true) {
      UserExerciseDAO ueDAO = new UserExerciseDAO(db);
      List<SlickUserExercise> bulk = new ArrayList<>();

      try {
        for (UserExercise result : ueDAO.getUserExercisesList()) {
          Integer userID = oldToNewUser.get(result.getCreator());
          if (userID == null) {
            logger.error("no user " + result.getCreator());
          } else {
            result.setCreator(userID);
            bulk.add(slickUEDAO.toSlick(result,language));
          }
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
      slickUEDAO.addBulk(bulk);
    }

    // phone DAO
    if (true) {
      PhoneDAO ueDAO = new PhoneDAO(db);
      List<SlickPhone> bulk = new ArrayList<>();

      for (Phone result : ueDAO.getAll()) {
        Integer rid = oldToNewResult.get((int) result.getRid());
        if (rid == null) {
          logger.error("no user " + result.getRid());
        } else {
          result.setRID(rid);
          bulk.add(slickPhoneAO.toSlick(result, language));
        }
      }

      slickPhoneAO.addBulk(bulk);
    }

    // word DAO
    // anno DAO
    if (true) {
      AnnotationDAO dao = new AnnotationDAO(db,slickUserDAO);
    }
  }
}
