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
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.common.MessageHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.services.AnalysisService;
import mitll.langtest.client.services.AnalysisServiceAsync;
import mitll.langtest.server.services.AnalysisServiceImpl;
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
public class StudentAnalysis extends DivWidget {
  private final Logger logger = Logger.getLogger("StudentAnalysis");

 // private static final int LEFT_MARGIN = MemoryItemContainer.TABLE_WIDTH + 53;
  private static final String STUDENTS = "Students";
  private static final String OR_MORE_RECORDINGS = AnalysisServiceImpl.MIN_RECORDINGS + " or more recordings";
  private final AnalysisServiceAsync analysisServiceAsync = GWT.create(AnalysisService.class);

  /**
   * @see NewContentChooser#showProgress
   * @param controller
   * @param showTab
   */
  public StudentAnalysis(final ExerciseController controller, final ShowTab showTab) {
    //logger.info("StudentAnalysis got here " + appTitle);
//    getElement().setId("StudentAnalysis");

    final String selectedUserKey = getRememberedSelectedUser(controller);

    analysisServiceAsync.getUsersWithRecordings(new AsyncCallback<Collection<UserInfo>>() {
      @Override
      public void onFailure(Throwable throwable) {
        logger.warning("Got " + throwable);
        controller.handleNonFatalError("Error retrieving user performance!", throwable);
      }

      @Override
      public void onSuccess(Collection<UserInfo> users) {
        DivWidget rightSide = getRightSide();
        DivWidget bottom = new DivWidget();
        bottom.addStyleName("floatLeft");
        bottom.getElement().setId("StudentAnalysis_bottom");

//        logger.info("onSuccess get users " + users.size());
//        logger.info("onSuccess bottom " + bottom.getElement().getId());
        clear();

        UserContainer userContainer = new UserContainer(controller, rightSide, bottom, showTab, selectedUserKey);
        add(getTop(userContainer.getTable(getUserInfos(users), STUDENTS, OR_MORE_RECORDINGS), rightSide));
        add(bottom);
        //   logger.info("onSuccess added top and bottom " + top.getElement().getId());
        //   logger.info("onSuccess added top and bottom " + bottom.getElement().getId());
      }
    });
  }

  /**
   * TODO : use common key storage
   * @param controller
   * @return
   */
  @NotNull
  private String getRememberedSelectedUser(ExerciseController controller) {
    return getSelectedUserKey(controller, controller.getProps().getAppTitle());
  }

  @NotNull
  private DivWidget getRightSide() {
    DivWidget rightSide = new DivWidget();
    rightSide.getElement().setId("rightSide");
    rightSide.setWidth("100%");
    return rightSide;
  }

  /**
   *
   * @param leftSide
   * @param rightSide
   * @return
   */
  private DivWidget getTop(DivWidget leftSide, DivWidget rightSide) {
    DivWidget top = new DivWidget();
    top.addStyleName("inlineFlex");
    top.setWidth("100%");
   // top.getElement().getStyle().setMarginRight(70, Style.Unit.PX );

    top.getElement().setId("top");
    top.add(leftSide);
    top.add(rightSide);
    return top;
  }

  private String getSelectedUserKey(ExerciseController controller, String appTitle) {
    return getStoragePrefix(controller, appTitle) + SELECTED_USER;
  }

  private String getStoragePrefix(ExerciseController controller, String appTitle) {
    return appTitle + ":" + controller.getUser() + ":";
  }

  private List<UserInfo> getUserInfos(Collection<UserInfo> users) {
    List<UserInfo> filtered = new ArrayList<>();
    for (UserInfo userInfo : users) {
      String userID = userInfo.getUserID();
      if (userID != null && !userID.equals("defectDetector")) {
        filtered.add(userInfo);
      } else {
        //String userID = user == null ? "" :user.getUserID();
        //logger.warning("skip " + "'" + userID + "' : " +  user);
      }
    }
    return filtered;
  }
}
