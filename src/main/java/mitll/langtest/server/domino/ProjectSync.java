package mitll.langtest.server.domino;

import mitll.langtest.server.database.DAOContainer;
import mitll.langtest.server.database.copy.ExerciseCopy;
import mitll.langtest.server.database.exercise.ExerciseServices;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.project.IProjectManagement;
import mitll.langtest.server.database.project.ProjectServices;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.DominoProject;
import mitll.langtest.shared.project.Language;
import mitll.npdata.dao.SlickAudio;
import mitll.npdata.dao.SlickExercise;
import mitll.npdata.dao.SlickExerciseAttributeJoin;
import mitll.npdata.dao.SlickProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static mitll.langtest.server.database.project.ProjectManagement.MODIFIED;
import static mitll.langtest.server.database.project.ProjectManagement.NUM_ITEMS;
import static mitll.langtest.shared.exercise.DominoUpdateItem.ITEM_STATUS.*;
import static mitll.langtest.shared.exercise.DominoUpdateResponse.UPLOAD_STATUS.SUCCESS;

public class ProjectSync implements IProjectSync {
  private static final Logger logger = LogManager.getLogger(ProjectSync.class);
  public static final String ANY = "Any";

  public static final String ID = "_id";
  public static final String NAME = "name";
  public static final String CREATE_TIME = "createTime";
  private static final boolean DEBUG = false;

  public static final String UNKNOWN = "unknown";
  private final SimpleDateFormat format = new SimpleDateFormat("MMM d, yy h:mm a");

  static final String MONGO_TIME = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
  public static final ZoneId UTC = ZoneId.of("UTC");

  private final ProjectServices projectServices;
  private final IProjectManagement projectManagement;
  private IUserExerciseDAO userExerciseDAO;
  private final DAOContainer daoContainer;
  private ExerciseServices exerciseServices;
  private SlickUserExerciseDAO slickUEDAO;

  /**
   * @param projectServices
   * @param projectManagement
   * @param daoContainer
   * @param userExerciseDAO
   * @see mitll.langtest.server.services.ProjectServiceImpl#addPending
   */
  public ProjectSync(ProjectServices projectServices,
                     IProjectManagement projectManagement,
                     DAOContainer daoContainer,
                     IUserExerciseDAO userExerciseDAO,
                     ExerciseServices exerciseServices) {
    this.projectServices = projectServices;
    this.projectManagement = projectManagement;
    this.daoContainer = daoContainer;
    this.userExerciseDAO = userExerciseDAO;
    this.exerciseServices = exerciseServices;
    slickUEDAO = (SlickUserExerciseDAO) daoContainer.getUserExerciseDAO();
  }

  /**
   * Adding exercises to a project!
   * Copies any existing audio for the language (from a production project) that has matching transcripts.
   * <p>
   * I.e. if there's already a recording of "dog" in another english project, just reuse that recording for your
   * new "dog" vocabulary item.  Good for projects that are subsets (or collages?) of existing content.
   * <p>
   * Does some sanity checking - the exercises come from a domino project:
   * <p>
   * 1) Is the project an existing project and is it associated with the domino project for this bundle of exercises?
   * 2) Is the project a new project (and hence has no domino id yet) and is there already another project bound
   * to this exercise bundle's domino project?
   *
   * @param projectid
   * @param doChange
   * @see mitll.langtest.client.project.ProjectChoices#showImportDialog
   * @see mitll.langtest.server.services.ProjectServiceImpl#addPending
   */
  @Override
  public DominoUpdateResponse addPending(int projectid, int importUser, boolean doChange) {
    return getDominoUpdateResponse(projectid, importUser, doChange, projectManagement.getImportFromDomino(projectid));
  }

  @Override
  @NotNull
  public DominoUpdateResponse getDominoUpdateResponse(int projectid,
                                                      int importUser, boolean doChange,
                                                      ImportInfo importFromDomino) {
    long requestTime = System.currentTimeMillis();
    Project project = projectServices.getProject(projectid);

    int dominoid = project.getProject().dominoid();
    Timestamp modified = project.getProject().lastimport();
    String timestamp = format.format(modified);
    int jsonDominoID = importFromDomino.getDominoID();
    if (dominoid != -1 && dominoid != jsonDominoID) {
      logger.warn("addPending - json domino id = " + dominoid + " vs import project id " + jsonDominoID);
      return new DominoUpdateResponse(DominoUpdateResponse.UPLOAD_STATUS.WRONG_PROJECT,
          jsonDominoID, dominoid, new HashMap<>(), new ArrayList<>(), "");
    } else {
      if (dominoid == -1) {  // relevant? possible?
        List<Project> existingBound = getProjectForDominoID(jsonDominoID);
        if (!existingBound.isEmpty()) {
          return getAnotherProjectResponse(dominoid, jsonDominoID, existingBound);
        }
      }

      // so on update we have three sets of exercises - Add, change, delete
      //  new exercises we haven't seen before,
      //  ones we have which might have changed and would then need to be updated
      //  ones that are current but not in domino and have been deleted

      Map<Integer, SlickExercise> dominoToNonContextEx = slickUEDAO.getDominoToSlickEx(projectid);
      List<CommonExercise> newEx = new ArrayList<>();
      List<CommonExercise> newContextEx = new ArrayList<>();
      List<CommonExercise> importUpdateEx = new ArrayList<>();

      // so here we map import exercises to known exercise table ids
      Map<CommonExercise, Integer> importToKnownID = new HashMap<>();
      Map<Integer, Integer> dominoToExID = new HashMap<>();

      getNewAndChangedExercises(projectid, importFromDomino, dominoToNonContextEx, newEx, importUpdateEx, importToKnownID);

      List<DominoUpdateItem> updates = new ArrayList<>();

      {
        Collection<String> typeOrder = project.getTypeOrder();

        if (!newEx.isEmpty()) {
          // add new
          logger.info("addPending adding " + newEx.size() + " new non-context exercises");

          if (doChange) {
            new ExerciseCopy().addExercises(
                importUser,
                projectid,
                new HashMap<>(),
                slickUEDAO,
                newEx,
                typeOrder,
                new HashMap<>(),
                dominoToExID);
          }
          newEx.forEach(commonExercise -> updates.add(new DominoUpdateItem(commonExercise, ADD)));
        }

        // add change messages for all normal exercises
        importUpdateEx.forEach(commonExercise -> updates.add(new DominoUpdateItem(commonExercise, CHANGE)));

        Set<Integer> contextDeletes = new HashSet<>();
        updates.addAll(getNewAndChangedContextExercises(projectid, importFromDomino, newContextEx, importUpdateEx, contextDeletes, importToKnownID));

        logger.info("addPending Context # to delete " + contextDeletes.size() + " add " + newContextEx.size());

        if (!newContextEx.isEmpty() && doChange) {
          logger.info("addPending adding " + newContextEx.size() + " new context exercises");
          // creates and adds
          new ExerciseCopy().addContextExercises(importUser, projectid, slickUEDAO, newContextEx, typeOrder);
        }

        if (!importUpdateEx.isEmpty()) {
          // now update...
          // update the exercises...
          logger.info("addPending updating " + importUpdateEx.size() + " exercises");
          if (doChange) {
            doUpdate(projectid, importUser, slickUEDAO, importUpdateEx, typeOrder, dominoToNonContextEx);
          }

        }
        if (doChange) {
          logger.info("addPending deleting " + contextDeletes.size() + " context exercises");
          doContextDeletes(contextDeletes);
        }
        updates.addAll(doDelete(importFromDomino, dominoToNonContextEx, doChange, projectid));
      }

      logger.info("addPending doChange = " + doChange + " new " + newEx.size() + " update " + importUpdateEx.size() + " context " + newContextEx.size());
      if (doChange) {
        //   Map<String, Integer> oldToNew = slickUEDAO.getOldToNew(projectid).getOldToNew();
        if (!newEx.isEmpty()) {
          copyAudio(projectid, newEx, /*oldToNew,*/ dominoToExID);
        }
        if (!importUpdateEx.isEmpty()) {
          copyAudio(projectid, importUpdateEx, dominoToExID);
        }
        if (!newContextEx.isEmpty()) {
          copyAudio(projectid, newContextEx, dominoToExID);
        }
        updateProjectIfSomethingChanged(jsonDominoID, newEx, importUpdateEx, project.getProject(), requestTime);
      }

      logger.info("addPending got num updates = " + importUpdateEx.size());
      // todo : should we configure project if it didn't change?
      projectManagement.configureProject(project, false, doChange);
      int numExercises = project.getRawExercises().size();

      DominoUpdateResponse dominoUpdateResponse =
          new DominoUpdateResponse(SUCCESS, jsonDominoID, dominoid,
              getProps(project.getProject(), numExercises), updates, timestamp);
      logger.info("addPending returning" +
          "\n\tresp      " + dominoUpdateResponse +
          "\n\t# updates " + dominoUpdateResponse.getUpdates().size() +
          "\n\tprops     " + dominoUpdateResponse.getProps());
      //dominoUpdateResponse.getUpdates().forEach(logger::info);
      return dominoUpdateResponse;
    }
  }

