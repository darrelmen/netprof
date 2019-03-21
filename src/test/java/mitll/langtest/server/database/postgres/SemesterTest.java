package mitll.langtest.server.database.postgres;

import mitll.langtest.server.database.BaseTest;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.exercise.ITestSection;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.project.IProjectManagement;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.SectionNode;
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
  }
}
