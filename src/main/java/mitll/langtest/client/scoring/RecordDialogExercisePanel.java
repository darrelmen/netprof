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

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.banner.Emoticon;
import mitll.langtest.client.banner.SessionManager;
import mitll.langtest.client.dialog.IRehearseView;
import mitll.langtest.client.dialog.ITurnContainer;
import mitll.langtest.client.dialog.RehearseViewHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.SimpleColumnChart;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.instrumentation.SlimSegment;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.AlignmentOutput;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import static com.google.gwt.dom.client.Style.Unit.PX;
import static mitll.langtest.client.LangTest.RED_X_URL;
import static mitll.langtest.client.scoring.RecorderPlayAudioPanel.BLUE_INACTIVE_COLOR;

public class RecordDialogExercisePanel extends TurnPanel implements IRecordDialogTurn {
  private final Logger logger = Logger.getLogger("RecordDialogExercisePanel");

  private static final boolean DEBUG_PARTIAL = false;
  private static final boolean DEBUG = false;

  private static final long MOVE_ON_DUR = 3000L;
  private static final long END_SILENCE = 300L;

  private static final int DIM = 40;
  private static final long END_DUR_SKEW = 900L;

  /**
   * @see #startRecording
   */
  private long start = 0L;
  /**
   *
   */
  private long firstVAD = -1L;

  private NoFeedbackRecordAudioPanel<ClientExercise> recordAudioPanel;
  /**
   *
   */
  private long minDur;
  private long actualMinDur;
  /**
   * @see #addWidgets
   */
  private Emoticon emoticon;
  private final SessionManager sessionManager;

  private final IRehearseView rehearseView;

  private float refSpeechDur = 0F;
  private float studentSpeechDur = 0F;

  private AudioAttribute studentAudioAttribute;
  private AlignmentOutput studentAudioAlignmentOutput;
  private boolean gotStreamStop;
  private TreeMap<TranscriptSegment, IHighlightSegment> transcriptToHighlight = null;
  private final boolean doPushToTalk;

  /**
   * @param isRight
   * @param commonExercise
   * @param controller
   * @param listContainer
   * @param alignments
   * @param listenView
   * @param sessionManager
   * @param prevColumn
   * @see RehearseViewHelper#getRecordingTurnPanel
   * @see RehearseViewHelper#makeRecordingTurnPanel
   */
  public RecordDialogExercisePanel(final ClientExercise commonExercise,
                                   final ExerciseController<ClientExercise> controller,
                                   final ListInterface<?, ?> listContainer,
                                   Map<Integer, AlignmentOutput> alignments,
                                   IRehearseView listenView,
                                   SessionManager sessionManager,
                                   ITurnContainer.COLUMNS columns,
                                   ITurnContainer.COLUMNS prevColumn) {
    super(commonExercise, controller, listContainer, alignments, listenView, columns, false);
    this.rehearseView = listenView;
    setMinExpectedDur(commonExercise);
    this.sessionManager = sessionManager;
    doPushToTalk = listenView.isPressAndHold();
    if (columns == ITurnContainer.COLUMNS.MIDDLE) {
      if (prevColumn == ITurnContainer.COLUMNS.RIGHT) {
        addStyleName("floatRight");
      } else {
        addStyleName("floatLeft");
      }
    }
  }

//
//  @Override
//  public void styleMe(DivWidget wrapper) {
//    super.styleMe(wrapper);
//    wrapper.getElement().getStyle().setMarginBottom(0, PX);
//  }

  @Override
  protected void styleInterpreterTurn() {
  }

  private void setMinExpectedDur(ClientExercise commonExercise) {
    if (commonExercise.hasRefAudio()) {
      minDur = commonExercise.getFirst().getDurationInMillis();
      actualMinDur = minDur;

      minDur = (long) ((float) minDur);
      minDur -= END_DUR_SKEW;
    }
  }

  /**
   * @param id
   * @param duration
   * @param alignmentOutput
   * @return
   * @see #showScoreInfo
   * @see #maybeShowAlignment
   * @see #audioChanged
   */
  @Override
  protected TreeMap<TranscriptSegment, IHighlightSegment> showAlignment(int id, long duration, AlignmentOutput alignmentOutput) {
    TreeMap<TranscriptSegment, IHighlightSegment> transcriptSegmentIHighlightSegmentTreeMap =
        super.showAlignment(id, duration, alignmentOutput);
    this.refSpeechDur = getSpeechDur(id, alignmentOutput);
    return transcriptSegmentIHighlightSegmentTreeMap;
  }

