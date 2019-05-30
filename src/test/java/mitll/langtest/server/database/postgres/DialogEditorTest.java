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

import mitll.langtest.server.database.BaseTest;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.dialog.IDialogDAO;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.database.project.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.DialogMetadata;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.OOVWordsAndUpdate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static mitll.langtest.shared.project.ProjectMode.DIALOG;

public class DialogEditorTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(DialogEditorTest.class);

  public static final int MAX = 200;
  public static final int KOREAN_ID = 46;
  private static final String ANY1 = "Any";
  private static final String UNIT1 = "Unit";

  private static final int USERID = 6;
  private static final int PROJECTID = 21;//5;//21;//5;//21;
  public static final String SECOND = "second";
  public static final String FIRST = "first";

  @Test
  public void testNewDialog() {
    DatabaseImpl andPopulate = getDatabase();
    logger.warn("testNewDialog START ==--------- ");

    Project project = getProject(andPopulate);

    Dialog toAdd = addDialog(andPopulate, PROJECTID, DialogType.DIALOG);

    for (ClientExercise exercise : toAdd.getExercises()) {
      logger.info("new " + exercise);
      CommonExercise lookup = project.getExerciseByID(exercise.getID());
      logger.info("lookup " + lookup);
    }

    logger.warn("testNewDialog END ==--------- ");
    andPopulate.close();

  }

  @Test
  public void testGetDialog2() {
    DatabaseImpl andPopulate = getDatabase();
    logger.warn("testGetDialog2 START ==--------- ");

    Project project = getProject(andPopulate);

    logger.info("got " + project);

    IDialog dialog = project.getDialog(244);
    logger.info("got dialog " + dialog);

    for (ClientExercise exercise : dialog.getExercises()) {
      logger.info("new    " + exercise);
      CommonExercise lookup = project.getExerciseByID(exercise.getID());
      logger.info("lookup " + lookup);
    }

    logger.warn("testGetDialog2 END ==--------- ");
    andPopulate.close();

  }

  @Test
  public void testGetDialogs() {
    DatabaseImpl andPopulate = getDatabase();
    logger.warn("testGetDialogs START ==--------- ");

    Project project = getProject(andPopulate);

    andPopulate.waitForSetupComplete();
    logger.info("got " + project);

    IDialog dialog = project.getDialogs().iterator().next();
    logger.info("got dialog " + dialog);

    for (ClientExercise exercise : dialog.getExercises()) {
      logger.info("new    " + exercise);
      CommonExercise lookup = project.getExerciseByID(exercise.getID());
      logger.info("lookup " + lookup);
    }

    andPopulate.getDialogSectionHelper(PROJECTID).report();
    logger.warn("testGetDialog2 END ==--------- ");
    andPopulate.close();

  }


  private Project getProject(DatabaseImpl andPopulate) {
    Project project = andPopulate.getProject(PROJECTID, true);
    andPopulate.waitForSetupComplete();
    return project;
  }

  @Test
  public void testCoreVocab() {
    DatabaseImpl andPopulate = getDatabase();
    logger.warn("testCoreVocab START ==--------- ");

    Project project = getProject(andPopulate);

    // do create!
    IDialog toAdd = addDialog(andPopulate, PROJECTID, DialogType.DIALOG);
    int id = toAdd.getID();
    logger.info("new dialog " + toAdd);
    IDialogDAO dialogDAO = andPopulate.getDialogDAO();

    List<ClientExercise> coreVocabulary = toAdd.getCoreVocabulary();
    Assert.assertEquals(0, coreVocabulary.size());

    ClientExercise exercise = dialogDAO.addCoreVocab(toAdd, USERID, -1, System.currentTimeMillis());

    Assert.assertTrue("Expecting non zero id", exercise.getID() > 0);

//    coreVocabulary = toAdd.getCoreVocabulary();
//    Assert.assertEquals(1, coreVocabulary.size());

    // get it again
    toAdd = getiDialog(andPopulate, PROJECTID, id);

    // better be only one at the end
    coreVocabulary = toAdd.getCoreVocabulary();
    Assert.assertEquals(1, coreVocabulary.size());

    boolean success = dialogDAO.deleteCoreExercise(id, exercise.getID());
    Assert.assertTrue("Expecting success", success);

    // get it again
    toAdd = getiDialog(andPopulate, PROJECTID, id);

    // better be only one at the end
    coreVocabulary = toAdd.getCoreVocabulary();
    Assert.assertEquals(0, coreVocabulary.size());
  }

  @Test
  public void testCoreVocabInsertAfter() {
    DatabaseImpl andPopulate = getDatabase();
    logger.warn("testCoreVocabInsertAfter START ==--------- ");

    Project project = getProject(andPopulate);

    // do create!
    IDialog toAdd = addDialog(andPopulate, PROJECTID, DialogType.DIALOG);
    int dialogID = toAdd.getID();
    logger.info("new dialog " + toAdd);
    IDialogDAO dialogDAO = andPopulate.getDialogDAO();

    List<ClientExercise> coreVocabulary = toAdd.getCoreVocabulary();
    Assert.assertEquals(0, coreVocabulary.size());

    ClientExercise exercise = dialogDAO.addCoreVocab(toAdd, USERID, -1, System.currentTimeMillis());

    int id1 = exercise.getID();
    Assert.assertTrue("Expecting non zero dialogID", id1 > 0);

    OOVWordsAndUpdate first = andPopulate.getExerciseDAO(PROJECTID).updateText(project, dialogID, id1, -1, FIRST);
    logger.info("got 1 " + first);
    Assert.assertTrue("Expecting it to update", first.isDidUpdate());

//    coreVocabulary = toAdd.getCoreVocabulary();
//    Assert.assertEquals(1, coreVocabulary.size());

    // get it again
    toAdd = getiDialog(andPopulate, PROJECTID, dialogID);

    // better be only one at the end
    coreVocabulary = toAdd.getCoreVocabulary();
    Assert.assertEquals(1, coreVocabulary.size());

    Assert.assertTrue("Expecting it to be the text I set it to before", coreVocabulary.get(0).getForeignLanguage().equalsIgnoreCase(FIRST));

    ClientExercise exercise2 = dialogDAO.addCoreVocab(toAdd, USERID, id1, System.currentTimeMillis());

    OOVWordsAndUpdate second = andPopulate.getExerciseDAO(PROJECTID).updateText(project, dialogID, exercise2.getID(), -1, SECOND);
    logger.info("got 2 " + second);
    Assert.assertTrue("Expecting it to update", second.isDidUpdate());

    // get it again
    toAdd = getiDialog(andPopulate, PROJECTID, dialogID);

    // better be only one at the end
    coreVocabulary = toAdd.getCoreVocabulary();
    Assert.assertEquals(2, coreVocabulary.size());
    Assert.assertTrue("Expecting it to be the text I set it to before", coreVocabulary.get(1).getForeignLanguage().equalsIgnoreCase(SECOND));

    // better be first ex
    Assert.assertEquals(0, coreVocabulary.indexOf(exercise));

    // better be second ex
    Assert.assertEquals(1, coreVocabulary.indexOf(exercise2));


    coreVocabulary.forEach(clientExercise -> logger.info("2 ex " + clientExercise.getID() + " " + clientExercise.getForeignLanguage()));
    logger.info("Add new ex after " + id1);
    ClientExercise exercise3 = dialogDAO.addCoreVocab(toAdd, USERID, id1, System.currentTimeMillis());

    OOVWordsAndUpdate third = andPopulate.getExerciseDAO(PROJECTID).updateText(project, dialogID, exercise3.getID(), -1, "third");
    logger.info("got 3 " + third);
    Assert.assertTrue("Expecting it to update", third.isDidUpdate());


    // get it again
    toAdd = getiDialog(andPopulate, PROJECTID, dialogID);

    // better be only one at the end
    coreVocabulary = toAdd.getCoreVocabulary();
    Assert.assertEquals(3, coreVocabulary.size());
    coreVocabulary.forEach(clientExercise -> logger.info("3 ex " + clientExercise.getID() + " " + clientExercise.getForeignLanguage()));

//    Assert.assertTrue("Expecting it to be the text I set it to before", coreVocabulary.get(1).getForeignLanguage().equalsIgnoreCase(SECOND));

    // better be first ex
    Assert.assertEquals(0, coreVocabulary.indexOf(exercise));

    // better be second ex
    Assert.assertEquals(1, coreVocabulary.indexOf(exercise3));

    // better be third ex
    Assert.assertEquals(2, coreVocabulary.indexOf(exercise2));
  }

  @Test
  public void testNewDialogOps() {
    DatabaseImpl andPopulate = getDatabase();
    logger.warn("testNewDialogOps START ==--------- ");

    Project project = getProject(andPopulate);

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
      boolean b = dialogDAO.deleteExercise(PROJECTID, id, exid).size() == 1;
      if (!b) {
        logger.error("didn't delete the exercise " + exid);
      } else {
        logger.info("says it did delete " + exid);
      }
      Assert.assertTrue("should have deleted " + exid, b);

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
    logger.warn("testNewDialogOps END ==--------- ");
    andPopulate.close();

  }


  @Test
  public void testNewDialogOpsAlternate() {
    DatabaseImpl andPopulate = getDatabase();
    logger.warn("testNewDialogOpsAlternate START ==--------- ");

    Project project = getProject(andPopulate);

    //waitTillLoad();

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

    logger.warn("testNewDialogOpsAlternate END ==--------- ");

    andPopulate.close();

  }

  /**
   * TODO : also test left side right side stuff
   */
  @Test
  public void testNewIntepreterDialogOps() {
    DatabaseImpl andPopulate = getDatabase();
    logger.warn("testNewIntepreterDialogOps START ==--------- ");

    Project project = getProject(andPopulate);

    //waitTillLoad();

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
      // logger.info("lookup " + lookup);
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
      ClientExercise exByID = toAdd.getExByID(exid);

      logger.info("there are " + exercises.size() + " in dialog - last has exid " + exid + " found " + exByID);

      boolean b = dialogDAO.deleteExercise(PROJECTID, id, exid).size() == 2;

      if (!b) {
        logger.error("didn't delete the exercise " + exid);
      } else {
        logger.info("says it did delete " + exid);
      }
      Assert.assertTrue("should have deleted " + exid, b);

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

      boolean b = dialogDAO.deleteExercise(PROJECTID, toAdd.getID(), exid).size() == 2;
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
    logger.warn("testNewIntepreterDialogOps END ==--------- ");
    andPopulate.close();

  }


  /**
   * TODO : also test left side right side stuff
   */
  @Test
  public void testSimpleDelete() {
    DatabaseImpl andPopulate = getDatabase();
    logger.warn("testNewIntepreterDialogOps START ==--------- ");

    Project project = getProject(andPopulate);

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
      // logger.info("lookup " + lookup);
      if (!isFirst) {
        boolean bothEnglish = exercise.hasEnglishAttr() == prevEnglish;
        Assert.assertFalse(bothEnglish);
        isFirst = false;
      }
      prevEnglish = exercise.hasEnglishAttr();
    }

    // insert at end
    {
      int exid = exercises.get(exercises.size() - 1).getID();
      List<ClientExercise> clientExercises = dialogDAO.addEmptyExercises(toAdd, USERID, exid, true, System.currentTimeMillis());
      Assert.assertEquals(clientExercises.size(), 2);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
      Assert.assertEquals(4, exercises.size());

      ClientExercise added = clientExercises.get(0);

      // newly added exercise should be after the first one
      Assert.assertEquals(toAdd.getExercises().indexOf(added), toAdd.getExercises().size() - 2);

      toAdd.getExercises().forEach(logger::info);
    }

    // do delete!
    {
      int exid = exercises.get(exercises.size() - 1).getID();
      ClientExercise exByID = toAdd.getExByID(exid);

      logger.info("there are " + exercises.size() + " in dialog - last has exid " + exid + " found " + exByID);

      boolean b = dialogDAO.deleteExercise(PROJECTID, id, exid).size() == 2;

      if (!b) {
        logger.error("didn't delete the exercise " + exid);
      } else {
        logger.info("says it did delete " + exid);
      }
      Assert.assertTrue("should have deleted " + exid, b);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
    }

    logger.info("\n\n\nafter  has " + exercises.size());

    // after delete should have 0
    Assert.assertEquals(2, exercises.size());

    exercises.forEach(logger::info);

    logger.warn("testNewIntepreterDialogOps END ==--------- ");
    andPopulate.close();

  }

  /**
   * TODO : also test left side right side stuff
   */
  @Test
  public void testNewIntepreterLeftRightDialogOps() {
    DatabaseImpl andPopulate = getDatabase();
    logger.warn("testNewIntepreterLeftRightDialogOps START ==--------- ");

    Project project = getProject(andPopulate);

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
//      logger.info("new    " + exercise);
      CommonExercise lookup = project.getExerciseByID(exercise.getID());
      logger.info(id + " : lookup " + lookup);
      if (!isFirst) {
        boolean bothEnglish = exercise.hasEnglishAttr() == prevEnglish;
        Assert.assertFalse(bothEnglish);
        isFirst = false;
      }
      prevEnglish = exercise.hasEnglishAttr();
    }

    // do delete!
 /*   {
      int exid = exercises.get(exercises.size() - 1).getID();
      boolean b = dialogDAO.deleteExercise(PROJECTID, id, exid);
      if (!b) {
        logger.error("didn't delete the exercise " + exid);
      } else {
        logger.info("says it did delete " + exid);
      }

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
    }*/

