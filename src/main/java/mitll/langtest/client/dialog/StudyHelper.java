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

package mitll.langtest.client.dialog;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.banner.IBanner;
import mitll.langtest.client.banner.LearnHelper;
import mitll.langtest.client.banner.NewContentChooser;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.custom.content.FlexListLayout;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.list.StudyExerciseList;
import mitll.langtest.shared.dialog.DialogSession;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ScoredExercise;
import org.jetbrains.annotations.NotNull;

/**
 * Created by go22670 on 4/5/17.
 */
public class StudyHelper<T extends CommonShell & ScoredExercise> extends LearnHelper<T> {
  private int dialogSessionID = -1;

  /**
   * @param controller
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  public StudyHelper(ExerciseController controller) {
    super(controller);
  }

  @Override
  public void showContent(Panel listContent, INavigation.VIEWS views) {
    int dialogFromURL = getDialogFromURL();
    controller.getDialogService().getDialog(dialogFromURL, new AsyncCallback<IDialog>() {
      @Override
      public void onFailure(Throwable caught) {
        // TODO fill in
      }

      @Override
      public void onSuccess(IDialog dialog) {
        showDialogGetRef(dialogFromURL, dialog, listContent);
      }
    });
  }

  private void showDialogGetRef(int dialogFromURL, IDialog dialog, Panel child) {
    if (dialog == null) {
      child.add(new HTML("hmm can't find dialog #" + dialogFromURL + " in database?"));
    } else {
      child.add(getHeader(dialog));

      controller.getDialogService().addSession(new DialogSession(controller.getUser(),
          getProjectid(), dialog.getID(), INavigation.VIEWS.STUDY), new AsyncCallback<Integer>() {
        @Override
        public void onFailure(Throwable caught) {
          controller.handleNonFatalError("creating new dialog study session", caught);
        }

        @Override
        public void onSuccess(Integer result) {
          dialogSessionID = result;
        }
      });
    }
    super.showContent(child, INavigation.VIEWS.STUDY);
    hideList();
  }

  @NotNull
  private DivWidget getHeader(IDialog dialog) {
    DivWidget header = new DialogHeader(controller, INavigation.VIEWS.STUDY, getPrevView(), getNextView()).getHeader(dialog);
    header.addStyleName("bottomFiveMargin");
    return header;
  }

  private int getProjectid() {
    return controller.getProjectStartupInfo().getProjectid();
  }

  @NotNull
  private INavigation.VIEWS getPrevView() {
    return INavigation.VIEWS.DIALOG;
  }

  @NotNull
  private INavigation.VIEWS getNextView() {
    return INavigation.VIEWS.LISTEN;
  }

  private int getDialogFromURL() {
    return new SelectionState().getDialog();
  }

  @Override
  public int getDialogSessionID() {
    return dialogSessionID;
  }

  @Override
  protected FlexListLayout<T, ClientExercise> getMyListLayout(SimpleChapterNPFHelper<T, ClientExercise> outer) {
    return new MyFlexListLayout<T, ClientExercise>(controller, outer) {
      /**
       * @see FlexListLayout#makeNPFExerciseList
       * @param topRow
       * @param currentExercisePanel
       * @param instanceName
       * @param listHeader
       * @param footer
       * @return
       */
      @Override
      protected PagingExerciseList<T, ClientExercise> makeExerciseList(Panel topRow,
                                                                       Panel currentExercisePanel,
                                                                       INavigation.VIEWS instanceName,
                                                                       DivWidget listHeader,
                                                                       DivWidget footer) {
        StudyExerciseList<T> widgets = new StudyExerciseList<>(
            topRow,
            currentExercisePanel,
            controller,
            new ListOptions(instanceName),
            listHeader
        );
        widgets.hideSectionPanel();
        return widgets;
      }
    };
  }
}
