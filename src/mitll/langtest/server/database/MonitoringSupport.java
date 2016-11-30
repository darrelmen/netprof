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

package mitll.langtest.server.database;

import mitll.langtest.server.database.result.IResultDAO;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.server.database.result.ResultDAO;
import mitll.langtest.server.database.result.SessionInfo;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.shared.UserAndTime;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.monitoring.Session;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/1/13
 * Time: 11:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class MonitoringSupport {
  private static final int MIN_DESIRED = 2;
  private static final int MAX_PEOPLE = 21;
  private static final Logger logger = LogManager.getLogger(MonitoringSupport.class);

  private static final int MINUTE_MILLIS = 60 * 1000;
  private static final float MINUTE_MILLIS_FLOAT = (float) MINUTE_MILLIS;

  private final IUserDAO userDAO;
  private final IResultDAO resultDAO;

  private MonitoringSupport() {
    this(null, null);
  }

  MonitoringSupport(IUserDAO userDAO, IResultDAO resultDAO) {
    this.userDAO = userDAO;
    this.resultDAO = resultDAO;
  }

  /**
   * TODO : worry about duplicate userid?
   *
   * @return map of user to number of answers the user entered
   */
  public Map<User, Integer> getUserToResultCount() {
    List<User> users = getUsers();

    Map<User, Integer> idToCount = new HashMap<User, Integer>();
    Map<Integer, User> idToUser = new HashMap<>();
    for (User u : users) {
      idToUser.put(u.getID(), u);
      idToCount.put(u, 0);
    }
    for (UserAndTime r : resultDAO.getUserAndTimes()) {
      User user = idToUser.get(r.getUserid());
      Integer c = idToCount.get(user);
      if (c != null) {
        idToCount.put(user, c + 1);
      }
    }
    return idToCount;
  }

/*  public Map<User, Integer> getUserToPractice() {
    List<User> users = getUsers();
    Map<User, Integer> idToCount = new HashMap<User, Integer>();
    Map<Long, User> idToUser = new HashMap<Long, User>();
    for (User u : users) {
      idToUser.put(u.getID(), u);
      idToCount.put(u, 0);
    }
    for (UserAndTime r : resultDAO.getUserAndTimes()) {
      User user = idToUser.get(r.getUserid());
      Integer c = idToCount.get(user);
      if (c != null) {
        idToCount.put(user, c + 1);
      }
    }
    return idToCount;
  }*/

  /**
   * Determine sessions per user.  If two consecutive items are more than {@link ResultDAO#SESSION_GAP} seconds
   * apart, then we've reached a session boundary.
   * Remove all sessions that have just one answer - must be test sessions.
   * <p>
   * Multiple answers to the same exercise count as one answer.
   *
   * @return list of duration and numAnswer pairs
   */
  public SessionInfo getSessions() {
    return resultDAO.getSessions();
  }

  private long getRateInMillis(Collection<Session> sessionCollection) {
    long totalTime = 0;
    long total = 0;
    for (Session s : sessionCollection) {
      totalTime += s.duration;
      total += s.getNumAnswers();
    }
    if (total == 0) return 0l;
    return totalTime / total;
  }

  /**
   * Given the observed rate of responses and the number of exercises to get
   * responses for, make a map of number of responses->hours
   * required to get that number of responses.
   * @param exercises
   * @return # responses->hours to get that number
   */
/*  public Map<Integer,Float> getHoursToCompletion(List<CommonExercise> exercises) {
    long totalTime = 0;
    long total = 0;
    for (Session s: getSessions()) {
      totalTime += s.duration;
      total += s.numAnswers;
    }
    if (total == 0) return new HashMap<Integer,Float>();

    long rateInMillis = totalTime / total;

    List<Integer> overall = getOverallResultCount(exercises);
    int totalAnswers = 0;
    for (Integer c : overall) totalAnswers += c;
    double numItems = (double) overall.size();
    double ratio = ((double) totalAnswers)/ numItems;
    double next = Math.ceil(ratio);
    double remainingForNext = (next-ratio)* numItems;
    double dRate = (double) rateInMillis;
    double millisForNext = dRate *remainingForNext;
    double hoursForNext = millisForNext/ HOUR_IN_MILLIS;

    Map<Integer,Float> estToItems = new HashMap<Integer, Float>();
    estToItems.put((int) next, (float) hoursForNext);
    double millisForFollowing = numItems * dRate;
    double hoursPerFollowing = millisForFollowing / HOUR_IN_MILLIS;
    for (double i = 1d; i < 10d; i += 1.0d) {
      estToItems.put((int) (next + i), ((float) (hoursForNext + (i*hoursPerFollowing))));
    }

    // logger.info("Est time to completion " +estToItems);
    return estToItems;
  }*/

  /**
   * TODO : worry about duplicate userid?
   *
   * @return
   */
  public Map<Integer, Integer> getResultCountToCount(Collection<CommonExercise> exercises) {
    Map<Integer, Integer> idToCount = getExToCount(exercises, resultDAO.getUserAndTimes());
    Map<Integer, Integer> resCountToCount = new HashMap<Integer, Integer>();

    for (int i = 0; i < 10; i++) {
      resCountToCount.put(i, 0);
    }

    for (Integer c : idToCount.values()) {
      Integer rc = resCountToCount.get(c);
      if (rc == null) resCountToCount.put(c, 1);
      else resCountToCount.put(c, rc + 1);
    }
    return resCountToCount;
  }

  /**
   * @return
   * @see #getResultPerExercise
   * @see #getResultCountToCount
   */
/*  private Map<String, Integer> getExToCount(Collection<CommonExercise> exercises) {
    Collection<UserAndTime> results = resultDAO.getUserAndTimes();
    return getExToCount(exercises, results);
  }*/
  private Map<Integer, Integer> getExToCount(Collection<CommonExercise> exercises, Collection<UserAndTime> results) {
    Map<Integer, Integer> idToCount = getInitialIdToCount(exercises);
    Map<Integer, Set<Integer>> keyToUsers = new HashMap<>();
    for (UserAndTime r : results) {
      //String key = r.getID();//getExerciseID() + "/" + r.getQid();
      int key = r.getExid();
      Set<Integer> usersForResult = keyToUsers.get(key);

      if (usersForResult == null) {
        keyToUsers.put(key, usersForResult = new HashSet<>());
      }
      if (!usersForResult.contains(r.getUserid())) {
        usersForResult.add(r.getUserid());
        Integer c = idToCount.get(key);
        if (c == null) {
          idToCount.put(key, 1);
        } else idToCount.put(key, c + 1);
      }
    }

    return idToCount;
  }

  /**
   * round->correct/incorrect->id->count
   *
   * @param exercises
   * @return
   */
/*  public Map<Integer, Map<String, Map<String,Integer>>> getGradeCountPerExercise(List<CommonExercise> exercises) {
    List<Result> results = getResults();
    Map<Integer, List<Grade>> idToGrade = gradeDAO.getIdToGrade();
    Map<Integer, Map<String, Map<String,Integer>>> roundToGradeToCount = new HashMap<Integer, Map<String, Map<String, Integer>>>();

    int total = 0;
    for (Result r : results) {
      String key = r.getOldID();//.getID() + "/" + r.getQid();

      List<Grade> grades = idToGrade.get(r.getUniqueID());
      if (grades == null) {
        //logger.warn("no grade for result " + key);
      }
      else {
        total += grades.size();
        for (Grade g : grades) {
          Map<String, Map<String, Integer>> gradeToCount = roundToGradeToCount.get(g.gradeIndex);
          if (gradeToCount == null)
            roundToGradeToCount.put(g.gradeIndex, gradeToCount = new HashMap<String, Map<String, Integer>>());

          if (g.grade > 0) {   // only valid grades
            String key1 = (g.grade < 4) ? "incorrect" : "correct";
            Map<String, Integer> resultToGrade = gradeToCount.get(key1);
            if (resultToGrade == null) gradeToCount.put(key1, resultToGrade = getInitialIdToCount(exercises));
            Integer c = resultToGrade.get(key);
           // logger.debug("key " +key + " val " + c);
            resultToGrade.put(key, (c == null) ? 1 : c + 1);
          }
        }
      }
    }
    logger.info("found " + total + " grades for " + results.size() + " results");
    return roundToGradeToCount;
  }*/

  /**
   * Get results for only male or only female users.
   *
   * @param isMale
   * @return
   */
  private Map<Integer, Integer> getExToCountMaleOrFemale(Collection<CommonExercise> exercises,
                                                         boolean isMale,
                                                        Collection<UserAndTime> results) {
    // List<Result> results = getResults();
    Map<Integer, Integer> idToCount = getInitialIdToCount(exercises);

    Map<Integer, User> userMap = userDAO.getUserMap(isMale);
    Map<Integer, Set<Integer>> keyToUsers = new HashMap<>();

    // logger.debug("results " + results.size() + ","+(isMale ? "male":"female") + " users num = " + userMap.size());
    SortedSet<Integer> resultKeys = new TreeSet<>();
    for (UserAndTime r : results) {
      if (userMap.containsKey(r.getUserid())) {   // filter for just results by males or females
        int key = r.getExid();
        resultKeys.add(key);
        Set<Integer> usersForResult = keyToUsers.get(key);

        if (usersForResult == null) {
          keyToUsers.put(key, usersForResult = new HashSet<>());
        }
        if (!usersForResult.contains(r.getUserid())) {
          usersForResult.add(r.getUserid());
          Integer c = idToCount.get(key);
          int value = (c == null) ? 1 : c + 1;
          idToCount.put(key, value);
        }
      }
    }
    // logger.debug("keyToUsers " + keyToUsers.keySet().size() + " results : " +keyToUsers);
    //logger.debug("idToCount size = " + idToCount.size() + " resultKeys " +idToCount.keySet());
    //logger.debug("result id resultKeys = " + resultKeys);

    // post condition -- the result exercise foreign keys should be a subset of the exercise keys (exid/qid)
    if (resultKeys.size() > idToCount.keySet().size()) {
      logger.error("huh? there are " + resultKeys.size() +
          " result keys but only " + idToCount.keySet().size() + " exercise keys.");
    } else {
      /*boolean b =*/
      resultKeys.removeAll(idToCount.keySet());
      if (!resultKeys.isEmpty()) {
        logger.error("some result keys are not in the exercise keys " + resultKeys);
      }
    }

    return idToCount;
  }

  /**
   * Make a map of exerciseid->0
   *
   * @param exercises
   * @return
   */
  private Map<Integer, Integer> getInitialIdToCount(Collection<CommonExercise> exercises) {
    Map<Integer, Integer> idToCount = new HashMap<>();
    for (CommonExercise e : exercises) {
      //if (e.getNumQuestions() == 0) {
     // String key = e.getOldID() + "/0";
      idToCount.put(e.getID(), 0);
/*      }
      else {
        for (int i = 1; i < e.getNumQuestions()+1; i++) {   // for some reason we start from 1!
          String key = e.getOldID() + "/" + i;
          idToCount.put(key,0);
        }
      }*/
    }
    return idToCount;
  }

  /**
   * Sort first by exercise id then by question within the exercise
   */
  private static class CompoundKey implements Comparable<CompoundKey> {
    public final int first, second;

    public CompoundKey(int first, int second) {
      this.first = first;
      this.second = second;
    }

    @Override
    public boolean equals(Object obj) {
      CompoundKey other = (CompoundKey) obj;
      return compareTo(other) == 0;
    }

    @Override
    public int compareTo(CompoundKey o) {
      return first < o.first ? -1 : first > o.first ? +1 : second < o.second ? -1 : second > o.second ? +1 : 0;
    }

    public String toString() {
      return first + "/" + second;
    }
  }

  /**
   * Get counts of answers by date
   * TODO : worry about duplicate userid?
   *
   * @return
   */
  public Map<String, Integer> getResultByDay() {
    SimpleDateFormat df = new SimpleDateFormat("MM-dd-yy");
    Map<String, Integer> dayToCount = new HashMap<String, Integer>();
    for (UserAndTime r : resultDAO.getUserAndTimes()) {
      Date date = new Date(r.getTimestamp());
      String day = df.format(date);
      Integer c = dayToCount.get(day);
      if (c == null) {
        dayToCount.put(day, 1);
      } else dayToCount.put(day, c + 1);
    }
    return dayToCount;
  }

  /**
   * get counts of answers by hours of the day
   *
   * @return
   */
  public Map<String, Integer> getResultByHourOfDay() {
    SimpleDateFormat df = new SimpleDateFormat("HH");
    Map<String, Integer> dayToCount = new HashMap<String, Integer>();
    for (UserAndTime r : resultDAO.getUserAndTimes()) {
      Date date = new Date(r.getTimestamp());
      String day = df.format(date);
      Integer c = dayToCount.get(day);
      if (c == null) {
        dayToCount.put(day, 1);
      } else dayToCount.put(day, c + 1);
    }
    return dayToCount;
  }

  /**
   * Split exid->count by gender.
   *
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResultPerExercise
   */
  public Map<String, Map<Integer, Integer>> getResultPerExercise(Collection<CommonExercise> exercises) {
    Map<String, Map<Integer, Integer>> typeToList = new HashMap<>();
    Collection<UserAndTime> results = resultDAO.getUserAndTimes();

    typeToList.put("overall", getExToCount(exercises, results));
    typeToList.put("male", getExToCountMaleOrFemale(exercises, true, results));
    typeToList.put("female", getExToCountMaleOrFemale(exercises, false, results));

    return typeToList;
  }

  /**
   * @param exercises
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getResultCountsByGender()
   */
  public Map<String, Map<Integer, Integer>> getResultCountsByGender(Collection<CommonExercise> exercises) {
    //logger.debug("Examining " +exercises.size() + " exercises...");
    Map<String, Map<Integer, Integer>> typeToNumAnswerToCount = new HashMap<String, Map<Integer, Integer>>();

    Collection<UserAndTime> userAndTimes = resultDAO.getUserAndTimes();
    Map<Integer, Integer> exToCountMaleOrFemale = getExToCountMaleOrFemale(exercises, true, userAndTimes);
    //logger.debug("male map " +exToCountMaleOrFemale);

    List<Integer> male = getCountArray(exToCountMaleOrFemale);
    List<Integer> female = getCountArray(getExToCountMaleOrFemale(exercises, false, userAndTimes));

    //logger.debug("num male " +male.size() + " : " + male + " female " +female.size());

    Map<Integer, Integer> maleAnswerToCount = new HashMap<Integer, Integer>();
    Map<Integer, Integer> femaleAnswerToCount = new HashMap<Integer, Integer>();

    for (int i = 0; i < 10; i++) {   // make sure we have zero entries for 0-9
      maleAnswerToCount.put(i, 0);
      femaleAnswerToCount.put(i, 0);
    }

    for (Integer countAtExercise : male) {
      Integer count = maleAnswerToCount.get(countAtExercise);
      int currentValue = (count == null) ? 1 : count + 1;
      maleAnswerToCount.put(countAtExercise, currentValue);
    }

    for (Integer countAtExercise : female) {
      Integer count = femaleAnswerToCount.get(countAtExercise);
      if (count == null) femaleAnswerToCount.put(countAtExercise, 1);
      else femaleAnswerToCount.put(countAtExercise, count + 1);
    }

    logger.debug("getResultCountsByGender : male " + maleAnswerToCount);
    //logger.debug("getResultCountsByGender : female " + femaleAnswerToCount);

    typeToNumAnswerToCount.put("maleCount", maleAnswerToCount);
    typeToNumAnswerToCount.put("femaleCount", femaleAnswerToCount);
    return typeToNumAnswerToCount;
  }

  public Map<String, Map<Integer, Map<Integer, Integer>>> getDesiredCounts(Collection<CommonExercise> exercises,
                                                                           Collection<UserAndTime> userAndTimes) {
    Map<String, Map<Integer, Map<Integer, Integer>>> typeToNumAnswerToCount = new HashMap<String, Map<Integer, Map<Integer, Integer>>>();

    List<Integer> male = getCountArray(getExToCountMaleOrFemale(exercises, true, userAndTimes));
    List<Integer> female = getCountArray(getExToCountMaleOrFemale(exercises, false, userAndTimes));

    Map<Integer, Integer> maleAnswerToCount = new HashMap<Integer, Integer>();
    Map<Integer, Integer> femaleAnswerToCount = new HashMap<Integer, Integer>();

    for (int i = 0; i < 10; i++) {
      maleAnswerToCount.put(i, 0);
      femaleAnswerToCount.put(i, 0);
    }

    for (Integer countAtExercise : male) {
      Integer count = maleAnswerToCount.get(countAtExercise);
      if (count == null) maleAnswerToCount.put(countAtExercise, 1);
      else maleAnswerToCount.put(countAtExercise, count + 1);
    }

    for (Integer countAtExercise : female) {
      Integer count = femaleAnswerToCount.get(countAtExercise);
      if (count == null) femaleAnswerToCount.put(countAtExercise, 1);
      else femaleAnswerToCount.put(countAtExercise, count + 1);
    }

    Map<Integer, Map<Integer, Integer>> maleDesiredToPeopleToNumPer = getResourceCounts(maleAnswerToCount);
    Map<Integer, Map<Integer, Integer>> femaleDesiredToPeopleToNumPer = getResourceCounts(femaleAnswerToCount);
    typeToNumAnswerToCount.put("desiredToMale", maleDesiredToPeopleToNumPer);
    typeToNumAnswerToCount.put("desiredToFemale", femaleDesiredToPeopleToNumPer);
    //  logger.info("got " + maleDesiredToPeopleToNumPer);
    long rateInMillis = getRateInMillis(getSessions().getSessions());// logger.info("total at");
    float rateInHours = ((float) rateInMillis) / MINUTE_MILLIS_FLOAT;

    Map<Integer, Map<Integer, Integer>> maleDesiredToPeopleToHours = getResourceCounts(maleAnswerToCount, rateInHours);
    Map<Integer, Map<Integer, Integer>> femaleDesiredToPeopleToHours = getResourceCounts(femaleAnswerToCount, rateInHours);
    typeToNumAnswerToCount.put("desiredToMaleHours", maleDesiredToPeopleToHours);
    typeToNumAnswerToCount.put("desiredToFemaleHours", femaleDesiredToPeopleToHours);

    return typeToNumAnswerToCount;
  }

  private Map<Integer, Map<Integer, Integer>> getResourceCounts(Map<Integer, Integer> maleAnswerToCount) {
    return getResourceCounts(maleAnswerToCount, 1f);
  }

  private Map<Integer, Map<Integer, Integer>> getResourceCounts(Map<Integer, Integer> maleAnswerToCount, float rateInMinutes) {
    //int minPeople = 1;
    int maxPeople = MAX_PEOPLE;
    int maxDesired = 7;
    Map<Integer, Map<Integer, Integer>> desiredToNumPeopleToPerPerson = new HashMap<Integer, Map<Integer, Integer>>();
    //Map<Integer,Integer> numDesiredToTotal = new HashMap<Integer, Integer>();
    for (int numDesiredPer = MIN_DESIRED; numDesiredPer < maxDesired; numDesiredPer++) {
      int total = 0;
      Map<Integer, Integer> peopleToPerPerson = new HashMap<Integer, Integer>();
      desiredToNumPeopleToPerPerson.put(numDesiredPer, peopleToPerPerson);

      int numAnswersStart = -1;
      for (int numAnswers = 0; numAnswers < numDesiredPer; numAnswers++) {
        Integer count = maleAnswerToCount.get(numAnswers);
        if (count != null) {
          if (count > 0 && numAnswersStart == -1) {
            //System.out.println("there were " + count + " at " +numAnswers);
            numAnswersStart = numAnswers;
          }
          int numPer = (numDesiredPer - numAnswers) * count;
          total += numPer;
        }
      }
      total = Math.round((float) total * rateInMinutes);

      int peopleStart = Math.max(1, numDesiredPer - numAnswersStart);

      //System.out.println("num answer start " + numAnswersStart + " people start " +peopleStart);
      for (int people = peopleStart; people < maxPeople; people++) {
        float numPerPerson = (float) total / (float) people;
      /*  for (int numAnswers = 0; numAnswers < numDesiredPer; numAnswers++) {
          Integer count = maleAnswerToCount.get(numAnswers);
          int numPer = (numDesiredPer-numAnswers) * count;
         // total += numPer;
          numPerPerson += (float)numPer / (float)people;
        }*/
        int itemsOrMinutes = Math.round(numPerPerson);
        peopleToPerPerson.put(people, itemsOrMinutes);
        if (rateInMinutes != 1f && itemsOrMinutes == 1 || itemsOrMinutes == 0) break;
      }
      //numDesiredToTotal.put(numDesiredPer,total);
    }
    //System.out.println("total " + numDesiredToTotal);
    //  System.out.println("desired to people " + desiredToNumPeopleToPerPerson);

    return desiredToNumPeopleToPerPerson;
  }

  /**
   * Get list of counts of answers for each exercise, in order.
   * If the exercise ids are numbers, sort them as numbers, not as strings.
   * We don't want 11 to be between 109 and 110.
   *
   * @param exToCount
   * @return list of counts of answers per exercise
   */
  private List<Integer> getCountArray(Map<Integer, Integer> exToCount) {
    List<Integer> countArray = new ArrayList<Integer>(exToCount.size());
    //   String next = exToCount.keySet().iterator().next();
    boolean isInt = true;

    // NOTE : no more strings for exids
    /*for (String id : exToCount.keySet()) {
      try {
        String[] split = id.split("/");
        String left = split[0];
        Integer.parseInt(left);
      } catch (NumberFormatException e) {
        isInt = false;
        break;
      }
    }*/
  /*  try {
      String[] split = next.split("/");
      String left = split[0];
      Integer.parseInt(left);
      isInt = true;
    } catch (NumberFormatException e) {
      //logger.debug("Couldn't parse " + left);
    }*/
   // if (isInt) {
      Map<CompoundKey, Integer> keyToCount = new TreeMap<CompoundKey, Integer>();
      for (Map.Entry<Integer, Integer> pair : exToCount.entrySet()) {
        try {
//          String[] split = pair.getKey().split("/");
//          String left = split[0];
//          int exid = Integer.parseInt(left);
//          int qid = 0;
//          if (split.length > 1) {
//            String right = split[1];
//            qid = Integer.parseInt(right);
//          }
          keyToCount.put(new CompoundKey(pair.getKey(), 0), pair.getValue());
        } catch (Exception e) {
          logger.error("Got " + e, e);
        }
      }
      countArray.addAll(keyToCount.values());
      return countArray;
//    } else {
//      for (Map.Entry<String, Integer> pair : exToCount.entrySet()) {
//        countArray.add(pair.getValue());
//      }
//      return countArray;
//    }
  }

  private static final int MIN = (60 * 1000);
  private static final int HOUR = (60 * MIN);

  /**
   * Return some statistics related to the hours of audio that have been collected
   *
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getResultStats()
   */
  public Map<String, Number> getResultStats() {
    double total = 0;
    int count = 0;
    int badDur = 0;
    int maxWarns = 0;
    for (Result r : resultDAO.getResults()) {
      total += r.getDurationInMillis();
      if (r.getDurationInMillis() > 0) {
        count++;
      } else if (true
        //r.spoken /*|| r.audioType.equals(Result.UNSET)*/
          ) {
        if (r.getAnswer().endsWith(".wav")) {
          badDur++;
          if (maxWarns++ < 10) {
            logger.info("possible bad audio result " + r + " path " + r.getAnswer());
          }
        }
      }
    }

/*
    if (maxWarns > 0 && maxWarns < results.size()) {
      logger.warn("got " + maxWarns + " bad audio recordings out of " + results.size());
    }
*/

    Map<String, Number> typeToStat = new HashMap<String, Number>();
/*    List<User> users = getUsers();

    Map<Long, User> idToUser = new HashMap<Long, User>();
    for (User u : users) {
      idToUser.put(u.getID(), u);
    }*/

/*
    Map<Integer, Integer> resultIDToExp = new HashMap<Integer, Integer>();
    Set<Long> unknownUsers = new HashSet<Long>();
    for (UserAndTime r : resultDAO.getUserAndTimes()) {
      User user = idToUser.get(r.getUserid());
      if (user == null) {
        unknownUsers.add(r.getUserid());
        //System.err.println("unknown user " + r.userid);
      } else resultIDToExp.put(r.getUniqueID(), user.getExperience());
    }

    logger.warn("getResultStats : found " + unknownUsers.size() + " unknown users : " + unknownUsers);
*/

/*    Collection<Grade> grades1 = gradeDAO.getGrades();
    for (int i = 0; i < 3; i++) {
      GradeInfo gradeInfo = new GradeInfo(grades1, resultIDToExp, i);
      if (gradeInfo.gradeCount > 0) {
        typeToStat.put("totalGraded_" +i, gradeInfo.gradeCount);
        typeToStat.put("validGraded_" +i, gradeInfo.valid);
        typeToStat.put("averageNumGraded_" +i, gradeInfo.avgNumGrades);

        typeToStat.put("avgGrade_" +i, gradeInfo.avgGrade);
        typeToStat.put("incorrectGraded_" +i, gradeInfo.incorrect);
        typeToStat.put("averageNumIncorrect_" +i, (float)gradeInfo.incorrect/(float)gradeInfo.numExercises);
        typeToStat.put("correctGraded_" +i, gradeInfo.correct);
        typeToStat.put("averageNumCorrect_" +i, (float)gradeInfo.correct/(float)gradeInfo.numExercises);
        typeToStat.put("percentGraded_" +i, (int)(100f*(float)gradeInfo.gradeCount/(float)results.size()));

        for (Map.Entry<Integer,Integer> expToInc : gradeInfo.expToIncorrect.entrySet()) {
          String suffix = "_at_" + expToInc.getKey();
          typeToStat.put("incorrectGraded_" + i + suffix, expToInc.getValue());
          typeToStat.put("averageNumIncorrect_" +i+suffix, (float)expToInc.getValue()/(float)gradeInfo.numExercises);
        }

        for (Map.Entry<Integer,Integer> expToCorr : gradeInfo.expToCorrect.entrySet()) {
          String suffix = "_at_" + expToCorr.getKey();
          typeToStat.put("correctGraded_" + i + suffix, expToCorr.getValue());
          typeToStat.put("averageNumCorrect_" +i+suffix, (float)expToCorr.getValue()/(float)gradeInfo.numExercises);
        }
      }
    }*/

    Number aDouble = total / ((double) HOUR);
    typeToStat.put("totalHrs", aDouble);
    double value = count > 0 ? total / ((double) count) : 0;
    typeToStat.put("avgSecs", value / 1000);
    typeToStat.put("totalAudioAnswers", count);
    typeToStat.put("badRecordings", badDur);


    logger.debug("audio dur stats " + typeToStat);
    return typeToStat;
  }

/*  private static class GradeInfo {
    int gradeCount = 0;
    int valid = 0;
    int incorrect = 0;
    int correct = 0;
    int numExercises = 0;
    int gradeTotal = 0;  final float avgGrade; final float avgNumGrades;
    final Map<Integer,Integer> expToIncorrect = new HashMap<Integer, Integer>();
    final Map<Integer,Integer> expToCorrect = new HashMap<Integer, Integer>();

    */

  /**
   * @paramx gradeDAO
   * @paramx idToUser
   * @paramx index
   *//*
    public GradeInfo( Collection<Grade> grades, Map<Integer,Integer> resultToExp, int index) {
      Set<String> exids = new HashSet<String>();
      for (Grade g : grades) {
        if (g.gradeIndex == index) {  // for this grade round (1st, 2nd, etc)
          if (g.grade > 0) {    // all valid (non-skip) grades
            exids.add(g.exerciseID);
            gradeTotal += g.grade;
            valid++;

            if (g.grade < 4) {
              incorrect++;

              int exp = resultToExp.containsKey(g.resultID) ? resultToExp.get(g.resultID) : -1;
              Integer incorrectAtExp = expToIncorrect.get(exp);
              expToIncorrect.put(exp,(incorrectAtExp == null) ? 1 : incorrectAtExp+1);
            }
            else {
              correct++;

              int exp = resultToExp.containsKey(g.resultID) ? resultToExp.get(g.resultID) : -1;
              Integer correctAtExp = expToCorrect.get(exp);
              expToCorrect.put(exp,(correctAtExp == null) ? 1 : correctAtExp+1);
            }
          }
          gradeCount++;
        }
      }
      numExercises = exids.size();
      if (numExercises == 0) numExercises = 1; //avoid NaN
      avgGrade = (float) gradeTotal / (float) valid;
      avgNumGrades = (float) valid / (float) numExercises;
    }*/

/*    private int getExp(Map<Long, User> idToUser, Grade g) {
      User key = idToUser.get((long)g.grader);
      int exp = -1;
      if (key != null) exp = key.experience;
      return exp;
    }
  }*/
  private List<User> getUsers() {
    return userDAO.getUsers();
  }


  /**
   * Pulls the list of results out of the database.
   *
   * @return
   * @see #getExToCount
   */
/*  private List<Result> getResults() {
    return resultDAO.getResults();
  }*/
}
