package mitll.langtest.shared.project;

import com.google.gwt.user.client.rpc.IsSerializable;

public enum ProjectProperty implements IsSerializable {
  WEBSERVICE_HOST("webserviceHost", PropertyType.STRING),
  WEBSERVICE_HOST_PORT("webserviceHostPort", PropertyType.INTEGER),
  SHOW_ON_IOS("showOniOS", PropertyType.BOOLEAN),
  AUDIO_PER_PROJECT("audioPerProject", PropertyType.BOOLEAN),
  MODELS_DIR("MODELS_DIR", PropertyType.STRING),
  REPORT_LIST("reportList", PropertyType.LIST);

  public enum PropertyType implements IsSerializable {
    STRING, LIST, BOOLEAN, INTEGER;
  }

  private String name;
  private PropertyType type;

  ProjectProperty(String name, PropertyType type) {
    this.name = name;
    this.type = type;
  }

  public String getName() {
    return name;
  }
  public PropertyType getType() {
    return type;
  }
}
