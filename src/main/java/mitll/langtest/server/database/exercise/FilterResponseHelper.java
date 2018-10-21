package mitll.langtest.server.database.exercise;

import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class FilterResponseHelper {
  private static final Logger logger = LogManager.getLogger(FilterResponseHelper.class);
  //private static final String ANY = "Any";
  private static final String LISTS = "Lists";


  private DatabaseServices databaseServices;

  /**
   * TODO : remove?
   */
  //private static final String RECORDED1 = "Recorded";
  public FilterResponseHelper(DatabaseServices databaseServices) {
    this.databaseServices = databaseServices;
  }

  public FilterResponse getTypeToValues(FilterRequest request, int projid, int userID) {
    logger.info("getTypeToValues " + request);
    ISection<CommonExercise> sectionHelper = databaseServices.getSectionHelper(projid);
    if (sectionHelper == null) {
      logger.info("getTypeToValues no reponse...");// + "\n\ttype->selection" + typeToSelection);
      return new FilterResponse();
    } else {
      if (request.isRecordRequest()) { //how no user???
        return getFilterResponseForRecording(request, projid, userID);
      } else if (request.isOnlyUninspected()) {
        ExerciseListRequest request1 = new ExerciseListRequest()
            .setOnlyUninspected(true)
            .setOnlyExamples(request.isExampleRequest())
            .setUserID(userID);

        return getFilterResponse(request, projid, request1);
      } else {
        FilterResponse response = sectionHelper.getTypeToValues(request, false);
        addUserListFacet(request, response);
//        int userFromSessionID = getUserIDFromSessionOrDB();
//        if (userFromSessionID != -1) {
////        logger.info("getTypeToValues got " + userFromSession);
//          //       logger.info("getTypeToValues isRecordRequest " + request.isRecordRequest());
//
//
//        }

        return response;
      }
    }
  }

  private void addUserListFacet(FilterRequest request, FilterResponse typeToValues) {
    int userListID = request.getUserListID();
    UserList<CommonShell> next = userListID != -1 ? databaseServices.getUserListManager().getSimpleUserListByID(userListID) : null;

    if (next != null) {  // echo it back
      //logger.info("\tgetTypeToValues " + request + " include list " + next);
      typeToValues.getTypesToInclude().add(LISTS);
      Set<MatchInfo> value = new HashSet<>();
      value.add(new MatchInfo(next.getName(), next.getNumItems(), userListID, false, ""));
      typeToValues.getTypeToValues().put(LISTS, value);
    }
  }

  private void populate(int projid,
                        Collection<CommonExercise> all,
                        ISection<CommonExercise> sectionHelper,
                        List<String> typeOrder) {
    Map<Integer, ExerciseAttribute> allAttributesByProject = databaseServices.getUserExerciseDAO().getExerciseAttribute().getIDToPair(projid);
    getExercises(all, typeOrder, sectionHelper, databaseServices.getProject(projid), allAttributesByProject);
  }

  /**
   * @param all
   * @param typeOrder
   * @param sectionHelper
   * @param lookup
   * @return
   * @paramx addTypesToSection
   * @seex #getByProject
   * @seex #getContextByProject
   */
  private void getExercises(Collection<CommonExercise> all,
                            List<String> typeOrder,
                            ISection<CommonExercise> sectionHelper,

                            Project lookup,
                            Map<Integer, ExerciseAttribute> allByProject) {
    List<List<Pair>> allAttributes = new ArrayList<>();

    List<String> baseTypeOrder = lookup.getBaseTypeOrder();

    // List<CommonExercise> copy = new ArrayList<>();

//    List<SlickExercisePhone> pairs = new ArrayList<>();

    long then = System.currentTimeMillis();
    {
      Collection<String> allFacetTypes = allByProject
          .values()
          .stream()
          .filter(ExerciseAttribute::isFacet)
          .map(Pair::getProperty)
          .collect(Collectors.toCollection(HashSet::new));

/*
      logger.info("all facet types:");
      allFacetTypes.forEach(type -> logger.info("\t" + type));
*/

//    logger.info("ExToPhones " + exToPhones.size());
      //  logger.info("examining  " + all.size() + " exercises...");

      int n = 0;
      for (CommonExercise exercise : all) {
        List<Pair> e = getPairs(exercise, baseTypeOrder, sectionHelper, allFacetTypes);

    /*    e.forEach(pair -> {
          if (pair.getProperty().startsWith("Speaker")) {
            logger.info("got speaker attr " + pair);
          }
        });
*/
        allAttributes.add(e);
        //   copy.add(exercise);
      }

      // if (!pairs.isEmpty()) {
      //   logger.info("updating " + pairs.size() + " exercises for num phones.");
      //  }
    }
    // long then2 = System.currentTimeMillis();


    long now = System.currentTimeMillis();
//    if (now - then2 > 50) {
//      logger.info("getExercises took " + (now - then) + " to update # phones on " + pairs.size() + " exercises.");
//    }
    if (now - then > 50) {
      logger.info("getExercises took " + (now - then) + " to attach attributes to " + all.size() + " exercises.");
    }

    if (true) {
      logger.info("getExercises type order " + typeOrder);

      sectionHelper.rememberTypesInOrder(typeOrder, allAttributes);
    }
    //  logger.info("getExercises created " + copy.size() + " exercises");
    // return copy;
  }

  private List<Pair> getPairs(CommonExercise exercise,
                              Collection<String> baseTypeOrder,
                              ISection<CommonExercise> sectionHelper,
                              //Exercise exercise,
                              Collection<String> attrTypes) {
    List<Pair> pairs = getUnitToValue(exercise, baseTypeOrder, sectionHelper);

//    if (exercise.isPredefined() && !slick.iscontext()) {
    //addPairsToSectionHelper(sectionHelper, exercise, attrTypes, pairs);
    sectionHelper.addPairs(exercise, exercise, attrTypes, pairs);
    //  }

    return pairs;
  }

  private List<Pair> getUnitToValue(CommonExercise slick, Collection<String> typeOrder, ISection<CommonExercise> sectionHelper) {
    int id = slick.getID();
    Iterator<String> iterator = typeOrder.iterator();
    String unit = slick.getUnitToValue().get(iterator.next());
    String lesson = iterator.hasNext() ? slick.getUnitToValue().get(iterator.next()) : "";
    if (lesson == null) {
      logger.warn("hmm no lesson value on " + slick.getUnitToValue() + " for " +slick.getID() + " " + slick.getEnglish());
      lesson = "";
    }
    boolean ispredef = slick.isPredefined();

    return sectionHelper.getPairs(typeOrder, id, unit, lesson, ispredef);
  }

  /**
   * So make a new SectionHelper from the results of the filter for unrecorded.
   * Want to do similar thing with other filtered lists - items that need to be fixed, for instance.
   *
   * @param request
   * @param userFromSessionID
   * @return
   * @paramx response
   */
  private FilterResponse getFilterResponseForRecording(FilterRequest request, int projectID, int userFromSessionID) {
    ExerciseListRequest request1 = new ExerciseListRequest()
        .setOnlyUnrecordedByMe(true)
        .setOnlyExamples(request.isExampleRequest())
        .setUserID(userFromSessionID);

    return getFilterResponse(request, projectID, request1);
  }

  /**
   * First make an exercise list of just what you're looking for, then build a type hierarchy on the fly from it.
   * @param request
   * @param projectID
   * @param request1
   * @return
   */
  private FilterResponse getFilterResponse(FilterRequest request, int projectID, ExerciseListRequest request1) {
    logger.info("getFilterResponse exercise req " + request1);

    SectionHelper<CommonExercise> unrecordedSectionHelper = getSectionHelperFromFiltered(projectID, request1);

    // unrecordedSectionHelper.report();

    FilterResponse typeToValues = unrecordedSectionHelper.getTypeToValues(request, false);

    logger.info("getFilterResponse resp " + typeToValues);

    return typeToValues;
  }

  @NotNull
  private SectionHelper<CommonExercise> getSectionHelperFromFiltered(int projectID, ExerciseListRequest request1) {
    List<CommonExercise> filtered = filterExercises(request1, getExercises(projectID), projectID);

    logger.info("getFilterResponse build section helper from " + filtered.size());

    ISection<CommonExercise> sectionHelper = getSectionHelper(projectID);
    List<String> typeOrder = sectionHelper.getTypeOrder();


    SectionHelper<CommonExercise> unrecordedSectionHelper = sectionHelper.getCopy(filtered);

    logger.info("getFilterResponse types " + unrecordedSectionHelper.getTypeOrder() + " vs " + typeOrder);
    logger.info("getFilterResponse type->distinct " + unrecordedSectionHelper.getTypeToDistinct());
    populate(projectID, filtered, unrecordedSectionHelper, typeOrder);
    return unrecordedSectionHelper;
  }

  protected ISection<CommonExercise> getSectionHelper(int projectID) {
    return databaseServices.getSectionHelper(projectID);
  }

  /**
   * TODO : this doesn't make sense - we need to do this on all projects, once.
   * Here it's just doing it on the first project that's asked for.
   * <p>
   * TODO : remove duplicate
   * Called from the client:
   *
   * @return
   * @seex #getExerciseWhenNoUnitChapter
   * @seex #getExerciseWhenNoUnitChapter(ExerciseListRequest, int, UserList)
   */
  private List<CommonExercise> getExercises(int projectID) {
    long then = System.currentTimeMillis();
    List<CommonExercise> exercises = databaseServices.getExercises(projectID, false);
    long now = System.currentTimeMillis();
    if (now - then > 200) {
      logger.info("getExercises took " + (now - then) + " millis to get the raw exercise list for " + projectID);// getLanguage(projectID));
    }

    return exercises;
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#filterExercises
   * @param request
   * @param exercises
   * @param projid
   * @return
   */
  public List<CommonExercise> filterExercises(ExerciseListRequest request,
                                              List<CommonExercise> exercises,
                                              int projid) {
//    logger.info("filter req " + request);
    exercises = filterByUnrecorded(request, exercises, projid);

    if (request.isOnlyWithAudioAnno()) {
      exercises = filterByOnlyAudioAnno(request.isOnlyWithAudioAnno(), exercises);
    }
    if (request.isOnlyDefaultAudio()) {
      exercises = filterByOnlyDefaultAudio(request.isOnlyDefaultAudio(), exercises);
    }
    if (request.isOnlyUninspected()) {
      exercises = filterByUninspected(exercises);
    }
 /*   if (request.isOnlyForUser()) {
      exercises = filterOnlyPracticedByUser(request, exercises, projid);
    }*/
    return exercises;
  }


  /**
   * @param onlyAudioAnno
   * @param exercises
   * @return
   * @seex #getExerciseIds
   */
  private List<CommonExercise> filterByOnlyAudioAnno(boolean onlyAudioAnno,
                                                     List<CommonExercise> exercises) {
    if (onlyAudioAnno) {
      Collection<Integer> audioAnnos = databaseServices.getUserListManager().getAudioAnnos();
      List<CommonExercise> copy = new ArrayList<>();
      for (CommonExercise exercise : exercises) {
        if (audioAnnos.contains(exercise.getID())) copy.add(exercise);
      }
      return copy;
    } else {
      return exercises;
    }
  }

  private List<CommonExercise> filterByOnlyDefaultAudio(boolean onlyDefault,
                                                        List<CommonExercise> exercises) {
    if (onlyDefault) {
      List<CommonExercise> copy = new ArrayList<>();
      for (CommonExercise exercise : exercises) {
        for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) {
          if (audioAttribute.getUserid() == BaseUserDAO.DEFAULT_USER_ID) {
            copy.add(exercise);
            break;
          }
        }
      }
      return copy;
    } else {
      return exercises;
    }
  }

  /**
   * Remove any items that have been inspected already.
   *
   * @param exercises
   * @return
   */
  private List<CommonExercise> filterByUninspected(Collection<CommonExercise> exercises) {
    long then = System.currentTimeMillis();
    Collection<Integer> inspected = databaseServices.getStateManager().getInspectedExercises();
    long now = System.currentTimeMillis();
    logger.info("filterByUninspected found " + inspected.size() + " in "+(now-then));
    List<CommonExercise> copy = new ArrayList<>();
    for (CommonExercise exercise : exercises) {
      if (!inspected.contains(exercise.getID())) {
        copy.add(exercise);
      }
    }
    return copy;
  }


  /**
   * For all the exercises the user has not recorded, do they have the required reg and slow speed recordings by a matching gender.
   * <p>
   * Or if looking for example audio, find ones missing examples.
   *
   * @param exercises to filter
   * @param projid
   * @return exercises missing audio, what we want to record
   * @paramx userID                   exercise not recorded by this user and matching the user's gender
   * @paramx onlyUnrecordedByMyGender do we filter by gender
   * @paramx onlyExamples             only example audio
   * @seex #getExerciseIds
   * @seex #getExercisesForSelectionState
   * @seex #filterExercises
   */
  private List<CommonExercise> filterByUnrecorded(
      ExerciseListRequest request,
      List<CommonExercise> exercises,
      int projid) {
    if (request.isOnlyUnrecordedByMe()) {
      logger.info("filterByUnrecorded : Filter for matching gender to " + request.getUserID());
      return getRecordFilterExercisesMatchingGender(request.getUserID(), exercises, projid, request.isOnlyExamples(), false);
    } else if (request.isOnlyRecordedByMatchingGender()) {
      logger.info("filterByUnrecorded : Filter for matching gender to " + request.getUserID());
      return getRecordFilterExercisesMatchingGender(request.getUserID(), exercises, projid, request.isOnlyExamples(), true);
    } else {
      return request.isOnlyExamples() ? getExercisesWithContext(exercises) : exercises;
    }
  }

  /**
   * TODO : way too much work here... why go through all exercises?
   * TODO : why return all exercises?
   *
   * @param userID
   * @param exercises
   * @param projid
   * @param onlyExamples
   * @param onlyRecorded
   * @return
   */
  @NotNull
  private List<CommonExercise> getRecordFilterExercisesMatchingGender(int userID,
                                                                      Collection<CommonExercise> exercises,
                                                                      int projid,
                                                                      boolean onlyExamples,
                                                                      boolean onlyRecorded) {

    Set<Integer> unrecordedIDs = new HashSet<>(exercises.size());

    Map<Integer, String> exToTranscript = new HashMap<>();

    for (CommonExercise exercise : exercises) {
      if (onlyExamples) {
        exercise.getDirectlyRelated().forEach(dir -> {
          int id = dir.getID();
          unrecordedIDs.add(id);
          exToTranscript.put(id, dir.getForeignLanguage());
        });
      } else {
        int id = exercise.getID();
        unrecordedIDs.add(id);
        exToTranscript.put(id, exercise.getForeignLanguage());
      }
    }

    logger.info("getRecordFilterExercisesMatchingGender " + onlyExamples +
        "\n\texToTranscript " + exToTranscript.size());
    Collection<Integer> recordedBySameGender = getRecordedByMatchingGender(userID, projid, onlyExamples, exToTranscript);

    logger.info("getRecordFilterExercisesMatchingGender" +
        "\n\tall exercises " + unrecordedIDs.size() +
        "\n\tfor project # " + projid +
        "\n\tuserid #      " + userID +
        "\n\tremoving      " + recordedBySameGender.size() +
        "\n\tretain        " + onlyRecorded
    );

    if (onlyRecorded) {
      unrecordedIDs.retainAll(recordedBySameGender);
    } else {
      unrecordedIDs.removeAll(recordedBySameGender);
    }

    logger.info("getRecordFilterExercisesMatchingGender after removing recorded exercises " + unrecordedIDs.size());

    List<CommonExercise> unrecordedExercises = new ArrayList<>();

    for (CommonExercise exercise : exercises) {
      if (onlyExamples) {
        for (ClientExercise dir : exercise.getDirectlyRelated()) {
          if (unrecordedIDs.contains(dir.getID())) {
            unrecordedExercises.add(dir.asCommon());
          }
        }
      } else {
        if (unrecordedIDs.contains(exercise.getID())) {
          unrecordedExercises.add(exercise);
        }
      }
    }

    logger.info("getRecordFilterExercisesMatchingGender to be recorded " + unrecordedExercises.size() + " from " + exercises.size());

    return unrecordedExercises;
  }

  /**
   * @param userID
   * @param projid
   * @param onlyExamples
   * @return
   * @see #getRecordFilterExercisesMatchingGender
   */
  private Collection<Integer> getRecordedByMatchingGender(int userID,
                                                          int projid,
                                                          boolean onlyExamples,
                                                          Map<Integer, String> exToTranscript) {
    logger.debug("getRecordedByMatchingGender : for " + userID +
        " only by same gender examples only " + onlyExamples);// + " from " + exercises.size());

    return onlyExamples ?
        databaseServices.getAudioDAO().getRecordedBySameGenderContext(userID, projid, exToTranscript) :
        databaseServices.getAudioDAO().getRecordedBySameGender(userID, projid, exToTranscript);
  }

  /**
   * @param exercises
   * @return
   * @see #filterByUnrecorded
   */
  @NotNull
  private List<CommonExercise> getExercisesWithContext(Collection<CommonExercise> exercises) {
    List<CommonExercise> copy = new ArrayList<>();
    Set<Integer> seen = new HashSet<>();
    for (CommonExercise exercise : exercises) {
      if (seen.contains(exercise.getID())) logger.warn("saw " + exercise.getID() + " " + exercise + " again!");
      if (hasContext(exercise)) {
        seen.add(exercise.getID());
        //    copy.add(exercise);
        exercise.getDirectlyRelated().forEach(clientExercise -> copy.add(clientExercise.asCommon()));
        //copy.addAll(exercise.getDirectlyRelated());
      }
    }
    //   logger.debug("ONLY EXAMPLES - to be recorded " + copy.size() + " from " + exercises.size());

    return copy;
  }

  private <X extends CommonExercise> boolean hasContext(X exercise) {
    return !exercise.getDirectlyRelated().isEmpty();//.getContext() != null && !exercise.getContext().isEmpty();
  }

}
