package mitll.langtest.shared.dialog;

import mitll.langtest.server.database.dialog.DialogType;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.exercise.HasID;
import mitll.npdata.dao.SlickDialog;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Dialog implements IDialog {
  private int id;
  private int userid;
  private int projid;
  private int dominoid;
  private long modified;

  private String fltitle;
  private String entitle;

  private DialogType kind = DialogType.DIALOG;
  private String orientation;
  private String imageRef;
  private List<ExerciseAttribute> attributes = new ArrayList<>();
  private List<CommonExercise> exercises = new ArrayList<>();

  float score;

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
                String fltitle,
                String entitle,
                List<ExerciseAttribute> attributes,
                List<CommonExercise> exercises) {
    this.id = id;
    this.userid = userid;
    this.projid = projid;
    this.dominoid = dominoid;
    this.imageRef = imageRef;
    this.modified = modified;
    this.entitle = entitle;
    this.fltitle = fltitle;
    this.orientation = orientation;
    this.attributes = attributes;
    this.exercises = exercises;
  }

  public Dialog(SlickDialog slickDialog) {
    this(slickDialog.id(),
        slickDialog.userid(),
        slickDialog.projid(),
        slickDialog.dominoid(),
        slickDialog.modified().getTime(),
        slickDialog.orientation(),
        "",
        "",
        slickDialog.entitle(), new ArrayList<>(), new ArrayList<>());
    this.slickDialog = slickDialog;
  }

  @Override
  public int getID() {
    return id;
  }

  @Override
  public int compareTo(@NotNull HasID o) {
    return Integer.compare(id, o.getID());
  }


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

  public void setImageRef(String imageRef) {
    this.imageRef = imageRef;
  }

/*
  public void setAttributes(List<ExerciseAttribute> attributes) {
    this.attributes = attributes;
  }
*/

/*
  public void setExercises(List<CommonExercise> exercises) {
    this.exercises = exercises;
  }
*/

  public String toString() {
    return "Dialog #" + id +
        "\n\ttitle       " + entitle +
        "\n\torientation " + orientation +
        "\n\timage       " + imageRef +
        "\n\t# ex        " + exercises.size() +
        "\n\tattr        " + attributes;
  }
}
