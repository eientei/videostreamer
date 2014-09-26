<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>


<div class="fancybox">
    <h1>How to stream</h1>
    <h2>Streaming your desktop</h2>
    <h3>ffmpeg (Unix)</h3>
    <div class="code">
        ffmpeg -r 30 -f x11grab -s 1366x768 -i :0.0 -vcodec libx264 -preset ultrafast -tune zerolatency -pix_fmt yuv420p -s 1366x768 -ar 44100 -threads 0 -f flv ${rtmpBase}/live/128c853bb99bfeb971f9f766c92326c8
    </div>
    <p>
        This will run ffmpeg grabbing display of size 1366x768 using pulseaudio audio source (configurable in pavucontrol) with reasonable defaults for live streaming. ${rtmpBase}/live/128c853bb99bfeb971f9f766c92326c8 is your rtmp publish url as seen in your profile.
    </p>
    <h3>ffmpeg (Windows)</h3>
    <div class="code">
        ffmpeg -f dshow -i video="screen-capture-recorder" -vcodec libx264 -preset ultrafast -tune zerolatency -pix_fmt yuv420p -ar 44100 -threads 0 -f flv ${rtmpBase}/live/128c853bb99bfeb971f9f766c92326c8
    </div>
    <p>
        This will run ffmpeg grabbing dshow device.
    </p>
    <p>
        More info on ffmpeg is available here:
        <a href="https://trac.ffmpeg.org/wiki/How%20to%20grab%20the%20desktop%20(screen)%20with%20FFmpeg">https://trac.ffmpeg.org/wiki/How%20to%20grab%20the%20desktop%20(screen)%20with%20FFmpeg</a>
    </p>

    <h2>OpenBroadcaster (aka OBS, Windows)</h2>
    <p>
        Grab OBS here: <a href="https://obsproject.com">https://obsproject.com</a>
        And use this settings file: TBA
    </p>
</div>