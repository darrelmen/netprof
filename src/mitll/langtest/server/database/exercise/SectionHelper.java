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

package mitll.langtest.server.database.exercise;

import mitll.langtest.shared.SectionNode;
import mitll.langtest.shared.exercise.Shell;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/29/13
 * Time: 4:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class SectionHelper<T extends Shell> {
  private static final Logger logger = Logger.getLogger(SectionHelper.class);
  private List<String> predefinedTypeOrder = new ArrayList<>();

  private final Map<String, Map<String, Lesson<T>>> typeToUnitToLesson = new HashMap<>();
  // e.g. "week"->"week 5"->[unit->["unit A","unit B"]],[chapter->["chapter 3","chapter 5"]]
  private final Map<String, Map<String, Map<String, Collection<String>>>> typeToSectionToTypeToSections = new HashMap<String, Map<String, Map<String, Collection<String>>>>();
  private static final boolean DEBUG = false;

  public void clear() {
    typeToUnitToLesson.clear();
    typeToSectionToTypeToSections.clear();
  }

  /**
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getTypeOrder
   */
  public List<String> getTypeOrder() {
//    logger.info("getTypeOrder predefinedTypeOrder : " + predefinedTypeOrder);
    if (predefinedTypeOrder.isEmpty()) {
      List<String> types = new ArrayList<String>();
      types.addAll(typeToSectionToTypeToSections.keySet());
      if (types.isEmpty()) {
        types.addAll(typeToUnitToLesson.keySet());
      } else {
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
      Set<String> validTypes = typeToUnitToLesson.keySet();
      List<String> valid = new ArrayList<String>(predefinedTypeOrder);
      valid.retainAll(validTypes);
      return valid;
    }
  }

  boolean allKeysValid() {
    for (String type : typeToUnitToLesson.keySet()) {
      if (type == null || type.equals("null")) {
        logger.error("ERROR ERROR \n\n - the tierIndex property is out of sync with the spreadsheet columns! - " +
            "types are " + typeToUnitToLesson.keySet() + "\n\n");
        return false;
      }
    }
    return true;
  }

  /**
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getSectionNodes
   */
  public Collection<SectionNode> getSectionNodes() {
    return getChildren(getTypeOrder());
  }

  private List<SectionNode> getChildren(List<String> typeOrder) {
    if (typeOrder.isEmpty()) {
      if (DEBUG) logger.info("type order is empty");
      return Collections.emptyList();
    }
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
      Map<String, Lesson<T>> stringLessonMap = typeToUnitToLesson.get(root);
      if (stringLessonMap == null) {
        logger.error("no entry for " + root + " in " + typeToUnitToLesson.keySet());
      } else {
        //logger.debug("for " + root + " got " + stringLessonMap);
        for (Map.Entry<String, Lesson<T>> rootSection : stringLessonMap.entrySet()) {
          SectionNode parent = new SectionNode(root, rootSection.getKey());
          firstSet.add(parent);
        }
      }
    }

    //logger.info("first set " + firstSet);

    return firstSet;
  }

  /**
   * @param typeOrder
   * @param parent
   * @param typeToSections
   * @see #getChildren(java.util.List)
   */
  private void addChildren(List<String> typeOrder, SectionNode parent, Map<String, Collection<String>> typeToSections) {
    List<String> remainingTypes = typeOrder.subList(1, typeOrder.size());
    String nextType = typeOrder.iterator().next();

    Collection<String> children = typeToSections.get(nextType);

    //logger.info("children of " + nextType);
    //for (String child : children) logger.info(child);
    if (children == null) {
      logger.error("huh? can't find " + nextType + " in " + typeToSections);
    } else {
      List<String> sorted = getSortedChildren(children);

     // for (String child : sorted) logger.info("Sort " +child);

      for (String childSection : sorted) {
        SectionNode child = new SectionNode(nextType, childSection);
        parent.addChild(child);

        Map<String, Map<String, Collection<String>>> sectionToTypeToSections = typeToSectionToTypeToSections.get(nextType);
        Map<String, Collection<String>> typeToSections2 = sectionToTypeToSections.get(childSection);

        if (!remainingTypes.isEmpty()) {
          addChildren(remainingTypes, child, typeToSections2);
        }
      }
    }
  }

  private List<String> getSortedChildren(Collection<String> children) {
    List<String> sorted = new ArrayList<>(children);
    Collections.sort(sorted, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        String[] s1 = o1.split(":");
        boolean firstHasInt = false;
        int firstIndex = 0;
        if (s1.length == 2) {
          try {
            firstIndex = Integer.parseInt(s1[0]);
            firstHasInt = true;
          } catch (NumberFormatException e) {
            //e.printStackTrace();
          }
        }
        String[] s2 = o2.split(":");
        boolean secondHasInt = false;
        int secondIndex = 0;
        if (s2.length == 2) {
          try {
            secondIndex = Integer.parseInt(s2[0]);
            secondHasInt = true;
          } catch (NumberFormatException e) {
            //e.printStackTrace();
          }
        }

        if (firstHasInt) {
          if (secondHasInt) {
            return Integer.valueOf(firstIndex).compareTo(secondIndex);
          } else return -1;
        } else if (secondHasInt) {
          return -1;
        } else return o1.compareTo(o2);
      }
    });
    return sorted;
  }