//    logger.info("\n\n\nafter  has " + exercises.size());

    // after delete should have 0
    Assert.assertEquals(2, exercises.size());

    exercises.forEach(logger::info);

    // insert a couple on right
    {
      List<ClientExercise> clientExercises = dialogDAO.addEmptyExercises(toAdd, USERID, toAdd.getLastID(), false, System.currentTimeMillis());
      Assert.assertEquals(clientExercises.size(), 2);

      // speaker attrbute (why?) and language
      Assert.assertEquals(clientExercises.get(0).getAttributes().size(), 2);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();

      Assert.assertEquals(4, exercises.size());
      Assert.assertEquals(2, exercises.get(0).getAttributes().size());

      exercises.forEach(logger::info);

      Assert.assertTrue(toAdd.getLast().hasEnglishAttr());
    }

    // insert a couple on left
    {
      List<ClientExercise> clientExercises = dialogDAO.addEmptyExercises(toAdd, USERID, toAdd.getLastID(), true, System.currentTimeMillis());
      Assert.assertEquals(clientExercises.size(), 2);
      Assert.assertEquals(clientExercises.get(0).getAttributes().size(), 2);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
      Assert.assertEquals(6, exercises.size());

      ClientExercise added = clientExercises.get(0);

      // newly added exercise should be after the first one
      Assert.assertEquals(4, toAdd.getExercises().indexOf(added));

      toAdd.getExercises().forEach(exercise -> Assert.assertEquals(exercise.getAttributes().size(), 2));

      toAdd.getExercises().forEach(logger::info);

      Assert.assertFalse(toAdd.getLast().hasEnglishAttr());
    }

    // delete in the middle
    {
      ClientExercise toDelete = exercises.get(3);
      ClientExercise next = exercises.get(4);
      int exid = toDelete.getID();

      boolean b = dialogDAO.deleteExercise(PROJECTID, toAdd.getID(), exid).size() == 2;
      Assert.assertTrue(b);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      Assert.assertEquals(4, toAdd.getExercises().size());

      // deleted shouldn't be there
      Assert.assertEquals(-1, toAdd.getExercises().indexOf(toDelete));
      // next should be in place of deleted
      Assert.assertEquals(2, toAdd.getExercises().indexOf(next));

      toAdd.getExercises().forEach(logger::info);
    }
    logger.warn("testNewIntepreterLeftRightDialogOps END ==--------- ");
    andPopulate.close();
  }


  /**
   * TODO : also test left side right side stuff
   */
  @Test
  public void testDeleteNormalDialog() {
    DatabaseImpl andPopulate = getDatabase();
    logger.warn("testDeleteNormalDialog START ==--------- ");

    Project project = getProject(andPopulate);

    // do create!
    DialogType interpreter = DialogType.DIALOG;
    IDialog toAdd = addDialog(andPopulate, PROJECTID, interpreter);
    int id = toAdd.getID();
    logger.info("new dialog " + toAdd);
    IDialogDAO dialogDAO = andPopulate.getDialogDAO();

    List<ClientExercise> exercises = toAdd.getExercises();

    // now should have one exercise
    Assert.assertEquals(1, exercises.size());

    boolean prevEnglish = false;
    boolean isFirst = true;
    for (ClientExercise exercise : exercises) {
//      logger.info("new    " + exercise);
      CommonExercise lookup = project.getExerciseByID(exercise.getID());
      logger.info(id + " : lookup " + lookup);
      if (!isFirst) {
        boolean bothEnglish = exercise.hasEnglishAttr() == prevEnglish;
        Assert.assertFalse(bothEnglish);
        isFirst = false;
      }
      prevEnglish = exercise.hasEnglishAttr();
    }

    // do delete!
 /*   {
      int exid = exercises.get(exercises.size() - 1).getID();
      boolean b = dialogDAO.deleteExercise(PROJECTID, id, exid);
      if (!b) {
        logger.error("didn't delete the exercise " + exid);
      } else {
        logger.info("says it did delete " + exid);
      }

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
    }*/

//    logger.info("\n\n\nafter  has " + exercises.size());

    // after delete should have 0
    Assert.assertEquals(1, exercises.size());

    exercises.forEach(logger::info);

    // insert a couple on right
  /*  {
      List<ClientExercise> clientExercises = dialogDAO.addEmptyExercises(toAdd, USERID, toAdd.getLastID(), false, System.currentTimeMillis());
      Assert.assertEquals(2,clientExercises.size());

      // speaker attrbute (why?) and language
      Assert.assertEquals(clientExercises.get(0).getAttributes().size(), 2);

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();

      Assert.assertEquals(4, exercises.size());
      Assert.assertEquals(2, exercises.get(0).getAttributes().size());

      exercises.forEach(logger::info);

      Assert.assertTrue(toAdd.getLast().hasEnglishAttr());
    }*/

    // insert a couple on left
    {
      List<ClientExercise> clientExercises = dialogDAO.addEmptyExercises(toAdd, USERID, toAdd.getLastID(), true, System.currentTimeMillis());
      Assert.assertEquals(1, clientExercises.size());
      Assert.assertEquals(2, clientExercises.get(0).getAttributes().size());

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
      Assert.assertEquals(2, exercises.size());

      ClientExercise added = clientExercises.get(0);

      // newly added exercise should be after the first one
      Assert.assertEquals(1, toAdd.getExercises().indexOf(added));

      toAdd.getExercises().forEach(exercise -> Assert.assertEquals(exercise.getAttributes().size(), 2));

      toAdd.getExercises().forEach(logger::info);

      Assert.assertFalse(toAdd.getLast().hasEnglishAttr());
    }

    // delete last
    {
      ClientExercise toDelete = exercises.get(exercises.size() - 1);
      ClientExercise prev = exercises.get(0);
      int exid = toDelete.getID();

      boolean b = dialogDAO.deleteExercise(PROJECTID, toAdd.getID(), exid).size() == 1;
      Assert.assertTrue(b);

      logger.info("reload the dialog " + id);
      toAdd = getiDialog(andPopulate, PROJECTID, id);
      Assert.assertEquals(1, toAdd.getExercises().size());

      // deleted shouldn't be there
      Assert.assertEquals(-1, toAdd.getExercises().indexOf(toDelete));
      // next should be in place of deleted
      Assert.assertEquals(0, toAdd.getExercises().indexOf(prev));

      toAdd.getExercises().forEach(logger::info);
    }

    // if we put it back, we should be back to where we were before the delete
    {
      List<ClientExercise> clientExercises = dialogDAO.addEmptyExercises(toAdd, USERID, toAdd.getLastID(), true, System.currentTimeMillis());
      Assert.assertEquals(1, clientExercises.size());
      Assert.assertEquals(2, clientExercises.get(0).getAttributes().size());

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
      Assert.assertEquals(2, exercises.size());

      ClientExercise added = clientExercises.get(0);

      // newly added exercise should be after the first one
      Assert.assertEquals(1, toAdd.getExercises().indexOf(added));

      toAdd.getExercises().forEach(exercise -> Assert.assertEquals(exercise.getAttributes().size(), 2));

      toAdd.getExercises().forEach(logger::info);

      Assert.assertFalse(toAdd.getLast().hasEnglishAttr());
    }

    {
      List<ClientExercise> clientExercises = dialogDAO.addEmptyExercises(toAdd, USERID, toAdd.getLastID(), false, System.currentTimeMillis());
      Assert.assertEquals(1, clientExercises.size());
      Assert.assertEquals(2, clientExercises.get(0).getAttributes().size());

      toAdd = getiDialog(andPopulate, PROJECTID, id);
      exercises = toAdd.getExercises();
      Assert.assertEquals(3, exercises.size());

      ClientExercise added = clientExercises.get(0);

      // newly added exercise should be after the first one
      Assert.assertEquals(2, toAdd.getExercises().indexOf(added));

      toAdd.getExercises().forEach(exercise -> Assert.assertEquals(exercise.getAttributes().size(), 2));

      toAdd.getExercises().forEach(logger::info);

      Assert.assertFalse(toAdd.getLast().hasEnglishAttr());
    }

    // delete in the middle
    {
      ClientExercise toDelete = exercises.get(1);
      ClientExercise prev = exercises.get(0);
      ClientExercise next = exercises.get(2);
      int exid = toDelete.getID();

      boolean b = dialogDAO.deleteExercise(PROJECTID, toAdd.getID(), exid).size() == 1;
      Assert.assertTrue(b);

      logger.info("reload the dialog " + id);
      toAdd = getiDialog(andPopulate, PROJECTID, id);
      Assert.assertEquals(2, toAdd.getExercises().size());

      // deleted shouldn't be there
      Assert.assertEquals(-1, toAdd.getExercises().indexOf(toDelete));
      // next should be in place of deleted
      Assert.assertEquals(0, toAdd.getExercises().indexOf(prev));
      Assert.assertEquals(1, toAdd.getExercises().indexOf(next));

      toAdd.getExercises().forEach(logger::info);
    }

    // delete last
    {
      ClientExercise toDelete = exercises.get(exercises.size() - 1);
      ClientExercise prev = exercises.get(0);
      int exid = toDelete.getID();

      boolean b = dialogDAO.deleteExercise(PROJECTID, toAdd.getID(), exid).size() == 1;
      Assert.assertTrue(b);

      logger.info("reload the dialog " + id);
      toAdd = getiDialog(andPopulate, PROJECTID, id);
      Assert.assertEquals(1, toAdd.getExercises().size());

      // deleted shouldn't be there
      Assert.assertEquals(-1, toAdd.getExercises().indexOf(toDelete));
      // next should be in place of deleted
      Assert.assertEquals(0, toAdd.getExercises().indexOf(prev));

      toAdd.getExercises().forEach(logger::info);
    }


    logger.warn("testDeleteNormalDialog END ==--------- ");
    andPopulate.close();
  }

  @NotNull
  private Dialog addDialog(DatabaseImpl andPopulate, int projectid, DialogType dialog) {
    // DialogType dialog = DialogType.DIALOG;
    Dialog toAdd = new Dialog(-1,
        USERID,
        projectid,
        -1,
        System.currentTimeMillis(),
        "1", "1", "orient", "", "fl Wednesday", "en",
        new ArrayList<>(),
        new ArrayList<>(),
        new ArrayList<>(),
        dialog,
        "us", true
    );

    andPopulate.getDialogDAO().add(toAdd, getLanguage(andPopulate, projectid));
    return toAdd;
  }

  private String getLanguage(DatabaseImpl andPopulate, int projectid) {
    return andPopulate.getProject(projectid).getLanguage();
  }

  @Test
  public void testNewDialogAndInsert() {
    DatabaseImpl andPopulate = getDatabase();
    logger.warn("testNewDialogAndInsert START ==--------- ");


    Project project = getProject(andPopulate);

    Dialog toAdd = new Dialog(-1,
        6,
        PROJECTID,
        -1,
        System.currentTimeMillis(),
        "1", "1", "orient", "", "fl Friday", "en",
        new ArrayList<>(),
        new ArrayList<>(),
        new ArrayList<>(),
        DialogType.DIALOG,
        "us", true
    );


    andPopulate.getDialogDAO().add(toAdd, getLanguage(andPopulate, PROJECTID));

    for (ClientExercise exercise : toAdd.getExercises()) {
      logger.info("new " + exercise);
      CommonExercise lookup = project.getExerciseByID(exercise.getID());
      logger.info("lookup " + lookup);
    }
    logger.warn("testNewDialogAndInsert END ==--------- ");
    andPopulate.close();

  }

  @Test
  public void testGetNewDialog() {
    logger.warn("testGetNewDialog START ==--------- ");
    DatabaseImpl andPopulate = getDatabase();
    Project project = andPopulate.getProject(PROJECTID, true);
    project.getDialogs().forEach(logger::info);
    logger.warn("testGetNewDialog END ==--------- ");
    andPopulate.close();

  }

  @Ignore
  @Test
  public void testGetDialog() {
    logger.warn("testGetDialog START ==--------- ");
    DatabaseImpl andPopulate = getDatabase();
    Project project = getProject(andPopulate);

    List<IDialog> collect = project.getDialogs().stream().filter(dialog -> dialog.getID() == 83).collect(Collectors.toList());

    IDialog iDialog = collect.get(0);

    logger.info("new dialog " + iDialog);

    Assert.assertEquals(iDialog.getExercises().size(), 1);
    logger.warn("testGetDialog END ==--------- ");
    andPopulate.close();

  }

  @Ignore
  public void testGetExercisesDialog() {
    DatabaseImpl andPopulate = getDatabase();
    logger.warn("testGetExercisesDialog START ==--------- ");
    int projectid = PROJECTID;
    int i = 32;

    Project project = getProject(andPopulate);

    IDialog iDialog = getiDialog(andPopulate, projectid, i);
    logger.info("before has " + iDialog.getExercises().size());
    iDialog.getExercises().forEach(logger::info);
    andPopulate.close();
  }

  @Ignore
  @Test
  public void testGetExercisesDialog2() {
    DatabaseImpl andPopulate = getDatabase();
    int projectid = PROJECTID;
    int i = 33;

    Project project = getProject(andPopulate);

    IDialog iDialog = getiDialog(andPopulate, projectid, i);
    logger.info("before has " + iDialog.getExercises().size());
    iDialog.getExercises().forEach(logger::info);
    andPopulate.close();

  }

  @Test
  public void testChangeDialog() {
    DatabaseImpl andPopulate = getDatabase();
    logger.warn("testChangeDialog START ==--------- ");

    Project project = getProject(andPopulate);

    IDialog iDialog = project.getLastDialog();
    {
      logger.info("before dialog " + iDialog.getID() + " : " + iDialog.isPrivate());
      iDialog.getMutable().setIsPrivate(false);
    }

    {
      andPopulate.getDialogDAO().update(iDialog);

      iDialog = project.getLastDialog();
      logger.info("after dialog " + iDialog.getID() + " : " + iDialog.isPrivate());
    }

    Assert.assertFalse(iDialog.isPrivate());

    {
      logger.info("2 before dialog " + iDialog.getID() + " : " + iDialog.isPrivate());
      iDialog.getMutable().setIsPrivate(true);
    }


    {
      andPopulate.getDialogDAO().update(iDialog);

      iDialog = project.getLastDialog();
      logger.info("2 after dialog " + iDialog.getID() + " : " + iDialog.isPrivate());
    }

    Assert.assertTrue(iDialog.isPrivate());
    logger.warn("testChangeDialog END ==--------- ");
    andPopulate.close();

  }

  @Test
  public void testInsertExAtFrontDialog() {
    DatabaseImpl andPopulate = getDatabase();
    logger.warn("testInsertAtFrontDialog START ==--------- ");
    int projectid = PROJECTID;

    Project project = getProject(andPopulate);
    IDialog last = project.getLastDialog();
    int id = last.getID();
    IDialog iDialog = getiDialog(andPopulate, projectid, id);

    doInsert(andPopulate, projectid, id, iDialog);
    logger.warn("testInsertAtFrontDialog END ==--------- ");
    andPopulate.close();
  }

