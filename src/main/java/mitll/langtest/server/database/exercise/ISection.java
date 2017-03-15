package mitll.langtest.server.database.exercise;

import mitll.langtest.shared.exercise.SectionNode;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by go22670 on 3/9/17.
 */
public interface ISection<T> {
  void clear();

  List<String> getTypeOrder();

  boolean allKeysValid();

 // Collection<SectionNode> getRootNodes();
  Collection<SectionNode> getSectionNodesForTypes();

  /**
   * Initial map of facet to all possible values for facet
   * @return
   */
  Map<String, Set<String>> getTypeToDistinct();
  //Collection<SectionNode> getSectionNodesForTypes2();

/*
  @Deprecated
  Collection<SectionNode> getSectionNodesForTypes(List<String> typeOrder);
*/


  Collection<T> getExercisesForSimpleSelectionState(Map<String, String> simpleMap);

  Collection<T> getExercisesForSelectionState(String type, String value);

  Collection<T> getExercisesForSelectionState(Map<String, Collection<String>> typeToSection);

  void addExercise(T exercise);

  /**
   * @param exercise
   * @param type
   * @param unitName
   * @return
   */
  Pair addExerciseToLesson(T exercise, String type, String unitName);

  /**
   * @param exercise
   * @param type
   * @param unitName
   * @return
   */
  Pair getPairForExerciseAndLesson(T exercise, String type, String unitName);

  boolean removeExercise(T exercise);

  void refreshExercise(T exercise);

  void setPredefinedTypeOrder(List<String> predefinedTypeOrder);

  //void addAssociations(Collection<Pair> pairs);

  void report();

  void rememberTypesInOrder(final List<String> predefinedTypeOrder, List<List<Pair>> seen);

  void rememberTypesFor(List<List<Pair>> seen);

  SectionNode getRoot();

  SectionNode getFirstNode(String name);

  SectionNode getNode(SectionNode node, String type, String name);

}
