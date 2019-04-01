/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.scoring;

import com.google.gson.*;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.audio.HTTPClient;
import mitll.langtest.server.database.project.IProject;
import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

import static mitll.langtest.server.scoring.HydraOutput.STATUS_CODES.*;

public class KaldiSupport implements IKaldiSupport {
  private static final Logger logger = LogManager.getLogger(KaldiSupport.class);
  private static final String WAV1 = ".wav";
  private static final String WAV = WAV1;
  private static final String SCORE = "score";
  private static final String STATUS = "status";
  private static final String LOG = "log";
  private static final String HTTPS = "https"; //""https";

  /**
   * @see #callKaldiOOV
   */
  private static final String OOV = "oov";
  private static final String NORM = "norm";
  private static final String NORM_TRANSCRIPT = "norm_transcript";
  private static final String TRANSCRIPT = "transcript";
  private static final String IN_VOCAB = "in_vocab";

  private static final String WAVEFORM = "waveform";

  private static final String INVALID_CHAR_INDS = "invalid_char_inds";
  /**
   * left french quote -- \\u00AB
   * right quote - 00BB
   * ellipsis - 2026
   */
  private static final String CLEAN_BEFORE_KALDI = "[()/\\u00AB\\u00BB\\u2026]";

  private final IPronunciationLookup pronunciationLookup;
  private final IProject project;
  private final ServerProperties props;

  KaldiSupport(IPronunciationLookup pronunciationLookup,
               IProject project,
               ServerProperties props) {
    this.pronunciationLookup = pronunciationLookup;
    this.project = project;
    this.props = props;

  }

  /**
   * http://hydra-dev.llan.ll.mit.edu:5000/score/%7B%22reqid%22:1234,%22request%22:%22decode%22,%22phrase%22:%22%D8%B9%D8%B1%D8%A8%D9%8A%D9%91%22,%22file%22:%22/opt/netprof/bestAudio/msa/bestAudio/2549/regular_1431731290207_by_511_16K.wav%22%7D
   *
   * @param audioPath
   * @param rawSentence
   * @return
   * @see #scoreRepeatExercise
   */
  @Override
  public HydraOutput runKaldi(String audioPath, String rawSentence) {
    try {
      long then = System.currentTimeMillis();
      //logger.info("runKaldi replace " + rawSentence + " with " + rawSentence);
      String json = callKaldi(rawSentence, audioPath);

      if (json == null) {
        return getHydraOutputForError("runKaldi got no json from kaldi?");
      }

      long now = System.currentTimeMillis();
      long processDur = now - then;

//      logger.info("runKaldi took " + processDur + " for " + sentence + " on " + audioPath);

      try {
        JsonObject parse = new JsonParser().parse(json).getAsJsonObject();

        HydraOutput.STATUS_CODES status = getStatus(parse);
        String log = parse.has(LOG) ? parse.get(LOG).getAsString() : "";
        float score = -1F;

        if (status == SUCCESS) {
          score = parse.get(SCORE).getAsFloat();
          logger.info("runKaldi " +
              "\n\ttook      " + processDur +
              "\n\tdecoding '" + rawSentence + "'" +
              "\n\tfile      " + audioPath +
              "\n\tscore " + score);
        } else {
          logger.warn("runKaldi failed " +
              "\n\tstatus " + status +
              "\n\tlog    " + log.trim()
          );
          parse = new JsonObject();
        }

        return new HydraOutput(
            new Scores(score, new HashMap<>(), (int) processDur)
                .setKaldiJsonObject(parse),
            getWordAndProns(rawSentence))
            .setStatus(status)
            .setLog(log);
      } catch (JsonSyntaxException e) {
        logger.error("got unparseable " +
                "\n\tjson " + json +
                "\n\tmesssage " + e.getMessage(),
            e
        );

        return new HydraOutput(
            new Scores(-1F, new HashMap<>(), 0)
                .setKaldiJsonObject(new JsonObject()),
            null)
            .setStatus(ERROR)
            .setLog(e.getMessage());
      }

    } catch (Exception e) {
      logger.error("Got " + e, e);
      return getHydraOutputForError(e.getMessage());
    }
  }

  @NotNull
  private List<WordAndProns> getWordAndProns(String sentence) {
    List<WordAndProns> possibleProns = new ArrayList<>();
    getHydraDict(sentence, "", possibleProns);
    return possibleProns;
  }

  private void getHydraDict(String cleaned, String transliteration, List<WordAndProns> possibleProns) {
    pronunciationLookup.createHydraDict(cleaned, transliteration, possibleProns);
  }

