import Widget from require "lapis.html"

class extends Widget
  content: =>
    code = @res.status
    messages = {
      [404]: "Not Found"
      [403]: "Access Denied"
      [401]: "Not Authorized"
      [500]: "Server Error"
    }
    message = messages[code] or messages[500]
    @content_for "title", "Error #{code}"
    div class: "root-container", ->
      div class: "inner-container", ->
        div class: "fancybox", ->
          h1 ->
            text "Error #{code} - #{message}"
    
