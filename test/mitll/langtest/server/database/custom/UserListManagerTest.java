package mitll.langtest.server.database.custom;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.PhoneDAO;
import mitll.langtest.shared.*;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.instrumentation.Event;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by GO22670 on 1/30/14.
 */
public class UserListManagerTest {
  public static final String CHANGED = "changed";
  public static final String ENGLISH = "english";
  private static Logger logger = Logger.getLogger(UserListManagerTest.class);
  private static DatabaseImpl database;
  private static String test;

  @BeforeClass
  public static void setup() {
    logger.debug("setup called");

    String english = "mandarin";
    File file = new File("war" + File.separator + "config" + File.separator + english + File.separator + "quizlet.properties");
    String parent = file.getParent();
    logger.debug("config dir " + parent);
    logger.debug("config     " + file.getName());
    test = "forMe2";
    database = new DatabaseImpl(parent, file.getName(), test, new ServerProperties(parent,file.getName()),new PathHelper("war"), false,null);
    logger.debug("made " +database);
    database.setInstallPath(".", parent +File.separator+database.getServerProps().getLessonPlan(),english,true,".");

    database.getExercises();
  }

/*  private static DatabaseImpl makeDatabaseImpl(String configDir) {
    ServerProperties serverProps = new ServerProperties(configDir, "quizlet.properties");
    String h2Database = serverProps.getH2Database();
    return new DatabaseImpl(configDir, configDir, h2Database, serverProps, null, true, null);
  }*/

  @Test
  public void testSorted() {
    Map<String, Collection<String>> typeToValues = new HashMap<String, Collection<String>>();
    typeToValues.put("Lesson",Arrays.asList("1"));
    int userid = 1;
    database.getJsonPhoneReport(userid,typeToValues);
    database.getJsonScoreHistory(userid,typeToValues);
  }

  @Test
  public void testQuery() {
    database.getResultDAO().attachScoreHistory(1,database.getExercise("1"),true);

    List<String> strings = Arrays.asList("1", "2", "3", "4", "5");
    ArrayList<CommonExercise> commonExercises = new ArrayList<CommonExercise>();
    for (String id : strings) commonExercises.add(database.getExercise(id));
    List<CommonExercise> exercisesSortedIncorrectFirst = database.getResultDAO().getExercisesSortedIncorrectFirst(commonExercises, 1);
    logger.debug("got " +exercisesSortedIncorrectFirst);

  }

