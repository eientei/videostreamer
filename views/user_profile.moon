import Widget from require "lapis.html"

class extends Widget
  content: =>
    @content_for "title", "Profile"
    div class: "root-container", ->
      div class: "inner-container", ->
        div class: "fancybox profile", ->
          if @notices
            div class: "notices", ->
              ul ->
                for notice in *@notices
                  li ->
                    text notice
          if @errors
            div class: "errors", ->
              ul ->
                for err in *@errors
                  li ->
                    text err
          form method: "POST", action: @url_for("user_profile"), ->
            input type: "hidden", name: "csrf_token", value: @csrf_token
            element "table", ->
              tr ->
                td ->
                  input type: "password", name: "password"
                td ->
                  label for: "password", "password (3..64 chars length)"
              tr ->
                td ->
                  input type: "password", name: "repeat_password"
                td ->
                  label for: "repeat_password", "repeat password"
            input type: "submit", name: "update_password", value: "Update"

          form method: "POST", action: @url_for("user_profile"), ->
            input type: "hidden", name: "csrf_token", value: @csrf_token
            element "table", ->
              tr ->
                rawemail = @params.email or @user.email
                td ->
                  input { type: "text", name: "email",
                          value: rawemail }
                td ->
                  label for: "email", "email"
                  hashedemail = ngx.md5 rawemail
                  image_source = "http://www.gravatar.com/avatar/"
                  image_source ..= hashedemail .. "?d=identicon&s=32"
                  text " ( "
                  img src: image_source, alt: hashedmail
                  text " ) "
            input type: "submit", name: "update_email", value: "Update"
          if @user.streams
            form method: "POST", action: @url_for("user_profile"), ->
              input type: "hidden", name: "csrf_token", value: @csrf_token
              ul ->
                streams = {}
                for stream_id, stream in pairs(@user.streams)
                  table.insert streams, { key: stream_id, value: stream }
                
                table.sort streams, (a,b) ->
                  return a.key < b.key
                  
                for v in *streams
                  stream_id = v.key
                  stream = v.value
                  li ->
                    div class: "stream", ->
                      div class: "token", ->
                        text stream.token
                      element "select", name: "app[" .. stream_id .. "]", ->
                        for app, v in pairs(config.apps)
                          if app == stream.app
                            option value: app, selected: "selected", ->
                              text app
                          else
                            option value: app, ->
                              text app
                      text " / "
                      input
                        type: "text",
                        name: "name[" .. stream_id .. "]",
                        value: stream.name
                      input
                        type: "submit",
                        name: "update[" .. stream_id .. "]",
                        value: "Update"
                      input
                        type: "submit",
                        name: "remove[" .. stream_id .. "]",
                        value: "Remove"
                      input
                        type: "submit",
                        name: "tokenupd[" .. stream_id .. "]",
                        value: "Update token"
                    div class: "url", ->
                      text "Publish url: "
                      a href: "#{@rtmp_base}/#{stream.app}/#{stream.token}", ->
                        text "#{@rtmp_base}/#{stream.app}/#{stream.token}"
                    div class: "url", ->
                      text "View url: "
                      a href: "#{@rtmp_base}/#{stream.app}/#{stream.name}", ->
                        text "#{@rtmp_base}/#{stream.app}/#{stream.name}"
                    div class: "url", ->
                      text "Web player url: "
                      a href: "#{@http_base}/#{stream.app}/#{stream.name}", ->
                        text "#{@http_base}/#{stream.app}/#{stream.name}"
              input type: "submit", name: "add", value: "Add"

