package mitll.langtest.server.database.exercise;

import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.userexercise.ExercisePhoneInfo;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.npdata.dao.SlickExercise;
import mitll.npdata.dao.SlickExerciseAttributeJoin;
import mitll.npdata.dao.SlickExercisePhone;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class FilterResponseHelper {
  private static final Logger logger = LogManager.getLogger(FilterResponseHelper.class);
  private static final String ANY = "Any";
  private static final String LISTS = "Lists";


  private DatabaseServices databaseServices;

  /**
   * TODO : remove?
   */
  private static final String RECORDED1 = "Recorded";

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
                        ISection<CommonExercise> sectionHelper) {
    Map<Integer, ExerciseAttribute> allAttributesByProject = databaseServices.getUserExerciseDAO().getExerciseAttribute().getIDToPair(projid);
    getExercises(all, sectionHelper.getTypeOrder(), sectionHelper, databaseServices.getProject(projid), allAttributesByProject);
  }

  /**
   * @param all
   * @param typeOrder
   * @param sectionHelper
   * @param lookup
   * @paramx addTypesToSection
   * @return
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
    boolean ispredef = slick.isPredefined();

    return sectionHelper.getPairs(typeOrder, id, unit, lesson, ispredef);
  }

  /**
   * @param request
   * @param userFromSessionID
   * @return
   * @paramx response
   */
  private FilterResponse getFilterResponseForRecording(FilterRequest request, int projectID, int userFromSessionID) {
    // int projectID = getProjectIDFromUser(userFromSessionID);

    Map<String, Collection<String>> typeToSelection = getTypeToSelection(request);


  //  List<CommonExercise> exercisesForState = new ArrayList<>(getExercisesForSelection(projectID, typeToSelection));
    List<CommonExercise> exercisesForState = getExercises(projectID);
    //  List<String> typeOrder = getProject(projectID).getTypeOrder();
//    if (typeOrder.isEmpty()) {
//
//    } else {
    //   String firstType = typeOrder.get(0);
    //   List<CommonExercise> rec = filterByUnrecorded(request1.setOnlyUnrecordedByMe(false).setOnlyRecordedByMatchingGender(true), exercisesForState, projectID);

    ExerciseListRequest request1 = new ExerciseListRequest()
        .setOnlyUnrecordedByMe(true)
        .setOnlyExamples(request.isExampleRequest())
        .setUserID(userFromSessionID);

    List<CommonExercise> unRec = filterByUnrecorded(request1, exercisesForState, projectID);

    logger.info("build section helper from " + unRec.size());

    SectionHelper<CommonExercise> unrecordedSectionHelper = getSectionHelper(projectID).getCopy(unRec);

    populate(projectID, unRec, unrecordedSectionHelper);

    unrecordedSectionHelper.report();

    return unrecordedSectionHelper.getTypeToValues(request, false);

/*    //   logger.info("found " + unRec.size() + " unrecorded");

    Map<String, Long> collect = unRec.stream().collect(Collectors.groupingBy(ex -> ex.getUnitToValue().get(firstType) == null ? "Unknown" : ex.getUnitToValue().get(firstType), Collectors.counting()));

    // logger.info("map is " + collect);

    Map<String, Set<MatchInfo>> typeToValues = new HashMap<>();

    Set<MatchInfo> matches = new TreeSet<>();
    collect.forEach((k, v) -> matches.add(new MatchInfo(k, v.intValue())));
    //    logger.info("matches is " + matches);
    typeToValues.put(firstType, matches);

    HashSet<String> typesToInclude = new HashSet<>();
    typesToInclude.add(firstType);

    FilterResponse response = new FilterResponse(request.getReqID(), typeToValues, typesToInclude, -1);
    // }
    return response;*/
  }


  protected ISection<CommonExercise> getSectionHelper(int projectID) {
    return databaseServices.getSectionHelper(projectID);
  }

  @NotNull
  private Map<String, Collection<String>> getTypeToSelection(FilterRequest request) {
    Map<String, Collection<String>> typeToSelection = new HashMap<>();
    request.getTypeToSelection().forEach(pair -> {
      String value1 = pair.getValue();
      if (!value1.equalsIgnoreCase(ANY)) {
        typeToSelection.put(pair.getProperty(), Collections.singleton(value1));
      }
    });
    return typeToSelection;
  }


  /**
   * Ask section helper for matching item to type -> value
   *
   * @param projid
   * @param typeToSelection
   * @return
   */
  private Collection<CommonExercise> getExercisesForSelection(int projid, Map<String, Collection<String>> typeToSelection) {
    ISection<CommonExercise> sectionHelper = databaseServices.getSectionHelper(projid);
    typeToSelection.remove(RECORDED1);

    return typeToSelection.isEmpty() ? getExercises(projid) :
        sectionHelper.getExercisesForSelectionState(typeToSelection);
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
