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

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.ModalFooter;
import com.github.gwtbootstrap.client.ui.constants.BackdropType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import java.util.logging.Logger;

/**
 * 
 * DominoSimpleModal: A simple modal with a close button.
 *
 * @author Raymond Budd <a href="mailto:raymond.budd@ll.mit.edu">raymond.budd@ll.mit.edu</a>
 * @since Mar 29, 2015 3:07:38 PM
 */
public abstract class DominoSimpleModal extends Modal {
	
	public static enum ModalSize { Big, Normal, Small };
	
	private static final Logger log = Logger.getLogger(DominoSimpleModal.class.getName());
	private static final String DEF_CLOSE_BTN_NM = "Close";
    private Button closeBtn = null;
	private Column footerCol;
	
	private String title; 
	private String closeBtnNm = DEF_CLOSE_BTN_NM;
	private ModalSize mType;
	protected Widget contentWidget;
	
	public DominoSimpleModal(String title, ModalSize mType) {
		this(true, title, DEF_CLOSE_BTN_NM, ModalSize.Big, null); 		
	}
	
	public DominoSimpleModal(String title, Widget contentWidget) {
		this(true, title, DEF_CLOSE_BTN_NM, ModalSize.Big, contentWidget); 		
	}
		
	public DominoSimpleModal(boolean animated, String title, 
			String closeBtnNm, ModalSize mType) {
		this(animated, title, closeBtnNm, mType, null);
	}

	public DominoSimpleModal(boolean animated, String title, 
			String closeBtnNm, ModalSize mType, Widget contentWidget) {
		super(animated, true);
		this.title = title;
		this.closeBtnNm = closeBtnNm;
		this.mType = mType;
		this.contentWidget = contentWidget;
	}
	
	protected Column getFooterCol() { return footerCol; }
	
	public String getCloseButtonName() {
		return closeBtnNm;
	}

	public void setCloseButtonName(String closeBtnNm) {
		this.closeBtnNm = closeBtnNm;
	}

	public void init() {
		log.info("initializing item modal " + title);
		if (mType == ModalSize.Big) {
			addStyleName("big-modal");
		} else if (mType == ModalSize.Small) {
			addStyleName("small-modal");
		}

		addStyleName("domino-modal");
		setCloseVisible(false);
		setAnimation(mType == ModalSize.Big);
		setBackdrop(BackdropType.STATIC);
		setTitle(title);
		prepareContentWidget();
		add(contentWidget);
		add(createFooter());
		show();
		log.info("Showing item modal " + title);
	}
	
	/**
	 * Allow late bindings or configuration of content widget during initialization.
	 * the contentWidget should be initialized after this is invoked.
	 */
	protected void prepareContentWidget() {	}
	
	protected Widget getContentWidget() {
		return contentWidget;
	}
	
	@Override 
	public void show() {
//		MessageHelper.cleanupInlineMessages();
		super.show();
	}
	
	protected ModalFooter createFooter() {
		UIHandler handler = new UIHandler();
		ModalFooter modalFooter = new ModalFooter();
		FluidRow footerRow = new FluidRow();
		closeBtn = new Button(closeBtnNm);
		closeBtn.addClickHandler(handler);
		footerCol = new Column(12, closeBtn);
		footerRow.add(footerCol);
		modalFooter.add(footerRow);
		return modalFooter;
	}
	
	public Button getCloseButton() { return closeBtn; }
	
	/**
	 * Respond to the close click. Simply hide the modal by default.
	 */
	protected void handleClose() { hide(); }
		
	
	private class UIHandler implements ClickHandler {
		@Override
		public void onClick(ClickEvent event) {
			if (event.getSource() == closeBtn) {
				handleClose();
			} 
		}
	}
}
