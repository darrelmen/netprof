package mitll.langtest.server.database.exercise;

import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.SlickExercise;

import java.io.InputStream;
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

  /**
   * @see ExcelImport#readExercises(InputStream)
   * @return
   */
  boolean allKeysValid();

  Collection<SectionNode> getSectionNodesForTypes();

  /**
   * Initial map of facet to all possible values for facet
   * @see mitll.langtest.server.database.project.ProjectManagement#setStartupInfo
   * @return
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
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#addPhoneInfo
   * @param exercise
   * @param pair
   */
  void addPairs(T exercise, List<Pair> pair);

  boolean removeExercise(T exercise);

  void refreshExercise(T exercise);

  /**
   * @see DBExerciseDAO#setRootTypes
   * @param predefinedTypeOrder
   */
  void setPredefinedTypeOrder(List<String> predefinedTypeOrder);

  void report();

 /**
  * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#getExercises
  * @param predefinedTypeOrder
  * @param seen
  */
  void rememberTypesInOrder(final List<String> predefinedTypeOrder, List<List<Pair>> seen);

  /**
   * @see DBExerciseDAO#getTypeOrderFromProject
   * @param types
   */
  void reorderTypes(List<String> types);

  /**
   * @see mitll.langtest.server.database.project.ProjectManagement#setStartupInfo
   * @return
   */
  Set<String> getRootTypes();

  /**
   * @see DBExerciseDAO#setRootTypes
   * @param rootTypes
   */
  void setRootTypes(Set<String> rootTypes);

  /**
   * @see mitll.langtest.server.database.project.ProjectManagement#setStartupInfoOnUser
   * @return
   */
  Map<String, String> getParentToChildTypes();

  /**
   * @see DBExerciseDAO#setRootTypes
   * @param parentToChildTypes
   */
  void setParentToChildTypes(Map<String, String> parentToChildTypes);

  /**
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getTypeToValues
   * @param request
   * @return
   */
  FilterResponse getTypeToValues(FilterRequest request);
}
