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

package mitll.langtest.client.analysis;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.services.AnalysisServiceAsync;
import mitll.langtest.shared.analysis.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/20/15.
 */
class PhoneContainer extends PhoneContainerBase implements AnalysisPlot.TimeChangeListener {
  private final Logger logger = Logger.getLogger("PhoneContainer");

  private static final String SOUND = "Sound";

  private final BigramContainer bigramContainer;

  private final DateTimeFormat debugShortFormat = DateTimeFormat.getFormat("MMM d yyyy HH:mm:ss");

  private static final boolean DEBUG = false;

  /**
   * @param controller
   * @param bigramContainer
   * @param reqInfo
   * @see AnalysisTab#getPhoneReport
   */
  PhoneContainer(ExerciseController controller,
                 BigramContainer bigramContainer,
                 AnalysisServiceAsync analysisServiceAsync,
                 AnalysisTab.ReqInfo reqInfo) {
    super(controller, analysisServiceAsync, reqInfo);
    this.bigramContainer = bigramContainer;
  }

  @Override
  protected String getLabel() {
    return SOUND;
  }

  /**
   * @param phoneReport
   * @return
   * @see AnalysisTab#getPhoneReport
   */
  public Panel getTableWithPager(PhoneSummary phoneReport) {
    from = 0;
    to = System.currentTimeMillis();
    return getTableWithPagerForHistory(getPhoneAndStatsList(phoneReport, from, to));
  }

  /**
   * @param from
   * @param to
   * @see AnalysisPlot#timeChanged
   * @see #getTableWithPager
   */
  @Override
  public void timeChanged(long from, long to) {
    long diff = to - from;
    if (DEBUG) {
      logger.info("PhoneContainer.timeChanged From (" + diff +
          ") " + debugFormat(from) + " : " + debugFormat(to));
    }

    this.from = from;
    this.to = to;

    if (diff == 1) {
      this.from--;
      this.to++;
    }
    long then = System.currentTimeMillis();

    AnalysisRequest analysisRequest = getAnalysisRequest(from, to);


    analysisServiceAsync.getPhoneSummary(
        analysisRequest, new AsyncCallback<PhoneSummary>() {
          @Override
          public void onFailure(Throwable caught) {
            logger.warning("\n\n\n-> getPhoneSummary " + caught);
            controller.getMessageHelper().handleNonFatalError("problem getting phone summary", caught);
          }

          @Override
          public void onSuccess(PhoneSummary result) {
            long now = System.currentTimeMillis();
            long total = now - then;
            if (DEBUG) {
              logger.info("getPhoneSummary userid " + reqInfo.getUserid() + " req " + reqid +
                  "\n\ttook   " + total +
                  "\n\tserver " + result.getServerTime() +
                  "\n\tclient " + (total - result.getServerTime()));
            }

            if (result.getReqid() + 1 != reqid) {
              if (DEBUG) logger.info("skip stale req");
            } else {
              gotNewPhoneSummary(result);
            }
          }
        });
  }

  /**
   * TODO :  ??? why filter by time twice??
   *
   * @param result
   */
  private void gotNewPhoneSummary(PhoneSummary result) {
    List<PhoneAndStats> phoneAndStatsList = getPhoneAndStatsListForPeriod(result.getPhoneToAvgSorted(), from, to);
    addItems(phoneAndStatsList);
    showExamplesForSelectedSound();
  }

  private List<PhoneAndStats> getPhoneAndStatsList(PhoneSummary phoneReport, long from, long to) {
    return (phoneReport == null) ? Collections.emptyList() :
        getPhoneAndStatsListForPeriod(phoneReport.getPhoneToAvgSorted(), from, to);
  }


  private List<PhoneAndStats> getPhoneAndStatsListForPeriod(Map<String, PhoneStats> phoneToAvgSorted,
                                                            long first,
                                                            long last) {

    List<PhoneAndStats> phoneAndStatsList = new ArrayList<>();
    if (phoneToAvgSorted == null) {
      logger.warning("getPhoneAndStatsListForPeriod huh? phoneToAvgSorted is null ");
    } else {
      if (DEBUG)
        logger.info("getPhoneAndStatsListForPeriod From " + first +
            "/" + debugFormat(first) + " : " + last + "/" + debugFormat(last) + " over " + phoneToAvgSorted.size());

      getPhoneStatuses(phoneAndStatsList, phoneToAvgSorted, first, last);
      if (phoneAndStatsList.isEmpty()) {
        logger.warning("getPhoneAndStatsListForPeriod phoneAndStatsList is empty? (" + (last - first) +
            ") ");
      }
    }
    return phoneAndStatsList;
  }

