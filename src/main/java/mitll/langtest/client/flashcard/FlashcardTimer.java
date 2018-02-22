package mitll.langtest.client.flashcard;

import com.google.gwt.user.client.Timer;

class FlashcardTimer {
  private final TimerListener listener;
  /**
   * @see #cancelTimer
   * @see #isTimerNotRunning
   * @see #loadNextOnTimer
   */
  private Timer currentTimer = null;

  FlashcardTimer(TimerListener listener) {
    this.listener = listener;
  }

  /**
   * @param delay
   * @see #checkThenLoadNextOnTimer
   * @see #playAudioAndAdvance
   * @see #playRefAndGoToNext
   * @see StatsFlashcardFactory.StatsPracticePanel#maybeAdvance
   */
  boolean scheduleIn(final int delay) {
    //   logger.info("loadNextOnTimer ----> load next on " + delay);
    cancel();

    if (isTimerNotRunning()) {
      currentTimer = new Timer() {
        @Override
        public void run() {
//          logger.info("loadNextOnTimer ----> at " + System.currentTimeMillis() + "  firing on " + currentTimer);
          //loadNext();
          listener.timerFired();
        }
      };
      currentTimer.schedule(delay);
      return true;
    } else {
      //    logger.info("loadNextOnTimer ----> ignoring next current timer is running");
      return false;
    }
  }

  private boolean isTimerNotRunning() {
    return (currentTimer == null) || !currentTimer.isRunning();
  }

  void cancelTimer() {
    cancel();
    listener.timerCancelled();
  }

  private void cancel() {
    if (currentTimer != null) currentTimer.cancel();
  }
}
