package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.download.DownloadContainer;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.SegmentHighlightAudioControl;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Created by go22670 on 5/19/17.
 */
public class ScoreFeedbackDiv extends ScoreProgressBar {
//  private Logger logger = Logger.getLogger("ScoreFeedbackDiv");

  private static final double NATIVE_THRSHOLD = 0.75D;
  private static final String OVERALL_SCORE = "Overall Score";

  /**
   * @see #getWordTableContainer
   */
  private static final String SCORE_LOW_TRY_AGAIN = "Score low, try again.";

  private static final List<String> praise = Arrays.asList(
      "Fantastic!", "Outstanding!", "Great!", "Well done!", "Good Job!",
      "Two thumbs up!", "Awesome!", "Fabulous!", "Splendid!", "Amazing!",
      "Terrific!", "Superb!", "Nice!", "Bravo!", "Magnificent!",
      "Wonderful!", "Terrific!", "Groovy!", "Adroit!", "First-rate!");


  public static final int FIRST_STEP = 35;
  public static final int SECOND_STEP = 75;

  private final PlayAudioPanel playAudioPanel;
  private final DownloadContainer downloadContainer;

  /**
   * @param playAudioPanel
   * @param downloadContainer
   * @see SimpleRecordAudioPanel#addWidgets
   */
  public ScoreFeedbackDiv(PlayAudioPanel playAudioPanel, DownloadContainer downloadContainer) {
    super();
    styleTheProgressBar(progressBar);
    addTooltip(progressBar, OVERALL_SCORE);
    this.playAudioPanel = playAudioPanel;
    this.downloadContainer = downloadContainer;
  }

  private void addTooltip(Widget w, String tip) {
    new TooltipHelper().addTooltip(w, tip);
  }

  void hideScore() {
    progressBar.setVisible(false);
  }

  /**
   * TODO : do in CSS
   * Add score feedback to the right of the play button.
   *
   * @return
   */
  private void styleTheProgressBar(ProgressBar progressBar) {
    Style style = progressBar.getElement().getStyle();
    style.setMarginTop(5, Style.Unit.PX);
    style.setMarginLeft(5, Style.Unit.PX);
    style.setMarginBottom(0, Style.Unit.PX);
    style.setHeight(25, Style.Unit.PX);
    style.setFontSize(16, Style.Unit.PX);
    progressBar.setVisible(false);
  }

  /**
   * Horizontal - play audio, score feedback, download widget
   * Shows a little praise message too!
   *
   * @param pretestScore
   * @param isRTL
   * @return
   * @see SimpleRecordAudioPanel#scoreAudio
   */
  @NotNull
  public DivWidget getWordTableContainer(PretestScore pretestScore, boolean isRTL) {
    DivWidget wordTableContainer = new DivWidget();
    wordTableContainer.getElement().setId("wordTableContainer");
    wordTableContainer.addStyleName("inlineFlex");
    wordTableContainer.addStyleName("floatLeft");
    wordTableContainer.addStyleName("scoringFeedback");

    wordTableContainer.add(getPlayButtonDiv());

    float hydecScore = pretestScore.getHydecScore();
    //  logger.info("score " + hydecScore);
    if (hydecScore > 0) {
      showScoreFeedback(pretestScore, isRTL, wordTableContainer, hydecScore);
      //   logger.info("getWordTableContainer heard " + pretestScore.getRecoSentence());
    } else {
      Heading w = new Heading(4, SCORE_LOW_TRY_AGAIN);
      w.addStyleName("leftFiveMargin");
      wordTableContainer.add(w);
      enablePlayAudio();
    }

    Panel container = downloadContainer.getDownloadContainer();
    container.addStyleName("topFiveMargin");
    wordTableContainer.add(container);

    return wordTableContainer;
  }

  private void showScoreFeedback(PretestScore pretestScore, boolean isRTL, DivWidget wordTableContainer,
                                 float hydecScore) {
    DivWidget scoreFeedbackDiv = new DivWidget();
    scoreFeedbackDiv.add(progressBar);

    Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> typeToSegmentToWidget = new HashMap<>();


    scoreFeedbackDiv.add(new WordTable()
        .getDivWord(pretestScore.getTypeToSegments(), playAudioPanel, typeToSegmentToWidget, isRTL));

    playAudioPanel.setListener(new SegmentHighlightAudioControl(typeToSegmentToWidget));
    // so it will play on drill tab...
    enablePlayAudio();

    wordTableContainer.add(scoreFeedbackDiv);

//    logger.info("showScoreFeedback hydec score " + hydecScore);
    if (hydecScore > NATIVE_THRSHOLD && pretestScore.isFullMatch()) {
      wordTableContainer.add(getPraise());
    }
  }

  private void enablePlayAudio() {
    playAudioPanel.setEnabled(true);
  }

  private DivWidget getPraise() {
    Heading w = new Heading(4, getPraiseMessage());
    w.addStyleName("leftFiveMargin");
    w.addStyleName("correctCard");
    DivWidget praise = new DivWidget();
    praise.add(w);
    return praise;
  }

  private Random rand = new Random();

  @NotNull
  private String getPraiseMessage() {
    return praise.get(rand.nextInt(praise.size()));
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
