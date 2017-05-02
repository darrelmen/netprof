package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.exercise.CommentBox;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.sound.AllHighlight;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.client.sound.SegmentHighlightAudioControl;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.scoring.AlignmentOutput;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 3/23/17.
 */
public class TwoColumnExercisePanel<T extends CommonExercise> extends DivWidget implements AudioChangeListener {
  private Logger logger = Logger.getLogger("TwoColumnExercisePanel");

  private static final String EMAIL = "Email Item";
  public static final int CONTEXT_INDENT = 56;//40;

  private final List<CorrectAndScore> correctAndScores;
  private final T exercise;
  private final ExerciseController controller;

  private final AnnotationHelper annotationHelper;
  private final ClickableWords<T> clickableWords;
  private final boolean showInitially = false;
  private final UnitChapterItemHelper<CommonExercise> commonExerciseUnitChapterItemHelper;
  private final ListInterface<CommonShell, T> listContainer;
  private ChoicePlayAudioPanel playAudio;
  Map<Integer, AlignmentOutput> alignments = new HashMap<>();
  private Map<Integer, Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>>> idToTypeToSegmentToWidget = new HashMap<>();
  private List<IHighlightSegment> altflClickables;
  private List<IHighlightSegment> flclickables;

  /**
   * Has a left side -- the question content (Instructions and audio panel (play button, waveform)) <br></br>
   * and a right side --
   *
   * @param commonExercise for this exercise
   * @param controller
   * @param listContainer
   * @paramx screenPortion
   * @paramx instance
   * @paramx allowRecording
   * @paramx includeListButtons
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel
   * @see mitll.langtest.client.banner.NewLearnHelper#getFactory
   */
  public TwoColumnExercisePanel(final T commonExercise,
                                final ExerciseController controller,
                                final ListInterface<CommonShell, T> listContainer,
                                List<CorrectAndScore> correctAndScores
  ) {
    this.exercise = commonExercise;
    this.controller = controller;
    this.listContainer = listContainer;

    getElement().setId("TwoColumnExercisePanel");
    addStyleName("cardBorderShadow");
    addStyleName("bottomFiveMargin");
    addStyleName("floatLeftAndClear");
    setWidth("100%");

    annotationHelper = new AnnotationHelper(controller, commonExercise.getID());
    clickableWords = new ClickableWords<T>(listContainer, commonExercise, controller.getLanguage(), controller.getExerciseService());
    this.correctAndScores = correctAndScores;
    commonExerciseUnitChapterItemHelper = new UnitChapterItemHelper<>(controller.getTypeOrder());
    add(getItemContent(commonExercise));

    addMouseOverHandler(event -> getRefAudio(controller));
  }

  private void getRefAudio(ExerciseController controller) {
    if (playAudio != null && playAudio.getCurrentAudioAttr() != null) {
      AudioAttribute currentAudioAttr = playAudio.getCurrentAudioAttr();

      int refID = currentAudioAttr.getUniqueID();
      int contextRefID = -1; //contextPlay.getCurrentAudioID();

      //  logger.info("getRefAudio asking for " + refID);
//    logger.info("asking for " + contextRefID);

      List<Integer> req = new ArrayList<>();
      if (refID != -1) {
        if (!alignments.containsKey(refID))
          req.add(refID);
      }

      if (contextRefID != -1) {
        if (!alignments.containsKey(contextRefID))
          req.add(contextRefID);
      }

      if (req.isEmpty()) {
        registerSegmentHighlight(refID, contextRefID, currentAudioAttr.getDurationInMillis());
      } else {
        controller.getScoringService().getAlignments(controller.getProjectStartupInfo().getProjectid(),
            req, new AsyncCallback<Map<Integer, AlignmentOutput>>() {
              @Override
              public void onFailure(Throwable caught) {

              }

              @Override
              public void onSuccess(Map<Integer, AlignmentOutput> result) {
                alignments.putAll(result);
                registerSegmentHighlight(refID, contextRefID, currentAudioAttr.getDurationInMillis());
                cacheOthers();
              }
            });
      }
    }
  }