  /**
   * @param contextDeletes
   * @see #getDominoUpdateResponse(int, int, boolean, ImportInfo)
   */
  private void doContextDeletes(Set<Integer> contextDeletes) {
    userExerciseDAO.deleteByExID(contextDeletes);

    for (int id : contextDeletes) {
      int i = userExerciseDAO.deleteRelated(id);
      if (i == 1) {
        logger.info("doContextDeletes deleted relation for context " + id);
      } else if (i == 0) {
        logger.warn("doContextDeletes Did not deleted relation for context " + id);
      } else {
        logger.warn("doContextDeletes (?) deleted " + i + " relations for context " + id);

      }
    }
  }

  /**
   * @param projectid
   * @param importFromDomino
   * @param dominoToNonContextEx for all known exercises in project
   * @param newEx
   * @param importUpdateEx
   * @return
   * @see #getDominoUpdateResponse
   */
  @NotNull
  private void getNewAndChangedExercises(int projectid,
                                         ImportInfo importFromDomino,
                                         Map<Integer, SlickExercise> dominoToNonContextEx,

                                         List<CommonExercise> newEx,
                                         List<CommonExercise> importUpdateEx,
                                         Map<CommonExercise, Integer> importToKnownID) {
    logger.info("addPending found " + dominoToNonContextEx.size() + " current exercises for project #" + projectid);

    {
      Set<Integer> currentIDs = dominoToNonContextEx.keySet();
      Map<Integer, CommonExercise> dominoIDToChangedExercise = getDominoIDToExercise(importFromDomino.getChangedExercises());
      Map<Integer, CommonExercise> dominoIDToAddedExercise = getDominoIDToExercise(importFromDomino.getAddedExercises());

      logger.info("addPending importing " +
          dominoIDToAddedExercise.size() + " added, " +
          dominoIDToChangedExercise.size() + " changed exercises ");

      // three piles:
      // currentIDs exercises for the project that are not in the import should be deleted
      // import exercises not in the currentIDs set are new and need to be added
      // matching exercises need to be checked to see if they have changed


      addChangedExercises(dominoIDToChangedExercise, dominoToNonContextEx,
          currentIDs,
          newEx, importUpdateEx, importToKnownID);
      addNewExercises(dominoToNonContextEx, newEx,
          dominoIDToAddedExercise);

    }
    //  return oldIDToExer;
  }

  private void addChangedExercises(Map<Integer, CommonExercise> dominoIDToChangedExercise,
                                   Map<Integer, SlickExercise> dominoToNonContextEx,
                                   Set<Integer> currentIDs,

                                   List<CommonExercise> newEx,
                                   List<CommonExercise> importUpdateEx,
                                   Map<CommonExercise, Integer> importToKnownID) {
    dominoIDToChangedExercise.forEach((dominoID, importEx) -> {
      logger.info("addPending import" +
          "\n\tcontext   " + importEx.isContext() +
          "\n\tdomino id " + dominoID +
          "\n\teng       " + importEx.getEnglish() +
          "\n\tfl        " + importEx.getForeignLanguage()
      );
      // logger.info("addPending import importEx  '" + importEx.getEnglish() + "' = " + importEx.getForeignLanguage());

      // try to find it by domino or np id
      // np id for production projects.
      SlickExercise currentKnownExercise = dominoToNonContextEx.get(dominoID);

      if (currentKnownExercise == null) {  // how can this happen???
        logger.warn("\n\n\naddPending found new CHANGED ex for domino id " + dominoID + //" / " + npID +
            " import " + importEx.getEnglish() + " " + importEx.getForeignLanguage() + " context " + importEx.isContext());
        //    newEx.add(importEx);
      } else {

        if (currentIDs.contains(dominoID)/* || oldIDs.contains(npID)*/) {
          importUpdateEx.add(importEx);
          MutableExercise mutable = importEx.getMutable();
          int id = currentKnownExercise.id();

          if (id == -1) {
            logger.error("huh? no id on " + currentKnownExercise);
          } else {
            logger.info("\tbefore " + id + "/" + importEx.getID() + "/" + importEx.getOldID() + " domino " + importEx.getDominoID());
            mutable.setID(currentKnownExercise.id());
            importToKnownID.put(importEx, id);
            logger.info("\tafter  " + id + "/" + importEx.getID() + " domino " + importEx.getDominoID());
          }
        } else { // impossible?
          newEx.add(importEx);
          logger.warn("\n\n\n addPending 2 found new ex for domino id " + dominoID +// " / " + npID +
              " import " + importEx.getEnglish() + " " + importEx.getForeignLanguage() + " context " + importEx.isContext());
        }
      }
    });
  }

  private void addNewExercises(Map<Integer, SlickExercise> dominoToNonContextEx,
                               List<CommonExercise> newEx,
                               Map<Integer, CommonExercise> dominoIDToAddedExercise) {
    dominoIDToAddedExercise.forEach((dominoID, importEx) -> {
      if (DEBUG) {
        logger.info("addPending import ADDED" +
            "\n\tcontext   " + importEx.isContext() +
            "\n\tdomino id " + dominoID +
            "\n\teng       " + importEx.getEnglish() +
            "\n\tfl        " + importEx.getForeignLanguage()
        );
      }
      // logger.info("addPending import importEx  '" + importEx.getEnglish() + "' = " + importEx.getForeignLanguage());

      // try to find it by domino or np id
      // np id for production projects.
      SlickExercise currentKnownExercise = dominoToNonContextEx.get(dominoID);

      if (currentKnownExercise == null) {
        if (DEBUG) logger.info("addPending found new ADDED ex for domino id " + dominoID +
            " import " + importEx.getEnglish() + " " + importEx.getForeignLanguage() + " context " + importEx.isContext());
        newEx.add(importEx);
      } else {
        logger.warn("addPending huh? already know about " + currentKnownExercise);
      }
    });
    logger.info("addPending import ADDED " + newEx.size() + " new exercises...");
  }


