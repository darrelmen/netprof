package mitll.langtest.client.taboo;

import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.DialogHelper;
import mitll.langtest.client.LangTestDatabaseAsync;
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
      if (buttonRow != null) {
        //buttonRow.setVisible(false);
        // TODO : this stuff doesn't work properly...
        for (int i = 0; i < buttonRow.getWidgetCount(); i++) {
          if (buttonRow.getWidget(i) != statusHeader) buttonRow.getWidget(i).setVisible(false);
        }
      }
    }
    else {
      hideExerciseList();
      if (buttonRow != null) {
        //buttonRow.setVisible(true);

        for (int i = 0; i < buttonRow.getWidgetCount(); i++) {
          if (buttonRow.getWidget(i) != statusHeader) buttonRow.getWidget(i).setVisible(true);
        }
      }
    }
  }

  protected void tellUserPanelIsBusy() {
    new ModalInfoDialog("Please wait", "Please wait for you partner to respond.");
  }

  private FluidContainer buttonRow;
  @Override
  protected void addBottomText(FluidContainer container) {
    super.addBottomText(container);
    buttonRow = container;
    if (isGiver) {
      System.out.println("----> GIVER:  addBottomText.hiding container for " + userID);
      //container.setVisible(false);
      for (int i = 0; i < container.getWidgetCount(); i++) {
        if (container.getWidget(i) != statusHeader) container.getWidget(i).setVisible(false);
      }
      showExerciseList();
    } else {
      System.out.println("----> RECEIVER:  addBottomText.showing container for " + userID);
      hideExerciseList();
      for (int i = 0; i < container.getWidgetCount(); i++) {
        if (container.getWidget(i) != statusHeader) container.getWidget(i).setVisible(true);
      }
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
      "state is '" + selectionState + "'");

    if (isGiver) {
      super.rememberExercises(result);
    } else {
      currentExercises = result; // remember current exercises
      idToExercise = new HashMap<String, ExerciseShell>();
      clear();
      for (final ExerciseShell es : result) {
        idToExercise.put(es.getID(), es);
        // addExerciseToList(es);
      }
      flush();
      if (receiverFactory != null) {
        System.out.println("rememberExercises : remembering " + result.size());
        receiverFactory.setExerciseShells(new ArrayList<ExerciseShell>(getExerciseShells()));
      }
      else {
        System.err.println("no factory!!! \n\n\n ");
      }
      tellPartnerMyChapterSelection(selectionState);
    }
  }

  private void tellPartnerMyChapterSelection(SelectionState selectionState) {
    System.out.println("telling partner selection state for " + userID + " is " + selectionState);
    service.registerSelectionState(userID, selectionState.getTypeToSection(), new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Can't contact server.");
      }

      @Override
      public void onSuccess(Void result) {}
    });
  }

  /**
   * TODO : get game statistics and show leaderboard plot
   *
   * So there are two stopping points - at the end of the game and the end of the chapter...
   * @see #loadNextExercise(mitll.langtest.shared.ExerciseShell)
   */
  @Override
  protected void onLastItem() {
    if (isGiver) {
      List<String> message = new ArrayList<String>();
      message.add("Please wait for receiver to choose another chapter.");
      message.add("Or you could stop playing by clicking sign out.");
      new ModalInfoDialog("Game complete!", message);
    } else {
      new ModalInfoDialog("Chapter(s) complete.", "Would you like to practice this chapter again?");
      new DialogHelper(true).showErrorMessage("Chapter(s) complete.", "Would you like to practice this chapter(s) again?", "Yes", new DialogHelper.CloseListener() {
        @Override
        public void gotYes() {
          receiverFactory.setExerciseShells(new ArrayList<ExerciseShell>(getExerciseShells()));
        }

        @Override
        public void gotNo() {

        }
      });
    }
  }

/*  @Override
  protected boolean isOnLastItem(int i) {
    if (isGiver) {
      return super.isOnLastItem(i);    //To change body of overridden methods use File | Settings | File Templates.
    } else {
      return receiverFactory.onLastItem();
    }
  }*/
}
