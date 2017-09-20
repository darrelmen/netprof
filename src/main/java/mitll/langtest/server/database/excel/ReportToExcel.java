package mitll.langtest.server.database.excel;

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.ReportStats;
import mitll.langtest.shared.instrumentation.Event;
import net.liftweb.util.RE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * Created by go22670 on 3/25/16.
 */
public class ReportToExcel {
  private static final Logger logger = LogManager.getLogger(ReportToExcel.class);
//  private static final List<String> COLUMNS2 = Arrays.asList("Language", "type", "exercise", "context", "userid", "timestamp", "time_millis", "hitID", "device");

  protected final LogAndNotify logAndNotify;

  public static final String ID = "ID";

  public ReportToExcel(LogAndNotify logAndNotify) {
    this.logAndNotify = logAndNotify;
  }

  /**
   * @param out
   * @see mitll.langtest.server.DownloadServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public void toXLSX(List<ReportStats> stats, OutputStream out) {
    long then = System.currentTimeMillis();
    long now = System.currentTimeMillis();

    SXSSFWorkbook wb = new SXSSFWorkbook(1000); // keep 100 rows in memory, exceeding rows will be flushed to disk

    Sheet sheet = wb.createSheet("NetProF Historical");

    int rownum = 0;

    short yellow = IndexedColors.YELLOW.getIndex();
    Set<Integer> years = new TreeSet<>();
    {
      Row headerRow = sheet.createRow(rownum++);
      // headerRow.getRowStyle().setFillBackgroundColor(IndexedColors.YELLOW.getIndex());
      int col = 0;
      Cell headerCell = headerRow.createCell(col++);
      makeYellow(yellow, headerCell);
      headerCell.setCellValue("Language");

      stats.forEach(reportStats -> years.add(getYearSafe(reportStats)));
      for (Integer year : years) {
        Cell cell = headerRow.createCell(col++);
        cell.setCellValue(year);
        makeYellow(yellow, cell);
      }
      Cell cell = headerRow.createCell(col++);
      cell.setCellValue("Languages");
      makeYellow(yellow, cell);
    }

/*    CellStyle cellStyle = wb.createCellStyle();
    DataFormat dataFormat = wb.createDataFormat();
    cellStyle.setDataFormat(dataFormat.getFormat("MMM dd HH:mm:ss"));*/

    Map<Integer, Integer> yearToTotal = new HashMap<>();

    Map<String, List<ReportStats>> langToStats = new HashMap<>();

    for (ReportStats reportStats : stats) {
      List<ReportStats> reportStats1 = langToStats.computeIfAbsent(reportStats.getLanguage(), k -> new ArrayList<>());
      reportStats1.add(reportStats);
    }

    Map<String, Map<Integer, ReportStats>> langToYearToStats = new HashMap<>();
    langToStats.forEach((k, v) -> {
      Map<Integer, ReportStats> yearToStats = langToYearToStats.computeIfAbsent(k, k1 -> new HashMap<>());

      for (ReportStats reportStats : v) {
        int year = getYearSafe(reportStats);
        ReportStats current = yearToStats.get(year);
        if (current == null) {
          yearToStats.put(year, reportStats);
        } else {
          current.merge(reportStats);
        }
      }
    });

    TreeSet<String> sortedLang = new TreeSet<>(langToYearToStats.keySet());
    for (String lang : sortedLang) {
      Row row = sheet.createRow(rownum++);

      int col = 0;

      row.createCell(col++).setCellValue(lang);
      for (Integer year : years) {
        Map<Integer, ReportStats> yearToStats = langToYearToStats.get(lang);
        ReportStats reportStats = yearToStats.get(year);
        logger.info("For " + lang + " " + year + " got " + reportStats);
        Integer orDefault = reportStats == null ? 0 : reportStats.getIntKeyToValue().getOrDefault(ReportStats.INFO.ALL_RECORDINGS, 0);
        row.createCell(col++).setCellValue(orDefault);
        yearToTotal.put(year, yearToTotal.getOrDefault(year, 0) + orDefault);
      }
      ;
      row.createCell(col++).setCellValue(lang);
    }

    Row row = sheet.createRow(rownum++);
    int col = 0;
    row.createCell(col++).setCellValue("TOTALS");

    for (Integer year : years) {
      row.createCell(col++).setCellValue(yearToTotal.getOrDefault(year, 0));
    }

    row.createCell(col++).setCellValue("TOTAL");

    now = System.currentTimeMillis();
    if (now - then > 100) logger.warn("toXLSX : took " + (now - then) + " millis to write " + rownum +
        " rows to sheet, or " + (now - then) / rownum + " millis/row");
    then = now;

    writeTheFile(out, then, wb);
  }

  private void makeYellow(short yellow, Cell headerCell) {
    headerCell.getCellStyle().setFillBackgroundColor(yellow);
  }

  private int getYearSafe(ReportStats reportStats) {
    return reportStats.getYear() == -1 ? getThisYear() : reportStats.getYear();
  }

  private void writeTheFile(OutputStream out, long then, SXSSFWorkbook wb) {
    long now;
    try {
      wb.write(out);
      now = System.currentTimeMillis();
      if (now - then > 100) {
        logger.warn("toXLSX : took " + (now - then) + " millis to write excel to output stream ");
      }
      out.close();
      wb.dispose();
    } catch (IOException e) {
      logger.error("got " + e, e);
      logAndNotify.logAndNotifyServerException(e);
    }
  }


  private int getThisYear() {
    return Calendar.getInstance().get(Calendar.YEAR);
  }

}
