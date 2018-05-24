package mitll.langtest.server.database.copy;

import mitll.langtest.server.database.DatabaseImpl;
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
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * Created by go22670 on 2/22/17.
 */
public class ExerciseCopy {
  private static final Logger logger = LogManager.getLogger(ExerciseCopy.class);

  private boolean DEBUG = false;

  /**
   * TODO :  How to make sure we don't add duplicates?
   *
   * @param db
   * @param oldToNewUser
   * @param projectid
   * @param typeOrder
   * @see CopyToPostgres#copyUserAndPredefExercisesAndLists
   */
  Map<String, Integer> copyUserAndPredefExercises(DatabaseImpl db,
                                                  Map<Integer, Integer> oldToNewUser,
                                                  int projectid,
                                                  Map<Integer, String> idToFL,
                                                  Collection<String> typeOrder,
                                                  Map<String, Integer> parentToChild) {

    logger.info("copyUserAndPredefExercises for " + projectid + " typeOrder is " + typeOrder);
    if (typeOrder.isEmpty()) {
      logger.error("copyUserAndPredefExercises huh? type order is empty?\n\n\n");
    }
    Map<String, List<Exercise>> idToCandidateOverride = new HashMap<>();
    List<Exercise> exercises = addUserExercises(db, oldToNewUser, DatabaseImpl.IMPORT_PROJECT_ID, typeOrder, idToCandidateOverride);

    List<CommonExercise> toImport = db.getExercises(DatabaseImpl.IMPORT_PROJECT_ID);

    logger.info("importing " + toImport.size() + " exercises");

    SlickUserExerciseDAO slickUEDAO = (SlickUserExerciseDAO) db.getUserExerciseDAO();
    parentToChild.putAll(addExercises(
        db.getUserDAO().getImportUser(),
        projectid,
        idToFL,
        slickUEDAO,
        toImport,
        typeOrder,
        idToCandidateOverride));

    Map<String, Integer> exToInt = slickUEDAO.getOldToNew(projectid).getOldToNew();
    reallyAddingUserExercises(projectid, typeOrder, slickUEDAO, exToInt, exercises);

    logger.info("copyUserAndPredefExercises : finished copying exercises - found " + exToInt.size());
    return exToInt;
  }

  private void reallyAddingUserExercises(int projectid,
                                         Collection<String> typeOrder,
                                         SlickUserExerciseDAO slickUEDAO,
                                         Map<String, Integer> exToInt,
                                         List<Exercise> exercises) {
    List<SlickExercise> bulk = new ArrayList<>();

    for (Exercise userCandidate : exercises) {
      String oldID = userCandidate.getOldID();
      Integer existingPredefID = exToInt.get(oldID);

      if (existingPredefID != null) {
        CommonExercise byExID = slickUEDAO.getByExID(existingPredefID);
        if (byExID.getEnglish().equals(userCandidate.getEnglish()) &&
            byExID.getForeignLanguage().equals(userCandidate.getForeignLanguage())) {
          logger.debug("reallyAddingUserExercises - user exercise with same old id " + oldID + " as predef " + byExID);
        } else {
          logger.warn("reallyAddingUserExercises Collision - user exercise with same old id " + oldID + " as predef " + byExID);
        }
      } else {
        bulk.add(slickUEDAO.toSlick(userCandidate, projectid, typeOrder));
      }
    }
    logger.info("reallyAddingUserExercises Adding " + bulk.size() + " user exercises");
    slickUEDAO.addBulk(bulk);
  }

  /**
   * @param importUser
   * @param projectid
   * @param idToFL
   * @param slickUEDAO
   * @param exercises
   * @param typeOrder
   * @param idToCandidateOverride
   * @see #copyUserAndPredefExercises
   */
  public Map<String, Integer> addExercises(int importUser,
                                           int projectid,
                                           Map<Integer, String> idToFL,
                                           SlickUserExerciseDAO slickUEDAO,
                                           Collection<CommonExercise> exercises,
                                           Collection<String> typeOrder,
                                           Map<String, List<Exercise>> idToCandidateOverride) {

    logger.info("copyUserAndPredefExercises for project " + projectid +
        "\n\tfound " + exercises.size() + " old exercises" +
        "\n\tand   " + idToCandidateOverride.size() + " overrides");

    // TODO : why not add it to interface?
    Map<String, Integer> exToInt = addExercisesAndAttributes(importUser, projectid, slickUEDAO, exercises, typeOrder, idToCandidateOverride);
    idToFL.putAll(slickUEDAO.getIDToFL(projectid));

    logger.info("copyUserAndPredefExercises old->new for project #" + projectid + " : " + exercises.size() + " exercises, " + exToInt.size());
    return addContextExercises(projectid, slickUEDAO, exToInt, importUser, exercises, typeOrder);
  }

