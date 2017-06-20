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

package mitll.langtest.server.amas;

import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.shared.exercise.HasUnitChapter;
import mitll.langtest.shared.exercise.Pair;
import mitll.langtest.shared.exercise.Shell;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/7/14.
 */
public class ILRMapping<T extends Shell & HasUnitChapter> {
  private static final Logger logger = LogManager.getLogger(ILRMapping.class);
  public static final String ILR_LEVEL = "ILR Level";
  public static final String TEST_TYPE = "Test type";
  public static final String LISTENING = "Listening";
  public static final String READING = "Reading";
  private static final String QUIZ = "Quiz";

  private final Map<String, Set<String>> levelToExercises = new HashMap<String, Set<String>>();
  private final Map<String, String> exerciseToLevel = new HashMap<String, String>();
  private final Collection<String> readingExercises = new ArrayList<String>();
  private final Collection<String> listeningExercises = new ArrayList<String>();
  private final Map<String, String> exerciseToQuiz = new HashMap<String, String>();

  private final SectionHelper<T> sectionHelper;
  private static final String ENCODING = "UTF8";
  private final boolean usePredefOrder;

  /**
   * @param configDir
   * @param sectionHelper
   * @param mappingFile
   * @param usePredefOrder
   * @see mitll.langtest.server.amas.FileExerciseDAO#FileExerciseDAO
   */
  public ILRMapping(String configDir, SectionHelper<T> sectionHelper, String mappingFile, boolean usePredefOrder) {
    this.sectionHelper = sectionHelper;
    this.usePredefOrder = usePredefOrder;
    File ilrMapping = new File(configDir, mappingFile);
//    logger.debug("config " + configDir + " " + new File(configDir).getAbsolutePath());
    if (!ilrMapping.exists()) {
      logger.warn("ILRMapping : can't find " + ilrMapping.getAbsolutePath());

      ilrMapping = new File(mappingFile);
    }
    if (ilrMapping.exists()) {
      readILRMapping2(ilrMapping);
    } else {
      logger.warn("ILRMapping : can't find " + ilrMapping.getAbsolutePath());
    }
    sectionHelper.report();
  }

  /**
   * For now we only have listening at level 0+
   *
   * @see mitll.langtest.server.amas.FileExerciseDAO#readExercises
   */
  public void finalStep() {
    if (useMapping()) {
      sectionHelper.setPredefinedTypeOrder(Arrays.asList(QUIZ, ILRMapping.TEST_TYPE, ILRMapping.ILR_LEVEL));
    }
  }

  private boolean useMapping() {
    return !exerciseToLevel.isEmpty();
  }

  private void readILRMapping2(File ilrMapping) {
    try {
      BufferedReader reader = getReader(ilrMapping.getAbsolutePath());
      String line;
      while ((line = reader.readLine()) != null) {
        String[] split = line.split("\t");
        if (split.length < 2) continue;
        String ilr = split[1].trim();
        ilr = ilr.split("/")[0];
        String id = split[0].trim();
        Set<String> ids = levelToExercises.get(ilr);
        if (ids == null) {
          levelToExercises.put(ilr, ids = new HashSet<String>());
        }
        exerciseToLevel.put(id, ilr);
        ids.add(id);

        String type = split[2].trim();
        if (type.equals("listening")) {
          listeningExercises.add(id);
        } else if (type.equals("reading")) {
          readingExercises.add(id);
        }
        if (usePredefOrder) {
          exerciseToQuiz.put(id, "1");
        } else {
          String quiz = split[split.length - 1];
          exerciseToQuiz.put(id, quiz.trim());
        }
      }
      logger.debug("level->exercise map has size " + levelToExercises.size() + " keys " + levelToExercises.keySet());
      //logger.debug("listening has size " + listeningExercises.size() + " reading " + readingExercises.size());
      //logger.debug("ex->quiz " +exerciseToQuiz.size() + " " +exerciseToQuiz.values());
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * @param exid
   * @param e
   * @see mitll.langtest.server.amas.FileExerciseDAO#readExercises(String, String, String, InputStream)
   */
  public void addMappingAssoc(String exid, T e) {
    List<Pair> pairs = new ArrayList<Pair>();

    Pair ilrAssoc = sectionHelper.addExerciseToLesson(e, ILR_LEVEL, exerciseToLevel.get(exid));
    pairs.add(ilrAssoc);

    String type = listeningExercises.contains(exid) ? LISTENING : readingExercises.contains(exid) ? READING : "other";
    Pair typeAssoc = sectionHelper.addExerciseToLesson(e, TEST_TYPE, type);
    pairs.add(typeAssoc);

    List<Pair> pairs2 = new ArrayList<Pair>(pairs);

    Pair quiz = sectionHelper.addExerciseToLesson(e, QUIZ, exerciseToQuiz.get(exid));
    pairs.add(quiz);

    //   Pair quiz2 = sectionHelper.addExerciseToLesson(e, QUIZ, "Test");
    //   pairs2.add(quiz2);

    // TODO : consider putting this back?
//    sectionHelper.addAssociations(pairs);
//    sectionHelper.addAssociations(pairs2);
  }

  private BufferedReader getReader(String lessonPlanFile) throws FileNotFoundException, UnsupportedEncodingException {
    return new BufferedReader(new InputStreamReader(new FileInputStream(lessonPlanFile), ENCODING));
  }

  public void report(Map<String, T> idToExercise) {
    int size = idToExercise.keySet().size();
    Set<String> mappedExercises = getMappedExercises();
    int size1 = mappedExercises.size();
    if (size != size1) {
      logger.warn("report huh? there are " + size + " ids from reading the database, but " + size1 + " from reading the mapping file");
      Set<String> strings = new HashSet<String>(idToExercise.keySet());
      strings.removeAll(mappedExercises);
      if (!strings.isEmpty()) logger.warn("unmapped are these ids " + strings);

      Set<String> mapped = new TreeSet<String>(mappedExercises);
      mapped.removeAll(idToExercise.keySet());
      if (!mapped.isEmpty()) {
        logger.warn("mapped that don't appear in exercise ids " + mapped);
      }

    }
  }

  private Set<String> getMappedExercises() {
    Set<String> strings = new HashSet<String>();
    for (Set<String> ids : levelToExercises.values()) {
      strings.addAll(ids);
    }
    return strings;
  }
}
