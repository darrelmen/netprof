package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
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
  public static final String WORDS = "Words";
  public static final String WORDS_USING_SOUND = "Words using Sound";
  public static final String SOUNDS = "Sounds";
  public static final String SUBTITLE = "scores > 20";
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
    final AnalysisPlot analysisPlot = new AnalysisPlot(service, userid, userChosenID, minRecordings,
        controller.getSoundManager());
    analysisPlot.addStyleName("cardBorderShadow");
    analysisPlot.getElement().getStyle().setMargin(10, Style.Unit.PX);

    add(analysisPlot);
 //   bottom = new DivWidget();
    bottom = new HorizontalPanel();
    bottom.getElement().setId("bottom");
    bottom.addStyleName("floatLeft");

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

        DivWidget wordsContainer = getWordContainerDiv(tableWithPager, "WordsContainer", new Heading(3, WORDS, SUBTITLE));
        wordsContainer.addStyleName("cardBorderShadow");
        wordsContainer.getElement().getStyle().setMargin(10, Style.Unit.PX);

        lowerHalf.add(wordsContainer);


        DivWidget soundsDiv = new DivWidget();
     //   Widget soundsDiv = new DivWidget();
        soundsDiv.addStyleName("cardBorderShadow");
        soundsDiv.addStyleName("floatRight");
        soundsDiv.getElement().getStyle().setMargin(10, Style.Unit.PX);
//        soundsDiv.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);

        lowerHalf.add(soundsDiv);
        getPhoneReport(service, controller, userid, soundsDiv, analysisPlot, showTab, minRecordings);
      }
    });
  }

  public DivWidget getWordContainerDiv(Panel tableWithPager, String containerID, Heading w) {
    DivWidget vert = new DivWidget();
    vert.getElement().setId(containerID);
    vert.addStyleName("floatLeft");
    vert.add(w);
    vert.add(tableWithPager);
    return vert;
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
        // #1 - phones
        Panel phones = phoneContainer.getTableWithPager(phoneReport);
        DivWidget sounds = getSoundsContainer(phones);
        lowerHalf.add(sounds);

        // #2 - word examples
        Panel examples = exampleContainer.getTableWithPager();
        DivWidget wordExamples = getWordExamples(examples);
        lowerHalf.add(wordExamples);

        // #3 - phone plot
        phonePlot.addStyleName("topMargin");
        phonePlot.addStyleName("floatLeft");
        lowerHalf.add(phonePlot);

        phoneContainer.showExamplesForSelectedSound();
      }
    });
  }

  public DivWidget getSoundsContainer(Panel phones) {
    DivWidget sounds = new DivWidget();
    sounds.getElement().setId("SoundsContainer");
    sounds.add(new Heading(3, SOUNDS));
    sounds.addStyleName("floatLeft");
    sounds.add(phones);
    return sounds;
  }

  public DivWidget getWordExamples(Panel examples) {
    DivWidget wordExamples = getWordContainerDiv(examples, "WordExamples", new Heading(3, WORDS_USING_SOUND));
    wordExamples.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
    return wordExamples;
  }
}
