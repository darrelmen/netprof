function __log(e, data) {
    //log.innerHTML += "\n" + e + " " + (data || '');
    $('#status').append("<p>"+e + "  at " + new Date().getTime());
}

var audio_context;
var recorder;
var rememberedInput;

function startUserMedia(stream) {
    var input = audio_context.createMediaStreamSource(stream);
    __log('Media stream created.');

    //input.connect(audio_context.destination);
    __log('Input connected to audio context destination.');

    recorder = new Recorder(input);
    __log('Recorder initialised.');

    rememberedInput = input;
    webAudioMicAvailable();
    document.addEventListener('webkitvisibilitychange', onVisibilityChange);
}
/*
function gotStreamOld(stream) {
    var inputPoint = audio_context.createGain();
    __log('inputPoint gain ' + inputPoint);

    // Create an AudioNode from the stream.
    var realAudioInput = audio_context.createMediaStreamSource(stream);
    __log('Media stream created.');

    inputPoint.connect(realAudioInput);
    rememberedInput = realAudioInput;

    recorder = new Recorder( inputPoint );
    __log('Recorder initialised.');

    webAudioMicAvailable();
    document.addEventListener('webkitvisibilitychange', onVisibilityChange);
}*/

/*
function gotStream(stream) {
    //var inputPoint = audio_context.createGain();

    // Create an AudioNode from the stream.
    var realAudioInput = audio_context.createMediaStreamSource(stream);
    var  audioInput = realAudioInput;

    //audioInput.connect(audioInput);
    //audioInput.connect(audio_context.destination);


//    audioInput = convertToMono( input );

    //analyserNode = audioContext.createAnalyser();
    // analyserNode.fftSize = 2048;
    //inputPoint.connect( analyserNode );

    recorder = new Recorder( audioInput );
    __log('Recorder initialised.');

    var zeroGain = audio_context.createGain();
    zeroGain.gain.value = 0.0;
    audioInput.connect( zeroGain );
    zeroGain.connect( audio_context.destination );

    webAudioMicAvailable();
}
*/


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

};

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

};

function grabWav() {
    recorder && recorder.exportWAV(function (blob) {
        __log('Got ... wav data!');
        try {
            var reader = new FileReader();

            var arrayBuffer;
            reader.onloadend = function () {
                arrayBuffer = reader.result;

                var myArray = new Uint8Array(arrayBuffer);

                __log('5 Got ... array ' + myArray);

                var bytes = bytesToBase64(myArray);
                var length = bytes.length;

                __log('8 Got ... wav data!' + length);

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
        console.error(e);
        webAudioMicNotAvailable();
    });
}
