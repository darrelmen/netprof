package mitll.langtest.server.database;

import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Lesson;
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
  private static Logger logger = Logger.getLogger(SectionHelper.class);
  private static final String unitType = "unit";
  private static final String chapterType = "chapter";
  private static final String weekType = "week";

  private List<String> predefinedTypeOrder = new ArrayList<String>();

  private Map<String,Map<String,Lesson>> typeToUnitToLesson = new HashMap<String,Map<String,Lesson>>();
  // e.g. "week"->"week 5"->[unit->["unit A","unit B"]],[chapter->["chapter 3","chapter 5"]]
  private Map<String, Map<String, Map<String, Collection<String>>>> typeToSectionToTypeToSections = new HashMap<String, Map<String, Map<String, Collection<String>>>>();

  public List<String> getTypeOrder() {
    if (predefinedTypeOrder.isEmpty()) {
      List<String> types = new ArrayList<String>();
      types.addAll(typeToSectionToTypeToSections.keySet());
      Collections.sort(types, new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
          int first = typeToSectionToTypeToSections.get(o1).size();
          int second = typeToSectionToTypeToSections.get(o2).size();
          return first > second ? +1 : first < second ? -1 : 0;
        }
      });

      return types;
    } else {
      return predefinedTypeOrder;
    }
  }

  public List<SectionNode> getSectionNodes() {
    return getChildren(getTypeOrder());
  }

  private List<SectionNode> getChildren(List<String> typeOrder) {
    String root = typeOrder.iterator().next();
    List<SectionNode> firstSet = new ArrayList<SectionNode>();

    Map<String, Map<String, Collection<String>>> sectionToTypeToSections = typeToSectionToTypeToSections.get(root);
    for (Map.Entry<String, Map<String, Collection<String>>> rootSection : sectionToTypeToSections.entrySet()) {
      SectionNode parent = new SectionNode(root, rootSection.getKey());
      firstSet.add(parent);

      Map<String, Collection<String>> typeToSections = rootSection.getValue();

      if (!typeOrder.isEmpty()) {
        List<String> remainingTypes = typeOrder.subList(1, typeOrder.size());
        addChildren(remainingTypes, parent, typeToSections);
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

  private Map<String,Collection<String>> getTypeToSectionsForTypeAndSection2(String type, String section) {
    Map<String, Map<String, Collection<String>>> sectionToSub = typeToSectionToTypeToSections.get(type);
    if (sectionToSub == null)
      return Collections.emptyMap();
    else
      return sectionToSub.get(section);
  }

  public Map<String, Collection<String>> getTypeToSectionsForTypeAndSection(Map<String, Collection<String>> typeToSections) {
    Map<String, Collection<String>> resultMap = null;
    Map<String, Map<String, Collection<String>>> typeToTypeToSections = new HashMap<String, Map<String, Collection<String>>>();

    for (Map.Entry<String, Collection<String>> pair : typeToSections.entrySet()) {
      String type = pair.getKey();
      if (isKnownType(type)) {
        Map<String, Collection<String>> typeToSectionsForType = typeToTypeToSections.get(type);

        Collection<String> sectionsForType = pair.getValue();
        logger.debug("looking for matches to user selections " + type + "=" + sectionsForType);

        for (String sectionForType : sectionsForType) {   // user selections
          Map<String, Collection<String>> typeToSectionsForTypeAndSection2 = getTypeToSectionsForTypeAndSection2(type, sectionForType);
          //logger.debug("\tresult is " + typeToSectionsForTypeAndSection2);

          if (typeToSectionsForType == null) {
            typeToTypeToSections.put(type, typeToSectionsForType = new HashMap<String, Collection<String>>());
            //resultMap = new HashMap<String, Set<String>>(typeToSectionsForTypeAndSection2);
            typeToSectionsForType.putAll(typeToSectionsForTypeAndSection2);
            logger.debug("\tinitial for " + type + "/" +sectionForType + " : "+typeToSectionsForType);
          } else {
            combineMapsTogether(typeToSectionsForType, typeToSectionsForTypeAndSection2, true);
            logger.debug("\tcurrent for " + type + "/" +sectionForType + " : "+ typeToSectionsForType);
          }

          //combineMapsTogether(typeToSectionsForType, typeToSections, false);
      //    logger.debug("\tafter AND against selection for " + type + "/" +sectionForType + " : "+ typeToSectionsForType);

        }

/*        for (String type2 : typeToSections.keySet()) {
           if (!type2.equals(type))  {
             Collection<String> currentFilterResults = typeToSectionsForType.get(type2);
             Collection<String> userSelections = typeToSections.get(type2);
             Set<String> copy = new HashSet<String>(userSelections);
             copy.retainAll(currentFilterResults);
             typeToSections.put(type2,copy);
             logger.debug("\tafter AND against results for " + type + "/" +currentFilterResults + " : "+ typeToSections.get(type2));
           }
        }*/
        //combineMapsTogether(typeToSections, typeToSections, false);
        //    logger.debug("\tafter AND against selection for " + type + "/" +sectionForType + " : "+ typeToSectionsForType);
      }
      else {
        logger.warn("huh? got unknown type " + type);
      }
    }

    for (Map<String, Collection<String>> values : typeToTypeToSections.values()) {
      if (resultMap == null) {
        resultMap = values;
      }
      else {
        combineMapsTogether(resultMap,values,false);
      }
    }

    if (resultMap == null) {
      logger.error("couldn't find any valid types given " + typeToSections);
      resultMap = Collections.emptyMap();
    }
    else {
      logger.debug("initial result " + resultMap + ", now AND with user selections " + typeToSections);

      Map<String, Collection<String>> copy = new HashMap<String, Collection<String>>();
      for (String key : resultMap.keySet()) copy.put(key, new HashSet<String>(resultMap.get(key)));
      // filter again to make sure result is consistent...
      for (Map.Entry<String, Collection<String>> pair : resultMap.entrySet()) {
        String resultType = pair.getKey();

        Map<String,Collection<String>> mergedResult = new HashMap<String,Collection<String>>();

        for (String section : pair.getValue()) {
          Map<String, Collection<String>> typeToSectionsForTypeAndSection2 = getTypeToSectionsForTypeAndSection2(resultType, section);

          for (Map.Entry<String, Collection<String>> otherTypeToSections : typeToSectionsForTypeAndSection2.entrySet()) {
            String constrainedType = otherTypeToSections.getKey();
            if (!constrainedType.equals(resultType)) {
              Collection<String> matches = otherTypeToSections.getValue();
              Collection<String> sectionsForType = mergedResult.get(constrainedType);
              if (sectionsForType == null) mergedResult.put(constrainedType, sectionsForType = new HashSet<String>());
              sectionsForType.addAll(matches);


          /*    Collection<String> filteredSections = copy.get(constrainedType);
              logger.debug("\tsection " + section + " type " + constrainedType + ", matches " + matches + " examining " + filteredSections);
              if (filteredSections != null) {
                filteredSections.retainAll(matches);
                logger.debug("\tafter AND " + filteredSections);
              }
*/
            }
          }
        }

        logger.debug("mergedResult is " + mergedResult);

        for (Map.Entry<String, Collection<String>> typeToSectionsAgain : copy.entrySet()) {
          Collection<String> sections = mergedResult.get(typeToSectionsAgain.getKey());
          if (sections != null) {
            typeToSectionsAgain.getValue().retainAll(sections);
          }
        }

      }

      resultMap = copy;
      logger.debug("result is " + resultMap);
      // user selections override filtered results
 /*     for (String type  : resultMap.keySet()) {
       // String type = typeToSectionsResult.getKey();
        if (typeToSections.containsKey(type)) {
          resultMap.put(type,typeToSections.get(type));
        }
      }*/
      //combineMapsTogether(resultMap,typeToSections,false);
   //   logger.debug("result is " + resultMap);

    }
    return resultMap;
  }

  private void combineMapsTogether(Map<String, Collection<String>> typeToSectionsForType,
                                   Map<String, Collection<String>> typeToSectionsForTypeAndSection2, boolean doOr) {
    for (String currentType : typeToSectionsForTypeAndSection2.keySet()) {
      Set<String> copy;
      Collection<String> setToAdd = typeToSectionsForTypeAndSection2.get(currentType);
      if (typeToSectionsForType.containsKey(currentType)) {
        copy = new HashSet<String>(typeToSectionsForType.get(currentType));
        if (setToAdd != null) {
          if (doOr) {
            copy.addAll(setToAdd);
          } else {
            copy.retainAll(setToAdd);
          }
        } else {
          logger.debug("\tno result matches for " + currentType);
        }
      } else {
        copy = new HashSet<String>(setToAdd);
      }
      typeToSectionsForType.put(currentType, copy);
    }
  }

  public Map<String, Map<String,Integer>> getTypeToSectionToCount() {
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
  }

    /**
     * Return an overlap of all the type=section exercise sets (think venn diagram overlap).
     *
     * @param typeToSection
     * @return
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
    }
    if (currentList == null) {
      logger.error("couldn't find any valid types given " + typeToSection);
      currentList = Collections.emptyList();
    }
    logger.debug("getExercisesForSelectionState : request " + typeToSection + " yielded " + currentList.size() + " exercises");
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
          exercises.addAll(lesson.getExercises());
        }
      }
      return exercises;
    }
  }
/*
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
  }*/

  public Pair addUnitToLesson(Exercise exercise, String unitName) { return addExerciseToLesson(exercise, unitType, unitName);}
  public Pair addChapterToLesson(Exercise exercise, String unitName) { return addExerciseToLesson(exercise, chapterType, unitName);}
  public Pair addWeekToLesson(Exercise exercise, String unitName) { return addExerciseToLesson(exercise, weekType, unitName);}

  public Pair addExerciseToLesson(Exercise exercise, String type, String unitName) {
    Map<String, Lesson> unit = getSectionToLesson(type);

    Lesson even = unit.get(unitName);
    if (even == null) {
      unit.put(unitName, even = new Lesson(unitName, "", ""));
    }
    even.addExercise(exercise);

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
  public Map<String, Lesson> getSection(String type) {
    return typeToUnitToLesson.get(type);
  }

  public void report() {
    for (String key : typeToUnitToLesson.keySet()) {
      Map<String, Lesson> categoryToLesson = typeToUnitToLesson.get(key);
      Set<String> sections = categoryToLesson.keySet();
      if (!sections.isEmpty()) {
        logger.debug("report : Section type : " + key + " : sections " + sections);
      }
    }
  }
}
