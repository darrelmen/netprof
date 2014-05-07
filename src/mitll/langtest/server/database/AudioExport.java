package mitll.langtest.server.database;

import com.github.gwtbootstrap.client.ui.Section;
import mitll.langtest.server.ExerciseSorter;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.AudioExercise;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.Exercise;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by GO22670 on 5/7/2014.
 */
public class AudioExport {
  private static final Logger logger = Logger.getLogger(AudioExport.class);

  public void writeZip(OutputStream out, Map<String, Collection<String>> typeToSection,
                       SectionHelper sectionHelper,
                       Collection<CommonExercise> exercisesForSelectionState,
                       String language1,
                       AudioDAO audioDAO) throws Exception {
//    Collection<CommonExercise> exercisesForSelectionState = typeToSection.isEmpty()? getExercises() :sectionHelper.getExercisesForSelectionState(typeToSection);

  //  String language1 = getServerProps().getLanguage();
    List<String> typeOrder = sectionHelper.getTypeOrder();
    String prefix = getPrefix(typeToSection, typeOrder);
    List<CommonExercise> copy = new ArrayList<CommonExercise>(exercisesForSelectionState);

    new ExerciseSorter(typeOrder).getSortedByUnitThenAlpha(copy, false);

    writeArchive(copy, audioDAO, ".", "war", prefix, typeOrder, language1, out, typeToSection.isEmpty());
  }

  public String getPrefix(SectionHelper sectionHelper,Map<String, Collection<String>> typeToSection) {
    List<String> typeOrder = sectionHelper.getTypeOrder();
    return getPrefix(typeToSection, typeOrder);
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

  private void writeExcelToStream(Collection<CommonExercise> exercises, OutputStream out, Collection<String> typeOrder,String russian) {
    SXSSFWorkbook wb = writeExcel(exercises, russian,typeOrder);
    long then = System.currentTimeMillis();
    try {
      wb.write(out);
      long now2 = System.currentTimeMillis();
      if (now2-then > 100) {
        logger.warn("toXLSX : took " + (now2-then) + " millis to write excel to output stream ");
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

/*    List<CommonExercise> copy = new ArrayList<CommonExercise>(results);
    Collections.sort(copy, new Comparator<CommonExercise>() {
      @Override
      public int compare(CommonExercise o1, CommonExercise o2) {
        return o1.getEnglish().compareTo(o2.getEnglish());
      }
    });*/
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

  private void writeArchive(List<CommonExercise> toWrite, AudioDAO audioDAO,
                            String installPath, String relativeConfigDir1, String name, List<String> typeOrder,
                            String language1, OutputStream out, boolean skipAudio) throws Exception {
/*    File archiveParent = archiveContentDir.getParentFile();
    File zipFile = new File(archiveParent, archiveContentDir.getName()+ ".zip");

    OutputStream out = new FileOutputStream(zipFile);*/

    writeToStream(toWrite, audioDAO, installPath, relativeConfigDir1, name, typeOrder, language1, out, skipAudio);

    /*zOut.flush();
    zOut.closeEntry();

    zOut.close();*/


    //  logger.debug("wrote to " + zipFile.getAbsolutePath());
    // return zipFile;
  }

  private void writeToStream(List<CommonExercise> toWrite, AudioDAO audioDAO, String installPath,
                             String relativeConfigDir1, String name, List<String> typeOrder,
                             String language1, OutputStream out, boolean skipAudio) throws Exception {
    ZipOutputStream zOut = new ZipOutputStream(out);

    if (!skipAudio) {
      writeFolderContents(zOut, /*"",*/ toWrite, audioDAO, installPath, relativeConfigDir1, name);
    }

    //logger.debug("Adding xls file under " + name);
    zOut.putNextEntry(new ZipEntry(name+".xlsx"));

    writeExcelToStream(toWrite, zOut, typeOrder, language1);
  }

  private void writeFolderContents(ZipOutputStream zOut,
                                   List<CommonExercise> toWrite, AudioDAO audioDAO,
                                   String installPath, String relativeConfigDir1, String overallName
  ) throws Exception {
    int c = 0;
    long then = System.currentTimeMillis();
    String realContextPath = ".";
    Set<String> names = new HashSet<String>();

    Map<String, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio();
    logger.debug("found audio for " + exToAudio.size() + " items");
    for (CommonExercise ex : toWrite) {
      //audioDAO.attachAudio(ex, installPath, relativeConfigDir1);
      List<AudioAttribute> audioAttributes = exToAudio.get(ex.getID());
      if (audioAttributes != null) {
        audioDAO.attachAudio(ex, installPath, relativeConfigDir1, audioAttributes);
      }
      String name = ex.getEnglish() + "_" + ex.getForeignLanguage();
      name = name.trim();
      name = name.replaceAll("\"", "\\'").replaceAll("\\?", "").replaceAll("\\:", "").replaceAll("/"," or ").replaceAll("\\\\"," or ");
      if (names.contains(name)) {
        name += "_" + ex.getID();
      }
      names.add(name);

      Collection<AudioAttribute> reg = ex.getAudioAtSpeed(AudioExercise.REGULAR);
      // logger.debug("entry for " + name + " has  " + reg.size() + " cuts/" + ex.getAudioAttributes().size());
      Collection<AudioAttribute> slow = ex.getAudioAtSpeed(AudioExercise.SLOW);
      // logger.debug("entry for " + name + " has  " + slow.size() + " cuts/" + ex.getAudioAttributes().size());

      String parent = overallName+"_audio" + File.separator + name;
      if (!reg.isEmpty()) {
        copyAudio(zOut, reg, parent, false, realContextPath);
      }
      if (!slow.isEmpty()) {
        copyAudio(zOut, slow, parent, true, realContextPath);
      }
      //  }
      //  else {
      //    logger.debug("no audio for exercise " + ex.getID());
      //  }
    }
    long now = System.currentTimeMillis();
    logger.debug("took " + (now - then) + " millis to export " + toWrite.size() + " items");
  }

  private void copyAudio(ZipOutputStream zOut, Collection<AudioAttribute> reg, String parent, boolean isSlow, String realContextPath) throws IOException {
    //logger.debug("copyAudio writing to '" + parent + "' given " + reg.size() + " cuts");
    AudioConversion audioConversion = new AudioConversion();

    for (AudioAttribute attribute : reg) {
      MiniUser user = attribute.getUser();

      String userInfo = user.isDefault() ? "DefaultSpeaker" :(attribute.isMale() ? "Male_" : "Female_") + "age_" + user.getAge() + "_(" +user.getId()+ ")";
      String baseName = parent + File.separator + userInfo + (isSlow ? "_Slow" : "");
      String name = baseName + ".mp3";
      String audioRef = attribute.getAudioRef();
      //  logger.debug("\twriting audio under " + name + " at " + audioRef);

      File input = new File(audioRef);
      if (input.exists()) {
        String s = audioConversion.ensureWriteMP3(audioRef, realContextPath, false);
        File mp3 = new File(s);
        if (mp3.exists()) {
          // audioRef = audioRef.replaceAll(".wav",".mp3");
          //   logger.warn("---> Did write " + mp3.getAbsolutePath());

          zOut.putNextEntry(new ZipEntry(name));
          FileUtils.copyFile(mp3, zOut);
          zOut.flush();
          zOut.closeEntry();
        }
        else {
          logger.warn("\tDidn't write " + mp3.getAbsolutePath());
        }
      }
      else {
        //logger.warn("\tDidn't write " + input.getAbsolutePath());
      }
    }
  }
}
