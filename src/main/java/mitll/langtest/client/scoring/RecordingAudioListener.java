package mitll.langtest.client.scoring;

import mitll.langtest.shared.answer.AudioAnswer;

/**
 * Created by go22670 on 3/30/17.
 */
public interface RecordingAudioListener {
  void startRecording();
  void stopRecording();

  void postAudioFile();

  void useResult(AudioAnswer result);
  void useInvalidResult(boolean isValid);

  void flip(boolean first);
}
