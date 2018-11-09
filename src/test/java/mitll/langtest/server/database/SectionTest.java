package mitll.langtest.server.database;

import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.exercise.ITestSection;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.shared.exercise.*;
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
  public void testShort2() {
    SectionHelper<CommonExercise> sectionHelper = new SectionHelper<>();

    addShortData(sectionHelper);
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

    List<Pair> toMatch = new ArrayList<>();
    toMatch.add(getUnitOne());
    Map<String, Set<MatchInfo>> unit = sectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(getUnitOne());
    toMatch.add(getChapterA());
    unit = sectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(getUnitAll());
    toMatch.add(getChapterA());
    unit = sectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(getUnit2());
    toMatch.add(getChapterA());
    unit = sectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + unit);
  }

  @Test
  public void testShort3() {
    SectionHelper<CommonExercise> sectionHelper = new SectionHelper<>();

    addShortData(sectionHelper);
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

    List<Pair> toMatch = new ArrayList<>();
    toMatch.add(getUnitOne());
    Map<String, Set<MatchInfo>> unit = sectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(getUnitOne());
    toMatch.add(getChapterA());
    unit = sectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(getUnitAll());
    toMatch.add(getChapterA());
    unit = sectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(getUnit2());
    toMatch.add(getChapterA());
    unit = sectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(getUnit2());
    toMatch.add(getChapterA());
    FilterResponse typeToValues = sectionHelper.getTypeToValues(new FilterRequest(-1, toMatch, -1), false);

    logger.info("match for " + toMatch + " is " + typeToValues);
  }

  @Test
  public void testShort() {
    SectionHelper<CommonExercise> sectionHelper = new SectionHelper<>();

    addShortData(sectionHelper);
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

    List<Pair> toMatch = new ArrayList<>();
    toMatch.add(getUnitOne());
    Map<String, Set<MatchInfo>> unit = sectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(getUnitOne());
    toMatch.add(getChapterA());
    unit = sectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(getChapterA());
    unit = sectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(getUnit2());
    unit = sectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(getUnit2());
    toMatch.add(getChapterA());
    unit = sectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(getUnit2());
    toMatch.add(getChapterA());
    toMatch.add(getSoundX());
    unit = sectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + unit);


    toMatch = new ArrayList<>();
    toMatch.add(getUnitOne());
    toMatch.add(getChapterA());
    toMatch.add(getSoundX());
    unit = sectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(getUnit2());
    //  toMatch.add(new Pair("Chapter", "a"));
    unit = sectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(getUnitAll());
    toMatch.add(getChapterA());
    toMatch.add(getSoundAll());

    //  toMatch.add(new Pair("Chapter", "a"));
    unit = sectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + unit);

    toMatch = new ArrayList<>();
    toMatch.add(getUnitAll());
    toMatch.add(getChapterB());
    toMatch.add(getSoundAll());

    //  toMatch.add(new Pair("Chapter", "a"));
    unit = sectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + unit);
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

    {
      List<Pair> toMatch = new ArrayList<>();
      toMatch.add(getUnitOne());
      toMatch.add(getChapterA());
      Map<String, Set<MatchInfo>> unit2 = sectionHelper.getTypeToMatches(toMatch, false);
      logger.info("match for " + toMatch + " is " + unit2);

      toMatch = new ArrayList<>();
      toMatch.add(getUnit2());
      toMatch.add(getChapterA());
      unit2 = sectionHelper.getTypeToMatches(toMatch, false);
      logger.info("match for " + toMatch + " is " + unit2);

      toMatch = new ArrayList<>();
      toMatch.add(getUnit2());
      toMatch.add(getChapterA());
      toMatch.add(getSoundX());
      unit2 = sectionHelper.getTypeToMatches(toMatch, false);
      logger.info("match for " + toMatch + " is " + unit2);


      toMatch = new ArrayList<>();
      toMatch.add(getUnitOne());
      toMatch.add(getChapterA());
      toMatch.add(getSoundX());
      unit2 = sectionHelper.getTypeToMatches(toMatch, false);
      logger.info("match for " + toMatch + " is " + unit2);

      toMatch = new ArrayList<>();
      toMatch.add(getUnit2());
      //  toMatch.add(new Pair("Chapter", "a"));
      unit2 = sectionHelper.getTypeToMatches(toMatch, false);
      logger.info("match for " + toMatch + " is " + unit2);

      toMatch = new ArrayList<>();
      toMatch.add(getUnitAll());
      toMatch.add(getChapterA());
      toMatch.add(getSoundAll());

      //  toMatch.add(new Pair("Chapter", "a"));
      unit2 = sectionHelper.getTypeToMatches(toMatch, false);
      logger.info("match for " + toMatch + " is " + unit2);

      toMatch = new ArrayList<>();
      toMatch.add(getUnitAll());
      toMatch.add(getChapterB());
      toMatch.add(getSoundAll());

      //  toMatch.add(new Pair("Chapter", "a"));
      unit2 = sectionHelper.getTypeToMatches(toMatch, false);
      logger.info("match for " + toMatch + " is " + unit2);
    }
  }

  @NotNull
  private Pair getSoundAll() {
    return new Pair("Sound", "all");
  }

  @NotNull
  private Pair getUnitAll() {
    return getUnit("all");
  }

  @NotNull
  private Pair getSoundX() {
    return getSound("x");
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

  private void addShortData(SectionHelper<CommonExercise> sectionHelper) {
    List<Pair> pairs = new ArrayList<>();

    pairs.add(getUnitOne());
    pairs.add(getChapterA());
    sectionHelper.rememberPairs(pairs);

    pairs = new ArrayList<>();
    pairs.add(getUnitOne());
    pairs.add(getChapterB());
    sectionHelper.rememberPairs(pairs);

    pairs = new ArrayList<>();
    pairs.add(getUnit2());
    pairs.add(getChapterC());
    sectionHelper.rememberPairs(pairs);

    pairs = new ArrayList<>();
    pairs.add(getUnit2());
    pairs.add(getChapter("d"));
    sectionHelper.rememberPairs(pairs);

  }

  private void addData(SectionHelper<CommonExercise> sectionHelper) {
    List<Pair> pairs = new ArrayList<>();

    pairs.add(getUnitOne());
    pairs.add(getChapterA());

    List<Pair> copy = new ArrayList<>(pairs);
    Pair ng = new Pair("Sound", "ng");
    copy.add(ng);
    sectionHelper.rememberPairs(copy);

    Pair r = new Pair("Sound", "r");
    pairs.add(r);

    sectionHelper.rememberPairs(pairs);

    pairs = new ArrayList<>();
    pairs.add(getUnitOne());
    pairs.add(getChapterB());
    pairs.add(ng);

    sectionHelper.rememberPairs(pairs);

    pairs = new ArrayList<>();
    pairs.add(getUnitOne());
    pairs.add(getChapterC());
    pairs.add(ng);

    sectionHelper.rememberPairs(pairs);

    pairs = new ArrayList<>();
    pairs.add(getUnit2());
    pairs.add(getChapterA());
    pairs.add(ng);
    sectionHelper.rememberPairs(pairs);

    pairs = new ArrayList<>();
    pairs.add(getUnit2());
    pairs.add(getChapterA());
    pairs.add(getSoundX());
    sectionHelper.rememberPairs(pairs);

    pairs = new ArrayList<>();
    pairs.add(getUnit2());
    pairs.add(getChapterB());

    List<Pair> copy2 = new ArrayList<>(pairs);
    List<Pair> copy3 = new ArrayList<>(pairs);
    List<Pair> copy4 = new ArrayList<>(pairs);

    copy2.add(ng);
    sectionHelper.rememberPairs(copy2);

    copy3.add(r);
    sectionHelper.rememberPairs(copy3);
    copy4.add(getSoundT());
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

  @NotNull
  private Pair getSoundT() {
    String t = "t";
    return getSound(t);
  }

  @NotNull
  private Pair getSound(String t) {
    return new Pair("Sound", t);
  }

  @NotNull
  private Pair getChapterC() {
    return getChapter("c");
  }

  @NotNull
  private Pair getUnit2() {
    String value = "2";
    return getUnit(value);
  }

  @NotNull
  private Pair getUnit(String value) {
    return new Pair("Unit", value);
  }

  @NotNull
  private Pair getChapterB() {
    return getChapter("b");
  }

  @NotNull
  private Pair getChapterA() {
    String a = "a";
    return getChapter(a);
  }

  @NotNull
  private Pair getChapter(String a) {
    return new Pair("Chapter", a);
  }

  @NotNull
  private Pair getUnitOne() {
    return getUnit("1");
  }

  @Test
  public void testSectionP() {
    DatabaseImpl andPopulate = getAndPopulate();
    IProjectDAO projectDAO = andPopulate.getProjectDAO();
    int byLanguage = projectDAO.getByLanguage("portuguese");
    Project project = andPopulate.getProject(byLanguage);

    ISection<CommonExercise> sectionHelper = project.getSectionHelper();
    ITestSection<CommonExercise> tsectionHelper = (ITestSection<CommonExercise>)project.getSectionHelper();

    logger.info("sections " + sectionHelper.getSectionNodesForTypes());
   // SectionNode unit = sectionHelper.getFirstNode("1");

    SectionNode unit = tsectionHelper.getNode(tsectionHelper.getRoot(), "Unit", "1");

    logger.info("Got for 2 = " + unit);
    if (unit != null) {
      Collection<SectionNode> children = unit.getChildren();
      for (SectionNode node:children) logger.warn("node " + node.getType() + " " + node.getName());
      SectionNode chapter = tsectionHelper.getNode(unit, "Lesson", "1");
      logger.info("unit 1 lesson 1 " +chapter);
    }

    SectionNode unit2 = tsectionHelper.getNode(tsectionHelper.getRoot(), "Unit", "2");
    if (unit2 != null) {
      Collection<SectionNode> children = unit2.getChildren();
      for (SectionNode node:children) logger.warn("node " + node.getType() + " " + node.getName());
      SectionNode chapter = tsectionHelper.getNode(unit2, "Lesson", "2");
      logger.info("unit 2 lesson 2 " +chapter);
    }

    SectionNode root = tsectionHelper.getRoot();
    logger.info("Got " + root);
    logger.info("depth " + root.childCount());
    logger.info("type to distinct " + sectionHelper.getTypeToDistinct());

    // [Lesson=Any, Unit=1, Topic=Any, Dialect=Any]
    List<Pair> pairs =new ArrayList<>();
    pairs.add(new Pair("Unit","1"));
    pairs.add(new Pair("Lesson","Any"));
    pairs.add(new Pair("Topic","Any"));
    pairs.add(new Pair("Dialect","Any"));

    Map<String, Set<MatchInfo>> typeToMatches = tsectionHelper.getTypeToMatches(pairs, false);
    logger.info("got " +typeToMatches);

    Map<String, Set<MatchInfo>> typeToMatches2 = tsectionHelper.getTypeToMatches(pairs, false);
    logger.info("got " +typeToMatches2);

    List<Pair> pairs2 =new ArrayList<>();
    pairs2.add(new Pair("Unit","Any"));
    pairs2.add(new Pair("Lesson","Any"));
    pairs2.add(new Pair("Topic","Any"));
    pairs2.add(new Pair("Dialect","Any"));

    Map<String, Set<MatchInfo>> typeToMatches3 = tsectionHelper.getTypeToMatches(pairs2, false);
    logger.info("got " +typeToMatches3);

    Map<String, Set<MatchInfo>> typeToMatches4 = tsectionHelper.getTypeToMatches(pairs2, false);
    logger.info("got " +typeToMatches4);
  }

  @Test
  public void testSection2() {
    DatabaseImpl andPopulate = getAndPopulate();
    IProjectDAO projectDAO = andPopulate.getProjectDAO();
    int byLanguage = projectDAO.getByLanguage("spanish");
    Project project = andPopulate.getProject(byLanguage);

    ISection<CommonExercise> sectionHelper = project.getSectionHelper();
    ITestSection<CommonExercise> tsectionHelper = (ITestSection<CommonExercise>)project.getSectionHelper();

    SectionNode unit = tsectionHelper.getFirstNode("2");
    logger.info("Got for 2 = " + unit);
    SectionNode chapter = tsectionHelper.getNode(unit, "Chapter", "2");

    if (chapter != null) {
      logger.info("Got " + chapter);
      SectionNode sound = tsectionHelper.getNode(chapter, "Sound", "a");
      logger.info("Got " + sound);
      SectionNode sound2 = tsectionHelper.getNode(chapter, "Sound", "ng");
      logger.info("Got " + sound2);
    } else {
      logger.warn("nothing for chapter 2?");
    }

    SectionNode root = tsectionHelper.getRoot();
    logger.info("Got " + root);
    logger.info("depth " + root.childCount());
    logger.info("type to distinct " + sectionHelper.getTypeToDistinct());
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

    ITestSection<CommonExercise> tsectionHelper = (ITestSection<CommonExercise>)project.getSectionHelper();

    logger.info("root " + tsectionHelper.getRoot());
    logger.info("root children " + tsectionHelper.getRoot().getChildren());
    logger.info("initial values " + sectionHelper.getTypeToDistinct());

    SectionNode firstNode = tsectionHelper.getFirstNode("1");
    logger.info("Got first node for 1 " + firstNode);
    if (firstNode != null) {
      firstNode = tsectionHelper.getNode(firstNode, "Chapter", "1");
      logger.info("Got " + firstNode);
    }
    if (firstNode != null) {
      firstNode = tsectionHelper.getNode(firstNode, "Sound", "b");
      logger.info("Got " + firstNode);
    }
    Collection<CommonExercise> unit = tsectionHelper.getExercisesForSelectionState("Unit", "1");
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
    toMatch.add(getUnitOne());
    //toMatch.add(new Pair("Chapter", "a"));
    //toMatch.add(new Pair("Sound", "x"));
    Map<String, Set<MatchInfo>> typeToMatches = tsectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + typeToMatches);

    toMatch = new ArrayList<>();
    toMatch.add(getUnitOne());
    //toMatch.add(new Pair("Chapter", "a"));
    //toMatch.add(new Pair("Sound", "x"));
    typeToMatches = tsectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + typeToMatches);

    toMatch = new ArrayList<>();
    toMatch.add(getUnitOne());
    toMatch.add(new Pair("Topic", "Basics"));
    //toMatch.add(new Pair("Sound", "x"));
    typeToMatches = tsectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + typeToMatches);

    toMatch = new ArrayList<>();
    toMatch.add(getUnit("0"));
    toMatch.add(new Pair("Topic", "Basics"));
    //toMatch.add(new Pair("Sound", "x"));
    typeToMatches = tsectionHelper.getTypeToMatches(toMatch, false);
    logger.info("match for " + toMatch + " is " + typeToMatches);

    andPopulate.close();
  }

  private void forChild(ITestSection<CommonExercise> sectionHelper, SectionNode type, Map<String, String> map, int level) {
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

  private void forChildren(ITestSection<CommonExercise> sectionHelper, SectionNode type, Map<String, String> map) {
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
