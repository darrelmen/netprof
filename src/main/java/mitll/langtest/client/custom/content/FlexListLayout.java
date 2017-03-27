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

import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Shell;

import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/28/2014.
 */
public abstract class FlexListLayout<T extends CommonShell, U extends Shell> implements RequiresResize {
  private final Logger logger = Logger.getLogger("FlexListLayout");

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
    twoRows.getElement().setId("NPFHelper_twoRows");

    Panel exerciseListContainer = new SimplePanel();
    exerciseListContainer.addStyleName("floatLeftAndClear");
    exerciseListContainer.getElement().setId("NPFHelper_exerciseListContainer");

    // second row ---------------
    Panel topRow = hasTopRow ? new FluidRow() : new FlowPanel("nav");
    topRow.getElement().setId("NPFHelper_" + (hasTopRow ? "topRow" : "leftSide"));
    if (!hasTopRow) {
      topRow.addStyleName("floatLeft");
      topRow.addStyleName("leftBlock");
      topRow.addStyleName("rightFiveMargin");

      FlowPanel section = new FlowPanel("section");
      section.addStyleName("sidebar");

      twoRows.add(section);
      section.add(topRow);
    } else {
      twoRows.add(topRow);
    }

    DivWidget bottomRowDiv = new DivWidget();
    DivWidget listHeader = new DivWidget();
    if (!hasTopRow) {
      bottomRowDiv.addStyleName("floatLeft");
      bottomRowDiv.addStyleName("mainBlock");
      bottomRowDiv.getElement().setId("rightSideDiv");

     // listHeader.addStyleName("listHeader");
      bottomRowDiv.add(listHeader);
    }

  //  Panel bottomRow = new HorizontalPanel();
    Panel bottomRow = new DivWidget();
    bottomRow.add(exerciseListContainer);
    bottomRow.getElement().setId("NPFHelper_bottomRow");
    styleBottomRow(bottomRow);

    bottomRowDiv.add(bottomRow);
    twoRows.add(bottomRowDiv);

    Panel currentExerciseVPanel = getCurrentExercisePanel();
    bottomRow.add(currentExerciseVPanel);

    // TODO : only has to be paging b/c it needs to setUserListID
    PagingExerciseList<T, U> widgets = makeNPFExerciseList(topRow, currentExerciseVPanel, instanceName, uniqueID,
        listHeader);
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
   * @return
   * @see #doInternalLayout
   */
  private PagingExerciseList<T, U> makeNPFExerciseList(final Panel topRow,
                                                       Panel currentExercisePanel,
                                                       String instanceName,
                                                       long userListID,
                                                       DivWidget listHeader) {
    final PagingExerciseList<T, U> exerciseList = makeExerciseList(topRow, currentExercisePanel, instanceName, listHeader);
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
                                                               DivWidget listHeader);

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