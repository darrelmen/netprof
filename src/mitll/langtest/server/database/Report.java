/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.instrumentation.EventDAO;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;
import mitll.langtest.shared.UserAndTime;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.instrumentation.Event;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by go22670 on 10/16/14.
 */
public class Report {
  private static final Logger logger = Logger.getLogger(Report.class);

  private static final String NP_SERVER = "np.ll.mit.edu";
  private static final String MY_EMAIL = "gordon.vidaver@ll.mit.edu";

  private static final int MIN_MILLIS = (1000 * 60);
  private static final int TEN_SECONDS = 1000 * 10;
  private static final boolean CLEAR_DAY_HOUR_MINUTE = true;
  private static final boolean WRITE_RESULTS_TO_FILE = false;
  private static final String ACTIVE_USERS = "Active Users";
  private static final String TIME_ON_TASK_MINUTES = "Time on Task Minutes ";
  private static final String TOTAL_TIME_ON_TASK_HOURS = "Total time on task (hours)";
  private static final String MONTH = "By Month";
  private static final String WEEK = "By Week";
  private static final String top = "<td style='vertical-align: top;'>";
  private static final String DEVICE_RECORDINGS = "Device Recordings";
  private static final String ALL_RECORDINGS = "All Recordings";
  private static final String MM_DD_YY = "MM_dd_yy";
  //  public static final String MM_DD_YY_HH_MM_SS = "MM_dd_yy_hh_mm_ss";
  private static final boolean SHOW_TEACHER_SKIPS = false;
  private static final boolean DO_2014 = false;
  private static final String NEW_I_PAD_I_PHONE_USERS = "New iPad/iPhone Users";
  private static final String TIME_ON_TASK = "Time on Task";
  public static final String MM_DD = "MM-dd";
  public static final String ALL_NEW_USERS = "All New Users";

  private final UserDAO userDAO;
  private final ResultDAO resultDAO;
  private final EventDAO eventDAO;
  private final AudioDAO audioDAO;
  private final String prefix;
  private final String language;
  private BufferedWriter csv;
  private final Map<Long, Long> userToStart = new HashMap<>();
  private static final boolean DEBUG = false;

  private final Set<String> lincoln = new HashSet<>(Arrays.asList("gvidaver", "rbudd", "jmelot", "esalesky", "gatewood",
      "testing", "grading", "fullperm", "0001abcd", "egodoy",
      "rb2rb2",
      "dajone3",
      "WagnerSandy",
      "rbtrbt"));

  public Report(UserDAO userDAO, ResultDAO resultDAO, EventDAO eventDAO, AudioDAO audioDAO, String language,
                String prefix) {
    this.userDAO = userDAO;
    this.resultDAO = resultDAO;
    this.eventDAO = eventDAO;
    this.audioDAO = audioDAO;
    this.language = language;
    this.prefix = prefix;
  }

