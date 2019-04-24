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

package mitll.langtest.server.database.dialog;

import mitll.langtest.server.database.project.Project;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.DialogMetadata;
import mitll.langtest.shared.dialog.DialogStatus;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.Exercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.exercise.MutableExercise;
import mitll.langtest.shared.project.Language;
import mitll.npdata.dao.SlickDialog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Sheet;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;

import static mitll.langtest.shared.dialog.DialogMetadata.FLTITLE;

public class BaseDialogReader {
  public static final String ENGLISH_SPEAKER = "English Speaker";
  protected static final String INTERPRETERSPEAKER = "Interpreter";
  private static final Logger logger = LogManager.getLogger(BaseDialogReader.class);
  private static final String OPT_NETPROF_DIALOG = "/opt/netprof/dialog/";
  private static final List<String> SPEAKER_LABELS = Arrays.asList("A", "B", "C", "D", "E", "F", "I");
  private static final String IMAGES = "images/";
  protected String unit;
  protected String chapter;

  BaseDialogReader(String unit, String chapter) {
    this.unit = unit;
    this.chapter = chapter;
  }
  // private static final String JPG = ".jpg";

  /**
   * @param defaultUser
   * @param projID
   * @param modified
   * @param imageRef
   * @param unit
   * @param chapter
   * @param attributes
   * @param exercises
   * @param coreExercises
   * @param orientation
   * @param title
   * @param fltitle
   * @param outputDialogToSlick
   * @param dialogType
   * @param countryCode
   * @see DialogReader#getDialogsByProp
   */
  protected void addDialogPair(int defaultUser,
                               int projID,
                               Timestamp modified,

                               String imageRef,
                               String unit, String chapter,
                               List<ExerciseAttribute> attributes,
                               List<ClientExercise> exercises,
                               Set<ClientExercise> coreExercises,

                               String orientation, String title, String fltitle,

                               Map<Dialog, SlickDialog> outputDialogToSlick,
                               DialogType dialogType, String countryCode) {
    List<ExerciseAttribute> dialogAttr = new ArrayList<>(attributes);
    dialogAttr.add(new ExerciseAttribute(FLTITLE.toString().toLowerCase(), fltitle, false));

    SlickDialog slickDialog = new SlickDialog(-1,
        defaultUser,
        projID,
        -1,
        -1,
        modified,
        modified,
        unit, chapter,
        dialogType.toString(),
        DialogStatus.DEFAULT.toString(),
        title,
        orientation,
        false
    );


    Dialog dialog = new Dialog(-1, defaultUser, projID, -1, -1, modified.getTime(),
        unit, chapter,
        orientation,
        imageRef,
        fltitle,
        title,

        dialogAttr,
        exercises,
        new ArrayList<>(coreExercises), dialogType, countryCode, false);

    outputDialogToSlick.put(dialog, slickDialog);
  }

  @NotNull
  String getDialogDataDir(Project project) {
    return OPT_NETPROF_DIALOG + project.getLanguage().toLowerCase() + File.separator;
  }

  void addSpeakerAttrbutes(List<ExerciseAttribute> attributes, Set<String> speakers) {
    List<String> speakersList = new ArrayList<>(speakers);
    speakersList
        .forEach(s -> {
          int index = speakersList.indexOf(s);
          if (index == -1) logger.warn("no speaker label for " + s);
          attributes
              .add(new ExerciseAttribute(
                  DialogMetadata.SPEAKER.getCap() +
                      " " + SPEAKER_LABELS.get(index), s, false));
        });
  }

  @NotNull
  String getImageRef(String imageBaseDir, String image) {
    return imageBaseDir + image + ".png";
  }

  /**
   * images/russian/
   *
   * @param project
   * @return
   */
  @NotNull
  String getImageBaseDir(Project project) {
    return IMAGES + project.getLanguage().toLowerCase() + File.separator;
  }

  /**
   * @param speakers  - modified maybe!
   * @param text
   * @param speakerID
   * @param lang
   * @param unitChapterPairs
   * @return
   * @see #readFromSheet(int, Sheet, Project, Project)
   */
  protected Exercise getExercise(String text,
                                 String english,
                                 String transliteration,

                                 String speakerID,
                                 Language lang,
                                 String turnID,
                                 Map<String, String> unitChapterPairs) {
    Exercise exercise = new Exercise();

    MutableExercise mutable = exercise.getMutable();
    if (!turnID.isEmpty()) {
      mutable.setOldID(turnID);
    }

    exercise.setUnitToValue(unitChapterPairs);

    addAttributes(speakerID, lang, exercise);

    mutable.setForeignLanguage(text);
    mutable.setEnglish(english);
    mutable.setTransliteration(transliteration);

    return exercise;
  }

  private void addAttributes(String speakerID, Language lang, Exercise exercise) {
    if (!speakerID.isEmpty()) {
      exercise.addAttribute(new ExerciseAttribute(DialogMetadata.SPEAKER.getCap(), speakerID, false));
    }
    exercise.addAttribute(new ExerciseAttribute(DialogMetadata.LANGUAGE.getCap(), lang.name(), false));
  }

  @NotNull
  protected Map<String, String> getDefaultUnitAndChapter(List<String> typeOrder) {
    Map<String, String> unitToValue = new HashMap<>();
    unitToValue.put(typeOrder.get(0), unit);
    unitToValue.put(typeOrder.get(1), chapter);
    return unitToValue;
  }
}
