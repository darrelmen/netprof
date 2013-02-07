package mitll.langtest.server.scoring;

import corpus.package$;
import mitll.langtest.server.ProcessRunner;
import mitll.langtest.server.database.FileExerciseDAO;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates the LM for a small vocab decoder from foreground and background sentences.
 *
 * User: GO22670
 * Date: 2/7/13
 * Time: 12:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class SmallVocabDecoder {
  private static Logger logger = Logger.getLogger(ASRScoring.class);

  private static final String FINAL_BLEND_VOCAB = "finalBlend.vocab";
  /**
   * How much weight to give the foreground vs the background lm.
   */
  private static final float BLEND_FOREGROUND_BACKGROUND = 0.8f;

  private static final String SMALL_LM_SLF = "smallLM.slf"; // just for testing on windows
  /**
   * Limit on vocabulary size -- too big and dcodr will run out of memory and segfault
   */
  private static final int MAX_AUTO_CRT_VOCAB = 200;
  /**
   * Platform -- windows, mac, linux, etc.
   */
  private final String platform = Utils.package$.MODULE$.platform();

  /**
   * Get the foreground and background sentences. <br></br>
   * Create an srilm file using ngram-count <br></br>
   * Create an slf file using HBuild <br></br>
   * Do the octal conversion to utf-8 text on the result <br></br>
   *
   * This only works properly on the mac and linux, sorta emulated on win32
   *
   * @see ASRScoring#getScoreForAudio(String, String, String, String, java.util.List, java.util.List)
   * @param lmSentences foreground sentences
   * @param background background sentences
   * @param tmpDir where everything is written
   * @param modelsDir just for testing on windows
   * @param scoringDir hydec location
   * @return SLF file that is created, might not exist if any of the steps fail (e.g. if bin exes are not marked executable)
   */
  public String createSLFFile(List<String> lmSentences, List<String> background, String tmpDir,
                              String modelsDir, String scoringDir) {
    String convertedFile = tmpDir + File.separator + SMALL_LM_SLF;
    List<String> vocab = getVocab(background);
    if (platform.startsWith("win")) {
      // hack -- get slf file from model dir
      String slfDefaultFile = modelsDir + File.separator + SMALL_LM_SLF;
      doOctalConversion(slfDefaultFile, convertedFile);
    }
    else {
      String platform = Utils.package$.MODULE$.platform();
      String pathToBinDir = scoringDir + File.separator + "bin." + platform;
      //logger.info("platform  "+platform + " bins " + pathToBinDir);
      File foregroundLMSentenceFile = writeLMToFile(lmSentences, tmpDir);
      File foreGroundSRILMFile = runNgramCount(tmpDir, "smallLMOut.srilm", foregroundLMSentenceFile, null, pathToBinDir, true);

      File backgroundLMSentenceFile = writeLMToFile(background, tmpDir);
      String vocabFile = tmpDir + File.separator + "largeVocab.txt";
      writeVocab(vocabFile,vocab);
      File backgroundSRILMFile = runNgramCount(tmpDir, "backgroundLMOut.srilm", backgroundLMSentenceFile, vocabFile, pathToBinDir, false);

      File combinedSRILM =runNgram(tmpDir,"combined.srilm",foreGroundSRILMFile,backgroundSRILMFile,pathToBinDir);

      //String slfFile = runHBuild(tmpDir,foreGroundSRILMFile,pathToBinDir); // only use foreground model
      String slfFile = runHBuild(tmpDir,combinedSRILM,pathToBinDir);

      doOctalConversion(slfFile, convertedFile);
    }
    if (!new File(convertedFile).exists()) logger.error("Couldn't create " +convertedFile);
    return convertedFile;
  }

  /**
   * Get the vocabulary to use when generating a language model. <br></br>
   * Very important to limit the vocabulary (less than 300 words) or else the small vocab dcodr will run out of
   * memory and segfault! <br></br>
   * Remember to add special tokens like silence, pause, and unk
   * @see #MAX_AUTO_CRT_VOCAB
   * @param background sentences
   * @return most frequent vocabulary words
   */
  private List<String> getVocab(List<String> background) {
    List<String> all = new ArrayList<String>();
    all.addAll(Arrays.asList("-pau-", "</s>", "<s>", "<unk>"));

    final Map<String,Integer> sc = new HashMap<String, Integer>();
    for (String l : background) {
      for (String t : l.split("\\s")) {
        String tt = t.replaceAll("\\p{P}","");
        if (tt.trim().length() > 0) {
          Integer c = sc.get(t);
          if (c == null) sc.put(t,1);
          else sc.put(t,c+1);
        }
      }
    }
    List<String> vocab = new ArrayList<String>();
    try {
/*      System.out.println("map : " +sc);

      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("map"), FileExerciseDAO.ENCODING));
      for (Map.Entry<String, Integer> kv : sc.entrySet()) writer.write(kv.getKey() +"\t"+kv.getValue()+"\n");
      writer.close();*/

      vocab = new ArrayList<String>(sc.keySet());
      Collections.sort(vocab, new Comparator<String>() {
        public int compare(String s, String s2) {
          Integer first = sc.get(s);
          Integer second = sc.get(s2);
          return first < second ? +1 : first > second ? -1 : 0;
        }
      });

 /*     writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("vocab"), FileExerciseDAO.ENCODING));
      for (String v : vocab) writer.write(v+"\n");
      writer.close();*/
    } catch (Exception e) {
      e.printStackTrace();
    }

    //System.out.println("map : " +sc);

    all.addAll(vocab.subList(0,Math.min(vocab.size(), MAX_AUTO_CRT_VOCAB)));
    //  System.out.println("vocab " + new HashSet<String>(all));
    return all;
  }

  private void doOctalConversion(String slfFile, String convertedFile) {
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(slfFile), FileExerciseDAO.ENCODING));
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(convertedFile), FileExerciseDAO.ENCODING));
      String line2;
      while ((line2 = reader.readLine()) != null) {
        writer.write(package$.MODULE$.oct2string(line2).trim());
        writer.write("\n");
      }
      reader.close();
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void writeVocab(String vocabFile, List<String> vocab) {
    try {
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(vocabFile), FileExerciseDAO.ENCODING));

      for (String v : vocab) {
        writer.write(v +"\n");
        //   writer.write("\n");
      }
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * ngram-count -text smallLM.txt -lm smallLmOut.srilm -write-vocab out.vocab -order 2 -cdiscount 0.0001 –unk
   * HBuild -n smallLmOut.srilm -s '<s>' '</s>' out.vocab smallLM.slf
   *
   * @param lmSentences
   * @param tmpDir
   * @return
   */

  private File writeLMToFile(List<String> lmSentences, String tmpDir) {
    try {
      File outFile = new File(tmpDir, "smallLM_" +lmSentences.size()+ ".txt");
      logger.info("wrote lm to " +outFile.getAbsolutePath());
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), FileExerciseDAO.ENCODING));
      for (String s : lmSentences) writer.write(s.trim().replaceAll("\\p{P}","") + "\n");
      writer.close();
      return outFile;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * ngram-count -text smallLM.txt -lm smallLmOut.srilm -write-vocab out.vocab -order 2 -cdiscount 0.0001 –unk
   * ngram-count -text $1 -lm $2 -order 2 -kndiscount -interpolate -unk
   * @param tmpDir
   * @param lmFile
   * @param pathToBinDir
   * @return
   */
  private File runNgramCount(String tmpDir, String srllmOut, File lmFile, String vocabFile, String pathToBinDir, boolean isSmall) {
    String srilm = tmpDir + File.separator + srllmOut;
    ProcessBuilder soxFirst = isSmall ? new ProcessBuilder(pathToBinDir +File.separator+"ngram-count",
        "-text",
        lmFile.getAbsolutePath(),
        "-lm",
        srilm,
        "-write-vocab",
        tmpDir + File.separator + "small_out.vocab",
        "-order",
        "2",
        "-cdiscount",
        "0.0001",
        "-unk"
    ) : new ProcessBuilder(pathToBinDir +File.separator+"ngram-count",
        "-text",
        lmFile.getAbsolutePath(),
        "-lm",
        srilm,
        // "-gt1min", "3",
        // "-gt2min", "3",
        "-vocab",
        vocabFile,
        "-write-vocab",
        tmpDir + File.separator + "large_out.vocab",
        "-order",
        "2",
        "-kndiscount",
        "-interpolate",
        "-unk"
    );

    logger.info("ran " +pathToBinDir +File.separator+"ngram-count"+" "+
        "-text"+" "+
        lmFile.getAbsolutePath()+" "+
        "-lm"+" "+
        srilm+" "+
        //  "-gt1min"+" "+ "3"+" "+
        //   "-gt2min"+" "+ "3"+" "+
        "-write-vocab"+" "+
        tmpDir + File.separator + "large_out.vocab"+" "+
        "-order"+" "+
        "2"+" "+
        "-kndiscount"+" "+
        "-interpolate"+" "+
        "-unk");

    try {
      new ProcessRunner().runProcess(soxFirst);
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (!new File(srilm).exists()) {
      logger.info("didn't make " + srilm + " so trying again with cdiscount = 0.0001");
      if (!isSmall) {
        ProcessBuilder soxFirst2 = new ProcessBuilder(pathToBinDir +File.separator+"ngram-count",
            "-text",
            lmFile.getAbsolutePath(),
            "-lm",
            srilm,
            //     "-gt1min", "3",
            //    "-gt2min", "3",
            "-write-vocab",
            tmpDir + File.separator + "large_out.vocab",
            "-order",
            "2",
            "-cdiscount",
            "0.0001",
            "-unk");

        try {
          new ProcessRunner().runProcess(soxFirst2);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }


    if (!new File(srilm).exists()) {
      logger.error("didn't make " + srilm);
    }
    return new File(srilm);
  }

  /**
   * By default the weight is 0.8 for the foreground model.
   *
   * @see #createSLFFile
   * $BIN/ngram -lm $1 -lambda $3 -mix-lm $2 -order 2 -unk -write-lm $4.srilm -write-vocab $4.vocab
   * @param tmpDir
   * @param foregroundLM
   * @param pathToBinDir
   * @return
   */
  private File runNgram(String tmpDir, String srilmOut, File foregroundLM, File backgroundLM, String pathToBinDir) {
    String srilm = tmpDir + File.separator + srilmOut;
    float weightForForeground = BLEND_FOREGROUND_BACKGROUND;
    ProcessBuilder ngram = new ProcessBuilder(pathToBinDir +File.separator+"ngram",
        "-lm",
        foregroundLM.getAbsolutePath(),
        "-lambda",
        ""+weightForForeground,
        "-mix-lm",
        backgroundLM.getAbsolutePath(),
        "-order",
        "2",
        "-unk",
        "-write-lm",
        srilm,
        "-write-vocab",
        tmpDir + File.separator + FINAL_BLEND_VOCAB
    );

    try {
      new ProcessRunner().runProcess(ngram);
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (!new File(srilm).exists()) logger.error("didn't make " + srilm);
    return new File(srilm);
  }


  /**
   * HBuild -n smallLmOut.srilm -s '<s>' '</s>' out.vocab smallLM.slf
   * @param tmpDir
   * @param srilmFile
   * @param pathToBinDir
   */
  private String runHBuild(String tmpDir, File srilmFile, String pathToBinDir) {
    logger.info("running hbuild on " + srilmFile);
    String slfOut = tmpDir + File.separator + "smallOctalLMFromHBuild.slf";
    ProcessBuilder soxFirst = new ProcessBuilder(pathToBinDir +File.separator+"HBuild",
        "-n",
        srilmFile.getAbsolutePath(),
        "-s",
        "<s>",
        "</s>",
        tmpDir + File.separator + FINAL_BLEND_VOCAB,
        slfOut
    );

    try {
      new ProcessRunner().runProcess(soxFirst);
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (!new File(slfOut).exists()) logger.error("runHBuild didn't make " + slfOut);
    return slfOut;
  }

}