  private float getSpeechDur(int id, AlignmentOutput alignmentOutput) {
    List<TranscriptSegment> transcriptSegments = alignmentOutput == null ? null : alignmentOutput.getTypeToSegments().get(NetPronImageType.WORD_TRANSCRIPT);
    if (transcriptSegments == null || transcriptSegments.isEmpty()) {
      logger.warning("getSpeechDur : huh? no transcript for " + id + " for ex " + exercise.getID() + " " + exercise.getForeignLanguage() + " " + exercise.getEnglish());
      return 0F;
    } else {
      TranscriptSegment first = transcriptSegments.get(0);
      float start = first.getStart();
      TranscriptSegment last = transcriptSegments.get(transcriptSegments.size() - 1);
      float end = last.getEnd();
      float dur = end - start;
      // logger.info("getSpeechDur (" + getExID() + ") : " + first.getEvent() + " - " + last.getEvent() + " = " + dur);
      return dur;
    }
  }

  @Override
  public void clearScoreInfo() {
    transcriptToHighlight = null;
    gotStreamStop = false;
    hideEmoticon();

    flclickables.forEach(iHighlightSegment -> {
      iHighlightSegment.setHighlightColor(IHighlightSegment.DEFAULT_HIGHLIGHT);
      iHighlightSegment.clearHighlight();
    });

    rememberAudio();
  }

  private void hideEmoticon() {
    if (emoticon != null) {
      emoticon.setVisible(false);
    }
  }

  private void showEmoticon() {
    if (emoticon != null) {
      emoticon.setVisible(true);
    }
  }

  /**
   * Don't show emoticons for english turns.
   *
   * @see RehearseViewHelper#showScores
   * @see #switchAudioToStudent
   */
  @Override
  public void showScoreInfo() {
    //   logger.info("showScoreInfo for " + this);
    if (!exercise.hasEnglishAttr()) {
      showEmoticon();
    }

    if (transcriptToHighlight == null) {
      if (studentAudioAttribute != null) {
        AlignmentOutput alignmentOutput = studentAudioAlignmentOutput;//studentAudioAttribute.getAlignmentOutput();
        alignmentOutput.setShowPhoneScores(true);

        transcriptToHighlight = showAlignment(0, studentAudioAttribute.getDurationInMillis(), alignmentOutput);
      } else {
        logger.warning("showScoreInfo no student audio for " + this);
      }
    }

    revealScore();
  }

  public void revealScore() {
    if (transcriptToHighlight != null) {
      transcriptToHighlight.forEach(this::showWordScore);
    } else {
      logger.warning("revealScore : no transcript map for " + this);
    }
  }

  private void showWordScore(SlimSegment withScore, IHighlightSegment v) {
    v.restoreText();
    v.setHighlightColor(SimpleColumnChart.getColor(withScore.getScore()));
    v.showHighlight();
  }

  /**
   * @param result
   * @see RehearseViewHelper#useResult
   */
  @Override
  public void useResult(AudioAnswer result) {
    logger.info("useResult got " + result.getScore() + " for " + result.getPath());

    hideAudioFeedback();

    this.studentSpeechDur = getSpeechDur(result.getExid(), result.getPretestScore());

    studentAudioAttribute = new AudioAttribute();
    studentAudioAttribute.setAudioRef(result.getPath());
    studentAudioAlignmentOutput = result.getPretestScore();
    studentAudioAttribute.setDurationInMillis(result.getDurationInMillis());

    emoticon.setEmoticon(result.getScore(), controller.getLanguageInfo());
  }

  @Override
  public void switchAudioToStudent() {
    //logger.info("switchAudioToStudent " + this);
    rememberAudio(studentAudioAttribute);
    showScoreInfo();
  }

  @Override
  public void switchAudioToReference() {
    //logger.info("switchAudioToReference " + this);
    clearScoreInfo();
  }

  /**
   * @see RehearseViewHelper#useInvalidResult
   * Maybe color in the words red?
   */
  @Override
  public void useInvalidResult() {
    emoticon.setUrl(RED_X_URL);
  }

  private DivWidget placeForRecordButton;
  private DivWidget audioFeedback;

