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
import mitll.langtest.shared.exercise.*;
import mitll.npdata.dao.SlickExercise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
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
public class SectionHelper<T extends Shell & HasUnitChapter> implements ISection<T>, ITestSection<T> {
  private static final Logger logger = LogManager.getLogger(SectionHelper.class);

  static final String TOPIC = "Topic";
  static final String SUB_TOPIC = "Sub-topic";
  static final String SUBTOPIC = "subtopic";
  static final Set<String> SUBTOPICS = new HashSet<>(Arrays.asList(SUB_TOPIC, SUBTOPIC));
  private static final String GRAMMAR = "Grammar";
  private static final String DIALECT = "Dialect";
  private static final String DIFFICULTY = "Difficulty";
  private static final String ANY = "any";
  private static final String ALL = "all";
  private static final String LISTS = "Lists";
  private List<String> predefinedTypeOrder = new ArrayList<>();

  private final Map<String, Map<String, Lesson<T>>> typeToUnitToLesson = new HashMap<>();
  // e.g. "week"->"week 5"->[unit->["unit A","unit B"]],[chapter->["chapter 3","chapter 5"]]

  private SectionNode root = null;
  private Set<String> rootTypes = new HashSet<>();
  private Map<String, String> parentToChildTypes = new HashMap<>();

  private final boolean DEBUG = false;
  private final boolean DEBUG_TYPE_ORDER = false;

  public SectionHelper() {
    makeRoot();
  }

