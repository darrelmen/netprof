package mitll.langtest.shared.scoring;

import com.google.gwt.user.client.rpc.IsSerializable;

public enum RecalcResponses implements IsSerializable {
  WORKING("In progress..."),
  COMPLETED("Already completed."),
  ERROR("Error? No exercises in project."),
  STOPPED("Stopped");

  private String disp;

  RecalcResponses(String disp) {
    this.disp = disp;
  }

  public String getDisp() {
    return disp;
  }
}
