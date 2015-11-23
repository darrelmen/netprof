package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;
import mitll.langtest.shared.analysis.UserInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/27/15.
 */
public class StudentAnalysis extends DivWidget {
  private final Logger logger = Logger.getLogger("StudentAnalysis");

  private static final int LEFT_MARGIN = UserContainer.TABLE_WIDTH+ 53;
 // public static final int TOP_MARGIN = -55;
  private static final String STUDENTS = "Students";
  private static final String OR_MORE_RECORDINGS = "5 or more recordings";
  private static final int STUDENT_WIDTH = 300;

 // private Long selectedUser = null;

  public StudentAnalysis(final LangTestDatabaseAsync service, final ExerciseController controller,
                         final ShowTab showTab) {
    String appTitle = controller.getProps().getAppTitle();
    final String selectedUserKey = getSelectedUserKey(controller, appTitle);

    service.getUsersWithRecordings(new AsyncCallback<Collection<UserInfo>>() {
      @Override
      public void onFailure(Throwable throwable) {
        logger.warning("Got " + throwable);
      }

      @Override
      public void onSuccess(Collection<UserInfo> users) {
        DivWidget rightSide = new DivWidget();
        rightSide.getElement().setId("rightSide");
       // rightSide.addStyleName("floatNone");
       // rightSide.addStyleName("floatLeftList");
       // rightSide.getElement().getStyle().setOverflow(Style.Overflow.AUTO);

        DivWidget bottom    = new DivWidget();
        bottom.addStyleName("floatLeftList");

        UserContainer userContainer = new UserContainer(service, controller, rightSide, bottom, showTab, selectedUserKey);

        Panel tableWithPager = userContainer.getTableWithPager(getUserInfos(users));

        DivWidget leftSide = getStudentContainer(tableWithPager);

        DivWidget top       = new DivWidget();
        top.getElement().setId("top");

       //top.addStyleName("inlineBlockStyleOnly");
        top.add(leftSide);
        top.add(rightSide);
        add(top);
       // rightSide.getElement().getStyle().setMarginTop(TOP_MARGIN, Style.Unit.PX);
        rightSide.getElement().getStyle().setMarginLeft(LEFT_MARGIN, Style.Unit.PX);
        add(bottom);
      }
    });
  }

  private String getSelectedUserKey(ExerciseController controller, String appTitle) {
    return getStoragePrefix(controller, appTitle) + "selectedUser";
  }

  private String getStoragePrefix(ExerciseController controller, String appTitle) {
    return appTitle + ":" + controller.getUser() + ":";
  }

  private DivWidget getStudentContainer(Panel tableWithPager) {
    Heading students = new Heading(3, STUDENTS, OR_MORE_RECORDINGS);
    students.setWidth(STUDENT_WIDTH + "px");
    //VerticalPanel leftSide = new VerticalPanel();
    DivWidget leftSide = new DivWidget();
    leftSide.getElement().setId("studentDiv");
    leftSide.addStyleName("floatLeftList");
    leftSide.add(students);
    leftSide.add(tableWithPager);
    return leftSide;
  }

  private List<UserInfo> getUserInfos(Collection<UserInfo> users) {
    List<UserInfo> filtered = new ArrayList<UserInfo>();
    for (UserInfo userInfo : users) {
      User user = userInfo.getUser();
      if (user != null && user.getUserID() != null && !user.getUserID().equals("defectDetector")) {
        filtered.add(userInfo);
      }
      else {
        logger.warning("skip " + user);
      }
    }
    return filtered;
  }
}
