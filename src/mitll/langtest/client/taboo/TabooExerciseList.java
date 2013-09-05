package mitll.langtest.client.taboo;

import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.SelectionState;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.taboo.GameInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  private GiverExerciseFactory giverExerciseFactory;

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
   * @see mitll.langtest.client.LangTest#setTabooFactory
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
    } else {
      giverExerciseFactory = (GiverExerciseFactory) factory;
      isGiver = true;
      System.out.println("TabooExerciseList.setFactory : " + factory.getClass() + " for user " + user.getUser() + " GIVER ");
    }

    hideExerciseList();
    if (buttonRow != null) {
      for (int i = 0; i < buttonRow.getWidgetCount(); i++) {
        if (buttonRow.getWidget(i) != statusHeader) buttonRow.getWidget(i).setVisible(true);
      }
    }
  }

  /**
   * Timed to the spanish II class schedule.
   */
  private void setDefaultChapter() {
    long startOfChapter11 = 1380081600000l;
    long startOfChapter12 = 1381204800000l;
    long startOfChapter13 = 1383019200000l;
    long startOfChapter14 = 1384318800000l;

    long[] longs = {startOfChapter11, startOfChapter12, startOfChapter13, startOfChapter14};
    int chapter = 10;
    boolean found = false;
    for (long start : longs) {
      if (System.currentTimeMillis() < start) {
        Map<String, Collection<String>> selectionState = getChapter(chapter);
        setSelectionState(selectionState);
        found = true;
        break;
      }
      chapter++;
    }
    if (!found) setSelectionState(getChapter(10));
  }

  private Map<String, Collection<String>> getChapter(int chapter) {
    Map<String, Collection<String>> selectionState = new HashMap<String, Collection<String>>();
    List<String> value = new ArrayList<String>();
    value.add(""+chapter);
    selectionState.put("chapter", value);
    return selectionState;
  }

  protected void tellUserPanelIsBusy() {
    new ModalInfoDialog("Please wait", "Please wait for you partner to respond.");
  }

  private long lastTimestamp;
  /**
   * @see mitll.langtest.client.LangTest#setGameOnGiver
   * @see mitll.langtest.client.taboo.Taboo#pollForPartnerOnline(long, boolean)
   * @param game
   */
  public void setGameOnGiver(GameInfo game) {
    if (!isGiver) System.err.println("set game on giver on receiver???");
    if (/*isGiver &&*/ game != null) {
      int numExercises = game.getNumExercises();
      if (numExercises > -1 && game.getTimestamp() != lastTimestamp && giverExerciseFactory != null) {
        lastTimestamp = game.getTimestamp();

        giverExerciseFactory.setGameOnGiver(game);
        if (game.hasStarted()) {
          String firstItem = game.getGameItems().get(0).getID();
          System.out.println("----> GIVER:  loading " + firstItem);

          loadByID(firstItem);
        }
      }
    }
  }

  private FluidContainer buttonRow;
  @Override
  protected Widget addBottomText(FluidContainer container) {
    Widget widget = super.addBottomText(container);
    buttonRow = container;

    for (int i = 0; i < container.getWidgetCount(); i++) {
      if (container.getWidget(i) != widget) container.getWidget(i).setVisible(!isGiver);
    }

    setDefaultChapter();
    return widget;
  }

  /**
   * @see mitll.langtest.client.exercise.ExerciseList.SetExercisesCallback#onSuccess(mitll.langtest.shared.ExerciseListWrapper)
   * @see mitll.langtest.client.exercise.SectionExerciseList.MySetExercisesCallback#onSuccess(mitll.langtest.shared.ExerciseListWrapper)
   * @param result
   */
  protected void rememberExercises(List<ExerciseShell> result) {
    SelectionState selectionState = getSelectionState(History.getToken());
    System.out.println("TabooExerciseList.rememberExercises : user " + userID + " " + (isGiver ? " giver " : " receiver ") +
      " remembering " + result.size() + " exercises : " + result +
      "state is '" + selectionState + "'");

    if (isGiver) {
      super.rememberExercises(result);
      giverExerciseFactory.setSelectionState(selectionState.getTypeToSection());
    } else {  // I am a receiver!
      halfRemember(result);

      tellPartnerMyChapterSelection(selectionState);
    }
  }

  /**
   * HUH? why are we doing this?  what's going on? who else is loading the item?
   * @param exercises
   */
  @Override
  public void rememberAndLoadFirst(List<ExerciseShell> exercises) {
    if (isGiver) {
      rememberExercises(exercises);
    }
    else {
      super.rememberAndLoadFirst(exercises);
    }
  }

  // TODO : huh? do we need to do this if we're hiding things...?
  private void halfRemember(List<ExerciseShell> result) {
    currentExercises = result; // remember current exercises
    idToExercise = new HashMap<String, ExerciseShell>();
    clear();
    for (final ExerciseShell es : result) {
      idToExercise.put(es.getID(), es);
      // addExerciseToList(es);
    }
    flush();
  }

  private void tellPartnerMyChapterSelection(final SelectionState selectionState) {
    System.out.println("telling partner selection state for " + userID + " is '" + selectionState +"'");
    final Map<String,Collection<String>> typeToSection = selectionState.getTypeToSection();

    if (receiverFactory != null) {
      System.out.println("TabooExerciseList.tellPartnerMyChapterSelection : remembering " + currentExercises.size());
      receiverFactory.setExerciseShells(new ArrayList<ExerciseShell>(currentExercises), typeToSection);
    }
    else {
      System.err.println("\n\n\nTabooExerciseList.rememberExercises : no factory!!! \n\n\n ");
    }

    service.registerSelectionState(userID, typeToSection, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Can't contact server.");
      }

      @Override
      public void onSuccess(Void result) {
      }
    });
  }

  /**
   * TODOx : get game statistics and show leaderboard plot
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
      System.err.println("We shouldn't get here...\n\n\n\n");
     // new ModalInfoDialog("Chapter(s) complete.", "Would you like to practice this chapter again?");
     /* new DialogHelper(true).showErrorMessage("Chapter(s) complete.", "Would you like to practice this chapter(s) again?", "Yes", new DialogHelper.CloseListener() {
        @Override
        public void gotYes() {
          startOver();
        }

        @Override
        public void gotNo() {

        }
      });*/
    }
  }

  public void startOver() {
    receiverFactory.setExerciseShells(new ArrayList<ExerciseShell>(currentExercises), null); // TODO : ARG how can I get the selection state????
    askServerForExercise(currentExercises.get(0));
  }
}
