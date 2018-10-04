package mitll.langtest.shared.dialog;

import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static mitll.langtest.shared.exercise.STATE.UNSET;

/**
 * @see
 */
public class Dialog implements IDialog, MutableShell {

  private final transient Logger logger = Logger.getLogger("Dialog");


  private static final String SPEAKER = "Speaker".toLowerCase();
  public static final String UNIT = "unit";
  private static final String CHAPTER = "chapter";
  private static final String SPEAKER1 = "Speaker";
  public static final String UNKNOWN = "UNKNOWN";

  private int id;
  private int userid;
  private int projid;
  private int imageid;
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
  private List<ClientExercise> exercises = new ArrayList<>();
  private List<ClientExercise> coreVocabulary = new ArrayList<>();

  private float score = -1;

  public Dialog() {
  }

  /**
   * @param id
   * @param userid
   * @param projid
   * @param imageid
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
   * @param coreExercises
   * @see mitll.langtest.server.database.dialog.DialogDAO#makeDialog
   * @see mitll.langtest.server.database.dialog.KPDialogs#getDialogs
   */
  public Dialog(int id,
                int userid,
                int projid,
                int imageid,
                int dominoid,
                long modified,
                String unit,
                String chapter,
                String orientation,
                String imageRef,
                String fltitle,
                String entitle,
                List<ExerciseAttribute> attributes,
                List<ClientExercise> exercises,
                List<ClientExercise> coreExercises) {
    this.id = id;
    this.userid = userid;
    this.projid = projid;
    this.imageid = imageid;
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
    this.coreVocabulary = coreExercises;
  }

  @Override
  public int getID() {
    return id;
  }

  @Override
  public int compareTo(@NotNull HasID o) {
    return Integer.compare(id, o.getID());
  }

  /**
   * @return
   */
  @Override
  public List<ExerciseAttribute> getAttributes() {
    return attributes;
  }

  /**
   * @param metadata
   * @return empty if no attribute of that type
   */
  @Override
  public String getAttributeValue(IDialog.METADATA metadata) {
    List<ExerciseAttribute> collect1 = getAttributes().stream().filter(
        exerciseAttribute -> exerciseAttribute.getProperty().equalsIgnoreCase(metadata.toString())
    ).collect(Collectors.toList());
    return collect1.isEmpty() ? "" : collect1.iterator().next().getValue();
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
  public List<ClientExercise> getExercises() {
    return exercises;
  }

  public void setImageRef(String imageRef) {
    this.imageRef = imageRef;
  }

  @Override
  public Map<String, String> getUnitToValue() {
    Map<String, String> pv = new HashMap<>();
    pv.put(UNIT, unit);
    pv.put(CHAPTER, chapter);
    return pv;
  }

  @Override
  public void addPair(Pair pair) {
    addUnitToValue(pair.getProperty(), pair.getValue());
  }

  @Override
  public void addUnitToValue(String unit, String value) {
    if (unit.equalsIgnoreCase(UNIT)) this.unit = value;
    else if (unit.equalsIgnoreCase(CHAPTER)) this.chapter = value;
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
    //   List<String> speakers = new ArrayList<>();
    //Set<ExerciseAttribute> speakerAttr = new TreeSet<>();
    List<ExerciseAttribute> speakers = attributes
        .stream()
        .filter(exerciseAttribute -> (exerciseAttribute.getProperty().toLowerCase().startsWith(SPEAKER)))
        .sorted(Comparator.comparing(Pair::getProperty))
        .collect(Collectors.toList());

    return speakers.stream().map(Pair::getValue)
        .collect(Collectors.toList());

    // return speakers;
  }

  @Override
  public Map<String, List<ClientExercise>> groupBySpeaker() {
    Map<String, List<ClientExercise>> speakerToExercises = new HashMap<>();
    exercises.forEach(commonExercise -> {
//          logger.info("For " +
//              getID() +
//              " ex " + commonExercise.getID() +
//              commonExercise.getOldID() +
//              " speaker " + getSpeaker(commonExercise) + " " + commonExercise.getForeignLanguage());

          List<ClientExercise> exercises = speakerToExercises.computeIfAbsent(getSpeaker(commonExercise), k -> new ArrayList<>());
          exercises.add(commonExercise);
        }
    );
    return speakerToExercises;
  }

  private String getSpeaker(ClientExercise commonExercise) {
    List<ExerciseAttribute> speakerAttr = getSpeakerAttr(commonExercise);
    return speakerAttr.isEmpty() ? UNKNOWN : speakerAttr.stream().iterator().next().getValue();
  }

  private List<ExerciseAttribute> getSpeakerAttr(ClientExercise commonExercise) {
    return commonExercise
        .getAttributes()
        .stream()
        .filter(exerciseAttribute -> exerciseAttribute.getProperty().toUpperCase().startsWith(SPEAKER1.toUpperCase()))
        .collect(Collectors.toList());
  }

  @Override
  public String getEnglish() {
    return entitle;
  }

  @Override
  public String getMeaning() {
    return "";
  }

  @Override
  public String getForeignLanguage() {
    return fltitle;
  }

  @Override
  public void setEnglish(String english) {
    this.entitle = english;
  }

  public void setForeignLanguage(String foreignLanguage) {
    this.fltitle = foreignLanguage;
  }

  @Override
  public void setMeaning(String meaning) {

  }

  /**
   * TODO : fill in
   *
   * @param score
   */
  @Override
  public void setScore(float score) {
    logger.info("setScore " + score);
    this.score = score;
  }

  /**
   * @param scoreTotal
   */
  @Override
  public void setScores(List<CorrectAndScore> scoreTotal) {
    logger.info("got " + scoreTotal);
  }

  @Override
  public int getRawScore() {
    return Math.round(score / 100F);
  }

  /**
   * @see mitll.langtest.client.list.FacetExerciseList#setProgressBarScore
   * @return
   */
  @Override
  public float getScore() {
    return score / 10000F;
  }

  @Override
  public boolean hasScore() {
    return score > 0F;
  }

  @Override
  public String getFLToShow() {
    return fltitle;
  }

  @Override
  public MutableShell getMutableShell() {
    return this;
  }

  @Override
  public int getNumPhones() {
    return 0;
  }

  @Override
  public STATE getState() {
    return UNSET;
  }

  @Override
  public void setState(STATE state) {
  }

  @Override
  public STATE getSecondState() {
    return UNSET;
  }

  @Override
  public void setSecondState(STATE state) {
  }

  public int getImageid() {
    return imageid;
  }

  @Override
  public List<ClientExercise> getCoreVocabulary() {
    return coreVocabulary;
  }

  public String toString() {
    return "Dialog #" + id +
        "\n\tunit        " + unit +
        "\n\tchapter     " + chapter +
        "\n\ttitle       " + entitle +
        "\n\tfltitle     " + fltitle +
        "\n\torientation " + orientation +
        "\n\timage       " + imageRef +
        "\n\t# ex        " + exercises.size() +
        "\n\t# core      " + coreVocabulary.size() +
        "\n\tattr        " + attributes;
  }
}
