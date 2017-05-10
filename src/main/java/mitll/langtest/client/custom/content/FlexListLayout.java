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

package mitll.langtest.client.custom.content;

import com.github.gwtbootstrap.client.ui.Affix;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Shell;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/28/2014.
 */
public abstract class FlexListLayout<T extends CommonShell, U extends Shell> implements RequiresResize {
  private final Logger logger = Logger.getLogger("FlexListLayout");

  //  private static final int RIGHT_SIDE_DIV_WIDTH = 70;
  public PagingExerciseList<T, U> npfExerciseList;
  private final ExerciseController controller;

  /**
   * @param controller
   * @see ReviewItemHelper#doInternalLayout(mitll.langtest.shared.custom.UserList, String)
   */
  public FlexListLayout(ExerciseController controller) {
    this.controller = controller;
  }

  /**
   * TODO : don't pass in user list
   *
   * @param uniqueID
   * @param instanceName
   * @param hasTopRow
   * @return
   * @see ReviewItemHelper#doInternalLayout(mitll.langtest.shared.custom.UserList, String)
   */
  public Panel doInternalLayout(long uniqueID, String instanceName, boolean hasTopRow) {
    Panel twoRows = hasTopRow ? new FlowPanel() : new DivWidget();
    twoRows.getElement().setId("FlexListLayout_twoRows");

    Panel exerciseListContainer = new SimplePanel();
    exerciseListContainer.addStyleName("floatLeftAndClear");
    exerciseListContainer.getElement().setId("FlexListLayout_exerciseListContainer");

    // second row ---------------
    Panel topRow = hasTopRow ? new FluidRow() : new FlowPanel("nav");
    topRow.getElement().setId("NPFHelper_" + (hasTopRow ? "topRow" : "leftSide"));
    if (!hasTopRow) {
      styleTopRow(twoRows, topRow);
    } else {
      twoRows.add(topRow);
    }

    DivWidget bottomRowDiv = new DivWidget();
    DivWidget listHeader = new DivWidget();
    if (!hasTopRow) {
      styleBottomRowDiv(bottomRowDiv, listHeader);
    }

    //  Panel bottomRow = new HorizontalPanel();
    Panel bottomRow = new DivWidget();
    bottomRow.add(exerciseListContainer);
    bottomRow.getElement().setId("NPFHelper_bottomRow");

    //  bottomRow.addStyleName("inlineFlex");
    styleBottomRow(bottomRow);
    if (!hasTopRow) bottomRow.setWidth("100%");

    bottomRowDiv.add(bottomRow);
//    ScrollPanel widgets1 = new ScrollPanel(bottomRowDiv);
    twoRows.add(bottomRowDiv);

    Panel currentExerciseVPanel = getCurrentExercisePanel();
    bottomRow.add(currentExerciseVPanel);

    DivWidget footer = getFooter(bottomRow);

    // TODO : only has to be paging b/c it needs to setUserListID
    PagingExerciseList<T, U> widgets = makeNPFExerciseList(topRow, currentExerciseVPanel, instanceName, uniqueID,
        listHeader, footer);
    npfExerciseList = widgets;

    addThirdColumn(bottomRow);

    if (npfExerciseList == null) {
      logger.warning("huh? exercise list is null for " + instanceName + " and " + uniqueID);
    } else {
      exerciseListContainer.add(npfExerciseList.getExerciseListOnLeftSide());
    }

    widgets.addWidgets();
    return twoRows;
  }

  boolean atTop = true;

  protected void styleTopRow(Panel twoRows, Panel topRow) {
    topRow.addStyleName("floatLeft");
    topRow.addStyleName("leftBlock");
    topRow.addStyleName("rightFiveMargin");

    twoRows.addStyleName("inlineFlex");

    FlowPanel section = new FlowPanel("section");
    section.addStyleName("sidebar");
    section.addStyleName("initialpos");

    twoRows.add(section);
    section.add(topRow);

    makeSureFacetsAlwaysVisible(section);
  }

  private void makeSureFacetsAlwaysVisible(FlowPanel section) {
    Window.addWindowScrollHandler(new Window.ScrollHandler() {
      @Override
      public void onWindowScroll(Window.ScrollEvent event) {
        //logger.info("got scroll " + event + " " + event.getScrollTop());

        boolean nowAtTop = event.getScrollTop() == 0;
        if (atTop != nowAtTop) {
          atTop = nowAtTop;
          if (!atTop) {
            section.removeStyleName("initialpos");
            section.addStyleName("scrolledpos");
          } else {
            section.addStyleName("initialpos");
            section.removeStyleName("scrolledpos");
          }
        }
      }
    });
  }

  private void styleBottomRowDiv(DivWidget bottomRowDiv, DivWidget listHeader) {
    bottomRowDiv.addStyleName("floatLeft");
    bottomRowDiv.addStyleName("mainBlock");
    bottomRowDiv.getElement().setId("rightSideDiv");
    //bottomRowDiv.setWidth(RIGHT_SIDE_DIV_WIDTH +    "%");
    // listHeader.addStyleName("listHeader");
    bottomRowDiv.add(listHeader);
  }

  @NotNull
  private DivWidget getFooter(Panel bottomRow) {
    DivWidget footer = new DivWidget();
    footer.getElement().setId("footer");
    bottomRow.add(footer);
    footer.setWidth("100%");
    footer.addStyleName("floatLeft");
    return footer;
  }

  protected Panel getCurrentExercisePanel() {
    FlowPanel currentExerciseVPanel = new FlowPanel();
    currentExerciseVPanel.getElement().setId("NPFHelper_defect_currentExercisePanel");
    currentExerciseVPanel.addStyleName("floatLeft");
    return currentExerciseVPanel;
  }

  protected void addThirdColumn(Panel bottomRow) {
  }

  protected void styleBottomRow(Panel bottomRow) {
    //bottomRow.setWidth("100%");
    //bottomRow.addStyleName("trueInlineStyle");
  }

  /**
   * @param topRow
   * @param currentExercisePanel
   * @param instanceName
   * @param userListID
   * @param listHeader
   * @param footer
   * @return
   * @see #doInternalLayout
   */
  private PagingExerciseList<T, U> makeNPFExerciseList(final Panel topRow,
                                                       Panel currentExercisePanel,
                                                       String instanceName,
                                                       long userListID,
                                                       DivWidget listHeader,
                                                       DivWidget footer) {
    final PagingExerciseList<T, U> exerciseList = makeExerciseList(topRow, currentExercisePanel, instanceName,
        listHeader, footer);
    exerciseList.setUserListID(userListID);

    exerciseList.setFactory(getFactory(exerciseList));
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        exerciseList.onResize();
      }
    });
    return exerciseList;
  }

  protected abstract PagingExerciseList<T, U> makeExerciseList(final Panel topRow,
                                                               Panel currentExercisePanel,
                                                               final String instanceName,
                                                               DivWidget listHeader, DivWidget footer);

  protected abstract ExercisePanelFactory<T, U> getFactory(final PagingExerciseList<T, U> exerciseList);

  @Override
  public void onResize() {
    if (npfExerciseList != null) {
      npfExerciseList.onResize();
    }
  }

  public ExerciseController getController() {
    return controller;
  }
}