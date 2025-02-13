package mitll.langtest.client.exercise;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by go22670 on 11/18/16.
 */
public class DefectEvent extends GwtEvent<DefectEventHandler> {
  public static Type<DefectEventHandler> TYPE = new Type<DefectEventHandler>();
  String source;
  public DefectEvent(String source) { this.source = source;}

  public String getSource() {return source;}
  @Override
  public Type<DefectEventHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(DefectEventHandler handler) {
    handler.onDefectChanged(this);
  }
}
