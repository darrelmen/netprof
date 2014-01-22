package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.FluidRow;
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
  private String responseType;
  private ChoiceMade choiceMade = null;

  /**
   * @see mitll.langtest.client.list.TableSectionExerciseList#TableSectionExerciseList(com.github.gwtbootstrap.client.ui.FluidRow, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, boolean, boolean, mitll.langtest.client.exercise.ExerciseController, String)
   * @param responseType
   */
  public ResponseChoice(String responseType) {
    this.responseType = responseType;
  }

  /**
   * @see mitll.langtest.client.bootstrap.ResponseExerciseList#ResponseExerciseList(com.github.gwtbootstrap.client.ui.FluidRow, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, boolean, boolean, mitll.langtest.client.exercise.ExerciseController, String)
   * @param responseType
   * @param choiceMade
   */
  public ResponseChoice(String responseType, ChoiceMade choiceMade) {
    this.responseType = responseType;
    this.choiceMade = choiceMade;
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
  public Panel getResponseTypeWidget() {
    Panel instructions = new FluidRow();
    instructions.addStyleName("trueInlineStyle");
    Nav div = new Nav();
    DOM.setStyleAttribute(div.getElement(), "marginBottom", "0px");

    Dropdown menu = new Dropdown("Response type");

    final Heading responseTypeDisplay = new Heading(5);
    DOM.setStyleAttribute(responseTypeDisplay.getElement(), "marginTop", "0px");

    setDisplay(responseType, responseTypeDisplay);
    NavLink audio = new NavLink(AUDIO);
    audio.setIcon(IconType.MICROPHONE);
    audio.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        responseType = AUDIO;
        setDisplay(responseType, responseTypeDisplay);
      }
    });
    menu.add(audio);

    NavLink text = new NavLink(TEXT);
    text.setIcon(IconType.PENCIL);
    text.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        responseType = TEXT;
        setDisplay(responseType, responseTypeDisplay);
      }
    });
    menu.add(text);

    NavLink both = new NavLink(BOTH);
    IconAnchor anchor = both.getAnchor();
    anchor.setIconPosition(IconPosition.LEFT);
    anchor.add(new Icon(IconType.MICROPHONE));
    anchor.add(new Icon(IconType.PENCIL));
    anchor.setIconPosition(IconPosition.LEFT);

    both.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        responseType = BOTH;
        setDisplay(responseType, responseTypeDisplay);
      }
    });
    menu.add(both);

    div.add(menu);
    Column child = new Column(2, 5, div);
    instructions.add(child);
    Column child1 = new Column(1, responseTypeDisplay);
    instructions.add(child1);
    return instructions;
  }

  private void setDisplay(String responseType, Heading responseTypeDisplay) {
    if(responseType.equals(BOTH)) {
      responseTypeDisplay.setText("<i class='icon-microphone'></i><i class='icon-pencil'></i>&nbsp;"+responseType);
    } else if(responseType.equals(TEXT)) {
      responseTypeDisplay.setText("<i class='icon-pencil'></i>&nbsp;"+responseType);
    } else if(responseType.equals(AUDIO)) {
      responseTypeDisplay.setText("<i class='icon-microphone'></i>&nbsp;"+responseType);
    } else {
      responseTypeDisplay.setText(responseType);
    }
    if (choiceMade != null) choiceMade.choiceMade(responseType);
  }
}
