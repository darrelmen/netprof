/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.custom.exercise;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ButtonGroup;
import com.github.gwtbootstrap.client.ui.ButtonToolbar;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.client.scoring.FastAndSlowASRScoringAudioPanel;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.ExerciseFormatter;
import mitll.langtest.shared.exercise.AnnotationExercise;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.AudioRefExercise;
import mitll.langtest.shared.exercise.CommonAudioExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/12/13
 * Time: 5:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class ContextCommentNPFExercise<T extends CommonExercise> extends NPFExercise<T> {
  private Logger logger = Logger.getLogger("CommentNPFExercise");

  private static final String CONTEXT_SENTENCE = "Context Sentence";
  private static final String DEFAULT = "Default";

  private static final String NO_REFERENCE_AUDIO = "No reference audio";
  private static final String M = "M";
  private static final String F = "F";
  private static final String PUNCT_REGEX = "[\\?\\.,-\\/#!$%\\^&\\*;:{}=\\-_`~()]";
  private static final String SPACE_REGEX = " ";
  private static final String REF_AUDIO = "refAudio";

  private AudioAttribute defaultAudio, maleAudio, femaleAudio;
  private PlayAudioPanel contextPlay;

  /**
   * @param e
   * @param controller
   * @param listContainer
   * @param addKeyHandler
   * @param instance
   * @paramx mutableAnnotation
   * @see mitll.langtest.client.custom.Navigation#Navigation(LangTestDatabaseAsync, UserManager, ExerciseController, UserFeedback)
   * @see mitll.langtest.client.custom.content.NPFHelper#getFactory(PagingExerciseList, String, boolean)
   */
  public ContextCommentNPFExercise(T e, ExerciseController controller, ListInterface<CommonShell> listContainer,
                                   boolean addKeyHandler, String instance) {
    super(e, controller, listContainer, 1.0f, addKeyHandler, instance);
  }

  /**
   * @return
   * @see GoodwaveExercisePanel#getQuestionContent
   */
  @Override
  protected Widget getItemContent(final T e) {
    Panel column = new VerticalPanel();
    column.getElement().setId("QuestionContent");
    column.setWidth("100%");

    DivWidget row = new DivWidget();
    row.getElement().setId("QuestionContent_item");

    Widget entry = getEntry(e, QCNPFExercise.FOREIGN_LANGUAGE, ExerciseFormatter.FOREIGN_LANGUAGE_PROMPT, e.getForeignLanguage());
    entry.addStyleName("floatLeft");
    row.add(entry);

//    addContextButton(e, row);

    column.add(row);

    addTransliteration(e, column);

    boolean isEnglish = controller.getLanguage().equalsIgnoreCase("english");
    boolean meaningValid = isMeaningValid(e);
    boolean useMeaningInsteadOfEnglish = isEnglish && meaningValid;
    String english = useMeaningInsteadOfEnglish ? e.getMeaning() : e.getEnglish();
    if (!english.isEmpty() && !english.equals("N/A")) {
      String englishPrompt = useMeaningInsteadOfEnglish ? ExerciseFormatter.MEANING_PROMPT : ExerciseFormatter.ENGLISH_PROMPT;
      column.add(getEntry(e, QCNPFExercise.ENGLISH, englishPrompt, english));
    }

    if (!useMeaningInsteadOfEnglish && meaningValid) {
      column.add(getEntry(e, QCNPFExercise.MEANING, ExerciseFormatter.MEANING_PROMPT, e.getMeaning()));
    }

    // TODO : support multiple context sentences
    for (CommonExercise exercise : e.getDirectlyRelated()) {
      if (exercise.isSafeToDecode()) {
        column.add(getContext(exercise, e.getForeignLanguage()));
        break;
      }
    }

    return column;
  }

  private void addTransliteration(T e, Panel column) {
    String translitSentence = e.getTransliteration();
    if (!translitSentence.isEmpty() && !translitSentence.equals("N/A")) {
      column.add(getEntry(e, QCNPFExercise.TRANSLITERATION, ExerciseFormatter.TRANSLITERATION, translitSentence));
    }
  }

  private boolean isMeaningValid(T e) {
    return e.getMeaning() != null && !e.getMeaning().trim().isEmpty();
  }

/*  private void addContextButton(final T e, DivWidget row) {
    //String originalContext = e.getContext();
    //String context = originalContext != null && !originalContext.trim().isEmpty() ? originalContext : "";

    if (!e.getDirectlyRelated().isEmpty() && controller.getProps().showContextButton()) {
      Button show = new Button(CONTEXT_SENTENCE);
      show.setIcon(IconType.QUOTE_RIGHT);
      show.setType(ButtonType.SUCCESS);
      show.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          DivWidget container = new DivWidget();
          for (CommonExercise contextEx : e.getDirectlyRelated()) {
            container.add(getContext(contextEx, e.getForeignLanguage()));
          }
          new ModalInfoDialog(CONTEXT_SENTENCE, container);//getContext(e, e.getForeignLanguage()));
        }
      });

      show.addStyleName("floatRight");
      show.getElement().getStyle().setMarginBottom(5, Style.Unit.PX);
      show.getElement().getStyle().setMarginTop(-10, Style.Unit.PX);
      show.getElement().getStyle().setMarginRight(5, Style.Unit.PX);

      row.add(show);
    }
  }*/

  /**
   * @param e
   * @return
   * @see #addContextButton
   */
  private <U extends CommonAudioExercise> Panel getContext(U e, String itemText) {
    String context = e.getForeignLanguage();//e.getContext() != null && !e.getContext().trim().isEmpty() ? e.getContext() : "";
    String contextTranslation = e.getEnglish();//e.getContextTranslation() != null && !e.getContextTranslation().trim().isEmpty() ? e.getContextTranslation() : "";
    boolean same = context.equals(contextTranslation);

    if (!context.isEmpty()) {
      Panel hp = new HorizontalPanel();
      Panel vp = new VerticalPanel();
      addGenderChoices(e, hp);
      String highlightedVocabItemInContext = getHighlightedItemInContext(context,itemText);
      Widget entry = getEntry(e, QCNPFExercise.CONTEXT, ExerciseFormatter.CONTEXT, highlightedVocabItemInContext);
      vp.add(entry);

      addContextTranslation(e, contextTranslation, same, vp);
      hp.add(vp);
      return hp;
    } else {
      return null;
    }
  }

  private void addContextTranslation(AnnotationExercise e, String contextTranslation, boolean same, Panel vp) {
    if (!contextTranslation.isEmpty() && !same) {
      Widget translationEntry = getEntry(e, QCNPFExercise.CONTEXT_TRANSLATION, ExerciseFormatter.CONTEXT_TRANSLATION,
          contextTranslation);
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
/*
  private String highlightVocabItemInContext(CommonShell e, String context) {
    return getHighlightedItemInContext(context, e.getForeignLanguage());
  }
*/

  /**
   * @param context
   * @param foreignLanguage
   * @return html with underlines on the item text
   * @see
   */
  private String getHighlightedItemInContext(String context, String foreignLanguage) {
    String trim = foreignLanguage.trim();
    String toFind = removePunct(trim);

    // todone split on spaces, find matching words if no contigious overlap
    int i = context.indexOf(toFind);
    if (i == -1) { // maybe mixed case - 'where' in Where is the desk?
      String str = toFind.toLowerCase();
      i = context.toLowerCase().indexOf(str);
    }
    int end = i + toFind.length();
    if (i > -1) {
      //log("marking underline from " + i + " to " + end + " for '" + toFind +  "' in '" + trim + "'");
      context = context.substring(0, i) + "<u>" + context.substring(i, end) + "</u>" + context.substring(end);
    } else {
      //log("NOT marking underline from " + i + " to " + end);
      //log("trim   " + trim + " len " + trim.length());
      //log("toFind " + toFind + " len " + trim.length());

      Collection<String> tokens = getTokens(trim);
      int startToken;
      int endToken = 0;
      StringBuilder builder = new StringBuilder();
      for (String token : tokens) {
        startToken = context.indexOf(token, endToken);
        if (startToken != -1) {
          builder.append(context.substring(endToken, startToken));
          builder.append("<u>");
          builder.append(context.substring(startToken, endToken = startToken + token.length()));
          builder.append("</u>");
        }
        //else {
        //log("from " + endToken + " couldn't find token '" + token + "' len " + token.length() + " in '" + context + "'");
        //}
      }
      builder.append(context.substring(endToken));
      // System.out.println("before " + context + " after " + builder.toString());
      context = builder.toString();
    }
    return context;
  }

  /**
   * @param sentence
   * @return
   * @see #getHighlightedItemInContext(String, String)
   */
  private Collection<String> getTokens(String sentence) {
    List<String> all = new ArrayList<String>();
    sentence = removePunct(sentence);
    for (String untrimedToken : sentence.split(ContextCommentNPFExercise.SPACE_REGEX)) { // split on spaces
      String tt = untrimedToken.replaceAll(ContextCommentNPFExercise.PUNCT_REGEX, ""); // remove all punct
      String token = tt.trim();  // necessary?
      if (token.length() > 0) {
        all.add(token);
      }
    }

    return all;
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
        long user = audioAttribute.getUser().getID();
        if (user == -1) {
          defaultAudio = audioAttribute;
        } else if (audioAttribute.getUser().isMale()) {
          if (audioAttribute.getTimestamp() > maleTime) {
            if (maleAudio == null || !preferredUsers.contains(maleAudio.getUser().getID())) {
              maleAudio = audioAttribute;
              maleTime = audioAttribute.getTimestamp();
            }
          }
        } else if (audioAttribute.getTimestamp() > femaleTime) {
          if (femaleAudio == null || !preferredUsers.contains(femaleAudio.getUser().getID())) {
            femaleAudio = audioAttribute;
            femaleTime = audioAttribute.getTimestamp();
          }
        }
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
    if (path != null) {
      contextPlay = new PlayAudioPanel(controller, path)
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
   * @see GoodwaveExercisePanel#getQuestionContent(CommonShell)
   * @see #getContext
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
    return getCommentBox().getEntry(field, getContentWidget(label, value, false), annotation);
  }

  /**
   * @param path
   * @return
   * @see #getAudioPanel
   */
  @Override
  protected ASRScoringAudioPanel makeFastAndSlowAudio(final String path) {
    return new FastAndSlowASRScoringAudioPanel<T>(getLocalExercise(), path, controller, instance) {
      @Override
      protected void addAudioRadioButton(Panel vp, RadioButton fast, AudioAttribute audioAttribute) {
        vp.add(getCommentBox().getEntry(audioPath, fast, exercise.getAnnotation(path)));
      }

      @Override
      protected void addNoRefAudioWidget(Panel vp) {
        Widget entry = getEntry(REF_AUDIO, "ReferenceAudio", ContextCommentNPFExercise.NO_REFERENCE_AUDIO, exercise.getAnnotation(REF_AUDIO));
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
  private CommentBox getCommentBox() {
    if (logger == null) {
      logger = Logger.getLogger("CommentNPFExercise");
    }
    T exercise = this.exercise;
    return new CommentBox(this.exercise.getID(), controller, this, exercise.getMutableAnnotation(),true);
  }
}
