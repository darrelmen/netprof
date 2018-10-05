package mitll.langtest.client.analysis;

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.common.MessageHelper;
import mitll.langtest.client.services.AnalysisServiceAsync;
import mitll.langtest.shared.analysis.TimeAndScore;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.*;
import java.util.logging.Logger;

public class CommonShellCache<T extends CommonShell> implements ICommonShellCache<T> {
  private final Logger logger = Logger.getLogger("CommonShellCache");

  private Map<Integer, T> idToEx = new TreeMap<>();
  private MessageHelper messageHelper;

  public CommonShellCache(MessageHelper messageHelper) {
    this.messageHelper = messageHelper;
  }

  /**
   * @return
   * @seex #getShell
   */
  @Override
  public Map<Integer, T> getIdToEx() {
    return idToEx;
  }

  @Override
  public void setIdToEx(Map<Integer, T> idToEx) {
    this.idToEx = idToEx;
  }

  @Override
  public boolean isKnown(int exid) {
    return idToEx.containsKey(exid);
  }

  @Override
  public T getShell(int id) {
    return getIdToEx().get(id);
  }

  /**
   * Only get exercises this person has practiced.
   * Use this to get info for tooltips, etc.
   * Too heavy?
   * Why not get that info on demand?
   *
   * @paramx service
   * @paramx userid
   * @see AnalysisTab#useReport
   */
/*
  @Override
  public void populateExerciseMap(ExerciseServiceAsync service, int userid) {
    // logger.info("populateExerciseMap : get exercises for user " + userid);
    service.getExerciseIds(
        new ExerciseListRequest(1, userid)
            .setOnlyForUser(true),
        new AsyncCallback<ExerciseListWrapper<T>>() {
          @Override
          public void onFailure(Throwable throwable) {
            logger.warning("\n\n\n-> getExerciseIds " + throwable);
            messageHelper.handleNonFatalError("problem getting exercise ids", throwable);
          }

          @Override
          public void onSuccess(ExerciseListWrapper<T> exerciseListWrapper) {
            if (exerciseListWrapper != null && exerciseListWrapper.getExercises() != null) {
              //       Map<Integer, CommonShell> idToEx = getIdToEx();
//              logger.info("populateExerciseMap : got back " + exerciseListWrapper.getExercises().size() +
//                  "  exercises for user " + userid);
              exerciseListWrapper.getExercises().forEach(commonShell -> rememberExercise(commonShell));
            }
          }
        });
  }
*/


  @Override
  public void useTimeAndScore(AnalysisServiceAsync service,
                              List<TimeAndScore> rawBestScores) {
    Set<Integer> toGet = new HashSet<>();

    for (TimeAndScore timeAndScore : rawBestScores) {
      Integer id = timeAndScore.getExid();
      //addTimeToExID(timeAndScore.getTimestamp(), id);
      toGet.add(id);
    }

    if (!rawBestScores.isEmpty()) {
      //setTimeRange(rawBestScores);

      if (!toGet.isEmpty()) {
        service.getShells(toGet, new AsyncCallback<List<CommonShell>>() {
          @Override
          public void onFailure(Throwable throwable) {
            messageHelper.handleNonFatalError("problem getting exercise shells", throwable);
          }

          @Override
          public void onSuccess(List<CommonShell> commonShells) {
            commonShells.forEach(commonShell -> rememberExercise((T)commonShell));
          }
        });
      }
    }
  }

  /**
   * @param commonShell
   * @seez AnalysisPlot#populateExerciseMap
   */
  //@Override
  public void rememberExercise(T commonShell) {
    idToEx.put(commonShell.getID(), commonShell);
  }
}
