package mitll.langtest.server.scoring;

import com.google.gson.JsonObject;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.image.ImageType;
import mitll.langtest.server.audio.image.TranscriptEvent;
import mitll.langtest.server.audio.imagewriter.EventAndFileInfo;
import mitll.langtest.shared.instrumentation.SlimSegment;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.npdata.dao.lts.KoreanLTS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static mitll.langtest.shared.scoring.NetPronImageType.*;

public class TranscriptSegmentGenerator {

  private static final Logger logger = LogManager.getLogger(TranscriptSegmentGenerator.class);

  protected ServerProperties serverProps;
  //Language language;

  public TranscriptSegmentGenerator(ServerProperties serverProperties) {
    this.serverProps = serverProperties;
  }

  /**
   * @param typeToEvent
   * @param language
   * @return
   * @see ASRWebserviceScoring#getPretestScore
   */
  @NotNull
  public Map<NetPronImageType, List<TranscriptSegment>> getTypeToSegments(
      Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent,
      Language language) {
    Map<NetPronImageType, List<TranscriptSegment>> typeToEndTimes = new HashMap<>();

    Map<String, String> phoneToDisplay = serverProps.getPhoneToDisplay(language);
    for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : typeToEvent.entrySet()) {
      NetPronImageType key = valueOf(typeToEvents.getKey().toString());
      boolean isPhone = key == PHONE_TRANSCRIPT;

      List<TranscriptSegment> endTimes = typeToEndTimes.computeIfAbsent(key, k -> new ArrayList<>());

      StringBuilder builder = new StringBuilder();
      for (Map.Entry<Float, TranscriptEvent> event : typeToEvents.getValue().entrySet()) {
        TranscriptEvent value = event.getValue();
        String event1 = value.getEvent();
        String displayName = isPhone ? getDisplayName(event1, phoneToDisplay) : event1;
        endTimes.add(new TranscriptSegment(value.getStart(), value.getEnd(), event1, value.getScore(), displayName, builder.length()));

        if (!isPhone) {
          builder.append(event1);
        }
      }
    }

    if (language == Language.KOREAN) {


      List<TranscriptSegment> hydraPhoneSegments = typeToEndTimes.get(PHONE_TRANSCRIPT);


      if (hydraPhoneSegments != null) {
        String before = getSeguence(hydraPhoneSegments);
        //  List<TranscriptSegment> hydraWordSegments = typeToEndTimes.get(WORD_TRANSCRIPT);

//        Map<String, List<TranscriptSegment>> wordToPhoneSeg = new HashMap<>();

        List<TranscriptSegment> allKoreanPhones = new ArrayList<>();
        typeToEndTimes.get(WORD_TRANSCRIPT).forEach(wordSeg -> {
          String event = wordSeg.getEvent();
          List<TranscriptSegment> segs = getSegs(hydraPhoneSegments, wordSeg);
          List<TranscriptSegment> koreanSegments = getKoreanSegments(event, segs);
          logger.info("word " + wordSeg +
              "\n\tphones " + new HashSet<>(segs) +
              "\n\tkorean " + new HashSet<>(koreanSegments)
          );
          allKoreanPhones.addAll(koreanSegments);
          //wordToPhoneSeg.put(event, segs);
        });
//        List<TranscriptSegment> koreanSegmentsForWord = getKoreanSegments(hydraPhoneSegments);
        //      List<TranscriptSegment> koreanSegments = koreanSegments1;
        String after = getSeguence(allKoreanPhones);

        logger.info("segments : " +
            "\n\tbefore " + before +
            "\n\tafter " + after);

        typeToEndTimes.put(PHONE_TRANSCRIPT, allKoreanPhones);
      }
    }
    return typeToEndTimes;
  }

  private List<TranscriptSegment> getSegs(List<TranscriptSegment> phones, TranscriptSegment word) {
    List<TranscriptSegment> phonesInWord = new ArrayList<>();
    for (TranscriptSegment phone : phones) {
      if (phone.getStart() >= word.getStart() && phone.getEnd() <= word.getEnd()) {
        phonesInWord.add(phone);
      }
    }
    return phonesInWord;
  }

  private String getSeguence(List<TranscriptSegment> transcriptSegments) {
    StringBuilder builder = new StringBuilder();
    transcriptSegments.stream().forEach(transcriptSegment -> builder.append(transcriptSegment.getEvent()).append(" "));
    String compact = builder.toString();
    return compact;
  }

  private KoreanLTS koreanLTS = new KoreanLTS();

  private List<TranscriptSegment> getKoreanSegments(String word, List<TranscriptSegment> hydraPhones) {
    //List<List<String>> fragmentList = getKoreanFragments(word, koreanLTS);
    return getKoreanFragmentSequence(getKoreanFragments(word, koreanLTS), hydraPhones);
  }

  private String getDisplayName(String event, Map<String, String> phoneToDisplay) {
    String displayName = phoneToDisplay.get(event);
    displayName = displayName == null ? event : displayName;
    return displayName;
  }


  // so take every pronunciation in the dict and map back into fragment sequence
  // if two hydra phonemes combine to form one compound, use it and skip ahead two
  // if multiple fragments are possible, try to chose the one that is expected from the compound character
  // if it's not there, use the first simple match...
