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

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ModalFooter;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

/**
 * DominoSaveableModal: basic extension to modal with Domino configuration.
 * Includes save/cancel buttons. Users attach a save handler to the 
 * save button and perform validation or other actions. See closeOnSave argument, and
 * NewItemSpecificationView for an example use.  
 *
 * @author Raymond Budd <a href=mailto:raymond.budd@ll.mit.edu>raymond.budd@ll.mit.edu</a>
 * @since Apr 19, 2013 2:06:36 PM
 */
public abstract class DominoSaveableModal extends DominoSimpleModal {
	
	private static final String DEF_SAVE_BTN_NM = "Save";
	private static final String DEF_CANCEL_BTN_NM = "Cancel";
	private Button saveBtn = null;
	private String saveBtnNm = DEF_SAVE_BTN_NM;
	
	public DominoSaveableModal(String title, Widget contentWidget) {
		this(true, title, DEF_SAVE_BTN_NM, ModalSize.Big, contentWidget); 		
	}
		
	public DominoSaveableModal(boolean animated, String title, 
			String saveBtnNm, ModalSize mType) {
		this(animated, title, saveBtnNm, mType, null);
	}

	public DominoSaveableModal(boolean animated, String title, 
			String saveBtnNm, ModalSize mType, Widget contentWidget) {
		super(animated, title, DEF_CANCEL_BTN_NM, mType, contentWidget);
		this.saveBtnNm = saveBtnNm;
	}
	
	protected ModalFooter createFooter() {
		ModalFooter footer = super.createFooter();
		UIHandler handler = new UIHandler();
		saveBtn = new Button(saveBtnNm);
		saveBtn.addClickHandler(handler);
		getFooterCol().add(saveBtn);
		return footer;
	}
	
	public Button getSaveButton() { return saveBtn; }
	
	/**
	 * Respond to the save click. Note implementing classes should hide the
	 * modal if desired.
	 */
	protected abstract void handleSave();
	
	private class UIHandler implements ClickHandler {
		@Override
		public void onClick(ClickEvent event) {
			if (event.getSource() == saveBtn) {
				handleSave();
			}
		}
	}
}
