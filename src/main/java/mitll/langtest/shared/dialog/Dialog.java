package mitll.langtest.shared.dialog;

import mitll.langtest.server.database.dialog.DialogType;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.exercise.HasID;
import mitll.npdata.dao.SlickDialog;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class Dialog implements IDialog {
  public static final String SPEAKER = "Speaker".toLowerCase();
  private int id;
  private int userid;
  private int projid;
  private int dominoid;
  private long modified;

  private String fltitle;
  private String entitle;
  private String unit;
  private String chapter;

  private DialogType kind = DialogType.DIALOG;
  private String orientation;
  private String imageRef;
  private List<ExerciseAttribute> attributes = new ArrayList<>();
  private List<CommonExercise> exercises = new ArrayList<>();

  float score;

  private transient SlickDialog slickDialog;

  public Dialog() {
  }

  /**
   * @param id
   * @param userid
   * @param projid
   * @param dominoid
   * @param modified
   * @param unit
   * @param chapter
   * @param orientation
   * @param imageRef
   * @param fltitle
   * @param entitle
   * @param attributes
   * @param exercises
   */
  public Dialog(int id,
                int userid,
                int projid,
                int dominoid,
                long modified,
                String unit,
                String chapter,
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
    this.unit = unit;
    this.chapter = chapter;
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
        slickDialog.unit(),
        slickDialog.lesson(),
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


  @Override
  public List<ExerciseAttribute> getAttributes() {
    return attributes;
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
  public int getDominoid() {
    return dominoid;
  }

  @Override
  public long getModified() {
    return modified;
  }

  @Override
  public DialogType getKind() {
    return kind;
  }

  @Override
  public String getOrientation() {
    return orientation;
  }

  @Override
  public String getImageRef() {
    return imageRef;
  }

  @Override
  public List<CommonExercise> getExercises() {
    return exercises;
  }

  @Override
  public String getFltitle() {
    return fltitle;
  }

  @Override
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


  @Override
  public String getUnit() {
    return unit;
  }

  @Override
  public String getChapter() {
    return chapter;
  }

  @Override
  public List<String> getSpeakers() {
    Set<String> speakers = new LinkedHashSet<>();
    attributes.forEach(exerciseAttribute -> {
          if (exerciseAttribute.getProperty().toLowerCase().startsWith(SPEAKER)) {
            speakers.add(exerciseAttribute.getValue());
          }
        }
    );
    return new ArrayList<>(speakers);
  }

  @Override
  public Map<String, List<CommonExercise>> groupBySpeaker() {
    Map<String, List<CommonExercise>> speakerToExercises = new HashMap<>();
    exercises.forEach(commonExercise -> {
          List<CommonExercise> exercises = speakerToExercises.computeIfAbsent(getSpeaker(commonExercise), k -> new ArrayList<>());
          exercises.add(commonExercise);
        }
    );
    return speakerToExercises;
  }

  private String getSpeaker(CommonExercise commonExercise) {
    List<ExerciseAttribute> speakerAttr = getSpeakerAttr(commonExercise);
    return speakerAttr.isEmpty() ? "UNKNOWN" : speakerAttr.stream().iterator().next().getValue();
  }

  private List<ExerciseAttribute> getSpeakerAttr(CommonExercise commonExercise) {
    return commonExercise
        .getAttributes()
        .stream()
        .filter(exerciseAttribute -> exerciseAttribute.getProperty().equalsIgnoreCase("Speaker"))
        .collect(Collectors.toList());
  }

  public String toString() {
    return "Dialog #" + id +
        "\n\tunit        " + unit +
        "\n\tchapter     " + chapter +
        "\n\ttitle       " + entitle +
        "\n\torientation " + orientation +
        "\n\timage       " + imageRef +
        "\n\t# ex        " + exercises.size() +
        "\n\tattr        " + attributes;
  }
}
