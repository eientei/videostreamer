class extends lapis.Application
  [chat_pub: "/pub/:app/:name"]: =>
    ngx.req.read_body!
    body = ngx.req.get_body_data!
    if body
      msg = ChatManager\add_message util.unescape(@params.app),
                                    util.unescape(@params.name),
                                    ngx.var.remote_addr,
                                    @user.id,
                                    @user.email,
                                    body
      url = "/realpub/" .. util.unescape(@params.app) .. "_" .. util.unescape(@params.name)
      ngx.location.capture(url, { method: ngx.HTTP_POST, body: to_json(msg) })
      return render: false, json: msg
    render: "error", status: 500
   
  [chat_status: "/status/:app/:name"]: =>
    author = ngx.var.remote_addr
    if @user.email
      author = @user.email
    hash = ngx.md5(author)
    usrs = ChatManager\get_active_users util.unescape(@params.app), util.unescape(@params.name), hash
    render: false, json: usrs


  [chat_history: "/history/:app/:name"]: =>
    msgs = ChatManager\get_last_history util.unescape(@params.app), util.unescape(@params.name)
    if msgs and next(msgs)
      return render: false, json: msgs
    render: "error", status: 500


  [chat_prev_history: "/history/:app/:name/:postid/before"]: =>
    msgs = ChatManager\get_previous_history util.unescape(@params.app),
                                            util.unescape(@params.name),
                                            tonumber(@params.postid)
    if msgs and next(msgs)
      return render: false, json: msgs
    render: "error", status: 500
  
  [chat_prev_history_signle: "/history/:app/:name/:postid"]: =>
    msg = ChatManager\get_previous_single util.unescape(@params.app),
                                          util.unescape(@params.name),
                                          tonumber(@params.postid)
    if msg and next(msg)
      return render: false, json: msg
    render: "error", status: 500