  /**
   * @return
   * @see DialogExercisePanel#addWidgets(boolean, boolean, PhonesChoices, EnglishDisplayChoices)
   */
  @Override
  @NotNull
  protected DivWidget getBubble() {
    DivWidget widgets = new DivWidget();
    widgets.add(placeForRecordButton = new DivWidget());
    placeForRecordButton.getElement().setId("placeForRecordButton");
    placeForRecordButton.addStyleName("bottomFiveMargin");
    placeForRecordButton.addStyleName("topFiveMargin");
    placeForRecordButton.addStyleName("leftFiveMargin");

    return widgets;
  }

  @NotNull
  private DivWidget getAudioFeedback() {
    DivWidget w = new DivWidget();
    w.setId("audioFeedback_" + getExID());
    w.setHeight("10px");
    w.setWidth("95%");

    w.addStyleName("leftFiveMargin");
    w.addStyleName("rightFiveMargin");

    w.getElement().getStyle().setClear(Style.Clear.LEFT);
    w.getElement().getStyle().setBackgroundColor("white");
    w.getElement().getStyle().setBorderColor("black");
    w.getElement().getStyle().setBorderStyle(Style.BorderStyle.SOLID);
    w.getElement().getStyle().setBorderWidth(1, PX);
    audioFeedback = w;
    audioFeedback.addStyleName("inlineFlex");
    audioFeedback.setVisible(false);

    return w;
  }

  /**
   * @param showFL
   * @param showALTFL
   * @param phonesChoices
   * @param englishDisplayChoices
   * @return
   * @see mitll.langtest.client.dialog.ListenViewHelper#getTurnPanel
   */
  @Override
  public DivWidget addWidgets(boolean showFL, boolean showALTFL, PhonesChoices phonesChoices, EnglishDisplayChoices englishDisplayChoices) {
    NoFeedbackRecordAudioPanel<ClientExercise> recordPanel =
        new ContinuousDialogRecordAudioPanel(exercise, controller, sessionManager, rehearseView, this);

    this.recordAudioPanel = recordPanel;

    recordPanel.addWidgets();

    DivWidget flContainer = getHorizDiv();
    if (isRight()) {
      addStyleName("floatRight");
    } else if (isLeft()) {
      flContainer.addStyleName("floatLeft");
    }

    PostAudioRecordButton postAudioRecordButton = null;
    DivWidget buttonContainer = new DivWidget();
    buttonContainer.setId("recordButtonContainer_" + getExID());

    // add  button
    boolean showRecordButton = shouldShowRecordButton();
    if (showRecordButton) {
      {
        postAudioRecordButton = getPostAudioWidget(recordPanel);
        buttonContainer.add(postAudioRecordButton);
      }
      {
        Emoticon w = getEmoticonPlaceholder();
        emoticon = w;
        flContainer.add(w);

        if (isRight()) {
          flContainer.addStyleName("floatRight");
        }
      }
    }
    //else {
    //logger.warning("addWidgets : hide record button for " + getText());
    // }

    add(flContainer);

    super.addWidgets(showFL, showALTFL, phonesChoices, englishDisplayChoices);

    if (showRecordButton) {
      flClickableRow.addStyleName("inlineFlex");

      if (exercise.hasEnglishAttr()) {
        for (int i = 0; i < flClickableRow.getWidgetCount(); i++) {
          flClickableRow.getWidget(i).addStyleName("eightMarginTop");
        }
      } else {
        flClickableRow.getElement().getStyle().setMarginTop(14, PX);
      }

      placeForRecordButton.getParent().addStyleName("inlineFlex");


      if (rehearseView.isPressAndHold()) {
        placeForRecordButton.add(buttonContainer);
        addPressAndHoldStyle(postAudioRecordButton);
      } else {
        placeForRecordButton.add(recordPanel.getScoreFeedback());
      }

      add(getAudioFeedback());
     // audioFeedback.setVisible(true);
    }
    return flContainer;
  }

  /**
   * For now only right side of conversation can be practiced.
   *
   * @return
   */
  protected boolean shouldShowRecordButton() {
    return isMiddle() || (rehearseView.isSimpleDialog() && isRight());
  }

  @NotNull
  private PostAudioRecordButton getPostAudioWidget(NoFeedbackRecordAudioPanel<?> recordPanel) {
    PostAudioRecordButton postAudioRecordButton = recordPanel.getPostAudioRecordButton();
    postAudioRecordButton.setEnabled(false);
    postAudioRecordButton.getElement().getStyle().setBackgroundColor(BLUE_INACTIVE_COLOR);
    return postAudioRecordButton;
  }

