/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.shared.result;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.server.database.result.ResultDAO;
import mitll.langtest.shared.UserAndTime;
import mitll.langtest.shared.answer.AudioType;
import mitll.npdata.dao.SlickResult;

import java.util.*;

/**
 * @see mitll.langtest.client.result.ResultManager
 */
public class MonitorResult implements IsSerializable, UserAndTime {
  private static final String ASC = "ASC";
  public static final String USERID = "userid";
  public static final String ID = "oldExID";
  public static final String VALID = "valid";
  public static final String TIMESTAMP = "timestamp";
  public static final String AUDIO_TYPE = "audioType";
  public static final String DURATION_IN_MILLIS = "durationInMillis";
  public static final String CORRECT = "correct";
  public static final String PRON_SCORE = "pronScore";
  public static final String DEVICE = "device";
  public static final String TEXT = "text";
  private static final String DYNAMIC_RANGE = "Dynamic Range";
  private static final String VALIDITY = "Validity";

  private int uniqueID;
  private int userid;
  private int exid;

  private String foreignText = "";

  private String answer;
  private boolean valid;
  private long timestamp;
  private AudioType audioType;
  private long durationInMillis;
  private boolean correct;
  private float pronScore;
  private String device;
  private String validity;
  private float snr;
  private boolean withFlash;
  private long processDur;
  private long roundTripDur;

  private Map<String, String> unitToValue;

  private transient String deviceType;

  public MonitorResult() {
  }

  /**
   * @param uniqueID
   * @param userid
   * @param answer
   * @param valid
   * @param timestamp
   * @param audioType
   * @param durationInMillis
   * @param correct
   * @param pronScore
   * @param device
   * @param processDur
   * @param roundTripDur
   * @param withFlash
   * @param exid
   * @see ResultDAO#getMonitorResultsForQuery
   * @see mitll.langtest.server.database.result.SlickResultDAO#fromSlickToMonitorResult(SlickResult)
   */
  public MonitorResult(int uniqueID, int userid, String answer,
                       boolean valid, long timestamp,
                       AudioType audioType, long durationInMillis,
                       boolean correct, float pronScore, String device,
                       long processDur, long roundTripDur, boolean withFlash, float dynamicRange,
                       String validity,
                       String deviceType,
                       String transcript, int exid) {
    this.uniqueID = uniqueID;
    this.userid = userid;
    this.answer = answer;
    this.valid = valid;
    this.timestamp = timestamp;
    this.audioType = audioType;
    this.durationInMillis = durationInMillis;
    this.correct = correct;
    this.pronScore = pronScore;
    this.device = device;
    this.withFlash = withFlash;
    this.processDur = processDur;
    this.roundTripDur = roundTripDur;
    this.validity = validity;
    this.snr = dynamicRange;
    this.deviceType = deviceType;
//    String simpleDevice1 = simpleDevice;
//    String scoreJSON1 = scoreJSON;
    this.foreignText = transcript;
    this.exid = exid;
  }

  public int getUniqueID() {
    return uniqueID;
  }

  public int getUserid() {
    return userid;
  }

  public int getExID() {
    return exid;
  }

  /**
   * @return
   */
  public String getForeignText() {
    return foreignText;
  }

  /**
   * @return
   * @see mitll.langtest.client.result.ResultManager#respondToClick
   */
  public String getAnswer() {
    return answer;
  }

  public void setAnswer(String orig) {
    this.answer = orig;
  }

  public boolean isValid() {
    return valid;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public int getExid() {
    return exid;
  }

  public AudioType getAudioType() {
    return audioType;
  }

  public long getDurationInMillis() {
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

  /**
   * @param unitToValue
   * @see mitll.langtest.server.database.DatabaseImpl#addUnitAndChapterToResults
   */
  public void setUnitToValue(Map<String, String> unitToValue) {
    this.unitToValue = unitToValue;
  }

  /**
   * ONLY does the first column, for now...
   * <p>
   * Expects a query where the columns are field_ASC or field_DESC
   *
   * @param columns
   * @return
   */
  public Comparator<MonitorResult> getComparator(final Collection<String> columns) {
    //logger.info("getComparator columns " + columns);
    final List<String> copy = new ArrayList<String>(columns);
    if (copy.isEmpty() || copy.iterator().next().equals("")) {
      return Comparator.comparingInt(MonitorResult::getUniqueID);
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
          int comp = 0;
          if (field.equals(USERID)) {
            comp = Integer.compare(o1.userid, o2.userid);
          }
          if (comp != 0) return getComp(asc, comp);

          // oldExID
//          if (field.equals(ID)) {
//            String id1 = o1.oldExID;
//            String id2 = o2.oldExID;
//            comp = compareTwoMaybeInts(id1, id2);
//          }
//          if (comp != 0) return getComp(asc, comp);

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
            comp = Long.compare(o1.timestamp, o2.timestamp);
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
            comp = Long.compare(o1.durationInMillis, o2.durationInMillis);
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
            comp = Float.compare(pronScore1, pronScore2);
          }
          if (comp != 0) return getComp(asc, comp);

          if (field.equals(VALIDITY)) {
            comp = o1.getValidity().compareTo(o2.getValidity());
          }
          if (comp != 0) return getComp(asc, comp);

          if (field.equals(DYNAMIC_RANGE)) {
            comp = Float.compare(o1.getSnr(), o2.getSnr());
          }
          if (comp != 0) return getComp(asc, comp);

          // unit and chapter
          Map<String, String> unitToValue1 = o1.getUnitToValue();
          Map<String, String> unitToValue2 = o2.getUnitToValue();

          if (unitToValue1 != null && unitToValue2 != null) {
            if (unitToValue1.containsKey(field) || unitToValue2.containsKey(field)) {
              String first = unitToValue1.get(field);
              String second = unitToValue2.get(field);
              if (first == null && second == null) {
                comp = 0;
              } else {
                comp = first == null ? +1 : second == null ? -1 : 0;
                if (comp == 0) {
                  comp = compareTwoMaybeInts(first, second);
                }
              }
            }
          }

          if (comp == 0) comp = Integer.compare(o1.getUniqueID(), o2.getUniqueID());

          return getComp(asc, comp);
        }

        int getComp(boolean asc, int comp) {
          return (asc ? comp : -1 * comp);
        }

        int compareTwoMaybeInts(String id1, String id2) {
          int comp;
          try {   // this could be slow
            int i = Integer.parseInt(id1);
            int j = Integer.parseInt(id2);
            comp = Integer.compare(i, j);
          } catch (NumberFormatException e) {
            comp = id1.compareTo(id2);
          }
          return comp;
        }
      };
    }
  }

  public String getDeviceType() {
    return deviceType;
  }

  /**
   * @return
   * @see mitll.langtest.client.result.ResultManager#addResultColumn
   */
  public String getDevice() {
    return device;
  }

  public boolean isWithFlash() {
    return withFlash;
  }

  public long getProcessDur() {
    return processDur;
  }

  public long getRoundTripDur() {
    return roundTripDur;
  }

  public String getValidity() {
    return validity;
  }

  public float getSnr() {
    return snr;
  }

  @Override
  public String toString() {
    return "MonitorResult #" + uniqueID + "\t\tby user " + userid +
        //"\toldExID " + oldExID + " " +
        " at " + new Date(timestamp) +
        "  ans " + answer +
        " audioType : " + audioType +
        " device " + device +
        " valid " + valid + " " + (correct ? "correct" : "incorrect") + " score " + pronScore;
  }
}
