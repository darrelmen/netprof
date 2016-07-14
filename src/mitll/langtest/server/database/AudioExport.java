/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.database;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.scoring.LTSFactory;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Take a selection and write out a zip with a spreadsheet and the audio as mp3 files.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since
 */
public class AudioExport {
  private static final Logger logger = Logger.getLogger(AudioExport.class);
  private static final Collection<Boolean> GENDERS = Arrays.asList(Boolean.TRUE, Boolean.FALSE);
  private static final Collection<String> SPEEDS = Arrays.asList(AudioAttribute.REGULAR, AudioAttribute.SLOW,
      AudioAttribute.REGULAR_AND_SLOW);
  private static final String ID = "ID";
  private static final String WORD_EXPRESSION = "Word/Expression";
  private static final String TRANSLITERATION = "Transliteration";
  private static final String MEANING = "Meaning";
  private static final String MALE = "Male";
  private static final String FEMALE = "Female";
  private static final String CONTEXT_SENTENCE = "Context Sentence";
  private static final String CONTEXT_TRANSLATION = "Context Translation";
  private Collection<String> typeOrder;
  private final ServerProperties props;

  AudioExport(ServerProperties props) {
    this.props = props;
  }

  /**
   * @param out
   * @param typeToSection
   * @param sectionHelper
   * @param exercisesForSelectionState
   * @param language1
   * @param audioDAO
   * @param installPath
   * @param relPath
   * @param isDefectList
   * @throws Exception
   * @see DatabaseImpl#writeZip
   */
  void writeZip(OutputStream out,
                       Map<String, Collection<String>> typeToSection,
                       SectionHelper<?> sectionHelper,
                       Collection<CommonExercise> exercisesForSelectionState,
                       String language1,
                       IAudioDAO audioDAO,
                       String installPath,
                       String relPath,
                       boolean isDefectList) throws Exception {
    List<CommonExercise> copy = getSortedExercises(sectionHelper, exercisesForSelectionState);
    writeToStream(copy, audioDAO, installPath, relPath, getPrefix(typeToSection, typeOrder), typeOrder, language1, out,
        typeToSection.isEmpty(), isDefectList);
  }

  private List<CommonExercise> getSortedExercises(SectionHelper<?> sectionHelper, Collection<CommonExercise> exercisesForSelectionState) {
    List<CommonExercise> copy = getSortableExercises(sectionHelper, exercisesForSelectionState);
    new ExerciseSorter(typeOrder).getSortedByUnitThenAlpha(copy, false);
    return copy;
  }

  /**
   * @param out
   * @param typeToSection
   * @param sectionHelper
   * @param exercisesForSelectionState
   * @param language1
   * @param audioDAO
   * @param installPath
   * @param relPath
   * @throws Exception
   * @see mitll.langtest.server.database.DatabaseImpl#writeContextZip
   */
  void writeContextZip(OutputStream out,
                              Map<String, Collection<String>> typeToSection,
                              SectionHelper<?> sectionHelper,
                              Collection<CommonExercise> exercisesForSelectionState,
                              String language1,
                              IAudioDAO audioDAO,
                              String installPath,
                              String relPath) throws Exception {
    List<CommonExercise> copy = getSortedExercises(sectionHelper, exercisesForSelectionState);
    writeContextToStream(copy, audioDAO, installPath, relPath, getPrefix(typeToSection, typeOrder), typeOrder, language1, out);
  }

  private List<CommonExercise> getSortableExercises(SectionHelper<?> sectionHelper, Collection<? extends CommonExercise> exercisesForSelectionState) {
    this.typeOrder = sectionHelper.getTypeOrder();
    return new ArrayList<>(exercisesForSelectionState);
  }

