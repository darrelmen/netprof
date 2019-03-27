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

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.analysis.MemoryItemContainer;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.userlist.TableAndPager;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.exercise.OOV;

import java.util.List;

public class OOVViewHelper extends TableAndPager implements ContentView {
  ExerciseController controller;

  public OOVViewHelper(ExerciseController controller, INavigation.VIEWS oovEditor) {
    this.controller = controller;
  }

  @Override
  public void showContent(Panel listContent, INavigation.VIEWS instanceName) {
    listContent.clear();

    DivWidget outer = new DivWidget();
    listContent.add(outer);
    addVisited(outer);
//    listContent.add(new MemoryItemContainer());
  }

  private void addVisited(DivWidget top) {
    MemoryItemContainer<OOV> listContainer = addVisitedTable(top);

    controller.getScoringService().getOOVs(controller.getProjectID(), new AsyncCallback<List<OOV>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getting lists visited by user", caught);
      }

      @Override
      public void onSuccess(List<OOV> result) {
        listContainer.populateTable(result);
      }
    });
  }

  private MemoryItemContainer<OOV> addVisitedTable(DivWidget top) {

    MemoryItemContainer<OOV> oovMemoryItemContainer = new MemoryItemContainer<OOV>(controller,
        "oov", "OOV", 10, 10) {
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
    };

    Panel tableWithPager = getTableWithPager(top, oovMemoryItemContainer, "OOV", "Select and enter equivalent.");


//    tableWithPager.setHeight(VISITED_HEIGHT + "px");

    DivWidget ldButtons = new DivWidget();
    {
      ldButtons.addStyleName("topFiveMargin");
//      ldButtons.add(getRemoveVisitorButton(listContainer));
//      addDrillAndLearn(ldButtons, listContainer);
    }
    top.add(ldButtons);
    return oovMemoryItemContainer;
  }
}
