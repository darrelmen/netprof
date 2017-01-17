/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/2/13
 * Time: 10:40 AM
 * To change this template use File | Settings | File Templates.
 */
class ButtonContainer {
  private final Logger logger = Logger.getLogger("ButtonContainer");

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

  private void addEnabled(Button b) {
    enabled.add(b);
  }

  public Collection<Button> getButtons() {
    return buttons;
  }

  private void addButtonToName(String name, Button b) {
    Collection<Button> buttonsAtName = nameToButton.get(name);
    if (buttonsAtName == null) {
      nameToButton.put(name, buttonsAtName = new ArrayList<Button>());
    }
    buttonsAtName.add(b);
  }

  /**
   * @see ButtonGroupSectionWidget#selectItem
   * @param sections
   * @return
   */
  Collection<Button> getButtonsByName(Collection<String> sections) {
    Set<Button> toSelectSet = new HashSet<>();
    for (String toSelect : sections) {
      Collection<Button> buttonsAtName = nameToButton.get(toSelect);
      if (buttonsAtName == null) {
        logger.warning("getButtonsByName : huh? can't find '" + toSelect + "' in " + nameToButton.keySet());
      } else {
        toSelectSet.addAll(buttonsAtName);
      }
    }
    return toSelectSet;
  }

  Collection<Button> getButtonsForName(String name) {
    return nameToButton.get(name);

  }

  FlexSectionExerciseList.ButtonWithChildren getFirstButton() {
    if (buttons.isEmpty()) return null;
    return (FlexSectionExerciseList.ButtonWithChildren) buttons.iterator().next();
  }

/*  public void disableButton(Button button) {
    disabled.add(button);
    enabled.remove(button);
  }*/

  void enableAll() {
    // logger.info("----> enableAll for " + this);
    enabled.addAll(buttons);
    disabled.clear();
    showEnabled();
  }

  /**
   * @param buttonChildren
   * @param isEnable
   * @see ButtonGroupSectionWidget#enableChildrenButtons
   */
  void rememberEnabled(Collection<FlexSectionExerciseList.ButtonWithChildren> buttonChildren, boolean isEnable) {
    if (debug) logger.info(this + " rememberEnabled for " + buttonChildren + " : to enable = " + isEnable);

    if (!buttons.containsAll(buttonChildren)) {
      logger.warning(this + " rememberEnabled  children = " + buttonChildren + " are not part of this set of buttons");
    }
    if (isEnable) {
   /*   if (enabled.size() == buttons.size()) {
        logger.info("\n\n\n"+this + " rememberEnabled everything enabled, so clearing first ----> ");

        clearEnabled();
      }*/
      enabled.addAll(buttonChildren);
    } else {
      enabled.removeAll(buttonChildren);
    }
/*    if (enabled.isEmpty()) {
      logger.info("\n\n\n"+this + " rememberEnabled enabled is empty so enabling all!");

      enabled.addAll(buttons);
    }*/
    disabled.clear();
    disabled.addAll(buttons);
    disabled.removeAll(enabled);
    // if (debug) logger.info(this + " rememberEnabled after ");
  }

  /**
   * @see mitll.langtest.client.bootstrap.ButtonGroupSectionWidget#clearEnabled()
   */
  void clearEnabled() {
  //  logger.info(this + " : clearEnabled ");
    enabled.clear();
    disabled.addAll(buttons);
  }

  void showEnabled() {
    // logger.info(this + " : showEnabled ");
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
