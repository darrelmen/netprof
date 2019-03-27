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

package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.analysis.MemoryItemContainer;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.userlist.TableAndPager;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.shared.exercise.OOV;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class OOVViewHelper extends TableAndPager implements ContentView {
  private final Logger logger = Logger.getLogger("OOVViewHelper");
  public static final String EQUIVALENT = "Equivalent";

  private final ExerciseController controller;

  OOVViewHelper(ExerciseController controller, INavigation.VIEWS oovEditor) {
    this.controller = controller;
  }

  @Override
  public void showContent(Panel listContent, INavigation.VIEWS instanceName) {
    listContent.clear();

    DivWidget outer = new DivWidget() {
      @Override
      protected void onLoad() {
        super.onLoad();
        grabFocus();
      }

      @Override
      protected void onUnload() {
        super.onUnload();

        logger.info("Got unload!!!");
        logger.info("Got unload!!!");
        logger.info("Got unload!!!");
      }
    };

    listContent.add(outer);
    addVisited(outer);
//    grabFocus();
//    listContent.add(new MemoryItemContainer());
  }

  private MemoryItemContainer<OOV> listContainer;

  private void addVisited(DivWidget top) {
    listContainer = addVisitedTable(top);

    controller.getScoringService().getOOVs(controller.getProjectID(), new AsyncCallback<List<OOV>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getting lists visited by user", caught);
      }

      @Override
      public void onSuccess(List<OOV> result) {
        listContainer.populateTable(result);
        if (!result.isEmpty()) {
          listContainer.gotClickOnItem(result.get(0));
        }
      }
    });
  }

  private HTML oov;
  private TextBox equivalent;

  private MemoryItemContainer<OOV> addVisitedTable(DivWidget top) {
    MemoryItemContainer<OOV> oovMemoryItemContainer = new OOVMemoryItemContainer();
    Panel tableWithPager =
        getTableWithPager(top, oovMemoryItemContainer, "OOV", "Select and enter equivalent.",
            Placement.TOP);

//    tableWithPager.setHeight(VISITED_HEIGHT + "px");

    DivWidget ldButtons = new DivWidget();
    {
      ldButtons.addStyleName("inlineFlex");
      ldButtons.addStyleName("topFiveMargin");
      oov = new HTML();
      oov.setWidth("100px");
      ldButtons.add(oov);
      equivalent = new TextBox();
      equivalent.addBlurHandler(event -> gotBlur(currentOOV));
      equivalent.setVisibleLength(100);
      equivalent.addStyleName("leftFiveMargin");
      ldButtons.add(equivalent);

      equivalent.addKeyUpHandler(event -> {
        //  logger.info("getTypeahead got key up "+ quickAddText.getText());
        checkForKeyUpDown(event, oovMemoryItemContainer);
      });
//      ldButtons.add(getRemoveVisitorButton(listContainer));
//      addDrillAndLearn(ldButtons, listContainer);
    }


    oovMemoryItemContainer.getCellTable().addDomHandler(event -> checkForKeyUpDown(event, oovMemoryItemContainer), KeyUpEvent.getType());


    top.add(ldButtons);
    return oovMemoryItemContainer;
  }

  private void gotBlur(OOV currentOOV) {
    final String text = equivalent.getText();
    logger.info("got " + text);
    controller.getScoringService().isValidForeignPhrase(controller.getProjectID(),
        text, "", new AsyncCallback<Collection<String>>() {
          @Override
          public void onFailure(Throwable caught) {

          }

          @Override
          public void onSuccess(Collection<String> result) {
            if (result.isEmpty()) {
              currentOOV.setEquivalent(text);
              listContainer.redraw();
            }
          }
        });
  }

  public void grabFocus() {
    Scheduler.get().scheduleDeferred((Command) () -> equivalent.setFocus(true));
  }

  private void checkForKeyUpDown(KeyUpEvent event, MemoryItemContainer<OOV> oovMemoryItemContainer) {
    // arrow up down Paul suggestion.
    int keyCode = event.getNativeEvent().getKeyCode();

    int index = oovMemoryItemContainer.getIndex(oovMemoryItemContainer.getCurrentSelection());
    if (keyCode == 40) {  // down
      gotBlur(currentOOV);
      goToNext(oovMemoryItemContainer, index);
    } else if (keyCode == 38) {
      gotBlur(currentOOV);
      OOV at;
      if (index == 0) {
        at = oovMemoryItemContainer.getAt(oovMemoryItemContainer.getSize() - 1);
      } else {
        at = oovMemoryItemContainer.getAt(--index);
      }
      oovMemoryItemContainer.markCurrentExercise(at.getID());
      oovMemoryItemContainer.gotClickOnItem(at);
    } else if (keyCode == 13) {
    //  logger.info("got return " + keyCode);
      gotBlur(currentOOV);
      goToNext(oovMemoryItemContainer, index);
    }
    else {
      //logger.info("got keyCode " + keyCode);
    }
  }

  private void goToNext(MemoryItemContainer<OOV> oovMemoryItemContainer, int index) {
    OOV at;
    if (index == oovMemoryItemContainer.getSize() - 1) {
      at = oovMemoryItemContainer.getAt(0);
    } else {
      at = oovMemoryItemContainer.getAt(++index);
    }
    oovMemoryItemContainer.markCurrentExercise(at.getID());
    oovMemoryItemContainer.gotClickOnItem(at);
  }

  private OOV currentOOV;

  private class OOVMemoryItemContainer extends MemoryItemContainer<OOV> {


    OOVMemoryItemContainer() {
      super(OOVViewHelper.this.controller, "oov", "OOV", 10, 10);
    }

    @Override
    public void gotClickOnItem(OOV user) {
      super.gotClickOnItem(user);
      oov.setText(user.getOOV());
      equivalent.setText(user.getEquivalent());
      currentOOV = user;
    }

    /**
     * @see SimplePagingContainer#configureTable(boolean)
     */
    @Override
    protected void addColumnsToTable() {
      List<OOV> list = getList();
      addItemID(list, getMaxLengthId());
      addEquivalent(list, 100);
    }

    /**
     * @param list
     * @param maxLength
     */
    private void addEquivalent(List<OOV> list, int maxLength) {
      Column<OOV, SafeHtml> userCol = getEquivColumn(maxLength);
      table.setColumnWidth(userCol, getIdWidth() + "px");
      addColumn(userCol, new TextHeader(EQUIVALENT));
      table.addColumnSortHandler(getSorter(userCol, list));
    }

    private Column<OOV, SafeHtml> getEquivColumn(int maxLength) {
      return getTruncatedCol(maxLength, this::getEquivValue);
    }

    private ColumnSortEvent.ListHandler<OOV> getSorter(Column<OOV, SafeHtml> englishCol,
                                                       List<OOV> dataList) {
      ColumnSortEvent.ListHandler<OOV> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
      columnSortHandler.setComparator(englishCol, this::getEquivCompare);
      return columnSortHandler;
    }

    private int getEquivCompare(OOV o1, OOV o2) {
      int i = o1.getEquivalent().compareTo(o2.getEquivalent());
      return i == 0 ? o1.getOOV().compareTo(o2.getOOV()) : i;
    }

    private String getEquivValue(OOV thing) {
      return thing.getEquivalent();
    }

    @Override
    protected int getIDCompare(OOV o1, OOV o2) {
      return Integer.compare(o1.getID(), o2.getID());
    }

    @Override
    protected int getDateCompare(OOV o1, OOV o2) {
      return Long.compare(o1.getModified(), o2.getModified());
    }

    @Override
    protected String getItemLabel(OOV shell) {
      return shell.getOOV();
    }

    @Override
    protected Long getItemDate(OOV shell) {
      return shell.getModified();
    }
  }
}
