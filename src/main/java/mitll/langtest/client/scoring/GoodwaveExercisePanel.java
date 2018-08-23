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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.services.LangTestDatabaseAsync;
import mitll.langtest.shared.exercise.ClientExercise;
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
public abstract class GoodwaveExercisePanel<T extends ClientExercise>
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

  // public static final String PUNCT_REGEX = "[\\?\\.,-\\/#!$%\\^&\\*;:{}=\\-_`~()]";
  /**
   * Removed dashes since broke French.
   */
  public static final String PUNCT_REGEX = "[\\?\\.,\\/#!$%\\^&\\*;:{}=_`~()]";
  static final String SPACE_REGEX = " ";

  private final ListInterface<?, ?> listContainer;

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

  protected final NavigationHelper navigationHelper;
  private boolean hasClickable = false;
  private boolean isJapanese = false;
  private boolean isUrdu = false;
  protected final ExerciseOptions options;

  /**
   * Has a left side -- the question content (Instructions and audio panel (play button, waveform)) <br></br>
   * and a right side -- the charts and gauges
   *
   * @param clientExercise for this exercise
   * @param controller
   * @param listContainer
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel
   */
  protected GoodwaveExercisePanel(final T clientExercise,
                                  final ExerciseController controller,
                                  final ListInterface<?, ?> listContainer,
                                  ExerciseOptions options
  ) {
    this.options = options;
    this.exercise = clientExercise;
    this.controller = controller;
    String language = controller.getLanguage();

    isJapanese = language.equalsIgnoreCase(JAPANESE);
    isUrdu = language.equalsIgnoreCase("urdu");
    this.hasClickable = language.equalsIgnoreCase(MANDARIN) || language.equals(KOREAN) || isJapanese;
    setWidth("100%");

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

//    if (!controller.showOnlyOneExercise()) { // headstart doesn't need navigation, lists, etc.
    center.add(navigationHelper);
    //  }
  }

  protected abstract NavigationHelper getNavigationHelper(ExerciseController controller,
                                                          final ListInterface<?, ?> listContainer,
                                                          boolean addKeyHandler, boolean includeListButtons);

  public void wasRevealed() {
  }

  protected abstract void makeScorePanel(T e, INavigation.VIEWS instance);

  protected void loadNext() {
    listContainer.loadNextExercise(exercise.getID());
  }

  protected void nextWasPressed(ListInterface<?, ?> listContainer, HasID completedExercise) {
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
  protected abstract void addUserRecorder(LangTestDatabaseAsync service, ExerciseController controller, Panel toAddTo,
                                          float screenPortion, T exercise);

  protected abstract void addGroupingStyle(Widget div);

  public void onResize() {
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
  protected abstract Widget getScoringAudioPanel(final T e);


  /**
   * @param exid
   * @param field         @see mitll.langtest.client.qc.QCNPFExercise#makeCommentEntry(String, ExerciseAnnotation)
   * @param commentToPost
   */
  @Override
  public void addIncorrectComment(int exid, final String field, final String commentToPost) {
    addAnnotation(field, ExerciseAnnotation.TYPICAL.INCORRECT, commentToPost, exid);
  }

  @Override
  public void addCorrectComment(int exid, final String field) {
    addAnnotation(field, ExerciseAnnotation.TYPICAL.CORRECT, "", exid);
  }

  private void addAnnotation(final String field, final ExerciseAnnotation.TYPICAL status, final String commentToPost, int exid) {
    controller.getQCService().addAnnotation(exid, field, status.toString(), commentToPost,
        new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("adding annotation", caught);
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
   * @see mitll.langtest.client.qc.QCNPFExercise#getEntry
   */
  protected Panel getContentWidget(String label, String value, boolean includeLabel) {
    //Panel nameValueRow = new FlowPanel();
    DivWidget nameValueRow = new DivWidget();
    nameValueRow.addStyleName("inlineFlex");
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
      InlineHTML englishPhrase = new InlineHTML(value, getDirection(value));
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
    return getDirection(content) == HasDirection.Direction.RTL;
  }

  private HasDirection.Direction getDirection(String content) {
    return WordCountDirectionEstimator.get().estimateDirection(content);
  }

  private InlineHTML makeClickableText(String label, String value, final String html, boolean chineseCharacter) {
    final InlineHTML w = new InlineHTML(html, getDirection(value));
    w.getElement().setId(label + "_" + value + "_" + html);
    if (!removePunct(html).isEmpty()) {
      w.getElement().getStyle().setCursor(Style.Cursor.POINTER);
      w.addClickHandler(clickEvent -> Scheduler.get().scheduleDeferred(() -> {
        String s1 = html.replaceAll(GoodwaveExercisePanel.PUNCT_REGEX, " ").replaceAll("’", " ");
        String s2 = s1.split(GoodwaveExercisePanel.SPACE_REGEX)[0].toLowerCase();
        listContainer.searchBoxEntry(s2);
      }));
      w.addMouseOverHandler(mouseOverEvent -> w.addStyleName("underline"));
      w.addMouseOutHandler(mouseOutEvent -> w.removeStyleName("underline"));
    }

    w.addStyleName(isUrdu ? "urdubigflfont" : "Instruction-data-with-wrap-keep-word");
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

  protected String removePunct(String t) {
    return t.replaceAll(GoodwaveExercisePanel.PUNCT_REGEX, "");
  }

  @Deprecated
  public boolean isBusy() {
    return isBusy;
  }

  protected INavigation.VIEWS getInstance() {
    return options.getInstance();
  }
}
