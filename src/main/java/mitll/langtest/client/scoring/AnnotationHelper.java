package mitll.langtest.client.scoring;

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.common.MessageHelper;
import mitll.langtest.client.exercise.Services;
import mitll.langtest.shared.exercise.ExerciseAnnotation;

/**
 * Created by go22670 on 3/23/17.
 */
public class AnnotationHelper implements CommentAnnotator {
  private final Services controller;
  MessageHelper messageHelper;

  /**
   * @param controller
   * @paramx exid
   * @see TwoColumnExercisePanel#TwoColumnExercisePanel
   */
  public AnnotationHelper(Services controller, MessageHelper messageHelper) {
    this.controller = controller;
    this.messageHelper = messageHelper;
  }

  /**
   * @param exid
   * @param field
   * @see mitll.langtest.client.qc.QCNPFExercise#makeCommentEntry(String, ExerciseAnnotation)
   * @param commentToPost
   */
  public void addIncorrectComment(int exid, final String field, final String commentToPost) {
    addAnnotation(exid, field, ExerciseAnnotation.TYPICAL.INCORRECT, commentToPost);
  }

  public void addCorrectComment(int exid, final String field) {
    addAnnotation(exid, field, ExerciseAnnotation.TYPICAL.CORRECT, "");
  }

  private void addAnnotation(int exid, final String field, final ExerciseAnnotation.TYPICAL status, final String commentToPost) {
    controller.getQCService().addAnnotation(exid, field, status.toString(), commentToPost,
        new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            messageHelper.handleNonFatalError("adding annotation", caught);
          }

          @Override
          public void onSuccess(Void result) {
//logger.info("\t" +   " : onSuccess : posted to server " + getExercise().getOldID() +
//            " field '" + field + "' commentLabel '" + commentToPost + "' is " + status);//, took " + (now - then) + " millis");
          }
        });
  }
}
