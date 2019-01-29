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

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.BaseTest;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.database.project.DialogPopulate;
import mitll.langtest.shared.dialog.DialogMetadata;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static mitll.langtest.shared.project.ProjectMode.DIALOG;

public class DialogTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(DialogTest.class);
  public static final int MAX = 200;
  public static final int KOREAN_ID = 46;
  private static final String TOPIC_PRESENTATION_C = "Topic Presentation C";
  private static final String TOPIC_PRESENTATION_A = "Topic Presentation A";
  public static final String PRESENTATION1 = "presentation";
  private static final String PRESENTATION = PRESENTATION1;
  public static final String ANY1 = "Any";
  private static final String ANY = ANY1;
  private static final String CHAPTER = "Chapter";
  public static final String U5 = "" + 5;
  public static final String UNIT1 = "Unit";
  public static final String UNIT = UNIT1;
  public static final String C17 = "" + 17;
  public static final String PAGE = "page";
  public static final String KOREAN = "Korean";

  @Test
  public void testDict() {
    testDialogPopulate(KOREAN);
  }

  @Test
  public void testInterpreter() {
    testDialogPopulate("Chinese");
  }

  @Test
  public void testInterpreterStored() {
    DatabaseImpl andPopulate = getDatabase();
    Project project = andPopulate.getProject(12);
    report(andPopulate, project);
  }


  @Test
  public void testInterpreterFrenchToRecord() {
    DatabaseImpl andPopulate = getDatabase();
    andPopulate.getProject(12);
    Project project = andPopulate.getProjectManagement().getProductionByLanguage(Language.FRENCH);
    int projectid = project.getID();

    FilterRequest request = new FilterRequest()
        .setRecordRequest(true)
        .setMode(DIALOG);

    // project.getTypeOrder().forEach(type -> request.addPair(new Pair(type, SectionHelper.ANY)));

    request.addPair(new Pair("Book", ANY1));
    request.addPair(new Pair("Module", ANY1));
    request.addPair(new Pair("LANGUAGE", ANY1));
    request.addPair(new Pair("SPEAKER", ANY1));
    //request.addPair(new Pair("SPEAKER","A"));

    logger.info("types " + request + " for " + project.getTypeOrder());

    FilterResponse typeToValues = getTypeToValues(andPopulate, projectid, request);

    logger.info("typeToValues for " +
        "\n\treq          " + request +
        "\n\ttype->values " + typeToValues);

  }

    @Test
  public void testInterpreterFrench() {
    DatabaseImpl andPopulate = getDatabase();
    andPopulate.getProject(12);
    Project project = andPopulate.getProjectManagement().getProductionByLanguage(Language.FRENCH);
    int projectid = project.getID();

    FilterRequest request = new FilterRequest()
        .setRecordRequest(true)
        .setMode(DIALOG);

    // project.getTypeOrder().forEach(type -> request.addPair(new Pair(type, SectionHelper.ANY)));

    request.addPair(new Pair("Book", ANY1));
    request.addPair(new Pair("Module",  ANY1));
    request.addPair(new Pair("LANGUAGE",  ANY1));
    request.addPair(new Pair("SPEAKER",  ANY1));
    //request.addPair(new Pair("SPEAKER","A"));

    logger.info("types " + request + " for " + project.getTypeOrder());

    FilterResponse typeToValues = getTypeToValues(andPopulate, projectid, request);

    logger.info("typeToValues for " +
        "\n\treq          " + request +
        "\n\ttype->values " + typeToValues);

//    request.addPair(new Pair("Book","1"));
//    request.addPair(new Pair("SPEAKER", "English Speaker"));
//
//    logger.info("types " + request + " for " + project.getTypeOrder());
//
//    typeToValues = getTypeToValues(andPopulate, projectid, request);
//
//    logger.info("typeToValues for " +
//        "\n\treq          " + request +
//        "\n\ttype->values " + typeToValues);
//

    request = new FilterRequest()
        .setOnlyUninspected(true)
        .setMode(DIALOG);

    logger.info("types " + request + " for " + project.getTypeOrder());

    typeToValues = getTypeToValues(andPopulate, projectid, request);

    logger.info("typeToValues for " +
        "\n\treq          " + request +
        "\n\ttype->values " + typeToValues);
  }


  @Test
  public void testNormalFrench() {
    DatabaseImpl andPopulate = getDatabase();
    andPopulate.getProject(12);
    Project project = andPopulate.getProjectManagement().getProductionByLanguage(Language.FRENCH);
    int projectid = project.getID();

    FilterRequest request = new FilterRequest()
        .setRecordRequest(true);

    // project.getTypeOrder().forEach(type -> request.addPair(new Pair(type, SectionHelper.ANY)));

    request.addPair(new Pair("Book", "1"));
    //request.addPair(new Pair("SPEAKER","A"));

    logger.info("types " + request + " for " + project.getTypeOrder());

    FilterResponse typeToValues = getTypeToValues(andPopulate, projectid, request);

    logger.info("typeToValues for " +
        "\n\treq          " + request +
        "\n\ttype->values " + typeToValues);

//    request.addPair(new Pair("Book","1"));
//    request.addPair(new Pair("SPEAKER","English Speaker"));
//
//    logger.info("types " + request + " for " + project.getTypeOrder());
//
//    typeToValues = getTypeToValues(andPopulate, projectid, request);
//
//    logger.info("typeToValues for " +
//        "\n\treq          " + request+
//        "\n\ttype->values " + typeToValues);


    request.setOnlyUninspected(true);

    logger.info("types " + request + " for " + project.getTypeOrder());

    typeToValues = getTypeToValues(andPopulate, projectid, request);

    logger.info("typeToValues for " +
        "\n\treq          " + request +
        "\n\ttype->values " + typeToValues);
  }

  private FilterResponse getTypeToValues(DatabaseImpl andPopulate, int projectid, FilterRequest request) {
    return andPopulate.getFilterResponseHelper().getTypeToValues(request, projectid, 6);
  }

  @Test
  public void testInterpreterRecord() {
    DatabaseImpl andPopulate = getDatabase();
    int projectid = 12;
    Project project = andPopulate.getProject(projectid);

    FilterRequest request = new FilterRequest().setRecordRequest(true).setMode(DIALOG);
    project.getTypeOrder().forEach(type -> request.addPair(new Pair(type, SectionHelper.ANY)));

    FilterResponse typeToValues = getTypeToValues(andPopulate, projectid, request);

    logger.info("typeToValues " + typeToValues);

    ExerciseListRequest request1 = new ExerciseListRequest(1, 6).setMode(DIALOG);
    request1.setOnlyUnrecordedByMe(true);
    HashMap<String, Collection<String>> typeToSelection = new HashMap<>();
    typeToSelection.put(UNIT1, Collections.singleton("1"));
    typeToSelection.put(DialogMetadata.LANGUAGE.name(), Collections.singleton(Language.ENGLISH.name()));
    request1.setTypeToSelection(typeToSelection);

    {
      List<CommonExercise> exercisesForSelectionState =
          andPopulate.getFilterResponseHelper().getExercisesForSelectionState(request1, projectid);

      exercisesForSelectionState.forEach(ex -> logger.info("ENGLISH got " + ex.getID() + " " + ex.getEnglish() + " " + ex.getForeignLanguage()));
    }

    typeToSelection.put(DialogMetadata.LANGUAGE.name(), Collections.singleton(Language.MANDARIN.name()));

    {
      List<CommonExercise> exercisesForSelectionState =
          andPopulate.getFilterResponseHelper().getExercisesForSelectionState(request1, projectid);

      exercisesForSelectionState.forEach(ex -> logger.info("CHINESE " +
          "\n\ttype->sel " + typeToSelection +
          " got " + ex.getID() + " " + ex.getEnglish() + " " + ex.getForeignLanguage() + " " + ex.getTokens()));
    }

    typeToSelection.put(DialogMetadata.SPEAKER.name(), Collections.singleton("B"));

    {
      List<CommonExercise> exercisesForSelectionState =
          andPopulate.getFilterResponseHelper().getExercisesForSelectionState(request1, projectid);

      exercisesForSelectionState.forEach(ex -> logger.info("CHINESE" +
          "\n\ttype->sel " + typeToSelection +
          " (A) got " + ex.getID() + " " + ex.getEnglish() + " " + ex.getForeignLanguage() + " " + ex.getTokens()));
    }

    typeToSelection.remove(UNIT1);

    {
      List<CommonExercise> exercisesForSelectionState =
          andPopulate.getFilterResponseHelper().getExercisesForSelectionState(request1, projectid);

      exercisesForSelectionState.forEach(ex -> logger.info("CHINESE got " + ex.getID() + " " + ex.getEnglish() + " " + ex.getForeignLanguage() + " " + ex.getTokens()));
    }


    //  report(andPopulate, project);
  }

  private void testDialogPopulate(String korean) {
    DatabaseImpl andPopulate = getDatabase();
    Project project = andPopulate.getProject(12);
//    Project project = andPopulate.getProjectByName(korean);

    if (!new DialogPopulate(andPopulate, getPathHelper(andPopulate)).populateDatabase(project, andPopulate.getProjectManagement().getProductionByLanguage(Language.ENGLISH))) {
      logger.info("testDialogPopulate project " + project + " already has dialog data.");
    }

    report(andPopulate, project);
  }

  private void report(DatabaseImpl andPopulate, Project project) {
    List<IDialog> dialogs = andPopulate.getDialogDAO().getDialogs(project.getID());
    dialogs.forEach(iDialog -> {
      logger.info("dialog " + iDialog);

      logger.info("sp    " + iDialog.getSpeakers());
      logger.info("attr  " + iDialog.getAttributes());
      // logger.info("by sp " + iDialog.groupBySpeaker());
      //   logger.info("core  " + iDialog.getCoreVocabulary().size());
      iDialog.getCoreVocabulary().forEach(clientExercise -> {
        List<String> tokens = project.getAudioFileHelper().getASR().getTokens(clientExercise.getForeignLanguage(), clientExercise.getTransliteration());
//        String pronunciationsFromDictOrLTS = project.getAudioFileHelper().getPronunciationsFromDictOrLTS(clientExercise.getForeignLanguage(), clientExercise.getTransliteration());
        logger.info("core " + clientExercise.getForeignLanguage() + " -> " + tokens);
      });
      // logger.info("\n\n\n");

//      iDialog.getExercises().forEach(clientExercise -> clientExercise.getAttributes().forEach(exerciseAttribute -> logger.info("\t" + exerciseAttribute)));
      iDialog.getExercises().forEach(clientExercise -> {
        List<ExerciseAttribute> collect = clientExercise.getAttributes().stream().filter(exerciseAttribute -> exerciseAttribute.getProperty().equalsIgnoreCase(DialogMetadata.LANGUAGE.name())).collect(Collectors.toList());
        if (!collect.isEmpty()) {
          boolean isEnglish = collect.get(0).getValue().equalsIgnoreCase(Language.ENGLISH.name());
          if (!isEnglish) {
//            String pronunciationsFromDictOrLTS = project.getAudioFileHelper().getPronunciationsFromDictOrLTS(clientExercise.getForeignLanguage(), clientExercise.getTransliteration());
//            logger.info(clientExercise.getForeignLanguage() + " -> " + pronunciationsFromDictOrLTS);

            // List<String> tokens = project.getAudioFileHelper().getASR().getTokens(clientExercise.getForeignLanguage(), clientExercise.getTransliteration());

            List<String> tokens = clientExercise.getTokens();
            if (tokens == null) {
              logger.error("ex #" + clientExercise.getID() + " " + clientExercise.getForeignLanguage() + " -> " + tokens);
            }
            logger.info("ex #" + clientExercise.getID() + " " + clientExercise.getForeignLanguage() + " -> " + tokens);

          }
        }

      });
    });
  }


  @NotNull
  private static PathHelper getPathHelper(DatabaseImpl database) {
    return new PathHelper("war", database.getServerProps());
  }


  @Test
  public void testEx() {
    DatabaseImpl andPopulate = getDatabase().setInstallPath("");

    Project project = andPopulate.getProjectByName(KOREAN);

    if (project == null) {
      logger.warn("no korean");
    } else {
      List<ClientExercise> all = new ArrayList<>();

      List<IDialog> dialogs = andPopulate.getDialogDAO().getDialogs(project.getID());
      dialogs.forEach(iDialog -> {
        logger.info("dialog " + iDialog);

        logger.info("sp    " + iDialog.getSpeakers());
        logger.info("attr  " + iDialog.getAttributes());
        logger.info("by sp " + iDialog.groupBySpeaker());
        logger.info("core  " + iDialog.getCoreVocabulary());
        logger.info("\n\n\n");

//      iDialog.getExercises().forEach(clientExercise -> clientExercise.getAttributes().forEach(exerciseAttribute -> logger.info("\t" + exerciseAttribute)));
        all.addAll(iDialog.getExercises());
      });
      logger.info("total is " + all.size());
//    assertEquals("onetwo", result);
    }
  }

  @Test
  public void testSH() {
    DatabaseImpl andPopulate = getDatabase().setInstallPath("");

    Project project = andPopulate.getProjectByName(KOREAN);
    ISection<IDialog> dialogSectionHelper = project.getDialogSectionHelper();


    List<IDialog> dialogs = andPopulate.getDialogDAO().getDialogs(project.getID());

    // dialogs.forEach(dialog -> logger.info("dialog " + dialog));

    IDialog iDialog = dialogs.get(0);

    List<ClientExercise> coreVocabulary = iDialog.getCoreVocabulary();
    logger.info("\n\n\tgot " + coreVocabulary.size() + " core");
    coreVocabulary.forEach(clientExercise -> logger.info("\t" + clientExercise.getID() +
        " " + clientExercise.getEnglish() + " " + clientExercise.getForeignLanguage()));
    //  project.getSectionHelper().report();

    {
      List<Pair> pairs = new ArrayList<>();
      pairs.add(new Pair(UNIT, U5));
      pairs.add(new Pair(CHAPTER, C17));
      pairs.add(new Pair(PAGE, ANY));
      pairs.add(new Pair(PRESENTATION1, ANY));
      FilterRequest request = new FilterRequest(-1, pairs, -1);
      FilterResponse typeToValues = dialogSectionHelper.getTypeToValues(request, false);
      logger.info("got " + typeToValues);
    }
    {
      List<Pair> pairs = new ArrayList<>();
      pairs.add(new Pair(UNIT, U5));
      pairs.add(new Pair(CHAPTER, C17));

      FilterRequest request = new FilterRequest(-1, pairs, -1);
      FilterResponse typeToValues = dialogSectionHelper.getTypeToValues(request, false);
      logger.info("got " + typeToValues);
    }


    {
      List<Pair> pairs = new ArrayList<>();
      pairs.add(new Pair(UNIT, U5));
      pairs.add(new Pair(CHAPTER, C17));

      HashMap<String, Collection<String>> objectObjectHashMap = new HashMap<>();
      objectObjectHashMap.put(UNIT, Collections.singletonList(U5));
      objectObjectHashMap.put(CHAPTER, Collections.singletonList(C17));

      Collection<IDialog> exercisesForSelectionState = dialogSectionHelper.getExercisesForSelectionState(objectObjectHashMap);
      logger.info("got " + exercisesForSelectionState);
    }
  }

  /**
   * Test adding the dialog data.
   */
  @Test
  public void testEnglishFromCannedData() {
    DatabaseImpl andPopulate = getDatabase().setInstallPath("");

    Project project = andPopulate.getProjectByName("English");

    logger.info("english " + project);
    List<IDialog> dialogs = andPopulate.getDialogDAO().getDialogs(project.getID());

    dialogs.forEach(dialog -> logger.info("dialog " + dialog));

    IDialog iDialog = dialogs.get(0);

    logger.info("First " + iDialog);
    List<String> speakers = iDialog.getSpeakers();

    logger.info("Speakers " + speakers);
    logger.info("Image    " + iDialog.getImageRef());

    iDialog.getAttributes().forEach(exerciseAttribute -> logger.info("\t" + exerciseAttribute));

    logger.info("Exercises : ");
    List<ClientExercise> exercises = iDialog.getExercises();
    /*   exercises.forEach(exercise -> logger.info(getShort(exercise)));*/

    Map<String, List<ClientExercise>> stringListMap = iDialog.groupBySpeaker();
    stringListMap.forEach((k, v) -> {
      logger.info(k + " : ");
      v.forEach(commonExercise -> logger.info(getShort(commonExercise)));
    });
  }

  /**
   * Test adding the dialog data.
   */
  @Test
  public void testKPFromCannedData() {
    DatabaseImpl andPopulate = getDatabase().setInstallPath("");

    Project project = andPopulate.getProjectByName(KOREAN);
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
    List<ClientExercise> exercises = iDialog.getExercises();
    /*   exercises.forEach(exercise -> logger.info(getShort(exercise)));*/

    Map<String, List<ClientExercise>> stringListMap = iDialog.groupBySpeaker();
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

    List<ClientExercise> coreVocabulary = iDialog.getCoreVocabulary();
    logger.info("\n\n\tgot " + coreVocabulary.size() + " core");
    coreVocabulary.forEach(clientExercise -> logger.info("\t" + clientExercise.getID() +
        " " + clientExercise.getEnglish() + " " + clientExercise.getForeignLanguage()));
    //  project.getSectionHelper().report();

    {
      logger.info("OK - unit and chapter only\n\n\n\n");
      List<Pair> pairs = new ArrayList<>();
      pairs.add(new Pair(UNIT, U5));
      pairs.add(new Pair(CHAPTER, C17));
      pairs.add(new Pair(PAGE, ANY));
      pairs.add(new Pair(PRESENTATION1, ANY));
      FilterRequest request = new FilterRequest(-1, pairs, -1);
      FilterResponse typeToValues = project.getSectionHelper().getTypeToValues(request, false);
      logger.info("got " + typeToValues);

      if (false) {
        HashMap<String, Collection<String>> typeToSection = new HashMap<>();
        typeToSection.put(UNIT, Collections.singletonList(U5));
        typeToSection.put(CHAPTER, Collections.singletonList(C17));
        Collection<CommonExercise> exercisesForSelectionState = project.getSectionHelper().getExercisesForSelectionState(typeToSection);

        exercisesForSelectionState.stream().filter(ex -> ex.getEnglish().isEmpty()).forEach(commonExercise -> logger.info(getShort(commonExercise)));
      }
    }

    {
      logger.info("OK - unit and chapter and presentation \n\n\n\n");

      //   project.getSectionHelper().report();
      List<Pair> pairs = new ArrayList<>();
      pairs.add(new Pair(UNIT, U5));
      pairs.add(new Pair(CHAPTER, C17));
      pairs.add(new Pair(PAGE, ANY));
      pairs.add(new Pair(PRESENTATION, TOPIC_PRESENTATION_A));
      FilterRequest request = new FilterRequest(-1, pairs, -1);
      FilterResponse typeToValues = project.getSectionHelper().getTypeToValues(request, false);
      logger.info("got " + typeToValues);

      HashMap<String, Collection<String>> typeToSection = new HashMap<>();
      typeToSection.put(UNIT, Collections.singletonList(U5));
      typeToSection.put(CHAPTER, Collections.singletonList(C17));
      typeToSection.put(PRESENTATION, Collections.singletonList(TOPIC_PRESENTATION_A));
      Collection<CommonExercise> exercisesForSelectionState = project.getSectionHelper().getExercisesForSelectionState(typeToSection);

      exercisesForSelectionState.stream().filter(ex -> ex.getEnglish().isEmpty()).forEach(commonExercise -> logger.info(getShort(commonExercise)));
    }
    {
      logger.info("OK - unit and chapter and presentation \n\n\n\n");

      // project.getSectionHelper().report();
      List<Pair> pairs = new ArrayList<>();
      pairs.add(new Pair(UNIT, U5));
      pairs.add(new Pair(CHAPTER, C17));
      pairs.add(new Pair(PAGE, ANY));
      pairs.add(new Pair(PRESENTATION, TOPIC_PRESENTATION_C));
      FilterRequest request = new FilterRequest(-1, pairs, -1);
      FilterResponse typeToValues = project.getSectionHelper().getTypeToValues(request, false);
      logger.info("got " + typeToValues);

      HashMap<String, Collection<String>> typeToSection = new HashMap<>();
      typeToSection.put(UNIT, Collections.singletonList(U5));
      typeToSection.put(CHAPTER, Collections.singletonList(C17));
      typeToSection.put(PRESENTATION, Collections.singletonList(TOPIC_PRESENTATION_C));
      Collection<CommonExercise> exercisesForSelectionState = project.getSectionHelper().getExercisesForSelectionState(typeToSection);

      exercisesForSelectionState
          .stream()
          .filter(ex -> ex
              .getEnglish()
              .isEmpty())
          .forEach(commonExercise -> logger.info(getShort(commonExercise)));
    }

    if (false) {
      for (int unit = 1; unit < 9; unit++) {
        List<Pair> pairs = new ArrayList<>();
        pairs.add(new Pair(UNIT, "" + unit));
        pairs.add(new Pair(CHAPTER, ANY));
        pairs.add(new Pair(PAGE, ANY));
        pairs.add(new Pair(PRESENTATION, ANY));

        FilterRequest request = new FilterRequest(-1, pairs, -1);
        FilterResponse typeToValues = project.getSectionHelper().getTypeToValues(request, false);
        logger.info("got " + typeToValues);
      }
    }
    andPopulate.close();
  }

  @NotNull
  private String getShort(ClientExercise exercise) {
    return "\t" + exercise.getOldID() + " : " + exercise.getForeignLanguage() + " : " + exercise.getAttributes();
  }
}
