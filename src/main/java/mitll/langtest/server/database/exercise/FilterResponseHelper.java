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

package mitll.langtest.server.database.exercise;

import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.dialog.DialogMetadata;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static mitll.langtest.server.database.exercise.SectionHelper.ANY;

public class FilterResponseHelper implements IResponseFilter {
  private static final Logger logger = LogManager.getLogger(FilterResponseHelper.class);

  private static final String LISTS = "Lists";
  private static final String CONTENT = "Content";
  public static final String SENTENCES = "Sentences";
  private static final String SENTENCES_ONLY = "Sentences Only";
  private static final String LANGUAGE_META_DATA = DialogMetadata.LANGUAGE.name();
  private static final String SPEAKER_META_DATA = DialogMetadata.SPEAKER.name();
  private static final String RECORDED1 = "Recorded";
  private static final boolean DEBUG = false;

  private final DatabaseServices databaseServices;

  public FilterResponseHelper(DatabaseServices databaseServices) {
    this.databaseServices = databaseServices;
  }

  /**
   * @param request
   * @param projid
   * @param userID
   * @return
   * @see mitll.langtest.client.list.FacetExerciseList#getTypeToValues
   */
  @Override
  public FilterResponse getTypeToValues(FilterRequest request, int projid, int userID) {
    logger.info("getTypeToValues " + request);

    request.prune();

    //logger.info("getTypeToValues 2 " + request);

    ISection<CommonExercise> sectionHelper = getSectionHelper(projid);
    if (sectionHelper == null) {
      logger.info("getTypeToValues no response...");// + "\n\ttype->selection" + typeToSelection);
      return new FilterResponse();
    } else {
      if (request.isRecordRequest()) { //how no user???
        return getFilterResponseForRecording(request, projid, userID);
      } else if (request.isOnlyUninspected()) {
        FilterResponse filterResponse = getFilterResponse(request, projid, getRequestForUninspected(request, userID));
        addInterpreterMetaData(request, hasDialogs(projid) && request.getMode() == ProjectMode.DIALOG, filterResponse);
        return filterResponse;
      } else if (request.isOnlyWithAnno()) {
        return getFilterResponse(request, projid, getExerciseListRequest(request, userID).setOnlyWithAnno(true));
      } else if (request.isExampleRequest()) {
        logger.info("\n\n\ngetTypeToValues isExampleRequest " + request);
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
    FilterResponse filterResponse = getFilterResponse(request, projectID,
        getExerciseListRequest(request, userFromSessionID).setOnlyUnrecordedByMe(true));

    addInterpreterMetaData(request, hasDialogs(projectID), filterResponse);

    return filterResponse;
  }

  private void addInterpreterMetaData(FilterRequest request, boolean includeLanguage, FilterResponse filterResponse) {
    if (includeLanguage && isDialog(request)) {
      filterResponse.addTypeToInclude(LANGUAGE_META_DATA);
      filterResponse.addTypeToInclude(SPEAKER_META_DATA);
    }
  }

  private boolean isDialog(IRequest request) {
    return request.getMode() == ProjectMode.DIALOG;
  }

  private boolean hasDialogs(int projectID) {
    return !getDialogs(projectID).isEmpty();
  }


  private ExerciseListRequest getRequestForUninspected(FilterRequest request, int userID) {
    return getExerciseListRequest(request, userID).setOnlyUninspected(true);
  }

  private ExerciseListRequest getExerciseListRequest(FilterRequest request, int userID) {
    boolean exampleRequest = request.isExampleRequest();

    if (DEBUG) logger.info("getExerciseListRequest isExampleRequest " + request + " : " + exampleRequest);

    return new ExerciseListRequest()
        .setOnlyExamples(exampleRequest)
        .setMode(request.getMode())
        .setUserID(userID);
  }

  private void maybeAddUserListFacet(int userListID, FilterResponse typeToValues) {
    UserList<CommonShell> next = userListID != -1 ? databaseServices.getUserListManager().getSimpleUserListByID(userListID) : null;

    if (next != null) {  // echo it back
      //logger.info("\tgetTypeToValues " + request + " include list " + next);
      typeToValues.addTypeToInclude(LISTS);

      Set<MatchInfo> value = new HashSet<>();

      value.add(new MatchInfo(next.getName(), next.getNumItems(), userListID, false, ""));

      typeToValues.getTypeToValues().put(LISTS, value);
    }
  }

  /**
   * @param request
   * @param typeToValues
   * @param projid
   * @see #getTypeToValues
   */
  private void maybeAddContent(FilterRequest request, FilterResponse typeToValues, int projid) {
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
   * @param doQC
   * @param doDialogFilter
   * @see #getSectionHelperFromFiltered(int, ExerciseListRequest)
   */
  private void populate(int projid,
                        Collection<CommonExercise> all,
                        ISection<CommonExercise> sectionHelper,
                        List<String> typeOrder,
                        boolean includeLanguageFacets) {
    populate(all, typeOrder, sectionHelper,
        databaseServices.getProject(projid).getBaseTypeOrder(),
        databaseServices.getUserExerciseDAO().getExerciseAttribute().getIDToPair(projid),
        includeLanguageFacets);
  }

  /**
   * @param all
   * @param typeOrder
   * @param sectionHelper
   * @param lookup
   * @param doQC
   * @param doDialogFilter
   * @return
   * @paramx addTypesToSection
   * @seex #getByProject
   * @seex #getContextByProject
   * @see #populate(int, Collection, ISection, List, boolean, boolean, boolean)
   */
  private void populate(Collection<CommonExercise> all,
                        List<String> typeOrder,
                        ISection<CommonExercise> sectionHelper,
                        List<String> baseTypeOrder,
                        Map<Integer, ExerciseAttribute> allByProject,
                        boolean includeLanguageFacets) {
    List<List<Pair>> allAttributes = new ArrayList<>();

    long then = System.currentTimeMillis();

    Collection<String> allFacetTypes = allByProject
        .values()
        .stream()
        .filter(ExerciseAttribute::isFacet)
        .map(Pair::getProperty)
        .collect(Collectors.toCollection(HashSet::new));


    if (includeLanguageFacets) {
      if (DEBUG) logger.info("populate : including facets for language - base types " + baseTypeOrder);
      allFacetTypes.add(LANGUAGE_META_DATA);
      allFacetTypes.add(SPEAKER_META_DATA);
    }

    //logger.info("populate : including facets " + allFacetTypes);

    all.forEach(commonExercise -> allAttributes.add(getPairs(commonExercise, baseTypeOrder, sectionHelper,
        allFacetTypes, !includeLanguageFacets)));

    long now = System.currentTimeMillis();

    if (now - then > 0) {
      logger.info("populate took " + (now - then) +
          "\n\tto attach " + allAttributes.size() + //" : " + allAttributes +
          "\n\tattributes to " + all.size() + " exercises for " +
          "\n\tfacets  " + allFacetTypes);
    }

    sectionHelper.rememberTypesInOrder(typeOrder, allAttributes);
  }

  private List<Pair> getPairs(CommonExercise exercise,
                              Collection<String> baseTypeOrder,
                              ISection<CommonExercise> sectionHelper,
                              Collection<String> attrTypes,
                              boolean onlyIncludeFacetAttributes) {
    List<Pair> pairs = getUnitToValue(exercise, baseTypeOrder, sectionHelper);
    sectionHelper.addPairs(exercise, exercise, attrTypes, pairs, onlyIncludeFacetAttributes);
    return pairs;
  }

  private List<Pair> getUnitToValue(CommonExercise exercise, Collection<String> typeOrder, ISection<CommonExercise> sectionHelper) {
    int id = exercise.getID();
    Iterator<String> iterator = typeOrder.iterator();
    String unit = exercise.getUnitToValue().get(iterator.next());
    String lesson = iterator.hasNext() ? exercise.getUnitToValue().get(iterator.next()) : "";

    if (lesson == null) {
      logger.warn("getUnitToValue hmm no lesson value on " + exercise.getUnitToValue() + " for " + exercise.getID() + " " + exercise.getEnglish());
      lesson = "";
    } else if (lesson.isEmpty()) {
      logger.info("getUnitToValue empty lesson value on " + exercise.getUnitToValue() + " for " + exercise.getID() + " " + exercise.getEnglish());
    }
//    boolean ispredef = exercise.isPredefined();
//    logger.info("getUnitToValue #id " + id + " : '" + unit + "' - '" + lesson + "' predef " +ispredef);

    List<Pair> pairs = sectionHelper.getPairs(typeOrder, id, unit, lesson);

    return pairs;
  }

  /**
   * First make an exercise list of just what you're looking for, then build a type hierarchy on the fly from it.
   *
   * @param request
   * @param projectID
   * @param exerciseListRequest
   * @return
   * @see #getTypeToValues
   * @see #getFilterResponseForRecording(FilterRequest, int, int)
   */
  private FilterResponse getFilterResponse(FilterRequest request, int projectID, ExerciseListRequest exerciseListRequest) {
    logger.info("getFilterResponse FilterRequest " + request);
    logger.info("getFilterResponse ExerciseListRequest " + exerciseListRequest);
    SectionHelper<CommonExercise> unrecordedSectionHelper = getSectionHelperFromFiltered(projectID, exerciseListRequest);

    //  unrecordedSectionHelper.report();

    FilterResponse typeToValues = unrecordedSectionHelper.getTypeToValues(request, false);

    logger.info("getFilterResponse resp " + typeToValues);
    return typeToValues;
  }

  /**
   * Start from dialog exercise set if dialog exercise request.
   *
   * @param projectID
   * @param request
   * @return
   */
  @NotNull
  private SectionHelper<CommonExercise> getSectionHelperFromFiltered(int projectID, ExerciseListRequest request) {
    boolean dialog = isDialog(request);

    List<CommonExercise> initialExercisesToFilter = getInitialExercisesToFilter(projectID, request, dialog);
    logger.info("getSectionHelperFromFiltered for " + projectID + " and " + dialog + " got " + initialExercisesToFilter.size());

    List<CommonExercise> filtered = filterExercises(request, initialExercisesToFilter, projectID);
    logger.info("getSectionHelperFromFiltered for " + projectID + " and " + dialog + " got filtered " + filtered.size());

    SectionHelper<CommonExercise> unrecordedSectionHelper = new SectionHelper<>();
    {
      boolean includeLanguage = (request.isOnlyUnrecordedByMe() || request.isOnlyUninspected()) && dialog;

      List<String> typeOrder = getSectionHelper(projectID).getTypeOrder();

      if (dialog) {
        logger.info("getSectionHelperFromFiltered build section helper from " + filtered.size() +
            " and types " + typeOrder + " dialog " + dialog);

        typeOrder = new ArrayList<>(typeOrder);
        typeOrder.add(LANGUAGE_META_DATA);
        typeOrder.add(SPEAKER_META_DATA);
      }

      populate(projectID, filtered, unrecordedSectionHelper, typeOrder, includeLanguage);
    }
    return unrecordedSectionHelper;
  }

  private List<CommonExercise> getInitialExercisesToFilter(int projectID, ExerciseListRequest request, boolean dialog) {
    List<CommonExercise> filtered;
    if (dialog) {
      filtered = getDialogExercises(projectID);
      if (request.isOnlyExamples()) {
        int before = filtered.size();
        filtered = filtered.stream().filter(CommonShell::isContext).collect(Collectors.toList());
        if (filtered.size() != before) {
          logger.info("getInitialExercisesToFilter context from " + before + " to " + filtered.size());
        }
      }

    } else {
      filtered = getExercises(projectID);
    }
    return filtered;
  }

  @NotNull
  private List<CommonExercise> getDialogExercises(int projectID) {
    List<CommonExercise> filtered = new ArrayList<>();
    List<IDialog> dialogs = getDialogs(projectID);
    dialogs.forEach(iDialog -> filtered.addAll(toCommon(iDialog.getBothExercisesAndCore())));
    if (dialogs.isEmpty()) {
      logger.warn("getDialogExercises no dialogs in " + projectID);
    } else {
      logger.info("getDialogExercises ok found " + filtered.size() + " for " + projectID + " and " + dialogs.size() + " dialogs");
    }

    return filtered;
  }

  @NotNull
  private List<CommonExercise> getDialogExercisesFiltered(int projectID, List<CommonExercise> toFilterDown) {
    Set<Integer> ids = new HashSet<>();
    getDialogs(projectID).forEach(iDialog ->
        iDialog.getBothExercisesAndCore().forEach(clientExercise -> ids.add(clientExercise.getID())));
    return toFilterDown.stream().filter(commonExercise -> ids.contains(commonExercise.getID())).collect(Collectors.toList());
  }

  private List<IDialog> getDialogs(int projectID) {
    return databaseServices.getProject(projectID).getDialogs();
  }

  @NotNull
  private List<CommonExercise> toCommon(Collection<ClientExercise> exercises) {
    List<CommonExercise> commonExercises = new ArrayList<>(exercises.size());
    exercises.forEach(clientExercise -> commonExercises.add(clientExercise.asCommon()));
    return commonExercises;
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

  @Override
  public List<CommonExercise> filter(ExerciseListRequest request, List<CommonExercise> exercises, int projid) {
    exercises = maybeFilterDownToJustDialog(request, projid, exercises);
    return filterExercises(request, exercises, projid);
  }

  /**
   * Depending on the request, filter down to only the requested exercises.
   * E.g. items that have not been recorded yet.
   *
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
//        logger.info("filterExercises OK doing examples");
        exercises = getContextExercises(exercises);
      }

      boolean filterOnBothSpeeds = request.getMode() != ProjectMode.DIALOG;

      if (DEBUG) {
        if (!filterOnBothSpeeds) {
          logger.info("only filter on reg speed");
        }
        if (request.getMode() == ProjectMode.DIALOG) {
          logger.info("request mode is dialog.");
        }
      }
      exercises = getRecordFilterExercisesMatchingGender(request.getUserID(), exercises, projid, onlyExamples, filterOnBothSpeeds);
      exercises = maybeDoInterpreterFilter(request, exercises);
    } else if (request.isOnlyWithAnno()) {
      boolean isContext = onlyExamples || request.shouldAddContext();
      if (isContext) {
        //      logger.info("filterExercises OK doing examples 2");
        exercises = getContextExercises(exercises);
      }

      exercises = filterByOnlyAnno(exercises, projid, isContext);

      if (isContext) {
        exercises = getParentChildPairs(exercises, projid);
      }
    } else if (request.isOnlyUninspected()) {
      exercises = filterByUninspected(exercises);
      exercises = maybeDoInterpreterFilter(request, exercises);
    } else if (onlyExamples) {
      logger.info("filterExercises OK doing examples 3");
      exercises = getContextExercises(exercises);
    } else {
      boolean includeLanguage = hasDialogs(projid);
      if (includeLanguage) {
//        logger.info("filterExercises remove english - before " + exercises.size());
        exercises = getCommonExercisesWithoutEnglish(exercises);
  //      logger.info("filterExercises remove english - after  " + exercises.size());
      }
    }

    logger.info("filterExercises" +
        "\n\tfilter req " + request +
        "\n\treturn     " + exercises.size());

    return exercises;
  }

  private List<CommonExercise> maybeDoInterpreterFilter(ExerciseListRequest request, List<CommonExercise> exercises) {
    Map<String, Collection<String>> typeToSelection = request.getTypeToSelection();
    Collection<String> languageFilter = typeToSelection.get(LANGUAGE_META_DATA);
    Collection<String> speakerFilter = typeToSelection.get(SPEAKER_META_DATA);

    if (DEBUG) {
      logger.info("filterByUnrecordedOrGetContext : Filter for matching gender " +
          "\n\tto user         " + request.getUserID() + " only recorded" +
          "\n\ttype->selection " + typeToSelection.keySet() +
          "\n\tlangauge        " + languageFilter +
          "\n\tspeaker         " + speakerFilter
      );
    }

    exercises = maybeDoInterpreterFilter(languageFilter, speakerFilter, exercises);
    return exercises;
  }

  @Override
  @NotNull
  public List<CommonExercise> getCommonExercisesWithoutEnglish(List<CommonExercise> exercises) {
    exercises = exercises
        .stream()
        .filter(ex ->
            ex.getAttributes()
                .stream()
                .filter(attr ->
                    attr.getProperty().equalsIgnoreCase(LANGUAGE_META_DATA) &&
                        attr.getValue().equalsIgnoreCase(Language.ENGLISH.name()))
                .collect(Collectors.toList())
                .isEmpty())
        .collect(Collectors.toList());
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
   * @param languageFilter
   * @param userID
   * @param exercises
   * @param projid
   * @param onlyExamples
   * @param filterOnBothSpeeds
   * @return
   * @paramx onlyRecorded
   * @see #filterExercises
   */
  @NotNull
  private List<CommonExercise> getRecordFilterExercisesMatchingGender(int userID,
                                                                      Collection<CommonExercise> exercises,
                                                                      int projid,
                                                                      boolean onlyExamples,
                                                                      boolean filterOnBothSpeeds) {
    Set<Integer> unrecordedIDs = new HashSet<>(exercises.size());

    Map<Integer, String> exToTranscript = new HashMap<>();

    for (CommonExercise exercise : exercises) {
      int id = exercise.getID();
      unrecordedIDs.add(id);
      exToTranscript.put(id, exercise.getForeignLanguage());
    }

    logger.info("getRecordFilterExercisesMatchingGender # exToTranscript " + exToTranscript.size());
    Collection<Integer> recordedBySameGender =
        getRecordedByMatchingGender(userID, projid, onlyExamples, exToTranscript, filterOnBothSpeeds);

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

    if (DEBUG)
      logger.info("getRecordFilterExercisesMatchingGender after removing recorded exercises " + unrecordedIDs.size());

    List<CommonExercise> unrecordedExercises = getUnrecordedForIDs(exercises, unrecordedIDs);

    // boolean useLangFilter = languageFilter != null && !languageFilter.isEmpty();


    if (DEBUG) {
      logger.info("getRecordFilterExercisesMatchingGender to be recorded " + unrecordedExercises.size() + " from " + exercises.size());

      unrecordedExercises.forEach(ex -> {
        List<ExerciseAttribute> collect = getDialogAttributes(ex);

        logger.info("getRecordFilterExercisesMatchingGender ex " + ex.getID() + " '" + ex.getEnglish() + "' = '" + ex.getForeignLanguage() + "' : " + collect);
      });
    }
    return unrecordedExercises;
  }

  @NotNull
  private List<ExerciseAttribute> getDialogAttributes(ClientExercise ex) {
    return ex.getAttributes()
        .stream()
        .filter(exerciseAttribute -> {
          String property = exerciseAttribute.getProperty();
          return (
              property.equalsIgnoreCase(SPEAKER_META_DATA) || property.equalsIgnoreCase(LANGUAGE_META_DATA));
        }).collect(Collectors.toList());
  }

  private List<CommonExercise> maybeDoInterpreterFilter(Collection<String> languageFilter,
                                                        Collection<String> speakerFilter, List<CommonExercise> unrecordedExercises) {
    if (languageFilter != null && !languageFilter.isEmpty()) {
      logger.info("getRecordFilterExercisesMatchingGender before " + unrecordedExercises.size() + " = " + languageFilter);
      unrecordedExercises = filterByMatchingLanguage(languageFilter, unrecordedExercises, LANGUAGE_META_DATA);
      logger.info("getRecordFilterExercisesMatchingGender after  " + unrecordedExercises.size());
    }

    if (speakerFilter != null && !speakerFilter.isEmpty()) {
      logger.info("getRecordFilterExercisesMatchingGender before " + unrecordedExercises.size() + " = " + speakerFilter);

      //   unrecordedExercises.forEach(commonExercise -> logger.info(commonExercise.getID() + " " + commonExercise.getEnglish() + " " + commonExercise.getForeignLanguage() + " " + commonExercise.getAttributes()));
      unrecordedExercises = filterByMatchingLanguage(speakerFilter, unrecordedExercises, SPEAKER_META_DATA);
      logger.info("getRecordFilterExercisesMatchingGender after  " + unrecordedExercises.size() + " = " + speakerFilter);
    }
    return unrecordedExercises;
  }

  @NotNull
  private List<CommonExercise> filterByMatchingLanguage(Collection<String> languageFilter, List<CommonExercise> unrecordedExercises, String languageMetaData) {
    return unrecordedExercises
        .stream()
        .filter(ex ->
            !ex.getAttributes()
                .stream()
                .filter(attr ->
                {
                  boolean typeMatch = attr.getProperty().equalsIgnoreCase(languageMetaData);
                  String value = attr.getValue();
                  boolean valueMatch = languageFilter.contains(value);
                  return typeMatch && valueMatch;
                })
                .collect(Collectors.toSet()).isEmpty()
        )
        .collect(Collectors.toList());
  }

  @NotNull
  private List<CommonExercise> getUnrecordedForIDs(Collection<CommonExercise> exercises, Set<Integer> unrecordedIDs) {
    List<CommonExercise> unrecordedExercises = new ArrayList<>();

    for (CommonExercise exercise : exercises) {
      if (unrecordedIDs.contains(exercise.getID())) {
        unrecordedExercises.add(exercise);
      }
    }
    return unrecordedExercises;
  }

  /**
   * @param userID
   * @param projid
   * @param onlyExamples
   * @param filterOnBothSpeeds
   * @return
   * @see #getRecordFilterExercisesMatchingGender
   */
  private Collection<Integer> getRecordedByMatchingGender(int userID,
                                                          int projid,
                                                          boolean onlyExamples,
                                                          Map<Integer, String> exToTranscript, boolean filterOnBothSpeeds) {
    logger.info("getRecordedByMatchingGender : for " +
        "\n\tuser #" + userID +
        "\n\tonly by same gender examples only " + onlyExamples);// + " from " + exercises.size());

    return onlyExamples ?
        databaseServices.getAudioDAO().getRecordedBySameGenderContext(userID, projid, exToTranscript) :
        databaseServices.getAudioDAO().getRecordedBySameGender(userID, projid, exToTranscript, filterOnBothSpeeds);
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

  /**
   * So we're asking for the exercises for this selection -
   *
   * @param request
   * @param projid
   * @return
   */
  @Override
  public List<CommonExercise> getExercisesForSelectionState(ExerciseListRequest request, int projid) {
    Collection<CommonExercise> exercisesForState = getExercisesForSelection(projid, request.getTypeToSelection());
    List<CommonExercise> copy = new ArrayList<>(exercisesForState);  // TODO : avoidable???
    copy = maybeFilterDownToJustDialog(request, projid, copy);
    return filterExercises(request, copy, projid);
  }

  private List<CommonExercise> maybeFilterDownToJustDialog(IRequest request, int projid, List<CommonExercise> copy) {
    if (request.getMode() == ProjectMode.DIALOG) {
      int before = copy.size();
      copy = getDialogExercisesFiltered(projid, copy);
      int after = copy.size();
      if (after != before) {
        logger.info("getExercisesForSelectionState after " + after + " before " + before);
      }
    }
    return copy;
  }


  /**
   * Ask section helper for matching item to type -> value
   *
   * @param projid
   * @param typeToSelection
   * @return
   * @see SectionHelper#getExercisesForSelectionState(Map)
   */
  private Collection<CommonExercise> getExercisesForSelection(int projid, Map<String, Collection<String>> typeToSelection) {
    Map<String, Collection<String>> copy = new HashMap<>(typeToSelection);
    copy.remove(RECORDED1);
    copy.remove(CONTENT);
    copy.remove(LANGUAGE_META_DATA);
    copy.remove(SPEAKER_META_DATA);

    boolean empty = copy.isEmpty();
    //logger.info("getExercisesForSelection empty = " + empty + " : " + copy);
    return empty ?
        getExercises(projid) :
        getSectionHelper(projid).getExercisesForSelectionState(copy);
  }
}