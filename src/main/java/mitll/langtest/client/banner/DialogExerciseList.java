package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.banner.DialogViewHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.FacetExerciseList;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DialogExerciseList<T extends CommonShell & ScoredExercise> extends FacetExerciseList<T, IDialog> {
  //private DialogViewHelper dialogViewHelper;
  private final Logger logger = Logger.getLogger("DialogExerciseList");

  public DialogExerciseList(DialogViewHelper dialogViewHelper,
                            Panel topRow, Panel currentExercisePanel, String instanceName, DivWidget listHeader,
                            ExerciseController controller) {
    super(topRow, currentExercisePanel, controller, new ListOptions(instanceName), listHeader, false);
    //this.dialogViewHelper = dialogViewHelper;
  }

  protected void getTypeToValues(Map<String, String> typeToSelection, int userListID) {
    if (!isThereALoggedInUser()) return;

    //List<Pair> pairs = getPairs(typeToSelection);
    //  logger.info("getTypeToValues request " + pairs + " list " + userListID);

    final long then = System.currentTimeMillis();

    controller.getDialogService().getTypeToValues(getFilterRequest(userListID, getPairs(typeToSelection)),
        new AsyncCallback<FilterResponse>() {
          @Override
          public void onFailure(Throwable caught) {
            if (caught instanceof DominoSessionException) {
              logger.info("getTypeToValues : got " + caught);
            }
            controller.handleNonFatalError(GETTING_TYPE_VALUES, caught);
          }

          /**
           * fixes downstream selections that no longer make sense.
           * @param response
           */
          @Override
          public void onSuccess(FilterResponse response) {
            gotFilterResponse(response, then, typeToSelection);
          }
        });
  }

  protected void getExerciseIDs(Map<String, Collection<String>> typeToSection,
                                String prefix,
                                int exerciseID,
                                ExerciseListRequest request) {
    waitCursorHelper.scheduleWaitTimer();

    logger.info("getExerciseIDs " +
        "\n\trequest " + request +
        "\n\t ex     " + exerciseID + " type " + typeToSection);

    if (controller.getUser() > 0) {
      controller.getDialogService().getDialogs(request,
          new AsyncCallback<ExerciseListWrapper<IDialog>>() {
            @Override
            public void onFailure(Throwable caught) {
              waitCursorHelper.showFinished();

            }

            @Override
            public void onSuccess(ExerciseListWrapper<IDialog> result) {
              waitCursorHelper.showFinished();
              logger.info("got back "+ result.getExercises().size());
            }
          });
    }

//    if (controller.getUser() > 0) {
//      // final long then = System.currentTimeMillis();
//      service.getExerciseIds(
//          request,
//          new SetExercisesCallback(userListID + "_" + typeToSection.toString(), prefix, exerciseID, request));
//    }
  }

  @Override
  protected void getFullExercises(Collection<Integer> visibleIDs, int currentReq, Collection<Integer> requested, List<IDialog> alreadyFetched) {
    logger.info("getFullExercises " + visibleIDs);
    controller.getDialogService().getDialogs(new ExerciseListRequest(),
        new AsyncCallback<ExerciseListWrapper<IDialog>>() {
          @Override
          public void onFailure(Throwable caught) {

          }

          @Override
          public void onSuccess(ExerciseListWrapper<IDialog> result) {

          }
        });
  }

  @Override
  protected void goGetNextPage() {

  }
}
