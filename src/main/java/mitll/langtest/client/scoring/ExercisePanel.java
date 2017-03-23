package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
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
   * @return
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#getQuestionContent
   */

  protected Widget getItemContent(final T e) {
    //Panel column = new VerticalPanel();
    Panel column = new DivWidget();
    column.getElement().setId("CommentNPFExercise_QuestionContent");
    column.setWidth("100%");

    DivWidget row = new DivWidget();
    row.getElement().setId("QuestionContent_item");

    Widget entry = getEntry(e, QCNPFExercise.FOREIGN_LANGUAGE, e.getForeignLanguage(), true, false, false, showInitially);
    entry.addStyleName("floatLeft");
    row.add(entry);

//    addContextButton(e, row);
    column.add(row);

    addAltFL(e, column);
    addTransliteration(e, column);

    boolean isEnglish = controller.getLanguage().equalsIgnoreCase("english");
    boolean meaningValid = isMeaningValid(e);
    boolean useMeaningInsteadOfEnglish = isEnglish && meaningValid;
    String english = useMeaningInsteadOfEnglish ? e.getMeaning() : e.getEnglish();

/*
    logger.info("getItemContent meaningValid " + meaningValid + " is english " + isEnglish + " use it " + useMeaningInsteadOfEnglish + "" +
        " english " + english);
        */

    if (!english.isEmpty() && !english.equals("N/A")) {
      column.add(getEntry(e, QCNPFExercise.ENGLISH, english, false, false, false, showInitially));
    }

    if (!useMeaningInsteadOfEnglish && meaningValid) {
      column.add(getEntry(e, QCNPFExercise.MEANING, e.getMeaning(), false, false, true, showInitially));
    }

    DivWidget container = new DivWidget();
    String foreignLanguage = e.getForeignLanguage();
    String altFL = e.getAltFL();

    logger.info("for " + e.getID() + " found " + e.getDirectlyRelated().size() + " context sentence ");

    for (CommonExercise contextEx : e.getDirectlyRelated()) {
      logger.info("Add context " + contextEx.getID());
      container.add(getContext(contextEx, foreignLanguage, altFL));
    }

    column.add(container);

    return column;
  }

  private void addAltFL(T e, Panel column) {
    String translitSentence = e.getAltFL().trim();
    if (!translitSentence.isEmpty() && !translitSentence.equals("N/A")) {
      column.add(getEntry(e, QCNPFExercise.ALTFL, translitSentence, true, true, false, showInitially));
    }
  }

  private void addTransliteration(T e, Panel column) {
    String translitSentence = e.getTransliteration();
    if (!translitSentence.isEmpty() && !translitSentence.equals("N/A")) {
      column.add(getEntry(e, QCNPFExercise.TRANSLITERATION, translitSentence, false, true, false, showInitially));
    }
  }

  private boolean isMeaningValid(T e) {
    return e.getMeaning() != null && !e.getMeaning().trim().isEmpty();
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
    String contextTranslation = exercise.getEnglish();

    boolean same = context.equals(contextTranslation);

    if (!context.isEmpty()) {
      Panel hp = new HorizontalPanel();
      new ContextAudioChoices(controller, exercise, exercise.getID()).addGenderChoices(hp);

//      String highlightedVocabItemInContext = highlightVocabItemInContext(exercise, itemText);

//      Panel contentWidget =
//          clickableWords.getClickableWords(highlightedVocabItemInContext, true, false, false);
      //String field = QCNPFExercise.CONTEXT;
      //ExerciseAnnotation annotation = exercise.getAnnotation(field);

//      logger.info("getContext context " + exercise.getID() + " : " + annotation);

/*
      Widget commentRow = getCommentBox(false)
          .getNoPopup(
              field,
              contentWidget,
              annotation,
              exercise);
*/


      Panel contentWidget = clickableWords.getClickableWordsHighlight(context, itemText,
          true, false, false);
      Widget commentRow =
          getCommentBox(true).getEntry(QCNPFExercise.CONTEXT, contentWidget,
              exercise.getAnnotation(QCNPFExercise.CONTEXT), showInitially);

      Panel vp = new VerticalPanel();
      vp.add(commentRow);
      addContextTranslation(exercise, contextTranslation, same, vp);
      hp.add(vp);
      return hp;
    } else {
      return null;
    }
  }

  /**
   * Add underlines of item tokens in context sentence.
   * <p>
   * TODO : don't do this - make spans with different colors
   * <p>
   * Worries about lower case/upper case mismatch.
   *
   * @param e
   * @param context
   * @return
   * @see #getContext
   */
  private String highlightVocabItemInContext(CommonShell e, String context) {
    return new ContextSupport<>().getHighlightedSpan(context, e.getForeignLanguage());
  }

  private void addContextTranslation(AnnotationExercise e, String contextTranslation, boolean same, Panel vp) {
    if (!contextTranslation.isEmpty() && !same) {
/*      Panel contentWidget =
          clickableWords.getClickableWords(contextTranslation, true, false, false);*/
  /*    Widget translationEntry = getCommentBox(false).getNoPopup(QCNPFExercise.CONTEXT_TRANSLATION, contentWidget,
          e.getAnnotation(QCNPFExercise.CONTEXT_TRANSLATION),
          exercise);*/

      Widget translationEntry = getEntry(e, QCNPFExercise.CONTEXT_TRANSLATION, contextTranslation, false, false, false, showInitially);

      vp.add(translationEntry);
    }
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
