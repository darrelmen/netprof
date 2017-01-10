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

package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PopupHelper;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.services.AmasService;
import mitll.langtest.client.services.AmasServiceAsync;
import mitll.langtest.shared.answer.Answer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.scoring.AudioContext;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/8/13
 * Time: 1:45 PM
 * To change this template use File | Settings | File Templates.
 */
class TextResponse {
  private final Logger logger = Logger.getLogger("TextResponse");
  private static final int TEXT_BOX_WIDTH = 400;
  private static final int FEEDBACK_HEIGHT = 40;

  private KeyPressHelper enterKeyButtonHelper;
  private ScoreFeedback textScoreFeedback;
  private final int user;
  private AnswerPosted answerPosted;
  private Widget textResponseWidget;

  private long timeShown = System.currentTimeMillis();
  private final Map<String, Collection<String>> typeToSelection;
  private final AmasServiceAsync amasService = GWT.create(AmasService.class);

  /**
   * @param user
   * @param typeToSelection
   * @see mitll.langtest.client.amas.FeedbackRecordPanel.AnswerPanel#doText
   */
  public TextResponse(int user, Map<String, Collection<String>> typeToSelection) {
    this.user = user;
    this.typeToSelection = typeToSelection;
  }

  public interface AnswerPosted {
    void answerPosted();
  }

  public void setAnswerPostedCallback(AnswerPosted answerPosted) {
    this.answerPosted = answerPosted;
  }

  /**
   * Has two rows -- the text input box and the score feedback
   *
   * @param toAddTo
   * @param exerciseID
   * @param service
   * @param controller
   * @param centered
   * @param useWhite
   * @param addKeyBinding
   * @param questionID
   * @param buttonTitle
   * @param getFocus
   * @return
   * @see mitll.langtest.client.amas.FeedbackRecordPanel.AnswerPanel#doText
   */
  public Widget addWidgets(Panel toAddTo,
                           int exerciseID, LangTestDatabaseAsync service, ExerciseController controller,
                           boolean centered, boolean useWhite, boolean addKeyBinding, int questionID,
                           String buttonTitle, boolean getFocus) {
    textScoreFeedback = new ScoreFeedback(useWhite);

    textResponseWidget = getTextResponseWidget(exerciseID, service, controller, getTextScoreFeedback(), centered,
        addKeyBinding, questionID, buttonTitle, getFocus);

    if (controller.isRightAlignContent()) {
      textResponseWidget.addStyleName("floatRight");
    } else {
      textResponseWidget.addStyleName("floatLeft");
    }
    textResponseWidget.addStyleName("topFiveMargin");

    Panel scoreFeedbackRow = getTextScoreFeedback().getScoreFeedbackRow(FEEDBACK_HEIGHT, controller);

    Panel hp = new HorizontalPanel();
    hp.add(getTextScoreFeedback().getFeedbackImage());
    hp.add(scoreFeedbackRow);
    hp.add(textResponseWidget);
    toAddTo.add(hp);

    return textResponseWidget;
  }

  /**
   * Three parts - text input, check button, and feedback icon
   *
   * @param exerciseID
   * @param service
   * @param controller
   * @param scoreFeedback
   * @param centered
   * @param addEnterKeyBinding
   * @param questionID
   * @param buttonTitle
   * @param getFocus
   * @return
   * @see #addWidgets
   */
  private Widget getTextResponseWidget(int exerciseID,
                                       LangTestDatabaseAsync service, ExerciseController controller,
                                       ScoreFeedback scoreFeedback, boolean centered, boolean addEnterKeyBinding,
                                       int questionID, String buttonTitle, boolean getFocus) {
    final Button answerButton = getAnswerButton(buttonTitle);

    boolean allowPaste = controller.getProps().isDemoMode();
    final TextBox noPasteAnswer = getAnswerBox(controller, allowPaste, answerButton, getFocus);
    noPasteAnswer.setWidth(TEXT_BOX_WIDTH + "px");
    setupSubmitButton(exerciseID, service, answerButton, noPasteAnswer, scoreFeedback, AudioType.TEXT,
        addEnterKeyBinding,
        questionID);

    // button then text box
    Panel row = new HorizontalPanel();
    row.getElement().setId("textResponseRow");
    row.add(answerButton);
    row.add(noPasteAnswer);

    return centered ? getRecordButtonRow(row) : row;
  }

  private Button getAnswerButton(String buttonTitle) {
    final Button answerButton = new Button(buttonTitle);
    answerButton.getElement().setId("check_" + buttonTitle);
    answerButton.addStyleName("rightFiveMargin");
    return answerButton;
  }

  private Widget getRecordButtonRow(Widget recordButton) {
    FluidRow recordButtonRow = new FluidRow();
    Paragraph recordButtonContainer = new Paragraph();
    recordButtonContainer.addStyleName("alignCenter");
    recordButtonContainer.add(recordButton);
    recordButton.addStyleName("alignCenter");
    recordButtonRow.add(new Column(12, recordButtonContainer));
    recordButtonRow.getElement().setId("recordButtonRow");
    return recordButtonRow;
  }

