package mitll.langtest.client.taboo;

import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.SelectionState;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.ExerciseShell;

import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/13/13
 * Time: 7:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class TabooExerciseList extends FlexSectionExerciseList {
  private boolean isGiver = true;
  private ReceiverExerciseFactory receiverFactory;

  /**
   * @see mitll.langtest.client.ExerciseListLayout#makeExerciseList(com.github.gwtbootstrap.client.ui.FluidRow, boolean, mitll.langtest.client.user.UserFeedback, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
   * @param secondRow
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param showTurkToken
   * @param showInOrder
   * @param showListBox
   * @param controller
   */
  public TabooExerciseList(FluidRow secondRow, Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                           UserFeedback feedback, boolean showTurkToken, boolean showInOrder, boolean showListBox,
                           ExerciseController controller) {
    super(secondRow, currentExerciseVPanel, service, feedback, showTurkToken, showInOrder, showListBox, controller);
  }

  /**
   *
   * TODO : how do we get the receiver to notify giver of chapter choices?
   *
   * @param factory
   * @param user
   * @param expectedGrades
   */
  @Override
  public void setFactory(ExercisePanelFactory factory, UserManager user, int expectedGrades) {
    super.setFactory(factory, user, expectedGrades);
    if (factory instanceof ReceiverExerciseFactory) {
      receiverFactory = (ReceiverExerciseFactory) factory;

      //receiverFactory.setExerciseList(this);
    }
 /*   if (!isGiver) {
      ((ReceiverExerciseFactory) factory).isSinglePlayer())
    }*/
    if (!isGiver) makeExercisePanel(null);
  }

  @Override
  public Widget getExerciseListOnLeftSide(PropertyHandler props) {
    Panel correctAndImageRow = new FlowPanel();

    return correctAndImageRow;
  }

  /**
   * @see #getWidgetsForTypes(long)
   * @param userID
   * @param container
   */
  @Override
  protected void getTypeOrder(long userID, FluidContainer container) {
    if (!isGiver) {
      super.getTypeOrder(userID, container);
    }
    else {
      addBottomText(container);
    }
  }

  public void setGiver(boolean isGiver) {
    this.isGiver = isGiver;
  }

  protected void rememberExercises(List<ExerciseShell> result) {
    SelectionState selectionState = getSelectionState(History.getToken());
    System.out.println("rememberExercises : user " + userID + " " + (isGiver ? " giver " : " receiver ") +
      " remembering " + result.size() + " exercises, " +
      "state is " + selectionState);
    currentExercises = result; // remember current exercises
    idToExercise = new HashMap<String, ExerciseShell>();
    clear();
    for (final ExerciseShell es : result) {
      idToExercise.put(es.getID(), es);
      if (isGiver) addExerciseToList(es);
    }
    flush();
    if (receiverFactory != null) {
      receiverFactory.setExerciseShells(getExerciseShells());
    }
    if (!isGiver) {
      service.registerSelectionState(userID, selectionState.getTypeToSection(), new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("Can't contact server.");
        }

        @Override
        public void onSuccess(Void result) {

        }
      });
    }
  }
}