  /**
   * @see BaseExerciseDAO#reload()
   */
  @Override
  public void clear() {
    typeToUnitToLesson.clear();
    makeRoot();
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
      List<String> types = new ArrayList<>(typeToUnitToLesson.keySet());
      if (DEBUG_TYPE_ORDER) logger.info("getTypeOrder predef = " + predefinedTypeOrder + " : " + types);
      if (DEBUG_TYPE_ORDER) logger.info("getTypeOrder typeToCount = " + typeToCount);

      if (types.isEmpty()) {
        types.addAll(typeToCount.keySet());
      }

      if (!typeToCount.isEmpty()) {
        types.sort((o1, o2) -> {
          int first = typeToCount.get(o1).size();
          int second = typeToCount.get(o2).size();
          int i = Integer.compare(first, second);
          return i == 0 ? o1.compareTo(o2) : i;
        });
        if (DEBUG_TYPE_ORDER) logger.info("getTypeOrder after sort now " + types);
      } else {
        types.sort((o1, o2) -> {
          int first = typeToUnitToLesson.get(o1).size();
          int second = typeToUnitToLesson.get(o2).size();
          int i = Integer.compare(first, second);
          return i == 0 ? o1.compareTo(o2) : i;
        });
        if (DEBUG_TYPE_ORDER) logger.info("getTypeOrder 2 after sort now " + types);
      }

      // TODO : I feel like I did this before...?
      // put sound at end...
      reorderTypes(types);
      // }
      //if (DEBUG)
//        logger.warn("getTypeOrder types " + types);

      return types;
    } else {
      Set<String> validTypes = typeToUnitToLesson.keySet();
      if (DEBUG_TYPE_ORDER) logger.info("getTypeOrder validTypes " + validTypes);

      List<String> valid = new ArrayList<>(predefinedTypeOrder);
      valid.retainAll(validTypes);
      return valid;
    }
  }

  private final ItemSorter itemSorter = new ItemSorter();

  private void recurseAndCount(SectionNode node, Map<String, Set<String>> typeToCount) {
    String childType = node.getChildType();

    if (childType != null) { // i.e. not leaf
      // logger.info("recurseAndCount on " + node.getName() + " child type " + childType);
      Set<String> members = typeToCount.computeIfAbsent(childType, k -> new TreeSet<>(itemSorter));

      for (SectionNode child : node.getChildren()) {
        if (!child.getType().equals(childType)) {
          logger.error("child " + child + " doesn't match " + childType);
        }
        members.add(child.getName());
        recurseAndCount(child, typeToCount);
      }
    }
  }

  /**
   * Make arbitrary order here...
   * TODO : maybe let user configure this somehow.
   *
   * @param types
   */
  public void reorderTypes(List<String> types) {
    //logger.info("reorderTypes " + types);
    putAtEnd(types, TOPIC);
    putAtEnd(types, SUB_TOPIC);
    putAtEnd(types, SUBTOPIC);
    putAtEnd(types, GRAMMAR);
    putAtEnd(types, DIALECT);
    putAtEnd(types, DIFFICULTY);
    //logger.info("reorderTypes " + types);
  }

  private void putAtEnd(List<String> types, String sound) {
    if (types.contains(sound)) {
      types.remove(sound);
      types.add(sound);
    } else {
      String o = sound.toLowerCase();
      if (types.contains(o)) {
        types.remove(o);
        types.add(o);
      }
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

  @Override
  public Collection<SectionNode> getSectionNodesForTypes() {
    return root.getChildren();
  }

  @Override
  public SectionNode getFirstNode(String name) {
    return getMatch(getSectionNodesForTypes(), name);
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
    if (!parent.isLeaf() && parent.getChildType().equals(type)) {
      return getMatch(parent.getChildren(), name);
    } else {
      return null;
    }
  }

  @Nullable
  private SectionNode getMatch(Collection<SectionNode> sectionNodesForTypes, String name) {
    for (SectionNode node : sectionNodesForTypes) {
      if (node.getName().equals(name)) return node;
    }
    return null;
  }


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
   * @see #report
   */
  public Collection<T> getExercisesForSelectionState(String type, String value) {
    Map<String, Collection<String>> typeToSection = new HashMap<>();
    typeToSection.put(type, Collections.singleton(value));
    return getExercisesForSelectionState(typeToSection);
  }

  /**
   * JUST FOR TESTING
   *
   * @param type
   * @param value
   * @return
   */
  public Map<String, Set<String>> getTypeToMatches(String type, String value) {
    return getTypeToMatch(type, value, this.root);
  }

  /**
   * @param pairs
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getTypeToValues
   */
  @Override
  public Map<String, Set<MatchInfo>> getTypeToMatches(Collection<Pair> pairs) {
    Map<String, Map<String, MatchInfo>> typeToMatchPairs = getTypeToMatchPairs(new ArrayList<>(pairs), this.root);
    return getTypeToMatches(typeToMatchPairs);
  }

  /**
   * Assumes a partial path implies = type=any for unmentioned?
   *
   * @param pairs
   * @param node
   * @return
   */
  @NotNull
  private Map<String, Map<String, MatchInfo>> getTypeToMatchPairs(List<Pair> pairs, SectionNode node) {
    Map<String, Map<String, MatchInfo>> typeToMatch = new HashMap<>();

    if (pairs.isEmpty()) {
      logger.warn("getTypeToMatchPairs no pairs for " + node);
      return typeToMatch;
    }

    Iterator<Pair> iterator = pairs.iterator();
    Pair next = iterator.next();
    String type = next.getProperty();
    String toMatch = next.getValue();

    boolean isAll = toMatch.equalsIgnoreCase(ANY) || toMatch.equalsIgnoreCase(ALL);
    if (DEBUG) logger.info("getTypeToMatchPairs to match " + type + "=" + toMatch + " out of " + pairs + " is all " + isAll);
    if (!node.isLeaf() && node.getChildType().equals(type)) {
      Map<String, MatchInfo> matches = new HashMap<>();
      typeToMatch.put(type, matches);

      Collection<SectionNode> childMatches = null;

      if (isAll) {
        childMatches = node.getChildren();
        for (SectionNode child : childMatches) {
          if (!child.getType().equals(type)) {
            logger.error("huh? adding " + child + " to " + type);
          }

          addOrMerge(matches, child);
        }
      } else {
        for (SectionNode child : node.getChildren()) {
          if (child.getName().equals(toMatch)) {
//            matches.add(new MatchInfo(child));
            addOrMerge(matches, child);
            childMatches = new ArrayList<>();
            childMatches.add(child);
            break;
          }
        }
      }

      if (DEBUG) {
        logger.info("getTypeToMatchPairs children of " + node.getName() + " match for " + next + " is " + (childMatches == null ? "none" : childMatches.size()));
      }

      if (childMatches == null || childMatches.isEmpty()) {  // couldn't find matching value
        return typeToMatch;
      } else {
        iterator.remove();
        if (DEBUG) logger.info("getTypeToMatchPairs path now " + pairs);

        if (pairs.isEmpty()) {
          for (SectionNode childMatch : childMatches) {
            recurseAndCountMatchInfo(childMatch, typeToMatch);
          }
          return typeToMatch;
        } else {
          if (DEBUG) logger.info("recurse on " + childMatches.size() + " with path " + pairs);

          for (SectionNode childMatch : childMatches) {
            Map<String, Map<String, MatchInfo>> typeToMatchPairs = getTypeToMatchPairs(new ArrayList<>(pairs), childMatch);
            mergeMaps2(typeToMatch, typeToMatchPairs);
          }
        }
      }
    } else {
      Map<String, MatchInfo> matches = new HashMap<>();
      typeToMatch.put(type, matches);
      Map<String, MatchInfo> matchesForType = typeToMatchInfo.get(type);
      if (matchesForType == null) {
        if (!type.equals(LISTS)) {
          logger.warn("getTypeToMatchPairs no known type " + type + " in " + typeToMatchInfo.keySet());
        }
      } else {
        matches.putAll(matchesForType);
      }
      iterator.remove();

      if (pairs.isEmpty()) {
        if (!type.equals(LISTS)) {
          logger.warn("getTypeToMatchPairs : huh? pairs is empty for type " + type);
        }
      } else {
        for (SectionNode child : node.getChildren()) {
          Map<String, Map<String, MatchInfo>> typeToMatchPairs = getTypeToMatchPairs(pairs, child);
          mergeMaps2(typeToMatch, typeToMatchPairs);
        }
      }
    }

    return typeToMatch;
  }

  /**
   * @param typeToMatch
   * @return
   * @see #getTypeToMatches
   */
  private Map<String, Set<MatchInfo>> getTypeToMatches(Map<String, Map<String, MatchInfo>> typeToMatch) {
    Map<String, Set<MatchInfo>> typeToMatchRet = new HashMap<>();
    for (Map.Entry<String, Map<String, MatchInfo>> pair : typeToMatch.entrySet()) {
      typeToMatchRet.put(pair.getKey(), new TreeSet<>(pair.getValue().values()));
    }
    return typeToMatchRet;
  }

  private void recurseAndCountMatchInfo(SectionNode node, Map<String, Map<String, MatchInfo>> typeToCount) {
    String childType = node.getChildType();

    if (childType != null) { // i.e. not leaf
      // logger.info("recurseAndCount on " + node.getName() + " child type " + childType);
      Map<String, MatchInfo> members = typeToCount.get(childType);
      if (members == null) {
        typeToCount.put(childType, members = new HashMap<>());
      }

      for (SectionNode child : node.getChildren()) {
        if (!child.getType().equals(childType)) {
          logger.error("child " + child + " doesn't match " + childType);
        }

        addOrMerge(members, child);
        recurseAndCountMatchInfo(child, typeToCount);
      }
    }
  }

  private void addOrMerge(Map<String, MatchInfo> matches, SectionNode child) {
    String name = child.getName();
    MatchInfo matchInfo = matches.get(name);

    if (DEBUG) logger.info("addOrMerge " + name + " match info " + matchInfo);
    if (matchInfo == null) {
      matches.put(name, new MatchInfo(child));
    } else {
      int count = child.getCount();
      if (DEBUG)
        logger.info("\taddOrMerge " + name + " add " + child.getName() + "=" + child.getCount() + " to " + matchInfo);
      matchInfo.incr(count);
    }
  }

  private void mergeMaps(Map<String, Set<String>> typeToMatch,
                         Map<String, Set<String>> typeToMatch1) {
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

  private void mergeMaps2(Map<String, Map<String, MatchInfo>> current,
                          Map<String, Map<String, MatchInfo>> toMergeIn) {
    for (Map.Entry<String, Map<String, MatchInfo>> mergePair : toMergeIn.entrySet()) {
      String type = mergePair.getKey();
      Map<String, MatchInfo> currentMatches = current.get(type);
      Map<String, MatchInfo> mergeMatches = mergePair.getValue();
      if (currentMatches == null) {
        current.put(type, mergeMatches);
      } else {
//        Map<String, MatchInfo> currentNameToInfo = getNameToInfo(currentMatches);
//        Map<String, MatchInfo> toMergeNameToInfo = getNameToInfo(mergeMatches);
        Set<String> toAdd = new HashSet<>(mergeMatches.keySet());

        // so for every name that overlap, increment the count
        for (Map.Entry<String, MatchInfo> currentNameToInfo : currentMatches.entrySet()) {
          String name = currentNameToInfo.getKey();
          MatchInfo matchInfo = mergeMatches.get(name);
          if (matchInfo != null) {
            currentNameToInfo.getValue().incr(matchInfo.getCount());
            toAdd.remove(name);
          }
        }

        // for every new name, add the match info
        for (String name : toAdd) currentMatches.put(name, mergeMatches.get(name));
        //currentMatches.addAll(mergeMatches);
      }
    }
  }
/*
  private Map<String, MatchInfo> getNameToInfo(Set<MatchInfo> currentMatches) {
    Map<String, MatchInfo> nameToInfo = new HashMap<>();
    for (MatchInfo info : currentMatches) nameToInfo.put(info.getValue(), info);
    return nameToInfo;
  }*/

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
        mergeMaps(typeToMatch, getTypeToMatch(type, value, child));
      }
    }

    return typeToMatch;
  }

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

        if (DEBUG)
          logger.info("getExercisesForSelectionState query " + type + " = " + pair.getValue() + " -> " + exercisesForSection.size());

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
          logger.warn("getExercisesForSection : Couldn't find section '" + section + "' in type " + type + " keys " + sectionToLesson.keySet());
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

  /**
   * @param exercise
   * @param pairs
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#addPhoneInfo
   */
  public void addPairs(T exercise, List<Pair> pairs) {
    pairs.forEach(pair -> addExerciseToLesson(exercise, pair));
  }

  private void addExerciseToLesson(T exercise, Pair pair) {
    addPairEntry(exercise, pair);
    exercise.addPair(pair);
  }

  private Pair getPairForExerciseAndLesson(T exercise, String type, String unitName) {
    addPairEntry(exercise, type, unitName);
    return new Pair(type, unitName);
  }

  private void addPairEntry(T exercise, Pair pair) {
    addUnitNameEntry(exercise, pair.getValue(), getSectionToLesson(pair.getProperty()));
  }

  private void addPairEntry(T exercise, String type, String unitName) {
    addUnitNameEntry(exercise, unitName, getSectionToLesson(type));
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
   * @seex mitll.langtest.server.database.DatabaseImpl#deleteItem
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
        } else {
          logger.info("removeExercise remove " + exercise.getID() + " " + exercise);
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
    if (section.isEmpty()) {
      logger.error("huh? section is empty ", new Exception());
    }
    Map<String, Lesson<T>> unit = typeToUnitToLesson.get(section);
    if (unit == null) {
      typeToUnitToLesson.put(section, unit = new HashMap<>());
    }
    return unit;
  }

  /**
   * @param predefinedTypeOrder
   * @see mitll.langtest.server.database.exercise.ExcelImport#readFromSheet
   */
  @Override
  public void setPredefinedTypeOrder(List<String> predefinedTypeOrder) {
    this.predefinedTypeOrder = predefinedTypeOrder;
  }

  @Override
  public void report() {
    logger.debug("report : type order " + getTypeOrder());
    for (String key : typeToUnitToLesson.keySet()) {
      Map<String, Lesson<T>> categoryToLesson = typeToUnitToLesson.get(key);
      Set<String> sections = new TreeSet<>(categoryToLesson.keySet());
      if (!sections.isEmpty()) {
        String message = "\treport : " + key + " = (" + sections.size() + ") " + sections;

        logger.debug(message);
        //  writer.write(message);

        for (String section : sections) {
          Lesson<T> tLesson = categoryToLesson.get(section);
          String message1 = "\t\t" + section + " : " + tLesson.size();
          logger.debug(message1);
          //  writer.write(message1);

        }
        // writer.write("\n");
      }
    }
    logger.debug("\t# section nodes " + getSectionNodesForTypes().size());
    for (SectionNode node : getSectionNodesForTypes()) {
      logger.info("--- node " + node.getType() + " : " + node.getName());
      Collection<T> exercisesForSelectionState = getExercisesForSelectionState(node.getType(), node.getName());
      String message = "\tfor " + node.toComplete(0) + " got " + exercisesForSelectionState.size();
      logger.info(message);
      //  writer.write(message);
      //  writer.write("\n");
    }

//      writer.close();
    //   } catch (IOException e) {
    //     e.printStackTrace();
    //   }
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

  private Map<String, Set<String>> typeToCount = new HashMap<>();
  private Map<String, Map<String, MatchInfo>> typeToMatchInfo = new HashMap<>();
  private Map<String, Set<MatchInfo>> typeToDistinct = new HashMap<>();

  /**
   * @param predefinedTypeOrder
   * @param seen
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#getExercises
   */
  public void rememberTypesInOrder(final List<String> predefinedTypeOrder, List<List<Pair>> seen) {
    SectionNode child = root;
    //if (seen.isEmpty()) logger.error("huh? no types to remember?");

    if (DEBUG) {
      logger.info("rememberTypesInOrder" +
          "\n\ttype order " + predefinedTypeOrder +
          "\n\troot " + root.getName() +
          "\n\tchildren  " + root.getChildren().size() +
          "\n\tnum seen " + seen.size());
    }

    for (List<Pair> pairs : seen) {
      child = rememberOne(predefinedTypeOrder, child, pairs);
    }

    typeToCount = new HashMap<>();
    recurseAndCount(root, typeToCount);

    typeToMatchInfo = new HashMap<>();
    recurseAndCountMatchInfo(root, typeToMatchInfo);

    typeToDistinct = new HashMap<>();
    for (Map.Entry<String, Map<String, MatchInfo>> p : typeToMatchInfo.entrySet()) {
      typeToDistinct.put(p.getKey(), new TreeSet<>(p.getValue().values()));
    }

    if (DEBUG) logger.info("rememberTypesInOrder type->childCount " + typeToCount);
  }

  private void makeRoot() {
    root = new SectionNode("root", "root");
  }

  private int spew = 0;

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
        int i = predefinedTypeOrder.indexOf(o1.getProperty());
        int anotherInteger = predefinedTypeOrder.indexOf(o2.getProperty());
        if (i > 0 && anotherInteger == -1) return -1;
        else if (i == -1 && anotherInteger > 0) return +1;
        else if (i == -1 && anotherInteger == -1) return o1.getProperty().compareTo(o2.getProperty());
        else return Integer.compare(i, anotherInteger);
      }
    });

