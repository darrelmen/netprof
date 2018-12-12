package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.exercise.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * A facet exercise list that gets client exercises for those that are currently visible.
 *
 * Full exercises returns ClientExercise
 *
 * @param <T>
 */
public class ClientExerciseFacetExerciseList<T extends CommonShell & ScoredExercise>
    extends FacetExerciseList<T, ClientExercise> {
  private final Logger logger = Logger.getLogger("ClientExerciseFacetExerciseList");

  private static final boolean DEBUG = false;

  /**
   * @param secondRow
   * @param currentExerciseVPanel
   * @param controller
   * @param listOptions
   * @param listHeader
   * @param views
   * @see mitll.langtest.client.banner.LearnHelper#getMyListLayout(SimpleChapterNPFHelper)
   */
  public ClientExerciseFacetExerciseList(Panel secondRow,
                                         Panel currentExerciseVPanel,
                                         ExerciseController controller,
                                         ListOptions listOptions,
                                         DivWidget listHeader,
                                         INavigation.VIEWS views) {
    super(secondRow, currentExerciseVPanel, controller, listOptions, listHeader, views);
  }

  /**
   * @param visibleIDs
   * @param currentReq
   * @param requested
   * @param alreadyFetched
   * @see #reallyGetExercises
   */
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

    service.getFullExercises(getExerciseListRequest("").setReqID(currentReq),
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

              //    result.getExercises().forEach(ex->logger.info("got " + ex.getID() + " " + ex.getEnglish() + " " + ex.getForeignLanguage()));
              getFullExercisesSuccess(result, alreadyFetched, visibleIDs);
            } else {
              logger.warning("getFullExercises huh? no exercises from " + requested);
            }
          }
        });
  }

  /**
   * @param result
   * @param alreadyFetched
   * @param visibleIDs
   * @see #reallyGetExercises
   */
  private void getFullExercisesSuccess(ExerciseListWrapper<ClientExercise> result,
                                       List<ClientExercise> alreadyFetched,
                                       Collection<Integer> visibleIDs) {
    // long now = System.currentTimeMillis();
//    int size = result.getVisibleExercises().isEmpty() ? 0 : result.getVisibleExercises().size();
    //  logger.info("getFullExercisesSuccess got " + size + " exercises vs " + visibleIDs.size() + " visible.");
    int reqID = result.getReqID();


    if (DEBUG) logger.info("\tgetFullExercisesSuccess for each visible : " + visibleIDs.size());

    if (isCurrentReq(reqID)) {
      Map<Integer, ClientExercise> idToEx = rememberFetched(result, alreadyFetched);

      gotFullExercises(reqID, getVisibleExercises(visibleIDs, idToEx));
    } else {
      if (DEBUG_STALE)
        logger.info("getFullExercisesSuccess : ignoring req " + reqID + " vs current " + getCurrentExerciseReq());
    }
  }

  /**
   * @param visibleIDs
   * @param idToEx
   * @return
   * @see #getFullExercises(Collection, int, Collection, List)
   */
  List<ClientExercise> getVisibleExercises(Collection<Integer> visibleIDs, Map<Integer, ClientExercise> idToEx) {
    List<ClientExercise> toShow = new ArrayList<>();
    for (int id : visibleIDs) {
      ClientExercise e = idToEx.get(id);
      if (e == null) {
        logger.warning("\n\ngetVisibleExercises : huh? can't find exercise for visible id " + id + " in " + idToEx.keySet());
      } else {
        //   logger.info("getFullExercisesSuccess : show id " + id + " = " + e.getID() + " : " + e.getEnglish());
        toShow.add(e);
      }
    }
    return toShow;
  }


  private int nextPageReq = 0;

  /**
   * Cache the next page so we don't have to wait for it.
   */
  protected void goGetNextPage() {
    Set<Integer> toAskFor = getNextPageIDs();
    if (toAskFor.isEmpty()) {
      //    logger.info("goGetNextPage already has cached total " + fetched.size());
    } else {
      long then = System.currentTimeMillis();
      //     logger.info("goGetNextPage toAskFor " + toAskFor.size() + " exercises.");
      int currentReq = nextPageReq++;
      service.getFullExercises(getExerciseListRequest("").setReqID(currentReq), toAskFor,
          new AsyncCallback<ExerciseListWrapper<ClientExercise>>() {
            @Override
            public void onFailure(Throwable caught) {
              fullExerciseFailure(caught);
            }

            @Override
            public void onSuccess(final ExerciseListWrapper<ClientExercise> result) {
              if (result.getReqID() != currentReq) {
                logger.info("goGetNextPage skip stale req " + result.getReqID());
              } else {
                if (result.getExercises() != null) {
                  long now = System.currentTimeMillis();
                  int size = result.getExercises().isEmpty() ? 0 : result.getExercises().size();
                  if (now - then > 150 || DEBUG) {
                    logger.info("getFullExercisesSuccess took " + (now - then) + " to get " + size + " exercises");
                  }
                  setScoreHistory(result.getScoreHistoryPerExercise(), result.getExercises());
                  result.getExercises().forEach(ex -> addExerciseToCached(ex));
                } else {
                  logger.warning("getFullExercises huh? no exercises");
                }
              }
            }
          });
    }
  }

  @NotNull
  protected void addDynamicFacetToPairs(Map<String, String> typeToSelection, String languageMetaData, List<Pair> pairs) {
    addPairForTypeSelection(typeToSelection, pairs, languageMetaData);
   // return pairs;
  }

  void addPairForTypeSelection(Map<String, String> typeToSelection, List<Pair> pairs, String dynamicFacet) {
    if (typeToSelection.containsKey(dynamicFacet)) {
      pairs.add(new Pair(dynamicFacet, typeToSelection.get(dynamicFacet)));
    }
  }
}
