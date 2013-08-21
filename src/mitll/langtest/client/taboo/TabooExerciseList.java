package mitll.langtest.client.taboo;

import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
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
  private Heading correct = new Heading(4);
  private int correctCount, incorrectCount;

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
   // makeExercisePanel(null);
  }

  /**
   * TODO : replace the item list with a correct/incorrect counter
   * TODO : how do we get the receiver to choose the chapter?
   *
   * @param factory
   * @param user
   * @param expectedGrades
   */
  @Override
  public void setFactory(ExercisePanelFactory factory, UserManager user, int expectedGrades) {
    super.setFactory(factory, user, expectedGrades);
    if (factory instanceof ReceiverExerciseFactory) {
      ((ReceiverExerciseFactory)factory).setExerciseList(this);
    }
 /*   if (!isGiver) {
      ((ReceiverExerciseFactory) factory).isSinglePlayer())
    }*/
    if (!isGiver) makeExercisePanel(null);
  }

  public void incCorrect()   { correctCount++; setCorrect(); }
  public void incIncorrect() { incorrectCount++; setCorrect(); }

  @Override
  public Widget getExerciseListOnLeftSide(PropertyHandler props) {
    Panel correctAndImageRow = new FlowPanel();

    return correctAndImageRow;    //To change body of overridden methods use File | Settings | File Templates.
  }

  public void setCorrect() {
    correct.setText(correctCount + "/" + (correctCount + incorrectCount));

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
}
