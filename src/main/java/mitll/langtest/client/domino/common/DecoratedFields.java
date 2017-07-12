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
package mitll.langtest.client.domino.common;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * DecoratedF: One or more decorated/related fields. This basic container 
 * allows fields to contain a common label, 
 * help and error formatting/messages. 
 *
 * @author Raymond Budd <a href=mailto:raymond.budd@ll.mit.edu>raymond.budd@ll.mit.edu</a>
 * @since May 16, 2013 3:43:26 PM
 */
public class DecoratedFields {

	protected static final Logger log = Logger.getLogger(DecoratedFields.class.getName());
	
	private ControlGroup ctrlGroup;
	private ControlLabel label;
	private List<Widget> controls;
	private HelpBlock messageLocation;
	private String helpMsg;
	private Label fieldLabel = null;
	private Icon fieldIcon = null;
	
	public DecoratedFields() {}

	public DecoratedFields(String mainLabel, Widget control) {
		this(mainLabel, control, null, null);
	}
	
	public DecoratedFields(String mainLabel, Widget control,
                         String helpMsg, IconType labelIcon) {
		List<Widget> controls = new ArrayList<>();
		if (control != null) {
			controls.add(control);
		}
		init(mainLabel, controls, helpMsg, labelIcon);
	}
	
	public DecoratedFields(String mainLabel, Widget[] controlArr) {
		this(mainLabel, controlArr, null, null);
	}
	
	public DecoratedFields(String mainLabel, Widget[] controlArr, String helpMsg, IconType labelIcon) {
		List<Widget> controls = new ArrayList<>();
		if (controlArr != null) {
			for (int i = 0; i < controlArr.length; i++) {
				if (controlArr[i] != null) {
					controls.add(controlArr[i]);
				} else {
					log.warning("DecoratedFields init error! Ignoring null control!");
				}
			}
		}
		init(mainLabel, controls, helpMsg, labelIcon);
	}

	public DecoratedFields(String mainLabel, List<Widget> controls) {
		init(mainLabel, controls, null, null);
	}
	
	protected void init(String fieldLabelVal, List<Widget> controls,
			String helpMsg, IconType fieldIconType) {
		ctrlGroup = new ControlGroup();
		Controls ctrls = new Controls();
		ctrls.setControlsRow(true);

		// populate the ControlLabel depending on the content.
		// This uses an add-on when both are available, or the direct
		// element otherwise. Use a Label within the ControlLabel to allow
		// updates to the label value
		if (fieldLabelVal != null || fieldIconType != null) {
			label = new ControlLabel();
			if (fieldIconType != null) {
				fieldIcon = new Icon(fieldIconType);
			}
			if (fieldLabelVal != null) {
				fieldLabel = new Label(fieldLabelVal);
			}
			
			if (fieldLabel != null && fieldIcon != null) {
				AddOn content = new AddOn();
				content.add(fieldIcon);
				content.add(fieldLabel);
				label.add(content);
			} else if (fieldIcon != null) {
				label.add(fieldIcon);
			} else {
				label.add(fieldLabel);
			}
			ctrlGroup.add(label);
		}

		ctrlGroup.add(ctrls);
		
		this.helpMsg = (helpMsg != null) ? helpMsg : "";
		messageLocation = new HelpBlock();
		messageLocation.setText(this.helpMsg);
		if ((controls == null)||controls.size() < 1) {
			log.warning("DecoratedFields init error! No controls provided!");
			this.controls = new ArrayList<>();
		} else {
			this.controls = controls;
			Widget firstControl = controls.get(0);
			for(Widget control : controls) {
				ctrls.add(control);
			}
			makeAndSetID(firstControl);
			setErrorLabel(firstControl);
		}
		ctrls.add(messageLocation);
	}
	
	public void updateFieldLabelValue(String newVal) {
		fieldLabel.setText(newVal);
	}
	
