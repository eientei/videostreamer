import Widget from require "lapis.html"

class extends Widget
  content: =>
    text "$(document).ready(function(){"
    text "Recaptcha.create('" .. config.recaptcha_public .. "',"
    text "'captcha', { theme: 'red' } ); });"
