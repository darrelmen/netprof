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
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.DominoUpdateResponse;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.exercise.MutableExercise;
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static mitll.langtest.server.database.project.ProjectManagement.MODIFIED;
import static mitll.langtest.server.database.project.ProjectManagement.NUM_ITEMS;
import static mitll.langtest.shared.exercise.DominoUpdateResponse.UPLOAD_STATUS.SUCCESS;

public class ProjectSync implements IProjectSync {
  private static final Logger logger = LogManager.getLogger(ProjectSync.class);
  public static final String ANY = "Any";

  public static final String ID = "_id";
  public static final String NAME = "name";
  public static final String CREATE_TIME = "createTime";
  private static final boolean DEBUG = false;
  static final String MONGO_TIME = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
  private static final long FIVE_YEARS = (5L * 365L * 24L * 60L * 60L * 1000L);

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
  public ProjectSync(ProjectServices projectServices, IProjectManagement projectManagement,
                     DAOContainer daoContainer, IUserExerciseDAO userExerciseDAO,
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
   * @see mitll.langtest.client.project.ProjectChoices#showImportDialog
   * @see mitll.langtest.server.services.ProjectServiceImpl#addPending
   */
  public DominoUpdateResponse addPending(int projectid, int importUser) {
    long requestTime = System.currentTimeMillis();
    Project project = projectServices.getProject(projectid);

    Timestamp modified = project.getProject().lastimport();

    String sinceInUTC = getModifiedTimestamp(project, modified);

    logger.info("addPending getting changes sinceInUTC last import " + new Date(modified.getTime()) + " = " + sinceInUTC);

    int dominoid = project.getProject().dominoid();
    ImportInfo importFromDomino = projectManagement.getImportFromDomino(projectid, dominoid, sinceInUTC);
    int jsonDominoID = importFromDomino.getDominoID();
    if (dominoid != -1 && dominoid != jsonDominoID) {
      logger.warn("addPending - json domino id = " + dominoid + " vs import project id " + jsonDominoID);
      return new DominoUpdateResponse(DominoUpdateResponse.UPLOAD_STATUS.WRONG_PROJECT, jsonDominoID, dominoid, new HashMap<>());
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

      Map<Integer, SlickExercise> dominoToNonContextEx = slickUEDAO.getLegacyToEx(projectid);
      List<CommonExercise> newEx = new ArrayList<>();
      List<CommonExercise> newContextEx = new ArrayList<>();
      List<CommonExercise> importUpdateEx = new ArrayList<>();

      Map<String, SlickExercise> oldIDToExer = getNewAndChangedExercises(projectid, importFromDomino, dominoToNonContextEx, newEx, importUpdateEx);

      {
        Collection<String> typeOrder2 = project.getTypeOrder();

        if (!newEx.isEmpty()) {
          // add new
          logger.info("addPending adding " + newEx.size() + " new non-context exercises");

          new ExerciseCopy().addExercises(
              importUser,
              projectid,
              new HashMap<>(),
              slickUEDAO,
              newEx,
              typeOrder2,
              new HashMap<>());
        }

        if (!newContextEx.isEmpty()) {
          logger.info("addPending adding " + newEx.size() + " new Context exercises");
          new ExerciseCopy().addContextExercises(importUser, projectid, slickUEDAO, newContextEx, typeOrder2);
        }

        if (!importUpdateEx.isEmpty()) {
          // now update...
          // update the exercises...
          logger.info("addPending updating  " + importUpdateEx.size() + " exercises");
          doUpdate(projectid, importUser, slickUEDAO, importUpdateEx, typeOrder2, dominoToNonContextEx, oldIDToExer);//, bringBack);
        }

        doDelete(importFromDomino, dominoToNonContextEx);

/*
          if (!deleteEx.isEmpty()) {
            logger.info("deleting " + deleteEx.size() + " exercises");
            db.getUserExerciseDAO().deleteByExID(deleteEx);
          }*/
      }

      Map<String, Integer> oldToNew = slickUEDAO.getOldToNew(projectid).getOldToNew();
      copyAudio(projectid, newEx, oldToNew);
      copyAudio(projectid, importUpdateEx, oldToNew);

      updateProjectIfSomethingChanged(jsonDominoID, newEx, importUpdateEx, project.getProject(), requestTime);

      // todo : shoudl we configure project if it didn't change?
      int i = projectManagement.configureProject(project, false, true);
      return new DominoUpdateResponse(SUCCESS, jsonDominoID, dominoid, getProps(project.getProject(), i));
    }
  }

  @NotNull
  private Map<String, SlickExercise> getNewAndChangedExercises(int projectid,
                                                               ImportInfo importFromDomino,
                                                               Map<Integer, SlickExercise> dominoToNonContextEx,
                                                               List<CommonExercise> newEx,
                                                               List<CommonExercise> importUpdateEx) {
    Map<String, SlickExercise> oldIDToExer = new HashMap<>();
    dominoToNonContextEx.values().forEach(slickExercise -> {
      oldIDToExer.put(slickExercise.exid(), slickExercise);
    });

    logger.info("addPending found " + dominoToNonContextEx.size() + " current exercises for project #" + projectid);

    {
      Set<Integer> currentIDs = dominoToNonContextEx.keySet();
      Set<String> oldIDs = oldIDToExer.keySet();

      Map<Integer, CommonExercise> dominoIDToChangedExercise = getDominoIDToExercise(importFromDomino.getChangedExercises());
      Map<Integer, CommonExercise> dominoIDToAddedExercise = getDominoIDToExercise(importFromDomino.getAddedExercises());

      logger.info("addPending importing " +
          dominoIDToAddedExercise.size() + " added, " +
          dominoIDToChangedExercise.size() + " changed exercises ");

      // three piles:
      // currentIDs exercises for the project that are not in the import should be deleted
      // import exercises not in the currentIDs set are new and need to be added
      // matching exercises need to be checked to see if they have changed

      dominoIDToChangedExercise.forEach((dominoID, importEx) -> {
        String npID = importEx.getOldID();

        logger.info("addPending import" +
            "\n\tcontext   " + importEx.isContext() +
            "\n\tdomino id " + dominoID +
            "\n\tnpID      '" + npID + "'" +
            "\n\teng       " + importEx.getEnglish() +
            "\n\tfl        " + importEx.getForeignLanguage()
        );
        // logger.info("addPending import importEx  '" + importEx.getEnglish() + "' = " + importEx.getForeignLanguage());

        // try to find it by domino or np id
        // np id for production projects.
        SlickExercise currentKnownExercise = getKnownSlickExercise(dominoToNonContextEx, oldIDToExer, dominoID, npID);

        if (currentKnownExercise == null) {
          logger.info("addPending found new CHANGED ex for domino id " + dominoID + " / " + npID + " import " + importEx.getEnglish() + " " + importEx.getForeignLanguage() + " context " + importEx.isContext());
          newEx.add(importEx);
        } else {
          int exID = currentKnownExercise.id();

          if (currentIDs.contains(dominoID) || oldIDs.contains(npID)) {
            importUpdateEx.add(importEx);
            MutableExercise mutable = importEx.getMutable();
            mutable.setID(exID);
            mutable.setOldID(npID);
          } else { // impossible?
            newEx.add(importEx);
            logger.warn("\n\n\n addPending 2 found new ex for domino id " + dominoID + " / " + npID + " import " + importEx.getEnglish() + " " + importEx.getForeignLanguage() + " context " + importEx.isContext());
          }
        }
      });


      dominoIDToAddedExercise.forEach((dominoID, importEx) -> {
        String npID = importEx.getOldID();

        logger.info("addPending import ADDED" +
            "\n\tcontext   " + importEx.isContext() +
            "\n\tdomino id " + dominoID +
            "\n\tnpID      '" + npID + "'" +
            "\n\teng       " + importEx.getEnglish() +
            "\n\tfl        " + importEx.getForeignLanguage()
        );
        // logger.info("addPending import importEx  '" + importEx.getEnglish() + "' = " + importEx.getForeignLanguage());

        // try to find it by domino or np id
        // np id for production projects.
        SlickExercise currentKnownExercise = getKnownSlickExercise(dominoToNonContextEx, oldIDToExer, dominoID, npID);

        if (currentKnownExercise == null) {
          logger.info("addPending found new ADDED ex for domino id " + dominoID + " / " + npID + " import " + importEx.getEnglish() + " " + importEx.getForeignLanguage() + " context " + importEx.isContext());
          newEx.add(importEx);
        } else {
          logger.warn("addPending huh? already know about " + currentKnownExercise);
        }
      });

    }
    return oldIDToExer;
  }

  /**
   * TODO : change handling for context exercises
   *
   * @param dominoToNonContextEx
   * @param oldIDToExer
   * @param dominoID
   * @param npID
   * @return
   */
/*  @NotNull
  private void getNewAndChangedContextExercises(int projectid,
                                                               ImportInfo importFromDomino,
                                                                      Map<String, SlickExercise> oldIDToExer,
                                                               List<CommonExercise> newContextEx,
                                                               List<CommonExercise> importUpdateEx) {
   // Map<Integer, SlickExercise> dominoToNonContextEx = slickUEDAO.getLegacyToEx(projectid);

//    Map<String, SlickExercise> oldIDToExer = new HashMap<>();
//    dominoToNonContextEx.values().forEach(slickExercise -> {
//      oldIDToExer.put(slickExercise.exid(), slickExercise);
//    });

    //logger.info("addPending found " + dominoToNonContextEx.size() + " current exercises for " + projectid);

    {
//      Set<Integer> currentIDs = dominoToNonContextEx.keySet();
      Set<String> oldIDs = oldIDToExer.keySet();

      Map<String, CommonExercise> dominoIDToContextExercise = getDominoIDToContextExercise(importFromDomino.getChangedExercises());

      logger.info("addPending importing " + dominoIDToContextExercise.size() + " context ");

      // three piles:
      // currentIDs exercises for the project that are not in the import should be deleted
      // import exercises not in the currentIDs set are new and need to be added
      // matching exercises need to be checked to see if they have changed
      // * but previously deleted exercises can be brought back

      dominoIDToContextExercise.forEach((dominoID, importEx) -> {
        String npID = importEx.getOldID();

        logger.info("addPending import" +
            "\n\tcontext   " + importEx.isContext() +
            "\n\tdomino id " + dominoID +
            "\n\tnpID      '" + npID + "'" +
            "\n\teng       " + importEx.getEnglish() +
            "\n\tfl        " + importEx.getForeignLanguage()
        );
        // logger.info("addPending import importEx  '" + importEx.getEnglish() + "' = " + importEx.getForeignLanguage());

        // try to find it by domino or np id
        // np id for production projects.
        SlickExercise currentKnownExercise = getKnownSlickExercise(dominoIDToContextExercise, oldIDToExer, dominoID, npID);

        if (currentKnownExercise != null) {
          int exID = currentKnownExercise.id();

          if (currentIDs.contains(dominoID) || oldIDs.contains(npID)) {
            importUpdateEx.add(importEx);
            MutableExercise mutable = importEx.getMutable();
            mutable.setID(exID);
            mutable.setOldID(npID);
          } else { // impossible?
            newEx.add(importEx);
            logger.warn("\n\n\n addPending 2 found new ex for domino id " + dominoID + " / " + npID + " import " + importEx.getEnglish() + " " + importEx.getForeignLanguage() + " context " + importEx.isContext());
          }
        } else {
          logger.info("addPending found new ex for domino id " + dominoID + " / " + npID + " import " + importEx.getEnglish() + " " + importEx.getForeignLanguage() + " context " + importEx.isContext());
          if (importEx.isContext()) {
            matchContext(projectid, newContextEx, importUpdateEx, dominoToNonContextEx, dominoID, importEx);
          } else {
            newEx.add(importEx);
          }
        }
      });
    }
    return oldIDToExer;
  }*/
  @Nullable
  private SlickExercise getKnownSlickExercise(Map<Integer, SlickExercise> dominoToNonContextEx,
                                              Map<String, SlickExercise> oldIDToExer,
                                              Integer dominoID,
                                              String npID) {
    SlickExercise currentKnownExercise = dominoToNonContextEx.get(dominoID);
    if (currentKnownExercise == null && !npID.isEmpty()) {
      logger.info("\taddPending can't find ex by domino id " + dominoID);

      currentKnownExercise = oldIDToExer.get(npID);
      if (currentKnownExercise != null) {
        logger.info("\taddPending found ex by netprof id " + npID);
      }
    }
    return currentKnownExercise;
  }

  private void matchContext(int projectid, List<CommonExercise> newContextEx, List<CommonExercise> importUpdateEx, Map<Integer, SlickExercise> dominoToEx, Integer dominoID, CommonExercise importEx) {
    SlickExercise knownParent = dominoToEx.get(importEx.getParentDominoID());
    if (knownParent != null) {
      int parentID = knownParent.id();
      logger.info("\tmatchContext found parent " + parentID + " before parent = " + importEx.getParentExerciseID());
      importEx.getMutable().setParentExerciseID(parentID);

      // so either it's a known context sentence or it isn't
      // #1 context sentence

      // we may already know the domino id, if so find it by that.

      CommonExercise parent = exerciseServices.getExercise(projectid, parentID);
      Map<Integer, CommonExercise> dominoToContextEx = new HashMap<>();
      List<CommonExercise> directlyRelated = parent.getDirectlyRelated();

      directlyRelated.forEach(exercise -> {
        if (exercise.getDominoID() > 0) {
          dominoToContextEx.put(exercise.getDominoID(), exercise);
        }
      });

      int dominoID1 = importEx.getDominoID();
      CommonExercise exercise = dominoToContextEx.get(dominoID);
      if (exercise != null) { // hey found it
        logger.info("matchContext found by " + dominoID1);
        if (didChange(importEx, exercise)) {
          logger.info("\tmatchContext 1 changed : " + importEx.getEnglish() + " " + importEx.getForeignLanguage());
          importUpdateEx.add(importEx);
        }
      } else {  // OK, we don't have it by domino id
        int index = dominoID1 % 10;
        logger.info("matchContext looking for # " + index);

        if (index < directlyRelated.size()) {
          CommonExercise parentSentence = directlyRelated.get(index);
          if (didChange(importEx, parentSentence)) {
            logger.info("\tmatchContext 2 changed : " + importEx.getEnglish() + " " + importEx.getForeignLanguage());
            importUpdateEx.add(importEx);
          }
        } else {
          // must be new
          logger.info("\tmatchContext 3 new context : " + importEx.getEnglish() + " " + importEx.getForeignLanguage());
          newContextEx.add(importEx);
        }
      }
    } else {
      logger.error("\tmatchContext can't find parent " + importEx.getParentDominoID());
    }
  }

  private boolean didChange(CommonExercise importEx, CommonExercise exercise) {
    return
        !exercise.getForeignLanguage().equals(importEx.getForeignLanguage()) ||
            !exercise.getEnglish().equals(importEx.getEnglish()) ||
            !exercise.getAltFL().equals(importEx.getAltFL());
  }

  /**
   * TODO : delete by np id
   *
   * @param importFromDomino
   * @param dominoToEx
   * @see #addPending
   */
  private void doDelete(ImportInfo importFromDomino, Map<Integer, SlickExercise> dominoToEx) {
    Collection<Integer> deletedDominoIDs = importFromDomino.getDeletedDominoIDs();
    List<Integer> toDelete = new ArrayList<>(deletedDominoIDs.size());

    Set<Integer> missing = new TreeSet<>();
    deletedDominoIDs.forEach(id -> {
      SlickExercise slickExercise = dominoToEx.get(id);
      if (slickExercise == null) {
        missing.add(id);
      } else {
        int exid = slickExercise.id();
        toDelete.add(exid);

        CommonExercise byExID = userExerciseDAO.getByExID(exid);
        if (byExID == null) {
          logger.warn("doDelete :  no ex by " + exid);
        } else {
          toDelete.add(byExID.getID());
        }
      }
    });


    logger.info("doDelete : Deleting " + toDelete.size() + " exercises, given " + deletedDominoIDs.size());

    if (!missing.isEmpty()) {
      logger.warn("doDelete : " + missing + " could not be deleted?");
    }
    userExerciseDAO.deleteByExID(toDelete);
  }

  /**
   * If the project is empty, go to the beginning of time, more or less.
   * So if you make a project in domino, then in netprof, will pick up everything in domino.
   *
   * @param project
   * @param modified
   * @return
   */
  @NotNull
  private String getModifiedTimestamp(Project project, Timestamp modified) {
    ZonedDateTime zdt;

    if (project.getRawExercises().isEmpty()) {
      Date fiveYearsAgo = new Date(System.currentTimeMillis() - FIVE_YEARS);
      //  logger.info("Start from " + fiveYearsAgo);
      zdt = ZonedDateTime.ofInstant(fiveYearsAgo.toInstant(), ZoneId.of("UTC"));
    } else {
      zdt = ZonedDateTime.ofInstant(modified.toInstant(), ZoneId.of("UTC"));
    }
    return zdt.format(DateTimeFormatter.ofPattern(MONGO_TIME));
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
      Language language = getLanguage(lang);
      String dominoName = language.getDominoName();
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

  @NotNull
  private DominoUpdateResponse getAnotherProjectResponse(int dominoid, int jsonDominoID, List<Project> existingBound) {
    SlickProject project = existingBound.iterator().next().getProject();
    String name = project.name();

    logger.info("getAnotherProjectResponse found existing (" + name + ") project " + project);

    DominoUpdateResponse dominoUpdateResponse = new DominoUpdateResponse(DominoUpdateResponse.UPLOAD_STATUS.ANOTHER_PROJECT, jsonDominoID, dominoid, new HashMap<>());
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

/*
    toImport.forEach(ex ->
        ex.getDirectlyRelated().forEach(commonExercise -> {
          int dominoID = commonExercise.getDominoID();
          if (dominoID > 0) {
            dominoToEx.put(dominoID, commonExercise);
          } else {
            logger.error("getDominoIDToExercise : huh? no domino ID for " + commonExercise);
          }
        })
    );
    logger.info("getDominoIDToExercise importing " + toImport.size() + " vocab items + context exercises...");
*/

    return dominoToEx;
  }

  @NotNull
  private Map<String, CommonExercise> getDominoIDToContextExercise(Collection<CommonExercise> toImport) {
    Map<String, CommonExercise> dominoToEx = new HashMap<>();

    toImport.forEach(ex ->
        ex.getDirectlyRelated().forEach(commonExercise -> {
          int dominoID = commonExercise.getDominoID();
          if (dominoID > 0) {
            dominoToEx.put(dominoID + "_" + commonExercise.getDominoContextIndex(), commonExercise);
          } else {
            logger.error("getDominoIDToContextExercise : huh? no domino ID for " + commonExercise);
          }
        })
    );
    logger.info("getDominoIDToContextExercise importing " + toImport.size() + " vocab items + context exercises...");

    return dominoToEx;
  }

  /**
   * @param projectid
   * @param newEx
   * @param exToInt
   * @see #addPending
   */
  private void copyAudio(int projectid, List<CommonExercise> newEx, Map<String, Integer> exToInt) {
    try {
      List<Project> matches = getProjectsForSameLanguage(projectid);

      logger.info("copyAudio found " + matches.size() + " source projects for project " + projectid + " : ex->id " + exToInt.size());
      Collection<AudioMatches> copyAudioForEx = new ArrayList<>();
      Collection<AudioMatches> copyAudioForContext = new ArrayList<>();

      for (Project match : matches) {
        Map<String, List<SlickAudio>> transcriptToAudio = getTranscriptToAudio(match.getID());
        logger.info("copyAudio for project " + match.getID() + "/" + match.getProject().name() +
            " got " + transcriptToAudio.size() + " source candidates");
        getSlickAudios(projectid,
            newEx,
            exToInt,
            transcriptToAudio,
            copyAudioForEx,
            copyAudioForContext);
      }

      if (!matches.isEmpty()) {
        List<SlickAudio> copies = getSlickAudios(copyAudioForEx, copyAudioForContext);
        logger.info("copyAudio :" +
            "\n\tcopying " + copyAudioForEx + "/" + copyAudioForContext +
            "audio " + copies.size() +
            "\n\tfrom " + newEx.size() +
            "\n\tfrom " + matches.size() +
            " projects, e.g. " + matches.iterator().next().getProject().name());

        daoContainer.getAudioDAO().addBulk(copies);
      }

    } catch (Exception e) {
      logger.info("Got " + e, e);
    }
  }

  @NotNull
  private List<SlickAudio> getSlickAudios(Collection<AudioMatches> copyAudioForEx,
                                          Collection<AudioMatches> copyAudioForContext) {
    List<SlickAudio> copies = new ArrayList<>();

    // Collection<AudioMatches> copyAudioForEx = transcriptToAudioMatch.values();
    for (AudioMatches m : copyAudioForEx) {
//          logger.info("copyAudio got transcript match " + m);
      m.deposit(copies);
    }
    // Collection<AudioMatches> copyAudioForContext = transcriptToContextAudioMatch.values();
    for (AudioMatches m : copyAudioForContext) {
      logger.info("copyAudio got context match " + m);
      m.deposit(copies);
    }
    return copies;
  }

  private static class AudioMatches {
    private SlickAudio mr = null;
    private SlickAudio ms = null;

    private SlickAudio fr = null;
    private SlickAudio fs = null;

    /**
     * @param candidate
     * @see #copyMatchingAudio(int, int, List, AudioMatches)
     */
    void add(SlickAudio candidate) {
      //   int gender = candidate.gender();
      boolean regularSpeed = getAudioType(candidate).isRegularSpeed();
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

  @NotNull
  private static AudioType getAudioType(SlickAudio candidate) {
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

  /**
   * @param maxID
   * @return
   * @see #copyAudio(int, List, Map)
   */
  @NotNull
  private Map<String, List<SlickAudio>> getTranscriptToAudio(int maxID) {
    Map<String, List<SlickAudio>> transcriptToAudio = new HashMap<>();

    Collection<SlickAudio> audioAttributesByProjectThatHaveBeenChecked
        = maxID == -1 ? Collections.EMPTY_LIST : daoContainer.getAudioDAO().getAllNoExistsCheck(maxID);

    logger.info("getTranscriptToAudio found " + audioAttributesByProjectThatHaveBeenChecked.size() + " audio entries for " + maxID);
    for (SlickAudio audioAttribute : audioAttributesByProjectThatHaveBeenChecked) {
      List<SlickAudio> audioAttributes = transcriptToAudio.computeIfAbsent(audioAttribute.transcript(), k -> new ArrayList<>());
      audioAttributes.add(audioAttribute);
    }
    return transcriptToAudio;
  }

  /**
   * @param projectid
   * @return
   * @see #copyAudio(int, List, Map)
   */
  private List<Project> getProjectsForSameLanguage(int projectid) {
    String language = projectServices.getProject(projectid).getLanguage();

    logger.info("getProjectsForSameLanguage look for " + language + " for " + projectid);

    return projectManagement
        .getProductionProjects()
        .stream()
        .filter(project ->
            project.getLanguage().equals(language) &&
                project.getID() != projectid).collect(Collectors.toList());
  }

  /**
   * @param projectid
   * @param newEx
   * @param exToInt
   * @param transcriptToAudio
   * @param transcriptToMatches
   * @param transcriptToContextMatches
   * @see #copyAudio
   */
  @NotNull
  private void getSlickAudios(int projectid,
                              List<CommonExercise> newEx,
                              Map<String, Integer> exToInt,
                              Map<String, List<SlickAudio>> transcriptToAudio,
                              Collection<AudioMatches> transcriptToMatches,
                              Collection<AudioMatches> transcriptToContextMatches) {
//    int match = 0;
//    int nomatch = 0;
    logger.info("getSlickAudios exToInt                    " + exToInt.size());
    logger.info("getSlickAudios transcriptToAudio          " + transcriptToAudio.size());
    logger.info("getSlickAudios transcriptToMatches        " + transcriptToMatches.size());
    logger.info("getSlickAudios transcriptToContextMatches " + transcriptToContextMatches.size());

    MatchInfo vocab = new MatchInfo(0, 0);
    MatchInfo contextCounts = new MatchInfo(0, 0);

    for (CommonExercise ex : newEx) {
      String oldID = ex.getOldID();
      Integer exid = exToInt.get(oldID);
      logger.info("getSlickAudios exercise old " + oldID + " -> " + exid + " " + ex.getEnglish() + " " + ex.getForeignLanguage());

      if (exid == null) {
        logger.error("getSlickAudios : huh? can't find " + oldID + " in " + exToInt.size());
      } else {
        boolean hasAudioAlready = daoContainer.getAudioDAO().hasAudio(exid);

        //        if (exercise != null) {
//          logger.info("getSlickAudios found known ex " + exercise.getID() + " with " + exercise.getAudioAttributes().size() + " audio attributes");
//          hasAudioAlready = !exercise.getAudioAttributes().isEmpty();
//        } else {
//          logger.info("getSlickAudios no known ex " + exid);
//        }

        if (hasAudioAlready) {
          logger.info("getSlickAudios skipping " + ex.getID() + " since it already has audio");
        } else {
          vocab.add(addAudioForVocab(projectid, transcriptToAudio, transcriptToMatches, ex, exid));
        }
        contextCounts.add(addAudioForContext(projectid, exToInt, transcriptToAudio, transcriptToContextMatches, ex.getDirectlyRelated()));
      }
    }
    logger.info("getSlickAudio  : vocab " + vocab + " contextCounts " + contextCounts);
  }

  /**
   * Only does match on fl, not on pair of fl/english... might be better.
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
                                     CommonExercise ex,
                                     Integer exid) {
    int match = 0;
    int nomatch = 0;
    String fl = ex.getForeignLanguage();

    logger.info("addAudioForVocab looking for match to ex " + exid + "/" + ex.getID() + " '" + ex.getEnglish() + "' = '" + fl + "'");

    List<SlickAudio> audioAttributes = transcriptToAudio.get(fl);
    if (audioAttributes != null) {
      AudioMatches audioMatches = new AudioMatches();//transcriptToMatches.computeIfAbsent(fl, k -> new AudioMatches());
      copyMatchingAudio(projectid, exid, audioAttributes, audioMatches);
      transcriptMatches.add(audioMatches);
      match++;
    } else {
      logger.info("addAudioForVocab vocab no match " + ex.getEnglish() + " '" + fl + "'");
      nomatch++;
    }

    if (match == 0) {
      logger.info("addAudioForVocab vocab no match '" + ex.getEnglish() + "' = '" + fl + "' in " + transcriptToAudio.size() + " transcripts");
    }

    return new MatchInfo(match, nomatch);
  }

  /**
   * What about delete - we need to remove audio who transcripts no longer match.
   *
   * @param projectid
   * @param exToInt
   * @param transcriptToAudio
   * @param transcriptToContextMatches
   * @param contextExercises
   * @return
   */
  private MatchInfo addAudioForContext(int projectid,
                                       Map<String, Integer> exToInt,
                                       Map<String, List<SlickAudio>> transcriptToAudio,
                                       Collection<AudioMatches> transcriptToContextMatches,
                                       Collection<CommonExercise> contextExercises) {
    int match = 0;
    int nomatch = 0;
    for (CommonExercise context : contextExercises) {
      if (context.getAudioAttributes().isEmpty()) {
        String prefix = context.getID() + "/" + context.getDominoID();

        String cfl = context.getForeignLanguage();
        List<SlickAudio> audioAttributes = transcriptToAudio.get(cfl);

        String coldID = context.getOldID();
        Integer cexid = exToInt.get(coldID);

        logger.info("getSlickAudios context " + prefix + " old '" + coldID + "' -> '" + cexid + "'");
        if (audioAttributes != null && cexid != null) {
          AudioMatches audioMatches = new AudioMatches();
          copyMatchingAudio(projectid, cexid, audioAttributes, audioMatches);
          transcriptToContextMatches.add(audioMatches);
          match++;
        } else {
          logger.info("getSlickAudios context " + prefix +
              "  no match '" + context.getEnglish() + "' = '" + cfl + "'");
          nomatch++;
        }
      }
    }
    return new MatchInfo(match, nomatch);
  }

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

/*    public int getMatch() {
      return match;
    }
    public int getNoMatch() {
      return noMatch;
    }*/

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
   * @param matches
   * @see #addAudioForVocab
   */
  private void copyMatchingAudio(int projectid,
                                 int exid,
                                 List<SlickAudio> audioAttributes,

                                 AudioMatches matches) {
    if (exid == -1)
      logger.error("copyMatchingAudio huh? exid -1 for project " + projectid + " " + audioAttributes.size());
    for (SlickAudio audio : audioAttributes) {
      SlickAudio audio1 = new SlickAudio(
          -1,
          audio.userid(),
          exid,
          audio.modified(),
          audio.audioref(),
          audio.audiotype(),
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
      matches.add(audio1);
    }
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
   * @param typeOrder2
   * @see #addPending
   */
  private void doUpdate(int projectid,
                        int importUser,
                        SlickUserExerciseDAO slickUEDAO,
                        List<CommonExercise> updateEx,
                        Collection<String> typeOrder2,
                        Map<Integer, SlickExercise> legacyToEx,
                        Map<String, SlickExercise> oldIDToExer
                        //    ,
                        //                    Set<CommonExercise> bringBack
  ) {
    int failed = 0;

    Map<Integer, ExerciseAttribute> allByProject = slickUEDAO.getIDToPair(projectid);
    Map<ExerciseAttribute, Integer> attrToID = getAttributeToID(allByProject);

    Collection<ExerciseAttribute> allKnownAttributes = new HashSet<>(allByProject.values());

    logger.info("doUpdate for project " + projectid + " found " + allKnownAttributes.size() + " attributes");

    allKnownAttributes.forEach(exerciseAttribute -> logger.debug("\t " + exerciseAttribute));

    Map<Integer, Collection<SlickExerciseAttributeJoin>> exToAttrs = slickUEDAO.getAllJoinByProject(projectid);

    long now = System.currentTimeMillis();
    Timestamp modified = new Timestamp(now);

    List<SlickExerciseAttributeJoin> newJoins = new ArrayList<>();
    List<SlickExerciseAttributeJoin> removeJoins = new ArrayList<>();

    Iterator<String> iterator = typeOrder2.iterator();
    String first = iterator.hasNext() ? iterator.next() : "";
    String second = iterator.hasNext() ? iterator.next() : "";

    for (CommonExercise toUpdate : updateEx) {
      SlickExercise currentExercise = legacyToEx.get(toUpdate.getDominoID());

      boolean newImport = currentExercise == null;
      if (newImport) {
        currentExercise = oldIDToExer.get(toUpdate.getOldID());
        logger.info("Exercise #" + currentExercise.id() + " with domino id " + toUpdate.getDominoID() + " : '" + currentExercise.english() + "' is a new import!");
      }

      boolean changed = newImport || changed(currentExercise, toUpdate, first, second);// || bringBack.contains(toUpdate);
      if (!changed) {
        logger.info("Exercise #" + currentExercise.id() + " " + currentExercise.english() + " has not changed");
      }

      if (changed &&
          !slickUEDAO.update(toUpdate, false, typeOrder2)) {
        logger.warn("doUpdate update failed to update " + toUpdate);
        failed++;
      }

      // compare new attributes on toUpdate to existing attributes...
      int updateExerciseID = toUpdate.getID();
      Collection<SlickExerciseAttributeJoin> currentAttributeJoinsOnThisExercise = exToAttrs.get(updateExerciseID);

      if (DEBUG) {
        if (currentAttributeJoinsOnThisExercise != null) {
          currentAttributeJoinsOnThisExercise.forEach(slickExerciseAttributeJoin -> logger.debug("current for " + updateExerciseID + " = " + slickExerciseAttributeJoin));
        }
      }

      // import attributes are either known or unknown...
      // after reading in the exercise, it has a set of attributes, some of which may already be in the database...
      List<ExerciseAttribute> updateAttributes = toUpdate.getAttributes();
      logger.info("doUpdate exercise " + toUpdate.getID() + " has " + updateAttributes.size() + " attributes");

      List<ExerciseAttribute> newAttributes = getNewAttributes(allKnownAttributes, updateAttributes);

      if (!newAttributes.isEmpty()) {
        logger.info("doUpdate found " + newAttributes.size() + " new attributes for " + updateExerciseID + " given " + allKnownAttributes.size() + " known.");
      }
      // first, figure out which are new attributes (no join yet) and store them so we can join/reference to them
      // store and remember the new ones
      storeAndRememberAttributes(projectid, importUser, slickUEDAO, attrToID, now, newAttributes);
      allKnownAttributes.addAll(newAttributes);

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
    slickUEDAO.addBulkAttributeJoins(newJoins);
    slickUEDAO.removeBulkAttributeJoins(removeJoins);
    if (failed > 0)
      logger.warn("\n\n\n\ndoUpdate somehow failed to update " + failed + " out of " + updateEx.size() + " exercises");
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

  @NotNull
  private List<ExerciseAttribute> getNewAttributes(Collection<ExerciseAttribute> allKnownAttributes,
                                                   //         int updateExerciseID,
                                                   List<ExerciseAttribute> updateAttributes) {
    List<ExerciseAttribute> newAttributes = new ArrayList<>();
    for (ExerciseAttribute updateAttr : updateAttributes) {
      //logger.info("doUpdate examine " + updateExerciseID + " : " + updateAttr);
      if (allKnownAttributes.contains(updateAttr)) {
        //  knownAttributes.add(updateAttr);
      } else {
        newAttributes.add(updateAttr);
        logger.info("attr " + updateAttr + " is new");
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
      int i = slickUEDAO.addAttribute(projectid, now, importUser, newAttr);
      attrToID.put(newAttr, i);
      //   logger.info("doUpdate remember new import attribute " + i + " = " + newAttr);
    }
  }
}