  private void addPressAndHoldStyle(UIObject postAudioRecordButton) {
    Style style = postAudioRecordButton.getElement().getStyle();
    style.setProperty("borderRadius", "18px");
    style.setPadding(8, PX);
    style.setWidth(19, PX);
    style.setMarginRight(5, PX);
    style.setHeight(19, PX);
  }

  private PostAudioRecordButton getPostAudioRecordButton(NoFeedbackRecordAudioPanel<ClientExercise> recordPanel) {
    return recordPanel.getPostAudioRecordButton();
  }

  long totalDur = 0;

  /**
   * @param response
   * @see ContinuousDialogRecordAudioPanel#usePartial(StreamResponse)
   */
  public void usePartial(StreamResponse response) {
    if (isRecording()) {
      Validity validity = response.getValidity();
      rehearseView.addPacketValidity(validity);

      if (validity == Validity.OK && firstVAD == -1) {
        firstVAD = response.getStreamTimestamp();

        if (DEBUG_PARTIAL) {
          logger.info("usePartial : (" + rehearseView.getNumValidities() +
              ") got first vad : " + firstVAD +
              " (" + (start - firstVAD) + ")" +
              " for " + report() + " diff " + (System.currentTimeMillis() - start) + " dur " + response.getDuration());
        }
        DivWidget w = getPacketDiv(response);
        w.getElement().getStyle().setBackgroundColor("green");

        totalDur += response.getDuration();
        audioFeedback.add(w);
      } else {
        if (DEBUG_PARTIAL) {
          logger.info("usePartial : (" + rehearseView.getNumValidities() +
              " packets) skip validity " + validity +
              " vad " + firstVAD +
              " for " + report() + " diff " + (System.currentTimeMillis() - start) + " dur " + response.getDuration());
        }

        DivWidget w = getPacketDiv(response);
        if (firstVAD != -1) {
          totalDur += response.getDuration();
        }
        String value = validity == Validity.OK ? "green" : "black";
        if (validity != Validity.OK && firstVAD != -1) {
          if (totalDur < actualMinDur) value = "grey";
        }
        w.getElement().getStyle().setBackgroundColor(value);
        audioFeedback.add(w);
      }

      if (response.isStreamStop()) {
        logger.info("usePartial stopStream");
        gotStreamStop = true;
      }
    } else {
      logger.info("usePartial hmm " + report() + " getting response " + response + " but not recording...?");
    }
  }

  @NotNull
  private DivWidget getPacketDiv(StreamResponse response) {
    DivWidget w = new DivWidget();
    w.setHeight("10px");
    long l = (100 * response.getDuration()) / actualMinDur;
  //  logger.info("1 percent " + l);
    w.setWidth(l + "%");
    return w;
  }

  private String report() {
    return this.toString();
  }

  public Widget myGetPopupTargetWidget() {
    return this;
  }

  /**
   * TODO :subclass!
   */
  @Override
  public void markCurrent() {
    super.markCurrent();

    if (doPushToTalk) {
      if (shouldShowRecordButton()) {
        enableRecordButton();
      }
    }
  }

  @Override
  public void enableRecordButton() {
    PostAudioRecordButton recordButton = getRecordButton();
    recordButton.setEnabled(true);
    if (DEBUG) {
      logger.info("enable record button " + getExID());
    }
    recordButton.getElement().getStyle().setBackgroundColor("#bd362f");
  }

  @Override
  public void removeMarkCurrent() {
    super.removeMarkCurrent();
   // logger.info("removeMarkCurrent");

    if (doPushToTalk) {
      //  logger.info("removeMarkCurrent");
      PostAudioRecordButton recordButton = getRecordButton();
      if (recordButton.isRecording()) {
        cancelRecording();
      }

      disableRecordButton();
    }
    hideAudioFeedback();
  }

  @Override
  public void disableRecordButton() {
    PostAudioRecordButton recordButton = getRecordButton();
    recordButton.setEnabled(false);
    recordButton.getElement().getStyle().setBackgroundColor(BLUE_INACTIVE_COLOR);
  }

  /**
   * @see RehearseViewHelper#gotClickOnPlay()
   */
  @Override
  public void cancelRecording() {
    recordAudioPanel.cancelRecording();
  }

  /**
   * @return
   * @see #addWidgets(boolean, boolean, PhonesChoices, EnglishDisplayChoices)
   */
  @NotNull
  private Emoticon getEmoticonPlaceholder() {
    Emoticon w = new Emoticon();
    w.getElement().setId("emoticon");
    w.setVisible(false);
    w.setHeight(DIM + "px");
    w.getElement().getStyle().setMarginTop(-5, PX);
    return w;
  }

