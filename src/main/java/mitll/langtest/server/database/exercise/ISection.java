package mitll.langtest.server.database.exercise;

import mitll.langtest.shared.exercise.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by go22670 on 3/9/17.
 */
public interface ISection<T extends HasID & HasUnitChapter> {
  //SectionHelper<T> getCopy(List<T> exercises);

  void clear();

  List<String> getTypeOrder();

  /**
   * @return
   * @see ExcelImport#readExercises
   */
  boolean allKeysValid();

  Collection<SectionNode> getSectionNodesForTypes();

  /**
   * Initial map of facet to all possible values for facet
   *
   * @return
   * @see mitll.langtest.server.database.project.ProjectManagement#setStartupInfo
   */
  Map<String, Set<MatchInfo>> getTypeToDistinct();

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
   * @param pair
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#addPhoneInfo
   */
  void addPairs(T exercise, List<Pair> pair);

  List<Pair> getPairs(Collection<String> typeOrder, int id, String unit, String lesson, boolean ispredef);

  void addPairs(T t,
                CommonExercise exercise,
                Collection<String> attrTypes,
                List<Pair> pairs);

  boolean removeExercise(T exercise);

  void refreshExercise(T exercise);

  /**
   * @param predefinedTypeOrder
   * @see DBExerciseDAO#setRootTypes
   */
  void setPredefinedTypeOrder(List<String> predefinedTypeOrder);

  void report();

  Collection<T> getFirst();

  /**
   * @param predefinedTypeOrder
   * @param seen
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#getExercises
   */
  void rememberTypesInOrder(final List<String> predefinedTypeOrder, List<List<Pair>> seen);

  /**
   * @param types
   * @see DBExerciseDAO#getTypeOrderFromProject
   */
  void reorderTypes(List<String> types);

  /**
   * @return
   * @see mitll.langtest.server.database.project.ProjectManagement#setStartupInfo
   */
  Set<String> getRootTypes();

  /**
   * @param rootTypes
   * @see DBExerciseDAO#setRootTypes
   */
  void setRootTypes(Set<String> rootTypes);

  /**
   * @return
   * @see mitll.langtest.server.database.project.ProjectManagement#setStartupInfoOnUser
   */
  Map<String, String> getParentToChildTypes();

  /**
   * @param parentToChildTypes
   * @see DBExerciseDAO#setRootTypes
   */
  void setParentToChildTypes(Map<String, String> parentToChildTypes);

  /**
   * @param request
   * @param debug
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getTypeToValues
   */
  FilterResponse getTypeToValues(FilterRequest request, boolean debug);
}
