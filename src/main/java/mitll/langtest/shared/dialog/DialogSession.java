package mitll.langtest.shared.dialog;

import mitll.langtest.client.custom.INavigation;
import mitll.langtest.shared.exercise.HasID;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class DialogSession implements IDialogSession {
  private int id;
  private int userid;
  private int projid;
  private int dialogid;
  private long modified, end;

  private INavigation.VIEWS views;
  private DialogStatus status;
  private int numRecordings;

  private float score;
  private float speakingRate;


  public DialogSession() {
  }

  public DialogSession(int id, int userid, int projid, int dialogid, long modified, long end, INavigation.VIEWS views,
                       DialogStatus status, int numRecordings, float score, float speakingRate) {
    this.id = id;
    this.userid = userid;
    this.projid = projid;
    this.dialogid = dialogid;
    this.modified = modified;
    this.end = end;
    this.views = views;
    this.status = status;
    this.numRecordings = numRecordings;
    this.score = score;
    this.speakingRate = speakingRate;
  }


  @Override
  public int getUserid() {
    return userid;
  }

  @Override
  public int getProjid() {
    return projid;
  }

  @Override
  public int getDialogid() {
    return dialogid;
  }

  @Override
  public long getModified() {
    return modified;
  }

  @Override
  public long getEnd() {
    return end;
  }

  @Override
  public INavigation.VIEWS getView() {
    return views;
  }

  @Override
  public DialogStatus getStatus() {
    return status;
  }


  @Override
  public float getScore() {
    return score;
  }

  @Override
  public float getSpeakingRate() {
    return speakingRate;
  }

  @Override
  public int getID() {
    return id;
  }

  @Override
  public int compareTo(@NotNull HasID o) {
    return Integer.compare(id, o.getID());
  }

  @Override
  public int getNumRecordings() {
    return numRecordings;
  }


  public String toString() {
    return "DialogSession #" + getID() +
        "\n\tdialog " +getDialogid() + " by " + getUserid()+ " " +getView()+
        " at " + new Date(getModified());
  }
}
