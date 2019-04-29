/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.postgres;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.BaseTest;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.dialog.IDialogDAO;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.project.Project;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.shared.analysis.AnalysisReport;
import mitll.langtest.shared.analysis.AnalysisRequest;
import mitll.langtest.shared.analysis.PhoneSession;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.DialogMetadata;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static mitll.langtest.shared.project.ProjectMode.DIALOG;

public class DialogTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(DialogTest.class);


  public static final int MAX = 200;
  public static final int KOREAN_ID = 46;
  private static final String TOPIC_PRESENTATION_C = "Topic Presentation C";
  private static final String TOPIC_PRESENTATION_A = "Topic Presentation A";
  private static final String PRESENTATION1 = "presentation";
  private static final String PRESENTATION = PRESENTATION1;
  private static final String ANY1 = "Any";
  private static final String ANY = ANY1;
  private static final String CHAPTER = "Chapter";
  private static final String U5 = "" + 5;
  private static final String UNIT1 = "Unit";
  private static final String UNIT = UNIT1;
  private static final String C17 = "" + 17;
  private static final String PAGE = "page";
  private static final String KOREAN = "Korean";

  private static final int USERID = 6;
  private static final int PROJECTID = 21;

  @Test
  public void testNewDialog() {
    DatabaseImpl andPopulate = getDatabase();

    Project project = andPopulate.getProject(PROJECTID, true);
    andPopulate.waitForDefaultUser();

    Dialog toAdd = addDialog(andPopulate, PROJECTID, DialogType.DIALOG);

    for (ClientExercise exercise : toAdd.getExercises()) {
      logger.info("new " + exercise);
      CommonExercise lookup = project.getExerciseByID(exercise.getID());
      logger.info("lookup " + lookup);
    }
  }

  @Test
  public void testNewDialogOps() {
    DatabaseImpl andPopulate = getDatabase();

    Project project = andPopulate.getProject(PROJECTID, true);
    andPopulate.waitForDefaultUser();

    waitTillLoad();

    // do create!
    IDialog toAdd = addDialog(andPopulate, PROJECTID, DialogType.DIALOG);
    int id = toAdd.getID();
    logger.info("new dialog " + toAdd);
    IDialogDAO dialogDAO = andPopulate.getDialogDAO();

    List<ClientExercise> exercises = toAdd.getExercises();

    // now should have one exercise
    Assert.assertEquals(exercises.size(), 1);

    for (ClientExercise exercise : exercises) {
      logger.info("new " + exercise);
      CommonExercise lookup = project.getExerciseByID(exercise.getID());
      logger.info("lookup " + lookup);
    }

    // do delete!
    {
      int exid = exercises.get(exercises.size() - 1).getID();
      boolean b = dialogDAO.deleteExercise(PROJECTID, id, exid);
      if (!b) {
        logger.error("didn't delete the exercise " + exid);
      } else {
        logger.info("says it did delete " + exid);

      }

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
    }

    logger.info("\n\n\nafter  has " + exercises.size());

    // after delete should have 0
    Assert.assertEquals(exercises.size(), 0);

    exercises.forEach(logger::info);

    // insert into empty list
    {
      //    int exid = exercises.get(exercises.size() - 1).getID();
      List<ClientExercise> clientExercises = dialogDAO.addEmptyExercises(toAdd, USERID, -1, true, System.currentTimeMillis());
      Assert.assertEquals(clientExercises.size(), 1);

      // speaker attrbute (why?) and language
      Assert.assertEquals(clientExercises.get(0).getAttributes().size(), 2);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();

      Assert.assertEquals(exercises.size(), 1);
      Assert.assertEquals(exercises.get(0).getAttributes().size(), 2);
    }

    // insert after first
    {
      int exid = exercises.get(0).getID();
      logger.info("first exid is " + exid);
      List<ClientExercise> clientExercises = dialogDAO.addEmptyExercises(toAdd, USERID, exid, true, System.currentTimeMillis());
      Assert.assertEquals(clientExercises.size(), 1);
      Assert.assertEquals(clientExercises.get(0).getAttributes().size(), 2);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
      Assert.assertEquals(exercises.size(), 2);

      ClientExercise added = clientExercises.get(0);

      // newly added exercise should be after the first one
      Assert.assertEquals(toAdd.getExercises().indexOf(added), 1);

      toAdd.getExercises().forEach(exercise -> Assert.assertEquals(exercise.getAttributes().size(), 2));

      toAdd.getExercises().forEach(logger::info);
    }

    // insert after first again
    {
      int exid = exercises.get(0).getID();
      List<ClientExercise> clientExercises = dialogDAO.addEmptyExercises(toAdd, USERID, exid, true, System.currentTimeMillis());
      Assert.assertEquals(clientExercises.size(), 1);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
      Assert.assertEquals(exercises.size(), 3);

      ClientExercise added = clientExercises.get(0);

      // newly added exercise should be after the first one
      Assert.assertEquals(toAdd.getExercises().indexOf(added), 1);

      toAdd.getExercises().forEach(logger::info);
    }

    // insert at end
    {
      int exid = exercises.get(exercises.size() - 1).getID();
      List<ClientExercise> clientExercises = dialogDAO.addEmptyExercises(toAdd, USERID, exid, true, System.currentTimeMillis());
      Assert.assertEquals(clientExercises.size(), 1);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
      Assert.assertEquals(exercises.size(), 4);

      ClientExercise added = clientExercises.get(0);

      // newly added exercise should be after the first one
      Assert.assertEquals(toAdd.getExercises().indexOf(added), toAdd.getExercises().size() - 1);

      toAdd.getExercises().forEach(logger::info);
    }

    // delete in the middle
    {
      ClientExercise toDelete = exercises.get(1);
      ClientExercise next = exercises.get(2);
      int exid = toDelete.getID();

      dialogDAO.deleteExercise(PROJECTID, toAdd.getID(), exid);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
      Assert.assertEquals(exercises.size(), 3);

      // deleted shouldn't be there
      Assert.assertEquals(toAdd.getExercises().indexOf(toDelete), -1);
      // next should be in place of deleted
      Assert.assertEquals(toAdd.getExercises().indexOf(next), 1);

      toAdd.getExercises().forEach(logger::info);
    }
  }

  @Test
  public void testNewDialogOpsAlternate() {
    DatabaseImpl andPopulate = getDatabase();

    Project project = andPopulate.getProject(PROJECTID, true);
    andPopulate.waitForDefaultUser();

    waitTillLoad();

    // do create!
    IDialog toAdd = addDialog(andPopulate, PROJECTID, DialogType.DIALOG);
    int id = toAdd.getID();
    logger.info("new dialog " + toAdd);
    IDialogDAO dialogDAO = andPopulate.getDialogDAO();

    List<ClientExercise> exercises = toAdd.getExercises();

    // now should have one exercise
    Assert.assertEquals(1, exercises.size());

    for (ClientExercise exercise : exercises) {
      logger.info("new " + exercise);
      CommonExercise lookup = project.getExerciseByID(exercise.getID());
      logger.info("lookup " + lookup);
    }

    {
      List<ClientExercise> clientExercises = dialogDAO.addEmptyExercises(toAdd, USERID, exercises.get(0).getID(), false, System.currentTimeMillis());
      Assert.assertEquals(clientExercises.size(), 1);

      // speaker attrbute (why?) and language
      Assert.assertEquals(clientExercises.get(0).getAttributes().size(), 2);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();

      Assert.assertEquals(2, exercises.size());
      Assert.assertEquals(exercises.get(0).getAttributes().size(), 2);
    }

    // insert after first again
    {
      int exid = exercises.get(0).getID();
      logger.info("first exid is " + exid);
      List<ClientExercise> clientExercises = dialogDAO.addEmptyExercises(toAdd, USERID, exid, true, System.currentTimeMillis());
      Assert.assertEquals(clientExercises.size(), 1);
      Assert.assertEquals(clientExercises.get(0).getAttributes().size(), 2);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
      Assert.assertEquals(3, exercises.size());

      ClientExercise added = clientExercises.get(0);

      // newly added exercise should be after the first one
      Assert.assertEquals(toAdd.getExercises().indexOf(added), 1);

      toAdd.getExercises().forEach(exercise -> Assert.assertEquals(exercise.getAttributes().size(), 2));

      toAdd.getExercises().forEach(logger::info);
    }

    // insert after first again
    {
      int exid = exercises.get(0).getID();
      List<ClientExercise> clientExercises = dialogDAO.addEmptyExercises(toAdd, USERID, exid, true, System.currentTimeMillis());
      Assert.assertEquals(clientExercises.size(), 1);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
      Assert.assertEquals(4, exercises.size());

      ClientExercise added = clientExercises.get(0);

      // newly added exercise should be after the first one
      Assert.assertEquals(toAdd.getExercises().indexOf(added), 1);

      toAdd.getExercises().forEach(logger::info);
    }

    // insert at end
    {
      int exid = exercises.get(exercises.size() - 1).getID();
      List<ClientExercise> clientExercises = dialogDAO.addEmptyExercises(toAdd, USERID, exid, true, System.currentTimeMillis());
      Assert.assertEquals(clientExercises.size(), 1);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
      Assert.assertEquals(exercises.size(), 5);

      ClientExercise added = clientExercises.get(0);

      // newly added exercise should be after the first one
      Assert.assertEquals(toAdd.getExercises().indexOf(added), toAdd.getExercises().size() - 1);

      toAdd.getExercises().forEach(logger::info);
    }

    // delete in the middle
    {
      ClientExercise toDelete = exercises.get(1);
      ClientExercise next = exercises.get(2);
      int exid = toDelete.getID();

      dialogDAO.deleteExercise(PROJECTID, toAdd.getID(), exid);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
      Assert.assertEquals(exercises.size(), 4);

      // deleted shouldn't be there
      Assert.assertEquals(toAdd.getExercises().indexOf(toDelete), -1);
      // next should be in place of deleted
      Assert.assertEquals(toAdd.getExercises().indexOf(next), 1);

      toAdd.getExercises().forEach(logger::info);
    }
  }

  /**
   * TODO : also test left side right side stuff
   */
  @Test
  public void testNewIntepreterDialogOps() {
    DatabaseImpl andPopulate = getDatabase();

    Project project = andPopulate.getProject(PROJECTID, true);
    andPopulate.waitForDefaultUser();

    waitTillLoad();

    // do create!
    DialogType interpreter = DialogType.INTERPRETER;
    IDialog toAdd = addDialog(andPopulate, PROJECTID, interpreter);
    int id = toAdd.getID();
    logger.info("new dialog " + toAdd);
    IDialogDAO dialogDAO = andPopulate.getDialogDAO();

    List<ClientExercise> exercises = toAdd.getExercises();

    // now should have one exercise
    Assert.assertEquals(exercises.size(), 2);

    boolean prevEnglish = false;
    boolean isFirst = true;
    for (ClientExercise exercise : exercises) {
      logger.info("new    " + exercise);
      CommonExercise lookup = project.getExerciseByID(exercise.getID());
      logger.info("lookup " + lookup);
      if (!isFirst) {
        boolean bothEnglish = exercise.hasEnglishAttr() == prevEnglish;
        Assert.assertFalse(bothEnglish);
        isFirst = false;
      }
      prevEnglish = exercise.hasEnglishAttr();
    }


    // do delete!
    {
      int exid = exercises.get(exercises.size() - 1).getID();
      boolean b = dialogDAO.deleteExercise(PROJECTID, id, exid);
      if (!b) {
        logger.error("didn't delete the exercise " + exid);
      } else {
        logger.info("says it did delete " + exid);
      }

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
    }

    logger.info("\n\n\nafter  has " + exercises.size());

    // after delete should have 0
    Assert.assertEquals(exercises.size(), 0);

    exercises.forEach(logger::info);

    // insert into empty list
    {
      List<ClientExercise> clientExercises = dialogDAO.addEmptyExercises(toAdd, USERID, -1, true, System.currentTimeMillis());
      Assert.assertEquals(clientExercises.size(), 2);

      // speaker attrbute (why?) and language
      Assert.assertEquals(clientExercises.get(0).getAttributes().size(), 2);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();

      Assert.assertEquals(exercises.size(), 2);
      Assert.assertEquals(exercises.get(0).getAttributes().size(), 2);
    }

    // insert after first
    {
      int exid = exercises.get(1).getID();
      logger.info("first exid is " + exid);
      List<ClientExercise> clientExercises = dialogDAO.addEmptyExercises(toAdd, USERID, exid, true, System.currentTimeMillis());
      Assert.assertEquals(clientExercises.size(), 2);
      Assert.assertEquals(clientExercises.get(0).getAttributes().size(), 2);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
      Assert.assertEquals(exercises.size(), 4);

      ClientExercise added = clientExercises.get(0);

      // newly added exercise should be after the first one
      Assert.assertEquals(toAdd.getExercises().indexOf(added), 2);

      toAdd.getExercises().forEach(exercise -> Assert.assertEquals(exercise.getAttributes().size(), 2));

      toAdd.getExercises().forEach(logger::info);
    }

    // insert after first again
    {
      int exid = exercises.get(1).getID();
      logger.info("exids are ");

      exercises.stream().forEach(exercise -> logger.info(exercise.getID() + " : " + exercise.getUpdateTime()));

      logger.info("insert after " + exid);
      List<ClientExercise> clientExercises = dialogDAO.addEmptyExercises(toAdd, USERID, exid, true, System.currentTimeMillis());
      Assert.assertEquals(clientExercises.size(), 2);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
      Assert.assertEquals(exercises.size(), 6);

      ClientExercise added = clientExercises.get(0);

      toAdd.getExercises().forEach(logger::info);

      // newly added exercise should be after the first one
      Assert.assertEquals(2, toAdd.getExercises().indexOf(added));

    }

    // insert at end
    {
      int exid = exercises.get(exercises.size() - 1).getID();
      List<ClientExercise> clientExercises = dialogDAO.addEmptyExercises(toAdd, USERID, exid, true, System.currentTimeMillis());
      Assert.assertEquals(clientExercises.size(), 2);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
      Assert.assertEquals(exercises.size(), 8);

      ClientExercise added = clientExercises.get(0);

      // newly added exercise should be after the first one
      Assert.assertEquals(toAdd.getExercises().indexOf(added), toAdd.getExercises().size() - 2);

      toAdd.getExercises().forEach(logger::info);
    }

    // delete in the middle
    {
      ClientExercise toDelete = exercises.get(3);
      ClientExercise next = exercises.get(4);
      int exid = toDelete.getID();

      boolean b = dialogDAO.deleteExercise(PROJECTID, toAdd.getID(), exid);
      Assert.assertTrue(b);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
      Assert.assertEquals(exercises.size(), 6);

      // deleted shouldn't be there
      Assert.assertEquals(toAdd.getExercises().indexOf(toDelete), -1);
      // next should be in place of deleted
      Assert.assertEquals(toAdd.getExercises().indexOf(next), 2);

      toAdd.getExercises().forEach(logger::info);
    }
  }

  @NotNull
  private Dialog addDialog(DatabaseImpl andPopulate, int projectid, DialogType dialog) {
    // DialogType dialog = DialogType.DIALOG;
    Dialog toAdd = new Dialog(-1,
        USERID,
        projectid,
        -1,
        -1,
        System.currentTimeMillis(),
        "1", "1", "orient", "", "fl Wednesday", "en",
        new ArrayList<>(),
        new ArrayList<>(),
        new ArrayList<>(),
        dialog,
        "us", true
    );

    andPopulate.getDialogDAO().add(toAdd);
    return toAdd;
  }

  @Test
  public void testNewDialogAndInsert() {
    DatabaseImpl andPopulate = getDatabase();
    Project project = andPopulate.getProject(21, true);
    Dialog toAdd = new Dialog(-1,
        6,
        21,
        -1,
        -1,
        System.currentTimeMillis(),
        "1", "1", "orient", "", "fl Friday", "en",
        new ArrayList<>(),
        new ArrayList<>(),
        new ArrayList<>(),
        DialogType.DIALOG,
        "us", true
    );

    andPopulate.waitForDefaultUser();

    andPopulate.getDialogDAO().add(toAdd);

    for (ClientExercise exercise : toAdd.getExercises()) {
      logger.info("new " + exercise);
      CommonExercise lookup = project.getExerciseByID(exercise.getID());
      logger.info("lookup " + lookup);
    }
  }

  @Test
  public void testGetNewDialog() {
    DatabaseImpl andPopulate = getDatabase();
    Project project = andPopulate.getProject(PROJECTID, true);
    project.getDialogs().forEach(logger::info);
  }

  @Test
  public void testGetDialog() {
    DatabaseImpl andPopulate = getDatabase();
    Project project = andPopulate.getProject(PROJECTID, true);

    List<IDialog> collect = project.getDialogs().stream().filter(dialog -> dialog.getID() == 83).collect(Collectors.toList());

    IDialog iDialog = collect.get(0);

    logger.info("new dialog " + iDialog);

    Assert.assertEquals(iDialog.getExercises().size(), 1);
  }

  public void testGetExercisesDialog() {
    DatabaseImpl andPopulate = getDatabase();
    int projectid = PROJECTID;
    int i = 32;

    Project project = andPopulate.getProject(projectid, true);

    andPopulate.getProject(projectid, true);

    andPopulate.waitForDefaultUser();

    IDialog iDialog = getiDialog(andPopulate, projectid, i);
    logger.info("before has " + iDialog.getExercises().size());
    iDialog.getExercises().forEach(logger::info);
  }

  @Test
  public void testGetExercisesDialog2() {
    DatabaseImpl andPopulate = getDatabase();
    int projectid = PROJECTID;
    int i = 33;

    Project project = andPopulate.getProject(projectid, true);

    andPopulate.getProject(projectid, true);

    andPopulate.waitForDefaultUser();

    IDialog iDialog = getiDialog(andPopulate, projectid, i);
    logger.info("before has " + iDialog.getExercises().size());
    iDialog.getExercises().forEach(logger::info);
  }

  @Test
  public void testChangeDialog() {
    DatabaseImpl andPopulate = getDatabase();
    int projectid = PROJECTID;
    Project project = andPopulate.getProject(projectid, true);

    int i = 32;
    List<IDialog> collect = project.getDialogs().stream().filter(dialog -> dialog.getID() == i).collect(Collectors.toList());

    collect.forEach(logger::info);

    IDialog iDialog = collect.get(0);
    {
      logger.info("before dialog " + iDialog.getID() + " : " + iDialog.isPrivate());
      iDialog.getMutable().setIsPrivate(false);
    }

    {
      andPopulate.getDialogDAO().update(iDialog);

      collect = project.getDialogs().stream().filter(dialog -> dialog.getID() == i).collect(Collectors.toList());
      iDialog = collect.get(0);
      logger.info("after dialog " + iDialog.getID() + " : " + iDialog.isPrivate());
    }

    Assert.assertFalse(iDialog.isPrivate());

    {
      logger.info("2 before dialog " + iDialog.getID() + " : " + iDialog.isPrivate());
      iDialog.getMutable().setIsPrivate(true);
    }


    {
      andPopulate.getDialogDAO().update(iDialog);

      collect = project.getDialogs().stream().filter(dialog -> dialog.getID() == i).collect(Collectors.toList());
      iDialog = collect.get(0);
      logger.info("2 after dialog " + iDialog.getID() + " : " + iDialog.isPrivate());
    }

    Assert.assertTrue(iDialog.isPrivate());
  }

  @Test
  public void testInsertAtFrontDialog() {
    DatabaseImpl andPopulate = getDatabase();
    int projectid = PROJECTID;
    int i = 32;
    andPopulate.getProject(projectid, true);

    andPopulate.waitForDefaultUser();

    waitTillLoad();

    IDialog iDialog = getiDialog(andPopulate, projectid, i);

    doInsert(andPopulate, projectid, i, iDialog);
  }

  private void waitTillLoad() {
    try {
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testInsertAtFrontDialog2() {
    DatabaseImpl andPopulate = getDatabase();
    int projectid = PROJECTID;
    int i = 32;
    andPopulate.getProject(projectid, true);

    andPopulate.waitForDefaultUser();

    waitTillLoad();

    IDialog iDialog = getiDialog(andPopulate, projectid, i);

    doInsert(andPopulate, projectid, i, iDialog);
  }

  private void doInsert(DatabaseImpl andPopulate, int projectid, int i, IDialog iDialog) {
    logger.info("before has " + iDialog.getExercises().size());
    iDialog.getExercises().forEach(logger::info);

    andPopulate.getDialogDAO().addEmptyExercises(iDialog, USERID, -1, true, System.currentTimeMillis());

    logger.info("after  has " + iDialog.getExercises().size());
    iDialog.getExercises().forEach(logger::info);

    IDialog iDialog2 = getiDialog(andPopulate, projectid, i);
    logger.info("after2  has " + iDialog2.getExercises().size());
    iDialog2.getExercises().forEach(logger::info);
  }

  private IDialog getiDialog(DatabaseImpl andPopulate, int projectid, int i) {
    return andPopulate.getProject(projectid, true).getDialog(i);
  }

  @Test
  public void testDeleteEx() {
    DatabaseImpl andPopulate = getDatabase();
    int projectid = PROJECTID;
    andPopulate.getProject(projectid, true);

    andPopulate.waitForDefaultUser();

    IDialog iDialog = getiDialog(andPopulate, projectid, 32);

    List<ClientExercise> exercises = iDialog.getExercises();
    logger.info("before has " + exercises.size());
    exercises.forEach(logger::info);

    IDialogDAO dialogDAO = andPopulate.getDialogDAO();
    int id = exercises.get(exercises.size() - 1).getID();
    boolean b = dialogDAO.deleteExercise(projectid, iDialog.getID(), id);
    if (!b) logger.error("didn't delete the exercise " + id);


    //dialogDAO.addEmptyExercises(iDialog, projectid, USERID, -1, true, System.currentTimeMillis());

    logger.info("after  has " + exercises.size());
    exercises.forEach(logger::info);

    iDialog = getiDialog(andPopulate, projectid, 32);

    exercises = iDialog.getExercises();
    logger.info("after 2 has " + exercises.size());
    exercises.forEach(logger::info);
  }


  @Test
  public void testInterpreterStored() {
    DatabaseImpl andPopulate = getDatabase();
    Project project = andPopulate.getProject(12);
    report(andPopulate, project);
  }

  @Test
  public void testSessions() {
    DatabaseImpl andPopulate = getDatabase();
    Project project = andPopulate.getProject(21, true);
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    AnalysisReport performanceReportForUser = project.getAnalysis().getPerformanceReportForUser(new AnalysisRequest().setUserid(USERID));

    logger.info("Got " + performanceReportForUser);
    Map<Long, List<PhoneSession>> granularityToSessions = performanceReportForUser.getUserPerformance().getGranularityToSessions();
    logger.info("keys " + granularityToSessions.keySet());

    granularityToSessions.forEach((k, v) -> logger.info(" " + k + " = " + v));
//    List<PhoneSession> phoneSessions = granularityToSessions.get(-1);

    //  phoneSessions.forEach(phoneSession -> logger.info("Got " +phoneSession));

    //  report(andPopulate, project);
  }


  @Test
  public void testInterpreterFrenchToRecord() {
    DatabaseImpl andPopulate = getDatabase();
    andPopulate.getProject(12);
    Project project = andPopulate.getProjectManagement().getProductionByLanguage(Language.FRENCH);
    int projectid = project.getID();

    FilterRequest request = new FilterRequest()
        .setRecordRequest(true)
        .setMode(DIALOG);

    // project.getTypeOrder().forEach(type -> request.addPair(new Pair(type, SectionHelper.ANY)));

    request.addPair(new Pair("Book", ANY1));
    request.addPair(new Pair("Module", ANY1));
    request.addPair(new Pair("LANGUAGE", ANY1));
    request.addPair(new Pair("SPEAKER", ANY1));
    //request.addPair(new Pair("SPEAKER","A"));

    logger.info("types " + request + " for " + project.getTypeOrder());

    FilterResponse typeToValues = getTypeToValues(andPopulate, projectid, request);

    logger.info("typeToValues for " +
        "\n\treq          " + request +
        "\n\ttype->values " + typeToValues);

  }

  @Test
  public void testInterpreterFrench() {
    DatabaseImpl andPopulate = getDatabase();
    andPopulate.getProject(12);
    Project project = andPopulate.getProjectManagement().getProductionByLanguage(Language.FRENCH);
    int projectid = project.getID();

    FilterRequest request = new FilterRequest()
        .setRecordRequest(true)
        .setMode(DIALOG);

    // project.getTypeOrder().forEach(type -> request.addPair(new Pair(type, SectionHelper.ANY)));

    request.addPair(new Pair("Book", ANY1));
    request.addPair(new Pair("Module", ANY1));
    request.addPair(new Pair("LANGUAGE", ANY1));
    request.addPair(new Pair("SPEAKER", ANY1));
    //request.addPair(new Pair("SPEAKER","A"));

    logger.info("types " + request + " for " + project.getTypeOrder());

    FilterResponse typeToValues = getTypeToValues(andPopulate, projectid, request);

    logger.info("typeToValues for " +
        "\n\treq          " + request +
        "\n\ttype->values " + typeToValues);

//    request.addPair(new Pair("Book","1"));
//    request.addPair(new Pair("SPEAKER", "English Speaker"));
//
//    logger.info("types " + request + " for " + project.getTypeOrder());
//
//    typeToValues = getTypeToValues(andPopulate, projectid, request);
//
//    logger.info("typeToValues for " +
//        "\n\treq          " + request +
//        "\n\ttype->values " + typeToValues);
//

    request = new FilterRequest()
        .setOnlyUninspected(true)
        .setMode(DIALOG);

    logger.info("types " + request + " for " + project.getTypeOrder());

    typeToValues = getTypeToValues(andPopulate, projectid, request);

    logger.info("typeToValues for " +
        "\n\treq          " + request +
        "\n\ttype->values " + typeToValues);
  }


  @Test
  public void testNormalFrench() {
    DatabaseImpl andPopulate = getDatabase();
    andPopulate.getProject(12);
    Project project = andPopulate.getProjectManagement().getProductionByLanguage(Language.FRENCH);
    int projectid = project.getID();

    FilterRequest request = new FilterRequest()
        .setRecordRequest(true);

    // project.getTypeOrder().forEach(type -> request.addPair(new Pair(type, SectionHelper.ANY)));

    request.addPair(new Pair("Book", "1"));
    //request.addPair(new Pair("SPEAKER","A"));

    logger.info("types " + request + " for " + project.getTypeOrder());

    FilterResponse typeToValues = getTypeToValues(andPopulate, projectid, request);

    logger.info("typeToValues for " +
        "\n\treq          " + request +
        "\n\ttype->values " + typeToValues);

//    request.addPair(new Pair("Book","1"));
//    request.addPair(new Pair("SPEAKER","English Speaker"));
//
//    logger.info("types " + request + " for " + project.getTypeOrder());
//
//    typeToValues = getTypeToValues(andPopulate, projectid, request);
//
//    logger.info("typeToValues for " +
//        "\n\treq          " + request+
//        "\n\ttype->values " + typeToValues);


    request.setOnlyUninspected(true);

    logger.info("types " + request + " for " + project.getTypeOrder());

    typeToValues = getTypeToValues(andPopulate, projectid, request);

    logger.info("typeToValues for " +
        "\n\treq          " + request +
        "\n\ttype->values " + typeToValues);
  }

  private FilterResponse getTypeToValues(DatabaseImpl andPopulate, int projectid, FilterRequest request) {
    return andPopulate.getFilterResponseHelper().getTypeToValues(request, projectid, USERID);
  }

  @Test
  public void testInterpreterRecord() {
    DatabaseImpl andPopulate = getDatabase();
    int projectid = 12;
    Project project = andPopulate.getProject(projectid);

    FilterRequest request = new FilterRequest().setRecordRequest(true).setMode(DIALOG);
    project.getTypeOrder().forEach(type -> request.addPair(new Pair(type, SectionHelper.ANY)));

    FilterResponse typeToValues = getTypeToValues(andPopulate, projectid, request);

    logger.info("typeToValues " + typeToValues);

    ExerciseListRequest request1 = new ExerciseListRequest(1, USERID, projectid).setMode(DIALOG);

    request1.setOnlyUnrecordedByMe(true);
    HashMap<String, Collection<String>> typeToSelection = new HashMap<>();
    typeToSelection.put(UNIT1, Collections.singleton("1"));
    typeToSelection.put(DialogMetadata.LANGUAGE.name(), Collections.singleton(Language.ENGLISH.name()));
    request1.setTypeToSelection(typeToSelection);

    {
      List<CommonExercise> exercisesForSelectionState =
          andPopulate.getFilterResponseHelper().getExercisesForSelectionState(request1, projectid);

      exercisesForSelectionState.forEach(ex -> logger.info("ENGLISH got " + ex.getID() + " " + ex.getEnglish() + " " + ex.getForeignLanguage()));
    }

    typeToSelection.put(DialogMetadata.LANGUAGE.name(), Collections.singleton(Language.MANDARIN.name()));

    {
      List<CommonExercise> exercisesForSelectionState =
          andPopulate.getFilterResponseHelper().getExercisesForSelectionState(request1, projectid);

      exercisesForSelectionState.forEach(ex -> logger.info("CHINESE " +
          "\n\ttype->sel " + typeToSelection +
          " got " + ex.getID() + " " + ex.getEnglish() + " " + ex.getForeignLanguage() + " " + ex.getTokens()));
    }

    typeToSelection.put(DialogMetadata.SPEAKER.name(), Collections.singleton("B"));

    {
      List<CommonExercise> exercisesForSelectionState =
          andPopulate.getFilterResponseHelper().getExercisesForSelectionState(request1, projectid);

      exercisesForSelectionState.forEach(ex -> logger.info("CHINESE" +
          "\n\ttype->sel " + typeToSelection +
          " (A) got " + ex.getID() + " " + ex.getEnglish() + " " + ex.getForeignLanguage() + " " + ex.getTokens()));
    }

    typeToSelection.remove(UNIT1);

    {
      List<CommonExercise> exercisesForSelectionState =
          andPopulate.getFilterResponseHelper().getExercisesForSelectionState(request1, projectid);

      exercisesForSelectionState.forEach(ex -> logger.info("CHINESE got " + ex.getID() + " " + ex.getEnglish() + " " + ex.getForeignLanguage() + " " + ex.getTokens()));
    }


    //  report(andPopulate, project);
  }

/*  private void testDialogPopulate(String korean) {
    DatabaseImpl andPopulate = getDatabase();
    Project project = andPopulate.getProject(12);
//    Project project = andPopulate.getProjectByName(korean);

    if (!new DialogPopulate(andPopulate, getPathHelper(andPopulate)).populateDatabase(project, andPopulate.getProjectManagement().getProductionByLanguage(Language.ENGLISH), false, excel, appendOK)) {
      logger.info("testDialogPopulate project " + project + " already has dialog data.");
    }

    report(andPopulate, project);
  }*/

  private void report(DatabaseImpl andPopulate, Project project) {
    List<IDialog> dialogs = andPopulate.getDialogDAO().getDialogs(project.getID());
    dialogs.forEach(iDialog -> {
      logger.info("dialog " + iDialog);

      logger.info("sp    " + iDialog.getSpeakers());
      logger.info("attr  " + iDialog.getAttributes());
      // logger.info("by sp " + iDialog.groupBySpeaker());
      //   logger.info("core  " + iDialog.getCoreVocabulary().size());
      iDialog.getCoreVocabulary().forEach(clientExercise -> {
        List<String> tokens = project.getAudioFileHelper().getASR().getTokens(clientExercise.getForeignLanguage(), clientExercise.getTransliteration());
//        String pronunciationsFromDictOrLTS = project.getAudioFileHelper().getPronunciationsFromDictOrLTS(clientExercise.getForeignLanguage(), clientExercise.getTransliteration());
        logger.info("core " + clientExercise.getForeignLanguage() + " -> " + tokens);
      });
      // logger.info("\n\n\n");

//      iDialog.getExercises().forEach(clientExercise -> clientExercise.getAttributes().forEach(exerciseAttribute -> logger.info("\t" + exerciseAttribute)));
      iDialog.getExercises().forEach(clientExercise -> {
        List<ExerciseAttribute> collect = clientExercise.getAttributes().stream().filter(exerciseAttribute -> exerciseAttribute.getProperty().equalsIgnoreCase(DialogMetadata.LANGUAGE.name())).collect(Collectors.toList());
        if (!collect.isEmpty()) {
          boolean isEnglish = collect.get(0).getValue().equalsIgnoreCase(Language.ENGLISH.name());
          if (!isEnglish) {
//            String pronunciationsFromDictOrLTS = project.getAudioFileHelper().getPronunciationsFromDictOrLTS(clientExercise.getForeignLanguage(), clientExercise.getTransliteration());
//            logger.info(clientExercise.getForeignLanguage() + " -> " + pronunciationsFromDictOrLTS);

            // List<String> tokens = project.getAudioFileHelper().getASR().getTokens(clientExercise.getForeignLanguage(), clientExercise.getTransliteration());

            List<String> tokens = clientExercise.getTokens();
            if (tokens == null) {
              logger.error("ex #" + clientExercise.getID() + " " + clientExercise.getForeignLanguage() + " -> " + tokens);
            }
            logger.info("ex #" + clientExercise.getID() + " " + clientExercise.getForeignLanguage() + " -> " + tokens);

          }
        }

      });
    });
  }


  @NotNull
  private static PathHelper getPathHelper(DatabaseImpl database) {
    return new PathHelper("war", database.getServerProps());
  }


  @Test
  public void testEx() {
    DatabaseImpl andPopulate = getDatabase().setInstallPath("");

    Project project = andPopulate.getProjectByName(KOREAN);

    if (project == null) {
      logger.warn("no korean");
    } else {
      List<ClientExercise> all = new ArrayList<>();

      List<IDialog> dialogs = andPopulate.getDialogDAO().getDialogs(project.getID());
      dialogs.forEach(iDialog -> {
        logger.info("dialog " + iDialog);

        logger.info("sp    " + iDialog.getSpeakers());
        logger.info("attr  " + iDialog.getAttributes());
        logger.info("by sp " + iDialog.groupBySpeaker());
        logger.info("core  " + iDialog.getCoreVocabulary());
        logger.info("\n\n\n");

//      iDialog.getExercises().forEach(clientExercise -> clientExercise.getAttributes().forEach(exerciseAttribute -> logger.info("\t" + exerciseAttribute)));
        all.addAll(iDialog.getExercises());
      });
      logger.info("total is " + all.size());
//    assertEquals("onetwo", result);
    }
  }

  @Test
  public void testSH() {
    DatabaseImpl andPopulate = getDatabase().setInstallPath("");

    Project project = andPopulate.getProjectByName(KOREAN);
    ISection<IDialog> dialogSectionHelper = project.getDialogSectionHelper();


    List<IDialog> dialogs = andPopulate.getDialogDAO().getDialogs(project.getID());

    // dialogs.forEach(dialog -> logger.info("dialog " + dialog));

    IDialog iDialog = dialogs.get(0);

    List<ClientExercise> coreVocabulary = iDialog.getCoreVocabulary();
    logger.info("\n\n\tgot " + coreVocabulary.size() + " core");
    coreVocabulary.forEach(clientExercise -> logger.info("\t" + clientExercise.getID() +
        " " + clientExercise.getEnglish() + " " + clientExercise.getForeignLanguage()));
    //  project.getSectionHelper().report();

    {
      List<Pair> pairs = new ArrayList<>();
      pairs.add(new Pair(UNIT, U5));
      pairs.add(new Pair(CHAPTER, C17));
      pairs.add(new Pair(PAGE, ANY));
      pairs.add(new Pair(PRESENTATION1, ANY));
      FilterRequest request = new FilterRequest(-1, pairs, -1);
      FilterResponse typeToValues = dialogSectionHelper.getTypeToValues(request, false);
      logger.info("got " + typeToValues);
    }
    {
      List<Pair> pairs = new ArrayList<>();
      pairs.add(new Pair(UNIT, U5));
      pairs.add(new Pair(CHAPTER, C17));

      FilterRequest request = new FilterRequest(-1, pairs, -1);
      FilterResponse typeToValues = dialogSectionHelper.getTypeToValues(request, false);
      logger.info("got " + typeToValues);
    }


    {
      List<Pair> pairs = new ArrayList<>();
      pairs.add(new Pair(UNIT, U5));
      pairs.add(new Pair(CHAPTER, C17));

      HashMap<String, Collection<String>> objectObjectHashMap = new HashMap<>();
      objectObjectHashMap.put(UNIT, Collections.singletonList(U5));
      objectObjectHashMap.put(CHAPTER, Collections.singletonList(C17));

      Collection<IDialog> exercisesForSelectionState = dialogSectionHelper.getExercisesForSelectionState(objectObjectHashMap);
      logger.info("got " + exercisesForSelectionState);
    }
  }

  /**
   * Test adding the dialog data.
   */
  @Test
  public void testEnglishFromCannedData() {
    DatabaseImpl andPopulate = getDatabase().setInstallPath("");

    Project project = andPopulate.getProjectByName("English");

    logger.info("english " + project);
    List<IDialog> dialogs = andPopulate.getDialogDAO().getDialogs(project.getID());

    dialogs.forEach(dialog -> logger.info("dialog " + dialog));

    IDialog iDialog = dialogs.get(0);

    logger.info("First " + iDialog);
    List<String> speakers = iDialog.getSpeakers();

    logger.info("Speakers " + speakers);
    logger.info("Image    " + iDialog.getImageRef());

    iDialog.getAttributes().forEach(exerciseAttribute -> logger.info("\t" + exerciseAttribute));

    logger.info("Exercises : ");
    List<ClientExercise> exercises = iDialog.getExercises();
    /*   exercises.forEach(exercise -> logger.info(getShort(exercise)));*/

    Map<String, List<ClientExercise>> stringListMap = iDialog.groupBySpeaker();
    stringListMap.forEach((k, v) -> {
      logger.info(k + " : ");
      v.forEach(commonExercise -> logger.info(getShort(commonExercise)));
    });
  }

  /**
   * Test adding the dialog data.
   */
  @Test
  public void testKPFromCannedData() {
    DatabaseImpl andPopulate = getDatabase().setInstallPath("");

    Project project = andPopulate.getProjectByName(KOREAN);
    logger.info("korean " + project);
    List<IDialog> dialogs = andPopulate.getDialogDAO().getDialogs(project.getID());

    dialogs.forEach(dialog -> logger.info("dialog " + dialog));

    IDialog iDialog = dialogs.get(0);

    logger.info("First " + iDialog);
    List<String> speakers = iDialog.getSpeakers();

    logger.info("Speakers " + speakers);
    logger.info("Image    " + iDialog.getImageRef());

    iDialog.getAttributes().forEach(exerciseAttribute -> logger.info("\t" + exerciseAttribute));

    logger.info("Exercises : ");
    List<ClientExercise> exercises = iDialog.getExercises();
    /*   exercises.forEach(exercise -> logger.info(getShort(exercise)));*/

    Map<String, List<ClientExercise>> stringListMap = iDialog.groupBySpeaker();
    stringListMap.forEach((k, v) -> {
      logger.info(k + " : ");
      v.forEach(commonExercise -> logger.info(getShort(commonExercise)));
    });

    // exercises.forEach(commonExercise -> logger.info("ex " + commonExercise.getID() + " " + commonExercise.getOldID() + " has " + commonExercise.getDirectlyRelated()));

    // when shown generally, the exercises shouldn't have it
    exercises.forEach(commonExercise -> {
      CommonExercise exerciseByID = project.getExerciseByID(commonExercise.getID());
      if (!exerciseByID.getDirectlyRelated().isEmpty()) {
        logger.info("ex " + commonExercise.getID() + " has context?");
      }
    });

    List<ClientExercise> coreVocabulary = iDialog.getCoreVocabulary();
    logger.info("\n\n\tgot " + coreVocabulary.size() + " core");
    coreVocabulary.forEach(clientExercise -> logger.info("\t" + clientExercise.getID() +
        " " + clientExercise.getEnglish() + " " + clientExercise.getForeignLanguage()));
    //  project.getSectionHelper().report();

    {
      logger.info("OK - unit and chapter only\n\n\n\n");
      List<Pair> pairs = new ArrayList<>();
      pairs.add(new Pair(UNIT, U5));
      pairs.add(new Pair(CHAPTER, C17));
      pairs.add(new Pair(PAGE, ANY));
      pairs.add(new Pair(PRESENTATION1, ANY));
      FilterRequest request = new FilterRequest(-1, pairs, -1);
      FilterResponse typeToValues = project.getSectionHelper().getTypeToValues(request, false);
      logger.info("got " + typeToValues);

      if (false) {
        HashMap<String, Collection<String>> typeToSection = new HashMap<>();
        typeToSection.put(UNIT, Collections.singletonList(U5));
        typeToSection.put(CHAPTER, Collections.singletonList(C17));
        Collection<CommonExercise> exercisesForSelectionState = project.getSectionHelper().getExercisesForSelectionState(typeToSection);

        exercisesForSelectionState.stream().filter(ex -> ex.getEnglish().isEmpty()).forEach(commonExercise -> logger.info(getShort(commonExercise)));
      }
    }

    {
      logger.info("OK - unit and chapter and presentation \n\n\n\n");

      //   project.getSectionHelper().report();
      List<Pair> pairs = new ArrayList<>();
      pairs.add(new Pair(UNIT, U5));
      pairs.add(new Pair(CHAPTER, C17));
      pairs.add(new Pair(PAGE, ANY));
      pairs.add(new Pair(PRESENTATION, TOPIC_PRESENTATION_A));
      FilterRequest request = new FilterRequest(-1, pairs, -1);
      FilterResponse typeToValues = project.getSectionHelper().getTypeToValues(request, false);
      logger.info("got " + typeToValues);

      HashMap<String, Collection<String>> typeToSection = new HashMap<>();
      typeToSection.put(UNIT, Collections.singletonList(U5));
      typeToSection.put(CHAPTER, Collections.singletonList(C17));
      typeToSection.put(PRESENTATION, Collections.singletonList(TOPIC_PRESENTATION_A));
      Collection<CommonExercise> exercisesForSelectionState = project.getSectionHelper().getExercisesForSelectionState(typeToSection);

      exercisesForSelectionState.stream().filter(ex -> ex.getEnglish().isEmpty()).forEach(commonExercise -> logger.info(getShort(commonExercise)));
    }
    {
      logger.info("OK - unit and chapter and presentation \n\n\n\n");

      // project.getSectionHelper().report();
      List<Pair> pairs = new ArrayList<>();
      pairs.add(new Pair(UNIT, U5));
      pairs.add(new Pair(CHAPTER, C17));
      pairs.add(new Pair(PAGE, ANY));
      pairs.add(new Pair(PRESENTATION, TOPIC_PRESENTATION_C));
      FilterRequest request = new FilterRequest(-1, pairs, -1);
      FilterResponse typeToValues = project.getSectionHelper().getTypeToValues(request, false);
      logger.info("got " + typeToValues);

      HashMap<String, Collection<String>> typeToSection = new HashMap<>();
      typeToSection.put(UNIT, Collections.singletonList(U5));
      typeToSection.put(CHAPTER, Collections.singletonList(C17));
      typeToSection.put(PRESENTATION, Collections.singletonList(TOPIC_PRESENTATION_C));
      Collection<CommonExercise> exercisesForSelectionState = project.getSectionHelper().getExercisesForSelectionState(typeToSection);

      exercisesForSelectionState
          .stream()
          .filter(ex -> ex
              .getEnglish()
              .isEmpty())
          .forEach(commonExercise -> logger.info(getShort(commonExercise)));
    }

    if (false) {
      for (int unit = 1; unit < 9; unit++) {
        List<Pair> pairs = new ArrayList<>();
        pairs.add(new Pair(UNIT, "" + unit));
        pairs.add(new Pair(CHAPTER, ANY));
        pairs.add(new Pair(PAGE, ANY));
        pairs.add(new Pair(PRESENTATION, ANY));

        FilterRequest request = new FilterRequest(-1, pairs, -1);
        FilterResponse typeToValues = project.getSectionHelper().getTypeToValues(request, false);
        logger.info("got " + typeToValues);
      }
    }
    andPopulate.close();
  }

  @NotNull
  private String getShort(ClientExercise exercise) {
    return "\t" + exercise.getOldID() + " : " + exercise.getForeignLanguage() + " : " + exercise.getAttributes();
  }
}
