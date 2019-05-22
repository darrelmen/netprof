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

package mitll.langtest.client.dialog;

import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.ui.HTML;
import mitll.langtest.client.scoring.EnglishDisplayChoices;
import mitll.langtest.client.scoring.ISimpleTurn;
import mitll.langtest.client.scoring.PhonesChoices;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.project.Language;
import org.jetbrains.annotations.NotNull;

import static mitll.langtest.client.dialog.TurnViewHelper.SPEAKER_A;
import static mitll.langtest.client.dialog.TurnViewHelper.SPEAKER_B;

public class EditableTurnHelper {
  private TextBox contentTextBox;
  private HTML turnFeedback;
  private final Language language;
  ISimpleTurn simpleTurn;
  boolean isInterpreter;
  String placeholder;
  ClientExercise clientExercise;

  IFocusListener focusListener;
  private String prev = "";


  public EditableTurnHelper(Language language,
                            ISimpleTurn simpleTurn,
                            boolean isInterpreter,
                            String placeholder,
                            ClientExercise clientExercise,
                            IFocusListener focusListener
  ) {
    this.language = language;
    this.simpleTurn = simpleTurn;
    this.isInterpreter = isInterpreter;
    this.placeholder = placeholder;
    this.clientExercise = clientExercise;
    this.focusListener = focusListener;
  }


  public void grabFocus() {
    if (contentTextBox == null) {
     // logger.info("grabFocus no contentTextBox yet for " + getText());
    } else {
    //  logger.info("grabFocus on " + getText());
      contentTextBox.setFocus(true);
    }
  }

  public void setBackgroundColor(String color) {
    contentTextBox.getElement().getStyle().setBackgroundColor(color);

  }

  @NotNull
  public DivWidget getTextBox() {
    DivWidget textBoxContainer = new DivWidget();
    textBoxContainer.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);
    textBoxContainer.add(contentTextBox = addTextBox());
    textBoxContainer.add(getTurnFeedback());
    return textBoxContainer;
  }

  @NotNull
  private HTML getTurnFeedback() {
    HTML turnFeedback = new HTML("");

    Style style = turnFeedback.getElement().getStyle();

    style.setMarginTop(-12, Style.Unit.PX);
    style.setMarginLeft(12, Style.Unit.PX);
    style.setTextAlign(Style.TextAlign.LEFT);
    this.turnFeedback = turnFeedback;
    return turnFeedback;
  }

  /**
   * @param wrapper
   * @see #addWidgets(boolean, boolean, PhonesChoices, EnglishDisplayChoices)
   */
  private TextBox addTextBox() {
    // TODO : instead, make this a div contenteditable!
    TextBox w = new TextBox();
    w.getElement().getStyle().setFontSize(16, Style.Unit.PX);
//    w.setId("TextBox_" + getExID());
    w.setWidth(350 + "px");

    String foreignLanguage = clientExercise.getForeignLanguage();
    if (foreignLanguage.isEmpty()) {
//      String placeholder = clientExercise.hasEnglishAttr() ? "English... (" + simpleTurn.getExID() +
//          ")" : language.toDisplay() + " translation (" + simpleTurn.getExID() +
//          ")";

/*      if (addPlaceHolder) {
        if (!turnContainer.isInterpreter()) {
          placeholder = (columns == ITurnContainer.COLUMNS.LEFT ? SPEAKER_A : SPEAKER_B) + " says...";
        }
      }*/
      w.setPlaceholder(placeholder);
    } else {
      w.setText(foreignLanguage);
      prev = foreignLanguage;
    }
    w.addBlurHandler(event -> gotBlur());
    w.addFocusHandler(event -> gotFocus());
    w.addKeyUpHandler(this::gotKey);

    w.addStyleName("leftTenMargin");
    w.addStyleName("rightTenMargin");
    w.addStyleName("topFiveMargin");

    return w;
  }

  public String getPlaceholder(boolean isInterpreter, ITurnContainer.COLUMNS columns) {
    String placeholder = clientExercise.hasEnglishAttr() ? "English... (" + simpleTurn.getExID() +
        ")" : language.toDisplay() + " translation (" + simpleTurn.getExID() +
        ")";
    return isInterpreter ? (columns == ITurnContainer.COLUMNS.LEFT ? SPEAKER_A : SPEAKER_B) + " says..." : placeholder;
  }

  private void gotBlur() {
    String s = getContent();
    if (s.equals(prev)) {
    } else {
      prev = s;
      focusListener.gotBlur();
      // call focus listener

    }
  }

  public String getContent() {
    return SimpleHtmlSanitizer.sanitizeHtml(contentTextBox.getText()).asString();
  }

  void gotFocus() {
    // call focus listener

  }

  void gotKey(KeyUpEvent event) {
    // call focus listener

  }


}
