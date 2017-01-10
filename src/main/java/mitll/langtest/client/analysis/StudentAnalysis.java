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
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.services.AnalysisService;
import mitll.langtest.client.services.AnalysisServiceAsync;
import mitll.langtest.client.services.ExerciseServiceAsync;
import mitll.langtest.shared.analysis.UserInfo;
import mitll.langtest.shared.user.MiniUser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/27/15.
 */
public class StudentAnalysis extends DivWidget {
  private final Logger logger = Logger.getLogger("StudentAnalysis");

  private static final int LEFT_MARGIN = MemoryItemContainer.TABLE_WIDTH + 53;
  private static final String STUDENTS = "Students";
  private static final String OR_MORE_RECORDINGS = "5 or more recordings";
//  private static final int STUDENT_WIDTH = 300;
  private final AnalysisServiceAsync analysisServiceAsync = GWT.create(AnalysisService.class);

  public StudentAnalysis(final ExerciseServiceAsync vanillaService, final ExerciseController controller,
                         final ShowTab showTab) {
    String appTitle = controller.getProps().getAppTitle();
    final String selectedUserKey = getSelectedUserKey(controller, appTitle);

    analysisServiceAsync.getUsersWithRecordings(new AsyncCallback<Collection<UserInfo>>() {
      @Override
      public void onFailure(Throwable throwable) {
        logger.warning("Got " + throwable);
      }

      @Override
      public void onSuccess(Collection<UserInfo> users) {
        DivWidget rightSide = new DivWidget();
        rightSide.getElement().setId("rightSide");
        rightSide.getElement().getStyle().setMarginLeft(LEFT_MARGIN, Style.Unit.PX);
        // rightSide.addStyleName("floatNone");
        // rightSide.addStyleName("floatLeftList");
        // rightSide.getElement().getStyle().setOverflow(Style.Overflow.AUTO);

        DivWidget bottom = new DivWidget();
        bottom.addStyleName("floatLeftList");

        UserContainer userContainer = new UserContainer(vanillaService, controller, rightSide, bottom, showTab, selectedUserKey);
        //DivWidget leftSide = getStudentContainer(userContainer.getTableWithPager(getUserInfos(users)));

//        String title = STUDENTS;
//        String subtitle = OR_MORE_RECORDINGS;
        DivWidget leftSide = userContainer.getTable(getUserInfos(users), STUDENTS, OR_MORE_RECORDINGS);

        DivWidget top = getTop(rightSide, leftSide);

        add(top);
        add(bottom);
      }
    });
  }

  private DivWidget getTop(DivWidget rightSide, DivWidget leftSide) {
    DivWidget top = new DivWidget();
    top.getElement().setId("top");
    //top.addStyleName("inlineBlockStyleOnly");
    top.add(leftSide);
    top.add(rightSide);
    return top;
  }

  private String getSelectedUserKey(ExerciseController controller, String appTitle) {
    return getStoragePrefix(controller, appTitle) + "selectedUser";
  }

  private String getStoragePrefix(ExerciseController controller, String appTitle) {
    return appTitle + ":" + controller.getUser() + ":";
  }

/*  private DivWidget getStudentContainer(Panel tableWithPager) {
    String title = STUDENTS;
    String subtitle = OR_MORE_RECORDINGS;
    return getStudentContainer(tableWithPager, title, subtitle);
  }

  private DivWidget getStudentContainer(Panel tableWithPager, String title, String subtitle) {
    Heading students = new Heading(3, title, subtitle);
    students.setWidth(STUDENT_WIDTH + "px");
    students.getElement().getStyle().setMarginBottom(2, Style.Unit.PX);
    DivWidget leftSide = new DivWidget();
    leftSide.getElement().setId("studentDiv");
    leftSide.addStyleName("floatLeftList");
    leftSide.add(students);
    leftSide.add(tableWithPager);
    return leftSide;
  }*/

  private List<UserInfo> getUserInfos(Collection<UserInfo> users) {
    List<UserInfo> filtered = new ArrayList<UserInfo>();
    for (UserInfo userInfo : users) {
      MiniUser user = userInfo.getUser();
      if (user != null && user.getUserID() != null && !user.getUserID().equals("defectDetector")) {
        filtered.add(userInfo);
      } else {
        //String userID = user == null ? "" :user.getUserID();
        //logger.warning("skip " + "'" + userID + "' : " +  user);
      }
    }
    return filtered;
  }
}
