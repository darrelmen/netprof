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

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.instrumentation.IEventDAO;
import mitll.langtest.server.database.result.IResultDAO;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.UserAndTime;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.SlickProject;
import mitll.npdata.dao.SlickSlimEvent;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.uadetector.OperatingSystemFamily;
import net.sf.uadetector.ReadableUserAgent;
import net.sf.uadetector.UserAgentStringParser;
import net.sf.uadetector.VersionNumber;
import net.sf.uadetector.service.UADetectorServiceFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/16/14.
 */
public class Report {
  private static final Logger logger = LogManager.getLogger(Report.class);

  private static final String NP_SERVER = "np.ll.mit.edu";
  private static final String MY_EMAIL = "gordon.vidaver@ll.mit.edu";

  private static final int MIN_MILLIS = (1000 * 60);
  private static final int TEN_SECONDS = 1000 * 10;
  // private static final boolean CLEAR_DAY_HOUR_MINUTE = true;
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

  private static final String NEW_I_PAD_I_PHONE_USERS = "New iPad/iPhone Users";
  private static final String TIME_ON_TASK = "Time on Task";
  private static final String MM_DD = "MM-dd";
  private static final String ALL_NEW_USERS = "All New Users";
  private static final String ALL_USERS = "allUsers";
  private static final String I_PAD_USERS = "iPadUsers";
  private static final String OVERALL_TIME_ON_TASK = "overallTimeOnTask";
  private static final String DEVICE_TIME_ON_TASK = "deviceTimeOnTask";
  //  private static final String UNIQUE_USERS_YTD = "uniqueUsersYTD";
  private static final String ALL_RECORDINGS1 = "allRecordings";
  private static final String DEVICE_RECORDINGS1 = "deviceRecordings";
  private static final String MONTH1 = "month";
  private static final String COUNT = "count";
  private static final String YTD = COUNT;
  private static final String YTD1 = " YTD ";
  private static final String REF_AUDIO_RECORDINGS = "Ref Audio Recordings";
  private static final String OPERATING_SYSTEM = "operatingSystem";
  private static final String OPERATING_SYSTEM_VERSION = "operatingSystemVersion";
  private static final String BROWSER = "browser";
  private static final String BROWSER_VERSION = "browserVersion";
  private static final String NAME = "name";
  public static final String ACTIVE_I_PAD = "Active iPad/iPhone Users";
  public static final int EVIL_LAST_WEEK = 54;
  public static final String SKIP_USER = "gvidaver";

  private final IUserDAO userDAO;
  private final IResultDAO resultDAO;
  private final IEventDAO eventDAO;
  private final IAudioDAO audioDAO;

  private final String prefix;
  private final String language;
  private BufferedWriter csv;
  private final Map<Long, Long> userToStart = new HashMap<>();
  private static final boolean DEBUG = false;

  private final Set<String> lincoln = new HashSet<>(Arrays.asList(SKIP_USER, "rbudd", "jmelot", "esalesky", "gatewood",
      "testing", "grading", "fullperm", "0001abcd", "egodoy",
      "rb2rb2",
      "dajone3",
      "WagnerSandy",
      "rbtrbt"));

  Report(IUserDAO userDAO, IResultDAO resultDAO, IEventDAO eventDAO, IAudioDAO audioDAO, String language,
         String prefix) {
    this.userDAO = userDAO;
    this.resultDAO = resultDAO;
    this.eventDAO = eventDAO;
    this.audioDAO = audioDAO;
    this.language = language;
    this.prefix = prefix;
  }

  /**
   * TODO : do in reference to projects
   *
   * Sends a usage report to the email list at property {@link mitll.langtest.server.ServerProperties#getReportEmails()}.
   * Sends it out first thing every monday.
   * Subject disambiguates between multiple sites for the same language.
   * Also writes the report out to the report directory... TODO : necessary?
   *
   * @see mitll.langtest.server.database.DatabaseImpl#doReport
   */
  void doReport(ServerProperties serverProps, String site, MailSupport mailSupport,
                PathHelper pathHelper) {
    List<String> reportEmails = serverProps.getReportEmails();

    InetAddress ip = null;
    boolean skipReport = false;
    try {
      ip = InetAddress.getLocalHost();

      logger.info("\n\n\ndoReport got " + ip);
      logger.info("\n\n\ndoReport got " + ip.getHostName());
      skipReport = ip.getHostName().contains("MITLL");
      if (skipReport) {
        logger.info("skip writing report while testing.... " + ip.getHostName());
      }
      else {
        logger.info("will write report");
      }
    } catch (UnknownHostException e) {
      logger.error("Got " + e,e);
      e.printStackTrace();
    }

    // check if it's a monday
    if (!skipReport &&
        Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY &&
        !reportEmails.isEmpty()) {
      writeAndSendReport(serverProps.getLanguage(), site, mailSupport, pathHelper, reportEmails, getThisYear());
    } else {
//      logger.debug("not sending email report since this is not monday");
    }
  }

  /**
   * @param language
   * @param site
   * @param mailSupport
   * @param pathHelper
   * @param reportEmails who to send to
   * @param year         which year you want data for
   * @see #doReport(ServerProperties, String, MailSupport, PathHelper)
   */
  private void writeAndSendReport(String language,
                                  String site,
                                  MailSupport mailSupport,
                                  PathHelper pathHelper,
                                  List<String> reportEmails, int year) {
    String today = new SimpleDateFormat("MM_dd_yy").format(new Date());
    File file = getReportFile(pathHelper, today, language);
    if (file.exists()) {
      logger.debug("writeAndSendReport already did report for " + today + " : " + file.getAbsolutePath());
    } else {
      logger.debug("writeAndSendReport Site real path " + site);
      try {
        String message = writeReportToFile(file, pathHelper, language, new JSONObject(), year);
        sendEmails(language, site, mailSupport, reportEmails, message);
      } catch (Exception e) {
        logger.error("got " + e, e);
      }
    }
  }

