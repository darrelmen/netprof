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
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.ExerciseFormatter;

import java.util.ArrayList;
import java.util.Collections;
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
public class CommentNPFExercise extends NPFExercise {
  public static final String CONTEXT = "context";
  private Logger logger = Logger.getLogger("CommentNPFExercise");
  private static final String CONTEXT_SENTENCE = "Context Sentence";
  private static final String DEFAULT = "Default";

  private static final String NO_REFERENCE_AUDIO = "No reference audio";
  private static final String M = "M";
  private static final String F = "F";
  public static final String PUNCT_REGEX = "[\\?\\.,-\\/#!$%\\^&\\*;:{}=\\-_`~()]";//"\\p{P}";
  public static final String SPACE_REGEX = " ";
  public static final String REF_AUDIO = "refAudio";

  private AudioAttribute defaultAudio, maleAudio, femaleAudio;
  private PlayAudioPanel contextPlay;
  private CommentBox commentBox;

  public CommentNPFExercise(CommonExercise e, ExerciseController controller, ListInterface listContainer,
                            boolean addKeyHandler, String instance) {
    super(e, controller, listContainer, 1.0f, addKeyHandler, instance);
  }

  /**
   * @param content
   * @return
   * @see #getQuestionContent(mitll.langtest.shared.CommonExercise)
   */
  @Override
  protected Widget getQuestionContent(final CommonExercise e, String content) {
    if (commentBox == null) // defensive...
     this.commentBox = new CommentBox(e, controller, this);

    Panel column = new VerticalPanel();
    column.getElement().setId("QuestionContent");
    column.setWidth("100%");

    DivWidget row = new DivWidget();
    row.getElement().setId("QuestionContent_item");

    Widget entry = getEntry(e, QCNPFExercise.FOREIGN_LANGUAGE, ExerciseFormatter.FOREIGN_LANGUAGE_PROMPT, e.getRefSentence());
    entry.addStyleName("floatLeft");
    row.add(entry);

    addContextButton(e, row);

    column.add(row);

    String translitSentence = e.getTransliteration();
    if (!translitSentence.isEmpty() && !translitSentence.equals("N/A")) {
      column.add(getEntry(e, QCNPFExercise.TRANSLITERATION, ExerciseFormatter.TRANSLITERATION, translitSentence));
    }

    String english = e.getMeaning() != null && !e.getMeaning().trim().isEmpty() ? e.getMeaning() : e.getEnglish();
    if (!english.isEmpty() && !english.equals("N/A")) {
      column.add(getEntry(e, QCNPFExercise.ENGLISH, ExerciseFormatter.ENGLISH_PROMPT, english));
    }

    return column;
  }

