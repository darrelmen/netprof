package mitll.langtest.client.scoring;

import java.util.Set;

/**
 * Created by go22670 on 5/3/17.
 */
public interface RefAudioGetter {
  void getRefAudio(RefAudioListener listener);
  Set<Integer> getReqAudio();
}
