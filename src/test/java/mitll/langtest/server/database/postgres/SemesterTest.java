package mitll.langtest.server.database.postgres;

import mitll.langtest.server.database.BaseTest;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.exercise.ITestSection;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.project.IProjectManagement;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.scoring.TextNormalizer;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.SectionNode;
import mitll.langtest.shared.project.OOVInfo;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

public class SemesterTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(SemesterTest.class);

  @Test
  public void testGerman() {
    DatabaseImpl andPopulate = getDatabase().setInstallPath("", false);
    IProjectManagement projectManagement = andPopulate.getProjectManagement();
    Project project = projectManagement.getProject(10, true);

//    project.getSectionHelper().report();

    ISection<CommonExercise> sectionHelper = project.getSectionHelper();
    List<String> typeOrder = sectionHelper.getTypeOrder();
    typeOrder.forEach(type -> logger.info("Got type " + type));

    logger.info("sections " + sectionHelper.getSectionNodesForTypes());
    // SectionNode unit = sectionHelper.getFirstNode("1");
    ITestSection<CommonExercise> tsectionHelper = (ITestSection<CommonExercise>) project.getSectionHelper();

    SectionNode root = tsectionHelper.getRoot();

    logger.info("Root is " + root.getType() + " " + root.getName() + " " + root);
    SectionNode unit = tsectionHelper.getNode(root, "Semester", "1");
    logger.info("got " + unit);

    unit = tsectionHelper.getNode(root, "Semester", "2");
    logger.info("got " + unit);

    unit = tsectionHelper.getNode(root, "Semester", "3");
    logger.info("got " + unit);

    Collection<CommonExercise> semester = tsectionHelper.getExercisesForSelectionState("Semester", "1");
    logger.info("Got " + semester.size());

    semester = tsectionHelper.getExercisesForSelectionState("Semester", "2");
    logger.info("Got " + semester.size());

    semester = tsectionHelper.getExercisesForSelectionState("Semester", "3");
    logger.info("Got " + semester.size());

    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
//    andPopulate.getUserDAO().getUsers();

    int admin = 6;
    //  addRemove(andPopulate, admin);
    addRemove(andPopulate, 738);
  }

  @Test
  public void testTeacher() {
    DatabaseImpl andPopulate = getDatabase().setInstallPath("", false);
    IProjectManagement projectManagement = andPopulate.getProjectManagement();
    Project project = projectManagement.getProject(10, true);

    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
//    andPopulate.getUserDAO().getUsers();

    int admin = 6;
    //  addRemove(andPopulate, admin);
    addRemove(andPopulate, 738);
  }


  @Test
  public void testNorm() {

    TextNormalizer textNormalizer = new TextNormalizer("");

    String s = textNormalizer.fromFull("Rome is about 4,500 mi. away from here.");

    logger.info("now " + s);

    String f="１９９０";

    s = textNormalizer.fromFull(f);

    logger.info("was  " +f+
        "" +
        "now " + s);
  }



  @Test
  public void testOOV() {
    DatabaseImpl andPopulate = getDatabase().setInstallPath("", false);
    IProjectManagement projectManagement = andPopulate.getProjectManagement();
    int projectid = 15;
    Project project = projectManagement.getProject(projectid, true);

    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
//    andPopulate.getUserDAO().getUsers();

    CommonExercise exerciseByID = project.getExerciseByID(128430);

    boolean safeToDecode = exerciseByID.isSafeToDecode();

    logger.info("Safe " + safeToDecode + " " + exerciseByID);

    OOVInfo oovInfo = andPopulate.getProjectManagement().checkOOV(projectid);

    logger.info("oovInfo " + oovInfo);
  }

  private void addRemove(DatabaseImpl andPopulate, int admin) {
    IUserDAO userDAO = andPopulate.getUserDAO();
    User userWhere = userDAO.getUserWhere(admin);
    Collection<User.Permission> permissions = userWhere.getPermissions();
    logger.info("permissions " + permissions);
    boolean b = userDAO.removeTeacherRole(admin);
    logger.info("remove " + b);

    userWhere = userDAO.getUserWhere(admin);
    permissions = userWhere.getPermissions();
    logger.info("after permissions " + permissions);

    b = userDAO.addTeacherRole(admin);
    logger.info("add " + b);

    userWhere = userDAO.getUserWhere(admin);
    permissions = userWhere.getPermissions();
    logger.info("after add permissions " + permissions);
  }
}
