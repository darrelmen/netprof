package mitll.langtest.server.audio.image;

public interface ITranscriptEvent {
  float getStart();

  float getEnd();

  String getEvent();

  float getScore();
}
