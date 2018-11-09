package mitll.langtest.client.scoring;

import mitll.langtest.shared.answer.AudioAnswer;

/**
 * Created by go22670 on 3/30/17.
 * @see RecordDialogExercisePanel#addWidgets
 */
interface RecordingAudioListener {
  void startRecording();

  void stopRecording();

  void gotShortDurationRecording();

  void useResult(AudioAnswer result);

  void useInvalidResult(int exid, boolean isValid);

  void usePartial(StreamResponse validity);

  void gotAbort();

  void onPostFailure();

  int getDialogSessionID();
}
