package mitll.langtest.shared.scoring;

import com.google.gwt.user.client.rpc.IsSerializable;

public class RecalcRefResponse implements IsSerializable {

  private RecalcResponses recalcRefResponse;
  private int num = 0;

  public RecalcRefResponse() {
  }

  public RecalcRefResponse(RecalcResponses recalcRefResponse) {
    this.recalcRefResponse = recalcRefResponse;
  }

  public RecalcRefResponse(RecalcResponses recalcRefResponse, int num) {
    this(recalcRefResponse);
    this.num = num;
  }

  public RecalcResponses getRecalcRefResponse() {
    return recalcRefResponse;
  }

  public int getNum() {
    return num;
  }
}
