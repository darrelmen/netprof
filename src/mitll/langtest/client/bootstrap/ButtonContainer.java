/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/2/13
 * Time: 10:40 AM
 * To change this template use File | Settings | File Templates.
 */
class ButtonContainer {
  private final List<Button> buttons = new ArrayList<Button>();
  private final Set<Button> enabled = new HashSet<Button>();
  private final Set<Button> disabled = new HashSet<Button>();
  private final Map<String, Collection<Button>> nameToButton = new HashMap<String, Collection<Button>>();
  private final boolean debug = false;

  public void add(Button b, String name) {
    buttons.add(b);
    addButtonToName(name, b);
    addEnabled(b);
  }

  private void addEnabled(Button b) { enabled.add(b); }
  public Collection<Button> getButtons() { return buttons; }

  private void addButtonToName(String name, Button b) {
    Collection<Button> buttonsAtName = nameToButton.get(name);
    if (buttonsAtName == null) {
      nameToButton.put(name, buttonsAtName = new ArrayList<Button>());
    }
    buttonsAtName.add(b);
  }

 public Collection<Button> getButtonsByName(Collection<String> sections) {
   Set<Button> toSelectSet = new HashSet<Button>();
   for (String toSelect : sections) {
     Collection<Button> buttonsAtName = nameToButton.get(toSelect);
     if (buttonsAtName == null) System.err.println("getButtonsByName : huh? can't find " + toSelect + " in " + nameToButton.keySet());
     toSelectSet.addAll(buttonsAtName);
   }
   return toSelectSet;
 }
  public Collection<Button> getButtonsForName(String name) {
    return nameToButton.get(name);

  }
  public FlexSectionExerciseList.ButtonWithChildren getFirstButton() {
    if (buttons.isEmpty()) return null;
    return (FlexSectionExerciseList.ButtonWithChildren) buttons.iterator().next();
  }

/*  public void disableButton(Button button) {
    disabled.add(button);
    enabled.remove(button);
  }*/

  public void enableAll() {
   // System.out.println("----> enableAll for " + this);
    enabled.addAll(buttons);
    disabled.clear();
    showEnabled();
  }

  /**
   * @see ButtonGroupSectionWidget#enableChildrenButtons
   * @param buttonChildren
   * @param isEnable
   */
  public void rememberEnabled(Collection<FlexSectionExerciseList.ButtonWithChildren> buttonChildren, boolean isEnable) {
    if (debug) System.out.println(this + " rememberEnabled for " + buttonChildren + " : to enable = " + isEnable);

    if (!buttons.containsAll(buttonChildren)) {
      System.err.println(this + " rememberEnabled  children = " + buttonChildren + " are not part of this set of buttons");
    }
    if (isEnable) {
   /*   if (enabled.size() == buttons.size()) {
        System.out.println("\n\n\n"+this + " rememberEnabled everything enabled, so clearing first ----> ");

        clearEnabled();
      }*/
      enabled.addAll(buttonChildren);
    }
    else {
      enabled.removeAll(buttonChildren);
    }
/*    if (enabled.isEmpty()) {
      System.out.println("\n\n\n"+this + " rememberEnabled enabled is empty so enabling all!");

      enabled.addAll(buttons);
    }*/
    disabled.clear();
    disabled.addAll(buttons);
    disabled.removeAll(enabled);
    if (debug) System.out.println(this + " rememberEnabled after ");
  }

  /**
   * @see mitll.langtest.client.bootstrap.ButtonGroupSectionWidget#clearEnabled()
   * @see FlexSectionExerciseList#clearEnabled(String)
   */
  public void clearEnabled() {
   // System.out.println(this + " : clearEnabled ");

    enabled.clear();
    disabled.addAll(buttons);
  }

  public void showEnabled() {
   // System.out.println(this + " : showEnabled ");

    for (Button b : enabled) {
      b.setEnabled(true);
    }
    for (Button b : disabled) {
      b.setEnabled(false);
    }
  }

  public String toString() {
    return "Buttons Set : " + buttons.size() + " buttons (" + enabled.size() + "/" + disabled.size() + ")";
  }
}
