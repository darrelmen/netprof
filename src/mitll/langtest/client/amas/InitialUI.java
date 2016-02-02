/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.Container;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.flashcard.Flashcard;


/**
 * Created by go22670 on 12/21/15.
 */
public class InitialUI {
  private final LangTest langTest;
  private final PropertyHandler props;
  private AutoCRTChapterNPFHelper learnHelper;


  private final LangTestDatabaseAsync service = GWT.create(LangTestDatabase.class);

  private final Flashcard flashcard;

  private Panel headerRow;
  private Panel firstRow;


  public InitialUI(LangTest langTest, Flashcard flashcard) {
    this.langTest = langTest;
    this.flashcard = flashcard;
    this.props = langTest.getProps();
  }

  /**
   * If in ODA Mode, wait until we start a QUIZ?
   * <p>
   * Then only show the content part of it...
   *
   * @return
   * @see #onModuleLoad2()
   * @see #showLogin
   * @see #populateRootAfterLogin()
   */
  void populateRootPanel() {
    Container verticalContainer = getRootContainer();
    // header/title line
    // first row ---------------
    Panel firstRow = makeFirstTwoRows(verticalContainer);

    if (props.isOdaMode()) {
      headerRow.setVisible(false);
      populateBelowHeader(verticalContainer, firstRow);
    } else if (!langTest.showLogin(verticalContainer, firstRow)) {
      populateBelowHeader(verticalContainer, firstRow);
    }
  }


  /**
   * @param verticalContainer
   * @return
   * @see #populateRootPanel()
   */
  private Panel makeFirstTwoRows(Container verticalContainer) {
    verticalContainer.add(headerRow = makeHeaderRow());
    headerRow.getElement().setId("headerRow");

    Panel firstRow = new DivWidget();
    verticalContainer.add(firstRow);
    this.firstRow = firstRow;
    firstRow.getElement().setId("firstRow");
    return firstRow;
  }

  /**
   * TODO : FIX ME
   * @return
   * @see #populateRootPanel()
   */
  private Panel makeHeaderRow() {
    headerRow = new FluidRow();
   // headerRow.add(new Column(12, flashcard.getNPFHeaderRow()));
    return headerRow;
  }

  /**
   *
   * @return
   */
  private Container getRootContainer() {
    RootPanel.get().clear();   // necessary?

    Container verticalContainer = new FluidContainer();
    verticalContainer.getElement().setId("root_vertical_container");
    return verticalContainer;
  }

  public int getHeightOfTopRows() {
    return headerRow.getOffsetHeight();
  }

  /**
   *    * TODO : FIX ME

   * @param verticalContainer
   * @param firstRow          where we put the flash permission window if it gets shown
   * @seex #handleCDToken(com.github.gwtbootstrap.client.ui.Container, com.google.gwt.user.client.ui.Panel, String, String)
   * @see #populateRootPanel()
   * @see #showLogin()
   */
  void populateBelowHeader(Container verticalContainer, Panel firstRow) {
    RootPanel.get().clear();
    RootPanel.get().add(verticalContainer);
    /**
     * {@link #makeFlashContainer}
     */
  //  firstRow.add(langTest.getFlashRecordPanel());
    langTest.modeSelect();


    //  TODO : FIX ME
    learnHelper = new AutoCRTChapterNPFHelper(service, langTest,
        //langTest.getUserManager(),
        null,
        langTest);
    learnHelper.addNPFToContent(firstRow, "autoCRT");
  }


  public void onResize() {
    if (learnHelper != null) learnHelper.onResize();
  }

  /**
   * @see mitll.langtest.client.user.UserManager#getPermissionsAndSetUser(int)
   * @see mitll.langtest.client.user.UserManager#login()
   */

  void showLogin() {
    populateRootPanel();
  }

  /**
   * @see #gotUser
   * @see #configureUIGivenUser(long) (long)
   */
  void populateRootAfterLogin() {
    if (!props.isOdaMode()) {
      int childCount = firstRow.getElement().getChildCount();
      // logger.info("populateRootAfterLogin root " + firstRow.getElement().getNodeName() + " childCount " + childCount);
      if (childCount > 0) {
        Node child = firstRow.getElement().getChild(0);
        Element as = Element.as(child);
        if (as.getId().contains("Login")) {
          //   logger.info("populateRootAfterLogin found login...");
          populateRootPanel();
        }
      }
    }
  }

  /**
   *  TODO : FIX ME
   */
  void configureUIGivenUser() {
    if (learnHelper != null && learnHelper.getExerciseList() != null) {
      //learnHelper.getExerciseList().restoreListFromHistory();
    }
  }
}
