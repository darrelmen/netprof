package mitll.langtest.client.custom;

import com.google.gwt.user.client.ui.FlexTable;
import mitll.langtest.server.database.AudioDAO;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Shows recording progress in a table.
 * Created by go22670 on 3/2/16.
 */
public class RecordingProgressTable extends FlexTable {
  private static final String MALE_CONTEXT = AudioDAO.MALE_CONTEXT;
  private static final String FEMALE_SLOW = AudioDAO.FEMALE_SLOW;
  private static final String FEMALE_FAST = AudioDAO.FEMALE_FAST;
  private static final String FEMALE_CONTEXT = AudioDAO.FEMALE_CONTEXT;
  private static final String FEMALE = AudioDAO.FEMALE;
  private static final String MALE_SLOW = AudioDAO.MALE_SLOW;
  private static final String MALE = AudioDAO.MALE;
  private static final String MALE_FAST = AudioDAO.MALE_FAST;

  /**
   * @see RecorderNPFHelper#getProgressInfo
   * @param result
   */
  public void populate(Map<String, Float> result) {
    float total = result.get("total");

    getElement().setId("RecordingProgressTable");

    int r = 0;
    int col = 0;

    List<String> labels = Arrays.asList("Male ", "regular", "slow");
    List<String> keys = Arrays.asList(MALE, MALE_FAST, MALE_SLOW);
    for (int i = 0; i < labels.size(); i++) {
      col = setLabelAndVal(result, total, r, col, labels.get(i), keys.get(i));
    }

    labels = Arrays.<String>asList("Female ", "regular", "slow");
    keys = Arrays.asList(FEMALE, FEMALE_FAST, FEMALE_SLOW);
    col = 0;
    r++;
    for (int i = 0; i < labels.size(); i++) {
      col = setLabelAndVal(result, total, r, col, labels.get(i), keys.get(i));
    }

    labels = Arrays.<String>asList("Context Male ", "Female");
    keys = Arrays.asList(MALE_CONTEXT, FEMALE_CONTEXT);
    col = 0;
    r++;
    for (int i = 0; i < labels.size(); i++) {
      col = setLabelAndVal(result, total, r, col, labels.get(i), keys.get(i));
    }
  }

  int setLabelAndVal(Map<String, Float> result, float total, int r, int col, String label, String key) {
    setHTML(r, col++, label);
    col = setCol(result, total, r, col, key);
    return col;
  }

  int setCol(Map<String, Float> result, float total, int r, int col, String male) {
    String sp = "&nbsp;";
    String p = "<b>";
    String s = "</b>" + sp;

    setHTML(r, col++, sp + result.get(male).intValue() + "");
    int percent = getPercent(result.get(male), total);

    setHTML(r, col++, p + "<span" + ((percent == 100) ? " style='color:green'":"") +
        ">"+percent + "%</span>" + s);
    return col;
  }

  private int getPercent(Float male, float total) {
    float ratio = total > 0 ? male / (total) : 0;
    return (int) (ratio * 100f);
  }
}
