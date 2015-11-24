/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

var recorderHasConsole = (window.console || console.log);
var recordingStart;

function microphone_recorder_events() {
    //$('#status').append("<p>Microphone recorder event: " + arguments[0] + "  at " + new Date().getTime());
    if (recorderHasConsole) {
        console.log("got event " + arguments[0] + " at " + new Date().getTime());
    }
    switch (arguments[0]) {
        case "ready":
            //  $('#status').css({'color': '#000'}).append("<p>ready: ");
            try {
                FlashRecorderLocal.connect("recorderApp", 0);
            } catch (e) {
                console.log("got error " + e);
            }
            break;

        case "no_microphone_found":
            //  $('#status').css({'color': '#000'}).append("<p>no_microphone_found: ");
            noMicrophoneFound();
            break;

        case "microphone_user_request":
            //  $('#status').css({'color': '#000'}).append("<p>microphone_user_request: ");
            break;

        case "microphone_connected":
            var mic = arguments[1];
            //  $('#status').css({'color': '#000'}).append("<p>Microphone: " + mic.name);
            micConnected();
            break;

        case "microphone_not_connected":
            //console.log("got microphone_not_connected");
            //  $('#status').css({'color': '#000'}).append("<p>microphone_not_connected: ");
            micNotConnected();
            break;

        case "recording":
            var name = arguments[1];
            //  $('#status').css({'color': '#000'}).append("<p> recording - " + name + " at " + new Date().getTime());
            recordingStart = new Date().getTime();
            break;

        case "recording_stopped":
            var name = arguments[1];
            var duration = arguments[2];
            //$('#status').css({'color': '#000'}).append("<p> recording_stopped - " + name + " Duration: " + duration + "  at " + new Date().getTime());
            if (recorderHasConsole) console.log("recording duration " + (new Date().getTime() - recordingStart))
            break;
    }
}

FlashRecorderLocal = {
    recorder: null,
    uploadFormId: "#uploadForm",
    uploadFieldName: "upload_file[filename]",
    permitCalled: 0,

    connect: function (name, attempts) {
        //$('#status').css({'color': '#0F0'}).append("<p>connect called:  at " + new Date().getTime());

        if (navigator.appName.indexOf("Microsoft") != -1) {
            FlashRecorderLocal.recorder = window[name];
        } else {
            FlashRecorderLocal.recorder = document[name];
        }

        if (attempts >= 40) {
            return;
        }

        if (recorderHasConsole) {
            console.log("about to show showPrivacy");
        }

        // flash app needs time to load and initialize
        if (FlashRecorderLocal.recorder /*&& FlashRecorderLocal.recorder.init*/) {
            //$('#status').css({'color': '#0F0'}).append("<p>calling permit at " + new Date().getTime());
            if (recorderHasConsole) {
                console.log("FlashRecorderLocal.showPrivacy");
            }
            FlashRecorderLocal.recorder.showPrivacy();
            return;
        }
        else {
            if (recorderHasConsole) {
                console.log("no recorder?");
            }
        }

        setTimeout(function () {
            if (recorderHasConsole) {
                console.log("-- trying again - waiting for Flash to install recorder");
            }
            FlashRecorderLocal.connect(name, attempts + 1);
        }, 100);
    },

    record: function (name, filename) {
        //$('#status').css({'color': '#0F0'}).append("<p>record at " + new Date().getTime());
        FlashRecorderLocal.recorder.record(name, filename);
    },

    hide2: function () {
        FlashRecorderLocal.recorder.width = 8 + "px";
        FlashRecorderLocal.recorder.height = 8 + "px";
    },

    show: function () {
        FlashRecorderLocal.recorder.show();
    },

    hide: function () {
        FlashRecorderLocal.recorder.hide();
    },

    stop: function () {
        //$('#status').css({'color': '#0F0'}).append("<p>stop at " + new Date().getTime());
        FlashRecorderLocal.recorder.stopRecording();
    },

    getWav: function () {
        return FlashRecorderLocal.recorder.getwavbase64();
    },

    isMicrophoneAvailable: function () {
        return FlashRecorderLocal.recorder.isMicrophoneAvailable();
    },

    showPermission: function () {
        FlashRecorderLocal.recorder.permit();
    },

    showPrivacy: function () {
        FlashRecorderLocal.recorder.showPrivacy();
    }
};
