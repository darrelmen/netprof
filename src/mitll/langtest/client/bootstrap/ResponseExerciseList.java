package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ResponseChoice;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.ExerciseShell;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/8/13
 * Time: 4:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResponseExerciseList<T extends ExerciseShell> extends FlexSectionExerciseList<T> {
  public static final String RESPONSE_TYPE = "responseType";
  private static final String SECOND_RESPONSE_TYPE = "secondResponseType";
  public static final String RESPONSE_TYPE_DIVIDER = "###";
  private static final String MSA = "MSA";
  private static final String ARABIC = "Arabic";
  private static final String RESPONSE_TYPE1 = " Response Type";
  private static final String ENGLISH = "English";
  private ResponseChoice responseChoice;
  private ResponseChoice secondResponseChoice;

  /**
   * @see mitll.langtest.client.ExerciseListLayout#makeExerciseList(com.github.gwtbootstrap.client.ui.FluidRow, boolean, mitll.langtest.client.user.UserFeedback, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
   * @param secondRow
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param showTurkToken
   * @param showInOrder
   * @param controller
   */
  public ResponseExerciseList(FluidRow secondRow, Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                              UserFeedback feedback, boolean showTurkToken, boolean showInOrder,
                              final ExerciseController controller, String instance) {
    super(secondRow, currentExerciseVPanel, service, feedback, showTurkToken, showInOrder, controller, !controller.getProps().isCRTDataCollectMode(), instance);
    String responseType = controller.getProps().getResponseType();
    responseChoice = new ResponseChoice(responseType, new ResponseChoice.ChoiceMade() {
      @Override
      public void choiceMade(String responseType) {
        controller.getProps().setResponseType(responseType);
        setHistoryItem(History.getToken());
      }
    });

    String secondResponseType = controller.getProps().getSecondResponseType();
    secondResponseChoice = new ResponseChoice(secondResponseType, new ResponseChoice.ChoiceMade() {
      @Override
      public void choiceMade(String responseType) {
        controller.getProps().setSecondResponseType(responseType);
        setHistoryItem(History.getToken());
      }
    });
  }

  /**
   * Adds the response type to the end of the history token
   * @param historyToken
   */
  protected void setHistoryItem(String historyToken) {
    historyToken = historyToken.contains(RESPONSE_TYPE_DIVIDER) ? historyToken.split(RESPONSE_TYPE_DIVIDER)[0] : historyToken;
    String historyToken1 =historyToken +
      RESPONSE_TYPE_DIVIDER + RESPONSE_TYPE + "=" + responseChoice.getResponseType() +
    "***" + SECOND_RESPONSE_TYPE + "=" + secondResponseChoice.getResponseType()
      ;
    History.newItem(historyToken1);
  }

  /**
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#addButtonRow(java.util.List, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection, boolean)
   * @param container
   * @return
   */
  @Override
  protected Widget addBottomText(FluidContainer container) {
    DivWidget right = new DivWidget();
    right.addStyleName("leftFiftyMargin");
    right.add(getStatusRow());
    right.add(getResponseChoiceWidget());

    container.add(right);
    return right;
  }

  private Widget getResponseChoiceWidget() {
    Grid grid = new Grid(2,2);
    grid.getElement().setId("ResponseChoiceWidget");
    String caption = (controller.getLanguage().equals(MSA)? ARABIC : controller.getLanguage()) + RESPONSE_TYPE1;
    ResponseChoice.LeftRight leftRight1 = responseChoice.getResponseTypeWidget(caption, false);
    grid.setWidget(0, 0, leftRight1.left);
    grid.setWidget(0, 1, leftRight1.right);

    String caption2 = ENGLISH + RESPONSE_TYPE1;
    ResponseChoice.LeftRight leftRight2 = secondResponseChoice.getResponseTypeWidget(caption2, true);

    grid.setWidget(1, 0, leftRight2.left);
    grid.setWidget(1, 1, leftRight2.right);

    return grid;
  }
}
