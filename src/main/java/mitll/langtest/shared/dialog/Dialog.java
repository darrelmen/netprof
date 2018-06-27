package mitll.langtest.shared.dialog;

import mitll.langtest.server.database.dialog.DialogType;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.exercise.HasID;
import mitll.npdata.dao.SlickDialog;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Dialog implements IDialog {
  private int id;
  private int userid;
  private int projid;
  private int dominoid;
  private long modified;
  private DialogType kind = DialogType.DIALOG;
  private String orientation;
  private String imageRef;
  private List<CommonExercise> exercises;

  private transient SlickDialog slickDialog;

  public Dialog() {
  }

  public Dialog(int id,
                int userid,
                int projid,
                int dominoid,
                long modified,
                String orientation,
                String imageRef,
                List<ExerciseAttribute> attributes,
                List<CommonExercise> exercises) {
    this.id = id;
    this.userid = userid;
    this.projid = projid;
    this.dominoid = dominoid;
    this.imageRef = imageRef;
    this.modified = modified;
    this.orientation = orientation;
    this.attributes = attributes;
    this.exercises = exercises;
  }

  @Override
  public int getID() {
    return id;
  }

  @Override
  public int compareTo(@NotNull HasID o) {
    return Integer.compare(id, o.getID());
  }

  private String fltitle;
  private String entitle;

  private List<ExerciseAttribute> attributes;// = new ArrayList<>();

  public List<ExerciseAttribute> getAttributes() {
    return attributes;
  }

  public int getUserid() {
    return userid;
  }

  public int getProjid() {
    return projid;
  }

  public int getDominoid() {
    return dominoid;
  }

  public long getModified() {
    return modified;
  }

  public DialogType getKind() {
    return kind;
  }

  public String getOrientation() {
    return orientation;
  }

  public String getImageRef() {
    return imageRef;
  }

  public List<CommonExercise> getExercises() {
    return exercises;
  }

  public String getFltitle() {
    return fltitle;
  }

  public String getEntitle() {
    return entitle;
  }

  public void setSlickDialog(SlickDialog e) {
    this.slickDialog = e;
  }

  public SlickDialog getSlickDialog() {
    return slickDialog;
  }

  public String toString() {
    return "Dialog #" + id + " " + attributes;
  }
}
