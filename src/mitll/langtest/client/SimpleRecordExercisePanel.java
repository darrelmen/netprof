package mitll.langtest.client;

import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.recorder.SimpleRecordPanel;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleRecordExercisePanel extends ExercisePanel {
//  private static final String IMAGES_CHECKMARK = "images/checkmark.png";
//  private static final String IMAGES_REDX_PNG = "images/redx.png";
 // private ExerciseController controller;
//  private LangTestDatabaseAsync service;

  /**
   * @see mitll.langtest.client.LangTest#loadExercise(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   */
  public SimpleRecordExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                                   final ExerciseController controller) {
    super(e,service,userFeedback,controller);
  //  this.controller = controller;
   // this.service = service;




  }

  /**
   * Has a answerPanel mark to indicate when the saved audio has been successfully posted to the server.
   *
   *
   * @see mitll.langtest.client.ExercisePanel#ExercisePanel(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.UserFeedback, mitll.langtest.client.ExerciseController)
   * @param exercise
   * @param service
   * @param controller
   * @param index  @return
   */
  @Override
  protected Widget getAnswerWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {
    //protected Widget getAnswerWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {
      return new SimpleRecordPanel(service);
  }

  @Override
  protected String getQuestionPrompt(Exercise e) {
    return "&nbsp;&nbsp;&nbsp;Speak and record your answer in " +(e.promptInEnglish ? "english" : " the foreign language") +" :";
  }

  /**
   * Remembers answerPanel image widget so we can show it when save is complete.
   */
/*  private class ImageAnchor extends Anchor {
    public ImageAnchor() {}
    public void setResource(Image img) {
      DOM.insertBefore(getElement(), img.getElement(), DOM.getFirstChild(getElement()));
    }
  }*/

  /**
   * TODO : on the server, notice which audio posts have arrived, and take the latest ones...
   *
   * @param service
   * @param userFeedback
   * @param controller
   * @param completedExercise
   */
  @Override
  protected void postAnswers(LangTestDatabaseAsync service, UserFeedback userFeedback, ExerciseController controller, Exercise completedExercise) {
    controller.loadNextExercise(completedExercise);
  }
}
