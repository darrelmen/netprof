package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.Popover;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.exercise.CommentBox;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.ASRScorePanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.services.ListService;
import mitll.langtest.client.services.ListServiceAsync;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 3/23/17.
 */
public class TwoColumnExercisePanel<T extends CommonExercise> extends DivWidget {
  public static final int CONTEXT_PADDING = 10;
  private final List<CorrectAndScore> correctAndScores;
  private Logger logger = Logger.getLogger("TwoColumnExercisePanel");

  public static final String CONTEXT = "Context";

  public static final String DEFAULT_SPEAKER = "Default Speaker";

  /**
   * TODO make better relationship with ASRRecordAudioPanel
   */
  Image recordImage1;
  Image recordImage2;

  private final T exercise;
  private final ExerciseController controller;

  protected final ListServiceAsync listService = GWT.create(ListService.class);
  private final AnnotationHelper annotationHelper;
  private final ClickableWords<T> clickableWords;
  private final boolean showInitially = false;
  private UnitChapterItemHelper<CommonExercise> commonExerciseUnitChapterItemHelper;

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
   * @see mitll.langtest.client.custom.Navigation#getLearnHelper(ExerciseController)
   */
  public TwoColumnExercisePanel(final T commonExercise,
                                final ExerciseController controller,
                                final ListInterface<CommonShell> listContainer,
                                List<CorrectAndScore> correctAndScores,
                                ExerciseOptions options
  ) {
    this.exercise = commonExercise;
    this.controller = controller;

    getElement().setId("TwoColumnExercisePanel");
    addStyleName("cardBorderShadow");
    addStyleName("bottomFiveMargin");
    addStyleName("floatLeftAndClear");
    setWidth("100%");

    annotationHelper = new AnnotationHelper(controller, commonExercise.getID());

    clickableWords = new ClickableWords<T>(listContainer, commonExercise, controller.getLanguage());

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

    SimpleRecordAudioPanel<T> recordPanel = getRecordPanel(e);

    DivWidget flContainer = getHorizDiv();
    flContainer.getElement().setId("flWidget");

    flContainer.add(recordPanel.getPostAudioRecordButton());
    AudioAttribute audioAttribute = e.getAudioAttributePrefGender(controller.getUserManager().isMale(), true);

    if (audioAttribute != null) {
//      logger.info("found audio for gender male = " + male + "  audio is male " + audioAttribute.isMale() + " by " + audioAttribute.getUser());
/*      if (audioAttribute.isMale() != male) {
        for (AudioAttribute attr : e.getAudioAttributes()) {
          logger.info("no match - for ex " + e.getID() +
              "  had " + attr.getID() + " " + attr.isMale() + " by " + attr.getUser());
        }
      }*/
      flContainer.add(getPlayAudioPanel(e, audioAttribute));
    }

    Widget flEntry = getEntry(e, QCNPFExercise.FOREIGN_LANGUAGE, e.getForeignLanguage(), true, false, false, showInitially);
    flEntry.addStyleName("floatLeft");
    flContainer.add(flEntry);

    DivWidget rowWidget = getRowWidget();
    rowWidget.add(flContainer);
    rowWidget.getElement().setId("firstRow");

    flContainer.setWidth("50%");
    card.add(rowWidget);

    if (isValid(english)) {
      DivWidget lr = getHorizDiv();
      lr.addStyleName("floatLeft");
      lr.setWidth("50%");

      lr.add(getEnglishWidget(e, english));
      lr.add(getItemWidget(e));

      rowWidget.add(lr);

      rowWidget = getRowWidget();
      rowWidget.getElement().setId("secondRow");

      card.add(rowWidget);
    }

    addField(card, addAltFL(e), "altflrow");
    addField(card, addTransliteration(e), "transliterationrow");

    if (!useMeaningInsteadOfEnglish && meaningValid) {
      Widget meaningWidget = getEntry(e, QCNPFExercise.MEANING, e.getMeaning(), false, false, true, showInitially);
      addField(card, meaningWidget, "meaningRow");
    }

    //  logger.info("for " + e.getID() + " found " + e.getDirectlyRelated().size() + " context sentence ");

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

  private void addContext(T e, Panel card, DivWidget rowWidget) {
    int c = 0;
    String foreignLanguage = e.getForeignLanguage();
    for (CommonExercise contextEx : e.getDirectlyRelated()) {
      //logger.info("Add context " + contextEx.getID());
      Panel context = getContext(contextEx, foreignLanguage);
      if (context != null) {
        context.getElement().getStyle().setPadding(CONTEXT_PADDING, Style.Unit.PX);
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

          contextTransWidget.getElement().getStyle().setPadding(CONTEXT_PADDING, Style.Unit.PX);
          rowWidget.add(contextTransWidget);
        }
      }

      c++;

      if (c < e.getDirectlyRelated().size()) {
        rowWidget = getRowWidget();
        card.add(rowWidget);
        rowWidget.getElement().setId("contextRow_again");
      }
    }
  }

