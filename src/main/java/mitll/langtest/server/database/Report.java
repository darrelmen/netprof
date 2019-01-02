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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.ReportStats.INFO;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.excel.ReportToExcel;
import mitll.langtest.server.database.instrumentation.IEventDAO;
import mitll.langtest.server.database.report.ReportingServices;
import mitll.langtest.server.database.result.IResultDAO;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.UserTimeBase;
import mitll.langtest.shared.result.MonitorResult;
import mitll.langtest.shared.user.Kind;
import mitll.langtest.shared.user.ReportUser;
import mitll.npdata.dao.SlickProject;
import mitll.npdata.dao.SlickSlimEvent;

import net.sf.uadetector.OperatingSystemFamily;
import net.sf.uadetector.ReadableUserAgent;
import net.sf.uadetector.UserAgentStringParser;
import net.sf.uadetector.VersionNumber;
import net.sf.uadetector.service.UADetectorServiceFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
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
public class Report implements IReport {
  private static final Logger logger = LogManager.getLogger(Report.class);

  private static final int MIN_MILLIS = (1000 * 60);
  private static final int TEN_SECONDS = 1000 * 10;
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
  private static final boolean SHOW_TEACHER_SKIPS = false;

  private static final String NEW_I_PAD_I_PHONE_USERS = "New iPad/iPhone Users";
  private static final String TIME_ON_TASK = "Time on Task";
  private static final String ALL_NEW_USERS = "All New Users";
  private static final String ALL_USERS = "allUsers";
  private static final String I_PAD_USERS = "iPadUsers";
  private static final String OVERALL_TIME_ON_TASK = "overallTimeOnTask";
  private static final String DEVICE_TIME_ON_TASK = "deviceTimeOnTask";
  private static final String ALL_RECORDINGS1 = "allRecordings";
  private static final String DEVICE_RECORDINGS1 = "deviceRecordings";
  private static final String MONTH1 = "month";
  private static final String COUNT = "childCount";
  private static final String YTD = COUNT;
  private static final String YTD1 = " YTD ";
  /**
   *
   */
  private static final String REF_AUDIO_RECORDINGS = "Ref Audio Recordings";
  private static final String OPERATING_SYSTEM = "operatingSystem";
  private static final String OPERATING_SYSTEM_VERSION = "operatingSystemVersion";
  private static final String BROWSER = "browser";
  private static final String BROWSER_VERSION = "browserVersion";
  private static final String NAME = "name";
  private static final String ACTIVE_I_PAD = "Active iPad/iPhone Users";
  private static final int EVIL_LAST_WEEK = 54;
  private static final String SKIP_USER = "gvidaver";
  static final int DAY_TO_SEND_REPORT = Calendar.SATURDAY;// Calendar.SUNDAY;
  private static final int MIN_DURATION = 250;
  private static final String WEEK1 = "week";
  private static final String YEAR = "year";
  private static final String REFERENCE_RECORDINGS = "referenceRecordings";
  private static final String HOST = "host";
  private static final String TIME_ON_TASK_IOS = "iPad/iPhone Time on Task";
  private static final int ALL_YEARS = -1;
  private static final String FOOTER = "</body></head></html>";

  /**
   * When sending all years, don't go back before this year.
   */
  public static final int EARLIEST_YEAR = 2015;
  private static final String DATA = "data";
  private static final String WEEK_OF_YEAR = "weekOfYear";
  private static final String DATE = "date";
  private static final String YYYY_MM_DD = "yyyy_MM_dd";
  private static final String DLIFLC_NET_PRO_F_QUICK_LOOK_SUMMARY = "_DLIFLC_NetProF_Quick-Look-Summary";
  public static final String WEEKLY_FORMAT = "MM-dd";
  private static final String MM_DD_YY1 = "MM-dd-yy";

  /**
   * @see #getReportForProject
   */
  private final IResultDAO resultDAO;
  private final IEventDAO eventDAO;
  private final IAudioDAO audioDAO;

  private final Map<Integer, Long> userToStart = new HashMap<>();

  private final Map<Integer, String> idToUser = new HashMap<>();

  /**
   * @see #isLincoln
   * @see #shouldSkipUser
   */
  private final Set<String> lincoln = new HashSet<>(Arrays.asList(SKIP_USER,
      "demo",
      "demo_",
      "rbudd",
      "jmelot",
      "esalesky",
      "gatewood",
      "testing",
      "grading",
      "fullperm",
      "0001abcd",
      "egodoy",
      "rb2rb2",
      "dajone3",
      "WagnerSandy",
      "SWagner",
      "rbtrbt",
      "tamas01",
      "tmarius",
      "teacher",
      "newteacher"));

  private final List<ReportUser> users;
  private final Set<Integer> allTeachers = new HashSet<>();
  private final Set<Integer> allStudents = new HashSet<>();
  private final List<ReportUser> deviceUsers;

  private final Map<Integer, Integer> userToProject;
  private final LogAndNotify logAndNotify;

  private final Map<Integer, String> idToUserID = new HashMap<>();

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_TEACHERS = false;

  /**
   * @param resultDAO
   * @param eventDAO
   * @param audioDAO
   * @param userToProject
   * @see DatabaseImpl#getReport
   */
  Report(IResultDAO resultDAO,
         IEventDAO eventDAO,
         IAudioDAO audioDAO,

         List<ReportUser> users,
         List<ReportUser> deviceUsers,
         Map<Integer, Integer> userToProject,
         String hostname,
         LogAndNotify logAndNotify) {
    this.resultDAO = resultDAO;
    this.eventDAO = eventDAO;
    this.audioDAO = audioDAO;
    this.users = users;

    users.forEach(user -> idToUserID.put(user.getID(), user.getUserID()));

    Set<ReportUser> foundLincoln = new HashSet<>();
    users.forEach(reportUser -> {
      Kind userKind = reportUser.getUserKind();
      if (userKind == Kind.STUDENT) {
        if (isLincoln(reportUser)) {
          foundLincoln.add(reportUser);
        } else {
          allStudents.add(reportUser.getID());
        }
      }
      if (userKind == Kind.TEACHER) allTeachers.add(reportUser.getID());
    });

    StringBuilder builder = new StringBuilder();
    foundLincoln.forEach(reportUser -> builder.append(reportUser.getUserID()).append(", "));
    //  logger.info("found lincoln users " + builder);

    this.deviceUsers = deviceUsers;
    this.userToProject = userToProject;
    this.logAndNotify = logAndNotify;
  }

  /**
   * in reference to projects
   * <p>
   * Sends a usage report to the email list at property {@link mitll.langtest.server.ServerProperties#getReportEmails()}.
   * Sends it out first thing every monday.
   * Subject disambiguates between multiple sites for the same language.
   * Also writes the report out to the report directory... TODO : necessary?
   *
   * @see ReportingServices#doReport
   * @see DatabaseImpl#getReportStats
   */
  @Override
  public List<ReportStats> doReport(int projid,
                                    String language,
                                    String site,

                                    PathHelper pathHelper,
                                    boolean forceSend,
                                    boolean getAllYears) {
    // check if it's a monday
    if (!getShouldSkip()) {
      int thisYear = getAllYears ? ALL_YEARS : getThisYear();
      return writeReport(projid, language, site, pathHelper, thisYear, forceSend);
    } else {
      return Collections.emptyList();
    }
  }

