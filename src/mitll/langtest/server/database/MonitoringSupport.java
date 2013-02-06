package mitll.langtest.server.database;

import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.Session;
import mitll.langtest.shared.User;
import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 2/1/13
 * Time: 11:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class MonitoringSupport {
  private static final int MIN_DESIRED = 2;
  private static Logger logger = Logger.getLogger(MonitoringSupport.class);

  //private static final int KB = (1024);
  //private static final int MB = (KB * KB);
  private static final int SESSION_GAP = 10 * 60 * 1000;
  private static final int MINUTE_MILLIS = 60 * 1000;
  private static final int HOUR_IN_MILLIS = (60 * MINUTE_MILLIS);
  //private static final float HOUR_IN_MILLIS_FLOAT = (float)HOUR_IN_MILLIS;
  private static final float MINUTE_MILLIS_FLOAT = (float)MINUTE_MILLIS;

  private final UserDAO userDAO;
  private final ResultDAO resultDAO;
  private String outsideFile;

  public MonitoringSupport() {
    this(null,null);
  }

  public MonitoringSupport(UserDAO userDAO, ResultDAO resultDAO) {
    this.userDAO = userDAO;
    this.resultDAO = resultDAO;
  }

  public void setOutsideFile(String outsideFile) { this.outsideFile = outsideFile; }

  /**
   * TODO : worry about duplicate userid?
   * @return map of user to number of answers the user entered
   */
  public Map<User, Integer> getUserToResultCount() {
    List<User> users = getUsers();
    List<Result> results = getResults();

    Map<User,Integer> idToCount = new HashMap<User, Integer>();
    Map<Long,User> idToUser = new HashMap<Long, User>();
    for (User u : users) {
      idToUser.put(u.id,u);
      idToCount.put(u,0);
    }
    for (Result r : results) {
      User user = idToUser.get(r.userid);
      Integer c = idToCount.get(user);
      if (c != null) {
        idToCount.put(user, c + 1);
      }
    }
    return idToCount;
  }

  /**
   * Determine sessions per user.  If two consecutive items are more than {@link #SESSION_GAP} seconds
   * apart, then we've reached a session boundary.
   * Remove all sessions that have just one answer - must be test sessions.
   * @return list of duration and numAnswer pairs
   */
  public List<Session> getSessions() {
    List<Result> results = getResults();

    Map<Long,List<Result>> userToAnswers = new HashMap<Long, List<Result>>();
    for (Result r : results) {
      List<Result> results1 = userToAnswers.get(r.userid);
      if (results1 == null) userToAnswers.put(r.userid, results1 = new ArrayList<Result>());
      results1.add(r);
    }
    List<Session> sessions = new ArrayList<Session>();
    for (List<Result> resultList : userToAnswers.values()) {
      Collections.sort(resultList, new Comparator<Result>() {
        @Override
        public int compare(Result o1, Result o2) {
          return o1.timestamp < o2.timestamp ? -1 : o1.timestamp > o2.timestamp ? +1 : 0;
        }
      });
      Session s = null;
      long last = 0;
      for (Result r : resultList) {
        if (s == null || r.timestamp - last > SESSION_GAP) {
          s = new Session();
          sessions.add(s);
        } else {
          s.duration += r.timestamp - last;
        }
        s.numAnswers++;
        last = r.timestamp;
      }
    }
    Iterator<Session> iter = sessions.iterator();
    while(iter.hasNext()) if (iter.next().numAnswers < 2) iter.remove();
    return sessions;
  }

  private long getRateInMillis(Collection<Session> sessionCollection) {
    long totalTime = 0;
    long total = 0;
    for (Session s: sessionCollection) {
        totalTime += s.duration;
        total += s.numAnswers;
    }
    return totalTime/total;
  }

  /**
   * Given the observed rate of responses and the number of exercises to get
   * responses for, make a map of number of responses->hours
   * required to get that number of responses.
   * @param exercises
   * @return # responses->hours to get that number
   */
  public Map<Integer,Float> getHoursToCompletion(List<Exercise> exercises) {
    long totalTime = 0;
    long total = 0;
    for (Session s: getSessions()) {
      totalTime += s.duration;
      total += s.numAnswers;
    }
    if (total == 0) return new HashMap<Integer,Float>();

    long rateInMillis = totalTime / total;

    Map<String, Integer> exToCount = getExToCount(exercises);



    List<Integer> overall = getCountArray(exToCount);
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
  }

  /**
   * TODO : worry about duplicate userid?
   * @return
   */
  public Map<Integer, Integer> getResultCountToCount(List<Exercise> exercises) {
    Map<String, Integer> idToCount = getExToCount(exercises);
    Map<Integer,Integer> resCountToCount = new HashMap<Integer, Integer>();

    for (int i =0; i< 10; i++) {
      resCountToCount.put(i,0);
    }

    for (Integer c : idToCount.values()) {
      Integer rc = resCountToCount.get(c);
      if (rc == null) resCountToCount.put(c, 1);
      else resCountToCount.put(c, rc + 1);
    }
    return resCountToCount;
  }

  /**
   * @see #getHoursToCompletion
   * @see #getResultCountToCount
   * @see #getResultPerExercise
   * @return
   */
  private Map<String, Integer> getExToCount(List<Exercise> exercises) {
    Map<String, Integer> idToCount = getInitialIdToCount(exercises);
    Map<String, Set<Long>> keyToUsers = new HashMap<String, Set<Long>>();
    List<Result> results = getResults();
    for (Result r : results) {
      String key = r.id + "/" + r.qid;
      Set<Long> usersForResult = keyToUsers.get(key);

      if (usersForResult == null) {
        keyToUsers.put(key, usersForResult = new HashSet<Long>());
      }
      if (!usersForResult.contains(r.userid)) {
        usersForResult.add(r.userid);
        Integer c = idToCount.get(key);
        if (c == null) {
          idToCount.put(key, 1);
        } else idToCount.put(key, c + 1);
      }
    }

    if (outsideFile != null) {
      Map<String, Integer> idToCountOutsideMale = new OutsideCount().getExerciseIDToOutsideCount( true, outsideFile,exercises);
      Map<String, Integer> idToCountOutsideFemale = new OutsideCount().getExerciseIDToOutsideCount( false, outsideFile,exercises);

      //  logger.info("map of outside counts is size = " + idToCountOutside.size() +" " + idToCountOutside.values().size());
      for (Map.Entry<String, Integer> pair : idToCountOutsideMale.entrySet()) {
        String key = pair.getKey() + "/0";
        Integer count = idToCount.get(key);
        if (count == null) logger.warn("missing exercise id " + key);
        else idToCount.put(key,count+pair.getValue());
      }
      for (Map.Entry<String, Integer> pair : idToCountOutsideFemale.entrySet()) {
        String key = pair.getKey() + "/0";
        Integer count = idToCount.get(key);
        if (count == null) logger.warn("missing exercise id " + key);
        else idToCount.put(key,count+pair.getValue());
      }
    }
    return idToCount;
  }

  /**
   * Get results for only male or only female users.
   *
   * @param isMale
   * @return
   */
  private Map<String, Integer> getExToCountMaleOrFemale(List<Exercise> exercises, boolean isMale) {
    Map<String, Integer> idToCount = getInitialIdToCount(exercises);

    Map<Long, User> userMap = userDAO.getUserMap(isMale);
    Map<String, Set<Long>> keyToUsers = new HashMap<String, Set<Long>>();

    List<Result> results = getResults();
    for (Result r : results) {
      if (userMap.containsKey(r.userid)) {
        String key = r.id + "/" + r.qid;
        Set<Long> usersForResult = keyToUsers.get(key);

        if (usersForResult == null) {
          keyToUsers.put(key, usersForResult = new HashSet<Long>());
        }
        if (!usersForResult.contains(r.userid)) {
          usersForResult.add(r.userid);
          Integer c = idToCount.get(key);
          if (c == null) {
            idToCount.put(key, 1);
          } else idToCount.put(key, c + 1);
        }
      }
    }

    //  int total = 0;
    Map<String, Integer> idToCountOutsideMale = new OutsideCount().getExerciseIDToOutsideCount( isMale, outsideFile,exercises);
    for (Map.Entry<String, Integer> pair : idToCountOutsideMale.entrySet()) {
      String key = pair.getKey() + "/0";
      Integer count = idToCount.get(key);
      if (count == null) logger.warn("missing exercise id " + key);
      else {
        int value = count + pair.getValue();
        idToCount.put(key, value);
      }
    }

    // for (Integer counts : idToCount.values()) total += counts;
    // logger.info("ismale " +isMale + " total " +total);

    return idToCount;
  }

  /**
   * Make a map of exerciseid->0
   * @param exercises
   * @return
   */
  private Map<String, Integer> getInitialIdToCount( List<Exercise> exercises) {
//    List<Exercise> exercises = getExercises(useFile);
    Map<String,Integer> idToCount = new HashMap<String, Integer>();
    for (Exercise e : exercises) {
      if (e.getNumQuestions() == 0) {
        String key = e.getID() + "/0";
        idToCount.put(key,0);
      }
      else {
        for (int i = 0; i < e.getNumQuestions(); i++) {
          String key = e.getID() + "/" + i;
          idToCount.put(key,0);
        }
      }
    }
    return idToCount;
  }

  /**
   * Sort first by exercise id then by question within the exercise
   */
  private static class CompoundKey implements Comparable<CompoundKey> {
    public final int first,second;

    public CompoundKey(int first, int second) {
      this.first = first;
      this.second = second;
    }

    @Override
    public boolean equals(Object obj) {
      CompoundKey other =(CompoundKey) obj;
      return compareTo(other) == 0;
    }

    @Override
    public int compareTo(CompoundKey o) {
      return first < o.first ? -1 : first > o.first ? +1 : second < o.second ? -1 : second > o.second ? +1 : 0;
    }

    public String toString() { return first +"/"+second; }
  }

  /**
   * Get counts of answers by date
   * TODO : worry about duplicate userid?
   * @return
   */
  public Map<String, Integer> getResultByDay() {
    List<Result> results = getResults();
    SimpleDateFormat df = new SimpleDateFormat("MM-dd-yy");
    Map<String,Integer> dayToCount = new HashMap<String, Integer>();
    for (Result r : results) {
      Date date = new Date(r.timestamp);
      String day = df.format(date);
      Integer c = dayToCount.get(day);
      if (c == null) {
        dayToCount.put(day, 1);
      }
      else dayToCount.put(day, c + 1);
    }
    return dayToCount;
  }

  /**
   * get counts of answers by hours of the day
   * @return
   */
  public Map<String, Integer> getResultByHourOfDay() {
    List<Result> results = getResults();
    SimpleDateFormat df = new SimpleDateFormat("HH");
    Map<String,Integer> dayToCount = new HashMap<String, Integer>();
    for (Result r : results) {
      Date date = new Date(r.timestamp);
      String day = df.format(date);
      Integer c = dayToCount.get(day);
      if (c == null) {
        dayToCount.put(day, 1);
      }
      else dayToCount.put(day, c + 1);
    }
    return dayToCount;
  }

  /**
   * Split exid->count by gender.
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResultPerExercise(boolean)
   * @return
   */
  public Map<String,List<Integer>> getResultPerExercise(List<Exercise> exercises) {
    Map<String, Integer> exToCount = getExToCount(exercises);
    List<Integer> overall = getCountArray(exToCount);
    List<Integer> male = getCountArray(getExToCountMaleOrFemale(exercises,true));
    List<Integer> female = getCountArray(getExToCountMaleOrFemale(exercises,false));


    Map<String,List<Integer>> typeToList = new HashMap<String, List<Integer>>();
    typeToList.put("overall",overall);
    typeToList.put("male",male);
    typeToList.put("female",female);

    return typeToList;
  }

  public Map<String,Map<Integer,Integer>> getResultCountsByGender(List<Exercise> exercises) {
    Map<String,Map<Integer,Integer>> typeToNumAnswerToCount = new HashMap<String, Map<Integer, Integer>>();

    List<Integer> male = getCountArray(getExToCountMaleOrFemale(exercises,true));
    List<Integer> female = getCountArray(getExToCountMaleOrFemale(exercises,false));

    Map<Integer,Integer> maleAnswerToCount = new HashMap<Integer,Integer>();
    Map<Integer,Integer> femaleAnswerToCount = new HashMap<Integer,Integer>();

    for (int i =0; i< 10; i++) {
      maleAnswerToCount.put(i,0);
      femaleAnswerToCount.put(i,0);
    }

    for (Integer countAtExercise : male) {
      Integer count = maleAnswerToCount.get(countAtExercise);
      if (count == null) maleAnswerToCount.put(countAtExercise,1);
      else maleAnswerToCount.put(countAtExercise,count+1);
    }

    for (Integer countAtExercise : female) {
      Integer count = femaleAnswerToCount.get(countAtExercise);
      if (count == null) femaleAnswerToCount.put(countAtExercise,1);
      else femaleAnswerToCount.put(countAtExercise,count+1);
    }

    typeToNumAnswerToCount.put("maleCount",maleAnswerToCount);
    typeToNumAnswerToCount.put("femaleCount",femaleAnswerToCount);
    return typeToNumAnswerToCount;
  }

  public Map<String,Map<Integer, Map<Integer, Integer>>> getDesiredCounts(List<Exercise> exercises) {
    Map<String,Map<Integer, Map<Integer, Integer>>> typeToNumAnswerToCount = new HashMap<String, Map<Integer, Map<Integer, Integer>>>();

    List<Integer> male = getCountArray(getExToCountMaleOrFemale(exercises,true));
    List<Integer> female = getCountArray(getExToCountMaleOrFemale(exercises,false));

    Map<Integer,Integer> maleAnswerToCount = new HashMap<Integer,Integer>();
    Map<Integer,Integer> femaleAnswerToCount = new HashMap<Integer,Integer>();

    for (int i =0; i< 10; i++) {
      maleAnswerToCount.put(i,0);
      femaleAnswerToCount.put(i,0);
    }

    for (Integer countAtExercise : male) {
      Integer count = maleAnswerToCount.get(countAtExercise);
      if (count == null) maleAnswerToCount.put(countAtExercise,1);
      else maleAnswerToCount.put(countAtExercise,count+1);
    }

    for (Integer countAtExercise : female) {
      Integer count = femaleAnswerToCount.get(countAtExercise);
      if (count == null) femaleAnswerToCount.put(countAtExercise,1);
      else femaleAnswerToCount.put(countAtExercise,count+1);
    }

    Map<Integer, Map<Integer, Integer>> maleDesiredToPeopleToNumPer   = getResourceCounts(maleAnswerToCount);
    Map<Integer, Map<Integer, Integer>> femaleDesiredToPeopleToNumPer = getResourceCounts(femaleAnswerToCount);
    typeToNumAnswerToCount.put("desiredToMale",maleDesiredToPeopleToNumPer);
    typeToNumAnswerToCount.put("desiredToFemale",femaleDesiredToPeopleToNumPer);
  //  logger.info("got " + maleDesiredToPeopleToNumPer);
    long rateInMillis = getRateInMillis(getSessions());// logger.info("total at");
    float rateInHours =((float)rateInMillis)/MINUTE_MILLIS_FLOAT;

    Map<Integer, Map<Integer, Integer>> maleDesiredToPeopleToHours   = getResourceCounts(maleAnswerToCount,rateInHours);
    Map<Integer, Map<Integer, Integer>> femaleDesiredToPeopleToHours= getResourceCounts(femaleAnswerToCount,rateInHours);
    typeToNumAnswerToCount.put("desiredToMaleHours",maleDesiredToPeopleToHours);
    typeToNumAnswerToCount.put("desiredToFemaleHours",femaleDesiredToPeopleToHours);

    return typeToNumAnswerToCount;
  }

  public Map<Integer, Map<Integer, Integer>> getResourceCounts(Map<Integer, Integer> maleAnswerToCount) {
    return getResourceCounts(maleAnswerToCount, 1f);
  }

  public Map<Integer, Map<Integer, Integer>> getResourceCounts(Map<Integer, Integer> maleAnswerToCount, float rateInMinutes) {
    //int minPeople = 1;
    int maxPeople = 7;
    int maxDesired = 7;
    Map<Integer,Map<Integer,Integer>> desiredToNumPeopleToPerPerson = new HashMap<Integer, Map<Integer, Integer>>();
    Map<Integer,Integer> numDesiredToTotal = new HashMap<Integer, Integer>();
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
      total = Math.round((float)total*rateInMinutes);

      int peopleStart = Math.max(1,numDesiredPer - numAnswersStart);

      //System.out.println("num answer start " + numAnswersStart + " people start " +peopleStart);
      for (int people = peopleStart; people < maxPeople; people++) {
        float numPerPerson = (float) total/(float) people;
      /*  for (int numAnswers = 0; numAnswers < numDesiredPer; numAnswers++) {
          Integer count = maleAnswerToCount.get(numAnswers);
          int numPer = (numDesiredPer-numAnswers) * count;
         // total += numPer;
          numPerPerson += (float)numPer / (float)people;
        }*/
        peopleToPerPerson.put(people, Math.round(numPerPerson));
      }
      numDesiredToTotal.put(numDesiredPer,total);
    }
    //System.out.println("total " + numDesiredToTotal);
    //System.out.println("desired to people " + desiredToNumPeopleToPerPerson);

    return desiredToNumPeopleToPerPerson;
  }

  /**
   * Get list of counts of answers for each exercise, in order.
   * If the exercise ids are numbers, sort them as numbers, not as strings.
   * We don't want 11 to be between 109 and 110.
   * @param exToCount
   * @return list of counts of answers per exercise
   */
  private List<Integer> getCountArray(Map<String, Integer> exToCount) {
    List<Integer> countArray = new ArrayList<Integer>(exToCount.size());
    String next = exToCount.keySet().iterator().next();
    boolean isInt = false;
    try {
      String left = next.split("/")[0];
      Integer.parseInt(left);
      isInt = true;
    } catch (NumberFormatException e) {
    }
    if (isInt) {
      Map<CompoundKey, Integer> keyToCount = new TreeMap<CompoundKey, Integer>();
      for (Map.Entry<String, Integer> pair : exToCount.entrySet()) {
        try {
          String[] split = pair.getKey().split("/");
          String left = split[0];
          int exid = Integer.parseInt(left);
          String right = split[0];
          int qid = Integer.parseInt(right);

          keyToCount.put(new CompoundKey(exid, qid), pair.getValue());
        } catch (NumberFormatException e) {
          e.printStackTrace();
        }
      }
      countArray.addAll(keyToCount.values());
      return countArray;
    } else {
      for (Map.Entry<String, Integer> pair : exToCount.entrySet()) {
        countArray.add(pair.getValue());
      }
      return countArray;
    }
  }

  private static final int MIN = (60 * 1000);
  private static final int HOUR = (60 * MIN);

  /**
   * Return some statistics related to the hours of audio that have been collected
   * @return
   */
  public Map<String,Number> getResultStats() {
    List<Result> results = getResults();
    double total = 0;
    int count = 0;
    int badDur = 0;

    for (Result r : results) {
      total += r.durationInMillis;
      if (r.durationInMillis > 0) {
        count++;
      }
      else if (r.spoken /*|| r.audioType.equals(Result.AUDIO_TYPE_UNSET)*/) {
        badDur++;
        logger.info("got bad audio result " + r + " path " + r.answer);
      }
    }
    Map<String,Number> typeToStat = new HashMap<String,Number>();
    Number aDouble = total / ((double)HOUR);
    typeToStat.put("totalHrs", aDouble);
    double value = total / ((double)count);
    typeToStat.put("avgSecs", value/1000);
    typeToStat.put("totalAudioAnswers", count);
    typeToStat.put("badRecordings", badDur);
    logger.debug("audio dur stats " + typeToStat);
    return typeToStat;
  }

  public List<User> getUsers() {
    return userDAO.getUsers();
  }


  /**
   * Pulls the list of results out of the database.
   *
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResults(int, int)
   */
  public List<Result> getResults() {
    return resultDAO.getResults();
  }

   public static void main(String [] arg) {
   //  DatabaseImpl langTestDatabase = new DatabaseImpl("C:\\Users\\go22670\\DLITest\\","farsi2");
   //  langTestDatabase.setInstallPath("C:\\Users\\go22670\\DLITest\\clean\\netPron2\\war\\config\\urdu","C:\\Users\\go22670\\DLITest\\clean\\netPron2\\war\\config\\urdu\\5000-no-english.unvow.farsi.txt","",false);


     long rateInMillis = 15*1000;
     float rateInMinutes = (float) rateInMillis/MINUTE_MILLIS_FLOAT;

     MonitoringSupport monitoringSupport = new MonitoringSupport();
     Map<Integer,Integer> answerToCount = new HashMap<Integer, Integer>();
     answerToCount.put(0,16);
     answerToCount.put(1,8);
     answerToCount.put(2,4);
     answerToCount.put(3,2);
     answerToCount.put(4,1);
     answerToCount.put(5,0);
     monitoringSupport.getResourceCounts(answerToCount);
 //    monitoringSupport.getResourceCounts(answerToCount,rateInMinutes);

     Map<Integer,Integer> answerToCount2 = new HashMap<Integer, Integer>();
     answerToCount2.put(0,100);
     answerToCount2.put(1,50);
     answerToCount2.put(2,0);
     answerToCount2.put(3,0);
     answerToCount2.put(4,0);
     answerToCount2.put(5,0);
     monitoringSupport.getResourceCounts(answerToCount2);
   //  monitoringSupport.getResourceCounts(answerToCount2,rateInMinutes);

     Map<Integer,Integer> answerToCount3 = new HashMap<Integer, Integer>();
     answerToCount3.put(0,0);
     answerToCount3.put(1,10);
     answerToCount3.put(2,5);
     answerToCount3.put(3,0);
     answerToCount3.put(4,0);
     answerToCount3.put(5,0);
     monitoringSupport.getResourceCounts(answerToCount3);
   //  monitoringSupport.getResourceCounts(answerToCount3,rateInMinutes);

     Map<Integer,Integer> answerToCount4 = new HashMap<Integer, Integer>();
     answerToCount4.put(0,0);
     answerToCount4.put(1,0);
     answerToCount4.put(2,5);
     answerToCount4.put(3,0);
     answerToCount4.put(4,0);
     answerToCount4.put(5,0);
     monitoringSupport.getResourceCounts(answerToCount4);
   }
}
