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
    ISection<CommonExercise> sectionHelper = getSectionHelper(projid);
    if (sectionHelper == null) {
      logger.info("getTypeToValues no reponse...");// + "\n\ttype->selection" + typeToSelection);
      return new FilterResponse();
    } else {
      if (request.isRecordRequest()) { //how no user???
        return getFilterResponseForRecording(request, projid, userID);
      } else if (request.isOnlyUninspected()) {
        return getFilterResponse(request, projid, getRequestForUninspected(request, userID));
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

  private ExerciseListRequest getRequestForUninspected(FilterRequest request, int userID) {
    return new ExerciseListRequest()
        .setOnlyUninspected(true)
        .setOnlyExamples(request.isExampleRequest())
        .setUserID(userID);
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

  /**
   * @param projid
   * @param all
   * @param sectionHelper
   * @param typeOrder
   * @see #getSectionHelperFromFiltered(int, ExerciseListRequest)
   */
  private void populate(int projid,
                        Collection<CommonExercise> all,
                        ISection<CommonExercise> sectionHelper,
                        List<String> typeOrder) {
    Map<Integer, ExerciseAttribute> allAttributesByProject = databaseServices.getUserExerciseDAO().getExerciseAttribute().getIDToPair(projid);
    populate(all, typeOrder, sectionHelper, databaseServices.getProject(projid), allAttributesByProject);
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
  private void populate(Collection<CommonExercise> all,
                        List<String> typeOrder,
                        ISection<CommonExercise> sectionHelper,
                        Project lookup,
                        Map<Integer, ExerciseAttribute> allByProject) {
    List<List<Pair>> allAttributes = new ArrayList<>();
    List<String> baseTypeOrder = lookup.getBaseTypeOrder();

    long then = System.currentTimeMillis();
    {
      Collection<String> allFacetTypes = allByProject
          .values()
          .stream()
          .filter(ExerciseAttribute::isFacet)
          .map(Pair::getProperty)
          .collect(Collectors.toCollection(HashSet::new));

      for (CommonExercise exercise : all) {
        List<Pair> e = getPairs(exercise, baseTypeOrder, sectionHelper, allFacetTypes);
        allAttributes.add(e);
      }
    }

    long now = System.currentTimeMillis();

    if (now - then > 50) {
      logger.info("getExercises took " + (now - then) + " to attach attributes to " + all.size() + " exercises.");
    }

    sectionHelper.rememberTypesInOrder(typeOrder, allAttributes);

  }

  private List<Pair> getPairs(CommonExercise exercise,
                              Collection<String> baseTypeOrder,
                              ISection<CommonExercise> sectionHelper,
                              Collection<String> attrTypes) {
    List<Pair> pairs = getUnitToValue(exercise, baseTypeOrder, sectionHelper);
    sectionHelper.addPairs(exercise, exercise, attrTypes, pairs);
    return pairs;
  }

  private List<Pair> getUnitToValue(CommonExercise slick, Collection<String> typeOrder, ISection<CommonExercise> sectionHelper) {
    int id = slick.getID();
    Iterator<String> iterator = typeOrder.iterator();
    String unit = slick.getUnitToValue().get(iterator.next());
    String lesson = iterator.hasNext() ? slick.getUnitToValue().get(iterator.next()) : "";
    if (lesson == null) {
      logger.warn("hmm no lesson value on " + slick.getUnitToValue() + " for " + slick.getID() + " " + slick.getEnglish());
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
   *
   * @param request
   * @param projectID
   * @param request1
   * @return
   */
  private FilterResponse getFilterResponse(FilterRequest request, int projectID, ExerciseListRequest request1) {
    logger.info("getFilterResponse exercise req " + request1);
    SectionHelper<CommonExercise> unrecordedSectionHelper = getSectionHelperFromFiltered(projectID, request1);
    FilterResponse typeToValues = unrecordedSectionHelper.getTypeToValues(request, false);
    logger.info("getFilterResponse resp " + typeToValues);

    return typeToValues;
  }

  @NotNull
  private SectionHelper<CommonExercise> getSectionHelperFromFiltered(int projectID, ExerciseListRequest request) {
//    request.shouldAddContext() && request.
    List<CommonExercise> exercises = getExercises(projectID);
    List<CommonExercise> filtered = filterExercises(request, exercises, projectID);

    logger.info("getFilterResponse build section helper from " + filtered.size());

    ISection<CommonExercise> sectionHelper = getSectionHelper(projectID);
    SectionHelper<CommonExercise> unrecordedSectionHelper = sectionHelper.getCopy(filtered);
    populate(projectID, filtered, unrecordedSectionHelper, sectionHelper.getTypeOrder());
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
   * @param request
   * @param exercises
   * @param projid
   * @return
   * @see #getSectionHelperFromFiltered
   * @see mitll.langtest.server.database.DatabaseImpl#filterExercises
   */
  public List<CommonExercise> filterExercises(ExerciseListRequest request,
                                              List<CommonExercise> exercises,
                                              int projid) {
    logger.info("filterExercises filter " +
        "\n\treq       " + request +
        "\n\texercises " + exercises.size(), new Exception());

    //exercises = filterByUnrecordedOrGetContext(request, exercises, projid);

    if (request.isOnlyUnrecordedByMe()) {
      if (request.isOnlyExamples()) {
        logger.info("\n\n\n\filterExercises OK doing examples");

        exercises = getContextExercises(exercises);
      }
      logger.info("filterByUnrecordedOrGetContext : Filter for matching gender to " + request.getUserID() + " only recorded");
      exercises = getRecordFilterExercisesMatchingGender(request.getUserID(), exercises, projid, request.isOnlyExamples());
    }
//    else if (request.isOnlyRecordedByMatchingGender()) {
//      logger.info("filterByUnrecordedOrGetContext : Filter for matching gender to " + request.getUserID() + " only recorded!");
//      exercises = getRecordFilterExercisesMatchingGender(request.getUserID(), exercises, projid, request.isOnlyExamples(), true);
//    }


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

    logger.info("filterExercises" +
        "\n\tfilter req " + request +
        "\n\treturn     " + exercises.size());

    Set<Integer> seen = new HashSet<>();
    for (CommonExercise exercise : exercises) {
      int id = exercise.getID();
      if (seen.add(id)) {

      }
      else {
        logger.info("WARN : dup! " + id + " ex " + exercise.getEnglish()+ " " + exercise.getForeignLanguage());
      }
    }
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
   * Two cases : common vocab items - only return those that haven't been inspected
   * context sentences - only return the PAIR of parent and context sentence for which the context sentence has not been inspected
   *
   * @param exercises
   * @return
   */
  private List<CommonExercise> filterByUninspected(Collection<CommonExercise> exercises) {
    long then = System.currentTimeMillis();
    Collection<Integer> inspected = databaseServices.getStateManager().getInspectedExercises();
    long now = System.currentTimeMillis();
    logger.info("filterByUninspected found " + inspected.size() + " in " + (now - then));
    List<CommonExercise> copy = new ArrayList<>(inspected.size());
    for (CommonExercise exercise : exercises) {
      if (!inspected.contains(exercise.getID())) {
        copy.add(exercise);
      }
    }
    return copy;
  }

  private List<CommonExercise> filterByUninspectedContext(Collection<CommonExercise> exercises) {
    long then = System.currentTimeMillis();
    Collection<Integer> inspected = databaseServices.getStateManager().getInspectedExercises();
    long now = System.currentTimeMillis();
    logger.info("filterByUninspected found " + inspected.size() + " in " + (now - then));
    List<CommonExercise> copy = new ArrayList<>(inspected.size());
    for (CommonExercise exercise : exercises) {
      boolean found = false;
      for (ClientExercise context : exercise.getDirectlyRelated()) {
        if (!inspected.contains(context.getID())) {
          found = true;
          break;
        }
      }
      if (!found) {  // if not found on inspected list, add parent
        copy.add(exercise);
      }
    }
    return copy;
  }

  @NotNull
  private List<CommonExercise> getParentChildPairs(Collection<CommonExercise> exercises) {
    List<CommonExercise> withContext = new ArrayList<>();
    exercises.forEach(commonExercise -> {
      //  logger.info("\t" + commonExercise.getID() + " " + commonExercise.getDirectlyRelated().size());
      commonExercise.getDirectlyRelated().forEach(clientExercise -> {
        //withContext.add(commonExercise);
        Exercise copy = new Exercise(commonExercise);
        withContext.add(copy);
        copy.getDirectlyRelated().clear();
        copy.getDirectlyRelated().add(clientExercise.asCommon());
//            withContext.add(clientExercise.asCommon());
      });
      // withContext.addAll(commonExercise.getDirectlyRelated());
    });
    return withContext;
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
   * @see #filterExercises
   */
/*  private List<CommonExercise> filterByUnrecordedOrGetContext(
      ExerciseListRequest request,
      List<CommonExercise> exercises,
      int projid) {
    if (request.isOnlyUnrecordedByMe()) {
      logger.info("filterByUnrecordedOrGetContext : Filter for matching gender to " + request.getUserID() + " only recorded false");
      return getRecordFilterExercisesMatchingGender(request.getUserID(), exercises, projid, request.isOnlyExamples(), false);
    } else if (request.isOnlyRecordedByMatchingGender()) {
      logger.info("filterByUnrecordedOrGetContext : Filter for matching gender to " + request.getUserID() + " only recorded!");
      return getRecordFilterExercisesMatchingGender(request.getUserID(), exercises, projid, request.isOnlyExamples(), true);
    } else {
      return request.isOnlyExamples() ? getContextExercises(exercises) : exercises;
    }
  }*/

  /**
   * TODO : way too much work here... why go through all exercises?
   * TODO : why return all exercises?
   *
   * @param userID
   * @param exercises
   * @param projid
   * @param onlyExamples
   * @return
   * @paramx onlyRecorded
   * @see #filterExercises
   */
  @NotNull
  private List<CommonExercise> getRecordFilterExercisesMatchingGender(int userID,
                                                                      Collection<CommonExercise> exercises,
                                                                      int projid
      ,
                                                                      boolean onlyExamples
      /*,
                                                                      boolean onlyRecorded*/) {

    Set<Integer> unrecordedIDs = new HashSet<>(exercises.size());

    Map<Integer, String> exToTranscript = new HashMap<>();

    for (CommonExercise exercise : exercises) {
    /*  if (onlyExamples) {
        exercise.getDirectlyRelated().forEach(dir -> {
          int id = dir.getID();
          unrecordedIDs.add(id);
          exToTranscript.put(id, dir.getForeignLanguage());
        });
      } else {*/
      int id = exercise.getID();
      unrecordedIDs.add(id);
      exToTranscript.put(id, exercise.getForeignLanguage());
      //}
    }

    logger.info("getRecordFilterExercisesMatchingGender # exToTranscript " + exToTranscript.size());
    Collection<Integer> recordedBySameGender = getRecordedByMatchingGender(userID, projid, onlyExamples, exToTranscript);

    logger.info("getRecordFilterExercisesMatchingGender" +
            "\n\tall exercises " + unrecordedIDs.size() +
            "\n\tfor project # " + projid +
            "\n\tuserid #      " + userID +
            "\n\tremoving      " + recordedBySameGender.size()
        //+
        //"\n\tretain        " + onlyRecorded
    );

//    if (onlyRecorded) {
//      unrecordedIDs.retainAll(recordedBySameGender);
//    } else {
    unrecordedIDs.removeAll(recordedBySameGender);
//    }

    logger.info("getRecordFilterExercisesMatchingGender after removing recorded exercises " + unrecordedIDs.size());

    List<CommonExercise> unrecordedExercises = getUnrecordedForIDs(exercises,/* onlyExamples,*/ unrecordedIDs);

    logger.info("getRecordFilterExercisesMatchingGender to be recorded " + unrecordedExercises.size() + " from " + exercises.size());

    return unrecordedExercises;
  }

  @NotNull
  private List<CommonExercise> getUnrecordedForIDs(Collection<CommonExercise> exercises, /*boolean onlyExamples,*/ Set<Integer> unrecordedIDs) {
    List<CommonExercise> unrecordedExercises = new ArrayList<>();

    for (CommonExercise exercise : exercises) {
     /* if (onlyExamples) {
        for (ClientExercise dir : exercise.getDirectlyRelated()) {
          if (unrecordedIDs.contains(dir.getID())) {
            unrecordedExercises.add(dir.asCommon());
          }
        }
      } else {*/
      if (unrecordedIDs.contains(exercise.getID())) {
        unrecordedExercises.add(exercise);
      }
      // }
    }
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
    logger.info("getRecordedByMatchingGender : for " + userID +
        " only by same gender examples only " + onlyExamples);// + " from " + exercises.size());

    return onlyExamples ?
        databaseServices.getAudioDAO().getRecordedBySameGenderContext(userID, projid, exToTranscript) :
        databaseServices.getAudioDAO().getRecordedBySameGender(userID, projid, exToTranscript);
  }

  /**
   * @param exercises
   * @return
   * @see #filterExercises
   */
  @NotNull
  private List<CommonExercise> getContextExercises(Collection<CommonExercise> exercises) {
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
    logger.info("getContextExercises - to be recorded " + copy.size() + " from " + exercises.size());

    return copy;
  }

  private <X extends CommonExercise> boolean hasContext(X exercise) {
    return !exercise.getDirectlyRelated().isEmpty();//.getContext() != null && !exercise.getContext().isEmpty();
  }

  public List<CommonExercise> getExercisesForSelectionState(ExerciseListRequest request, int projid) {
    Map<String, Collection<String>> typeToSelection = request.getTypeToSelection();


    Collection<CommonExercise> exercisesForState = getExercisesForSelection(projid, typeToSelection);


    List<CommonExercise> copy = new ArrayList<>(exercisesForState);  // TODO : avoidable???
    return filterExercises(request, copy, projid);
    //   return getExerciseListWrapperForPrefix(request, exercisesForState, projid);
  }

  private static final String RECORDED1 = "Recorded";

  /**
   * Ask section helper for matching item to type -> value
   *
   * @param projid
   * @param typeToSelection
   * @return
   */
  private Collection<CommonExercise> getExercisesForSelection(int projid, Map<String, Collection<String>> typeToSelection) {
    ISection<CommonExercise> sectionHelper = getSectionHelper(projid);
    typeToSelection.remove(RECORDED1);

    return typeToSelection.isEmpty() ? getExercises(projid) :
        sectionHelper.getExercisesForSelectionState(typeToSelection);
  }
}
