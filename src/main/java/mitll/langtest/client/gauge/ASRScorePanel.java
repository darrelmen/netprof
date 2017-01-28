/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.gauge;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.pretest.PretestGauge;
import mitll.langtest.client.scoring.ScoreListener;
import mitll.langtest.client.sound.PlayAudioWidget;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.scoring.PretestScore;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * ASR Scoring panel -- shows phonemes.
 *
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since
 */
public class ASRScorePanel extends FlowPanel implements ScoreListener {
  private Logger logger = Logger.getLogger("ASRScorePanel");

  private static final int NUM_TO_SHOW = 5;
  public static final String INSTRUCTIONS = "Your speech is scored by a speech recognizer trained on speech from many native speakers. " +
      "The recognizer generates scores for each word and phonetic unit (see the color-coded transcript for details).";
  private static final String WARNING = "Repeat the phrase exactly as it is written. " +
      "<b>Saying something different or adding/omitting words could result in incorrect scores.</b>";

  private static final String SOUND_ACCURACY = "Sound Accuracy";
  private static final String SCORE_HISTORY = "Score History";
  private static final String SCORE = "Score";
  private static final int HEIGHT = 18;
  private static final int ROW_LEFT_MARGIN = 18 + 5;
  private static final String PLAY_REFERENCE = "";

  private final PretestGauge ASRGauge;
  private final Panel phoneList;
  /**
   *
   */
  private final List<CorrectAndScore> scores2 = new ArrayList<CorrectAndScore>();
  private final SimplePanel scoreHistoryPanel;
  private float classAvg = 0f;
  private String refAudio;
  private final ExerciseController controller;
  private final int exerciseID;

  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#GoodwaveExercisePanel
   */
  public ASRScorePanel(String parent, ExerciseController controller, int exerciseID) {
    this.controller = controller;
    this.exerciseID = exerciseID;
    addStyleName("leftTenMargin");
    addStyleName("floatRight");
    getElement().setId("ASRScorePanel");
    CaptionPanel chartCaptionPanel = new CaptionPanel(SCORE_HISTORY);

    scoreHistoryPanel = new SimplePanel();
    chartCaptionPanel.add(scoreHistoryPanel);
    scoreHistoryPanel.getElement().setId("ScoreHistoryPanel");

    if (!controller.showOnlyOneExercise()) {
      add(chartCaptionPanel);
      //  add(new Heading(4,SCORE_HISTORY));
      add(scoreHistoryPanel);
    }

    CaptionPanel captionPanel = new CaptionPanel(SOUND_ACCURACY);

    phoneList = new FlowPanel();
    phoneList.getElement().getStyle().setFloat(Style.Float.LEFT);

    captionPanel.add(phoneList);
    add(captionPanel);

    CaptionPanel gaugeCaptionPanel = new CaptionPanel(SCORE);

    Panel gaugePanel = new FlowPanel();
    gaugePanel.setHeight("100%");
    gaugePanel.setWidth("100%");
    ASRGauge = new PretestGauge("ASR_" + parent);
    gaugePanel.add(ASRGauge);

    gaugeCaptionPanel.add(gaugePanel);
    add(gaugeCaptionPanel);

    Panel instructionsPanel = new FlowPanel();
    add(instructionsPanel);
    HTML child = new HTML(WARNING);
    instructionsPanel.add(child);
    child.addStyleName("wrapword");
    child.addStyleName("maxWidth");
  }

