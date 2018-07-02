package mitll.langtest.server.database.exercise;

import mitll.langtest.shared.exercise.MatchInfo;
import mitll.langtest.shared.exercise.Pair;
import mitll.langtest.shared.exercise.SectionNode;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface ITestSection<T> {

  /**
   * JUST FOR TESTING
   * @param simpleMap
   * @return
   */
  Collection<T> getExercisesForSimpleSelectionState(Map<String, String> simpleMap);

  /**
   * JUST FOR TESTING
   * @param type
   * @param value
   * @return
   */
  Collection<T> getExercisesForSelectionState(String type, String value);

  Map<String, Set<MatchInfo>> getTypeToMatches(Collection<Pair> pairs, boolean debug);
  SectionNode getRoot();
  SectionNode getFirstNode(String name);
  SectionNode getNode(SectionNode node, String type, String name);

}