  /**
   * @return
   * @see #addWidgets(boolean, boolean, PhonesChoices, EnglishDisplayChoices)
   */
  @NotNull
  private DivWidget getHorizDiv() {
    DivWidget flContainer = new DivWidget();
    flContainer.addStyleName("inlineFlex");
    Style style = flContainer.getElement().getStyle();
    style.setMarginTop(15, PX);

    if (isMiddle() && exercise.hasEnglishAttr()) {
      style.setProperty("marginLeft", "auto");
    }

    flContainer.getElement().setId("RecordDialogExercisePanel_horiz");
    return flContainer;
  }

  /**
   * @see RehearseViewHelper#startRecordingTurn
   */
  public void startRecording() {
    start = System.currentTimeMillis();
    boolean pushToTalk = isPushToTalk();

    if (DEBUG) logger.info("startRecording " +
        "\n\tfor  " + getExID() + " " +
        "\n\tat   " + start + " or " + new Date(start) +
        "\n\tpush " + pushToTalk
    );

    firstVAD = -1;
    totalDur = 0;
    audioFeedback.clear();
    audioFeedback.setVisible(true);
    if (pushToTalk) {
      enableRecordButton();
    } else {
      reallyStartOrStopRecording();
    }
  }

  public boolean reallyStartOrStopRecording() {
    if (DEBUG) logger.info("reallyStartOrStopRecording " + "\n\tfor  " + getExID());
    boolean b = getRecordButton().startOrStopRecording();
//    if (!b) {
//      hideAudioFeedback();
//    }
    return b;
  }

  private void hideAudioFeedback() {
   // logger.info("hideAudioFeedback");
    if (audioFeedback != null) {
      audioFeedback.clear();
      audioFeedback.setVisible(false);
    }
  }

  /**
   * @see RehearseViewHelper#stopRecordingSafe()
   */
  public void stopRecordingSafe() {
    hideAudioFeedback();
    getRecordButton().stopRecordingSafe();
  }

  public boolean isPushToTalk() {
    return doPushToTalk;
  }

  private PostAudioRecordButton getRecordButton() {
    return getPostAudioRecordButton(recordAudioPanel);
  }

  /**
   * TODO : simplify this a lot...
   * Check against expected duration to see when to end.
   *
   * @see #startRecording
   * @see RehearseViewHelper#gotEndSilenceMaybeStopRecordingTurn()
   */
  public boolean gotEndSilenceMaybeStopRecordingTurn() {
    if (doPushToTalk) {
 //     logger.info("gotEndSilenceMaybeStopRecordingTurn ");
      return false;
    } else {

      long now = System.currentTimeMillis();

      long diff = now - start;

      long minDurPlusMoveOn = minDur + MOVE_ON_DUR;
      long diffVAD = now - firstVAD;
      boolean clientVAD = firstVAD > 0 && diffVAD > minDur + END_SILENCE;
      boolean vadCheck = clientVAD && gotStreamStop;

      boolean shouldStop = vadCheck || diff > minDurPlusMoveOn;// || doPushToTalk;
      if (shouldStop) {
        // recordAudioPanel.showWaitCursor();

        logger.info("gotEndSilenceMaybeStopRecordingTurn " + this +
            "\n\tvadCheck  " + vadCheck +
            "\n\tgotStreamStop " + gotStreamStop +
            "\n\tfirstVAD  " + firstVAD +
            "\n\tVAD delay " + (diffVAD - diff) +
            "\n\tdiffVAD   " + diffVAD +
            "\n\tminDur    " + minDur +
            "\n\tminDur+move on " + minDurPlusMoveOn +
            "\n\tdiff      " + diff
        );
        reallyStartOrStopRecording();
        return true;
      } else {
/*
      logger.info("stopRecording for " + getExID() +
          " : ignore too short " + diffVAD + " vad vs " + minDur + " expected client " + clientVAD + " vs server " + gotStreamStop);
*/
        return false;
      }
    }
  }

  /**
   * @return
   * @see RehearseViewHelper#mySilenceDetected()
   */
  public boolean isRecording() {
    return getRecordButton().isRecording();
  }

  public float getRefSpeechDur() {
    return refSpeechDur;
  }

  public float getStudentSpeechDur() {
    return studentSpeechDur;
  }

}