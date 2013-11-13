package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ResponseChoice;

import mitll.langtest.client.user.UserFeedback;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/8/13
 * Time: 4:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResponseExerciseList extends FlexSectionExerciseList /*implements ResponseTypeView*/ {
  private ResponseChoice responseChoice;
  //WirePlaces wirePlaces;

  /**
   * @see mitll.langtest.client.ExerciseListLayout#makeExerciseList(com.github.gwtbootstrap.client.ui.FluidRow, boolean, mitll.langtest.client.user.UserFeedback, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
   * @param secondRow
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param showTurkToken
   * @param showInOrder
   * @param controller
   * @param isCRTDataMode
   */
  public ResponseExerciseList(FluidRow secondRow, Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                              UserFeedback feedback, boolean showTurkToken, boolean showInOrder,
                              final ExerciseController controller, boolean isCRTDataMode) {
    super(secondRow, currentExerciseVPanel, service, feedback, showTurkToken, showInOrder, controller, isCRTDataMode);
    String responseType = controller.getProps().getResponseType();
   // wirePlaces = new WirePlaces(responseType);
   // wirePlaces.wire(this,responseType);
    responseChoice = new ResponseChoice(responseType, new ResponseChoice.ChoiceMade() {
      @Override
      public void choiceMade(String responseType) {
        setHistoryItem(History.getToken());
        controller.getProps().setResponseType(responseType);
      }
    });
  }

  /**
   * Adds the response type to the end of the history token
   * @param historyToken
   */
  protected void setHistoryItem(String historyToken) {
  //  System.out.println("history token '" +historyToken+"'");
    historyToken = historyToken.contains("###") ? historyToken.split("###")[0] : historyToken;
   // System.out.println("history token '" +historyToken+ "'");
    String historyToken1 =historyToken + "###" +  "responseType=" + responseChoice.getResponseType();
    //System.out.println("history token1 '" +historyToken1+"'");
    History.newItem(historyToken1);
  }

  @Override
  protected Widget addBottomText(FluidContainer container) {
    Widget widget = super.addBottomText(container);
    container.add(responseChoice.getResponseTypeWidget());
    return widget;
  }

/*  @Override
  public void selectResponseType(String responseType) {
    controller.getProps().setResponseType(responseType);
  }*/
}
