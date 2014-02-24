package mitll.langtest.server.database;

import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.SectionNode;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/29/13
 * Time: 4:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class SectionHelper {
  private static final Logger logger = Logger.getLogger(SectionHelper.class);
  private static final String unitType = "unit";
  private static final String chapterType = "chapter";
  private static final String weekType = "week";

  private List<String> predefinedTypeOrder = new ArrayList<String>();

  private final Map<String,Map<String,Lesson>> typeToUnitToLesson = new HashMap<String,Map<String,Lesson>>();
  // e.g. "week"->"week 5"->[unit->["unit A","unit B"]],[chapter->["chapter 3","chapter 5"]]
  private final Map<String, Map<String, Map<String, Collection<String>>>> typeToSectionToTypeToSections = new HashMap<String, Map<String, Map<String, Collection<String>>>>();

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getTypeOrder()
   * @return
   */
  public List<String> getTypeOrder() {
    if (predefinedTypeOrder.isEmpty()) {
      List<String> types = new ArrayList<String>();
      types.addAll(typeToSectionToTypeToSections.keySet());
      if (types.isEmpty()) {
        types.addAll(typeToUnitToLesson.keySet());
      }
      else {
        Collections.sort(types, new Comparator<String>() {
          @Override
          public int compare(String o1, String o2) {
            int first = typeToSectionToTypeToSections.get(o1).size();
            int second = typeToSectionToTypeToSections.get(o2).size();
            return first > second ? +1 : first < second ? -1 : 0;
          }
        });
      }
      return types;
    } else {
      return predefinedTypeOrder;
    }
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getSectionNodes()
   * @return
   */
  public List<SectionNode> getSectionNodes() {
    return getChildren(getTypeOrder());
  }

  private List<SectionNode> getChildren(List<String> typeOrder) {
    if (typeOrder.isEmpty()) return Collections.emptyList();
    String root = typeOrder.iterator().next();
    List<SectionNode> firstSet = new ArrayList<SectionNode>();

    Map<String, Map<String, Collection<String>>> sectionToTypeToSections = typeToSectionToTypeToSections.get(root);
    if (sectionToTypeToSections != null) {
      for (Map.Entry<String, Map<String, Collection<String>>> rootSection : sectionToTypeToSections.entrySet()) {
        SectionNode parent = new SectionNode(root, rootSection.getKey());
        firstSet.add(parent);

        Map<String, Collection<String>> typeToSections = rootSection.getValue();

        if (!typeOrder.isEmpty()) {
          List<String> remainingTypes = typeOrder.subList(1, typeOrder.size());
          addChildren(remainingTypes, parent, typeToSections);
        }
      }
    } else {
      Map<String, Lesson> stringLessonMap = typeToUnitToLesson.get(root);
      if (stringLessonMap == null) {
        logger.error("no entry for " + root + " in " + typeToUnitToLesson.keySet());
      } else {
        //logger.debug("for " + root + " got " + stringLessonMap);
        for (Map.Entry<String, Lesson> rootSection : stringLessonMap.entrySet()) {
          SectionNode parent = new SectionNode(root, rootSection.getKey());
          firstSet.add(parent);
        }
      }
    }

    return firstSet;
  }

  private void addChildren(List<String> typeOrder, SectionNode parent, Map<String, Collection<String>> typeToSections) {
    List<String> remainingTypes = typeOrder.subList(1, typeOrder.size());
    String nextType = typeOrder.iterator().next();

    Collection<String> children = typeToSections.get(nextType);
    for (String childSection : children) {
      SectionNode child = new SectionNode(nextType, childSection);
      parent.addChild(child);

      Map<String, Map<String, Collection<String>>> sectionToTypeToSections = typeToSectionToTypeToSections.get(nextType);
      Map<String, Collection<String>> typeToSections2 = sectionToTypeToSections.get(childSection);

      if (!remainingTypes.isEmpty()) {
        addChildren(remainingTypes, child, typeToSections2);
      }
    }
  }

/*  public Map<String, Map<String,Integer>> getTypeToSectionToCount() {
    Map<String,Map<String,Integer>> typeToSectionToCount = new HashMap<String, Map<String, Integer>>();
    for (String key : typeToUnitToLesson.keySet()) {
      Map<String, Lesson> stringLessonMap = typeToUnitToLesson.get(key);
      Map<String, Integer> sectionToCount = new HashMap<String, Integer>();
      typeToSectionToCount.put(key, sectionToCount);
      for (Map.Entry<String, Lesson> pair : stringLessonMap.entrySet()) {
        sectionToCount.put(pair.getKey(), pair.getValue().getExercises().size());
      }
    }
    return typeToSectionToCount;
  }*/

    /**
     * Return an overlap of all the type=section exercise sets (think venn diagram overlap).
     *
     * @param typeToSection
     * @return
     * @see mitll.langtest.server.LangTestDatabaseImpl#getExercisesForState(int, java.util.Map, long)
     */
  public Collection<Exercise> getExercisesForSelectionState(Map<String, Collection<String>> typeToSection) {
    Collection<Exercise> currentList = null;

    for (Map.Entry<String, Collection<String>> pair : typeToSection.entrySet()) {
      String type = pair.getKey();
      if (isKnownType(type)) {
        Collection<Exercise> exercisesForSection = new HashSet<Exercise>(getExercisesForSection(type, pair.getValue()));

        if (currentList == null) {
          currentList = exercisesForSection;
        } else {
          currentList.retainAll(exercisesForSection);
        }
      }
      else {
        logger.warn("huh? typeToSelection type " + type + " is not in " + typeToUnitToLesson.keySet());
      }
    }
    if (currentList == null) {
   //   logger.error("couldn't find any valid types given request " + typeToSection);
      currentList = Collections.emptyList();
    }
    if (currentList.isEmpty()) {
      logger.warn("getExercisesForSelectionState : request " + typeToSection + " yielded " + currentList.size() + " exercises");
    }
    return currentList;
  }

  private boolean isKnownType(String type) {
    return typeToUnitToLesson.containsKey(type);
  }

  /**
   * @see #getExercisesForSelectionState(java.util.Map)
   * @param type
   * @param sections
   * @return
   */
  private Collection<Exercise> getExercisesForSection(String type, Collection<String> sections) {
    Map<String, Lesson> sectionToLesson = typeToUnitToLesson.get(type);
    if (sectionToLesson == null) {
      return Collections.emptyList();
    } else {
      List<Exercise> exercises = new ArrayList<Exercise>();
      for (String section : sections) {
        Lesson lesson = sectionToLesson.get(section);
        if (lesson == null) {
          logger.error("Couldn't find section " + section);
          return Collections.emptyList();
        } else {
          Collection<Exercise> exercises1 = lesson.getExercises();
          if (exercises1.isEmpty()) {
            logger.warn("getExercisesForSection : huh? section " + section + " has no exercises : " + lesson);
          }
          exercises.addAll(exercises1);
        }
      }
      return exercises;
    }
  }

  public Pair addUnitToLesson(Exercise exercise, String unitName) { return addExerciseToLesson(exercise, unitType, unitName);}
  public Pair addChapterToLesson(Exercise exercise, String unitName) { return addExerciseToLesson(exercise, chapterType, unitName);}
  public Pair addWeekToLesson(Exercise exercise, String unitName) { return addExerciseToLesson(exercise, weekType, unitName);}

  /**
   * @see ExcelImport#recordUnitChapterWeek(int, int, int, org.apache.poi.ss.usermodel.Row, mitll.langtest.shared.Exercise, String, String, String)
   * @param exercise
   * @param type
   * @param unitName
   * @return
   */
  public Pair addExerciseToLesson(Exercise exercise, String type, String unitName) {
    Map<String, Lesson> unit = getSectionToLesson(type);

    Lesson unitForName = unit.get(unitName);
    if (unitForName == null) {
      unit.put(unitName, unitForName = new Lesson(unitName));
    }
    unitForName.addExercise(exercise);

    exercise.addUnitToValue(type,unitName);

    return new Pair(type,unitName);
  }

  private Map<String, Lesson> getSectionToLesson( String section) {
    Map<String, Lesson> unit = typeToUnitToLesson.get(section);
    if (unit == null) {
      typeToUnitToLesson.put(section, unit = new HashMap<String, Lesson>());
    }
    return unit;
  }

  public void setPredefinedTypeOrder(List<String> predefinedTypeOrder) {
    this.predefinedTypeOrder = predefinedTypeOrder;
  }

  public static class Pair {
    private final String type; private final String section;

    public Pair(String type, String section) {
      this.type = type;
      this.section = section;
    }
  }

  /**
   * @see ExcelImport#recordUnitChapterWeek(int, int, int, org.apache.poi.ss.usermodel.Row, mitll.langtest.shared.Exercise, String, String, String)
   * @param pairs
   */
  public void addAssociations(Collection<Pair> pairs) {
    for (Pair p : pairs) {
      List<Pair> others = new ArrayList<Pair>(pairs);
      others.remove(p);
      for (Pair o : others) {
        addAssociation(p, o);
        // addAssociation(o, p);
      }
    }
  }

  private void addAssociation(Pair first, Pair second) {
    addAssociation(first.type, first.section, second.type, second.section);
  }

  private void addAssociation(String type, String unitName, String otherType, String otherSection) {
    Map<String, Map<String, Collection<String>>> sectionToTypeToSections = typeToSectionToTypeToSections.get(type);
    if (sectionToTypeToSections == null) {
      typeToSectionToTypeToSections.put(type, sectionToTypeToSections = new HashMap<String, Map<String, Collection<String>>>());
    }
    Map<String, Collection<String>> subsections = sectionToTypeToSections.get(unitName);
    if (subsections == null) {
      sectionToTypeToSections.put(unitName, subsections = new HashMap<String, Collection<String>>());
    }
    Collection<String> sections = subsections.get(otherType);
    if (sections == null) subsections.put(otherType, sections = new HashSet<String>());
    sections.add(otherSection);
  }

  public Set<String> getSections() { return typeToUnitToLesson.keySet(); }
  public Map<String, Lesson> getSection(String type) { return typeToUnitToLesson.get(type);  }

  public void report() {
    logger.debug("type order " + getTypeOrder());
    for (String key : typeToUnitToLesson.keySet()) {
      Map<String, Lesson> categoryToLesson = typeToUnitToLesson.get(key);
      Set<String> sections = categoryToLesson.keySet();
      if (!sections.isEmpty()) {
        logger.debug("report : Section type : " + key + " : sections " + sections);
      }
    }
  }
}
