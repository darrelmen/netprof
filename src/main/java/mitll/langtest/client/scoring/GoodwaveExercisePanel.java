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
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.exercise.PostAnswerProvider;
import mitll.langtest.client.gauge.ASRScorePanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.services.ListService;
import mitll.langtest.client.services.ListServiceAsync;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseAnnotation;
import mitll.langtest.shared.exercise.HasID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class GoodwaveExercisePanel<T extends CommonExercise>
    extends HorizontalPanel
    implements BusyPanel, RequiresResize, ProvidesResize, CommentAnnotator {
  //private Logger logger = Logger.getLogger("GoodwaveExercisePanel");
  /**
   *
   */
  public static final String CONTEXT = "Context";
  private static final String SAY = "Say";
  private static final String TRANSLITERATION = "Transliteration";

  private static final String MANDARIN = "Mandarin";
  private static final String KOREAN = "Korean";
  private static final String JAPANESE = "Japanese";
  public static final String DEFAULT_SPEAKER = "Default Speaker";
  private static final String MEANING = "Meaning";

  public static final String PUNCT_REGEX = "[\\?\\.,-\\/#!$%\\^&\\*;:{}=\\-_`~()]";
  public static final String SPACE_REGEX = " ";

  private final ListInterface listContainer;

  /**
   * TODO : remove me
   */
  @Deprecated
  boolean isBusy = false;

  /**
   * TODO make better relationship with ASRRecordAudioPanel
   */
  Image recordImage1;
  Image recordImage2;

  protected final T exercise;
  protected final ExerciseController controller;

  protected final ListServiceAsync listService = GWT.create(ListService.class);

  private AudioPanel contentAudio, answerAudio;
  protected final NavigationHelper navigationHelper;
  //  private final float screenPortion;
//  protected final String instance;
  private boolean hasClickable = false;
  private boolean isJapanese = false;
  //  private boolean allowRecording = true;
  protected ExerciseOptions options;

  /**
   * Has a left side -- the question content (Instructions and audio panel (play button, waveform)) <br></br>
   * and a right side -- the charts and gauges {@link ASRScorePanel}
   *
   * @param commonExercise for this exercise
   * @param controller
   * @param listContainer
   * @paramx screenPortion
   * @paramx instance
   * @paramx allowRecording
   * @paramx includeListButtons
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel
   */
  protected GoodwaveExercisePanel(final T commonExercise,
                                  final ExerciseController controller,
                                  final ListInterface<CommonShell,T> listContainer,
                                  ExerciseOptions options
  ) {
    this.options = options;
    this.exercise = commonExercise;
    this.controller = controller;
    String language = controller.getLanguage();

    isJapanese = language.equalsIgnoreCase(JAPANESE);
    this.hasClickable = language.equalsIgnoreCase(MANDARIN) || language.equals(KOREAN) || isJapanese;
    setWidth("100%");
    //   addStyleName("inlineBlockStyle");
    getElement().setId("GoodwaveExercisePanel");

    this.navigationHelper = getNavigationHelper(controller, listContainer, options.isAddKeyHandler(), options.isIncludeListButtons());
    this.listContainer = listContainer;

    addContent();
  }

  protected boolean isRTL(T exercise) {
    return isRTLContent(exercise.getForeignLanguage());
  }

  /**
   * @see #GoodwaveExercisePanel
   */
  private void addContent() {
    //  final Panel center = new VerticalPanel();
    final Panel center = new DivWidget();
    center.getElement().setId("GoodwaveVerticalCenter");
    center.addStyleName("floatLeftAndClear");
    // attempt to left justify

    makeScorePanel(exercise, options.getInstance());

    addQuestionContentRow(exercise, center);

    // content is on the left side
    add(center);

    if (controller.isRecordingEnabled() && options.isAllowRecording()) {
      addUserRecorder(controller.getService(), controller, center, options.getScreenPortion(), exercise); // todo : revisit screen portion...
    }

    if (!controller.showOnlyOneExercise()) { // headstart doesn't need navigation, lists, etc.
      center.add(navigationHelper);
    }
  }

  protected NavigationHelper<CommonShell> getNavigationHelper(ExerciseController controller,
                                                              final ListInterface<CommonShell,T> listContainer,
                                                              boolean addKeyHandler, boolean includeListButtons) {
    NavigationHelper<CommonShell> widgets = new NavigationHelper<>(getLocalExercise(), controller, new PostAnswerProvider() {
      @Override
      public void postAnswers(ExerciseController controller, HasID completedExercise) {
        nextWasPressed(listContainer, completedExercise);
      }
    },
        listContainer,
        true,
        addKeyHandler,
        false,
        false);
    widgets.addStyleName("topBarMargin");
    return widgets;
  }

  public void wasRevealed() {
  }

  protected ASRScorePanel makeScorePanel(T e, String instance) {
    return null;
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
    ScoringAudioPanel answerWidget = getAnswerWidget(controller, screenPortion);
    showRecordingHistory(exercise, answerWidget);
    div.add(answerWidget);

    addGroupingStyle(div);
    toAddTo.add(div);
  }

  private void showRecordingHistory(T exercise, ScoringAudioPanel answerWidget) {
//    answerWidget.setRefAudio(refAudio);
    //  for (CorrectAndScore score : exercise.getScores()) {
    answerWidget.addScores(exercise.getScores());
    // }
//    answerWidget.setClassAvg(exercise.getAvgScore());
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
    div.addStyleName("floatLeftAndClear");
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
    contentAudio.setScreenPortion(options.getScreenPortion());
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
    return new FastAndSlowASRScoringAudioPanel(getLocalExercise(), path, controller, options.getInstance());
  }

  /**
   * @param commentToPost
   * @param field
   * @see mitll.langtest.client.qc.QCNPFExercise#makeCommentEntry(String, ExerciseAnnotation)
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
    controller.getQCService().addAnnotation(getLocalExercise().getID(), field, status.toString(), commentToPost,
        new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Void result) {
//        System.out.println("\t" + new Date() + " : onSuccess : posted to server " + getExercise().getOldID() +
//            " field '" + field + "' commentLabel '" + commentToPost + "' is " + status);//, took " + (now - then) + " millis");
          }
        });
  }

  protected int getUser() {
    return controller.getUserState().getUser();
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

//    logger.info("got content widget " + label + "'" +value+
//        "'");

    if (includeLabel) {
      InlineHTML labelWidget = new InlineHTML(label);
      labelWidget.addStyleName("Instruction-title");
      nameValueRow.add(labelWidget);
      labelWidget.setWidth("150px");
    }

    // TODO : for now, since we need to deal with underline... somehow...
    // and when clicking inside dialog, seems like we need to dismiss dialog...
    if (label.contains(CONTEXT)) {
      InlineHTML englishPhrase = new InlineHTML(value, WordCountDirectionEstimator.get().estimateDirection(value));
      englishPhrase.addStyleName("Instruction-data-with-wrap");
      if (label.contains(MEANING)) {
        englishPhrase.addStyleName("englishFont");
      }
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
    horizontal.getElement().getStyle().setDisplay(Style.Display.INLINE_BLOCK);

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
      tokens = Arrays.asList(value.split(GoodwaveExercisePanel.SPACE_REGEX));
    }

    if (isRTL(exercise) && flLine) {
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
              String s1 = html.replaceAll(GoodwaveExercisePanel.PUNCT_REGEX, " ").replaceAll("’", " ");
              String s2 = s1.split(GoodwaveExercisePanel.SPACE_REGEX)[0].toLowerCase();
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
    if (label.contains(MEANING)) {
      w.addStyleName("englishFont");
    }
    if (!chineseCharacter) w.addStyleName("rightFiveMargin");

    return w;
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
   * @param controller
   * @param screenPortion
   * @return
   * @see #addUserRecorder
   */
  private ScoringAudioPanel getAnswerWidget(final ExerciseController controller, float screenPortion) {
    ScoringAudioPanel widgets =
        new ASRRecordAudioPanel<T>(this, controller, getLocalExercise(), options.getInstance());
    //widgets.addScoreListener();
    answerAudio = widgets;
    answerAudio.setScreenPortion(screenPortion);

    return widgets;
  }

  protected String removePunct(String t) {
    return t.replaceAll(GoodwaveExercisePanel.PUNCT_REGEX, "");
  }

  protected T getLocalExercise() {
    return exercise;
  }

  @Deprecated
  public boolean isBusy() {
    return isBusy;
  }

  protected String getInstance() { return options.getInstance(); }
}