  //call this after adding the widget to the page
  @Override
  public void onLoad() {
    // System.out.println("looking for dom element id " + id + " width " + canvas.getOffsetWidth() );
    ASRGauge.createCanvasElementOld();
    initGauge(ASRGauge);
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        addPlayer();
      }
    });
  }

  private void initGauge(PretestGauge gauge) {
    try {
      gauge.initialize();
    } catch (Exception e) {
      System.err.println("gauge.initialize : of " + gauge + " : Got exception " + e.getMessage());
    }
  }

  private void setASRGaugeValue(float v) {
    ASRGauge.setValue(v);
  }

  /**
   * @param score
   * @param showOnlyOneExercise
   * @param path
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#useResult
   */
  public void gotScore(PretestScore score, boolean showOnlyOneExercise, String path) {
    float hydecScore = score.getHydecScore();
    float zeroToHundred = hydecScore * 100f;
    setASRGaugeValue(Math.min(100.0f, zeroToHundred));
    updatePhoneAccuracy(score.getPhoneScores());
    addScore(new CorrectAndScore(hydecScore, path));
    scoreHistoryPanel.clear();
    showChart(showOnlyOneExercise);
    addPlayer();
  }

  private native void addPlayer() /*-{
      $wnd.basicMP3Player.init();
  }-*/;

  /**
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#addScore(CorrectAndScore)
   * @param hydecScore
   */
  @Override
  public void addScore(CorrectAndScore hydecScore) {
    scores2.add(hydecScore);
  }

  /**
   * @param classAvg
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#setClassAvg(float)
   */
  @Override
  public void setClassAvg(float classAvg) {
    this.classAvg = classAvg;
  }

  @Override
  public void setRefAudio(String refAudio) {
    this.refAudio = refAudio;
  }

  /**
   * @param showOnlyOneExercise
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#showChart()
   * @see mitll.langtest.client.gauge.ASRScorePanel#gotScore(mitll.langtest.shared.scoring.PretestScore, boolean, String)
   */
  @Override
  public void showChart(boolean showOnlyOneExercise) {
    VerticalPanel vp = new VerticalPanel();
    List<CorrectAndScore> scoreAndPaths = addHistory(vp);

    if (classAvg > 0) {
      TooltipHelper tooltipHelper = new TooltipHelper();
      vp.add(getClassAverage(tooltipHelper, !scoreAndPaths.isEmpty()));
    }

    if (refAudio != null && !scoreAndPaths.isEmpty()) { // show audio to compare yours against
      vp.add(getRefAudio());
    }
    scoreHistoryPanel.add(vp);
  }

  @NotNull
  private List<CorrectAndScore> addHistory(VerticalPanel vp) {
    List<CorrectAndScore> scoreAndPaths = scores2;

    if (scores2.size() > NUM_TO_SHOW) {
      scoreAndPaths = scores2.subList(scores2.size() - NUM_TO_SHOW, scores2.size());
    }

    TooltipHelper tooltipHelper = new TooltipHelper();
    for (CorrectAndScore scoreAndPath : scoreAndPaths) {
      int i = scores2.indexOf(scoreAndPath);
      Panel hp = getAudioAndScore(tooltipHelper, scoreAndPath, "Score #" + (i + 1), i);
      vp.add(hp);
    }
    return scoreAndPaths;
  }

  /**
   * @return
   * @see #showChart(boolean)
   */
  private Panel getRefAudio() {
    Widget audioWidget = getAudioWidget(new CorrectAndScore(classAvg, refAudio), PLAY_REFERENCE);
    makeChildGreen(audioWidget);

    Panel hp = new HorizontalPanel();
    hp.add(audioWidget);
    HTML child = new HTML("Default Reference");
    child.addStyleName("leftFiveMargin");
    hp.add(child);
    return hp;
  }

  /**
   *
   * @param tooltipHelper
   * @param addLeftMargin
   * @return
   */
  private Panel getClassAverage(TooltipHelper tooltipHelper, boolean addLeftMargin) {
    String prefix = "Class Avg";
    CorrectAndScore scoreAndPath = new CorrectAndScore(classAvg, "");
    prefix += " " + scoreAndPath.getPercentScore() + "%";

    Widget widget = makeRow2(tooltipHelper, scoreAndPath, prefix);
    if (addLeftMargin) {
      widget.getElement().getStyle().setMarginLeft(ROW_LEFT_MARGIN, Style.Unit.PX);
    }
    Panel hp2 = new HorizontalPanel();
    hp2.add(widget);
    InlineHTML w1 = new InlineHTML("Class Avg");
    w1.getElement().getStyle().setColor("gray");
    w1.getElement().getStyle().setFontWeight(Style.FontWeight.LIGHTER);
    w1.getElement().getStyle().setProperty("fontSize", "smaller");
    w1.getElement().getStyle().setMarginLeft(3, Style.Unit.PX);
    hp2.add(w1);
    return hp2;
  }

  /**
   * @param tooltipHelper to make tooltips
   * @param scoreAndPath  the audio path and score for the audio
   * @param title         link title
   * @return
   * @see #showChart(boolean)
   */
  private Panel getAudioAndScore(TooltipHelper tooltipHelper, CorrectAndScore scoreAndPath, String title,
                                 int i) {
    Widget w = getAudioWidget(scoreAndPath, title);
    Widget row = makeRow(tooltipHelper, scoreAndPath);
    row.addStyleName("leftFiveMargin");

    Panel hp = new HorizontalPanel();
    hp.add(w);
    DivWidget container = new DivWidget();
    container.setWidth("100px");
    container.add(row);
    hp.add(container);
    long timestamp = scoreAndPath.getTimestamp();
    // logger.info("timestamp " + timestamp);
    String format = timestamp > 0 ?  this.format.format(new Date(timestamp)) : "";
    hp.add(getDownload(scoreAndPath.getPath(), i, format));

    return hp;
  }

  private final DateTimeFormat format = DateTimeFormat.getFormat("MMM d");

  private void makeChildGreen(Widget w) {
    Node child = w.getElement().getChild(0);
    Element as = Element.as(child);
    as.getStyle().setBackgroundColor("green");
  }

  /**
   * @param audioPath
   * @return link for this audio
   * @see #getAudioAndScore
   */
  private IconAnchor getDownload(final String audioPath, int i, String dateFormat) {
    final IconAnchor download = new IconAnchor();
    download.getElement().setId("Download_user_audio_link_" + i);
    download.setIcon(IconType.DOWNLOAD);
    download.setIconSize(IconSize.LARGE);
    download.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);

    addTooltip(download, dateFormat);
    setDownloadHref(download, audioPath);

    download.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.logEvent(download, "DownloadUserAudio_History",
            exerciseID, "downloading audio file " + audioPath);
      }
    });

    return download;
  }

  /**
   * @param download
   * @param audioPath
   * @see #getDownload
   */
  private void setDownloadHref(IconAnchor download, String audioPath) {
    audioPath = audioPath.endsWith(".ogg") ? audioPath.replaceAll(".ogg", ".mp3") : audioPath;

    String href = "downloadAudio?" +
        "file=" + audioPath + "&" +
        "exerciseID=" + exerciseID + "&" +
        "userID=" + controller.getUserState().getUser();
    download.setHref(href);
  }

  /**
   * @param w
   * @see #getDownload
   */
  private void addTooltip(Widget w, String dateFormat) {
    String tip = "Download your recording" + (dateFormat.isEmpty()
        ? "" : " from " + dateFormat);
    new TooltipHelper().createAddTooltip(w, tip, Placement.LEFT);
  }

  /**
   * @param scoreAndPath
   * @param title
   * @return
   * @see #getAudioAndScore(TooltipHelper, CorrectAndScore, String, int)
   * @see #getRefAudio()
   */
  private Anchor getAudioWidget(CorrectAndScore scoreAndPath, String title) {
    return new PlayAudioWidget().getAudioWidgetWithEventRecording(scoreAndPath.getPath(), title,
        exerciseID, controller);
  }

  /**
   * This row will be 3-100 pixels wide.
   *
   * @param tooltipHelper
   * @param scoreAndPath
   * @return
   * @see #getAudioAndScore
   */
  private Widget makeRow(TooltipHelper tooltipHelper, CorrectAndScore scoreAndPath) {
    Widget row = getRow(scoreAndPath);
    int iScore = scoreAndPath.getPercentScore();
    row.setWidth(Math.max(3, iScore) + "px");

    tooltipHelper.createAddTooltip(row, "Score" + (" " + iScore + "%"), Placement.BOTTOM);
    return row;
  }

  private Widget makeRow2(TooltipHelper tooltipHelper, CorrectAndScore scoreAndPath, String prefix) {
    Widget row = getRow(scoreAndPath);
    int iScore = scoreAndPath.getPercentScore();
    row.setWidth(iScore + "px");

    tooltipHelper.createAddTooltip(row, prefix, Placement.BOTTOM);
    return row;
  }

  private Widget getRow(CorrectAndScore scoreAndPath) {
    Widget row = new DivWidget();
    row.setHeight(HEIGHT + "px");
    row.getElement().getStyle().setBackgroundColor(SimpleColumnChart.getColor(scoreAndPath.getScore()));
    row.getElement().getStyle().setMarginTop(2, Style.Unit.PX);
    return row;
  }

  /**
   * @param phoneAccuracies
   * @see mitll.langtest.client.scoring.ScoreListener#gotScore
   */
  private void updatePhoneAccuracy(final Map<String, Float> phoneAccuracies) {
    phoneList.clear();

    if (phoneAccuracies != null) {
      List<String> phones = new ArrayList<String>(phoneAccuracies.keySet());
      Collections.sort(phones, new Comparator<String>() {
        public int compare(String key1, String key2) {
          float value1 = phoneAccuracies.get(key1);
          float value2 = phoneAccuracies.get(key2);

          return ((value1 < value2) ? 1 : -1);
        }
      });

      Panel vp = new VerticalPanel();
      HorizontalPanel hp = new HorizontalPanel();
      vp.add(hp);
      hp.setSpacing(5);
      hp.getElement().getStyle().setFloat(Style.Float.LEFT);

      int c = 0;
      for (String phone : phones) {
        hp.add(getPhone(phoneAccuracies, phone));
        if (++c % 5 == 0) {
          hp = new HorizontalPanel();
          hp.setSpacing(5);
          vp.add(hp);
        }
      }

      phoneList.add(vp);
    }
  }

  private Panel getPhone(Map<String, Float> phoneAccuracies, String phone) {
    HorizontalPanel p = new HorizontalPanel();

    p.getElement().getStyle().setFloat(Style.Float.LEFT);

    SimplePanel left = new SimplePanel();
    left.setWidth("5px");
    left.setHeight("5px");
    p.add(left);
    HTML child = new HTML(phone);
    Float score = phoneAccuracies.get(phone);
    p.getElement().getStyle().setBackgroundColor(SimpleColumnChart.getColor(score));
    float x = score * 100.0f;
    child.setTitle(((int) x) + "%");
    p.add(child);
    SimplePanel r = new SimplePanel();
    r.setWidth("5px");
    r.setHeight("5px");

    p.add(r);
    return p;
  }
}
