import Widget from require "lapis.html"

class extends Widget
  content: =>
    raw "$(document).ready(function(){"
    raw "Recaptcha.create(\"" .. config.recaptcha_public .. "\","
    raw "\"captcha\", { theme: \"red\" } ); });"
