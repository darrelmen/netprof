/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

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
  private final MessageHelper messageHelper;

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
