package mitll.langtest.client.taboo;

import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
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

  public TabooExerciseList(FluidRow secondRow, Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                           UserFeedback feedback, boolean showTurkToken, boolean showInOrder, boolean showListBox,
                           ExerciseController controller) {
    super(secondRow, currentExerciseVPanel, service, feedback, showTurkToken, showInOrder, showListBox, controller);
   // makeExercisePanel(null);
  }

  @Override
  public void setFactory(ExercisePanelFactory factory, UserManager user, int expectedGrades) {
    super.setFactory(factory, user, expectedGrades);
    if (!isGiver) makeExercisePanel(null);
  }

  /**
   * @see #getWidgetsForTypes(long)
   * @param userID
   * @param container
   */
  @Override
  protected void getTypeOrder(long userID, FluidContainer container) {
    if (isGiver) {
      super.getTypeOrder(userID, container);    //To change body of overridden methods use File | Settings | File Templates.
    }
    else {
      addBottomText(container);
    }
  }

  public void setGiver(boolean isGiver) { this.isGiver = isGiver; }

  protected void rememberExercises(List<ExerciseShell> result) {
    System.out.println("remembering " + result.size() + " exercises");
    currentExercises = result; // remember current exercises
    idToExercise = new HashMap<String, ExerciseShell>();
    clear();
    for (final ExerciseShell es : result) {
      idToExercise.put(es.getID(),es);
      if (isGiver) addExerciseToList(es);
    }
    flush();
  }

/*  @Override
  protected void addColumnsToTable(boolean consumeClicks) {
    super.addColumnsToTable(isGiver);
  }*/
}
