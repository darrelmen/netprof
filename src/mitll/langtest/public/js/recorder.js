
function microphone_recorder_events()
{
  $('#status').append("<p>Microphone recorder event: " + arguments[0]);

  switch(arguments[0]) {
  case "ready":
    Recorder.connect("recorderApp", 0);
    break;

  case "no_microphone_found":
    break;

  case "microphone_user_request":
    break;

  case "microphone_connected":
    var mic = arguments[1];
    micConnected();

    $('#status').css({'color': '#000'}).append("<p>Microphone: " + mic.name);
    break;

  case "microphone_not_connected":
    micNotConnected();
    break;

  case "microphone_activity":
    $('#activity_level').text(arguments[1]);
    break;

  case "recording":
    var name = arguments[1];
    break;

  case "recording_stopped":
    var name = arguments[1];
    var duration = arguments[2];
    break;

  case "playing":
    var name = arguments[1];
    break;

  case "playback_started":
    var name = arguments[1];
    var latency = arguments[2];
    break;

  case "stopped":
    var name = arguments[1];
    playbackStopped();
    break;

  case "save_pressed":
    break;

  case "saving":
    var name = arguments[1];
    break;

  case "saved":
    var name = arguments[1];
    var data = $.parseJSON(arguments[2]);
    gotSaveComplete();
    if(data.saved) {
      $('#upload_status').css({'color': '#0F0'}).text(name + " was saved");
    } else {
      $('#upload_status').css({'color': '#F00'}).text(name + " was not saved");
    }
    break;

  case "save_failed":
    var name = arguments[1];
    var errorMessage = arguments[2];
    $('#upload_status').css({'color': '#F00'}).text(name + " failed: " + errorMessage);
    break;

  case "save_progress":
    var name = arguments[1];
    var bytesLoaded = arguments[2];
    var bytesTotal = arguments[3];
    $('#upload_status').css({'color': '#000'}).text(name + " progress: " + bytesLoaded + " / " + bytesTotal);
    break;
  }
}

Recorder = {
  recorder: null,
  uploadFormId: "#uploadForm",
  uploadFieldName: "upload_file[filename]",
  permitCalled: 0,

  connect: function(name, attempts) {
    $('#status').css({'color': '#0F0'}).append("<p>connect called: ");

    if(navigator.appName.indexOf("Microsoft") != -1) {
      Recorder.recorder = window[name];
    } else {
      Recorder.recorder = document[name];
    }

    if(attempts >= 40) {
      return;
    }

    // flash app needs time to load and initialize
    if(Recorder.recorder && Recorder.recorder.init) {
      $('#status').css({'color': '#0F0'}).append("<p>calling permit");
      Recorder.recorder.permit();
      return;
    }

    setTimeout(function() {Recorder.connect(name, attempts+1);}, 100);
  },

  playBack: function(name) {
    Recorder.recorder.playBack(name);
  },

  record: function(name, filename) {
    Recorder.recorder.record(name, filename);
  },

  show: function() {
    Recorder.recorder.show();
  },

  hide: function() {
    Recorder.recorder.hide();
  },

  updateForm: function() {
    var frm = $(Recorder.uploadFormId);
    Recorder.recorder.update(frm.serializeArray());
  },

  stop: function() {
    Recorder.recorder.record("", "");
  },

  getWav: function() {
    return Recorder.recorder.getwavbase64();
  },

  showPermission: function() {
    Recorder.recorder.permit();
  },

  showPermissionWindow: function() {
      $('#upload_status').css({'color': '#0F0'}).append(" permit called: ");

    // need to wait until app is resized before displaying permissions screen
      setTimeout(function(){Recorder.recorder.permit();}, 1);
  }
}
