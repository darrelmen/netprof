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

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.google.gwt.user.client.ui.HTML;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/4/15.
 */
class TimeWidgets {
  //private final Logger logger = Logger.getLogger("TimeWidgets");
  private final Button prevButton;
  private final Button nextButton;
  private final Button all;
  private final Button day;
  private final Button week;
  private final Button minute;
  private final Button month;
  private final HTML display;
  private final Heading score;
  private final ListBox timeScale;

  /**
   * @param prevButton
   * @param nextButton
   * @param display
   * @param all
   * @param week
   * @param month
   * @see AnalysisTab#getTimeWindowStepper
   */
  TimeWidgets(Button prevButton,
              Button nextButton,

              HTML display,

              Button all,
              Button day,
              Button week,
              Button month,
              Button minute,

              Heading score,
              ListBox timeScale) {
    this.prevButton = prevButton;
    this.nextButton = nextButton;
    this.display = display;
    this.all = all;
    this.day = day;
    this.week = week;
    this.month = month;
    this.minute = minute;
    this.score = score;
    this.timeScale=timeScale;
  }

  /**
   * @param text
   * @see AnalysisPlot#showTimePeriod
   */
  void setDisplay(String text) {
    this.display.setHTML(text);
  }

  void setScore(String text) {
    if (this.score != null) {
      this.score.setText(text);
    }
  }

  public void reset() {
    if (all != null) all.setActive(true);
    if (day != null) day.setActive(false);
    if (week != null) week.setActive(false);
    if (month != null) month.setActive(false);
    if (minute != null) minute.setActive(false);
    if (timeScale != null) timeScale.setSelectedIndex(timeScale.getItemCount()-1);
  }

  Button getPrevButton() {
    return prevButton;
  }

  public Button getNextButton() {
    return nextButton;
  }
}
