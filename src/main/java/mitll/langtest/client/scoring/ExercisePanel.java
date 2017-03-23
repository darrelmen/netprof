package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.custom.exercise.CommentBox;
import mitll.langtest.client.custom.exercise.ContextAudioChoices;
import mitll.langtest.client.custom.exercise.ContextSupport;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.ASRScorePanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.services.ListService;
import mitll.langtest.client.services.ListServiceAsync;
import mitll.langtest.shared.exercise.*;

import java.util.logging.Logger;

/**
 * Created by go22670 on 3/23/17.
 */
public class ExercisePanel<T extends CommonExercise> extends DivWidget {
  private Logger logger = Logger.getLogger("CommentNPFExercise");

  public static final String CONTEXT = "Context";

  public static final String DEFAULT_SPEAKER = "Default Speaker";

  /**
   * TODO make better relationship with ASRRecordAudioPanel
   */
  Image recordImage1;
  Image recordImage2;

  protected final T exercise;
  protected final ExerciseController controller;

  protected final ListServiceAsync listService = GWT.create(ListService.class);

  protected ExerciseOptions options;
  AnnotationHelper annotationHelper;
  ClickableWords<T> clickableWords;
  boolean showInitially = false;

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
  public ExercisePanel(final T commonExercise,
                       final ExerciseController controller,
                       final ListInterface<CommonShell> listContainer,
                       ExerciseOptions options
  ) {
    this.options = options;
    this.exercise = commonExercise;
    this.controller = controller;

    getElement().setId("ExercisePanel");
    addStyleName("cardBorderShadow");
    addStyleName("bottomFiveMargin");

    annotationHelper = new AnnotationHelper(controller, commonExercise.getID());

    clickableWords = new ClickableWords<T>(listContainer, commonExercise, controller.getLanguage());

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
  protected Widget getItemContent(final T e) {
    //Panel column = new VerticalPanel();
    Panel card = new DivWidget();
    card.getElement().setId("CommentNPFExercise_QuestionContent");

    card.setWidth("100%");
    int numRows = 2;
    int row = 0;

    boolean meaningValid = isMeaningValid(e);
    boolean isAltValid = isValid(e.getAltFL());
    boolean isTranslitValid = isValid(e.getTransliteration());

    boolean isEnglish = controller.getLanguage().equalsIgnoreCase("english");
    boolean useMeaningInsteadOfEnglish = isEnglish && meaningValid;
    String english = useMeaningInsteadOfEnglish ? e.getMeaning() : e.getEnglish();

    if (meaningValid) numRows++;
    if (isAltValid) numRows++;
    if (isTranslitValid) numRows++;
    Grid grid = new Grid(numRows, 2);
    grid.getColumnFormatter().setWidth(0, "50%");
    grid.getColumnFormatter().setWidth(1, "50%");

    for (int i = 0; i < numRows; i++) {
      grid.getRowFormatter().setStyleName(i, "bottomFiveMargin");
    }
    card.add(grid);

    //DivWidget row = new DivWidget();
    //row.getElement().setId("QuestionContent_item");

    Widget entry = getEntry(e, QCNPFExercise.FOREIGN_LANGUAGE, e.getForeignLanguage(), true, false, false, showInitially);
    //entry.addStyleName("floatLeft");
    //row.add(entry);
    grid.setWidget(row, 0, entry);

    if (isValid(english)) {
      Widget entry1 = getEntry(e, QCNPFExercise.ENGLISH, english, false, false, false, showInitially);
      entry1.addStyleName("rightsidecolor");
      grid.setWidget(row++, 1, entry1);
    }

    Widget widget = addAltFL(e);
    if (widget != null) grid.setWidget(row++, 0, widget);
    Widget widget1 = addTransliteration(e);
    if (widget1 != null) grid.setWidget(row++, 0, widget1);

    if (!useMeaningInsteadOfEnglish && meaningValid) {
      Widget entry1 = getEntry(e, QCNPFExercise.MEANING, e.getMeaning(), false, false, true, showInitially);
      if (entry1 != null) grid.setWidget(row++, 0, entry1);
    }

   // DivWidget container = new DivWidget();
    String foreignLanguage = e.getForeignLanguage();
    String altFL = e.getAltFL();

    logger.info("for " + e.getID() + " found " + e.getDirectlyRelated().size() + " context sentence ");

    for (CommonExercise contextEx : e.getDirectlyRelated()) {
      logger.info("Add context " + contextEx.getID());
      Panel context = getContext(contextEx, foreignLanguage, altFL);
      if (context != null) {
        context.getElement().getStyle().setPadding(10, Style.Unit.PX);
        grid.setWidget(row, 0, context);
      }

      String contextTranslation = contextEx.getEnglish();

      boolean same = contextEx.getForeignLanguage().equals(contextTranslation);
      if (!same) {
        Widget widget2 = addContextTranslation(contextEx, contextTranslation);
        if (widget2 != null) {
          widget2.addStyleName("rightsidecolor");
          widget2.getElement().getStyle().setPadding(10, Style.Unit.PX);
          grid.setWidget(row, 1, widget2);
        }
      }
      //container.add(context);
    }

    for (int i = 0; i < numRows; i++) {
      grid.getRowFormatter().setStyleName(i, "bottomFiveMargin");
    }
    //card.add(container);

    return card;
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

  private ContextSupport<T> contextSupport = new ContextSupport<T>();

  /**
   * @param exercise
   * @param altText
   * @return
   * @seex #addContextButton
   */
  private <U extends CommonAudioExercise> Panel getContext(U exercise, String itemText, String altText) {
    String context = exercise.getForeignLanguage();

    if (!context.isEmpty()) {
      Panel hp = new DivWidget();
      new ContextAudioChoices(controller, exercise, exercise.getID()).addGenderChoices(hp);
      Panel contentWidget = clickableWords.getClickableWordsHighlight(context, itemText,
          true, false, false);

      Widget commentRow =
          getCommentBox(true).getEntry(QCNPFExercise.CONTEXT, contentWidget,
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
    }
    else return null;
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