  public void addContextExercises(int importUser,
                                  int projectid,
                                  SlickUserExerciseDAO slickUEDAO,
                                  Collection<CommonExercise> exercises,
                                  Collection<String> typeOrder) {
    List<SlickRelatedExercise> pairs = new ArrayList<>();

    Timestamp now = new Timestamp(System.currentTimeMillis());

    for (CommonExercise context : exercises) {
//      logger.info("addContextExercises adding context " + context);
 //     logger.info("addContextExercises context id " + context.getID() + " with parent " + context.getParentExerciseID());
      if (context.getParentExerciseID() > 0) {
        SlickRelatedExercise e = insertContextExercise(projectid, slickUEDAO, importUser, typeOrder,
            now, context.getParentExerciseID(), context);
        pairs.add(e);
      }else {
        logger.warn("addContextExercises ex " + context.getID() + " " + context.getEnglish() + " has no parent id set?");
      }
    }

    slickUEDAO.addBulkRelated(pairs);
  }

  /**
   * @param importUser
   * @param projectid
   * @param slickUEDAO
   * @param exercises
   * @param typeOrder
   * @param idToCandidateOverride
   * @return
   * @see #addExercises(int, int, Map, SlickUserExerciseDAO, Collection, Collection, Map)
   * @see #addPredefExercises(int, SlickUserExerciseDAO, int, Collection, Collection, Map)
   */
  private Map<String, Integer> addExercisesAndAttributes(int importUser,
                                                         int projectid,
                                                         SlickUserExerciseDAO slickUEDAO,
                                                         Collection<CommonExercise> exercises,
                                                         Collection<String> typeOrder,
                                                         Map<String, List<Exercise>> idToCandidateOverride) {
    Map<String, Integer> exToInt;
    Map<String, List<Integer>> exToJoins = addPredefExercises(projectid, slickUEDAO, importUser, exercises, typeOrder, idToCandidateOverride);
    SlickUserExerciseDAO.BothMaps oldToNew = slickUEDAO.getOldToNew(projectid);
    exToInt = oldToNew.getOldToNew();

    List<SlickExerciseAttributeJoin> joins = getSlickExerciseAttributeJoins(exToInt, importUser, exToJoins);

    logger.info("copyUserAndPredefExercises adding " + joins.size() + " attribute joins");
    slickUEDAO.addBulkAttributeJoins(joins);
    return exToInt;
  }

  @NotNull
  private List<SlickExerciseAttributeJoin> getSlickExerciseAttributeJoins(Map<String, Integer> exToInt,
                                                                          int importUser,
                                                                          Map<String, List<Integer>> exToJoins) {
    Timestamp nowT = new Timestamp(System.currentTimeMillis());
    List<SlickExerciseAttributeJoin> joins = new ArrayList<>();
    for (Map.Entry<String, List<Integer>> pair : exToJoins.entrySet()) {
      Integer dbID = exToInt.get(pair.getKey());
      for (Integer attrid : pair.getValue()) {
        joins.add(new SlickExerciseAttributeJoin(-1, importUser, nowT, dbID, attrid));
      }
    }
    return joins;
  }

  /**
   * Assumes we don't have exercise ids on the exercises yet.
   *
   * @param projectid
   * @param slickUEDAO
   * @param exToInt
   * @param importUser
   * @param exercises
   * @param typeOrder
   * @return
   * @see #copyUserAndPredefExercises
   */
  private Map<String, Integer> addContextExercises(int projectid,
                                                   SlickUserExerciseDAO slickUEDAO,
                                                   Map<String, Integer> exToInt,
                                                   int importUser,
                                                   Collection<CommonExercise> exercises,
                                                   Collection<String> typeOrder) {
    int n = 0;
    int ct = 0;
    List<SlickRelatedExercise> pairs = new ArrayList<>();

    Timestamp now = new Timestamp(System.currentTimeMillis());

    if (typeOrder.isEmpty()) {
      logger.error("addContextExercises : huh? type order is empty...?");
    }

    Set<String> missing = new HashSet<>();

    Map<String, Integer> parentToChild = new HashMap<>();

    for (CommonExercise ex : exercises) {
      String oldID = ex.getOldID();

      if (DEBUG) {
        logger.info("addContextExercises adding ex " + ex.getID() + " old " + oldID + " : " + ex.getEnglish() + " : " + ex.getForeignLanguage() + " with " + ex.getDirectlyRelated().size() + " sentences");
      }

      if (oldID == null) logger.error("addContextExercises : huh? old parentID is null for " + ex);
      Integer parentID = exToInt.get(oldID);

//      logger.info("exToInt '" +oldID + "' => " +parentID + " vs ex parentID " + ex.getID());
      if (parentID == null) {
        logger.error("addContextExercises can't find " + oldID + " in map of " + exToInt.size());
        missing.add(oldID);
      } else {
        int contextCount = 1;
        for (CommonExercise context : ex.getDirectlyRelated()) {
          context.getMutable().setOldID(parentID + "_" + (contextCount++));

          SlickRelatedExercise relation = insertContextExercise(projectid, slickUEDAO, importUser, typeOrder, now, parentID, context);
          pairs.add(relation);
          parentToChild.put(oldID, relation.contextexid());

          if (DEBUG) {
            logger.info("addContextExercises map parent ex " + parentID + " -> child ex " + relation.contextexid() +
                " ( " + ex.getDirectlyRelated().size());
          }

          ct++;
          if (ct % 400 == 0) logger.debug("addContextExercises inserted " + ct + " context exercises");
        }
        n++;
      }
    }

    if (!missing.isEmpty()) logger.error("huh? couldn't find " + missing.size() + " exercises : " + missing);

    slickUEDAO.addBulkRelated(pairs);
    logger.info("addContextExercises imported " + n + " predef exercises and " + ct + " context exercises, parent->child size " + parentToChild.size());

    return parentToChild;
  }

