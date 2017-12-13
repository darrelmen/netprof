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
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.MiniScoreListener;
import mitll.langtest.client.scoring.SimpleRecordAudioPanel;
import mitll.langtest.client.scoring.WordTable;
import mitll.langtest.client.sound.PlayAudioWidget;
import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.instrumentation.SlimSegment;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static mitll.langtest.client.download.DownloadContainer.getDownloadAudio;

/**
 * ASR Scoring panel -- shows phonemes.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class ASRHistoryPanel extends FlowPanel implements MiniScoreListener {
  private final Logger logger = Logger.getLogger("ASRHistoryPanel");

  private static final int YEAR_LENGTH = 2;
  private static final int YEAR_SUFFIX = (YEAR_LENGTH + 2);
  private static final int NUM_TO_SHOW = 2;

  /**
   *
   */
  private final List<CorrectAndScore> scores2 = new ArrayList<>();

  private final ExerciseController controller;
  private final int exerciseID;

  private final DateTimeFormat format = DateTimeFormat.getFormat("MMM d, yy");
  private final DateTimeFormat todayTimeFormat = DateTimeFormat.getFormat("h:mm a");
  private final String todaysDate,todayYear;

  /**
   * @see SimpleRecordAudioPanel#getScoreHistory
   */
  public ASRHistoryPanel(ExerciseController controller, int exerciseID) {
    this.controller = controller;
    this.exerciseID = exerciseID;
    getElement().setId("ASRHistoryPanel");
    addStyleName("inlineFlex");

    todaysDate = format.format(new Date());
    todayYear = todaysDate.substring(todaysDate.length() - YEAR_LENGTH);
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
    showChart(controller.getHost());
    addPlayer();

    CorrectAndScore hydecScore = new CorrectAndScore(score.getHydecScore(), path);
    hydecScore.setScores(score.getTypeToSegments());
    hydecScore.setJson(score.getJson());
    addScore(hydecScore);
  }
/*
  @Override
  public void gotScore(CorrectAndScore hydecScore) {
    showChart(controller.getHost());
    addPlayer();
    addScore(hydecScore);
  }*/

  /**
   * @param hydecScore
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#addScores
   */
  @Override
  public void addScore(CorrectAndScore hydecScore) {
    scores2.add(hydecScore);
  }

  /**
   * @see mitll.langtest.client.scoring.ScoringAudioPanel#showChart()
   * @see MiniScoreListener#gotScore(PretestScore, String)
   * @param host
   */
  public void showChart(String host) {
    clear();
    addHistory(this, host);
  }

  //call this after adding the widget to the page
  @Override
  public void onLoad() {
    Scheduler.get().scheduleDeferred(this::addPlayer);
  }

  private native void addPlayer() /*-{
      $wnd.basicMP3Player.init();
  }-*/;

  /**
   * @param vp
   * @param host
   * @return
   * @see MiniScoreListener#showChart
   */
  @NotNull
  private List<CorrectAndScore> addHistory(Panel vp, String host) {
    List<CorrectAndScore> scoreAndPaths = scores2;

    int numScores = scores2.size();
    if (numScores >= NUM_TO_SHOW) {
      scoreAndPaths = scores2.subList(numScores - NUM_TO_SHOW, numScores);
    }

    Collections.reverse(scoreAndPaths);   //???? right thing to do ? TODO?

    TooltipHelper tooltipHelper = new TooltipHelper();
    int j = 0;
    for (CorrectAndScore scoreAndPath : scoreAndPaths) {
      int i = scoreAndPaths.size() - (j++);
      Panel hp = getAudioAndScore(tooltipHelper, scoreAndPath, "Score #" + i, i, host);
      hp.addStyleName("floatLeft");
      hp.addStyleName("rightTenMargin");
      hp.addStyleName("buttonGroupInset6");
      vp.add(hp);
    }
    return scoreAndPaths;
  }

  /**
   * Three parts - left to right - audio play widget, colored feedback table, and download button/link.
   * @param tooltipHelper to make tooltips
   * @param scoreAndPath  the audio path and score for the audio
   * @param title         link title
   * @param host
   * @return
   * @see #addHistory
   */
  private Panel getAudioAndScore(TooltipHelper tooltipHelper,
                                 CorrectAndScore scoreAndPath,
                                 String title,
                                 int linkIndex,
                                 String host) {
    Panel hp = new DivWidget();
    hp.addStyleName("inlineFlex");

    DivWidget audioContainer = new DivWidget();
    audioContainer.add(getAudioWidget(scoreAndPath, title));

    hp.add(audioContainer);
    hp.add(makeColoredTable(tooltipHelper, scoreAndPath));

    DivWidget downloadC = new DivWidget();
    downloadC.add(getDownload(scoreAndPath.getPath(), linkIndex, getDateToDisplay(scoreAndPath.getTimestamp()), host));
    hp.add(downloadC);

    return hp;
  }

  private String getDateToDisplay(long timestamp) {
    return timestamp > 0 ?
        getVariableInfoDateStamp(new Date(timestamp)) :
        "";
  }


  /**
   * @param audioPath
   * @param host
   * @return link for this audio
   * @see #getAudioAndScore
   */
  private IconAnchor getDownload(final String audioPath, int linkIndex, String dateFormat, String host) {
    final IconAnchor download = new IconAnchor();
    download.getElement().setId("Download_user_audio_link_" + linkIndex);
    download.setIcon(IconType.DOWNLOAD);
    download.setIconSize(IconSize.LARGE);
    download.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);

    addTooltip(download, dateFormat);
    setDownloadHref(download, audioPath, host);

    download.addClickHandler(event -> controller.logEvent(download, "DownloadUserAudio_History",
        exerciseID, "downloading audio file " + audioPath));

    return download;
  }

  /**
   * @param download
   * @param audioPath
   * @param host
   * @see #getDownload
   */
  private void setDownloadHref(IconAnchor download, String audioPath, String host) {
    audioPath = audioPath.endsWith(".ogg") ? audioPath.replaceAll(".ogg", ".mp3") : audioPath;
    String href = getDownloadAudio(host) +
        "?" +
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
    String tip = "Download recording" +
        (dateFormat.isEmpty()
        ? "" : " from " + dateFormat);
    new TooltipHelper().createAddTooltip(w, tip, Placement.LEFT);
  }

  /**
   * @param scoreAndPath
   * @param title
   * @return
   * @see #getAudioAndScore(TooltipHelper, CorrectAndScore, String, int, String)
   */
  private Anchor getAudioWidget(CorrectAndScore scoreAndPath, String title) {
    return new PlayAudioWidget().getAudioWidgetWithEventRecording(scoreAndPath.getPath(), title,
        exerciseID, controller);
  }

  private Widget makeColoredTable(TooltipHelper tooltipHelper, CorrectAndScore scoreAndPath) {
    Widget row = new DivWidget();
    //row.addStyleName("inlineFlex");
    Map<NetPronImageType, List<TranscriptSegment>> scores = scoreAndPath.getScores();
    if (scores.isEmpty()) {
      logger.warning("makeColoredTable no segments for " + scoreAndPath);
    }
    row.getElement().setInnerHTML(new WordTable().makeColoredTableFull(scores));
    tooltipHelper.createAddTooltip(row, "Score" + (" " + scoreAndPath.getPercentScore() + "%"), Placement.BOTTOM);
    row.addStyleName("leftFiveMargin");
    return row;
  }

}
