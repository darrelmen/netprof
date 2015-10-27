package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/27/15.
 */
public class StudentAnalysis extends DivWidget {
  private final Logger logger = Logger.getLogger("StudentAnalysis");

  public StudentAnalysis(final LangTestDatabaseAsync service, final ExerciseController controller,
                         final ShowTab showTab) {
    service.getUserToResultCount(new AsyncCallback<Map<User, Integer>>() {
      @Override
      public void onFailure(Throwable throwable) {

      }

      @Override
      public void onSuccess(Map<User, Integer> userIntegerMap) {
        DivWidget rightSide = new DivWidget();
        UserContainer userContainer = new UserContainer(service, controller, rightSide, showTab);
        List<User> users = new ArrayList<User>();
        //HorizontalPanel hp = new HorizontalPanel();
        //add(hp);

        for (Map.Entry<User, Integer> pair : userIntegerMap.entrySet()) {
          if (pair.getValue() > 0) {
            User key = pair.getKey();
          //  if (!key.getUserID().equals("gvidaver")) {
            if (!key.isTeacher()) {
              users.add(key);
            }
          //  }
          }
        }
        // hp.add(userContainer.getTableWithPager(users));
        add(userContainer.getTableWithPager(users));

        // hp.add(rightSide);
        add(rightSide);
      }
    });

//    service.getWordScores(userid, new AsyncCallback<List<WordScore>>() {
//      @Override
//      public void onFailure(Throwable throwable) {
//        logger.warning("Got " + throwable);
//      }
//
//      @Override
//      public void onSuccess(List<WordScore> wordScores) {
//        final WordContainer wordContainer = new WordContainer(controller, analysisPlot, showTab);
//        lowerHalf.add(wordContainer.getTableWithPager(wordScores));
//        getPhoneReport(service, controller, userid, lowerHalf, analysisPlot, showTab);
//      }
//    });
  }
}
