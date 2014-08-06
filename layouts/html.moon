import Widget from require "lapis.html"

class HtmlLayout extends Widget
  content: =>
    html_5 ->
      head ->
        meta charset: "UTF-8"
        title ->
          if @title
            text @title .. " - Yukkuri Video Streamer"
          else
            text "Yukkuri Video Streamer"
        link rel: "stylesheet", href: "/css/style.css"
        if @has_content_for "script"
          script type: "application/javascript", src: "/js/jquery-1.11.0.js"
          script type: "application/javascript", src: "/js/jquery-ui-1.10.4.custom.js"
          script type: "application/javascript", src: "/js/jquery.cookie.js"
          @content_for "script"
      body ->
        div class: "header", ->
          div class: "right", ->
            if @has_content_for "controls"
              @content_for "controls"
            a href: @url_for("play_index"), ->
              text "Home"
            a href: @url_for("info"), ->
              text "Info"
            if @user.name == "Anonymous"
              a href: @url_for("user_login"), ->
                text "Login"
              a href: @url_for("user_signup"), ->
                text "Signup"
            else
              a href: @url_for("user_profile"), ->
                text "Profile"
              a href: @url_for("user_logout"), ->
                text "Logout"
        @content_for "inner"

