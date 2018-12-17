package mitll.langtest.server.database.report;

import mitll.langtest.server.LangTestDatabaseImpl;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.IReport;
import mitll.langtest.server.database.ReportStats;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.mail.MailSupport;
import mitll.langtest.shared.project.ProjectProperty;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ReportHelper {
  private static final Logger logger = LogManager.getLogger(ReportHelper.class);
  private static final int DAY_TO_SEND_REPORT = Calendar.SATURDAY;// Calendar.SUNDAY;

  private static final boolean REPORT_ALL_PROJECTS = true;
  private static final boolean SEND_ALL_YEARS = true;
  private static final int REPORT_THIS_PROJECT = 9;

  private final DatabaseServices services;
  private final IProjectDAO projectDAO;
  private final IUserDAO userDAO;
  private final PathHelper pathHelper;
  private final MailSupport mailSupport;

  public ReportHelper(DatabaseServices services,
                      IProjectDAO projectDAO,
                      IUserDAO userDAO,
                      PathHelper pathHelper,
                      MailSupport mailSupport) {
    this.services = services;
    this.projectDAO = projectDAO;
    this.userDAO = userDAO;
    this.pathHelper = pathHelper;
    this.mailSupport = mailSupport;
  }

  public void sendReports(IReport report) {
    sendReports(report, false, -1);
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
   * @seex #doReport
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
    return services.getProjectManagement().getProjects();
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
