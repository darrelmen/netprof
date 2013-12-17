package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.grade.Grade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * An answer to a question. <br></br>
 * Records who answered it, which plan, which exercise, which question within a multi-question exercise, and
 *  the answer, which may either be a) the text of a written response or b) a path to an audio file response
 * <br></br>
 * May be marked with whether the audio file was "valid" - long enough and not silence.<br></br>
 * Also records the timestamp, and optionally whether the result was to a fl/english and spoken/written question.
 * These may be added later via enrichment (joining) against the schedule, which says for a specific user, which
 * of these two flags was presented.
 *
 * User: go22670
 * Date: 5/18/12
 * Time: 5:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class Result implements IsSerializable {
  //userid INT, plan VARCHAR, id VARCHAR, qid INT, answer VARCHAR, audioFile VARCHAR, valid BOOLEAN, timestamp
  public int uniqueID;
  public long userid;
  public String plan;
  public String id;
  public int qid;
  public String answer;
  public boolean valid;
  public long timestamp;
  public boolean flq;
  public boolean spoken;
  //private AudioType audioType; // so having another object in here seemed to slow down serialization a lot
  public String audioType;
  public String gradeInfo = "";
  public int durationInMillis;
  private boolean correct;
  private float pronScore;
  private String stimulus;

  public static final String AUDIO_TYPE_UNSET = "unset";
  public static final String AUDIO_TYPE_REGULAR = "regular";
  public static final String AUDIO_TYPE_FAST_AND_SLOW = "fastAndSlow";
  public static final String AUDIO_TYPE_DEMO = "demo";
  public static final String AUDIO_TYPE_PRACTICE = "practice";
  public static final String AUDIO_TYPE_REVIEW = "review";

/*  public enum AudioType implements IsSerializable {
    UNSET,
    REGULAR,
    FAST_AND_SLOW;

    AudioType() {} // for gwt serialization
  }*/

  public Result() {}

  /**
   * @see mitll.langtest.server.database.ResultDAO#getResults()
   * @param userid
   * @param plan
   * @param id
   * @param qid
   * @param answer
   * @param valid
   * @param timestamp
   * @param flq
   * @param spoken
   * @param answerType
   * @param durationInMillis
   * @param correct
   * @param pronScore
   */
  public Result(int uniqueID, long userid, String plan, String id, int qid, String answer,
                boolean valid, long timestamp, boolean flq, boolean spoken, String answerType, int durationInMillis, boolean correct, float pronScore) {
    this.uniqueID = uniqueID;
    this.userid = userid;
    this.plan = plan;
    this.id = id;
    this.qid = qid;
    this.answer = answer;
    this.valid = valid;
    this.timestamp = timestamp;
    this.flq = flq;
    this.spoken = spoken;
    this.audioType = answerType == null || answerType.length() == 0 ? AUDIO_TYPE_UNSET : answerType;
    this.durationInMillis = durationInMillis;
    this.correct = correct;
    this.pronScore = pronScore;
  }

  /**
   * Compound key of exercise id and question id within that exercise.
   * @return
   */
  public String getID() {
    return id + "/" +qid;
  }

  public void setFLQ(boolean flq) {
    this.flq = flq;
  }

  public void setSpoken(boolean v) {
    this.spoken = v;
  }

  // public boolean isRegularAudio() { return audioType == null || audioType.equals(AUDIO_TYPE_UNSET) || audioType.equals(AUDIO_TYPE_REGULAR); }
  public boolean isFastAndSlowAudio() {
    return audioType != null && audioType.equals(AUDIO_TYPE_FAST_AND_SLOW);
  }

  public String getAudioType() {
    return audioType;
  }

  public void addGrade(Grade g) {
    gradeInfo += g.grade +",";
  }

  public boolean isCorrect() {
    return correct;
  }

  public float getPronScore() {
    return pronScore;
  }

  public String getStimulus() {
    return stimulus;
  }

  public void setStimulus(String stimulus) {
    this.stimulus = stimulus;
  }

  public Comparator<Result> getComparator(final Collection<String> columns) {
//    System.out.println("columns " + columns);
    final List<String> copy = new ArrayList<String>(columns);
    if (copy.isEmpty() || copy.iterator().next().equals("")) {
      return new Comparator<Result>() {
        @Override
        public int compare(Result o1, Result o2) {
          return o1.uniqueID < o2.uniqueID ? -1 : o1.uniqueID > o2.uniqueID ? +1 : 0;
        }
      };
    } else {
      return new Comparator<Result>() {
        @Override
        public int compare(Result o1, Result o2) {
          for (String col : copy) {
            String[] split = col.split("_");
            String field = split[0];

            if (split.length != 2) System.err.println("huh? col = " + col);
            boolean asc = split.length <= 1 || split[1].equals("ASC");

            int comp = 0;
            if (field.equals("userid")) {
              comp = o1.userid < o2.userid ? -1 : o2.userid < o1.userid ? +1 : 0;
            }
            if (comp != 0) return asc ? comp : -1 * comp;

            if (field.equals("id")) {
              comp = o1.id.compareTo(o2.id);
            }
            if (comp != 0) return asc ? comp : -1 * comp;

            if (field.equals("qid")) {
              comp = o1.qid < o2.qid ? -1 : o2.qid < o1.qid ? +1 : 0;
            }
            if (comp != 0) return asc ? comp : -1 * comp;

            if (field.equals("valid")) {
              comp = o1.valid == o2.valid ? 0 : (!o1.valid && o2.valid ? -1 : +1);
            }
            if (comp != 0) return asc ? comp : -1 * comp;

            if (field.equals("timestamp")) {
              comp = o1.timestamp < o2.timestamp ? -1 : o2.timestamp < o1.timestamp ? +1 : 0;
            }
            if (comp != 0) return asc ? comp : -1 * comp;

            if (o1.audioType != null) {
              if (field.equals("audioType")) {
                comp = o1.audioType.compareTo(o2.audioType);
              }
              if (comp != 0) return asc ? comp : -1 * comp;
            }

            if (o1.gradeInfo != null) {
              if (field.equals("gradeInfo")) {
                comp = o1.gradeInfo.compareTo(o2.gradeInfo);
              }
              if (comp != 0) return asc ? comp : -1 * comp;
            }
            if (field.equals("durationInMillis")) {
              comp = o1.durationInMillis < o2.durationInMillis ? -1 : o2.durationInMillis < o1.durationInMillis ? +1 : 0;
            }
            if (comp != 0) return asc ? comp : -1 * comp;

            if (field.equals("correct")) {
              comp = o1.correct == o2.correct ? 0 : (!o1.correct && o2.correct ? -1 : +1);
            }
            if (comp != 0) return asc ? comp : -1 * comp;

            if (field.equals("pronScore")) {
              comp = o1.pronScore < o2.pronScore ? -1 : o2.pronScore < o1.pronScore ? +1 : 0;
            }
          /*if (comp != 0) */
            return asc ? comp : -1 * comp;
          }
          return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }
      };
    }
  }

  @Override
  public String toString() {
    return "Result #" + uniqueID + "\t\tby user " + userid + "\texid " + id + " " +
        (flq ? "flq" : "english") + "  ans " +answer+
      " " + (spoken ? "spoken" : "written") + " audioType : " + audioType +
        " valid " + valid + " " + (correct ? "correct":"incorrect") + " score " + pronScore;
  }
}