  private void addContextButton(final CommonExercise e, DivWidget row) {
    String context = e.getContext() != null && !e.getContext().trim().isEmpty() ? e.getContext() : "";

    if (!context.isEmpty() && controller.getProps().showContextButton()) {
      Button show = new Button(CONTEXT_SENTENCE);
      show.setIcon(IconType.QUOTE_RIGHT);
      show.setType(ButtonType.SUCCESS);
      show.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          new ModalInfoDialog(CONTEXT_SENTENCE, Collections.EMPTY_LIST, getContext(e),
              null
          );
        }
      });

      show.addStyleName("floatRight");
      show.getElement().getStyle().setMarginBottom(5, Style.Unit.PX);
      show.getElement().getStyle().setMarginTop(-10, Style.Unit.PX);
      show.getElement().getStyle().setMarginRight(5, Style.Unit.PX);

      row.add(show);
    }
  }

  /**
   * @see #addContextButton
   * @param e
   * @return
   */
  private Panel getContext(CommonExercise e) {
    String context = e.getContext() != null && !e.getContext().trim().isEmpty() ? e.getContext() : "";
    String contextTranslation = e.getContextTranslation() != null && !e.getContextTranslation().trim().isEmpty() ? e.getContextTranslation() : "";

    if (!context.isEmpty()) {
      Panel hp = new HorizontalPanel();
      Panel vp = new VerticalPanel();
      addGenderChoices(e, hp);
      context = highlightVocabItemInContext(e, context);
      Widget entry = getEntry(e, QCNPFExercise.CONTEXT, ExerciseFormatter.CONTEXT, context);
      vp.add(entry);

      if (!contextTranslation.isEmpty()) {
        Widget translationEntry = getEntry(e, QCNPFExercise.CONTEXT_TRANSLATION, ExerciseFormatter.CONTEXT_TRANSLATION, contextTranslation);
        vp.add(translationEntry);
      }
      hp.add(vp);
      return hp;
    } else {
      return null;
    }
  }

  /**
   * Add underlines of item tokens in context sentence.
   *
   * TODO : don't do this - make spans with different colors
   *
   * @param e
   * @param context
   * @return
   * @see #getContext(CommonExercise)
   */
  private String highlightVocabItemInContext(CommonExercise e, String context) {
    String trim = e.getRefSentence().trim();
    // String trim = ref;
    String toFind = removePunct(trim);

    // todone split on spaces, find matching words if no contigious overlap
    int i = context.indexOf(toFind);
    int end = i + toFind.length();
    if (i > -1) {
      //log("marking underline from " + i + " to " + end + " for '" + toFind +  "' in '" + trim + "'");
      context = context.substring(0, i) + "<u>" + context.substring(i, end) + "</u>" + context.substring(end);
    } else {
      //log("NOT marking underline from " + i + " to " + end);
      //log("trim   " + trim + " len " + trim.length());
      //log("toFind " + toFind + " len " + trim.length());

      List<String> tokens = getTokens(trim);
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
        } else {
          //log("from " + endToken + " couldn't find token '" + token + "' len " + token.length() + " in '" + context + "'");
        }
      }
      builder.append(context.substring(endToken));
      // System.out.println("before " + context + " after " + builder.toString());
      context = builder.toString();
    }
    return context;
  }

  /*
  private void log(String message) {
    System.out.println(message);
    console(message);
  }

  private void console(String message) {
    int ieVersion = BrowserCheck.getIEVersion();
    if (ieVersion == -1 || ieVersion > 9) {
      consoleLog(message);
    }
  }

  private native static void consoleLog(String message) */
