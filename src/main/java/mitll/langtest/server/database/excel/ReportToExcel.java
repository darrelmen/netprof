package mitll.langtest.server.database.excel;

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.database.ReportStats;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.jetbrains.annotations.NotNull;

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

    Map<String, Map<Integer, ReportStats>> langToYearToStats = getLangToYearToStats(stats, false);

    int rownum = writeHistoricalSheet(stats, wb.createSheet("NetProF Historical"), langToYearToStats);

    now = System.currentTimeMillis();
    if (now - then > 100) logger.warn("toXLSX : took " + (now - then) + " millis to write " + rownum +
        " rows to sheet, or " + (now - then) / rownum + " millis/row");
    then = now;

     rownum = writeWeeklySheet( wb.createSheet("NetProF YTD"), langToYearToStats);


    writeTheFile(out, then, wb);
  }

  private int writeHistoricalSheet(List<ReportStats> stats, Sheet sheet,
                                   Map<String, Map<Integer, ReportStats>> langToYearToStats) {
    int rownum = 0;

    short yellow = IndexedColors.YELLOW.getIndex();
    Set<Integer> yearsFromStats = getYearsFromStats(stats);

    rownum = writeHeaderRow(sheet, rownum, yellow, yearsFromStats);

    // Map<String, Map<Integer, ReportStats>> langToYearToStats = getLangToYearToStats(stats,false);
    Map<Integer, Integer> yearToTotal = new HashMap<>();
    Set<String> sortedLang = new TreeSet<>(langToYearToStats.keySet());
    for (String lang : sortedLang) {
      Row row = sheet.createRow(rownum++);

      int col = 0;

      row.createCell(col++).setCellValue(lang);
      for (Integer year : yearsFromStats) {
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

    for (Integer year : yearsFromStats) {
      row.createCell(col++).setCellValue(yearToTotal.getOrDefault(year, 0));
    }

    row.createCell(col++).setCellValue("TOTAL");
    return rownum;
  }

  @NotNull
  private Map<String, Map<Integer, ReportStats>> getLangToYearToStats(List<ReportStats> stats, boolean onlyThisYear) {
    Map<String, List<ReportStats>> langToStats = getLangToReports(stats);
 //   int thisYear = getThisYear();
    Map<String, Map<Integer, ReportStats>> langToYearToStats = new HashMap<>();
    langToStats.forEach((k, v) -> {
      Map<Integer, ReportStats> yearToStats = langToYearToStats.computeIfAbsent(k, k1 -> new HashMap<>());

      for (ReportStats reportStats : v) {
        int year = getYearSafe(reportStats);

        //if (!onlyThisYear || year == thisYear) {
        ReportStats current = yearToStats.get(year);
        if (current == null) {
          yearToStats.put(year, reportStats);
        } else {
          current.merge(reportStats);
        }
        // }
      }
    });
    return langToYearToStats;
  }

  @NotNull
  private Map<String, List<ReportStats>> getLangToReports(List<ReportStats> stats) {
    Map<String, List<ReportStats>> langToStats = new HashMap<>();

    for (ReportStats reportStats : stats) {
      List<ReportStats> reportStats1 = langToStats.computeIfAbsent(reportStats.getLanguage(), k -> new ArrayList<>());
      reportStats1.add(reportStats);
    }
    return langToStats;
  }

  private int writeHeaderRow(Sheet sheet, int rownum, short yellow, Set<Integer> years) {
    Row headerRow = sheet.createRow(rownum++);
    // headerRow.getRowStyle().setFillBackgroundColor(IndexedColors.YELLOW.getIndex());
    int col = 0;
    Cell headerCell = headerRow.createCell(col++);
    makeYellow(yellow, headerCell);
    headerCell.setCellValue("Language");

    for (Integer year : years) {
      Cell cell = headerRow.createCell(col++);
      cell.setCellValue(year);
      makeYellow(yellow, cell);
    }
    Cell cell = headerRow.createCell(col++);
    cell.setCellValue("Languages");
    makeYellow(yellow, cell);
    return rownum;
  }

  private int writeHeaderRow2(Sheet sheet, int rownum, short yellow, Set<String> years) {
    Row headerRow = sheet.createRow(rownum++);
    // headerRow.getRowStyle().setFillBackgroundColor(IndexedColors.YELLOW.getIndex());
    int col = 0;
    Cell headerCell = headerRow.createCell(col++);
    makeYellow(yellow, headerCell);
    headerCell.setCellValue("Language");

    for (String year : years) {
      Cell cell = headerRow.createCell(col++);
      cell.setCellValue(year);
      makeYellow(yellow, cell);
    }
    Cell cell = headerRow.createCell(col++);
    cell.setCellValue("Languages");
    makeYellow(yellow, cell);
    return rownum;
  }

  private Set<Integer> getYearsFromStats(List<ReportStats> stats) {
    Set<Integer> years = new TreeSet<>();
    stats.forEach(reportStats -> years.add(getYearSafe(reportStats)));
    return years;
  }

  private int writeWeeklySheet(Sheet sheet,
                               Map<String, Map<Integer, ReportStats>> langToYearToStats) {
    int rownum = 0;

    short yellow = IndexedColors.YELLOW.getIndex();
    Set<Integer> years = new TreeSet<>();
    years.add(getThisYear());
    Set<String> sortedLang = new TreeSet<>(langToYearToStats.keySet());
    rownum = writeHeaderRow2( sheet, rownum, yellow, sortedLang);
   // Map<String, Map<Integer, ReportStats>> langToYearToStats = getLangToYearToStats(stats, true);

    Map<Integer, Integer> yearToTotal = new HashMap<>();
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
    return rownum;
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
