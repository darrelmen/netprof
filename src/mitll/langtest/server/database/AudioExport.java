package mitll.langtest.server.database;

import mitll.langtest.server.ExerciseSorter;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.MiniUser;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Take a selection and write out a zip with a spreadsheet and the audio as mp3 files.
 *
 */
public class AudioExport {
  private static final Logger logger = Logger.getLogger(AudioExport.class);

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#writeZip(java.io.OutputStream, java.util.Map)
   * @param out
   * @param typeToSection
   * @param sectionHelper
   * @param exercisesForSelectionState
   * @param language1
   * @param audioDAO
   * @param installPath
   * @param relPath
   * @throws Exception
   */
  public void writeZip(OutputStream out,
                       Map<String, Collection<String>> typeToSection,
                       SectionHelper sectionHelper,
                       Collection<CommonExercise> exercisesForSelectionState,
                       String language1,
                       AudioDAO audioDAO,
                       String installPath,
                       String relPath) throws Exception {
    List<String> typeOrder = sectionHelper.getTypeOrder();
    String prefix = getPrefix(typeToSection, typeOrder);
    List<CommonExercise> copy = new ArrayList<CommonExercise>(exercisesForSelectionState);

    new ExerciseSorter(typeOrder).getSortedByUnitThenAlpha(copy, false);
    writeToStream(copy, audioDAO, installPath, relPath, prefix, typeOrder, language1, out, typeToSection.isEmpty());
  }

  public String getPrefix(SectionHelper sectionHelper,Map<String, Collection<String>> typeToSection) {
    return getPrefix(typeToSection, sectionHelper.getTypeOrder());
  }

  private String getPrefix(Map<String, Collection<String>> typeToSection, List<String> typeOrder) {
    String prefix = "";
    for (String type : typeOrder) {
      Collection<String> selections = typeToSection.get(type);
      if (selections != null) {
        prefix += type + "_";
        for (String sel : selections) {
          prefix += sel + ",";
        }
        if (!selections.isEmpty()) prefix = prefix.substring(0, prefix.length() - 1);
        prefix += "_";
      }
    }
    if (prefix.isEmpty()) {
      prefix = "All";
    } else {
      prefix = prefix.substring(0, prefix.length() - 1);
    }
    return prefix;
  }

  private void writeExcelToStream(Collection<CommonExercise> exercises, OutputStream out, Collection<String> typeOrder, String language) {
    SXSSFWorkbook wb = writeExcel(exercises, language, typeOrder);
    long then = System.currentTimeMillis();
    try {
      wb.write(out);
      long now2 = System.currentTimeMillis();
      if (now2 - then > 500) {
        logger.warn("toXLSX : took " + (now2 - then) + " millis to write excel to output stream ");
      }
      //  out.close();
      wb.dispose();
    } catch (IOException e) {
      logger.error("got " + e, e);
    }
  }

  private SXSSFWorkbook writeExcel(Collection<CommonExercise> copy, String language, Collection<String> typeOrder) {
    long now;
    long then = System.currentTimeMillis();

    SXSSFWorkbook wb = new SXSSFWorkbook(1000); // keep 100 rows in memory, exceeding rows will be flushed to disk
    Sheet sheet = wb.createSheet("Exercises");
    int rownum = 0;
    Row headerRow = sheet.createRow(rownum++);

    List<String> columns = new ArrayList<String>();
    columns.add("ID");
    columns.add("Word/Expression");
    columns.add(language);
    columns.add("Transliteration");
    for (String type : typeOrder) {
      columns.add(type);
    }

    for (int i = 0; i < columns.size(); i++) {
      Cell headerCell = headerRow.createCell(i);
      headerCell.setCellValue(columns.get(i));
    }

    for (CommonExercise exercise : copy) {
      Row row = sheet.createRow(rownum++);
      int j = 0;

      Cell cell = row.createCell(j++);
      cell.setCellValue(exercise.getID());

      cell = row.createCell(j++);
      cell.setCellValue(exercise.getEnglish());

      cell = row.createCell(j++);
      cell.setCellValue(exercise.getForeignLanguage());

      cell = row.createCell(j++);
      cell.setCellValue(exercise.getTransliteration());

      for (String type : typeOrder) {
        cell = row.createCell(j++);
        cell.setCellValue(exercise.getUnitToValue().get(type));
      }
    }
    now = System.currentTimeMillis();
    if (now-then > 100) {
      logger.warn("toXLSX : took " + (now-then) + " millis to add " + rownum + " rows to sheet, or " + (now-then)/rownum + " millis/row");
    }
    return wb;
  }

  private void writeToStream(List<CommonExercise> toWrite, AudioDAO audioDAO, String installPath,
                             String relativeConfigDir1, String name, List<String> typeOrder,
                             String language1, OutputStream out, boolean skipAudio) throws Exception {
    ZipOutputStream zOut = new ZipOutputStream(out);

    String overallName = language1 + "_" + name;
    overallName = overallName.replaceAll("\\,","_");
    if (!skipAudio) {
      writeFolderContents(zOut, toWrite, audioDAO, installPath, relativeConfigDir1,
        overallName,
        language1.equalsIgnoreCase("English"));
    }

    zOut.putNextEntry(new ZipEntry(overallName+File.separator+overallName+".xlsx"));

    writeExcelToStream(toWrite, zOut, typeOrder, language1);
  }

