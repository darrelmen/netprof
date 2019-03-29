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

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
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
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.exercise.OOV;
import mitll.langtest.shared.project.OOVInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OOVViewHelper extends TableAndPager implements ContentView {
  private final Logger logger = Logger.getLogger("OOVViewHelper");

  public static final String CHECK_COMPLETE = "Check complete.";
  public static final String PLEASE_WAIT = "Please wait...";
  public static final String CHECK_ITEMS_IN_DICT = "Check Items In Dict.";
  public static final String SELECT_AND_ENTER_EQUIVALENT = "Select and enter equivalent.";

  private static final String TITLE = "Words Not In Dictionary";
  private static final String EQUIVALENT = "Equivalent";

  private final ExerciseController controller;
  private MemoryItemContainer<OOV> oovContainer;
  private HTML oov;
  private TextBox equivalent;
  private final HTML message = new HTML();
  private Button checkButton;

  OOVViewHelper(ExerciseController controller, INavigation.VIEWS oovEditor) {
    this.controller = controller;
  }

  @Override
  public void showContent(Panel listContent, INavigation.VIEWS instanceName) {
    listContent.clear();

    DivWidget outer = new DivWidget() {
//      @Override
//      protected void onLoad() {
//        super.onLoad();
//        //   grabFocus();
//      }

      @Override
      protected void onUnload() {
        super.onUnload();
        List<OOV> dirtyItems = getDirtyItems();
        if (!dirtyItems.isEmpty()) {
          controller.getAudioService().updateOOV(controller.getProjectID(), dirtyItems, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
              message.setText("Error - please try again.");
              controller.handleNonFatalError("updateOOV...", caught);
            }

            @Override
            public void onSuccess(Void result) {

            }
          });
        }
      }
    };

    listContent.add(outer);
    addOOVList(outer);
  }

  private void addOOVList(DivWidget top) {
    top.add(new HTML("Getting words that are not in the dictionary. Please wait..."));
    int projectID = controller.getProjectID();

    controller.getAudioService().checkOOV(projectID, new AsyncCallback<OOVInfo>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("Checking OOV...", caught);
      }

      @Override
      public void onSuccess(OOVInfo oovInfo) {

        logger.info("addOOVList Got " + oovInfo);
        controller.getScoringService().getOOVs(projectID, new AsyncCallback<List<OOV>>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("getting oovs", caught);
          }

          @Override
          public void onSuccess(List<OOV> result) {
            top.clear();
            oovContainer = addOOVTable(top, oovInfo);
            sortUnsetToTop(result);
            oovContainer.populateTable(result);
            if (!result.isEmpty()) {
              oovContainer.gotClickOnItem(result.get(0));
            }
            grabFocus();
          }
        });
      }
    });
  }

  private void sortUnsetToTop(List<OOV> result) {
    result.sort((o1, o2) -> {
      boolean o1Empty = o1.getEquivalent().isEmpty();
      boolean o2Empty = o2.getEquivalent().isEmpty();
      if (o1Empty && !o2Empty) {
        return -1;
      } else if (o2Empty && !o1Empty) {
        return +1;
      } else {
        return o1.compareTo(o2);
      }
    });
  }

  private void grabFocus() {
    Scheduler.get().scheduleDeferred((Command) () -> equivalent.setFocus(true));
  }

  UnsafeItems unsafeItems;
  private MemoryItemContainer<OOV> addOOVTable(DivWidget top, OOVInfo oovInfo) {
    MemoryItemContainer<OOV> oovMemoryItemContainer = new OOVMemoryItemContainer();

    DivWidget leftRight = new DivWidget();
    leftRight.addStyleName("inlineFlex");
    top.add(leftRight);

    DivWidget left = new DivWidget();
    leftRight.add(left);
    getTableWithPager(left, oovMemoryItemContainer, TITLE, SELECT_AND_ENTER_EQUIVALENT,
        Placement.TOP).setWidth("450px");

    DivWidget right = new DivWidget();
    right.addStyleName("leftFiveMargin");
    leftRight.add(right);
    unsafeItems = new UnsafeItems();
    getTableWithPager(right,
        unsafeItems, "Items with missing words", "example items",
        Placement.TOP).setWidth(550 +
        "px");

//    logger.info("showing " + toShow.size());

    unsafeItems.populateTable(getWrappers(oovInfo));

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

      equivalent.addKeyUpHandler(event -> checkForKeyUpDown(event, oovMemoryItemContainer));
    }

    oovMemoryItemContainer.getCellTable().addDomHandler(event -> checkForKeyUpDown(event, oovMemoryItemContainer), KeyUpEvent.getType());

    left.add(ldButtons);

    {
      DivWidget buttonRow = new DivWidget();
      left.add(buttonRow);
      buttonRow.add(checkButton = getCheckButton());
    }
    {
      DivWidget buttonRow = new DivWidget();
      left.add(buttonRow);
      buttonRow.add(message);
    }
    return oovMemoryItemContainer;
  }

  @NotNull
  private List<Wrapper> getWrappers(OOVInfo oovInfo) {
    List<Wrapper> toShow = new ArrayList<>();
    logger.info("got " + oovInfo.getUnsafe().size());
    oovInfo.getUnsafe().forEach(item -> toShow.add(new Wrapper(toShow.size(), item)));
    logger.info("made " + toShow.size());
    return toShow;
  }

  @NotNull
  private Button getCheckButton() {
    final Button add = new Button(CHECK_ITEMS_IN_DICT, IconType.CHECK);
    add.setEnabled(false);
    add.addStyleName("leftFiveMargin");
    add.addClickHandler(event -> gotCheck(add));
    add.setType(ButtonType.SUCCESS);
    return add;
  }

  private void gotCheck(Button add) {
    int projectID = controller.getProjectID();
    add.setEnabled(false);
    message.setText(PLEASE_WAIT);

    updateAndCheck(projectID, getDirtyItems());
  }

  private void updateAndCheck(  int projectID, List<OOV> items) {
    controller.getAudioService().updateOOV(projectID, items, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        message.setText("Error - please try again.");
        controller.handleNonFatalError("updateOOV...", caught);
      }

      @Override
      public void onSuccess(Void result) {
        checkOOVAgain(projectID);
      }
    });
  }

  @NotNull
  private List<OOV> getDirtyItems() {
    return oovContainer.getItems().stream().filter(OOV::isDirty).collect(Collectors.toList());
  }

  private void checkOOVAgain(int projectID) {
    message.setText("Checking dictionary again, please wait...");
    controller.getAudioService().checkOOV(projectID, new AsyncCallback<OOVInfo>() {
      @Override
      public void onFailure(Throwable caught) {
        // if (add != null) add.setEnabled(true);
        controller.handleNonFatalError("Checking OOV...", caught);
      }

      @Override
      public void onSuccess(OOVInfo result) {
//        if (add != null) add.setEnabled(true);
        unsafeItems.populateTable(getWrappers(result));

        if (result.isNeedsReload()) {
          controller.getExerciseService().reload(projectID, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
              controller.handleNonFatalError("updating exercises...", caught);
            }

            @Override
            public void onSuccess(Void result) {
              logger.info("update complete...");
            }
          });
        }
        controller.getScoringService().getOOVs(projectID, new AsyncCallback<List<OOV>>() {
          @Override
          public void onFailure(Throwable caught) {
            message.setText("Error - please try again.");
            controller.handleNonFatalError("getting oovs", caught);
          }

          @Override
          public void onSuccess(List<OOV> result) {
            message.setText(CHECK_COMPLETE);
          }
        });
      }
    });
  }

  private void gotBlur(OOV currentOOV) {
    final String text = equivalent.getText();
    // logger.info("got " + text);
    controller.getScoringService().isValidForeignPhrase(controller.getProjectID(),
        text, "", new AsyncCallback<Collection<String>>() {
          @Override
          public void onFailure(Throwable caught) {

          }

          @Override
          public void onSuccess(Collection<String> result) {
            if (result.isEmpty()) {
              currentOOV.setEquivalent(text);
              oovContainer.redraw();
              checkButton.setEnabled(!getDirtyItems().isEmpty());
            }
          }
        });
  }

  /**
   * @param event
   * @param oovMemoryItemContainer
   */
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
    } else {
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

  private static class Wrapper implements HasID {
    int id;
    private String value;

    public Wrapper() {
    }

    public Wrapper(int id, String value) {
      this.id = id;
      this.value = value;
    }

    @Override
    public int getID() {
      return 0;
    }

    @Override
    public int compareTo(@NotNull HasID o) {
      return 0;
    }

    public String getValue() {
      return value;
    }
  }

  private class UnsafeItems extends MemoryItemContainer<Wrapper> {
    UnsafeItems() {
      super(OOVViewHelper.this.controller, "unsafe", "Items with Missing Words", 10, 10);
    }

//    @Override
//    public void gotClickOnItem(OOV user) {
//      super.gotClickOnItem(user);
//      oov.setText(user.getOOV());
//      equivalent.setText(user.getEquivalent());
//      currentOOV = user;
//    }

    /**
     * @see SimplePagingContainer#configureTable(boolean)
     */
    @Override
    protected void addColumnsToTable() {
      List<Wrapper> list = getList();
      addItemID(list, getMaxLengthId());
      //addEquivalent(list, 100);
    }

    @Override
    protected Column<Wrapper, SafeHtml> getTruncatedCol(int maxLength, GetSafe<Wrapper> getSafe) {
      Column<Wrapper, SafeHtml> column = new Column<Wrapper, SafeHtml>(new ClickableCell()) {
        @Override
        public void onBrowserEvent(Cell.Context context, Element elem, Wrapper object, NativeEvent event) {
          super.onBrowserEvent(context, elem, object, event);
          checkGotClick(object, event);
        }

        @Override
        public SafeHtml getValue(Wrapper shell) {
          return getNoWrapContent(shell.getValue());
        }
      };
      column.setSortable(true);

      return column;
    }

    @Override
    protected int getIDCompare(Wrapper o1, Wrapper o2) {
      return Integer.compare(o1.getID(), o2.getID());
    }

    @Override
    protected int getDateCompare(Wrapper o1, Wrapper o2) {
      return 0;
    }

    @Override
    protected String getItemLabel(Wrapper shell) {
      return shell.getValue();
    }

    @Override
    protected Long getItemDate(Wrapper shell) {
      return 0L;
    }
  }
}