/*-{
      console.log("LangTest:" + message);
  }-*//*
;
*/

  /**
   * For context audio!
   *
   * From all the reference audio recorded, show male and female, and default (unknown gender) audio.
   * TODO : why show default audio if we have both male and female ???
   * Choose latest recording, unless we have a preferred user id match.
   *
   * @param e
   * @param hp
   * @see #getContext
   */
  private void addGenderChoices(CommonExercise e, Panel hp) {
    // first, choose male and female voices

    long maleTime = 0, femaleTime = 0;
    Set<Long> preferredUsers = controller.getProps().getPreferredVoices();
    for (AudioAttribute audioAttribute : e.getAudioAttributes()) {
      if (audioAttribute.getAudioType().startsWith(CONTEXT)) {
        long user = audioAttribute.getUser().getId();
        if (user == -1) {
          defaultAudio = audioAttribute;
        } else if (audioAttribute.getUser().isMale()) {
          if (audioAttribute.getTimestamp() > maleTime) {
            if (maleAudio == null || !preferredUsers.contains(maleAudio.getUser().getId())) {
              maleAudio = audioAttribute;
              maleTime = audioAttribute.getTimestamp();
            }
          }
        } else if (audioAttribute.getTimestamp() > femaleTime) {
          if (femaleAudio == null || !preferredUsers.contains(femaleAudio.getUser().getId())) {
            femaleAudio = audioAttribute;
//            if (preferredUsers.contains(femaleAudio.getUser().getId()))
//              logger.info("found " + femaleAudio.getUser());

            femaleTime = audioAttribute.getTimestamp();
          }
        }
      }
    }

    addPlayAndVoiceChoices(hp);
  }

  /**
   * choose male first if multiple choices
   * @param hp
   */
  private void addPlayAndVoiceChoices(Panel hp) {
    AudioAttribute toUse = maleAudio != null ? maleAudio : femaleAudio != null ? femaleAudio : defaultAudio;
    String path = toUse == null ? null : toUse.getAudioRef();
    if (path != null) {
      //System.out.println("adding context play option " + path);
      contextPlay = new PlayAudioPanel(controller, path)
          .setPlayLabel("")
          .setPauseLabel("")
          .setMinWidth(12);
      contextPlay.getPlayButton().getElement().getStyle().setMarginTop(-6, Style.Unit.PX);

      hp.add(contextPlay);

      List<String> choices = new ArrayList<String>();
      if (maleAudio != null) choices.add(M);
      if (femaleAudio != null) choices.add(F);
      if (defaultAudio != null && (maleAudio == null || femaleAudio == null)) {
        logger.info("Adding default choice since found " + defaultAudio);
        choices.add(DEFAULT); //better not happen
      }

      hp.add(getShowGroup(choices));
    }
  }

  /**
   * @see #addPlayAndVoiceChoices
   * @param choices
   * @return
   */
  private DivWidget getShowGroup(List<String> choices) {
    ButtonToolbar w = new ButtonToolbar();
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setToggle(ToggleType.RADIO);
    w.add(buttonGroup);

    boolean first = true;
    for (final String choice : choices) {
      Button choice1 = getChoice(choice, first, new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          if (choice.equals(M)) {
            //System.out.println("playing male audio! " + maleAudio.getAudioRef());
            contextPlay.playAudio(maleAudio.getAudioRef());
          } else if (choice.equals(F)) {
            //System.out.println("playing female audio! " + femaleAudio.getAudioRef());
            contextPlay.playAudio(femaleAudio.getAudioRef());
          } else {
            contextPlay.playAudio(defaultAudio.getAudioRef());
          }
        }
      });
      buttonGroup.add(choice1);
      if (choice.equals(M)) choice1.setIcon(IconType.MALE);
      else if (choice.equals(F)) choice1.setIcon(IconType.FEMALE);
      first = false;
    }

    Style style = w.getElement().getStyle();
    style.setMarginTop(-6, Style.Unit.PX);
    style.setMarginBottom(0, Style.Unit.PX);
    style.setMarginLeft(5, Style.Unit.PX);

    return w;
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
   * @see #getQuestionContent(mitll.langtest.shared.CommonExercise, String)
   * @see #getContext(mitll.langtest.shared.CommonExercise)
   */
  private Widget getEntry(CommonExercise e, final String field, final String label, String value) {
    return getEntry(field, label, value, e.getAnnotation(field));
  }

  /**
   * @param field
   * @param label
   * @param value
   * @param annotation
   * @return
   * @see #getEntry(mitll.langtest.shared.CommonExercise, String, String, String)
   * @see #makeFastAndSlowAudio(String)
   */
  private Widget getEntry(final String field, final String label, String value, ExerciseAnnotation annotation) {
    return commentBox.getEntry(field, getContentWidget(label, value, false), annotation);
  }

  /**
   * @param path
   * @return
   * @see #getAudioPanel
   */
  @Override
  protected ASRScoringAudioPanel makeFastAndSlowAudio(final String path) {
    return new FastAndSlowASRScoringAudioPanel(exercise, path, service, controller, scorePanel) {
      @Override
      protected void addAudioRadioButton(Panel vp, RadioButton fast) {
        vp.add(commentBox.getEntry(audioPath, fast, exercise.getAnnotation(path)));
      }

      @Override
      protected void addNoRefAudioWidget(Panel vp) {
        Widget entry = getEntry(REF_AUDIO, "ReferenceAudio", CommentNPFExercise.NO_REFERENCE_AUDIO, exercise.getAnnotation(REF_AUDIO));
        entry.setWidth("500px");
        vp.add(entry);
      }
    };
  }
}
