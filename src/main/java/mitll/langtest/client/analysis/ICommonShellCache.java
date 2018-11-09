package mitll.langtest.client.analysis;

import mitll.langtest.client.services.AnalysisServiceAsync;
import mitll.langtest.shared.analysis.TimeAndScore;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.List;
import java.util.Map;

public interface ICommonShellCache<T extends CommonShell> extends ExerciseLookup<T> {
  Map<Integer, T> getIdToEx();

  void setIdToEx(Map<Integer, T> idToEx);

//  boolean isKnown(int exid);
//
//  T getShell(int id);

//  void populateExerciseMap(ExerciseServiceAsync service, int userid);

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


  void useTimeAndScore(AnalysisServiceAsync service,
                       List<TimeAndScore> rawBestScores);

//  void rememberExercise(T commonShell);
}