  /**
   * @param exerciseID
   * @param service
   * @param check
   * @param noPasteAnswer
   * @param scoreFeedback
   * @param answerType
   * @param addEnterKeyBinding
   * @param questionID
   * @see #getTextResponseWidget
   */
  private void setupSubmitButton(final int exerciseID, final LangTestDatabaseAsync service, final Button check,
                                 final TextBox noPasteAnswer, final ScoreFeedback scoreFeedback,
                                 final AudioType answerType,
                                 boolean addEnterKeyBinding, final int questionID) {
    check.setType(ButtonType.PRIMARY);
    check.setEnabled(false);
    check.addStyleName("leftFiveMargin");

    if (addEnterKeyBinding) {
      enterKeyButtonHelper = new KeyPressHelper(false);
      enterKeyButtonHelper.addKeyHandler(check);
      check.setTitle("Hit Enter to submit answer.");
    }

    check.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        getScoreForGuess(noPasteAnswer.getText(), service, exerciseID, check, scoreFeedback, answerType, questionID);
      }
    });
  }

  /**
   * Text box goes RTL.
   *
   * @param controller
   * @param allowPaste
   * @param check
   * @param getFocus
   * @return
   * @see #getTextResponseWidget
   */
  private TextBox getAnswerBox(ExerciseController controller, boolean allowPaste, final Button check, boolean getFocus) {
    final TextBox noPasteAnswer = allowPaste ? new TextBox() : new NoPasteTextBox();
    noPasteAnswer.setPlaceholder("Answer in " + controller.getLanguage());
    if (controller.isRightAlignContent()) {
      noPasteAnswer.addStyleName("rightAlign");
    }

    if (controller.isRightAlignContent()) {
      setLanguageSpecificFont(controller, noPasteAnswer);//.addStyleName(controller.getLanguage().toLowerCase());
      noPasteAnswer.setDirection(HasDirection.Direction.RTL);
    }

    noPasteAnswer.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        String text = noPasteAnswer.getText();
        text = sanitize(text);
        check.setEnabled(!text.isEmpty());
      }
    });

    if (getFocus) {
      Scheduler.get().scheduleDeferred(new Command() {
        public void execute() {
          noPasteAnswer.setFocus(true);
        }
      });
    }
    return noPasteAnswer;
  }

  private void setLanguageSpecificFont(ExerciseController controller, Widget widget) {
    widget.addStyleName(controller.getLanguage().toLowerCase());

    String fontFamily = controller.getProps().getFontFamily();
    if (!fontFamily.isEmpty()) {
      logger.info("using font family " + fontFamily);
      widget.getElement().getStyle().setProperty("fontFamily", fontFamily);
    } else {
      logger.info("not using font family property");
    }
  }

  private String sanitize(String text) {
    return SimpleHtmlSanitizer.sanitizeHtml(text).asString();
  }

  /**
   * TODO : fill in project id here -
   *
   * @param guess
   * @param service
   * @param exerciseID
   * @param check
   * @param scoreFeedback
   * @param answerType    probably should be removed
   * @param questionID
   * @see #setupSubmitButton
   */
  private void getScoreForGuess(final String guess,
                                LangTestDatabaseAsync service,
                                int exerciseID,
                                final Button check,
                                final ScoreFeedback scoreFeedback,
                                AudioType answerType,
                                int questionID) {
    if (guess.isEmpty() || removePunct(guess.trim()).isEmpty()) {
      new PopupHelper().showPopup
          ("Try again.", "Please type a non-empty response.", textResponseWidget);
    } else {
      scoreFeedback.setWaiting();
      check.setEnabled(false);

      long timeSpent = System.currentTimeMillis() - timeShown;
      timeShown = System.currentTimeMillis();

      String language = "unknown"; // TODO : fix this later
      AudioContext audioContext =
          new AudioContext(0, user, -1, language,exerciseID, questionID, answerType);

      logger.info("contexxt " + audioContext);
      amasService.getScoreForAnswer(
          audioContext, guess, timeSpent, typeToSelection,
          new AsyncCallback<Answer>() {
            @Override
            public void onFailure(Throwable caught) {
              check.setEnabled(true);
            }

            @Override
            public void onSuccess(Answer result) {
              check.setEnabled(true);
              gotScoreForGuess(result);
            }
          });
    }
  }

  private static final String PUNCT_REGEX = "[\\?\\.,-\\/#!$%\\^&\\*;:{}=\\-_`~()]";//"\\p{P}";

  private String removePunct(String t) {
    return t.replaceAll(PUNCT_REGEX, "");
  }

  /**
   * @param result
   * @see #getScoreForGuess
   */
  void gotScoreForGuess(Answer result) {
    getTextScoreFeedback().showCRTFeedback();
    if (answerPosted != null) {
      answerPosted.answerPosted();
    }
  }

  public void onUnload() {
    if (enterKeyButtonHelper != null) {
      enterKeyButtonHelper.removeKeyHandler();
    }
  }

  private ScoreFeedback getTextScoreFeedback() {
    return textScoreFeedback;
  }
}
