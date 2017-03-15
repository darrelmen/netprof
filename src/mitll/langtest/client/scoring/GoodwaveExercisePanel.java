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

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.custom.exercise.CommentNPFExercise;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.exercise.PostAnswerProvider;
import mitll.langtest.client.gauge.ASRScorePanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.exercise.AudioRefExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.exercise.ScoredExercise;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class GoodwaveExercisePanel<T extends CommonShell & AudioRefExercise & ScoredExercise> extends HorizontalPanel
    implements BusyPanel, RequiresResize, ProvidesResize, CommentAnnotator {
  public static final String INSTRUCTION_TITLE = "Instruction-title";
  public static final String INSTRUCTION_DATA_WITH_WRAP = "Instruction-data-with-wrap";
  private Logger logger = Logger.getLogger("GoodwaveExercisePanel");

  public static final String CONTEXT = "Context";
  private static final String SAY = "Say";
  private static final String TRANSLITERATION = "Transliteration";

  private static final String MANDARIN = "Mandarin";
  private static final String KOREAN = "Korean";
  private static final String JAPANESE = "Japanese";

  private static final String REFERENCE = "";
  private static final String RECORD_YOURSELF = "Record";
  private static final String RELEASE_TO_STOP = "Release";
  public static final String DEFAULT_SPEAKER = "Default Speaker";

  /**
   * @see ASRScorePanel#addTooltip
   */
  private static final String DOWNLOAD_YOUR_RECORDING = "Download your recording.";

  private final ListInterface listContainer;
  private boolean isBusy = false;

  private Image recordImage1;
  private Image recordImage2;

  protected final T exercise;
  protected final ExerciseController controller;
  protected final LangTestDatabaseAsync service;
  protected ScoreListener scorePanel;
  private AudioPanel contentAudio, answerAudio;
  protected final NavigationHelper navigationHelper;
  private final float screenPortion;
  protected final String instance;
  private boolean hasClickable = false;
  private boolean isJapanese = false;

  /**
   * Has a left side -- the question content (Instructions and audio panel (play button, waveform)) <br></br>
   * and a right side -- the charts and gauges {@link ASRScorePanel}
   *
   * @param commonExercise for this exercise
   * @param controller
   * @param listContainer
   * @param screenPortion
   * @param instance
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel
   */
  protected GoodwaveExercisePanel(final T commonExercise, final ExerciseController controller,
                                  final ListInterface<CommonShell> listContainer,
                                  float screenPortion, boolean addKeyHandler, String instance) {
    this.exercise = commonExercise;
    this.controller = controller;
    this.service = controller.getService();
    this.screenPortion = screenPortion;
    this.instance = instance;
    String language = controller.getLanguage();

    isJapanese = language.equalsIgnoreCase(JAPANESE);
    this.hasClickable = language.equalsIgnoreCase(MANDARIN) || language.equals(KOREAN) || isJapanese;
    setWidth("100%");
    //   addStyleName("inlineBlockStyle");
    getElement().setId("GoodwaveExercisePanel");

    this.navigationHelper = getNavigationHelper(controller, listContainer, addKeyHandler);
    this.listContainer = listContainer;
    navigationHelper.addStyleName("topBarMargin");

    addContent();
  }

  private void addContent() {
    final Panel center = new VerticalPanel();
    center.getElement().setId("GoodwaveVerticalCenter");
    center.addStyleName("floatLeft");
    // attempt to left justify

    ASRScorePanel widgets = makeScorePanel(exercise, instance);

    addQuestionContentRow(exercise, center);

    // content is on the left side
    add(center);

    // score panel with gauge is on the right
    if (widgets != null && !controller.getProps().isNoModel() && controller.isRecordingEnabled()) {
      add(widgets);
    }
    if (controller.isRecordingEnabled() && !controller.getProps().isNoModel()) {
      addUserRecorder(service, controller, center, screenPortion, exercise); // todo : revisit screen portion...
    }

    if (!controller.showOnlyOneExercise()) { // headstart doesn't need navigation, lists, etc.
      center.add(navigationHelper);
    }
  }

  protected NavigationHelper<CommonShell> getNavigationHelper(ExerciseController controller,
                                                              final ListInterface<CommonShell> listContainer, boolean addKeyHandler) {
    return new NavigationHelper<CommonShell>(getLocalExercise(), controller, new PostAnswerProvider() {
      @Override
      public void postAnswers(ExerciseController controller, HasID completedExercise) {
        nextWasPressed(listContainer, completedExercise);
      }
    }, listContainer, true, addKeyHandler, false);
  }

  public void wasRevealed() {
  }

  protected ASRScorePanel makeScorePanel(T e, String instance) {
    ASRScorePanel widgets = new ASRScorePanel("GoodwaveExercisePanel_" + instance, controller, e.getID());
    scorePanel = widgets;
    return widgets;
  }

  protected void loadNext() {
    listContainer.loadNextExercise(exercise.getID());
  }

  protected void nextWasPressed(ListInterface listContainer, HasID completedExercise) {
    navigationHelper.enableNextButton(false);
    listContainer.loadNextExercise(completedExercise.getID());
  }

  protected void addQuestionContentRow(T e, Panel hp) {
    hp.add(getQuestionContent(e));
  }

  public void setBusy(boolean v) {
    this.isBusy = v;
  }

  /**
   * For every question,
   * <ul>
   * <li>show the text of the question,  </li>
   * <li>the prompt to the test taker (e.g "Speak your response in English")  </li>
   * <li>an answer widget (either a simple text box, an flash audio record and playback widget, or a list of the answers, when grading </li>
   * </ul>     <br></br>
   * Remember the answer widgets so we can notice which have been answered, and then know when to enable the next button.
   *
   * @param service
   * @param controller    used in subclasses for audio control
   * @param screenPortion
   * @param exercise
   * @see #GoodwaveExercisePanel
   */
  protected void addUserRecorder(LangTestDatabaseAsync service, ExerciseController controller, Panel toAddTo,
                                 float screenPortion, T exercise) {
    DivWidget div = new DivWidget();
    div.getElement().setId("GoodwaveExercisePanel_UserRecorder");
    ScoringAudioPanel answerWidget = getAnswerWidget(service, controller, screenPortion);
    String refAudio = exercise.getRefAudio();
    if (refAudio == null) {
      refAudio = exercise.getSlowAudioRef();
    }

    showRecordingHistory(exercise, answerWidget, refAudio);
    div.add(answerWidget);

    addGroupingStyle(div);

    //   addBelowPlaybackWidget(exercise, toAddTo);
    toAddTo.add(div);
  }

  private void showRecordingHistory(T exercise, ScoringAudioPanel answerWidget, String refAudio) {
    answerWidget.setRefAudio(refAudio);
    for (CorrectAndScore score : exercise.getScores()) {
      answerWidget.addScore(score);
    }
    answerWidget.setClassAvg(exercise.getAvgScore());
    answerWidget.showChart();
  }

  protected void addGroupingStyle(Widget div) {
    div.addStyleName("buttonGroupInset6");
  }

  public void onResize() {
    // logger.info("got onResize for" + instance);
    if (contentAudio != null) {
      //  logger.info("got onResize  contentAudio for" + instance);
      contentAudio.onResize();
    }
    if (answerAudio != null) {
      //   logger.info("got onResize answerAudio for" + instance);
      answerAudio.onResize();
    }
  }

  /**
   * Three lines - the unit/chapter/item info, then the item content - vocab item, english, etc.
   * And finally the audio panel showing the waveform.
   *
   * @param exercise for this exercise
   * @return the panel that has the instructions and the audio panel
   * @see #GoodwaveExercisePanel
   */
  private Widget getQuestionContent(T exercise) {
    Panel vp = new VerticalPanel();
    vp.getElement().setId("getQuestionContent_verticalContainer");
    vp.addStyleName("blockStyle");
//    vp.addStyleName("topFiveMargin");

    new UnitChapterItemHelper<T>(controller.getTypeOrder()).addUnitChapterItem(exercise, vp);
    vp.add(getItemContent(exercise));
    vp.add(getAudioPanel(exercise));
    return vp;
  }

  private Panel getAudioPanel(T e) {
    Panel div = new SimplePanel(getScoringAudioPanel(e));
    div.getElement().setId("scoringAudioPanel_div");
    div.addStyleName("trueInlineStyle");
    div.addStyleName("floatLeft");
    addGroupingStyle(div);
    return div;
  }

  protected abstract Widget getItemContent(T e);

  /**
   * @param e
   * @return
   * @see #getQuestionContent
   */
  protected Widget getScoringAudioPanel(final T e) {
    String path = e.getRefAudio() != null ? e.getRefAudio() : e.getSlowAudioRef();

    if (path != null) {
      path = CompressedAudio.getPathNoSlashChange(path);
    }
    //else {
//      logger.info("getScoringAudioPanel path is " +path +
//          " for " + e.getAudioAttributes());
    // }
    contentAudio = getAudioPanel(path);
    contentAudio.setScreenPortion(screenPortion);
    return contentAudio;
  }

  private ASRScoringAudioPanel getAudioPanel(String path) {
    ASRScoringAudioPanel audioPanel = makeFastAndSlowAudio(path);
    audioPanel.getElement().setId("ASRScoringAudioPanel");
    if (audioPanel.hasAudio()) {
      Style style = audioPanel.getPlayButton().getElement().getStyle();
      style.setMarginTop(10, Style.Unit.PX);
      style.setMarginBottom(10, Style.Unit.PX);
    }
    return audioPanel;
  }

  protected ASRScoringAudioPanel makeFastAndSlowAudio(String path) {
    return new FastAndSlowASRScoringAudioPanel(getLocalExercise(), path, service, controller, scorePanel, instance);
  }

  /**
   * @param commentToPost
   * @param field
   * @see mitll.langtest.client.qc.QCNPFExercise#makeCommentEntry(String, mitll.langtest.shared.ExerciseAnnotation)
   */
  @Override
  public void addIncorrectComment(final String commentToPost, final String field) {
    addAnnotation(field, ExerciseAnnotation.TYPICAL.INCORRECT, commentToPost);
  }

  @Override
  public void addCorrectComment(final String field) {
    addAnnotation(field, ExerciseAnnotation.TYPICAL.CORRECT, "");
  }

  private void addAnnotation(final String field, final ExerciseAnnotation.TYPICAL status, final String commentToPost) {
    service.addAnnotation(getLocalExercise().getID(), field, status.toString(), commentToPost, controller.getUser(),
        new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Void result) {
//        System.out.println("\t" + new Date() + " : onSuccess : posted to server " + getExercise().getID() +
//            " field '" + field + "' commentLabel '" + commentToPost + "' is " + status);//, took " + (now - then) + " millis");
          }
        });
  }

  /**
   * TODO : also should get back button to work... maybe needs to encode the text box state?
   *
   * @param label
   * @param value
   * @param includeLabel
   * @return
   * @see mitll.langtest.client.custom.exercise.CommentNPFExercise#getEntry
   * @see mitll.langtest.client.qc.QCNPFExercise#getEntry
   */
  protected Panel getContentWidget(String label, String value, boolean includeLabel) {
    Panel nameValueRow = new FlowPanel();
    nameValueRow.getElement().setId("nameValueRow_" + label);
    nameValueRow.addStyleName("Instruction");

    if (includeLabel) {
      InlineHTML labelWidget = new InlineHTML(label);
      labelWidget.addStyleName(INSTRUCTION_TITLE);
      nameValueRow.add(labelWidget);
      labelWidget.setWidth("150px");
    }

    // TODO : for now, since we need to deal with underline... somehow...
    // and when clicking inside dialog, seems like we need to dismiss dialog...
    if (label.contains(CONTEXT)) {
      InlineHTML englishPhrase = new InlineHTML(value, WordCountDirectionEstimator.get().estimateDirection(value));
      englishPhrase.addStyleName(INSTRUCTION_DATA_WITH_WRAP);
      if (label.contains("Meaning")) {
        englishPhrase.addStyleName("englishFont");
      }
      if (isPashto()) englishPhrase.addStyleName("pashtofont");
      nameValueRow.add(englishPhrase);
      addTooltip(englishPhrase, label.replaceAll(":", ""));
      englishPhrase.addStyleName("leftFiveMargin");
    } else {
      getClickableWords(label, value, nameValueRow);
    }

    return nameValueRow;
  }

  /**
   * @param label
   * @param value
   * @param nameValueRow
   * @see #getContentWidget(String, String, boolean)
   */
  private void getClickableWords(String label, String value, Panel nameValueRow) {
    DivWidget horizontal = new DivWidget();

    Element element = horizontal.getElement();

    element.setId("clickableWords_" + label);
    Style style = element.getStyle();
    style.setDisplay(Style.Display.INLINE_BLOCK);
    style.setMarginBottom(-8, Style.Unit.PX);

    List<String> tokens = new ArrayList<>();
    boolean flLine = label.contains(SAY) || (isJapanese && label.contains(TRANSLITERATION));
    boolean isChineseCharacter = flLine && hasClickable;
    if (isChineseCharacter) {
      for (int i = 0, n = value.length(); i < n; i++) {
        char c = value.charAt(i);
        Character character = c;
        final String html = character.toString();
        tokens.add(html);
      }
    } else {
      tokens = Arrays.asList(value.split(CommentNPFExercise.SPACE_REGEX));
    }

    if (isRTLContent(value)) {
      Collections.reverse(tokens);
    }
    for (String token : tokens) {
      horizontal.add(makeClickableText(label, value, token, isChineseCharacter));
    }

    nameValueRow.add(horizontal);
    horizontal.addStyleName("leftFiveMargin");
  }

  private boolean isRTLContent(String content) {
    return controller.isRightAlignContent() || WordCountDirectionEstimator.get().estimateDirection(content) == HasDirection.Direction.RTL;
  }

  private InlineHTML makeClickableText(String label, String value, final String html, boolean chineseCharacter) {
    final InlineHTML w = new InlineHTML(html, WordCountDirectionEstimator.get().estimateDirection(value));

    String s = removePunct(html);
    if (!s.isEmpty()) {
      w.getElement().getStyle().setCursor(Style.Cursor.POINTER);
      w.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent clickEvent) {
          Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
              String s1 = html.replaceAll(CommentNPFExercise.PUNCT_REGEX, " ").replaceAll("’", " ");
              String s2 = s1.split(CommentNPFExercise.SPACE_REGEX)[0].toLowerCase();
              listContainer.searchBoxEntry(s2);
            }
          });
        }
      });
      w.addMouseOverHandler(new MouseOverHandler() {
        @Override
        public void onMouseOver(MouseOverEvent mouseOverEvent) {
          w.addStyleName("underline");
        }
      });
      w.addMouseOutHandler(new MouseOutHandler() {
        @Override
        public void onMouseOut(MouseOutEvent mouseOutEvent) {
          w.removeStyleName("underline");
        }
      });
    }

    w.addStyleName("Instruction-data-with-wrap-keep-word");
    if (label.contains("Meaning")) {
      w.addStyleName("englishFont");
    }
    if (isPashto()) {
      w.addStyleName("pashtofont");
    }
    if (!chineseCharacter) w.addStyleName("rightFiveMargin");

    return w;
  }

  private boolean isPashto() {
    return controller.getLanguage().equalsIgnoreCase("Pashto");
  }


  /**
   * @return
   * @see mitll.langtest.client.qc.QCNPFExercise#populateCommentRow
   */
  protected Label getCommentLabel() {
    final Label commentLabel = new Label("comment?");
    commentLabel.getElement().getStyle().setBackgroundColor("#ff0000");
    commentLabel.addStyleName("ImageOverlay");
    return commentLabel;
  }

  protected Tooltip addTooltip(Widget w, String tip) {
    return new TooltipHelper().addTooltip(w, tip);
  }

  /**
   * Has a answerPanel mark to indicate when the saved audio has been successfully posted to the server.
   *
   * @param service
   * @param controller
   * @param screenPortion
   * @return
   * @see #addUserRecorder
   */
  private ScoringAudioPanel getAnswerWidget(LangTestDatabaseAsync service, final ExerciseController controller, float screenPortion) {
    ScoringAudioPanel widgets = new ASRRecordAudioPanel(service, controller, getLocalExercise(), instance);
    widgets.addScoreListener(scorePanel);
    answerAudio = widgets;
    answerAudio.setScreenPortion(screenPortion);

    return widgets;
  }

  protected String removePunct(String t) {
    return t.replaceAll(CommentNPFExercise.PUNCT_REGEX, "");
  }

  protected T getLocalExercise() {
    return exercise;
  }

  /**
   * An ASR scoring panel with a record button.
   */
  private class ASRRecordAudioPanel extends ASRScoringAudioPanel<T> {
    static final String DOWNLOAD_AUDIO = "downloadAudio";
    private final int index;
    private PostAudioRecordButton postAudioRecordButton;
    private PlayAudioPanel playAudioPanel;
    private IconAnchor download;
    private Anchor downloadAnchor;
    private Panel downloadContainer;

    /**
     * @param service
     * @param controller
     * @param exercise
     * @param instance
     * @see GoodwaveExercisePanel#getAnswerWidget
     */
    ASRRecordAudioPanel(LangTestDatabaseAsync service, ExerciseController controller, T exercise, String instance) {
      super(exercise.getForeignLanguage(), exercise.getTransliteration(), service, controller, scorePanel, REFERENCE,
          exercise.getID(), exercise, instance, Result.AUDIO_TYPE_PRACTICE);
      this.index = 1;
      getElement().setId("ASRRecordAudioPanel");
    }

    /**
     * So here we're trying to make the record and play buttons know about each other
     * to the extent that when we're recording, we can't play audio, and when we're playing
     * audio, we can't record. We also mark the widget as busy so we can't move on to a different exercise.
     *
     * @param toTheRightWidget
     * @param buttonTitle
     * @param recordButtonTitle
     * @return
     * @see AudioPanel#getPlayButtons
     */
    @Override
    protected PlayAudioPanel makePlayAudioPanel(Widget toTheRightWidget, String buttonTitle, String audioType, String recordButtonTitle) {
      recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png"));
      recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png"));
      postAudioRecordButton = new MyPostAudioRecordButton(controller);
      postAudioRecordButton.getElement().getStyle().setMargin(8, Style.Unit.PX);
      playAudioPanel = new MyPlayAudioPanel(recordImage1, recordImage2, soundManager, postAudioRecordButton,
          GoodwaveExercisePanel.this);
      return playAudioPanel;
    }

    private class MyPlayAudioPanel extends PlayAudioPanel {
      public MyPlayAudioPanel(Image recordImage1, Image recordImage2, SoundManagerAPI soundManager,
                              final PostAudioRecordButton postAudioRecordButton1,
                              final GoodwaveExercisePanel goodwaveExercisePanel) {
        super(soundManager, new PlayListener() {
          public void playStarted() {
            goodwaveExercisePanel.setBusy(true);
            postAudioRecordButton1.setEnabled(false);
          }

          public void playStopped() {
            goodwaveExercisePanel.setBusy(false);
            postAudioRecordButton1.setEnabled(true);
          }
        }, "", null);
        add(recordImage1);
        recordImage1.setVisible(false);
        add(recordImage2);
        recordImage2.setVisible(false);
        getElement().setId("GoodwaveExercisePanel_MyPlayAudioPanel");
      }

      /**
       * @param optionalToTheRight
       * @see mitll.langtest.client.sound.PlayAudioPanel#PlayAudioPanel(mitll.langtest.client.sound.SoundManagerAPI, String, com.google.gwt.user.client.ui.Widget)
       */
      @Override
      protected void addButtons(Widget optionalToTheRight) {
        add(postAudioRecordButton);
        postAudioRecordButton.addStyleName("rightFiveMargin");
        super.addButtons(optionalToTheRight);

        addDownloadAudioWidget();
      }

      private void addDownloadAudioWidget() {
        downloadContainer = new DivWidget();
        downloadContainer.setWidth("40px");

        DivWidget north = new DivWidget();
        north.add(getDownloadIcon());
        downloadContainer.add(north);

        DivWidget south = new DivWidget();
        south.add(getDownloadAnchor());
        downloadContainer.add(south);
        downloadContainer.setVisible(false);
        downloadContainer.addStyleName("leftFiveMargin");

        add(downloadContainer);
      }
    }

    private Anchor getDownloadAnchor() {
      downloadAnchor = new Anchor();
      downloadAnchor.setHTML("<span><font size=-1>Download</font></span>");
      addTooltip(downloadAnchor, DOWNLOAD_YOUR_RECORDING);

      downloadAnchor.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          controller.logEvent(downloadAnchor, "DownloadUserAudio_Anchor",
              exerciseID, "downloading audio file " + audioPath);
        }
      });
      return downloadAnchor;
    }

    private IconAnchor getDownloadIcon() {
      download = new IconAnchor();
      download.getElement().setId("Download_user_audio_link");
      download.setIcon(IconType.DOWNLOAD);
      download.setIconSize(IconSize.TWO_TIMES);
      download.getElement().getStyle().setMarginLeft(19, Style.Unit.PX);

      addTooltip(download, DOWNLOAD_YOUR_RECORDING);

      download.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          controller.logEvent(download, "DownloadUserAudio_Icon", exerciseID,
              "downloading audio file " + audioPath);
        }
      });
      return download;
    }

    /**
     * @see mitll.langtest.server.DownloadServlet#returnAudioFile(javax.servlet.http.HttpServletResponse, mitll.langtest.server.database.DatabaseImpl, String)
     * @see #useResult(PretestScore, ImageAndCheck, ImageAndCheck, boolean, String)
     */
    private void setDownloadHref() {
      downloadContainer.setVisible(true);

      String audioPathToUse = audioPath.endsWith(".ogg") ? audioPath.replaceAll(".ogg", ".mp3") : audioPath;

      String href = DOWNLOAD_AUDIO +
          "?file=" +
          audioPathToUse +
          "&" +
          "exerciseID=" +
          exerciseID +
          "&" +
          "userID=" +
          controller.getUser();
      download.setHref(href);
      downloadAnchor.setHref(href);
    }

    /**
     * @see #makePlayAudioPanel(com.google.gwt.user.client.ui.Widget, String, String, String)
     */
    private class MyPostAudioRecordButton extends PostAudioRecordButton {
      MyPostAudioRecordButton(ExerciseController controller) {
        super(getLocalExercise().getID(), controller, ASRRecordAudioPanel.this.service, ASRRecordAudioPanel.this.index,
            true,
            RECORD_YOURSELF, controller.getProps().doClickAndHold() ? RELEASE_TO_STOP : "Stop", Result.AUDIO_TYPE_PRACTICE);
      }

      @Override
      public void useResult(AudioAnswer result) {
        setResultID(result.getResultID());
        getImagesForPath(result.getPath());
        setDownloadHref();
      }

      @Override
      public void startRecording() {
        playAudioPanel.setEnabled(false);
        isBusy = true;
        controller.logEvent(this, "RecordButton", getExerciseID(), "startRecording");

        super.startRecording();
        recordImage1.setVisible(true);
        downloadContainer.setVisible(false);
      }

      @Override
      public void stopRecording(long duration) {
        controller.logEvent(this, "RecordButton", getExerciseID(), "stopRecording");

        playAudioPanel.setEnabled(true);
        isBusy = false;
        super.stopRecording(duration);
        recordImage1.setVisible(false);
        recordImage2.setVisible(false);
      }

      @Override
      public void flip(boolean first) {
        recordImage1.setVisible(first);
        recordImage2.setVisible(!first);
      }

      /**
       * @param result
       * @see RecordingListener#stopRecording(long)
       */
      @Override
      protected void useInvalidResult(AudioAnswer result) {
        super.useInvalidResult(result);
        playAudioPanel.setEnabled(false);
      }
    }
  }

  public boolean isBusy() {
    return isBusy;
  }
}