  /**
   * Sends a usage report to the email list at property {@link mitll.langtest.server.ServerProperties#getReportEmails()}.
   * Sends it out first thing every monday.
   * Subject disambiguates between multiple sites for the same language.
   * Also writes the report out to the report directory... TODO : necessary?
   *
   * @see mitll.langtest.server.database.DatabaseImpl#doReport
   */
  public void doReport(ServerProperties serverProps, String site, MailSupport mailSupport,
                       PathHelper pathHelper) {
    List<String> reportEmails = serverProps.getReportEmails();

    // check if it's a monday
    if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY &&
        !reportEmails.isEmpty()) {
      writeAndSendReport(serverProps.getLanguage(), site, mailSupport, pathHelper, reportEmails, getThisYear());
    } else {
//      logger.debug("not sending email report since this is not monday");
    }
  }

  private void writeAndSendReport(String language, String site, MailSupport mailSupport,
                                  PathHelper pathHelper, List<String> reportEmails, int year) {
    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM_dd_yy");
    String today = simpleDateFormat2.format(new Date());
    File file = getReportFile(pathHelper, today, language);
    //logger.debug("checking file " + file.getAbsolutePath());
    if (file.exists()) {
      logger.debug("already did report for " + today + " : " + file.getAbsolutePath());
    } else {
      logger.debug("Site real path " + site);
      try {
        JSONObject jsonObject = new JSONObject();

        String message = writeReportToFile(file, pathHelper, language, jsonObject, year);
        sendEmails(language, site, mailSupport, reportEmails, message);

      } catch (IOException e) {
        logger.error("got " + e, e);
      }
    }
  }

  /**
   * @param pathHelper
   * @param language
   * @throws IOException
   * @see DatabaseImpl#doReport
   */
  public JSONObject writeReportToFile(PathHelper pathHelper, String language, int year) throws IOException {
    File file = getReportPath(pathHelper, language);
    JSONObject jsonObject = new JSONObject();
    writeReportToFile(file, pathHelper, language, jsonObject, year);
    logger.debug("wrote to " + file.getAbsolutePath());

    //logger.debug("\n" + jsonObject.toString());

    return jsonObject;
  }

  private File getReportPath(PathHelper pathHelper, String language) {
    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat(MM_DD_YY);
    String today = simpleDateFormat2.format(new Date());
    return getReportFile(pathHelper, today, language);
  }

  private void sendEmails(String language, String site, MailSupport mailSupport, List<String> reportEmails, String message) {
    String suffix = "";
    if (site != null && site.contains("npfClassroom")) {
      site = site.substring(site.indexOf("npfClassroom"));
      suffix = " at " + site;
    }

    String subject = "Weekly Usage Report for " + language + suffix;
    for (String dest : reportEmails) {
      mailSupport.sendEmail(NP_SERVER, dest, MY_EMAIL, subject, message);
    }
  }

  /**
   * @param file       write html to a file
   * @param pathHelper
   * @param language
   * @return html of report
   * @throws IOException
   * @see
   */
  private String writeReportToFile(File file, PathHelper pathHelper, String language, JSONObject jsonObject, int year) throws IOException {
    String message = doReport(pathHelper, language, jsonObject, year);

    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
    writer.write(message);
    writer.close();

    csv.close();
    return message;
  }

  private File getReportFile(PathHelper pathHelper, String today, String language) {
    File reports = pathHelper.getAbsoluteFile("reports");
    if (!reports.exists()) {
      logger.debug("making dir " + reports.getAbsolutePath());
      reports.mkdirs();
    } else {
      logger.debug("reports dir exists at " + reports.getAbsolutePath());
    }
    String fileName = prefix + "_" + language + "_report_" + today + ".html";
    return new File(reports, fileName);
  }

  /**
   * @return html of report
   * @see #writeReportToFile
   */
  private String doReport(PathHelper pathHelper, String language, JSONObject jsonObject, int year) {
    openCSVWriter(pathHelper, language);
    return getReport(jsonObject, year);
  }

  public String getReport(JSONObject jsonObject, int year) {
  //  logger.info("doing year " + year);
    setUserStart();

    StringBuilder builder = new StringBuilder();
    builder.append("<html><head><body>");

    // all users
    JSONObject allUsers = new JSONObject();
    Set<Long> users = getUsers(builder, allUsers, year);
    jsonObject.put("allUsers", allUsers);

    // ipad users
    JSONObject iPadUsers = new JSONObject();
    getUsers(builder, userDAO.getUsersDevices(), NEW_I_PAD_I_PHONE_USERS, iPadUsers, year);
    jsonObject.put("iPadUsers", iPadUsers);

    JSONObject timeOnTaskJSON = new JSONObject();
    Set<Long> events = getEvents(builder, users, timeOnTaskJSON, year);
    jsonObject.put("overallTimeOnTask", timeOnTaskJSON);

    JSONObject deviceTimeOnTaskJSON = new JSONObject();
    Set<Long> eventsDevices = getEventsDevices(builder, users, deviceTimeOnTaskJSON, year);
    jsonObject.put("deviceTimeOnTask", deviceTimeOnTaskJSON);

    events.addAll(eventsDevices);

    builder.append(getActiveUserHeader(events, year));

    JSONObject uniqueUsersYTD = new JSONObject();
    uniqueUsersYTD.put("count", events.size());
    jsonObject.put("uniqueUsersYTD", uniqueUsersYTD);

    JSONObject allRecordings = new JSONObject();
    getResults(builder, users, /*pathHelper, language,*/ allRecordings, year);
    jsonObject.put("allRecordings", allRecordings);

    JSONObject deviceRecordings = new JSONObject();
    getResultsDevices(builder, users, /*pathHelper, language,*/ deviceRecordings, year);
    jsonObject.put("deviceRecordings", deviceRecordings);

    Calendar calendar = getCalendarForYear(year);
    Date january1st = getJanuaryFirst(calendar, year);
    Date january1stNextYear = getNextYear(year);

    if (DEBUG) {
      logger.info("doReport : between " + january1st + " and " + january1stNextYear);
    }

    JSONObject referenceRecordings = new JSONObject();
    addRefAudio(builder, calendar, january1st, january1stNextYear, audioDAO.getAudioAttributes(), referenceRecordings, year);
    jsonObject.put("referenceRecordings", referenceRecordings);

    builder.append("</body></head></html>");
    return builder.toString();
  }

  private void openCSVWriter(PathHelper pathHelper, String language) {
    try {
      File reportPath = getReportPath(pathHelper, language);
      String s = reportPath.getAbsolutePath().replaceAll(".html", ".csv");
      this.csv = new BufferedWriter(new FileWriter(s));
    } catch (IOException e) {
      logger.error("got " + e, e);
    }
  }

  private String getActiveUserHeader(Set<Long> events, int year) {
    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM/dd/yy");
    String start = simpleDateFormat2.format(getJanuaryFirst(getCalendarForYear(year), year));
    String end = simpleDateFormat2.format(getNextYear(year));

    return "<h2>Unique Active Users between " + start + " and " + end +
        "</h2>" +
        "<table ><tr>" +
        top + events.size() + "</td>" +
        "</tr></table>";
  }

