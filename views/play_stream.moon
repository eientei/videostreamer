import Widget from require "lapis.html"

class extends Widget
  content: =>
    @content_for "title", "#{@rtmp_pair}"
    @content_for "script", ->
      script type: "application/javascript", src: "/js/index.js"
