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

package mitll.langtest.client.custom.recording;

import com.google.gwt.user.client.ui.FlexTable;
import mitll.langtest.server.database.audio.BaseAudioDAO;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Shows recording progress in a table.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/2/16.
 */
public class RecordingProgressTable extends FlexTable {
  private static final String FEMALE_SLOW = BaseAudioDAO.FEMALE_SLOW;
  private static final String FEMALE_FAST = BaseAudioDAO.FEMALE_FAST;

//  private static final String MALE_CONTEXT = BaseAudioDAO.MALE_CONTEXT;
//  private static final String FEMALE_CONTEXT = BaseAudioDAO.FEMALE_CONTEXT;

  private static final String FEMALE = BaseAudioDAO.FEMALE;
  private static final String MALE_SLOW = BaseAudioDAO.MALE_SLOW;
  private static final String MALE = BaseAudioDAO.MALE;
  private static final String MALE_FAST = BaseAudioDAO.MALE_FAST;
  public static final String REGULAR = "regular";
  public static final String SLOW = "slow";
  public static final String TOTAL = "Total";

  /**
   * 4 cols for male
   * 3 for female
   *
   * @param result
   * @see RecorderNPFHelper#getProgressInfo
   */
  public void populate(Map<String, Float> result) {
    float total  = result.get(BaseAudioDAO.TOTAL);
    //getElement().setId("RecordingProgressTable");

    int r = 0;
    int col;

    List<String> labels = Arrays.asList("Male ", REGULAR, SLOW);
    List<String> keys   = Arrays.asList(MALE, MALE_FAST, MALE_SLOW);
    col = addCols(result, total, 0, 0, labels, keys);

    // fourth col
    setLabelAndVal(result, total, r, col, TOTAL, "total", false);

    labels = Arrays.asList("Female ", REGULAR, SLOW);
    keys = Arrays.asList(FEMALE, FEMALE_FAST, FEMALE_SLOW);
    r++;
    addCols(result, total, r, 0, labels, keys);


    float ctotal = result.get(BaseAudioDAO.TOTAL_CONTEXT);

    //labels = Arrays.<String>asList("Context Male ", "Female");
    labels = Arrays.asList("Context Male ", REGULAR, SLOW);
    keys   = Arrays.asList(BaseAudioDAO.CMALE, BaseAudioDAO.CMALE_FAST, BaseAudioDAO.CMALE_SLOW);

    r++;
    col = addCols(result, ctotal, r, 0, labels, keys);
    setLabelAndVal(result, total, r, col, TOTAL, BaseAudioDAO.TOTAL_CONTEXT, false);

    labels = Arrays.asList("Context Female ", REGULAR, SLOW);
    keys   = Arrays.asList(BaseAudioDAO.CFEMALE, BaseAudioDAO.CFEMALE_FAST, BaseAudioDAO.CFEMALE_SLOW);

    r++;
    addCols(result, ctotal, r, 0, labels, keys);

//    col += 3;
  //  col = setLabelAndVal(result, total, r, col, TOTAL, BaseAudioDAO.TOTAL_CONTEXT, false);

  }

  private int addCols(Map<String, Float> result, float total, int r, int col, List<String> labels, List<String> keys) {
    for (int i = 0; i < labels.size(); i++) {
      col = setLabelAndVal(result, total, r, col, labels.get(i), keys.get(i), true);
    }
    return col;
  }

  private int setLabelAndVal(Map<String, Float> result, float total, int r, int col, String label, String key, boolean addPercent) {
    setHTML(r, col++, label);
    col = setCol(result, total, r, col, key, addPercent);
    return col;
  }

  private int setCol(Map<String, Float> result, float total, int r, int col, String male, boolean addPercent) {
    String sp = "&nbsp;";
    String p = "<b>";
    String s = "</b>" + sp;

    setHTML(r, col++, sp + result.get(male).intValue() + "");
    int percent = getPercent(result.get(male), total);

    setHTML(r, col++, p +
        (addPercent ? getPercent(percent) : "")
        + s);
    return col;
  }

  private String getPercent(int percent) {
    return "<span" + ((percent == 100) ? " style='color:green'" : "") +
        ">" + percent + "%</span>";
  }

  private int getPercent(Float male, float total) {
    float ratio = total > 0 ? male / (total) : 0;
    return (int) (ratio * 100f);
  }
}
