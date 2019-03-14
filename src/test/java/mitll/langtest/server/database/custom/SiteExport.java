/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.custom;

public class SiteExport {
  //private static final Logger logger = LogManager.getLogger(SiteExport.class);
/*
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
  *//*      for (Iterator<Cell> cellsIT = row.cellIterator(); cellsIT.hasNext(); ) {
          Cell cell = cellsIT.next();
      //    logger.debug("col " + i + " is a " + cellType);
          String key = header.get(i++);
          addField(jRow, cell, key);

        }*//*
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
  }*/
}
