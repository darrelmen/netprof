package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;

import java.util.Collection;
import java.util.Set;

/**
 * Created by go22670 on 5/3/17.
 */
public interface RefAudioGetter {
  void addWidgets(boolean showFL, boolean showALTFL, PhonesChoices phonesChoices);

  void getRefAudio(RefAudioListener listener);

  Set<Integer> getReqAudio();

  /**
   * @see mitll.langtest.client.list.FacetExerciseList#makeExercisePanels(Collection, DivWidget, int)
   * @param req
   */
  void setReq(int req);

  int getReq();
}
