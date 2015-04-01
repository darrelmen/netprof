package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.instrumentation.EventDAO;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;
import mitll.langtest.shared.instrumentation.Event;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
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
  public static final boolean WRITE_RESULTS_TO_FILE = false;
  public static final String ACTIVE_USERS = "# Active Users";
  public static final String TIME_ON_TASK_MINUTES = "Time on Task Minutes ";
  public static final String TOTAL_TIME_ON_TASK_HOURS = "Total time on task (hours)";
  public static final String MONTH = "month";
  public static final String WEEK = "week";
  private final UserDAO userDAO;
  private final ResultDAO resultDAO;
  private final EventDAO eventDAO;
  private final AudioDAO audioDAO;

  public Report(UserDAO userDAO, ResultDAO resultDAO, EventDAO eventDAO, AudioDAO audioDAO) {
    this.userDAO = userDAO;
    this.resultDAO = resultDAO;
    this.eventDAO = eventDAO;
    this.audioDAO = audioDAO;
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
    Calendar calendar = Calendar.getInstance();
    int i = calendar.get(Calendar.DAY_OF_WEEK);
    List<String> reportEmails = serverProps.getReportEmails();
    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM_dd_yy");
    String today = simpleDateFormat2.format(new Date());

    String suffix = "";
    if (site != null && site.contains("npfClassroom")) {
      site = site.substring(site.indexOf("npfClassroom"));
      suffix = " at " + site;
    }
    String subject = "Weekly Usage Report for " + serverProps.getLanguage() + suffix;

    // check if it's a monday
    if (i == Calendar.MONDAY
        && !reportEmails.isEmpty()) {
      File file = getReportFile(pathHelper, today);
      //logger.debug("checking file " + file.getAbsolutePath());
      if (file.exists()) {
        logger.debug("already did report for " + today + " : " + file.getAbsolutePath());
      } else {
        logger.debug("Site real path " + site);
        try {
          String message = writeReport(file, pathHelper);
          for (String dest : reportEmails) {
            mailSupport.sendEmail(NP_SERVER, dest, MY_EMAIL, subject, message);
          }
        } catch (IOException e) {
          logger.error("got " + e, e);
        }
      }
    } else {
      logger.debug("not sending email report since not Monday");
    }
  }

  public void writeReport(PathHelper pathHelper) throws IOException {
    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM_dd_yy");
    String today = simpleDateFormat2.format(new Date());
    File file = getReportFile(pathHelper, today);
    writeReport(file, pathHelper);
    logger.debug("wrote to " + file.getAbsolutePath());
  }

  private String writeReport(File file, PathHelper pathHelper) throws IOException {
    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
    String message = doReport(pathHelper);
    writer.write(message);
    writer.close();
    return message;
  }

  public File getReportFile(PathHelper pathHelper, String today) {
    File reports = pathHelper.getAbsoluteFile("reports");
    //File test = new File("reports");
    if (!reports.exists()) {
      logger.debug("making dir " + reports.getAbsolutePath());
      reports.mkdirs();
    } else {
      logger.debug("reports dir exists at " + reports.getAbsolutePath());
    }
    String fileName = "report_" + today + ".html";
    return new File(reports, fileName);
  }

  /**
   * @return
   * @see #writeReport
   */
  private String doReport(PathHelper pathHelper) {
    StringBuilder builder = new StringBuilder();

    Set<Long> users = getUsers(builder);
    getEvents(builder, users);
    getResults(builder, users, pathHelper);

    builder.append("</body></head></html>");
    return builder.toString();
  }

  public Map<Long, Map<String, Integer>> getUserToDayToRecordings(PathHelper pathHelper) {
    return getResults(new StringBuilder(), getUsers(new StringBuilder()), pathHelper);
  }

  /**
   * @param builder
   * @return
   * @see #doReport
   * @see #getUserToDayToRecordings
   */
  private Set<Long> getUsers(StringBuilder builder) {
    List<User> users = userDAO.getUsers();
    Calendar calendar = getCal();
    Date january1st = getJanuaryFirst(calendar);

    int ytd = 0;

    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM/dd/yy h:mm aaa");
    Map<Integer, Integer> monthToCount = new TreeMap<Integer, Integer>();
    Map<Integer, Integer> weekToCount = new TreeMap<Integer, Integer>();
    Set<Long> students = new HashSet<Long>();
    for (User user : users) {
      try {
        boolean isStudent = (user.getAge() == 89 && user.getUserID().isEmpty()) || user.getAge() == 0;

        if (user.getTimestamp().isEmpty()) continue;
        if (isStudent) {
          students.add(user.getId());
          Date parse = simpleDateFormat2.parse(user.getTimestamp());
          if (parse.getTime() > january1st.getTime()) {
            ytd++;

            calendar.setTime(parse);
            int month = calendar.get(Calendar.MONTH);
            Integer integer = monthToCount.get(month);
            monthToCount.put(month, (integer == null) ? 1 : integer + 1);

            int w = calendar.get(Calendar.WEEK_OF_YEAR);
            Integer integer2 = weekToCount.get(w);

            weekToCount.put(w, (integer2 == null) ? 1 : integer2 + 1);
          } else {
            //   logger.debug("NO time " +user.getTimestamp() + " " + parse);
          }
        } else {
          //logger.warn("skipping teacher " + user);
        }
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    String users1 = "New Users";// (users enrolled after 10/8)";
    builder.append("<html><head><body>" +
            getYTD(ytd, users1) +
            getMC(monthToCount, MONTH, users1) +
            getWC(weekToCount, WEEK, users1)
    );

    return students;
  }

  private String getYTD(int ytd, String users1) {
    int i = getCal().get(Calendar.YEAR);
    return "<table>" +
        "<tr>" +
        "<th>" +
        users1 +
        " YTD (" +i+
        ")</th>" + "</tr>" +
        "<tr>" +
        "<td>" + ytd +
        "</td>" + "</tr>" +
        "</table><br/>\n";
  }

  private String getMC(Map<Integer, ?> monthToCount, String unit, String count) {
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
    return "<table>" +
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
    return "<table>" +
        "<tr>" +
        "<th>" +
        unit +
        "</th>" +
        "<th>" + count + "</th>" +
        "</tr>" +
        s +
        "</table><br/>\n";
  }

  private Calendar getCal() {
    Calendar instance = Calendar.getInstance();
    int i = instance.get(Calendar.YEAR);
    instance.clear();
    instance.set(Calendar.YEAR,i);
    return instance;
  }

  /**
   * @param builder
   * @see #doReport
   */
  private Map<Long, Map<String, Integer>> getResults(StringBuilder builder, Set<Long> students, PathHelper pathHelper) {
    Calendar calendar = getCal();
    Date january1st = getJanuaryFirst( getCal());
   // logger.debug("starting at " + january1st);
    int ytd = 0;

    List<Result> results = resultDAO.getResults();
    Map<String, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio();

    Map<Integer, Integer> monthToCount = new TreeMap<Integer, Integer>();
    Map<Integer, Integer> weekToCount = new TreeMap<Integer, Integer>();
    Map<Long, Map<String, Integer>> userToDayToCount = new TreeMap<Long, Map<String, Integer>>();

    List<Result> refAudio = null;
    int teacherAudio = 0;
    int invalid = 0;
    int invalidScore = 0;
    //int scoresByTeachers = 0;
    int beforeJanuary = 0;
    try {
      SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM_dd_yy");
      String today = simpleDateFormat2.format(new Date());
      File file = getReportFile(pathHelper, today + "_all");
//      logger.debug("wrote to " + file.getAbsolutePath());
      BufferedWriter writer = new BufferedWriter(new FileWriter(file));

      refAudio = new ArrayList<Result>();
      teacherAudio = 0;
      invalid = 0;
      calendar = getCal();

      for (Result result : results) {
        if (result.getTimestamp() > january1st.getTime()) {
          if (result.isValid()) {
            if (isRefAudioResult(exToAudio, result)) {
              refAudio.add(result);
            } else {
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
                //} else {
                //  scoresByTeachers++;
                //}
              } else {
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
      writer.close();
    } catch (IOException e) {
      logger.error("got " + e, e);
    }

    logger.debug("Skipped " + invalid + " invalid recordings, " + invalidScore + " -1 score items, " +
        //scoresByTeachers + " scores by teachers(?) " + scoresBeforeOctober + " before 10/8 " +
        beforeJanuary + " beforeJan1st");
    logger.debug("skipped " + teacherAudio + " teacher recordings");
    logger.debug("userToDayToCount " + userToDayToCount);

    String recordings = "Recordings";

    builder.append("\n<br/><span>Valid student recordings</span>");
    builder.append(
        getYTD(ytd, recordings) +
        getMC(monthToCount, MONTH, recordings) +
        getWC(weekToCount, WEEK, recordings));

    addRefAudio(builder, calendar, january1st, refAudio);
    return userToDayToCount;
  }

  private void addRefAudio(StringBuilder builder, Calendar calendar, Date january1st, List<Result> refAudio) {
    int ytd = 0;
    Map<Integer, Integer> monthToCount = new TreeMap<Integer, Integer>();
    Map<Integer, Integer> weekToCount = new TreeMap<Integer, Integer>();
    Map<Long, Map<String, Integer>> userToDayToCount = new TreeMap<Long, Map<String, Integer>>();
    for (Result result : refAudio) {
      if (result.getTimestamp() > january1st.getTime()) {
        ytd++;
        tallyByMonthAndWeek(calendar, monthToCount, weekToCount, result, userToDayToCount);
      }
    }

    String refAudioRecs = "Ref Audio Recordings";
    builder.append(
        getYTD(ytd, refAudioRecs) +
        getMC(monthToCount, MONTH, refAudioRecs) +
        getWC(weekToCount, WEEK, refAudioRecs));
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
                                   Result result,
                                   Map<Long, Map<String, Integer>> userToDayToCount) {
    calendar.setTimeInMillis(result.getTimestamp());
    int month = calendar.get(Calendar.MONTH);
    Integer integer = monthToCount.get(month);
    monthToCount.put(month, (integer == null) ? 1 : integer + 1);

    int w = calendar.get(Calendar.WEEK_OF_YEAR);
    Integer integer2 = weekToCount.get(w);

    Map<String, Integer> dayToCount = userToDayToCount.get(result.getUserid());
    if (dayToCount == null) userToDayToCount.put(result.getUserid(), dayToCount = new TreeMap<String, Integer>());
    String key = calendar.get(Calendar.YEAR) + "," +
        calendar.get(Calendar.MONTH) + "," +
        calendar.get(Calendar.DAY_OF_MONTH);
    Integer countAtDay = dayToCount.get(key);
    dayToCount.put(
        key
        , countAtDay == null ? 1 : countAtDay + 1);
    weekToCount.put(w, (integer2 == null) ? 1 : integer2 + 1);
  }

  private boolean isRefAudioResult(Map<String, List<AudioAttribute>> exToAudio, Result result) {
    boolean skip = false;
    List<AudioAttribute> audioAttributes = exToAudio.get(result.getExerciseID());
    if (audioAttributes != null) {
      for (AudioAttribute audioAttribute : audioAttributes) {
        if (audioAttribute.getDurationInMillis() == result.getDurationInMillis()) {
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

  /**
   * @param calendar
   * @return
   * @see #doReport
   * @see #getEvents
   * @see #getResults
   */
  private Date getJanuaryFirst(Calendar calendar) {
    int year = calendar.get(Calendar.YEAR);
    // logger.debug("year " + year);
    calendar.set(Calendar.YEAR, year);
    calendar.set(Calendar.DAY_OF_YEAR, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    return calendar.getTime();
  }

  /**
   * @param builder
   * @param students
   * @see #doReport
   */
  private void getEvents(StringBuilder builder, Set<Long> students) {
    Calendar calendar = getCal();
    Date january1st = getJanuaryFirst(calendar);

    List<Event> all = eventDAO.getAll();
    Map<Integer, Set<Long>> monthToCount = new TreeMap<Integer, Set<Long>>();

    Map<Integer, Map<Long, Set<Event>>> monthToCount2 = new TreeMap<Integer, Map<Long, Set<Event>>>();
    Map<Integer, Map<Long, Set<Event>>> weekToCount2 = new TreeMap<Integer, Map<Long, Set<Event>>>();

    Map<Integer, Set<Long>> weekToCount = new TreeMap<Integer, Set<Long>>();
    Set<Long> teachers = new HashSet<Long>();
    int skipped = 0;

    for (Event event : all) {
      long creatorID = event.getCreatorID();
      if (event.getTimestamp() > january1st.getTime() && students.contains(creatorID)) {
        if (isValidUser(creatorID)) {
          statsForEvent(calendar, monthToCount, monthToCount2, weekToCount2, weekToCount, event, creatorID);
        }
      } else if (!students.contains(creatorID)) {
        skipped++;
        teachers.add(creatorID);
      }
    }
    logger.debug("skipped  " + skipped + " events from teachers " + teachers);

    String activeUsers = ACTIVE_USERS;
    builder.append(getMC(monthToCount, MONTH, activeUsers) +
        getWC(weekToCount, WEEK, activeUsers));
    Map<Integer, Long> monthToDur = getMonthToDur(monthToCount2);
    long total = 0;
    for (Long v : monthToDur.values()) total += v;

    total /= MIN_MILLIS;

    Map<Integer, Long> weekToDur = getWeekToDur(weekToCount2);
    //logger.debug("week to dur " + weekToDur);

    getMinMap(monthToDur);

    String timeOnTaskMinutes = TIME_ON_TASK_MINUTES;
    builder.append(getYTD(Math.round(total / 60), TOTAL_TIME_ON_TASK_HOURS) +
        getMC(getMinMap(monthToDur), MONTH, timeOnTaskMinutes) +
        getWC(getMinMap(weekToDur), WEEK, timeOnTaskMinutes));
  }

  private boolean isValidUser(long creatorID) {
    return creatorID != 1;
  }

  private void statsForEvent(Calendar calendar, Map<Integer, Set<Long>> monthToCount, Map<Integer, Map<Long, Set<Event>>> monthToCount2, Map<Integer, Map<Long, Set<Event>>> weekToCount2, Map<Integer, Set<Long>> weekToCount, Event event, long creatorID) {
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
        Long user = eventsForUser.getKey();
        long start = 0;
        long dur = 0;
        long last = 0;
        Event sevent = null, levent = null;
        for (Event event : eventsForUser.getValue()) {
          long now = event.getTimestamp();
          if (start == 0) {
            start = now;
            sevent = event;
          } else if (now - last > 1000 * 300) {
            long session = (last - start);
            if (session == 0) {
              session = TEN_SECONDS;
            }
            dur += session;
            start = now;
            sevent = event;
          }

          last = now;
          levent = event;
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
        Long user = eventsForUser.getKey();
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
