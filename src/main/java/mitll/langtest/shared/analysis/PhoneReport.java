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

package mitll.langtest.shared.analysis;

import mitll.langtest.server.database.analysis.IAnalysis;
import mitll.langtest.server.database.word.Word;
import mitll.langtest.shared.project.Language;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/22/15.
 */
public class PhoneReport extends PhoneSummary {
  private int overallPercent;
  private Map<String, Map<String, List<WordAndScore>>> phoneToWordAndScoreSorted = new HashMap<>();
  private Map<String, List<Bigram>> phoneToBigrams;

  // can't be final!
  private boolean valid;
  private long serverTime;
  private transient Language language;
  private List<String> sortedPhones;
  private transient Map<Integer, List<Word>> ridToWords;

  public PhoneReport() {
    valid = false;
  }

  /**
   * @param overallPercent
   * @param language
   * @paramx phoneToWordAndScoreSorted
   * @see mitll.langtest.client.analysis.PhoneContainer
   * @see mitll.langtest.server.database.phone.MakePhoneReport#getPhoneReport
   */
  public PhoneReport(int overallPercent,
                     Map<String, List<Bigram>> phoneToBigrams,
                     Map<String, PhoneStats> phoneToAvgSorted,
                     Map<String, Map<String, List<WordAndScore>>> phoneToWordAndScoreSorted,
                     List<String> sortedPhones,
                     Language language,
                     Map<Integer, List<Word>> ridToWords) {
    super(phoneToAvgSorted);

    this.overallPercent = overallPercent;
    this.phoneToBigrams = phoneToBigrams;
    this.phoneToWordAndScoreSorted = phoneToWordAndScoreSorted;
    this.sortedPhones = sortedPhones;
    this.language = language;
    this.ridToWords = ridToWords;
    valid = true;
  }

  /**
   * Map of each phone to words it appears in.
   *
   * @return
   * @see IAnalysis#getPhoneReportFor(AnalysisRequest)
   */
  public Map<String, Map<String, List<WordAndScore>>> getPhoneToWordAndScoreSorted() {
    return phoneToWordAndScoreSorted;
  }

  public int getOverallPercent() {
    return overallPercent;
  }

  public boolean isValid() {
    return valid;
  }

  public Map<String, List<Bigram>> getPhoneToBigrams() {
    return phoneToBigrams;
  }

  public long getServerTime() {
    return serverTime;
  }

  public void setServerTime(long serverTime) {
    this.serverTime = serverTime;
  }

  public PhoneReport setReqid(int reqid) {
    super.setReqid(reqid);
    return this;
  }

  public Language getLanguage() {
    return language;
  }

  public List<String> getSortedPhones() {
    return sortedPhones;
  }

  public String toString() {
    return "valid " + valid + " : " + super.toString();
  }

  public Map<Integer, List<Word>> getRidToWords() {
    return ridToWords;
  }
}