  private boolean getShouldSkip() {
 /*   try {
      InetAddress ip = InetAddress.getLocalHost();
      String hostName = ip.getHostName().toLowerCase();
      skipReport = hostName.contains(MITLL) || hostName.contains("hydra");
      if (skipReport) {
        logger.info("skip writing report while testing.... " + ip.getHostName());
      } else {
        logger.info("will write report");
      }
    } catch (UnknownHostException e) {
      logger.error("Got " + e, e);
      e.printStackTrace();
    }*/
    return false;
  }

  /**
   * @param projid
   * @param language
   * @param site
   * @param pathHelper
   * @param year       which year you want data for
   * @param forceSend  if true don't check if this is sunday morning for sending report
   * @see #doReport
   */
  private List<ReportStats> writeReport(int projid,
                                        String language,
                                        String site,
                                        PathHelper pathHelper,
                                        int year,
                                        boolean forceSend) {
    String today = new SimpleDateFormat("MM_dd_yy").format(new Date());
    File file = getReportFile(pathHelper, today, language, site, ".html");
    if (file.exists() && !forceSend) {
      logger.debug("writeReport already did report for " + today + " : " + file.getAbsolutePath());
      return Collections.emptyList();
    } else {
      logger.debug("writeReport Site real path " + site);
      try {
        // if (SEND_EACH_REPORT) sendEmails(stats, mailSupport, reportEmails);
        return writeReportToFile(file, new ReportStats(projid, language, site, year));
      } catch (Exception e) {
        logger.error("got " + e, e);
        return Collections.emptyList();
      }
    }
  }

  /**
   * @param pathHelper
   * @param allReports
   * @throws IOException
   * @see ReportingServices#doReport
   */
  @Override
  public JsonObject writeReportToFile(ReportStats reportStats, PathHelper pathHelper, List<ReportStats> allReports) throws IOException {
    File file = getReportPath(pathHelper, reportStats.getLanguage(), reportStats.getName(), ".html");
    List<ReportStats> reportStats1 = writeReportToFile(file, reportStats);
    logger.debug("writeReportToFile wrote to " + file.getAbsolutePath());

    allReports.addAll(reportStats1);

/*
    File file2 = getReportPath(pathHelper, reportStats.getLanguage(), reportStats.getName(), ".xlsx");
    new ReportToExcel(logAndNotify).toXLSX(reportStats1, new FileOutputStream(file2));
    logger.debug("writeReportToFile wrote to " + file2.getAbsolutePath());
*/
    //logger.debug("\n" + jsonObject.toString());
    return reportStats.getJsonObject();
  }

  /**
   * @param mailSupport
   * @param reportEmails
   * @param receiverNames
   * @param reportStats
   * @param pathHelper
   * @see DatabaseImpl#sendReports
   */
  @Override
  public void sendExcelViaEmail(MailSupport mailSupport,
                                List<String> reportEmails, List<String> receiverNames,
                                List<ReportStats> reportStats,
                                PathHelper pathHelper) {
    sendReports(mailSupport, reportEmails, receiverNames, getSummaryReport(reportStats, pathHelper));
  }

  private void sendReports(MailSupport mailSupport, List<String> reportEmails,
                           List<String> receiverNames, File summaryReport) {
    String subject = getFileName();
    String messageBody = "Hi,<br>Here is the current usage report for NetProF on " + getHostInfo() +
        ".<br>Thanks, Administrator";

    logger.info("sending excel to recipients " + reportEmails + " using file " + summaryReport.getAbsolutePath());

    if (!reportEmails.isEmpty() && !receiverNames.isEmpty()) {
      for (int i = 0; i < reportEmails.size(); i++) {
        String dest = reportEmails.get(i);
        String name = receiverNames.get(i);
        if (!mailSupport.emailAttachment(dest, subject, messageBody, summaryReport, name)) {
          logger.warn("couldn't send email to " + dest);
        }
      }
    }
  }

  /**
   * @param allReports
   * @param pathHelper
   * @return
   * @see DatabaseImpl#doReportForYear(int)
   */
  @Override
  public File getSummaryReport(List<ReportStats> allReports, PathHelper pathHelper) {
    try {
      File file2 = getReportPathDLI(pathHelper, ".xlsx");
      new ReportToExcel(logAndNotify).toXLSX(allReports, new FileOutputStream(file2));
      logger.info("writeReportToFile wrote to " + file2.getAbsolutePath());
      return file2;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      logAndNotify.logAndNotifyServerException(e);
      return null;
    }

  }

  /**
   * @param file        write html to a file
   * @param reportStats
   * @return html of report
   * @throws IOException
   * @see #writeReport
   */
  private List<ReportStats> writeReportToFile(File file, ReportStats reportStats) throws IOException {
    List<ReportStats> reportStats1 = getReport(reportStats);
    writeHTMLFile(file, reportStats);
    return reportStats1;
  }

  private void writeHTMLFile(File file, ReportStats reportStats) throws IOException {
    //logger.info("writeHTMLFile to " + file.getAbsolutePath());
    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
    writer.write(reportStats.getHtml());
    writer.close();
  }

  private File getReportPath(PathHelper pathHelper, String language, String site, String suffix) {
    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat(MM_DD_YY);
    String today = simpleDateFormat2.format(new Date());
    return getReportFile(pathHelper, today, language, site, suffix);
  }

  @Override
  public File getReportPathDLI(PathHelper pathHelper, String suffix) {
    return new File(getReportsDir(pathHelper), getFileName() + suffix);
  }

  @NotNull
  private String getFileName() {
    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat(YYYY_MM_DD);
    String today = simpleDateFormat2.format(new Date());
    return today + DLIFLC_NET_PRO_F_QUICK_LOOK_SUMMARY;
  }

  private File getReportFile(PathHelper pathHelper, String today, String language, String site, String suffix) {
    String fileName = site + "_" + language + "_report_" + today + suffix;
    return new File(getReportsDir(pathHelper), fileName);
  }

  @NotNull
  private File getReportsDir(PathHelper pathHelper) {
    File reports = pathHelper.getAbsoluteFile("reports");
    if (!reports.exists()) {
      logger.debug("making dir " + reports.getAbsolutePath());
      reports.mkdirs();
    } else {
      // logger.debug("reports dir exists at " + reports.getAbsolutePath());
    }
    return reports;
  }

  /**
   * @param projects
   * @param jsonObject
   * @param year
   * @param allReports
   * @return
   * @see DatabaseImpl#getReport(int, JsonObject)
   */
  @Override
  public String getAllReports(Collection<SlickProject> projects, JsonObject jsonObject, int year, List<ReportStats> allReports) {
    StringBuilder builder = new StringBuilder();
    builder.append(getHeader("All Languages", "All Projects"));
    projects.forEach(project ->
        allReports.addAll(getReportForProject(new ReportStats(project, year, jsonObject), builder, true))
    );

    builder.append(FOOTER);
    return builder.toString();
  }

  /**
   * @param reportStats
   * @return
   * @see #doReport
   */
  private List<ReportStats> getReport(ReportStats reportStats) {
    StringBuilder builder = new StringBuilder();
    builder.append(getHeader(reportStats.getLanguage(), reportStats.getName()));

    List<ReportStats> reportsForProject = getReportForProject(reportStats, builder, false);

    builder.append(FOOTER);
    reportStats.setHtml(builder.toString());
    return reportsForProject;
  }

