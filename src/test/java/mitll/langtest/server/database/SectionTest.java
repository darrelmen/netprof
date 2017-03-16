package mitll.langtest.server.database;

import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.shared.exercise.Pair;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.SectionNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.*;

/**
 * Created by go22670 on 3/9/17.
 */
public class SectionTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(SectionTest.class);

  @Test
  public void testSectionSpanish() {
    doReport("spanish");
  }

  @Test
  public void testSectionPashto() {
    doReport("pashto");
  }

  @Test
  public void testAgain() {
    SectionHelper<CommonExercise> sectionHelper = new SectionHelper<>();

    addData(sectionHelper);
    sectionHelper.rememberTypes();

    logger.info("root " + sectionHelper.getRoot());

    Collection<SectionNode> sectionNodesForTypes = sectionHelper.getSectionNodesForTypes();
//    Map<String, String> map = new LinkedHashMap<>();
    for (SectionNode child : sectionNodesForTypes) {
      //forChild(sectionHelper, child, map, 0);
      logger.info("child " + child.getType() + " " + child.getName());
    }

    logger.info("type order " + sectionHelper.getTypeOrder());
    logger.info("type to sections " + sectionHelper.getTypeToDistinct());

    Map<String, String> test = new HashMap<>();
    test.put("Unit", "1");
    Map<String, Set<String>> unit = sectionHelper.getTypeToMatches("Unit", "1");
    logger.info("match for " + test + " is " + unit);


    test = new HashMap<>();
    test.put("Unit", "2");
    unit = sectionHelper.getTypeToMatches("Unit", "2");
    logger.info("match for " + test + " is " + unit);

    test = new HashMap<>();
    test.put("Chapter", "a");
    unit = sectionHelper.getTypeToMatches("Chapter", "a");
    logger.info("match for " + test + " is " + unit);

    test = new HashMap<>();
    test.put("Chapter", "b");
    unit = sectionHelper.getTypeToMatches("Chapter", "b");
    logger.info("match for " + test + " is " + unit);

    test = new HashMap<>();
    test.put("Chapter", "dude");
    unit = sectionHelper.getTypeToMatches("Chapter", "dude");
    logger.info("match for " + test + " is " + unit);

    test = new HashMap<>();
    test.put("Unit", "1");
    test.put("Chapter", "a");

    List<Pair> toMatch = new ArrayList<>();
    toMatch.add(new Pair("Unit", "1"));
    toMatch.add(new Pair("Chapter", "a"));
    unit = sectionHelper.getTypeToMatches(toMatch);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(new Pair("Unit", "2"));
    toMatch.add(new Pair("Chapter", "a"));
    unit = sectionHelper.getTypeToMatches(toMatch);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(new Pair("Unit", "2"));
    toMatch.add(new Pair("Chapter", "a"));
    toMatch.add(new Pair("Sound", "x"));
    unit = sectionHelper.getTypeToMatches(toMatch);
    logger.info("match for " + toMatch + " is " + unit);


    toMatch = new ArrayList<>();
    toMatch.add(new Pair("Unit", "1"));
    toMatch.add(new Pair("Chapter", "a"));
    toMatch.add(new Pair("Sound", "x"));
    unit = sectionHelper.getTypeToMatches(toMatch);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(new Pair("Unit", "2"));
    //  toMatch.add(new Pair("Chapter", "a"));
    unit = sectionHelper.getTypeToMatches(toMatch);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(new Pair("Unit", "all"));
    toMatch.add(new Pair("Chapter", "a"));
    toMatch.add(new Pair("Sound", "all"));

    //  toMatch.add(new Pair("Chapter", "a"));
    unit = sectionHelper.getTypeToMatches(toMatch);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(new Pair("Unit", "all"));
    toMatch.add(new Pair("Chapter", "b"));
    toMatch.add(new Pair("Sound", "all"));

    //  toMatch.add(new Pair("Chapter", "a"));
    unit = sectionHelper.getTypeToMatches(toMatch);
    logger.info("match for " + toMatch + " is " + unit);
  }

