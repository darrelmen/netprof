package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.AnalysisPlot;
import mitll.langtest.client.custom.Navigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.WordScore;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/21/15.
 */
public class AnalysisTab extends DivWidget {
  private final Logger logger = Logger.getLogger("AnalysisTab");

  /**
   * @param service
   * @param controller
   * @param userid
   * @see Navigation#showAnalysis()
   */
  public AnalysisTab(final LangTestDatabaseAsync service, final ExerciseController controller, final int userid,
                     ShowTab showTab) {
    final AnalysisPlot analysisPlot = new AnalysisPlot(service, userid);
    final WordContainer wordContainer = new WordContainer(controller, analysisPlot, showTab);

    add(analysisPlot);

    service.getWordScores(userid, new AsyncCallback<List<WordScore>>() {
      @Override
      public void onFailure(Throwable throwable) {
        logger.warning("Got " + throwable);
      }

      @Override
      public void onSuccess(List<WordScore> wordScores) {
        HorizontalPanel lowerHalf = new HorizontalPanel();
        add(lowerHalf);
        lowerHalf.add(wordContainer.getTableWithPager(wordScores));

     //   logger.info("getWordScores " + wordScores.size());

        getPhoneReport(service, controller, userid, lowerHalf, analysisPlot);
      }
    });
  }

  public void getPhoneReport(LangTestDatabaseAsync service,
                             final ExerciseController controller,
                             int userid,
                             final Panel lowerHalf,
                             AnalysisPlot analysisPlot) {
    final PhoneExampleContainer exampleContainer = new PhoneExampleContainer(controller, analysisPlot);
    final PhoneContainer phoneContainer = new PhoneContainer(controller, exampleContainer);

    service.getPhoneScores(userid, new AsyncCallback<PhoneReport>() {
      @Override
      public void onFailure(Throwable throwable) {
        logger.warning("Got " + throwable);
      }

      @Override
      public void onSuccess(PhoneReport phoneReport) {
       // logger.info("getPhoneScores " + phoneReport);
        lowerHalf.add(phoneContainer.getTableWithPager(phoneReport));
        lowerHalf.add(exampleContainer.getTableWithPager());
        phoneContainer.showExamplesForSelectedSound();
      }
    });
  }
}
