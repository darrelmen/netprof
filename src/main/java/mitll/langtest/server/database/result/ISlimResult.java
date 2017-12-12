package mitll.langtest.server.database.result;

/**
 * Created by go22670 on 4/13/17.
 */
public interface ISlimResult {
  float getPronScore();
  String getJsonScore();
  boolean isValid();
  int getAudioID();
  int getExID();
}
