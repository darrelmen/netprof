
function microphone_recorder_events()
{
  $('#status').append("<p>Microphone recorder event: " + arguments[0]);

  switch(arguments[0]) {
  case "ready":
  //  var width = parseInt(arguments[1]);
  //  var height = parseInt(arguments[2]);
    //Recorder.uploadFormId = "#uploadForm";
   // Recorder.uploadFieldName = "upload_file[filename]";
    Recorder.connect("recorderApp", 0);
   // Recorder.recorderOriginalWidth = width;
   // Recorder.recorderOriginalHeight = height;
   // $('#play_button').css({'margin-left': width + 8});
/*
    $('#save_button').css({'width': width, 'height': height});
*/
  break;

  case "no_microphone_found":
    break;

  case "microphone_user_request":
    //Recorder.showPermissionWindow();
    break;

  case "microphone_connected":
    var mic = arguments[1];
   // Recorder.defaultSize();
    micConnected();

    $('#status').css({'color': '#000'}).append("<p>Microphone: " + mic.name);
    break;

  case "microphone_not_connected":
   // Recorder.defaultSize();
    micNotConnected();

    break;

  case "microphone_activity":
    $('#activity_level').text(arguments[1]);
    break;

  case "recording":
    var name = arguments[1];
//    Recorder.hide();
//    $('#record_button img').attr('src', 'images/stop.png');
  //  $('#play_button').hide();
    break;

  case "recording_stopped":
    var name = arguments[1];
    var duration = arguments[2];
//    Recorder.show();
 //   $('#record_button img').attr('src', 'images/record.png');
 //   $('#play_button').show();
    break;

  case "playing":
    var name = arguments[1];
 //   $('#record_button img').attr('src', 'images/record.png');
 //   $('#play_button img').attr('src', 'images/stop.png');
    break;

  case "playback_started":
    var name = arguments[1];
    var latency = arguments[2];
    break;

  case "stopped":
    var name = arguments[1];
//    $('#record_button img').attr('src', 'images/record.png');
 //   $('#play_button img').attr('src', 'images/play.png');
    break;

  case "save_pressed":
  //  Recorder.updateForm();
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
  //recorderOriginalWidth: 0,
  //recorderOriginalHeight: 0,
  uploadFormId: "#uploadForm",
  uploadFieldName: "upload_file[filename]",
  permitCalled: 0,

  connect: function(name, attempts) {
   // if (Recorder.recorder == null) {
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

      // Recorder.recorderOriginalWidth = Recorder.recorder.width;
   //   Recorder.recorderOriginalHeight = Recorder.recorder.height;
  /*    if(Recorder.uploadFormId && $) {
        var frm = $(Recorder.uploadFormId);
        Recorder.recorder.init(frm.attr('action').toString(), Recorder.uploadFieldName, frm.serializeArray());
      }*/
      Recorder.recorder.permit();
      return;
    }

    setTimeout(function() {Recorder.connect(name, attempts+1);}, 100);
  //  }
  },

  playBack: function(name) {
    Recorder.recorder.playBack(name);
  },

  record: function(name, filename) {
    Recorder.recorder.record(name, filename);
  },

/*  resize: function(width, height) {
    Recorder.recorder.width = width + "px";
    Recorder.recorder.height = height + "px";
  },*/

/*  defaultSize: function(width, height) {
   // Recorder.resize(Recorder.recorderOriginalWidth, Recorder.recorderOriginalHeight);
  },*/

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
  //  if (Recorder.permitCalled == 0) {
      $('#upload_status').css({'color': '#0F0'}).append(" permit called: ");

    //  Recorder.resize(240, 160);
    // need to wait until app is resized before displaying permissions screen
      setTimeout(function(){Recorder.recorder.permit();}, 1);
    //  Recorder.permitCalled = 1;
    /*  }
    else {
      $('#upload_status').css({'color': '#0F0'}).text(" permit not called: ");
    }*/
  }
}
