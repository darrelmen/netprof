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

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.image.ImageType;
import mitll.langtest.server.audio.image.TranscriptEvent;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.npdata.dao.lts.KoreanLTS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static mitll.langtest.server.audio.image.TranscriptImage.IGNORE_TOKENS;
import static mitll.langtest.shared.scoring.NetPronImageType.*;

public class TranscriptSegmentGenerator {
  private static final Logger logger = LogManager.getLogger(TranscriptSegmentGenerator.class);
  protected final ServerProperties serverProps;
  private static final boolean DEBUG = false;
  private static final boolean WARN_ABOUT_FALLBACK = false;

  /**
   * @param serverProperties
   * @see ASRWebserviceScoring#ASRWebserviceScoring
   */
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

//    Map<String, String> phoneToDisplay = serverProps.getPhoneToDisplay(language);
    for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : typeToEvent.entrySet()) {
      NetPronImageType key = valueOf(typeToEvents.getKey().toString());
      // boolean isPhone = key == PHONE_TRANSCRIPT;

      List<TranscriptSegment> endTimes = typeToEndTimes.computeIfAbsent(key, k -> new ArrayList<>());
      //   StringBuilder builder = new StringBuilder();

      List<TranscriptEvent> events = new ArrayList<>();

      for (Map.Entry<Float, TranscriptEvent> event : typeToEvents.getValue().entrySet()) {
        events.add(event.getValue());
      }

//      StringBuilder builder = new StringBuilder();
//      StringBuilder builder2 = new StringBuilder();
      int size = events.size();
      for (int i = 0; i < size; i++) {
//      for (Map.Entry<Float, TranscriptEvent> event : typeToEvents.getValue().entrySet()) {
        TranscriptEvent value = events.get(i);

        String prevEvent = i == 0 ? null : events.get(i - 1).getEvent();
        String nextEvent = i < size - 1 ? events.get(i + 1).getEvent() : null;

        String event1 = value.getEvent();
        String displayName = serverProps.getDisplayPhoneme(language, event1, prevEvent, nextEvent);
//        builder.append(event1);
//        builder2.append(displayName);

//        String displayName = isPhone ? getDisplayName(event1, phoneToDisplay) : event1;
        endTimes.add(new TranscriptSegment(value.getStart(), value.getEnd(), event1, value.getScore(), displayName));
//        if (!isPhone) {
//          builder.append(event1);
//        }
      }

 /*     if (key == PHONE_TRANSCRIPT)
        logger.info("getTypeToSegments from " + builder + " -> " + builder2);*/
    }


    if (language == Language.KOREAN) {
      doKoreanPhoneTranslation(typeToEndTimes);
    }
    return typeToEndTimes;
  }

  private void doKoreanPhoneTranslation(Map<NetPronImageType, List<TranscriptSegment>> typeToEndTimes) {
    List<TranscriptSegment> hydraPhoneSegments = typeToEndTimes.get(PHONE_TRANSCRIPT);

    if (hydraPhoneSegments != null) {
      String before = DEBUG ? getSeguence(hydraPhoneSegments) : "";
      List<TranscriptSegment> allKoreanPhones = new ArrayList<>();
      typeToEndTimes.get(WORD_TRANSCRIPT).forEach(wordSeg -> {
        if (isValid(wordSeg)) {
          List<TranscriptSegment> segs = getSegs(hydraPhoneSegments, wordSeg);
          List<TranscriptSegment> koreanSegments = getKoreanSegments(wordSeg.getEvent(), segs);
          if (DEBUG) {
            logger.info("word " + wordSeg +
                "\n\tphones " + getSeguence(segs) +
                "\n\tkorean " + getSeguence(koreanSegments)
            );
          }
          allKoreanPhones.addAll(koreanSegments);
        }
      });
      Collections.sort(allKoreanPhones);
      if (DEBUG) {
        String after = getSeguence(allKoreanPhones);

        if (DEBUG) logger.info("segments : " +
            "\n\tbefore " + before +
            "\n\tafter " + after);
      }

      typeToEndTimes.put(PHONE_TRANSCRIPT, allKoreanPhones);
    }
  }

  private boolean isValid(TranscriptSegment wordSeg) {
    return !IGNORE_TOKENS.contains(wordSeg.getEvent());
  }

  /**
   * @param phones
   * @param word
   * @return
   * @see mitll.langtest.client.scoring.DialogExercisePanel#getSegsWithinWordTimeWindow
   */
  private List<TranscriptSegment> getSegs(List<TranscriptSegment> phones, TranscriptSegment word) {
    List<TranscriptSegment> phonesInWord = new ArrayList<>();

    float start = word.getStart();
    float end = word.getEnd();

    for (TranscriptSegment phone : phones) {
      if (phone.getStart() >= start && phone.getEnd() <= end) {
        phonesInWord.add(phone);
      }
      if (phone.getStart() > end) {
        break;
      }
    }
    return phonesInWord;
  }

  private String getSeguence(List<TranscriptSegment> transcriptSegments) {
    StringBuilder builder = new StringBuilder();
    transcriptSegments.forEach(transcriptSegment -> builder.append(transcriptSegment.getEvent()).append(" "));
    return builder.toString();
  }

  private final KoreanLTS koreanLTS = new KoreanLTS();

  /**
   * @param word
   * @param hydraPhones
   * @return
   * @see #doKoreanPhoneTranslation(Map)
   */
  private List<TranscriptSegment> getKoreanSegments(String word, List<TranscriptSegment> hydraPhones) {
    return getKoreanFragmentSequence(word, getKoreanFragments(word, koreanLTS), hydraPhones);
  }

  private String getDisplayName(String event, Map<String, String> phoneToDisplay) {
    String displayName = phoneToDisplay.get(event);
    displayName = displayName == null ? event : displayName;
    return displayName;
  }

  @NotNull
  private List<List<String>> getKoreanFragments(String foreignLanguage, KoreanLTS koreanLTS) {
    char[] chars = foreignLanguage.toCharArray();
    List<List<String>> fragmentList = new ArrayList<>();
    for (char aChar : chars) {
      List<String> e = koreanLTS.expectedFragments(aChar);
      fragmentList.add(e);

      if (DEBUG) {
        e.forEach(f -> logger.info("getKoreanFragments : for '" + foreignLanguage + "' '" + aChar +
            "'  expected '" + f + "' of " + e.size()));
      }
      // logger.info("for " + foreignLanguage + " expected "+fragmentList);
    }
    return fragmentList;
  }

  /**
   * @param fragmentList
   * @param hydraPhoneSequence
   * @return
   * @see #getKoreanSegments(String, List)
   */
  private List<TranscriptSegment> getKoreanFragmentSequence(String word,
                                                            List<List<String>> fragmentList,
                                                            List<TranscriptSegment> hydraPhoneSequence) {
    int length = hydraPhoneSequence.size();

    int fragIndex = 0;
    int fragCount = 0;
    List<String> currentFragments = fragmentList.get(fragIndex);

    List<TranscriptSegment> koreanPhones = new ArrayList<>();

    String prevMatch = null;
    for (int j = 0; j < length; j++) {
      TranscriptSegment currentSegment = hydraPhoneSequence.get(j);
      TranscriptSegment nextSegment = j < length - 1 ? hydraPhoneSequence.get(j + 1) : null;
      String nextHydraPhone = nextSegment == null ? "" : nextSegment.getEvent();
      List<String> compoundKorean = LTSFactory.getCompoundKorean(currentSegment.getEvent(), nextHydraPhone);


      if (compoundKorean == null || compoundKorean.isEmpty()) {
        String event = currentSegment.getEvent();
        List<String> simpleKorean = LTSFactory.getSimpleKorean(event);
        if (DEBUG)
          logger.info("getKoreanFragmentSequence got #" + j + " " + currentSegment.getEvent() + //"+" + nextHydraPhone +
              " = " + simpleKorean);

        if (simpleKorean == null) {
          if (DEBUG) logger.info("getKoreanFragmentSequence : no korean fragment for '" + event + "' ??? ");
          addKoreanSegment(koreanPhones, currentSegment, event);
        } else if (simpleKorean.size() == 1) {
          // builder.append(koreanFragment).append(" ");
          // SlimSegment slimSegment = currentSegment.setEvent(koreanFragment);
          String koreanFragment = simpleKorean.get(0);
          addKoreanSegment(koreanPhones, currentSegment, koreanFragment);
          prevMatch = koreanFragment;
        } else {
          String match = getMatch(fragIndex, currentFragments, simpleKorean);
          if (match != null) {
            //  builder.append(match).append(" ");
            addKoreanSegment(koreanPhones, currentSegment, match);

            prevMatch = match;
          } else {
            if (false && currentFragments.contains(prevMatch)) {
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
              String koreanFragment = simpleKorean.get(0);
              if (WARN_ABOUT_FALLBACK)
                logger.warn("getKoreanFragmentSequence (" + word + ") fall back to " + koreanFragment + " given expected " + new HashSet<>(currentFragments));
              //  builder.append(koreanFragment).append(" ");
              addKoreanSegment(koreanPhones, currentSegment, koreanFragment);
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
            if (DEBUG) logger.info("1 frag index now " + fragIndex + " " + new HashSet<>(currentFragments));
          } else {
            if (DEBUG) logger.info("1 frag index NOPE " + fragIndex + " " + new HashSet<>(currentFragments));
          }
        } else {
//            logger.info("1 " + fragCount + " vs " + currentFragments.size());
        }


      } else {
        j++;

        if (DEBUG)
          logger.info("getKoreanFragmentSequence got #" + j + " " + currentSegment.getEvent() + "+" + nextHydraPhone +
              " = " + compoundKorean);

        String match = getMatch(fragIndex, currentFragments, compoundKorean);

        if (match != null) {
          //builder.append(match).append(" ");
          addCombinedKoreanSegment(koreanPhones, currentSegment, nextSegment, match);
        } else {
          String s = compoundKorean.get(0);
          if (WARN_ABOUT_FALLBACK)
            logger.info("getKoreanFragmentSequence (" + word + ") 2 fall back to '" + s + "' given " + new HashSet<>(currentFragments));
          //     builder.append(s).append(" ");
          addCombinedKoreanSegment(koreanPhones, currentSegment, nextSegment, s);
        }
        fragCount++;
        if (fragCount == currentFragments.size()) {
          fragCount = 0;
          fragIndex++;
          if (fragIndex < fragmentList.size()) {
            currentFragments = fragmentList.get(fragIndex);
            if (DEBUG) logger.info("2 frag index now " + fragIndex + " " + new HashSet<>(currentFragments));
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
    if (DEBUG) logger.info("addKoreanSegment from '" + currentSegment.getEvent() + "' to " + str);
    koreanPhones.add(new TranscriptSegment(currentSegment).setEvent(str));
  }

  private void addCombinedKoreanSegment(List<TranscriptSegment> koreanPhones,
                                        TranscriptSegment currentSegment,
                                        TranscriptSegment nextSegment, String str) {
    if (DEBUG) logger.info("addCombinedKoreanSegment from " + currentSegment.getEvent() + " to " + str);
    float avg = (currentSegment.getScore() + nextSegment.getScore()) / 2F;
    koreanPhones.add(new TranscriptSegment(currentSegment.getStart(), nextSegment.getEnd(), str, avg, str).setEvent(str));
  }

  @Nullable
  private String getMatch(int fragIndex, List<String> currentFragments, List<String> simpleKorean) {
    String match = null;
    for (String candidate : simpleKorean) {
      boolean contains = currentFragments.contains(candidate);
      if (contains)
        if (DEBUG)
          logger.info("getMatch : check " + candidate + " in (" + fragIndex + ")" + new HashSet<>(currentFragments));

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
