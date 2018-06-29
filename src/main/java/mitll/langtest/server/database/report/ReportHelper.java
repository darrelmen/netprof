package mitll.langtest.server.database.report;

import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.IReport;
import mitll.langtest.server.database.ReportStats;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.server.database.project.IProjectManagement;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.project.ProjectProperty;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static mitll.langtest.server.database.project.ProjectDAO.DAY;

public class ReportHelper {
  private static final Logger logger = LogManager.getLogger(ReportHelper.class);
  private static final int DAY_TO_SEND_REPORT = Calendar.SATURDAY;// Calendar.SUNDAY;

  private static final boolean REPORT_ALL_PROJECTS = true;
  private static final boolean SEND_ALL_YEARS = true;
  private static final int REPORT_THIS_PROJECT = 9;

  private final ServerProperties serverProps;
  // private IReport report;
  private final IProjectManagement projectManagement;

  private final IProjectDAO projectDAO;
  private final IUserDAO userDAO;
  private final PathHelper pathHelper;
  private final MailSupport mailSupport;

  public ReportHelper(ServerProperties serverProperties,
                      IProjectManagement projectManagement,
                      IProjectDAO projectDAO,
                      IUserDAO userDAO,
                      PathHelper pathHelper,
                      MailSupport mailSupport) {

    this.projectManagement = projectManagement;
    serverProps = serverProperties;
    this.projectDAO = projectDAO;
    this.userDAO = userDAO;
    this.pathHelper = pathHelper;
    this.mailSupport = mailSupport;
  }

  private void doReport(IReport report) {
    if (serverProps.isFirstHydra()) {
      if (isTodayAGoodDay()) {
        sendReports(report);
      } else {
        logger.info("doReport : not sending email report since this is not Sunday...");
      }
      tryTomorrow(report);
    } else {
      logger.info("doReport host " + serverProps.getHostName() + " not generating a report.");
    }
  }

  public void sendReports(IReport report) {
    sendReports(report, false, -1);
  }

  /**
   * Fire at Saturday night, just before midnight EST (or local)
   * Smarter would be to figure out how long to wait until sunday...
   * <p>
   * fire at 11:59:30 PM Saturday, so the report ends this saturday and not next saturday...
   * i.e. if it's Sunday 12:01 AM, it rounds up and includes a line for the whole upcoming week
   */
  public void tryTomorrow(IReport report) {
    ZoneId zone = ZoneId.systemDefault();
    ZonedDateTime now = ZonedDateTime.now(zone);

    LocalDate tomorrow = now.toLocalDate().plusDays(1);
    ZonedDateTime tomorrowStart = tomorrow.atStartOfDay(zone);
    Duration duration = Duration.between(now, tomorrowStart);
    long candidate = duration.toMillis() - 30 * 1000;
    long toWait = candidate > 0 ? candidate : candidate + DAY;
    new Thread(() -> {
      try {
        logger.info("tryTomorrow :" +
            "\n\tWaiting for " + toWait + " or " + toWait / 1000 + " sec or " + toWait / (60 * 1000) + " min or " + toWait / (60 * 60 * 1000) + " hours" +
            "\n\tto fire at " + tomorrowStart);
        Thread.sleep(toWait);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      doReport(report); // try again later
    }).start();
  }

  public boolean isTodayAGoodDay() {
    return Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == DAY_TO_SEND_REPORT;
  }

  /**
   * @param userID
   * @see LangTestDatabaseImpl#sendReport
   */

  public void sendReport(IReport report, int userID) {
    sendReports(report, true, userID);
  }

  /**
   * @param report
   * @param forceSend
   * @param userID    if -1 uses report list property to determine recipients
   * @see #doReport
   * @see #sendReport
   */
  private void sendReports(IReport report, boolean forceSend, int userID) {
    List<String> reportEmails = new ArrayList<>();
    List<String> receiverNames = new ArrayList<>();

    populateRecipients(userID, reportEmails, receiverNames);

    logger.info("sendReports to" +
        "\n\tat     : " + new Date() +
        "\n\temails : " + reportEmails +
        "\n\tnames  : " + receiverNames
    );
    report.sendExcelViaEmail(mailSupport,
        reportEmails,
        receiverNames,
        getReportStats(report, forceSend), pathHelper);

  }

  private void populateRecipients(int userID, List<String> reportEmails, List<String> receiverNames) {
    if (userID != -1) {
      sendToRequester(userID, reportEmails, receiverNames);
    } else {
      reportEmails.addAll(projectDAO.getListProp(projectDAO.getDefault(), ProjectProperty.REPORT_LIST));

      for (String email : reportEmails) {
        String trim = email.trim();
        if (!trim.isEmpty()) {
          String nameForEmail = userDAO.getNameForEmail(trim);
          if (nameForEmail == null) nameForEmail = trim;
          receiverNames.add(nameForEmail);
        }
      }
    }
  }

  private void sendToRequester(int userID, List<String> reportEmails, List<String> receiverNames) {
    User byID = userDAO.getByID(userID);
    if (byID == null) {
      logger.error("huh? can't find user " + userID + " in db?");
    } else {
//          logger.info("using user email " + byID.getEmail());
      reportEmails.add(byID.getEmail());
      receiverNames.add(byID.getFullName());
    }
  }

  /**
   * @param report
   * @param forceSend
   * @return
   * @see #sendReports
   */
  @NotNull
  private List<ReportStats> getReportStats(IReport report, boolean forceSend) {
    return getReportStats(report, forceSend, getReportableProjects());
  }

  /**
   * Don't want to report on deleted or demo projects.
   *
   * @return
   */
  @NotNull
  private List<Project> getReportableProjects() {
    List<Project> filtered = getProjects()
        .stream()
        .filter(project -> project.getStatus().shouldReportOn())
        .collect(Collectors.toList());

    StringBuilder names = new StringBuilder();
    filtered.forEach(project -> names.append(project.getName()).append(", "));
    logger.info("getReportStats : reporting on " + filtered.size() + " projects:" +
        "\n\tnames " + names);
    return filtered;
  }


  private Collection<Project> getProjects() {
    return projectManagement.getProjects();
  }

  @NotNull
  private List<ReportStats> getReportStats(IReport report, boolean forceSend, List<Project> filtered) {
    List<ReportStats> stats = new ArrayList<>();
    filtered
        .forEach(project -> {

          int id = project.getID();
          if (REPORT_ALL_PROJECTS || id == REPORT_THIS_PROJECT) {
            stats.addAll(report
                .doReport(id,
                    project.getLanguage(),
                    project.getProject().name(),
                    pathHelper,
                    forceSend,
                    SEND_ALL_YEARS));
          }
        });
    return stats;
  }


}
