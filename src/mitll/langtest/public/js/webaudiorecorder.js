function __log(e, data) {
    $('#status').append("<p>"+e + "  at " + new Date().getTime());
}

var audio_context;
var recorder;
var rememberedInput;

// called from initWebAudio
function startUserMedia(stream) {
    var input = audio_context.createMediaStreamSource(stream);
    __log('Media stream created.');

    recorder = new Recorder(input);
    __log('Recorder initialised.');

    rememberedInput = input;
    webAudioMicAvailable();
    document.addEventListener('webkitvisibilitychange', onVisibilityChange);
}

function onVisibilityChange() {
    if (document.webkitHidden) {
        __log('webkitHidden');

        rememberedInput.stop(0);
    } else {
        __log('webkitRevealed');

        rememberedInput.start(0);
    }
}

function startRecording() {
    recorder.clear();
    recorder && recorder.record();
    __log('Recording...');
}

function stopRecording() {
    recorder && recorder.stop();
    __log('Stopped recording.');

    // get WAV from audio data blob
    grabWav();
}

function uint6ToB64 (nUint6) {

    return nUint6 < 26 ?
        nUint6 + 65
        : nUint6 < 52 ?
        nUint6 + 71
        : nUint6 < 62 ?
        nUint6 - 4
        : nUint6 === 62 ?
        43
        : nUint6 === 63 ?
        47
        :
        65;

}

function bytesToBase64(aBytes) {

    var sB64Enc = "";

    for (var nMod3, nLen = aBytes.length, nUint24 = 0, nIdx = 0; nIdx < nLen; nIdx++) {
        nMod3 = nIdx % 3;
        if (nIdx > 0 && (nIdx * 4 / 3) % 76 === 0) { sB64Enc += "\r\n"; }
        nUint24 |= aBytes[nIdx] << (16 >>> nMod3 & 24);
        if (nMod3 === 2 || aBytes.length - nIdx === 1) {
            sB64Enc += String.fromCharCode(uint6ToB64(nUint24 >>> 18 & 63), uint6ToB64(nUint24 >>> 12 & 63), uint6ToB64(nUint24 >>> 6 & 63), uint6ToB64(nUint24 & 63));
            nUint24 = 0;
        }
    }

    return sB64Enc.replace(/A(?=A$|$)/g, "=");

}

function grabWav() {
    recorder && recorder.exportWAV(function (blob) {
        try {
            var reader = new FileReader();

            var arrayBuffer;
            reader.onloadend = function () {
                arrayBuffer = reader.result;

                var myArray = new Uint8Array(arrayBuffer);
                var bytes = bytesToBase64(myArray);
                getBase64(bytes);
            }

            reader.readAsArrayBuffer(blob);
        } catch (e) {
            __log('Bad call to blob');

            var vDebug = "";
            for (var prop in e)
            {
                vDebug += "property: "+ prop+ " value: ["+ e[prop]+ "]\n";
            }
            vDebug += "toString(): " + " value: [" + e.toString() + "]";
            __log(vDebug);
            throw e;
        }
    });
}

function initWebAudio() {
    var gotAudioContext = false;
    try {
        // webkit shim
        window.AudioContext = window.AudioContext || window.webkitAudioContext;
        navigator.getMedia = ( navigator.getUserMedia ||
            navigator.webkitGetUserMedia ||
            navigator.mozGetUserMedia ||
            navigator.msGetUserMedia);
        window.URL = window.URL || window.webkitURL;
       // __log('Audio context is something...');
        console.info("getting audio context...");

        //  __log('Audio context is '+window.AudioContext);

        audio_context = new AudioContext;
        gotAudioContext = true;
        __log('Audio context set up.');
        console.info('Audio context set up.');

        __log('navigator.getUserMedia ' + (navigator.getMedia ? 'available.' : 'not present!'));
    } catch (e) {
        __log('No web audio support in this browser!');
        console.error(e);
        webAudioMicNotAvailable();
    }

    if (gotAudioContext) {
        try {
            if (navigator.getMedia) {
                navigator.getMedia({audio: true}, startUserMedia, function (e) {
                    __log('No live audio input: ' + e);
                    if (e.name == "PermissionDeniedError") {
                        webAudioPermissionDenied();
                    }
                    console.error(e);
                    webAudioMicNotAvailable();
                });
            }
            else {
                webAudioMicNotAvailable();
            }
        } catch (e) {
            __log('No navigator.getMedia in this browser!');
            console.error(e);
            webAudioMicNotAvailable();
        }
    }
}
