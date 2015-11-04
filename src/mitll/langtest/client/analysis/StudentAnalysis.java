package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
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
        UserContainer userContainer = new UserContainer(service, controller, rightSide, showTab);
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
        Heading students = new Heading(3, "Students", "5 or more recordings");
        students.setWidth("300px");
        //VerticalPanel leftSide = new VerticalPanel();
        DivWidget leftSide = new DivWidget();
        leftSide.add(students);
        Panel tableWithPager = userContainer.getTableWithPager(filtered);
        leftSide.add(tableWithPager);

        add(leftSide);
        rightSide.getElement().getStyle().setMarginTop(-40, Style.Unit.PX);
        rightSide.getElement().getStyle().setMarginLeft(540, Style.Unit.PX);
        add(rightSide);
      }
    });
  }
}
