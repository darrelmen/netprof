package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.Paragraph;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import mitll.langtest.client.LangTest;

/**
 * Does fancy font sizing depending on available width...
 */
public class Flashcard implements RequiresResize {
  private static final String AVP = "AUDIO VOCAB PRACTICE";
  private static final String PRONUNCIATION_FEEDBACK = "PRONUNCIATION FEEDBACK";
  private static final double MAX_FONT_EM = 1.8d;
  private Paragraph appName;
  private Image flashcardImage;
  private Image collab;

  /**
   * @see mitll.langtest.client.LangTest#doFlashcard()
   * @see mitll.langtest.client.LangTest#makeHeaderRow()
   */
  public Flashcard() {}

  public HorizontalPanel makeFlashcardHeaderRow(String splashText) {
    String appIcon = "flashcardIcon2.png";
    return getHeaderRow(splashText, appIcon, AVP);
  }

  public HorizontalPanel makeNPFHeaderRow(String splashText) {
    String appIcon = "npfIcon.png";
    return getHeaderRow(splashText, appIcon, PRONUNCIATION_FEEDBACK);
  }

  private HorizontalPanel getHeaderRow(String splashText, String appIcon, String appTitle) {
    HorizontalPanel headerRow = new HorizontalPanel();
    headerRow.setWidth("100%");
    headerRow.addStyleName("headerBackground");
    headerRow.addStyleName("headerLowerBorder");

    flashcardImage = new Image(LangTest.LANGTEST_IMAGES + appIcon);
    flashcardImage.addStyleName("floatLeft");

    FlowPanel iconLeftHeader = new FlowPanel();
    headerRow.add(iconLeftHeader);

    Panel flashcard = new FlowPanel();
    flashcard.addStyleName("inlineStyle");
    flashcard.addStyleName("headerBackground");
    flashcard.addStyleName("leftAlign");
    appName = new Paragraph("<span>" + appTitle + "</span>");
    appName.addStyleName("bigFont");

    flashcard.add(appName);
    Paragraph subtitle = new Paragraph(splashText);
    subtitle.addStyleName("subtitleForeground");
    DOM.setStyleAttribute(subtitle.getElement(), "marginBottom", "5px");

    flashcard.add(subtitle);
    iconLeftHeader.add(flashcardImage);
    iconLeftHeader.add(flashcard);
    headerRow.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);

    collab = new Image(LangTest.LANGTEST_IMAGES + "collabIcon3.png");
    headerRow.add(collab);

      headerRow.addAttachHandler(new AttachEvent.Handler() {
        @Override
        public void onAttachOrDetach(AttachEvent event) {
          onResize();
        }
      });

    return headerRow;
  }

  private int min = 720;

  @Override
  public void onResize() {
      int clientWidth = Window.getClientWidth();

      if (clientWidth < 1100) {
        setFontWidth();
      } else {
        DOM.setStyleAttribute(appName.getElement(), "fontSize", MAX_FONT_EM +
          "em");
      }
  }

  private void setFontWidth() {
    int clientWidth = Window.getClientWidth();

    int offsetWidth = flashcardImage.getOffsetWidth();
    int offsetWidth1 = collab.getOffsetWidth();
    int residual = clientWidth - offsetWidth - offsetWidth1 - 40;

//   System.out.println("setFontWidth : left " + offsetWidth + " right " + offsetWidth1 + " window " + clientWidth + " residual " + residual);

    double ratio = 2.0d * (double) residual / (double) min;
    ratio *= 10;
    ratio = Math.floor(ratio);
    ratio /= 10;
    if (ratio < 0.7) ratio = 0.7;
    if (ratio > MAX_FONT_EM) ratio =  MAX_FONT_EM;
    String fontsize = ratio + "em";
  //  System.out.println("setFontWidth : Setting font size to " + fontsize);
    DOM.setStyleAttribute(appName.getElement(), "fontSize", fontsize);
  }
}