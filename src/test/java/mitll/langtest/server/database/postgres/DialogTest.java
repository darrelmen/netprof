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

package mitll.langtest.server.database.postgres;

import mitll.langtest.server.database.BaseTest;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.*;

public class DialogTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(DialogTest.class);
  public static final int MAX = 200;
  public static final int KOREAN_ID = 46;
  public static final String TOPIC_PRESENTATION_C = "Topic Presentation C";
  public static final String TOPIC_PRESENTATION_A = "Topic Presentation A";
  public static final String PRESENTATION = "presentation";
  public static final String ANY = "Any";
  public static final String CHAPTER = "Chapter";

  @Test
  public void testKP() {
    DatabaseImpl andPopulate = getDatabase().setInstallPath("");

    Project project = andPopulate.getProjectByName("Korean");
    logger.info("korean " + project);
    List<IDialog> dialogs = andPopulate.getDialogDAO().getDialogs(project.getID());

    dialogs.forEach(dialog -> logger.info("dialog " + dialog));

    IDialog iDialog = dialogs.get(0);

    logger.info("First " + iDialog);
    List<String> speakers = iDialog.getSpeakers();

    logger.info("Speakers " + speakers);
    logger.info("Image    " + iDialog.getImageRef());

    iDialog.getAttributes().forEach(exerciseAttribute -> logger.info("\t" + exerciseAttribute));

    logger.info("Exercises : ");
    List<CommonExercise> exercises = iDialog.getExercises();
    /*   exercises.forEach(exercise -> logger.info(getShort(exercise)));*/

    Map<String, List<CommonExercise>> stringListMap = iDialog.groupBySpeaker();
    stringListMap.forEach((k, v) -> {
      logger.info(k + " : ");
      v.forEach(commonExercise -> logger.info(getShort(commonExercise)));
    });

    // exercises.forEach(commonExercise -> logger.info("ex " + commonExercise.getID() + " " + commonExercise.getOldID() + " has " + commonExercise.getDirectlyRelated()));

    // when shown generally, the exercises shouldn't have it
    exercises.forEach(commonExercise -> {
      CommonExercise exerciseByID = project.getExerciseByID(commonExercise.getID());
      if (!exerciseByID.getDirectlyRelated().isEmpty()) {
        logger.info("ex " + commonExercise.getID() + " has context?");
      }
    });

    project.getSectionHelper().report();

    {
      logger.info("OK - unit and chapter only\n\n\n\n");
      List<Pair> pairs = new ArrayList<>();
      pairs.add(new Pair("Unit", "" + 5));
      pairs.add(new Pair(CHAPTER, "" + 17));
      pairs.add(new Pair("page", ANY));
      pairs.add(new Pair("presentation", ANY));
      FilterRequest request = new FilterRequest(-1, pairs, -1);
      FilterResponse typeToValues = project.getSectionHelper().getTypeToValues(request, true);
      logger.info("got " + typeToValues);

      if (false) {
        HashMap<String, Collection<String>> typeToSection = new HashMap<>();
        typeToSection.put("Unit", Collections.singletonList("" + 5));
        typeToSection.put(CHAPTER, Collections.singletonList("" + 17));
        Collection<CommonExercise> exercisesForSelectionState = project.getSectionHelper().getExercisesForSelectionState(typeToSection);

        exercisesForSelectionState.stream().filter(ex -> ex.getEnglish().isEmpty()).forEach(commonExercise -> logger.info(getShort(commonExercise)));
      }
    }

    {
      logger.info("OK - unit and chapter and presentation \n\n\n\n");

      //   project.getSectionHelper().report();
      List<Pair> pairs = new ArrayList<>();
      pairs.add(new Pair("Unit", "" + 5));
      pairs.add(new Pair(CHAPTER, "" + 17));
      pairs.add(new Pair("page", ANY));
      pairs.add(new Pair(PRESENTATION, TOPIC_PRESENTATION_A));
      FilterRequest request = new FilterRequest(-1, pairs, -1);
      FilterResponse typeToValues = project.getSectionHelper().getTypeToValues(request, false);
      logger.info("got " + typeToValues);

      HashMap<String, Collection<String>> typeToSection = new HashMap<>();
      typeToSection.put("Unit", Collections.singletonList("" + 5));
      typeToSection.put(CHAPTER, Collections.singletonList("" + 17));
      typeToSection.put(PRESENTATION, Collections.singletonList(TOPIC_PRESENTATION_A));
      Collection<CommonExercise> exercisesForSelectionState = project.getSectionHelper().getExercisesForSelectionState(typeToSection);

      exercisesForSelectionState.stream().filter(ex -> ex.getEnglish().isEmpty()).forEach(commonExercise -> logger.info(getShort(commonExercise)));
    }
    {
      logger.info("OK - unit and chapter and presentation \n\n\n\n");

      // project.getSectionHelper().report();
      List<Pair> pairs = new ArrayList<>();
      pairs.add(new Pair("Unit", "" + 5));
      pairs.add(new Pair(CHAPTER, "" + 17));
      pairs.add(new Pair("page", ANY));
      pairs.add(new Pair(PRESENTATION, TOPIC_PRESENTATION_C));
      FilterRequest request = new FilterRequest(-1, pairs, -1);
      FilterResponse typeToValues = project.getSectionHelper().getTypeToValues(request, false);
      logger.info("got " + typeToValues);

      HashMap<String, Collection<String>> typeToSection = new HashMap<>();
      typeToSection.put("Unit", Collections.singletonList("" + 5));
      typeToSection.put(CHAPTER, Collections.singletonList("" + 17));
      typeToSection.put(PRESENTATION, Collections.singletonList(TOPIC_PRESENTATION_C));
      Collection<CommonExercise> exercisesForSelectionState = project.getSectionHelper().getExercisesForSelectionState(typeToSection);

      exercisesForSelectionState.stream().filter(ex -> ex.getEnglish().isEmpty()).forEach(commonExercise -> logger.info(getShort(commonExercise)));
    }

    if (false) {
      for (int unit = 1; unit < 9; unit++) {
        List<Pair> pairs = new ArrayList<>();
        pairs.add(new Pair("Unit", "" + unit));
        pairs.add(new Pair(CHAPTER, ANY));
        pairs.add(new Pair("page", ANY));
        pairs.add(new Pair(PRESENTATION, ANY));

        FilterRequest request = new FilterRequest(-1, pairs, -1);
        FilterResponse typeToValues = project.getSectionHelper().getTypeToValues(request, false);
        logger.info("got " + typeToValues);
      }
    }
    andPopulate.close();
  }

  @NotNull
  private String getShort(CommonExercise exercise) {
    return "\t" + exercise.getOldID() + " : " + exercise.getForeignLanguage() + " : " + exercise.getAttributes();
  }
}