  private void cacheOthers() {
    Set<Integer> req = new HashSet<>(playAudio.getAllAudioIDs());

    req.removeAll(alignments.keySet());

    if (!req.isEmpty()) {
      logger.info("Asking for audio alignments for " + req);

      ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
      if (projectStartupInfo != null) {
        controller.getScoringService().getAlignments(projectStartupInfo.getProjectid(),
            req, new AsyncCallback<Map<Integer, AlignmentOutput>>() {
              @Override
              public void onFailure(Throwable caught) {

              }

              @Override
              public void onSuccess(Map<Integer, AlignmentOutput> result) {
                alignments.putAll(result);
                //registerSegmentHighlight(refID, contextRefID);
              }
            });
      }
    }
  }


  private void registerSegmentHighlight(int refID, int contextRefID, long durationInMillis) {
    if (refID != -1) {
      audioChanged(refID, durationInMillis);
//      matchSegmentToWidgetForAudio(refID, alignments.get(refID));
//      Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> typeToSegmentToWidget = idToTypeToSegmentToWidget.get(refID);
//      logger.info("registerSegment for " + refID + " : " + typeToSegmentToWidget);
//      playAudio.setListener(new SegmentHighlightAudioControl(typeToSegmentToWidget));
    }

//    if (contextRefID != -1) {
//      matchSegmentToWidgetForAudio(contextRefID, alignments.get(contextRefID));
//    }
  }

  private static final boolean DEBUG = false;

  @Override
  public void audioChanged(int id, long duration) {
    AlignmentOutput value2 = alignments.get(id);
    if (value2 != null) {
      if (DEBUG) logger.info("audioChanged for ex " + exercise.getID() + " audio id " + id);
      matchSegmentToWidgetForAudio(id, duration, value2);
      Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> typeToSegmentToWidget = idToTypeToSegmentToWidget.get(id);
      if (DEBUG) logger.info("audioChanged for ex " + exercise.getID() +
          " audio id " + id + " : " +
          (typeToSegmentToWidget == null ? "missing" : typeToSegmentToWidget.size()));
      if (typeToSegmentToWidget == null) {
        logger.warning("audioChanged no type to segment for " + id + " and exercise " + exercise.getID());
      } else {
        TreeMap<TranscriptSegment, IHighlightSegment> transcriptSegmentIHighlightSegmentTreeMap = typeToSegmentToWidget.get(NetPronImageType.WORD_TRANSCRIPT);
        if (DEBUG) logger.info("audioChanged segments now " + transcriptSegmentIHighlightSegmentTreeMap.keySet());
        playAudio.setListener(new SegmentHighlightAudioControl(typeToSegmentToWidget));
      }
    }
  }

/*
  private void matchSegmentToWidget(Map<Integer, AlignmentOutput> result) {
    for (Map.Entry<Integer, AlignmentOutput> pair : result.entrySet()) {
      Integer audioID = pair.getKey();
      matchSegmentToWidgetForAudio(audioID, pair.getValue());
    }
  }
*/

  /**
   * TODO : what to do about chinese?
   *
   * @param audioID
   * @param value2
   * @see #
   */
  private void matchSegmentToWidgetForAudio(Integer audioID, long durationInMillis, AlignmentOutput value2) {
    Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> value = new HashMap<>();
    idToTypeToSegmentToWidget.put(audioID, value);
    TreeMap<TranscriptSegment, IHighlightSegment> segmentToWidget = new TreeMap<>();
    value.put(NetPronImageType.WORD_TRANSCRIPT, segmentToWidget);

    if (value2 == null) {
      logger.warning("matchSegmentToWidgetForAudio no alignment for " + audioID);
      segmentToWidget.put(new TranscriptSegment(0, (float) durationInMillis, "all", 0), new AllHighlight(flclickables));
    } else {
      Iterator<IHighlightSegment> iterator = flclickables.iterator();

      List<TranscriptSegment> transcriptSegments = value2.getTypeToSegments().get(NetPronImageType.WORD_TRANSCRIPT);
      if (transcriptSegments != null) {
        for (TranscriptSegment seg : transcriptSegments) {
          if (!seg.getEvent().equalsIgnoreCase("sil")) {
            if (iterator.hasNext()) {
              segmentToWidget.put(seg, iterator.next());
            } else {
              logger.warning("matchSegmentToWidgetForAudio no match for " + seg);
            }
          }
        }
      }
    }
  }

