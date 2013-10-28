package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/25/13
 * Time: 12:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class DataCollectionFlashcard extends BootstrapExercisePanel {
  private Panel leftButtonContainer, rightButtonContainer;
  private NavigationHelper navigationHelper;

  public DataCollectionFlashcard(Exercise e, LangTestDatabaseAsync service, ExerciseController controller) {
    super(e, service, controller);
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand () {
      public void execute () {
        int offsetHeight = cardPrompt.getOffsetHeight();
        DOM.setStyleAttribute(leftButtonContainer.getElement(), "marginTop", (offsetHeight / 2) + "px");
        DOM.setStyleAttribute(rightButtonContainer.getElement(), "marginTop", ((offsetHeight/2)-50) +"px");
      }
    });
    navigationHelper.enablePrevButton(!controller.onFirst(null));
  }

  @Override
  protected FlashcardRecordButtonPanel getAnswerWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, int index) {
    return new FlashcardRecordButtonPanel(this, service, controller, exercise, index, false);
  }

  protected Widget getCardPrompt(Exercise e, ExerciseController controller) {
    FluidRow questionRow = new FluidRow();
    Widget questionContent = getQuestionContent(e, controller);
    Column contentContainer = new Column(8, questionContent);
    contentContainer.addStyleName("blueBackground");
    contentContainer.addStyleName("userNPFContent");
    contentContainer.addStyleName("topMargin");
    contentContainer.addStyleName("marginBottomTen");
    navigationHelper = new NavigationHelper(e,controller,false);

    Widget prev = navigationHelper.getPrev();
    HorizontalPanel hp = getCenteredContainer(prev);
    leftButtonContainer = hp;
    questionRow.add(new Column(2, hp));
    questionRow.add(contentContainer);
    questionRow.add(new Column(2,rightButtonContainer = getCenteredContainer(navigationHelper.getNext())));

    return questionRow;
  }

  private HorizontalPanel getCenteredContainer(Widget prev) {
    HorizontalPanel hp = new HorizontalPanel();
    hp.setHeight("100%");
    hp.setWidth("100%");
    hp.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
    hp.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
    hp.add(prev);
    return hp;
  }

  @Override
  protected void onUnload() {
    super.onUnload();
    navigationHelper.removeKeyHandler();
  }
}
