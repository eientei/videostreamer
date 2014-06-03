import config from require "lapis.config"

config "development", ->
  host "127.0.0.1"
  port 8080

  lua_code_cache "on"
  postgresql_url "postgres://video:video@127.0.0.1/video"

  secret "shla sobaka po royalyu"
  session_name "rememberme"

  captcha false
  recaptcha_public "your-recaptcha-public-key"
  recaptcha_private "your-recaptcha-private-key"

  apps {
    live: true
    code: true
  }
  default_app "live"

  maxstreams 5

  -- ommit for same value as the request' hostname
  rtmp_host "127.0.0.1"
  
  -- ommit for default value: 1935
  rtmp_port 1935

  rtmp_sup_host "127.0.0.1"
  rtmp_sup_port 8080

