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
 * © 2015-2018 Massachusetts Institute of Technology.
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

package mitll.langtest.client.custom.userlist;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.TextArea;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.Collection;
import java.util.List;

/**
 * Created by go22670 on 7/7/17.
 */
class ImportBulk {
  private static final int IMPORT_WIDTH = 500;
  private static final int VISIBLE_LINES = 20;
  private static final int CHARACTER_WIDTH = 150;
  private TextArea textArea;

  DivWidget showImportItem(String language) {
    DivWidget inner = new DivWidget();
    DivWidget upper = new DivWidget();
    String width = IMPORT_WIDTH + "px";
    upper.setWidth(width);
    textArea = new TextArea();
    textArea.setWidth(width);

    upper.add(textArea);
    inner.add(upper);
    textArea.setVisibleLines(VISIBLE_LINES);
    textArea.setCharacterWidth(CHARACTER_WIDTH);
    textArea.setPlaceholder("paste text here.");
    inner.add(new Heading(4, "Copy and paste tab separated lines with pairs of " + language + " item and its translation."));
    inner.add(new Heading(4, "(Quizlet tab-separated export format.)"));

/*    Button anImport = new Button("Import");
    anImport.addClickHandler(event -> doBulk(controller, ul));

    DivWidget bottom = new DivWidget();
    bottom.add(anImport);
    bottom.addStyleName("topFiveMargin");
    inner.add(bottom);*/
    return inner;
  }

  void doBulk(ExerciseController controller, UserList<CommonShell> ulid, ListContainer myLists) {
    controller.getListService().reallyCreateNewItems(ulid.getID(), sanitize(textArea.getText()),
        new AsyncCallback<List<CommonShell>>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(List<CommonShell> newExercises) {
            //logger.info("before " + ul.getExercises().size());
            //   for (CommonExercise exercise : newExercise) {
            //    ul.addExercise(exercise);
            // }
            //   tabPanel.selectTab(0);
            ///     editItem.reload();
//                showEditItem();
            //logger.info("after  " + ul.getExercises().size());
            //     reallyShowLearnTab(tabPanel, learnTab, ul, instanceName);

            int numItems = ulid.getNumItems();
            ulid.setExercises(newExercises);
            int after = ulid.getNumItems();
            if (after == numItems)
              new ModalInfoDialog("No changes", "No items were imported, check the format and try again.");
//            int numItems = currentSelection.getNumItems();
//            logger.info("editList : on " + currentSelection.getName() + " now " + numItems);

            myLists.flush();
            myLists.redraw();
          }
        });
  }

  private String sanitize(String text) {
    return SimpleHtmlSanitizer.sanitizeHtml(text).asString();
  }
}