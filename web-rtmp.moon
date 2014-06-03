lapis.serve class extends lapis.Application
  [index: "/"]: =>
    url = "http://" .. config.host .. ":" .. config.port .. "/remote_control"
    http.simple url, {
      action: encoding.encode_with_secret("reset_streams")
    }