	public void updateFieldIconType(IconType fieldIconType) {
		fieldIcon.setIcon(fieldIconType);
	}
	
	private void makeAndSetID(Widget control) {
		if (control instanceof HasId) {
			((HasId) control).setId(makeAndSetIDOnLabel());
		} else {
			log.info("*** Can not set ID for " + control.getClass());
		}
	}

	private void setErrorLabel(Widget control) {
		if (control instanceof FileUpload) {
			((FileUpload)control).setErrorLabel(messageLocation);
		} else if (control instanceof ValueBoxBase) {
			((ValueBoxBase<?>)control).setErrorLabel(messageLocation);
		} else if (control instanceof ValueBoxBase) {
			((ValueBoxBase<?>)control).setErrorLabel(messageLocation);
//		} else {
//			log.info("Can not set error label for " + control.getClass());
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
		messageLocation.setHTML(newMsg);
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
			((Focusable)w).setFocus(val);
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
			log.info("Validating '" + label + "' " + w.getClass());
		}
		if (w instanceof HasValue<?>) {
			HasValue<?> valw = (HasValue<?>)w;
			Object valObj = valw.getValue();
			
			if (label != null) {
				log.info("Validating " + label.getFor());
			}
			log.info("Validation value '" + valObj + "'");
			
			if (valObj == null) { // this implies a parse error.
				msg = "Invalid value";
			} else if (valObj != null) {
				if (valObj instanceof String) {
					String vStr = ((String)valObj).trim();
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
	 * @return the value, or null if the field is not of the required type, 
	 * or has no value 
	 */
	public Object getValue() {
		Widget w = getPrimaryControl();
		if (w instanceof HasValue<?>) {
			HasValue<?> valw = (HasValue<?>)w;
			Object val = valw.getValue();
			if (val instanceof String) {
				val = ((String) val).trim();
			}
			return val;
		} else if (w instanceof ListBox) {
			ListBox theBox = (ListBox)w;
			int selected = theBox.getSelectedIndex();
			if (selected >= 0 ) {
				return theBox.getValue(selected);
			}
		}
		return null;
	}
	
	public boolean isDirty(Object original) {
		Object val = getValue();
		if (val == null) {
			log.warning("Value should not be null for " + getLabel());
			return true;
		}
		if (val instanceof String) {
			val = ((String)val).trim();
		}
		
		if (original instanceof String) {
			original = ((String)original).trim();
		}
		boolean isDirty = (!(val.equals(original)));
//		log.fine("               Dirty check on " + mainLabelText + " shows " + 
//				isDirty + " for " + val + " and " + original);
		return isDirty;
	}
	
	
	/**
	 * Try to get a selected enumeration value. This assumes
	 * the fields contain a set of checkboxes that correspond
	 * to an enumeration. 
	 * 
	 * NOTE: This should not be used for dropdown lists
	 *  
	 * @param eType The enumeration type to retrieve.

	 * @return Return the first value selected in the group. 
	 */
	public <T extends Enum<T>> T getFirstSelectedValue(Class<T> eType) {
		List<Widget> ctrls = getControls();
		for (int i = 0; i < ctrls.size(); i++) {
			Widget w = ctrls.get(i);
			if (w instanceof CheckBox) {
				CheckBox cb = (CheckBox)w;
				if (cb.getValue()) {
					return Enum.valueOf(eType, cb.getFormValue());
				}
			}
		}
		return null;
	}
	
	public <T extends Enum<T>> Set<T> getAllSelectedValues(Class<T> eType) {	
		List<Widget> ctrls = getControls();
		Set<T> selected = new LinkedHashSet<>();
		for (int i = 0; i < ctrls.size(); i++) {
			Widget w = ctrls.get(i);
			if (w instanceof CheckBox) {
				CheckBox cb = (CheckBox)w;
				if (cb.getValue()) {
					T val = Enum.valueOf(eType, cb.getFormValue());
					selected.add(val);
				}
			}
		}
		return selected;
	}
}
