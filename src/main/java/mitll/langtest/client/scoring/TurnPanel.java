/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.dialog.IListenView;
import mitll.langtest.client.dialog.ListenViewHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.AllHighlight;
import mitll.langtest.client.sound.AudioControl;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.AlignmentOutput;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Does left, right, or middle justify
 *
 * @param <T>
 * @see ListenViewHelper#reallyGetTurnPanel
 */
public class TurnPanel extends DialogExercisePanel<ClientExercise> {
  private final Logger logger = Logger.getLogger("TurnPanel");

  private static final String FLOAT_LEFT = "floatLeft";

  final ListenViewHelper.COLUMNS columns;
  private DivWidget bubble;
  private static final String HIGHLIGHT_COLOR = "green";
  private boolean rightJustify;

  /**
   * @param clientExercise
   * @param controller
   * @param listContainer
   * @param alignments
   * @param listenView
   * @param columns
   * @param rightJustify
   * @see ListenViewHelper#reallyGetTurnPanel
   */
  public TurnPanel(final ClientExercise clientExercise,
                   final ExerciseController controller,
                   final ListInterface<?, ?> listContainer,
                   Map<Integer, AlignmentOutput> alignments,
                   IListenView listenView,
                   ListenViewHelper.COLUMNS columns, boolean rightJustify) {
    super(clientExercise, controller, listContainer, alignments, listenView);
    this.columns = columns;
    this.rightJustify = rightJustify;
    Style style = getElement().getStyle();
    style.setOverflow(Style.Overflow.HIDDEN);
    style.setClear(Style.Clear.BOTH);

    if (columns == ListenViewHelper.COLUMNS.RIGHT) addStyleName("floatRight");
    else if (columns == ListenViewHelper.COLUMNS.LEFT) addStyleName(FLOAT_LEFT);
  }

  @Override
  boolean shouldShowPhones() {
    List<ExerciseAttribute> speaker = exercise.getAttributes().stream().filter(exerciseAttribute -> exerciseAttribute.getProperty().equals("SPEAKER")).collect(Collectors.toList());
    boolean hasEnglishAttr = exercise.hasEnglishAttr();
    boolean b = !hasEnglishAttr && isInterpreterTurn(speaker);
//    if (b)
//      logger.info("ex " + exercise.getID() + " " + exercise.getEnglish() + " " + exercise.getForeignLanguage() + " Got show phones " + b);
    return b;
  }

  private boolean isInterpreterTurn(List<ExerciseAttribute> speaker) {
    boolean isInterpreterTurn = false;

    if (!speaker.isEmpty()) {
      isInterpreterTurn = speaker.get(0).getValue().equals("I");
    }

    return isInterpreterTurn;
  }

  @Override
  @NotNull
  protected DivWidget getPhoneDivBelowWord(TranscriptSegment wordSegment,
                                           List<TranscriptSegment> phonesInWord,
                                           AudioControl audioControl,
                                           boolean simpleLayout, IHighlightSegment wordHighlight) {
    return new WordTable().getPhoneDivBelowWord(audioControl, phonesInWord, simpleLayout, wordSegment, false, wordHighlight);
  }

  /**
   * @param wrapper
   * @see RefAudioGetter#addWidgets(boolean, boolean, PhonesChoices, EnglishDisplayChoices)
   */
  protected void styleMe(DivWidget wrapper) {
    this.bubble = wrapper;
    wrapper.getElement().setId("bubble_" + getExID());
    wrapper.addStyleName("bubble");

    // decide on left right or middle justify
    {
      if (columns == ListenViewHelper.COLUMNS.LEFT) wrapper.addStyleName("leftbubble");
      else if (columns == ListenViewHelper.COLUMNS.RIGHT) wrapper.addStyleName("rightbubble");
      else if (columns == ListenViewHelper.COLUMNS.MIDDLE) {
        Style style = wrapper.getElement().getStyle();

        String middlebubble2 = "middlebubble2";
        if (exercise.hasEnglishAttr()) {
          middlebubble2 = "middlebubbleRight";
          if (rightJustify) style.setProperty("marginLeft", "auto");
        }

        wrapper.addStyleName(middlebubble2);
        style.setTextAlign(Style.TextAlign.CENTER);
      }
    }

//    Button play = new Button("play");
//    play.addClickHandler(new ClickHandler() {
//      @Override
//      public void onClick(ClickEvent event) {
//        String cc = controller.getLanguageInfo().getLocale();
//        if (columns == ListenViewHelper.COLUMNS.LEFT ||  exercise.hasEnglishAttr()) {
//          cc = "en-US";
//        }
//        if (cc.equals("ko")) cc = "ko-KR";
//        logger.info("speak " + exercise.getForeignLanguage() + " as " + cc);
//        speak2(exercise.getForeignLanguage(), cc);
//      }
//    });
//    flClickableRow.add(play);


    flClickableRow.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    addMarginStyle();
  }

  /**
   * <script src='https://code.responsivevoice.org/responsivevoice.js'></script>
   *
   * <input onclick="responsiveVoice.speak('This is the text you want to speak');" type='button' value='🔊 Play' />
   *
   * @param w
   */
  public native void speak() /*-{
      $wnd.responsiveVoice.speak('This is the text you want to speak');
  }-*/;


  /**
   * var msg = new SpeechSynthesisUtterance();
   * var voices = window.speechSynthesis.getVoices();
   * msg.voice = voices[10]; // Note: some voices don't support altering params
   * msg.voiceURI = 'native';
   * msg.volume = 1; // 0 to 1
   * msg.rate = 1; // 0.1 to 10
   * msg.pitch = 2; //0 to 2
   * msg.text = 'Hello World';
   * msg.lang = 'en-US';
   *
   * msg.onend = function(e) {
   * console.log('Finished in ' + event.elapsedTime + ' seconds.');
   * };
   *
   * speechSynthesis.speak(msg);
   */
  public native void speak2(String text, String language) /*-{
      var msg = new SpeechSynthesisUtterance();
      var voices = window.speechSynthesis.getVoices();
      msg.voice = voices[10]; // Note: some voices don't support altering params
      msg.voiceURI = 'native';
      msg.volume = 1; // 0 to 1
      msg.rate = 1; // 0.1 to 10
      msg.pitch = 1; //0 to 2
      msg.text = text;//'Hello World';
      msg.lang = language;//'en-US';

      msg.onend = function (e) {
          console.log('Finished in ' + event.elapsedTime + ' seconds.');
      };

      speechSynthesis.speak(msg);
  }-*/;

  @Override
  protected void addFloatLeft(Widget w) {
    if (shouldAddFloatLeft()) {
      w.addStyleName(FLOAT_LEFT);
    }
  }

  @Override
  protected boolean shouldAddFloatLeft() {
    return (columns != ListenViewHelper.COLUMNS.MIDDLE);
  }

  @NotNull
  protected AllHighlight getAllHighlight(Collection<IHighlightSegment> flclickables) {
    return new AllHighlight(flclickables, columns != ListenViewHelper.COLUMNS.MIDDLE);
  }

  /**
   * @see ListenViewHelper#removeMarkCurrent
   */
  public void removeMarkCurrent() {
    setBorderColor("white");
  }

  /**
   * @see ListenViewHelper#markCurrent
   */
  public void markCurrent() {
    setBorderColor(HIGHLIGHT_COLOR);
  }

  public boolean hasCurrentMark() {
    return bubble.getElement().getStyle().getBorderColor().equalsIgnoreCase(HIGHLIGHT_COLOR);
  }

  private void setBorderColor(String white) {
    bubble.getElement().getStyle().setBorderColor(white);
  }
}