  private HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
    return addDomHandler(handler, MouseOverEvent.getType());
  }

  /**
   * Row 1: FL - ENGLISH
   * Row 2: AltFL
   * Row 3: Transliteration
   * Row 4: Meaning
   * Row 5: context sentence fl - eng
   *
   * @return
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#getQuestionContent
   */
  private Widget getItemContent(final T e) {
    Panel card = new DivWidget();
    card.getElement().setId("CommentNPFExercise_QuestionContent");
    card.setWidth("100%");

    boolean meaningValid = isMeaningValid(e);
    boolean isEnglish = controller.getLanguage().equalsIgnoreCase("english");
    boolean useMeaningInsteadOfEnglish = isEnglish && meaningValid;
    String english = useMeaningInsteadOfEnglish ? e.getMeaning() : e.getEnglish();

    DivWidget rowWidget = getRowWidget();
    rowWidget.getElement().setId("firstRow");

    SimpleRecordAudioPanel<T> recordPanel = makeFirstRow(e, rowWidget);
    card.add(rowWidget);

    if (isValid(english)) {
      DivWidget lr = getHorizDiv();
      lr.addStyleName("floatLeft");
      lr.setWidth("50%");

      lr.add(getEnglishWidget(e, english));
      lr.add(getItemWidget(e));
      lr.add(getDropdown());

      rowWidget.add(lr);
    }

    rowWidget = getRowWidget();
    card.add(rowWidget);

    rowWidget.getElement().setId("scoringRow");
    rowWidget.add(recordPanel);

    rowWidget = getRowWidget();
    card.add(rowWidget);
    rowWidget.getElement().setId("contextRow");

    addContext(e, card, rowWidget);

    return card;
  }

  @NotNull
  private String getMailTo() {
    String s1 = trimURL(Window.Location.getHref());

    String s = s1 +
        "#" +
        SelectionState.SECTION_SEPARATOR + "search=" + exercise.getID() +
        SelectionState.SECTION_SEPARATOR + "project=" + controller.getProjectStartupInfo().getProjectid();

    String encode = URL.encode(s);
    return "mailto:" +
        "?" +
        "Subject=Share netprof item " + exercise.getEnglish() +
        "&body=Link to " + exercise.getEnglish() + "/" + exercise.getForeignLanguage() + " : " +
        encode;
  }

  private String trimURL(String url) {
    return url.split("\\?")[0].split("#")[0];
  }

  private boolean showingComments = false;

  @NotNull
  private Dropdown getDropdown() {
    Dropdown dropdownContainer = new Dropdown("");
    dropdownContainer.setIcon(IconType.REORDER);
    dropdownContainer.setRightDropdown(true);
    dropdownContainer.getMenuWiget().getElement().getStyle().setTop(10, Style.Unit.PCT);

    dropdownContainer.addStyleName("leftThirtyMargin");
    dropdownContainer.getElement().getStyle().setListStyleType(Style.ListStyleType.NONE);
    dropdownContainer.getTriggerWidget().setCaret(false);


    new UserListSupport(controller).addListOptions(dropdownContainer, exercise.getID());

    NavLink share = new NavLink(EMAIL);
    dropdownContainer.add(share);
    share.setHref(getMailTo());

    dropdownContainer.add(getShowComments());


    return dropdownContainer;
  }

  @NotNull
  private NavLink getShowComments() {
    NavLink widget = new NavLink("Show Comments");
    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        for (CommentBox box : comments) {
          if (showingComments) {
            box.hideButtons();
          } else {
            box.showButtons();
          }
        }
        showingComments = !showingComments;
        if (showingComments) {
          widget.setText("Hide Comments");
        } else {
          widget.setText("Show Comments");
        }
      }
    });
    return widget;
  }


  @NotNull
  private SimpleRecordAudioPanel<T> makeFirstRow(T e, DivWidget rowWidget) {
    SimpleRecordAudioPanel<T> recordPanel = getRecordPanel(e);

    DivWidget flContainer = getHorizDiv();
    flContainer.getElement().setId("flWidget");

    DivWidget recordButtonContainer = new DivWidget();
    recordButtonContainer.add(recordPanel.getPostAudioRecordButton());
    flContainer.add(recordButtonContainer);

    if (hasAudio(e)) {
      flContainer.add(playAudio = getPlayAudioPanel());
      //playAudio.setListener(new SegmentHighlightAudioControl(idToTypeToSegmentToWidget.get()));
    }

    flclickables = new ArrayList<>();
    Widget flEntry =
        getEntry(e, QCNPFExercise.FOREIGN_LANGUAGE, e.getForeignLanguage(), true, false, false, showInitially, flclickables);
    flEntry.addStyleName("floatLeft");
    //logger.info("makeFirstRow Set with on " + flEntry.getElement().getId());

    flEntry.setWidth("100%");

    DivWidget fieldContainer = new DivWidget();
    fieldContainer.getElement().setId("fieldContainer");
    fieldContainer.add(flEntry);

    addField(fieldContainer, addAltFL(e), "altflrow");
    addField(fieldContainer, addTransliteration(e), "transliterationrow");

    boolean meaningValid = isMeaningValid(e);
    boolean isEnglish = controller.getLanguage().equalsIgnoreCase("english");
    boolean useMeaningInsteadOfEnglish = isEnglish && meaningValid;

    if (!useMeaningInsteadOfEnglish && meaningValid) {
      Widget meaningWidget = getEntry(e, QCNPFExercise.MEANING, e.getMeaning(), false, false, true, showInitially, new ArrayList<>());
      addField(fieldContainer, meaningWidget, "meaningRow");
    }


    flContainer.add(fieldContainer);
    flContainer.setWidth("50%");


    rowWidget.add(flContainer);
    return recordPanel;
  }

  private boolean hasAudio(T e) {
    return e.getAudioAttributePrefGender(controller.getUserManager().isMale(), true) != null;
  }

  private void addContext(T e, Panel card, DivWidget rowWidget) {
    int c = 0;
    String foreignLanguage = e.getForeignLanguage();
    String altFL = e.getAltFL();
    for (CommonExercise contextEx : e.getDirectlyRelated()) {
      //logger.info("Add context " + contextEx.getID());
      addContextFields(rowWidget, foreignLanguage, altFL, contextEx);

      c++;

      if (c < e.getDirectlyRelated().size()) {
        rowWidget = getRowWidget();
        card.add(rowWidget);
        rowWidget.getElement().setId("contextRow_again");
      }
    }
  }

  private void addContextFields(DivWidget rowWidget, String foreignLanguage,
                                String altFL, CommonExercise contextEx) {
    Panel context = getContext(contextEx, foreignLanguage, altFL);
    if (context != null) {
      rowWidget.add(context);
      context.setWidth("100%");
    }

    String contextTranslation = contextEx.getEnglish();

    boolean same = contextEx.getForeignLanguage().equals(contextTranslation);
    if (!same) {
      if (context != null) {
        context.setWidth("50%");
      }

      Widget contextTransWidget = addContextTranslation(contextEx, contextTranslation);

      if (contextTransWidget != null) {
        contextTransWidget.addStyleName("rightsidecolor");
        contextTransWidget.setWidth("50%");
        // contextTransWidget.getElement().getStyle().setFontWeight(Style.FontWeight.LIGHTER);
        rowWidget.add(contextTransWidget);
      }
    }
  }

  private Widget getAltContext(String flToHighlight, String altFL) {
    Panel contentWidget = clickableWords.getClickableWordsHighlight(altFL, flToHighlight,
        true, false, false);

    CommentBox commentBox = getCommentBox(true);
    return commentBox
        .getEntry(QCNPFExercise.ALTCONTEXT, contentWidget,
            exercise.getAnnotation(QCNPFExercise.ALTCONTEXT), showInitially);
  }

  /**
   * @return
   * @see #makeFirstRow
   */
  @NotNull
  private ChoicePlayAudioPanel getPlayAudioPanel() {
    return new ChoicePlayAudioPanel(controller.getSoundManager(), exercise, controller, false, this);
  }

  /**
   * @param e
   * @return
   */
  @NotNull
  private DivWidget getItemWidget(T e) {
    InlineLabel itemHeader = commonExerciseUnitChapterItemHelper.getLabel(e);
    showPopup(itemHeader, commonExerciseUnitChapterItemHelper.getUnitLessonForExercise2(e));
    itemHeader.addStyleName("floatRight");
    DivWidget itemContainer = new DivWidget();
    itemContainer.add(itemHeader);
    itemContainer.addStyleName("floatRight");
    return itemContainer;
  }

  @NotNull
  private Widget getEnglishWidget(T e, String english) {
    Widget englishWidget = getEntry(e, QCNPFExercise.ENGLISH, english, false, false, false, showInitially, new ArrayList<>());
    englishWidget.addStyleName("rightsidecolor");
    englishWidget.getElement().setId("englishWidget");
    englishWidget.addStyleName("floatLeft");
    englishWidget.setWidth("90%");
    return englishWidget;
  }

  private void addField(Panel grid, Widget widget, String altflrow) {
    if (widget != null) {
      widget.addStyleName("topFiveMargin");
      grid.add(widget);
    }
  }

  private void showPopup(InlineLabel label, String toShow) {
    label.addMouseOverHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        new BasicDialog().showPopover(
            label,
            null,
            toShow,
            Placement.LEFT);
      }
    });
  }

  /**
   * @param e
   * @return
   * @see #getItemContent
   */
  @NotNull
  private SimpleRecordAudioPanel<T> getRecordPanel(T e) {
    return new SimpleRecordAudioPanel<T>(new BusyPanel() {
      @Override
      public boolean isBusy() {
        return false;
      }

      @Override
      public void setBusy(boolean v) {
      }
    }, controller, e, correctAndScores, listContainer);
  }

  @NotNull
  private DivWidget getHorizDiv() {
    DivWidget flContainer = new DivWidget();
    flContainer.addStyleName("inlineFlex");
    return flContainer;
  }

  @NotNull
  private DivWidget getRowWidget() {
    DivWidget rowWidget = getHorizDiv();
    rowWidget.addStyleName("bottomFiveMargin");
    rowWidget.addStyleName("floatLeft");
    rowWidget.setWidth("100%");
    return rowWidget;
  }


  private Widget addAltFL(T e) {
    String translitSentence = e.getAltFL().trim();
    if (!translitSentence.isEmpty() && !translitSentence.equals("N/A") && !e.getForeignLanguage().trim().equals(translitSentence)) {
      altflClickables = new ArrayList<>();
      Widget entry = getEntry(e, QCNPFExercise.ALTFL, translitSentence, true, true, false, showInitially, altflClickables);
      entry.getElement().getStyle().setMarginTop(10, Style.Unit.PX);
      return entry;
    } else return null;
  }

  private Widget addTransliteration(T e) {
    String translitSentence = e.getTransliteration();
    if (!translitSentence.isEmpty() && !translitSentence.equals("N/A")) {
      return getEntry(e, QCNPFExercise.TRANSLITERATION, translitSentence, false, true, false, showInitially, new ArrayList<>());
    }
    return null;
  }

  private boolean isMeaningValid(T e) {
    String meaning = e.getMeaning();
    return isValid(meaning);
  }

  private boolean isValid(String meaning) {
    return meaning != null && !meaning.trim().isEmpty() && !meaning.equals("N/A");
  }

  private final List<CommentBox> comments = new ArrayList<>();
  ChoicePlayAudioPanel contextPlay;

  /**
   * @param contextExercise
   * @return
   * @seex #addContextButton
   */
  private Panel getContext(CommonExercise contextExercise, String itemText, String altFL) {
    String context = contextExercise.getForeignLanguage();

    if (!context.isEmpty()) {
      Panel hp = new DivWidget();
      hp.addStyleName("inlineFlex");
      hp.getElement().setId("contentContainer");
      DivWidget spacer = new DivWidget();
      spacer.getElement().setId("spacer");
      spacer.getElement().getStyle().setProperty("minWidth", CONTEXT_INDENT + "px");
      hp.add(spacer);

      AudioAttribute audioAttrPrefGender = contextExercise.getAudioAttrPrefGender(controller.getUserManager().isMale());

      contextPlay
          = new ChoicePlayAudioPanel(controller.getSoundManager(), contextExercise, controller, true, this);
      contextPlay.setEnabled(audioAttrPrefGender != null);
      hp.add(contextPlay);

      Panel contentWidget = clickableWords.getClickableWordsHighlight(context, itemText,
          true, false, false);

      CommentBox commentBox = getCommentBox(true);
      Widget commentRow =
          commentBox
              .getEntry(QCNPFExercise.CONTEXT, contentWidget,
                  contextExercise.getAnnotation(QCNPFExercise.CONTEXT), showInitially);

      DivWidget col = new DivWidget();
      col.setWidth("100%");
      hp.add(col);

      col.add(commentRow);

      String altFL1 = contextExercise.getAltFL();
      if (!altFL1.isEmpty() && !context.equals(altFL1)) {
        col.add(getAltContext(altFL, altFL1));
      }

      return hp;
    } else {
      return null;
    }
  }

  private Widget addContextTranslation(AnnotationExercise e, String contextTranslation) {
    if (!contextTranslation.isEmpty()) {
      return getEntry(e, QCNPFExercise.CONTEXT_TRANSLATION, contextTranslation,
          false, false, false, showInitially, new ArrayList<>());
    } else return null;
  }

  /**
   * @param e
   * @param field
   * @param value
   * @param showInitially
   * @param clickables
   * @return
   * @paramx label
   * @see #getItemContent
   */
  private Widget getEntry(AnnotationExercise e, final String field, String value, boolean isFL,
                          boolean isTranslit,
                          boolean isMeaning, boolean showInitially, List<IHighlightSegment> clickables) {
    return getEntry(field, value, e.getAnnotation(field), isFL, isTranslit, isMeaning, showInitially, clickables);
  }

  /**
   * @param field
   * @param value
   * @param annotation
   * @param showInitially
   * @param clickables
   * @return
   * @paramx label
   * @seex #makeFastAndSlowAudio(String)
   * @see #getEntry
   */
  private Widget getEntry(final String field,
                          String value, ExerciseAnnotation annotation, boolean isFL, boolean isTranslit,
                          boolean isMeaning, boolean showInitially, List<IHighlightSegment> clickables) {
    Panel contentWidget = clickableWords.getClickableWords(value, isFL, isTranslit, isMeaning, clickables);
    if (!isFL) contentWidget.addStyleName("topFiveMargin");
    return getCommentBox(true).getEntry(field, contentWidget, annotation, showInitially);
  }

  /**
   * @return
   * @seex x#getEntry(String, String, String, ExerciseAnnotation)
   * @seex #makeFastAndSlowAudio(String)
   */
  private CommentBox getCommentBox(boolean tooltipOnRight) {
    if (logger == null) {
      logger = Logger.getLogger("CommentNPFExercise");
    }
    T exercise = this.exercise;
    CommentBox commentBox =
        new CommentBox(this.exercise.getID(), controller, annotationHelper, exercise.getMutableAnnotation(), tooltipOnRight);
    comments.add(commentBox);
    return commentBox;
  }

}
