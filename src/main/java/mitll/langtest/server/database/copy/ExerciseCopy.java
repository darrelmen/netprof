package mitll.langtest.server.database.copy;

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.user.DominoUserDAOImpl;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.server.database.userexercise.UserExerciseDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.npdata.dao.SlickExercise;
import mitll.npdata.dao.SlickExerciseAttributeJoin;
import mitll.npdata.dao.SlickRelatedExercise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

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
      Map<String, List<Integer>> exToJoins = addPredefExercises(projectid, slickUEDAO, importUser, exercises);
      exToInt = slickUEDAO.getOldToNew(projectid);

      Timestamp nowT = new Timestamp(System.currentTimeMillis());
      List<SlickExerciseAttributeJoin> joins = new ArrayList<>();
      for (Map.Entry<String, List<Integer>> pair : exToJoins.entrySet()) {
        Integer dbID = exToInt.get(pair.getKey());
        for (Integer attrid : pair.getValue()) {
          joins.add(new SlickExerciseAttributeJoin(-1, importUser, nowT, dbID, attrid));
        }
      }

      logger.info("adding " + joins.size() + " attribute joins");
      slickUEDAO.addBulkAttributes(joins);
      //joins.add(new SlickExerciseAttributeJoin(-1, importUser, nowT, ))


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

  /**
   * @param projectid
   * @param slickUEDAO
   * @param importUser
   * @param exercises
   * @see #copyUserAndPredefExercises(DatabaseImpl, Map, int, Map)
   */
  private Map<String, List<Integer>> addPredefExercises(int projectid,
                                                        SlickUserExerciseDAO slickUEDAO,

                                                        int importUser,
                                                        Collection<CommonExercise> exercises) {
    List<SlickExercise> bulk = new ArrayList<>();
    logger.info("addPredefExercises copying   " + exercises.size() + " exercises");
    Set<ExerciseAttribute> known = new HashSet<>();
    long now = System.currentTimeMillis();

    Map<ExerciseAttribute, Integer> attrToID = new HashMap<>();

//    List<SlickExerciseAttributeJoin> joins = new ArrayList<>();
    Map<String, List<Integer>> exToJoins = new HashMap<>();

    for (CommonExercise ex : exercises) {
      bulk.add(slickUEDAO.toSlick(ex,
          false,
          projectid,
          //  true,
          importUser,
          false));
      List<ExerciseAttribute> attributes = ex.getAttributes();

      List<Integer> joins = null;
      if (!attributes.isEmpty()) {
        exToJoins.put(ex.getOldID(), joins = new ArrayList<Integer>());
        for (ExerciseAttribute attribute : attributes) {
          boolean contains = known.contains(attribute);
          int id;
          if (!contains) {
            known.add(attribute);
            id = slickUEDAO.addAttribute(projectid, now, importUser, attribute);
            attrToID.put(attribute, id);
            logger.info("addPredef " + attribute + " = " + id);
          } else {
            id = attrToID.get(attribute);
          }
          joins.add(id);
          //joins.add(new SlickExerciseAttributeJoin(-1, importUser, nowT, ))
        }
      }
    }
    logger.info("addPredefExercises add   bulk  " + bulk.size() + " exercises");
    slickUEDAO.addBulk(bulk);
    logger.info("addPredefExercises added bulk  " + bulk.size() + " exercises");
    logger.info("addPredefExercises will add    " + exToJoins.size() + " attributes");
    return exToJoins;
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