  /**
   * @param fl
   * @return
   * @see AudioFileHelper#isValidForeignPhrase(Set, Set, CommonExercise, Set, boolean)
   */
  @Override
  public Collection<String> getKaldiOOV(String fl) {
    String fl1 = fl.replaceAll(CLEAN_BEFORE_KALDI, " ");
    if (!fl1.equals(fl)) {
      logger.info("getKaldiOOV : replaced " + fl + " with " + fl1);
    }

    return runOOV(runNorm(fl1));
  }

  /**
   * @param tokens
   * @return
   * @see #getKaldiOOV
   */
  public List<String> runOOV(final List<String> tokens) {
    List<Boolean> oov = new ArrayList<>();
    try {
      String json = callKaldiOOV(tokens);
      JsonObject parse = new JsonParser().parse(json).getAsJsonObject();
      HydraOutput.STATUS_CODES status = getStatus(parse);

      if (status == SUCCESS) {
        parse
            .getAsJsonArray(IN_VOCAB)
            .forEach(jsonElement -> oov.add(jsonElement.getAsBoolean()));
      } else if (status == OOV_IN_TRANS) {
        parse
            .getAsJsonArray(IN_VOCAB)
            .forEach(jsonElement -> oov.add(jsonElement.getAsBoolean()));
      } else {
        String log = parse.has(LOG) ? parse.get(LOG).getAsString() : "";
        logger.warn("runOOV failed " +
            "\n\tstatus " + status +
            "\n\ttokens " + tokens +
            "\n\tjson   " + json +
            "\n\tlog    " + log.trim()
        );
      }

    } catch (Exception e) {
      logger.error("Got " + e, e);
    }

    if (oov.isEmpty()) {
      return tokens;
    } else {
      List<String> oovTokens = new ArrayList<>(tokens.size());
      for (int i = 0; i < tokens.size(); i++) {
        if (!oov.get(i)) {
          oovTokens.add(tokens.get(i));
        }
      }
      if (!oovTokens.isEmpty()) {
        logger.warn("runOOV : from " + tokens + " got oov " + oovTokens);
      }
      return oovTokens;
    }
  }

  private List<String> runNorm(String sentence) {
    List<String> tokens = new ArrayList<>();
    try {
      String json = callKaldiNorm(sentence);
      JsonObject parse = new JsonParser().parse(json).getAsJsonObject();
      HydraOutput.STATUS_CODES status = getStatus(parse);

      if (status == SUCCESS) {
        //JsonArray asJsonArray = parse.getAsJsonArray(NORM_TRANSCRIPT);
        parse.getAsJsonArray(NORM_TRANSCRIPT).forEach(jsonElement -> tokens.add(jsonElement.getAsString()));
        if (tokens.isEmpty()) {
          logger.info("runNorm : kaldi tokens were " + tokens + " for " + sentence);
        }
      } else if (status == TEXT_NORM_FAILED) {
        // JsonArray asJsonArray = parse.getAsJsonArray(INVALID_CHAR_INDS);
        // List<IPair> pairs = getInvalidCharRanges(parse);
        logger.info("runNorm " + sentence);
        getInvalidCharRanges(parse)
            .forEach(pair -> {
              String substring = sentence.substring(pair.getFrom(), pair.getTo());
              tokens.add(substring);
              logger.info("\toov range " + pair + " = " + substring);
            });
      } else {
        String log = parse.has(LOG) ? parse.get(LOG).getAsString() : "";
        logger.warn("runNorm failed " +
            "\n\tstatus   " + status +
            "\n\tsentence " + sentence +
            "\n\tjson     " + json +
            "\n\tlog      " + log.trim()
        );
      }
    } catch (
        Exception e) {
      logger.error("Running norm on " +
          "\n\tsentence " + sentence +
          "\n\tGot " + e, e);
    }
    return tokens;
  }

  @NotNull
  private List<IPair> getInvalidCharRanges(JsonObject parse) {
    List<IPair> pairs = new ArrayList<>();
    parse.getAsJsonArray(INVALID_CHAR_INDS).forEach(jsonElement -> {
      pairs.add(new IPair(jsonElement.getAsJsonArray().get(0).getAsInt(),
          jsonElement.getAsJsonArray().get(1).getAsInt()));
      //  jsonElement.getAsJsonArray().forEach(jsonElement1 -> jsonElement1.getAsInt());
    });
    return pairs;
  }

  private static class IPair {
    private int from;
    private int to;

    IPair(int from, int to) {
      this.from = from;
      this.to = to;
    }

    int getFrom() {
      return from;
    }

    int getTo() {
      return to;
    }

    public String toString() {
      return "[" + from + " - " + to + "]";
    }

//    public void setFrom(int from) {
//      this.from = from;
//    }
//
//    public void setTo(int to) {
//      this.to = to;
//    }
  }


