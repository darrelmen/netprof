/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.scoring;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * The types of images we can generate and show.
 * User: go22670
 * Date: 9/10/12
 * Time: 12:54 PM
 * To change this template use File | Settings | File Templates.
 */
public enum NetPronImageType implements IsSerializable {
  WAVEFORM, SPECTROGRAM, WORD_TRANSCRIPT, PHONE_TRANSCRIPT
}
