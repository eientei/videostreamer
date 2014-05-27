class extends lapis.Application
  [chat_pub: "/pub/:app/:name"]: =>
    ngx.req.read_body!
    body = ngx.req.get_body_data!
    if body != nil
      msg = ChatManager\add_message @params.app,
                                    @params.name,
                                    ngx.var.remote_addr,
                                    @user.id,
                                    @user.email,
                                    body
      url = "/realpub/" .. @params.app .. "/" .. @params.name
      rprint to_json(msg)
      ngx.location.capture(url, { method: ngx.HTTP_POST, body: to_json(msg) })
      render: false, json: msg
    render: false
    

  [chat_history: "/history/:app/:name"]: =>
    msgs = ChatManager\get_last_history @params.app, @params.name
    render: false, json: msgs

  [chat_prev_history: "/history/:app/:name/:postid/before"]: =>
    msgs = ChatManager\get_previous_history @params.app,
                                            @params.name,
                                            tonumber(@params.postid)
    render: false, json: msgs
  
  [chat_prev_history_signle: "/history/:app/:name/:postid"]: =>
    msg = ChatManager\get_previous_single @params.app,
                                          @params.name,
                                          tonumber(@params.postid)
    render: false, json: msg
