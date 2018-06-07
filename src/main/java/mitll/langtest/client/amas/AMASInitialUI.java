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

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.services.ExerciseService;
import mitll.langtest.client.services.ExerciseServiceAsync;
import mitll.langtest.client.services.LangTestDatabase;
import mitll.langtest.client.services.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserManager;


/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/21/15.
 */
public class AMASInitialUI extends InitialUI {
  //  private final Logger logger = Logger.getLogger("AMASInitialUI");
  private AutoCRTChapterNPFHelper learnHelper;
  private final ExerciseServiceAsync exerciseServiceAsync = GWT.create(ExerciseService.class);

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
   * @paramx contentRow          where we put the flash permission window if it gets shown
   * @seex #handleCDToken(com.github.gwtbootstrap.client.ui.Container, com.google.gwt.user.client.ui.Panel, String, String)
   * @see #populateRootPanel()
   * @see #showLogin()
   */
  @Override
  protected void populateBelowHeader(DivWidget verticalContainer/*, Panel contentRow*/) {
    RootPanel.get().clear();
    RootPanel.get().add(verticalContainer);
    /**
     * {@link #makeFlashContainer}
     */
    contentRow.add(lifecycleSupport.getFlashRecordPanel());
    lifecycleSupport.recordingModeSelect();
  //  LangTestDatabaseAsync service = GWT.create(LangTestDatabase.class);
    learnHelper = new AutoCRTChapterNPFHelper(controller);
    learnHelper.showContent(contentRow, "", true);
  }

  @Override
  public void onResize() {
    if (learnHelper != null) learnHelper.onResize();
  }

  /**
   * @see UILifecycle#gotUser
   * @see #configureUIGivenUser(long) (long)
   */
  @Override
  public void populateRootPanelIfLogin() {
//    logger.info("populateRootPanelIfLogin");
    if (!props.isOdaMode()) {
      int childCount = contentRow.getElement().getChildCount();
      // logger.info("populateRootAfterLogin root " + contentRow.getElement().getNodeName() + " childCount " + childCount);
      if (childCount > 0) {
        Node child = contentRow.getElement().getChild(0);
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
    DivWidget verticalContainer = getRootContainer();
    // header/title line
    // first row ---------------
    Panel firstRow = makeFirstTwoRows(verticalContainer);

    if (props.isOdaMode()) {
      headerRow.setVisible(false);
      populateBelowHeader(verticalContainer);
    } else if (!showLogin()) {
      populateBelowHeader(verticalContainer);
    }
  }

  /**
   * @param userID
   * @return
   * @see UILifecycle#gotUser
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
