package mitll.langtest.shared;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.rpc.IsSerializable;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

/**
 * So egyptian monitoring needs to show :
 * user id/id/unit/chapter/exercise text/audio/time/valid/duration/correct/score
 * <p/>
 * Search on user, id,unit,chapter,exer
 * Created by go22670 on 9/24/14.
 */
public class MonitorResult implements IsSerializable {
  private static final String ASC = "ASC";
  public static final String USERID = "userid";
  public static final String ID = "id";
  public static final String VALID = "valid";
  public static final String TIMESTAMP = "timestamp";
  public static final String AUDIO_TYPE = "audioType";
  public static final String DURATION_IN_MILLIS = "durationInMillis";
  public static final String CORRECT = "correct";
  public static final String PRON_SCORE = "pronScore";
  public static final String DEVICE = "device";
  public static final String TEXT = "text";
  public static final String DYNAMIC_RANGE = "Dynamic Range";
  public static final String VALIDITY = "Validity";

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
  private String device;
  private String validity;
  private float snr;
  private boolean withFlash;
  private int processDur;
  private int roundTripDur;

  private Map<String, String> unitToValue;

  public MonitorResult() {}

  /**
   *  @param uniqueID
   * @param userid
   * @param id
   * @param answer
   * @param valid
   * @param timestamp
   * @param answerType
   * @param durationInMillis
   * @param correct
   * @param pronScore
   * @param device
   * @param withFlash
   * @param processDur
   * @param roundTripDur
   *  @see mitll.langtest.server.database.ResultDAO#getMonitorResultsForQuery(Connection, PreparedStatement)
   */
  public MonitorResult(int uniqueID, long userid, String id, String answer,
                       boolean valid, long timestamp, String answerType, int durationInMillis,
                       boolean correct, float pronScore, String device, boolean withFlash, int processDur, int roundTripDur, String validity, float snr) {
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
    this.device = device;
    this.withFlash = withFlash;
    this.processDur   = processDur;
    this.roundTripDur = roundTripDur;
    this.validity = validity;
    this.snr = snr;
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
  }

  public void setUnitToValue(Map<String, String> unitToValue) {
    this.unitToValue = unitToValue;
  }

