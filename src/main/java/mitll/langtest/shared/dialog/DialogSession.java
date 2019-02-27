package mitll.langtest.shared.dialog;

import mitll.langtest.client.custom.INavigation;
import mitll.langtest.shared.exercise.HasID;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class DialogSession implements IDialogSession {
  private int id = -1;
  private int userid =-1;
  private int projid = -1;
  private int dialogid = -1;
  private long modified = 0L, end = 0L;

  private INavigation.VIEWS views = INavigation.VIEWS.STUDY;
  private DialogStatus status = DialogStatus.DEFAULT;
  private int numRecordings = 0;

  private float score = 0F;
  private float speakingRate = 0F;

  public DialogSession() {
  }

  public DialogSession(int userid, int projid, int dialogid, INavigation.VIEWS views) {
    this.userid = userid;
    this.projid = projid;
    this.dialogid = dialogid;
    this.views = views;
  }

  public DialogSession(int id, int userid, int projid, int dialogid, long modified, long end, INavigation.VIEWS views,
                       DialogStatus status, int numRecordings, float score, float speakingRate) {
    this(userid, projid, dialogid, views);
    this.id = id;
    this.modified = modified;
    this.end = end;
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

  public DialogSession setID(int id) {
    this.id = id;
    return this;
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
        "\n\tdialog " + getDialogid() +
        "\n\tby   " + getUserid() +
        "\n\tview " + getView() +
        "\n\tat   " + new Date(getModified());
  }
}