/*
  public int getActiveUsersYTD() {
    StringBuilder builder = new StringBuilder();
    builder.append("<html><head><body>");

    Set<Long> users = getUsers(builder);
    Set<Long> events = getEvents(builder, users);
    Set<Long> eventsDevices = getEventsDevices(builder, users);

    events.addAll(eventsDevices);

    return events.size();
  }
*/

  /**
   * @param builder
   * @return
   * @see #doReport
   */
  private Set<Long> getUsers(StringBuilder builder, JSONObject jsonObject, int year) {
    return getUsers(builder, fixUserStarts(), ALL_NEW_USERS, jsonObject, year);
  }

  private List<User> fixUserStarts() {
    List<User> users = userDAO.getUsers();
    for (User user : users) {
      Long aLong = userToStart.get(user.getId());
      if (aLong != null) user.setTimestampMillis(aLong);
      //else {
      //  logger.error("no events for " + user.getId());
      //  }
    }
    return users;
  }

  /**
   * @param builder
   * @param users
   * @param users1
   * @return set of valid users
   * @see #doReport
   * @see #getUsers
   */
  private Set<Long> getUsers(StringBuilder builder, Collection<User> users, String users1, JSONObject jsonObject, int year) {
    Calendar calendar = getCalendarForYear(year);
    Date january1st = getJanuaryFirst(calendar, year);
    Date january1stNextYear = getNextYear(year);

    // logger.info("getUsers between " + january1st + " and " + january1stNextYear);

    int ytd = 0;

    Map<Integer, Integer> monthToCount = new TreeMap<>();
    Map<Integer, Integer> weekToCount = new TreeMap<>();
    Set<Long> students = new HashSet<>();

    for (User user : users) {
      boolean isStudent = (user.getAge() == 89 && user.getUserID().isEmpty()) || user.getAge() == 0 ||
          user.getUserKind() == User.Kind.STUDENT;

      if (user.getUserKind() == User.Kind.TEACHER) isStudent = false;
      boolean contains = false;
      for (String ll : lincoln) {
        if (user.getUserID().startsWith(ll)) {
          contains = true;
          break;
        }
      }
      if (contains) isStudent = false;

      if (user.getId() == 3 || user.getId() == 1 ||
          lincoln.contains(user.getUserID()) || user.getUserID().startsWith("gvidaver")
          ) {
        if (SHOW_TEACHER_SKIPS) logger.warn("skipping ? " + user);
        continue;
      }
      if (isStudent) {
        students.add(user.getId());
        long userCreated = user.getTimestampMillis();
        if (userCreated > january1st.getTime() &&
            userCreated < january1stNextYear.getTime()
            ) {
          ytd++;

          calendar.setTimeInMillis(userCreated);
          int month = calendar.get(Calendar.MONTH);
          Integer integer = monthToCount.get(month);
          monthToCount.put(month, (integer == null) ? 1 : integer + 1);
          int w = calendar.get(Calendar.WEEK_OF_YEAR);
          Integer integer2 = weekToCount.get(w);
          weekToCount.put(w, (integer2 == null) ? 1 : integer2 + 1);
        } else {
          //   logger.debug("NO time " +user.getTimestamp() + " " + parse);
        }
        //}
      } else {
        if (SHOW_TEACHER_SKIPS) logger.warn("skipping teacher " + user);
      }
    }
    builder.append(getSectionReport(ytd, monthToCount, weekToCount, users1, jsonObject, year));

//    logger.info("Students " + students);
    return students;
  }

