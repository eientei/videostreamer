lapis.serve class extends lapis.Application
  [started: "/started"]: =>
    url = "http://" .. config.host .. ":" .. config.port .. "/remote_control"
    http.simple url, {
      action: encoding.encode_with_secret("reset_streams")
    }
