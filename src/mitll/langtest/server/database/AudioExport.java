package mitll.langtest.server.database;

import mitll.langtest.server.ExerciseSorter;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.MiniUser;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
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
 */
public class AudioExport {
  private static final Logger logger = Logger.getLogger(AudioExport.class);
  private static final List<Boolean> GENDERS = Arrays.asList(Boolean.TRUE, Boolean.FALSE);
  private static final List<String> SPEEDS = Arrays.asList(AudioAttribute.REGULAR, AudioAttribute.SLOW, AudioAttribute.REGULAR_AND_SLOW);
  private static final String ID = "ID";
  private static final String WORD_EXPRESSION = "Word/Expression";
  private static final String TRANSLITERATION = "Transliteration";
  private static final String MEANING = "Meaning";

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

  public void writeZip(OutputStream out,
                       String prefix,
                       SectionHelper sectionHelper,
                       Collection<? extends CommonExercise> exercisesForSelectionState,
                       String language1,
                       AudioDAO audioDAO,
                       String installPath,
                       String relPath) throws Exception {
    List<String> typeOrder = sectionHelper.getTypeOrder();
    List<CommonExercise> copy = new ArrayList<CommonExercise>(exercisesForSelectionState);

    new ExerciseSorter(typeOrder).sortByTooltip(copy);
    writeToStream(copy, audioDAO, installPath, relPath, prefix, typeOrder, language1, out, false);
  }

  public void writeZipJustOneAudio(OutputStream out,
                                   SectionHelper sectionHelper,
                                   Collection<? extends CommonExercise> exercisesForSelectionState,
                                   String installPath) throws Exception {
    List<CommonExercise> copy = new ArrayList<CommonExercise>(exercisesForSelectionState);

    new ExerciseSorter(sectionHelper.getTypeOrder()).sortByTooltip(copy);
    writeToStreamJustOneAudio(copy, installPath, out );
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
    columns.add(ID);
    columns.add(WORD_EXPRESSION);
    boolean english = isEnglish(language);
    if (!english) {
      columns.add(language);
    }
    columns.add(english ? MEANING :TRANSLITERATION);
 /*   if (english) {
      columns.add("Context");
    }*/
    columns.addAll(typeOrder);

    for (int i = 0; i < columns.size(); i++) {
      headerRow.createCell(i).setCellValue(columns.get(i));
    }

    for (CommonExercise exercise : copy) {
      Row row = sheet.createRow(rownum++);

      int j = 0;

      row.createCell(j++).setCellValue(exercise.getID());
      row.createCell(j++).setCellValue(exercise.getEnglish());

      if (!english) {
        row.createCell(j++).setCellValue(exercise.getForeignLanguage());
      }

      row.createCell(j++).setCellValue(english ? exercise.getMeaning() : exercise.getTransliteration());

      for (String type : typeOrder) {
        row.createCell(j++).setCellValue(exercise.getUnitToValue().get(type));
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
        isEnglish(language1));
    }

    zOut.putNextEntry(new ZipEntry(overallName+File.separator+overallName+".xlsx"));

    writeExcelToStream(toWrite, zOut, typeOrder, language1);
  }


  private void writeToStreamJustOneAudio(List<CommonExercise> toWrite, String installPath,
                                         OutputStream out) throws Exception {
     writeFolderContentsSimple(new ZipOutputStream(out), toWrite, installPath);
  }

  private boolean isEnglish(String language1) {
    return language1.equalsIgnoreCase("English");
  }