/*
  public Collection<T> getExercisesForSimpleSelectionState(Map<String, String> simpleMap) {
    Map<String, Collection<String>> typeToSection = new HashMap<>();
    for (Map.Entry<String,String> pair : simpleMap.entrySet()) typeToSection.put(pair.getKey(),Collections.singleton(pair.getValue()));
    return getExercisesForSelectionState(typeToSection);
  }
*/

  public Collection<T> getExercisesForSelectionState(String type, String value) {
    Map<String, Collection<String>> typeToSection = new HashMap<>();
    typeToSection.put(type, Collections.singleton(value));
    return getExercisesForSelectionState(typeToSection);
  }

  /**
   * Return an overlap of all the type=section exercise sets (think venn diagram overlap).
   *
   * @param typeToSection
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercisesForSelectionState
   */
  public Collection<T> getExercisesForSelectionState(Map<String, Collection<String>> typeToSection) {
    Collection<T> currentList = null;

//    logger.info("getExercisesForSelectionState got " + typeToSection);
    for (Map.Entry<String, Collection<String>> pair : typeToSection.entrySet()) {
      String type = pair.getKey();
      if (isKnownType(type)) {
        Collection<String> value = pair.getValue();
        //      logger.info("looking for exercises under '" +value +"' (" +value.size()+ ")");
        Collection<T> exercisesForSection = new HashSet<T>(getExercisesForSection(type, value));

        if (currentList == null) {
          currentList = exercisesForSection;
        } else {
          currentList.retainAll(exercisesForSection);
        }
      } else {
        logger.warn("getExercisesForSelectionState huh? typeToSelection type " + type + " is not in " + typeToUnitToLesson.keySet());
      }
    }
    if (currentList == null) {
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
   * @param type
   * @param sections
   * @return
   * @see #getExercisesForSelectionState(java.util.Map)
   */
  private Collection<T> getExercisesForSection(String type, Collection<String> sections) {
    Map<String, Lesson<T>> sectionToLesson = typeToUnitToLesson.get(type);
    if (sectionToLesson == null) {
      return Collections.emptyList();
    } else {
      List<T> exercises = new ArrayList<T>();
      for (String section : sections) {
        Lesson<T> lesson = sectionToLesson.get(section);
        if (lesson == null) {
          logger.error("getExercisesForSection : Couldn't find section '" + section + "'");
          return Collections.emptyList();
        } else {
          Collection<T> exercises1 = lesson.getExercises();
          if (exercises1.isEmpty()) {
            logger.warn("getExercisesForSection : huh? section '" + section + "' has no exercises : " + lesson);
          }
          exercises.addAll(exercises1);
        }
      }
      return exercises;
    }
  }

  /**
   * @param where
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercisesFromFiltered
   * @see mitll.langtest.server.database.exercise.ExcelImport#getRawExercises()
   */
  public void addExercise(T where) {
    List<SectionHelper.Pair> pairs = new ArrayList<SectionHelper.Pair>();
    for (Map.Entry<String, String> pair : where.getUnitToValue().entrySet()) {
      pairs.add(addExerciseToLesson(where, pair.getKey(), pair.getValue()));
    }
    addAssociations(pairs);
  }

  /**
   * @param exercise
   * @param type
   * @param unitName
   * @return
   * @see mitll.langtest.server.database.exercise.ExcelImport#recordUnitChapterWeek
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExercisesFromFiltered(java.util.Map, mitll.langtest.shared.custom.UserList)
   */
  public Pair addExerciseToLesson(T exercise, String type, String unitName) {
    Map<String, Lesson<T>> unit = getSectionToLesson(type);

    addUnitNameEntry(exercise, unitName, unit);

    exercise.addUnitToValue(type, unitName);

    return new Pair(type, unitName);
  }

  private void addUnitNameEntry(T exercise, String unitName, Map<String, Lesson<T>> unit) {
    Lesson<T> unitForName = unit.get(unitName);
    if (unitForName == null) {
      unit.put(unitName, unitForName = new Lesson<T>(unitName));
    }
    unitForName.addExercise(exercise);
  }

  /**
   * @param exercise
   * @see mitll.langtest.server.database.DatabaseImpl#deleteItem(String)
   * @see BaseExerciseDAO#removeExercises()
   */
  public boolean removeExercise(T exercise) {
    Map<String, String> unitToValue = exercise.getUnitToValue();
    //  logger.debug("Removing " + exercise.getID() + " with " +unitToValue);
    boolean didRemove = false;
    if (unitToValue != null) {
      didRemove = true;
      for (Map.Entry<String, String> pair : unitToValue.entrySet()) {
        if (!removeExerciseToLesson(exercise, pair.getKey(), pair.getValue())) {
          logger.warn("removeExercise didn't remove " + exercise.getID() + " for " + pair);
          didRemove = false;
        }
      }
    }
    return didRemove;
  }

  public void refreshExercise(T exercise) {
    removeExercise(exercise);
    addExercise(exercise);
  }

  /**
   * @param exercise
   * @param type
   * @param unitName
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#deleteItem(String)
   */
  private boolean removeExerciseToLesson(T exercise, String type, String unitName) {
    Map<String, Lesson<T>> unit = getSectionToLesson(type);
    if (unit == null) {
      logger.error("no unit for " + type + " in " + typeToUnitToLesson.keySet());
      return false;
    } else {
      Lesson<T> tLesson = unit.get(unitName);
      if (tLesson == null) {
        logger.error("no lesson for " + type + "/" + unitName + " in " + unit.keySet());
        return false;
      } else {
        return tLesson.remove(exercise);
      }
    }
  }

  /**
   * @param section
   * @return
   * @see #addExerciseToLesson(Shell, String, String)
   */
  private Map<String, Lesson<T>> getSectionToLesson(String section) {
    Map<String, Lesson<T>> unit = typeToUnitToLesson.get(section);
    if (unit == null) {
      typeToUnitToLesson.put(section, unit = new HashMap<>());
    }
    return unit;
  }

  /**
   * @param predefinedTypeOrder
   * @see mitll.langtest.server.database.exercise.ExcelImport#readFromSheet(org.apache.poi.ss.usermodel.Sheet)
   */
  public void setPredefinedTypeOrder(List<String> predefinedTypeOrder) {
    this.predefinedTypeOrder = predefinedTypeOrder;
  }

  public final static class Pair {
    private final String type;
    private final String section;

    Pair(String type, String section) {
      this.type = type;
      this.section = section;
    }
  }

  /**
   * @param pairs
   * @see mitll.langtest.server.database.exercise.ExcelImport#recordUnitChapterWeek
   */
  public void addAssociations(Collection<Pair> pairs) {
    for (Pair p : pairs) {
      List<Pair> others = new ArrayList<Pair>(pairs);
      others.remove(p);
      for (Pair o : others) {
        addAssociation(p, o);
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

  public void report() {
    logger.debug("report : type order " + getTypeOrder());
    for (String key : typeToUnitToLesson.keySet()) {
      Map<String, Lesson<T>> categoryToLesson = typeToUnitToLesson.get(key);
      Set<String> sections = categoryToLesson.keySet();
      if (!sections.isEmpty()) {
        logger.debug("\treport : Section type : " + key + " : sections " + sections);
      }
    }
    logger.debug("\t# section nodes " + getSectionNodes().size());
    for (SectionNode node : getSectionNodes()) {
      Collection<T> exercisesForSelectionState = getExercisesForSelectionState(node.getType(), node.getName());
      logger.info("\tfor " + node + " got " + exercisesForSelectionState.size());
    }
  }
}
