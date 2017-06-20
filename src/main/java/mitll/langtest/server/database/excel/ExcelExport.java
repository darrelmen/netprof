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

package mitll.langtest.server.database.excel;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.ExerciseAnnotation;
import mitll.langtest.shared.user.MiniUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

public class ExcelExport {
  private static final Logger logger = LogManager.getLogger(ExcelExport.class);

  private static final String ID = "ID";
  private static final String WORD_EXPRESSION = "Word/Expression";
  private static final String TRANSLITERATION = "Transliteration";
  private static final String MEANING = "Meaning";
  private static final String MALE = "Male";
  private static final String FEMALE = "Female";
  private static final String CONTEXT_SENTENCE = "Context Sentence";
  private static final String CONTEXT_TRANSLATION = "Context Translation";
  private final ServerProperties props;

  public ExcelExport(ServerProperties props) {
    this.props = props;
  }

  /**
   * @param exercises
   * @param out
   * @param typeOrder
   * @param language
   * @param isDefectList
   * @seex #writeToStream
   * @seex #addSpreadsheetToZip
   */
  public void writeExcelToStream(Collection<CommonExercise> exercises,
                                 OutputStream out,
                                 Collection<String> typeOrder,
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
  private SXSSFWorkbook writeExcel(Collection<CommonExercise> copy,
                                   String language,
                                   Collection<String> typeOrder,
                                   boolean isDefectList) {
    long now;
    long then = System.currentTimeMillis();

    SXSSFWorkbook wb = new SXSSFWorkbook(1000); // keep 100 rows in memory, exceeding rows will be flushed to disk
    Sheet sheet = wb.createSheet("Exercises");
    int rownum = 0;
    Row headerRow = sheet.createRow(rownum++);

    boolean english = addHeaderRow(language, typeOrder, headerRow, isDefectList);

    /**
     * Deal with per-project voices...
     */
    Set<Integer> preferredVoices = props.getPreferredVoices();

    for (CommonExercise exercise : copy) {
      Row row = sheet.createRow(rownum++);

      int j = 0;

      row.createCell(j++).setCellValue(exercise.getID());

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
      Collection<CommonExercise> directlyRelated = exercise.getDirectlyRelated();
      if (!directlyRelated.isEmpty()) {
        CommonExercise next = directlyRelated.iterator().next();
        row.createCell(j++).setCellValue(next.getForeignLanguage());
        row.createCell(j++).setCellValue(next.getEnglish());
      }
      if (isDefectList) {
        addDefects(english, preferredVoices, exercise, row, j);

      }
    }
    now = System.currentTimeMillis();
    if (now - then > 100) {
      logger.warn("toXLSX : took " + (now - then) + " millis to add " + rownum + " rows to sheet, or " + (now - then) / rownum + " millis/row");
    }
    return wb;
  }

  private void addDefects(boolean english, Set<Integer> preferredVoices, CommonExercise exercise, Row row, int j) {
    //        logger.debug("annos for " + exercise.getID() + "\tfields : " + exercise.getFieldToAnnotation());
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

    Map<MiniUser, List<AudioAttribute>> malesMap = exercise.getMostRecentAudio(true, preferredVoices, false);
//        logger.debug("for ex " + exercise.getID() + " males " + malesMap);
//        if (malesMap.isEmpty()) {
//          logger.debug("ex " + exercise.getID() + " males   " + exercise.getUserMap(true));
//        }
    Collection<AudioAttribute> defaultUserAudio = exercise.getDefaultUserAudio();

    addColsForGender(exercise, row, j, malesMap, exercise.getSortedUsers(malesMap), defaultUserAudio);

    Map<MiniUser, List<AudioAttribute>> femalesMap = exercise.getMostRecentAudio(false, preferredVoices, false);
//        logger.debug("for ex " + exercise.getID() + " females " + femalesMap);
//        if (femalesMap.isEmpty()) {
//          logger.debug("ex " + exercise.getID() + " females " + exercise.getUserMap(false));
//        }

    addColsForGender(exercise, row, j, femalesMap, exercise.getSortedUsers(femalesMap), defaultUserAudio);
  }

  private void addColsForGender(CommonExercise exercise, Row row, int j,
                                Map<MiniUser, List<AudioAttribute>> malesMap,
                                Collection<MiniUser> maleUsers,
                                Collection<AudioAttribute> defaultUserAudio) {
    if (maleUsers.isEmpty() && defaultUserAudio.isEmpty()) {
      row.createCell(j++).setCellValue("");
      row.createCell(j++).setCellValue("");//sCorrect() ? "":annotation.getValue());
    } else {
      Collection<AudioAttribute> audioAttributes = maleUsers.isEmpty() ? defaultUserAudio : malesMap.get(maleUsers.iterator().next());
//      logger.debug("for ex " + exercise.getID() + " first male " + maleUsers.get(0) +  " = " + audioAttributes);
      if (audioAttributes == null || audioAttributes.isEmpty()) audioAttributes = defaultUserAudio;
      addColsForAudio(exercise, row, j, audioAttributes);
    }
  }

  private void addColsForAudio(CommonExercise exercise, Row row, int j,
                               Collection<AudioAttribute> audioAttributes) {
    AudioAttribute regAttr = null;
    AudioAttribute slowAttr = null;
    if (audioAttributes.isEmpty()) logger.debug("huh? no audio for " + exercise.getID());
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

  private boolean isEnglish(String language1) {
    return language1.equalsIgnoreCase("English");
  }
}
