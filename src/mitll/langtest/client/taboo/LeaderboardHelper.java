package mitll.langtest.client.taboo;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/4/13
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class LeaderboardHelper {

/*
  private void showLeaderboard(final LangTestDatabaseAsync service, final ExerciseController controller, final ReceiverPanel outer) {
    if (gameInfo.anyGamesRemaining()) {
      service.getLeaderboard(selectionState, new AsyncCallback<Leaderboard>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Leaderboard result) {
          Modal plot = new LeaderboardPlot().showLeaderboardPlot(result, controller.getUser(), 0, selectionState,
            "Game complete! Your score was " + score + " out of " + totalClues, 5000);
          plot.addHideHandler(new HideHandler() {
            @Override
            public void onHide(HideEvent hideEvent) {
              doNextGame(controller, service, outer);
            }
          });
        }
      });
    } else {
      showLeaderboard(service, controller, "Would you like to practice this chapter(s) again?",
        "To continue playing, choose another chapter.");
    }
  }
*/


 /* private void showLeaderboard(LangTestDatabaseAsync service, final ExerciseController controller, final String prompt1,
                               final String clickNoMessage
  ) {
    ClickHandler onYes = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.startOver();
      }
    };
    ClickHandler onNo = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        showPopup(clickNoMessage);
      }
    };
    showLeaderboard(service, controller, prompt1, onYes, onNo);
  }
*/
/*  private void showLeaderboard(LangTestDatabaseAsync service, final ExerciseController controller, final String prompt1,
                               final ClickHandler onYes, final ClickHandler onNo
  ) {
    service.getLeaderboard(selectionState, new AsyncCallback<Leaderboard>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Leaderboard result) {
        new LeaderboardPlot().showLeaderboardPlot(result, controller.getUser(), 0, selectionState,
          prompt1,
          onYes,
          onNo,0);
      }
    });
  }*/
}
