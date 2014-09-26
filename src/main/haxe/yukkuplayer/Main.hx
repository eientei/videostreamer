package yukkuplayer;

import flash.text.TextField;
import flash.display.MovieClip;
import flash.display.Stage;
import flash.Lib;
import flash.system.Capabilities;

class Main {
    static var stage : Stage;
    static var movieClip : MovieClip;
    static var trace : TextField;

    static function initTrace() {
        trace = new flash.text.TextField();
        trace.y = 20;
        trace.thickness = 1;
        trace.width = stage.stageWidth;
        trace.height = stage.stageHeight;
        trace.selectable = true;
        trace.textColor = 0xFFFFFF;
        trace.mouseEnabled = false;
    }

    static function doTrace( v : Dynamic, ?pos : haxe.PosInfos ) {
        trace.text += pos.fileName+"("+pos.lineNumber+") : "+Std.string(v)+"\n";
        flash.Lib.current.addChild(trace);
    }

    static function main() {
        stage = Lib.current.stage;
        movieClip = Lib.current;

        initTrace();
        haxe.Log.trace = doTrace;

        var parameters:Dynamic<String> = flash.Lib.current.loaderInfo.parameters;
        var videoUrl = parameters.videoUrl;
        var buffer = Std.parseFloat(parameters.buffer);
        var player : VideoPlayer = new VideoPlayer(videoUrl, buffer);
    }
}
