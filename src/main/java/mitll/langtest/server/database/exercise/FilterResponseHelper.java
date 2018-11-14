package mitll.langtest.server.database.exercise;

import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mitll.langtest.server.database.exercise.SectionHelper.ANY;

public class FilterResponseHelper {
  private static final Logger logger = LogManager.getLogger(FilterResponseHelper.class);
  private static final String LISTS = "Lists";
  private static final String CONTENT = "Content";
  public static final String SENTENCES = "Sentences";
  private static final String SENTENCES_ONLY = "Sentences Only";

  private final DatabaseServices databaseServices;

  public FilterResponseHelper(DatabaseServices databaseServices) {
    this.databaseServices = databaseServices;
  }

  public FilterResponse getTypeToValues(FilterRequest request, int projid, int userID) {
    logger.info("getTypeToValues " + request);

    request.prune();

    //logger.info("getTypeToValues 2 " + request);

    ISection<CommonExercise> sectionHelper = getSectionHelper(projid);
    if (sectionHelper == null) {
      logger.info("getTypeToValues no reponse...");// + "\n\ttype->selection" + typeToSelection);
      return new FilterResponse();
    } else {
      if (request.isRecordRequest()) { //how no user???
        return getFilterResponseForRecording(request, projid, userID);
      } else if (request.isOnlyUninspected()) {
        return getFilterResponse(request, projid, getRequestForUninspected(request, userID));
      } else if (request.isOnlyWithAnno()) {
        return getFilterResponse(request, projid, getExerciseListRequest(request, userID).setOnlyWithAnno(true));
      } else if (request.isExampleRequest()) {
        logger.info("getTypeToValues isExampleRequest " + request);
        return getFilterResponse(request, projid, getExerciseListRequest(request, userID).setOnlyExamples(true));
      } else {
       // logger.info("getTypeToValues normal req " + request);
        FilterResponse response = sectionHelper.getTypeToValues(request, false);
        maybeAddUserListFacet(request.getUserListID(), response);
        maybeAddContent(request, response, projid);
      //  logger.info("getTypeToValues normal response " + response);

        return response;
      }
    }
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
    return getFilterResponse(request, projectID,
        getExerciseListRequest(request, userFromSessionID).setOnlyUnrecordedByMe(true));
  }

  private ExerciseListRequest getRequestForUninspected(FilterRequest request, int userID) {
    return getExerciseListRequest(request, userID).setOnlyUninspected(true);
  }

  private ExerciseListRequest getExerciseListRequest(FilterRequest request, int userID) {
    boolean exampleRequest = request.isExampleRequest();

    logger.info("getExerciseListRequest isExampleRequest " + request + " : " + exampleRequest);

    return new ExerciseListRequest()
        .setOnlyExamples(exampleRequest)
        .setUserID(userID);
  }

  private void maybeAddUserListFacet(int userListID, FilterResponse typeToValues) {
    UserList<CommonShell> next = userListID != -1 ? databaseServices.getUserListManager().getSimpleUserListByID(userListID) : null;

    if (next != null) {  // echo it back
      //logger.info("\tgetTypeToValues " + request + " include list " + next);
      String type = LISTS;

      typeToValues.addTypeToInclude(type);

      Set<MatchInfo> value = new HashSet<>();

      value.add(new MatchInfo(next.getName(), next.getNumItems(), userListID, false, ""));

      typeToValues.getTypeToValues().put(type, value);
    }
  }

