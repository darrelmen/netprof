package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Icon;
import com.github.gwtbootstrap.client.ui.Nav;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.constants.IconPosition;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Panel;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/7/13
 * Time: 3:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResponseChoice {
  public static final String BOTH = "Both";
  public static final String TEXT = "Text";
  public static final String AUDIO = "Audio";
  public static final String NONE = "None";
  private static final IconType MICROPHONE = IconType.MICROPHONE;
  private static final IconType PENCIL = IconType.PENCIL;
  private String responseType;
  private ChoiceMade choiceMade = null;

  /**
   * @see mitll.langtest.client.list.TableSectionExerciseList#TableSectionExerciseList(com.github.gwtbootstrap.client.ui.FluidRow, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, boolean, boolean, mitll.langtest.client.exercise.ExerciseController, String)
   * @param responseType
   */
  private ResponseChoice(String responseType) { this.responseType = responseType; }

  /**
   * @see mitll.langtest.client.bootstrap.ResponseExerciseList#ResponseExerciseList(com.github.gwtbootstrap.client.ui.FluidRow, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, boolean, boolean, mitll.langtest.client.exercise.ExerciseController, String)
   * @param responseType
   * @param choiceMade
   */
  public ResponseChoice(String responseType, ChoiceMade choiceMade) {
    this(responseType);
    this.choiceMade = choiceMade;
  }

  public static boolean knownChoice(String choice) {
    return BOTH.equals(choice) || TEXT.equals(choice) || AUDIO.equals(choice) || NONE.equals(choice);
  }

  public static interface ChoiceMade {
    void choiceMade(String responseType);
  }

  public String getResponseType() {
    return responseType;
  }

  /**
   * @see mitll.langtest.client.bootstrap.ResponseExerciseList#addBottomText(com.github.gwtbootstrap.client.ui.FluidContainer)
   * @return
   */
  public LeftRight getResponseTypeWidget(String caption, boolean addNone) {
    Nav div = new Nav();
    DOM.setStyleAttribute(div.getElement(), "marginBottom", "0px");
    Dropdown menu = new Dropdown(caption);

    final Heading responseTypeDisplay = new Heading(5);
    DOM.setStyleAttribute(responseTypeDisplay.getElement(), "marginTop", "0px");
    //DOM.setStyleAttribute(responseTypeDisplay.getElement(), "marginLeft", "5px");

    setDisplay(responseType, responseTypeDisplay);

    addAudioChoice(menu, responseTypeDisplay);
    addTextChoice(menu, responseTypeDisplay);
    addBothChoice(menu, responseTypeDisplay);
    if (addNone) addNoneChoice (menu, responseTypeDisplay);

    div.add(menu);

    //Panel container = new FluidRow();
    //Panel container = new HorizontalPanel();
    //Column child = new Column(3, 5, div);
    //container.add(child);
   // container.add(div);
    //grid.add()
    //Column child1 = new Column(1, responseTypeDisplay);
    //container.add(child1);
    //container.add(responseTypeDisplay);
    //container.addStyleName("leftFifteenPercentMargin");
    return new LeftRight(div,responseTypeDisplay);
  }

  public static class LeftRight {
    public final Panel left;
    public final Panel right;
    LeftRight(Panel left, Panel right) { this.left = left; this.right = right; }
  }

  void addAudioChoice(Dropdown menu, final Heading responseTypeDisplay) {
    addChoice(menu, responseTypeDisplay, AUDIO, MICROPHONE);
  }

  void addTextChoice(Dropdown menu, final Heading responseTypeDisplay) {
    addChoice(menu, responseTypeDisplay, TEXT, PENCIL);
  }

  void addNoneChoice(Dropdown menu, final Heading responseTypeDisplay) {
    addChoice(menu, responseTypeDisplay, NONE, IconType.REMOVE);
  }

  private void addChoice(Dropdown menu, final Heading responseTypeDisplay, final String responseType, IconType iconType) {
    NavLink audio = new NavLink(responseType);
    audio.setIcon(iconType);
    audio.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        ResponseChoice.this.responseType = responseType;
        setDisplay(ResponseChoice.this.responseType, responseTypeDisplay);
      }
    });
    menu.add(audio);
  }

  void addBothChoice(Dropdown menu, final Heading responseTypeDisplay) {
    NavLink both = new NavLink(BOTH);
    IconAnchor anchor = both.getAnchor();
    anchor.setIconPosition(IconPosition.LEFT);
    anchor.add(new Icon(MICROPHONE));
    anchor.add(new Icon(PENCIL));
    anchor.setIconPosition(IconPosition.LEFT);

    both.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        responseType = BOTH;
        setDisplay(responseType, responseTypeDisplay);
      }
    });
    menu.add(both);
  }

  private void setDisplay(String responseType, Heading responseTypeDisplay) {
    if(responseType.equals(BOTH)) {
      responseTypeDisplay.setText(
        "<i class='icon-microphone'></i>" +
        "<i class='icon-pencil'></i>&nbsp;"+responseType);
    } else if(responseType.equals(TEXT)) {
      responseTypeDisplay.setText("<i class='icon-pencil'></i>&nbsp;"+responseType);
    } else if(responseType.equals(AUDIO)) {
      responseTypeDisplay.setText("<i class='icon-microphone'></i>&nbsp;"+responseType);
    } else if(responseType.equals(NONE)) {
      responseTypeDisplay.setText("<i class='icon-remove'></i>&nbsp;"+responseType);
    } else {
      responseTypeDisplay.setText(responseType);
    }

    if (choiceMade != null) choiceMade.choiceMade(responseType);
  }
}