  private void writeFolderContents(ZipOutputStream zOut,
                                   List<CommonExercise> toWrite,
                                   AudioDAO audioDAO,
                                   String installPath,
                                   String relativeConfigDir1,
                                   String overallName,
                                   boolean isEnglish) throws Exception {
    //int c = 0;
    long then = System.currentTimeMillis();
    Set<String> names = new HashSet<String>();

    Map<String, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio();
    //logger.debug("found audio for " + exToAudio.size() + " items and writing " + toWrite.size() + " items ");
   // logger.debug("realContextPath " + realContextPath + " installPath " + installPath + " relativeConfigDir1 " +relativeConfigDir1);
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
      for (Boolean gender : GENDERS) {
        MiniUser majorityUser = gender ? male : female;
        for (String speed : SPEEDS) {
          AudioAttribute recording = getAudioAttribute(majorityUser, ex, gender, speed);
          if (recording != null) {

           // logger.debug("found " + recording + " by " + recording.getUser());
            String name = overallName + File.separator + getUniqueName(ex, !isEnglish);
            copyAudio(zOut, names, name, speed, installPath, audioConversion, recording, ex.getID());
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
    long diff = now - then;
    if (diff > 1000) {
      logger.debug("took " + diff + " millis to export " + toWrite.size() + " items, num attached " + numAttach + " missing audio " + numMissing);
    }
  }


  private void writeFolderContentsSimple(ZipOutputStream zOut,
                                         List<CommonExercise> toWrite,
                                         String installPath) throws Exception {
    long then = System.currentTimeMillis();
    AudioConversion audioConversion = new AudioConversion();
    logger.debug("writing " + toWrite.size());
    int c =0;
    int d =0;
    for (CommonExercise ex : toWrite) {
      if (ex.getRefAudio() != null) {
        try {
          copyAudioSimple(zOut, installPath, audioConversion, ex.getRefAudio());
        } catch (IOException e) {
          //logger.debug("skipping duplicate " +e);
          d++;
        }
        c++;
      }
    }
    zOut.close();
    long now = System.currentTimeMillis();
    long diff = now - then;
    logger.debug("took " + diff + " millis to export " + toWrite.size() + " items " +c + " duplicates " + d);

   // if (diff > 1000) {
  //    logger.debug("took " + diff + " millis to export " + toWrite.size() + " items");
   // }
  }

/*  private AudioAttribute getAudioAttribute(MiniUser majorityUser, CommonExercise ex, boolean isMale, boolean regularSpeed) {
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
  }*/

  /**
   * @see #writeFolderContents(java.util.zip.ZipOutputStream, java.util.List, AudioDAO, String, String, String, boolean)
   * @param majorityUser
   * @param ex
   * @param isMale
   * @param speed
   * @return
   */
  private AudioAttribute getAudioAttribute(MiniUser majorityUser, CommonExercise ex, boolean isMale, String speed) {
    AudioAttribute recordingAtSpeed = majorityUser != null ? ex.getRecordingsBy(majorityUser.getId(), speed) : null;

    if (recordingAtSpeed == null) {  // can't find majority user or nothing by this person at that speed
      Collection<AudioAttribute> byGender = ex.getByGender(isMale);
      // choose the first one if not the majority majorityUser
      for (AudioAttribute attribute : byGender) {
        if (attribute.getSpeed().equalsIgnoreCase(speed)) {
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

  /**
   *
   * @param zOut
   * @param names
   * @param parent
   * @param speed
   * @param realContextPath
   * @param audioConversion
   * @param attribute
   * @param exid
   * @return
   * @throws IOException
   */
  private String copyAudio(ZipOutputStream zOut, Set<String> names, String parent, String speed,
                           String realContextPath, AudioConversion audioConversion, AudioAttribute attribute,
                           String exid) throws IOException {
    String audioRef = attribute.getAudioRef();
/*    logger.debug("\tfor ex id " +exid +
      " writing audio under context path " + realContextPath + " at " + audioRef);*/

    String s = audioConversion.ensureWriteMP3(audioRef, realContextPath, false);
    File mp3 = new File(s);
    if (mp3.exists()) {
      //  logger.debug("---> Did write " + mp3.getAbsolutePath());
      String name = getName(parent, speed, attribute);
      if (names.contains(name)) {
        name += "_" + exid;
      }
      names.add(name);
      name = name.replaceAll(" ","_");
      name += ".mp3";

      //logger.debug("copyAudio : mp3 name is " + name);
      addZipEntry(zOut, mp3, name);
      return name;
    } else {
      logger.warn("\tDidn't write " + mp3.getAbsolutePath());
      return "";
    }
  }

  /**
   * @see #writeFolderContentsSimple(java.util.zip.ZipOutputStream, java.util.List, String)
   * @param zOut
   * @param realContextPath
   * @param audioConversion
   * @param audioRef
   * @return
   * @throws IOException
   */
  private String copyAudioSimple(ZipOutputStream zOut,
                                 String realContextPath,
                                 AudioConversion audioConversion, String audioRef) throws IOException {
    String s = audioConversion.ensureWriteMP3(audioRef, realContextPath, false);
    File mp3 = new File(s);
    if (mp3.exists()) {
      String name = audioRef.replaceAll(".wav", ".mp3");
      //logger.debug("copyAudioSimple : mp3 name is " + name);
      addZipEntry(zOut, mp3, name);
      return mp3.getPath();
    } else {
      logger.warn("\tDidn't write " + mp3.getAbsolutePath());
      return "";
    }
  }

 // private void addZipEntry(ZipOutputStream zOut, File mp3) throws IOException {
 //   addZipEntry(zOut, mp3, mp3.getPath());
 // }

  private void addZipEntry(ZipOutputStream zOut, File mp3, String name) throws IOException {
    zOut.putNextEntry(new ZipEntry(name));
    FileUtils.copyFile(mp3, zOut);
    zOut.flush();
    zOut.closeEntry();
  }

  /**
   * @see #copyAudio
   * @param parent
   * @param speed
   * @param attribute
   * @return
   */
  private String getName(String parent, String speed, AudioAttribute attribute) {
    MiniUser user = attribute.getUser();
    String userInfo = user.isDefault() ?
      "" :
      (attribute.isMale() ? "Male" : "Female");// + "_age_" + user.getAge() + "_(" +user.getId()+ ")";
    String speedSuffix = speed.equals(AudioAttribute.REGULAR) ? "" : "_" + speed;
    return parent + (user.isDefault() ? "" : "_" + userInfo) + speedSuffix;
  }
}
