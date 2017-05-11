package mitll.langtest.client.scoring;

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.ExerciseAnnotation;

import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 3/23/17.
 */
public class AnnotationHelper implements CommentAnnotator {
  private final ExerciseController controller;
  private final int exid;

  /**
   * @see TwoColumnExercisePanel#TwoColumnExercisePanel(CommonExercise, ExerciseController, ListInterface, List, ShowChoices, PhonesChoices, Map)
   * @param controller
   * @param exid
   */
  AnnotationHelper(ExerciseController controller, int exid) {
    this.controller = controller;
    this.exid = exid;
  }

  /**
   * @param commentToPost
   * @param field
   * @see mitll.langtest.client.qc.QCNPFExercise#makeCommentEntry(String, ExerciseAnnotation)
   */
  public void addIncorrectComment(final String commentToPost, final String field) {
    addAnnotation(field, ExerciseAnnotation.TYPICAL.INCORRECT, commentToPost);
  }

  public void addCorrectComment(final String field) {
    addAnnotation(field, ExerciseAnnotation.TYPICAL.CORRECT, "");
  }

  private void addAnnotation(final String field, final ExerciseAnnotation.TYPICAL status, final String commentToPost) {
    controller.getQCService().addAnnotation(exid, field, status.toString(), commentToPost,
        new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Void result) {
//        System.out.println("\t" + new Date() + " : onSuccess : posted to server " + getExercise().getOldID() +
//            " field '" + field + "' commentLabel '" + commentToPost + "' is " + status);//, took " + (now - then) + " millis");
          }
        });
  }
}
