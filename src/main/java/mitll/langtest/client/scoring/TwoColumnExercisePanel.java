package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.DropdownContainer;
import com.github.gwtbootstrap.client.ui.DropdownSubmenu;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.DropdownBase;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.github.gwtbootstrap.client.ui.event.ShowEvent;
import com.github.gwtbootstrap.client.ui.event.ShowHandler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.exercise.CommentBox;
import mitll.langtest.client.custom.exercise.PopupContainerFactory;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.ASRScorePanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.services.ListServiceAsync;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import org.jetbrains.annotations.NotNull;
import slick.ast.Drop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 3/23/17.
 */
public class TwoColumnExercisePanel<T extends CommonExercise> extends DivWidget {
  private Logger logger = Logger.getLogger("TwoColumnExercisePanel");

  private static final String EMAIL = "Email Item";
  public static final int CONTEXT_INDENT = 40;

  private final List<CorrectAndScore> correctAndScores;
  private final T exercise;
  private final ExerciseController controller;

  private final AnnotationHelper annotationHelper;
  private final ClickableWords<T> clickableWords;
  private final boolean showInitially = false;
  private final UnitChapterItemHelper<CommonExercise> commonExerciseUnitChapterItemHelper;
  private final ListInterface<CommonShell> listContainer;
/*
  public static final String MAKE_A_NEW_LIST = "Make a new list";

  private static final String ADD_ITEM = "Add Item to List";
  private static final String ITEM_ALREADY_ADDED = "Item already added to your list(s)";
  private static final String ADD_TO_LIST = "Add to List";
  *//**
   * @seex #getNewListButton
   *//*
  private static final String NEW_LIST = "New List";
  private static final String ITEM_ADDED = "Item Added!";
  private static final String ADDING_TO_LIST = "Adding to list ";*/

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
   * @see mitll.langtest.client.banner.NewLearnHelper#getFactory
   */
  public TwoColumnExercisePanel(final T commonExercise,
                                final ExerciseController controller,
                                final ListInterface<CommonShell> listContainer,
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
      Dropdown w = getDropdown();
      lr.add(w);

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
    // logger.info("base is "+s1);
    String s = s1 +
        "#" +
        SelectionState.SECTION_SEPARATOR + "item=" + exercise.getID() +
        SelectionState.SECTION_SEPARATOR + "project=" + controller.getProjectStartupInfo().getProjectid();

    String encode = URL.encode(s);
    return "mailto:" +
        //NETPROF_HELP_LL_MIT_EDU +
        "?" +
        //   "cc=" + LTEA_DLIFLC_EDU + "&" +
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
    //DropdownContainer dropdownContainer = new DropdownContainer("");
    dropdownContainer.setIcon(IconType.REORDER);
    dropdownContainer.setRightDropdown(true);
    dropdownContainer.getMenuWiget().getElement().getStyle().setTop(10, Style.Unit.PCT);

    new UserListSupport(controller).addListOptions(dropdownContainer,exercise.getID());

/*    DropdownSubmenu addToList = new DropdownSubmenu("Add to List");
    addToList.setRightDropdown(true);
  //  addToList.setStyleDependentName("pull-left", true);
    DropdownSubmenu removeFromList = new DropdownSubmenu("Remove from List");
    removeFromList.setRightDropdown(true);
    dropdownContainer.addShowHandler(new ShowHandler() {
      @Override
      public void onShow(ShowEvent showEvent) {
        populateListChoices(exercise.getID(), addToList, removeFromList, dropdownContainer);
      }
    });
    //  NavLink addToList = new NavLink("Add to List");
    dropdownContainer.add(addToList);
    dropdownContainer.add(removeFromList);


    NavLink widget = new NavLink("New List");
    dropdownContainer.add(widget);*/
//    widget.addClickHandler()
    NavLink share = new NavLink(EMAIL);
    dropdownContainer.add(share);
    share.setHref(getMailTo());
    dropdownContainer.add(getShowComments());
    dropdownContainer.addStyleName("leftThirtyMargin");
    dropdownContainer.getElement().getStyle().setListStyleType(Style.ListStyleType.NONE);
    dropdownContainer.getTriggerWidget().setCaret(false);
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
    PostAudioRecordButton postAudioRecordButton = recordPanel.getPostAudioRecordButton();
    recordButtonContainer.add(postAudioRecordButton);
    //postAudioRecordButton.setVisible(controller.isRecordingEnabled());
    flContainer.add(recordButtonContainer);

    if (hasAudio(e)) {
      flContainer.add(getPlayAudioPanel());
    }

    Widget flEntry =
        getEntry(e, QCNPFExercise.FOREIGN_LANGUAGE, e.getForeignLanguage(), true, false, false, showInitially);
    flEntry.addStyleName("floatLeft");

    DivWidget fieldContainer = new DivWidget();
    fieldContainer.getElement().setId("fieldContainer");
    fieldContainer.add(flEntry);

    addField(fieldContainer, addAltFL(e), "altflrow");
    addField(fieldContainer, addTransliteration(e), "transliterationrow");

    boolean meaningValid = isMeaningValid(e);
    boolean isEnglish = controller.getLanguage().equalsIgnoreCase("english");
    boolean useMeaningInsteadOfEnglish = isEnglish && meaningValid;

    if (!useMeaningInsteadOfEnglish && meaningValid) {
      Widget meaningWidget = getEntry(e, QCNPFExercise.MEANING, e.getMeaning(), false, false, true, showInitially);
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
    // DivWidget col = new DivWidget();
    // col.setWidth("100%");
    Panel context = getContext(contextEx, foreignLanguage, altFL);
    if (context != null) {
      //Style style = context.getElement().getStyle();
      //style.setFontWeight(Style.FontWeight.LIGHTER);
      rowWidget.add(context);
      //  col.add(context);
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
  private DivWidget getPlayAudioPanel() {
    return new ChoicePlayAudioPanel(controller.getSoundManager(), exercise, controller, false);
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
    Widget englishWidget = getEntry(e, QCNPFExercise.ENGLISH, english, false, false, false, showInitially);
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
      Widget entry = getEntry(e, QCNPFExercise.ALTFL, translitSentence, true, true, false, showInitially);
      entry.getElement().getStyle().setMarginTop(10, Style.Unit.PX);
      return entry;
    } else return null;
  }

  private Widget addTransliteration(T e) {
    String translitSentence = e.getTransliteration();
    if (!translitSentence.isEmpty() && !translitSentence.equals("N/A")) {
      return getEntry(e, QCNPFExercise.TRANSLITERATION, translitSentence, false, true, false, showInitially);
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

      spacer.getElement().getStyle().setProperty("minWidth", CONTEXT_INDENT + "px");
      hp.add(spacer);

      AudioAttribute audioAttrPrefGender = contextExercise.getAudioAttrPrefGender(controller.getUserManager().isMale());

      ChoicePlayAudioPanel child
          = new ChoicePlayAudioPanel(controller.getSoundManager(), contextExercise, controller, true);
      child.setEnabled(audioAttrPrefGender != null);
      hp.add(child);

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

      if (!context.equals(contextExercise.getAltFL())) {
        col.add(getAltContext(altFL, contextExercise.getAltFL()));
      }

      return hp;
    } else {
      return null;
    }
  }

  private Widget addContextTranslation(AnnotationExercise e, String contextTranslation) {
    if (!contextTranslation.isEmpty()) {
      return getEntry(e, QCNPFExercise.CONTEXT_TRANSLATION, contextTranslation, false, false, false, showInitially);
    } else return null;
  }

  /**
   * @param e
   * @param field
   * @param value
   * @param showInitially
   * @return
   * @paramx label
   * @see #getItemContent
   */
  private Widget getEntry(AnnotationExercise e, final String field, String value, boolean isFL, boolean isTranslit,
                          boolean isMeaning, boolean showInitially) {
    return getEntry(field, value, e.getAnnotation(field), isFL, isTranslit, isMeaning, showInitially);
  }

  /**
   * @param field
   * @param value
   * @param annotation
   * @param showInitially
   * @return
   * @paramx label
   * @seex #makeFastAndSlowAudio(String)
   * @see #getEntry
   */
  private Widget getEntry(final String field,
                          String value, ExerciseAnnotation annotation, boolean isFL, boolean isTranslit,
                          boolean isMeaning, boolean showInitially) {
    Panel contentWidget = clickableWords.getClickableWords(value, isFL, isTranslit, isMeaning);
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