  /**
   * Recalculate an average score for those sessions within the time period first to last.
   *
   * @param phoneAndStatses
   * @param phoneToAvgSorted
   * @param first
   * @param last
   * @see #timeChanged
   */
  private void getPhoneStatuses(List<PhoneAndStats> phoneAndStatses,
                                Map<String, PhoneStats> phoneToAvgSorted,
                                long first, long last) {
    if (DEBUG) {
      logger.info("getPhoneStatuses From    " + first + "/" + debugFormat(first) + " : " + last + "/" + debugFormat(last));
      logger.info("getPhoneStatuses examine " + phoneToAvgSorted.entrySet().size());
    }

    // if avg score is less than 75 = native
    // get diff, weighted by total
    // this diff is the rank - highest go first
    // then above 75 weighted... how?

    for (Map.Entry<String, PhoneStats> ps : phoneToAvgSorted.entrySet()) {
      PhoneStats value = ps.getValue();
      List<PhoneSession> filtered = getFiltered(first, last, value);
      //    logger.info("key " + ps.getKey() + " value " + filtered.size());
      //  logger.info("Filtered " + filtered.size());

      if (!filtered.isEmpty()) {
        float total = 0;
        long ltotal = 0;
        float avg = 0;
        float ndiff = 0;
        for (PhoneSession session : filtered) {
          long count1 = session.getCount();
          ltotal += count1;
          float fcount = Long.valueOf(count1).floatValue();
          total += fcount;
          double mean = session.getMean();
          float fmean = Double.valueOf(mean).floatValue();
          float diffNative = 0.75F - fmean;
          if (fmean > 0.75) {
            float diffN1 = (1F - fmean) / 100F;
            ndiff += diffN1 * fcount;
          } else {
            float ldiff = (float) diffNative;
            float weight = ldiff * fcount;
            ndiff += weight;
          }

          avg += fmean * fcount;
        }
        float overall = avg / total;

        int v = Float.valueOf(overall * 100).intValue();
        if (DEBUG) {
          logger.info("getPhoneStatuses : overall " + overall + " avg " + avg + " total " + total + " report " + v);
        }

        // int totalCount = value.getTotalCount(filtered);
        String thePhone = ps.getKey();
        //if (SORT_BY_RANK) logger.info(thePhone + " : total " + total + " ndiff " + ndiff);
        phoneAndStatses.add(new PhoneAndStats(thePhone, v, Long.valueOf(ltotal).intValue()));
      }
    }

//    if (SORT_BY_RANK) {
//      phoneAndStatses.sort((o1, o2) -> {
//        int compare = -1 * Float.compare(o1.getNdiff(), o2.getNdiff());
//        return compare == 0 ? o1.compareTo(o2) : compare;
//      });
//
//      for (int i = 0; i < phoneAndStatses.size(); i++) phoneAndStatses.get(i).setRank(i + 1);
//    } else {
    Collections.sort(phoneAndStatses);
//    }
    if (DEBUG) {
      logger.info("getPhoneStatuses returned " + phoneAndStatses.size());
      if (phoneAndStatses.isEmpty()) {
        phoneToAvgSorted.forEach((k, v) -> {
          logger.info(k + " = " + v.getSessions().size() + " sessions");
          v.getSessions()
              .forEach(phoneSession -> logger.info("\t" + k + " = " +
                  debugFormat(phoneSession.getStart()) + "-" + debugFormat(phoneSession.getEnd())));
        });
      }
    }
  }