  /**
   * ONLY does the first column, for now...
   *
   * Expects a query where the columns are field_ASC or field_DESC
   *
   * @param columns
   * @return
   */
  public Comparator<MonitorResult> getComparator(final Collection<String> columns) {
    //logger.info("getComparator columns " + columns);
    final List<String> copy = new ArrayList<String>(columns);
    if (copy.isEmpty() || copy.iterator().next().equals("")) {
      return new Comparator<MonitorResult>() {
        @Override
        public int compare(MonitorResult o1, MonitorResult o2) {
          return o1.uniqueID < o2.uniqueID ? -1 : o1.uniqueID > o2.uniqueID ? +1 : 0;
        }
      };
    } else {
      return new Comparator<MonitorResult>() {
        @Override
        public int compare(MonitorResult o1, MonitorResult o2) {
          //for (String col : copy) {
          String col = copy.get(0);
          String[] split = col.split("_");
          String field = split[0];

          if (split.length != 2) System.err.println("huh? col = " + col);
          boolean asc = split.length <= 1 || split[1].equals(ASC);

        //  logger.info("col " + col + " asc = " + asc);

          // USERID ---------------
          long comp = 0;
          if (field.equals(USERID)) {
            comp = o1.userid < o2.userid ? -1 : o1.userid > o2.userid ? +1 : 0;
          }
          if (comp != 0) return getComp(asc, comp);

          // id
          if (field.equals(ID)) {
            String id1 = o1.id;
            String id2 = o2.id;
            comp = compareTwoMaybeInts(id1, id2);
          }
          if (comp != 0) return getComp(asc, comp);

          // text
          if (field.equals(TEXT)) {
            comp = o1.getForeignText().compareTo(o2.getForeignText());
          }
          if (comp != 0) return getComp(asc, comp);

          // valid
          if (field.equals(VALID)) {
            comp = o1.valid == o2.valid ? 0 : (!o1.valid && o2.valid ? -1 : +1);
          }
          if (comp != 0) return getComp(asc, comp);

          // timestamp
          if (field.equals(TIMESTAMP)) {
            comp = o1.timestamp < o2.timestamp ? -1 : o1.timestamp > o2.timestamp ? +1 : 0;
          }
          if (comp != 0) return getComp(asc, comp);

          // audio type
          if (o1.audioType != null) {
            if (field.equals(AUDIO_TYPE)) {
              comp = o1.audioType.compareTo(o2.audioType);
            }
            if (comp != 0) return getComp(asc, comp);
          }

          if (o1.device != null) {
            if (field.equals(DEVICE)) {
              comp = o1.device.compareTo(o2.device);
            }
            if (comp != 0) return getComp(asc, comp);
          }

          // duration
          if (field.equals(DURATION_IN_MILLIS)) {
            comp = o1.durationInMillis < o2.durationInMillis ? -1 : o1.durationInMillis > o2.durationInMillis ? +1 : 0;
          }
          if (comp != 0) return getComp(asc, comp);

          // correct
          if (field.equals(CORRECT)) {
            comp = o1.isCorrect() == o2.isCorrect() ? 0 : (!o1.isCorrect() ? -1 : +1);
          }
          if (comp != 0) return getComp(asc, comp);

          if (field.equals("withFlash")) {
            comp = o1.isWithFlash() == o2.isWithFlash() ? 0 : (!o1.isWithFlash() ? -1 : +1);
          }
          if (comp != 0) return getComp(asc, comp);

          // score ------------
          if (field.equals(PRON_SCORE)) {
            float pronScore1 = o1.getPronScore();
            float pronScore2 = o2.getPronScore();
            comp = pronScore1 < pronScore2 ? -1 : pronScore1 > pronScore2 ? +1 : 0;
          }
          if (comp != 0) return getComp(asc, comp);

          if (field.equals(VALIDITY)) {
            comp = o1.getValidity().compareTo(o2.getValidity());
          }
          if (comp != 0) return getComp(asc, comp);

          if (field.equals(DYNAMIC_RANGE)) {
            float pronScore1 = o1.getSnr();
            float pronScore2 = o2.getSnr();
            comp = pronScore1 < pronScore2 ? -1 : pronScore1 > pronScore2 ? +1 : 0;
          }
          if (comp != 0) return getComp(asc, comp);

          // unit and chapter
          Map<String, String> unitToValue1 = o1.getUnitToValue();
          Map<String, String> unitToValue2 = o2.getUnitToValue();
          if (unitToValue1.containsKey(field) || unitToValue2.containsKey(field)) {
            String first = unitToValue1.get(field);
            String second = unitToValue2.get(field);
            comp = first == null ? +1 : second == null ? -1 : 0;
            if (comp == 0) {
              comp = compareTwoMaybeInts(first, second);
            }
          }

          if (comp == 0) comp = Integer.valueOf(o1.getUniqueID()).compareTo(o2.getUniqueID());

          return getComp(asc, comp);
        }

        protected int getComp(boolean asc, long comp) {
          return (int) (asc ? comp : -1 * comp);
        }

        protected int compareTwoMaybeInts(String id1, String id2) {
          int comp;
          try {   // this could be slow
            int i = Integer.parseInt(id1);
            int j = Integer.parseInt(id2);
            comp = i < j ? -1 : i > j ? +1 : 0;
          } catch (NumberFormatException e) {
            comp = id1.compareTo(id2);
          }
          return comp;
        }
      };
    }
  }

  /**
   * @see mitll.langtest.client.result.ResultManager#addResultColumn(CellTable)
   * @return
   */
  public String getDevice() {
    return device;
  }

  @Override
  public String toString() {
    return "MonitorResult #" + uniqueID + "\t\tby user " + userid + "\texid " + id + " " +
        " at " + new Date(timestamp) +
        "  ans " + answer +
        " audioType : " + audioType +
     //   " context : " + context +
        " device " + device+
        " valid " + valid + " " + (correct ? "correct" : "incorrect") + " score " + pronScore;
  }

  public boolean isWithFlash() {
    return withFlash;
  }

  public int getProcessDur() {
    return processDur;
  }

  public int getRoundTripDur() {
    return roundTripDur;
  }

  public String getValidity() {
    return validity;
  }

  public float getSnr() {
    return snr;
  }
}
