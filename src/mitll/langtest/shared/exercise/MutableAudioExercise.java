package mitll.langtest.shared.exercise;

/**
 * Created by go22670 on 2/1/16.
 */
public interface MutableAudioExercise {
  void addAudio(AudioAttribute audioAttribute);

  boolean removeAudio(AudioAttribute audioAttribute);

  void clearRefAudio();

  void clearSlowRefAudio();
}
