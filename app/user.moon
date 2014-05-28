import validate_functions, assert_valid from require "lapis.validate"
csrf = require "lapis.csrf"
http = require "lapis.nginx.http"

class extends lapis.Application
  @before_filter =>
    @csrf_token = csrf.generate_token @

  [jsc_signup: "/dynamic/js/signup.js"]: =>
    render: "jsc_signup", layout: false, content_type: "application/javascript"

  [user_login: "/login"]: respond_to {
    GET: =>
      if @user.name != "Anonymous"
        redirect_to: @url_for("user_profile"), status: 302, render: false
      else
        render: true
    POST: capture_errors {
      on_error: =>
        render: true
      =>
        csrf.assert_token @
        assert_valid @params, {
          { "login", exists: true }
          { "password", exists: true }
        }

        user = UserManager\get_user_by_name @params.login
        
        if not user or user.passwordhash != ngx.md5(@params.password)
          yield_error "wrong login or password"

        @session.username = user.name

        redirect_to: @url_for("user_profile"), status: 302, render: false
    }
  }

  [user_logout: "/logout"]: =>
    @session.username = nil
    @app.cookie_attributes = {}
    redirect_to: @url_for("play_index"), status: 302, render: false

  [user_profile: "/profile"]: respond_to {
    GET: =>
      if not @user.roles["see_profile"]
        @code = 401
        @message = "Unauthorized"
        render: "error", status: 401
      else
        render: true
    POST: capture_errors {
      on_error: =>
        render: true
      =>
        csrf.assert_token @

        @notices = nil

        if @params.email
          @user.email = @params.email
          UserManager\update_email @user.id, @params.email
          
          if not @notices
            @notices = {}
          table.insert @notices, "Email updated"

        if @params.password
          assert_valid @params, {
            { "password", exists: true, min_length: 3, max_length: 64 }
            { "repeat_password", exists: true, min_length: 3, max_length: 64
              "password confirmation must be provided" }
            { "repeat_password", equals: @params.password,
              "passwords do not match" }
          }
          @user.passwordhash = ngx.md5(@params.password)
          UserManager\update_password @user.id, @params.password

          table.insert @notices, "Password updated"

        if @params.app
          for k,v in pairs(@params.app)
            k = tonumber k
            stream = @user.streams[k]
            if stream and config.apps[v] and stream.app != v
              oldapp = stream.app
              @user.streams[k].app = v
              StreamManager\update_app k, oldapp, v

        if @params.name
         for k,v in pairs(@params.name)
            if not v or v == "" or v\len! < 3
              yield_error "Stream name \"#{v\lower!}\" is too short!"
            k = tonumber k
            v = v\lower!
            stream = @user.streams[k]
            if stream and stream.name != v
              if (UserManager\check_user_exists v) and (@user.name != v)
                errmsg = "Cannot change name, there is a user with nick \""
                errmsg ..= v
                errmsg ..= "\", sorry"
                yield_error errmsg

              if v\match "%W"
                yield_error "Only letters are allowed in strream name: \"#{v}\""

              if StreamManager\check_stream_exists v, stream.app
                yield_error "Stream name \"#{v}\" already taken"

              @user.streams[k].name = v
              StreamManager\update_name k, stream.app, v

        if @params.tokenupd
          for k,v in pairs(@params.tokenupd)
            k = tonumber k
            stream = @user.streams[k]
            if stream
              tok = StreamManager\update_token k, stream.app, stream.name, @user.id
              @user.streams[k].token = tok

        if @params.remove
          for k,v in pairs(@params.remove)
            k = tonumber k
            stream = @user.streams[k]
            if stream
              @user.streams[k] = nil
              StreamManager\remove_stream k, stream.app

        if @params.add
          host_app = StreamManager\get_app config.default_app, true
          rad = ""
          n = 0
          total = 0
          for k,v in pairs(@user.streams)
            total += 1
            if k > n
              n = k

          if total >= config.maxstreams
            yield_error "Stream limit number reached"

          if n > 0
            rad = tostring n
          
          stream = host_app\create_token (@user.name\lower! .. rad), @user.id, @user.name
          @user.streams[stream.id] = stream

        redirect_to: @url_for("user_profile"), status: 302, render: false
    }
  }

  [user_signup: "/signup"]: respond_to {
    GET: =>
      if @user.name != "Anonymous"
        redirect_to: @url_for("user_profile"), status: 302, render: false
      else
        render: true
    POST: capture_errors {
      on_error: =>
        render: true
      =>
        csrf.assert_token @

        assert_valid @params, {
          { "login", exists: true, min_length: 3, max_length: 64 }
          { "password", exists: true, min_length: 3, max_length: 64 }
          { "repeat_password", exists: true, min_length: 3, max_length: 64
            "password confirmation must be provided" }
          { "repeat_password", equals: @params.password,
            "passwords do not match" }
          { "recaptcha_response_field", exists: true,
            "CAPTCHA must be provided"
          }
        }

        body = http.simple "http://www.google.com/recaptcha/api/verify", {
          privatekey: config.recaptcha_private
          remoteip: ngx.var.remote_addr
          challenge: @params.recaptcha_challenge_field
          response: @params.recaptcha_response_field
        }

        if body != "true"
          yield_error "Invalid CAPTCHA"

        if UserManager\check_user_exists @params.login
          yield_error "user with such login already exists"

        id = UserManager\create_user @params.login,
                                     @params.password,
                                     @params.email

        host_app = StreamManager\get_app config.default_app, true
        host_app\create_token @params.login\lower!, id, @params.login
        
        @session.username = @params.login

        redirect_to: @url_for("user_profile"), status: 302, render: false
    }

  }