  @Test
  public void doReport() {
    String x = database.getReport().doReport();
    System.out.println(x);
    try {
      //FileOutputStream fileOutputStream = new FileOutputStream("test2.html");
      BufferedWriter writer = new BufferedWriter(new FileWriter("test2.html"));
      writer.write(x);
      writer.close();
    } catch (IOException e) {


    }
    if (true) return;
    //  if (reported) return;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("u");
    String format = simpleDateFormat.format(new Date());
    if (format.equals("1")) {
      logger.debug("it's monday");
    }
    else logger.debug("it's " +format);
   // reported = true;
    List<User> users = database.getUserDAO().getUsers();

    Calendar calendar = new GregorianCalendar();
    int year       = calendar.get(Calendar.YEAR);
    logger.debug("year " + year);
    calendar.set(Calendar.YEAR, year);
    calendar.set(Calendar.DAY_OF_YEAR, 1);
    Date january1st = calendar.getTime();
    logger.debug("jan first " +january1st);

    int ytd = 0;

    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM/dd/yy h:mm aaa");
    Map<String,Integer> monthToCount = new TreeMap<String, Integer>();
    Map<Integer,Integer> weekToCount = new TreeMap<Integer, Integer>();
    for (User user : users) {
      try {
        if (user.getTimestamp().isEmpty()) continue;
        Date parse = simpleDateFormat2.parse(user.getTimestamp());
        if (parse.getTime() > january1st.getTime()) {
          ytd++;

          calendar.setTime(parse);
          int i = calendar.get(Calendar.MONTH);
          String month1 = getMonth(i);
          Integer integer = monthToCount.get(month1);
          monthToCount.put(month1,(integer == null) ? 1 : integer+1);

          int w = calendar.get(Calendar.WEEK_OF_YEAR);
          Integer integer2 = weekToCount.get(w);

          weekToCount.put(w,(integer2 == null) ? 1 : integer2+1);
        }
        else {
          logger.debug("NO time " +user.getTimestamp() + " " + parse);
        }
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    logger.debug("ytd " +ytd);
    logger.debug("month " +monthToCount);
    logger.debug("week " +weekToCount);

    getResults();
    getEvents();

  }

  private void getResults() {
    Calendar calendar = new GregorianCalendar();
    int year = calendar.get(Calendar.YEAR);
    logger.debug("year " + year);
    calendar.set(Calendar.YEAR, year);
    calendar.set(Calendar.DAY_OF_YEAR, 1);
    Date january1st = calendar.getTime();
    logger.debug("jan first " + january1st);

    int ytd = 0;

    List<Result> results = database.getResultDAO().getResults();
    Map<String, Integer> monthToCount = new TreeMap<String, Integer>();
    Map<Integer, Integer> weekToCount = new TreeMap<Integer, Integer>();
    for (Result result : results) {
      if (result.getTimestamp() > january1st.getTime()) {
        ytd++;
        calendar.setTimeInMillis(result.getTimestamp());
        int i = calendar.get(Calendar.MONTH);
        String month1 = getMonth(i);
        Integer integer = monthToCount.get(month1);
        monthToCount.put(month1, (integer == null) ? 1 : integer + 1);

        int w = calendar.get(Calendar.WEEK_OF_YEAR);
        Integer integer2 = weekToCount.get(w);

        weekToCount.put(w, (integer2 == null) ? 1 : integer2 + 1);
      }
    }
    logger.debug("ytd " + ytd);
    logger.debug("month " + monthToCount);
    logger.debug("week " + weekToCount);
  }


  private void getEvents() {
    Calendar calendar = new GregorianCalendar();
    int year = calendar.get(Calendar.YEAR);
    logger.debug("year " + year);
    calendar.set(Calendar.YEAR, year);
    calendar.set(Calendar.DAY_OF_YEAR, 1);
    Date january1st = calendar.getTime();
    logger.debug("jan first " + january1st);

    int ytd = 0;

    List<Event> all = database.getEventDAO().getAll();
    Map<String, Set<Long>> monthToCount = new TreeMap<String, Set<Long>>();

    Map<String, Map<Long,Set<Event>>> monthToCount2 = new TreeMap<String, Map<Long,Set<Event>>>();
    Map<Integer, Map<Long,Set<Event>>> weekToCount2 = new TreeMap<Integer, Map<Long,Set<Event>>>();

    Map<Integer, Set<Long>> weekToCount = new TreeMap<Integer, Set<Long>>();

    for (Event event : all) {
      if (event.getTimestamp() > january1st.getTime()) {
        ytd++;
        calendar.setTimeInMillis(event.getTimestamp());

        // months
        int i = calendar.get(Calendar.MONTH);
        String month1 = getMonth(i);
        long creatorID = event.getCreatorID();
        Set<Long> users = monthToCount.get(month1);
        if (users == null) {
          monthToCount.put(month1, users = new HashSet<Long>());
        }
        users.add(creatorID);


        Map<Long, Set<Event>> userToEvents = monthToCount2.get(month1);
        if (userToEvents == null) {
          monthToCount2.put(month1, userToEvents = new HashMap<Long, Set<Event>>());
        }
        Set<Event> events = userToEvents.get(creatorID);
        if (events == null) userToEvents.put(creatorID, events = new TreeSet<Event>());
        events.add(event);

        // weeks

        int w = calendar.get(Calendar.WEEK_OF_YEAR);
        Set<Long> users2 = weekToCount.get(w);
        if (users2 == null) {
          weekToCount.put(w, users2 = new HashSet<Long>());
        }
        users2.add(creatorID);

        userToEvents = weekToCount2.get(w);
        if (userToEvents == null) {
          weekToCount2.put(w, userToEvents = new HashMap<Long, Set<Event>>());
        }
        events = userToEvents.get(creatorID);

        if (events == null) userToEvents.put(creatorID, events = new TreeSet<Event>());
        events.add(event);
      }
    }
    logger.debug("ytd " + ytd);

    logger.debug("month " + monthToCount);
    logger.debug("week " + weekToCount);

    Map<String, Long> monthToDur = getMonthToDur(monthToCount2);
    logger.debug("month to dur " + monthToDur);

    Map<Integer, Long> weekToDur = getWeekToDur(weekToCount2);
    logger.debug("week to dur " + weekToDur);
  }

  private Map<String, Long> getMonthToDur(Map<String, Map<Long, Set<Event>>> monthToCount2) {
    Map<String,Long> monthToDur = new TreeMap<String, Long>();
    for (Map.Entry<String, Map<Long, Set<Event>>> monthToUserToEvents : monthToCount2.entrySet()) {
      String month = monthToUserToEvents.getKey();
      //logger.debug("month " + month);

      Map<Long, Set<Event>> userToEvents = monthToUserToEvents.getValue();

      for (Map.Entry<Long, Set<Event>> eventsForUser : userToEvents.entrySet()) {
        Long user = eventsForUser.getKey();
        //logger.debug("\tuser " + user);
        long start = 0;
        long dur = 0;
       // long begin = 0;
        long last = 0;
        for (Event event : eventsForUser.getValue()) {
          long now = event.getTimestamp();
     //     if (user == INTTEST) {
       //     logger.debug("Event " + event);
        //  }
          if (start == 0) {
            start = now;
          }
          else if (now - last > 1000*300) {
            dur += (last-start)/1000;
            start = now;
          }

          last = now;
        }
        dur += (last-start)/1000;
//        if (user == INTTEST) {
  //        logger.debug("dur " + dur);
   //     }
        Long aLong = monthToDur.get(month);
        monthToDur.put(month,aLong == null ? dur : aLong + dur);
      }
    }
    return monthToDur;
  }

  private Map<Integer, Long> getWeekToDur(Map<Integer, Map<Long, Set<Event>>> weekToCount) {
    Map<Integer,Long> weekToDur = new TreeMap<Integer, Long>();
    for (Map.Entry<Integer, Map<Long, Set<Event>>> weekToUserToEvents : weekToCount.entrySet()) {
      Integer week = weekToUserToEvents.getKey();
      logger.debug("week " + week);

      Map<Long, Set<Event>> userToEvents = weekToUserToEvents.getValue();

      for (Map.Entry<Long, Set<Event>> eventsForUser : userToEvents.entrySet()) {
        Long user = eventsForUser.getKey();
        logger.debug("\tuser " + user);
        long start = 0;
        long dur = 0;
        // long begin = 0;
        long last = 0;
        for (Event event : eventsForUser.getValue()) {
          long now = event.getTimestamp();

          if (start == 0) {
            start = now;
          }
          else if (now - last > 1000*300) {
            dur += (last-start)/1000;
            start = now;
          }

          last = now;
        }
        dur += (last-start)/1000;
//        if (user == INTTEST) {
        //        logger.debug("dur " + dur);
        //     }
        Long aLong = weekToDur.get(week);
        weekToDur.put(week,aLong == null ? dur : aLong + dur);
      }
    }
    return weekToDur;
  }


  private String getMonth(int i) {
    return Arrays.asList("JANUARY",
        "FEBRUARY",
        "MARCH",
        "APRIL",
        "MAY",
        "JUNE",
        "JULY",
        "AUGUST",
        "SEPTEMBER",
        "OCTOBER",
        "NOVEMBER",
        "DECEMBER").get(i);
  }

  @AfterClass
  public static void tearDown() {
    try {
      database.closeConnection();
      File db = new File(getConfigDir(), test + ".h2.db");
      if (db.exists()) {
        if (!db.delete()) {
          logger.error("huh? couldn't delete " + db.getAbsolutePath());

        }
      } else {
        logger.error("huh? no " + db.getAbsolutePath());
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private static String getConfigDir() {
    return "war" + File.separator + "config" + File.separator + ENGLISH;
  }

  @Test
  public void testAdd() {
    List<User> users = database.getUsers();
    long user;
    //if (users.isEmpty()) {
      users = addAndGetUsers("test22");
    //}
    user = users.get(users.size()-1).getId();
    logger.debug("Got user " + user);
    UserListManager userListManager = database.getUserListManager();

    Collection<UserList> listsForUser = userListManager.getListsForUser(user, true, false);
    int size = listsForUser.size();
    assertTrue("size is " + size, size >= 1);
    assertTrue("first is favorite", listsForUser.iterator().next().isFavorite());

    // add
    String test1 = "test";
    long listid = addListCheck(user, userListManager, test1);
    // what happens if add list a second time?
    long listid2 = addListCheck(user, userListManager, test1, false);

    // remove
    removeList(user, userListManager, listid, true);
    assertFalse(userListManager.listExists(listid));

    // remove again
    removeList(user, userListManager, listid, false);

  }

  // make sure we can remove lists that have been visited
  @Test
  public void testAddVisitor() {
    List<User> users = addAndGetUsers("test2");
    logger.debug("1 users size " + users.size());

    logger.debug("2 users size " + users.size());

    User user = users.iterator().next();
    UserListManager userListManager = database.getUserListManager();

    long listid = addListCheck(user.getId(), userListManager, "test");
    if (listid == -1) {
      UserList test1 = userListManager.getByName(user.getId(), "test");
      listid = test1.getUniqueID();
    }
    assertTrue(userListManager.getUserListsForText("", -1).contains(userListManager.getUserListByID(listid, new ArrayList<String>())));

    Iterator<UserList> iterator = userListManager.getListsForUser(user.getId(), false, false).iterator();
    UserList favorite = iterator.next();
    assertTrue(favorite.isFavorite());
    UserList test = iterator.next();
    assertFalse(test.isFavorite());
    assertTrue(test.getName().equals("test"));

    long visitor = getUser("visitor");
    int size = database.getUsers().size();
    assertTrue("size is " +size,size >= 2);

    Collection<UserList> listsForUser = userListManager.getListsForUser(visitor, false, false);
    assertTrue(!listsForUser.contains(test));  // haven't visited yet, shouldn't see it

    userListManager.addVisitor(test.getUniqueID(), visitor);
    listsForUser = userListManager.getListsForUser(visitor, false, false);
    assertTrue(listsForUser.contains(test));   // now that I visited, I should see it on my list
    Collection<UserList> listsForUser2 = userListManager.getListsForUser(visitor, true, false);
    assertTrue(!listsForUser2.contains(test)); // list isn't mine, I'm just a visitor

    removeList(user.getId(), userListManager, listid);

    // after removing, I shouldn't see it
    Collection<UserList> listsForUser3 = userListManager.getListsForUser(visitor, false, false);
    assertTrue(!listsForUser3.contains(test));
  }

  @Test
  public void testAddVisitor2() {
    List<User> users = database.getUsers();
    logger.debug("1 users size " + users.size());
    if (users.isEmpty()) {
      users = addAndGetUsers("test2");
    }

    logger.debug("2 users size " + users.size());

    User owner = users.iterator().next();
    UserListManager userListManager = database.getUserListManager();

    long listid = addListCheck(owner.getId(), userListManager, "test");
    UserList testList = userListManager.getUserListByID(listid, new ArrayList<String>());
    assertTrue(userListManager.getUserListsForText("", -1).contains(testList));

    long visitor = getUser("visitor");
    int size = database.getUsers().size();
    assertTrue("size was " +size,size == 2);

    userListManager.addVisitor(testList.getUniqueID(), visitor);

    // what happens if the owner adds himself as a visitor
    userListManager.addVisitor(testList.getUniqueID(), owner.getId());

    Collection<UserList> listsForUser = userListManager.getListsForUser(owner.getId(), false, false);
    assertTrue(listsForUser.contains(testList));   // should be able to see it, it's mine

    listsForUser = userListManager.getListsForUser(owner.getId(), true, false);
    assertTrue(listsForUser.contains(testList));  // should be able to see it, it's mine

    listsForUser = userListManager.getListsForUser(visitor, false, false);
    assertTrue(listsForUser.contains(testList));   // should be able to see it, it's mine

    listsForUser = userListManager.getListsForUser(visitor, true, false);
    assertTrue(!listsForUser.contains(testList));   // should be able to see it, it's mine

    removeList(owner.getId(), userListManager, listid);

    // after removing, I shouldn't see it
    listsForUser = userListManager.getListsForUser(visitor, false, false);
    assertTrue(!listsForUser.contains(testList));

    listsForUser = userListManager.getListsForUser(owner.getId(), false, false);
    assertTrue(!listsForUser.contains(testList));
  }

  @Test
  public void testAddExercise() {
    List<User> users = addAndGetUsers("test2");
    User owner = users.iterator().next();

    UserListManager userListManager = database.getUserListManager();

    long listid = addListCheck(owner.getId(), userListManager, "test");
    logger.debug("list id " +listid);
    UserList testList = userListManager.getUserListByID(listid, new ArrayList<String>());
    assertTrue(userListManager.getUserListsForText("", -1).contains(testList));

    UserExercise english = addAndDelete(owner, userListManager, listid, testList);

    // after delete, it should be gone
    testList = userListManager.getUserListByID(listid, new ArrayList<String>());
    assertTrue(!testList.getExercises().contains(english));

    userListManager.reallyCreateNewItem(listid, english, "");
    testList = userListManager.getUserListByID(listid, new ArrayList<String>());
    CommonUserExercise next = testList.getExercises().iterator().next();
    assertTrue(next.getEnglish().equals(ENGLISH));

    List<CommonExercise> exercises1 = database.getExercises();
    assertTrue(!exercises1.isEmpty());
    CommonExercise exercise = database.getCustomOrPredefExercise(next.getID());
    assertNotNull("huh? no exercise by id " + next.getID()+"?",exercise);

    String englishSentence = exercise.getEnglish();
    assertNotNull("huh? exercise " + exercise + " has no english?",englishSentence);
    assertTrue("english is " + englishSentence, englishSentence.equals(ENGLISH));
    assertTrue(exercise.getTooltip().equals(ENGLISH));

    next.toUserExercise().setEnglish(CHANGED);
    assertTrue(next.getEnglish().equals(CHANGED));
    assertTrue(!next.getTooltip().isEmpty());
    assertTrue(next.getTooltip().equals(CHANGED));

    userListManager.editItem(next.toUserExercise(),false, "");

    testList = userListManager.getUserListByID(listid, new ArrayList<String>());
    next = testList.getExercises().iterator().next();
    assertTrue(next.getEnglish().equals(CHANGED));
  }

  public UserExercise addAndDelete(User owner, UserListManager userListManager, long listid, UserList testList) {
    UserExercise english = addExercise(owner, userListManager, listid, testList);

    boolean b = userListManager.deleteItemFromList(listid, english.getID(), new ArrayList<String>());
    assertTrue(b);
    return english;
  }

  public UserExercise addExercise(User owner, UserListManager userListManager, long listid, UserList testList) {
    UserExercise english = createNewItem(owner.getId());
    assertTrue(!english.getTooltip().isEmpty());
    userListManager.reallyCreateNewItem(listid, english, "");

    // have to go back to database to get user list
    assertTrue(!testList.getExercises().contains(english));

    // OK, database should show it now
    testList = userListManager.getUserListByID(listid, new ArrayList<String>());
    Collection<CommonUserExercise> exercises = testList.getExercises();
    assertTrue(exercises.contains(english));
    // tooltip should never be empty
    for (CommonUserExercise ue : exercises) {
      //logger.debug("\t" + ue.getTooltip());
      assertTrue(!ue.getTooltip().isEmpty());
    }
    return english;
  }
  private int userExerciseCount = 0;

  private UserExercise createNewItem(long userid) {
    int uniqueID = userExerciseCount++;
    return new UserExercise(uniqueID, UserExercise.CUSTOM_PREFIX+uniqueID, userid, " ", "", "");
  }

  /**
   * TODO exercise other methods
   */
  @Test
  public void testReview() {
    UserListManager userListManager = database.getUserListManager();
    UserList reviewList = userListManager.getDefectList(new ArrayList<String>());
    //userListManager.getReviewedExercises();
    //UserExercise english = addExercise(owner, userListManager, listid, testList);
    //boolean b = userListManager.deleteItemFromList(listid, english.getID());
    // assertTrue(b);
    //    userListManager.markApproved();
  }

  private List<User> addAndGetUsers(String test2) {
    List<User> users;
    long l = getUser(test2);
    users = database.getUsers();
//    assertTrue(users.size() == 1);
    assertTrue(database.userExists(test2) == l);
    return users;
  }

  private long getUser(String test2) {
    long l = database.userExists(test2);
    if (l == -1) {
      l = database.addUser(89, "male", 1, "", ENGLISH, "boston", test2, new ArrayList<User.Permission>(), "browser");
    }
    return l;
  }

  private long addListCheck(long user, UserListManager userListManager, String name) {
    return addListCheck(user, userListManager, name, true);
  }

  private long addListCheck(long user, UserListManager userListManager, String name, boolean expectSuccess) {
    long listid = addList(user, userListManager, name);
    if (expectSuccess) {
      if (listid == -1) {
        assertTrue(userListManager.hasByName(user, name));
      } else {
        assertTrue("got list id " + listid, userListManager.listExists(listid));
        assertTrue(userListManager.hasByName(user, name));
        UserList userListByID = userListManager.getUserListByID(listid, new ArrayList<String>());
        assertNotNull(userListByID);
      }
    } else {
      assertTrue(listid == -1);
    }
    return listid;
  }

  private void removeList(long user, UserListManager userListManager, long listid) {
    removeList(user, userListManager, listid, true);
  }

  private void removeList(long user, UserListManager userListManager, long listid, boolean expectSuccess) {
    boolean b = userListManager.deleteList(listid);
    if (expectSuccess) {
      assertTrue(b);
    } else {
      assertFalse(b);
    }
    Collection<UserList> listsForUser2 = userListManager.getListsForUser(user, false, false);
    assertTrue(listsForUser2.size() == 1);
  }

  private long addList(long user, UserListManager userListManager, String name) {
    long listid = userListManager.addUserList(user, name, "", "", false);
    logger.debug("adding list " + name + " got " + listid);
    Collection<UserList> listsForUser1 = userListManager.getListsForUser(user, false, false);
    assertTrue(" size is " + listsForUser1.size(), listsForUser1.size() == 2);
    return listid;
  }
}
