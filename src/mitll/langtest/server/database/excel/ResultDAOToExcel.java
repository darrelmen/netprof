package mitll.langtest.server.database.excel;

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.ResultDAO;
import mitll.langtest.shared.MonitorResult;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * Created by go22670 on 10/16/15.
 */
public class ResultDAOToExcel {
  private static final Logger logger = Logger.getLogger(ResultDAOToExcel.class);
  private static final String YES = "Yes";
  private static final String NO = "No";

  /**
   * @param typeOrder
   * @param out
   * @see mitll.langtest.server.DownloadServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public void writeExcelToStream(Collection<MonitorResult> results, List<String> typeOrder, OutputStream out) {
    writeToStream(out, writeExcel(results, typeOrder));
  }

  private SXSSFWorkbook writeExcel(Collection<MonitorResult> results, List<String> typeOrder
  ) {
    long now;
    long then = System.currentTimeMillis();

    SXSSFWorkbook wb = new SXSSFWorkbook(10000); // keep 100 rows in memory, exceeding rows will be flushed to disk
    Sheet sheet = wb.createSheet("Results");
    int rownum = 0;
    CellStyle cellStyle = wb.createCellStyle();
    DataFormat dataFormat = wb.createDataFormat();

    cellStyle.setDataFormat(dataFormat.getFormat("MMM dd HH:mm:ss 'yy"));
    //DateTimeFormat format = DateTimeFormat.getFormat("MMM dd h:mm:ss a z ''yy");
    Row headerRow = sheet.createRow(rownum++);

    List<String> columns = new ArrayList<String>(Arrays.asList(
        ResultDAO.USERID, "Exercise", "Text"));


    for (final String type : typeOrder) {
      columns.add(type);
    }

    List<String> columns2 = Arrays.asList(
        "Recording",
        Database.TIME,
        ResultDAO.AUDIO_TYPE,
        ResultDAO.DURATION,
        "Valid",
        ResultDAO.CORRECT,
        ResultDAO.PRON_SCORE,
        "Device",
        "w/Flash",
        "Process",
        "RoundTrip"
    );

    columns.addAll(columns2);

    for (int i = 0; i < columns.size(); i++) {
      Cell headerCell = headerRow.createCell(i);
      headerCell.setCellValue(columns.get(i));
    }

    for (MonitorResult result : results) {
      Row row = sheet.createRow(rownum++);
      int j = 0;
      Cell cell = row.createCell(j++);
      cell.setCellValue(result.getUserid());
      cell = row.createCell(j++);
      cell.setCellValue(result.getId());
      cell = row.createCell(j++);
      cell.setCellValue(result.getForeignText());

      for (String type : typeOrder) {
        cell = row.createCell(j++);
        cell.setCellValue(result.getUnitToValue().get(type));
      }

      cell = row.createCell(j++);
      cell.setCellValue(result.getAnswer());

      cell = row.createCell(j++);
      cell.setCellValue(new Date(result.getTimestamp()));
      cell.setCellStyle(cellStyle);

      cell = row.createCell(j++);
      String audioType = result.getAudioType();
      cell.setCellValue(audioType.equals("avp") ? "flashcard" : audioType);

      cell = row.createCell(j++);
      cell.setCellValue(result.getDurationInMillis());

      cell = row.createCell(j++);
      cell.setCellValue(result.isValid() ? YES : NO);

      cell = row.createCell(j++);
      cell.setCellValue(result.isCorrect() ? YES : NO);

      cell = row.createCell(j++);
      cell.setCellValue(result.getPronScore());

      cell = row.createCell(j++);
      cell.setCellValue(result.getDevice());

      cell = row.createCell(j++);
      cell.setCellValue(result.isWithFlash() ? YES : NO);

      cell = row.createCell(j++);
      cell.setCellValue(result.getProcessDur());

      cell = row.createCell(j++);
      cell.setCellValue(result.getRoundTripDur());
    }
    now = System.currentTimeMillis();
    if (now - then > 100) {
      logger.warn("toXLSX : took " + (now - then) + " millis to add " + rownum + " rows to sheet, or " + (now - then) / rownum + " millis/row");
    }
    return wb;
  }

  private void writeToStream(OutputStream out, SXSSFWorkbook wb) {
    long then = System.currentTimeMillis();
    try {
      wb.write(out);
      long now2 = System.currentTimeMillis();
      if (now2 - then > 100) {
        logger.warn("toXLSX : took " + (now2 - then) + " millis to write excel to output stream ");
      }
      out.close();
      wb.dispose();
    } catch (IOException e) {
      logger.error("got " + e, e);
    }
  }
}
