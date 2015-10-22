package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.AnalysisPlot;
import mitll.langtest.client.custom.Navigation;
import mitll.langtest.client.custom.tabs.TabAndContent;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.analysis.WordScore;

import java.util.List;

/**
 * Created by go22670 on 10/21/15.
 */
public class AnalysisTab extends DivWidget {
  /**
   * @see Navigation#showAnalysis()
   * @param service
   * @param controller
   * @param userid
   */
  public AnalysisTab(LangTestDatabaseAsync service, ExerciseController controller, int userid, ShowTab showTab) {
    AnalysisPlot analysisPlot = new AnalysisPlot(service, userid);
    final WordContainer wordContainer = new WordContainer(controller,analysisPlot,showTab);

    add(analysisPlot);

    service.getWordScores(userid, new AsyncCallback<List<WordScore>>() {
      @Override
      public void onFailure(Throwable throwable) {

      }

      @Override
      public void onSuccess(List<WordScore> wordScores) {
        HorizontalPanel lowerHalf = new HorizontalPanel();
        add(lowerHalf);
        lowerHalf.add(wordContainer.getTableWithPager(wordScores));
      }
    });
  }
}
