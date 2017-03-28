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

package mitll.langtest.client.custom.exercise;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ButtonGroup;
import com.github.gwtbootstrap.client.ui.ButtonToolbar;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.client.scoring.ExerciseOptions;
import mitll.langtest.client.scoring.FastAndSlowASRScoringAudioPanel;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.shared.exercise.ExerciseAnnotation;
import mitll.langtest.shared.ExerciseFormatter;
import mitll.langtest.shared.exercise.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/12/13
 * Time: 5:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class CommentNPFExercise<T extends CommonExercise> extends NPFExercise<T> {
  private Logger logger = Logger.getLogger("CommentNPFExercise");

  //  private static final String HIGHLIGHT_START = "<span style='background-color:#5bb75b;color:black'>"; //#5bb75b
//  private static final String HIGHLIGHT_END = "</span>";

  private static final String CONTEXT_SENTENCE = "Context Sentence";
  private static final String DEFAULT = "Default";

  private static final String NO_REFERENCE_AUDIO = "No reference audio";
  private static final String M = "M";
  private static final String F = "F";
  public static final String PUNCT_REGEX = "[\\?\\.,-\\/#!$%\\^&\\*;:{}=\\-_`~()]";
  public static final String SPACE_REGEX = " ";
  private static final String REF_AUDIO = "refAudio";

  private AudioAttribute defaultAudio, maleAudio, femaleAudio;
  private PlayAudioPanel contextPlay;

  /**
   * @param e
   * @param controller
   * @param listContainer
   * @param instance
   * @param allowRecording
   * @see mitll.langtest.client.custom.Navigation#Navigation
   * @see mitll.langtest.client.custom.content.NPFHelper#getFactory(PagingExerciseList, String, boolean)
   */
  public CommentNPFExercise(T e,
                            ExerciseController controller,
                            ListInterface<CommonShell> listContainer,
                            ExerciseOptions options
  ) {
    super(e, controller, listContainer,options);
  }

  /**
   * @return
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#getQuestionContent
   */
  @Override
  protected Widget getItemContent(final T e) {
    Panel column = new VerticalPanel();
    //   Panel column = new DivWidget();
    column.getElement().setId("CommentNPFExercise_QuestionContent");
    column.setWidth("100%");

    DivWidget row = new DivWidget();
    row.getElement().setId("QuestionContent_item");

    Widget entry = getEntry(e, QCNPFExercise.FOREIGN_LANGUAGE, ExerciseFormatter.FOREIGN_LANGUAGE_PROMPT, e.getForeignLanguage());
    entry.addStyleName("floatLeftAndClear");
    row.add(entry);

    addContextButton(e, row);

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
      String englishPrompt = useMeaningInsteadOfEnglish ? ExerciseFormatter.MEANING_PROMPT : ExerciseFormatter.ENGLISH_PROMPT;
      column.add(getEntry(e, QCNPFExercise.ENGLISH, englishPrompt, english));
    }

    if (!useMeaningInsteadOfEnglish && meaningValid) {
      column.add(getEntry(e, QCNPFExercise.MEANING, ExerciseFormatter.MEANING_PROMPT, e.getMeaning()));
    }

    return column;
  }

  private void addTransliteration(T e, Panel column) {
    String translitSentence = e.getTransliteration();
    if (!translitSentence.isEmpty() && !translitSentence.equals("N/A")) {
      column.add(getEntry(e, QCNPFExercise.TRANSLITERATION, ExerciseFormatter.TRANSLITERATION, translitSentence));
    }
  }

  private void addAltFL(T e, Panel column) {
    String translitSentence = e.getAltFL().trim();
    if (!translitSentence.isEmpty() && !translitSentence.equals("N/A")) {
      column.add(getEntry(e, QCNPFExercise.ALTFL, ExerciseFormatter.ALTFL, translitSentence));
    }
  }

  private boolean isMeaningValid(T e) {
    return e.getMeaning() != null && !e.getMeaning().trim().isEmpty();
  }

  private void addContextButton(final T e, DivWidget row) {
    final Collection<CommonExercise> directlyRelated = e.getDirectlyRelated();

    if (!directlyRelated.isEmpty() && controller.getProps().showContextButton()) {
      Button show = new Button(CONTEXT_SENTENCE);
      show.setIcon(IconType.QUOTE_RIGHT);
      show.setType(ButtonType.SUCCESS);

      show.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          DivWidget container = new DivWidget();
          String foreignLanguage = e.getForeignLanguage();
          String altFL = e.getAltFL();

          for (CommonExercise contextEx : directlyRelated) {
            container.add(getContext(contextEx, foreignLanguage, altFL));
          }
          new ModalInfoDialog(CONTEXT_SENTENCE, container, false, 600, 400, null);
        }
      });

      show.addStyleName("floatRight");
      Style style = show.getElement().getStyle();
      style.setMarginBottom(5, Style.Unit.PX);
      style.setMarginTop(-10, Style.Unit.PX);
      style.setMarginRight(5, Style.Unit.PX);

      row.add(show);
    }
  }

  private ContextSupport<T> contextSupport = new ContextSupport<T>();

  /**
   * @param exercise
   * @param altText
   * @return
   * @see #addContextButton
   */
  private <U extends CommonAudioExercise> Panel getContext(U exercise, String itemText, String altText) {
    String context = exercise.getForeignLanguage();
    String contextTranslation = exercise.getEnglish();

    boolean same = context.equals(contextTranslation);

    if (!context.isEmpty()) {
      Panel hp = new HorizontalPanel();
      addGenderChoices(exercise, hp);

//      Panel vp = new VerticalPanel();
//
//      Widget entry = getEntry(exercise, QCNPFExercise.CONTEXT, ExerciseFormatter.CONTEXT,
//          contextSupport.getHighlightedItemInContext(context, itemText));
//      vp.add(entry);
//
//      if (!exercise.getAltFL().isEmpty()) {
//        Widget entry2 = getEntry(exercise, QCNPFExercise.ALTCONTEXT, ExerciseFormatter.ALTCONTEXT,
//            contextSupport.getHighlightedItemInContext(exercise.getAltFL(), altText));
//        vp.add(entry2);
//      }
//
//      addContextTranslation(exercise, contextTranslation, same, vp);

      String highlightedVocabItemInContext = highlightVocabItemInContext(exercise, itemText);

      Panel contentWidget = getContentWidget(ExerciseFormatter.CONTEXT, highlightedVocabItemInContext, false);
      String field = QCNPFExercise.CONTEXT;
      ExerciseAnnotation annotation = exercise.getAnnotation(field);

//      logger.info("getContext context " + exercise.getID() + " : " + annotation);

      Widget commentRow = getCommentBox(false)
          .getNoPopup(
              field,
              contentWidget,
              annotation,
              exercise);

      Panel vp = new VerticalPanel();
      vp.add(commentRow);
      addContextTranslation(exercise, contextTranslation, same, vp);
      hp.add(vp);
      return hp;
    } else {
      return null;
    }
  }

  private void addContextTranslation(AnnotationExercise e, String contextTranslation, boolean same, Panel vp) {
    if (!contextTranslation.isEmpty() && !same) {
      Panel contentWidget = getContentWidget(ExerciseFormatter.CONTEXT_TRANSLATION, contextTranslation, false);
      Widget translationEntry = getCommentBox(false).getNoPopup(QCNPFExercise.CONTEXT_TRANSLATION, contentWidget,
          e.getAnnotation(QCNPFExercise.CONTEXT_TRANSLATION),
          exercise);

      vp.add(translationEntry);
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

  /**
   * For context audio!
   * <p>
   * From all the reference audio recorded, show male and female, and default (unknown gender) audio.
   * TODO : why show default audio if we have both male and female ???
   * Choose latest recording, unless we have a preferred user id match.
   *
   * @param e
   * @param hp
   * @see #getContext
   */
  private void addGenderChoices(AudioRefExercise e, Panel hp) {
    // first, choose male and female voices
    long maleTime = 0, femaleTime = 0;
    Set<Long> preferredUsers = controller.getProps().getPreferredVoices();
    for (AudioAttribute audioAttribute : e.getAudioAttributes()) {
      if (audioAttribute.isContextAudio()) {
        logger.info("addGenderChoices : adding context audio " + audioAttribute);
        long user = audioAttribute.getUser().getID();
        if (user == -1) {
          defaultAudio = audioAttribute;
        } else if (audioAttribute.getUser().isMale()) {
          if (audioAttribute.getTimestamp() > maleTime) {
            if (maleAudio == null || !preferredUsers.contains((long) maleAudio.getUser().getID())) {
              maleAudio = audioAttribute;
              maleTime = audioAttribute.getTimestamp();
            }
          }
        } else if (audioAttribute.getTimestamp() > femaleTime) {
          if (femaleAudio == null || !preferredUsers.contains((long) femaleAudio.getUser().getID())) {
            femaleAudio = audioAttribute;
            femaleTime = audioAttribute.getTimestamp();
          }
        }
      } else {
        logger.info("skipping non-context " + audioAttribute);
      }
    }

    addPlayAndVoiceChoices(hp);
  }

  /**
   * choose male first if multiple choices
   *
   * @param hp
   */
  private void addPlayAndVoiceChoices(Panel hp) {
    AudioAttribute toUse = maleAudio != null ? maleAudio : femaleAudio != null ? femaleAudio : defaultAudio;
    String path = toUse == null ? null : toUse.getAudioRef();
    logger.info("addPlayAndVoiceChoices choosing to play " + toUse);
    logger.info("addPlayAndVoiceChoices path             " + path);
    if (path != null) {
      contextPlay = new PlayAudioPanel(controller, path, false)
          .setPlayLabel("")
          .setPauseLabel("")
          .setMinWidth(12);
      contextPlay.getPlayButton().getElement().getStyle().setMarginTop(-6, Style.Unit.PX);

      hp.add(contextPlay);

      List<String> choices = new ArrayList<>();
      if (maleAudio != null) choices.add(M);
      if (femaleAudio != null) choices.add(F);
      if (defaultAudio != null && (maleAudio == null || femaleAudio == null)) {
        //logger.info("Adding default choice since found " + defaultAudio);
        choices.add(DEFAULT); //better not happen
      }

      hp.add(getShowGroup(choices));
    }
  }

  /**
   * @param choices
   * @return
   * @see #addPlayAndVoiceChoices
   */
  private DivWidget getShowGroup(Collection<String> choices) {
    ButtonToolbar buttonToolbar = new ButtonToolbar();
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setToggle(ToggleType.RADIO);
    buttonToolbar.add(buttonGroup);

    boolean first = true;
    for (final String choice : choices) {
      Button choice1 = getChoice(choice, first, new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          contextPlay.playAudio(getAudioRef(choice));
        }
      });
      buttonGroup.add(choice1);
      if (choice.equals(M)) choice1.setIcon(IconType.MALE);
      else if (choice.equals(F)) choice1.setIcon(IconType.FEMALE);
      first = false;
    }

    Style style = buttonToolbar.getElement().getStyle();
    style.setMarginTop(-6, Style.Unit.PX);
    style.setMarginBottom(0, Style.Unit.PX);
    style.setMarginLeft(5, Style.Unit.PX);

    return buttonToolbar;
  }

  private String getAudioRef(String choice) {
    String audioRef;
    switch (choice) {
      case M:
        audioRef = maleAudio.getAudioRef();
        break;
      case F:
        audioRef = femaleAudio.getAudioRef();
        break;
      default:
        audioRef = defaultAudio.getAudioRef();
        break;
    }
    return audioRef;
  }

  private Button getChoice(String title, boolean isActive, ClickHandler handler) {
    Button onButton = new Button(title.equals(M) ? "" : title.equals(F) ? "" : title);
    onButton.getElement().setId("Choice_" + title);
    controller.register(onButton, exercise.getID());
    onButton.addClickHandler(handler);
    onButton.setActive(isActive);
    onButton.getElement().getStyle().setZIndex(0);
    return onButton;
  }

  /**
   * @param e
   * @param field
   * @param label
   * @param value
   * @return
   * @see #getItemContent
   */
  private Widget getEntry(AnnotationExercise e, final String field, final String label, String value) {
    return getEntry(field, label, value, e.getAnnotation(field));
  }

  /**
   * @param field
   * @param label
   * @param value
   * @param annotation
   * @return
   * @see #getEntry
   * @see #makeFastAndSlowAudio(String)
   */
  private Widget getEntry(final String field, final String label, String value, ExerciseAnnotation annotation) {
    return getCommentBox(true).getEntry(field, getContentWidget(label, value, false), annotation, true);
  }

  /**
   * @param path
   * @return
   * @see #getAudioPanel
   */
  @Override
  protected ASRScoringAudioPanel makeFastAndSlowAudio(final String path) {
//    return new FastAndSlowASRScoringAudioPanel<T>(getLocalExercise(), path, controller, scorePanel, instance) {
//      @Override
//      protected void addAudioRadioButton(Panel vp, RadioButton fast) {
//        vp.add(getCommentBox(true).getEntry(audioPath, fast, exercise.getAnnotation(path)));
    return new FastAndSlowASRScoringAudioPanel<T>(getLocalExercise(), path, controller, options.getInstance()) {

      /**
       * @see #addRegularAndSlow
       * @param vp
       * @param radioButton
       */
      @Override
      protected void addAudioRadioButton(Panel vp, RadioButton radioButton, AudioAttribute audioAttribute) {
        if (path == null) return;
        String audioRef = audioAttribute.getAudioRef();
        ExerciseAnnotation annotation = exercise.getAnnotation(audioRef);
        vp.add(getCommentBox(true).getEntry(audioRef, radioButton, annotation, true));
      }

      /**
       * @see #getAfterPlayWidget
       * @param vp
       */
      @Override
      protected void addNoRefAudioWidget(Panel vp) {
        Widget entry = getEntry(REF_AUDIO, "ReferenceAudio", CommentNPFExercise.NO_REFERENCE_AUDIO, exercise.getAnnotation(REF_AUDIO));
        entry.setWidth("500px");
        vp.add(entry);
      }
    };
  }

  /**
   * @return
   * @see #getEntry(String, String, String, ExerciseAnnotation)
   * @see #makeFastAndSlowAudio(String)
   */
  private CommentBox getCommentBox(boolean tooltipOnRight) {
    if (logger == null) {
      logger = Logger.getLogger("CommentNPFExercise");
    }
    T exercise = this.exercise;
    return new CommentBox(this.exercise.getID(), controller, this, exercise.getMutableAnnotation(), tooltipOnRight);
  }
}
