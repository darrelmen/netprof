package mitll.langtest.client.recorder;

import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.BootstrapExercisePanel;
import mitll.langtest.client.bootstrap.ScoreFeedback;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/1/13
 * Time: 6:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class FeedbackRecordPanel extends SimpleRecordExercisePanel {
  public FeedbackRecordPanel(Exercise e, LangTestDatabaseAsync service, UserFeedback userFeedback, ExerciseController controller) {
    super(e, service, userFeedback, controller);
  }

  private AutoCRTRecordPanel autoCRTRecordPanel;
 // private ScoreFeedback scoreFeedback;

  @Override
  protected Widget getAnswerWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {
    autoCRTRecordPanel = new AutoCRTRecordPanel(service, controller, exercise, this, index);
    Panel panel = autoCRTRecordPanel.getPanel();
    return getVerticalPanel(panel,new ScoreFeedback(true));
  }

  public Panel getVerticalPanel(Panel panel, ScoreFeedback scoreFeedback) {
    FluidContainer container = new FluidContainer();
    FluidRow row1 = new FluidRow();
    container.add(row1);
    row1.add(panel);

   // scoreFeedback = new ScoreFeedback(true);

    SimplePanel simplePanel = new SimplePanel(scoreFeedback.getFeedbackImage());
    simplePanel.addStyleName("floatLeft");

    Panel scoreFeedbackRow = scoreFeedback.getSimpleRow(simplePanel,40);
    scoreFeedback.getScoreFeedback().setWidth(Window.getClientWidth() * 0.5 + "px");
    scoreFeedback.getScoreFeedback().addStyleName("topBarMargin");

    container.add(scoreFeedbackRow);

    HTML warnNoFlash = new HTML(BootstrapExercisePanel.WARN_NO_FLASH);
    autoCRTRecordPanel.setSoundFeedback(new SoundFeedback(controller.getSoundManager(), warnNoFlash));
    autoCRTRecordPanel.setScoreFeedback(scoreFeedback);
    warnNoFlash.setVisible(false);
    FluidRow row3 = new FluidRow();
    container.add(row3);
    row3.add(warnNoFlash);

    return container;
  }

  @Override
  protected String getInstructions() {
    return "";
  }
}