  /**
   * @param request
   * @param typeToValues
   * @param projid
   * @see #getTypeToValues
   */
  private void maybeAddContent(FilterRequest request, FilterResponse typeToValues, int projid) {
   /* Optional<Pair> content = request.getTypeToSelection().stream().filter(pair -> pair.getProperty().equalsIgnoreCase(CONTENT)).findAny();

    if (content.isPresent()) {
      logger.info("maybeAddContent found content in request...");
      String value1 = content.get().getValue();

      if (value1.startsWith(SENTENCES)) {
        typeToValues.addTypeToInclude(CONTENT);

        Set<MatchInfo> value = new HashSet<>();
        value.add(new MatchInfo(value1, getNumContextSentences(request, projid), -1, false, ""));
        typeToValues.getTypeToValues().put(CONTENT, value);
      }
    }
    else {
      logger.info("maybeAddContent no content in request...");

      typeToValues.addTypeToInclude(CONTENT);

      Set<MatchInfo> value = new HashSet<>();
      value.add(new MatchInfo(SENTENCES_ONLY, getNumContextSentences(request, projid), -1, false, ""));
      typeToValues.getTypeToValues().put(CONTENT, value);
    }
*/

    int numContextSentences = getNumContextSentences(request, projid);

    if (numContextSentences > 0) {
      typeToValues.addTypeToInclude(CONTENT);

      Set<MatchInfo> value = new HashSet<>();
      value.add(new MatchInfo(SENTENCES_ONLY, numContextSentences, -1, false, ""));
      typeToValues.getTypeToValues().put(CONTENT, value);
    }
  }

