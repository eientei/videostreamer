class extends lapis.Application
  [chat_pub: "/pub/:app/:name"]: =>
    ngx.req.read_body!
    body = ngx.req.get_body_data!
    if body != nil
      msg = ChatManager\add_message @params.app,
                                    @params.name,
                                    ngx.var.remote_addr,
                                    @user.id,
                                    body
      rprint msg
    render: false
    

  [chat_history: "/history/:app/:name"]: =>
    render: false

