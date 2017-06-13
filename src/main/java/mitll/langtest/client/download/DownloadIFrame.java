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

package mitll.langtest.client.download;

import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.RootPanel;
import mitll.langtest.client.initial.InitialUI;

/** 
 * DownloadIFrame: Extend the default frame to enable a hidden one time use frame suitable for
 * file download. Once the URL is loaded, this will remove itself form the root panel.
 *
 * @author Raymond Budd <a href="mailto:raymond.budd@ll.mit.edu">raymond.budd@ll.mit.edu</a>
 * @since Apr 22, 2014 7:22:15 AM
 */
public class DownloadIFrame extends Frame {
	/** A separate area in the Domino.html that is used to manage download frames. */
	/**
	 * @see InitialUI
	 */
	public static final String DOWNLOAD_AREA_ID = "netProfDownloadArea";
	
	DownloadIFrame(String url) {
		addStyleName("hidden-download-frame");
		removeOldFrames();
		setUrl(url);
		
		RootPanel.get(DOWNLOAD_AREA_ID).add(this);
	}
	
	private void removeOldFrames() {
		RootPanel.get(DOWNLOAD_AREA_ID).clear();
	}
}