/*  private static class YearStats {
    private int ytd;
    private Map<Integer, Integer> monthToCount;
    private Map<Integer, Integer> weekToCount;

    public YearStats(int ytd, Map<Integer, Integer> monthToCount, Map<Integer, Integer> weekToCount) {
      this.ytd = ytd;
      this.monthToCount = monthToCount;
      this.weekToCount = weekToCount;
    }

    public int getYtd() {
      return ytd;
    }

    public Map<Integer, Integer> getMonthToCount() {
      return monthToCount;
    }

    public Map<Integer, Integer> getWeekToCount() {
      return weekToCount;
    }
  }*/

  /**
   * @param ytd
   * @param monthToCount
   * @param weekToCount
   * @param users1
   * @param jsonObject
   * @return
   * @see #addRefAudio
   * @see #getEvents(StringBuilder, Set, List, String, String, JSONObject, int)
   * @see #getResultsForSet
   * @see #getUsers
   */
  private String getSectionReport(int ytd,
                                  Map<Integer, ?> monthToCount,
                                  Map<Integer, ?> weekToCount, String users1,
                                  JSONObject jsonObject, int year) {
    JSONObject yearJSON = new JSONObject();
    JSONArray monthArray = new JSONArray();
    JSONArray weekArray = new JSONArray();

    String yearCol = ytd > -1 ? getYTD(ytd, users1, yearJSON, year) : "";
    String monthCol = getMonthToCount(monthToCount, MONTH, users1, "", monthArray, year);
    String weekCol = getWC(weekToCount, WEEK, users1, weekArray, year);

    jsonObject.put("year", yearJSON);
    jsonObject.put("month", monthArray);
    jsonObject.put("week", weekArray);

//    logger.debug("getSectionReport json " + jsonObject);

    return getYearMonthWeekTable(users1, yearCol, monthCol, weekCol);
  }

  private void writeMonthToCSV(Map<Integer, ?> monthToCount, String users1, String language, int year) {
    StringBuilder builder = new StringBuilder();
    //int i = getYear();

    String otherPrefix = this.prefix;
    String prefix = otherPrefix + "," + language + "," + year + "," + users1 + ",";
    builder.append(prefix);

    for (int j = 0; j < 12; j++) {
      Object o = monthToCount.get(j);
      Object o1 = o == null ? "0" : o;
      if (o1 instanceof Collection<?>) {
        o1 = ((Collection<?>) o1).size();
      }
      builder.append(o1 + ",");
    }
    builder.append("\n");

    if (DEBUG) logger.info(builder.toString());

    try {
      if (csv != null) csv.write(builder.toString());
    } catch (IOException e) {
      logger.error("Got " + e, e);
    }
  }

  private String getYearMonthWeekTable(String users1, String yearCol, String monthCol, String weekCol) {
    return "<h2>" + users1 + "</h2>" +
        "<table ><tr>" +
        top + yearCol + "</td>" +
        top + monthCol + "</td>" +
        top + weekCol + "</td>" +
        "</tr></table>";
  }

