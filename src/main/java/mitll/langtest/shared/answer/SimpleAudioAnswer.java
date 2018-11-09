package mitll.langtest.shared.answer;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.scoring.PretestScore;

public class SimpleAudioAnswer implements IsSerializable {

  protected String path = null;
  protected PretestScore pretestScore;

  public SimpleAudioAnswer() {
  }

  public String getPath() {
    return path;
  }

  public PretestScore getPretestScore() {
    return pretestScore;
  }
}
