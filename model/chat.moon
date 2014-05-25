import
  Users,
  Messages
from require "model.dao"
import query from require "lapis.db"
lpeg = require "lpeg"

class ChatManager
  format_msg: (message) =>
    spc = (expat) ->
      pat = (lpeg.B(lpeg.S(" ")) + -lpeg.B(1)) * expat * -expat
      pat += expat * (lpeg.S(" ") + -lpeg.P(1))
      pat

    markuphelper = (openpat, opentag, closetag, closepat) ->
      if not closetag
        closetag = "</" .. opentag .. ">"
        opentag = "<" .. opentag .. ">"
      if not closepat
        closepat = openpat

      term = lpeg.P("\n")
      term += spc(lpeg.P(closepat))
      term += (lpeg.V("mark") * spc(lpeg.P(closepat)))

      altconsume = (lpeg.P(1) - spc(lpeg.P("**") + lpeg.P("__") + lpeg.S("*_`")^1))
      subpat = ((lpeg.V("mark") - spc(lpeg.P(closepat))) + altconsume) - term

      prefix = (lpeg.P(" ") - (spc(lpeg.P(closepat)) * -lpeg.P(closepat)))^1
      prefix *= spc(lpeg.P(closepat))
  
      pat = (lpeg.P(openpat) * #(-lpeg.P(openpat))) / opentag
      pat *= #(-prefix)
      pat *= subpat^1
      pat *= (-lpeg.B(closepat) * lpeg.P(closepat)) / closetag
      pat

    concathelper = (tbl) ->
      pat = lpeg.P(false)
      for rule in *tbl
        pat += lpeg.V(rule)
      pat
    
    grouphelper = (surroundchars, tbl) ->
      pat = lpeg.B(lpeg.S(surroundchars)) + -lpeg.B(1)
      pat *= concathelper(tbl)
      pat *= #(lpeg.S(surroundchars) + -lpeg.P(1))
      pat

    pattern = lpeg.P({
      "post"
      post: lpeg.Cs((lpeg.V("mark") + lpeg.P(1))^0) / "%1"
      mark: concathelper({ "longestpair" })
      longestpair: grouphelper(" ", {
        "href_text"
        "strong_asterisk"
        "strong_underscore"
        "em_asterisk"
        "em_underscore"
        "pre_backtick"
      })
      strong_asterisk: markuphelper("**","strong")
      strong_underscore: markuphelper("__","strong")
      em_asterisk: markuphelper("*","em")
      em_underscore: markuphelper("_","em")
      pre_backtick: markuphelper("`","pre")

      inplace: grouphelper(" ", { "href_text" })
      
      href_text: (lpeg.P(">>") / "") * (lpeg.R("09")^1 / "<a href=\"%0\">&gt;&gt;%0</a>")
    })
    pattern\match(message)

  add_message: (app, name, remote, user_id, message) =>
    stmt = "insert into messages (remote, author, app, name, message) "
    stmt ..= "values(?,?,?,?,?) "
    stmt ..= "returning id, round (extract(epoch from posted) * 1000) as posted"

    row = query stmt, remote, user_id, app, name, message
    
    user = Users\find user_id
    entry = row[1]
    entry.remote = ngx.md5(remote)
    if user.email
      entry.author = ngx.md5(user.email)
    entry.app = app
    entry.name = name
    entry.message = @format_msg(message)
    entry

{ :ChatManager }
