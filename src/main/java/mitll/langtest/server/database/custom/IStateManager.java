package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.reviewed.IReviewedDAO;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.exercise.STATE;

import java.util.Collection;

/**
 * Created by go22670 on 7/5/17.
 */
public interface IStateManager {
//  @NotNull
//  Set<Integer> getDefectIDs();

 void setState(HasID shell, STATE state, long creatorID);

  void setSecondState(HasID shell, STATE state, long creatorID);

 // void removeReviewed(int exerciseid);

  STATE getCurrentState(int exerciseID);

  IReviewedDAO getReviewedDAO();

  IReviewedDAO getSecondStateDAO();

//  Map<Integer, StateCreator> getExerciseToState(boolean skipUnset);

  /**
   * @paramx shells
   * @seex UserListManager#getCommonUserList
   * @see mitll.langtest.server.services.ExerciseServiceImpl#markStateForActivity
   */
//  void markState(Collection<? extends CommonShell> shells);

//  Collection<Integer> getDefectExercises();

  Collection<Integer> getInspectedExercises();
}
