class extends lapis.Application
  [play_index: "/"]: =>
    @streams = StreamManager\get_all_active_streams!
    if @streams
      for k,v in pairs(@streams)
        for stream in *v
          user = UserManager\get_user_by_id stream.user_id
          stream.hash = ngx.md5 stream.addr
          if user.email
            stream.hash = ngx.md5 user.email
    render: true

  [play_stream: "/:rtmp_app/:rtmp_stream"]: =>
    @host_app = StreamManager\get_app @params.rtmp_app, false
    @stream = nil
    if @host_app
      @stream = @host_app\get_stream_by_name @params.rtmp_stream
      user = UserManager\get_user_by_id @stream.user_id
      @stream.hash = ngx.md5 @stream.addr
      if user.email
        @stream.hash = ngx.md5 @user.email
    rprint @stream
    @rtmp_pair = @params.rtmp_app .. "/" .. @params.rtmp_stream
    render: true

  [play_check_publish_access: "/check_access"]: =>
    @host_app = StreamManager\get_app @params.app, true
    @info = @host_app\get_stream_by_token @params.name
   
    if @info
      @info\activate @params.addr
      return redirect_to: @info.name, status: 301, render: false
    else
      return status: 403, render: false
  
  [play_check_publish_access: "/finish_stream"]: =>
    @host_app = StreamManager\get_app @params.app, true
    @info = @host_app\get_stream_by_token @params.name
   
    if @info
      @info\deactivate!
