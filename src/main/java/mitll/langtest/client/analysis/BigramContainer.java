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
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.services.AnalysisServiceAsync;
import mitll.langtest.shared.analysis.AnalysisRequest;
import mitll.langtest.shared.analysis.Bigram;
import mitll.langtest.shared.analysis.PhoneBigrams;
import mitll.langtest.shared.analysis.WordAndScore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/20/15.
 */
class BigramContainer extends PhoneContainerBase {
  private final Logger logger = Logger.getLogger("BigramContainer");

  /**
   * @see #setMaxWidth
   */
  private static final int MAX_EXAMPLES = 25;

  private static final String SOUND = "Context";

  private final PhoneExampleContainer exampleContainer;

  //  private static final boolean DEBUG = false;

  /**
   * @param controller
   * @param exampleContainer
   * @param listid
   * @param userid
   * @see AnalysisTab#getPhoneReport
   */
  BigramContainer(ExerciseController controller,
                  PhoneExampleContainer exampleContainer,
                  AnalysisServiceAsync analysisServiceAsync,
                  int listid,
                  int userid) {
    super(controller, analysisServiceAsync, listid, userid);
    this.exampleContainer = exampleContainer;
  }

  @Override
  protected String getLabel() {
    return SOUND;
  }

  /**
   * @return
   * @see AnalysisTab#getPhoneReport
   */
  public Panel getTableWithPager() {
    from = 0;
    to = System.currentTimeMillis();
    return getTableWithPagerForHistory(new ArrayList<>());
  }

  private String phone;

  /**
   * @param result
   * @param phone
   * @param from
   * @param to
   * @see PhoneContainer#clickOnPhone2
   */
  void gotNewPhoneBigrams(PhoneBigrams result, String phone, long from, long to) {
    this.from = from;
    this.to = to;

    List<Bigram> bigrams = result.getPhoneToBigrams().get(phone);

    {
      List<PhoneAndStats> phoneAndStatsList;
      if (bigrams == null) {
        logger.warning("no bigrams for phone " + phone);
        phoneAndStatsList = new ArrayList<>();
      } else {
    //    logger.info("gotNewPhoneReport Got " + bigrams.size() + " for " + phone);
        phoneAndStatsList = getPhoneAndStatsListForPeriod(bigrams);
      }
      //   logger.info("gotNewPhoneReport Got " + phoneAndStatsList.size() + " items for " + phone);

      addItems(phoneAndStatsList);
    }

    this.phone = phone;
    showExamplesForSelectedSound();
  }

  private List<PhoneAndStats> getPhoneAndStatsListForPeriod(List<Bigram> bigrams) {
    List<PhoneAndStats> phoneAndStatsList = new ArrayList<>();
    if (bigrams == null) {
      logger.warning("getPhoneAndStatsListForPeriod huh? phoneToAvgSorted is null ");
    } else {

      getPhoneStatuses(phoneAndStatsList, bigrams);
      if (phoneAndStatsList.isEmpty()) {
        logger.warning("getPhoneAndStatsListForPeriod phoneAndStatsList is empty? ");
      }
    }
    return phoneAndStatsList;
  }

  /**
   * Recalculate an average score for those sessions within the time period first to last.
   *
   * @param phoneAndStatses
   * @paramx first
   * @paramx last
   * @seex #timeChanged
   */
  private void getPhoneStatuses(List<PhoneAndStats> phoneAndStatses,
                                List<Bigram> bigrams) {
    bigrams.forEach(bigram ->
        phoneAndStatses.add(new PhoneAndStats(
            bigram.getBigram(),
            Math.round(100F * bigram.getScore()),
            bigram.getCount()))
    );
  }

  @Override
  protected CellTable.Resources chooseResources() {
    CellTable.Resources o;
    o = GWT.create(LocalTableResources.class);
    return o;
  }

  /**
   * TODO : common base class
   *
   * @param bigram
   * @see #checkForClick
   */
  void clickOnPhone2(String bigram) {
    //   logger.info("clickOnPhone2 bigram = " + bigram);
    analysisServiceAsync.getPerformanceReportForUserForPhone(
        getAnalysisRequest(from, to)
            .setPhone(phone)
            .setBigram(bigram),
        new AsyncCallback<List<WordAndScore>>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("getting performance report for user and phone", caught);
          }

          @Override
          public void onSuccess(List<WordAndScore> filteredWords) {
            if (filteredWords == null) {
              logger.warning("clickOnPhone2 no result for " + phone + " " + bigram);
              exampleContainer.addItems(phone, bigram, Collections.emptyList(), MAX_EXAMPLES);
            } else {
     /*     filteredWords.forEach(wordAndScore -> logger.info("clickOnPhone2 : for " + phone + " and bigram " + bigram +
              "  got " + wordAndScore));
          */

              exampleContainer.addItems(phone,
                  bigram, filteredWords.subList(0, Math.min(filteredWords.size(), MAX_EXAMPLES)),
                  MAX_EXAMPLES);
            }
          }
        });
  }

  private AnalysisRequest getAnalysisRequest(long from, long to) {
    return new AnalysisRequest()
        .setUserid(userid)
        .setListid(listid)
        .setFrom(from)
        .setTo(to)
        .setDialogID(new SelectionState().getDialog())
        .setReqid(reqid++);
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
    BigramContainer.LocalTableResources.TableStyle cellTableStyle();
  }
}
