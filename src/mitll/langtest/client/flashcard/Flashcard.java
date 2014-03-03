package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.Paragraph;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.dialog.DialogHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Does fancy font sizing depending on available width...
 */
public class Flashcard implements RequiresResize {
  private static final String AVP = "AUDIO VOCAB PRACTICE";
  public static final String PRONUNCIATION_FEEDBACK = "PRONUNCIATION FEEDBACK";
  private static final double MAX_FONT_EM = 1.8d;
  public static final int SLOP = 55;
  private static final String NEW_PRO_F1_PNG = "NewProF1.png";
  private static final String NEW_PRO_F2_PNG = "NewProF2.png";
  private Paragraph appName;
  private Image flashcardImage;
  private Image collab;
  private int min = 720;

  /**
   * @see mitll.langtest.client.LangTest#doFlashcard()
   * @see mitll.langtest.client.LangTest#makeHeaderRow()
   */
  public Flashcard() {}

  /**
   * @see mitll.langtest.client.LangTest#doFlashcard()
   * @param splashText
   * @return
   */
  public HorizontalPanel makeFlashcardHeaderRow(String splashText) {
    String appIcon = NEW_PRO_F2_PNG;
    return getHeaderRow(splashText, appIcon, AVP, false);
  }

  /**
   * @see mitll.langtest.client.LangTest#makeHeaderRow()
   * @param splashText
   * @return
   */
/*  public HorizontalPanel makeNPFHeaderRow(String splashText) {
    return makeNPFHeaderRow(splashText,PRONUNCIATION_FEEDBACK);
  }*/
  public HorizontalPanel makeNPFHeaderRow(String splashText, boolean isBeta) {
    return getHeaderRow(splashText, NEW_PRO_F1_PNG, PRONUNCIATION_FEEDBACK, isBeta);
  }

  public HorizontalPanel makeNPFHeaderRow(String splashText, String appTitle) {
    String appIcon = NEW_PRO_F1_PNG;
    return getHeaderRow(splashText, appIcon, appTitle, false);
  }

  public HorizontalPanel getHeaderRow(String splashText, String appIcon, String appTitle, boolean isBeta) {
    HorizontalPanel headerRow = new HorizontalPanel();
    headerRow.setWidth("100%");
    headerRow.addStyleName("headerBackground");
    headerRow.addStyleName("headerLowerBorder");

    FlowPanel iconLeftHeader = new FlowPanel();
    headerRow.add(iconLeftHeader);

    Panel flashcard = new FlowPanel();
    flashcard.addStyleName("inlineBlockStyle");
    flashcard.addStyleName("headerBackground");
    flashcard.addStyleName("leftAlign");
    appName = new Paragraph("<span>" + appTitle + "</span>" +(isBeta?("<span><font color='yellow'>" + "&nbsp;BETA" + "</font></span>"):""));
    appName.addStyleName("bigFont");

    flashcard.add(appName);
    Paragraph subtitle = new Paragraph(splashText);
    subtitle.addStyleName("subtitleForeground");
    DOM.setStyleAttribute(subtitle.getElement(), "marginBottom", "5px");

    flashcard.add(subtitle);

    flashcardImage = new Image(LangTest.LANGTEST_IMAGES + appIcon);
    flashcardImage.addStyleName("floatLeft");
    flashcardImage.addStyleName("rightFiveMargin");
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

  @Override
  public void onResize() {
      int clientWidth = Window.getClientWidth();

      if (clientWidth < 1100) {
        setFontWidth();
      } else {
        DOM.setStyleAttribute(appName.getElement(), "fontSize", MAX_FONT_EM + "em");
      }
  }

  private void setFontWidth() {
    int clientWidth = Window.getClientWidth();

    int offsetWidth = flashcardImage.getOffsetWidth();
    int offsetWidth1 = collab.getOffsetWidth();
    int residual = clientWidth - offsetWidth - offsetWidth1 - SLOP;

    //System.out.println("setFontWidth : left " + offsetWidth + " right " + offsetWidth1 + " window " + clientWidth + " residual " + residual);

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

  public void showFlashHelp(final LangTest langTest, boolean isFlashcard) {
    final PropertyHandler props = langTest.getProps();
    if (props.isTimedGame()) {
      GWT.runAsync(new RunAsyncCallback() {
        public void onFailure(Throwable caught) {
          Window.alert("Code download failed");
        }

        public void onSuccess() {
          showTimedGameHelp(langTest);
        }
      });
    } else {
      List<String> msgs = new ArrayList<String>();
      msgs.add(isFlashcard ? "Practice your vocabulary by saying the matching " + props.getLanguage() + " phrase.":"Listen to the audio, then answer the question below.");
      msgs.add("Press the space bar to begin recording your answer.");
      msgs.add("Release the space bar to end recording.");
      DialogHelper dialogHelper = new DialogHelper(false);
      dialogHelper.showErrorMessage("Help", msgs);
    }
  }

  public void showTimedGameHelp(LangTest langTest) {
    final PropertyHandler props = langTest.getProps();
    TimedGame timedGame = new TimedGame(langTest);
    timedGame.showTimedGameHelp(props);
  }

  public void setAppTitle(String appTitle) {
    appName.setText(/*"<span>" + */appTitle/* + "</span>"*/);
  //  this.appTitle = appTitle;
  }

}