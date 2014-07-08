package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 6/12/13
 * Time: 4:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExerciseListWrapper implements IsSerializable {
  public ExerciseListWrapper(){}

  private int reqID;
<<<<<<< HEAD
  private List<CommonShell> exercises;
  private CommonExercise firstExercise;

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#makeExerciseListWrapper(int, java.util.Collection)
=======
  private List<ExerciseShell> exercises;
  private Exercise firstExercise;

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#makeExerciseListWrapper(int, java.util.List)
>>>>>>> 9ea1717642f00415277fe4e6a352158a7530b162
   * @param reqID
   * @param ids
   * @param firstExercise
   */
<<<<<<< HEAD
  public ExerciseListWrapper(int reqID, List<CommonShell> ids, CommonExercise firstExercise) {
=======
  public ExerciseListWrapper(int reqID, List<ExerciseShell> ids, Exercise firstExercise) {
>>>>>>> 9ea1717642f00415277fe4e6a352158a7530b162
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

  public CommonExercise getFirstExercise() {
    return firstExercise;
  }
}
