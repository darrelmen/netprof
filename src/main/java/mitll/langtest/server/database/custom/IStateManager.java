package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.reviewed.IReviewedDAO;
import mitll.langtest.server.database.reviewed.StateCreator;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.STATE;
import mitll.langtest.shared.exercise.Shell;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by go22670 on 7/5/17.
 */
public interface IStateManager {


  @NotNull
  Set<Integer> getDefectIDs();

  void setState(Shell shell, STATE state, long creatorID);

  void setSecondState(Shell shell, STATE state, long creatorID);

  void removeReviewed(int exerciseid);

  STATE getCurrentState(int exerciseID);

  IReviewedDAO getReviewedDAO();

  IReviewedDAO getSecondStateDAO();

  Map<Integer, StateCreator> getExerciseToState(boolean skipUnset);

  /**
   * @see mitll.langtest.server.services.ExerciseServiceImpl#markStateForActivity
   * @see UserListManager#getCommonUserList
   * @param shells
   */
  void markState(Collection<? extends CommonShell> shells);

  Collection<Integer> getDefectExercises();

  Collection<Integer> getInspectedExercises();
}