  /**
   * @param sentence
   * @param audioPath
   * @return
   * @throws IOException
   * @see #runKaldi
   */
  private String callKaldi(String sentence, String audioPath) throws IOException {
    return doKaldiGet(sentence, getKaldiRequest(sentence, audioPath), SCORE);
  }

  private String callKaldiNorm(String sentence) throws IOException {
    // String s1 = "{\"reqid\":1234,\"request\":\"decode\",\"phrase\":\"عربيّ\",\"file\":\"/opt/netprof/bestAudio/msa/bestAudio/2549/regular_1431731290207_by_511_16K.wav\"}";
    return doKaldiGet(sentence, getKalidNormRequest(sentence), NORM);
  }

  /**
   * @param tokens
   * @return
   * @throws IOException
   * @see #runOOV(List)
   */
  private String callKaldiOOV(List<String> tokens) throws IOException {
    // String s1 = "{\"reqid\":1234,\"request\":\"decode\",\"phrase\":\"عربيّ\",\"file\":\"/opt/netprof/bestAudio/msa/bestAudio/2549/regular_1431731290207_by_511_16K.wav\"}";
    return doKaldiGet(tokens.toString(), getKalidOOVRequest(tokens), OOV);
  }

  private String doKaldiGet(String sentence, String jsonRequest, String operation) throws IOException {
    String prefix = getPrefix(operation);
    // String encode = URLEncoder.encode(jsonRequest, StandardCharsets.UTF_8.name());
    String url = prefix;// + encode;

    if (false) {
      logger.info("runKaldi " + operation +
          "\n\tcontent  " + sentence +
          //"\n\treq       " + encode +
          //  "\n\traw       " + (prefix + jsonRequest) +
          "\n\tpost      " + url);
    }

    return new HTTPClient(url).sendAndReceiveAndClose(jsonRequest);
  }

  /**
   * TODO : longer term might want different versions of service on same language
   *
   * Calls look like:
   *
   * http://172.25.252.196/en/oov/{“transcript”:”what”}
   * https://172.25.252.196/en/score/{“file”:”/opt/...blah.wav”, “transcript”:”what”}
   * http://172.25.252.196/en/score/{“waveform”:”/opt/blah.wav”, “transcript”:”what”}
   * http://172.25.252.196/en/norm/{“transcript”:”WhAt”}
   *
   * @param operation
   * @return
   */
  @NotNull
  private String getPrefix(String operation) {
    String localhost = props.useProxy() ? "hydra-dev" : props.getKaldiHost();
    //  logger.info("getPrefix using host " +localhost + " for " +operation);
    String languageCode = project.getLanguageEnum().getLocale();
    return HTTPS + "://" + localhost + "/" + languageCode + "/" + operation + "/";
  }


  /**
   * @param sentence
   * @param audioPath
   * @return
   * @see #callKaldi
   */
  private String getKaldiRequest(String sentence, String audioPath) {
    JsonObject jsonObject = new JsonObject();

    //    logger.info("KALDI " +
//        "\n\tsentence  " + sentence +
//        "\n\taudioPath " + audioPath
//    );
    jsonObject.addProperty(WAVEFORM, audioPath);
    jsonObject.addProperty(TRANSCRIPT, sentence.trim());

    return jsonObject.toString();
  }

  /**
   * norm/{"transcript":"what is your name"}
   *
   * @param sentence
   * @return
   */
  private String getKalidNormRequest(String sentence) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(TRANSCRIPT, sentence);
    return jsonObject.toString();
  }

  private String getKalidOOVRequest(List<String> tokens) {
    JsonObject jsonObject = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    tokens.forEach(jsonArray::add);
    jsonObject.add(NORM_TRANSCRIPT, jsonArray);
    return jsonObject.toString();
  }

  private HydraOutput getHydraOutputForError(String message) {
    return new HydraOutput(new Scores(-1F, new HashMap<>(), 0)
        .setKaldiJsonObject(new JsonObject()), null)
        .setStatus(ERROR)
        .setMessage(message)
        .setLog(message);
  }

  @NotNull
  private HydraOutput.STATUS_CODES getStatus(JsonObject parse) {
    JsonElement status1 = parse.get(STATUS);
    String status = status1 == null ? HydraOutput.STATUS_CODES.ERROR.toString() : status1.getAsString();
    try {
      return HydraOutput.STATUS_CODES.valueOf(status);
    } catch (IllegalArgumentException e) {
      logger.warn("getStatus : couldn't parse status " + status);
      return HydraOutput.STATUS_CODES.ERROR;
    }
  }
}
