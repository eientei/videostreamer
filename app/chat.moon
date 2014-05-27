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
      rprint msg
    render: false
    

  [chat_history: "/history/:app/:name"]: =>
    msgs = ChatManager\get_last_history @params.app, @params.name
    ids = {}
    if msgs
      for v in *msgs
       table.insert ids, v.id
    rprint ids
    render: false

  [chat_prev_history: "/history/:app/:name/:postid"]: =>
    msgs = ChatManager\get_previous_history @params.app,
                                            @params.name,
                                            tonumber(@params.postid)
    ids = {}
    if msgs
      for v in *msgs
       table.insert ids, v.id
    rprint ids
    render: false


