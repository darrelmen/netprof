package mitll.langtest.client.amas;

import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.sound.SoundFeedback;

/**
 * Created by go22670 on 12/23/15.
 */
public class MySoundFeedback extends SoundFeedback {
  public MySoundFeedback(ExerciseController controller) {
    super(controller.getSoundManager());
  }

  public synchronized void queueSong(String song) {
    destroySound(); // if there's something playing, stop it!
    createSound(song, null);
  }
}

