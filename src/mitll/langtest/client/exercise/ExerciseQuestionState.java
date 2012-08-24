package mitll.langtest.client.exercise;

import com.google.gwt.user.client.ui.Widget;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 7/6/12
 * Time: 2:57 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ExerciseQuestionState {
  void recordIncomplete(Widget answer);

  void recordCompleted(Widget answer);
}