/*  private String getYTD(int ytd, String users1, JSONObject jsonObject) {
    String ytd1 = getYTD(ytd, users1, jsonObject, getYear());
    return ytd1;
  }*/

  private String getYTD(int ytd, String users1, JSONObject jsonObject, int year) {
    jsonObject.put("label", users1);
    jsonObject.put("year", year);
    jsonObject.put("ytd", ytd);

    return "<table style='background-color: #eaf5fb'>" +
        "<tr>" +
        "<th>" +
        users1 +
        " YTD (" + year +
        ")</th>" + "</tr>" +
        "<tr>" +
        "<td>" + ytd +
        "</td>" + "</tr>" +
        "</table><br/>\n";
  }

  /**
   * @param monthToCount
   * @param unit
   * @param count
   * @param tableLabel
   * @param jsonArray
   * @return html for month
   */
  private String getMonthToCount(Map<Integer, ?> monthToCount, String unit, String count, String tableLabel,
                                 JSONArray jsonArray, int year) {
    writeMonthToCSV(monthToCount, tableLabel.isEmpty() ? count : tableLabel, language, year);

    String s = "";
    for (Map.Entry<Integer, ?> pair : monthToCount.entrySet()) {
      Object value = pair.getValue();
      if (value instanceof Collection<?>) {
        value = ((Collection<?>) value).size();
      }
      Integer key = pair.getKey();
      String month = getMonth(key);

      JSONObject jsonObject = new JSONObject();
      jsonObject.put("month", key);
      jsonObject.put("count", value);
      //   jsonObject.put("unit", unit);
      jsonArray.add(jsonObject);

      s += "<tr><td>" + month + "</td><td>" + value + "</td></tr>";
    }
    return "<table style='background-color: #eaf5fb' >" +
        "<tr>" +
        "<th>" +
        unit +
        "</th>" +
        "<th>" + count + "</th>" +
        "</tr>" +
        s +
        "</table><br/>\n";
  }

  /**
   * @param weekToCount
   * @param unit
   * @param count
   * @return
   * @see #doReport
   * @see #getEvents
   * @see #getResults
   */
  private String getWC(Map<Integer, ?> weekToCount, String unit, String count, JSONArray jsonArray, int year) {
    String s = "";
    Calendar calendar = getCalendarForYear(year);

    SimpleDateFormat df = new SimpleDateFormat(MM_DD);

    for (Map.Entry<Integer, ?> pair : weekToCount.entrySet()) {
      Object value = pair.getValue();
      if (value instanceof Collection<?>) {
        value = ((Collection<?>) value).size();
      }
      Integer key = pair.getKey();
      calendar.set(Calendar.WEEK_OF_YEAR, key);
      Date time = calendar.getTime();
      String format1 = df.format(time);

      JSONObject jsonObject = new JSONObject();
      jsonObject.put("weekOfYear", key);
      jsonObject.put("count", value);
      //  jsonObject.put("unit", unit);
      jsonArray.add(jsonObject);

      s += "<tr><td>" +
          "<span>" + format1 +
          "</span>" +
          "</td><td>" + value + "</td></tr>";
    }
    return "<table style='background-color: #eaf5fb'>" +
        "<tr>" +
        "<th>" +
        unit +
        "</th>" +
        "<th>" + count + "</th>" +
        "</tr>" +
        s +
        "</table><br/>\n";
  }


  /**
   * @param builder
   * @paramx language
   * @param year
   * @see #doReport
   */
  private void getResults(StringBuilder builder, Set<Long> students, /*PathHelper pathHelper, String language,*/
                          JSONObject jsonObject, int year) {
    List<Result> results = resultDAO.getResults();
    getResultsForSet(builder, students, /*pathHelper,*/ results, ALL_RECORDINGS, /*language,*/ jsonObject, year);
  }

  private void getResultsDevices(StringBuilder builder, Set<Long> students,/* PathHelper pathHelper, String language,*/
                                 JSONObject jsonObject, int year) {
    List<Result> results = resultDAO.getResultsDevices();
    getResultsForSet(builder, students, /*pathHelper, */results, DEVICE_RECORDINGS, /*language,*/ jsonObject, year);
  }

  private void getResultsForSet(StringBuilder builder, Set<Long> students,
                                //PathHelper pathHelper,
                                Collection<Result> results,
                                String recordings,// String language,
                                JSONObject jsonObject, int year) {
    Date january1st = getJanuaryFirst(getCalendarForYear(year), year);
    Date january1stNextYear = getNextYear(year);
    //  logger.info("between " + january1st + " and " + january1stNextYear);

    int ytd = 0;

    Map<String, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio();

    //logger.debug("found " + exToAudio.size() + " ref audio exercises");

    Map<Integer, Integer> monthToCount = new TreeMap<>();
    Map<Integer, Integer> weekToCount = new TreeMap<>();
    Map<Long, Map<String, Integer>> userToDayToCount = new TreeMap<>();

    int teacherAudio = 0;
    int invalid = 0;
    int invalidScore = 0;

    int beforeJanuary = 0;
    Set<Long> skipped = new TreeSet<>();
    try {
      SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM_dd_yy");
      String today = simpleDateFormat2.format(new Date());

      BufferedWriter writer = null;

/*      if (WRITE_RESULTS_TO_FILE) {
        File file = getReportFile(pathHelper, today + "_all", language);
        writer = new BufferedWriter(new FileWriter(file));
      }*/

      teacherAudio = 0;
      invalid = 0;
      Calendar calendar = getCalendarForYear(year);

      long time = january1st.getTime();
      long time1 = january1stNextYear.getTime();

      for (Result result : results) {
        long timestamp = result.getTimestamp();

        if (timestamp > time &&
            timestamp < time1
            ) {
          if (result.isValid()) {
            if (!isRefAudioResult(exToAudio, result)) {
              if (students.contains(result.getUserid())) {
                //if (result.getAudioType().equals("unset") || result.getAudioType().equals("_by_WebRTC")) {
                if (result.getPronScore() > -1) {
                  if (isValidUser(result.getUserid())) {
                    if (WRITE_RESULTS_TO_FILE) {
                      writer.write(result.toString());
                      writer.write("\n");
                    }
                    ytd++;
                    tallyByMonthAndWeek(calendar, monthToCount, weekToCount, result, userToDayToCount);
                  }
                } else {
                  invalidScore++;
                }
              } else {
                skipped.add(result.getUserid());
                teacherAudio++;
              }
            }
          } else {
            invalid++;
          }
        } else {
          beforeJanuary++;
        }
      }
      if (WRITE_RESULTS_TO_FILE) {
        writer.close();
      }
    } catch (IOException e) {
      logger.error("got " + e, e);
    }

    if (DEBUG)
      logger.debug("Skipped " + invalid + " invalid recordings, " + invalidScore + " -1 score items, " + beforeJanuary + " beforeJan1st");
    if (teacherAudio > 0) {
      if (DEBUG) logger.debug("skipped " + teacherAudio + " teacher recordings by " + skipped);
    }
    //logger.debug("userToDayToCount " + userToDayToCount.size());

    builder.append("\n<br/><span>Valid student recordings</span>");
    builder.append(
        getSectionReport(ytd, monthToCount, weekToCount, recordings, jsonObject, year)
    );
    // return userToDayToCount;
  }

  /**
   * @param builder
   * @param calendar
   * @param january1st
   * @param refAudio
   * @param jsonObject
   * @paramx language
   * @see #getResults
   */
  private <T extends UserAndTime> void addRefAudio(StringBuilder builder, Calendar calendar,
                                                   Date january1st,
                                                   Date january1stThisYear,
                                                   Collection<T> refAudio, JSONObject jsonObject, int year) {
    int ytd = 0;
    Map<Integer, Integer> monthToCount = new TreeMap<>();
    Map<Integer, Integer> weekToCount = new TreeMap<>();
    Map<Long, Map<String, Integer>> userToDayToCount = new TreeMap<>();
    long time = january1st.getTime();
    long time1 = january1stThisYear.getTime();

    for (T result : refAudio) {
      long timestamp = result.getTimestamp();
      if (timestamp > time && timestamp < time1) {
        ytd++;
        tallyByMonthAndWeek(calendar, monthToCount, weekToCount, result, userToDayToCount);
      }
    }

    String refAudioRecs = "Ref Audio Recordings";
    builder.append(getSectionReport(ytd, monthToCount, weekToCount, refAudioRecs, jsonObject, year));
  }

  /**
   * @param calendar
   * @param monthToCount
   * @param weekToCount
   * @param result
   * @param userToDayToCount
   * @see #addRefAudio
   * @see #getResults
   */
  private void tallyByMonthAndWeek(Calendar calendar,
                                   Map<Integer, Integer> monthToCount,
                                   Map<Integer, Integer> weekToCount,
                                   UserAndTime result,
                                   Map<Long, Map<String, Integer>> userToDayToCount) {
    long timestamp = result.getTimestamp();
    long userid = result.getUserid();

    tallyByMonthAndWeek(calendar, monthToCount, weekToCount, userToDayToCount, timestamp, userid);
  }

  private void tallyByMonthAndWeek(Calendar calendar, Map<Integer, Integer> monthToCount,
                                   Map<Integer, Integer> weekToCount,
                                   Map<Long, Map<String, Integer>> userToDayToCount, long timestamp, long userid) {
    calendar.setTimeInMillis(timestamp);
    int month = calendar.get(Calendar.MONTH);
    Integer integer = monthToCount.get(month);
    monthToCount.put(month, (integer == null) ? 1 : integer + 1);

    int w = calendar.get(Calendar.WEEK_OF_YEAR);
    Integer integer2 = weekToCount.get(w);

    Map<String, Integer> dayToCount = userToDayToCount.get(userid);
    if (dayToCount == null) userToDayToCount.put(userid, dayToCount = new TreeMap<>());
    String key = calendar.get(Calendar.YEAR) + "," +
        calendar.get(Calendar.MONTH) + "," +
        calendar.get(Calendar.DAY_OF_MONTH);
    Integer countAtDay = dayToCount.get(key);
    dayToCount.put(
        key
        , countAtDay == null ? 1 : countAtDay + 1);
    weekToCount.put(w, (integer2 == null) ? 1 : integer2 + 1);
  }

  /**
   * @param exToAudio
   * @param result
   * @return
   * @see #getResults
   */
  private boolean isRefAudioResult(Map<String, List<AudioAttribute>> exToAudio, Result result) {
    boolean skip = false;
    List<AudioAttribute> audioAttributes = exToAudio.get(result.getExerciseID());
    if (audioAttributes != null) {
      for (AudioAttribute audioAttribute : audioAttributes) {
        if (getFile(audioAttribute).equals(getFile(result))) {
          long userid = result.getUserid();
          if (audioAttribute.getUser().getId() == userid) {
            skip = true;
            break;
          }
        }
      }
    }
    return skip;
  }

  private String getFile(AudioAttribute attribute) {
    return getFile(attribute.getAudioRef());
  }

  private String getFile(Result result) {
    return getFile(result.getAnswer());
  }

  private String getFile(String audioRef) {
    String[] bestAudios = audioRef.split(File.separator);
    return bestAudios[bestAudios.length - 1];
  }

  /**
   * @param calendar
   * @return
   * @see #doReport
   * @see #getEvents
   * @see #getResults
   */
