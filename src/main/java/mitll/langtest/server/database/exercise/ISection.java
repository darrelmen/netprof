/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.exercise;

import mitll.langtest.shared.exercise.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by go22670 on 3/9/17.
 */
public interface ISection<T extends HasID & HasUnitChapter> {
  void clear();

  List<String> getTypeOrder();

  @NotNull
  List<String> getUniq(List<String> types);

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

  List<Pair> getPairs(Collection<String> typeOrder, int id, String unit, String lesson);
  List<Pair> getPairs(String first, String second, int id, String unit, String lesson);

  void addPairs(T t,
                CommonExercise exercise,
                Collection<String> attrTypes,
                List<Pair> pairs, boolean onlyIncludeFacetAttributes);

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
