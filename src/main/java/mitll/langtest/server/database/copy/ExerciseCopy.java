package mitll.langtest.server.database.copy;

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.user.DominoUserDAOImpl;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.server.database.userexercise.UserExerciseDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import mitll.npdata.dao.SlickExercise;
import mitll.npdata.dao.SlickRelatedExercise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 2/22/17.
 */
class ExerciseCopy {
  private static final Logger logger = LogManager.getLogger(ExerciseCopy.class);

  /**
   * TODO :  How to make sure we don't add duplicates?
   *
   * @param db
   * @param oldToNewUser
   * @param projectid
   * @see CopyToPostgres#copyUserAndPredefExercisesAndLists
   */
  Map<String, Integer> copyUserAndPredefExercises(DatabaseImpl db,
                                                  Map<Integer, Integer> oldToNewUser,
                                                  int projectid,
                                                  Map<Integer, String> idToFL) {
    SlickUserExerciseDAO slickUEDAO = (SlickUserExerciseDAO) db.getUserExerciseDAO();
    Map<String, Integer> exToInt;
    {
      Collection<CommonExercise> exercises = db.getExercises(DatabaseImpl.IMPORT_PROJECT_ID);
      logger.info("copyUserAndPredefExercises found " + exercises.size() + " old exercises.");

      // TODO : why not add it to interface?
      int importUser = ((DominoUserDAOImpl) db.getUserDAO()).getImportUser();
      addPredefExercises(projectid, slickUEDAO, importUser, exercises);
      exToInt = slickUEDAO.getOldToNew(projectid);

      idToFL.putAll(slickUEDAO.getIDToFL(projectid));

      logger.info("copyUserAndPredefExercises old->new for project #" + projectid + " : " + exercises.size() + " exercises");
      addContextExercises(projectid, slickUEDAO, exToInt, importUser, exercises);
    }

    addUserExercises(db, oldToNewUser, projectid, slickUEDAO);
    exToInt = slickUEDAO.getOldToNew(projectid);

    logger.info("copyUserAndPredefExercises : finished copying exercises - found " + exToInt.size());
    return exToInt;
  }

  /**
   * @param projectid
   * @param slickUEDAO
   * @param exToInt
   * @param importUser
   * @param exercises
   * @see #copyUserAndPredefExercises
   */
  private void addContextExercises(int projectid,
                                   SlickUserExerciseDAO slickUEDAO,
                                   Map<String, Integer> exToInt,
                                   int importUser,
                                   Collection<CommonExercise> exercises) {
    int n = 0;
    int ct = 0;
    List<SlickRelatedExercise> pairs = new ArrayList<>();

    Timestamp now = new Timestamp(System.currentTimeMillis());
    for (CommonExercise ex : exercises) {
      String oldID = ex.getOldID();
      if (oldID == null) logger.error("huh? old id is null for " + ex);
      int id = exToInt.get(oldID);
      for (CommonExercise context : ex.getDirectlyRelated()) {
        context.getMutable().setOldID("c" + id);
        int contextid = slickUEDAO.insert(slickUEDAO.toSlick(context, false, projectid, importUser, true));
        pairs.add(new SlickRelatedExercise(-1, id, contextid, projectid, now));
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
      bulk.add(slickUEDAO.toSlick(ex,
          false,
          projectid,
          //  true,
          importUser,
          false));
    }
    logger.info("addPredefExercises add   bulk  " + bulk.size() + " exercises");
    slickUEDAO.addBulk(bulk);
    logger.info("addPredefExercises added bulk  " + bulk.size() + " exercises");

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
}