package mitll.langtest.server.database.exercise;

public enum ProjectProperty {
  WEBSERVICE_HOST("webserviceHost"),
  WEBSERVICE_HOST_PORT("webserviceHostPort"),
  SHOW_ON_IOS("showOniOS"),
  AUDIO_PER_PROJECT("audioPerProject"),
  MODELS_DIR("MODELS_DIR")
  ;

  String name;

  ProjectProperty(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
