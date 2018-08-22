package mitll.langtest.client.scoring;

import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.Validity;

/**
 * Created by go22670 on 3/30/17.
 */
public interface RecordingAudioListener {
  void startRecording();

  void stopRecording();

  void gotShortDurationRecording();

  void useResult(AudioAnswer result);

  void useInvalidResult(boolean isValid);

  void usePartial(Validity validity);

  void onPostFailure();

/*
  void flip(boolean first);
*/
}
