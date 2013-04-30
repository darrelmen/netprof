package mitll.langtest.client;

import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.Paragraph;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;

public class Flashcard {
  HorizontalPanel makeFlashcardHeaderRow(String splashText) {
    HorizontalPanel headerRow = new HorizontalPanel();
    headerRow.setWidth("100%");
    headerRow.addStyleName("headerBackground");
    headerRow.addStyleName("headerLowerBorder");

    Image flashcardImage = new Image(LangTest.LANGTEST_IMAGES + "flashcardIcon2.png");
    flashcardImage.addStyleName("floatLeft");

    FlowPanel iconLeftHeader = new FlowPanel();
    headerRow.add(iconLeftHeader);

    Panel flashcard = new FlowPanel();
    flashcard.addStyleName("inlineStyle");
    flashcard.addStyleName("headerBackground");
    flashcard.addStyleName("leftAlign");
    Paragraph appName = new Paragraph("FLASHCARD");
    appName.addStyleName("bigFont");
    flashcard.add(appName);
    Paragraph subtitle = new Paragraph(splashText);
    subtitle.addStyleName("subtitleForeground");
    DOM.setStyleAttribute(subtitle.getElement(), "marginBottom", "5px");

    flashcard.add(subtitle);
    iconLeftHeader.add(flashcardImage);
    iconLeftHeader.add(flashcard);
    headerRow.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);

    Image collab = new Image(LangTest.LANGTEST_IMAGES + "collabIcon3.png");
    headerRow.add(collab);
    return headerRow;
  }
}