  @NotNull
  private DivWidget getPlayAudioPanel(T e, AudioAttribute audioAttribute) {
    UserManager userManager = controller.getUserManager();
    boolean male = userManager.isMale();

    PlayAudioPanel w = new PlayAudioPanel(controller.getSoundManager(), audioAttribute.getAudioRef(), false);
    DivWidget pap = new DivWidget();
    pap.getElement().setId("playAudioContainer");
    pap.addStyleName("floatLeft");
    pap.addStyleName("inlineFlex");
    pap.add(w);

    addSlow(e, male, pap);
    return pap;
  }

  private void addSlow(T e, boolean male, DivWidget pap) {
    AudioAttribute slow = e.getAudioAttributePrefGender(male, false);

    if (slow != null) {
      //      logger.info("found slow audio for gender male = " + male + "  audio is male " + slow.isMale() + " by " + slow.getUser());
      PlayAudioPanel w1 = new PlayAudioPanel(controller.getSoundManager(), slow.getAudioRef(), true);
      w1.addStyleName("floatLeft");
      pap.add(w1);
    }
  }

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
      DivWidget rowWidget;
      rowWidget = getRowWidget();
      rowWidget.getElement().setId(altflrow);
      rowWidget.addStyleName("leftMarginForFields");
      rowWidget.add(widget);
      grid.add(rowWidget);
    }
  }

  private void showPopup(InlineLabel label, String toShow) {
    label.addMouseOverHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        Popover widgets = new BasicDialog().showPopover(label,
            null,
            toShow, Placement.LEFT);
        // widgets.setHideDelay(2000);
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
    }, controller, e, correctAndScores);
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
    if (!translitSentence.isEmpty() && !translitSentence.equals("N/A")) {
      return getEntry(e, QCNPFExercise.ALTFL, translitSentence, true, true, false, showInitially);
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

  /**
   * @param exercise
   * @return
   * @seex #addContextButton
   */
  private <U extends CommonAudioExercise> Panel getContext(U exercise, String itemText) {
    String context = exercise.getForeignLanguage();

    if (!context.isEmpty()) {
      Panel hp = new DivWidget();
      hp.addStyleName("inlineFlex");
      AudioAttribute audioAttrPrefGender = exercise.getAudioAttrPrefGender(controller.getUserManager().isMale());

      if (audioAttrPrefGender != null) {
        PlayAudioPanel w = new PlayAudioPanel(controller.getSoundManager(), audioAttrPrefGender.getAudioRef(), false);
        hp.add(w);
      }

      Panel contentWidget = clickableWords.getClickableWordsHighlight(context, itemText,
          true, false, false);

      Widget commentRow =
          getCommentBox(true)
              .getEntry(QCNPFExercise.CONTEXT, contentWidget,
                  exercise.getAnnotation(QCNPFExercise.CONTEXT), showInitially);

      hp.add(commentRow);
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
   * @return
   * @seex x#getEntry(String, String, String, ExerciseAnnotation)
   * @seex #makeFastAndSlowAudio(String)
   */
  private CommentBox getCommentBox(boolean tooltipOnRight) {
    if (logger == null) {
      logger = Logger.getLogger("CommentNPFExercise");
    }
    T exercise = this.exercise;
    return new CommentBox(this.exercise.getID(), controller, annotationHelper, exercise.getMutableAnnotation(), tooltipOnRight);
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
    return getCommentBox(true).getEntry(field, contentWidget, annotation, showInitially);
  }
}
