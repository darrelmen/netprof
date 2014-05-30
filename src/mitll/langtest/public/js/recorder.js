function microphone_recorder_events()
{
  $('#status').append("<p>Microphone recorder event: " + arguments[0] + "  at " + new Date().getTime());

  switch(arguments[0]) {
  case "ready":
      $('#status').css({'color': '#000'}).append("<p>ready: ");
      try {
         //console.log("got ready");

          FlashRecorderLocal.connect("recorderApp", 0);
      } catch (e) {
          console.error("got " +e);
      }
    break;

  case "no_microphone_found":
      $('#status').css({'color': '#000'}).append("<p>no_microphone_found: ");
      noMicrophoneFound();
      break;

  case "microphone_user_request":
      $('#status').css({'color': '#000'}).append("<p>microphone_user_request: ");
      break;

  case "microphone_connected":
    var mic = arguments[1];
      //console.log("got microphone_connected");

      //    if (FlashRecorderLocal.isMicrophoneAvailable()) {
          $('#status').css({'color': '#000'}).append("<p>Microphone: " + mic.name);
          micConnected();
/*      }
      else {
          $('#status').css({'color': '#000'}).append("<p>Microphone: not connected " + mic.name);

          micNotConnected();
      }*/
    break;

  case "microphone_not_connected":
      //console.log("got microphone_not_connected");

      $('#status').css({'color': '#000'}).append("<p>microphone_not_connected: ");
      //if (FlashRecorderLocal.permitCalled > 0) {
          micNotConnected();
    //  }
    //  else {
          $('#status').css({'color': '#0F0'}).append("<p># permit is " + FlashRecorderLocal.permitCalled);

    //  }
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


FlashRecorderLocal = {
    recorder: null,
    uploadFormId: "#uploadForm",
    uploadFieldName: "upload_file[filename]",
    permitCalled: 0,


    detectIE: function () {
        var ua = window.navigator.userAgent;
        var msie = ua.indexOf('MSIE ');
        var trident = ua.indexOf('Trident/');

        if (msie > 0) {
            // IE 10 or older => return version number
            return parseInt(ua.substring(msie + 5, ua.indexOf('.', msie)), 10);
        }

        if (trident > 0) {
            // IE 11 (or newer) => return version number
            var rv = ua.indexOf('rv:');
            return parseInt(ua.substring(rv + 3, ua.indexOf('.', rv)), 10);
        }

        // other browser
        return false;
    },

    connect: function (name, attempts) {
        $('#status').css({'color': '#0F0'}).append("<p>connect called:  at " + new Date().getTime());

        if(navigator.appName.indexOf("Microsoft") != -1) {
            FlashRecorderLocal.recorder = window[name];
        } else {
            FlashRecorderLocal.recorder = document[name];
        }

        if(attempts >= 40) {
            return;
        }

        // flash app needs time to load and initialize
        if(FlashRecorderLocal.recorder && FlashRecorderLocal.recorder.init) {
            $('#status').css({'color': '#0F0'}).append("<p>calling permit at " + new Date().getTime());
          //  FlashRecorderLocal.permitCalled = FlashRecorderLocal.permitCalled + 1;
            if (this.detectIE()) {
                FlashRecorderLocal.recorder.showPrivacy();
            }
            else {
                FlashRecorderLocal.recorder.permit();
            }
            return;
        }

        setTimeout(function() {Recorder.connect(name, attempts+1);}, 100);
    },

    record: function(name, filename) {
        $('#status').css({'color': '#0F0'}).append("<p>record at " + new Date().getTime());
        FlashRecorderLocal.recorder.record(name, filename);
    },

    hide2:function () {
        FlashRecorderLocal.recorder.width = 8 + "px";
        FlashRecorderLocal.recorder.height = 8 + "px";
    },

    show: function() {
        FlashRecorderLocal.recorder.show();
    },

    hide: function() {
        FlashRecorderLocal.recorder.hide();
    },

    stop: function() {
        $('#status').css({'color': '#0F0'}).append("<p>stop at " + new Date().getTime());
        FlashRecorderLocal.recorder.stopRecording();
    },

    getWav: function() {
        return FlashRecorderLocal.recorder.getwavbase64();
    },

    isMicrophoneAvailable: function() {
        return FlashRecorderLocal.recorder.isMicrophoneAvailable();
    },

    showPermission: function() {
        FlashRecorderLocal.recorder.permit();
    },

    showPrivacy: function() {
        FlashRecorderLocal.recorder.showPrivacy();
    }

    /*,
     showPermissionWindow: function() {
     $('#upload_status').css({'color': '#0F0'}).append(" permit called: ");

     // need to wait until app is resized before displaying permissions screen
     setTimeout(function(){FlashRecorderLocal.recorder.permit();}, 1);
     }*/
};
