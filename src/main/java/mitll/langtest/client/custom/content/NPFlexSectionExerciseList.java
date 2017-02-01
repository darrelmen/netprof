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

package mitll.langtest.client.custom.content;

import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.list.SimpleSelectExerciseList;
import mitll.langtest.shared.answer.ActivityType;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/26/16.
 */
public class NPFlexSectionExerciseList extends SimpleSelectExerciseList {
  private static final String COMPLETE = "Complete";
  private static final String LIST_COMPLETE = "List complete!";

  /**
   * @param topRow
   * @param currentExercisePanel
   * @param instanceName
   * @param incorrectFirst
   * @param activityType
   * @see mitll.langtest.client.custom.Navigation#makePracticeHelper
   * @see FlexListLayout#makeExerciseList(Panel, Panel, String, boolean)
   */
  public NPFlexSectionExerciseList(FlexListLayout flexListLayout,
                                   Panel topRow,
                                   Panel currentExercisePanel,
                                   String instanceName,
                                   boolean incorrectFirst, ActivityType activityType) {
    super(topRow, currentExercisePanel, flexListLayout.exerciseServiceAsync, flexListLayout.feedback,
        flexListLayout.controller, instanceName, incorrectFirst,/*, showFirstNotCompleted*/
        activityType);
  }

  @Override
  protected void onLastItem() {
    new ModalInfoDialog(COMPLETE, LIST_COMPLETE, new HiddenHandler() {
      @Override
      public void onHidden(HiddenEvent hiddenEvent) {
        reloadExercises();
      }
    });
  }

  @Override
  protected void noSectionsGetExercises(long userID) {
    simpleLoadExercises(getInitialHistoryToken(), getPrefix());
  }
}
