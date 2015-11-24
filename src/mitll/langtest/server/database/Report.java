/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.instrumentation.EventDAO;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;
import mitll.langtest.shared.UserAndTime;
import mitll.langtest.shared.instrumentation.Event;
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
  public static final boolean CLEAR_DAY_HOUR_MINUTE = true;
  private static final boolean WRITE_RESULTS_TO_FILE = false;
  private static final String ACTIVE_USERS = "Active Users";
  private static final String TIME_ON_TASK_MINUTES = "Time on Task Minutes ";
  private static final String TOTAL_TIME_ON_TASK_HOURS = "Total time on task (hours)";
  private static final String MONTH = "By Month";
  private static final String WEEK = "By Week";
  private static final String top = "<td style='vertical-align: top;'>";
  public static final String DEVICE_RECORDINGS = "Device Recordings";
  public static final String ALL_RECORDINGS = "All Recordings";
  public static final String MM_DD_YY = "MM_dd_yy";
//  public static final String MM_DD_YY_HH_MM_SS = "MM_dd_yy_hh_mm_ss";
  public static final boolean SHOW_TEACHER_SKIPS = false;
  public static final boolean DO_2014 = false;

  private final UserDAO userDAO;
  private final ResultDAO resultDAO;
  private final EventDAO eventDAO;
  private final AudioDAO audioDAO;
  String prefix;
  String language;
  BufferedWriter csv;
  private Map<Long, Long> userToStart = new HashMap<>();
  private static final boolean DEBUG = false;

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
      writeAndSendReport(serverProps.getLanguage(), site, mailSupport, pathHelper, reportEmails);
    } else {
//      logger.debug("not sending email report since this is not monday");
    }
  }

  private void writeAndSendReport(String language, String site, MailSupport mailSupport,
                                  PathHelper pathHelper, List<String> reportEmails) {
    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM_dd_yy");
    String today = simpleDateFormat2.format(new Date());
    File file = getReportFile(pathHelper, today, language);
    //logger.debug("checking file " + file.getAbsolutePath());
    if (file.exists()) {
      logger.debug("already did report for " + today + " : " + file.getAbsolutePath());
    } else {
      logger.debug("Site real path " + site);
      try {
        String message = writeReport(file, pathHelper, language);
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
  public void writeReport(PathHelper pathHelper, String language) throws IOException {
    File file = getReportPath(pathHelper, language);
    writeReport(file, pathHelper, language);
    logger.debug("wrote to " + file.getAbsolutePath());
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


  private String writeReport(File file, PathHelper pathHelper, String language) throws IOException {
    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
    String message = doReport(pathHelper, language);
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
   * @return
   * @see #writeReport
   */
  private String doReport(PathHelper pathHelper, String language) {
    StringBuilder builder = new StringBuilder();
    builder.append("<html><head><body>");

    setUserStart();
    File reportPath = getReportPath(pathHelper, language);
    String s = reportPath.getAbsolutePath().replaceAll(".html", ".csv");
    try {
      this.csv = new BufferedWriter(new FileWriter(s));
    } catch (IOException e) {
      logger.error("got " + e, e);
    }

    Set<Long> users = getUsers(builder, language);
    getUsers(builder, userDAO.getUsersDevices(), "New iPad/iPhone Users", language);

    Set<Long> events = getEvents(builder, users);
    Set<Long> eventsDevices = getEventsDevices(builder, users);

    events.addAll(eventsDevices);

    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM/dd/yy");
    String start = simpleDateFormat2.format(getJanuaryFirst(getCal()));
    String end = simpleDateFormat2.format(getJanuaryFirstNextYear());

    builder.append("<h2>Unique Active Users between " + start + " and " + end +
        "</h2>" +
        "<table ><tr>" +
        top + events.size() + "</td>" +
        "</tr></table>");

    getResults(builder, users, pathHelper, language);
    getResultsDevices(builder, users, pathHelper, language);

    Collection<AudioAttribute> audioAttributes = audioDAO.getAudioAttributes();
    // logger.debug("got " + audioAttributes.size() + " audio attributes.");

    Calendar calendar = getCal();
    Date january1st = getJanuaryFirst(calendar);
    Date january1stNextYear = getJanuaryFirstNextYear();

    if (DEBUG) logger.info("doReport : between " + january1st + " and " + january1stNextYear);

    addRefAudio(builder, calendar, january1st, january1stNextYear, audioAttributes, language);

    builder.append("</body></head></html>");
    return builder.toString();
  }

  public int getActiveUsersYTD() {
    StringBuilder builder = new StringBuilder();
    builder.append("<html><head><body>");

    Set<Long> users = getUsers(builder, language);
    //   getUsers(builder, userDAO.getUsersDevices(), "New iPad/iPhone Users");

    Set<Long> events = getEvents(builder, users);
    Set<Long> eventsDevices = getEventsDevices(builder, users);

    events.addAll(eventsDevices);

    return events.size();
  }

  /**
   * @param builder
   * @return
   * @see #doReport
   */
  private Set<Long> getUsers(StringBuilder builder, String language) {
    List<User> users = userDAO.getUsers();
    for (User user : users) {
      Long aLong = userToStart.get(user.getId());
      if (aLong != null) user.setTimestampMillis(aLong);
      else {
        //  logger.error("no events for " + user.getId());
      }
    }
    String users1 = "All New Users";// (users enrolled after 10/8)";

    return getUsers(builder, users, users1, language);
  }

  private Set<Long> getUsers(StringBuilder builder, List<User> users, String users1, String language) {
    Calendar calendar = getCal();
    Date january1st = getJanuaryFirst(calendar);
    Date january1stNextYear = getJanuaryFirstNextYear();

   // logger.info("getUsers between " + january1st + " and " + january1stNextYear);

    int ytd = 0;

    Map<Integer, Integer> monthToCount = new TreeMap<Integer, Integer>();
    Map<Integer, Integer> weekToCount = new TreeMap<Integer, Integer>();
    Set<Long> students = new HashSet<Long>();

    Set<String> lincoln = new HashSet<>(Arrays.asList("gvidaver", "rbudd", "jmelot", "esalesky", "gatewood",
        "testing", "grading", "fullperm", "0001abcd", "egodoy",
        "rb2rb2",
        "dajone3",
        "WagnerSandy",
        "rbtrbt"));

    for (User user : users) {
      boolean isStudent = (user.getAge() == 89 && user.getUserID().isEmpty()) || user.getAge() == 0 || user.getUserKind() == User.Kind.STUDENT;

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
          // if (month >= 11) {
         // logger.warn(user.getUserID() + "\ton\t" + month);
          // }
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
    builder.append(getSectionReport(ytd, monthToCount, weekToCount, users1, language));

//    logger.info("Students " + students);
    return students;
  }

  private String getSectionReport(int ytd, Map<Integer, ?> monthToCount, Map<Integer, ?> weekToCount, String users1,
                                  String language) {
    String yearCol = ytd > -1 ? getYTD(ytd, users1) : "";
    String monthCol = getMC(monthToCount, MONTH, users1, "");

    //writeMonth(monthToCount, users1, language);
    String weekCol = getWC(weekToCount, WEEK, users1);
    return getYearMonthWeekTable(users1, yearCol, monthCol, weekCol);
  }

  private void writeMonth(Map<Integer, ?> monthToCount, String users1, String language) {
    StringBuilder builder = new StringBuilder();
    int i = getYear();

    String otherPrefix = this.prefix;
    String prefix = otherPrefix + "," + language + "," + i + "," + users1 + ",";
    builder.append(prefix);

//    if (false) {
//      for (int j = 0; j < 12; j++) {
//        String month = getMonth(j);
//        builder.append(month + ",");
//      }
//      builder.append("\n");
//
//      builder.append(prefix);
//    }
    for (int j = 0; j < 12; j++) {
      Object o = monthToCount.get(j);
      //  if (o == null) logger.error("no data for " +j);
      Object o1 = o == null ? "0" : o;
      if (o1 instanceof Collection<?>) {
        o1 = ((Collection<?>) o1).size();
      }
      builder.append(o1 + ",");
    }
    builder.append("\n");

    if (DEBUG) logger.info(builder.toString());

    try {
      csv.write(builder.toString());
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

  private String getYTD(int ytd, String users1) {
    int i = getYear();
    return "<table style='background-color: #eaf5fb'>" +
        "<tr>" +
        "<th>" +
        users1 +
        " YTD (" + i +
        ")</th>" + "</tr>" +
        "<tr>" +
        "<td>" + ytd +
        "</td>" + "</tr>" +
        "</table><br/>\n";
  }


  private String getMC(Map<Integer, ?> monthToCount, String unit, String count, String tableLabel) {
    writeMonth(monthToCount, tableLabel.isEmpty() ? count : tableLabel, language);

    String s = "";
    for (Map.Entry<Integer, ?> pair : monthToCount.entrySet()) {
      Object value = pair.getValue();
      if (value instanceof Collection<?>) {
        value = ((Collection<?>) value).size();
      }
      Integer key = pair.getKey();
      String month = getMonth(key);
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
  private String getWC(Map<Integer, ?> weekToCount, String unit, String count) {
    String s = "";
    Calendar calendar = getCal();

    String format = "MM-dd";

    SimpleDateFormat df = new SimpleDateFormat(format);

    for (Map.Entry<Integer, ?> pair : weekToCount.entrySet()) {
      Object value = pair.getValue();
      if (value instanceof Collection<?>) {
        value = ((Collection<?>) value).size();
      }
      Integer week = pair.getKey();
      calendar.set(Calendar.WEEK_OF_YEAR, week);
      // calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
      Date time = calendar.getTime();
      String format1 = df.format(time);
      s += "<tr><td>" +
          //week +
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
   * @param language
   * @see #doReport
   */
  private Map<Long, Map<String, Integer>> getResults(StringBuilder builder, Set<Long> students, PathHelper pathHelper, String language) {
    List<Result> results = resultDAO.getResults();
    return getResultsForSet(builder, students, pathHelper, results, ALL_RECORDINGS, language);
  }

  private Map<Long, Map<String, Integer>> getResultsDevices(StringBuilder builder, Set<Long> students, PathHelper pathHelper, String language) {
    List<Result> results = resultDAO.getResultsDevices();
    return getResultsForSet(builder, students, pathHelper, results, DEVICE_RECORDINGS, language);
  }

  private Map<Long, Map<String, Integer>> getResultsForSet(StringBuilder builder, Set<Long> students,
                                                           PathHelper pathHelper, List<Result> results, String recordings, String language) {
    Calendar calendar;
    Date january1st = getJanuaryFirst(getCal());
    Date january1stNextYear = getJanuaryFirstNextYear();
  //  logger.info("between " + january1st + " and " + january1stNextYear);

    int ytd = 0;

    Map<String, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio();

    //logger.debug("found " + exToAudio.size() + " ref audio exercises");

    Map<Integer, Integer> monthToCount = new TreeMap<Integer, Integer>();
    Map<Integer, Integer> weekToCount = new TreeMap<Integer, Integer>();
    Map<Long, Map<String, Integer>> userToDayToCount = new TreeMap<Long, Map<String, Integer>>();

    int teacherAudio = 0;
    int invalid = 0;
    int invalidScore = 0;

    int beforeJanuary = 0;
    Set<Long> skipped = new TreeSet<>();
    try {
      SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM_dd_yy");
      String today = simpleDateFormat2.format(new Date());

      BufferedWriter writer = null;

      if (WRITE_RESULTS_TO_FILE) {
        File file = getReportFile(pathHelper, today + "_all", language);
        writer = new BufferedWriter(new FileWriter(file));
      }

      teacherAudio = 0;
      invalid = 0;
      calendar = getCal();

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

    if (DEBUG) logger.debug("Skipped " + invalid + " invalid recordings, " + invalidScore + " -1 score items, " + beforeJanuary + " beforeJan1st");
    if (teacherAudio > 0) {
      if (DEBUG) logger.debug("skipped " + teacherAudio + " teacher recordings by " + skipped);
    }
    //logger.debug("userToDayToCount " + userToDayToCount.size());

    builder.append("\n<br/><span>Valid student recordings</span>");
    builder.append(
        getSectionReport(ytd, monthToCount, weekToCount, recordings, language)
    );


    return userToDayToCount;
  }

  /**
   * @param builder
   * @param calendar
   * @param january1st
   * @param refAudio
   * @param language
   * @see #getResults(StringBuilder, Set, PathHelper, String)
   */
  private <T extends UserAndTime> void addRefAudio(StringBuilder builder, Calendar calendar, Date january1st,
                                                   Date january1stThisYear, Collection<T> refAudio, String language) {
    int ytd = 0;
    Map<Integer, Integer> monthToCount = new TreeMap<Integer, Integer>();
    Map<Integer, Integer> weekToCount = new TreeMap<Integer, Integer>();
    Map<Long, Map<String, Integer>> userToDayToCount = new TreeMap<Long, Map<String, Integer>>();
    long time = january1st.getTime();
    long time1 = january1stThisYear.getTime();

    for (T result : refAudio) {
      long timestamp = result.getTimestamp();
      if (timestamp > time &&
          timestamp < time1
          ) {
        ytd++;
        tallyByMonthAndWeek(calendar, monthToCount, weekToCount, result, userToDayToCount);
      }
    }

    String refAudioRecs = "Ref Audio Recordings";
    builder.append(getSectionReport(ytd, monthToCount, weekToCount, refAudioRecs, language));
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
    if (dayToCount == null) userToDayToCount.put(userid, dayToCount = new TreeMap<String, Integer>());
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
   * @see #getResults(StringBuilder, Set, PathHelper, String)
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
  private Date getJanuaryFirst(Calendar calendar) {
    int year = getYear2(Calendar.getInstance());
    // logger.debug("year " + year);
    calendar.set(Calendar.YEAR, year);
    calendar.set(Calendar.DAY_OF_YEAR, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    return calendar.getTime();
  }

  private int getYear() {
    return DO_2014 ? 2014 : getCal().get(Calendar.YEAR);
  }

  private Calendar getCal() {
    Calendar instance = Calendar.getInstance();
    instance.clear();
    int year2 = getYear2(instance);
    //  logger.info("getCal year " + year2);
    instance.set(Calendar.YEAR, year2);
    return instance;
  }

  private int getYear2(Calendar calendar) {
    return DO_2014 ? 2014 : Calendar.getInstance().get(Calendar.YEAR);
  }

  private Date getJanuaryFirstNextYear() {
    Calendar instance = Calendar.getInstance();
    int year = getYear2(instance);
    // logger.debug("year " + year);
    instance.set(Calendar.YEAR, year);
    if (CLEAR_DAY_HOUR_MINUTE) {
      instance.set(Calendar.DAY_OF_YEAR, 1);
      instance.set(Calendar.HOUR_OF_DAY, 0);
      instance.set(Calendar.MINUTE, 0);
      instance.set(Calendar.YEAR, year + 1);
    } else {

    }
    return instance.getTime();
  }

  /**
   * @param builder
   * @param students
   * @see #doReport(PathHelper, String)
   */
  private Set<Long> getEvents(StringBuilder builder, Set<Long> students) {
    List<Event> all = eventDAO.getAll();
    String activeUsers = ACTIVE_USERS;
    String tableLabel = "Time on Task";
//    for (Event event : all) {
//      long creatorID = event.getCreatorID();
//      long timestamp = event.getTimestamp();
//      if (!userToStart.containsKey(creatorID) || timestamp < userToStart.get(creatorID)) {
//        userToStart.put(creatorID, timestamp);
//      }297
//    }
    return getEvents(builder, students, all, activeUsers, tableLabel, language);
  }

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

  private Set<Long> getEventsDevices(StringBuilder builder, Set<Long> students) {
    List<Event> all = eventDAO.getAllDevices();
    String activeUsers = "Active iPad/iPhone Users";
    String tableLabel = "iPad/iPhone Time on Task";
    return getEvents(builder, students, all, activeUsers, tableLabel, language);
  }

  /**
   * @param builder
   * @param students
   * @param all
   * @param activeUsers
   * @param tableLabel
   * @param language
   * @see #getEvents(StringBuilder, Set)
   */
  private Set<Long> getEvents(StringBuilder builder, Set<Long> students, List<Event> all, String activeUsers,
                              String tableLabel, String language) {
    Map<Integer, Set<Long>> monthToCount = new TreeMap<Integer, Set<Long>>();
    Map<Integer, Map<Long, Set<Event>>> monthToCount2 = new TreeMap<Integer, Map<Long, Set<Event>>>();
    Map<Integer, Map<Long, Set<Event>>> weekToCount2 = new TreeMap<Integer, Map<Long, Set<Event>>>();

    Map<Integer, Set<Long>> weekToCount = new TreeMap<Integer, Set<Long>>();
    Set<Long> teachers = new HashSet<Long>();
    int skipped = 0;

    Calendar calendar = getCal();
    Date january1st = getJanuaryFirst(calendar);
    Date january1stThisYear = getJanuaryFirstNextYear();
    if (DEBUG)  logger.info("getEvents from " + january1st + " to " + january1stThisYear);
    long time = january1st.getTime();
    long time1 = january1stThisYear.getTime();

//    logger.info("students " + students);

    Set<Long> users = new HashSet<>();

    for (Event event : all) {
      long creatorID = event.getCreatorID();
      long timestamp = event.getTimestamp();
      if (timestamp > time &&
          timestamp < time1 &&
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

    ArrayList<Long> longs = new ArrayList<>(users);
    Collections.sort(longs);
    logger.debug(activeUsers + " getEvents skipped " + skipped + " events from teachers " + teachers + "\nusers " + longs);

    builder.append(getSectionReport(-1, monthToCount, weekToCount, activeUsers, language));

    Map<Integer, Long> monthToDur = getMonthToDur(monthToCount2);
    long total = 0;
    for (Long v : monthToDur.values()) total += v;

    total /= MIN_MILLIS;

    Map<Integer, Long> weekToDur = getWeekToDur(weekToCount2);

    getMinMap(monthToDur);

    String timeOnTaskMinutes = TIME_ON_TASK_MINUTES;
    String yearMonthWeekTable = getYearMonthWeekTable(tableLabel,
        getYTD(Math.round(total / 60), TOTAL_TIME_ON_TASK_HOURS),
        getMC(getMinMap(monthToDur), MONTH, timeOnTaskMinutes, tableLabel),
        getWC(getMinMap(weekToDur), WEEK, timeOnTaskMinutes)
    );
    builder.append(yearMonthWeekTable);


    return users;
  }

  private boolean isValidUser(long creatorID) {
    return creatorID != 1;
  }

  private void statsForEvent(Calendar calendar, Map<Integer, Set<Long>> monthToCount,
                             Map<Integer, Map<Long, Set<Event>>> monthToCount2,
                             Map<Integer, Map<Long, Set<Event>>> weekToCount2,
                             Map<Integer, Set<Long>> weekToCount, Event event, long creatorID) {
    calendar.setTimeInMillis(event.getTimestamp());

    // months
    int month = calendar.get(Calendar.MONTH);

    Map<Long, Set<Event>> userToEvents = monthToCount2.get(month);
    Set<Long> users = monthToCount.get(month);
    if (users == null) {
      monthToCount.put(month, users = new HashSet<Long>());
    }
    users.add(creatorID);
    if (userToEvents == null) {
      monthToCount2.put(month, userToEvents = new HashMap<Long, Set<Event>>());
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

  private Map<Integer, Long> getMinMap(Map<Integer, Long> monthToDur) {
    Map<Integer, Long> copy = new TreeMap<Integer, Long>();
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
    Map<Integer, Long> monthToDur = new TreeMap<Integer, Long>();
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
    Map<Integer, Long> weekToDur = new TreeMap<Integer, Long>();
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
