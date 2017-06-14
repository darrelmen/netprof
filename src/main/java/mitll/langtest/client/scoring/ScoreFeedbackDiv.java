package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.SegmentHighlightAudioControl;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Created by go22670 on 5/19/17.
 */
public class ScoreFeedbackDiv {
  private Logger logger = Logger.getLogger("ScoreFeedbackDiv");

  private static final String OVERALL_SCORE = "Overall Score";

  private ProgressBar progressBar;
  /**
   * @see #getWordTableContainer
   */
  private static final String SCORE_LOW_TRY_AGAIN = "Score low, try again.";

  public static final int FIRST_STEP = 35;
  public static final int SECOND_STEP = 75;

  private PlayAudioPanel playAudioPanel;
  private DownloadContainer downloadContainer;

  /**
   * @param playAudioPanel
   * @param downloadContainer
   * @see SimpleRecordAudioPanel#addWidgets
   */
  public ScoreFeedbackDiv(PlayAudioPanel playAudioPanel,
                          DownloadContainer downloadContainer) {
    progressBar = new ProgressBar(ProgressBarBase.Style.DEFAULT);
    styleTheProgressBar(progressBar);
    addTooltip(progressBar, OVERALL_SCORE);
    this.playAudioPanel = playAudioPanel;
    this.downloadContainer = downloadContainer;
  }

  private Tooltip addTooltip(Widget w, String tip) {
    return new TooltipHelper().addTooltip(w, tip);
  }

  void showScore(double score) {
    double percent = score / 100d;
    progressBar.setPercent(100 * percent);
    progressBar.setText("" + Math.round(score));
    progressBar.setColor(
        score > SECOND_STEP ?
            ProgressBarBase.Color.SUCCESS :
            score > FIRST_STEP ?
                ProgressBarBase.Color.WARNING :
                ProgressBarBase.Color.DANGER);

    progressBar.setVisible(true);
  }

  void hideScore() {
    progressBar.setVisible(false);
  }

  /**
   * Add score feedback to the right of the play button.
   *
   * @return
   * @seex mitll.langtest.client.scoring.AudioPanel#addWidgets
   */
  private void styleTheProgressBar(ProgressBar progressBar) {
    Style style = progressBar.getElement().getStyle();
    style.setMarginTop(5, Style.Unit.PX);
    style.setMarginLeft(5, Style.Unit.PX);
    style.setMarginBottom(0, Style.Unit.PX);
    progressBar.setVisible(false);
  }

  @NotNull
  public DivWidget getWordTableContainer(PretestScore pretestScore, boolean isRTL) {
    DivWidget wordTableContainer = new DivWidget();
    wordTableContainer.getElement().setId("wordTableContainer");
    wordTableContainer.addStyleName("inlineFlex");
    wordTableContainer.addStyleName("floatLeft");

    wordTableContainer.add(getPlayButtonDiv());

    if (pretestScore.getHydecScore() > 0) {
      DivWidget scoreFeedbackDiv = new DivWidget();
      scoreFeedbackDiv.add(progressBar);

      Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> typeToSegmentToWidget = new HashMap<>();
      scoreFeedbackDiv.add(new WordTable()
          .getDivWord(pretestScore.getTypeToSegments(), playAudioPanel, typeToSegmentToWidget, isRTL));
      SegmentHighlightAudioControl listener = new SegmentHighlightAudioControl(typeToSegmentToWidget);
      playAudioPanel.setListener(listener);
      // so it will play on drill tab...
      playAudioPanel.setEnabled(true);

      wordTableContainer.add(scoreFeedbackDiv);
      //   logger.info("getWordTableContainer heard " + pretestScore.getRecoSentence());
    } else {
      Heading w = new Heading(4, SCORE_LOW_TRY_AGAIN);
      w.addStyleName("leftFiveMargin");
      wordTableContainer.add(w);
    }

    Panel container = downloadContainer.getDownloadContainer();
    container.addStyleName("topFiveMargin");
    wordTableContainer.add(container);

    return wordTableContainer;
  }

  public void setDownloadHref(String audioPathToUse, int id, int user, String host) {
    downloadContainer.setDownloadHref(audioPathToUse, id, user, host);
  }

  @NotNull
  private DivWidget getPlayButtonDiv() {
    DivWidget divForPlay = new DivWidget();

    Widget playButton = playAudioPanel.getPlayButton();
    playButton.addStyleName("topFiveMargin");

    divForPlay.add(playButton);
    return divForPlay;
  }
}