/*
  private Date getJanuaryFirst(Calendar calendar) {
    return getJanuaryFirst(calendar, getThisYear());
  }
*/
  private Date getJanuaryFirst(Calendar calendar, int year) {
    calendar.set(Calendar.YEAR, year);
    calendar.set(Calendar.DAY_OF_YEAR, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    return calendar.getTime();
  }

/*  private int getYear() {
    return DO_2014 ? 2014 : getThisYear();//getCalForThisYear().get(Calendar.YEAR);
  }*/

/*  private Calendar getCalForThisYear() {
    int year2 = getThisYear();
    return getCalendarForYear(year2);
  }*/

  private Calendar getCalendarForYear(int year) {
    Calendar instance = Calendar.getInstance();
    instance.clear();
    instance.set(Calendar.YEAR, year);
    return instance;
  }

/*  private Date getNextYear(year) {
    int year = getThisYear();
    return getNextYear(year);
  }*/

  private Date getNextYear(int year) {
    Calendar instance = Calendar.getInstance();
    if (CLEAR_DAY_HOUR_MINUTE) {
      instance.set(Calendar.DAY_OF_YEAR, 1);
      instance.set(Calendar.HOUR_OF_DAY, 0);
      instance.set(Calendar.MINUTE, 0);
      instance.set(Calendar.YEAR, year + 1);
    }
    return instance.getTime();
  }

  private int getThisYear() {
    return DO_2014 ? 2014 : Calendar.getInstance().get(Calendar.YEAR);
  }

  /**
   * @param builder
   * @param students
   * @param jsonObject
   * @see #doReport
   */
  private Set<Long> getEvents(StringBuilder builder, Set<Long> students, JSONObject jsonObject, int year) {
    return getEvents(builder, students, eventDAO.getAll(), ACTIVE_USERS, TIME_ON_TASK, jsonObject, year);
  }

  /**
   * Look at the event table to determine the first moment a user did anything
   * There was a bug where the user timestamp was not set properly.
   */
  private void setUserStart() {
    List<Event> all = eventDAO.getAll();
    for (Event event : all) {
      long creatorID = event.getCreatorID();
      long timestamp = event.getTimestamp();
      if (!userToStart.containsKey(creatorID) || timestamp < userToStart.get(creatorID)) {
        userToStart.put(creatorID, timestamp);
      }
    }
  }

  private Set<Long> getEventsDevices(StringBuilder builder, Set<Long> students, JSONObject jsonObject, int year) {
    List<Event> all = eventDAO.getAllDevices();
    String activeUsers = "Active iPad/iPhone Users";
    String tableLabel = "iPad/iPhone Time on Task";
    return getEvents(builder, students, all, activeUsers, tableLabel, jsonObject, year);
  }

  /**
   * @param builder
   * @param students
   * @param all
   * @param activeUsers
   * @param tableLabel
   * @param jsonObject
   * @param year
   * @see #getEvents
   */
  private Set<Long> getEvents(StringBuilder builder, Set<Long> students, List<Event> all, String activeUsers,
                              String tableLabel, JSONObject jsonObject, int year) {
    Map<Integer, Set<Long>> monthToCount = new TreeMap<>();
    Map<Integer, Map<Long, Set<Event>>> monthToCount2 = new TreeMap<>();
    Map<Integer, Map<Long, Set<Event>>> weekToCount2 = new TreeMap<>();

    Map<Integer, Set<Long>> weekToCount = new TreeMap<>();
    Set<Long> teachers = new HashSet<>();
    int skipped = 0;

    Calendar calendar = getCalendarForYear(year);
    Date january1st = getJanuaryFirst(calendar, year);
    Date january1stNextYear = getNextYear(year);
    if (DEBUG) logger.info("getEvents from " + january1st + " to " + january1stNextYear);
    long time = january1st.getTime();
    long time1 = january1stNextYear.getTime();

//    logger.info("students " + students);

    Set<Long> users = new HashSet<>();

    for (Event event : all) {
      long creatorID = event.getCreatorID();
      long timestamp = event.getTimestamp();
      if (timestamp > time && timestamp < time1 &&
          students.contains(creatorID)) {
        if (isValidUser(creatorID)) {
          users.add(creatorID);
          statsForEvent(calendar, monthToCount, monthToCount2, weekToCount2, weekToCount, event, creatorID);
        }
      } else if (!students.contains(creatorID)) {
        skipped++;
        teachers.add(creatorID);
      }
    }

    dumpActiveUsers(activeUsers, teachers, skipped, users);

    JSONObject activeJSON = new JSONObject();
    builder.append(getSectionReport(-1, monthToCount, weekToCount, activeUsers, activeJSON, year));
    jsonObject.put("activeUsers", activeJSON);

//    logger.debug("active users " + activeJSON);

    Map<Integer, Long> monthToDur = getMonthToDur(monthToCount2);
    long total = 0;
    for (Long v : monthToDur.values()) total += v;

    total /= MIN_MILLIS;

    Map<Integer, Long> weekToDur = getWeekToDur(weekToCount2);

    JSONObject timeOnTaskJSON = new JSONObject();

    JSONObject yearJSON = new JSONObject();
    JSONArray monthArray = new JSONArray();
    JSONArray weekArray = new JSONArray();

    int ytdHours = Math.round(total / 60);

//    logger.info("ytd hours " + ytdHours);

    String yearMonthWeekTable = getYearMonthWeekTable(tableLabel,
        getYTD(ytdHours, TOTAL_TIME_ON_TASK_HOURS, yearJSON, year),
        getMonthToCount(getMinMap(monthToDur), MONTH, TIME_ON_TASK_MINUTES, tableLabel, monthArray, year),
        getWC(getMinMap(weekToDur), WEEK, TIME_ON_TASK_MINUTES, weekArray, year)
    );

    timeOnTaskJSON.put("year", yearJSON);
    timeOnTaskJSON.put("month", monthArray);
    timeOnTaskJSON.put("week", weekArray);

    jsonObject.put("timeOnTask", timeOnTaskJSON);

    builder.append(yearMonthWeekTable);

    return users;
  }

  private void dumpActiveUsers(String activeUsers, Set<Long> teachers, int skipped, Set<Long> users) {
    List<Long> longs = new ArrayList<>(users);
    Collections.sort(longs);
//    logger.debug(activeUsers + " getEvents skipped " + skipped + " events from teachers " + teachers + "\nusers " + longs);
  }

  private boolean isValidUser(long creatorID) {
    return creatorID != 1;
  }

  private void statsForEvent(Calendar calendar,
                             Map<Integer, Set<Long>> monthToCount,
                             Map<Integer, Map<Long, Set<Event>>> monthToUserToEvents,
                             Map<Integer, Map<Long, Set<Event>>> weekToUserToEvents,
                             Map<Integer, Set<Long>> weekToCount,
                             Event event, long creatorID) {
    calendar.setTimeInMillis(event.getTimestamp());

    // months
    int month = calendar.get(Calendar.MONTH);

    Map<Long, Set<Event>> userToEvents = monthToUserToEvents.get(month);
    Set<Long> users = monthToCount.get(month);
    if (users == null) {
      monthToCount.put(month, users = new HashSet<>());
    }
    users.add(creatorID);

    if (userToEvents == null) {
      monthToUserToEvents.put(month, userToEvents = new HashMap<>());
    }

    Set<Event> events = userToEvents.get(creatorID);
    if (events == null) userToEvents.put(creatorID, events = new TreeSet<>());
    events.add(event);


    // weeks
    int w = calendar.get(Calendar.WEEK_OF_YEAR);
    Set<Long> userInWeek = weekToCount.get(w);
    if (userInWeek == null) {
      weekToCount.put(w, userInWeek = new HashSet<>());
    }
    userInWeek.add(creatorID);

    userToEvents = weekToUserToEvents.get(w);
    if (userToEvents == null) {
      weekToUserToEvents.put(w, userToEvents = new HashMap<>());
    }
    events = userToEvents.get(creatorID);

    if (events == null) userToEvents.put(creatorID, events = new TreeSet<>());
    events.add(event);
  }

  private Map<Integer, Long> getMinMap(Map<Integer, Long> monthToDur) {
    Map<Integer, Long> copy = new TreeMap<>();
    for (Map.Entry<Integer, Long> pair : monthToDur.entrySet()) {
      long value = pair.getValue() / MIN_MILLIS;
      if (value == 0) {
        //logger.debug("huh? " +pair.getKey() + " " + pair.getValue());
        value = 1;
      }
      copy.put(pair.getKey(), value);
    }

    return copy;
  }

  /**
   * @param monthToCount2
   * @return in minutes
   */
  private Map<Integer, Long> getMonthToDur(Map<Integer, Map<Long, Set<Event>>> monthToCount2) {
    Map<Integer, Long> monthToDur = new TreeMap<>();
    for (Map.Entry<Integer, Map<Long, Set<Event>>> monthToUserToEvents : monthToCount2.entrySet()) {
      Integer month = monthToUserToEvents.getKey();
      //logger.debug("month " + month);

      Map<Long, Set<Event>> userToEvents = monthToUserToEvents.getValue();

      for (Map.Entry<Long, Set<Event>> eventsForUser : userToEvents.entrySet()) {
//        Long user = eventsForUser.getKey();
        long start = 0;
        long dur = 0;
        long last = 0;
        //      Event sevent = null, levent = null;
        for (Event event : eventsForUser.getValue()) {
          long now = event.getTimestamp();
          if (start == 0) {
            start = now;
            //        sevent = event;
          } else if (now - last > 1000 * 300) {
            long session = (last - start);
            if (session == 0) {
              session = TEN_SECONDS;
            }
            dur += session;
            start = now;
            //      sevent = event;
          }

          last = now;
          //  levent = event;
        }
        long session = (last - start);
        if (session == 0) {
          session = TEN_SECONDS;
        }
        dur += session;
        Long aLong = monthToDur.get(month);

        monthToDur.put(month, aLong == null ? dur : aLong + dur);
      }
    }
    return monthToDur;
  }

  private Map<Integer, Long> getWeekToDur(Map<Integer, Map<Long, Set<Event>>> weekToCount) {
    Map<Integer, Long> weekToDur = new TreeMap<>();
    for (Map.Entry<Integer, Map<Long, Set<Event>>> weekToUserToEvents : weekToCount.entrySet()) {
      Integer week = weekToUserToEvents.getKey();
      //logger.debug("week " + week);

      Map<Long, Set<Event>> userToEvents = weekToUserToEvents.getValue();

      for (Map.Entry<Long, Set<Event>> eventsForUser : userToEvents.entrySet()) {
        //  Long user = eventsForUser.getKey();
        //logger.debug("\tuser " + user);
        long start = 0;
        long dur = 0;
        long last = 0;
        for (Event event : eventsForUser.getValue()) {
          long now = event.getTimestamp();
          if (start == 0) {
            start = now;
          } else if (now - last > 1000 * 300) {
            long session = last - start;
            if (session == 0) {
              session = TEN_SECONDS;
            }
            dur += session;// / MIN_MILLIS;
            start = now;
          }

          last = now;
        }
        long session = last - start;
        if (session == 0) {
          session = TEN_SECONDS;
        }
        dur += session;// / MIN_MILLIS;
        Long aLong = weekToDur.get(week);
        weekToDur.put(week, aLong == null ? dur : aLong + dur);
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
        "DECEMBER",
        "UNDECEMBER").get(i);
  }
}
