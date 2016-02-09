package mitll.langtest.server.database.excel;

import mitll.langtest.shared.User;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by go22670 on 1/27/16.
 */
public class UserDAOToExcel {
  private static final Logger logger = Logger.getLogger(UserDAOToExcel.class);

  private static final String MALE = "male";
  private static final String FEMALE = "female";
  private static final String KIND = "kind";
  private static final String PASS = "passwordH";
  private static final String EMAIL = "emailH";
  private static final String DEVICE = "device";

  private static final String ID = "id";
  private static final String AGE = "age";
  private static final String GENDER = "gender";
  private static final String EXPERIENCE = "experience";
  private static final String IPADDR = "ipaddr";
  private static final String DIALECT = "dialect";
  private static final String TIMESTAMP = "timestamp";
  private static final String SEC = "(sec)";

  /**
   * For spreadsheet download
   */
  private static final List<String> COLUMNS2 = Arrays.asList(ID,
      "userid",
      DIALECT,
      AGE,
      GENDER,
      EXPERIENCE,
      "permissions",
      "items complete?",
      "num recordings",
      "rate" +
          SEC,
      IPADDR,
      TIMESTAMP,
      KIND,
      PASS,
      EMAIL,
      DEVICE
  );

  /**
   * For spreadsheet download
   */
  private static final List<String> COLUMNSJSON = Arrays.asList(ID,
      "userid",
      DIALECT,
      AGE,
      GENDER,
      EXPERIENCE,
      "permissions",
      "itemsComplete",
      "numRecordings",
      "rate",
      IPADDR,
      TIMESTAMP,
      KIND,
      PASS,
      EMAIL,
      DEVICE
  );

  /**
   * @see mitll.langtest.server.database.UserDAO#toJSON(List)
   * @param users
   * @return
   */
  public JSON toJSON(List<User> users) {
    JSONArray json = new JSONArray();

    for (User user : users) {
      int j = 0;
      JSONObject jsonObject = new JSONObject();
      jsonObject.put(COLUMNSJSON.get(j++), user.getId());
      jsonObject.put(COLUMNSJSON.get(j++), user.getUserID());
      jsonObject.put(COLUMNSJSON.get(j++), user.getDialect());
      jsonObject.put(COLUMNSJSON.get(j++), user.getAge());
      jsonObject.put(COLUMNSJSON.get(j++), user.getGender() == 0 ? MALE : FEMALE);
      jsonObject.put(COLUMNSJSON.get(j++), user.getExperience());

      jsonObject.put(COLUMNSJSON.get(j++), user.getPermissions().toString().replaceAll("\\[", "").replaceAll("\\]", ""));
      jsonObject.put(COLUMNSJSON.get(j++), user.isComplete() ? "Yes" : ("No (" + Math.round(100 * user.getCompletePercent()) + "%)"));
      jsonObject.put(COLUMNSJSON.get(j++), "" + user.getNumResults());
      jsonObject.put(COLUMNSJSON.get(j++), "" + roundToHundredth(user.getRate()));

      jsonObject.put(COLUMNSJSON.get(j++), user.getIpaddr());
      try {
        SimpleDateFormat sdf = new SimpleDateFormat();
        String format = sdf.format(new Date(user.getTimestampMillis()));
        jsonObject.put(COLUMNSJSON.get(j++), format);
      } catch (Exception e) {
        logger.error("got " + e, e);
      }

      jsonObject.put(COLUMNSJSON.get(j++), user.getUserKind().toString());

      String passwordHash = user.getPasswordHash();
      jsonObject.put(COLUMNSJSON.get(j++), passwordHash == null || passwordHash.isEmpty() ? "NO_PASSWORD" : "HAS_PASSWORD");

      String emailHash = user.getEmailHash();
      jsonObject.put(COLUMNSJSON.get(j++), emailHash == null || emailHash.isEmpty() ? "NO_EMAIL" : "HAS_EMAIL");
      jsonObject.put(COLUMNSJSON.get(j++), user.getDevice());

      json.add(jsonObject);
    }
    return json;
  }

