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

import mitll.langtest.client.bootstrap.ItemSorter;
import mitll.langtest.server.database.Database;
import mitll.langtest.shared.exercise.SectionNode;
import mitll.langtest.shared.exercise.HasUnitChapter;
import mitll.langtest.shared.exercise.Shell;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static mitll.langtest.server.database.userexercise.SlickUserExerciseDAO.DIFFICULTY;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/29/13
 * Time: 4:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class SectionHelper<T extends Shell & HasUnitChapter> implements ISection<T> {
  private static final Logger logger = LogManager.getLogger(SectionHelper.class);
  private static final String SOUND = "Sound";
  private List<String> predefinedTypeOrder = new ArrayList<>();

  private final Map<String, Map<String, Lesson<T>>> typeToUnitToLesson = new HashMap<>();
  // e.g. "week"->"week 5"->[unit->["unit A","unit B"]],[chapter->["chapter 3","chapter 5"]]
/*  private final Map<String,
      Map<String,
          Map<String, Collection<String>>>> typeToSectionToTypeToSections = new HashMap<>();*/
  private SectionNode root = null;

  public SectionHelper() {
    makeRoot();
  }

  /**
   * @see BaseExerciseDAO#reload()
   */
  @Override
  public void clear() {
    typeToUnitToLesson.clear();
    //  typeToSectionToTypeToSections.clear();
    root = null;
  }

  /**
   * Try to put least numerous types at the top of the hierarchy
   *
   * @return
   * @see Database#getTypeOrder
   * @see Project#getTypeOrder()
   */
  @Override
  public List<String> getTypeOrder() {
    if (predefinedTypeOrder.isEmpty()) {
      List<String> types = new ArrayList<>();
      //  types.addAll(typeToSectionToTypeToSections.keySet());
      //   logger.info("getTypeOrder predef = " + predefinedTypeOrder + " : " + types);
      types.addAll(typeToUnitToLesson.keySet());

      if (types.isEmpty()) {
        types.addAll(typeToCount.keySet());
      } //else {
      Collections.sort(types, new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
          int first = typeToCount.get(o1).size();
          int second = typeToCount.get(o2).size();
          int i = first > second ? +1 : first < second ? -1 : 0;
          return i == 0 ? o1.compareTo(o2) : i;
        }
      });

      // TODO : I feel like I did this before...?
      // put sound at end...
      putSoundAtEnd(types);
      // }
      logger.info("getTypeOrder types " + types);

      return types;
    } else {
      Set<String> validTypes = typeToUnitToLesson.keySet();
      logger.info("getTypeOrder validTypes " + validTypes);

      List<String> valid = new ArrayList<>(predefinedTypeOrder);
      valid.retainAll(validTypes);
      return valid;
    }
  }

  private ItemSorter itemSorter = new ItemSorter();

  private void recurseAndCount(SectionNode node, Map<String, Set<String>> typeToCount) {
    String childType = node.getChildType();

    if (childType != null) { // i.e. not leaf
      // logger.info("recurseAndCount on " + node.getName() + " child type " + childType);
      Set<String> members = typeToCount.get(childType);//, new HashSet<>());
      if (members == null) {
        typeToCount.put(childType, members = new TreeSet<>(itemSorter));
      }

      for (SectionNode child : node.getChildren()) {
        members.add(child.getName());
        recurseAndCount(child, typeToCount);
      }
    }
  }

 /* public List<String> getTypeOrder2() {
    if (predefinedTypeOrder.isEmpty()) {
      List<String> types = new ArrayList<>();
      //  types.addAll(root.keySet());
      //   logger.info("getTypeOrder predef = " + predefinedTypeOrder + " : " + types);

      if (types.isEmpty()) {
        types.addAll(typeToUnitToLesson.keySet());
      } else {
        Collections.sort(types, new Comparator<String>() {
          @Override
          public int compare(String o1, String o2) {
            int first = typeToSectionToTypeToSections.get(o1).size();
            int second = typeToSectionToTypeToSections.get(o2).size();
            return new Integer(first).compareTo(second);
          }
        });

        // TODO : I feel like I did this before...?
        // put sound at end...
        putSoundAtEnd(types);
      }
//      logger.info("getTypeOrder types " + types);

      return types;
    } else {
      Set<String> validTypes = typeToUnitToLesson.keySet();
      //    logger.info("getTypeOrder validTypes " + validTypes);

      List<String> valid = new ArrayList<>(predefinedTypeOrder);
      valid.retainAll(validTypes);
      return valid;
    }
  }*/

  private void putSoundAtEnd(List<String> types) {
    //  String sound = SOUND;
    putAtEnd(types, SOUND);
    putAtEnd(types, DIFFICULTY);
  }

  private void putAtEnd(List<String> types, String sound) {
    if (types.contains(sound)) {
      types.remove(sound);
      types.add(sound);
    }
  }

  /**
   * @return
   * @see ExcelImport#readExercises(InputStream)
   */
  @Override
  public boolean allKeysValid() {
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
   * @see mitll.langtest.server.json.JsonExport#getContentAsJson
   */
/*  @Override
  public Collection<SectionNode> getSectionNodesForTypes() {
    List<String> typeOrder = getTypeOrder();
    logger.info("using type order " + typeOrder);
    return getChildren(typeOrder);
  }*/
  @Override
  public Collection<SectionNode> getSectionNodesForTypes() {
    return root.getChildren();
  }

  /**
   * @return
   * @paramx typeOrder
   * @see mitll.langtest.server.database.project.ProjectManagement#setStartupInfo
   */
/*
  @Override
  public Collection<SectionNode> getSectionNodesForTypes(List<String> typeOrder) {
    return getChildren(typeOrder);
  }
*/
  @Override
  public SectionNode getFirstNode(String name) {
    Collection<SectionNode> sectionNodesForTypes = getSectionNodesForTypes();
    return getMatch(sectionNodesForTypes, name);
  }

  /**
   * JUST FOR TESTING
   *
   * @param parent
   * @param type
   * @param name
   * @return
   */
  @Override
  public SectionNode getNode(SectionNode parent, String type, String name) {
    if (parent.getChildType().equals(type)) {
      return getMatch(parent.getChildren(), name);
    } else {
      return null;
    }
  }

  @Nullable
  private SectionNode getMatch(Collection<SectionNode> sectionNodesForTypes, String name) {
    for (SectionNode node : sectionNodesForTypes) {
      if (//node.getType().equals(type) &&
          node.getName().equals(name)) return node;
    }
    return null;
  }

  /**
   * @param typeOrder
   * @return
   * @see #getSectionNodesForTypes
   */
/*
  private List<SectionNode> getChildren(List<String> typeOrder) {
    if (typeOrder.isEmpty()) return Collections.emptyList();
    String root = typeOrder.iterator().next();
    List<SectionNode> firstSet = new ArrayList<>();

    Map<String, Map<String, Collection<String>>> sectionToTypeToSections = typeToSectionToTypeToSections.get(root);
    if (sectionToTypeToSections != null) {
      for (Map.Entry<String, Map<String, Collection<String>>> rootSection : sectionToTypeToSections.entrySet()) {
        SectionNode parent = new SectionNode(rootSection.getKey());
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
        logger.warn("getChildren no entry for " + root + " in " + typeToUnitToLesson.keySet());
      } else {
        //logger.debug("for " + root + " got " + stringLessonMap);
        for (Map.Entry<String, Lesson<T>> rootSection : stringLessonMap.entrySet()) {
          SectionNode parent = new SectionNode(rootSection.getKey());
          firstSet.add(parent);
        }
      }
    }

    return firstSet;
  }
*/

  /**
   * @param typeOrder
   * @param parent
   * @param typeToSections
   * @see #getChildren(java.util.List)
   */
/*
  private void addChildren(List<String> typeOrder,
                           SectionNode parent,
                           Map<String, Collection<String>> typeToSections) {
    List<String> remainingTypes = typeOrder.subList(1, typeOrder.size());
    String nextType = typeOrder.iterator().next();

    Collection<String> children = typeToSections.get(nextType);
    if (children == null) {
      logger.warn("addChildren huh? can't find " + nextType + " in " + typeToSections);
      //   report();
    } else {
      for (String childSection : children) {
        SectionNode child = new SectionNode(childSection);
        parent.addChild(child);

        Map<String, Map<String, Collection<String>>> sectionToTypeToSections = typeToSectionToTypeToSections.get(nextType);
        Map<String, Collection<String>> typeToSections2 = sectionToTypeToSections.get(childSection);

        if (!remainingTypes.isEmpty()) {
          addChildren(remainingTypes, child, typeToSections2);
        }
      }
    }
  }
*/

  /**
   * JUST FOR TESTING
   *
   * @param simpleMap
   * @return
   */
  @Override
  public Collection<T> getExercisesForSimpleSelectionState(Map<String, String> simpleMap) {
    Map<String, Collection<String>> typeToSection = new HashMap<>();
    for (Map.Entry<String, String> pair : simpleMap.entrySet())
      typeToSection.put(pair.getKey(), Collections.singleton(pair.getValue()));
    return getExercisesForSelectionState(typeToSection);
  }

  /**
   * @param type
   * @param value
   * @return
   * @see #report()
   */
  public Collection<T> getExercisesForSelectionState(String type, String value) {
    Map<String, Collection<String>> typeToSection = new HashMap<>();
    typeToSection.put(type, Collections.singleton(value));
    return getExercisesForSelectionState(typeToSection);
  }

  public Map<String, Set<String>> getTypeToMatches(String type, String value) {
    SectionNode root = this.root;
    return getTypeToMatch(type, value, root);
  }

  public Map<String, Set<String>> getTypeToMatches(Collection<Pair> pairs) {
    SectionNode root = this.root;
    List<Pair> copy = new ArrayList<>(pairs);
    return getTypeToMatchPairs(copy, root);
  }

  /**
   * Assumes a partial path implies = type=any for unmentioned?
   *
   * @param pairs
   * @param root
   * @return
   */
  @NotNull
  private Map<String, Set<String>> getTypeToMatchPairs(List<Pair> pairs, SectionNode root) {
    Map<String, Set<String>> typeToMatch = new HashMap<>();

    Iterator<Pair> iterator = pairs.iterator();
    Pair next = iterator.next();
    String type = next.getType();
    String toMatch = next.getSection();

    boolean isAll = toMatch.equalsIgnoreCase("any") || toMatch.equalsIgnoreCase("all");
    logger.info("to match " +type + "="+ toMatch + " out of " +pairs + " is all " +isAll);
    if (root.getChildType().equals(type)) {
      Set<String> matches = new HashSet<>();
      typeToMatch.put(type, matches);

      Collection<SectionNode> childMatches = null;

      if (isAll) {
        childMatches = root.getChildren();
        for (SectionNode child : childMatches) {
          matches.add(child.getName());
        }
      } else {
        for (SectionNode child : root.getChildren()) {
          if (child.getName().equals(toMatch)) {
            matches.add(toMatch);
            childMatches = new ArrayList<>();
            childMatches.add(child);
            break;
          }
        }
      }

      logger.info("children of " + root.getName() + " match for " + next + " is " +(childMatches == null ? "none":childMatches.size()));
      if (childMatches == null || childMatches.isEmpty()) {  // couldn't find matching value
        return typeToMatch;
      } else {
        iterator.remove();

        //logger.info("path now " + pairs);

        if (pairs.isEmpty()) {
          for (SectionNode childMatch : childMatches) {
            recurseAndCount(childMatch, typeToMatch);
          }
          return typeToMatch;
        }
        else {
          logger.info("recurse on " + childMatches.size() + " with path " + pairs);

          for (SectionNode childMatch : childMatches) {
            List<Pair> copy = new ArrayList<>(pairs);
            Map<String, Set<String>> typeToMatchPairs = getTypeToMatchPairs(copy, childMatch);
            mergeMaps(typeToMatch, typeToMatchPairs);
          }
        }
      }
    } else {
      Set<String> matches = new HashSet<>();
      typeToMatch.put(type, matches);
      matches.addAll(typeToCount.get(type));
      iterator.remove();

      if (pairs.isEmpty()) {
        logger.error("huh? pairs is empty for " + matches.size());
      }
      else {
        for (SectionNode child : root.getChildren()) {
          Map<String, Set<String>> typeToMatch1 = getTypeToMatchPairs(pairs, child);
          mergeMaps(typeToMatch, typeToMatch1);
        }
      }
    }

    return typeToMatch;
  }

  private void mergeMaps(Map<String, Set<String>> typeToMatch, Map<String, Set<String>> typeToMatch1) {
    for (Map.Entry<String, Set<String>> pair : typeToMatch1.entrySet()) {
      String key = pair.getKey();
      Set<String> currentMatches = typeToMatch.get(key);
      Set<String> matches = pair.getValue();
      if (currentMatches == null) {
        typeToMatch.put(key, matches);
      } else {
        currentMatches.addAll(matches);
      }
    }
  }

  @NotNull
  private Map<String, Set<String>> getTypeToMatch(String type, String value, SectionNode root) {
    Map<String, Set<String>> typeToMatch = new HashMap<>();

    if (root.getChildType().equals(type)) {

      SectionNode childWithName = root.getChildWithName(value);

      if (childWithName != null) {
        Set<String> matches = new HashSet<>();
        typeToMatch.put(type, matches);
        matches.add(value);

        recurseAndCount(childWithName, typeToMatch);
      }
    } else {
      for (SectionNode child : root.getChildren()) {
        Map<String, Set<String>> typeToMatch1 = getTypeToMatch(type, value, child);
        mergeMaps(typeToMatch, typeToMatch1);
      }
    }

    return typeToMatch;
  }

//  private void addChild(SectionNode child,Map<String, Set<String>> typeToMatch) {
//    typeToMatch.put(child.getType(),child.getChildren())
//  }

  /**
   * Return an overlap of all the type=section exercise sets (think venn diagram overlap).
   *
   * @param typeToSection
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getExercisesForSelectionState
   */
  @Override
  public Collection<T> getExercisesForSelectionState(Map<String, Collection<String>> typeToSection) {
    Collection<T> currentList = null;

    for (Map.Entry<String, Collection<String>> pair : typeToSection.entrySet()) {
      String type = pair.getKey();
      if (isKnownType(type)) {
        Collection<T> exercisesForSection = new HashSet<>(getExercisesForSection(type, pair.getValue()));

        // logger.info("getExercisesForSelectionState query " + type + " = " + pair.getValue() + " -> " + exercisesForSection.size());
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
      List<T> exercises = new ArrayList<>();
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
   * @param exercise
   * @sexe mitll.langtest.server.LangTestDatabaseImpl#getExercisesFromUserListFiltered
   * @see mitll.langtest.server.database.exercise.ExcelImport#getRawExercises()
   * @see BaseExerciseDAO#addNewExercises
   */
  @Override
  public void addExercise(T exercise) {
    List<Pair> pairs = new ArrayList<>();
    for (Map.Entry<String, String> pair : exercise.getUnitToValue().entrySet()) {
      pairs.add(addExerciseToLesson(exercise, pair.getKey(), pair.getValue()));
    }
    //  addAssociations(pairs);
    if (predefinedTypeOrder.isEmpty()) {
      rememberOne(root, pairs);
    } else {
      rememberOne(predefinedTypeOrder, root, pairs);
    }
  }

  /**
   * @param exercise
   * @param type     for this type - e.g. unit, chapter
   * @param unitName for this unit or chapter
   * @return
   * @seex mitll.langtest.server.LangTestDatabaseImpl#getExercisesFromFiltered
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#getByProject
   * @see mitll.langtest.server.database.exercise.ExcelImport#recordUnitChapterWeek
   */
  @Override
  public Pair addExerciseToLesson(T exercise, String type, String unitName) {
    Pair pair = getPairForExerciseAndLesson(exercise, type, unitName);
    exercise.addUnitToValue(type, unitName);

    return pair;
  }

  @Override
  public Pair getPairForExerciseAndLesson(T exercise, String type, String unitName) {
    Map<String, Lesson<T>> sectionToLesson = getSectionToLesson(type);
    addUnitNameEntry(exercise, unitName, sectionToLesson);
    return new Pair(type, unitName);
  }

  /**
   * @param exercise
   * @param unitName
   * @param sectionToLesson within a type, what groups are there - e.g. chapters
   * @see #getPairForExerciseAndLesson(Shell, String, String)
   */
  private void addUnitNameEntry(T exercise, String unitName, Map<String, Lesson<T>> sectionToLesson) {
    Lesson<T> unitForName = sectionToLesson.get(unitName);
    if (unitForName == null) {
      sectionToLesson.put(unitName, unitForName = new Lesson<>(unitName));
    }
    unitForName.addExercise(exercise);
  }

  /**
   * @param exercise
   * @see mitll.langtest.server.database.DatabaseImpl#deleteItem
   * @see BaseExerciseDAO#removeExercises()
   */
  @Override
  public boolean removeExercise(T exercise) {
    Map<String, String> unitToValue = exercise.getUnitToValue();
    //  logger.debug("Removing " + exercise.getOldID() + " with " +unitToValue);
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

  @Override
  public void refreshExercise(T exercise) {
    removeExercise(exercise);
    addExercise(exercise);
  }

  /**
   * @param exercise
   * @param type
   * @param unitName
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#deleteItem
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
  @Override
  public void setPredefinedTypeOrder(List<String> predefinedTypeOrder) {
    this.predefinedTypeOrder = predefinedTypeOrder;
  }

  /**
   * @param pairs
   * @see mitll.langtest.server.database.exercise.ExcelImport#recordUnitChapterWeek
   */
/*  @Override
  public void addAssociations(Collection<Pair> pairs) {
    for (Pair p : pairs) {
      List<Pair> others = new ArrayList<>(pairs);
      others.remove(p);
      for (Pair o : others) {
        addAssociation(p, o);
      }
    }
  }*/

/*  private void addAssociation(Pair first, Pair second) {
    addAssociation(first.type, first.section, second.type, second.section);
  }

  private void addAssociation(String type, String unitName, String otherType, String otherSection) {
    Map<String, Map<String, Collection<String>>> sectionToTypeToSections = typeToSectionToTypeToSections.get(type);
    if (sectionToTypeToSections == null) {
      typeToSectionToTypeToSections.put(type, sectionToTypeToSections = new HashMap<>());
    }
    Map<String, Collection<String>> subsections = sectionToTypeToSections.get(unitName);
    if (subsections == null) {
      sectionToTypeToSections.put(unitName, subsections = new HashMap<>());
    }
    Collection<String> sections = subsections.get(otherType);
    if (sections == null) subsections.put(otherType, sections = new HashSet<>());
    sections.add(otherSection);
  }*/

  private final File temp = new File("output.txt");

  @Override
  public void report() {
    logger.debug("report : type order " + getTypeOrder());

//    for (Map.Entry<String, Map<String, Map<String, Collection<String>>>> pair : typeToSectionToTypeToSections.entrySet()) {
//      logger.debug("pair " + pair.getKey() + " " + pair.getValue().size());
//
//    }
    try {
      FileWriter writer = new FileWriter(temp);
      logger.info("write to " + temp.getName());
      for (String key : typeToUnitToLesson.keySet()) {
        Map<String, Lesson<T>> categoryToLesson = typeToUnitToLesson.get(key);
        Set<String> sections = new TreeSet<>(categoryToLesson.keySet());
        if (!sections.isEmpty()) {
          String message = "\treport : " + key + " = (" + sections.size() + ") " + sections;

          logger.debug(message);
          writer.write(message);

          for (String section : sections) {
            Lesson<T> tLesson = categoryToLesson.get(section);
            String message1 = "\t\t" + section + " : " + tLesson.size();
            logger.debug(message1);
            writer.write(message1);

          }
          writer.write("\n");
        }
      }
      logger.debug("\t# section nodes " + getSectionNodesForTypes().size());
      for (SectionNode node : getSectionNodesForTypes()) {
        Collection<T> exercisesForSelectionState = getExercisesForSelectionState(node.getType(), node.getName());
        String message = "\tfor " + node.toComplete(0) + " got " + exercisesForSelectionState.size();
        logger.info(message);
        writer.write(message);
        writer.write("\n");
      }

      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private List<List<Pair>> seen = new ArrayList<>();

  public void rememberPairs(List<Pair> pairs) {
    seen.add(pairs);
  }

  /**
   * TESTING
   *
   * @param predefinedTypeOrder
   */
  public void rememberTypes(List<String> predefinedTypeOrder) {
    rememberTypesInOrder(predefinedTypeOrder, seen);
    //logger.info("rememberTypes root now " + root.easy());
    seen = null;
  }

  Map<String, Set<String>> typeToCount = new HashMap<>();

  /**
   * @param predefinedTypeOrder
   * @param seen
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#getExercises
   */
  public void rememberTypesInOrder(final List<String> predefinedTypeOrder, List<List<Pair>> seen) {
    SectionNode child = root;

    logger.info("type order " + predefinedTypeOrder);
    for (List<Pair> pairs : seen) {
      child = rememberOne(predefinedTypeOrder, child, pairs);
    }

    typeToCount = new HashMap<>();
    recurseAndCount(root, typeToCount);

    logger.info("type->count " + typeToCount);
  }

  private void makeRoot() {
    root = new SectionNode("root", "root");
    //  logger.info("NEW ROOT " + root, new Exception());
  }

  /**
   * @param predefinedTypeOrder
   * @param child
   * @param pairs
   * @return
   * @see #rememberTypesInOrder
   */
  private SectionNode rememberOne(final List<String> predefinedTypeOrder, SectionNode child, List<Pair> pairs) {
    pairs.sort(new Comparator<Pair>() {
      @Override
      public int compare(Pair o1, Pair o2) {
        int i = predefinedTypeOrder.indexOf(o1.getType());
        int anotherInteger = predefinedTypeOrder.indexOf(o2.getType());
        if (i > 0 && anotherInteger == -1) return -1;
        else if (i == -1 && anotherInteger > 0) return +1;
        else if (i == -1 && anotherInteger == -1) return o1.getType().compareTo(o2.getType());
        else return Integer.valueOf(i).compareTo(anotherInteger);
      }
    });

    return rememberOne(child, pairs);
  }

  /**
   * TESTING
   */
  public void rememberTypes() {
    rememberTypesFor(seen);
    seen = null;
    //logger.info("rememberTypes root now " + root.easy());
  }

  public void rememberTypesFor(List<List<Pair>> seen) {
    SectionNode child = root;

    for (List<Pair> pairs : seen) child = rememberOne(child, pairs);
    recurseAndCount(root, typeToCount = new HashMap<String, Set<String>>());

    logger.info("rememberTypesFor type->count " + typeToCount);
  }

  public Map<String, Set<String>> getTypeToDistinct() {
    return typeToCount;
  }

  private SectionNode rememberOne(SectionNode child, List<Pair> pairs) {
    for (Pair pair : pairs) {
      child = child.getChild(pair.getType(), pair.getSection());
    }
    child = root;
    return child;
  }

  public SectionNode getRoot() {
    return root;
  }
}