  private void writeFolderContents(ZipOutputStream zOut,
                                   List<CommonExercise> toWrite, AudioDAO audioDAO,
                                   String installPath, String relativeConfigDir1,
                                   String overallName,
                                   boolean isEnglish
  ) throws Exception {
    int c = 0;
    long then = System.currentTimeMillis();
    String realContextPath = installPath;
    Set<String> names = new HashSet<String>();

    Map<String, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio();
    logger.debug("found audio for " + exToAudio.size() + " items and writing " + toWrite.size() + " items ");
   // logger.debug("installPath path " + installPath);
   // logger.debug("rel path         " + relativeConfigDir1);
  //  logger.debug("realContextPath         " + realContextPath);
    int numAttach = 0;
    int numMissing = 0;

    // get male and female majority users
    Map<MiniUser,Integer> maleToCount   = new HashMap<MiniUser, Integer>();
    Map<MiniUser,Integer> femaleToCount = new HashMap<MiniUser, Integer>();

    for (CommonExercise ex : toWrite) {
      for (AudioAttribute attr : ex.getAudioAttributes()) {
        MiniUser user = attr.getUser();

        Map<MiniUser,Integer> userToCount = (user.isMale()) ? maleToCount : femaleToCount;
        Integer count = userToCount.get(user);
        userToCount.put(user, (count == null) ? 1 : count + 1);
      }
    }
    MiniUser male = null, female = null;
    int mCount = 0, fCount = 0;
    for (Map.Entry<MiniUser, Integer> pair : maleToCount.entrySet()) {
      if (male == null || pair.getValue() > mCount) {
        male = pair.getKey();
      }
    }
    for (Map.Entry<MiniUser, Integer> pair : femaleToCount.entrySet()) {
      if (female == null || pair.getValue() > fCount) {
        female = pair.getKey();
      }
    }
    AudioConversion audioConversion = new AudioConversion();

    for (CommonExercise ex : toWrite) {
      List<AudioAttribute> audioAttributes = exToAudio.get(ex.getID());
      if (audioAttributes != null) {
        audioDAO.attachAudio(ex, installPath, relativeConfigDir1, audioAttributes);
        numAttach++;
      }

      boolean someAudio = false;
      // choose male speaker
      for (Boolean gender : Arrays.asList(Boolean.TRUE, Boolean.FALSE)) {
        MiniUser majorityUser = gender ? male : female;
        for (Boolean speed : Arrays.asList(Boolean.TRUE, Boolean.FALSE)) {
          AudioAttribute recording = getAudioAttribute(majorityUser, ex, gender, speed);
          if (recording != null) {
            String name = overallName+File.separator+ getUniqueName(ex, !isEnglish);
            copyAudio(zOut, names, name, speed, realContextPath, audioConversion, recording, ex.getID());
            someAudio = true;
          }
        }
      }

      if (!someAudio) {
        if (numMissing < 10) {
          logger.debug("no audio for exercise " + ex.getID());
        }
        numMissing++;
      }
    }
    long now = System.currentTimeMillis();
    logger.debug("took " + (now - then) + " millis to export " + toWrite.size() + " items, num attached " + numAttach + " missing audio " + numMissing);
  }

  private AudioAttribute getAudioAttribute(MiniUser majorityUser, CommonExercise ex, boolean isMale, boolean regularSpeed) {
    AudioAttribute recordingAtSpeed = majorityUser != null ? ex.getRecordingsBy(majorityUser.getId(), regularSpeed) : null;
    if (recordingAtSpeed == null) {  // can't find majority user or nothing by this person at that speed
      Collection<AudioAttribute> byGender = ex.getByGender(isMale);
      // choose the first one if not the majority majorityUser
      for (AudioAttribute attribute : byGender) {
        if ((attribute.isRegularSpeed() && regularSpeed) || (!attribute.isRegularSpeed() && !regularSpeed)) {
          recordingAtSpeed = attribute;
          break;
        }
      }
    }
    return recordingAtSpeed;
  }

  /**
   * Worry about name collisions for entries composed of english-fl pairs
   * @paramx names
   * @param ex
   * @return
   */
  private String getUniqueName(CommonExercise ex, boolean includeFL) {
    String name = ex.getEnglish().trim() + (includeFL ? "_" + ex.getForeignLanguage() : "");
    name = name.trim();
    name = name.replaceAll("\"", "\\'").replaceAll("\\?", "").replaceAll("\\:", "").replaceAll("/", " or ").replaceAll("\\\\", " or ");
    return name;
  }

  private String copyAudio(ZipOutputStream zOut, Set<String> names, String parent, boolean isRegularSpeed,
                           String realContextPath, AudioConversion audioConversion, AudioAttribute attribute,
                           String exid) throws IOException {
    String audioRef = attribute.getAudioRef();
/*    logger.debug("\tfor ex id " +exid +
      " writing audio under context path " + realContextPath + " at " + audioRef);*/

    String s = audioConversion.ensureWriteMP3(audioRef, realContextPath, false);
    File mp3 = new File(s);
    if (mp3.exists()) {
      //  logger.debug("---> Did write " + mp3.getAbsolutePath());
      String name = getName(parent, isRegularSpeed, attribute);
      if (names.contains(name)) {
        name += "_" + exid;
      }
      names.add(name);

      name += ".mp3";

      zOut.putNextEntry(new ZipEntry(name));
      FileUtils.copyFile(mp3, zOut);
      zOut.flush();
      zOut.closeEntry();
      return name;
    } else {
      logger.warn("\tDidn't write " + mp3.getAbsolutePath());
      return "";
    }
  }

  private String getName(String parent, boolean isRegularSpeed, AudioAttribute attribute) {
    MiniUser user = attribute.getUser();
    String userInfo = user.isDefault() ?
      //"DefaultSpeaker" :
      "" :
      (attribute.isMale() ? "Male" : "Female");// + "_age_" + user.getAge() + "_(" +user.getId()+ ")";
  //  String baseName = parent + File.separator + userInfo + (isSlow ? "_Slow" : "");
    return parent + (user.isDefault() ? "" : "_" + userInfo) + (isRegularSpeed ? "":"_Slow");
  }
}
