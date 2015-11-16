package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
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
  private final Panel bottom;
  private boolean isNarrow = false;

  /**
   * @param service
   * @param controller
   * @param userid
   * @param minRecordings
   * @see Navigation#showAnalysis()
   * @see UserContainer#gotClickOnItem
   */
  public AnalysisTab(final LangTestDatabaseAsync service, final ExerciseController controller, final int userid,
                     final ShowTab showTab, String userChosenID, int minRecordings, DivWidget overallBottom) {
    getElement().setId("AnalysisTab");
    final AnalysisPlot analysisPlot = new AnalysisPlot(service, userid, userChosenID, minRecordings,controller.getSoundManager());
    add(analysisPlot);
   // bottom = new DivWidget();
    bottom = new HorizontalPanel();
    bottom.getElement().setId("bottom");
    bottom.addStyleName("floatLeft");

//    bottom = new HorizontalPanel();

    if (overallBottom != null) {
      overallBottom.add(bottom);
      isNarrow = true;
    } else {
      add(bottom);
    }

    getWordScores(service, controller, userid, showTab, analysisPlot, bottom, minRecordings);
  }

  private void getWordScores(final LangTestDatabaseAsync service, final ExerciseController controller,
                             final int userid, final ShowTab showTab, final AnalysisPlot analysisPlot,
                             final Panel lowerHalf, final int minRecordings) {
    service.getWordScores(userid, minRecordings, new AsyncCallback<List<WordScore>>() {
      @Override
      public void onFailure(Throwable throwable) {
        logger.warning("Got " + throwable);
      }

      @Override
      public void onSuccess(List<WordScore> wordScores) {
        final WordContainer wordContainer = new WordContainer(controller, analysisPlot, showTab);
        Panel tableWithPager = wordContainer.getTableWithPager(wordScores);

        DivWidget vert = new DivWidget();
        vert.getElement().setId("WordsContainer");
        vert.addStyleName("floatLeft");
        vert.add(new Heading(3, "Words"));
        vert.add(tableWithPager);
        lowerHalf.add(vert);
        getPhoneReport(service, controller, userid, lowerHalf, analysisPlot, showTab, minRecordings);
      }
    });
  }

  private void getPhoneReport(LangTestDatabaseAsync service,
                              final ExerciseController controller,
                              int userid,
                              final Panel lowerHalf,
                              AnalysisPlot analysisPlot,
                              final ShowTab showTab,
                              final int minRecordings) {
    final PhoneExampleContainer exampleContainer = new PhoneExampleContainer(controller, analysisPlot, showTab);
    final PhonePlot phonePlot = new PhonePlot();
    final PhoneContainer phoneContainer = new PhoneContainer(controller, exampleContainer, phonePlot, isNarrow);

    service.getPhoneScores(userid, minRecordings, new AsyncCallback<PhoneReport>() {
      @Override
      public void onFailure(Throwable throwable) {
        logger.warning("Got " + throwable);
      }

      @Override
      public void onSuccess(PhoneReport phoneReport) {
        // logger.info("getPhoneScores " + phoneReport);
        Panel phones = phoneContainer.getTableWithPager(phoneReport);

        DivWidget sounds = new DivWidget();
        sounds.getElement().setId("SoundsContainer");
        sounds.add(new Heading(3, "Sounds"));
        sounds.addStyleName("floatLeft");

        sounds.add(phones);
        //  sounds.addStyleName("leftTenMargin");
        lowerHalf.add(sounds);

        Panel examples = exampleContainer.getTableWithPager();

        DivWidget wordExamples = new DivWidget();
        wordExamples.getElement().setId("WordExamples");
        wordExamples.addStyleName("floatLeft");

        wordExamples.add(new Heading(3, "Words using Sound"));
        wordExamples.add(examples);
        wordExamples.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
        lowerHalf.add(wordExamples);

        phonePlot.addStyleName("topMargin");
        phonePlot.addStyleName("floatLeft");
        lowerHalf.add(phonePlot);

        phoneContainer.showExamplesForSelectedSound();
      }
    });
  }
}