  /**
   * @param out
   * @param prefix
   * @param sectionHelper
   * @param exercisesForSelectionState
   * @param language1
   * @param audioDAO
   * @param installPath
   * @param relPath
   * @param isDefectList
   * @throws Exception
   * @see mitll.langtest.server.database.DatabaseImpl#writeZip
   */
  void writeZip(OutputStream out,
                       String prefix,
                       SectionHelper<?> sectionHelper,
                       Collection<? extends CommonExercise> exercisesForSelectionState,
                       String language1,
                       IAudioDAO audioDAO,
                       String installPath,
                       String relPath, boolean isDefectList) throws Exception {
    List<CommonExercise> copy = getSortableExercises(sectionHelper, exercisesForSelectionState);
    new ExerciseSorter(typeOrder).sortByTooltip(copy);
    writeToStream(copy, audioDAO, installPath, relPath, prefix, typeOrder, language1, out, false, isDefectList);
  }

  /**
   * @param out
   * @param sectionHelper
   * @param exercisesForSelectionState
   * @param installPath
   * @throws Exception
   * @see mitll.langtest.server.database.DatabaseImpl#writeZip(java.io.OutputStream)
   */
  public void writeZipJustOneAudio(OutputStream out,
                                   SectionHelper<?> sectionHelper,
                                   Collection<? extends CommonExercise> exercisesForSelectionState,
                                   String installPath) throws Exception {
    List<CommonExercise> copy = new ArrayList<>(exercisesForSelectionState);
    new ExerciseSorter(sectionHelper.getTypeOrder()).sortByTooltip(copy);
    writeToStreamJustOneAudio(copy, installPath, out);
  }

  /**
   * @param sectionHelper
   * @param typeToSection
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getPrefix(java.util.Map)
   */
  public String getPrefix(SectionHelper<?> sectionHelper, Map<String, Collection<String>> typeToSection) {
    return getPrefix(typeToSection, sectionHelper.getTypeOrder());
  }

