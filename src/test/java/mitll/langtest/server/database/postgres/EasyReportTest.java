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

package mitll.langtest.server.database.postgres;


import mitll.langtest.server.autocrt.DecodeCorrectnessChecker;
import mitll.langtest.server.database.BaseTest;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.analysis.SlickAnalysis;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.database.result.SlickResultDAO;
import mitll.langtest.server.domino.ImportInfo;
import mitll.langtest.server.domino.ProjectSync;
import mitll.langtest.server.json.JsonExport;
import mitll.langtest.server.scoring.LTSFactory;
import mitll.langtest.shared.analysis.*;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectType;
import mitll.npdata.dao.lts.HTKDictionary;
import mitll.npdata.dao.lts.KoreanLTS;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.*;

import static java.lang.Thread.sleep;

public class EasyReportTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(EasyReportTest.class);
  public static final int MAX = 200;
  public static final int USERID = 1474;
  public static final int KOREAN = 2;
  public static final int SPANISH = 3;
  public static final int DEMO_USER = 659;
  public static final String KANGNU = "강루";
  String longer = "대폭강화하기로";

  @Test
  public void testComment() {
    DatabaseImpl english = getDatabase();
    Project project = english.getProject(4);

    FilterResponse typeToValues = english.getTypeToValues(new FilterRequest().setRecordRequest(true), 4, 6);
    logger.info("Got " + typeToValues);
//    english.getUserListManager().getCommentedList(4, false);
    // english.getUserListManager().getCommentedList(4, true);
  }

  @Test
  public void testSimpleRec() {
    DatabaseImpl english = getDatabase();
    /*Project project2 = */
    english.getProject(4);

    Project project = english.getProjectByName("Levantine");

    waitUntilDelegate(english, project);
  }

  private void waitUntilDelegate(DatabaseImpl db, Project project) {
    //new Thread(() -> {
    while (db.getUserDAO().getDefaultUser() < 1) {
      try {
        sleep(1000);
        logger.info("waitUntilDelegate ---> no default user yet.....");
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }


    {
        {
          FilterRequest request = new FilterRequest().setRecordRequest(true);
          project.getTypeOrder().forEach(type -> request.addPair(new Pair(type, SectionHelper.ANY)));
          FilterResponse typeToValues = db.getTypeToValues(request, project.getID(), 6);
        }


      FilterRequest request = new FilterRequest().setRecordRequest(true);

      {
        project.getTypeOrder().forEach(type ->
            request.addPair(new Pair(
                type,
                type.equals("Unit") ? "8" : SectionHelper.DEFAULT_FOR_EMPTY)));
      }

      logger.info("Request is " + request);

      FilterResponse typeToValues = db.getTypeToValues(request, project.getID(), 6);
      logger.info("\n\n\nGot " + typeToValues);

      ExerciseListRequest request1 = new ExerciseListRequest(-1, 6);
      request1.setOnlyUnrecordedByMe(true);
      {
        Map<String, Collection<String>> typeToSelection = new HashMap<>();
        typeToSelection.put("Unit",Collections.singleton("8"));

//        project.getTypeOrder().forEach(type ->
//            typeToSelection.put(
//                type,
//                Collections.singleton(type.equals("Unit") ? "8" : SectionHelper.DEFAULT_FOR_EMPTY)));
////              type.equals("Unit") ? "8" : SectionHelper.ANY)));

        request1.setTypeToSelection(typeToSelection);
      }

      List<CommonExercise> exercisesForSelectionState =
          db.getFilterResponseHelper().getExercisesForSelectionState(request1, project.getID());

      logger.info("Got back " + exercisesForSelectionState.size());
      exercisesForSelectionState.forEach(exercise -> logger.info("got " + exercise.getID() + " " + exercise.getForeignLanguage() + " context " +exercise.isContext()));
    }
    //}//, "waitUntilDelegate_" + projectID).start();
  }


  @Test
  public void testSimpleRecRequest() {
    DatabaseImpl english = getDatabase();
    /*Project project2 = */
    english.getProject(4);

    Project project = english.getProjectByName("Spanish");

    {
      FilterRequest request = new FilterRequest().setRecordRequest(true);
      project.getTypeOrder().forEach(type -> request.addPair(new Pair(type, SectionHelper.ANY)));

      logger.info("Request is " + request);

      FilterResponse typeToValues = english.getTypeToValues(request, project.getID(), 6);
      logger.info("Got " + typeToValues);
    }

    {
      FilterRequest request = new FilterRequest().setRecordRequest(true);
      project.getTypeOrder().forEach(type ->
          request.addPair(new Pair(
              type,
              type.equals("Unit") ? "1" : SectionHelper.ANY)));

      logger.info("Request is " + request);

      FilterResponse typeToValues = english.getTypeToValues(request, project.getID(), 6);
      logger.info("Got " + typeToValues);
    }


    {
      FilterRequest request = new FilterRequest().setRecordRequest(true);
      project.getTypeOrder().forEach(type ->
          request.addPair(new Pair(
              type,
              type.equals("Unit") ? "2" : SectionHelper.ANY)));

      logger.info("Request is " + request);

      FilterResponse typeToValues = english.getTypeToValues(request, project.getID(), 6);
      logger.info("Got " + typeToValues);
    }

  }

  @Test
  public void testContextRecRequest() {
    DatabaseImpl english = getDatabase();
    /*Project project2 = */
    english.getProject(4);

    Project project = english.getProjectByName("Spanish");

    {
      FilterRequest request = new FilterRequest().setRecordRequest(true).setExampleRequest(true);
      project.getTypeOrder().forEach(type -> request.addPair(new Pair(type, SectionHelper.ANY)));

      logger.info("\n\n\n - Request is " + request);

      FilterResponse typeToValues = english.getTypeToValues(request, project.getID(), 6);
      logger.info("Got " + typeToValues);
    }

    {
      FilterRequest request = new FilterRequest().setRecordRequest(true).setExampleRequest(true);
      project.getTypeOrder().forEach(type ->
          request.addPair(new Pair(
              type,
              type.equals("Unit") ? "1" : SectionHelper.ANY)));

      logger.info("\n\n\n -  Request is " + request);

      FilterResponse typeToValues = english.getTypeToValues(request, project.getID(), 6);
      logger.info("Got " + typeToValues);
    }


    {
      FilterRequest request = new FilterRequest().setRecordRequest(true).setExampleRequest(true);
      project.getTypeOrder().forEach(type ->
          request.addPair(new Pair(
              type,
              type.equals("Unit") ? "2" : SectionHelper.ANY)));

      logger.info("\n\n\n -  Request is " + request);

      FilterResponse typeToValues = english.getTypeToValues(request, project.getID(), 6);
      logger.info("Got " + typeToValues);
    }

  }

  @Test
  public void testCommentContext() {
    DatabaseImpl english = getDatabase();
    english.getProject(4);
    // english.getUserListManager().getCommentedList(4, false);
    english.getUserListManager().getCommentedList(4, true);
  }

  @Test
  public void testCom() {
//    String test = "연약한";
//    String test2= "침략당한";
//    List<String> koreanFragments = getKoreanFragments(test);
//    koreanFragments.forEach(logger::info);
//    koreanFragments = getKoreanFragments(test2);
//    koreanFragments.forEach(logger::info);

    List<String> koreanFragments = getKoreanFragments(KANGNU);
    koreanFragments.forEach(logger::info);
  }

  @Test
  public void testKorean() {
    //  DatabaseImpl db = getAndPopulate();
//    int projectid = KOREAN;
    //  Project project = db.getProject(projectid);

    //  CommonExercise next = project.getRawExercises().iterator().next();
    String foreignLanguage = longer;//"한글";//next.getForeignLanguage();

    HTKDictionary htkDictionary = new HTKDictionary("/opt/dcodr/scoring/models.dli-korean/rsi-sctm-hlda/dict-wo-sp");
    // SmallVocabDecoder smallVocabDecoder = new SmallVocabDecoder(htkDictionary, false);


    scala.collection.immutable.List<String[]> pronunciationList = htkDictionary.apply(foreignLanguage);

    // scala.collection.Iterator<String> keys = htkDictionary.keysIterator();

//    int n = 0;
//    while (keys.hasNext()) {
//      String next = keys.next();
//      java.util.List<java.util.List<String>> pronsFromDict = getPronsFromDict(htkDictionary.apply(next));
//
//      pronsFromDict.forEach(p -> {
//        StringBuilder builder = new StringBuilder();
//        p.forEach(c -> builder.append(c).append("-"));
//        logger.info(next + " got " + builder);
//      });
//
//      getKoreanFragments(next);
//      if (n++ > 10) break;
//    }

    List<List<String>> pronsFromDict = getPronsFromDict(pronunciationList);
    pronsFromDict.forEach(p -> {
      StringBuilder builder = new StringBuilder();
      p.forEach(c -> builder.append(c).append("-"));
      logger.info("for " + foreignLanguage + " got " + builder);


      String[][] process = new String[1][];
      int size = p.size();
      String[] strings1 = new String[size];
      process[0] = strings1;
      for (int i = 0; i < size; i++) strings1[i] = p.get(i);
      List<String> koreanFragments = getKoreanFragments(foreignLanguage, new KoreanLTS(), process);

      StringBuilder builder2 = new StringBuilder();
      koreanFragments.forEach(c -> builder2.append(c).append(" "));
      logger.info("for " + foreignLanguage + " fl (" + size +
          ") " + builder + " = frag (" + koreanFragments.get(0).split("\\s").length +
          ") " + builder2);
    });

//    getKoreanFragments(foreignLanguage);
//    getKoreanFragments(KANGNU);
  }

  private java.util.List<java.util.List<String>> getPronsFromDict(scala.collection.immutable.List<String[]> pronunciationList) {
    scala.collection.Iterator<String[]> iterator = pronunciationList.iterator();
    java.util.List<java.util.List<String>> prons = new ArrayList<>();
    while (iterator.hasNext()) {
      String[] next = iterator.next();
//      logger.info(next);
      ArrayList<String> pron = new ArrayList<>();
      prons.add(pron);
      pron.addAll(Arrays.asList(next));
    }
    return prons;
  }

//  List<WordAndProns> getProns(String foreignLanguage) {
//    List<WordAndProns> possibleProns = new ArrayList<>();
//
////    logger.info("getProxyScore " +
////
////        "\n\tdict " + getHydraDict(foreignLanguage, possibleProns));
//    return possibleProns;
//    //possibleProns.forEach(p -> logger.info(foreignLanguage + " : " + p));
//  }

  // so take every pronunciation in the dict and map back into fragment sequence
  // if two hydra phonemes combine to form one compound, use it and skip ahead two
  // if multiple fragments are possible, try to chose the one that is expected from the compound character
  // if it's not there, use the first simple match...
  private List<String> getKoreanFragments(String foreignLanguage) {
    KoreanLTS koreanLTS = new KoreanLTS();
    String[][] process = koreanLTS.process(foreignLanguage);
    return getKoreanFragments(foreignLanguage, koreanLTS, process);
  }

  @NotNull
  private List<String> getKoreanFragments(String foreignLanguage, KoreanLTS koreanLTS, String[][] process) {
    List<List<String>> fragmentList = getKoreanFragments(foreignLanguage, koreanLTS);

    // logger.info("for " + foreignLanguage + " expected "+fragmentList);
    // StringBuilder converted = new StringBuilder();
    List<String> ret = new ArrayList<>();
    for (int i = 0; i < process.length; i++) {
      logger.info("got " + foreignLanguage + " " + i);
      String[] hydraPhoneSequence = process[i];
      ret.add(getKoreanFragmentSequence(fragmentList, hydraPhoneSequence));
    }
    return ret;
  }

  private String getKoreanFragmentSequence(List<List<String>> fragmentList, String[] hydraPhoneSequence) {
    int length = hydraPhoneSequence.length;
    StringBuilder builder = new StringBuilder();

    int fragIndex = 0;
    int fragCount = 0;
    List<String> currentFragments = fragmentList.get(fragIndex);

    //sanityCheck();

    String prevMatch = null;
    for (int j = 0; j < length; j++) {
      String process1 = hydraPhoneSequence[j];
      String nextToken = j < length - 1 ? hydraPhoneSequence[j + 1] : "";
      List<String> simpleKorean = LTSFactory.getSimpleKorean(process1);
      List<String> compoundKorean = LTSFactory.getCompoundKorean(process1, nextToken);

      logger.info("got " + j + " " + process1 + "+" + nextToken +
          " = " + simpleKorean + " - " + compoundKorean);

      if (compoundKorean == null || compoundKorean.isEmpty()) {
        String str = simpleKorean.get(0);

        if (simpleKorean.size() == 1) {
          builder.append(str).append(" ");
          prevMatch = str;
        } else {
          String match = getMatch(fragIndex, currentFragments, simpleKorean);
          if (match != null) {
            builder.append(match).append(" ");
            prevMatch = match;
          } else {
            if (currentFragments.contains(prevMatch)) {
              logger.info("using prev match " + prevMatch + " for " + currentFragments);

              fragCount++;
              if (fragCount == currentFragments.size()) {
//            logger.info("1 frag index now " + fragCount + " vs " + currentFragments.size() + " index " + fragIndex);
                fragCount = 0;
                fragIndex++;
                if (fragIndex < fragmentList.size()) {
                  currentFragments = fragmentList.get(fragIndex);
                  logger.info("3 frag index now " + fragIndex + " " + new HashSet<>(currentFragments));
                } else {
                  logger.info("3 frag index NOPE " + fragIndex + " " + new HashSet<>(currentFragments));
                }
              } else {
//            logger.info("1 " + fragCount + " vs " + currentFragments.size());
              }

              match = getMatch(fragIndex, currentFragments, simpleKorean);
              if (match != null) {
                builder.append(match).append(" ");
                prevMatch = match;
              }
            } else {
              //match = getMatch(fragIndex, currentFragments, prevSimple);
              logger.warn("fall back to " + str + " given expected " + new HashSet<>(currentFragments));
              builder.append(str).append(" ");
            }
          }
        }

        fragCount++;
        if (fragCount == currentFragments.size()) {
//            logger.info("1 frag index now " + fragCount + " vs " + currentFragments.size() + " index " + fragIndex);
          fragCount = 0;
          fragIndex++;
          if (fragIndex < fragmentList.size()) {
            currentFragments = fragmentList.get(fragIndex);
            logger.info("1 frag index now " + fragIndex + " " + new HashSet<>(currentFragments));
          } else {
            logger.info("1 frag index NOPE " + fragIndex + " " + new HashSet<>(currentFragments));
          }
        } else {
//            logger.info("1 " + fragCount + " vs " + currentFragments.size());
        }


      } else {
        j++;

        String match = getMatch(fragIndex, currentFragments, compoundKorean);

        if (match != null) builder.append(match).append(" ");
        else {
          String s = compoundKorean.get(0);
          logger.warn("2 fall back to " + s + " given " + new HashSet<>(currentFragments));
          builder.append(s).append(" ");
        }
        fragCount++;
        if (fragCount == currentFragments.size()) {
          fragCount = 0;
          fragIndex++;
          if (fragIndex < fragmentList.size()) {
            currentFragments = fragmentList.get(fragIndex);
            logger.info("2 frag index now " + fragIndex + " " + new HashSet<>(currentFragments));
          }
        }
        //else logger.info("2 " + fragCount + " vs " + currentFragments.size());

      }
//        logger.info("got " + i + " " + j + " " + process1 + " = " + simpleKorean + " - " + compoundKorean);

    }
    return builder.toString();
    //ret.add(e);
  }

  private void sanityCheck() {
    new KoreanLTS().phoneToKoreanJava().forEach((k, v) -> {
      int lsize = v.size();
      HashSet<String> strings = new HashSet<>(v);
      int ssize = strings.size();
      //logger.info(k + "->" + v);
      if (lsize != ssize) logger.warn("l " + lsize + " s " + ssize);
    });
  }

  @NotNull
  private List<List<String>> getKoreanFragments(String foreignLanguage, KoreanLTS koreanLTS) {
    char[] chars = foreignLanguage.toCharArray();
    List<List<String>> fragmentList = new ArrayList<>();
    for (char aChar : chars) {
      List<String> e = koreanLTS.expectedFragments(aChar);
      fragmentList.add(e);

      e.forEach(f -> logger.info("for " + foreignLanguage + " '" + aChar +
          "'  expected '" + f + "' of " + e.size()));
      // logger.info("for " + foreignLanguage + " expected "+fragmentList);
    }
    return fragmentList;
  }

  @Nullable
  private String getMatch(int fragIndex, List<String> currentFragments, List<String> simpleKorean) {
    String match = null;
    for (String candidate : simpleKorean) {
      boolean contains = currentFragments.contains(candidate);
      if (contains)
        logger.info("check " + candidate + " in (" + fragIndex + ")" + new HashSet<>(currentFragments) + " = " + contains);
      if (contains) {
        match = candidate;
//                builder.append(candidate).append(" ");
//              found = true;
        break;
      }
    }
    return match;
  }
  //public static final int MAX = 200;
//  private static final int USERID = 1474;
//  private static final int SPANISH = 3;
//  private static final int DEMO_USER = 659;

  @Test

  public void testTurk() {
  }

  @Test
  public void testKaldi() {
    DatabaseImpl db = getAndPopulate();
    int projectid = 6;
    Project project = db.getProject(projectid);
    DecodeCorrectnessChecker decodeCorrectnessChecker = new DecodeCorrectnessChecker(null, 0, project.getAudioFileHelper().getSmallVocabDecoder());
    String phraseToDecode = decodeCorrectnessChecker.getPhraseToDecode("selamımı", Language.TURKISH);


    String iki = "İkizler";

    logger.info("Got " + phraseToDecode);

    phraseToDecode = decodeCorrectnessChecker.getPhraseToDecode(iki, Language.TURKISH);

    logger.info("Got " + phraseToDecode);

    //  project.recalcRefAudio();
  }

  @Test
  public void testTurkish() {
    DatabaseImpl db = getAndPopulate();
    int projectid = 6;
    Project project = db.getProject(projectid);
    CommonExercise exerciseByID = project.getExerciseByID(33981);
    String foreignLanguage = exerciseByID.getForeignLanguage();
    String segmented = project.getAudioFileHelper().getASR().getSegmented(foreignLanguage);
    logger.info("For " + exerciseByID);
    logger.info("For '" + foreignLanguage + "' : '" + segmented + "'");
    // project.getAudioFileHelper().checkLTSAndCountPhones(project.getRawExercises());
  }

  @Test
  public void testCroatian() {
    DatabaseImpl db = getAndPopulate();
    int projectid = 7;
    Project project = db.getProject(projectid);
    project.getAudioFileHelper().checkLTSAndCountPhones(project.getRawExercises());

    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testSerbian() {
    DatabaseImpl db = getAndPopulate();
    int projectid = 8;
    Project project = db.getProject(projectid);
    project.getAudioFileHelper().checkLTSAndCountPhones(project.getRawExercises());
  }

  @Test
  public void testPhoneReport() {
    DatabaseImpl db = getAndPopulate();
    int projectid = KOREAN;
    Project project = db.getProject(projectid);
    SlickAnalysis slickAnalysis = new SlickAnalysis(
        db.getDatabase(),
        db.getPhoneDAO(),
        db.getAudioDAO(),
        (SlickResultDAO) db.getResultDAO(),
        project.getLanguageEnum(),
        projectid,
        project.getKind() == ProjectType.POLYGLOT);

    AnalysisRequest analysisRequest = new AnalysisRequest(/*DEMO_USER, -1, -1, 0*/);
    AnalysisReport performanceReportForUser = slickAnalysis.getPerformanceReportForUser(analysisRequest);

    logger.info("Got " + performanceReportForUser);
    logger.info("phone summary " + performanceReportForUser.getPhoneSummary().getPhoneToAvgSorted());

    long maxValue = Long.MAX_VALUE;

    PhoneBigrams phoneBigramsForPeriod = slickAnalysis.getPhoneBigramsForPeriod(analysisRequest);

    logger.info("bigrams " + phoneBigramsForPeriod);

    Map<String, List<Bigram>> phoneToBigrams = phoneBigramsForPeriod.getPhoneToBigrams();
    phoneToBigrams.forEach((s, bigrams) -> logger.info(s + " -> " + bigrams.size() + " : " + bigrams));

    //  WordsAndTotal wordScoresForUser = slickAnalysis.getWordScoresForUser(DEMO_USER, -1, -1, 0, maxValue, 0, 100, "");


    String b = "b";
//    List<Bigram> bigrams = phoneToBigrams.get(b);
    long fiveYearsFromNow = System.currentTimeMillis() + 5 * 365 * 24 * 60 * 60 * 1000L;
/*

    bigrams.forEach(bigram -> {
      logger.info(b + " " + bigram + "\n\n\n");
      List<WordAndScore> nj = slickAnalysis.getPhoneReportFor(DEMO_USER, -1, b, bigram.getBigram(), 0, fiveYearsFromNow);

      if (nj == null) {
        logger.warn("testPhoneReport no results for " + b + " " + bigram);
      }
      else {
        nj.forEach(wordAndScore -> logger.info(b + " " + bigram + " : " + wordAndScore.getWord()));
      }
    });
*/

    phoneToBigrams.forEach((phone, bigrams) -> {
      logger.info(phone + " -> " + bigrams.size() + " : " + bigrams);
      bigrams.forEach(bigram -> {
        logger.info("\t" + phone + " -> " + bigram);
        List<WordAndScore> wordAndScoreForPhoneAndBigram = slickAnalysis.getWordAndScoreForPhoneAndBigram(
            new AnalysisRequest()
                .setUserid(DEMO_USER)
                .setPhone(phone)
                .setBigram(bigram.getBigram()));
        logger.info("\t" + phone + " -> " + bigram + " : " + wordAndScoreForPhoneAndBigram.size());

      });
    });

/*

    String bigram ="dh-b";
    logger.info(b + " " + bigram + "\n\n\n");
    List<WordAndScore> nj = slickAnalysis.getPhoneReportFor(new AnalysisRequest());

    if (nj == null) {
      logger.warn("testPhoneReport no results for " + b + " " + bigram);
    }
    else {
      nj.forEach(wordAndScore -> logger.info(b + " " + bigram + " : " + wordAndScore.getWord()));
    }
*/


  }

  @Test
  public void testAnalysis() {
    DatabaseImpl andPopulate = getAndPopulate();
    Project project = andPopulate.getProject(7);
    //   project.getAnalysis().getPerformanceReportForUser(USERID, 0, -1, 0);

    project.getAnalysis().getWordAndScoreForPhoneAndBigram(new AnalysisRequest());
    //  andPopulate.sendReport(-1);
    andPopulate.close();
  }

/*  @Test
  public void testReport() {
    DatabaseImpl andPopulate = getAndPopulate();
    andPopulate.getReport();
    andPopulate.close();
  }*/


  @Test
  public void test() {
    DatabaseImpl andPopulate = getAndPopulate();
    //  andPopulate.sendReport(-1);
    andPopulate.close();
  }

  @Test
  public void test2() {
    List<String> strings = Arrays.asList("au-dessus", "au -dessus", "abandonnée", "Appelez-moi.");
    strings.forEach(token -> {
      String[] split = token.split("['\\-]");
      for (String s : split) {

        logger.info(" " + token + " " + s);
      }
    });

  }

  @Test
  public void testFrench() {
    DatabaseImpl andPopulate = getAndPopulate();

    Project project = andPopulate.getProject(13);


    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    List<String> strings = Arrays.asList("au-dessus", "au -dessus", "abandonnée", "Appelez-moi.");
    {
      String aberration = "aberration";
      String res = project.getAudioFileHelper().getSegmented(aberration);
      logger.info("\n\ntestSegment res " + res);
      String res2 = project.getAudioFileHelper().getPronunciationsFromDictOrLTS(aberration, "");
      logger.info("\n\ntestSegment res2 " + res2);

    }
    {
      String aberration = "aberrations";
      String res = project.getAudioFileHelper().getSegmented(aberration);
      logger.info("\n\ntestSegment res " + res);
      String res2 = project.getAudioFileHelper().getPronunciationsFromDictOrLTS(aberration, "");
      logger.info("\n\ntestSegment res2 " + res2);

    }
    strings.forEach(s -> {
      String res = project.getAudioFileHelper().getSegmented(s);
      logger.info("\n\ntestSegment " + s +
          " res " + res);
      String res2 = project.getAudioFileHelper().getPronunciationsFromDictOrLTS(s, "");
      logger.info("\n\ntestSegment " + s +
          " res2 " + res2);
    });
    andPopulate.close();

  }

  @Test
  public void testSegment() {
    DatabaseImpl andPopulate = getAndPopulate();

    Project project = andPopulate.getProject(28);

    String temp = "１";
    String sentence = "きのうの午後５時から７時まで黒い車が止まっていました";
    logger.info("\n\ntestSegment for " + sentence);

    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    {
      String res = project.getAudioFileHelper().getSegmented(sentence);

      logger.info("\n\ntestSegment res " + res);
    }
    {
      CommonExercise exerciseByID = project.getExerciseByID(214154);

      String res = project.getAudioFileHelper().getSegmented(exerciseByID.getForeignLanguage());

      logger.info("\n\ntestSegment " + exerciseByID.getForeignLanguage() +
          " res " + res);
    }
    andPopulate.close();

  }

  @Test
  public void testSync() {
    DatabaseImpl andPopulate = getAndPopulate();
    int projectid = 16;

    ProjectSync projectSync = andPopulate.getProjectSync();

    ImportInfo importFromDomino = andPopulate.getProjectManagement().getImportFromDomino(projectid);
    DominoUpdateResponse dominoUpdateResponse = projectSync.getDominoUpdateResponse(projectid, 2, false, importFromDomino);


    logger.info("Got " + dominoUpdateResponse);

    andPopulate.close();
  }

  @Test
  public void testSendReport() {
    DatabaseImpl andPopulate = getAndPopulate();
    andPopulate.sendReport(-1);
    andPopulate.close();
  }

  @Test
  public void testQuiz() {
    DatabaseImpl andPopulate = getAndPopulate();

    int projectid = 2;
    showQuizInfo(andPopulate, projectid);
    showQuizInfo(andPopulate, 5);
    //  andPopulate.sendReport(-1);
    andPopulate.close();
  }

  private void showQuizInfo(DatabaseImpl andPopulate, int projectid) {
    Project project = andPopulate.getProject(projectid);

    ISection<CommonExercise> sectionHelper = project.getSectionHelper();
    //String next = sectionHelper.getTypeOrder().iterator().next();
    sectionHelper.report();

    Collection<CommonExercise> first = sectionHelper.getFirst();
    logger.info("first " + first.size() + " e.g. " + first.iterator().next());

    /*ISection<CommonExercise> quizSectionHelper = andPopulate.getQuizSectionHelper(projectid);//, project.getSectionHelper().getFirst());
    HashMap<String, Collection<String>> typeToSection = new HashMap<>();
    typeToSection.put("QUIZ", Arrays.asList("quiz"));
    typeToSection.put("Unit", Arrays.asList("Dry Run"));
*/
   /* Collection<CommonExercise> exercisesForSelectionState = quizSectionHelper.getExercisesForSelectionState(typeToSection);
    logger.info("1 for  " + typeToSection + " got " +exercisesForSelectionState.size());


    typeToSection.put("Unit", Arrays.asList("Quiz"));
    Collection<CommonExercise> exercisesForSelectionState2 = quizSectionHelper.getExercisesForSelectionState(typeToSection);

    logger.info("2 for  " + typeToSection + " got " +exercisesForSelectionState2.size());*/
  }

  @Test
  public void testReportWrite() {
    DatabaseImpl andPopulate = getAndPopulate();
    andPopulate.doReportForYear(-1);
    andPopulate.close();
  }

  @Test
  public void testFind() {
    DatabaseImpl andPopulate = getAndPopulate();
    Project project = andPopulate.getProject(3);
    CommonExercise vit = project.getExerciseBySearch("vit");
    logger.info(vit);
    logger.info(project.getExerciseBySearch("Vit"));
    logger.info(project.getExerciseBySearch("gourd"));
    //andPopulate.doReportForYear(-1);
    andPopulate.close();
  }

  @Test
  public void testJson() {
    DatabaseImpl andPopulate = getAndPopulate();
    //   Project project = andPopulate.getProject(3);
    JsonExport jsonExport = andPopulate.getJSONExport(3);
    // long now = System.currentTimeMillis();

    JSONArray contentAsJson = jsonExport.getContentAsJson(false);
    logger.info("Got\n\t" + contentAsJson);
  }

  @Test
  public void testJson2() {
    DatabaseImpl andPopulate = getAndPopulate();
    //   Project project = andPopulate.getProject(3);
    HashMap<String, Collection<String>> typeToValues = new HashMap<>();
    typeToValues.put("Unit", Collections.singleton("21"));
    JSONObject jsonPhoneReport = andPopulate.getJsonPhoneReport(295, 2, typeToValues);
    // long now = System.currentTimeMillis();

    logger.info("Got\n\t" + jsonPhoneReport);
  }


  @Test
  public void testProp() {
    DatabaseImpl andPopulate = getAndPopulate();

    Project project = andPopulate.getProject(2);
    int webservicePort = project.getWebservicePort();
    logger.info("port " + webservicePort);
    logger.info("host " + project.getWebserviceHost());
  }
}