  /**
   * change handling for context exercises
   *
   * for all changed vocab exercises
   *
   * examine the context sentences on import vs current - one-to-one
   * if new - add
   * if delete - remove current
   * if changed, change it
   *
   * match up with
   *
   * @return
   * @see #getDominoUpdateResponse
   */
  @NotNull
  private List<DominoUpdateItem> getNewAndChangedContextExercises(int projectid,
                                                                  ImportInfo importFromDomino,

                                                                  List<CommonExercise> newContextEx,
                                                                  List<CommonExercise> importUpdateEx,
                                                                  Set<Integer> toDelete,
                                                                  Map<CommonExercise, Integer> importToKnownID) {
    List<DominoUpdateItem> updateItems = new ArrayList<>();
    {
      Map<CommonExercise, CommonExercise> childToParent = new HashMap<>();
      Map<String, CommonExercise> dominoIDToContextChangedExercise = getDominoIDToContextExercise(importFromDomino.getChangedExercises(), childToParent);

      // keys will look like 1234_1 or 1234_2 for first or second sentence inside a domino doc
      logger.info("getNewAndChangedContextExercises " +
          "\n\timporting " + dominoIDToContextChangedExercise.size() +
          "\n\tcontext : " + dominoIDToContextChangedExercise.keySet());

      // three piles:
      // currentIDs exercises for the project that are not in the import should be deleted
      // import exercises not in the currentIDs set are new and need to be added
      // matching exercises need to be checked to see if they have changed

      importToKnownID.forEach((importEx, id) -> {
        logger.info("getNewAndChangedContextExercises " +
            "\n\tcurrent   " + importEx.getID() + " = " + id +
            "\n\tOLD ID    " + importEx.getOldID() +
            "\n\tDomino ID " + importEx.getDominoID());
        if (DEBUG) logger.info("getNewAndChangedContextExercises current " + importEx);

        CommonExercise currentParent = exerciseServices.getExercise(projectid, id);

        //List<CommonExercise> importContext = importEx.getDirectlyRelated();
        int importCount = importEx.getDirectlyRelated().size();
        if (currentParent == null) {
          logger.info("getNewAndChangedContextExercises import  " + importEx.getID() + "/" + importEx.getDominoID() +
              " has " + importCount + " but can't find it by " + id);
        } else {
          List<CommonExercise> currentContextOnParent = new ArrayList<>(currentParent.getDirectlyRelated());

          logger.info("getNewAndChangedContextExercises current " + currentParent.getID() + " has " + currentContextOnParent.size());
          logger.info("getNewAndChangedContextExercises import  " + importEx.getID() + "/" + importEx.getDominoID() + " has " + importCount + " context");

          // first figure out matching sentences, regardless of order
          List<CommonExercise> unchangedImport = new ArrayList<>();
          List<CommonExercise> changedOrNew = new ArrayList<>();


          for (CommonExercise importC : importEx.getDirectlyRelated()) {
            CommonExercise knownContextMatch = null;
            for (CommonExercise knownContext : currentContextOnParent) {
              if (!didChange(importC, knownContext)) {
                if (DEBUG) logger.info("\tgetNewAndChangedContextExercises no change for " + knownContext);
                unchangedImport.add(importC);
                if (DEBUG)
                  logger.info("\tgetNewAndChangedContextExercises unchanged (" + unchangedImport.size() + ") import context ex " + importC.getID() + " " + importC.getEnglish() + " : " + importC.getForeignLanguage());

                if (importC.getID() == -1) {
                  importC.getMutable().setID(knownContext.getID());
                  logger.info("\tgetNewAndChangedContextExercises NOW : import context ex " + importC.getID() + " " + importC.getEnglish() + " : " + importC.getForeignLanguage());
                }
                knownContextMatch = knownContext;
                break;
              }
            }

            if (knownContextMatch == null) {  // no match, must be changed or new
              changedOrNew.add(importC);
            } else {
              if (!currentContextOnParent.remove(knownContextMatch)) {
                logger.warn("huh? " + knownContextMatch);
              }
              ;
            }
          }
          if (DEBUG)
            logger.info("getNewAndChangedContextExercises : comparing current num = " + currentContextOnParent.size() + " vs import " + importCount + "  vs " + unchangedImport.size());

          Iterator<CommonExercise> currentSentences = currentContextOnParent.iterator();
          //importContext = new ArrayList<>(importContext);
          //importContext.forEach(ex -> logger.info("to        import " + ex.getID() + " / " + ex.getDominoID() + " " + ex.getEnglish()));
          if (DEBUG)
            unchangedImport.forEach(ex -> logger.info("unchanged   import " + ex.getID() + " / " + ex.getDominoID() + " eng '" + ex.getEnglish() + "' fl '" + ex.getForeignLanguage() + "'"));
          //importContext.removeAll(unchangedImport);
          if (DEBUG)
            changedOrNew.forEach(ex -> logger.info("new or changed import " + ex.getID() + " / " + ex.getDominoID() + " " + ex.getEnglish() + "' fl '" + ex.getForeignLanguage() + "'"));

          Iterator<CommonExercise> changedOrNewIter = changedOrNew.iterator();

          logger.info("comparing current num = " + currentContextOnParent.size() + " vs import changed or new " + changedOrNew.size());

          // go one-by-one comparing them
          while (currentSentences.hasNext() && changedOrNewIter.hasNext()) {
            CommonExercise currentSentence = currentSentences.next();
            CommonExercise importSentence = changedOrNewIter.next();

            if (didChange(importSentence, currentSentence)) {   // how can this be false?
              logger.info("\tchanged for       " + currentSentence);
              logger.info("\tchanged import is " + importSentence);
              updateItems.add(getChanged(currentSentence, importSentence));
              rememberExID(importUpdateEx, importSentence, currentSentence.getID());
            }
          }

          {
            String changedField = "for " + currentParent.getEnglish() + "/" + currentParent.getForeignLanguage();

            // run to the end of imports -- rest of current are deletes.
            while (currentSentences.hasNext()) {
              CommonExercise currentSentence = currentSentences.next();
              toDelete.add(currentSentence.getID());
              updateItems.add(new DominoUpdateItem(currentSentence, changedField, DELETE).setParent(id));
              logger.info("\tgetNewAndChangedContextExercises delete " + currentSentence.getID() +
                  "\n\t ex: " + currentSentence);
            }

            // run to end of current - rest of import are new to add
            while (changedOrNewIter.hasNext()) {
              CommonExercise importSentence = changedOrNewIter.next();

              logger.info("\tgetNewAndChangedContextExercises add import " + importSentence);
              //  logger.info("getNewAndChangedContextExercises no known ex by " + npID + " for " + dominoID);
              newContextEx.add(importSentence);
              importSentence.getMutable().setParentExerciseID(id);
              updateItems.add(new DominoUpdateItem(importSentence, changedField, ADD).setParent(currentParent));
            }
          }
        }
      });


    }

    return updateItems;
  }

  private DominoUpdateItem getChanged(CommonExercise contextEx, CommonExercise updatedContext) {
    return new DominoUpdateItem(contextEx, new ArrayList<>(), CHANGE)
        .addChangedField(updatedContext.getDominoID() + " : " + updatedContext.getEnglish() + "/" + updatedContext.getForeignLanguage())
        .setParent(contextEx);
  }

  private void rememberExID(List<CommonExercise> importUpdateEx, CommonExercise contextEx, int id) {
    importUpdateEx.add(contextEx);
    contextEx.getMutable().setID(id);
  }

