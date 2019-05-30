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

package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.dialog.IListenView;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.scoring.EnglishDisplayChoices;
import mitll.langtest.client.scoring.TwoColumnExercisePanel;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.scoring.AlignmentOutput;

import java.util.HashMap;
import java.util.Map;

public class EditItem {
//  private final Logger logger = Logger.getLogger("EditItem");
  public static final String DONE = "OK";
  private final ExerciseController controller;
  private PagingExerciseList<CommonShell, ClientExercise> exerciseList;

  /**
   * @param controller
   * @see ContentEditorView#editList
   */
  public EditItem(ExerciseController controller) {
    this.controller = controller;
  }

  /**
   * @param originalList
   * @return
   * @see ContentEditorView#editList
   */
  public Panel editItem(UserList<CommonShell> originalList) {
    Panel hp = new DivWidget();
    hp.addStyleName("inlineFlex");
//    hp.getElement().setId("EditItem_for_" + originalList.getName());

    Panel pagerOnLeft = new SimplePanel();
    hp.add(pagerOnLeft);
    pagerOnLeft.addStyleName("rightFiveMargin");

    final Panel contentOnRight = new SimplePanel();
    contentOnRight.getElement().setId("EditItem_content");
    hp.add(contentOnRight);

    exerciseList = makeExerciseList(contentOnRight, originalList);
    pagerOnLeft.add(exerciseList.getExerciseListOnLeftSide());
    return hp;
  }

  private int userListID = -1;
  private String userListName = "";

  /**
   * @param right
   * @param originalList
   * @return
   * @paramz ul
   * @paramz includeAddItem
   * @see #editItem
   */
  private PagingExerciseList<CommonShell, ClientExercise> makeExerciseList(Panel right, UserList<CommonShell> originalList) {
    // logger.info("EditItem.makeExerciseList - ul = " + originalList);// + " " + includeAddItem);
    userListID = originalList.getID();
    userListName = originalList.getName();
    this.exerciseList = new EditableExerciseList(controller, right, INavigation.VIEWS.LISTS, originalList);
    exerciseList.addComponents();
    setFactory(this.exerciseList);
    this.exerciseList.setUnaccountedForVertical(280);   // TODO do something better here
    // logger.info("setting vertical on " +exerciseList.getElement().getExID());
    return this.exerciseList;
  }

  public void grabFocus() {
    ((EditableExerciseList) exerciseList).grabFocus();
  }
  public void removeHistoryListener() {
    ((EditableExerciseList) exerciseList).removeHistoryListener();
  }

  public void onResize() {
    if (exerciseList != null) {
      exerciseList.onResize();
    }
  }

  private void setFactory(final PagingExerciseList<CommonShell, ClientExercise> exerciseList) {
    exerciseList.setFactory(new ExercisePanelFactory<CommonShell, ClientExercise>(controller, exerciseList) {
      private final Map<Integer, AlignmentOutput> alignments = new HashMap<>();

      @Override
      public Panel getExercisePanel(ClientExercise exercise) {
        //  logger.info("getExercisePanel got " +exercise.getID() + " " + exercise.getEnglish() + " - " + exercise.getForeignLanguage() + " predef " +exercise.isPredefined());

        boolean allBlank = true;
        for (String value : exercise.getUnitToValue().values()) {
          if (!value.isEmpty()) {
            allBlank = false;
            break;
          }
        }
        if (exercise.getUnitToValue().isEmpty() || allBlank) {
          exercise.getUnitToValue().put("List", userListName);
        }
        //else
        // logger.info("unit " + exercise.getUnitToValue());

        if (exercise.isPredefined() || exercise.isContext()) {
          TwoColumnExercisePanel<ClientExercise> widgets = new TwoColumnExercisePanel<ClientExercise>(exercise,
              controller,
              exerciseList,
              alignments, true, new IListenView() {
            @Override
            public int getVolume() {
              return 100;
            }

            @Override
            public int getDialogSessionID() {
              return -1;
            }
          },
              () -> "");
          widgets.addWidgets(getFLChoice(), false, getPhoneChoices(), EnglishDisplayChoices.SHOW);
          return widgets;
        } else {
          // logger.info("getExercisePanel got " + exercise.getID() + " " + exercise.getEnglish() + " - " + exercise.getForeignLanguage() + " predef " + exercise.isPredefined());
          // List<ClientExercise> directlyRelated = exercise.getDirectlyRelated();
          // logger.info("getExercisePanel got #" + directlyRelated.size());
          // directlyRelated.forEach(clientExercise -> logger.info("Got " + clientExercise.getID() + " " + clientExercise.getForeignLanguage()));

          EditableExerciseDialog<CommonShell, ClientExercise> reviewEditableExercise = new ListEditableDialog(controller, exercise);
          Panel widgets = reviewEditableExercise.addFields(exerciseList, new SimplePanel());
          reviewEditableExercise.setFields(exercise);
          return widgets;
        }
      }
    });
  }

  public void reload() {
    exerciseList.reload(new HashMap<>());
  }

  private class ListEditableDialog extends EditableExerciseDialog<CommonShell, ClientExercise> {
    ListEditableDialog(ExerciseController controller, ClientExercise exercise) {
      super(controller, exercise, EditItem.this.userListID, INavigation.VIEWS.LISTS);
    }

    @Override protected void addItemsAtTop(Panel container) {
    //  logger.info("not adding items at top?");
    }

    @Override
    protected DivWidget getDominoEditInfo() {
      return null;
    }

    @Override
    public void setFields(ClientExercise newUserExercise) {
      super.setFields(newUserExercise);
      translit.setVisible(false);
    }

    @Override
    protected Panel getCreateButton(Panel toAddTo, ControlGroup normalSpeedRecording) {
      return null;
    }
  }
}