/*
    if (pairs.size() != 3 && spew++ < 100)
      logger.info("after " + pairs);
*/

//    if (DEBUG) logger.info("for " + child + " got types " + predefinedTypeOrder + " " + pairs.size());

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

  private void rememberTypesFor(List<List<Pair>> seen) {
    SectionNode child = root;

    for (List<Pair> pairs : seen) child = rememberOne(child, pairs);
    recurseAndCount(root, typeToCount = new HashMap<>());

    logger.info("rememberTypesFor type->childCount " + typeToCount);
  }

  public Map<String, Set<MatchInfo>> getTypeToDistinct() {
    return typeToDistinct;
  }

  /**
   * @param child
   * @param pairs
   * @return
   * @see #addExercise
   */
  private SectionNode rememberOne(SectionNode child, List<Pair> pairs) {
    for (Pair pair : pairs) {
      child = child.getChild(pair.getProperty(), pair.getValue());

      if (child == null) {
        logger.warn("huh? no child of " + child + " for " + pair);
      }
    }
    child = root;
    return child;
  }

  public SectionNode getRoot() {
    return root;
  }

  @Override
  public Set<String> getRootTypes() {
    return rootTypes;
  }

  @Override
  public void setRootTypes(Set<String> rootTypes) {
    this.rootTypes = rootTypes;
  }

  @Override
  public Map<String, String> getParentToChildTypes() {
    return parentToChildTypes;
  }

  @Override
  public void setParentToChildTypes(Map<String, String> parentToChildTypes) {
    this.parentToChildTypes = parentToChildTypes;
  }

  /**
   * @param request
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getTypeToValues
   */
  @Override
  public FilterResponse getTypeToValues(FilterRequest request) {
    List<Pair> typeToSelection = request.getTypeToSelection();
    List<String> typesInOrder = getTypesFromRequest(typeToSelection);
    Set<String> typesToInclude1 = new HashSet<>(typesInOrder);

    logger.info("getTypeToValues typesToInclude1 " + typesToInclude1);
    logger.info("getTypeToValues request is      " + typeToSelection);
    Map<String, Set<MatchInfo>> typeToMatches = getTypeToMatches(typeToSelection);
    logger.info("getTypeToValues typeToMatches is " + typeToMatches);

    boolean someEmpty = checkIfAnyTypesAreEmpty(typesInOrder, typesToInclude1, typeToMatches);

    int userListID = request.getUserListID();
    if (someEmpty) {
      List<Pair> typeToSelection2 = new ArrayList<>();
      logger.info("getTypeToValues back off including  " + typesToInclude1);
      for (Pair pair : typeToSelection) {
        if (typesToInclude1.contains(pair.getProperty())) {
          typeToSelection2.add(pair);
        } else {
          typeToSelection2.add(new Pair(pair.getProperty(), "all"));
        }
      }
      logger.info("getTypeToValues try search again with " + typeToSelection2);

      Map<String, Set<MatchInfo>> typeToMatches1 = getTypeToMatches(typeToSelection2);
      return new FilterResponse(request.getReqID(), typeToMatches1, typesToInclude1, userListID);
    } else {
      logger.info("getTypeToValues typeToMatches " + typeToMatches + " to include " + typesToInclude1);

      return new FilterResponse(request.getReqID(), typeToMatches, typesToInclude1, userListID);
    }
  }

  @NotNull
  private List<String> getTypesFromRequest(List<Pair> typeToSelection) {
    List<String> typesInOrder = new ArrayList<>(typeToSelection.size());
    typeToSelection.forEach(pair -> typesInOrder.add(pair.getProperty()));
    return typesInOrder;
  }

  /**
   * Removes types that have no matches...
   *
   * @param typesInOrder
   * @param typesToInclude1
   * @param typeToMatches
   * @return
   */
  private boolean checkIfAnyTypesAreEmpty(List<String> typesInOrder,
                                          Set<String> typesToInclude1,
                                          Map<String, Set<MatchInfo>> typeToMatches) {
    boolean someEmpty = false;
    for (String type : typesInOrder) {
      Set<MatchInfo> matches = typeToMatches.get(type);
      if (matches == null || matches.isEmpty()) {
        typesToInclude1.remove(type);
        logger.info("checkIfAnyTypesAreEmpty removing " + type + " matches = " + matches);
        someEmpty = true;
      }
    }
    return someEmpty;
  }
}