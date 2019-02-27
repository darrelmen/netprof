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

import mitll.langtest.server.services.ExerciseServiceImpl;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseListRequest;

import java.util.Collection;

public class ExerciseFiltering<T extends CommonShell> {
 /* private static final Logger logger = LogManager.getLogger(ExerciseFiltering.class);
  ExerciseServices exerciseServices;
  AmasServices amasServices;
  boolean isAmas;

  public ExerciseFiltering(ExerciseServices exerciseServices, AmasServices amasServices, boolean isAmas) {
    this.exerciseServices = exerciseServices;
    this.amasServices = amasServices;
    this.isAmas = isAmas;
  }

  private ExerciseListWrapper<T> getExerciseWhenNoUnitChapter(ExerciseListRequest request,
                                                              int projectID,
                                                              Project project,
                                                              UserList<CommonExercise> userListByID) {
    List<CommonExercise> exercises;
    boolean predefExercises = userListByID == null;
    exercises = predefExercises ? getExercises(project.getID()) : getCommonExercises(userListByID);

    // now if there's a prefix, filter by prefix match

    TripleExercises<CommonExercise> exercisesForSearch = new TripleExercises<>().setByExercise(exercises);
    String prefix = request.getPrefix();
    if (!prefix.isEmpty()) {
      logger.info("getExerciseWhenNoUnitChapter found prefix for user list " + userListByID);
      // now do a trie over matches
      exercisesForSearch = getExercisesForSearch(project,prefix, exercises, predefExercises);
      if (request.getLimit() > 0) {
        exercisesForSearch.setByExercise(getFirstFew(prefix, request, exercisesForSearch.getByExercise()));
      }
    }
//    logger.info("triple resp " + exercisesForSearch);
    exercisesForSearch.setByExercise(filterExercises(request, exercisesForSearch.getByExercise(), projectID));
    //   logger.info("after triple resp " + exercisesForSearch);

    // TODO : I don't think we need this?
*//*        if (!isUserListReq) {
          markRecordedState(userID, request.getActivityType(), exercises, request.isOnlyExamples());
        }*//*

    // now sort : everything gets sorted the same way
    List<CommonExercise> commonExercises = getSortedExercises(request, exercisesForSearch, predefExercises, request.getUserID());
    ExerciseListWrapper<T> exerciseListWrapper = makeExerciseListWrapper(request, commonExercises);

//    if (request.isNoFilter()) {
//      rememberCachedWrapper(projectID, exerciseListWrapper);
//    }
    return exerciseListWrapper;
  }

  *//**
   * @param request
   * @param exercises
   * @param projid
   * @return
   * @see #getExerciseIds
   *//*
  private List<CommonExercise> filterExercises(ExerciseListRequest request,
                                               List<CommonExercise> exercises,
                                               int projid) {
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
    if (request.isOnlyForUser()) {
      exercises = filterOnlyPracticedByUser(request, exercises, projid);
    }
    return exercises;
  }

  @NotNull
  private List<CommonExercise> filterOnlyPracticedByUser(ExerciseListRequest request, List<CommonExercise> exercises, int projid) {
    Collection<Integer> practicedByUser = db.getResultDAO().getPracticedByUser(request.getUserID(), projid);
    List<CommonExercise> copy = new ArrayList<>();

    // TODO:  gosh - wasteful most of the time....
    long then = System.currentTimeMillis();
    for (CommonExercise ex : exercises) {
      if (practicedByUser.contains(ex.getID())) copy.add(ex);
    }
    long now = System.currentTimeMillis();
    if (now - then > 100) {
      logger.info("filterExercises : took " + (now - then));
    }
    return copy;
  }

  *//**
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
   * @see #getExerciseIds
   * @see #getExercisesForSelectionState
   * @see #filterExercises
   *//*
  private List<CommonExercise> filterByUnrecorded(
      ExerciseListRequest request,
      List<CommonExercise> exercises,
      int projid) {
    if (request.isOnlyUnrecordedByMe()) {
      return getUnrecordedExercisesMatchingGender(request.getUserID(), exercises, projid, request.isOnlyExamples());
    } else {
      return request.isOnlyExamples() ? getExercisesWithContext(exercises) : exercises;
    }
  }

  *//**
   * TODO : slow?
   *
   * @param <T>
   * @param commonExercises
   * @param sortByFL
   * @param searchTerm
   * @paramx role
   * @see #getExerciseListWrapperForPrefix(ExerciseListRequest, Collection)
   * @see #getSortedExercises
   *//*
  private <T extends CommonShell> void sortExercises(boolean isRecorder, List<T> commonExercises, boolean sortByFL, String searchTerm) {
    new ExerciseSorter().getSorted(commonExercises, isRecorder, sortByFL, searchTerm);
  }
  *//**
   * TODO : this doesn't make sense - we need to do this on all projects, once.
   * Here it's just doing it on the first project that's asked for.
   * <p>
   * TODO : remove duplicate
   * Called from the client:
   *
   * @return
   * @see mitll.langtest.client.list.ListInterface#getExercises
   *//*
  private List<CommonExercise> getExercises(int projID) {
    long then = System.currentTimeMillis();
    List<CommonExercise> exercises = exerciseServices.getExercises(projID);
    long now = System.currentTimeMillis();
    if (now - then > 200) {
      logger.info("getExercises took " + (now - then) + " millis to get the raw exercise list for ");// + getLanguage());
    }

    return exercises;
  }

  *//**
   * Copies the exercises....?
   *
   * @param userListByID
   * @return
   * @see #getExerciseIds
   * @see #getExercisesFromUserListFiltered(java.util.Map, mitll.langtest.shared.custom.UserList)
   *//*
  private <T extends CommonShell> List<T> getCommonExercises(UserList<T> userListByID) {
    return new ArrayList<>(userListByID.getExercises());
  }


  *//**
   * TODO : revisit the parameterized types here.
   *
   * @param prefix
   * @param exercises
   * @param predefExercises
   * @param <T>
   * @return
   * @see #getExerciseIds
   *//*
  private <T extends CommonExercise> TripleExercises<T> getExercisesForSearch(Project project, String prefix,
                                                                              Collection<T> exercises,
                                                                              boolean predefExercises) {
    ExerciseTrie<T> fullTrie = (ExerciseTrie<T>) project.getFullTrie();
    return getExercisesForSearchWithTrie(project, prefix, exercises, predefExercises, fullTrie, project.getID());
  }


  *//**
   * If not the full exercise list, build a trie here and use it.
   *
   * @param <T>
   * @param prefix
   * @param exercises
   * @param predefExercises
   * @param fullTrie
   * @param projectID
   * @return
   * @see #getExercisesForSearch
   *//*
  private <T extends CommonExercise> TripleExercises<T> getExercisesForSearchWithTrie(Project project, String prefix,
                                                                                      Collection<T> exercises,
                                                                                      boolean predefExercises,
                                                                                      ExerciseTrie<T> fullTrie,
                                                                                      int projectID) {
    ExerciseTrie<T> trie = predefExercises ? fullTrie : new ExerciseTrie<>(exercises, getLanguage(project), getSmallVocabDecoder(project), true);
    List<T> basicExercises = trie.getExercises(prefix);
    logger.info("getExercisesForSearchWithTrie : predef " + predefExercises +
        " prefix " + prefix + " matches " + basicExercises.size());
    ExerciseTrie<T> fullContextTrie = (ExerciseTrie<T>) project.getFullContextTrie();
    List<T> exercieByExid = getExerciseByExid(prefix, projectID);

    return new TripleExercises<>(exercieByExid, basicExercises, predefExercises ? fullContextTrie.getExercises(prefix) : Collections.emptyList());
  }

  private SmallVocabDecoder getSmallVocabDecoder(Project project) {
    return getAudioFileHelper(project).getSmallVocabDecoder();
  }

  @Nullable
  private AudioFileHelper getAudioFileHelper(Project project) {
    if (project == null) {
      logger.error("getAudioFileHelper no current project???");
      return null;
    }
    return project.getAudioFileHelper();
  }

  private String getLanguage(Project project) {
    if (project == null) {
      logger.error("getLanguage : no current project ");
      return "unset";
    } else {
      return project.getProject().language();
    }
  }

  private <T extends CommonExercise> List<T> getExerciseByExid(String prefix, int projectID) {
    int exid = getExid(prefix);

    if (exid > 0) {
      logger.info("getExerciseByExid return exid " + exid);
      T exercise = getExercise(exid, false);
      if (exercise != null && exercise.getProjectIDFromUser() == projectID) {
        return Collections.singletonList(exercise);
      }
    }
    return Collections.emptyList();
  }

  private int getExid(String prefix) {
    int exid = -1;
    if (!prefix.isEmpty()) {
      try {
        exid = Integer.parseInt(prefix);
      } catch (NumberFormatException e) {
        // logger.info("getExercisesForSearchWithTrie can't parse search number '" + prefix + "'");
      }
    }
    return exid;
  }
  *//**
   * Return shortest matches first (on fl term).
   *
   * @param request
   * @param exercises
   * @return
   *//*
  @NotNull
  private List<CommonExercise> getFirstFew(String prefix, ExerciseListRequest request, List<CommonExercise> exercises) {
    //logger.info("getFirstFew only taking " + request.getLimit() + " from " + exercises.size() + " that match " + prefix);

    exercises.sort((o1, o2) -> {
      String foreignLanguage = o1.getForeignLanguage();
      boolean hasSearch1 = foreignLanguage.toLowerCase().contains(prefix.toLowerCase());
      String foreignLanguage1 = o2.getForeignLanguage();
      boolean hasSearch2 = foreignLanguage1.toLowerCase().contains(prefix.toLowerCase());

      if (hasSearch1 && !hasSearch2) return -1;
      else if (hasSearch2 && !hasSearch1) return +1;
      else if (hasSearch1 && hasSearch2) {
        int i = Integer.compare(foreignLanguage.length(), foreignLanguage1.length());
        return i == 0 ? foreignLanguage.compareTo(foreignLanguage1) : i;
      } else {
        String cforeignLanguage = o1.getContext();
        String cforeignLanguage1 = o2.getContext();
        int i = Integer.compare(cforeignLanguage.length(), cforeignLanguage1.length());
        return i == 0 ? cforeignLanguage.compareTo(cforeignLanguage1) : i;
      }
    });

    exercises = exercises.subList(0, Math.min(exercises.size(), request.getLimit()));

    return exercises;
  }

  *//**
   * NOTE NOTE NOTE : not doing ensureMP3 - since we likely don't have access to file system for here.
   * Joins with annotation data when doing QC.
   *
   * @param exid
   * @param isFlashcardReq
   * @return
   * @see mitll.langtest.client.list.ExerciseList#askServerForExercise
   * @see mitll.langtest.client.list.ExerciseList#goGetNextAndCacheIt
   * @see mitll.langtest.client.analysis.PlayAudio#playLast
   *//*
  public <T extends Shell> T getExercise(int userID, int projID, int exid, boolean isFlashcardReq) {
    if (isAmas) { // TODO : HOW TO AVOID CAST???
      return (T) amasServices.getAMASExercise(exid);
    }
    return getAnnotatedExercise(userID, projID, exid, isFlashcardReq);
  }

  *//**
   * Three buckets - match by id, matches on vocab item, then match on context sentences
   *
   * @param <T>
   *//*
  private static class TripleExercises<T extends CommonExercise> {
    private List<T> byID = Collections.emptyList();
    private List<T> byExercise = Collections.emptyList();
    private List<T> byContext = Collections.emptyList();

    TripleExercises() {
    }

    TripleExercises(List<T> byID, List<T> byExercise, List<T> byContext) {
      this.byID = byID;
      this.byExercise = byExercise;
      this.byContext = byContext;
    }

    public List<T> getByID() {
      return byID;
    }

 *//*   public void setByID(List<T> byID) {
      this.byID = byID;
    }*//*

    List<T> getByExercise() {
      return byExercise;
    }

    TripleExercises<T> setByExercise(List<T> byExercise) {
      this.byExercise = byExercise;
      return this;
    }

    *//**
     * @return
     * @see #getSortedExercises(ExerciseListRequest, ExerciseServiceImpl.TripleExercises, boolean, int)
     *//*
    List<T> getByContext() {
      return byContext;
    }

    *//**
     * @paramx byContext
     *//*
*//*
    public void setByContext(List<T> byContext) {
      this.byContext = byContext;
    }
*//*
    public String toString() {
      return "by id " + byID.size() + "  by ex " + byExercise.size() + " by context " + byContext.size();
    }
  }
*/
}