/*
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
*/


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


  private List<TranscriptSegment> getKoreanFragmentSequence(List<List<String>> fragmentList, List<TranscriptSegment> hydraPhoneSequence) {
    int length = hydraPhoneSequence.size();
    // StringBuilder builder = new StringBuilder();

    int fragIndex = 0;
    int fragCount = 0;
    List<String> currentFragments = fragmentList.get(fragIndex);

    //sanityCheck();

    List<TranscriptSegment> koreanPhones = new ArrayList<>();

    String prevMatch = null;
    for (int j = 0; j < length; j++) {
      TranscriptSegment currentSegment = hydraPhoneSequence.get(j);
      TranscriptSegment nextSegment = j < length - 1 ? hydraPhoneSequence.get(j + 1) : null;
      String nextHydraPhone = nextSegment == null ? "" : nextSegment.getEvent();
      //String currentHydraPhone = currentSegment.getEvent();

      List<String> simpleKorean = LTSFactory.getSimpleKorean(currentSegment.getEvent());
      List<String> compoundKorean = LTSFactory.getCompoundKorean(currentSegment.getEvent(), nextHydraPhone);

      logger.info("got " + j + " " + currentSegment + "+" + nextHydraPhone +
          " = " + simpleKorean + " - " + compoundKorean);

      if (compoundKorean == null || compoundKorean.isEmpty()) {
        String str = simpleKorean.get(0);

        if (simpleKorean.size() == 1) {
          // builder.append(str).append(" ");
          // SlimSegment slimSegment = currentSegment.setEvent(str);
          addKoreanSegment(koreanPhones, currentSegment, str);
          prevMatch = str;
        } else {
          String match = getMatch(fragIndex, currentFragments, simpleKorean);
          if (match != null) {
            //  builder.append(match).append(" ");
            addKoreanSegment(koreanPhones, currentSegment, match);

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
                // builder.append(match).append(" ");
                addKoreanSegment(koreanPhones, currentSegment, match);

                prevMatch = match;
              }
            } else {
              //match = getMatch(fragIndex, currentFragments, prevSimple);
              logger.warn("fall back to " + str + " given expected " + new HashSet<>(currentFragments));
              //  builder.append(str).append(" ");

              addKoreanSegment(koreanPhones, currentSegment, str);

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

        if (match != null) {
          //builder.append(match).append(" ");
          addCombinedKoreanSegment(koreanPhones, currentSegment, nextSegment, match);
        } else {
          String s = compoundKorean.get(0);
          logger.warn("2 fall back to '" + s + "' given " + new HashSet<>(currentFragments));
          //     builder.append(s).append(" ");

          addCombinedKoreanSegment(koreanPhones, currentSegment, nextSegment, s);
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
//        logger.info("got " + i + " " + j + " " + currentSegment + " = " + simpleKorean + " - " + compoundKorean);

    }
//    return builder.toString();
    //ret.add(e);

    return koreanPhones;
  }

  private void addKoreanSegment(List<TranscriptSegment> koreanPhones, TranscriptSegment currentSegment, String str) {
    logger.info("from " + currentSegment.getEvent() + " to " + str);
    koreanPhones.add(new TranscriptSegment(currentSegment).setEvent(str));
  }

  private void addCombinedKoreanSegment(List<TranscriptSegment> koreanPhones, TranscriptSegment currentSegment, TranscriptSegment nextSegment, String str) {
    logger.info("from " + currentSegment.getEvent() + " to " + str);
    float avg = (currentSegment.getScore() + nextSegment.getScore()) / 2F;
    koreanPhones.add(new TranscriptSegment(currentSegment.getStart(), nextSegment.getEnd(), str, avg, str, str.length()).setEvent(str));
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
}