  /**
   * @param stats                - if year = ALL_YEARS generates a sequence of reports for all years of data
   * @param builder
   * @param includeProjectHeader
   * @return list of reports for each year requested
   * @see #getAllReports
   */
  private List<ReportStats> getReportForProject(ReportStats stats,
                                                StringBuilder builder,
                                                boolean includeProjectHeader) {
    int projid = stats.getProjid();
    String language = stats.getLanguage();
    String projectName = stats.getName();
    int year = stats.getYear();


    List<SlickSlimEvent> allSlim = eventDAO.getAllSlim(projid);
    List<SlickSlimEvent> allDevicesSlim = eventDAO.getAllDevicesSlim(projid);
    Collection<UserTimeBase> audioAttributes = audioDAO.getAudioForReport(projid);
    List<MonitorResult> results = resultDAO.getMonitorResults(projid);
    Collection<MonitorResult> resultsDevices = resultDAO.getResultsDevices(projid);

    Set<Integer> usersOnProject = new HashSet<>();
    userToProject.forEach((key, value) -> {
      if (value == projid) usersOnProject.add(key);
    });

    if (includeProjectHeader) builder.append(getProjectHeader(language, projectName, ""));

    List<ReportStats> reportStats = new ArrayList<>();

    {
      JsonObject jsonObject = stats.getJsonObject();
      jsonObject.addProperty(HOST, getHostInfo());

      JsonArray dataArray = new JsonArray();
      if (year == ALL_YEARS) {
        int firstYear = getFirstYear(getEarliest(projid));
        if (firstYear < EARLIEST_YEAR) firstYear = EARLIEST_YEAR;
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);

        logger.info(language + " doReportForYear for " + firstYear + "->" + thisYear);

        for (int i = firstYear; i <= thisYear; i++) {
          ReportStats reportForYear = new ReportStats(stats);
          reportForYear.setYear(i);
          reportStats.add(reportForYear);
          addYear(dataArray, builder, i, allSlim, allDevicesSlim,
              audioAttributes, results, resultsDevices, language,
              usersOnProject,
              reportForYear);
        }
      } else {
        reportStats.add(stats);
        addYear(dataArray, builder, year, allSlim, allDevicesSlim,
            audioAttributes, results, resultsDevices, language,
            usersOnProject,
            stats);
      }
      jsonObject.add(DATA, dataArray);
    }
    return reportStats;
  }

  private long getEarliest(int projid) {
    SlickSlimEvent firstSlim = eventDAO.getFirstSlim(projid);
    long timestamp = (firstSlim != null) ? firstSlim.modified() : System.currentTimeMillis();
    long firstTime = resultDAO.getFirstTime(projid);
    timestamp = timestamp < firstTime ? timestamp : firstTime;
    return timestamp;
  }

  private int getFirstYear(long timestamp) {
    Calendar instance = Calendar.getInstance();
    instance.clear();
    instance.setTimeInMillis(timestamp);
    return instance.get(Calendar.YEAR);
  }

  /**
   * @param dataArray
   * @param builder
   * @param i
   * @param allSlim
   * @param allDevicesSlim
   * @param audioAttributes
   * @param results
   * @param resultsDevices
   * @param language
   * @param usersForProject
   * @see #getReportForProject
   */
  private void addYear(JsonArray dataArray,
                       StringBuilder builder,
                       int i,
                       List<SlickSlimEvent> allSlim,
                       List<SlickSlimEvent> allDevicesSlim,
                       Collection<UserTimeBase> audioAttributes,
                       Collection<MonitorResult> results,
                       Collection<MonitorResult> resultsDevices,
                       String language,
                       Collection<Integer> usersForProject,
                       ReportStats reportStats) {
    JsonObject forYear = new JsonObject();
    builder.append("<h1>").append(i).append("</h1>");
    builder.append(getReportForYear(forYear,
        i, allSlim, allDevicesSlim,
        audioAttributes, results, resultsDevices, language, getFileName(), usersForProject, reportStats));
    dataArray.add(forYear);
  }

  /**
   * @param jsonObject
   * @param year
   * @param language
   * @param name
   * @param usersForProject
   * @return
   * @see #addYear
   * @see DatabaseImpl#getReport(int, JsonObject)
   */
  private String getReportForYear(JsonObject jsonObject,
                                  int year,
                                  List<SlickSlimEvent> allSlim,
                                  List<SlickSlimEvent> allDevicesSlim,
                                  //  Map<Integer, List<AudioAttribute>> exToAudio,
                                  Collection<UserTimeBase> audioAttributes,
                                  Collection<MonitorResult> results,
                                  Collection<MonitorResult> resultsDevices,
                                  String language,
                                  String name, Collection<Integer> usersForProject,
                                  ReportStats reportStats) {
    jsonObject.addProperty("forYear", year);

    long then = System.currentTimeMillis();
    if (DEBUG) logger.info(language + " : doing year " + year);

    setUserStart(allSlim);

    StringBuilder builder = new StringBuilder();
    Set<Integer> users = getUserIDs(jsonObject, year, usersForProject, builder, language);

    {
      JsonObject iPadUsers = new JsonObject();
      getUsers(builder, getIOSUsers(usersForProject), NEW_I_PAD_I_PHONE_USERS, iPadUsers, year, false, language);
      jsonObject.add(I_PAD_USERS, iPadUsers);
    }

    JsonObject timeOnTaskJSON = new JsonObject();
    Set<Integer> events = getEvents(builder, users, timeOnTaskJSON, year, allSlim);
    jsonObject.add(OVERALL_TIME_ON_TASK, timeOnTaskJSON);

    JsonObject deviceTimeOnTaskJSON = new JsonObject();
    Set<Integer> eventsDevices = getEventsDevices(builder, users, deviceTimeOnTaskJSON, year, allDevicesSlim);
    jsonObject.add(DEVICE_TIME_ON_TASK, deviceTimeOnTaskJSON);

    events.addAll(eventsDevices);

    logger.info(language + " : doing year " + year + " got " + results.size() + " recordings");
    addRecordings(jsonObject, year, results, builder, users, reportStats, language, name);

    addDeviceRecordings(jsonObject, year, resultsDevices, builder, users, reportStats, language, name);

    Calendar calendar = getCalendarForYear(year);
//    Date january1st = getJanuaryFirst(calendar, year);
    //   Date january1stNextYear = getNextYear(year);

//    if (DEBUG) {
//      logger.info("doReportForYear : between " + january1st + " and " + january1stNextYear);
//    }
    addReferenceRecordings(jsonObject, year, audioAttributes, builder, calendar);
//    addBrowserReport(jsonObject, year, usersForProject, builder);

    long now = System.currentTimeMillis();
    long l = now - then;
    if (l > 100) {
      logger.info(language + " took " + l + " millis to generate report for " + year);
    }
    return builder.toString();
  }

