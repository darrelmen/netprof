package mitll.langtest.client.scoring;

import mitll.langtest.shared.scoring.AlignmentOutput;

/**
 * Created by go22670 on 4/25/17.
 */
public interface AudioChangeListener {
  void audioChanged(int id, long duration);
  void audioChangedWithAlignment(int id, long duration, AlignmentOutput alignmentOutputFromAudio);
}
