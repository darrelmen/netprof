package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.instrumentation.EventDAO;
import mitll.langtest.server.mail.EmailHelper;
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

  private static final String NP_SERVER = EmailHelper.NP_SERVER;
  private static final String MY_EMAIL = EmailHelper.MY_EMAIL;

  private static final int MIN_MILLIS = (1000 * 60);
  private static final int TEN_SECONDS = 1000 * 10;
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
   */
  public void doReport(ServerProperties serverProps, String site, MailSupport mailSupport,
                       PathHelper pathHelper) {
    Calendar calendar = new GregorianCalendar();
    int i = calendar.get(Calendar.DAY_OF_WEEK);
    List<String> reportEmails = serverProps.getReportEmails();
    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM_dd_yy");
    String today = simpleDateFormat2.format(new Date());

    logger.debug("Site real path " + site);
    String suffix = "";
    if (site != null && site.contains("npfClassroom")) {
      site = site.substring(site.indexOf("npfClassroom"));
      suffix = " at " + site;
    }
    String subject = "Weekly Usage Report for " + serverProps.getLanguage() + suffix;
    if (i == Calendar.MONDAY && !reportEmails.isEmpty()) {
      File file = getReportFile(pathHelper, today);
      //logger.debug("checking file " + file.getAbsolutePath());
      if (file.exists()) {
        logger.debug("already did report for " + today + " : " + file.getAbsolutePath());
      } else {
        try {
          BufferedWriter writer = new BufferedWriter(new FileWriter(file));
          String message = doReport();
          writer.write(message);
          writer.close();
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

  private File getReportFile(PathHelper pathHelper, String today) {
    File reports = pathHelper.getAbsoluteFile("reports");
    if (!reports.exists()) {
      // logger.debug("making dir " + reports.getAbsolutePath());
      reports.mkdirs();
    } else {
      // logger.debug("reports dir exists at " + reports.getAbsolutePath());
    }
    String fileName = "report_" + today + ".html";
    return new File(reports, fileName);
  }

  /**
   * @return
   * @see DatabaseImpl#doReport()
   * @see mitll.langtest.server.LangTestDatabaseImpl#doReport()
   */
  public String doReport() {
    List<User> users = userDAO.getUsers();

    Calendar calendar = new GregorianCalendar();
    Date january1st = getJanuaryFirst(calendar);

    int ytd = 0;

    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM/dd/yy h:mm aaa");
    Map<Integer, Integer> monthToCount = new TreeMap<Integer, Integer>();
    Map<Integer, Integer> weekToCount = new TreeMap<Integer, Integer>();
    for (User user : users) {
      try {
        if (user.getTimestamp().isEmpty()) continue;
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
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
//    logger.debug("ytd " +ytd);
//    logger.debug("month " +monthToCount);
//    logger.debug("week " +weekToCount);

    StringBuilder builder = new StringBuilder();
    String users1 = "New Users";
    builder.append("<html><head><body>" +
            //"<div style=\"width:100%\">" +"<div style=\"position: relative; width:25%\">"+
            //"\n<table>" +  "<tr><td>"+
            getYTD(ytd, users1) +
            //"</td>" +  "<td>\n"+
            // "</div>"+"<div style=\"position: relative; width:25%\">"+
            getMC(monthToCount, "month", "New Users") +
            //"</td><td>"+
            //"</div>"+"<div style=\"position: relative; width:25%\">"+
            getWC(weekToCount, "week", "New Users")
        // +        "</td></tr></table>"

        //    +"</div>"+"</div>"
    );
    getEvents(builder);
    getResults(builder);

    builder.append("</body></head></html>");
    return builder.toString();
  }

  private String getYTD(int ytd, String users1) {
    return "<table>" +
        "<tr>" +
        "<th>" +
        users1 +
        " YTD</th>" + "</tr>" +
        "<tr>" +
        "<td>" + ytd +
        "</td>" + "</tr>" +
        "</table><br/>\n";
  }

/*  private String getMC(Map<Integer, ?> monthToCount, String unit) {
    String count = "Count";
    return getMC(monthToCount, unit, count);
  }*/

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

/*
  private String getWC(Map<?, ?> monthToCount, String unit) {
    String count = "Count";
    return getWC(monthToCount, unit, count);
  }
*/

  private String getWC(Map<?, ?> monthToCount, String unit, String count) {
    String s = "";
    for (Map.Entry<?, ?> pair : monthToCount.entrySet()) {
      Object value = pair.getValue();
      if (value instanceof Collection<?>) {
        value = ((Collection<?>) value).size();
      }
      s += "<tr><td>" + pair.getKey() + "</td><td>" + value + "</td></tr>";
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

  private void getResults(StringBuilder builder) {
    Calendar calendar = new GregorianCalendar();
    Date january1st = getJanuaryFirst(calendar);

    int ytd = 0;

    List<Result> results = resultDAO.getResults();
    Map<String, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio();

    Map<Integer, Integer> monthToCount = new TreeMap<Integer, Integer>();
    Map<Integer, Integer> weekToCount = new TreeMap<Integer, Integer>();

    List<Result> refAudio = new ArrayList<Result>();
    for (Result result : results) {
      if (result.getTimestamp() > january1st.getTime()) {
        boolean skip = isRefAudioResult(exToAudio, result);
        if (skip) {
          refAudio.add(result);
        } else {
          ytd++;
          tallyByMonthAndWeek(calendar, monthToCount, weekToCount, result);
        }
      }
    }
    //  logger.debug("ytd " + ytd);
    //  logger.debug("month " + monthToCount);
    //  logger.debug("week " + weekToCount);
    // logger.debug("ref " + refSkip);
    String recordings = "Recordings";
    builder.append(getYTD(ytd, recordings) +
        getMC(monthToCount, "month", recordings) +
        getWC(weekToCount, "week", recordings));

    monthToCount = new TreeMap<Integer, Integer>();
    weekToCount = new TreeMap<Integer, Integer>();
    ytd = 0;
    for (Result result : refAudio) {
      if (result.getTimestamp() > january1st.getTime()) {
        ytd++;
        tallyByMonthAndWeek(calendar, monthToCount, weekToCount, result);
      }
    }

    String refAudioRecs = "Ref Audio Recordings";
    builder.append(getYTD(ytd, refAudioRecs) +
        getMC(monthToCount, "month", refAudioRecs) +
        getWC(weekToCount, "week", refAudioRecs));

  }

  private void tallyByMonthAndWeek(Calendar calendar, Map<Integer, Integer> monthToCount, Map<Integer, Integer> weekToCount, Result result) {
    calendar.setTimeInMillis(result.getTimestamp());
    int month = calendar.get(Calendar.MONTH);
    Integer integer = monthToCount.get(month);
    monthToCount.put(month, (integer == null) ? 1 : integer + 1);

    int w = calendar.get(Calendar.WEEK_OF_YEAR);
    Integer integer2 = weekToCount.get(w);

    weekToCount.put(w, (integer2 == null) ? 1 : integer2 + 1);
  }

  private boolean isRefAudioResult(Map<String, List<AudioAttribute>> exToAudio, Result result) {
    boolean skip = false;
    List<AudioAttribute> audioAttributes = exToAudio.get(result.getExerciseID());
    if (audioAttributes != null) {
      for (AudioAttribute audioAttribute : audioAttributes) {
        if (audioAttribute.getDuration() == result.getDurationInMillis()) {
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

/*
   private String normalize(String audioRef) {
    int answer = audioRef.indexOf("regular_");
    if (answer != -1)
      audioRef = audioRef.substring(answer + "regular_".length());

    answer = audioRef.indexOf("slow_");
    if (answer != -1)
      audioRef = audioRef.substring(answer + "slow_".length());
    if (audioRef.endsWith(".wav")) {
      audioRef = audioRef.substring(0, audioRef.length() - 4);
    }

    return audioRef;
  }

  private String normalize2(String audioRef) {
    int answer = audioRef.indexOf("answer_");
    if (answer != -1) {
      audioRef = audioRef.substring(answer + "answer_".length());
    }
    if (audioRef.endsWith(".wav")) {
      audioRef = audioRef.substring(0, audioRef.length() - 4);
    }
    return audioRef;
  }
*/

  private Date getJanuaryFirst(Calendar calendar) {
    int year = calendar.get(Calendar.YEAR);
    // logger.debug("year " + year);
    calendar.set(Calendar.YEAR, year);
    calendar.set(Calendar.DAY_OF_YEAR, 1);
    Date january1st = calendar.getTime();
    //   logger.debug("jan first " + january1st);
    return january1st;
  }


  private void getEvents(StringBuilder builder) {
    Calendar calendar = new GregorianCalendar();
    Date january1st = getJanuaryFirst(calendar);

    int ytd = 0;

    List<Event> all = eventDAO.getAll();
    Map<Integer, Set<Long>> monthToCount = new TreeMap<Integer, Set<Long>>();

    Map<Integer, Map<Long, Set<Event>>> monthToCount2 = new TreeMap<Integer, Map<Long, Set<Event>>>();
    Map<Integer, Map<Long, Set<Event>>> weekToCount2 = new TreeMap<Integer, Map<Long, Set<Event>>>();

    Map<Integer, Set<Long>> weekToCount = new TreeMap<Integer, Set<Long>>();

    for (Event event : all) {
      if (event.getTimestamp() > january1st.getTime()) {
        ytd++;
        calendar.setTimeInMillis(event.getTimestamp());

        // months
        int month = calendar.get(Calendar.MONTH);
        // String month1 = getMonth(i);
        long creatorID = event.getCreatorID();
        Set<Long> users = monthToCount.get(month);
        if (users == null) {
          monthToCount.put(month, users = new HashSet<Long>());
        }
        users.add(creatorID);


        Map<Long, Set<Event>> userToEvents = monthToCount2.get(month);
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
    }
    //logger.debug("ytd " + ytd);

    // logger.debug("month " + monthToCount);
    // logger.debug("week " + weekToCount);

    builder.append(//getYTD(ytd, "Active Users") +
        getMC(monthToCount, "month", "# Active Users") +
            getWC(weekToCount, "week", "# Active Users"));

    Map<Integer, Long> monthToDur = getMonthToDur(monthToCount2);
    //  logger.debug("month to dur " + monthToDur);
    long total = 0;
    for (Long v : monthToDur.values()) total += v;

    total /= MIN_MILLIS;

    Map<Integer, Long> weekToDur = getWeekToDur(weekToCount2);
    //logger.debug("week to dur " + weekToDur);

    getMinMap(monthToDur);

    builder.append(getYTD(Math.round(total / 60), "Total time on task (hours)") +
        getMC(getMinMap(monthToDur), "month", "Time on Task Minutes") +
        getWC(getMinMap(weekToDur), "week", "Time on Task Minutes"));
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
        //logger.debug("\tuser " + user);
        long start = 0;
        long dur = 0;
        // long begin = 0;
        long last = 0;
        Event sevent = null, levent = null;
        for (Event event : eventsForUser.getValue()) {
      /*    if (eventsForUser.getKey() > 36) {
            logger.debug("event " +event);
          }*/
          long now = event.getTimestamp();
          //     if (user == INTTEST) {
          //     logger.debug("Event " + event);
          //  }
          if (start == 0) {
            start = now;
            sevent = event;
          } else if (now - last > 1000 * 300) {
            long session = (last - start);
            if (session == 0) {
/*              logger.warn("huh " +last + " " + start);
              logger.warn("huh sevent " +sevent);
              logger.warn("huh levent " +levent);
              logger.warn("huh event " +event);*/
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
/*
          logger.warn("huh 2 " +last + " " + start);
          logger.warn("huh 2 sevent " +sevent);
          logger.warn("huh 2 levent " +levent);
*/
          // logger.warn("huh event " +event);
          session = TEN_SECONDS;

        }
        dur += session;
//        if (user == INTTEST) {
        //        logger.debug("dur " + dur);
        //     }
        Long aLong = monthToDur.get(month);

        //  dur /= MIN_MILLIS;
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
        // long begin = 0;
        long last = 0;
        for (Event event : eventsForUser.getValue()) {
          long now = event.getTimestamp();
   /*       if (user == INTTEST) {
            logger.debug("Event " + event);
          }
      */
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
//        if (user == INTTEST) {
        //        logger.debug("dur " + dur);
        //     }
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
