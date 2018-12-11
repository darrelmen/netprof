package mitll.langtest.server.database.copy;

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.server.database.userexercise.UserExerciseDAO;
import mitll.langtest.shared.exercise.ClientExercise;
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

  private static final boolean DEBUG = false;
  public static final String INTEGRATED_CHINESE_2 = "Integrated Chinese 2";
  public static final String UNIT = "Unit";
  public static final String LESSON = "Lesson";
  public static final String CUSTOM = "Custom";
  private static final int DEFAULT_DIALOG_ID = 1;

  /**
   * TODO :  How to make sure we don't add duplicates?
   *
   * @param db
   * @param oldToNewUser
   * @param projectid
   * @param typeOrder
   * @param checkConvert
   * @see CopyToPostgres#copyUserAndPredefExercisesAndLists
   */
  Map<String, Integer> copyUserAndPredefExercises(DatabaseImpl db,
                                                  Map<Integer, Integer> oldToNewUser,
                                                  int projectid,
                                                  Map<Integer, String> idToFL,
                                                  Collection<String> typeOrder,
                                                  Map<String, Integer> parentToChild, boolean checkConvert) {

    logger.info("copyUserAndPredefExercises for " + projectid + " typeOrder is " + typeOrder);
    if (typeOrder.isEmpty()) {
      logger.error("copyUserAndPredefExercises huh? type order is empty?\n\n\n");
    }
    Map<String, List<Exercise>> idToCandidateOverride = new HashMap<>();
    List<Exercise> customExercises = addUserExercises(db, oldToNewUser, DatabaseImpl.IMPORT_PROJECT_ID, typeOrder, idToCandidateOverride);

    List<Exercise> converted = new ArrayList<>();

    if (checkConvert) {
      new ChineseMapping().doConversion(customExercises, converted);
    }

    customExercises.removeAll(converted);
    List<CommonExercise> toImport = db.getExercises(DatabaseImpl.IMPORT_PROJECT_ID, false);
    toImport.addAll(converted);
    logger.info("importing " + toImport.size() + " customExercises with " + converted.size());

    Map<Integer, Integer> dominoToExID = new HashMap<>();
    IUserExerciseDAO slickUEDAO =  db.getUserExerciseDAO();
    parentToChild.putAll(addExercises(
        db.getUserDAO().getImportUser(),
        projectid,
        idToFL,
        slickUEDAO,
        toImport,
        typeOrder,
        idToCandidateOverride,
        dominoToExID,
        DEFAULT_DIALOG_ID));

    Map<String, Integer> exToInt = getOldToNewExIDs(db, projectid);
    reallyAddingUserExercises(projectid, typeOrder, slickUEDAO, exToInt, customExercises);

    logger.info("copyUserAndPredefExercises : finished copying customExercises - found " + exToInt.size());
    return exToInt;
  }


  Map<String, Integer> getOldToNewExIDs(DatabaseImpl db, int projectid) {
    return db.getUserExerciseDAO().getOldToNew(projectid).getOldToNew();
  }

  /**
   * @param projectid
   * @param typeOrder
   * @param slickUEDAO
   * @param exToInt
   * @param exercises
   * @see #copyUserAndPredefExercises
   */
  private void reallyAddingUserExercises(int projectid,
                                         Collection<String> typeOrder,
                                         IUserExerciseDAO slickUEDAO,
                                         Map<String, Integer> exToInt,
                                         List<Exercise> exercises) {
    List<SlickExercise> bulk = new ArrayList<>();

    for (Exercise userCandidate : exercises) {
      String oldID = userCandidate.getOldID();
      Integer existingPredefID = exToInt.get(oldID);

      if (existingPredefID != null) {
        CommonExercise byExID = slickUEDAO.getByExID(existingPredefID, false);
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
   * @param dominoToExID
   * @param dialogID
   * @return parent->child map - NOT USED IN SYNC
   * @see #copyUserAndPredefExercises
   * @see mitll.langtest.server.domino.ProjectSync#getDominoUpdateResponse
   */
  public Map<String, Integer> addExercises(int importUser,
                                           int projectid,
                                           Map<Integer, String> idToFL,
                                           IUserExerciseDAO slickUEDAO,
                                           Collection<CommonExercise> exercises,
                                           Collection<String> typeOrder,
                                           Map<String, List<Exercise>> idToCandidateOverride,
                                           Map<Integer, Integer> dominoToExID,
                                           int dialogID) {

    logger.info("copyUserAndPredefExercises for project " + projectid +
        "\n\tfound " + exercises.size() + " old exercises" +
        "\n\tand   " + idToCandidateOverride.size() + " overrides");

    // TODO : why not add it to interface?
    Map<CommonExercise, Integer> exToInt = addExercisesAndAttributes(importUser, projectid, slickUEDAO, exercises, typeOrder,
        idToCandidateOverride, dominoToExID, false);

    idToFL.putAll(slickUEDAO.getIDToFL(projectid));

    logger.info("copyUserAndPredefExercises old->new for project #" + projectid +
        " : " + exercises.size() + " exercises, " + exToInt.size());

    return addContextExercises(projectid, slickUEDAO, exToInt, importUser, exercises, typeOrder, dialogID);
  }

  /**
   * @param importUser
   * @param projectid
   * @param slickUEDAO
   * @param exercises
   * @param typeOrder
   * @see mitll.langtest.server.domino.ProjectSync#getDominoUpdateResponse
   */
  public void addContextExercises(int importUser,
                                  int projectid,
                                  IUserExerciseDAO slickUEDAO,
                                  Collection<CommonExercise> exercises,
                                  Collection<String> typeOrder) {
    List<SlickRelatedExercise> pairs = new ArrayList<>();

    Timestamp now = new Timestamp(System.currentTimeMillis());

    logger.info("addContextExercises adding " + exercises.size() + " context exercises ");
    for (CommonExercise context : exercises) {
      logger.info("addContextExercises adding context " + context);
      logger.info("addContextExercises context id     " + context.getID() + " with parent " + context.getParentExerciseID());
      if (context.getParentExerciseID() > 0) {
        SlickRelatedExercise e = insertContextExercise(projectid, slickUEDAO, importUser, typeOrder,
            now, context.getParentExerciseID(), context, 1);
        context.getMutable().setID(e.contextexid());
        pairs.add(e);
      } else {
        logger.warn("addContextExercises ex " + context.getID() + " " + context.getEnglish() + " has no parent id set?");
      }
    }
    logger.info("addContextExercises adding " + pairs.size() + " pairs exercises ");

    slickUEDAO.getRelatedExercise().addBulkRelated(pairs);
  }

  /**
   * @param importUser
   * @param projectid
   * @param slickUEDAO
   * @param exercises
   * @param typeOrder
   * @param idToCandidateOverride
   * @param dominoToExID
   * @param checkExists if true, don't add redundant exercise attributes
   * @return
   * @see #addExercises(int, int, Map, IUserExerciseDAO, Collection, Collection, Map, Map, int)
   * @see #addPredefExercises
   */
  public Map<CommonExercise, Integer> addExercisesAndAttributes(int importUser,
                                                                int projectid,
                                                                IUserExerciseDAO slickUEDAO,
                                                                Collection<CommonExercise> exercises,
                                                                Collection<String> typeOrder,
                                                                Map<String, List<Exercise>> idToCandidateOverride,
                                                                Map<Integer, Integer> dominoToExID,
                                                                boolean checkExists) {
    Map<CommonExercise, Integer> exToInt = new HashMap<>();

    logger.info("addExercisesAndAttributes typeOrder : " + typeOrder);

    Map<Integer, List<Integer>> exToJoins =
        addPredefExercises(projectid, slickUEDAO, importUser, exercises, typeOrder, idToCandidateOverride, exToInt, checkExists);
    exToInt.forEach((commonExercise, exid) -> dominoToExID.put(commonExercise.getDominoID(), exid));

    List<SlickExerciseAttributeJoin> joins = getSlickExerciseAttributeJoins(importUser, exToJoins);

    logger.info("addExercisesAndAttributes adding " + joins.size() + " attribute joins");
    slickUEDAO.getExerciseAttributeJoin().addBulkAttributeJoins(joins);
    return exToInt;
  }

  /**
   * @param importUser
   * @param exToJoins
   * @return
   * @paramx exToInt
   * @see #addExercisesAndAttributes(int, int, IUserExerciseDAO, Collection, Collection, Map, Map, boolean)
   */
  @NotNull
  private List<SlickExerciseAttributeJoin> getSlickExerciseAttributeJoins(int importUser,
                                                                          Map<Integer, List<Integer>> exToJoins) {
    Timestamp nowT = new Timestamp(System.currentTimeMillis());
    List<SlickExerciseAttributeJoin> joins = new ArrayList<>();

    for (Map.Entry<Integer, List<Integer>> pair : exToJoins.entrySet()) {
//      logger.info("getSlickExerciseAttributeJoins : ex->id  " + pair);
      // String key = pair.getKey();
      Integer dbID = pair.getKey();
      //     Integer dbID = exToInt.get(key);
      //logger.info("getSlickExerciseAttributeJoins : ex " + key + " = " + dbID);
//
//      if (dbID == null) {
//        logger.warn("getSlickExerciseAttributeJoins : huh? no db id for exercise " + key);
//      } else {
      pair.getValue().forEach(attrid -> joins.add(new SlickExerciseAttributeJoin(-1, importUser, nowT, dbID, attrid)));
//      }
    }

    return joins;
  }

  /**
   * Assumes we don't have exercise ids on the exercises yet.
   * <p>
   * NOTE : only can handle one exercise and one matching context exercise.
   *
   * @param projectid
   * @param slickUEDAO
   * @param exToInt
   * @param importUser
   * @param exercises
   * @param typeOrder
   * @param dialogID
   * @return parentToChild
   * @see #copyUserAndPredefExercises
   */
  private Map<String, Integer> addContextExercises(int projectid,
                                                   IUserExerciseDAO slickUEDAO,
                                                   Map<CommonExercise, Integer> exToInt,
                                                   int importUser,
                                                   Collection<CommonExercise> exercises,
                                                   Collection<String> typeOrder, int dialogID) {
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
      Integer parentID = exToInt.get(ex);

//      logger.info("exToInt '" +oldID + "' => " +parentID + " vs ex parentID " + ex.getID());
      if (parentID == null) {
        logger.error("addContextExercises can't find " + oldID + " in map of " + exToInt.size());
        missing.add(oldID);
      } else {
        int contextCount = 1;
        for (ClientExercise context : ex.getDirectlyRelated()) {
          CommonExercise serverContext = (CommonExercise) context;
          serverContext.getMutable().setOldID(parentID + "_" + (contextCount++));

          SlickRelatedExercise relation =
              insertContextExercise(projectid, slickUEDAO, importUser, typeOrder, now, parentID, serverContext, dialogID);
          pairs.add(relation);
          int newContextExID = relation.contextexid();
          //  logger.info("\taddContextExercises context id is "+ context.getID());
          if (context.getID() == -1) {
            if (DEBUG) logger.info("---> addContextExercises set context id to " + newContextExID);
            serverContext.getMutable().setID(newContextExID);
          }
          parentToChild.put(oldID, newContextExID);

          if (DEBUG) {
            logger.info("addContextExercises map parent ex " + parentID + " -> child ex " + newContextExID +
                " ( " + ex.getDirectlyRelated().size());
          }

          ct++;
          if (ct % 400 == 0) logger.debug("addContextExercises inserted " + ct + " context exercises");
        }
        n++;
      }
    }

    if (!missing.isEmpty()) logger.error("huh? couldn't find " + missing.size() + " exercises : " + missing);

    slickUEDAO.getRelatedExercise().addBulkRelated(pairs);
    logger.info("addContextExercises imported " + n + " predef exercises and " + ct + " context exercises, parent->child size " + parentToChild.size());

    return parentToChild;
  }

  /**
   * @param projectid
   * @param slickUEDAO
   * @param importUser
   * @param typeOrder
   * @param now
   * @param parentExerciseID
   * @param context
   * @param dialogID
   * @return
   */
  private SlickRelatedExercise insertContextExercise(int projectid,
                                                     IUserExerciseDAO slickUEDAO,
                                                     int importUser,
                                                     Collection<String> typeOrder,
                                                     Timestamp now,
                                                     Integer parentExerciseID,
                                                     CommonExercise context,
                                                     int dialogID) {
    int contextid =
        slickUEDAO.insert(slickUEDAO.toSlick(context, projectid, importUser, true, typeOrder));

    return new SlickRelatedExercise(-1, parentExerciseID, contextid, projectid, dialogID, now);
  }

  /**
   * Actually add the exercises to postgres exercise table.
   *
   * @param projectid
   * @param slickUEDAO
   * @param importUser
   * @param exercises
   * @param typeOrder
   * @param idToCandidateOverride
   * @param exToInt
   * @param checkExists if true, don't add an attribute if it's already in there
   * @see #addExercisesAndAttributes(int, int, IUserExerciseDAO, Collection, Collection, Map, Map, boolean)
   */
  private Map<Integer, List<Integer>> addPredefExercises(int projectid,
                                                         IUserExerciseDAO slickUEDAO,

                                                         int importUser,
                                                         Collection<CommonExercise> exercises,
                                                         Collection<String> typeOrder,
                                                         Map<String, List<Exercise>> idToCandidateOverride,
                                                         Map<CommonExercise, Integer> exToInt,
                                                         boolean checkExists) {
    logger.info("addPredefExercises for " + projectid + " copying " + exercises.size() + " exercises");
    if (typeOrder == null || typeOrder.isEmpty()) {
      logger.error("addPredefExercises huh? no type order?");
    }
    long now = System.currentTimeMillis();

    Map<ExerciseAttribute, Integer> attrToID = new HashMap<>();
    Map<Integer, List<Integer>> exToJoins = new HashMap<>();

    int replacements = 0;
    int converted = 0;
//    logger.info("addPredefExercises adding " + exercises.size());

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

      int exerciseID = slickUEDAO.insert(slickUEDAO.toSlick(exToUse,
          projectid,
          importUser,
          false, typeOrder));
      exToInt.put(exToUse, exerciseID);


      addAttributesAndRememberIDs(slickUEDAO,
          projectid, importUser,
          now, attrToID, exToJoins,

          exerciseID, ex.getAttributes(), checkExists);
    }

//    logger.info("addPredefExercises add   bulk  " + bulk.size() + " exercises");
    // slickUEDAO.addBulk(bulk);
    logger.info("addPredefExercises " + replacements + " replaced, " + converted + " converted");
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
   * @param newID      - at this point we don't have exercise db ids - could be done differently...
   * @param attributes
   * @param checkExists
   * @see #addPredefExercises
   */
  private void addAttributesAndRememberIDs(IUserExerciseDAO slickUEDAO,
                                           int projectid,
                                           int importUser,
                                           long now,
                                           Map<ExerciseAttribute, Integer> attrToID,
                                           Map<Integer, List<Integer>> exToJoins,


                                           int newID,
                                           List<ExerciseAttribute> attributes,
                                           boolean checkExists) {
    if (attributes != null && !attributes.isEmpty()) {
      List<Integer> joins = new ArrayList<>();
      exToJoins.put(newID, joins);
      addAttributes(slickUEDAO,
          projectid,
          importUser,
          attributes,
          now,
          attrToID,
          joins,
          checkExists);
    }
  }

  /**
   * @param slickUEDAO
   * @param projectid
   * @param importUser
   * @param attributes  to translate into slick attributes
   * @param now
   * @param attrToID    map of attribute to db id, so we can only store unique attributes (generally)
   * @param joins       attribute ids to associate with this exercise
   * @param checkExists
   */
  private void addAttributes(IUserExerciseDAO slickUEDAO,
                             int projectid,
                             int importUser,
                             List<ExerciseAttribute> attributes,

                             long now,
                             Map<ExerciseAttribute, Integer> attrToID,
                             List<Integer> joins, boolean checkExists) {
    for (ExerciseAttribute attribute : attributes) {
      int id;
      if (attrToID.containsKey(attribute)) {
        id = attrToID.get(attribute);
      } else {
        id = slickUEDAO.getExerciseAttribute().findOrAddAttribute(projectid, now, importUser, attribute, checkExists);
        attrToID.put(attribute, id);

        logger.info("addPredef " + attribute + " = " + id);
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