/*
  private void forChild2(ISection<CommonExercise> sectionHelper, SectionNode type, Map<String, String> map, int level) {
    //for (SectionNode type : sectionNodesForTypes) {
    Map<String, String> map2 = new LinkedHashMap<>();
    map2.putAll(map);
    map2.put(type.getProperty(), type.getName());
    //Collection<CommonExercise> exercisesForSelectionState = sectionHelper.getExercisesForSimpleSelectionState(map2);
    String s = getDepth(level);
    logger.info(s + " for " + type.getProperty() + " " + type.getName() + " (" + map2 +
        ") " + exercisesForSelectionState.size());

    for (SectionNode sectionNode : type.getChildren()) {
      forChild(sectionHelper, sectionNode, map2, level + 1);
    }
//    forChildren(sectionHelper, type, map2);
    //}
  }
*/

  @Test
  public void testOrder() {
    SectionHelper<CommonExercise> sectionHelper = new SectionHelper<>();
    addData(sectionHelper);
    sectionHelper.rememberTypes(Arrays.asList("Sound", "Chapter", "Unit"));
    Collection<SectionNode> sectionNodesForTypes = sectionHelper.getSectionNodesForTypes();

    logger.info("root " + sectionHelper.getRoot());
    Map<String, String> map = new LinkedHashMap<>();
    for (SectionNode child : sectionNodesForTypes) {
      forChild(sectionHelper, child, map, 0);
    }
  }

  @Test
  public void testOrder2() {
    SectionHelper<CommonExercise> sectionHelper = new SectionHelper<>();
    addData(sectionHelper);
    sectionHelper.rememberTypes(Arrays.asList("Unit", "Chapter"));
    Collection<SectionNode> sectionNodesForTypes = sectionHelper.getSectionNodesForTypes();

    logger.info("root " + sectionHelper.getRoot());
    Map<String, String> map = new LinkedHashMap<>();
    for (SectionNode child : sectionNodesForTypes) {
      forChild(sectionHelper, child, map, 0);
    }
  }

  private void addData(SectionHelper<CommonExercise> sectionHelper) {
    List<Pair> pairs = new ArrayList<>();

    pairs.add(new Pair("Unit", "1"));
    pairs.add(new Pair("Chapter", "a"));

    List<Pair> copy = new ArrayList<>(pairs);
    Pair ng = new Pair("Sound", "ng");
    copy.add(ng);
    sectionHelper.rememberPairs(copy);

    Pair r = new Pair("Sound", "r");
    pairs.add(r);

    sectionHelper.rememberPairs(pairs);

    pairs = new ArrayList<>();
    pairs.add(new Pair("Unit", "1"));
    pairs.add(new Pair("Chapter", "b"));
    pairs.add(ng);

    sectionHelper.rememberPairs(pairs);

    pairs = new ArrayList<>();
    pairs.add(new Pair("Unit", "1"));
    pairs.add(new Pair("Chapter", "c"));
    pairs.add(ng);

    sectionHelper.rememberPairs(pairs);

    pairs = new ArrayList<>();
    pairs.add(new Pair("Unit", "2"));
    pairs.add(new Pair("Chapter", "a"));
    pairs.add(ng);
    sectionHelper.rememberPairs(pairs);

    pairs = new ArrayList<>();
    pairs.add(new Pair("Unit", "2"));
    pairs.add(new Pair("Chapter", "a"));
    pairs.add(new Pair("Sound", "x"));
    sectionHelper.rememberPairs(pairs);

    pairs = new ArrayList<>();
    pairs.add(new Pair("Unit", "2"));
    pairs.add(new Pair("Chapter", "b"));

    List<Pair> copy2 = new ArrayList<>(pairs);
    List<Pair> copy3 = new ArrayList<>(pairs);
    List<Pair> copy4 = new ArrayList<>(pairs);

    copy2.add(ng);
    sectionHelper.rememberPairs(copy2);

    copy3.add(r);
    sectionHelper.rememberPairs(copy3);
    copy4.add(new Pair("Sound", "t"));
    sectionHelper.rememberPairs(copy4);

/*
    pairs = new ArrayList<>();
    pairs.add(new Pair("Unit", "3"));
    pairs.add(new Pair("Chapter", "a"));
    pairs.add(new Pair("Sound", "ng"));

    sectionHelper.rememberPairs(pairs);
    pairs = new ArrayList<>();
    pairs.add(new Pair("Unit", "4"));
    pairs.add(new Pair("Chapter", "a"));
    pairs.add(new Pair("Sound", "ng"));

    sectionHelper.rememberPairs(pairs);*/

  }

  @Test
  public void testSection2() {
    DatabaseImpl andPopulate = getAndPopulate();
    IProjectDAO projectDAO = andPopulate.getProjectDAO();
    int byLanguage = projectDAO.getByLanguage("spanish");
    Project project = andPopulate.getProject(byLanguage);

    ISection<CommonExercise> sectionHelper = project.getSectionHelper();
    SectionNode unit = sectionHelper.getFirstNode("2");
    logger.info("Got " + unit);
    SectionNode chapter = sectionHelper.getNode(unit, "Chapter", "2");

    if (chapter != null) {
      logger.info("Got " + chapter);
      SectionNode sound = sectionHelper.getNode(chapter, "Sound", "a");
      logger.info("Got " + sound);
      SectionNode sound2 = sectionHelper.getNode(chapter, "Sound", "ng");
      logger.info("Got " + sound2);
    }

    SectionNode root = sectionHelper.getRoot();
    logger.info("Got " + root);
    logger.info("depth " + root.count());
  }

  private void doReport(String croatian) {
    DatabaseImpl andPopulate = getAndPopulate();
    IProjectDAO projectDAO = andPopulate.getProjectDAO();
    int byLanguage = projectDAO.getByLanguage(croatian);
    Project project = andPopulate.getProject(byLanguage);
    if (project == null) {
      logger.error("no project " + byLanguage);
      return;
    }

    ISection<CommonExercise> sectionHelper = project.getSectionHelper();

    // sectionHelper.report();

    List<String> typeOrder = sectionHelper.getTypeOrder();
    logger.info("type order " + typeOrder);
    Collection<SectionNode> sectionNodesForTypes = sectionHelper.getSectionNodesForTypes();

 /*
    Map<String, String> map = new LinkedHashMap<>();
    for (SectionNode type : sectionNodesForTypes) {
      forChild(sectionHelper, type, map, 0);
    }
    */

    logger.info("root " + sectionHelper.getRoot());
    logger.info("root children " + sectionHelper.getRoot().getChildren());
    logger.info("initial values " + sectionHelper.getTypeToDistinct());

    SectionNode firstNode = sectionHelper.getFirstNode("1");
    logger.info("Got first node for 1 " + firstNode);
    if (firstNode != null) {
      firstNode = sectionHelper.getNode(firstNode, "Chapter", "1");
      logger.info("Got " + firstNode);
    }
    if (firstNode != null) {
      firstNode = sectionHelper.getNode(firstNode, "Sound", "b");
      logger.info("Got " + firstNode);
    }
    Collection<CommonExercise> unit = sectionHelper.getExercisesForSelectionState("Unit", "1");
    logger.info("found " + unit.size());

    Map<String, Collection<String>> search = new HashMap<>();
    search.put("Unit", Collections.singleton("1"));

    unit = sectionHelper.getExercisesForSelectionState(search);
    logger.info("for " + search + " found " + unit.size());

    search.put("Chapter", Collections.singleton("1"));
    unit = sectionHelper.getExercisesForSelectionState(search);
    logger.info("for " + search + " found " + unit.size());

    search.put("Sound", Collections.singleton("b"));
    unit = sectionHelper.getExercisesForSelectionState(search);
    logger.info("for " + search + " found " + unit.size());

    search.put("Difficulty", Collections.singleton("7"));
    unit = sectionHelper.getExercisesForSelectionState(search);
    logger.info("for " + search + " found " + unit.size());

    ArrayList<Pair> toMatch = new ArrayList<>();
    toMatch.add(new Pair("Unit", "1"));
    //toMatch.add(new Pair("Chapter", "a"));
    //toMatch.add(new Pair("Sound", "x"));
    Map<String, Set<String>> typeToMatches = sectionHelper.getTypeToMatches(toMatch);
    logger.info("match for " + toMatch + " is " + typeToMatches);

    toMatch = new ArrayList<>();
    toMatch.add(new Pair("Unit", "1"));
    //toMatch.add(new Pair("Chapter", "a"));
    //toMatch.add(new Pair("Sound", "x"));
    typeToMatches = sectionHelper.getTypeToMatches(toMatch);
    logger.info("match for " + toMatch + " is " + typeToMatches);

    toMatch = new ArrayList<>();
    toMatch.add(new Pair("Unit", "1"));
    toMatch.add(new Pair("Topic", "Basics"));
    //toMatch.add(new Pair("Sound", "x"));
    typeToMatches = sectionHelper.getTypeToMatches(toMatch);
    logger.info("match for " + toMatch + " is " + typeToMatches);

    toMatch = new ArrayList<>();
    toMatch.add(new Pair("Unit", "0"));
    toMatch.add(new Pair("Topic", "Basics"));
    //toMatch.add(new Pair("Sound", "x"));
    typeToMatches = sectionHelper.getTypeToMatches(toMatch);
    logger.info("match for " + toMatch + " is " + typeToMatches);

    andPopulate.close();
  }

  private void forChild(ISection<CommonExercise> sectionHelper, SectionNode type, Map<String, String> map, int level) {
    //for (SectionNode type : sectionNodesForTypes) {
    Map<String, String> map2 = new LinkedHashMap<>();
    map2.putAll(map);
    map2.put(type.getType(), type.getName());
    Collection<CommonExercise> exercisesForSelectionState = sectionHelper.getExercisesForSimpleSelectionState(map2);
    String s = getDepth(level);
    logger.info(s + " for " + type.getType() + " " + type.getName() + " (" + map2 +
        ") " + exercisesForSelectionState.size());

    for (SectionNode sectionNode : type.getChildren()) {
      forChild(sectionHelper, sectionNode, map2, level + 1);
    }
//    forChildren(sectionHelper, type, map2);
    //}
  }

  @NotNull
  private String getDepth(int level) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < level; i++) builder.append("\t");
    return builder.toString();
  }

  private void forChildren(ISection<CommonExercise> sectionHelper, SectionNode type, Map<String, String> map) {
    Collection<SectionNode> children = type.getChildren();
    for (SectionNode sectionNode : children) {
      Map<String, String> map2 = new HashMap<>();
      map2.putAll(map);
      map2.put(sectionNode.getType(), sectionNode.getName());

      Collection<CommonExercise> exercisesForSelectionState2 = sectionHelper.getExercisesForSimpleSelectionState(map);
      logger.info("\tfor " + sectionNode.getType() + " " + sectionNode.getName() + " " + exercisesForSelectionState2.size());

      forChildren(sectionHelper, sectionNode, map2);
    }
  }
}