  private int getNumContextSentences(FilterRequest request, int projid) {
    ExerciseListRequest exerciseListRequest = new ExerciseListRequest();
    exerciseListRequest.setOnlyExamples(true);

    request.getTypeToSelection().forEach(pair -> {
      String value = pair.getValue();
      if (!value.equalsIgnoreCase(ANY))
        exerciseListRequest.getTypeToSelection().put(pair.getProperty(), Collections.singleton(value));
    });

    return getExercisesForSelectionState(exerciseListRequest, projid).size();
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
   * First make an exercise list of just what you're looking for, then build a type hierarchy on the fly from it.
   *
   * @param request
   * @param projectID
   * @param exerciseListRequest
   * @return
   */
  private FilterResponse getFilterResponse(FilterRequest request, int projectID, ExerciseListRequest exerciseListRequest) {
    SectionHelper<CommonExercise> unrecordedSectionHelper = getSectionHelperFromFiltered(projectID, exerciseListRequest);
    FilterResponse typeToValues = unrecordedSectionHelper.getTypeToValues(request, false);
    logger.info("getFilterResponse resp " + typeToValues);
    return typeToValues;
  }

  @NotNull
  private SectionHelper<CommonExercise> getSectionHelperFromFiltered(int projectID, ExerciseListRequest request) {
    List<CommonExercise> filtered = filterExercises(request, getExercises(projectID), projectID);

    logger.info("getFilterResponse build section helper from " + filtered.size());

    ISection<CommonExercise> sectionHelper = getSectionHelper(projectID);
    SectionHelper<CommonExercise> unrecordedSectionHelper = new SectionHelper<>();
    populate(projectID, filtered, unrecordedSectionHelper, sectionHelper.getTypeOrder());
    return unrecordedSectionHelper;
  }

  private ISection<CommonExercise> getSectionHelper(int projectID) {
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
  public List<CommonExercise> filterExercises(ExerciseListRequest request, List<CommonExercise> exercises, int projid) {
    logger.info("filterExercises filter " +
        "\n\treq       " + request +
        "\n\texercises " + exercises.size());

    boolean onlyExamples = request.isOnlyExamples();

    Collection<String> content = request.getTypeToSelection().get(CONTENT);

    if (content != null) {
      onlyExamples = true;
    }

    if (request.isOnlyUnrecordedByMe()) {
      if (onlyExamples) {
        logger.info("filterExercises OK doing examples");
        exercises = getContextExercises(exercises);
      }
      logger.info("filterByUnrecordedOrGetContext : Filter for matching gender to " + request.getUserID() + " only recorded");
      exercises = getRecordFilterExercisesMatchingGender(request.getUserID(), exercises, projid, onlyExamples);

    } else if (request.isOnlyWithAnno()) {
      boolean isContext = onlyExamples || request.shouldAddContext();
      if (isContext) {
        logger.info("filterExercises OK doing examples 2");
        exercises = getContextExercises(exercises);
      }

      exercises = filterByOnlyAnno(exercises, projid, isContext);

      if (isContext) {
        exercises = getParentChildPairs(exercises, projid);
      }

    } else if (request.isOnlyUninspected()) {
      exercises = filterByUninspected(exercises);

    } else if (onlyExamples) {
      logger.info("filterExercises OK doing examples 3");
      exercises = getContextExercises(exercises);

    }

    logger.info("filterExercises" +
        "\n\tfilter req " + request +
        "\n\treturn     " + exercises.size());

/*    Set<Integer> seen = new HashSet<>();
    for (CommonExercise exercise : exercises) {
      int id = exercise.getID();
      if (seen.add(id)) {

      } else {
        logger.info("WARN : dup! " + id + " ex " + exercise.getEnglish() + " " + exercise.getForeignLanguage());
      }
    }*/

    return exercises;
  }

  private List<CommonExercise> getParentChildPairs(List<CommonExercise> exercises, int projid) {
    List<CommonExercise> pairs = new ArrayList<>();
    exercises.forEach(contextEx -> {
      int parentExerciseID = contextEx.getParentExerciseID();
      if (parentExerciseID < 0) logger.warn("no parent for " + contextEx.getID());
      else {
        Exercise parent = new Exercise(databaseServices.getExercise(projid, parentExerciseID));
        pairs.add(parent);
        parent.getDirectlyRelated().clear();
        parent.getDirectlyRelated().add(contextEx);
      }
    });
    return pairs;
  }

  private List<CommonExercise> filterByOnlyAnno(List<CommonExercise> exercises, int projID, boolean isContext) {
    Collection<Integer> audioAnnos = databaseServices.getUserListManager().getAnnotationDAO().getExercisesWithIncorrectAnnotations(projID, isContext);
    List<CommonExercise> copy = new ArrayList<>(audioAnnos.size());
    for (CommonExercise exercise : exercises) {
      if (audioAnnos.contains(exercise.getID())) {
        copy.add(exercise);
        logger.info("filterByOnlyAnno for " + exercise.getID() +
            " parent is " + exercise.getParentExerciseID());
      }
    }
    logger.info("filterByOnlyAnno from " + exercises.size() + " to " + copy.size());
    return copy;
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
    long then = System.currentTimeMillis();
    List<CommonExercise> copy = new ArrayList<>(exercises.size());
    Set<Integer> seen = new HashSet<>();
    for (ClientExercise parent : exercises) {
      if (hasContext(parent)) {
        parent.getDirectlyRelated().forEach(clientExercise -> {
          if (seen.contains(clientExercise.getID())) {
            logger.warn("getContextExercises saw " + clientExercise.getID() + " " + clientExercise + " again!");
          } else {
            copy.add(clientExercise.asCommon());
          }
          seen.add(clientExercise.getID());
        });
      }
    }

    long now = System.currentTimeMillis();
    if (now - then > 50) {
      logger.info("getContextExercises - (" + (now - then) +
          " millis) return " + copy.size() + " (" + seen.size() +
          ") from " + exercises.size());
    }

    return copy;
  }

  private <X extends ClientExercise> boolean hasContext(X exercise) {
    return !exercise.getDirectlyRelated().isEmpty();
  }

  public List<CommonExercise> getExercisesForSelectionState(ExerciseListRequest request, int projid) {
    Map<String, Collection<String>> typeToSelection = request.getTypeToSelection();

    logger.info("getExercisesForSelectionState " + typeToSelection);

    Collection<CommonExercise> exercisesForState = getExercisesForSelection(projid, typeToSelection);
    List<CommonExercise> copy = new ArrayList<>(exercisesForState);  // TODO : avoidable???
    return filterExercises(request, copy, projid);
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
    //  ISection<CommonExercise> sectionHelper = getSectionHelper(projid);
    Map<String, Collection<String>> copy = new HashMap<>(typeToSelection);
    copy.remove(RECORDED1);
    copy.remove(CONTENT);

    boolean empty = copy.isEmpty();
    logger.info("getExercises " + empty + " : " + copy);
    return empty ?
        getExercises(projid) :
        getSectionHelper(projid).getExercisesForSelectionState(copy);
  }
}
