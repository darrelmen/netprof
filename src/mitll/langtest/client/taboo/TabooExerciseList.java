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

import java.util.ArrayList;
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
   * If giving, show exercise list, hide selection buttons
   * if receiving, hide exercise list, show selection buttons
   * @param factory
   * @param user
   * @param expectedGrades
   */
  @Override
  public void setFactory(ExercisePanelFactory factory, UserManager user, int expectedGrades) {
    super.setFactory(factory, user, expectedGrades);
    if (factory instanceof ReceiverExerciseFactory) {
      receiverFactory = (ReceiverExerciseFactory) factory;
      isGiver = false;
      System.out.println("TabooExerciseList.setFactory : " + factory.getClass() + " for user " + user.getUser() + " RECEIVER ");
    }
    else {
      isGiver = true;
      System.out.println("TabooExerciseList.setFactory : " + factory.getClass() + " for user " + user.getUser()+ " GIVER ");
    }

    if (isGiver) {
      showExerciseList();
      if (buttonRow != null) buttonRow.setVisible(false);
    }
    else {
      hideExerciseList();
      if (buttonRow != null) buttonRow.setVisible(true);
    }
  }

  FluidContainer buttonRow;
  @Override
  protected void addBottomText(FluidContainer container) {
    super.addBottomText(container);
    buttonRow = container;
    if (isGiver) {
      System.out.println("\n\n----> GIVER:  addBottomText.hiding container for " + userID);
      container.setVisible(false);
      showExerciseList();
    } else {
      hideExerciseList();

      System.out.println("\n\n----> RECEIVER:  addBottomText.showing container for " + userID);
    }
  }

  /**
   * @see mitll.langtest.client.exercise.ExerciseList.SetExercisesCallback#onSuccess(mitll.langtest.shared.ExerciseListWrapper)
   * @see mitll.langtest.client.exercise.SectionExerciseList.MySetExercisesCallback#onSuccess(mitll.langtest.shared.ExerciseListWrapper)
   * @param result
   */
  protected void rememberExercises(List<ExerciseShell> result) {
    SelectionState selectionState = getSelectionState(History.getToken());
    System.out.println("rememberExercises : user " + userID + " " + (isGiver ? " giver " : " receiver ") +
      " remembering " + result.size() + " exercises, " +
      "state is '" + selectionState +"'");

    if (isGiver) {
      super.rememberExercises(result);
    }
    else {
      currentExercises = result; // remember current exercises
      idToExercise = new HashMap<String, ExerciseShell>();
      clear();
      for (final ExerciseShell es : result) {
        idToExercise.put(es.getID(), es);
        // addExerciseToList(es);
      }
      flush();
      if (receiverFactory != null) {
        receiverFactory.setExerciseShells(getExerciseShells());
      }
      tellPartnerMyChapterSelection(selectionState);
    }
  }

  private void tellPartnerMyChapterSelection(SelectionState selectionState) {
   // if (!isGiver) { // tell your partner you changed the chapter selection
      System.out.println("telling partner selection state for " + userID + " is " + selectionState);
      service.registerSelectionState(userID, selectionState.getTypeToSection(), new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("Can't contact server.");
        }

        @Override
        public void onSuccess(Void result) {}
      });
  //  }
  }

  @Override
  protected void onLastItem() {
    if (isGiver) {
      new ModalInfoDialog("Word list complete", "Perhaps you like to choose another chapter?");
    }
    else {
      List<String> message = new ArrayList<String>();
      message.add("Please wait for giver to choose another chapter.");
      message.add("Or you could stop playing by clicking sign out.");
      new ModalInfoDialog("Word list complete", message);
    }
  }
}