  /**
   * Cheesy - just want it for it's id not the whole object...
   *
   * @paramx dominoToNonContextEx
   * @paramx dominoID
   * @return
   * @paramx oldIDToExer
   * @paramx npID
   */
/*  @Nullable
  private SlickExercise getKnownSlickExercise(Map<Integer, SlickExercise> dominoToNonContextEx,
                                              //Map<String, SlickExercise> oldIDToExer,
                                              Integer dominoID
                                              //    ,
                                              //                                          String npID
  ) {
    SlickExercise currentKnownExercise = dominoToNonContextEx.get(dominoID);
    if (currentKnownExercise == null) {// && !npID.isEmpty() && !npID.equals(UNKNOWN)) {
      logger.info("\tgetKnownSlickExercise can't find ex by domino id " + dominoID);

      //currentKnownExercise = oldIDToExer.get(npID);
//      if (currentKnownExercise != null) {
//        logger.info("\tgetKnownSlickExercise found ex by netprof id " + npID);
//      } else {
//        logger.info("\tgetKnownSlickExercise can't found ex by netprof id '" + npID + "' in " + oldIDToExer.keySet().size() + " keys");
//      }
    }
    return currentKnownExercise;
  }*/
  private boolean didChange(CommonExercise importEx, CommonExercise exercise) {
    return
        !exercise.getForeignLanguage().equals(importEx.getForeignLanguage()) ||
            !exercise.getEnglish().equals(importEx.getEnglish()) ||
            !exercise.getMeaning().equals(importEx.getMeaning()) ||
            !exercise.getAltFL().equals(importEx.getAltFL());
  }

  /**
   * TODO : delete by np id
   *
   * @param importFromDomino
   * @param dominoToEx       domino id-> exercise
   * @see #addPending
   */
  private List<DominoUpdateItem> doDelete(ImportInfo importFromDomino,
                                          Map<Integer, SlickExercise> dominoToEx,
                                          boolean doChange,
                                          int projID) {
    Collection<Integer> deletedDominoIDs = importFromDomino.getDeletedDominoIDs();
    Collection<Integer> toDelete = new HashSet<>(deletedDominoIDs.size());

    List<DominoUpdateItem> deletes = new ArrayList<>();
    Set<Integer> missing = new TreeSet<>();

    deletedDominoIDs.forEach(dominoID -> {
      SlickExercise slickExercise = dominoToEx.get(dominoID);
      if (slickExercise == null) {
        logger.info("doDelete couldn't find domino dominoID " + dominoID + " in " + dominoToEx.keySet().size() + " keys.");
        missing.add(dominoID);
      } else {
        int exid = slickExercise.id();

        //  CommonExercise byExID = userExerciseDAO.getByExID(exid, false); // don't go to database - why would we?
        CommonExercise byExID = exerciseServices.getExercise(projID, exid);
        if (byExID == null) {
          logger.warn("doDelete : no ex by " + exid + " from domino #" + dominoID);
        } else {
          if (toDelete.add(byExID.getID())) {
            deletes.add(new DominoUpdateItem(byExID, new ArrayList<>(), DELETE));
          } else {
            logger.warn("doDelete huh? we already added exercise " + byExID.getID() + " to deleted list?");
          }
        }
      }
    });

/**
 * TODO : NO don't do this.
 */
    importFromDomino.getDeletedNPIDs().forEach(npExID -> {
      CommonExercise byExID = userExerciseDAO.getByExOldID(npExID, projID);
      if (byExID == null) {
        logger.warn("doDelete : no ex by old np id " + npExID);
      } else {
        boolean add = toDelete.add(byExID.getID());
        if (add) {
          deletes.add(new DominoUpdateItem(byExID, new ArrayList<>(), DELETE));
        }
      }
    });

    logger.info("doDelete :" +
        "\n\tDeleting " + toDelete.size() + " exercises," +
        "\n\tgiven " + deletedDominoIDs.size() + " deleted domino ids and" +
        "\n\t      " + importFromDomino.getDeletedNPIDs().size() + " np ids");

    if (!missing.isEmpty()) {
      logger.warn("doDelete : " + missing + " could not be deleted?");
    }

    if (doChange) {
      userExerciseDAO.deleteByExID(toDelete);
    }

    return deletes;
  }


  /**
   * @param lang
   * @return
   * @see mitll.langtest.server.services.ProjectServiceImpl#getDominoForLanguage
   */
  public List<DominoProject> getDominoForLanguage(String lang) {
    List<DominoProject> dominoProjects = new ArrayList<>();
    getForLanguage(lang, projectManagement.getVocabProjects())
        .forEach(importProjectInfo -> dominoProjects.add(getDominoProject(importProjectInfo)));
    return dominoProjects;
  }


  @NotNull
  private DominoProject getDominoProject(ImportProjectInfo importProjectInfo) {
    return new DominoProject(
        importProjectInfo.getDominoProjectID(),
        importProjectInfo.getName(),
        importProjectInfo.getUnitName(),
        importProjectInfo.getChapterName()
    );
  }

  @NotNull
  private List<ImportProjectInfo> getForLanguage(String lang, List<ImportProjectInfo> vocabProjects) {
    List<ImportProjectInfo> collect = getByLanguage(lang, vocabProjects);

    if (collect.isEmpty()) {
      String dominoName = getLanguage(lang).getDominoName();
      if (!dominoName.isEmpty()) {
        logger.debug("getForLanguage trying using domino language : " + dominoName);
        collect = getByLanguage(dominoName, vocabProjects);
        if (!collect.isEmpty()) {
          logger.info("getForLanguage found project using domino language : " + dominoName);
        }
      }
    }
    if (collect.isEmpty()) {
      logger.info("getForLanguage no projects in domino for language '" + lang + "'");
    }
    return collect;
  }