  public void toXLSX(OutputStream out, List<User> users, String language) {
    writeToStream(out, getSpreadsheet(users), language);
  }

  private SXSSFWorkbook getSpreadsheet(List<User> users) {
    long then = System.currentTimeMillis();

    SXSSFWorkbook wb = new SXSSFWorkbook(1000); // keep 100 rows in memory, exceeding rows will be flushed to disk

    Sheet sheet = wb.createSheet("Users");
    int rownum = 0;
    Row headerRow = sheet.createRow(rownum++);
    for (int i = 0; i < COLUMNS2.size(); i++) {
      Cell headerCell = headerRow.createCell(i);
      headerCell.setCellValue(COLUMNS2.get(i));
    }

    CellStyle cellStyle = wb.createCellStyle();
    DataFormat dataFormat = wb.createDataFormat();

    cellStyle.setDataFormat(dataFormat.getFormat("MMM dd HH:mm:ss"));

    for (User user : users) {
      Row row = sheet.createRow(rownum++);
      int j = 0;
      row.createCell(j++).setCellValue(user.getId());
      row.createCell(j++).setCellValue(user.getUserID());
      row.createCell(j++).setCellValue(user.getDialect());
      row.createCell(j++).setCellValue(user.getAge());
      row.createCell(j++).setCellValue(user.getGender() == 0 ? MALE : FEMALE);
      row.createCell(j++).setCellValue(user.getExperience());

      row.createCell(j++).setCellValue(user.getPermissions().toString().replaceAll("\\[", "").replaceAll("\\]", ""));
      row.createCell(j++).setCellValue(user.isComplete() ? "Yes" : ("No (" + Math.round(100 * user.getCompletePercent()) + "%)"));
      row.createCell(j++).setCellValue("" + user.getNumResults());
      row.createCell(j++).setCellValue("" + roundToHundredth(user.getRate()));

//      row.createCell(j++).setCellValue(user.getNativeLang());
      row.createCell(j++).setCellValue(user.getIpaddr());

      Cell cell = row.createCell(j++);
      try {
        SimpleDateFormat sdf = new SimpleDateFormat();
        String format = sdf.format(new Date(user.getTimestampMillis()));
        cell.setCellValue(format);
      } catch (Exception e) {
        cell.setCellValue("Unknown");
      }
      cell.setCellStyle(cellStyle);

      User.Kind userKind = user.getUserKind();
      row.createCell(j++).setCellValue(userKind.toString());
      String passwordHash = user.getPasswordHash();
      row.createCell(j++).setCellValue(passwordHash == null || passwordHash.isEmpty() ? "NO_PASSWORD" : "HAS_PASSWORD");
      String emailHash = user.getEmailHash();
      row.createCell(j++).setCellValue(emailHash == null || emailHash.isEmpty() ? "NO_EMAIL" : "HAS_EMAIL");
      row.createCell(j++).setCellValue(user.getDevice());
    }
    long now = System.currentTimeMillis();
    long diff = now - then;
    if (diff > 100) {
      logger.info("toXLSX : took " + diff + " millis to write " + rownum +
          " rows to sheet, or " + diff / rownum + " millis/row.");
    }
    return wb;
  }

  private void writeToStream(OutputStream out, SXSSFWorkbook wb, String language) {
    long now;
    try {
      long then = System.currentTimeMillis();
      wb.write(out);
      now = System.currentTimeMillis();
      if (now - then > 100) {
        logger.warn(language + " : toXLSX : took " + (now - then) + " millis to write excel to output stream ");
      }
      out.close();
      wb.dispose();
    } catch (IOException e) {
      logger.error("got " + e, e);
      //database.logEvent("unk", "(" + language + ") toXLSX: " + e.toString(), 0, UNKNOWN);
    }
  }

  private float roundToHundredth(double totalHours) {
    return ((float) ((Math.round(totalHours * 100)))) / 100f;
  }

}
