package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.List;

/**
 * Includes a reqID so if the client gets responses out of order it can ignore responses to old requests.
 * Also includes the first exercise.
 * The list of exercises here is just a list of {@link mitll.langtest.shared.CommonShell} objects to
 * reduce the bytes sent to get the exercise list. Perhaps in the future we could move to an async list.
 * <p/>
 * User: GO22670
 * Date: 6/12/13
 * Time: 4:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExerciseListWrapper implements IsSerializable {
  public ExerciseListWrapper() {} // req for serialization

  private int reqID;
  private List<CommonShell> exercises;
  private CommonExercise firstExercise;

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#makeExerciseListWrapper
   * @param reqID
   * @param ids
   * @param firstExercise
   */
  public ExerciseListWrapper(int reqID, List<CommonShell> ids, CommonExercise firstExercise) {
    this.reqID = reqID;
    this.exercises = ids;
    this.firstExercise = firstExercise;
  }

  public int getReqID() {
    return reqID;
  }
  public List<CommonShell> getExercises() {
    return exercises;
  }
  public CommonExercise getFirstExercise() { return firstExercise;  }

  public String toString() { return "req " + reqID + " has " + exercises.size() + " exercises, first is " + firstExercise.getID(); }
}
