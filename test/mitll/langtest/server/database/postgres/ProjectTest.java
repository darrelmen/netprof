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

package mitll.langtest.server.database.postgres;

import mitll.langtest.server.database.BaseTest;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.server.database.project.ProjectStatus;
import mitll.langtest.server.database.project.ProjectType;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.database.user.UserManagement;
import mitll.langtest.server.database.userexercise.ExercisePhoneInfo;
import mitll.langtest.server.database.userexercise.ExerciseToPhone;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.SlickProject;
import mitll.npdata.dao.SlickProjectProperty;
import org.apache.logging.log4j.*;
import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ProjectTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(ProjectTest.class);
  public static final int MAX = 200;

  @Test
  public void testProject() {
    DatabaseImpl spanish = getDatabase("spanish");
    IProjectDAO projectDAO = spanish.getProjectDAO();
    User gvidaver = spanish.getUserDAO().getUserByID("gvidaver");

    Iterator<String> iterator = spanish.getTypeOrder(-1).iterator();
    projectDAO.add(
        gvidaver.getID(),
        System.currentTimeMillis(),
        "my Spanish",
        spanish.getLanguage(),
        "ALL",
        ProjectType.NP,
        ProjectStatus.PRODUCTION, iterator.next(), iterator.next(), "es", 0);
  }

  @Test
  public void testTestProject() {
    DatabaseImpl database = getDatabase("netProf");

    IProjectDAO projectDAO = database.getProjectDAO();

    IUserDAO userDAO = database.getUserDAO();
    User gvidaver = userDAO.getUserByID("gvidaver");

    int i = projectDAO.addTest(
        gvidaver.getID(),
        "my Spanish",
        "Spanish",
        "Unit", "Chapter", "es");

    IUserListManager userListManager = database.getUserListManager();

    UserManagement userManagement = database.getUserManagement();
    String test345 = "test345";
//    User user = userManagement.addUser(test345, "test123", "1234", "", "", "");
//    if (user == null) {
//      user = userDAO.getUserByID(test345);
//    }
//    if (user != null) {
//      Collection<UserList<CommonShell>> myLists = userListManager.getMyLists(user.getID(), i);
//      logger.info("lists for " + user + " " + myLists);
//    }
  }

  @Test
  public void testListProjects() {
    DatabaseImpl spanish = getDatabase("spanish");

    IProjectDAO projectDAO = spanish.getProjectDAO();
    Collection<SlickProject> all = projectDAO.getAll();
    for (SlickProject project : all) {
      logger.info("Got " + project);
      for (SlickProjectProperty prop : project.getProps()) {
        logger.info("\t prop " + prop);
      }
    }
  }

  @Test
  public void testAddProperty() {
    DatabaseImpl spanish = getDatabaseVeryLight("spanish", "quizlet.properties", false);

    IProjectDAO projectDAO = spanish.getProjectDAO();
    SlickProject next = projectDAO.getAll().iterator().next();

    projectDAO.addProperty(next.id(), "key", "value");

    testListProjects();
//    next.addProp(new SlickProjectProperty(-1, new Timestamp(System.currentTimeMillis()), next.id(), "test", "test"));
  }

  @Test
  public void testByName() {
    DatabaseImpl spanish = getDatabaseVeryLight("netProf", "config.properties", false);
    IProjectDAO projectDAO = spanish.getProjectDAO();

    String english1 = "english";
    int english = projectDAO.getByName(english1);

    logger.info("found " + english + " for " + english1);
  }

  @Test
  public void testPhones() {
    DatabaseImpl spanish = getDatabaseVeryLight("netProf", "config.properties", false);
    IProjectDAO projectDAO = spanish.getProjectDAO();

    String english1 = "english";
    int english = projectDAO.getByName(english1);

    logger.info("found " + english + " for " + english1);
  }

  @Test
  public void testOneEditNeighbors() {
    DatabaseImpl spanish = getDatabaseVeryLight("netProf", "config.properties", false);
    IUserDAO userDAO = spanish.getUserDAO();
    logger.info("counts " + userDAO + " " + userDAO.getUsers().size());

    spanish.populateProjects();
    spanish.setInstallPath("", "", "");

    Project project = spanish.getProject(2);

    List<CommonExercise> rawExercises = project.getRawExercises();

    int i = 0;

    for (CommonExercise exercise : rawExercises) {
      ExercisePhoneInfo exercisePhoneInfo = project.getExToPhone().get(exercise.getID());
      if (exercisePhoneInfo != null) {
        Map<String, ExerciseToPhone.Info> wordToInfo = exercisePhoneInfo.getWordToInfo();
        logger.info("for " + exercise.getID() + " : " + exercise.getForeignLanguage() + " " + wordToInfo);

        if (wordToInfo != null) {
          for (Map.Entry<String, ExerciseToPhone.Info> pair : wordToInfo.entrySet()) {
            String pron = pair.getKey();
            ExerciseToPhone.Info value = pair.getValue();
            Map<String, Integer> pronToCount = value.getPronToCount();
            logger.info("\t" + pron + " = " + value.getPronToInfo());// + " (" +pronToCount.get(pron)+ ")");
            logger.info("\t" + pron + " = " + pronToCount);
          }
        }

        if (i++ > MAX) break;
      }
    }

/*    List<String> tests = Arrays.asList("l", "la", "las", "s", "se", "seg", "ak");
    for (String test : tests) {
      Collection<CommonExercise> matches = phoneTrie.getMatches(test);
      logger.info("for " + test + " got " + matches.size());
      int i = 0;
      for (CommonExercise exercise : matches) {
        logger.info("found " + test + " : " + exercise.getForeignLanguage());
        if (i++ > 10) break;
      }
    }*/
  }

  @Test
  public void testAgain() {
    DatabaseImpl spanish = getDatabaseVeryLight("netProf", "config.properties", false);
    IProjectDAO projectDAO = spanish.getProjectDAO();

    String english1 = "mandarin";
    int english = projectDAO.getByName(english1);

    logger.info("found " + english + " for " + english1);

    spanish.populateProjects();

    spanish.setInstallPath("", "", "");

    Project project = spanish.getProject(english);

    logger.info("project " + project);

    project.getSectionHelper().report();
  }

  @Test
  public void testListProjects2() {
    IProjectDAO projectDAO = getDatabaseVeryLight("netProf", "config.properties", false).getProjectDAO();
    projectDAO.getAll().stream().forEach(p -> logger.info("projec " + p));
    //  projectDAO.delete(14);
  }

  @Test
  public void testDrop() {
    IProjectDAO projectDAO = getDatabaseVeryLight("netProf", "config.properties", false).getProjectDAO();
    projectDAO.delete(6);
    //  projectDAO.delete(14);
  }

  @Test
  public void testMaleFemale() {
    DatabaseImpl netProf = getDatabaseVeryLight("netProf", "config.properties", false);
    netProf.populateProjects();
    netProf.setInstallPath("", "", "");

    Map<String, Float> maleFemaleProgress = netProf.getMaleFemaleProgress(2);

    logger.info(maleFemaleProgress.toString());

  }
}