  /**
   * @param pathHelper
   * @param language
   * @throws IOException
   * @see DatabaseImpl#doReport
   * @deprecated
   */
  JSONObject writeReportToFile(PathHelper pathHelper, String language, int year) throws IOException {
    File file = getReportPath(pathHelper, language);
    JSONObject jsonObject = new JSONObject();
    writeReportToFile(file, pathHelper, language, jsonObject, year);
    logger.debug("writeReportToFile wrote to " + file.getAbsolutePath());
    //logger.debug("\n" + jsonObject.toString());
    return jsonObject;
  }

  private File getReportPath(PathHelper pathHelper, String language) {
    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat(MM_DD_YY);
    String today = simpleDateFormat2.format(new Date());
    return getReportFile(pathHelper, today, language);
  }

  /**
   * Label report with hostname.
   *
   * @param language
   * @param site
   * @param mailSupport
   * @param reportEmails
   * @param message
   */
  private void sendEmails(String language, String site, MailSupport mailSupport, List<String> reportEmails, String message) {
    String suffix = "";
    if (site != null && site.contains("npfClassroom")) {
      site = site.substring(site.indexOf("npfClassroom"));
      suffix = " at " + site + " on " + getHostInfo();
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
  private String writeReportToFile(File file, PathHelper pathHelper, String language, JSONObject jsonObject,
                                   int year) throws IOException {
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
      // logger.debug("reports dir exists at " + reports.getAbsolutePath());
    }
    String fileName = prefix + "_" + language + "_report_" + today + ".html";
    return new File(reports, fileName);
  }

  /**
   * @return html of report
   * @see #writeReportToFile
   */
  private String doReport(PathHelper pathHelper, String language, JSONObject jsonObject, int year) {
    logger.info(language + " doReport for " + year);
    openCSVWriter(pathHelper, language);
    return getReport(language, jsonObject, year);
  }

  public String getAllReports(Collection<SlickProject> projects, JSONObject jsonObject, int year) {
    StringBuilder builder = new StringBuilder();
    builder.append(getHeader(this.language));

    for (SlickProject project:projects) {
      getReportForProject(language, jsonObject, year, builder, project.id());
    }

    builder.append(getFooter());
    return builder.toString();
  }
  /**
   *
   * @param language
   * @param jsonObject
   * @param year
   * @return
   */
  String getReport(String language, JSONObject jsonObject, int year) {
    StringBuilder builder = new StringBuilder();
    builder.append(getHeader(this.language));

    int projid = 0;

    getReportForProject(language, jsonObject, year, builder, projid);

    builder.append(getFooter());
    return builder.toString();
  }

  private void getReportForProject(String language, JSONObject jsonObject, int year, StringBuilder builder, int projid) {
    List<SlickSlimEvent> allSlim = eventDAO.getAllSlim(projid);
    List<SlickSlimEvent> allDevicesSlim = eventDAO.getAllDevicesSlim(projid);
    Map<Integer, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio(projid);
    Collection<AudioAttribute> audioAttributes   = audioDAO.getAudioAttributesByProject(projid);
    List<Result> results = resultDAO.getResults();
    Collection<Result> resultsDevices = resultDAO.getResultsDevices();

    {
      jsonObject.put("host", getHostInfo());

      JSONArray dataArray = new JSONArray();
      if (year == -1) {
        SlickSlimEvent firstSlim = eventDAO.getFirstSlim(projid);
        long timestamp = firstSlim.modified();
        Calendar instance = Calendar.getInstance();
        instance.clear();
        instance.setTimeInMillis(timestamp);
        int firstYear = instance.get(Calendar.YEAR);
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        logger.info(language + " doReport for " + firstYear + "->" + thisYear);

        for (int i = firstYear; i <= thisYear; i++) {
          addYear(dataArray, builder, i, allSlim, allDevicesSlim, exToAudio, audioAttributes, results, resultsDevices);
        }
      } else {
        addYear(dataArray, builder, year, allSlim, allDevicesSlim, exToAudio, audioAttributes, results, resultsDevices);
      }
      jsonObject.put("data", dataArray);
    }
  }

  private void addYear(JSONArray dataArray, StringBuilder builder, int i,
                       List<SlickSlimEvent> allSlim,
                       List<SlickSlimEvent> allDevicesSlim,
                       Map<Integer, List<AudioAttribute>> exToAudio,
                       Collection<AudioAttribute> audioAttributes,
                       Collection<Result> results,
                       Collection<Result> resultsDevices) {
    JSONObject forYear = new JSONObject();
    builder.append("<h1>" + i + "</h1>");
    builder.append(getReport(forYear, i, allSlim, allDevicesSlim, exToAudio, audioAttributes, results, resultsDevices));
    dataArray.add(forYear);
  }

  /**
   * @param jsonObject
   * @param year
   * @return
   * @see #addYear
   * @see DatabaseImpl#getReport(int, JSONObject)
   */
  private String getReport(JSONObject jsonObject, int year,
                           List<SlickSlimEvent> allSlim,
                           List<SlickSlimEvent> allDevicesSlim,
                           Map<Integer, List<AudioAttribute>> exToAudio,
                           Collection<AudioAttribute> audioAttributes,
                           Collection<Result> results,
                           Collection<Result> resultsDevices) {
    jsonObject.put("forYear", year);

    long then = System.currentTimeMillis();
    //   logger.info(language + " : doing year " + year);

    setUserStart(allSlim);

    StringBuilder builder = new StringBuilder();

    // all users
    JSONObject allUsers = new JSONObject();
    Set<Integer> users = getUsers(builder, allUsers, year);
    jsonObject.put(ALL_USERS, allUsers);

    // ipad users
    JSONObject iPadUsers = new JSONObject();
    getUsers(builder, userDAO.getUsersDevices(), NEW_I_PAD_I_PHONE_USERS, iPadUsers, year);
    jsonObject.put(I_PAD_USERS, iPadUsers);

    JSONObject timeOnTaskJSON = new JSONObject();
    Set<Integer> events = getEvents(builder, users, timeOnTaskJSON, year, allSlim);
    jsonObject.put(OVERALL_TIME_ON_TASK, timeOnTaskJSON);

    JSONObject deviceTimeOnTaskJSON = new JSONObject();
    Set<Integer> eventsDevices = getEventsDevices(builder, users, deviceTimeOnTaskJSON, year, allDevicesSlim);
    jsonObject.put(DEVICE_TIME_ON_TASK, deviceTimeOnTaskJSON);

    events.addAll(eventsDevices);

    JSONObject allRecordings = new JSONObject();
    getResults(builder, users, allRecordings, year, exToAudio, results);
    jsonObject.put(ALL_RECORDINGS1, allRecordings);

    JSONObject deviceRecordings = new JSONObject();
    getResultsDevices(builder, users, deviceRecordings, year, exToAudio, resultsDevices);
    jsonObject.put(DEVICE_RECORDINGS1, deviceRecordings);

    Calendar calendar = getCalendarForYear(year);
    Date january1st = getJanuaryFirst(calendar, year);
    Date january1stNextYear = getNextYear(year);

    if (DEBUG) {
      logger.info("doReport : between " + january1st + " and " + january1stNextYear);
    }

    JSONObject referenceRecordings = new JSONObject();
    addRefAudio(builder, calendar, audioAttributes, referenceRecordings, year);
    jsonObject.put("referenceRecordings", referenceRecordings);

    JSONObject browserReport = new JSONObject();
    getBrowserReport(getValidUsers(fixUserStarts()), year, browserReport, builder);
    jsonObject.put("hostInfo", browserReport);

    long now = System.currentTimeMillis();
    long l = now - then;
    if (l > 100) {
      logger.info(language + " took " + l + " millis to generate report for " + year);
    }
    return builder.toString();
  }

  private String getFooter() {
    return "</body></head></html>";
  }

  private String getHeader(String language) {
    String hostInfo = getHostInfo();
    //String language = this.language;
    return "<html><head>" +
        "<title>Report for " + language + " on " + hostInfo + "</title>" +
        "<body><h2>Host : " + hostInfo + "</h2>\n";
  }

  private Map<String, ReadableUserAgent> userAgentToReadable = new HashMap<>();

  /**
   * @param fusers
   * @param year
   * @param section
   * @param document
   * @see #getReport
   */
  private void getBrowserReport(List<User> fusers, int year, JSONObject section, StringBuilder document) {
    List<User> usersByYear = filterUsersByYear(fusers, year);

    UserAgentStringParser parser = UADetectorServiceFactory.getResourceModuleParser();

    Map<String, Integer> osToCount = new HashMap<>();
    Map<String, Integer> hostToCount = new HashMap<>();
    Map<String, Integer> familyToCount = new HashMap<>();
    Map<String, Integer> browserToCount = new HashMap<>();
    Map<String, Integer> browserVerToCount = new HashMap<>();
    int miss = 0;
    for (User f : usersByYear) {
      String ipaddr = f.getIpaddr();

      if (ipaddr != null) {
        String device = f.getDevice();
        boolean isIOS = device != null && device.startsWith("i");// == ReadableDeviceCategory.Category.UNKNOWN;
        if (isIOS) {
          String host = "iOS NetProF";
          incr(osToCount, host);
          incr(hostToCount, host);
          incr(familyToCount, host);
        } else {
          if (ipaddr.contains("at")) ipaddr = ipaddr.split("at")[0].trim();
          ReadableUserAgent agent = userAgentToReadable.get(ipaddr);
          if (agent == null) {
            //  logger.debug("cache miss for " + ipaddr);
            miss++;
            agent = parser.parse(ipaddr);
            userAgentToReadable.put(ipaddr, agent);
          }
          //logger.info("Got " + agent);
          //ReadableDeviceCategory.Category category = agent.getDeviceCategory().getCategory();
          String host;

          String suffix = ipaddr.contains("iPad") ? "iPad" : ipaddr.contains("iPhone") ? "iPhone" : "";
          VersionNumber versionNumber = agent.getOperatingSystem().getVersionNumber();
          incr(familyToCount, agent.getOperatingSystem().getFamily().getName());
          host = suffix + " " + agent.getOperatingSystem().getName();

          String version = versionNumber.getMajor() + "." + versionNumber.getMinor();// + (versionNumber.getBugfix().isEmpty() ? "" : "." + versionNumber.getBugfix());

          String hostVersion = (agent.getOperatingSystem().getFamily() == OperatingSystemFamily.WINDOWS) ? host : host + " " + version;
          incr(osToCount, hostVersion);
          incr(hostToCount, host);

//            logger.info("\tGot family   " + agent.getFamily().getName());
          //          logger.info("\tGot major version   " + agent.getVersionNumber().getMajor());
          String browser = agent.getFamily().getName() + " " + agent.getVersionNumber().getMajor();
          //   logger.info("\tGot browser   " + browser);
          incr(browserVerToCount, browser);
          incr(browserToCount, agent.getFamily().getName());

        }
      }
      // logger.info("osToCount " + osToCount);

    }
    JSONArray hostArray = new JSONArray();
    JSONArray familyArray = new JSONArray();
    JSONArray browserArray = new JSONArray();
    JSONArray browserVerArray = new JSONArray();

    document.append(getWrapper(OPERATING_SYSTEM, getCountTable(NAME, COUNT, familyArray, NAME, getSorted(familyToCount))));
    document.append(getWrapper(OPERATING_SYSTEM_VERSION, getCountTable(NAME, COUNT, hostArray, NAME, getSorted(hostToCount))));
    document.append(getWrapper(BROWSER, getCountTable(NAME, COUNT, browserArray, NAME, getSorted(browserToCount))));
    document.append(getWrapper(BROWSER_VERSION, getCountTable(NAME, COUNT, browserVerArray, NAME, getSorted(browserVerToCount))));

    section.put(OPERATING_SYSTEM, familyArray);
    section.put(OPERATING_SYSTEM_VERSION, hostArray);
    section.put(BROWSER, browserArray);
    section.put(BROWSER_VERSION, browserVerArray);

    if (miss > 0)
      logger.info(language + " for " + year + " users by year " + userAgentToReadable.size() + " miss " + miss);
  }

  private List<User> filterUsersByYear(List<User> fusers, int year) {
    Calendar calendar = getCalendarForYear(year);
    YearTimeRange yearTimeRange = new YearTimeRange(year, calendar).invoke();
    List<User> forYear = new ArrayList<>();
    for (User t : fusers) if (yearTimeRange.inYear(t.getTimestampMillis())) forYear.add(t);
    return forYear;
  }

  private Map<String, Integer> getSorted(Map<String, Integer> osToCount) {
    List<String> sorted = new ArrayList<>(osToCount.keySet());
    Collections.sort(sorted);
    Map<String, Integer> ret = new TreeMap<>();
    for (String key : sorted) {
      //  logger.info(key + " : " + osToCount.get(key));
      ret.put(key, osToCount.get(key));
    }
    return ret;
  }

  private void incr(Map<String, Integer> osToCount, String host) {
    osToCount.put(host, osToCount.getOrDefault(host, 0) + 1);
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

  /**
   * @param builder
   * @return
   * @see #getReport
   */
  private Set<Integer> getUsers(StringBuilder builder, JSONObject jsonObject, int year) {
    return getUsers(builder, fixUserStarts(), ALL_NEW_USERS, jsonObject, year);
  }

  private String getHostInfo() {
    String suffix = "";
    try {
      suffix = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      logger.error("getHostInfo got " + e);
      try {
        suffix = e.getMessage().split(":")[1];
      } catch (Exception e1) {
        logger.error("got " + e1);
      }
    }
    return suffix;
  }

  /**
   * setUserStart must be called first
   *
   * @return
   * @see #setUserStart(Collection)
   */
  private List<User> fixUserStarts() {
    List<User> users = userDAO.getUsers();
    for (User user : users) {
      Long aLong = userToStart.get(user.getID());
      if (aLong != null) user.setTimestampMillis(aLong);
      //else {
      //  logger.error("no events for " + user.getExID());
      //  }
    }
    return users;
  }

  private List<User> getValidUsers(List<User> all) {
    List<User> valid = new ArrayList<>();
    for (User u : all) if (!shouldSkipUser(u)) valid.add(u);
    return valid;
  }

  /**
   * @param builder
   * @param users
   * @param users1
   * @return set of valid users
   * @see #doReport
   * @see #getUsers
   */
  private Set<Integer> getUsers(StringBuilder builder, Collection<User> users, String users1, JSONObject jsonObject, int year) {
    Calendar calendar = getCalendarForYear(year);
    YearTimeRange yearTimeRange = new YearTimeRange(year, calendar).invoke();
    int ytd = 0;

    Counts counts = new Counts(year);
    Map<Integer, Integer> monthToCount = counts.getMonthToCount();
    Map<Integer, Integer> weekToCount = counts.getWeekToCount();

    Set<Integer> students = new HashSet<>();

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

      if (shouldSkipUser(user)) {
        if (SHOW_TEACHER_SKIPS) logger.warn("skipping ? " + user);
        continue;
      }
      if (isStudent) {
        students.add(user.getID());
        long userCreated = user.getTimestampMillis();
        if (yearTimeRange.inYear(userCreated)) {
          ytd++;
          calendar.setTimeInMillis(userCreated);

          int month = calendar.get(Calendar.MONTH);
          monthToCount.put(month, monthToCount.getOrDefault(month, 0) + 1);

          int w = calendar.get(Calendar.WEEK_OF_YEAR);
          weekToCount.put(w, weekToCount.getOrDefault(w, 0) + 1);
        } else {
          //   logger.debug("NO time " +user.getTimestamp() + " " + parse);
        }
        //}
      } else {
        if (SHOW_TEACHER_SKIPS) logger.warn("skipping teacher " + user);
      }
    }
    int monthTotal = 0;
    for (Integer count : monthToCount.values()) monthTotal += count;

    int weekTotal = 0;
    for (Integer count : weekToCount.values()) weekTotal += count;

    if (monthTotal != weekTotal) {
      logger.info("users month total " + monthTotal + " week total " + weekTotal);
      logger.info("weeks\n" + weekToCount);
      logger.info("users " + weekToCount.keySet());
    }
    //logger.info("users " + weekToCount.keySet());

    builder.append(getSectionReport(ytd, monthToCount, weekToCount, users1, jsonObject, year));
    return students;
  }

  private void ensureYTDEntries0(int year, Map<Integer, Long> monthToCount) {
    Calendar today = getCalendarForYearOrNow();
    int thisMonth = today.get(Calendar.MONTH);
    for (int i = 0; i <= thisMonth; i++) monthToCount.put(i, 0l);

    if (isNotThisYear(year)) {
      for (int i = 0; i < 12; i++) monthToCount.put(i, 0l);
    }
  }

  private boolean isNotThisYear(int year) {
    return getThisYear() != year;
  }

  private Calendar getCalendarForYearOrNow() {
    Calendar today = Calendar.getInstance();
    today.setTimeInMillis(System.currentTimeMillis());
    return today;
  }

  private void ensureYTDEntriesW(int year, Map<Integer, Long> weekToCount) {
    Calendar today = getCalendarForYearOrNow();
    int thisWeek = today.get(Calendar.WEEK_OF_YEAR);
    for (int i = 0; i <= thisWeek; i++) weekToCount.put(i, 0l);

    if (isNotThisYear(year)) {
      for (int i = 0; i < EVIL_LAST_WEEK; i++) weekToCount.put(i, 0l);
    }
  }

  private void ensureYTDEntries2(int year, Map<Integer, Set<Long>> monthToCount, Map<Integer, Set<Long>> weekToCount) {
    Calendar today = getCalendarForYearOrNow();
    int thisMonth = today.get(Calendar.MONTH);
    int thisWeek = today.get(Calendar.WEEK_OF_YEAR);
    for (int i = 0; i <= thisMonth; i++) monthToCount.put(i, new HashSet<>());
    for (int i = 0; i <= thisWeek; i++) weekToCount.put(i, new HashSet<>());

    if (isNotThisYear(year)) {
      for (int i = 0; i < 12; i++) monthToCount.put(i, new HashSet<>());
      for (int i = 0; i < EVIL_LAST_WEEK; i++) weekToCount.put(i, new HashSet<>());
    }
  }

  private void ensureYTDEntries3(int year, Map<Integer, Map<Long, Set<SlickSlimEvent>>> monthToCount, Map<Integer, Map<Long, Set<SlickSlimEvent>>> weekToCount) {
    Calendar today = getCalendarForYearOrNow();
    int thisMonth = today.get(Calendar.MONTH);
    int thisWeek = today.get(Calendar.WEEK_OF_YEAR);
    //   Date now = today.getTime();
    for (int i = 0; i <= thisMonth; i++) monthToCount.put(i, new HashMap<>());
    for (int i = 0; i <= thisWeek; i++) weekToCount.put(i, new HashMap<>());
    if (isNotThisYear(year)) {
      for (int i = 0; i < 12; i++) monthToCount.put(i, new HashMap<>());
      for (int i = 0; i < EVIL_LAST_WEEK; i++) weekToCount.put(i, new HashMap<>());
    }
  }

  private boolean shouldSkipUser(User user) {
    return user.getID() == 3 || user.getID() == 1 ||
        lincoln.contains(user.getUserID()) || user.getUserID().startsWith(SKIP_USER);
  }

  /**
   * @param ytd
   * @param monthToCount
   * @param weekToCount
   * @param users1
   * @param jsonObject
   * @return
   * @see #addRefAudio
   * @see #getEvents
   * @see #getResultsForSet
   * @see #getUsers
   */
  private String getSectionReport(int ytd,
                                  Map<Integer, ?> monthToCount,
                                  Map<Integer, ?> weekToCount,
                                  String users1,
                                  JSONObject jsonObject,
                                  int year) {

    JSONObject yearJSON = new JSONObject();
    String yearCol = getYTD(ytd, users1, yearJSON, year);
    jsonObject.put("year", yearJSON);

    JSONArray monthArray = new JSONArray();
    String monthCol = getMonthToCount(monthToCount, MONTH, users1, "", monthArray, year);
    jsonObject.put(MONTH1, monthArray);


    JSONArray weekArray = new JSONArray();
    String weekCol = getWC(weekToCount, WEEK, users1, weekArray, year);
    jsonObject.put("week", weekArray);

//    logger.debug("getSectionReport json " + jsonObject);

    return getYearMonthWeekTable(users1, yearCol, monthCol, weekCol);
  }

/*  private void writeMonthToCSV(Map<Integer, ?> monthToCount, String users1, String language, int year) {
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
  }*/

  private String getYearMonthWeekTable(String users1, String yearCol, String monthCol, String weekCol) {
    return "<h2>" + users1 + "</h2>" +
        "<table ><tr>" +
        top + yearCol + "</td>" +
        top + monthCol + "</td>" +
        top + weekCol + "</td>" +
        "</tr></table>";
  }

  private String getWrapper(String users1, String content) {
    return "<h2>" + users1 + "</h2>" + content;
  }

  private String getYTD(int ytd, String users1, JSONObject jsonObject, int year) {
    jsonObject.put("label", users1);
    jsonObject.put("year", year);
    jsonObject.put(YTD, ytd);

    boolean currentYear = year == getThisYear();

    String suffix = currentYear ? YTD1 : "";
    return "<table style='background-color: #eaf5fb'>" +
        "<tr>" +
        "<th>" + users1 + suffix + "</th>" +
        "</tr>" +
        "<tr>" +
        "<td>" + ytd + "</td>" +
        "</tr>" +
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
  private String getMonthToCount(Map<Integer, ?> monthToCount,
                                 String unit,
                                 String count,
                                 String tableLabel,
                                 JSONArray jsonArray,
                                 int year) {
    // writeMonthToCSV(monthToCount, tableLabel.isEmpty() ? count : tableLabel, language, year);
    return getIntCountTable(unit, count, jsonArray, MONTH1, monthToCount);
  }

  /**
   * Does months - 1-12
   *
   * @param unit
   * @param count
   * @param jsonArray
   * @param label
   * @param monthToValue
   * @return
   */
  private String getIntCountTable(String unit, String count, JSONArray jsonArray, String label, Map<Integer, ?> monthToValue) {
    StringBuilder s = new StringBuilder();
    Integer max = getMax(monthToValue);

    for (int month = 0; month <= max; month++) {
      Object value = monthToValue.get(month);
      if (value instanceof Collection<?>) {
        value = ((Collection<?>) value).size();
      }
      if (value == null) value = 0;
      addJsonRow(jsonArray, label, value, month + 1);
      s.append(getHTMLRow(getMonth(month), value));
    }
    return getTableHTML(unit, count, s.toString());
  }

  private Integer getMax(Map<Integer, ?> monthToValue) {
    Set<Integer> integers = monthToValue.keySet();
    Integer max = -1;
    for (Integer i : integers) if (max < i) max = i;
    return max;
  }

  /**
   * @param unit
   * @param count
   * @param jsonArray
   * @param label
   * @param monthToValue
   * @return
   * @see #getBrowserReport(List, int, JSONObject, StringBuilder)
   */
  private String getCountTable(String unit, String count, JSONArray jsonArray, String label, Map<String, ?> monthToValue) {
    String s = "";
    for (Map.Entry<String, ?> pair : monthToValue.entrySet()) {
      Object value = pair.getValue();
      if (value instanceof Collection<?>) {
        value = ((Collection<?>) value).size();
      }
      String month = pair.getKey();
      addJsonRow(jsonArray, label, value, month);
      s += getHTMLRow(month, value);
    }
    return getTableHTML(unit, count, s);
  }

  private String getHTMLRow(String month, Object value) {
    return "<tr>" +
        "<td>" + month + "</td>" +
        "<td>" + value + "</td>" +
        "</tr>";
  }

  private void addJsonRow(JSONArray jsonArray, String label, Object value, Object month) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(label, month);
    jsonObject.put(COUNT, value);
    jsonArray.add(jsonObject);
  }

  private String getTableHTML(String unit, String count, String s) {
    return "<table style='background-color: #eaf5fb' >" +
        "<tr>" +
        "<th>" + unit + "</th>" +
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
   * @see #getSectionReport(int, Map, Map, String, JSONObject, int)
   * @see #getEvents
   */
  private String getWC(Map<Integer, ?> weekToCount, String unit, String count, JSONArray jsonArray, int year) {
    String s = "";
    Calendar calendar = getCalendarForYear(year);
    SimpleDateFormat df = new SimpleDateFormat("MM-dd");
    Integer max = getMax(weekToCount);
    long initial = calendar.getTimeInMillis();
    SimpleDateFormat fullFormat = new SimpleDateFormat("MM-dd-yy");

//    logger.info(unit +" before  " + year + " = " + calendar.getTime() + " or " + df.format(calendar.getTime()));

    for (int week = 1; week <= max; week++) {
      // for (Map.Entry<Integer, ?> pair : weekToCount.entrySet()) {
      Object value = weekToCount.get(week);
      if (value == null) value = 0;
      if (value instanceof Collection<?>) {
        value = ((Collection<?>) value).size();
      }

      //   logger.info("getWC before week " +week + " = " + calendar.getTime() + " or " + df.format(calendar.getTime()));

      calendar.set(Calendar.WEEK_OF_YEAR, week);
      calendar.set(Calendar.DAY_OF_WEEK, 1);
      calendar.set(Calendar.YEAR, year);

      Date time = calendar.getTime();
      boolean before = time.getTime() < initial;
      String format1 = before ? fullFormat.format(time) : df.format(time);

      //logger.info("getWC after  week " + week + " = " + time + " " + time.getTime() +" or " + format1 + " = " + value);

      JSONObject jsonObject = new JSONObject();
      jsonObject.put("weekOfYear", week);
      jsonObject.put(COUNT, value);
      jsonObject.put("date", fullFormat.format(time));

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
   * @param year
   * @param exToAudio
   * @paramx language
   * @see #doReport
   */
  private void getResults(StringBuilder builder,
                          Set<Integer> students,
                          JSONObject jsonObject,
                          int year,
                          Map<Integer, List<AudioAttribute>> exToAudio,
                          Collection<Result> results) {
    getResultsForSet(builder, students, results, ALL_RECORDINGS, jsonObject, year, exToAudio);
  }

  private void getResultsDevices(StringBuilder builder, Set<Integer> students,
                                 JSONObject jsonObject, int year,
                                 Map<Integer, List<AudioAttribute>> exToAudio,
                                 Collection<Result> results) {
    getResultsForSet(builder, students, results, DEVICE_RECORDINGS, jsonObject, year, exToAudio);
  }

  private void getResultsForSet(StringBuilder builder,
                                Set<Integer> students,
                                Collection<Result> results,
                                String recordings,
                                JSONObject jsonObject, int year,
                                Map<Integer, List<AudioAttribute>> exToAudio) {
    YearTimeRange yearTimeRange = new YearTimeRange(year, getCalendarForYear(year)).invoke();

    int ytd = 0;
    Counts counts = new Counts(year);
    Map<Integer, Integer> monthToCount = counts.getMonthToCount();
    Map<Integer, Integer> weekToCount = counts.getWeekToCount();

    Map<Long, Map<String, Integer>> userToDayToCount = new TreeMap<>();

    int teacherAudio = 0;
    int invalid = 0;
    int invalidScore = 0;

    int beforeJanuary = 0;
    Set<Integer> skipped = new TreeSet<>();
    try {
      BufferedWriter writer = null;
      teacherAudio = 0;
      invalid = 0;
      Calendar calendar = getCalendarForYear(year);

      for (Result result : results) {
        long timestamp = result.getTimestamp();

        if (yearTimeRange.inYear(timestamp)) {
          if (result.isValid()) {
            if (!isRefAudioResult(exToAudio, result)) {
              int userid = result.getUserid();
              if (students.contains(userid)) {
                //if (result.getAudioType().equals("unset") || result.getAudioType().equals("_by_WebRTC")) {
                if (result.getPronScore() > -1) {
                  if (isValidUser(userid)) {
           /*         if (WRITE_RESULTS_TO_FILE) {
                      writer.write(result.toString());
                      writer.write("\n");
                    }*/
                    ytd++;
                    tallyByMonthAndWeek(calendar, monthToCount, weekToCount, result, userToDayToCount);
                  }
                } else {
                  invalidScore++;
                }
              } else {
                skipped.add(userid);
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
  }

  /**
   * @param builder
   * @param calendar
   * @param refAudio
   * @param jsonObject
   * @paramx language
   * @see #getResults
   */
  private <T extends UserAndTime> void addRefAudio(StringBuilder builder, Calendar calendar,
                                                   Collection<T> refAudio, JSONObject jsonObject, int year) {
    int ytd = 0;
    Counts counts = new Counts(year);
    Map<Integer, Integer> monthToCount = counts.getMonthToCount();
    Map<Integer, Integer> weekToCount = counts.getWeekToCount();

    Map<Long, Map<String, Integer>> userToDayToCount = new TreeMap<>();

    YearTimeRange yearTimeRange = new YearTimeRange(year, getCalendarForYear(year)).invoke();

    for (T result : refAudio) {
      if (yearTimeRange.inYear(result.getTimestamp())) {
        ytd++;
        tallyByMonthAndWeek(calendar, monthToCount, weekToCount, result, userToDayToCount);
      }
    }

    int monthTotal = 0;
    for (Integer count : monthToCount.values()) monthTotal += count;

    int weekTotal = 0;
    for (Integer count : weekToCount.values()) weekTotal += count;

    if (monthTotal != weekTotal) {
      logger.info("ref audio month total " + monthTotal + " week total " + weekTotal);
      logger.info("ref weeks " + weekToCount.keySet());
    }

//    logger.info("addRefAudio month\n" + monthToCount);
//    logger.info("week  \n" + weekToCount);

    builder.append(getSectionReport(ytd, monthToCount, weekToCount, REF_AUDIO_RECORDINGS, jsonObject, year));
  }

  private static class Counts {
    private Map<Integer, Integer> monthToCount = new TreeMap<>();
    private Map<Integer, Integer> weekToCount = new TreeMap<>();

    Counts(int year) {
      if (year == getThisYear()) {
        ensureYTDEntries(monthToCount, weekToCount);
        //      logger.info("months " + monthToCount.keySet() + " weeks " + weekToCount.keySet());
      } else {
        for (int i = 0; i < 12; i++) monthToCount.put(i, 0);
        for (int i = 0; i < EVIL_LAST_WEEK; i++) weekToCount.put(i, 0);
//        logger.info("months " + monthToCount.keySet() + " weeks " + weekToCount.keySet());
      }
    }

    private Calendar getCalendarForYearOrNow() {
      Calendar today = Calendar.getInstance();
      today.setTimeInMillis(System.currentTimeMillis());
      return today;
    }

    private int getThisYear() {
      return Calendar.getInstance().get(Calendar.YEAR);
    }

    private void ensureYTDEntries(Map<Integer, Integer> monthToCount, Map<Integer, Integer> weekToCount) {
      Calendar today = getCalendarForYearOrNow();
      int thisMonth = today.get(Calendar.MONTH);
      for (int i = 0; i <= thisMonth; i++) monthToCount.put(i, 0);
      int thisWeek = today.get(Calendar.WEEK_OF_YEAR);
      //  logger.info("week " +thisWeek);
      for (int i = 0; i <= thisWeek; i++) weekToCount.put(i, 0);
    }

    Map<Integer, Integer> getMonthToCount() {
      return monthToCount;
    }

    Map<Integer, Integer> getWeekToCount() {
      return weekToCount;
    }
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
    monthToCount.put(month, monthToCount.getOrDefault(month, 0) + 1);

    Map<String, Integer> dayToCount = userToDayToCount.get(userid);
    if (dayToCount == null) userToDayToCount.put(userid, dayToCount = new TreeMap<>());
    String key = calendar.get(Calendar.YEAR) + "," +
        month + "," +
        calendar.get(Calendar.DAY_OF_MONTH);
    dayToCount.put(key, dayToCount.getOrDefault(key, 0) + 1);

    int w = calendar.get(Calendar.WEEK_OF_YEAR);
    Integer orDefault = weekToCount.getOrDefault(w, 0);
//    if (orDefault == 0) {
//      logger.debug("got " + w + " for " + new Date(timestamp) + " ");
//    }
    weekToCount.put(w, orDefault + 1);
  }

  /**
   * @param exToAudio
   * @param result
   * @return
   * @see #getResults
   */
  private boolean isRefAudioResult(Map<Integer, List<AudioAttribute>> exToAudio, Result result) {
    boolean skip = false;
    List<AudioAttribute> audioAttributes = exToAudio.get(result.getExerciseID());
    if (audioAttributes != null) {
      for (AudioAttribute audioAttribute : audioAttributes) {
        if (getFile(audioAttribute).equals(getFile(result))) {
          long userid = result.getUserid();
          if (audioAttribute.getUser().getID() == userid) {
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

  private Date getJanuaryFirst(Calendar calendar, int year) {
    setToFirstMoment(calendar);
    calendar.set(Calendar.YEAR, year);
    return calendar.getTime();
  }

  private Calendar getCalendarForYear(int year) {
    Calendar instance = Calendar.getInstance();
    setToFirstMoment(instance);
    instance.set(Calendar.YEAR, year);
    return instance;
  }

  private void setToFirstMoment(Calendar instance) {
    instance.set(Calendar.DAY_OF_YEAR, 1);
    instance.set(Calendar.HOUR_OF_DAY, 0);
    instance.set(Calendar.MINUTE, 0);
    instance.set(Calendar.SECOND, 0);
  }

  private Date getNextYear(int year) {
    Calendar instance = Calendar.getInstance();
    // if (CLEAR_DAY_HOUR_MINUTE) {
    setToFirstMoment(instance);
    instance.set(Calendar.YEAR, year + 1);
    // }
    return instance.getTime();
  }

  private int getThisYear() {
    return Calendar.getInstance().get(Calendar.YEAR);
  }

  /**
   * @param builder
   * @param students
   * @param jsonObject
   * @see #doReport
   */
  private Set<Integer> getEvents(StringBuilder builder, Set<Integer> students, JSONObject jsonObject, int year,
                                 Collection<SlickSlimEvent> all) {
    return getEvents(builder, students, all, ACTIVE_USERS, TIME_ON_TASK, jsonObject, year);
  }

  /**
   * Look at the event table to determine the first moment a user did anything
   * There was a bug where the user timestamp was not set properly.
   */
  private void setUserStart(Collection<SlickSlimEvent> all) {
    for (SlickSlimEvent event : all) {
      long creatorID = event.userid();
      long timestamp = event.modified();
      if (!userToStart.containsKey(creatorID) || timestamp < userToStart.get(creatorID)) {
        userToStart.put(creatorID, timestamp);
      }
    }
  }

  /**
   * @param builder
   * @param students
   * @param jsonObject
   * @param year
   * @return
   * @see #getReport
   */
  private Set<Integer> getEventsDevices(StringBuilder builder, Set<Integer> students, JSONObject jsonObject, int year,
                                        List<SlickSlimEvent> allDevicesSlim) {
    String activeUsers = ACTIVE_I_PAD;
    String tableLabel = "iPad/iPhone Time on Task";
    return getEvents(builder, students, allDevicesSlim, activeUsers, tableLabel, jsonObject, year);
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
  private Set<Integer> getEvents(StringBuilder builder, Set<Integer> students, Collection<SlickSlimEvent> all, String activeUsers,
                                 String tableLabel, JSONObject jsonObject, int year) {
    Map<Integer, Set<Long>> monthToCount = new TreeMap<>();
    Map<Integer, Set<Long>> weekToCount = new TreeMap<>();
    ensureYTDEntries2(year, monthToCount, weekToCount);
//    logger.info("now " + monthToCount.keySet() + " " + weekToCount.keySet());

    Map<Integer, Map<Long, Set<SlickSlimEvent>>> monthToCount2 = new TreeMap<>();
    Map<Integer, Map<Long, Set<SlickSlimEvent>>> weekToCount2 = new TreeMap<>();
    ensureYTDEntries3(year, monthToCount2, weekToCount2);
    //  logger.info("now " + monthToCount2.keySet() + " " + weekToCount2.keySet());

    Set<Integer> teachers = new HashSet<>();
//    int skipped = 0;
    Calendar calendar = getCalendarForYear(year);
    YearTimeRange yearTimeRange = new YearTimeRange(year, calendar).invoke();

    Set<Integer> users = new HashSet<>();

    for (SlickSlimEvent event : all) {
      Integer creatorID = event.userid();
      long timestamp = event.modified();
      if (yearTimeRange.inYear(timestamp) && students.contains(creatorID)) {
        if (isValidUser(creatorID)) {
          users.add(creatorID);
          statsForEvent(calendar, monthToCount, monthToCount2, weekToCount, weekToCount2, event, creatorID);
        }
      } else if (!students.contains(creatorID)) {
        //  skipped++;
        teachers.add(creatorID);
      }
    }
    //dumpActiveUsers(activeUsers, teachers, skipped, users);

    JSONObject activeJSON = new JSONObject();
    builder.append(getSectionReport(users.size(), monthToCount, weekToCount, activeUsers, activeJSON, year));
    jsonObject.put("activeUsers", activeJSON);
//    logger.debug("active users " + activeJSON);

    Map<Integer, Long> monthToDur = getMonthToDur(monthToCount2, year);
    long total = 0;
    for (Long v : monthToDur.values()) total += v;

    total /= MIN_MILLIS;

    Map<Integer, Long> weekToDur = getWeekToDur(weekToCount2, year);

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
    timeOnTaskJSON.put(MONTH1, monthArray);
    timeOnTaskJSON.put("week", weekArray);

    jsonObject.put("timeOnTask", timeOnTaskJSON);

    builder.append(yearMonthWeekTable);

    return users;
  }

/*  private void dumpActiveUsers(String activeUsers, Set<Long> teachers, int skipped, Set<Long> users) {
    List<Long> longs = new ArrayList<>(users);
    Collections.sort(longs);
//    logger.debug(activeUsers + " getEvents skipped " + skipped + " events from teachers " + teachers + "\nusers " + longs);
  }*/

  private boolean isValidUser(long creatorID) {
    return creatorID != 1;
  }

  /**
   * @param calendar
   * @param monthToCount
   * @param monthToUserToEvents
   * @param weekToCount
   * @param weekToUserToEvents
   * @param event
   * @param creatorID
   * @see #getEvents
   */
  private void statsForEvent(Calendar calendar,

                             Map<Integer, Set<Long>> monthToCount,
                             Map<Integer, Map<Long, Set<SlickSlimEvent>>> monthToUserToEvents,

                             Map<Integer, Set<Long>> weekToCount,
                             Map<Integer, Map<Long, Set<SlickSlimEvent>>> weekToUserToEvents,
                             SlickSlimEvent event, long creatorID) {
    calendar.setTimeInMillis(event.modified());

    // months
    int month = calendar.get(Calendar.MONTH);

    Map<Long, Set<SlickSlimEvent>> userToEvents = monthToUserToEvents.get(month);
    Set<Long> users = monthToCount.get(month);
    if (users == null) {
      monthToCount.put(month, users = new HashSet<>());
    }
    users.add(creatorID);

    if (userToEvents == null) {
      monthToUserToEvents.put(month, userToEvents = new HashMap<>());
    }

    rememberEvent(event, creatorID, userToEvents);
//    Set<SlickSlimEvent> events;


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

//    events = userToEvents.get(creatorID);
//    if (events == null) userToEvents.put(creatorID, events = new TreeSet<>());
//    events.add(event);
    rememberEvent(event, creatorID, userToEvents);

  }

  private void rememberEvent(SlickSlimEvent event, long creatorID, Map<Long, Set<SlickSlimEvent>> userToEvents) {
    Set<SlickSlimEvent> events = userToEvents.get(creatorID);
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
  private Map<Integer, Long> getMonthToDur(Map<Integer, Map<Long, Set<SlickSlimEvent>>> monthToCount2, int year) {
    Map<Integer, Long> monthToDur = new TreeMap<>();
    ensureYTDEntries0(year, monthToDur);
    for (Map.Entry<Integer, Map<Long, Set<SlickSlimEvent>>> monthToUserToEvents : monthToCount2.entrySet()) {
      Integer month = monthToUserToEvents.getKey();
      //logger.debug("month " + month);

      Map<Long, Set<SlickSlimEvent>> userToEvents = monthToUserToEvents.getValue();

      for (Map.Entry<Long, Set<SlickSlimEvent>> eventsForUser : userToEvents.entrySet()) {
//        Long user = eventsForUser.getKey();
        long start = 0;
        long dur = 0;
        long last = 0;
        //      Event sevent = null, levent = null;
        for (SlickSlimEvent event : eventsForUser.getValue()) {
          long now = event.modified();
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

  /**
   * @param weekToCount
   * @return
   * @see #getEvents(StringBuilder, Set, Collection, String, String, JSONObject, int)
   */
  private Map<Integer, Long> getWeekToDur(Map<Integer, Map<Long, Set<SlickSlimEvent>>> weekToCount, int year) {
    Map<Integer, Long> weekToDur = new TreeMap<>();
    ensureYTDEntriesW(year, weekToDur);
    for (Map.Entry<Integer, Map<Long, Set<SlickSlimEvent>>> weekToUserToEvents : weekToCount.entrySet()) {
      Integer week = weekToUserToEvents.getKey();
      //logger.debug("week " + week);

      Map<Long, Set<SlickSlimEvent>> userToEvents = weekToUserToEvents.getValue();

      for (Map.Entry<Long, Set<SlickSlimEvent>> eventsForUser : userToEvents.entrySet()) {
        //  Long user = eventsForUser.getKey();
        //logger.debug("\tuser " + user);
        long start = 0;
        long dur = 0;
        long last = 0;
        for (SlickSlimEvent event : eventsForUser.getValue()) {
          long now = event.modified();
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

  private class YearTimeRange {
    private int year;
    private Calendar calendar;
    private long time;
    private long time1;

    YearTimeRange(int year, Calendar calendar) {
      this.year = year;
      this.calendar = calendar;
    }

    public long getStart() {
      return time;
    }

    public long getEnd() {
      return time1;
    }

    boolean inYear(long test) {
      return test >= time && test < time1;
    }

    YearTimeRange invoke() {
      Date january1st = getJanuaryFirst(calendar, year);
      Date january1stNextYear = getNextYear(year);
      if (DEBUG) logger.info("getEvents from " + january1st + " to " + january1stNextYear);
      time = january1st.getTime();
      time1 = january1stNextYear.getTime();
      return this;
    }
  }
}
