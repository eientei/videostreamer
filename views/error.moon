import Widget from require "lapis.html"

class extends Widget
  content: =>
    @content_for "title", "Error #{@code}"
    div class: "root-container", ->
      div class: "inner-container", ->
        div class: "fancybox", ->
          h1 ->
            text "Error #{@code} - #{@message}"
    
