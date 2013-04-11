package mitll.langtest.server.database;

import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Lesson;
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
  public static final String SEMESTER = "semester";
  private static Logger logger = Logger.getLogger(SectionHelper.class);
  private String unitType = "unit";
  private String chapterType = "chapter";
  private String weekType = "week";

  private Map<String,Map<String,Lesson>> typeToUnitToLesson = new HashMap<String,Map<String,Lesson>>();
  // e.g. "week"->"week 5"->[unit->["unit A","unit B"]],[chapter->["chapter 3","chapter 5"]]
  private Map<String,Map<String,Map<String,Set<String>>>> typeToSectionToTypeToSections = new HashMap<String, Map<String,Map<String,Set<String>>>>();

  public List<String> getTypeOrder() {
    List<String> types = new ArrayList<String>();
    types.addAll(typeToSectionToTypeToSections.keySet());
    Collections.sort(types, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        int first = typeToSectionToTypeToSections.get(o1).size();
        int second =  typeToSectionToTypeToSections.get(o2).size();
        return first > second ? +1 : first < second ? -1 : 0;
      }
    });
    return types;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getTypeToSectionsForTypeAndSection(String, String)
   * @param type
   * @param section
   * @return
   */
  public Map<String, Collection<String>> getTypeToSectionsForTypeAndSection(String type, String section) {
    Map<String, Map<String, Set<String>>> sectionToSub = typeToSectionToTypeToSections.get(type);
    if (sectionToSub == null) return Collections.emptyMap();
    Map<String, Set<String>> typeToSections = sectionToSub.get(section);
/*
    if (section == null || section.length() == 0) {
      Collection<Map<String, Set<String>>> values = sectionToSub.values();
      Map<String,Collection<String>> retval = new HashMap<String, Collection<String>>();
      for (Map<String, Set<String>> value : values) {
        for (Map.Entry<String,Set<String>> pair : value.entrySet()) {
          retval.put(pair.getKey(),new ArrayList<String>(pair.getValue()));
        }
      }
      return retval;
    }
    else {*/
      if (typeToSections == null) return Collections.emptyMap();

      Map<String,Collection<String>> retval = new HashMap<String, Collection<String>>();
      for (Map.Entry<String,Set<String>> pair : typeToSections.entrySet()) {
        retval.put(pair.getKey(),new ArrayList<String>(pair.getValue()));
      }
      logger.debug("getTypeToSectionsForTypeAndSection type=" + type + " section="+section + " yields " + retval);
      return retval;
  //  }
  }

  private Map<String,Set<String>> getTypeToSectionsForTypeAndSection2(String type, String section) {
    Map<String, Map<String, Set<String>>> sectionToSub = typeToSectionToTypeToSections.get(type);
    if (sectionToSub == null)
      return Collections.emptyMap();
    else
      return sectionToSub.get(section);
  }

  public Map<String, Set<String>> getTypeToSectionsForTypeAndSection(Map<String, String> typeToSection) {
    Map<String, Set<String>> resultMap = null;

    for (Map.Entry<String, String> pair : typeToSection.entrySet()) {
      String type = pair.getKey();
      if (isKnownType(type)) {
        String sectionForType = pair.getValue();
        logger.debug("looking for matches to " + type +"=" +sectionForType);

        Map<String, Set<String>> typeToSectionsForTypeAndSection2 = getTypeToSectionsForTypeAndSection2(type, sectionForType);
        logger.debug("\t result is " + typeToSectionsForTypeAndSection2);

        if (resultMap == null) {
          resultMap = new HashMap<String,Set<String>>(typeToSectionsForTypeAndSection2);
          logger.debug("\t resultMap now " + resultMap);
        }
        else {
          for (String currentType : typeToSectionsForTypeAndSection2.keySet()) {
            Set<String> copy;
            if (resultMap.containsKey(currentType)) {
              copy = new HashSet<String>(resultMap.get(currentType));
              Set<String> c = typeToSectionsForTypeAndSection2.get(currentType);
              if (c != null) {
                copy.retainAll(c);
              } else {
                logger.debug("\tno result matches for " + currentType);

              }
            } else {
              copy = new HashSet<String>(typeToSectionsForTypeAndSection2.get(currentType));
            }
            resultMap.put(currentType, copy);
            logger.debug("\t resultMap now " + resultMap);
          }
        }
      }
      else {
        logger.warn("huh? got unknown type " + type);
      }
    }
    if (resultMap == null) {
      logger.error("couldn't find any valid types given " + typeToSection);
      resultMap = Collections.emptyMap();
    }
    return resultMap;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getTypeToSection()
   * @return
   */
  public Map<String, Collection<String>> getTypeToSection() {
    Map<String,Collection<String>> typeToSection = new HashMap<String, Collection<String>>();
    for (String key : typeToUnitToLesson.keySet()) {
      Map<String, Lesson> stringLessonMap = typeToUnitToLesson.get(key);
      typeToSection.put(key, new ArrayList<String>(stringLessonMap.keySet()));
    }
    return typeToSection;
  }

  public Map<String, Map<String,Integer>> getTypeToSectionToCount() {
    Map<String,Map<String,Integer>> typeToSectionToCount = new HashMap<String, Map<String, Integer>>();
    for (String key : typeToUnitToLesson.keySet()) {
      Map<String, Lesson> stringLessonMap = typeToUnitToLesson.get(key);
      Map<String, Integer> sectionToCount = new HashMap<String, Integer>();
      typeToSectionToCount.put(key, sectionToCount);//new ArrayList<String>(stringLessonMap.keySet()));
      for (Map.Entry<String,Lesson> pair : stringLessonMap.entrySet()) sectionToCount.put(pair.getKey(),pair.getValue().getExercises().size());
    }
    return typeToSectionToCount;
  }

    /**
     * Return an overlap of all the type=section exercise sets (think venn diagram overlap).
     * @param typeToSection
     * @return
     */
  public Collection<Exercise> getExercisesForSelectionState(Map<String, String> typeToSection) {
    Collection<Exercise> currentList = null;
    for (Map.Entry<String, String> pair : typeToSection.entrySet()) {
      String type = pair.getKey();
      if (isKnownType(type)) {
        Collection<Exercise> exercisesForSection = new HashSet<Exercise>(getExercisesForSection(type, pair.getValue()));
        if (currentList == null) {
          currentList = exercisesForSection;
        } else {
          currentList.retainAll(exercisesForSection);
        }
      }
    }
    if (currentList == null) {
      logger.error("couldn't find any valid types given " + typeToSection);
      currentList = Collections.emptyList();
    }
    return currentList;
  }

  private boolean isKnownType(String type) {
    return typeToUnitToLesson.containsKey(type);
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

  public void checkIfSemesters() {
    if (typeToUnitToLesson.containsKey(unitType) && typeToUnitToLesson.containsKey(SEMESTER)) {
      int units = typeToUnitToLesson.get(unitType).size();
      int semesters = typeToUnitToLesson.get(SEMESTER).size();
      if (units == semesters) {
        logger.debug("Removing semesters...") ;
        typeToUnitToLesson.remove(SEMESTER);
        typeToSectionToTypeToSections.remove(SEMESTER);
        for (Map.Entry<String, Map<String, Map<String, Set<String>>>> pair : typeToSectionToTypeToSections.entrySet()) {
          Map<String, Map<String, Set<String>>> sectionToTypeToSections = pair.getValue();
          for (Map.Entry<String, Map<String, Set<String>>> pair2 : sectionToTypeToSections.entrySet()) {
            Map<String, Set<String>> typeToSections = pair2.getValue();
            typeToSections.remove(SEMESTER);
          }
        }
        logger.debug("typeToUnitToLesson "+typeToUnitToLesson) ;
        logger.debug("typeToSectionToTypeToSections "+typeToSectionToTypeToSections) ;

      }
      else {
        logger.debug("not Removing semesters... " + units + " vs " + semesters) ;

      }
    }
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
