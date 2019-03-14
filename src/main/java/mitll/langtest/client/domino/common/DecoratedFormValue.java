/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */
package mitll.langtest.client.domino.common;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.github.gwtbootstrap.client.ui.Controls;

import java.util.logging.Logger;

/**
 * DecoratedFormValue: A simple Read-only value with a label to be shown on a form.  
 *
 * @author Raymond Budd <a href=mailto:raymond.budd@ll.mit.edu>raymond.budd@ll.mit.edu</a>
 * @since May 16, 2013 3:43:26 PM
 */
public class DecoratedFormValue {

	protected static final Logger log = Logger.getLogger("DecoratedFormValue");
	
	private ControlGroup ctrlGroup;
	private ControlLabel label;
	private String formValue;
	private ControlLabel valueLabel;
	
	public DecoratedFormValue() {}
	
	public DecoratedFormValue(String mainLabel, String formValue) {
		init(mainLabel, formValue);
	}
	
	protected void init(String mainLabel, String formValue) {
		this.formValue = formValue;
		ctrlGroup = new ControlGroup();
		Controls ctrls = new Controls();
		ctrls.setControlsRow(true);

		if (mainLabel != null) {
			label = new ControlLabel(mainLabel);
			ctrlGroup.add(label);
		}

		valueLabel = new ControlLabel(formValue);
		ctrls.add(valueLabel);
		
		ctrlGroup.add(ctrls);//valueLabel);
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
	
	public ControlLabel getValueLabel() {
		return valueLabel;
	}

	public String getValue() {
		return formValue;
	}
}