/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.list;

import com.google.gwt.user.client.ui.Panel;
import com.sun.org.apache.regexp.internal.RE;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SectionWidget;
import mitll.langtest.client.services.ExerciseServiceAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/5/16.
 */
public class NPExerciseList<V extends SectionWidget> extends HistoryExerciseList<CommonShell, CommonExercise, V> {
  //private Logger logger = Logger.getLogger("NPExerciseList");
  protected NPExerciseList(Panel currentExerciseVPanel,
                           ExerciseServiceAsync service,
                           UserFeedback feedback,
                           ExerciseController controller,
                           boolean showTypeAhead,
                           String instance,
                           boolean incorrectFirst,
                           boolean showFirstNotCompleted,
                           ActivityType activityType) {
    super(currentExerciseVPanel, service, feedback, controller, showTypeAhead, instance, incorrectFirst, showFirstNotCompleted, activityType);
  }

  /**
   * @return
   * @see mitll.langtest.client.list.PagingExerciseList#addComponents
   */
  protected ClickablePagingContainer<CommonShell> makePagingContainer() {
    final PagingExerciseList<CommonShell, CommonExercise> outer = this;
//    if (logger == null) {
//      logger = Logger.getLogger("NPExerciseList");
//    }
//    logger.info("makePagingContainer : for " + getInstance() + " show first not complete " + showFirstNotCompleted);

    pagingContainer =
        new PagingContainer<CommonShell>(
            controller,
            getVerticalUnaccountedFor(),
            getActivityType() == ActivityType.RECORDER,
            showFirstNotCompleted,
            getInstance()) {
          @Override
          protected void gotClickOnItem(CommonShell e) {
            outer.gotClickOnItem(e);
          }
        };
    return pagingContainer;
  }
}
