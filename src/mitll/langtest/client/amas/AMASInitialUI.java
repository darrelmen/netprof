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

package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.Container;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import mitll.langtest.client.InitialUI;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.user.UserManager;

import java.util.logging.Logger;


/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/21/15.
 */
public class AMASInitialUI extends InitialUI {
  private final Logger logger = Logger.getLogger("AMASInitialUI");
  protected AutoCRTChapterNPFHelper learnHelper;

  public AMASInitialUI(LangTest langTest, UserManager userManager) {
    super(langTest, userManager);
  }

  public int getHeightOfTopRows() {
    return headerRow.getOffsetHeight();
  }

  /**
   * * TODO : FIX ME
   *
   * @param verticalContainer
   * @param firstRow          where we put the flash permission window if it gets shown
   * @seex #handleCDToken(com.github.gwtbootstrap.client.ui.Container, com.google.gwt.user.client.ui.Panel, String, String)
   * @see #populateRootPanel()
   * @see #showLogin()
   */
  @Override
  protected void populateBelowHeader(Container verticalContainer, Panel firstRow) {
    RootPanel.get().clear();
    RootPanel.get().add(verticalContainer);
    /**
     * {@link #makeFlashContainer}
     */
    firstRow.add(lifecycleSupport.getFlashRecordPanel());
    lifecycleSupport.recordingModeSelect();
    learnHelper = new AutoCRTChapterNPFHelper(service, userFeedback, null, controller);
    learnHelper.addNPFToContent(firstRow, "");
  }

  @Override
  public void onResize() {
    if (learnHelper != null) learnHelper.onResize();
  }

  /**
   * @see #gotUser
   * @see #configureUIGivenUser(long) (long)
   */
  @Override
  public void populateRootPanelIfLogin() {
//    logger.info("populateRootPanelIfLogin");
    if (!props.isOdaMode()) {
      int childCount = firstRow.getElement().getChildCount();
      // logger.info("populateRootAfterLogin root " + firstRow.getElement().getNodeName() + " childCount " + childCount);
      if (childCount > 0) {
        Node child = firstRow.getElement().getChild(0);
        Element as = Element.as(child);
        if (as.getId().contains(LOGIN)) {
          //   logger.info("populateRootAfterLogin found login...");
          populateRootPanel();
        }
      }
    }
  }


  /**
   * If in ODA Mode, wait until we start a QUIZ?
   * <p>
   * Then only show the content part of it...
   *
   * @return
   * @see #showLogin
   * @see #populateRootPanelIfLogin()
   */
  public void populateRootPanel() {
    Container verticalContainer = getRootContainer();
    // header/title line
    // first row ---------------
    Panel firstRow = makeFirstTwoRows(verticalContainer);

    if (props.isOdaMode()) {
      headerRow.setVisible(false);
      populateBelowHeader(verticalContainer, firstRow);
    } else if (!showLogin()) {
      populateBelowHeader(verticalContainer, firstRow);
    }
  }

  /**
   * @param userID
   * @return
   * @see #gotUser
   */
  @Override
  public void configureUIGivenUser(long userID) {
    boolean diff = lastUser != userID;
    showUserPermissions(userID);

    if (diff) configureUIGivenUser();
  }

  /**
   * TODO : FIX ME
   */
  private void configureUIGivenUser() {
    if (learnHelper != null && learnHelper.getExerciseList() != null) {
      SingleSelectExerciseList exerciseList = (SingleSelectExerciseList) learnHelper.getExerciseList();
      exerciseList.restoreListFromHistory();
    }
  }
}