/*  private void addBrowserReport(JsonObject jsonObject, int year, Collection<Integer> usersForProject, StringBuilder builder) {
    JsonObject browserReport = new JsonObject();
    getBrowserReport(getValidUsers(fixUserStarts(usersForProject)), year, browserReport, builder);
    jsonObject.put(HOST_INFO, browserReport);
  }*/

  /**
   * @param jsonObject
   * @param year
   * @param results
   * @param builder
   * @param users
   * @param reportStats
   * @param language
   * @param name
   * @see #getReportForYear(JsonObject, int, List, List, Collection, Collection, Collection, String, String, Collection, ReportStats)
   */
  private void addRecordings(JsonObject jsonObject, int year,
                             Collection<MonitorResult> results, StringBuilder builder,
                             Set<Integer> users, ReportStats reportStats, String language, String name) {
    JsonObject allRecordings = new JsonObject();
    getResults(builder, users, allRecordings, year, results, reportStats, language, name);
    jsonObject.add(ALL_RECORDINGS1, allRecordings);
  }

  private void addDeviceRecordings(JsonObject jsonObject, int year,
                                   Collection<MonitorResult> resultsDevices,
                                   StringBuilder builder, Set<Integer> users, ReportStats reportStats, String language, String name) {
    JsonObject deviceRecordings = new JsonObject();
    getResultsDevices(builder, users, deviceRecordings, year, resultsDevices, reportStats, language, name);
    jsonObject.add(DEVICE_RECORDINGS1, deviceRecordings);
  }

  private void addReferenceRecordings(JsonObject jsonObject, int year, Collection<UserTimeBase> audioAttributes,
                                      StringBuilder builder, Calendar calendar) {
    JsonObject referenceRecordings = new JsonObject();
    addRefAudio(builder, calendar, audioAttributes, referenceRecordings, year);
    jsonObject.add(REFERENCE_RECORDINGS, referenceRecordings);
  }

  @NotNull
  private List<ReportUser> getIOSUsers(Collection<Integer> usersForProject) {
    // ipad users
    List<ReportUser> filteredDevices = new ArrayList<>();
    deviceUsers.forEach(reportUser -> {
      if (usersForProject.contains(reportUser.getID())) {
        filteredDevices.add(reportUser);
      }
    });
    return filteredDevices;
  }

  /**
   * @param jsonObject
   * @param year
   * @param usersForProject
   * @param builder
   * @param language
   * @return
   * @see #getReportForYear(JsonObject, int, List, List, Collection, Collection, Collection, String, String, Collection, ReportStats)
   */
  private Set<Integer> getUserIDs(JsonObject jsonObject,
                                  int year,
                                  Collection<Integer> usersForProject,
                                  StringBuilder builder,
                                  String language) {
    // all users
    JsonObject allUsers = new JsonObject();
    Set<Integer> users = getUsers(builder, allUsers, year, usersForProject, language);
    jsonObject.add(ALL_USERS, allUsers);
    return users;
  }

  private String getHeader(String language, String projectName) {
    String hostInfo = getHostInfo();
    return "<html><head>" +
        "<title>Report for " + language + " on " + hostInfo + "</title>" +
        "<body>" +
        getProjectHeader(language, projectName, hostInfo);
  }

  private String getProjectHeader(String language, String projectName, String hostInfo) {
    return
        (hostInfo.isEmpty() ? "" : ("<h2>Host     : " + hostInfo + "</h2>\n")) +
            "<h2>Language : " + language + "</h2>\n" +
            "<h2>Project  : " + projectName + "</h2>\n";
  }

  private final Map<String, ReadableUserAgent> userAgentToReadable = new HashMap<>();

  /**
   * @param fusers
   * @param year
   * @param section
   * @param document
   * @see #getReport
   */
  private void getBrowserReport(List<ReportUser> fusers, int year, JsonObject section, StringBuilder document) {
    List<ReportUser> usersByYear = filterUsersByYear(fusers, year);

    UserAgentStringParser parser = UADetectorServiceFactory.getResourceModuleParser();

    Map<String, Integer> osToCount = new HashMap<>();
    Map<String, Integer> hostToCount = new HashMap<>();
    Map<String, Integer> familyToCount = new HashMap<>();
    Map<String, Integer> browserToCount = new HashMap<>();
    Map<String, Integer> browserVerToCount = new HashMap<>();
    int miss = 0;
    for (ReportUser f : usersByYear) {
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
    JsonArray hostArray = new JsonArray();
    JsonArray familyArray = new JsonArray();
    JsonArray browserArray = new JsonArray();
    JsonArray browserVerArray = new JsonArray();

    document.append(getWrapper(OPERATING_SYSTEM, getCountTable(NAME, COUNT, familyArray, NAME, getSorted(familyToCount))));
    document.append(getWrapper(OPERATING_SYSTEM_VERSION, getCountTable(NAME, COUNT, hostArray, NAME, getSorted(hostToCount))));
    document.append(getWrapper(BROWSER, getCountTable(NAME, COUNT, browserArray, NAME, getSorted(browserToCount))));
    document.append(getWrapper(BROWSER_VERSION, getCountTable(NAME, COUNT, browserVerArray, NAME, getSorted(browserVerToCount))));

    section.add(OPERATING_SYSTEM, familyArray);
    section.add(OPERATING_SYSTEM_VERSION, hostArray);
    section.add(BROWSER, browserArray);
    section.add(BROWSER_VERSION, browserVerArray);

    if (miss > 0)
      logger.info("getBrowserReport for " + year + " users by year " + userAgentToReadable.size() + " miss " + miss);
  }

  private List<ReportUser> filterUsersByYear(List<ReportUser> fusers, int year) {
    Calendar calendar = getCalendarForYear(year);
    YearTimeRange yearTimeRange = new YearTimeRange(year, calendar).invoke();
    List<ReportUser> forYear = new ArrayList<>();
    for (ReportUser t : fusers) if (yearTimeRange.inYear(t.getTimestampMillis())) forYear.add(t);
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

  /**
   * @param builder
   * @param usersForProject
   * @param language
   * @return
   * @see #getReport
   */
  private Set<Integer> getUsers(StringBuilder builder, JsonObject jsonObject, int year,
                                Collection<Integer> usersForProject, String language) {
    return getUsers(
        builder,
        fixUserStarts(usersForProject),
        ALL_NEW_USERS,
        jsonObject,
        year,
        true, language);
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
   * @param forThisProject
   * @return
   * @see #setUserStart(Collection)
   */
  private List<ReportUser> fixUserStarts(Collection<Integer> forThisProject) {
    List<ReportUser> forProject = new ArrayList<>();
    users.forEach(reportUser -> {
      if (forThisProject.contains(reportUser.getID())) forProject.add(reportUser);
    });
    for (ReportUser user : forProject) {
//      Long aLong = userToStart.get(user.getID());
//      if (aLong != null) user.setTimestampMillis(aLong);
      idToUser.put(user.getID(), user.getUserID());
      //else {
      //  logger.error("no events for " + user.getExID());
      //  }
    }
    return forProject;
  }

/*  private List<ReportUser> getValidUsers(List<ReportUser> all) {
    List<ReportUser> valid = new ArrayList<>();
    for (ReportUser u : all) if (!shouldSkipUser(u)) valid.add(u);
    return valid;
  }*/

  /**
   * @param builder
   * @param users
   * @param users1
   * @param language
   * @return set of valid users
   * @see IReport#doReport
   * @see #getUsers
   */
  private Set<Integer> getUsers(StringBuilder builder,
                                Collection<ReportUser> users,
                                String users1,

                                JsonObject jsonObject,
                                int year,
                                boolean reportTeachers, String language) {
    Calendar calendar = getCalendarForYear(year);
    YearTimeRange yearTimeRange = new YearTimeRange(year, calendar).invoke();
    int ytd = 0;
    int tytd = 0;

    Counts counts = new Counts(year);
    Map<Integer, Integer> monthToCount = counts.getMonthToCount();
    Map<Integer, Integer> weekToCount = counts.getWeekToCount();

    Set<Integer> students = new HashSet<>();

    Counts tcounts = new Counts(year);
    Map<Integer, Integer> tmonthToCount = tcounts.getMonthToCount();
    Map<Integer, Integer> tweekToCount = tcounts.getWeekToCount();

    //if (!users.isEmpty()) {
    if (DEBUG) logger.info(language + " : " + users1 + " examining " + users.size() + " users - year " + year);
    // }

    for (ReportUser user : users) {
      Kind userKind = user.getUserKind();
      boolean isStudent = userKind == Kind.STUDENT;
      boolean isTeacher = userKind == Kind.TEACHER;

      if (isTeacher || isLincoln(user)) isStudent = false;

      if (shouldSkipUser(user)) {
        if (SHOW_TEACHER_SKIPS) logger.warn(language + " skipping a priori user " + getUserInfo(user));
        continue;
      }

      long userCreated = user.getTimestampMillis();
      boolean inYear = yearTimeRange.inYear(userCreated);
      if (isStudent) {
        students.add(user.getID());
        //    logger.info(language + " student " + user.getID() + " " + user.getUserID());
        if (inYear) {
          ytd++;
          countByWeekAndMonth(calendar, monthToCount, weekToCount, userCreated);
        } else {
          //   logger.debug("NO time " +user.getTimestamp() + " " + parse);
        }
        //}
      } else {
        if (isTeacher) {
//          logger.info(language + " teacher " + user.getID() + " " + user.getID());
          if (inYear) {
            tytd++;
            countByWeekAndMonth(calendar, tmonthToCount, tweekToCount, userCreated);
          }
        }
        if (SHOW_TEACHER_SKIPS) logger.warn(language + " : skipping teacher " + getUserInfo(user));
      }
    }
    {
      int monthTotal = 0;
      for (Integer count : monthToCount.values()) monthTotal += count;

      int weekTotal = 0;
      for (Integer count : weekToCount.values()) weekTotal += count;

      if (monthTotal != weekTotal) {
        logger.info("\nusers month total " + monthTotal + " week total " + weekTotal);
        logger.info("\nweeks" + weekToCount);
        logger.info("\nusers " + weekToCount.keySet());
      }
    }
    //logger.info("users " + weekToCount.keySet());

    builder.append(getSectionReport(ytd, monthToCount, weekToCount, users1, jsonObject, year));
    if (reportTeachers) {
      builder.append(getSectionReport(tytd, tmonthToCount, tweekToCount, "New Teachers", jsonObject, year));
    }
    return students;
  }

  @NotNull
  private String getUserInfo(ReportUser user) {
    return user.getID() + " " + user.getUserID() + " " + user.getUserKind();
  }

  /**
   * @param user
   * @return
   * @see #getUsers(StringBuilder, Collection, String, JsonObject, int, boolean, String)
   */
  private boolean isLincoln(ReportUser user) {
    boolean contains = false;
    for (String ll : lincoln) {
      if (user.getUserID().startsWith(ll)) {
        contains = true;
        break;
      }
    }
    return contains;
  }

  private void countByWeekAndMonth(Calendar calendar, Map<Integer, Integer> monthToCount, Map<Integer, Integer> weekToCount, long userCreated) {
    calendar.setTimeInMillis(userCreated);

    int month = calendar.get(Calendar.MONTH);
    tallyWeek(monthToCount, month);

    int w = calendar.get(Calendar.WEEK_OF_YEAR);
    tallyWeek(weekToCount, w);
  }

  private void ensureYTDEntries0(int year, Map<Integer, Long> monthToCount) {
    Calendar today = getCalendarForYearOrNow();
    int thisMonth = today.get(Calendar.MONTH);
    for (int i = 0; i <= thisMonth; i++) monthToCount.put(i, 0L);

    if (isNotThisYear(year)) {
      for (int i = 0; i < 12; i++) monthToCount.put(i, 0L);
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
    for (int i = 0; i <= thisWeek; i++) weekToCount.put(i, 0L);

    if (isNotThisYear(year)) {
      for (int i = 0; i < EVIL_LAST_WEEK; i++) weekToCount.put(i, 0L);
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

  private boolean shouldSkipUser(ReportUser user) {
    return
        user.getID() == 3 ||
            user.getID() == 1 ||
            lincoln.contains(user.getUserID()) ||
            user.getUserID().startsWith(SKIP_USER);
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
                                  Map<Integer, Integer> monthToCount,
                                  Map<Integer, Integer> weekToCount,
                                  String users1,
                                  JsonObject jsonObject,
                                  int year) {
    JsonObject yearJSON = new JsonObject();
    String yearCol = getYTD(ytd, users1, yearJSON, year);
    jsonObject.add(YEAR, yearJSON);

    JsonArray monthArray = new JsonArray();
    String monthCol = getMonthToCount(monthToCount, MONTH, users1, "", monthArray, year);
    jsonObject.add(MONTH1, monthArray);

    JsonArray weekArray = new JsonArray();
    String weekCol = getWC(weekToCount, users1, weekArray, year);

    if (DEBUG) {
      logger.info("getSectionReport :" +
          "\n\tsection     '" + users1 + "' " +
          "\n\tyear        " + year +
          "\n\tweek->count " + weekToCount);
    }

    jsonObject.add(WEEK1, weekArray);
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

  private String getYTD(int ytd, String users1, JsonObject jsonObject, int year) {
    jsonObject.addProperty("label", users1);
    jsonObject.addProperty(YEAR, year);
    jsonObject.addProperty(YTD, ytd);

    String suffix = isCurrentYear(year) ? YTD1 : "";
    return "<table style='background-color: #eaf5fb'>" +
        "<tr>" +
        "<th>" + users1 + suffix + "</th>" +
        "</tr>" +
        "<tr>" +
        "<td>" + ytd + "</td>" +
        "</tr>" +
        "</table><br/>\n";
  }

  private boolean isCurrentYear(int year) {
    return year == getThisYear();
  }

  /**
   * @param monthToCount
   * @param unit
   * @param count
   * @param tableLabel
   * @param jsonArray
   * @return html for month
   */
  private String getMonthToCount(Map<Integer, Integer> monthToCount,
                                 String unit,
                                 String count,
                                 String tableLabel,
                                 JsonArray jsonArray,
                                 int year) {
    // writeMonthToCSV(monthToCount, tableLabel.isEmpty() ? childCount : tableLabel, language, year);
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
  private String getIntCountTable(String unit, String count, JsonArray jsonArray, String label, Map<Integer, Integer> monthToValue) {
    StringBuilder s = new StringBuilder();
    Integer max = getMax(monthToValue);

    for (int month = 0; month <= max; month++) {
      Number value = monthToValue.get(month);
//      if (value instanceof Collection<?>) {
//        value = ((Collection<?>) value).size();
//      }
      if (value == null) value = 0;
      int i = month + 1;
      addJsonRow(jsonArray, label, value, "" + i);
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
   * @see #getBrowserReport(List, int, JsonObject, StringBuilder)
   */
  private String getCountTable(String unit, String count, JsonArray jsonArray, String label, Map<String, Integer> monthToValue) {
    String s = "";
    for (Map.Entry<String, Integer> pair : monthToValue.entrySet()) {
      Integer value = pair.getValue();
//      if (value instanceof Collection<?>) {
//        value = ((Collection<?>) value).size();
//      }
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

  private void addJsonRow(JsonArray jsonArray, String label, Number value, String month) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(label, month);
    jsonObject.addProperty(COUNT, value);
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
   * @param count
   * @return
   * @see #getSectionReport(int, Map, Map, String, JsonObject, int)
   * @see #getEvents
   */
  private String getWC(Map<Integer, ?> weekToCount,
                       String count,
                       JsonArray jsonArray,
                       int year) {
    String s = "";
    Calendar calendar = getCalendarForYear(year);
    Integer max = getMax(weekToCount);
    long initial = calendar.getTimeInMillis();

    SimpleDateFormat df = new SimpleDateFormat(WEEKLY_FORMAT);
    SimpleDateFormat fullFormat = new SimpleDateFormat(MM_DD_YY1);

//    logger.info(unit +" before  " + year + " = " + calendar.getTime() + " or " + df.format(calendar.getTime()));
    for (int week = 1; week <= max; week++) {
      Integer value = getCountAtWeek(weekToCount, week);
      //   logger.info("getWC before week " +week + " = " + calendar.getTime() + " or " + df.format(calendar.getTime()));

      Date time = getThisWeek(year, calendar, week);
      boolean before = time.getTime() < initial;
      String format1 = before ? fullFormat.format(time) : df.format(time);

      //logger.info("getWC after  week " + week + " = " + time + " " + time.getTime() +" or " + format1 + " = " + value);
      addJSONForWeek(jsonArray, week, value, fullFormat.format(time));

      s += "<tr><td>" +
          "<span>" + format1 +
          "</span>" +
          "</td><td>" + value + "</td></tr>";
    }
    return "<table style='background-color: #eaf5fb'>" +
        "<tr>" +
        "<th>" +
        WEEK +
        "</th>" +
        "<th>" + count + "</th>" +
        "</tr>" +
        s +
        "</table><br/>\n";
  }

  /**
   * @param weekToCount
   * @param year
   * @return
   * @see #getResultsForSet
   */
  private Map<String, Integer> getWeekToCount(Map<Integer, ?> weekToCount,
                                              int year,
                                              String language) {
    if (DEBUG) logger.info("getWeekToCount " + year + " num weeks = " + weekToCount.size());
    Calendar calendar = getCalendarForYear(year);
    Integer max = getMax(weekToCount);
    SimpleDateFormat df = new SimpleDateFormat(WEEKLY_FORMAT);

    Map<String, Integer> weekToCountFormatted = new TreeMap<>();

    for (int week = 1; week <= max; week++) {
      String format1 = df.format(getThisWeek(year, calendar, week));
      Integer countAtWeek = getCountAtWeek(weekToCount, week);
      if (DEBUG && year == 2018)
        logger.info("getWeekToCount " + language + " year " + year + "  week " + week + " = " + format1 + " = " + countAtWeek);
      weekToCountFormatted.put(format1, countAtWeek);
    }

    return weekToCountFormatted;
  }

  private void addJSONForWeek(JsonArray jsonArray,
                              int week,
                              Integer value,
                              String format) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(WEEK_OF_YEAR, week);
    jsonObject.addProperty(COUNT, value);
    jsonObject.addProperty(DATE, format);
    jsonArray.add(jsonObject);
  }

  @NotNull
  private Integer getCountAtWeek(Map<Integer, ?> weekToCount, int week) {
    Object value = weekToCount.get(week);
    if (value == null) return 0;
    else if (value instanceof Collection<?>) {
      return ((Collection<?>) value).size();
    }
    return value instanceof Integer ? (Integer) value : ((Long) value).intValue();
  }

  /**
   * The week runs from Sunday->Saturday - saturday is the last day of the week (7)
   * For example, 1/06/18 is a saturday and the last day of the first week of 2018.
   *
   * @param year
   * @param calendar
   * @param week
   * @return
   */
  @NotNull
  private Date getThisWeek(int year, Calendar calendar, int week) {
    calendar.set(Calendar.WEEK_OF_YEAR, week);
    calendar.set(Calendar.DAY_OF_WEEK, 7);
    calendar.set(Calendar.YEAR, year);

    if (calendar.get(Calendar.YEAR) != year) {
      // logger.warn("getThisWeek after year is " + calendar.get(Calendar.YEAR) + " vs " + year);
      calendar.set(Calendar.YEAR, year);
      calendar.set(Calendar.MONTH, 11); // 11 = december
      calendar.set(Calendar.DAY_OF_MONTH, 31); // new years eve
    }
    return calendar.getTime();
  }

  /**
   * @param builder
   * @param year
   * @param language
   * @see #addRecordings(JsonObject, int, Collection, StringBuilder, Set, ReportStats, String, String)
   */
  private void getResults(StringBuilder builder,
                          Set<Integer> students,
                          JsonObject jsonObject,
                          int year,
                          Collection<MonitorResult> results,
                          ReportStats reportStats,
                          String language,
                          String name) {
    getResultsForSet(builder, students, results, ALL_RECORDINGS, jsonObject, year, reportStats, language, name);
  }

  private void getResultsDevices(StringBuilder builder, Set<Integer> students,
                                 JsonObject jsonObject, int year,
                                 Collection<MonitorResult> results,
                                 ReportStats reportStats, String language,
                                 String name) {
    getResultsForSet(builder, students, results, DEVICE_RECORDINGS, jsonObject, year, reportStats, language, name);
  }

  private void getResultsForSet(StringBuilder builder,
                                Set<Integer> students,
                                Collection<MonitorResult> results,
                                String recordings,
                                JsonObject jsonObject,
                                int year,
                                ReportStats reportStats,
                                String language,

                                String name) {
    YearTimeRange yearTimeRange = new YearTimeRange(year, getCalendarForYear(year)).invoke();

    int ytd = 0;
    Counts counts = new Counts(year);
    Map<Integer, Integer> monthToCount = counts.getMonthToCount();
    Map<Integer, Integer> weekToCount = counts.getWeekToCount();

    Map<Integer, Map<String, Integer>> userToDayToCount = new TreeMap<>();

    int teacherAudio = 0;
    int invalid = 0;
    int invalidScore = 0;

    int beforeJanuary = 0;
    Set<Integer> skipped = new TreeSet<>();
    int size = results.size();

    if (DEBUG) {
      logger.info("getResultsForSet " +
          "\n\tlanguage     " + language +
          "\n\tname         " + name +
          "\n\tyear         " + year +
          "\n\tStudents num " + students.size() +
          "\n\trecordings   " + recordings
      );
    }

    Map<Integer, Integer> idToCount = new HashMap<>();
    Map<Integer, Set<MonitorResult>> userToRecordings = new HashMap<>();

    int firstDayOfWeek = Calendar.getInstance(Locale.US).getFirstDayOfWeek();
    if (DEBUG) logger.info("getResultsForSet first day of week " + firstDayOfWeek + " vs monday " + Calendar.MONDAY);

    Map<Integer, Integer> weekToTeacher = new HashMap<>();
    Map<Integer, Integer> weekToTooShort = new HashMap<>();
    Map<Integer, Integer> weekToInvalid = new HashMap<>();
    Map<Integer, Integer> weekToAll = new HashMap<>();
    Map<Integer, Integer> weekToValid = new HashMap<>();
    try {
      BufferedWriter writer = null;
      Calendar calendar = getCalendarForYear(year);

      Set<String> seen = new HashSet<>();
      for (MonitorResult result : results) {
        long timestamp = result.getTimestamp();


        if (yearTimeRange.inYear(timestamp)) {  // if it's in the requested year
          calendar.setTimeInMillis(timestamp);
          int w = calendar.get(Calendar.WEEK_OF_YEAR);
          boolean firstWeeks = DEBUG & year == 2015 && w == 2;// w==5;

          if (!isRefAudioResult(result)) {      // and not ref audio
            tallyWeek(weekToAll, w);
            if (result.isValid()) {             // and valid
              tallyWeek(weekToValid, w);
              int userid = result.getUserid();
              if (isStudentUser(students, userid)) {  // and by a student
                if (isAboveMinDuration(result, seen)) {    // and is long enough
                  ytd++;
                  tallyByMonthAndWeek(calendar, monthToCount, weekToCount, result, userToDayToCount);
                  if (firstWeeks) {
                    logger.info("getResultsForSet " +
                        " year " + year +
                        " name " + name +
                        " weekToCount " + weekToCount.get(w) +
                        " week        " + w +
                        " include " + result.getUniqueID() + " " + new Date(result.getTimestamp()));
                  }
                  seen.add(result.getAnswer());
                } else {  // too short
                  if (firstWeeks) logger.warn(w + " min duration too short " + result);
                  invalidScore++;
                  tallyWeek(weekToTooShort, w);
                }
              } else {  // it's a teacher
                if (firstWeeks) {
                  logger.warn(w + " teacher score " + result);
                }
                tallyWeek(weekToTeacher, w);

                boolean add = skipped.add(userid);
                if (add) {
                  String userInfo = getUserInfo(userid);
                  if (DEBUG) {
                    logger.info(language + " " + w +
                        " skipping not a student " +
                        userInfo + "\n\tresult id " + result.getUniqueID() + " on " + new Date(result.getTimestamp()));
                  }

                  if (DEBUG_TEACHERS && !allTeachers.contains(userid) && !lincoln.contains(userInfo)) {
                    logger.info("getResultsForSet hmm " + userInfo + " is not a teacher?");
                  }
                }

                tallyWeek(idToCount, userid);
                Set<MonitorResult> orDefault = userToRecordings.getOrDefault(userid, new HashSet<>());
                orDefault.add(result);
                userToRecordings.put(userid, orDefault);

                teacherAudio++;
              }
            } else { //it's invalid
              if (DEBUG && firstWeeks) logger.warn(w + " invalid score " + result);
              invalid++;
              tallyWeek(weekToInvalid, w);
            }
          } else {  // ref audio

          }
        } else {  // wrong year
          beforeJanuary++;
        }
      }
      if (WRITE_RESULTS_TO_FILE) {
        writer.close();
      }
    } catch (IOException e) {
      logger.error("got " + e, e);
    }

    if (DEBUG) {
      logger.debug("getResultsForSet" +
          "\n\tyear     " + year +
          "\n\tout of   " + size +
          "\n\tSkipped  " + invalid + " invalid recordings, " +
          "\n\tinvalid  " + invalidScore + " -1 score items, " +
          //          (me > 0 ? "\n\tgvidaver " + me + " by gvidaver, " : "") +
          "\n\tbefore   " + beforeJanuary + " beforeJan1st" +
          "\n\tweek->all       " + weekToAll +
          "\n\tweek->valid     " + weekToValid +
          "\n\tweekToCount     " + weekToCount +
          "\n\tweek->teacher   " + weekToTeacher +
          "\n\tweek->too short " + weekToTooShort +
          "\n\tweek->invalid   " + weekToInvalid
      );
    }
    if (teacherAudio > 0) {
      StringBuilder builder1 = new StringBuilder();
      builder1.append("\n");
      for (Integer skip : skipped) {
        builder1
            .append(skip)
            .append("/")
            .append(idToUser.get(skip))
            .append("\t")
            .append(idToCount.get(skip))
            .append("\n");
      }
      if (DEBUG) logger.debug("getResultsForSet skipped " + teacherAudio + " teacher recordings by " + builder1);
    }
    //logger.debug("userToDayToCount " + userToDayToCount.size());

    builder.append("\n<br/><span>Valid student recordings</span>");
    builder.append(
        getSectionReport(ytd, monthToCount, weekToCount, recordings, jsonObject, year)
    );

    boolean allRecordings = recordings.equalsIgnoreCase(ALL_RECORDINGS);
    reportStats.putInt(allRecordings ? INFO.ALL_RECORDINGS : INFO.DEVICE_RECORDINGS, ytd);

    if (allRecordings) {
      reportStats.putIntMulti(INFO.ALL_RECORDINGS_WEEKLY, getWeekToCount(weekToCount, year, language));
    } else {
      reportStats.putIntMulti(INFO.DEVICE_RECORDINGS_WEEKLY, getWeekToCount(weekToCount, year, language));
    }
  }

  @NotNull
  private String getUserInfo(int userid) {
    return userid + "/" + idToUserID.get(userid);
  }

  private void tallyWeek(Map<Integer, Integer> weekToAll, int w) {
    weekToAll.put(w, weekToAll.getOrDefault(w, 0) + 1);
  }

  private boolean isStudentUser(Set<Integer> students, int userid) {
    return students.contains(userid) || allStudents.contains(userid);
  }

  /**
   * Don't childCount the same audio twice.
   *
   * @param result
   * @param seen
   * @return
   */
  private boolean isAboveMinDuration(MonitorResult result, Set<String> seen) {
    return
        result.getDurationInMillis() > MIN_DURATION &&
            !seen.contains(result.getAnswer());
  }

  /**
   * @param builder
   * @param calendar
   * @param refAudio
   * @param jsonObject
   * @see #getReport
   */
  private <T extends UserTimeBase> void addRefAudio(StringBuilder builder,
                                                    Calendar calendar,
                                                    Collection<T> refAudio,
                                                    JsonObject jsonObject, int year) {

    logger.info("addRefAudio " + refAudio.size() + " recordings from " + year);

    int ytd = 0;
    Counts counts = new Counts(year);
    Map<Integer, Integer> monthToCount = counts.getMonthToCount();
    Map<Integer, Integer> weekToCount = counts.getWeekToCount();

    Map<Integer, Map<String, Integer>> userToDayToCount = new TreeMap<>();

    YearTimeRange yearTimeRange = new YearTimeRange(year, getCalendarForYear(year)).invoke();

//    int n = 10;
    for (T result : refAudio) {
      if (yearTimeRange.inYear(result.getTimestamp())) {
        ytd++;
        tallyByMonthAndWeek(calendar, monthToCount, weekToCount, result, userToDayToCount);
      }
//      else if (n-- > 0) {
//        logger.warn("not right year " + new Date(result.getTimestamp()));
//      }
    }
    if (!refAudio.isEmpty() && ytd == 0) {
      logger.warn("addRefAudio huh? from ref audio " + refAudio.size() + " we found nothing for year " + year);
    }

    int monthTotal = 0;
    for (Integer count : monthToCount.values()) monthTotal += count;

    int weekTotal = 0;
    for (Integer count : weekToCount.values()) weekTotal += count;

    if (monthTotal != weekTotal) {
      logger.info("addRefAudio ref audio" +
          "\n\tmonth total " + monthTotal +
          "\n\tweek total  " + weekTotal +
          "\n\tref weeks   " + weekToCount.keySet());
    }
//    logger.info("addRefAudio month\n" + monthToCount);
//    logger.info("week  \n" + weekToCount);

    builder.append(getSectionReport(ytd, monthToCount, weekToCount, REF_AUDIO_RECORDINGS, jsonObject, year));
  }

  private static class Counts {
    private final Map<Integer, Integer> monthToCount = new TreeMap<>();
    private final Map<Integer, Integer> weekToCount = new TreeMap<>();

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
                                   UserTimeBase result,
                                   Map<Integer, Map<String, Integer>> userToDayToCount) {
    tallyByMonthAndWeek(calendar, monthToCount, weekToCount, userToDayToCount, result.getTimestamp(), result.getUserid());
  }

  private void tallyByMonthAndWeek(Calendar calendar,
                                   Map<Integer, Integer> monthToCount,
                                   Map<Integer, Integer> weekToCount,
                                   Map<Integer, Map<String, Integer>> userToDayToCount,
                                   long timestamp,
                                   int userid) {
    calendar.setTimeInMillis(timestamp);

    int month = calendar.get(Calendar.MONTH);
    tallyWeek(monthToCount, month);

    {
      Map<String, Integer> dayToCount = userToDayToCount.computeIfAbsent(userid, k -> new TreeMap<>());
      String yearMonthDayKey =
          calendar.get(Calendar.YEAR) + "," +
              month + "," +
              calendar.get(Calendar.DAY_OF_MONTH);

      dayToCount.put(yearMonthDayKey, dayToCount.getOrDefault(yearMonthDayKey, 0) + 1);
    }

    int w = calendar.get(Calendar.WEEK_OF_YEAR);
    Integer orDefault = weekToCount.getOrDefault(w, 0);
//    if (orDefault == 0) {
//      logger.debug("got " + w + " for " + new Date(timestamp) + " ");
//    }
    weekToCount.put(w, orDefault + 1);
  }

  private boolean isRefAudioResult(MonitorResult result) {
    return result.getAudioType().isRef();
  }

  /**
   * @return
   * @see #getResults
   */
 /* private boolean isRefAudioResult(Map<Integer, List<AudioAttribute>> exToAudio, MonitorResult result) {
    boolean skip = false;
    List<AudioAttribute> audioAttributes = exToAudio.get(result.getExID());
    if (audioAttributes != null) {
      for (AudioAttribute audioAttribute : audioAttributes) {
        if (getFile(audioAttribute).equals(getFile(result))) {
          long userid = result.getUserid();
          if (audioAttribute.getUserid() == userid) {
            skip = true;
            break;
          }
        }
      }
    }
    return skip;
  }*/
/*  private String getFile(AudioAttribute attribute) {
    return getFile(attribute.getAudioRef());
  }

  private String getFile(MonitorResult result) {
    return getFile(result.getAnswer());
  }*/

/*
  private String getFile(String audioRef) {
    String[] bestAudios = audioRef.split(File.separator);
    return bestAudios[bestAudios.length - 1];
  }
*/
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
   * @see IReport#doReport
   */
  private Set<Integer> getEvents(StringBuilder builder, Set<Integer> students, JsonObject jsonObject, int year,
                                 Collection<SlickSlimEvent> all) {
    return getEvents(builder, students, all, ACTIVE_USERS, TIME_ON_TASK, jsonObject, year);
  }

  /**
   * Look at the event table to determine the first moment a user did anything
   * There was a bug where the user timestamp was not set properly.
   */
  private void setUserStart(Collection<SlickSlimEvent> all) {
    for (SlickSlimEvent event : all) {
      int creatorID = event.userid();
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
  private Set<Integer> getEventsDevices(StringBuilder builder, Set<Integer> students, JsonObject jsonObject, int year,
                                        List<SlickSlimEvent> allDevicesSlim) {
    return getEvents(builder, students, allDevicesSlim, ACTIVE_I_PAD, TIME_ON_TASK_IOS, jsonObject, year);
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
  private Set<Integer> getEvents(StringBuilder builder,
                                 final Set<Integer> students,
                                 final Collection<SlickSlimEvent> all, String activeUsers,
                                 String tableLabel, JsonObject jsonObject, int year) {
    Map<Integer, Set<Long>> monthToCount = new TreeMap<>();
    Map<Integer, Set<Long>> weekToCount = new TreeMap<>();
    ensureYTDEntries2(year, monthToCount, weekToCount);
//    logger.info("now " + monthToCount.keySet() + " " + weekToCount.keySet());

    Map<Integer, Map<Long, Set<SlickSlimEvent>>> monthToCount2 = new TreeMap<>();
    Map<Integer, Map<Long, Set<SlickSlimEvent>>> weekToCount2 = new TreeMap<>();
    ensureYTDEntries3(year, monthToCount2, weekToCount2);
    //  logger.info("now " + monthToCount2.keySet() + " " + weekToCount2.keySet());

//    Set<Integer> teachers = new HashSet<>();
//    int skipped = 0;
    Calendar calendar = getCalendarForYear(year);
    YearTimeRange yearTimeRange = new YearTimeRange(year, calendar).invoke();

    Set<Integer> users = new HashSet<>();

    for (SlickSlimEvent event : all) {
      Integer creatorID = event.userid();
      long timestamp = event.modified();
      if (yearTimeRange.inYear(timestamp) && students.contains(creatorID)) {
        if (true) {
          users.add(creatorID);
          statsForEvent(calendar, monthToCount, monthToCount2, weekToCount, weekToCount2, event, creatorID);
        }
      } else if (!students.contains(creatorID)) {
        //  skipped++;
        //      teachers.add(creatorID);
      }
    }
    //dumpActiveUsers(activeUsers, teachers, skipped, users);

// TODO : consider putting this back???
/*
    JsonObject activeJSON = new JsonObject();
    builder.append(getSectionReport(users.size(), monthToCount, weekToCount, activeUsers, activeJSON, year));
    jsonObject.add("activeUsers", activeJSON);

    */


//    logger.debug("active users " + activeJSON);

    Map<Integer, Long> monthToDur = getMonthToDur(monthToCount2, year);
    long total = 0;
    for (Long v : monthToDur.values()) total += v;

    total /= MIN_MILLIS;

    Map<Integer, Long> weekToDur = getWeekToDur(weekToCount2, year);

    JsonObject timeOnTaskJSON = new JsonObject();

    JsonObject yearJSON = new JsonObject();
    JsonArray monthArray = new JsonArray();
    JsonArray weekArray = new JsonArray();

    int ytdHours = Math.round(total / 60);

//    logger.info("ytd hours " + ytdHours);

    String yearMonthWeekTable = getYearMonthWeekTable(tableLabel,
        getYTD(ytdHours, TOTAL_TIME_ON_TASK_HOURS, yearJSON, year),
        getMonthToCount(getMinMap(monthToDur), MONTH, TIME_ON_TASK_MINUTES, tableLabel, monthArray, year),
        getWC(getMinMap(weekToDur), TIME_ON_TASK_MINUTES, weekArray, year)
    );

    timeOnTaskJSON.add(YEAR, yearJSON);
    timeOnTaskJSON.add(MONTH1, monthArray);
    timeOnTaskJSON.add(WEEK1, weekArray);

    jsonObject.add("timeOnTask", timeOnTaskJSON);

    builder.append(yearMonthWeekTable);

    return users;
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

  private Map<Integer, Integer> getMinMap(Map<Integer, Long> monthToDur) {
    Map<Integer, Integer> copy = new TreeMap<>();
    for (Map.Entry<Integer, Long> pair : monthToDur.entrySet()) {
      long value = pair.getValue() / MIN_MILLIS;
      if (value == 0) {
        //logger.debug("huh? " +pair.getKey() + " " + pair.getValue());
        value = 1;
      }
      copy.put(pair.getKey(), Long.valueOf(value).intValue());
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
   * @see #getEvents(StringBuilder, Set, Collection, String, String, JsonObject, int)
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
    private final int year;
    private final Calendar calendar;
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
//      if (DEBUG) logger.info("getEvents from " + january1st + " to " + january1stNextYear);
      time = january1st.getTime();
      time1 = january1stNextYear.getTime();
      return this;
    }
  }
}