  private Language getLanguage(String lang) {
    Language language = Language.UNKNOWN;
    try {
      language = Language.valueOf(lang.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.error("unknown language " + lang);
    }
    return language;
  }

  private List<ImportProjectInfo> getByLanguage(String lang, List<ImportProjectInfo> vocabProjects) {
    return vocabProjects
        .stream()
        .filter(importProjectInfo -> importProjectInfo.getLanguage().toLowerCase().equals(lang.toLowerCase()))
        .collect(Collectors.toList());
  }

  /**
   * @param jsonDominoID
   * @param newEx
   * @param updateEx
   * @param project1
   * @param requestTime
   * @see #addPending
   */
  private void updateProjectIfSomethingChanged(int jsonDominoID,
                                               Collection<CommonExercise> newEx,
                                               Collection<CommonExercise> updateEx,
                                               SlickProject project1,
                                               long requestTime) {
    if (!newEx.isEmpty() || !updateEx.isEmpty()) {
      project1.updateDominoID(jsonDominoID);
    }

    project1.updateLastImport(requestTime);
    daoContainer.getProjectDAO().easyUpdate(project1);
//    logger.info("update modified time for project #" + project1.id());
  }

  @NotNull
  private List<Project> getProjectForDominoID(int jsonDominoID) {
    List<Project> existingBound = new ArrayList<>();
    projectServices.getProjects().forEach(project1 -> {
      if (project1.getProject().dominoid() == jsonDominoID) {
        existingBound.add(project1);
      }
    });
    return existingBound;
  }

  /**
   * Not sure how this can happen anymore...
   *
   * @param dominoid
   * @param jsonDominoID
   * @param existingBound
   * @return
   */
  @NotNull
  private DominoUpdateResponse getAnotherProjectResponse(int dominoid, int jsonDominoID, List<Project> existingBound) {
    SlickProject project = existingBound.iterator().next().getProject();
    String name = project.name();

    logger.info("getAnotherProjectResponse found existing (" + name + ") project " + project);

    DominoUpdateResponse dominoUpdateResponse = new DominoUpdateResponse(
        DominoUpdateResponse.UPLOAD_STATUS.ANOTHER_PROJECT,
        jsonDominoID, dominoid, new HashMap<>(), new ArrayList<>(), "");
    dominoUpdateResponse.setMessage(name);
    return dominoUpdateResponse;
  }

  @NotNull
  private Map<String, String> getProps(SlickProject project1, int numExercises) {
    DateFormat format = new SimpleDateFormat();
    Map<String, String> infoProps = new HashMap<>();
    infoProps.put(MODIFIED, format.format(project1.modified()));
    infoProps.put(NUM_ITEMS, "" + numExercises);
    return infoProps;
  }


  @NotNull
  private Map<Integer, CommonExercise> getDominoIDToExercise(Collection<CommonExercise> toImport) {
    Map<Integer, CommonExercise> dominoToEx = new HashMap<>();
    toImport.forEach(ex -> dominoToEx.put(ex.getDominoID(), ex));
    logger.info("getDominoIDToExercise importing " + toImport.size() + " vocab items");
    return dominoToEx;
  }

  /**
   * @param toImport
   * @return
   * @see #getNewAndChangedContextExercises
   */
  @NotNull
  private Map<String, CommonExercise> getDominoIDToContextExercise(Collection<CommonExercise> toImport, Map<CommonExercise, CommonExercise> childToParent) {
    Map<String, CommonExercise> dominoToEx = new HashMap<>();

    toImport.forEach(ex ->
        ex.getDirectlyRelated().forEach(contextSentence -> {
          int dominoID = contextSentence.getDominoID();
          if (dominoID > 0) {
            dominoToEx.put(dominoID + "_" + contextSentence.getDominoContextIndex(), contextSentence);
          } else {
            logger.info("getDominoIDToContextExercise : no domino ID for " + contextSentence);
          }

          childToParent.put(contextSentence, ex);
        })
    );
    logger.info("getDominoIDToContextExercise importing " + toImport.size() + " vocab items + context exercises...");

    return dominoToEx;
  }

  /**
   * Find source project(s) to copy audio from. Could be yourself.
   *
   * @param projectid
   * @param newEx
   * @param dominoToExID
   * @paramx exToInt
   * @see #addPending
   */
  private void copyAudio(int projectid, List<CommonExercise> newEx,
                         //Map<String, Integer> exToInt,
                         Map<Integer, Integer> dominoToExID) {
    try {
      List<Project> sourceProjects = getProjectsForSameLanguage(projectid);

      int nSourceProjects = sourceProjects.size();

      logger.info("copyAudio found " + nSourceProjects + " source projects for project " + projectid +
          //   "\n\tex->id     " + exToInt.size() +
          "\n\tdomino->ex " + dominoToExID.size());

      /**
       * Collect all the SlickAudio that needs to be copied for exercises and context exercises.
       */

      Collection<AudioMatches> copyAudioForEx = new ArrayList<>();
      Collection<AudioMatches> copyAudioForContext = new ArrayList<>();

      for (Project match : sourceProjects) {
        Map<String, List<SlickAudio>> transcriptToAudio = getTranscriptToAudio(match.getID());
        logger.info("copyAudio for " +
            "\n\tproject " + match.getID() + "/" + match.getProject().name() +
            "\n\tgot     " + transcriptToAudio.size() + " source candidates");
        getSlickAudios(projectid,
            newEx,
//            exToInt,
            dominoToExID, transcriptToAudio,

            copyAudioForEx,
            copyAudioForContext);
      }

      if (!sourceProjects.isEmpty()) {
        List<SlickAudio> copies = getSlickAudios(copyAudioForEx, copyAudioForContext);
        if (copies.isEmpty()) {
          logger.info("copyAudio - no audio copies for " + newEx.size() + " exercises...");
        } else {
          logger.info("copyAudio :" +
              "\n\tcopying      " + copyAudioForEx + "/" + copyAudioForContext +
              "\n\taudio        " + copies.size() +
              "\n\tfrom         " + newEx.size() +
              "\n\tfrom sources " + nSourceProjects +
              " projects, e.g.  " + sourceProjects.iterator().next().getProject().name());

          daoContainer.getAudioDAO().addBulk(copies);
        }
      }
    } catch (Exception e) {
      logger.info("Got " + e, e);
    }
  }

  /**
   * Get all the audio that needs to be copied
   *
   * @param copyAudioForEx
   * @param copyAudioForContext
   * @return
   */
  @NotNull
  private List<SlickAudio> getSlickAudios(Collection<AudioMatches> copyAudioForEx,
                                          Collection<AudioMatches> copyAudioForContext) {
    List<SlickAudio> copies = new ArrayList<>();
    copyAudioForEx.forEach(audioMatches -> audioMatches.deposit(copies));
    copyAudioForContext.forEach(audioMatches -> audioMatches.deposit(copies));
    return copies;
  }

  private static class AudioMatches {
    private SlickAudio mr = null;
    private SlickAudio ms = null;

    private SlickAudio fr = null;
    private SlickAudio fs = null;

    /**
     * @param candidate
     * @see #copyMatchingAudio
     */
    void add(SlickAudio candidate) {
      boolean regularSpeed = getAudioType(candidate).isRegularSpeed();
      //   int gender = candidate.gender();
//      logger.info("AudioMatches Examine candidate " + candidate);
//      logger.info("AudioMatches Examine regularSpeed " + regularSpeed + " " + audioType);
//      logger.info("AudioMatches Examine gender " + gender );
//      try {
//        audioType = AudioType.valueOf(candidate.audiotype());
//      } catch (IllegalArgumentException e) {
//        logger.error("Got " + e, e);
//      }
      int before = getCount();
      if (candidate.gender() == 0) {
        if (regularSpeed) {
          mr = mr == null ? candidate : mr.dnr() < candidate.dnr() ? candidate : mr;
        } else {
          ms = ms == null ? candidate : ms.dnr() < candidate.dnr() ? candidate : ms;
        }
      } else {
        if (regularSpeed) {
          fr = fr == null ? candidate : fr.dnr() < candidate.dnr() ? candidate : fr;
        } else {
          fs = fs == null ? candidate : fs.dnr() < candidate.dnr() ? candidate : fs;
        }
      }
      int after = getCount();
      if (after > before) {
        logger.info("AudioMatches now " + after + " added " + candidate);
      } else {
//        logger.info("AudioMatches not adding " + after+ " added " + candidate);
      }
    }

    /**
     * @param candidate
     * @return
     */
    @NotNull
    private AudioType getAudioType(SlickAudio candidate) {
      AudioType audioType = AudioType.UNSET;
      String rawAudioType = candidate.audiotype();
      try {
        if (rawAudioType.equals(AudioType.CONTEXT_REGULAR.toString())) {
          audioType = AudioType.CONTEXT_REGULAR;
        } else if (rawAudioType.equals(AudioType.CONTEXT_SLOW.toString())) {
          audioType = AudioType.CONTEXT_SLOW;
        } else {
          audioType = AudioType.valueOf(rawAudioType.toUpperCase());
        }
      } catch (IllegalArgumentException e) {
        logger.error("getAudioType : got unknown audio " + rawAudioType);
      }
      return audioType;
    }

 /*   public SlickAudio getMr() {
      return mr;
    }

    public SlickAudio getFr() {
      return fr;
    }

    public SlickAudio getMs() {
      return ms;
    }

    public SlickAudio getFs() {
      return fs;
    }*/

    int getCount() {
      int count = 0;
      if (mr != null) count++;
      if (fr != null) count++;
      if (ms != null) count++;
      if (fs != null) count++;
      return count;
    }

    public String toString() {
      return
          (mr == null ? "x" : "1") +
              (fr == null ? "x" : "2") +
              (ms == null ? "x" : "3") +
              (fs == null ? "x" : "4");
    }

    void deposit(List<SlickAudio> copies) {
      if (mr != null) copies.add(mr);
      if (fr != null) copies.add(fr);
      if (ms != null) copies.add(ms);
      if (fs != null) copies.add(fs);
    }
  }

  /**
   * transcript to lower case.
   *
   * @param maxID
   * @return map of transcript to audio with that transcript
   * @see #copyAudio
   */
  @NotNull
  private Map<String, List<SlickAudio>> getTranscriptToAudio(int maxID) {
    Map<String, List<SlickAudio>> transcriptToAudio = new HashMap<>();

    Collection<SlickAudio> audioAttributesByProjectThatHaveBeenChecked
        = maxID == -1 ? Collections.EMPTY_LIST : daoContainer.getAudioDAO().getAllNoExistsCheck(maxID);

    logger.info("getTranscriptToAudio found " + audioAttributesByProjectThatHaveBeenChecked.size() + " audio entries for " + maxID);
    for (SlickAudio audioAttribute : audioAttributesByProjectThatHaveBeenChecked) {
      List<SlickAudio> audioAttributes = transcriptToAudio.computeIfAbsent(audioAttribute.transcript().toLowerCase(), k -> new ArrayList<>());
      audioAttributes.add(audioAttribute);
    }
    return transcriptToAudio;
  }

  /**
   * OK to look for source audio from your own project...
   * Could be a copied entry...
   *
   * @param projectid
   * @return
   * @see #copyAudio
   */
  private List<Project> getProjectsForSameLanguage(int projectid) {
    String language = projectServices.getProject(projectid).getLanguage();

    // logger.info("getProjectsForSameLanguage look for " + language + " for " + projectid);

    return projectManagement
        .getProductionProjects()
        .stream()
        .filter(project ->
            project.getLanguage().equals(language)
        )
        .collect(Collectors.toList());
  }

  /**
   * @param projectid
   * @param newEx
   * @param dominoToExID
   * @param transcriptToAudio
   * @param transcriptToMatches        - output
   * @param transcriptToContextMatches
   * @paramx xoldIDToExID
   * @see #copyAudio
   */
  @NotNull
  private void getSlickAudios(int projectid,
                              List<CommonExercise> newEx,
                              //  Map<String, Integer> oldIDToExID,
                              Map<Integer, Integer> dominoToExID,
                              Map<String, List<SlickAudio>> transcriptToAudio,

                              Collection<AudioMatches> transcriptToMatches,
                              Collection<AudioMatches> transcriptToContextMatches) {
    logger.info("getSlickAudios" +
        "\n\tnewEx                      " + newEx.size() +
        //   "\n\toldIDToExID                " + oldIDToExID.size() +
        "\n\ttranscriptToAudio          " + transcriptToAudio.size()
    );

    MatchInfo vocab = new MatchInfo(0, 0);
    MatchInfo contextCounts = new MatchInfo(0, 0);

    for (CommonExercise ex : newEx) {
      Integer exid = ex.getID();

      if (exid == -1) {
        int dominoID = ex.getDominoID();
        exid = dominoToExID.get(dominoID);

        String oldID = ex.getOldID();
        if (exid == null && !oldID.equalsIgnoreCase(UNKNOWN)) {
          //  String oldID = oldID1;
          //   exid = oldIDToExID.get(oldID);
          logger.info("getSlickAudios exercise old " + oldID + " -> " + exid +
              "\n\teng " + ex.getEnglish() +
              "\n\tfl  " + ex.getForeignLanguage());
        } else {
          logger.info("getSlickAudios exercise dominoID " + dominoID + " -> ex id " + exid +
              "\n\teng " + ex.getEnglish() +
              "\n\tfl  " + ex.getForeignLanguage());
        }

        if (exid == null) {
          logger.error("getSlickAudios : huh? can't find domino ID " + dominoID + " in " + dominoToExID.size());
          // logger.error("getSlickAudios : huh? can't find old    ID " + oldID + " in " + oldIDToExID.size());
        }
      }

      if (exid == null || exid == -1) {
        logger.info("getSlickAudios : no exercise id found for domino ID " + ex.getDominoID() + " or old ID " + ex.getOldID());
      } else {
        boolean hasAudioAlready = daoContainer.getAudioDAO().hasAudio(exid);
        if (hasAudioAlready) {
          logger.info("getSlickAudios skipping " + exid + " since it already has audio");
        } else {
          vocab.add(addAudioForVocab(projectid, transcriptToAudio, transcriptToMatches, ex, exid));
        }
        contextCounts.add(addAudioForContext(projectid, //oldIDToExID,
            transcriptToAudio, transcriptToContextMatches, ex.getDirectlyRelated(), dominoToExID));
      }
    }
    logger.info("getSlickAudio  : vocab " + vocab + " contextCounts " + contextCounts);
  }

  /**
   * Only does match on fl, not on pair of fl/english... might be better.
   *
   * Matches case insensitive.
   *
   * @param projectid
   * @param transcriptToAudio
   * @param transcriptMatches
   * @param ex
   * @param exid
   * @return
   * @see #getSlickAudios
   */
  private MatchInfo addAudioForVocab(int projectid,
                                     Map<String, List<SlickAudio>> transcriptToAudio,
                                     Collection<AudioMatches> transcriptMatches,
                                     CommonShell ex,
                                     Integer exid) {
    int match = 0;
    int nomatch = 0;

    String fl = ex.getForeignLanguage().toLowerCase();
    List<SlickAudio> audioAttributes = transcriptToAudio.get(fl);

    logger.info("addAudioForVocab looking for match to ex " + exid + "/" + ex.getID() + " '" + ex.getEnglish() + "' = '" + fl + "'");
    if (audioAttributes != null) {
      transcriptMatches.add(copyMatchingAudio(projectid, exid, audioAttributes, false));
      match++;
    } else {
      logger.info("\taddAudioForVocab vocab no match " + ex.getEnglish() + " '" + fl + "'");
      nomatch++;
    }

    if (match == 0) {
      logger.info("\taddAudioForVocab vocab no match '" + ex.getEnglish() + "' = '" + fl + "' in " + transcriptToAudio.size() + " transcripts");
    }

    return new MatchInfo(match, nomatch);
  }

  /**
   * What about delete - we need to remove audio who transcripts no longer match.
   *
   * @param projectid
   * @param transcriptToAudio
   * @param transcriptToContextMatches
   * @param contextExercises
   * @param dominoToExID
   * @return
   * @paramx exToInt
   * @see #getSlickAudios
   */
  private MatchInfo addAudioForContext(int projectid,
                                       //   Map<String, Integer> exToInt,
                                       Map<String, List<SlickAudio>> transcriptToAudio,
                                       Collection<AudioMatches> transcriptToContextMatches,
                                       Collection<CommonExercise> contextExercises,
                                       Map<Integer, Integer> dominoToExID) {
    int match = 0;
    int nomatch = 0;
    for (CommonExercise context : contextExercises) {
      int cexid = context.getID();
      String prefix = cexid + "/" + context.getDominoID();
      if (context.getAudioAttributes().isEmpty()) {

        String cfl = context.getForeignLanguage().toLowerCase();
        List<SlickAudio> audioAttributes = transcriptToAudio.get(cfl);

        //   String coldID = context.getOldID();
        //  Integer cexid = exToInt.get(coldID);

        logger.info("getSlickAudios context " + prefix +
            //" old '" + coldID + "'" +
            " -> '" + cexid + "' matches = " + audioAttributes);
        if (audioAttributes != null && cexid != -1) {
          transcriptToContextMatches.add(copyMatchingAudio(projectid, cexid, audioAttributes, true));
          match++;
        } else {
          logger.info("getSlickAudios context " + prefix +
              "\n\tno match '" + context.getEnglish() + "'" +
              "\n\tfl       '" + cfl + "'" +
              "\n\tin        " + transcriptToAudio.size() + " possibilities");
          nomatch++;
        }
      } else {
        logger.info("getSlickAudios context " + prefix + " has audio already, so not adding audio to it.");
      }
    }
    return new MatchInfo(match, nomatch);
  }

  /**
   * For debug output...?
   */
  private static class MatchInfo {
    private int match;
    private int noMatch;

    MatchInfo(int match, int noMatch) {
      this.match = match;
      this.noMatch = noMatch;
    }

    public MatchInfo(MatchInfo matchInfo) {
      add(matchInfo);
    }

    void add(MatchInfo matchInfo) {
      this.match += matchInfo.match;
      this.noMatch += matchInfo.noMatch;
    }

    public String toString() {
      return match + "/" + noMatch;
    }
  }

  /**
   * Add to matches from input audio attributes
   *
   * @param projectid
   * @param exid
   * @param audioAttributes
   * @param isContext
   * @return SlickAudio collection of new SlickAudios to add to database.
   * @see #addAudioForVocab
   * @see #addAudioForContext
   */
  private AudioMatches copyMatchingAudio(int projectid,
                                         int exid,
                                         List<SlickAudio> audioAttributes,
                                         boolean isContext) {
    AudioMatches audioMatches = new AudioMatches();

    if (exid == -1)
      logger.error("copyMatchingAudio huh? exid -1 for project " + projectid + " " + audioAttributes.size());
    Timestamp modified = new Timestamp(System.currentTimeMillis());
    List<SlickAudio> audioToUse = audioAttributes;

    // if the audio match is from a vocab item, don't take slow speed audio, and then convert the type to context.
    if (isContext) {
      audioToUse = audioToUse
          .stream()
          .filter(slickAudio -> (
              slickAudio.audiotype().equalsIgnoreCase(AudioType.CONTEXT_REGULAR.toString()) ||
                  isRegular(slickAudio)
          ))
          .collect(Collectors.toList());
    }
    audioToUse.forEach(audio -> {
      String audiotype = audio.audiotype();
      if (isContext && isRegular(audio)) {
        audiotype = AudioType.CONTEXT_REGULAR.toString().toLowerCase();
      }
      SlickAudio audio1 = new SlickAudio(
          -1,
          audio.userid(),
          exid,
          modified,
          audio.audioref(),
          audiotype,
          audio.duration(),
          audio.defect(),
          audio.transcript(),
          projectid,
          audio.exists(),
          audio.lastcheck(),
          audio.actualpath(),
          audio.dnr(),
          audio.resultid(),
          audio.gender()
      );
      audioMatches.add(audio1);
    });

    return audioMatches;
  }

  private boolean isRegular(SlickAudio slickAudio) {
    return slickAudio.audiotype().equalsIgnoreCase(AudioType.REGULAR.toString());
  }

  /**
   * Worry a lot about making sure the exercise attributes are consistent -
   * <p>
   * removing stale ones, adding new ones, fixing join table.
   *
   * @param projectid
   * @param importUser
   * @param slickUEDAO
   * @param updateEx
   * @param typeOrder
   * @see #getDominoUpdateResponse
   */
  private void doUpdate(int projectid,
                        int importUser,
                        SlickUserExerciseDAO slickUEDAO,
                        List<CommonExercise> updateEx,
                        Collection<String> typeOrder,
                        Map<Integer, SlickExercise> legacyToEx
  ) {
    int failed = 0;

    Map<Integer, ExerciseAttribute> allByProject = slickUEDAO.getExerciseAttribute().getIDToPair(projectid);
    Map<ExerciseAttribute, Integer> attrToID = getAttributeToID(allByProject);

    Map<String, Map<String, ExerciseAttribute>> propToValueToAttr = populatePropToValue(allByProject.values());

    logger.info("doUpdate for project " + projectid +
        "\n\tupdate num " + updateEx.size() +
        "\n\tfound      " + allByProject.values().size() + " attributes" +
        "\n\tprops      " + propToValueToAttr.keySet() +
        "\n\tdoUpdate for project " + projectid + " values " + propToValueToAttr.values());

    Map<Integer, Collection<SlickExerciseAttributeJoin>> exToAttrs =
        slickUEDAO.getExerciseAttributeJoin().getAllJoinByProject(projectid);

    long now = System.currentTimeMillis();
    Timestamp modified = new Timestamp(now);

    List<SlickExerciseAttributeJoin> newJoins = new ArrayList<>();
    List<SlickExerciseAttributeJoin> removeJoins = new ArrayList<>();

    Iterator<String> iterator = typeOrder.iterator();
    String first = iterator.hasNext() ? iterator.next() : "";
    String second = iterator.hasNext() ? iterator.next() : "";

    for (CommonExercise toUpdate : updateEx) {
      int dominoID = toUpdate.getDominoID();
      if (DEBUG) logger.info("doUpdate exercise " + toUpdate + "\n\twith domino id " + dominoID);

      SlickExercise currentExercise = (toUpdate.isContext()) ? userExerciseDAO.getByID(toUpdate.getID()) : legacyToEx.get(dominoID);
      if (DEBUG) logger.info("doUpdate currentExercise " + currentExercise);

      boolean newImport = currentExercise == null;

      if (newImport) {
        logger.info("doUpdate no exercise " + " with domino id " + dominoID);
      }

      boolean changed = newImport || changed(currentExercise, toUpdate, first, second);
      if (!changed) {
        logger.info("doUpdate exercise #" + currentExercise.id() + " " +
            currentExercise.english() + "/" + currentExercise.foreignlanguage() +
            " has not changed");
      }

      if (changed && !slickUEDAO.update(toUpdate, false, typeOrder)) {
        logger.warn("\n\ndoUpdate update failed to update " + toUpdate);
        failed++;
      } else if (!slickUEDAO.updateModified(toUpdate.getID())) {
        logger.warn("\n\ndoUpdate update failed to update modified date on " + toUpdate);
      }

      // compare new attributes on toUpdate to existing attributes...
      int updateExerciseID = toUpdate.getID();
      Collection<SlickExerciseAttributeJoin> currentAttributeJoinsOnThisExercise = exToAttrs.get(updateExerciseID);

      if (DEBUG) {
        if (currentAttributeJoinsOnThisExercise != null) {
          currentAttributeJoinsOnThisExercise.forEach(slickExerciseAttributeJoin ->
              logger.debug("current for " + updateExerciseID + " = " + slickExerciseAttributeJoin));
        }
      }

      // import attributes are either known or unknown...
      // after reading in the exercise, it has a set of attributes, some of which may already be in the database...
      List<ExerciseAttribute> updateAttributes = toUpdate.getAttributes();
      logger.info("doUpdate exercise " + toUpdate.getID() + " has " + updateAttributes.size() + " attributes");

      List<ExerciseAttribute> newAttributes = getNewAttributes(propToValueToAttr, updateAttributes);

      if (!newAttributes.isEmpty()) {
        logger.info("doUpdate found " + newAttributes.size() + " new attributes for " + updateExerciseID + " given " + propToValueToAttr.size() + " known props.");
        // first, figure out which are new attributes (no join yet) and store them so we can join/reference to them
        // store and remember the new ones
        storeAndRememberAttributes(projectid, importUser, slickUEDAO, attrToID, now, newAttributes);
        rememberAttributes(newAttributes, propToValueToAttr);
      }

      // for join set, some are on there already, some need to be added, some need to be removed

      Set<Integer> attributeIDsOnExercise = updateAttributes
          .stream()
          .map(attrToID::get)
          .collect(Collectors.toCollection(HashSet::new));
      if (DEBUG)
        logger.info("doUpdate updateAttributes attr ids " + attributeIDsOnExercise + " for " + updateExerciseID);

      if (currentAttributeJoinsOnThisExercise == null) { // none on there yet, add all
        makeNewAttrJoins(importUser, modified, newJoins, updateExerciseID, attributeIDsOnExercise);
      } else {
        // figure out new set
        Set<Integer> currentSet = currentAttributeJoinsOnThisExercise
            .stream()
            .map(SlickExerciseAttributeJoin::attrid)
            .collect(Collectors.toCollection(HashSet::new));

        if (DEBUG) logger.info("doUpdate current attr ids " + currentSet + " for " + updateExerciseID);

        Set<Integer> newIDs = new HashSet<>(attributeIDsOnExercise);
        newIDs.removeAll(currentSet);

        if (!newIDs.isEmpty()) {
          logger.info("doUpdate Adding new ids " + newIDs + " to " + updateExerciseID);
          makeNewAttrJoins(importUser, modified, newJoins, updateExerciseID, newIDs);
        } else {
          if (DEBUG) logger.info("doUpdate no new attributes on " + updateExerciseID + " : still " + currentSet);
        }

        if (DEBUG) logger.info("doUpdate atrribute ids = " + currentSet);
        if (DEBUG) logger.info("doUpdate attributeIDsOnExercise " + attributeIDsOnExercise);
        currentSet.removeAll(attributeIDsOnExercise);

        if (!currentSet.isEmpty()) {
          logger.info("doUpdate after - items to remove " + currentSet);
          // only remove ones that stale - not on there...
          addRemoveJoins(removeJoins, currentAttributeJoinsOnThisExercise, currentSet);
        }
      }

      // so now we have known and new attributes.
      // compare new attributes to known...
    }

    logger.info("doUpdate now " + newJoins.size() + " attributes and remove " + removeJoins.size());
    slickUEDAO.getExerciseAttributeJoin().addBulkAttributeJoins(newJoins);
    slickUEDAO.getExerciseAttributeJoin().removeBulkAttributeJoins(removeJoins);
    if (failed > 0)
      logger.warn("\n\n\n\ndoUpdate somehow failed to update " + failed + " out of " + updateEx.size() + " exercises");
  }

  private Map<String, Map<String, ExerciseAttribute>> populatePropToValue(Collection<ExerciseAttribute> allKnownAttributes) {
    return rememberAttributes(allKnownAttributes, new HashMap<>());
  }

  private Map<String, Map<String, ExerciseAttribute>> rememberAttributes(Collection<ExerciseAttribute> allKnownAttributes,
                                                                         Map<String, Map<String, ExerciseAttribute>> propToValueToAttr) {
    allKnownAttributes.forEach(exerciseAttribute -> {
      Map<String, ExerciseAttribute> valueToAttr = propToValueToAttr.computeIfAbsent(getNormProp(exerciseAttribute), k -> new HashMap<>());
      valueToAttr.putIfAbsent(getNormValue(exerciseAttribute), exerciseAttribute);
    });
    return propToValueToAttr;
  }

  @NotNull
  private String getNormValue(ExerciseAttribute exerciseAttribute) {
    return exerciseAttribute.getValue().toLowerCase();
  }

  @NotNull
  private String getNormProp(ExerciseAttribute exerciseAttribute) {
    return exerciseAttribute.getProperty().toLowerCase().replaceAll("-", "");
  }

  private boolean changed(SlickExercise currentExercise, CommonExercise toUpdate, String first, String second) {
    String currentUnit = toUpdate.getUnitToValue().get(first);
    String currentChapter = toUpdate.getUnitToValue().get(second);

    return
        !currentExercise.english().equals(toUpdate.getEnglish()) ||
            !currentExercise.foreignlanguage().equals(toUpdate.getForeignLanguage()) ||
            !currentExercise.meaning().equals(toUpdate.getMeaning()) ||
            !currentExercise.altfl().equals(toUpdate.getAltFL()) ||
            !currentExercise.transliteration().equals(toUpdate.getTransliteration()) ||
            (currentUnit != null && !currentExercise.unit().equals(currentUnit)) ||
            (currentChapter != null && !currentExercise.lesson().equals(currentChapter))
        ;
  }

  private void addRemoveJoins(List<SlickExerciseAttributeJoin> removeJoins, Collection<SlickExerciseAttributeJoin> currentAttributesOnThisExercise, Set<Integer> currentSet) {
    for (SlickExerciseAttributeJoin current : currentAttributesOnThisExercise) {
      if (currentSet.contains(current.attrid())) {
        removeJoins.add(current);
        logger.info("doUpdate removing " + current);
      }
    }
  }

  /**
   * @param propToValueToAttr
   * @param updateAttributes
   * @return
   * @see #doUpdate
   */
  @NotNull
  private List<ExerciseAttribute> getNewAttributes(Map<String, Map<String, ExerciseAttribute>> propToValueToAttr, List<ExerciseAttribute> updateAttributes) {
    List<ExerciseAttribute> newAttributes = new ArrayList<>();
    for (ExerciseAttribute updateAttr : updateAttributes) {
      //logger.info("doUpdate examine " + updateExerciseID + " : " + updateAttr);
      Map<String, ExerciseAttribute> valueToAttr = propToValueToAttr.get(getNormProp(updateAttr));

      if (valueToAttr == null) {
        logger.info("getNewAttributes : attr " + updateAttr + " is new - prop " + getNormProp(updateAttr));
        newAttributes.add(updateAttr);
      } else {
        ExerciseAttribute knownAttr = valueToAttr.get(getNormValue(updateAttr));
        if (knownAttr == null) {
          logger.info("getNewAttributes : attr " + updateAttr + " is new - value " + getNormValue(updateAttr));
          newAttributes.add(updateAttr);
        }
      }
    }
    return newAttributes;
  }

  @NotNull
  private Map<ExerciseAttribute, Integer> getAttributeToID(Map<Integer, ExerciseAttribute> allByProject) {
    Map<ExerciseAttribute, Integer> attrToID = new HashMap<>();
    for (Map.Entry<Integer, ExerciseAttribute> pair : allByProject.entrySet()) {
      attrToID.put(pair.getValue(), pair.getKey());
    }
    return attrToID;
  }

  private void makeNewAttrJoins(int importUser,
                                Timestamp modified,
                                List<SlickExerciseAttributeJoin> newJoins,
                                int updateExerciseID,
                                Set<Integer> dbIDs) {
    dbIDs.forEach(dbID -> newJoins.add(new SlickExerciseAttributeJoin(-1, importUser, modified, updateExerciseID, dbID)));
  }

  private void storeAndRememberAttributes(int projectid,
                                          int importUser,
                                          SlickUserExerciseDAO slickUEDAO,
                                          Map<ExerciseAttribute, Integer> attrToID,
                                          long now,
                                          List<ExerciseAttribute> newAttributes) {
    for (ExerciseAttribute newAttr : newAttributes) {
      //  int i = slickUEDAO.addAttribute(projectid, now, importUser, newAttr);
      attrToID.put(newAttr, slickUEDAO.getExerciseAttribute().addAttribute(projectid, now, importUser, newAttr));
      //   logger.info("doUpdate remember new import attribute " + i + " = " + newAttr);
    }
  }
}
