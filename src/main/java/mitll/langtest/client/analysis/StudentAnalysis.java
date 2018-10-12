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

package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.analysis.UserInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import static mitll.langtest.client.analysis.MemoryItemContainer.SELECTED_USER;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/27/15.
 */
public class StudentAnalysis extends TwoColumnAnalysis<UserInfo> {
  private final Logger logger = Logger.getLogger("StudentAnalysis");

  /**
   * @param controller
   * @see NewContentChooser#showProgress
   */
  public StudentAnalysis(final ExerciseController controller) {
    Timer pleaseWaitTimer = getPleaseWaitTimer(controller);
    analysisServiceAsync.getUsersWithRecordings(new AsyncCallback<Collection<UserInfo>>() {
      @Override
      public void onFailure(Throwable throwable) {
        finishPleaseWait(pleaseWaitTimer, controller.getMessageHelper());

        logger.warning("Got " + throwable);
        controller.handleNonFatalError("Error retrieving user performance!", throwable);
      }

      @Override
      public void onSuccess(Collection<UserInfo> users) {
        showItems(users, pleaseWaitTimer, controller);
      }
    });
  }

  @Override protected String getStorageKey() {
    return SELECTED_USER;
  }

  @NotNull
  @Override
  protected String getNoDataYetMessage() {
    return "No Users Yet...";
  }

  @NotNull
  @Override
  protected MemoryItemContainer<UserInfo> getItemContainer(ExerciseController controller, DivWidget bottom, DivWidget rightSide) {
    return new UserContainer(controller, bottom, rightSide, getRememberedSelectedUser(controller));
  }

  @Override
  protected String getHeaderLabel() {
    return null;
  }

  /**
   * @see TwoColumnAnalysis#addTop
   * @param users
   * @param controller
   * @param bottom
   * @param rightSide
   * @param noDataMessage
   * @return
   */
/*  protected DivWidget getTable(Collection<UserInfo> users, ExerciseController controller, DivWidget bottom, DivWidget rightSide, String noDataMessage) {
    UserContainer userContainer = new UserContainer(controller, bottom, rightSide, getRememberedSelectedUser(controller));
    return userContainer.getTable(getUserInfos(users));
  }*/
/*
  private List<UserInfo> getUserInfos(Collection<UserInfo> users) {
    List<UserInfo> filtered = new ArrayList<>();
    for (UserInfo userInfo : users) {
      String userID = userInfo.getUserID();
      if (userID != null && !userID.equals("defectDetector")) {
        filtered.add(userInfo);
      }
    }
    return filtered;
  }*/
}
