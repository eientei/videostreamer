import Widget from require "lapis.html"

class extends Widget
  content: =>
    @content_for "title", "Info @ How To Stream"
    div class: "root-container", ->
      div class: "inner-container", ->
        div class: "fancybox", ->
          h1 ->
            text "How to stream"
          h2 ->
            text "Streaming your desktop"
          h3 ->
            text "ffmpeg (unix)"
          div class: "code", ->
            text "ffmpeg -f x11grab -s 1366x768 -i :0.0 -f alsa -ac 2 -i pulse -vcodec libx264 -preset veryfast -pix_fmt yuv420p -ar 44100 -threads 0 -f flv rtmp://" .. config.rtmp_host .. "/" .. config.default_app .. "/00000000000000000000000000000000"
          p ->
            text "This will run ffmpeg grabbing display of size 1366x768 using pulseaudio audio source (configurable in pavucontrol) with reasonable defaults for live streaming. rtmp://" .. config.rtmp_host .. "/" .. config.default_app .. "/00000000000000000000000000000000 is your rtmp publish url as seen in your profile."
          h3 ->
            text "ffmpeg (windows)"
          div class: "cpde" ,->
            text "ffmpeg -f dshow -i video=\"screen-capture-recorder\" -vcodec libx264 -preset veryfast -pix_fmt yuv420p -ar 44100 -threads 0 -f flv rtmp://" .. config.rtmp_host .. "/" .. config.default_app .. "/00000000000000000000000000000000"
          p ->
            text "This will run ffmpeg grabbing dshow device."
          p ->
            text "More info on ffmpeg is available here: "
            a href: "https://trac.ffmpeg.org/wiki/How%20to%20grab%20the%20desktop%20(screen)%20with%20FFmpeg", ->
              text "https://trac.ffmpeg.org/wiki/How%20to%20grab%20the%20desktop%20(screen)%20with%20FFmpeg"

          h2 ->
            text "OpenBroadcaster (OBS, windows)"
          p ->
            text "Grab OBS here: "
            a href: "https://obsproject.com", ->
              text "https://obsproject.com"
            text " And adjust following options: "
            ul ->
              li ->
                text "Encoding"
                ul ->
                  li ->
                    text "Max Bitrate: 2048 Kb/s"
                  li ->
                    text "Buffer Size: 2048 Kb/s"
                  li ->
                    text "Quality balance: 0"
              li ->
                text "Broadcast Settings"
                ul ->
                  li ->
                    text "Mode: Live Stream"
                  li ->
                    text "Streaming Service: Custom"
                  li ->
                    text "Server: rtmp://" .. config.rtmp_host .. "/" .. config.default_app
                  li ->
                    text "Play Path/Stream Key: 00000000000000000000000000000000"
                  li ->
                    text "Auto Reconnect: Checked"
                  li ->
                    text "Auto-Reconnect Timeout: 1"
              li ->
                text "Video"
                ul ->
                  li ->
                    text "Base Resolution: Your Resolution or Monitor"
                    p ->
                      text "If your display is big (>=1080p) it is recommended to have a apply a downscale to 720p, using Resolution Downscale option on this window and setting Filter to Lanczos"
                  li ->
                    text "FPS: 30"
              li ->
                text "Advanced"
                ul ->
                  li ->
                    text "Use Multithread Optimizations: Checked"
                  li ->
                    text "Process Priority Class: High"
                  li ->
                    text "x264 CPU Preset: Most likely 'superfast'"
                    p ->
                      text "If you have too bad image or having slow network channel, consider using 'veryfast' or even 'faster'; but if you have CPU hit either on your machine or clients, try 'ultrafast' and if you have good network channel, you could try also rising bitrate (on Encoding tab) up to 4096"
                  li ->
                    text "Use CFR: Checked"
                  li ->
                    text "Custom x264 Encoder Settings: -tune zerolatency"
                  li ->
                    text "Force desktop audio to use video timestamps: Checked"
