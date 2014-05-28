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
      url = "/realpub/" .. @params.app .. "_" .. @params.name
      submsg = {
        type: "message"
        data: msg
      }
      ngx.location.capture(url, { method: ngx.HTTP_POST, body: to_json(submsg) })
      render: false, json: submsg
    render: "error", status: 500
    

  [chat_history: "/history/:app/:name"]: =>
    msgs = ChatManager\get_last_history @params.app, @params.name
    if msgs
      return render: false, json: msgs
    render: "error", status: 500


  [chat_prev_history: "/history/:app/:name/:postid/before"]: =>
    msgs = ChatManager\get_previous_history @params.app,
                                            @params.name,
                                            tonumber(@params.postid)
    if msgs
      return render: false, json: msgs
    render: "error", status: 500
  
  [chat_prev_history_signle: "/history/:app/:name/:postid"]: =>
    msg = ChatManager\get_previous_single @params.app,
                                          @params.name,
                                          tonumber(@params.postid)
    render: false, json: msg
    if msg
      return render: false, json: msg
    render: "error", status: 500