  private SlickRelatedExercise insertContextExercise(int projectid,
                                                     SlickUserExerciseDAO slickUEDAO,
                                                     int importUser,
                                                     Collection<String> typeOrder,
                                                     Timestamp now,
                                                     Integer parentExerciseID,
                                                     CommonExercise context) {
    int contextid =
        slickUEDAO.insert(slickUEDAO.toSlick(context, false, projectid, importUser, true, typeOrder));

    return new SlickRelatedExercise(-1, parentExerciseID, contextid, projectid, now);
  }

  /**
   * Actually add the exercises to postgres exercise table.
   *
   * @param projectid
   * @param slickUEDAO
   * @param importUser
   * @param exercises
   * @param typeOrder
   * @see #addExercisesAndAttributes(int, int, SlickUserExerciseDAO, Collection, Collection, Map)
   */
  private Map<String, List<Integer>> addPredefExercises(int projectid,
                                                        SlickUserExerciseDAO slickUEDAO,

                                                        int importUser,
                                                        Collection<CommonExercise> exercises,
                                                        Collection<String> typeOrder,
                                                        Map<String, List<Exercise>> idToCandidateOverride) {
    List<SlickExercise> bulk = new ArrayList<>();
    logger.info("addPredefExercises for " + projectid + " copying " + exercises.size() + " exercises");
    if (typeOrder == null || typeOrder.isEmpty()) {
      logger.error("addPredefExercises huh? no type order?");
    }
    long now = System.currentTimeMillis();

    Map<ExerciseAttribute, Integer> attrToID = new HashMap<>();
    Map<String, List<Integer>> exToJoins = new HashMap<>();

    int replacements = 0;
    int converted = 0;
    logger.info("addPredefExercises adding " + exercises.size());

    for (CommonExercise ex : exercises) {
      String oldID = ex.getOldID();
      if (oldID.isEmpty()) {
        logger.warn("old id is empty for " + ex);
      }
      if (ex.isContext()) {
        logger.warn("addPredefExercises huh? ex " + ex.getOldID() + "/" + ex.getID() + " is a context exercise???\n\n\n");
      }
//      logger.info("addPredefExercises adding ex old #" + oldID + " " + ex.getEnglish() + " " + ex.getForeignLanguage());

      List<Exercise> exercises1 = idToCandidateOverride.get(oldID);

      CommonExercise exToUse = ex;
      if (exercises1 != null && !exercises1.isEmpty()) {
        for (CommonExercise candidate : exercises1) {
          if (candidate.getUpdateTime() > ex.getUpdateTime() &&
              !candidate.getEnglish().equals(ex.getEnglish()) &&
              !candidate.getForeignLanguage().equals(ex.getForeignLanguage())
              ) {
            logger.info("addPredefExercises" +
                "\n\tfor old id " + oldID +
                " replacing" +
                "\n\toriginal " + ex +
                "\n\twith     " + candidate);
            if (candidate.getDirectlyRelated().isEmpty()) {
              candidate.getDirectlyRelated().addAll(exToUse.getDirectlyRelated());
            }
            exToUse = candidate;
            replacements++;

            if (!exToUse.isPredefined()) {
              exToUse.getMutable().setPredef(true);
              converted++;
              logger.info("addPredefExercises converting " + exToUse.getID() + " " + exToUse.getForeignLanguage() + " " + ex.getEnglish());
            }
          }
        }
      }

      bulk.add(slickUEDAO.toSlick(exToUse,
          false,
          projectid,
          importUser,
          false, typeOrder));

      addAttributesAndRememberIDs(slickUEDAO,
          projectid, importUser,
          now, attrToID, exToJoins,

          oldID, ex.getAttributes());
    }

//    logger.info("addPredefExercises add   bulk  " + bulk.size() + " exercises");
    slickUEDAO.addBulk(bulk);
    logger.info("addPredefExercises added bulk  " + bulk.size() + " exercises, " + replacements + " replaced, " + converted + " converted");
    logger.info("addPredefExercises will add    " + exToJoins.size() + " attributes");
    return exToJoins;
  }

