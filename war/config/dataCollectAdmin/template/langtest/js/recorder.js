
function microphone_recorder_events()
{
  $('#status').append("<p>Microphone recorder event: " + arguments[0] + "  at " + new Date().getTime());

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

/*  case "microphone_activity":
    $('#activity_level').text(arguments[1]);
      $('#status').css({'color': '#000'}).append("<p> activity - " + arguments[1] + " at " + new Date().getTime());
      break;*/

  case "recording":
    var name = arguments[1];
    $('#status').css({'color': '#000'}).append("<p> recording - " + name + " at " + new Date().getTime());
    break;

  case "recording_stopped":
    var name = arguments[1];
    var duration = arguments[2];
    $('#status').css({'color': '#000'}).append("<p> recording_stopped - " + name + " Duration: " + duration + "  at " + new Date().getTime());
    break;
  }
}

Recorder = {
  recorder: null,
  uploadFormId: "#uploadForm",
  uploadFieldName: "upload_file[filename]",
  permitCalled: 0,

  connect: function(name, attempts) {
    $('#status').css({'color': '#0F0'}).append("<p>connect called:  at " + new Date().getTime());

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
      $('#status').css({'color': '#0F0'}).append("<p>calling permit at " + new Date().getTime());
      Recorder.recorder.permit();
      return;
    }

    setTimeout(function() {Recorder.connect(name, attempts+1);}, 100);
  },

  record: function(name, filename) {
      $('#status').css({'color': '#0F0'}).append("<p>record at " + new Date().getTime());
      Recorder.recorder.record(name, filename);
  },

  hide2:function () {
    Recorder.recorder.width = 8 + "px";
    Recorder.recorder.height = 8 + "px";
  },

  show: function() {
    Recorder.recorder.show();
  },

  hide: function() {
    Recorder.recorder.hide();
  },

  stop: function() {
      $('#status').css({'color': '#0F0'}).append("<p>stop at " + new Date().getTime());
      Recorder.recorder.stopRecording();
  },

  getWav: function() {
    return Recorder.recorder.getwavbase64();
  },

  showPermission: function() {
    Recorder.recorder.permit();
  }

  /*,

  showPermissionWindow: function() {
      $('#upload_status').css({'color': '#0F0'}).append(" permit called: ");

    // need to wait until app is resized before displaying permissions screen
      setTimeout(function(){Recorder.recorder.permit();}, 1);
  }*/
}