/*
  @Test
  public void testInsertAtFrontDialog2() {
    DatabaseImpl andPopulate = getDatabase();
    int projectid = PROJECTID;
  //  int i = 32;
    Project project = andPopulate.getProject(projectid, true);
    IDialog last = project.getLastDialog();

    andPopulate.waitForSetupComplete();

    waitTillLoad();

    IDialog iDialog = getiDialog(andPopulate, projectid, i);

    doInsert(andPopulate, projectid, i, iDialog);
  }
*/

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

/*  @Test
  public void testDeleteEx() {
    DatabaseImpl andPopulate = getDatabase();
    int projectid = PROJECTID;
    andPopulate.getProject(projectid, true);

    andPopulate.waitForSetupComplete();

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
  }*/

  @Test
  public void testInterpreterStored() {
    logger.warn("testInterpreterStored START ==--------- ");
    DatabaseImpl andPopulate = getDatabase();
    Project project = getProject(andPopulate);
    report(andPopulate, project);
    logger.warn("testInterpreterStored END ==--------- ");
    andPopulate.close();

  }

  @Test
  public void testInterpreterFrenchToRecord() {
    DatabaseImpl andPopulate = getDatabase();
    logger.warn("testInterpreterFrenchToRecord START ==--------- ");
    Project project = getProject(andPopulate);

//    Project project = andPopulate.getProjectManagement().getProductionByLanguage(Language.FRENCH);
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
    logger.warn("testInterpreterFrenchToRecord END ==--------- ");
    andPopulate.close();

  }

  @Test
  public void testInterpreterFrench() {
    DatabaseImpl andPopulate = getDatabase();
    logger.warn("testInterpreterFrench START ==--------- ");
    Project project = getProject(andPopulate);
    //   Project project = andPopulate.getProjectManagement().getProductionByLanguage(Language.FRENCH);
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
    logger.warn("testInterpreterFrench END ==--------- ");
    andPopulate.close();

  }


  @Test
  public void testNormalFrench() {
    DatabaseImpl andPopulate = getDatabase();
    logger.warn("testNormalFrench START ==--------- ");

    Project project = getProject(andPopulate);
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
    logger.warn("testNormalFrench END ==--------- ");
    andPopulate.close();

  }

  private FilterResponse getTypeToValues(DatabaseImpl andPopulate, int projectid, FilterRequest request) {
    return andPopulate.getFilterResponseHelper().getTypeToValues(request, projectid, USERID);
  }

  @Test
  public void testInterpreterRecord() {
    DatabaseImpl andPopulate = getDatabase();
    logger.warn("testInterpreterRecord START ==--------- ");
    int projectid = PROJECTID;
    Project project = getProject(andPopulate);

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

    logger.warn("testInterpreterRecord END ==--------- ");
    andPopulate.close();

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
}
