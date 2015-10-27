package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.User;

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
    service.getUsersWithRecordings(new AsyncCallback<Collection<User>>() {
      @Override
      public void onFailure(Throwable throwable) {
        logger.warning("Got " + throwable);
      }

      @Override
      public void onSuccess(Collection<User> users) {
//        logger.info("got users num =  " + users.size());
        DivWidget rightSide = new DivWidget();
        UserContainer userContainer = new UserContainer(service, controller, rightSide, showTab);
        List<User> filtered = new ArrayList<User>();
        for (User user : users) {
          if (!user.getUserID().equals("defectDetector")) {
            filtered.add(user);
          }
        }
        add(userContainer.getTableWithPager(filtered));
        add(rightSide);
      }
    });
  }
}
