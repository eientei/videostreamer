http = require "lapis.nginx.http"
encoding = require "lapis.util.encoding"


url = "http://" .. config.host .. ":" .. config.port .. "/remote_control"
http.simple url, {
  action: encoding.encode_with_secret("reset_streams")
}

