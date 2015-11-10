package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
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
  public static final int LEFT_MARGIN = UserContainer.TABLE_WIDTH+ 55;
  public static final int TOP_MARGIN = -55;
  public static final String STUDENTS = "Students";
  public static final String OR_MORE_RECORDINGS = "5 or more recordings";
  public static final int STUDENT_WIDTH = 300;
  private final Logger logger = Logger.getLogger("StudentAnalysis");

  public StudentAnalysis(final LangTestDatabaseAsync service, final ExerciseController controller,
                         final ShowTab showTab) {
    service.getUsersWithRecordings(new AsyncCallback<Collection<UserInfo>>() {
      @Override
      public void onFailure(Throwable throwable) {
        logger.warning("Got " + throwable);
      }

      @Override
      public void onSuccess(Collection<UserInfo> users) {
        DivWidget rightSide = new DivWidget();
        DivWidget top       = new DivWidget();
        DivWidget bottom    = new DivWidget();
        bottom.addStyleName("floatLeft");
        UserContainer userContainer = new UserContainer(service, controller, rightSide, bottom, showTab);
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
        Heading students = new Heading(3, STUDENTS, OR_MORE_RECORDINGS);
        students.setWidth(STUDENT_WIDTH + "px");
        //VerticalPanel leftSide = new VerticalPanel();
        DivWidget leftSide = new DivWidget();
        leftSide.add(students);
        Panel tableWithPager = userContainer.getTableWithPager(filtered);
        leftSide.add(tableWithPager);

        top.add(leftSide);
        top.add(rightSide);
        add(top);
        rightSide.getElement().getStyle().setMarginTop(TOP_MARGIN, Style.Unit.PX);
        rightSide.getElement().getStyle().setMarginLeft(LEFT_MARGIN, Style.Unit.PX);
        add(bottom);
      }
    });
  }
}
