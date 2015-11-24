/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.FileUpload;
import com.github.gwtbootstrap.client.ui.base.AddOn;
import com.github.gwtbootstrap.client.ui.base.HasId;
import com.github.gwtbootstrap.client.ui.base.ValueBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

import java.util.ArrayList;
import java.util.List;
//import java.util.logging.Logger;

/**
 * DecoratedF: One or more decorated/related fields. This basic container
 * allows fields to contain a common label,
 * help and error formatting/messages.
 *
 * @author Raymond Budd <a href=mailto:raymond.budd@ll.mit.edu>raymond.budd@ll.mit.edu</a>
 * @since May 16, 2013 3:43:26 PM
 */
public class DecoratedFields {
//  protected static final Logger log = Logger.getLogger(DecoratedFields.class.getName());

  private ControlGroup ctrlGroup;
  private ControlLabel label;
  private List<Widget> controls;
  private HelpBlock messageLocation;
  private String helpMsg;

  public DecoratedFields() {
  }

  public DecoratedFields(String mainLabel, Widget control) {
    this(mainLabel, control, null, null);
  }

  public DecoratedFields(String mainLabel, Widget control,
                         String helpMsg, IconType labelIcon) {
    List<Widget> controls = new ArrayList<Widget>();
    if (control != null) {
      controls.add(control);
    }
    init(mainLabel, controls, helpMsg, labelIcon);
  }

  public DecoratedFields(String mainLabel, Widget[] controlArr) {
    this(mainLabel, controlArr, null, null);
  }

  private void warn(String warn) {
    System.out.println("warn " + warn);
  }

  private void info(String warn) {
    System.out.println("info " + warn);
  }

  public DecoratedFields(String mainLabel, Widget[] controlArr, String helpMsg, IconType labelIcon) {
    List<Widget> controls = new ArrayList<Widget>();
    if (controlArr != null) {
      for (int i = 0; i < controlArr.length; i++) {
        if (controlArr[i] != null) {
          controls.add(controlArr[i]);
        } else {
          warn("DecoratedFields init error! Ignoring null control!");
        }
      }
    }
    init(mainLabel, controls, helpMsg, labelIcon);
  }

  public DecoratedFields(String mainLabel, List<Widget> controls) {
    init(mainLabel, controls, null, null);
  }

  protected void init(String mainLabel, List<Widget> controls,
                      String helpMsg, IconType labelIcon) {
    ctrlGroup = new ControlGroup();
    Controls ctrls = new Controls();
    ctrls.setControlsRow(true);

    if (mainLabel != null || labelIcon != null) {
      if (mainLabel != null && labelIcon != null) {
        AddOn content = new AddOn();
        content.add(new Icon(labelIcon));
        content.add(new Label(mainLabel));
        label = new ControlLabel();
        label.add(content);
      } else if (labelIcon != null) {
        label = new ControlLabel();
        label.add(new Icon(labelIcon));
      } else {
        label = new ControlLabel(mainLabel);
      }
      ctrlGroup.add(label);
    }

    ctrlGroup.add(ctrls);

    this.helpMsg = (helpMsg != null) ? helpMsg : "";
    messageLocation = new HelpBlock();
    messageLocation.setText(this.helpMsg);
    if ((controls == null) || controls.size() < 1) {
      warn("DecoratedFields init error! No controls provided!");
      this.controls = new ArrayList<Widget>();
    } else {
      this.controls = controls;
      Widget firstControl = controls.get(0);
      for (Widget control : controls) {
        ctrls.add(control);
      }
      makeAndSetID(firstControl);
      setErrorLabel(firstControl);
    }
    ctrls.add(messageLocation);
  }

  private void makeAndSetID(Widget control) {
    if (control instanceof HasId) {
      ((HasId) control).setId(makeAndSetIDOnLabel());
    } else {
      info("*** Can not set ID for " + control.getClass());
    }
  }

  private void setErrorLabel(Widget control) {
    if (control instanceof FileUpload) {
      ((FileUpload) control).setErrorLabel(messageLocation);
    } else if (control instanceof ValueBoxBase) {
      ((ValueBoxBase<?>) control).setErrorLabel(messageLocation);
    } else if (control instanceof ValueBoxBase) {
      ((ValueBoxBase<?>) control).setErrorLabel(messageLocation);
//		} else {
//			info("Can not set error label for " + control.getClass());
    }
  }

