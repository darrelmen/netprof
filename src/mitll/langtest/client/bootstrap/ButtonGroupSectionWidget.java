package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.SectionExerciseList;
import mitll.langtest.client.exercise.SectionWidget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 4/8/13
* Time: 11:19 AM
* To change this template use File | Settings | File Templates.
*/
class ButtonGroupSectionWidget implements SectionWidget {
  private List<Button> buttons = new ArrayList<Button>();
  private Button clearButton;
  private String type;

  public ButtonGroupSectionWidget(String type) {
    this.type = type;
  }

  public void addButton(Button b) {
    this.buttons.add(b);
  //  b.setActive(true);
  }
  public void addClearButton(Button b) {
    clearButton = b;
    clearButton.setEnabled(false);
    //clearButton.setVisible(false);
/*
    clearButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        for (Button b : buttons) {
          if (b.isActive()) {
            b.setActive(false);
           // break;
          }
        }
        System.out.println("disable clear button for type " +type);

        clearButton.setEnabled(false);
      }
    });*/
  }

  private void clearAll() {
    for (Button b : buttons) {
      if (b.isActive()) {
        b.setActive(false);
      }
    }
    System.out.println("disable clear button for type " +type);

    clearButton.setEnabled(false);
  }

  @Override
  public String getCurrentSelection() {
    //System.out.println("getCurrentSelection for " +type + " checking " + buttons.size() + " buttons.");
    StringBuilder builder = new StringBuilder();
    for (Button b : buttons) {
      if (b.isActive()) {
        //System.out.println("\tgetCurrentSelection for " +type + "=" + b.getText());
        builder.append(b.getText().trim());
        builder.append(",");
      }
    }
    if (builder.length() > 0) {
      return builder.toString();
    }
    else {
      return SectionExerciseList.ANY;
    }
    //System.out.println("\tgetCurrentSelection for " +type + " - none are selected");

  }

  private boolean isAnythingSelected() {
    for (Button b : buttons) if (b.isActive()) return true;
    return false;
  }

  /**
   * @see FlexSectionExerciseList#populateListBoxAfterSelection(java.util.Map)
   * @param inSet
   */
  @Override
  public void enableInSet(Collection<String> inSet) {
    System.out.println("enableInSet for " + type + " : " + inSet);

    for (Button b : buttons) {
      String trim = b.getText().trim();
      b.setEnabled(inSet.contains(trim));
    }
  }

  @Override
  public void enableAll() {
    for (Button b : buttons) {
      b.setEnabled(true);
    }
  }

  @Override
  public String getFirstItem() {
    return buttons.iterator().next().getText().trim();
  }

  /**
   * @deprecated -- we don't do this right now
   */
  @Override
  public void selectFirstAfterAny() {
    System.out.println("selectFirstAfterAny called?? --------------");

    //selectItem(getFirstItem(), false);
  }


  @Override
  public void selectItem(Collection<String> sections, boolean doToggle) {
    System.out.println("selectItem " + type + "="+sections);

    if (sections.size() == 1 && sections.iterator().next().equals(SectionExerciseList.ANY)) {
      clearAll();
    } else {
/*    for (Button b : buttons) {
      String trim = b.getText().trim();

      if (b.isActive() && !sections.contains(trim)) {
        b.setActive(false);
        break;
      }
    }*/

      boolean didSelect = false;
      // flip state
      for (Button b : buttons) {
        String trim = b.getText().trim();
        if (sections.contains(trim)) {
          //System.out.println("\tselectItem found button is active = " +b.isActive());

          b.setActive(!doToggle || !b.isActive());

          //System.out.println("\tselectItem after, button is active = " +b.isActive());
          didSelect = b.isActive();
          break;
        }
      }
      boolean anythingSelected = isAnythingSelected();
      if (didSelect && !anythingSelected) {
        System.err.println(">>>> selectItem " + type + "=" + sections + " but nothing selected?");
      }

      setClearButtonState(sections, anythingSelected);
    }
  }

  private void setClearButtonState(Collection<String> sections, boolean anythingSelected) {
    if (clearButton != null) {
      System.out.println("\tselectItem for type " +type+"="+sections + " set clear button.");
      clearButton.setEnabled(anythingSelected);
    }
    else {
      System.err.println("clear button is not set? ");
    }
  }

  /**
   * @see SectionExerciseList#populateListBox(String)
   * @param currentSelection
   */
  @Override
  public void retainCurrentSelectionState(Collection<String> currentSelection) {
/*    for (Button b : buttons) {
      if (b.isActive()) {
        b.setActive(false);
        break;
      }
    }
    boolean found = false;
    int num = 0;
    for (Button b : buttons) {
      String value = b.getText().trim();
      if (currentSelection.contains(value)) {
        b.setActive(true);
        found = true;
       // break;
      }
    }
    System.out.println("retainCurrentSelectionState for type " +type+", "+num + " selected.");

    if (!found) buttons.iterator().next().setActive(true);*/
  }

  @Override
  public void populateTypeWidget(Collection<String> items, Map<String, Integer> sectionToCount) {
  }

  @Override
  public Widget getWidget() {
    return null;
  }
}
