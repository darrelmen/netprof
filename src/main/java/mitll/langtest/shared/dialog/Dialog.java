/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.shared.dialog;

import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @see mitll.langtest.server.database.dialog.DialogDAO#getDialogs
 * @see mitll.langtest.server.database.dialog.DialogReader#addDialogPair
 */
public class Dialog implements IDialog, MutableShell, IMutableDialog {
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

  private long modified;

  private String fltitle;
  private String entitle;

  private String unit;
  private String chapter;
  private String countryCode;

  /**
   *
   */
  private DialogType kind = DialogType.DIALOG;
  private String orientation;
  private String imageRef;
  private List<ExerciseAttribute> attributes = new ArrayList<>();
  private List<ClientExercise> exercises = new ArrayList<>();
  private List<ClientExercise> coreVocabulary = new ArrayList<>();

  private boolean isPrivate;

  private float score = -1;

  public Dialog() {
  }

  public Dialog(IDialog copy) {
    this(copy.getID(),
        copy.getUserid(),
        copy.getProjid(),
        copy.getImageID(),
        copy.getModified(),
        copy.getUnit(),
        copy.getChapter(),
        copy.getOrientation(),
        copy.getImageRef(),
        copy.getForeignLanguage(),
        copy.getEnglish(),

        new ArrayList<>(),//copy.getAttributes(),
        new ArrayList<>(),// copy.getExercises(),
        new ArrayList<>(),//copy.getCoreVocabulary(),

        copy.getKind(),
        copy.getCountryCode(),
        copy.isPrivate()
    );

    copy.getAttributes().forEach(attr -> attributes.add(new ExerciseAttribute(attr)));
    copy.getExercises().forEach(ex -> exercises.add(new Exercise(ex.asCommon())));
    copy.getCoreVocabulary().forEach(ex -> coreVocabulary.add(new Exercise(ex.asCommon())));
  }

  /**
   * @param id
   * @param userid
   * @param projid
   * @param imageid
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
   * @param type
   * @param countryCode
   * @param isPrivate
   * @see mitll.langtest.server.database.dialog.DialogDAO#makeDialog
   * @see BasicDialogReader#addDialogPair
   */
  public Dialog(int id,
                int userid,
                int projid,
                int imageid,
                long modified,
                String unit,
                String chapter,
                String orientation,
                String imageRef,
                String fltitle,
                String entitle,
                List<ExerciseAttribute> attributes,
                List<ClientExercise> exercises,
                List<ClientExercise> coreExercises,
                DialogType type,
                String countryCode,
                boolean isPrivate) {
    this.id = id;
    this.userid = userid;
    this.projid = projid;
    this.imageid = imageid;

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
    this.kind = type;
    this.countryCode = countryCode;
    this.isPrivate = isPrivate;
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
  public String getAttributeValue(DialogMetadata metadata) {
    List<ExerciseAttribute> collect1 = getExerciseAttributes(metadata);
    return collect1.isEmpty() ? "" : collect1.iterator().next().getValue();
  }

  @NotNull
  private List<ExerciseAttribute> getExerciseAttributes(DialogMetadata metadata) {
    return getAttributes()
        .stream()
        .filter(attr -> attr.getProperty().equalsIgnoreCase(metadata.toString()))
        .collect(Collectors.toList());
  }

  public ExerciseAttribute getAttribute(DialogMetadata metadata) {
    List<ExerciseAttribute> exerciseAttributes = getExerciseAttributes(metadata);
    return (exerciseAttributes.isEmpty()) ? null : exerciseAttributes.iterator().next();
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
  public long getModified() {
    return modified;
  }

  /**
   * @return
   */
  @Override
  public DialogType getKind() {
    return kind;
  }

  @Override
  public String getOrientation() {
    return orientation;
  }

  /**
   * Default is default dialog.
   *
   * @return
   */
  @Override
  public String getImageRef() {
    String refToUse = imageRef;

    if (refToUse == null || refToUse.isEmpty()) {
      refToUse = "langtest/cc/" + "dialog.png";
    }

    return refToUse;
  }

  @Override
  public List<ClientExercise> getExercises() {
    return exercises;
  }

  @Override
  public ClientExercise getExByID(int exid) {
    List<ClientExercise> collect = exercises.stream().filter(exercise -> exercise.getID() == exid).limit(1).collect(Collectors.toList());
    return collect.isEmpty() ? null : collect.get(0);
  }

  @Override
  public int getLastID() {
    return getExercises().isEmpty() ? -1 : getExercises().get(getExercises().size() - 1).getID();
  }

  public ClientExercise getLast() {
    return getExercises().isEmpty() ? null : getExercises().get(getExercises().size() - 1);
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
  public boolean addUnitToValue(String unit, String value) {
    if (unit.equalsIgnoreCase(UNIT)) {
      this.unit = value;
      return true;
    } else if (unit.equalsIgnoreCase(CHAPTER)) {
      this.chapter = value;
      return true;
    } else {
      return false;
    }
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

    List<ExerciseAttribute> speakers = attributes
        .stream()
        .filter(exerciseAttribute -> (exerciseAttribute.getProperty().toLowerCase().startsWith(SPEAKER)))
        //  .sorted(Comparator.comparing(Pair::getProperty))
        .collect(Collectors.toList());

    return speakers.stream().map(Pair::getValue)
        .collect(Collectors.toList());
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
    //   logger.info("setScore " + score);
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
    return Math.round(score);
  }

  /**
   * @return
   * @see mitll.langtest.client.list.FacetExerciseList#setProgressBarScore
   */
  @Override
  public float getScore() {
    return score / 100F;
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
  public boolean isContext() {
    return false;
  }

  @Override
  public int getNumContext() {
    return 0;
  }

  public int getImageid() {
    return imageid;
  }

  @Override
  public List<ClientExercise> getCoreVocabulary() {
    return coreVocabulary;
  }

  @Override
  public List<ClientExercise> getBothExercisesAndCore() {
    List<ClientExercise> both = new ArrayList<>(coreVocabulary.size() + exercises.size());
    both.addAll(exercises);
    both.addAll(coreVocabulary);
    return both;
  }

  /**
   * TODO :remove?
   *
   * @return
   */
  // @Override
  public String getCountryCode() {
    return countryCode;
  }

  @Override
  public boolean isSafeToDecode() {
    return false;
  }

  @Override
  public String getName() {
    return getForeignLanguage();
  }

  @Override
  public boolean isPrivate() {
    return isPrivate;
  }

  @Override
  public void setIsPrivate(boolean val) {
    this.isPrivate = val;
  }

  public IMutableDialog getMutable() {
    return this;
  }

  @Override
  public void setOrientation(String orientation) {
    this.orientation = orientation;
  }

  @Override
  public int getImageID() {
    return imageid;
  }

  @Override
  public void setID(int id) {
    this.id = id;
  }

  public void setDialogType(DialogType dialogType) {
    this.kind = dialogType;
  }

  public String toString() {
    return "Dialog #" + id +
        "\n\tkind        " + kind +
        "\n\tunit        " + unit +
        "\n\tchapter     " + chapter +
        "\n\ttitle       " + entitle +
        "\n\tfltitle     " + fltitle +
        "\n\torientation " + orientation +
        "\n\timage       " + imageRef +
        "\n\tprivate     " + isPrivate +
        "\n\t# ex        " + exercises.size() +
        "\n\t# core      " + coreVocabulary.size() +
        "\n\tattr        " + attributes;
  }
}
