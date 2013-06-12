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

  public int reqID;
  public List<ExerciseShell> exercises;

  public ExerciseListWrapper(int reqID, List<ExerciseShell> ids) {
    this.reqID = reqID;
    this.exercises = ids;
  }
}
