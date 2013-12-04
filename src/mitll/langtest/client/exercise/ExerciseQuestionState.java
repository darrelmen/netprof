package mitll.langtest.client.exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 7/6/12
 * Time: 2:57 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ExerciseQuestionState {
  void recordIncomplete(Object answer);
  void recordCompleted(Object answer);
}
