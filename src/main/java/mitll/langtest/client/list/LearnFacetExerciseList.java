package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseListWrapper;
import mitll.langtest.shared.exercise.ScoredExercise;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Full exercises returns CommonExercise - so we're kinda stuck with it...
 *
 * @param <T>
 */
public class LearnFacetExerciseList<T extends CommonShell & ScoredExercise> extends FacetExerciseList<T, ClientExercise> {
  private final Logger logger = Logger.getLogger("FacetExerciseList");

  private static final boolean DEBUG = false;

  public LearnFacetExerciseList(Panel secondRow,
                                Panel currentExerciseVPanel,
                                ExerciseController controller,
                                ListOptions listOptions,
                                DivWidget listHeader,
                                boolean isDrillView) {
    super(secondRow, currentExerciseVPanel, controller, listOptions, listHeader, isDrillView);
  }

  protected void getFullExercises(Collection<Integer> visibleIDs,
                                  int currentReq,
                                  Collection<Integer> requested,
                                  List<ClientExercise> alreadyFetched) {
    long then = System.currentTimeMillis();

    if (DEBUG) {
      logger.info("getFullExercises" +
          "\n\trequest   " + visibleIDs.size() + " visible ids : " + visibleIDs +
          "\n\trequested " + requested +
          "\n\talready   " + getIDs(alreadyFetched));
    }

    service.getFullExercises(currentReq,
        requested,
        new AsyncCallback<ExerciseListWrapper<ClientExercise>>() {
          @Override
          public void onFailure(Throwable caught) {
            fullExerciseFailure(caught);
          }

          @Override
          public void onSuccess(final ExerciseListWrapper<ClientExercise> result) {
            if (DEBUG) {
              logger.info("getFullExercises onSuccess " + visibleIDs.size() + " visible ids : " + visibleIDs);
            }

            if (result.getExercises() != null) {
              long now = System.currentTimeMillis();
              int size = result.getExercises().isEmpty() ? 0 : result.getExercises().size();
              if (now - then > 150 || DEBUG) {
                logger.info("getFullExercises took " + (now - then) + " to get " + size + " exercises");
              }

              getFullExercisesSuccess(result, alreadyFetched, visibleIDs);
            } else {
              logger.warning("getFullExercises huh? no exercises from " + requested);
            }
          }
        });
  }

  protected void goGetNextPage() {
    Set<Integer> toAskFor = getNextPageIDs();
    if (toAskFor.isEmpty()) {
      //    logger.info("goGetNextPage already has cached total " + fetched.size());
    } else {
      long then = System.currentTimeMillis();
      //     logger.info("goGetNextPage toAskFor " + toAskFor.size() + " exercises.");
      service.getFullExercises(-1, toAskFor,
          new AsyncCallback<ExerciseListWrapper<ClientExercise>>() {
            @Override
            public void onFailure(Throwable caught) {
              fullExerciseFailure(caught);
            }

            @Override
            public void onSuccess(final ExerciseListWrapper<ClientExercise> result) {
              if (result.getExercises() != null) {
                long now = System.currentTimeMillis();
                int size = result.getExercises().isEmpty() ? 0 : result.getExercises().size();
                if (now - then > 150 || DEBUG) {
                  logger.info("getFullExercisesSuccess took " + (now - then) + " to get " + size + " exercises");
                }
                List<ClientExercise> exercises = result.getExercises();


                setScoreHistory(result.getScoreHistoryPerExercise(), exercises);
                result.getExercises().forEach(ex -> addExerciseToCached(ex));
              } else {
                logger.warning("getFullExercises huh? no exercises");
              }
            }
          });

    }
  }
}