  /**
   * Get sessions in this time period.
   *
   * @param first
   * @param last
   * @param value
   * @return
   * @see #getPhoneStatuses(List, Map, long, long)
   */
  private List<PhoneSession> getFiltered(long first, long last, PhoneStats value) {
    List<PhoneSession> sessions = value.getSessions();
    if (DEBUG) {
      logger.info("getFiltered " + first + "/" +
          debugFormat(first) + " - " + debugFormat(last) +
          " over " + sessions.size() + " sessions");
    }
    return first == 0 ? sessions : getFiltered(sessions, first, last);
  }

  private String debugFormat(long first) {
    return debugShortFormat.format(new Date(first));
  }


  /**
   * TODO : this doesn't work properly - should do any sessions that overlap with the window
   *
   * @param orig
   * @param first
   * @param last
   * @return
   * @see #clickOnPhone2
   * @see #getFiltered(long, long, PhoneStats)
   */
  private List<PhoneSession> getFiltered(List<PhoneSession> orig, long first, long last) {
    if (DEBUG && true) {
      logger.info("getFiltered : over " + orig.size() +
          " From " + first + "/" + debugFormat(first) + " - " + debugFormat(last) + " window dur " + (last - first));
    }

    List<PhoneSession> filtered = new ArrayList<>();
    for (PhoneSession session : orig) {
      //  String window = shortFormat(session.getStart()) + " - " + shortFormat(session.getEnd());
      if (doesSessionOverlap(first, last, session)) {
        filtered.add(session);

        if (DEBUG) {
          logger.info("getFiltered included " + session.getPhone() +
              " " +
              debugFormat(session.getStart()) + "-" +
              debugFormat(session.getEnd())
          );
        }
      } else if (DEBUG) {
        logger.info("getFiltered exclude " + session);
      }
    }
    if (DEBUG) {
      logger.info("getFiltered : over " + orig.size() +
          " From " + first + "/" + debugFormat(first) + " - " + debugFormat(last) +
          " window dur " + (last - first) + " found " + filtered.size());
//      logger.info("getFiltered : found " + filtered.size());
    }
    return filtered;
  }

  private boolean doesSessionOverlap(long first, long last, PhoneSession session) {
    long sessionStart = session.getStart();
    return
        (sessionStart >= first && sessionStart <= last) || // start inside window
            (session.getEnd() >= first && session.getEnd() <= last) ||    // end inside window
            (sessionStart < first && session.getEnd() > last);      // session starts before and ends after window
  }

  @Override
  protected void clickOnPhone2(String phone) {
    if (DEBUG) logger.info("clickOnPhone2 : got click on" +
        "\n\tphone " + phone +
        "\n\tfrom  " + from +
        "\n\tto    " + to +
        "\n\treqid " + reqid
    );

    long then = System.currentTimeMillis();

    AnalysisRequest analysisRequest = getAnalysisRequest(from, to);

    analysisServiceAsync.getPhoneBigrams(
        analysisRequest, new AsyncCallback<PhoneBigrams>() {
          @Override
          public void onFailure(Throwable caught) {
            logger.warning("\n\n\n-> getPhoneSummary " + caught);
            controller.getMessageHelper().handleNonFatalError("problem getting phone summary", caught);
          }

          @Override
          public void onSuccess(PhoneBigrams result) {
            if (DEBUG) {
              long total = System.currentTimeMillis() - then;
              logger.info("getPhoneBigrams userid " + reqInfo.getUserid() + " req " + reqid +
                  "\n\tphone  " + phone +
                  "\n\ttook   " + total +
                  "\n\tserver " + result.getServerTime() +
                  "\n\tclient " + (total - result.getServerTime()));
            }

            if (result.getReqid() + 1 != reqid) {
              logger.info("clickOnPhone2 : skip stale req");
            } else {
              bigramContainer.gotNewPhoneBigrams(result, phone, from, to);
            }
          }
        });
    // bigramContainer.gotNewPhoneSummary(phoneReport, phone, from, to);
  }

  @Override
  protected CellTable.Resources chooseResources() {
    CellTable.Resources o;
    o = GWT.create(LocalTableResources.class);
    return o;
  }

  /**
   * MUST BE PUBLIC
   */
  public interface LocalTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    /**
     * The styles applied to the table.
     */
    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "PhoneScoresCellTableStyleSheet.css"})
    PhoneContainer.LocalTableResources.TableStyle cellTableStyle();
  }
}
