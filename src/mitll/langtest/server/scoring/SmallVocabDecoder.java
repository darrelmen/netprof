package mitll.langtest.server.scoring;

import com.google.common.io.Files;
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
  private static final Logger logger = Logger.getLogger(ASRScoring.class);

  public static final int VOCAB_SIZE_LIMIT = 200;
  private static final String FINAL_BLEND_VOCAB = "finalBlend.vocab";
  private static final String SMALL_LMOUT_SRILM = "smallLMOut.srilm";
  private static final String LARGE_VOCAB_TXT = "largeVocab.txt";
  private static final String BACKGROUND_LMOUT_SRILM = "backgroundLMOut.srilm";
  private static final String COMBINED_SRILM = "combined.srilm";

  /**
   * How much weight to give the foreground vs the background lm.
   */
 // private static final float BLEND_FOREGROUND_BACKGROUND = 0.8f;

  private static final String SMALL_LM_SLF = ASRScoring.SMALL_LM_SLF;
  /**
   * Limit on vocabulary size -- too big and dcodr will run out of memory and segfault
   */
  //private static final int MAX_AUTO_CRT_VOCAB = 200;
  private static final String MAX_ORDER = "2";  // NOTE 3 does NOT work
  public static final String UNKNOWN_MODEL = "UNKNOWNMODEL";
  private static final String UNKNOWN_MODEL_BIAS = "-1.20";

  /**
   * Platform -- windows, mac, linux, etc.
   */
  private final String platform = Utils.package$.MODULE$.platform();
  private double foregroundBackgroundBlend;

  public String createSimpleSLFFile(List<String> lmSentences, String tmpDir) {
    String slfFile = tmpDir + File.separator + SMALL_LM_SLF;

    try {
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(slfFile), FileExerciseDAO.ENCODING));
      writer.write("VERSION=1.0\n");

      int linkCount = 0;
      StringBuilder nodesBuf = new StringBuilder();
      nodesBuf.append("I=0 W=<s>\n");
      nodesBuf.append("I=1 W=</s>\n");
      int newNodes = 2;
      StringBuilder linksBuf = new StringBuilder();
      List<String> sentencesToUse = new ArrayList<String>(lmSentences);
      sentencesToUse.add(UNKNOWN_MODEL);
      for (String sentence : sentencesToUse) {
        List<String> tokens = getTokens(sentence);
        int start = 0;

        for (String token : tokens) {
          int next = newNodes++;
          linksBuf.append("J=" + (linkCount++) + " S=" + start + " E=" + next +
            " l=" +
            (token.equals(UNKNOWN_MODEL) ? UNKNOWN_MODEL_BIAS : "-1.00") +
            "\n");
          nodesBuf.append("I=" +
            next +
            " W=" +
            token +
            "\n");

          start = next;
        }
        linksBuf.append("J=" + (linkCount++) + " S=" + start + " E=1" + " l=-1.00\n");
      }
      writer.write("N=" + newNodes + " L=" + linkCount + "\n");

      writer.write(nodesBuf.toString());
      writer.write(linksBuf.toString());

      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    //logger.debug("wrote " + slfFile + " exists " + new File(slfFile).exists());
    return slfFile;
  }

  /**
   * Get the foreground and background sentences. <br></br>
   * Create an srilm file using ngram-count <br></br>
   * Create an slf file using HBuild <br></br>
   * Do the octal conversion to utf-8 text on the result <br></br>
   *
   * This only works properly on the mac and linux, sorta emulated on win32
   *
   * @see ASRScoring#calcScoreForAudio
   * @param lmSentences foreground sentences
   * @param background background sentences
   * @param tmpDir where everything is written
   * @param modelsDir just for testing on windows
   * @param scoringDir hydec location
   * @return SLF file that is created, might not exist if any of the steps fail (e.g. if bin exes are not marked executable)
   */
  public String createSLFFile(List<String> lmSentences, List<String> background, List<String> vocab, String tmpDir, String modelsDir,
                              String scoringDir) {
    String convertedFile = tmpDir + File.separator + SMALL_LM_SLF;
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
      File foreGroundSRILMFile = runNgramCount(tmpDir, SMALL_LMOUT_SRILM, foregroundLMSentenceFile, null, pathToBinDir, true);

      File backgroundLMSentenceFile = writeLMToFile(background, tmpDir);
      String vocabFile = tmpDir + File.separator + LARGE_VOCAB_TXT;
      writeVocab(vocabFile,vocab);
      File backgroundSRILMFile = runNgramCount(tmpDir, BACKGROUND_LMOUT_SRILM, backgroundLMSentenceFile, vocabFile, pathToBinDir, false);

      File combinedSRILM =runNgram(tmpDir, COMBINED_SRILM,foreGroundSRILMFile,backgroundSRILMFile,pathToBinDir);

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
   * @see ASRScoring#getUsedTokens(java.util.List, java.util.List)
   * @param background sentences
   * @return most frequent vocabulary words
   */
  public List<String> getVocab(List<String> background, int vocabSizeLimit) {
    List<String> all = new ArrayList<String>();
/*    all.add("-pau-");  // include?
    all.add("<s>");
    all.add("</s>");*/
    /*, "<unk>"*/
    List<String> simpleVocab = getSimpleVocab(background, vocabSizeLimit);
    all.addAll(simpleVocab);

    if (false) {
      String sentence = background.iterator().next();
      logger.debug("num sentences " + background.size() +
        " first is " + sentence + " length " + sentence.length() + " got " + simpleVocab.size() + " tokens : '" + simpleVocab +
        "'");
    }

    return all;
  }

  /**
   * @see ASRScoring#getUniqueTokensInLM(java.util.List, java.util.List)
   * @param sentences
   * @param vocabSizeLimit
   * @return
   */
  public List<String> getSimpleVocab(List<String> sentences, int vocabSizeLimit) {
    // count the tokens
    final Map<String, Integer> sc = new HashMap<String, Integer>();
    for (String sentence : sentences) {
      for (String token : getTokens(sentence)) {
        Integer c = sc.get(token);
        sc.put(token, (c == null) ? 1 : c + 1);
      }
    }

    // sort by frequency
    List<String> vocab = new ArrayList<String>(sc.keySet());
    Collections.sort(vocab, new Comparator<String>() {
      public int compare(String s, String s2) {
        Integer first = sc.get(s);
        Integer second = sc.get(s2);
        return first < second ? +1 : first > second ? -1 : 0;
      }
    });

    // take top n most frequent
    List<String> all = new ArrayList<String>(); // copy list b/c sublist not serializable ???
    all.addAll(vocab.subList(0, Math.min(vocab.size(), vocabSizeLimit)));
    return all;
  }

  public List<String> getTokens(String sentence) {
    List<String> all = new ArrayList<String>();

    for (String untrimedToken : sentence.split("\\p{Z}+")) { // split on spaces
      String tt = untrimedToken.replaceAll("\\p{P}", ""); // remove all punct
      String token = tt.trim();  // necessary?
      if (token.length() > 0) {
        all.add(token);
      }
    }

    return all;
  }

  /**
   * So for some reason the slf file generated from HBuild is in octal, so we have to convert it to UTF-8.
   *
   * @see #createSLFFile
   * @param slfFile that is in octal
   * @param convertedFile that will be in UTF-8
   */
  private void doOctalConversion(String slfFile, String convertedFile) {
    try {
      if (!new File(slfFile).exists()) {
        if (platform.equals("win32")) {
          logger.debug("slf file " + slfFile + " doesn't exist, skipping octal conversion.");
        } else {
          logger.error("slf file " + slfFile + " doesn't exist, skipping octal conversion.");
        }
      } else {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(slfFile), FileExerciseDAO.ENCODING));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(convertedFile), FileExerciseDAO.ENCODING));
        String line2;
        while ((line2 = reader.readLine()) != null) {
          writer.write(package$.MODULE$.oct2string(line2).trim());
          writer.write("\n");
        }
        reader.close();
        writer.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void writeVocab(String vocabFile, List<String> vocab) {
    try {
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(vocabFile), FileExerciseDAO.ENCODING));

      for (String v : vocab) {
        writer.write(v +"\n");
      }
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Write LM sentences out, without punctuation.
   *
   * @see #createSLFFile
   * @param lmSentences
   * @param tmpDir
   * @return smallLM file
   */
  private File writeLMToFile(List<String> lmSentences, String tmpDir) {
    try {
      File outFile = new File(tmpDir, "smallLM_" +lmSentences.size()+ ".txt");
      logger.info("wrote lm with " + lmSentences.size() + " sentences to " +outFile.getAbsolutePath());
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), FileExerciseDAO.ENCODING));
      for (String s : lmSentences) {
        String punctRemoved = s.trim().replaceAll("\\p{P}", "");
        writer.write(punctRemoved + "\n");
      }
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
      MAX_ORDER,
        "-cdiscount",
        "0.0001"/*,
        "-unk"*/
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
      MAX_ORDER,
        "-kndiscount",
        "-interpolate"/*,
        "-unk"*/
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
      MAX_ORDER +" "+
        "-kndiscount"+" "+
        "-interpolate"/*+" "+
        "-unk"*/);

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
          MAX_ORDER,
            "-cdiscount",
            "0.0001"/*,
            "-unk"*/);

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
    ProcessBuilder ngram = new ProcessBuilder(pathToBinDir +File.separator+"ngram",
        "-lm",
        foregroundLM.getAbsolutePath(),
        "-lambda",
        ""+foregroundBackgroundBlend,
        "-mix-lm",
        backgroundLM.getAbsolutePath(),
        "-order",
      MAX_ORDER,
       /* "-unk",*/
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
   * @param srilmFile                                                                       -
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

/*
  public static void main(String []arg) {
    String tmpDir = Files.createTempDir().getAbsolutePath();
    String simpleSLFFile = new SmallVocabDecoder().createSimpleSLFFile(Arrays.asList("fred", "barney","fred barney","ace barry charley"), tmpDir);
    //String simpleSLFFile = new SmallVocabDecoder().createSimpleSLFFile(Arrays.asList("fred"), tmpDir);
   // String simpleSLFFile = new SmallVocabDecoder().createSimpleSLFFile(Arrays.asList("fred","barney"), tmpDir);
    System.out.println("wrote to " +simpleSLFFile);
  }
*/
}
