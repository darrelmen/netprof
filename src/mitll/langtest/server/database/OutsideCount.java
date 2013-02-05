package mitll.langtest.server.database;

import mitll.langtest.shared.Exercise;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Get statistics from the file produced by measure.scala from Jessica and Wade.
 *
 * User: GO22670
 * Date: 2/1/13
 * Time: 11:29 AM
 * To change this template use File | Settings | File Templates.
 */
public class OutsideCount {
  private static Logger logger = Logger.getLogger(OutsideCount.class);

  /**
   * @see DatabaseImpl#getExercisesBiasTowardsUnanswered(boolean, long, String)
   * @see MonitoringSupport#getExToCount(java.util.List)
   * @see MonitoringSupport#getExToCountMaleOrFemale(java.util.List, boolean)
   * @param isMale
   * @param outsideFile
   * @param rawExercises
   * @return
   */
  public Map<String, Integer> getExerciseIDToOutsideCount(boolean isMale, String outsideFile, List<Exercise> rawExercises){
    Map<String, Exercise> phraseToExercise = new HashMap<String, Exercise>();
    Map<String,Integer> idToCount = new HashMap<String, Integer>();

    //int c= 0;
   // logger.info("plan file " +/*lessonPlanFile + */" outside " + outsideFile);
    for (Exercise e : rawExercises) {
      idToCount.put(e.getID(), 0);
      String trim = e.getTooltip().trim();
      phraseToExercise.put(trim, e);
      // if (c++ < 20) logger.warn("phrase '" +trim+ "'");
    }

    Map<Boolean, Map<String, Integer>> counts = getCounts(outsideFile);
    Map<String, Integer> phraseToCount = counts != null ? counts.get(isMale) : new HashMap<String,Integer>();

    int count = 0;
    for (Map.Entry<String,Integer> pair : phraseToCount.entrySet()) {
      String phrase = pair.getKey();
      Exercise exercise = phraseToExercise.get(phrase);
      if (exercise == null) {
        if (count++ < 20) logger.warn("huh? couldn't find phrase '" + phrase +  "' in map of size " +phraseToExercise.size());
      }
      else {
        Integer current = idToCount.get(exercise.getID());
        if (current != null) {
          if (current > 0) {
            logger.warn("We've already seen " + exercise.getID() + " for " + phrase);
            idToCount.put(exercise.getID(), current + pair.getValue());
          }
          else {
            idToCount.put(exercise.getID(), pair.getValue());
          }
        }
        else {
          logger.error("huh? we didn't know about ex " + exercise.getID());
          idToCount.put(exercise.getID(), pair.getValue());
        }
      }
    }
    if (count > 0) logger.warn("there were " +count + " missing phrases ");
    return idToCount;
  }

  /**
   * Parse the external count file from Wade.
   * This should only be a one off if people are consistently using the website.
   *
   * @see DatabaseImpl#getExercisesBiasTowardsUnanswered(boolean, long, String)
   * @param overrideCountsFile
   * @return map of gender to phrase to count of answers for that phrase
   */
  private Map<Boolean,Map<String,Integer>> getCounts(String overrideCountsFile) {
    if (overrideCountsFile == null) return null;
    try {
      File file = new File(overrideCountsFile);
      if (!file.exists()) {
        logger.debug("can't find '" + file +"'");
        return null;
      }
      else {
        // logger.debug("found file at " + file.getAbsolutePath());
      }
      FileInputStream resourceAsStream = new FileInputStream(file);
      BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream,FileExerciseDAO.ENCODING));
      String line2;
      int count = 0;
      Map<Boolean,Map<String,Integer>> genderToPhraseToCount = new HashMap<Boolean, Map<String, Integer>>();
      genderToPhraseToCount.put(true, new HashMap<String, Integer>());
      genderToPhraseToCount.put(false, new HashMap<String, Integer>());
      line2 = reader.readLine();
      int totalMales =0;
      int dups = 0;

      //Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file.getName() + "_dups.txt"), "UTF-8"));
      //Writer w2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file.getName() + "_all.txt"), "UTF-8"));

      int totalFemales =0;
      Map<String, Integer> males = genderToPhraseToCount.get(true);
      Map<String, Integer> females = genderToPhraseToCount.get(false);
      while ((line2 = reader.readLine()) != null) {
        count++;
        String[] split = line2.split("\\s+");
        if (split.length < 3) {
          logger.warn(" Couldn't parse " + line2);
          continue;
        }
        String male = split[1].trim();
        String female = split[2].trim();
        String phrase = line2.split("--")[1].trim();
        //if (count < 20) logger.info("Got "+line2 + " male '" + male + "' female '" + female + "' phrase '" + phrase + "'");
        try {
          int maleCount = (int) Double.parseDouble(male);
          totalMales += maleCount;
          //   w2.write(phrase+"\n");

          if (males.containsKey(phrase)) {
            //    logger.warn("line # " + count+" : male huh? we've seen phrase " + phrase + " before?");
            //   w.write(phrase+"\n");
            males.put(phrase, males.get(phrase) + maleCount);
            dups++;
          } else {
            males.put(phrase, maleCount);
          }
        } catch (NumberFormatException e) {
          if (count < 20) logger.warn("male Couldn't parse " + line2 + " got " + e);
        }

        try {
          int femaleCount = (int) Double.parseDouble(female);
          totalFemales += femaleCount;

          if (females.containsKey(phrase)) {
            //logger.warn("female huh? we've seen phrase " + phrase + " before?");
            females.put(phrase, females.get(phrase) + femaleCount);
          } else {
            females.put(phrase, femaleCount);
          }
        } catch (NumberFormatException e) {
          if (count < 20) logger.warn("female Couldn't parse " + line2 + " got " +e);
        }
      }
      //logger.info("m " + totalMales + " f " + totalFemales + " dups " + dups);
      reader.close();
      //w.close();
      // w2.close();

/*      logger.info("map is " + genderToPhraseToCount.keySet() + " size males " + males.size() +
          " females " + females.size());*/
      return  genderToPhraseToCount;
    } catch (Exception e) {
      logger.error("got " +e,e);
    }
    return null;
  }
}