  private String getPrefix(Map<String, Collection<String>> typeToSection, Collection<String> typeOrder) {
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

  /**
   * @param exercises
   * @param out
   * @param typeOrder
   * @param language
   * @param isDefectList
   * @see #writeToStream
   * @see #addSpreadsheetToZip
   */
  private void writeExcelToStream(Collection<CommonExercise> exercises, OutputStream out, Collection<String> typeOrder,
                                  String language, boolean isDefectList) {
    SXSSFWorkbook wb = writeExcel(exercises, language, typeOrder, isDefectList);
    long then = System.currentTimeMillis();
    try {
      wb.write(out);
      long now2 = System.currentTimeMillis();
      if (now2 - then > 500) {
        logger.warn("toXLSX : took " + (now2 - then) + " millis to write excel to output stream ");
      }
      wb.dispose();
    } catch (IOException e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * TODO : come back to this to make sure meaning, english, and foreign language make sense for English
   * Exercises and UserExercises.
   *
   * @param copy
   * @param language
   * @param typeOrder
   * @param isDefectList
   * @return
   * @see #writeExcelToStream
   */
  private SXSSFWorkbook writeExcel(Collection<CommonExercise> copy, String language, Collection<String> typeOrder,
                                   boolean isDefectList) {
    long now;
    long then = System.currentTimeMillis();

    SXSSFWorkbook wb = new SXSSFWorkbook(1000); // keep 100 rows in memory, exceeding rows will be flushed to disk
    Sheet sheet = wb.createSheet("Exercises");
    int rownum = 0;
    Row headerRow = sheet.createRow(rownum++);

    boolean english = addHeaderRow(language, typeOrder, headerRow, isDefectList);
    Set<Long> preferredVoices = props.getPreferredVoices();

    for (CommonExercise exercise : copy) {
      Row row = sheet.createRow(rownum++);

      int j = 0;

      row.createCell(j++).setCellValue(exercise.getOldID());

      // logger.warn("English " + exercise.getEnglish() + " getMeaning " + exercise.getMeaning() + " getForeignLanguage " + exercise.getForeignLanguage() + " ref " + exercise.getRefSentence());

      // TODO : some horrible hacks to deal with UserExercise vs Exercise confusion about fields.
      // WORD_EXPRESSION
      String english1 = english ? exercise.getForeignLanguage() : exercise.getEnglish();
      row.createCell(j++).setCellValue(english1);

      if (!english) {
        row.createCell(j++).setCellValue(exercise.getForeignLanguage());
      }

      String meaning = english ?
          (exercise.getMeaning().isEmpty() ? exercise.getEnglish() : exercise.getMeaning()) :
          exercise.getTransliteration();

      // english ? MEANING : TRANSLITERATION
      // evil thing where the meaning is empty for UserExercise overrides, but on the spreadsheet
      row.createCell(j++).setCellValue(meaning);

      for (String type : typeOrder) {
        row.createCell(j++).setCellValue(exercise.getUnitToValue().get(type));
      }

      if (exercise.hasContext()) {
        CommonExercise next = exercise.getDirectlyRelated().iterator().next();
        row.createCell(j++).setCellValue(next.getForeignLanguage());
        row.createCell(j++).setCellValue(next.getEnglish());
      }
      else {
        row.createCell(j++).setCellValue("");
        row.createCell(j++).setCellValue("");
      }

      if (isDefectList) {
//        logger.debug("annos for " + exercise.getOldID() + "\tfields : " + exercise.getFieldToAnnotation());
        ExerciseAnnotation annotation = exercise.getAnnotation("english");
        row.createCell(j++).setCellValue(annotation == null || annotation.isCorrect() ? "" : annotation.getComment());

        if (!english) {
          annotation = exercise.getAnnotation("foreignLanguage");
          row.createCell(j++).setCellValue(annotation == null || annotation.isCorrect() ? "" : annotation.getComment());
        }

        annotation = exercise.getAnnotation(CONTEXT_SENTENCE);
        row.createCell(j++).setCellValue(annotation == null || annotation.isCorrect() ? "" : annotation.getComment());

        annotation = exercise.getAnnotation(CONTEXT_TRANSLATION);
        row.createCell(j++).setCellValue(annotation == null || annotation.isCorrect() ? "" : annotation.getComment());

        Map<MiniUser, List<AudioAttribute>> malesMap = exercise.getMostRecentAudio(true, preferredVoices);
//        logger.debug("for ex " + exercise.getOldID() + " males " + malesMap);
//        if (malesMap.isEmpty()) {
//          logger.debug("ex " + exercise.getOldID() + " males   " + exercise.getUserMap(true));
//        }
        Collection<AudioAttribute> defaultUserAudio = exercise.getDefaultUserAudio();

        addColsForGender(exercise, row, j, malesMap, exercise.getSortedUsers(malesMap), defaultUserAudio);

        Map<MiniUser, List<AudioAttribute>> femalesMap = exercise.getMostRecentAudio(false, preferredVoices);
//        logger.debug("for ex " + exercise.getOldID() + " females " + femalesMap);
//        if (femalesMap.isEmpty()) {
//          logger.debug("ex " + exercise.getOldID() + " females " + exercise.getUserMap(false));
//        }

        addColsForGender(exercise, row, j, femalesMap, exercise.getSortedUsers(femalesMap), defaultUserAudio);
      }
    }
    now = System.currentTimeMillis();
    if (now - then > 100) {
      logger.warn("toXLSX : took " + (now - then) + " millis to add " + rownum + " rows to sheet, or " + (now - then) / rownum + " millis/row");
    }
    return wb;
  }

  private void addColsForGender(CommonExercise exercise, Row row, int j,
                                Map<MiniUser, List<AudioAttribute>> malesMap,
                                Collection<MiniUser> maleUsers,
                                Collection<AudioAttribute> defaultUserAudio) {
    if (maleUsers.isEmpty() && defaultUserAudio.isEmpty()) {
      row.createCell(j++).setCellValue("");
      row.createCell(j++).setCellValue("");//sCorrect() ? "":annotation.getComment());
    } else {
      Collection<AudioAttribute> audioAttributes = maleUsers.isEmpty() ? defaultUserAudio : malesMap.get(maleUsers.iterator().next());
//      logger.debug("for ex " + exercise.getOldID() + " first male " + maleUsers.get(0) +  " = " + audioAttributes);
      if (audioAttributes == null || audioAttributes.isEmpty()) audioAttributes = defaultUserAudio;
      addColsForAudio(exercise, row, j, audioAttributes);
    }
  }

  private void addColsForAudio(CommonExercise exercise, Row row, int j,
                               Collection<AudioAttribute> audioAttributes) {
    AudioAttribute regAttr = null;
    AudioAttribute slowAttr = null;
    if (audioAttributes.isEmpty()) logger.debug("huh? no audio for " + exercise.getOldID());
    for (final AudioAttribute audioAttribute : audioAttributes) {
      if (audioAttribute.isRegularSpeed()) {
        regAttr = audioAttribute;
      } else if (audioAttribute.isSlow()) {  // careful not to get context sentence audio ...
        slowAttr = audioAttribute;
      }
    }
    if (regAttr == null) {
      row.createCell(j++).setCellValue("");
    } else {
      //    logger.debug("match " + regAttr);
      for (Map.Entry<String, ExerciseAnnotation> pair : exercise.getFieldToAnnotation().entrySet()) {
        if (regAttr.getAudioRef().endsWith(pair.getKey())) {
          ExerciseAnnotation value = pair.getValue();
          row.createCell(j++).setCellValue(value.isCorrect() ? "" : value.getComment());
          //      logger.debug("\t look for " + regAttr.getAudioRef() + " found " + value);
          break;
        } else {
          //    logger.debug("\t no match " + regAttr.getAudioRef());
        }
      }
    }

    if (slowAttr == null) {
      row.createCell(j++).setCellValue("");
    } else {
      for (Map.Entry<String, ExerciseAnnotation> pair : exercise.getFieldToAnnotation().entrySet()) {
        if (slowAttr.getAudioRef().endsWith(pair.getKey())) {
          ExerciseAnnotation value = pair.getValue();
          row.createCell(j++).setCellValue(value.isCorrect() ? "" : value.getComment());
          //  logger.debug("\t look for " + slowAttr.getAudioRef() + " found " + value);
          break;
        }
      }

      //logger.debug("\t look for " + slowAttr.getAudioRef() + " found " + annotation);
    }
  }

  private boolean addHeaderRow(String language, Collection<String> typeOrder, Row headerRow, boolean isDefectList) {
    List<String> columns = new ArrayList<>();
    columns.add(ID);
    columns.add(WORD_EXPRESSION);
    boolean english = isEnglish(language);
    if (!english) {
      columns.add(language);
    }
    columns.add(english ? MEANING : TRANSLITERATION);
    columns.addAll(typeOrder);
    columns.add(CONTEXT_SENTENCE);
    columns.add(CONTEXT_TRANSLATION);

    if (isDefectList) {
      //  logger.debug("adding defect columns");
      columns.add(WORD_EXPRESSION + "_comment");
      if (!english) columns.add(language + "_comment");
      columns.add(CONTEXT_SENTENCE + "_comment");
      columns.add(CONTEXT_TRANSLATION + "_comment");
      columns.add("male_reg");
      columns.add("male_slow");

      columns.add("female_reg");
      columns.add("female_slow");
    }

    for (int i = 0; i < columns.size(); i++) {
      headerRow.createCell(i).setCellValue(columns.get(i));
    }

    return english;
  }

  /**
   * @param toWrite
   * @param audioDAO
   * @param installPath
   * @param relativeConfigDir1
   * @param name
   * @param typeOrder
   * @param language1
   * @param out
   * @param skipAudio
   * @param isDefectList
   * @throws Exception
   * @see #writeZip
   */
  private void writeToStream(Collection<CommonExercise> toWrite, IAudioDAO audioDAO, String installPath,
                             String relativeConfigDir1, String name, Collection<String> typeOrder,
                             String language1, OutputStream out, boolean skipAudio, boolean isDefectList) throws Exception {
    ZipOutputStream zOut = new ZipOutputStream(out);

    String overallName = language1 + "_" + name;
    overallName = overallName.replaceAll("\\,", "_");
    if (!skipAudio) {
      writeFolderContents(zOut, toWrite, audioDAO, installPath, relativeConfigDir1,
          overallName,
          isEnglish(language1));
    }

    addSpreadsheetToZip(toWrite, typeOrder, language1, zOut, overallName, isDefectList);
  }

  private void writeContextToStream(Collection<CommonExercise> toWrite, IAudioDAO audioDAO, String installPath,
                                    String relativeConfigDir1, String name, Collection<String> typeOrder,
                                    String language1, OutputStream out) throws Exception {
    ZipOutputStream zOut = new ZipOutputStream(out);

    String overallName = language1 + "_" + name;
    overallName = overallName.replaceAll("\\,", "_");
    String twoChar = LTSFactory.getID(LTSFactory.Language.valueOf(language1.toUpperCase()));
    writeFolderContentsContextOnly(zOut, toWrite, audioDAO, installPath, relativeConfigDir1,
        overallName,
        isEnglish(language1), twoChar);

    addSpreadsheetToZip(toWrite, typeOrder, language1, zOut, overallName, false);
  }

  private void addSpreadsheetToZip(Collection<CommonExercise> toWrite, Collection<String> typeOrder,
                                   String language1, ZipOutputStream zOut, String overallName,
                                   boolean isDefectList) throws IOException {
    zOut.putNextEntry(new ZipEntry(overallName + File.separator + overallName + ".xlsx"));
    writeExcelToStream(toWrite, zOut, typeOrder, language1, isDefectList);
  }

  private void writeToStreamJustOneAudio(Collection<CommonExercise> toWrite, String installPath,
                                         OutputStream out) throws Exception {
    writeFolderContentsSimple(new ZipOutputStream(out), toWrite, installPath);
  }

  private boolean isEnglish(String language1) {
    return language1.equalsIgnoreCase("English");
  }

  /**
   * @param zOut
   * @param toWrite
   * @param audioDAO
   * @param installPath
   * @param relativeConfigDir1
   * @param overallName
   * @param isEnglish
   * @throws Exception
   * @see #writeToStream
   */
  private void writeFolderContents(ZipOutputStream zOut,
                                   Collection<CommonExercise> toWrite,
                                   IAudioDAO audioDAO,
                                   String installPath,
                                   String relativeConfigDir1,
                                   String overallName,
                                   boolean isEnglish) throws Exception {
    //int c = 0;
    long then = System.currentTimeMillis();

    //logger.debug("found audio for " + exToAudio.size() + " items and writing " + toWrite.size() + " items ");
    // logger.debug("realContextPath " + realContextPath + " installPath " + installPath + " relativeConfigDir1 " +relativeConfigDir1);

    // get male and female majority users - map of user->count of recordings for this exercise
    Map<MiniUser, Integer> maleToCount = new HashMap<>();
    Map<MiniUser, Integer> femaleToCount = new HashMap<>();

    // attach audio
    int numAttach = 0;
    Map<String, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio();
    for (CommonExercise ex : toWrite) {
      Collection<AudioAttribute> audioAttributes = exToAudio.get(ex.getOldID());
      if (audioAttributes != null) {
        audioDAO.attachAudio(ex, installPath, relativeConfigDir1, audioAttributes);
        numAttach++;
      }
    }

    populateGenderToCount(toWrite, maleToCount, femaleToCount);

    // find the male and female with most recordings for this exercise
    MiniUser male = getMaxUser(maleToCount);
    MiniUser female = getMaxUser(femaleToCount);

    AudioConversion audioConversion = new AudioConversion(props);

    int numMissing = 0;
    Set<String> names = new HashSet<>();
    for (CommonExercise ex : toWrite) {
      boolean someAudio = false;

      // write male/female fast/slow
      for (Boolean gender : GENDERS) {
        MiniUser majorityUser = gender ? male : female;
        for (String speed : SPEEDS) {
          AudioAttribute recording = getAudioAttribute(majorityUser, ex, gender, speed);
          if (recording != null) {
            // logger.debug("found " + recording + " by " + recording.getUser());
            String name = overallName + File.separator + getUniqueName(ex, !isEnglish);
            copyAudio(zOut, names, name, speed, installPath, audioConversion, recording, ex.getOldID(), ex.getForeignLanguage());
            someAudio = true;
          }
        }
      }

      AudioAttribute latestContext = ex.getLatestContext(true);
      if (latestContext != null) {
        copyContextAudio(zOut, installPath, overallName, isEnglish, audioConversion, names, ex, latestContext, "");
        someAudio = true;
      }

      latestContext = ex.getLatestContext(false);
      if (latestContext != null) {
        copyContextAudio(zOut, installPath, overallName, isEnglish, audioConversion, names, ex, latestContext, "");
        someAudio = true;
      }

      if (!someAudio) {
        if (numMissing < 10) {
          logger.debug("no audio for exercise " + ex.getOldID());
        }
        numMissing++;
      }
    }
    long now = System.currentTimeMillis();
    long diff = now - then;
    if (diff > 1000) {
      logger.debug("took " + diff + " millis to export " + toWrite.size() + " items, num attached " + numAttach +
          " missing audio " + numMissing);
    }
  }

  private void writeFolderContentsContextOnly(ZipOutputStream zOut,
                                              Collection<CommonExercise> toWrite,
                                              IAudioDAO audioDAO,
                                              String installPath,
                                              String relativeConfigDir1,
                                              String overallName,
                                              boolean isEnglish,
                                              String twoChar) throws Exception {
    long then = System.currentTimeMillis();

    // attach audio
    int numAttach = 0;
    Map<String, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio();
    for (CommonExercise ex : toWrite) {
      Collection<AudioAttribute> audioAttributes = exToAudio.get(ex.getOldID());
      if (audioAttributes != null) {
        audioDAO.attachAudio(ex, installPath, relativeConfigDir1, audioAttributes);
        numAttach++;
      }
    }

    AudioConversion audioConversion = new AudioConversion(props);

    int numMissing = 0;
    Set<String> names = new HashSet<>();
    for (CommonExercise ex : toWrite) {
      boolean someAudio = false;

      AudioAttribute latestContext = ex.getLatestContext(true);
      if (latestContext != null) {
        copyContextAudio(zOut, installPath, overallName, isEnglish, audioConversion, names, ex, latestContext, twoChar);
        someAudio = true;
      }

      latestContext = ex.getLatestContext(false);
      if (latestContext != null) {
        copyContextAudio(zOut, installPath, overallName, isEnglish, audioConversion, names, ex, latestContext, twoChar);
        someAudio = true;
      }

      if (!someAudio) {
        numMissing++;
      }
    }
    long now = System.currentTimeMillis();
    long diff = now - then;
    if (diff > 1000) {
      logger.debug("took " + diff + " millis to export " + toWrite.size() + " items, num attached " + numAttach +
          " missing audio " + numMissing);
    }
  }

  private void populateGenderToCount(Collection<CommonExercise> toWrite,
                                     Map<MiniUser, Integer> maleToCount,
                                     Map<MiniUser, Integer> femaleToCount) {
    for (CommonExercise ex : toWrite) {
      for (AudioAttribute attr : ex.getAudioAttributes()) {
        MiniUser user = attr.getUser();

        Map<MiniUser, Integer> userToCount = (user.isMale()) ? maleToCount : femaleToCount;
        Integer count = userToCount.get(user);
        userToCount.put(user, (count == null) ? 1 : count + 1);
      }
    }
  }

  /**
   * ae_cntxt_c01_u01_e0003. Mp3
   * <p>
   * ae = Egyptian Arabic
   * contxt = context
   * c01 = Chapter 01
   * U01 = Unit 01
   * E0001 = entry one
   *
   * @param zOut
   * @param installPath
   * @param overallName
   * @param isEnglish
   * @param audioConversion
   * @param names
   * @param ex
   * @param latestContext
   * @throws IOException
   */
  private void copyContextAudio(ZipOutputStream zOut, String installPath, String overallName, boolean isEnglish,
                                AudioConversion audioConversion, Set<String> names, CommonExercise ex,
                                AudioAttribute latestContext,
                                String twoChar) throws IOException {
    String speed = latestContext.getSpeed();
    String folder = overallName + File.separator;
    ex.getUnitToValue();
    //  String name = folder + getUniqueName(ex, !isEnglish) + "_context";
    String name;

    if (twoChar.isEmpty()) {
      name = folder + getUniqueName(ex, !isEnglish) + "_context";
    } else {
      // make a name that looks like: ae_cntxt_c01_u01_e0003. Mp3
      StringBuilder builder = new StringBuilder();
      builder.append(twoChar);
      builder.append("_cntxt_");
      for (String type : typeOrder) {
        builder.append(type.toLowerCase().substring(0, 1));
        builder.append(ex.getUnitToValue().get(type));
        builder.append("_");
      }
      builder.append("e");
      builder.append(ex.getOldID());
      name = folder + builder.toString();
    }
    copyAudio(zOut, names, name, speed == null ? "" : speed, installPath, audioConversion, latestContext, ex.getOldID(),
        ex.getForeignLanguage());
  }

  private MiniUser getMaxUser(Map<MiniUser, Integer> maleToCount) {
    MiniUser male = null;
    int mCount = 0;
    for (Map.Entry<MiniUser, Integer> pair : maleToCount.entrySet()) {
      if (male == null || pair.getValue() > mCount) {
        male = pair.getKey();
        mCount = pair.getValue();
      }
    }
    return male;
  }

  private void writeFolderContentsSimple(ZipOutputStream zOut,
                                         Collection<CommonExercise> toWrite,
                                         String installPath) throws Exception {
    long then = System.currentTimeMillis();
    AudioConversion audioConversion = new AudioConversion(props);
    logger.debug("writing " + toWrite.size());
    int c = 0;
    int d = 0;
    for (CommonExercise ex : toWrite) {
      if (ex.getRefAudio() != null) {
        try {
          AudioAttribute audio = ex.getRegularSpeed();
          String artist = audio.getUser().getUserID();

          copyAudioSimple(zOut, installPath, audioConversion, ex.getRefAudio(), ex.getForeignLanguage(), artist);
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
    logger.debug("took " + diff + " millis to export " + toWrite.size() + " items " + c + " duplicates " + d);

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
   * Prefer recordings by the speaker with the most recordings.
   * If that person didn't do this cut, find someone with the matching gender and speed.
   *
   * @param majorityUser
   * @param ex
   * @param isMale
   * @param speed
   * @return
   * @see #writeFolderContents
   */
  private AudioAttribute getAudioAttribute(MiniUser majorityUser, CommonExercise ex, boolean isMale, String speed) {
    AudioAttribute recordingAtSpeed = majorityUser != null ? ex.getRecordingsBy(majorityUser.getId(), speed) : null;

    if (recordingAtSpeed == null) {  // can't find majority user or nothing by this person at that speed
      Collection<AudioAttribute> byGender = ex.getByGender(isMale);
      // choose the first one if not the majority majorityUser
      for (AudioAttribute attribute : byGender) {
        String speed1 = attribute.getSpeed();
        if (speed1 != null &&
            speed1.equalsIgnoreCase(speed)) {
          recordingAtSpeed = attribute;
          break;
        }
      }
    }
    return recordingAtSpeed;
  }

  /**
   * Worry about name collisions for entries composed of english-fl pairs
   *
   * @param includeFL true if not english
   * @param ex
   * @return
   * @see #writeFolderContents
   */
  private String getUniqueName(CommonExercise ex, boolean includeFL) {
    String trim = ex.getEnglish().trim();
    String foreignLanguage = ex.getForeignLanguage();

    String name = trim.equals("N/A") ? foreignLanguage : trim + (includeFL ? "_" + foreignLanguage : "");
    name = name.trim();
    name = name.replaceAll("\"", "\\'").replaceAll("\\?", "").replaceAll("\\:", "").replaceAll("/", " or ").replaceAll("\\\\", " or ");
    return name;
  }

  /**
   * @param zOut
   * @param names           guarantees entries have unique names (sometimes duplicates in a chapter)
   * @param parent
   * @param speed
   * @param realContextPath
   * @param audioConversion
   * @param attribute
   * @param exid
   * @param title
   * @throws IOException
   * @see #writeFolderContents
   * @see #copyContextAudio
   */
  private void copyAudio(ZipOutputStream zOut, Set<String> names, String parent, String speed,
                         String realContextPath, AudioConversion audioConversion, AudioAttribute attribute,
                         String exid, String title) throws IOException {
    String audioRef = attribute.getAudioRef();
/*    logger.debug("\tfor ex id " +exid +
      " writing audio under context path " + realContextPath + " at " + audioRef);*/
    String author = attribute.getUser().getUserID();
    String s = audioConversion.ensureWriteMP3(audioRef, realContextPath, false, title, author);
    File mp3 = new File(s);
    if (mp3.exists()) {
      //  logger.debug("---> Did write " + mp3.getAbsolutePath());
      String name = getName(parent, speed, attribute);
      if (names.contains(name)) {
        name += "_" + exid;
      }
      names.add(name);
      name = name.replaceAll(" ", "_");
      name += ".mp3";

      //logger.debug("copyContextAudio : mp3 name is " + name);
      addZipEntry(zOut, mp3, name);
    } else {

      String absolutePath = mp3.getAbsolutePath();
      if (!absolutePath.endsWith(AudioConversion.FILE_MISSING)) {
        logger.warn("\tDidn't write " + absolutePath);
      }
    }
  }

  /**
   * @param zOut
   * @param realContextPath
   * @param audioConversion
   * @param audioRef
   * @param title
   * @return
   * @throws IOException
   * @see #writeFolderContentsSimple
   */
  private void copyAudioSimple(ZipOutputStream zOut,
                               String realContextPath,
                               AudioConversion audioConversion,
                               String audioRef,
                               String title, String author) throws IOException {
    String filePath = audioConversion.ensureWriteMP3(audioRef, realContextPath, false, title, author);
    File mp3 = new File(filePath);
    if (mp3.exists()) {
      String name = audioRef.replaceAll(".wav", ".mp3");
      //logger.debug("copyAudioSimple : mp3 name is " + name);
      addZipEntry(zOut, mp3, name);
    } else {
      logger.warn("\tDidn't write " + mp3.getAbsolutePath());
    }
  }

  private void addZipEntry(ZipOutputStream zOut, File mp3, String name) throws IOException {
    zOut.putNextEntry(new ZipEntry(name));
    FileUtils.copyFile(mp3, zOut);
    zOut.flush();
    zOut.closeEntry();
  }

  /**
   * @param parent
   * @param speed
   * @param attribute
   * @return
   * @see #copyContextAudio
   */
  private String getName(String parent, String speed, AudioAttribute attribute) {
    MiniUser user = attribute.getUser();
    String userInfo = user.isDefault() ?
        "" :
        (attribute.isMale() ? MALE : FEMALE);
    String speedSuffix = speed.equals(AudioAttribute.REGULAR) || speed.isEmpty() ? "" : "_" + speed;
    return parent + (user.isDefault() ? "" : "_" + userInfo) + speedSuffix;
  }
}
