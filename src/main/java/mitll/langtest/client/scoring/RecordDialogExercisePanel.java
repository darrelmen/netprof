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
import mitll.langtest.client.dialog.ListenViewHelper.COLUMNS;
import mitll.langtest.client.dialog.PerformViewHelper;
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

import java.util.*;
import java.util.logging.Logger;

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
  private long firstVAD = -1L;

  private NoFeedbackRecordAudioPanel<ClientExercise> recordAudioPanel;
  private static final float DELAY_SCALAR = 1.0F;
  /**
   *
   */
  private long minDur;
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
   * @param commonExercise
   * @param controller
   * @param listContainer
   * @param alignments
   * @param listenView
   * @param sessionManager
   * @param isRight
   * @see RehearseViewHelper#getRecordingTurnPanel
   * @see RehearseViewHelper#makeRecordingTurnPanel
   */
  public RecordDialogExercisePanel(final ClientExercise commonExercise,
                                   final ExerciseController<ClientExercise> controller,
                                   final ListInterface<?, ?> listContainer,
                                   Map<Integer, AlignmentOutput> alignments,
                                   IRehearseView listenView,
                                   SessionManager sessionManager,
                                   COLUMNS columns) {
    super(commonExercise, controller, listContainer, alignments, listenView, columns, false);
    this.rehearseView = listenView;
    setMinExpectedDur(commonExercise);
    this.sessionManager = sessionManager;
    doPushToTalk = listenView.isPressAndHold();
  }

  private void setMinExpectedDur(ClientExercise commonExercise) {
    if (commonExercise.hasRefAudio()) {
      minDur = commonExercise.getFirst().getDurationInMillis();
      minDur = (long) (((float) minDur) * DELAY_SCALAR);
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

    rememberAudio(getRegularSpeedIfAvailable(exercise));
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
   * TODOx : do this better - should
   *
   * @see RehearseViewHelper#showScores
   * @see #switchAudioToStudent
   */
  @Override
  public void showScoreInfo() {
    //   logger.info("showScoreInfo for " + this);
    showEmoticon();

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
   * Rules:
   * <p>
   * 1) don't obscure everything
   * 2) obscure something
   * 3) Don't obscure more than one or two or three? words?
   * 4) if choosing only two out of all of them, choose the longest ones?
   * 3) if you have a choice, don't obscure first token? ?
   *
   * @param coreVocab
   * @see PerformViewHelper#getTurnPanel
   * Or should we use exact match?
   */
  public void maybeSetObscure(Map<String, ClientExercise> turnToEx) {
    String oldID = exercise.getOldID();
    ClientExercise clientExercise = turnToEx.get(oldID);
    if (clientExercise == null) {
      logger.info("maybeSetObscure : couldn't find core for " + oldID);
    } else {
      Set<String> coreVocab = Collections.singleton(clientExercise.getForeignLanguage());
      //  logger.info("got " + clientExercise.getForeignLanguage() + " for " + exercise.getForeignLanguage());
      getObscureCandidates(coreVocab).forEach(IHighlightSegment::setObscurable);
    }
  }

  private Collection<IHighlightSegment> getObscureCandidates(Collection<String> coreVocab) {
    Collection<IHighlightSegment> candidates = new ArrayList<>();

    String coreMatch = null;
    for (IHighlightSegment segment : flclickables) {
      //   logger.warning("no match for " + segment.getContent() + " in " + coreVocab);
      String content = segment.getContent();

      if (!content.isEmpty()) {
        for (String core : coreVocab) {
          if (core.contains(content) || content.contains(core)) {
            coreMatch = core;
            //candidate = segment;
          }
          if (coreMatch != null) break;
        }
        if (coreMatch != null) break;
      }
    }

    if (coreMatch != null) {
      for (IHighlightSegment segment : flclickables) {
        String content = segment.getContent();

        if (coreMatch.startsWith(content)) {
          candidates.add(segment);
          coreMatch = coreMatch.substring(content.length());
          //   logger.info("getObscureCandidate core match now " + coreMatch + " against " + content);
          if (coreMatch.isEmpty()) break;
        } else if (content.startsWith(coreMatch)) {
          candidates.add(segment);
          logger.info("getObscureCandidate partial match for " + coreMatch + " against " + content);
          break;
        } else {
          //   logger.info("getObscureCandidate no match for " + coreMatch + " against " + content);
        }
      }
    }

    return candidates;
  }

  /**
   * @param result
   * @see RehearseViewHelper#useResult
   */
  @Override
  public void useResult(AudioAnswer result) {
    logger.info("useResult got " + result.getScore() + " for " + result.getPath());

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

  /**
   * @param showFL
   * @param showALTFL
   * @param phonesChoices
   * @param englishDisplayChoices
   * @see mitll.langtest.client.dialog.ListenViewHelper#getTurnPanel
   */
  @Override
  public void addWidgets(boolean showFL, boolean showALTFL, PhonesChoices phonesChoices, EnglishDisplayChoices englishDisplayChoices) {
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
    } else {
      //logger.warning("addWidgets : hide record button for " + getText());
    }

    add(flContainer);

    super.addWidgets(showFL, showALTFL, phonesChoices, englishDisplayChoices);

    if (showRecordButton) {
      flClickableRow.addStyleName("inlineFlex");

      if (exercise.hasEnglishAttr()) {
        for (int i = 0; i < flClickableRow.getWidgetCount(); i++) {
          flClickableRow.getWidget(i).addStyleName("eightMarginTop");
        }
      } else {
        flClickableRow.getElement().getStyle().setMarginTop(14, Style.Unit.PX);
      }

      placeForRecordButton.getParent().addStyleName("inlineFlex");


      if (rehearseView.isPressAndHold()) {
        placeForRecordButton.add(buttonContainer);
        addPressAndHoldStyle(postAudioRecordButton);
      } else {
        placeForRecordButton.add(recordPanel.getScoreFeedback());
      }
    }
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
    style.setPadding(8, Style.Unit.PX);
    style.setWidth(19, Style.Unit.PX);
    style.setMarginRight(5, Style.Unit.PX);
    style.setHeight(19, Style.Unit.PX);
  }

  private PostAudioRecordButton getPostAudioRecordButton(NoFeedbackRecordAudioPanel<ClientExercise> recordPanel) {
    return recordPanel.getPostAudioRecordButton();
  }

  /**
   * @param response
   * @see ContinuousDialogRecordAudioPanel#usePartial(StreamResponse)
   */
  public void usePartial(StreamResponse response) {
    if (isRecording()) {
      Validity validity = response.getValidity();
      rehearseView.addPacketValidity(validity);

      if (validity == Validity.OK && firstVAD == -1) {
        firstVAD = response.getStreamTimestamp();//System.currentTimeMillis();
        if (DEBUG_PARTIAL) {
          logger.info("usePartial : (" + rehearseView.getNumValidities() +
              ") got first vad : " + firstVAD +
              " (" + (start - firstVAD) + ")" +
              " for " + report() + " diff " + (System.currentTimeMillis() - start));
        }
      } else {
        if (DEBUG_PARTIAL) {
          logger.info("usePartial : (" + rehearseView.getNumValidities() +
              " packets) skip validity " + validity +
              " vad " + firstVAD +
              " for " + report() + " diff " + (System.currentTimeMillis() - start));
        }
      }

      if (response.isStreamStop()) {
        logger.info("usePartial stopStream");
        gotStreamStop = true;
      }
    } else {
      logger.info("usePartial hmm " + report() + " getting response " + response + " but not recording...?");
    }
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

    if (doPushToTalk) {
      //  logger.info("removeMarkCurrent");
      PostAudioRecordButton recordButton = getRecordButton();
      if (recordButton.isRecording()) {
        cancelRecording();
      }

      disableRecordButton();
    }
  }

  @Override
  public void disableRecordButton() {
//    logger.info("disableRecordButton");
//    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("disableRecordButton " +getExID()));
//    logger.info("logException stack " + exceptionAsString);

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
    //  w.setWidth(DIM + "px");
    w.getElement().getStyle().setMarginTop(-5, Style.Unit.PX);
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
    style.setMarginTop(15, Style.Unit.PX);

    if (isMiddle() && exercise.hasEnglishAttr()) {
      style.setProperty("marginLeft", "auto");
    }
//    else {
//      logger.info("setmargin NOT left  auto on " + getExID());
//    }

    flContainer.getElement().setId("RecordDialogExercisePanel_horiz");
    return flContainer;
  }

  /**
   * @see RehearseViewHelper#startRecordingTurn
   */
  public void startRecording() {
    start = System.currentTimeMillis();
    boolean pushToTalk = isPushToTalk();

    logger.info("startRecording " +
        "\n\tfor  " + getExID() + " " +
        "\n\tat   " + start + " or " + new Date(start) +
        "\n\tpush " + pushToTalk
    );

    firstVAD = -1;

    if (pushToTalk) {
      enableRecordButton();
    } else {
      reallyStartOrStopRecording();
    }
  }

  public boolean reallyStartOrStopRecording() {
    logger.info("reallyStartOrStopRecording " + "\n\tfor  " + getExID());
    return getRecordButton().startOrStopRecording();
  }

  /**
   * @see RehearseViewHelper#stopRecordingSafe()
   */
  public void stopRecordingSafe() {
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
   * @see RehearseViewHelper#stopRecordingTurn()
   */
  public boolean gotEndSilenceMaybeStopRecordingTurn() {
    if (doPushToTalk) {
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