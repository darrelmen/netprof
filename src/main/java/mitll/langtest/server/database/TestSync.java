package mitll.langtest.server.database;

import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.domino.ImportInfo;
import mitll.langtest.server.domino.ImportProjectInfo;
import mitll.langtest.server.domino.ProjectSync;
import mitll.langtest.shared.exercise.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static mitll.langtest.shared.exercise.DominoUpdateItem.ITEM_STATUS.*;

public class TestSync {
  private static final Logger logger = LogManager.getLogger(TestSync.class);

  final DatabaseImpl db;

  public TestSync(DatabaseImpl db) {
    this.db = db;
    try {
      syncTests();

      testSyncContextChange();
      stopNow();

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private void stopNow() {
    db.close();
    System.exit(0);
  }

  private void syncTests() {
    int projectid = 16;

    int importUser = db.getUserDAO().getImportUser();
    DominoUpdateResponse dominoUpdateResponse = getProjectSync()
        .addPending(projectid, importUser, false);

    logger.info("--- Got " + dominoUpdateResponse);
    dominoUpdateResponse.getUpdates().forEach(logger::info);

    // logger.info("Got " + dominoUpdateResponse);

    Project project = getProject(projectid);
    Iterator<String> iterator = project.getTypeOrder().iterator();
    String unit = iterator.next();
    String chapter = iterator.next();
    ImportProjectInfo importProjectInfo = new ImportProjectInfo(489, importUser,
        unit, chapter, new Date().getTime());

    {
      ImportInfo importFromDomino2 = new ImportInfo(importProjectInfo,
          new ArrayList<>(), new ArrayList<>(),
          new ArrayList<>(),
          new HashSet<>());

      DominoUpdateResponse dominoUpdateResponse2 = getProjectSync().getDominoUpdateResponse(projectid, importUser, false, importFromDomino2);

      logger.info("--- Got 2 " + dominoUpdateResponse2);
      dominoUpdateResponse2.getUpdates().forEach(logger::info);
    }

    {
      List<CommonExercise> changedExercises = new ArrayList<>();
      CommonExercise first = project.getRawExercises().iterator().next();
      logger.info("First " + first);
      changedExercises.add(first);
      ImportInfo importFromDomino2 = new ImportInfo(importProjectInfo,
          new ArrayList<>(),
          changedExercises,
          new ArrayList<>(),
          new HashSet<>());

      DominoUpdateResponse dominoUpdateResponse2 = getProjectSync().getDominoUpdateResponse(projectid, importUser, false, importFromDomino2);

      logger.info("--- Got add " + dominoUpdateResponse2);
      List<DominoUpdateItem> updates = dominoUpdateResponse2.getUpdates();
      updates.forEach(logger::info);

      if (updates.size() != 1) {
        logger.error("\n\nexpecting 1 but got " + updates.size());
        stopNow();
      } else {
        DominoUpdateItem next = updates.iterator().next();
        if (next.getStatus() != CHANGE) logger.error("should be add " + next);
        if (next.getExerciseID() != first.getID()) logger.error("should be add " + first.getID());
      }
    }

    int bogusDominoID = 999999;
    {
      List<CommonExercise> changedExercises = new ArrayList<>();
      CommonExercise first = new Exercise(-1, "unkn", importUser, "new add ", "new add trans", "new add trans", "alt fl", "transliter", false,
          new HashMap<>(), System.currentTimeMillis(), projectid, false, 1, false, 0, bogusDominoID, false);
      logger.info("\n\n\nFirst " + first);
      changedExercises.add(first);
      ImportInfo importFromDomino2 = new ImportInfo(importProjectInfo,
          changedExercises,
          new ArrayList<>(),
          new ArrayList<>(),
          new HashSet<>());

      DominoUpdateResponse dominoUpdateResponse2 = getProjectSync().getDominoUpdateResponse(projectid, importUser, false, importFromDomino2);

      logger.info("--- Got change " + dominoUpdateResponse2);
      List<DominoUpdateItem> updates = dominoUpdateResponse2.getUpdates();
      updates.forEach(logger::info);

      if (updates.size() != 1) {
        logger.error("expecting 1 but got " + updates.size());
        stopNow();

      } else {
        DominoUpdateItem next = updates.iterator().next();
        if (next.getStatus() != ADD) logger.error("should be add " + next);
        if (next.getExerciseID() != first.getID()) logger.error("should be add " + first.getID());
      }
    }

    {
//      int exid = 176521;

      CommonExercise first = project.getRawExercises().iterator().next();
      int dominoIDForTest = first.getDominoID();

      logger.info("delete " + dominoIDForTest + " first " + first);

      List<Integer> deletedDominoIDs = new ArrayList<>();

      deletedDominoIDs.add(dominoIDForTest);
      ImportInfo importFromDomino2 = new ImportInfo(importProjectInfo,
          new ArrayList<>(),
          new ArrayList<>(),
          deletedDominoIDs,
          new HashSet<>());

      DominoUpdateResponse dominoUpdateResponse2 = getProjectSync().getDominoUpdateResponse(projectid, importUser, false, importFromDomino2);

      logger.info("--- Got delete by domino id " + dominoUpdateResponse2);
      List<DominoUpdateItem> updates = dominoUpdateResponse2.getUpdates();
      updates.forEach(logger::info);

      if (updates.size() != 1) {
        logger.error("expecting 1 but got " + updates.size());
        stopNow();


      } else {
        DominoUpdateItem next = updates.iterator().next();
        if (next.getStatus() != DELETE) logger.error("should be add " + next);
        if (next.getExerciseID() != first.getID()) logger.error("should be add " + first.getID());
      }
    }

    // test deleting orig import
    {

      CommonExercise first = project.getExerciseByID(155368);
      String netprofID = first.getOldID();
      logger.info("delete by " + 155368 + " First " + first);

      HashSet<String> deletedNPIDs = new HashSet<>();
      deletedNPIDs.add(netprofID);
      ImportInfo importFromDomino2 = new ImportInfo(importProjectInfo,
          new ArrayList<>(),
          new ArrayList<>(),
          new ArrayList<>(),
          deletedNPIDs);

      DominoUpdateResponse dominoUpdateResponse2 = getProjectSync().getDominoUpdateResponse(projectid, importUser, false, importFromDomino2);

      logger.info("--- Got delete by np id " + dominoUpdateResponse2);
      List<DominoUpdateItem> updates = dominoUpdateResponse2.getUpdates();
      updates.forEach(logger::info);

      if (updates.size() != 1) {
        logger.error("expecting 1 but got " + updates.size());
        stopNow();
      } else {
        DominoUpdateItem next = updates.iterator().next();
        if (next.getStatus() != DELETE) logger.error("should be add " + next);
        if (next.getExerciseID() != first.getID()) logger.error("should be add " + first.getID());
      }
    }

    // test deleting context exercise with np id
    {
      // this guy is missing the context exercise...
      logger.info("--- test deleting context exercise with np id \n\n\n\n");

      CommonExercise withNoContext = new Exercise(-1, "" + 612, importUser, "new add ", "new add trans", "new add trans", "alt fl", "transliter", false,
          new HashMap<>(), System.currentTimeMillis(), projectid, false, 1, false, 0, bogusDominoID, false);

      CommonExercise parent = project.getExerciseByID(154838);
      HasID context = parent.getDirectlyRelated().iterator().next();

      List<CommonExercise> changedExercises = new ArrayList<>();
      changedExercises.add(withNoContext);
      ImportInfo importFromDomino2 = new ImportInfo(importProjectInfo,
          new ArrayList<>(),
          changedExercises,
          new ArrayList<>(),
          new HashSet<>());

      DominoUpdateResponse dominoUpdateResponse2 =
          getProjectSync().getDominoUpdateResponse(projectid, importUser, false, importFromDomino2);

      logger.info("--- Got delete context sentence  " + dominoUpdateResponse2);
      List<DominoUpdateItem> updates = dominoUpdateResponse2.getUpdates();
      updates.forEach(logger::info);

      if (updates.size() != 2) {
        logger.error("expecting 2 but got " + updates.size());
        stopNow();
      } else {
        DominoUpdateItem next = updates.iterator().next();

        updates.forEach(dominoUpdateItem -> {
          if (next.isContext() && next.getStatus() != DELETE) {
            logger.error("should be add " + next);
            stopNow();
          }
          if (next.isContext() && next.getExerciseID() != context.getID()) {
            logger.error("should be context id " + context.getID() + " but was " + next.getExerciseID());
          }
        });
      }
    }

    // test adding context exercise with np id
    {
      // this guy has a new context exercise...
      logger.info("--- adding context sentence\n\n\n\n");
      CommonExercise withAnotherContext = new Exercise(project.getExerciseByID(154838));

      Date date = new Date();
      withAnotherContext.getDirectlyRelated().add(
          new Exercise(-1, "", importUser, "second context " + date, "second context trans", "second context trans", "alt fl", "transliter", false,
              new HashMap<>(), System.currentTimeMillis(), projectid, false, 1, true, 0, bogusDominoID, false));

      List<CommonExercise> changedExercises = new ArrayList<>();
      changedExercises.add(withAnotherContext);
      ImportInfo importFromDomino2 = new ImportInfo(importProjectInfo,
          new ArrayList<>(),
          changedExercises,
          new ArrayList<>(),
          new HashSet<>());

      DominoUpdateResponse dominoUpdateResponse2 = getProjectSync().getDominoUpdateResponse(projectid, importUser, false, importFromDomino2);

      logger.info("--- Got add context  " + dominoUpdateResponse2);
      List<DominoUpdateItem> updates = dominoUpdateResponse2.getUpdates();
      updates.forEach(logger::info);

      if (updates.size() != 2) {
        logger.error("expecting 2 but got " + updates.size());
      } else {
        DominoUpdateItem next = updates.iterator().next();
        updates.forEach(dominoUpdateItem -> {
          if (next.isContext() && next.getStatus() != ADD) {
            logger.error("should be add " + next);
            stopNow();
          }
        });

        if (next.getStatus() != CHANGE) {
          logger.error("should be change " + next);
          stopNow();
        }
        if (next.getDominoID() != bogusDominoID) {
          logger.error("should have domino id " + bogusDominoID + " but was " + next.getDominoID());
        }
      }
    }
  }

  private Project getProject(int projectid) {
    return db.getProject(projectid);
  }

  @NotNull
  private ProjectSync getProjectSync() {
    return db.getProjectSync();
  }

  private void testSyncContextChange() {
    int projectid = 16;
    int dominoID = 999999;
    logger.info("--- testSyncContextChange context sentence\n\n\n\n");

    int importUser = db.getUserDAO().getImportUser();
    DominoUpdateResponse dominoUpdateResponse = getProjectSync()
        .addPending(projectid, importUser, false);

    logger.info("--- Got " + dominoUpdateResponse);
    dominoUpdateResponse.getUpdates().forEach(logger::info);

    // logger.info("Got " + dominoUpdateResponse);

    Project project = getProject(projectid);
    Iterator<String> iterator = project.getTypeOrder().iterator();
    String unit = iterator.next();
    String chapter = iterator.next();
    ImportProjectInfo importProjectInfo = new ImportProjectInfo(489, importUser,
        unit, chapter, new Date().getTime());

    // test changing context exercise with np id
    {
      // this guy has a changed context exercise...
      CommonExercise withAnotherContext = new Exercise(project.getExerciseByID(154838));

      List<ClientExercise> directlyRelated = withAnotherContext.getDirectlyRelated();
      ClientExercise orig = directlyRelated.iterator().next();

      CommonExercise copyContext = new Exercise(orig.asCommon());
      copyContext.getMutable().setEnglish(orig.getEnglish() + " _ CHANGED");
      withAnotherContext.getDirectlyRelated().clear();
      withAnotherContext.getDirectlyRelated().add(copyContext);

      List<CommonExercise> changedExercises = new ArrayList<>();
      changedExercises.add(withAnotherContext);
      ImportInfo importFromDomino2 = new ImportInfo(importProjectInfo,
          new ArrayList<>(),
          changedExercises,
          new ArrayList<>(),
          new HashSet<>());

      DominoUpdateResponse dominoUpdateResponse2 = getProjectSync().getDominoUpdateResponse(projectid, importUser, false, importFromDomino2);

      logger.info("--- Got changed context  " + dominoUpdateResponse2);
      List<DominoUpdateItem> updates = dominoUpdateResponse2.getUpdates();
      updates.forEach(logger::info);

      if (updates.size() != 2) {
        logger.error("expecting 2 but got " + updates.size());
        stopNow();
      } else {
        for (DominoUpdateItem next : updates) {
          if (next.getStatus() != CHANGE) {
            logger.error("should be change " + next);
          }

          if (next.getParent() > -1) {
            if (next.getExerciseID() != copyContext.getID()) {
              logger.error("should have ex id " + copyContext.getID() + " but was " + next.getExerciseID());
            }
          }
        }

      }
    }
  }

}
