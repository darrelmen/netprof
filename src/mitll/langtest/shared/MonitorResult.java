package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Date;
import java.util.Map;

/**
 * So egyptian monitoring needs to show :
 * user id/id/unit/chapter/exercise text/audio/time/valid/duration/correct/score
 *
 * Search on user, id,unit,chapter,exer
 * Created by go22670 on 9/24/14.
 */
public class MonitorResult implements IsSerializable {
  private int uniqueID;
  private long userid;
  private String id;

  private String foreignText;

  private String answer;
  private boolean valid;
  private long timestamp;
  private String audioType;
  private int durationInMillis;
  private boolean correct;
  private float pronScore;
  private Map<String, String> unitToValue;

  public MonitorResult() {}

  public MonitorResult(int uniqueID, long userid, String id, String answer,
                       boolean valid, long timestamp, String answerType, int durationInMillis, boolean correct, float pronScore) {
    this.uniqueID = uniqueID;
    this.userid = userid;
    this.id = id;
    this.answer = answer;
    this.valid = valid;
    this.timestamp = timestamp;
    this.audioType = answerType == null || answerType.length() == 0 ? Result.AUDIO_TYPE_UNSET : answerType;
    this.durationInMillis = durationInMillis;
    this.correct = correct;
    this.pronScore = pronScore;
  }

  public int getUniqueID() {
    return uniqueID;
  }

  public long getUserid() {
    return userid;
  }

  public String getId() {
    return id;
  }

  public String getForeignText() {
    return foreignText;
  }

  public String getAnswer() {
    return answer;
  }

  public boolean isValid() {
    return valid;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getAudioType() {
    return audioType;
  }

  public int getDurationInMillis() {
    return durationInMillis;
  }

  public boolean isCorrect() {
    return correct;
  }

  public float getPronScore() {
    return pronScore;
  }

  public void setForeignText(String foreignText) {
    this.foreignText = foreignText;
  }
  public Map<String, String> getUnitToValue() {
    return unitToValue;
  };

  public void setUnitToValue(Map<String, String> unitToValue) {
    this.unitToValue = unitToValue;
  }

  @Override
  public String toString() {
    return "MonitorResult #" + uniqueID + "\t\tby user " + userid + "\texid " + id + " " +
        " at " + new Date(timestamp)+
        "  ans " +answer+
        " " + " audioType : " + audioType +
        " valid " + valid + " " + (correct ? "correct":"incorrect") + " score " + pronScore;
  }
}
