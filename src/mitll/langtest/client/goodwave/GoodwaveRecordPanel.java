/**
 * 
 */
package mitll.langtest.client.goodwave;

import com.goodwave.client.GoodWavePanel;
import com.goodwave.client.sound.SoundManagerStatic;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseQuestionState;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.client.recorder.SimpleRecordPanel;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

/**
 *
 * Has three parts -- record/stop button, audio validity feedback, and audio html 5 control to playback audio just posted to the server.
 *
 * On click on the stop button, posts audio to the server.
 *
 * Automatically stops recording after 20 seconds.
 *
 * @author Gordon Vidaver
 * @deprecated
 */
public class GoodwaveRecordPanel extends GoodWavePanel {
  /**
   * Has three parts -- record/stop button, audio validity feedback icon, and the audio control widget that allows playback.
   *
   * @see mitll.langtest.client.SimpleRecordExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   */
	public GoodwaveRecordPanel(final LangTestDatabaseAsync service, final ExerciseController controller,
                             final Exercise exercise, final ExerciseQuestionState questionState, final int index){
    super("",false,false,true,true,null, new SoundManagerStatic());
    load();
    RecordButtonPanel widget = new RecordButtonPanel(service, controller, exercise, questionState, index);
    addButton(widget);
  }
}
