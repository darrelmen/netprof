package mitll.langtest.server.database;

import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Lesson;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
  private static Logger logger = Logger.getLogger(SectionHelper.class);
  String unitType = "unit";
  String chapterType = "chapter";
  String weekType = "week";

  private Map<String,Map<String,Lesson>> typeToUnitToLesson = new HashMap<String,Map<String,Lesson>>();
  // e.g. "week"->"week 5"->[unit->["unit A","unit B"]],[chapter->["chapter 3","chapter 5"]]
  private Map<String,Map<String,Map<String,Set<String>>>> typeToSectionToTypeToSections = new HashMap<String, Map<String,Map<String,Set<String>>>>();


  public Map<String, Collection<String>> getTypeToSectionsForTypeAndSection(String type, String section) {
    Map<String, Map<String, Set<String>>> sectionToSub = typeToSectionToTypeToSections.get(type);
    if (sectionToSub == null) return Collections.emptyMap();
    Map<String, Set<String>> typeToSections = sectionToSub.get(section);
    if (typeToSections == null) return Collections.emptyMap();
    Map<String,Collection<String>> retval = new HashMap<String, Collection<String>>();
    for (Map.Entry<String,Set<String>> pair : typeToSections.entrySet()) {
      retval.put(pair.getKey(),new ArrayList<String>(pair.getValue()));
    }
    return retval;
  }

  public Map<String, Collection<String>> getTypeToSections() {
    Map<String,Collection<String>> typeToSection = new HashMap<String, Collection<String>>();
    for (String key : typeToUnitToLesson.keySet()) {
      Map<String, Lesson> stringLessonMap = typeToUnitToLesson.get(key);
      typeToSection.put(key, new ArrayList<String>(stringLessonMap.keySet()));
    }
    return typeToSection;
  }

  /**
   * Return an overlap of all the type=section exercise sets (think venn diagram overlap).
   * @param typeToSection
   * @return
   */
  public Collection<Exercise> getExercisesForSelectionState(Map<String,String> typeToSection) {
    Collection<Exercise> currentList = null;
    for (Map.Entry<String,String> pair : typeToSection.entrySet()) {
      String type = pair.getKey();
      if (isKnownType(type)) {
      Collection<Exercise> exercisesForSection = new HashSet<Exercise>(getExercisesForSection(type, pair.getValue()));
      //logger.debug("For " + pair + " got " + exercisesForSection.size() + " items");
      if (currentList == null) {
        currentList = exercisesForSection;
        //logger.debug("\t current now " + currentList.size() + " items");
      }
      else {
        //logger.debug("\t retaining " + exercisesForSection.size() + " items from current " + currentList.size() + " items");

        currentList.retainAll(exercisesForSection);
        //logger.debug("\t result " + currentList.size() + " items");
      }
      }
    }
    if (currentList == null) {
      logger.error("couldn't find any valid types given " + typeToSection);
      currentList = Collections.emptyList();
    }
    return currentList;
  }

  public Collection<Exercise> getExercisesForSection(String type, String section) {
    Map<String, Lesson> sectionToLesson = typeToUnitToLesson.get(type);
    if (sectionToLesson == null) {
      return Collections.emptyList();
    }
    else {
      Lesson lesson = sectionToLesson.get(section);
      if (lesson == null) {
        logger.error("Couldn't find section " + section);
        return Collections.emptyList();
      } else {
        return lesson.getExercises();
      }
    }
  }

  private boolean isKnownType(String type) {
    return typeToUnitToLesson.containsKey(type);
  }

  public Pair addUnitToLesson(Exercise exercise, String unitName) { return addExerciseToLesson(exercise, unitType, unitName);}
  public Pair addChapterToLesson(Exercise exercise, String unitName) { return addExerciseToLesson(exercise, chapterType, unitName);}
  public Pair addWeekToLesson(Exercise exercise, String unitName) { return addExerciseToLesson(exercise, weekType, unitName);}

  public Pair addExerciseToLesson(Exercise exercise, String type, String unitName) {
    Map<String, Lesson> unit = getSectionToLesson(type);

    Lesson even = unit.get(unitName);
    if (even == null) unit.put(unitName, even = new Lesson(unitName, "", ""));
    even.addExercise(exercise);

    return new Pair(type,unitName);
  }

  private Map<String, Lesson> getSectionToLesson( String section) {
    Map<String, Lesson> unit = typeToUnitToLesson.get(section);
    if (unit == null) typeToUnitToLesson.put(section, unit = new HashMap<String, Lesson>());
    return unit;
  }

  public static class Pair {
    private String type; private String section;

    public Pair(String type, String section) {
      this.type = type;
      this.section = section;
    }
  }

  public void addAssociations(List<Pair> pairs) {
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
    Map<String, Map<String, Set<String>>> sectionToTypeToSections = typeToSectionToTypeToSections.get(type);
    if (sectionToTypeToSections == null) {
      typeToSectionToTypeToSections.put(type, sectionToTypeToSections = new HashMap<String, Map<String, Set<String>>>());
    }
    Map<String, Set<String>> subsections = sectionToTypeToSections.get(unitName);
    if (subsections == null) {
      sectionToTypeToSections.put(unitName, subsections = new HashMap<String, Set<String>>());
    }
    Set<String> sections = subsections.get(otherType);
    if (sections == null) subsections.put(otherType, sections = new HashSet<String>());
    sections.add(otherSection);
  }


  public Set<String> getSections() { return typeToUnitToLesson.keySet(); }
  public Map<String, Lesson> getSection(String type) {
    return typeToUnitToLesson.get(type);
  }

  public void report() {
    for (String key : typeToUnitToLesson.keySet()) {
      Map<String, Lesson> categoryToLesson = typeToUnitToLesson.get(key);
      //lessons.addAll(categoryToLesson.values());
      Set<String> sections = categoryToLesson.keySet();
      if (!sections.isEmpty()) {
        logger.debug("Section type : " + key + " : sections " + sections);
      }
    }
  }
}
