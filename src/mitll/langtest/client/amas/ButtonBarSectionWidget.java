package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.SectionWidget;

import java.util.*;
import java.util.logging.Logger;

/**
 * A group of buttons that need to maintain
 * User: GO22670
 * Date: 4/8/13
 * Time: 11:19 AM
 * To change this template use File | Settings | File Templates.
 */
class ButtonBarSectionWidget implements SectionWidget {
  private final Logger logger = Logger.getLogger("ButtonBarSectionWidget");
  //private final boolean debug = false;

  private final String type;

  private final List<Button> buttons = new ArrayList<Button>();
  private SingleSelectExerciseList singleSelectExerciseList;

  /**
   * @param type
   * @see SingleSelectExerciseList#addButtonRow(List, FluidContainer, Collection)
   */
  public ButtonBarSectionWidget(String type) {
    this.type = type;
  }

  public String getType() {  return type;  }

  /**
   * Label and two choice widget.
   *
   * @param container
   * @return
   * @see mitll.langtest.client.bootstrap.SingleSelectExerciseList#addButtonRow
   */
  public void getButtonBar(Panel container, String label, Collection<String> values, final String type, ButtonType buttonType,
                           SingleSelectExerciseList singleSelectExerciseList) {
    Panel horizontalPanel = new HorizontalPanel();
    horizontalPanel.getElement().getStyle().setMarginRight(5, Style.Unit.PX);

    // add label
    Heading child = new Heading(5, label);
    child.getElement().getStyle().setMarginTop(15, Style.Unit.PX);
    horizontalPanel.add(child);

    horizontalPanel.add(getButtonBarChoices(values, type, buttonType));

    Set<String> unique = new HashSet<String>(values);

    if (unique.size() < 2) {
      horizontalPanel.setVisible(false);
    }
    container.add(horizontalPanel);

    this.singleSelectExerciseList = singleSelectExerciseList;
  }

  private Widget getButtonBarChoices(Collection<String> values, final String type, ButtonType buttonType) {
    ButtonToolbar toolbar = new ButtonToolbar();
    toolbar.getElement().setId("Choices_" + type);
    styleToolbar(toolbar);

    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setToggle(ToggleType.RADIO);
    toolbar.add(buttonGroup);

    Set<String> seen = new HashSet<String>();
    for (String v : values) {
      if (!seen.contains(v)) {
        Button choice1 = getChoice(buttonGroup, v);
        buttons.add(choice1);
        choice1.setType(buttonType);
        buttonGroup.add(choice1);
        seen.add(v);
      }
    }
    return toolbar;
  }

  private Button getChoice(ButtonGroup buttonGroup, final String text) {
    final Button onButton = new Button(text);
    ClickHandler handler = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          public void execute() {
            singleSelectExerciseList.gotSelection();//type,text);
          }
        });
      }
    };

    Button choice1 = configure(text, handler, onButton);
    buttonGroup.add(choice1);
    return choice1;
  }

  private void styleToolbar(ButtonToolbar toolbar) {
    Style style = toolbar.getElement().getStyle();
    int topToUse = 10;
    style.setMarginTop(topToUse, Style.Unit.PX);
    style.setMarginBottom(topToUse, Style.Unit.PX);
    style.setMarginLeft(5, Style.Unit.PX);
  }

  private Button configure(String title, ClickHandler handler, Button onButton) {
    onButton.setType(ButtonType.INFO);
    String s = "Choice_" + title;
    onButton.getElement().setId(s);
    onButton.addClickHandler(handler);
    onButton.setActive(false);
    return onButton;
  }

  /**
   * @return
   * @see ButtonBarSectionWidget#selectItem(String)
   * @see SingleSelectExerciseList#getNumSelections()
   */
  @Override
  public String getCurrentSelection() {
    for (Button button : buttons) {
      if (button.isActive()) {
        return button.getText();
      }
    }
    return "";
  }

  @Override
  public List<String> getCurrentSelections() {
    List<String> strings = new ArrayList<String>();
    for (Button button : buttons) {
      //  logger.info("getCurrentSelection for  " + getType() + " button "  + button.getElement().getId() + " active " + button.isActive());
      if (button.isActive()) {
        strings.add(button.getText().trim());
      }
    }
    return strings;
  }


  public void clearSelectionState() {
    for (Button button : buttons) {
      button.setActive(false);
    }
  }

  @Override
  public void selectItem(Collection<String> section, boolean doToggle) {

  }

  @Override
  public void clearAll() {

  }

  @Override
  public void enableAll() {

  }

  @Override
  public void addButton(Button b) {

  }

  @Override
  public void addLabel(Widget label, String color) {

  }

  public boolean hasOnlyOne() {
    return buttons.size() == 1;
  }

  /**
   * @param sections
   * @seex mitll.langtest.client.bootstrap.FlexSectionExerciseList#selectItem(String, java.util.Collection)
   */
  public boolean selectItems(Collection<String> sections) {
    if (hasOnlyOne()) {
      simpleSelectOnlyOne();
      return true;
    }
    else {
      List<String> currentSelections = getCurrentSelections();
    //  logger.info("current " + currentSelections + " vs " + sections);
      boolean sameAsCurrent = currentSelections.containsAll(sections) && sections.containsAll(currentSelections);

      if (sameAsCurrent) return false;
      else {
        clearSelectionState();

        for (String item : sections) {
          selectItem(item);
        }
        return true;
      }
    }
  }

  /**
   *
   *
   * @param toSelect
   * @return true if changed selection
   * @see #selectItems(Collection)
   * @see SingleSelectExerciseList#selectItem
   */
  public boolean selectItem(String toSelect) {
    // logger.info("ButtonGroupSectionWidget: selectItem " + getType() + "=" + toSelect);
    String currentSelection = getCurrentSelection();
    boolean alreadySelected = toSelect.equals(currentSelection.trim());

    if (!alreadySelected) {
      boolean wasSelected = false;
      //logger.info("Examine " + buttons.size() + " buttons");
      for (Button button : buttons) {
        String buttonName = button.getText().trim();
        if (buttonName.equals(toSelect)) {
          button.setActive(true);
          wasSelected = true;
        }
      }
      if (!wasSelected && !toSelect.equals("Clear")) {
        logger.warning(getType() + " Selected no button for " + toSelect);
      }
    }
    return !alreadySelected;
  }

  /**
   * @see SingleSelectExerciseList#makeDefaultSelections()
   */
  public void simpleSelectOnlyOne() {
    if (hasOnlyOne()) {
      Button next = buttons.iterator().next();
      next.setActive(true);
    }
  }

  public String toString() {
    return "Group '" + type + "' with " + buttons;
  }
}
