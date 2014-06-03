encoding = require "lapis.util.encoding"

lapis.serve class extends lapis.Application
  layout: require "layouts.html"
  views_prefix: "views"

  @include require("app.streamer")
  @include require("app.chat")
  @include require("app.user")

  @before_filter =>
    expires = "Expires="
    expires ..= os.date("!%a, %d %b ")
    expires ..= (tonumber os.date("!%Y"))+1
    expires ..= os.date("! %H:%M:%S GMT")
    @app.cookie_attributes = { expires }
    StreamManager\init_streams!

  @before_filter =>
    @host = @req.parsed_url["host"]
    @rtmp_base = "rtmp://"
    @http_base = "http://#{@host}"

    if @req.parsed_url["port"] and @req.parsed_url["port"] != 80
      @http_base ..= ":" .. @req.parsed_url["port"]
    
    rtmp_host = config.rtmp_host
    if rtmp_host
      @rtmp_base ..= rtmp_host
    else
      @rtmp_base ..= @host

    rtmp_port = config.rtmp_port
    if rtmp_port and rtmp_port != 1935
      @rtmp_base ..= ":#{rtmp_port}"


  @before_filter =>
    if @session.username
      @user = UserManager\get_user_by_name @session.username

      if not @user
        @user = UserManager\get_user_by_name "Anonymous"
        @session.username = nil
    else
      @user = UserManager\get_user_by_name "Anonymous"

  [info: "/info"]: =>
    render: "info"

  [remote_control: "/remote_control"]: respond_to {
    GET: =>
      render: "error", status: 401
    POST: =>
      action = encoding.decode_with_secret(@params.action)
      if action
        if action == "reset_streams"
          StreamManager\reset_streams!
          StreamManager\init_streams!
  }

  handle_404: =>
    render: "error", status: 404

  handle_error: (r,t) =>
    rprint {r,t}