  /**
   * @param slickUEDAO
   * @param projectid
   * @param importUser
   * @param now
   * @param attrToID   map of attribute to db id
   * @param exToJoins  map of old ex id to new attribute db id
   * @param oldID      - at this point we don't have exercise db ids - could be done differently...
   * @see #addPredefExercises
   */
  private void addAttributesAndRememberIDs(SlickUserExerciseDAO slickUEDAO,
                                           int projectid,
                                           int importUser,
                                           long now,
                                           Map<ExerciseAttribute, Integer> attrToID,
                                           Map<String, List<Integer>> exToJoins,

                                           String oldID,
                                           List<ExerciseAttribute> attributes) {
    if (attributes != null && !attributes.isEmpty()) {
      List<Integer> joins;
      exToJoins.put(oldID, joins = new ArrayList<>());
      addAttributes(slickUEDAO,
          projectid,
          importUser,
          attributes,
          now,
          attrToID,
          joins);
    }
  }

  /**
   * @param slickUEDAO
   * @param projectid
   * @param importUser
   * @param attributes to translate into slick attributes
   * @param now
   * @param attrToID   map of attribute to db id, so we can only store unique attributes (generally)
   * @param joins      attribute ids to associate with this exercise
   *                   #addA
   */
  private void addAttributes(SlickUserExerciseDAO slickUEDAO,
                             int projectid,
                             int importUser,
                             List<ExerciseAttribute> attributes,

                             long now,
                             Map<ExerciseAttribute, Integer> attrToID,
                             List<Integer> joins) {
    for (ExerciseAttribute attribute : attributes) {
      int id;
      if (attrToID.containsKey(attribute)) {
        id = attrToID.get(attribute);
      } else {
//        known.add(attribute);
        id = slickUEDAO.addAttribute(projectid, now, importUser, attribute);
        attrToID.put(attribute, id);
        //  logger.info("addPredef " + attribute + " = " + id);
      }
      joins.add(id);
    }
  }

  /**
   * @param db
   * @param oldToNewUser
   * @param projectid
   * @param typeOrder
   * @see #copyUserAndPredefExercises
   */
  private List<Exercise> addUserExercises(DatabaseImpl db,
                                          Map<Integer, Integer> oldToNewUser,
                                          int projectid,
                                          Collection<String> typeOrder,
                                          Map<String, List<Exercise>> idToCandidateOverride) {
    List<Exercise> userExercises = new ArrayList<>();
    try {
      int c = 0;
      UserExerciseDAO ueDAO = new UserExerciseDAO(db);
      ueDAO.setExerciseDAO(db.getExerciseDAO(projectid)); // for the type order
      Collection<Exercise> allUserExercises = ueDAO.getAllUserExercises();
      if (allUserExercises.isEmpty()) {
        logger.error("addUserExercises : no user exercises for " + projectid + " and " + ueDAO);
      }
      logger.info("addUserExercises copying " + allUserExercises.size() + " user exercises for project " + projectid);

      if (typeOrder.isEmpty()) {
        logger.error("addUserExercises huh? for " + projectid + " type order is empty?\n\n\n");
      }

      int overrides = 0;
      int userEx = 0;
      for (Exercise userExercise : allUserExercises) {
        Integer userID = oldToNewUser.get(userExercise.getCreator());
        if (userID == null) {
          if (c++ < 50)
            logger.error("user exercise : no user " + userExercise.getCreator() + " for exercise " + userExercise);
        } else {
          userExercise.setCreator(userID);
          String oldID = userExercise.getOldID();
          if (userExercise.isOverride()) {
            List<Exercise> exercises = idToCandidateOverride.computeIfAbsent(oldID, k -> new ArrayList<>());
            exercises.add(userExercise);
            overrides++;
          } else {
            userExercises.add(userExercise);
            userEx++;
          }
        }
      }

      logger.info("addUserExercises overrides " + overrides + " user ex " + userEx);

    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
    return userExercises;
  }
}