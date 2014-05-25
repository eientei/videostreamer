import Widget from require "lapis.html"

class extends Widget
  content: =>
    @content_for "title", "#{@rtmp_pair}"
    @content_for "script", ->
      script type: "application/javascript", src: "/js/stream.js"
    div class: "root-container", ->
      div class: "player-container", ->
        if @stream
          object {
            data: "/swf/yukkuplayer.swf",
            type: "application/x-shockwave-flash",
            width: "100%", height: "100%", ->
              buffer = @params.buffer
              if not buffer
                buffer = 1.0
              flash_vars = "videoUrl="
              flash_vars ..= @rtmp_base .. "/" .. @rtmp_pair
              flash_vars ..= "&buffer=" .. buffer
              param name: "allowFullscreen", value: "true"
              param name: "scale", value: "noscale"
              param name: "bgcolor", value: "#000000"
              param name: "wmode", value: "direct"
              param name: "flashvars", value: flash_vars
          }

        else
          div class: "vertical", ->
            div class: "horizontal", ->
              h1 ->
                text "Sorry, stream is offline"
      div class: "chat-container", ->
        div class: "chat-wrapper", ->
          div class: "chat-area"
          div class: "botbar", ->
            div class: "counter"
            dic class: "charleft"
          div class: "textareawrap", ->
            textarea ->
