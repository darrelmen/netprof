package mitll.langtest.server.database.excel;

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.database.ReportStats;
import mitll.langtest.server.database.ReportStats.INFO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import static org.apache.poi.ss.usermodel.PatternFormatting.SOLID_FOREGROUND;

/**
 * Created by go22670 on 3/25/16.
 */
public class ReportToExcel {
  private static final Logger logger = LogManager.getLogger(ReportToExcel.class);
  public static final String NET_PRO_F_HISTORICAL = "NetProF Historical";
  public static final String NET_PRO_F_YTD = "NetProF-Vrt";

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

    XSSFWorkbook wb = new XSSFWorkbook();//1000); // keep 100 rows in memory, exceeding rows will be flushed to disk

    Map<String, Map<Integer, ReportStats>> langToYearToStats = getLangToYearToStats(stats, false);

    int rownum = writeWeeklySheet(wb, wb.createSheet(NET_PRO_F_YTD), langToYearToStats);

    now = System.currentTimeMillis();
    if (now - then > 100) logger.warn("toXLSX : took " + (now - then) + " millis to write " + rownum +
        " rows to sheet, or " + (now - then) / rownum + " millis/row");
    then = now;

    rownum = writeHistoricalSheet(stats, wb.createSheet(NET_PRO_F_HISTORICAL), langToYearToStats);

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
//        logger.info("For " + lang + " " + year + " got " + reportStats);
        Integer orDefault = reportStats == null ? 0 : reportStats.getIntKeyToValue().getOrDefault(INFO.ALL_RECORDINGS, 0);
        row.createCell(col++).setCellValue(orDefault);
        yearToTotal.put(year, yearToTotal.getOrDefault(year, 0) + orDefault);
      }
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

  private int writeHeaderRow2(Sheet sheet, int rownum, short yellow, Set<String> years, XSSFCellStyle lightGreenStyle) {
    Row headerRow = sheet.createRow(rownum++);
    // headerRow.getRowStyle().setFillBackgroundColor(IndexedColors.YELLOW.getIndex());
    int col = 0;
    Cell headerCell = headerRow.createCell(col++);
    //makeYellow(yellow, headerCell);
    headerCell.setCellValue("");
    headerCell.setCellStyle(lightGreenStyle);
    for (String year : years) {
      Cell cell = headerRow.createCell(col++);
      cell.setCellValue(year);
      cell.setCellStyle(lightGreenStyle);

    }
    Cell cell = headerRow.createCell(col++);
    cell.setCellValue("TOTAL");
    cell.setCellStyle(lightGreenStyle);
    //makeYellow(yellow, cell);
    return rownum;
  }

  private Set<Integer> getYearsFromStats(List<ReportStats> stats) {
    Set<Integer> years = new TreeSet<>();
    stats.forEach(reportStats -> years.add(getYearSafe(reportStats)));
    return years;
  }

  private int writeWeeklySheet(XSSFWorkbook workbook, Sheet sheet,
                               Map<String, Map<Integer, ReportStats>> langToYearToStats) {
    int rownum = 0;

    short yellow = IndexedColors.YELLOW.getIndex();
    Set<Integer> years = new TreeSet<>();
    int thisYear = getThisYear();
    years.add(thisYear);
    Set<String> sortedLang = new TreeSet<>(langToYearToStats.keySet());

    rownum = writeHeaderRow2(sheet, rownum, yellow, sortedLang, getLightGreenStyle(workbook));
    // Map<String, Map<Integer, ReportStats>> langToYearToStats = getLangToYearToStats(stats, true);

    Map<Integer, ReportStats> next = langToYearToStats.values().iterator().next();
    ReportStats reportStats1 = next.get(thisYear);
    Map<String, Integer> weekToCount = reportStats1.getKeyToValue(INFO.ALL_RECORDINGS_WEEKLY);

    Set<String> weeks = weekToCount.keySet();

    Map<String, Integer> langToLastWeek = new HashMap<>();
    Map<String, Integer> langToCurrent = new HashMap<>();
    Map<String, Integer> langToPrev = new HashMap<>();
    int marginalDiff = 0;
    CellStyle style = getCellStyle(workbook);
    XSSFCellStyle greenStyle = getDarkGreenStyle(workbook);

    for (String week : weeks) {
      Row row = sheet.createRow(rownum++);

      int col = 0;
      Cell cell1 = row.createCell(col++);
      cell1.setCellValue(getWeek(week));
      cell1.setCellStyle(greenStyle);
      int marginalTotal = 0;

      //int prev = 0;
      int prevMarginal = 0;

      // for every lang per week
      for (String lang : sortedLang) {
        Map<Integer, ReportStats> yearToStats = langToYearToStats.get(lang);
        ReportStats reportStats = yearToStats.get(thisYear);
        Map<String, Integer> weekToCountForLang = reportStats.getKeyToValue(INFO.ALL_RECORDINGS_WEEKLY);

        Integer count = weekToCountForLang.getOrDefault(week, 0);

        int cumulative = langToCurrent.getOrDefault(lang, 0) + count;
        marginalTotal += cumulative;
        langToCurrent.put(lang, cumulative);
        Cell cell = row.createCell(col++);
        cell.setCellStyle(style);
        cell.setCellValue(cumulative);
        langToLastWeek.put(lang, cumulative - langToPrev.getOrDefault(lang, 0));
        langToPrev.put(lang, cumulative);
      }

      Cell cell = row.createCell(col++);
      cell.setCellValue(marginalTotal);
      cell.setCellStyle(greenStyle);
      marginalDiff = marginalTotal - prevMarginal;
      prevMarginal = marginalTotal;
    }

    Row row = sheet.createRow(rownum++);

    int col = 0;

    Cell cell = row.createCell(col++);
    cell.setCellValue("INCREASE");
    XSSFCellStyle brightGreenStyle = getBrightGreenStyle(workbook);
    XSSFCellStyle yellowStyle = getYellowStyle(workbook);
    cell.setCellStyle(brightGreenStyle);
    for (String lang : sortedLang) {
      Integer value = langToLastWeek.get(lang);
      Cell cell1 = row.createCell(col++);
      cell1.setCellValue(value);
      if (value == 0) {
        cell.setCellStyle(yellowStyle);
      } else {
        cell.setCellStyle(brightGreenStyle);
      }
    }
    row.createCell(col++).setCellValue(marginalDiff);

    row = sheet.createRow(rownum++);
    col = 0;
    Cell cell1 = row.createCell(col++);
    cell1.setCellValue("Languages");
    cell1.setCellStyle(greenStyle);


    XSSFCellStyle lightGreenStyle = getLightGreenStyle(workbook);
    for (String lang : sortedLang) {
      Cell cell2 = row.createCell(col++);
      cell2.setCellValue(lang);
      cell2.setCellStyle(lightGreenStyle);
    }

    Cell cell2 = row.createCell(col++);
    cell2.setCellValue("TOTAL");
    cell2.setCellStyle(greenStyle);
    return rownum;
  }

  @NotNull
  private CellStyle getCellStyle(XSSFWorkbook workbook) {
    CellStyle style = workbook.createCellStyle();
    DataFormat format = workbook.createDataFormat();

    style.setDataFormat(format.getFormat("#,###"));
    style.setAlignment(HorizontalAlignment.CENTER);
    return style;
  }

  @NotNull
  private XSSFCellStyle getDarkGreenStyle(XSSFWorkbook workbook) {
/*
    XSSFCellStyle greenStyle = workbook.createCellStyle();
//    greenStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
    java.awt.Color darkGreen = new java.awt.Color(170, 207, 145);
    setColorStyle(workbook, greenStyle, darkGreen);
    return greenStyle;
*/

    java.awt.Color darkGreen = new java.awt.Color(170, 207, 145);
    return getColorStyle(workbook, darkGreen);
  }

  private void setColorStyle(XSSFWorkbook workbook, XSSFCellStyle greenStyle, java.awt.Color darkGreen) {
    greenStyle.setFillForegroundColor(new XSSFColor(darkGreen));
    greenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    greenStyle.setAlignment(HorizontalAlignment.CENTER);
    setBold(workbook, greenStyle);
  }

  private void setBold(XSSFWorkbook workbook, XSSFCellStyle greenStyle) {
    XSSFFont font = workbook.createFont();
    font.setBold(true);
    greenStyle.setFont(font);
  }

  @NotNull
  private XSSFCellStyle getLightGreenStyle(XSSFWorkbook workbook) {
    java.awt.Color darkGreen = new java.awt.Color(226, 239, 219);
    return getColorStyle(workbook, darkGreen);
  }

  @NotNull
  private XSSFCellStyle getBrightGreenStyle(XSSFWorkbook workbook) {
    java.awt.Color darkGreen = new java.awt.Color(109, 253, 110);
    return getColorStyle(workbook, darkGreen);
  }

  @NotNull
  private XSSFCellStyle getYellowStyle(XSSFWorkbook workbook) {
    java.awt.Color darkGreen = new java.awt.Color(255, 253, 56);
    return getColorStyle(workbook, darkGreen);
  }

  private XSSFCellStyle getColorStyle(XSSFWorkbook workbook, java.awt.Color darkGreen) {
    XSSFCellStyle greenStyle = workbook.createCellStyle();
    setColorStyle(workbook, greenStyle, darkGreen);
    return greenStyle;
  }

  private String getWeek(String week) {
    if (week.startsWith("0")) week = week.substring(1);
    return week.replace("-", "/");
  }

  private void makeYellow(short yellow, Cell headerCell) {
    headerCell.getCellStyle().setFillBackgroundColor(yellow);
  }

  private int getYearSafe(ReportStats reportStats) {
    return reportStats.getYear() == -1 ? getThisYear() : reportStats.getYear();
  }

  private void writeTheFile(OutputStream out, long then, XSSFWorkbook wb) {
    long now;
    try {
      wb.write(out);
      now = System.currentTimeMillis();
      if (now - then > 100) {
        logger.warn("toXLSX : took " + (now - then) + " millis to write excel to output stream ");
      }
      out.close();
      // wb.dispose();
    } catch (IOException e) {
      logger.error("got " + e, e);
      logAndNotify.logAndNotifyServerException(e);
    }
  }


  private int getThisYear() {
    return Calendar.getInstance().get(Calendar.YEAR);
  }

}
