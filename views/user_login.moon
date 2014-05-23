import Widget from require "lapis.html"

class extends Widget
  content: =>
    @content_for "title", "Login"
    div class: "root-container", ->
      div class: "inner-container", ->
        div class: "fancybox", ->
          if @errors
            div class: "errors", ->
              ul ->
                for err in *@errors
                  li ->
                    text err
          form method: "POST", action: @url_for("user_login"), ->
            input type: "hidden", name: "csrf_token", value: @csrf_token
            element "table", ->
              tr ->
                td ->
                  input type: "text", name: "login", value: @params.login
                td ->
                  label for: "login", "login"
              tr ->
                td ->
                  input type: "password", name: "password"
                td ->
                  label for: "password", "password"
            input type: "submit"

    
