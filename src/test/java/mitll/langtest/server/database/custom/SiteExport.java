package mitll.langtest.server.database.custom;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/12/16.
 */
public class SiteExport {
  private static final Logger logger = LogManager.getLogger(SiteExport.class);

  @Test
  public void testExport() {
    String file = "siteList.xlsx";
    try {
      FileInputStream inp = new FileInputStream(file);
      Workbook workbook = WorkbookFactory.create(inp);

// Get the first Sheet.
      Sheet sheet = workbook.getSheetAt(0);


      // Start constructing JSON.
      JSONObject json = new JSONObject();

      // Iterate through the rows.
      JSONArray rows = new JSONArray();
      Iterator<Row> rowsIT = sheet.rowIterator();

      List<String> header = getHeader(rowsIT.next());

      for (; rowsIT.hasNext(); ) {
        Row row = rowsIT.next();
        JSONObject jRow = new JSONObject();

        // Iterate through the cells.
        int i = 0;

        for (String col : header) {
          Cell cell = null;
          try {
            cell = row.getCell(i++);
          } catch (Exception e) {
            e.printStackTrace();
          }
          addField(jRow, cell, col);

        }
  /*      for (Iterator<Cell> cellsIT = row.cellIterator(); cellsIT.hasNext(); ) {
          Cell cell = cellsIT.next();
      //    logger.debug("col " + i + " is a " + cellType);
          String key = header.get(i++);
          addField(jRow, cell, key);

        }*/
        rows.add(jRow);
      }

      // Create the JSON.
      json.put("sites", rows);


      //   logger.info("got " + jsonObject);
      String text = json.toString();

      File fileToWrite = new File("sites.json");
      logger.debug("writing to  " + fileToWrite.getAbsolutePath());

      writeToFile(text, fileToWrite);


// Get the JSON text.
      logger.debug("got\n" + json.toString());
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InvalidFormatException e) {
      e.printStackTrace();
    }
  }

  void addField(JSONObject jRow, Cell cell, String key) {
    if (cell == null) jRow.put(key, "");
    else {
      int cellType = cell.getCellType();
      if (cellType == Cell.CELL_TYPE_BOOLEAN) {
        jRow.put(key, cell.getBooleanCellValue());
      } else if (cellType == Cell.CELL_TYPE_STRING) {
        jRow.put(key, cell.getStringCellValue());
      } else if (cellType == Cell.CELL_TYPE_NUMERIC) {
        jRow.put(key, cell.getNumericCellValue());
      }
    }
  }

  private List<String> getHeader(Row next) {
    List<String> columns = new ArrayList<String>();

    Iterator<Cell> cellIterator = next.cellIterator();
    while (cellIterator.hasNext()) {
      Cell next1 = cellIterator.next();
      columns.add(next1.toString().trim());
    }

    return columns;
  }


  void writeToFile(String text, File file) {
    try {

      String[] split = text.split("\\},\\{");
      try (PrintWriter out = new PrintWriter(file)) {
        for (int i = 0; i < split.length; i++) {
          String s = split[i];
          out.print(s);

          if (i < split.length - 1) {
            out.println("},");
            out.print("{");
          }
        }
        out.flush();
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }
}
