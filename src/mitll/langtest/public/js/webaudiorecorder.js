function __log(e, data) {
    //log.innerHTML += "\n" + e + " " + (data || '');
     $('#status').append("<p>"+e + "  at " + new Date().getTime());
}

var audio_context;
var recorder;

function startUserMedia(stream) {
    var input = audio_context.createMediaStreamSource(stream);
    __log('Media stream created.');

    input.connect(audio_context.destination);
    __log('Input connected to audio context destination.');

    recorder = new Recorder(input);
    __log('Recorder initialised.');

    webAudioMicAvailable();
}

function startRecording(button) {
    recorder && recorder.record();
    button.disabled = true;
    button.nextElementSibling.disabled = false;
    __log('Recording...');
}

function stopRecording(button) {
    recorder && recorder.stop();
    button.disabled = true;
    button.previousElementSibling.disabled = false;
    __log('Stopped recording.');

    // create WAV download link using audio data blob
    createDownloadLink();

    recorder.clear();
}

function initWebAudio() {
    try {
        // webkit shim
        window.AudioContext = window.AudioContext || window.webkitAudioContext;
        navigator.getMedia = ( navigator.getUserMedia ||
            navigator.webkitGetUserMedia ||
            navigator.mozGetUserMedia ||
            navigator.msGetUserMedia);
        window.URL = window.URL || window.webkitURL;

        audio_context = new AudioContext;
        __log('Audio context set up.');
        __log('navigator.getUserMedia ' + (navigator.getMedia ? 'available.' : 'not present!'));
    } catch (e) {
        alert('No web audio support in this browser!');
        webAudioMicNotAvailable();
    }

    navigator.getMedia({audio: true}, startUserMedia, function(e) {
        __log('No live audio input: ' + e);
        webAudioMicNotAvailable();
    });
}
