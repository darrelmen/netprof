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
import mitll.langtest.client.scoring.EnglishDisplayChoices;
import mitll.langtest.client.scoring.PhonesChoices;
import mitll.langtest.shared.project.Language;
import org.jetbrains.annotations.NotNull;

import static mitll.langtest.client.dialog.TurnViewHelper.SPEAKER_A;
import static mitll.langtest.client.dialog.TurnViewHelper.SPEAKER_B;

public class EditableTurnHelper {
  private TextBox contentTextBox;
  private final Language language;

  private String placeholder;
  private boolean hasEnglishAttr;
  private String initialContent;
  private IFocusListener focusListener;

  EditableTurnHelper(Language language,
                     boolean hasEnglishAttr,
                     String initialContent,
                     IFocusListener focusListener) {
    this.language = language;
    this.focusListener = focusListener;
    this.hasEnglishAttr = hasEnglishAttr;
    this.initialContent = initialContent;
  }

  public void setPlaceholder(boolean isInterpreter, ITurnContainer.COLUMNS columns) {
    this.placeholder = getPlaceholder(isInterpreter, columns);
  }

  public void setPlaceholder(String placeholder) {
    this.placeholder = placeholder;
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
  DivWidget getTextBox(boolean addMargin) {
    DivWidget textBoxContainer = new DivWidget();
    if (addMargin) {
      textBoxContainer.getElement().getStyle().setMarginBottom(6, Style.Unit.PX);
    }
    textBoxContainer.add(contentTextBox = addTextBox(addMargin));
    return textBoxContainer;
  }

  /**
   * @param wrapper
   * @param addTopMargin
   * @see #addWidgets(boolean, boolean, PhonesChoices, EnglishDisplayChoices)
   */
  private TextBox addTextBox(boolean addTopMargin) {
    TextBox w = new TextBox();
    w.getElement().getStyle().setFontSize(16, Style.Unit.PX);
//    w.setId("TextBox_" + getExID());
    w.setWidth(getTextBoxWidth() + "px");

    String foreignLanguage = getBoxContent();
    if (foreignLanguage.isEmpty()) {
      w.setPlaceholder(placeholder);
    } else {
      w.setText(foreignLanguage);
    }
    w.addBlurHandler(event -> gotBlur());
    w.addFocusHandler(event -> gotFocus());
    w.addKeyUpHandler(this::gotKey);

    w.addStyleName("leftTenMargin");
    w.addStyleName("rightTenMargin");
    if (addTopMargin)
      w.addStyleName("topFiveMargin");

    return w;
  }

  private String getBoxContent() {
    return initialContent;
  }

  protected int getTextBoxWidth() {
    return 350;
  }

  private String getPlaceholder(boolean isInterpreter, ITurnContainer.COLUMNS columns) {
    String placeholder = hasEnglishAttr ? "English..." //+" (" + simpleTurn.getExID() + ")"
        :
        language.toDisplay() + " translation" //+        " (" + simpleTurn.getExID() + ")"
        ;
    return isInterpreter ? (columns == ITurnContainer.COLUMNS.LEFT ? SPEAKER_A : SPEAKER_B) + " says..." : placeholder;
  }

  public String getSanitizedContent() {
    return getSanitized(getContent());
  }

  String getSanitized(String content) {
    return content == null ? "" : SimpleHtmlSanitizer.sanitizeHtml(content).asString();
  }

  String getContent() {
    return contentTextBox.getText();
  }

  private void gotBlur() {
    focusListener.gotBlur();
  }

  private void gotFocus() {
    focusListener.gotFocus();
  }

  private void gotKey(KeyUpEvent event) {
    focusListener.gotKey(event);
  }
}
