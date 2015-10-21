package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.AnalysisPlot;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.analysis.WordScore;

import java.util.List;

/**
 * Created by go22670 on 10/21/15.
 */
public class AnalysisTab extends DivWidget {
  public AnalysisTab(LangTestDatabaseAsync service, ExerciseController controller, ListInterface list, int userid) {
    AnalysisPlot analysisPlot = new AnalysisPlot(service, userid);
    final WordContainer wordContainer = new WordContainer(controller,analysisPlot);
    //   analysis.getContent().add(analysisPlot);

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
