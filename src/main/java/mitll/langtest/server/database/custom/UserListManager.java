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

package mitll.langtest.server.database.custom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.IDAO;
import mitll.langtest.server.database.annotation.IAnnotationDAO;
import mitll.langtest.server.database.annotation.UserAnnotation;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.project.ProjectServices;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.server.database.userlist.IUserExerciseListVisitorDAO;
import mitll.langtest.server.database.userlist.IUserListDAO;
import mitll.langtest.server.database.userlist.IUserListExerciseJoinDAO;
import mitll.langtest.server.database.userlist.SlickUserListDAO;
import mitll.langtest.shared.custom.*;
import mitll.langtest.shared.exercise.*;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickRelatedExercise;
import mitll.npdata.dao.SlickUserExerciseList;
import mitll.npdata.dao.userexercise.UserExerciseListVisitorDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/2/13
 * Time: 7:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserListManager implements IUserListManager {
  private static final Logger logger = LogManager.getLogger(UserListManager.class);

  public static final String CORRECT = "correct";
  private static final String INCORRECT = "incorrect";
  private static final String FIXED = "fixed";

  //  private static final String FAST = "regular";
//  private static final String SLOW = "slow";
  private static final String MY_FAVORITES = "My Favorites";
//  private static final String COMMENTS = "Comments";
//  private static final String ALL_ITEMS_WITH_COMMENTS = "All items with comments";


  private static final boolean DEBUG = false;

  private static final int MINUTE = 60 * 1000;
  private static final int HOUR = 60 * MINUTE;
  private static final int DAY = 24 * HOUR;
  private static final int YEAR = 365 * DAY;
  private static final int FIFTY_YEAR = 50 * YEAR;

  private final IUserDAO userDAO;
  private int i = 0;

  private IUserExerciseDAO userExerciseDAO;
  private final IUserListDAO userListDAO;
  private final IUserExerciseListVisitorDAO visitorDAO;
  private final IUserListExerciseJoinDAO userListExerciseJoinDAO;
  private final IAnnotationDAO annotationDAO;

  private final IStateManager stateManager;
  private final DatabaseServices databaseServices;

  /**
   * @param userDAO
   * @param userListDAO
   * @param userListExerciseJoinDAO
   * @param annotationDAO
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public UserListManager(IUserDAO userDAO,
                         IUserListDAO userListDAO,
                         IUserListExerciseJoinDAO userListExerciseJoinDAO,
                         IAnnotationDAO annotationDAO,
                         IStateManager stateManager,
                         IUserExerciseListVisitorDAO visitorDAO,
                         DatabaseServices databaseServices) {
    this.userDAO = userDAO;
    this.userListDAO = userListDAO;
    this.userListExerciseJoinDAO = userListExerciseJoinDAO;
    this.annotationDAO = annotationDAO;
    this.visitorDAO = visitorDAO;
    this.stateManager = stateManager;
    this.databaseServices = databaseServices;
  }

  /**
   * @param userid
   * @param name
   * @param description
   * @param dliClass
   * @param isPublic
   * @param projid
   * @return
   * @paramx start
   * @paramx end
   * @see mitll.langtest.server.services.ListServiceImpl#addUserList
   * @see mitll.langtest.client.custom.dialog.CreateListDialog#doCreate
   */
  @Override
  public UserList addUserList(int userid, String name, String description, String dliClass, boolean isPublic, int projid) {
    long start = System.currentTimeMillis();
    long end = start + FIFTY_YEAR;
    UserList userList = createUserList(userid, name, description, dliClass, !isPublic, projid, start, end);
    if (userList == null) {
      logger.warn("addUserList no user list??? for " + userid + " " + name);
      return null;
    } else {
      return userList;
    }
  }

  /**
   * @param userid
   * @param name
   * @param description
   * @param dliClass
   * @param isPublic
   * @param projid
   * @param size
   * @param duration
   * @param minScore
   * @param showAudio
   * @return
   * @see mitll.langtest.server.services.ListServiceImpl#addUserList
   */
  @Override
  public UserList addQuiz(int userid, String name, String description, String dliClass, boolean isPublic, int projid,
                          int size, int duration, int minScore, boolean showAudio, Map<String, String> unitChapter) {
    UserList userList = createQuiz(userid, name, description, dliClass, !isPublic, projid, size, false,
        new TimeRange(), duration, minScore, showAudio, unitChapter);
    if (userList == null) {
      logger.warn("addUserList no user list??? for " + userid + " " + name);
      return null;
    } else {
      return userList;
    }
  }

  /**
   * @param userid
   * @param name
   * @param description
   * @param dliClass
   * @param isPrivate
   * @param projid
   * @param start
   * @param end
   * @return null if user already has a list with this name
   * @see #addUserList
   * @see #createFavorites
   */
  private UserList createUserList(int userid,
                                  String name,
                                  String description,
                                  String dliClass,
                                  boolean isPrivate,
                                  int projid,
                                  long start,
                                  long end) {
    String userChosenID = userDAO.getUserChosenID(userid);
    String firstInitialName = userDAO.getFirstInitialName(userid);
    if (userChosenID == null) {
      logger.error("createUserList huh? no user with id " + userid);
      return null;
    } else {
      UserList e = new UserList(i++, userid, userChosenID, firstInitialName, name, description, dliClass, isPrivate,
          System.currentTimeMillis(), "", "", projid, UserList.LIST_TYPE.NORMAL, start, end, 10, 30, false);
      rememberList(projid, e);
//      new Thread(() -> logger.debug("createUserList : now there are " + userListDAO.getCount() + " lists total")).start();
      return e;
    }
  }

  /**
   * @param userid
   * @param name
   * @param description
   * @param dliClass
   * @param isPrivate
   * @param projid
   * @param reqSize
   * @param timeRange
   * @param duration
   * @param minScore
   * @param showAudio
   * @return
   */
  private UserList<CommonShell> createQuiz(int userid,
                                           String name,
                                           String description,
                                           String dliClass,
                                           boolean isPrivate,
                                           int projid,
                                           int reqSize,
                                           boolean isDryRun,
                                           TimeRange timeRange,
                                           int duration, int minScore, boolean showAudio, Map<String, String> unitChapter) {
    String userChosenID = userDAO.getUserChosenID(userid);
    String firstInitialName = userDAO.getFirstInitialName(userid);
    if (userChosenID == null) {
      logger.error("createUserList huh? no user with id " + userid);
      return null;
    } else {
      long now = System.currentTimeMillis();

      UserList<CommonShell> quiz = new UserList<>(i++, userid, userChosenID, firstInitialName, name, description, dliClass, isPrivate,
          now, "", "", projid, UserList.LIST_TYPE.QUIZ, timeRange.getStart(), timeRange.getEnd(), duration, minScore, showAudio);
      int userListID = rememberList(projid, quiz);

      logger.info("createQuiz made new quiz " + quiz + "\n\tfor size " + reqSize);
      Project project = databaseServices.getProject(projid);

      List<CommonExercise> items = new ArrayList<>();

      if (isDryRun) {
        getFirstEasyLength(project.getSectionHelper().getFirst());
        for (CommonExercise exercise : getFirstEasyLength(project.getSectionHelper().getFirst())) {
          items.add(exercise);
          if (items.size() == reqSize) break;
        }
      } else {

        addRandomItems(reqSize, project, items, unitChapter);
      }

      List<CommonShell> shells = getShells(items);
      logger.info("createQuiz made random ex list of size " + items.size() +
          " items " + items.size() +
          " shells " + shells.size()
      );
      quiz.setExercises(shells);
      items.forEach(ex -> addItemToList(userListID, ex.getID()));

      logger.info("createQuiz quiz has " + quiz.getExercises().size());
      return quiz;
    }
  }

  private void addRandomItems(int reqSize, Project project, List<CommonExercise> items, Map<String, String> unitChapter) {
    Map<String, Collection<String>> typeToSelection = new HashMap<>();
    unitChapter.forEach((k, v) -> {
      if (!v.equalsIgnoreCase("All")) {
        typeToSelection.put(k, Collections.singleton(v));
      }
    });

    Collection<CommonExercise> exercisesForSelectionState;
    if (typeToSelection.isEmpty()) {
      exercisesForSelectionState = project.getRawExercises();
    } else {
      exercisesForSelectionState = project.getSectionHelper().getExercisesForSelectionState(typeToSelection);
    }
    List<CommonExercise> rawExercises = new ArrayList<>(exercisesForSelectionState);
    addRandomItems(reqSize, items, rawExercises);

    logger.info("exercisesForSelectionState " + typeToSelection + " : " + exercisesForSelectionState.size() + " items " + items.size());
  }

  private void addRandomItems(int reqSize, List<CommonExercise> items, List<CommonExercise> toChooseFrom) {
    //  int misses = 0;
    Random random = new Random();

    //  int size = toChooseFrom.size();

    Set<Integer> exids = new TreeSet<>();

    reqSize = Math.min(reqSize, toChooseFrom.size());

    while (items.size() < reqSize) {
      int i = random.nextInt(toChooseFrom.size());
      CommonExercise commonExercise = toChooseFrom.get(i);
      boolean add = exids.add(commonExercise.getID());
      if (add) {
        items.add(commonExercise);
        toChooseFrom.remove(commonExercise);
/*
        int numPhones = commonExercise.getNumPhones();
        if (numPhones > 0 || misses > 100) {
          if (numPhones > 3) {
            if (numPhones > MIN_PHONE && numPhones < MAX_PHONE || items.size() > DRY_RUN_ITEMS) {
              items.add(commonExercise);
              // logger.info("createQuiz add " + commonExercise.getID() + " " + commonExercise.getForeignLanguage() + " " + numPhones);
            }
          }
        } else {
          logger.warn("no phones for " + commonExercise.getID() + " " + commonExercise.getForeignLanguage() + " " + numPhones);
          misses++;
        }
*/
      }
    }
  }

  @NotNull
  private List<CommonExercise> getFirstEasyLength(Collection<CommonExercise> firstCandidates) {
    List<CommonExercise> first = new ArrayList<>();
    int misses = 0;
    for (CommonExercise candidate : firstCandidates) {
      int length = candidate.getForeignLanguage().length();
      if (length > 4 && length < 10 || misses++ > 100) {
        first.add(candidate);
        if (first.size() == 10) break;
      }
    }
    return first;
  }

  private int rememberList(int projid, UserList e) {
    return userListDAO.add(e, projid);
  }

  /**
   * @param projid
   * @return
   * @see mitll.langtest.server.services.ListServiceImpl#getLightListsForUser
   */
  @Override
  public Collection<IUserList> getSimpleListsForUser(int projid,
                                                     int userid,
                                                     boolean listsICreated,
                                                     boolean visitedLists) {
    //List<SlickUserExerciseList> lists = getRawLists(userid, projid, listsICreated, visitedLists);
    //logger.info("getSimpleListsForUser for " + userid + " in " + projid + " found " + lists.size());
    return getSimpleLists(getRawLists(userid, projid, listsICreated, visitedLists));
  }

  /**
   * @param projid
   * @param userID
   * @param isQuiz
   * @return
   * @see mitll.langtest.server.services.ListServiceImpl#getSimpleListsForUser
   */
  @Override
  public Collection<IUserList> getAllPublicOrMine(int projid, int userID, boolean isQuiz) {
    return getSimpleLists(userListDAO.getSlickAllPublicOrMine(projid, userID, isQuiz));
  }

  @NotNull
  private Collection<IUserList> getSimpleLists(Collection<SlickUserExerciseList> lists) {
//    lists.forEach(slickUserExerciseList -> logger.info("\t" + slickUserExerciseList.id() + " " + slickUserExerciseList.name()));
    //Set<Integer> listIDs = getListIDs(lists);
    Map<Integer, Integer> numForList = userListExerciseJoinDAO.getNumExidsForList(getListIDs(lists));
    //  logger.info("asking for number of exercises for " + listIDs + "\n\tgot " + numForList);

    List<IUserList> names = new ArrayList<>(lists.size());
    Map<Integer, String> idToName = new HashMap<>();
    Map<Integer, String> idToFullName = new HashMap<>();
    lists.forEach(l -> {
      int id = l.id();

      Integer numItems = numForList.getOrDefault(id, 0);

      int userid1 = l.userid();
      String name = getUserName(userid1, idToName);
      String fullName = getFullName(userid1, idToFullName);
       logger.info("list #" + id + " - " + numItems + " "+fullName);

      names.add(
          new SimpleUserList(
              id,
              l.name(),
              l.projid(),
              userid1,
              name,
              fullName,
              numItems,
              l.duration(),
              l.minscore(),
              l.showaudio(),
              l.isprivate()));
    });
    return names;
  }

  @Nullable
  private String getUserName(int userid, Map<Integer, String> idToName) {
    String name = idToName.get(userid);
    if (name == null) {
      idToName.put(userid, userDAO.getUserChosenID(userid));
    }
    return name;
  }

  @Nullable
  private String getFullName(int userid, Map<Integer, String> idToName) {
    String name = idToName.get(userid);
    if (name == null) {
      idToName.put(userid, userDAO.getFirstInitialName(userid));
    }
    return name;
  }

  @Override
  public Collection<IUserListWithIDs> getListsWithIdsForUser(int userid,
                                                             int projid,
                                                             boolean listsICreated,
                                                             boolean visitedLists) {

    List<SlickUserExerciseList> lists = getRawLists(userid, projid, listsICreated, visitedLists);

    // logger.info("asking for number of exercises for " + listIDs);
    long then = System.currentTimeMillis();
    Map<Integer, Collection<Integer>> exidsForList = userListExerciseJoinDAO.getExidsForList(getListIDs(lists));

    long now = System.currentTimeMillis();

    if (now - then > 20) {
      logger.info("getListsWithIdsForUser found " + exidsForList.size() + " list->exids for " + userid + " and " + projid + " took " + (now - then));
    }
    Map<Integer, String> idToName = new HashMap<>();
    Map<Integer, String> idToFullName = new HashMap<>();

    List<IUserListWithIDs> names = new ArrayList<>(lists.size());
    lists.forEach(l -> {
      int id = l.id();

      int userid1 = l.userid();
      String name = getUserName(userid1, idToName);
      String fullName = getFullName(userid1, idToFullName);

      Collection<Integer> exids = exidsForList.getOrDefault(id, Collections.emptyList());
      logger.info("getListsWithIdsForUser For " + id + " got " + exids + " fullName " + fullName);
      names.add(
          new SimpleUserListWithIDs(
              id,
              l.name(),
              l.projid(),
              l.userid(),
              name,
              fullName,
              new ArrayList<>(exids),
              l.duration(),
              l.isprivate())
      );
    });
    return names;
  }

  private Set<Integer> getListIDs(Collection<SlickUserExerciseList> lists) {
    return lists
        .stream()
        .map(SlickUserExerciseList::id)
        .collect(Collectors.toSet());
  }

  /**
   * Sort returned list by time - desc - most recent first.
   *
   * @param userid
   * @param projid
   * @param listsICreated true if want to include my lists
   * @param visitedLists  true if want to include other's lists I've visited
   * @return
   */
  @NotNull
  private List<SlickUserExerciseList> getRawLists(int userid, int projid, boolean listsICreated, boolean visitedLists) {
    List<SlickUserExerciseList> lists = new ArrayList<>();
    SlickUserExerciseList favorite = listsICreated ? getCreatedAndFavorite(userid, projid, lists) : null;

    if (visitedLists) {
      {
        long then = System.currentTimeMillis();
        Collection<SlickUserExerciseList> visitedBy = userListDAO.getVisitedBy(userid, projid);
        long now = System.currentTimeMillis();
        long diff = now - then;
        if (diff > 20) {
          logger.info("getRawLists found " + visitedBy.size() + " visited lists for " + userid + " and " + projid + " took " + diff);
        }
        //logger.info("getRawLists found " + visitedBy.size() + " visited lists for " + userid + " and " + projid);
        lists.addAll(visitedBy);
      }
      sortByTime(getListToVisitTime(userid), lists);
    }

    {
      if (favorite != null) {
        lists.remove(favorite);
        lists.add(0, favorite);
      }
    }
    //logger.info("found " + lists.size() + " raw lists for " + userid + " and " + projid);

    return lists;
  }

  @NotNull
  private Map<Integer, Long> getListToVisitTime(int userid) {
    Map<Integer, Long> listToVisitTime = new HashMap<>();
    getVisitorDAOWrapper().allByUser(userid)
        .forEach(slickUserExerciseListVisitor ->
            listToVisitTime.put(slickUserExerciseListVisitor.userlistid(), slickUserExerciseListVisitor.modified().getTime()));
    return listToVisitTime;
  }

  /**
   * Descending - most recent first.
   *
   * @param listToVisitTime
   * @param sorted
   */
  private void sortByTime(Map<Integer, Long> listToVisitTime, List<SlickUserExerciseList> sorted) {
    sorted.sort((o1, o2) -> {
      int id = o1.id();
      int id2 = o2.id();

      long t1 = listToVisitTime.getOrDefault(id, o1.modified().getTime());
      long t2 = listToVisitTime.getOrDefault(id2, o2.modified().getTime());

      return -1 * Long.compare(t1, t2);
    });
  }

  /**
   * TODO : A little cheesy - why copy the list?
   *
   * @param userid
   * @param projid
   * @param lists  populated
   * @return favorites list out of all my lists
   */
  private SlickUserExerciseList getCreatedAndFavorite(int userid, int projid, List<SlickUserExerciseList> lists) {
    SlickUserExerciseList favorite = null;
    long then = System.currentTimeMillis();
    Collection<SlickUserExerciseList> byUser = userListDAO.getByUser(userid, projid);
    long now = System.currentTimeMillis();

    if (now - then > 30) {
      logger.info("getCreatedAndFavorite found " + byUser.size() + " lists by " + userid + " in " + projid + " took " + (now - then));
    }

    for (SlickUserExerciseList userList : byUser) {
      if (userList.isfavorite()) {
        favorite = userList;
      } else {
        //      logger.debug("not favorite " + userList + " " + userList.getName());
      }
      lists.add(userList);
    }

    return favorite;
  }

  /**
   * TODO : expensive -- could just be a query against your own lists and/or against visited lists...
   *
   * @param userid
   * @param projid
   * @param listsICreated
   * @param visitedLists
   * @param includeQuiz
   * @return
   * @seex #getMyLists
   * @see mitll.langtest.server.services.ListServiceImpl#getListsForUser
   */
  @Override
  public Collection<UserList<CommonShell>> getListsForUser(int userid,
                                                           int projid,
                                                           boolean listsICreated,
                                                           boolean visitedLists,
                                                           boolean includeQuiz) {
    if (userid == -1) {
      return Collections.emptyList();
    }
    if (DEBUG) {
      logger.debug("getListsForUser for user #" + userid + " only created " + listsICreated + " visited " + visitedLists);
    }

    List<UserList<CommonShell>> listsForUser = new ArrayList<>();
    UserList<CommonShell> favorite = null;
    Set<Integer> ids = new HashSet<>();

    if (listsICreated) {
      long then = System.currentTimeMillis();
      listsForUser = userListDAO.getAllByUser(userid, projid);
      long now = System.currentTimeMillis();

      if (DEBUG || now - then > 50) {
        logger.info("getListsForUser took " + (now - then) + " to find " + listsForUser.size() + " created by " + userid);
      }

      for (UserList<CommonShell> userList : listsForUser) {
        if (userList.isFavorite()) {
          favorite = userList;
        } else {
          //      logger.debug("not favorite " + userList + " " + userList.getName());
        }
        ids.add(userList.getID());
      }
    }

    if (visitedLists) {
      long then = System.currentTimeMillis();
      Collection<UserList<CommonShell>> listsForUser1 = userListDAO.getVisitedLists(userid, projid);
      long now = System.currentTimeMillis();

      if (DEBUG || now - then > 50) {
        logger.info("getListsForUser took " + (now - then) + " to find " + listsForUser1.size() + " visited by " + userid);
      }

      addIfNotThere(listsForUser, ids, listsForUser1);
    }

    boolean isAdmin = userDAO.isAdmin(userid);
    if (isAdmin && includeQuiz) {
      addIfNotThere(listsForUser, ids, userListDAO.getAllQuiz(projid));
    }

    // put favorite at front
    {
      if (listsForUser.isEmpty()) {
        if (DEBUG) {
          logger.warn("getListsForUser - list is empty for " + userid + " only created " +
              listsICreated + " visited " + visitedLists);
        }
      } else if (favorite != null) {
        listsForUser.remove(favorite);
        listsForUser.add(0, favorite);// put at front
      }
    }

/*    if (DEBUG) {
      logger.debug("getListsForUser found " + listsForUser.size() +
          " lists for user #" + userid + " only created " + listsICreated + " visited " + visitedLists +
          "\n\tfavorite " + favorite);
      if (listsForUser.size() < 4) {
        for (UserList ul : listsForUser) logger.debug("\t" + ul);
      }
    }*/

    return listsForUser;
  }

  private void addIfNotThere(List<UserList<CommonShell>> listsForUser, Set<Integer> ids, Collection<UserList<CommonShell>> listsForUser1) {
    for (UserList<CommonShell> userList : listsForUser1) {
      if (!ids.contains(userList.getID())) {
        listsForUser.add(userList);
      }
    }
  }

  /**
   * @param userid
   * @param projid
   * @return
   * @see ProjectServices#rememberUsersCurrentProject(int, int)
   */
  @Override
  public void createFavorites(int userid, int projid) {
    if (userListDAO.getByName(userid, UserList.MY_LIST, projid).isEmpty()) {
      long start = System.currentTimeMillis();
      createUserList(userid, UserList.MY_LIST, MY_FAVORITES, "", true, projid, start, start);
    }
  }

  @NotNull
  private List<CommonShell> getShells(Collection<CommonExercise> allKnown) {
    List<CommonShell> commonShells = new ArrayList<>(allKnown.size());
    allKnown.forEach(commonExercise -> commonShells.add(commonExercise.getShell()));
    return commonShells;
  }

  /**
   * Really create a new exercise and associated context exercise in database.
   * Add newly created exercise to the user list.
   *
   * @param userListID
   * @param userExercise notional until now!
   * @see mitll.langtest.server.services.ListServiceImpl#newExercise
   * @see mitll.langtest.server.services.ListServiceImpl#addItemsToList
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#afterValidForeignPhrase
   */
  @Override
  public void newExercise(int userListID, CommonExercise userExercise) {
    int projectID = userExercise.getProjectID();

    Project project = databaseServices.getProject(projectID);
    List<String> typeOrder = project.getTypeOrder();

//    exercisePhoneInfo = getExercisePhoneInfo(lookup, foreignlanguage, transliteration);
//    int n2 = getNumPhones(lookup, exercisePhoneInfo, foreignlanguage, transliteration);

    int newExerciseID = userExerciseDAO.add(userExercise, false, typeOrder);

    setNumPhones(userExercise, project, newExerciseID);

    logger.info("newExercise added exercise " + newExerciseID + " from " + userExercise);

    project.getExerciseDAO().addUserExercise(userExercise);

    int contextID = 0;
    try {
      contextID = makeContextExercise(userExercise, typeOrder);

      ClientExercise next = userExercise.getDirectlyRelated().iterator().next();
      project.getExerciseDAO().addUserExercise(next.asCommon());

      String foreignLanguage = next.getForeignLanguage();

      if (!foreignLanguage.isEmpty()) {
        setNumPhones(next.asCommon(), project, contextID);
      }

    } catch (Exception e) {
      logger.error("Got " + e, e);
    }

//    logger.info("newExercise added context exercise " + contextID + " tied to " + newExerciseID + " in " + projectID);

    addItemToList(userListID, newExerciseID);
  }

  private void setNumPhones(CommonExercise userExercise, Project project, int newExerciseID) {
    userExercise.getMutable().setNumPhones(
        databaseServices.getUserExerciseDAO().getAndRememberNumPhones(project,
            newExerciseID, userExercise.getForeignLanguage(), userExercise.getTransliteration()
        ));
  }

  public UserList getUserListNoExercises(int userListID) {
    logger.info("getUserListNoExercises for " + userListID);
    return userListDAO.getWhere(userListID, true);
  }

  /**
   * @param parentExercise
   * @return
   * @seex #newExerciseOnList(UserList, CommonExercise)
   * @paramx newExerciseID
   * @paramx projectID
   */
  private int makeContextExercise(CommonExercise parentExercise, List<String> typeOrder) {
    int projectID = parentExercise.getProjectID();
    Exercise contextExercise = new Exercise(-1, parentExercise.getCreator(), "", projectID, false);
    int contextID = userExerciseDAO.add(contextExercise, true, typeOrder);

    long time = System.currentTimeMillis();
    userExerciseDAO.getRelatedExercise().addBulkRelated(Collections.singletonList(new SlickRelatedExercise(-1, parentExercise.getID(), contextID, projectID, 1,
        new Timestamp(time))));

    parentExercise.getDirectlyRelated().add(contextExercise);

    return contextID;
  }

  /**
   * @param userListID
   * @param exid
   * @see mitll.langtest.server.services.ListServiceImpl#addItemToUserList
   */
  @Override
  public void addItemToList(int userListID, int exid) {
    userListExerciseJoinDAO.add(userListID, exid);
    userListDAO.updateModified(userListID);
  }

  /**
   * @param userExercise
   * @param typeOrder
   * @see mitll.langtest.server.database.DatabaseImpl#editItem
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#editItem
   */
  @Override
  public void editItem(CommonExercise userExercise, Collection<String> typeOrder) {
    boolean update = userExerciseDAO.update(userExercise, userExercise.isContext(), typeOrder);
    if (update) {
      databaseServices.getProject(userExercise.getProjectID()).getExerciseDAO().addUserExercise(userExercise);
      setNumPhones(userExercise, databaseServices.getProject(userExercise.getProjectID()), userExercise.getID());
    } else {
      logger.warn("editItem : did not update item  " + userExercise);
    }
  }

  public void clearAudio(int audioID) {
    databaseServices.getAudioDAO().markDefect(audioID);
  }


  /**
   * @param userExerciseDAO
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public void setUserExerciseDAO(IUserExerciseDAO userExerciseDAO) {
    this.userExerciseDAO = userExerciseDAO;
    userListDAO.setUserExerciseDAO(userExerciseDAO);
  }

  /**
   * consider how to ask for just annotations for a project, instead of getting all of them
   * and then filtering for just those on the requested project
   *
   * @param id
   * @return
   * @see
   * @see IUserListManager#deleteItemFromList(int, int)
   */
  @Override
  public UserList<CommonShell> getUserListByID(int id) {
    if (id == -1) {
      logger.error("getUserListByID : huh? asking for id " + id);
      return null;
    }
    UserList<CommonShell> where = userListDAO.getWhere(id, true);

    if (where != null) {
      final int projid = where.getProjid();
      getCommonExercisesOnList(projid, id).forEach(where::addExercise);
    }
    return where;
  }

  @NotNull
  public List<CommonExercise> getCommonExercisesOnList(int projid, int id) {
    Collection<Integer> exidsForList = userListExerciseJoinDAO.getExidsForList(id);
    logger.info("getCommonExercisesOnList found " + exidsForList.size() + " exids for list #" + id);

    List<CommonExercise> exercises = new ArrayList<>(exidsForList.size());
    exidsForList.forEach(exid -> exercises.add(databaseServices.getExercise(projid, exid)));
    return exercises;
  }

  @Override
  public UserList<CommonShell> getSimpleUserListByID(int id) {
    if (id == -1) {
      logger.error("getUserListByID : huh? asking for id " + id);
      return null;
    } else {
      return userListDAO.getWithExercises(id);
    }
  }

  /**
   * @param userListID
   * @param user
   * @return null if can't find the user list OR it's a quiz - can't visit a quiz
   * @seex mitll.langtest.client.custom.Navigation#addVisitor
   * @seex mitll.langtest.server.LangTestDatabaseImpl#addVisitor
   */
  @Override
  public UserList addVisitor(int userListID, int user) {
    logger.debug("addVisitor - user " + user + " visits " + userListID);
    UserList where = getUserListNoExercises(userListID);

    if (where != null) {
      if (where.getListType() == UserList.LIST_TYPE.QUIZ) {
        return null;
      } else {
        userListDAO.addVisitor(where.getID(), user);
        return where;
      }
    } else if (userListID > 0) {
      logger.warn("addVisitor - can't find list with id " + userListID);
      return null;
    } else {
      return null;
    }
  }

  public void removeVisitor(int userListID, int user) {
    userListDAO.removeVisitor(userListID, user);
  }

  private boolean listExists(int id) {
    return userListDAO.getWhere(id, false) != null;
  }

  /**
   * @param exercise
   * @param field
   * @param comment
   * @see mitll.langtest.server.database.exercise.BaseExerciseDAO#addDefects
   */
  @Override
  public boolean addDefect(CommonExercise exercise, String field, String comment) {
    int id = exercise.getID();
    if (!annotationDAO.hasDefect(id, field, INCORRECT, comment)) {
      addAnnotation(id, field, INCORRECT, comment, userDAO.getDefectDetector());
      markState(exercise, STATE.DEFECT, userDAO.getDefectDetector());
      return true;
    } else {
      return false;
    }
  }

  /**
   * @param exerciseID
   * @param field
   * @param status
   * @param comment
   * @param userid
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#addAnnotation
   */
  @Override
  public void addAnnotation(int exerciseID, String field, String status, String comment, int userid) {
    UserAnnotation annotation = new UserAnnotation(exerciseID, field, status, comment, userid, System.currentTimeMillis());
    //logger.debug("addAnnotation " + annotation);
    annotationDAO.add(annotation);
  }

  /**
   * TODO : really should do this in batch!
   *
   * @param exercise
   * @see mitll.langtest.server.services.ExerciseServiceImpl#addAnnotations
   * @see #markAllFieldsFixed
   */
  @Override
  public void addAnnotations(ClientExercise exercise) {
    if (exercise != null) {
      {
        MutableAnnotationExercise mutableAnnotation = exercise.getMutableAnnotation();
        Map<String, ExerciseAnnotation> latestByExerciseID = annotationDAO.getLatestByExerciseID(exercise.getID());
//      logger.info("addAnnotations to ex " +exercise.getID() + " got " + latestByExerciseID.size()+  " annos to fields " + latestByExerciseID.keySet());

        latestByExerciseID.forEach((k, v) -> mutableAnnotation.addAnnotation(k, v.getStatus(), v.getComment()));
      }

  /*    exercise.getDirectlyRelated().forEach(context -> {
        MutableAnnotationExercise mutableAnnotationContext = context.getMutableAnnotation();
        annotationDAO.getLatestByExerciseID(context.getID()).forEach((k, v) -> mutableAnnotationContext.addAnnotation(k, v.getStatus(), v.getComment()));
      });*/

//      for (Map.Entry<String, ExerciseAnnotation> pair : latestByExerciseID.entrySet()) {
//        mutableAnnotation.addAnnotation(pair.getKey(), pair.getValue().getStatus(), pair.getValue().getComment());
//      }
    } else {
      logger.warn("addAnnotations : on an empty exercise?");
    }
  }

  /**
   * @param exercise
   * @param correct
   * @param userid
   * @see mitll.langtest.client.qc.QCNPFExercise#markReviewed
   */
  @Override
  public void markCorrectness(CommonExercise exercise, boolean correct, int userid) {
    markState(exercise, correct ? STATE.APPROVED : STATE.DEFECT, userid);
  }

  /**
   * @param exercise
   * @param state
   * @param creatorID
   * @see #addAnnotation
   * @see mitll.langtest.client.qc.QCNPFExercise#markReviewed
   */
  @Override
  public void markState(CommonExercise exercise, STATE state, int creatorID) {
    logger.info("markState mark state " + exercise.getID() + " = " + state + " by " + creatorID);

    stateManager.setState(exercise, state, creatorID);

    if (state.equals(STATE.FIXED)) {
      markAllFieldsFixed(exercise, creatorID);
    }
  }

  /**
   * @param userExercise
   * @param userid
   * @see IUserListManager#markState
   */
  private void markAllFieldsFixed(CommonExercise userExercise, int userid) {
    Collection<String> fields = userExercise.getFields();
    // logger.debug("setExerciseState " + userExercise + "  has " + fields + " user " + userid);
    addAnnotations(userExercise);
    for (String field : fields) {
      ExerciseAnnotation annotation1 = userExercise.getAnnotation(field);
      if (!annotation1.isCorrect()) {
        logger.debug("\tsetExerciseState " + userExercise.getID() + "  has " + annotation1);

        addAnnotation(userExercise.getID(), field, CORRECT, FIXED, userid);
      }
    }
  }


  /**
   * @param id
   * @return
   * @seez ListManager#deleteList
   * @see mitll.langtest.server.services.ListServiceImpl#deleteList
   */
  @Override
  public boolean deleteList(int id) {
    logger.debug("deleteList " + id);
    userListExerciseJoinDAO.removeListRefs(id);
    boolean b = listExists(id);
    if (!b) logger.warn("\tdeleteList huh? no list " + id);
    return b && userListDAO.remove(id);
  }

  /**
   * @param listid
   * @param exid
   * @return
   * @see mitll.langtest.server.services.ListServiceImpl#deleteItemFromList
   */
  @Override
  public boolean deleteItemFromList(int listid, int exid) {
    logger.debug("deleteItemFromList " + listid + " ex = " + exid);

    UserList<?> userListByID = getUserListByID(listid);
    if (userListByID == null) {
      logger.warn("deleteItemFromList huh? no user list with id " + listid);
      return false;
    }

    userListByID.remove(exid);
    return userListExerciseJoinDAO.remove(listid, exid);
  }

  /**
   * @return
   * @seex mitll.langtest.server.services.ExerciseServiceImpl#filterByOnlyAudioAnno
   */
/*  @Override
  public Collection<Integer> getAudioAnnos() {
    return annotationDAO.getAudioAnnos();
  }*/
  @Override
  public IAnnotationDAO getAnnotationDAO() {
    return annotationDAO;
  }

  @Override
  public IUserListDAO getUserListDAO() {
    return userListDAO;
  }

  @Override
  public IUserListExerciseJoinDAO getUserListExerciseJoinDAO() {
    return userListExerciseJoinDAO;
  }

  @Override
  public void createTables(DBConnection dbConnection, List<String> created) {
    List<IDAO> idaos = Arrays.asList(
        userListDAO,
        userListExerciseJoinDAO,
        stateManager.getReviewedDAO(),
        stateManager.getSecondStateDAO()
    );

    for (IDAO dao : idaos) createIfNotThere(dbConnection, dao, created);

    String userexerciselistvisitor = "userexerciselistvisitor";
    if (!dbConnection.hasTable(userexerciselistvisitor)) {
      getVisitorDAOWrapper().createTable();
      created.add(userexerciselistvisitor);
    }
  }

  private UserExerciseListVisitorDAOWrapper getVisitorDAOWrapper() {
    return ((SlickUserListDAO) userListDAO).getVisitorDAOWrapper();
  }

  private void createIfNotThere(DBConnection dbConnection, IDAO slickUserDAO, List<String> created) {
    String name = slickUserDAO.getName();
    if (!dbConnection.hasTable(name)) {
      slickUserDAO.createTable();
      created.add(name);
    }
  }

  @Override
  public IUserExerciseListVisitorDAO getVisitorDAO() {
    return visitorDAO;
  }

  /**
   * @param userList
   * @see mitll.langtest.client.custom.dialog.CreateListDialog#doEdit
   * @see mitll.langtest.server.services.ListServiceImpl#update
   */
  @Override
  public void update(UserList userList) {
    userListDAO.update(userList);
  }

  @Override
  public boolean updateProject(int oldid, int newid) {
    return userListDAO.updateProject(oldid, newid);
  }

  public JsonObject getListsJson(int userID, int projid, boolean isQuiz) {
//    return getLists(getUserListDAO().getAllOrMineLight(projid, userID, isQuiz), isQuiz);
    Collection<IUserList> allPublicOrMine = getAllPublicOrMine(projid, userID, isQuiz);
    allPublicOrMine.removeIf(iUserList -> iUserList.getNumItems() == 0);
    return getLists(allPublicOrMine, isQuiz);
  }

  private JsonObject getLists(Collection<IUserList> lights, boolean isQuiz) {
    JsonArray lists = new JsonArray();
    JsonObject container = new JsonObject();
    for (IUserList light : lights) {
      JsonObject idAndName = new JsonObject();

      int id = light.getID();
      idAndName.addProperty("id", id);
      idAndName.addProperty("name", light.getName());
      idAndName.addProperty("numItems", light.getNumItems());

      if (isQuiz) {
        QuizSpec quizInfo = getQuizInfo(id);
        idAndName.addProperty("quizMinutes", quizInfo.getRoundMinutes());
        idAndName.addProperty("minScoreToAdvance", quizInfo.getMinScore());
        idAndName.addProperty("playAudio", quizInfo.isShowAudio());
      }
      lists.add(idAndName);
    }
    container.add("lists", lists);

    return container;
  }

  public QuizSpec getQuizInfo(int userListID) {
    UserList<?> list = getUserListDAO().getList(userListID);
    if (list == null) logger.warn("no quiz with list id " + userListID);
    QuizSpec quizSpec = list != null ? new QuizSpec(list.getRoundTimeMinutes(), list.getMinScore(), list.shouldShowAudio()) : new QuizSpec(10, 35, false);
    if (DEBUG) logger.info("Returning " + quizSpec + " for " + userListID);
    return quizSpec;
  }
}
