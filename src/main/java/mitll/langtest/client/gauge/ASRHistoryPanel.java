/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.gauge;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.download.DownloadContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.MiniScoreListener;
import mitll.langtest.client.scoring.SimpleRecordAudioPanel;
import mitll.langtest.client.scoring.WordTable;
import mitll.langtest.client.sound.PlayAudioWidget;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ASRHistoryPanel extends FlowPanel implements MiniScoreListener {
  private final Logger logger = Logger.getLogger("ASRHistoryPanel");

  private static final String BEST_SCORE = "Best Score";

  private static final int YEAR_LENGTH = 2;
  private static final int YEAR_SUFFIX = (YEAR_LENGTH + 2);

  /**
   *
   */
  private CorrectAndScore currentMax = null;

  private final ExerciseController controller;
  private final int exerciseID;

  private final DateTimeFormat format = DateTimeFormat.getFormat("MMM d, yy");
  private final DateTimeFormat todayTimeFormat = DateTimeFormat.getFormat("h:mm a");
  private final String todaysDate, todayYear;
  private final boolean addPlayer;

  /**
   * @see SimpleRecordAudioPanel#getScoreHistory
   */
  public ASRHistoryPanel(ExerciseController controller, int exerciseID, boolean addPlayer) {
    this.controller = controller;
    this.exerciseID = exerciseID;
    getElement().setId("ASRHistoryPanel_" + exerciseID);
    addStyleName("inlineFlex");

    todaysDate = format.format(new Date());
    todayYear = todaysDate.substring(todaysDate.length() - YEAR_LENGTH);
    this.addPlayer = addPlayer;
  }

  private String getVariableInfoDateStamp(Date date) {
    String signedUp = format.format(date);
    // drop year if this year
    if (signedUp.equals(todaysDate)) {
      signedUp = todayTimeFormat.format(date);
    } else if (todayYear.equals(signedUp.substring(signedUp.length() - YEAR_LENGTH))) {
      signedUp = signedUp.substring(0, signedUp.length() - YEAR_SUFFIX);
    }
    return signedUp;
  }

  /**
   * User just got a score back.
   * <p>
   * don't show the current score
   *
   * @param score
   * @param path
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#useResult
   */
  public void gotScore(PretestScore score, String path) {
    showChart();
    addPlayer();

    CorrectAndScore hydecScore = new CorrectAndScore(score.getOverallScore(), path);
    hydecScore.setScores(score.getTypeToSegments());
    hydecScore.setJson(score.getJson());
    addScore(hydecScore);
  }

  /**
   * @param hydecScore
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#addScores
   */
  @Override
  public void addScore(CorrectAndScore hydecScore) {
    if (currentMax == null || hydecScore.getScore() > currentMax.getScore()) {
      currentMax = hydecScore;
      //  logger.info("current max now " + overallScore);
    }
  }

  /**
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#showChart()
   * @see MiniScoreListener#gotScore(PretestScore, String)
   */
  public void showChart() {
    clear();
    addHistory(this);
  }

  //call this after adding the widget to the page
  @Override
  public void onLoad() {
    if (addPlayer) {
      Scheduler.get().scheduleDeferred(this::addPlayer);
    }
  }

  private native void addPlayer() /*-{
      $wnd.basicMP3Player.init();
  }-*/;

  /**
   * @param vp
   * @return
   * @see MiniScoreListener#showChart
   */
  @NotNull
  private void addHistory(Panel vp) {
    if (currentMax != null) {
      Panel hp = getAudioAndScore(new TooltipHelper(), currentMax, BEST_SCORE, 1);
      hp.addStyleName("floatLeft");
      hp.addStyleName("rightTenMargin");
      hp.addStyleName("buttonGroupInset6");
      vp.add(hp);
    }
  }

  /**
   * Three parts - left to right - audio play widget, colored feedback table, and download button/link.
   *
   * @param tooltipHelper to make tooltips
   * @param scoreAndPath  the audio path and score for the audio
   * @param title         link title
   * @return
   * @see #addHistory
   */
  private Panel getAudioAndScore(TooltipHelper tooltipHelper,
                                 CorrectAndScore scoreAndPath,
                                 String title,
                                 int linkIndex) {
    Panel hp = new DivWidget();
    hp.addStyleName("inlineFlex");

    {
      DivWidget audioContainer = new DivWidget();
      audioContainer.add(getAudioWidget(scoreAndPath, title));

      hp.add(audioContainer);
    }

    hp.add(makeColoredTable(tooltipHelper, scoreAndPath));

    {
      DivWidget downloadC = new DivWidget();
      downloadC.add(new DownloadContainer()
          .getDownload(scoreAndPath.getPath(), linkIndex, getDateToDisplay(scoreAndPath.getTimestamp()),
              controller.getHost(), exerciseID, controller));
      hp.add(downloadC);
    }

    return hp;
  }

  private String getDateToDisplay(long timestamp) {
    return timestamp > 0 ? getVariableInfoDateStamp(new Date(timestamp)) : "";
  }

  /**
   * @param scoreAndPath
   * @param title
   * @return
   * @see #getAudioAndScore(TooltipHelper, CorrectAndScore, String, int)
   */
  private Anchor getAudioWidget(CorrectAndScore scoreAndPath, String title) {
    return new PlayAudioWidget().getAudioWidgetWithEventRecording(scoreAndPath.getPath(), title,
        exerciseID, controller);
  }

  /**
   * Made word score use inline-flex layout...
   *
   * @param tooltipHelper
   * @param scoreAndPath
   * @return
   */
  private Widget makeColoredTable(TooltipHelper tooltipHelper, CorrectAndScore scoreAndPath) {
    Widget row = new DivWidget();
    row.getElement().setId("wordScores");
    row.addStyleName("inlineFlex");
    Map<NetPronImageType, List<TranscriptSegment>> scores = scoreAndPath.getScores();
    if (scores.isEmpty()) {
      logger.info("makeColoredTable no segments for " + scoreAndPath);
    }
    row.getElement().setInnerHTML(new WordTable().makeColoredTableFull(scores));
    tooltipHelper.createAddTooltip(row, "Score" + (" " + scoreAndPath.getPercentScore() + "%"), Placement.BOTTOM);
    row.addStyleName("leftFiveMargin");
    return row;
  }

}
