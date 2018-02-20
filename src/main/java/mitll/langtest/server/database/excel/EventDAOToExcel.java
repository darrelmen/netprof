package mitll.langtest.server.database.excel;

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.instrumentation.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by go22670 on 3/25/16.
 */
public class EventDAOToExcel {
  private static final Logger logger = LogManager.getLogger(EventDAOToExcel.class);

  private static final String EVENTS = "Events";
  private static final List<String> COLUMNS2 = Arrays.asList("id", "type", "exercise", "context", "userid", "timestamp", "time_millis", "hitID", "device");

  protected final LogAndNotify logAndNotify;

  public static final String ID = "ID";

  public EventDAOToExcel(Database database) {
    this.logAndNotify = database.getLogAndNotify();
  }

  /**
   * @param out
   * @see mitll.langtest.server.DownloadServlet#returnSpreadsheet(HttpServletResponse, DatabaseImpl, String, int, String)
   */
  public void toXLSX(List<Event> all, OutputStream out) {
    long then = System.currentTimeMillis();

    long now = System.currentTimeMillis();
    if (now - then > 100) logger.info("toXLSX : took " + (now - then) + " millis to read " + all.size() +
        " events from database");
    then = now;

    SXSSFWorkbook wb = new SXSSFWorkbook(1000); // keep 100 rows in memory, exceeding rows will be flushed to disk

    Sheet sheet = wb.createSheet(EVENTS);
    int rownum = 0;

    {
      Row headerRow = sheet.createRow(rownum++);
      for (int i = 0; i < COLUMNS2.size(); i++) {
        Cell headerCell = headerRow.createCell(i);
        headerCell.setCellValue(COLUMNS2.get(i));
      }
    }

    CellStyle cellStyle = wb.createCellStyle();
    DataFormat dataFormat = wb.createDataFormat();

    cellStyle.setDataFormat(dataFormat.getFormat("MMM dd HH:mm:ss"));

    for (Event event : all) {
      Row row = sheet.createRow(rownum++);
      int j = 0;
      Cell cell = row.createCell(j++);
      cell.setCellValue(event.getWidgetID());

      cell = row.createCell(j++);
      cell.setCellValue(event.getWidgetType());

      cell = row.createCell(j++);
      cell.setCellValue(event.getExerciseID());

      cell = row.createCell(j++);
      cell.setCellValue(event.getContext());

      cell = row.createCell(j++);
      cell.setCellValue(event.getUserID());

      cell = row.createCell(j++);
      cell.setCellValue(new Date(event.getTimestamp()));
      cell.setCellStyle(cellStyle);

      cell = row.createCell(j++);
      cell.setCellValue(event.getTimestamp());

      cell = row.createCell(j++);
      cell.setCellValue(event.getDevice());

    }
    now = System.currentTimeMillis();
    if (now - then > 100) logger.warn("toXLSX : took " + (now - then) + " millis to write " + rownum +
        " rows to sheet, or " + (now - then) / rownum + " millis/row");
    then = now;
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
}