  private String makeAndSetIDOnLabel() {
    String ctrlId = Document.get().createUniqueId();
    if (label != null) {
      label.setFor(ctrlId);
    }
    return ctrlId;
  }

  public void clearError() {
    setMessage(helpMsg);
  }

  public void setError(String errMsg) {
    setMessage(errMsg, ControlGroupType.ERROR);
  }

  public void setMessage(String newMsg) {
    setMessage(newMsg, ControlGroupType.NONE);
  }

  public void setWarning(String newMsg) {
    setMessage(newMsg, ControlGroupType.WARNING);
  }

  private void setMessage(String newMsg, ControlGroupType mType) {
    ctrlGroup.setType(mType);

    if (newMsg == null) {
      newMsg = "";
    }
    messageLocation.setText(newMsg);
  }

  public ControlGroup getCtrlGroup() {
    return ctrlGroup;
  }

  public boolean hasLabel() {
    return label != null;
  }

  public ControlLabel getLabel() {
    return label;
  }

  public HelpBlock getMessageLocation() {
    return messageLocation;
  }

  public List<Widget> getControls() {
    return controls;
  }

  public Widget getPrimaryControl() {
    return (controls.size() > 0) ? controls.get(0) : null;
  }

  public boolean isPrimaryFocusable() {
    Widget w = getPrimaryControl();
    return w != null && w instanceof Focusable;
  }

  public void setPrimaryFocus(boolean val) {
    Widget w = getPrimaryControl();
    if (w != null && w instanceof Focusable) {
      ((Focusable) w).setFocus(val);
    }
  }

  public boolean performBasicValidate() {
    return performBasicValidate(0);
  }

  public boolean performBasicValidate(int minLength) {
    this.clearError();
    String msg = null;

    Widget w = getPrimaryControl();
    if (label != null) {
      info("Validating '" + label + "' " + w.getClass());
    }
    if (w instanceof HasValue<?>) {
      HasValue<?> valw = (HasValue<?>) w;
      Object valObj = valw.getValue();

      if (label != null) {
        info("Validating " + label.getFor());
      }
      info("Validation value '" + valObj + "'");

      if (valObj == null) { // this implies a parse error.
        msg = "Invalid value";
      } else if (valObj != null) {
        if (valObj instanceof String) {
          String vStr = ((String) valObj).trim();
          if (vStr.isEmpty()) {
            msg = "Value required";
          } else if (minLength > 0 && vStr.length() < minLength) {
            msg = "Minimum length is " + minLength;
          }
        }
      }
    }

    if (msg != null) {
      this.setError(msg);
      return false;
    }

    return true;
  }

  /**
   * Try to get the first field value.
   *
   * @return the value, or null if the field is not of the required type,
   * or has no value
   */
  public Object getValue() {
    Widget w = getPrimaryControl();
    if (w instanceof HasValue<?>) {
      HasValue<?> valw = (HasValue<?>) w;
      Object val = valw.getValue();
      if (val instanceof String) {
        val = ((String) val).trim();
      }
      return val;
    } else if (w instanceof ListBox) {
      ListBox theBox = (ListBox) w;
      int selected = theBox.getSelectedIndex();
      if (selected >= 0) {
        return theBox.getValue(selected);
      }
    }
    return null;
  }

  public boolean isDirty(Object original) {
    Object val = getValue();
    if (val == null) {
      warn("Value should not be null for " + getLabel());
      return true;
    }
    if (val instanceof String) {
      val = ((String) val).trim();
    }

    if (original instanceof String) {
      original = ((String) original).trim();
    }
    boolean isDirty = (!(val.equals(original)));
//		log.fine("               Dirty check on " + mainLabelText + " shows " +
//				isDirty + " for " + val + " and " + original);
    return isDirty;
  }


  /**
   * Try to get the selected enumeration value. This assumes
   * the fields contain a set of checkboxes that correspond
   * to an enumeration.
   * <p/>
   * NOTE: This should not be used for dropdown lists
   *
   * @param eType The enumeration type to retrieve.
   * @return
   */
  public <T extends Enum<T>> T getSelectedValue(Class<T> eType) {
    List<Widget> ctrls = getControls();
    CheckBox selected = null;
    for (int i = 0; i < ctrls.size() && selected == null; i++) {
      Widget w = ctrls.get(i);
      if (w instanceof CheckBox && (((CheckBox) w).getValue())) {
        selected = (CheckBox) w;
      }
    }

    return Enum.valueOf(eType, selected.getFormValue());
  }
}
