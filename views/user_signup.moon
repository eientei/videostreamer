import Widget from require "lapis.html"

class extends Widget
  content: =>
    @content_for "title", "Signup"
    @content_for "script", ->
      script type: "application/javascript", src: "http://www.google.com/recaptcha/api/js/recaptcha_ajax.js"
      script type: "application/javascript", src: "/js/signup.js"
    div class: "root-container", ->
      div class: "inner-container", ->
        div class: "fancybox", ->
          if @errors
            div class: "errors", ->
              ul ->
                for err in *@errors
                  li ->
                    text err
          form method: "POST", action: @url_for("user_signup"), ->
            input type: "hidden", name: "csrf_token", value: @csrf_token
            element "table", ->
              tr ->
                td ->
                  input { type: "text", name: "login",
                          value: @params.login }
                td ->
                  label for: "login", "login (3..64 chars length)"
              tr ->
                td ->
                  input { type: "password", name: "password",
                          value: @params.password }
                td ->
                  label for: "password", "password (3..64 chars length)"
              tr ->
                td ->
                  input { type: "password", name: "repeat_password",
                          value: @params.repeat_password }
                td ->
                  label for: "repeat_password", "repeat password"
              tr ->
                td ->
                  input { type: "text", name: "email",
                          value: @params.email }
                td ->
                  label for: "email", "email (optional)"
              tr ->
                td colspan: "2", ->
                  div id: "captcha"
            input type: "submit"

    
