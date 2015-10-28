package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
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
                     final ShowTab showTab) {
    final AnalysisPlot analysisPlot = new AnalysisPlot(service, userid);

    add(analysisPlot);
    HorizontalPanel lowerHalf = new HorizontalPanel();
    add(lowerHalf);

    getWordScores(service, controller, userid, showTab, analysisPlot, lowerHalf);
  }

  private void getWordScores(final LangTestDatabaseAsync service, final ExerciseController controller,
                             final int userid, final ShowTab showTab, final AnalysisPlot analysisPlot,
                             final Panel lowerHalf) {
    service.getWordScores(userid, new AsyncCallback<List<WordScore>>() {
      @Override
      public void onFailure(Throwable throwable) {
        logger.warning("Got " + throwable);
      }

      @Override
      public void onSuccess(List<WordScore> wordScores) {
        final WordContainer wordContainer = new WordContainer(controller, analysisPlot, showTab);
        lowerHalf.add(wordContainer.getTableWithPager(wordScores));
        getPhoneReport(service, controller, userid, lowerHalf, analysisPlot, showTab);
      }
    });
  }

  private void getPhoneReport(LangTestDatabaseAsync service,
                              final ExerciseController controller,
                              int userid,
                              final Panel lowerHalf,
                              AnalysisPlot analysisPlot,
                              final ShowTab showTab) {
    final PhoneExampleContainer exampleContainer = new PhoneExampleContainer(controller, analysisPlot, showTab);
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
