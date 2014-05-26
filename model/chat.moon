import
  Users,
  Messages
from require "model.dao"
import query from require "lapis.db"
lpeg = require "lpeg"

class ChatRoom
  messages: {}
  totalmessages: 0
  bottomhit: false
  lowest: -1
  add_message: (message) ->
    messages[message.id] = message
    totalmessages += 1

  get_last_ns: (n) ->
    if totalmessages < n and not bottomhit
      n
      

class ChatManager
  rooms: {}

  format_msg: (message) =>
    termright = (set) -> #(lpeg.S(set .. "\n")) + #(-lpeg.P(1))
    termleft  = (set) -> lpeg.B(lpeg.S(set .. "\n")) + -lpeg.B(1)
   
    markuphelper = (openpat, opentag, closetag, closepat) ->
      if not closetag
        closetag = "</" .. opentag .. ">"
        opentag = "<" .. opentag .. ">"
      if not closepat
        closepat = openpat


      consumer = (conpat, ortest) ->
        term = lpeg.P(closepat) * termright(" ")
        if ortest
          term += ortest
        (conpat - term)^1
     
      lpat = termleft(" ") * lpeg.P(openpat)
      lpat *= consumer(lpeg.S("%*_`"))
      lpat *= lpeg.P(closepat) * termright(" ")

      tester = consumer(lpeg.V("mark") + lpeg.S("%*_` "), lpeg.V("mark"))

      rpat = termleft(" ") * (lpeg.P(openpat) / opentag)
      rpat *= #(-(tester * lpeg.P(closepat) * termright(" ")))
      rpat *= consumer(lpeg.V("mark") + lpeg.P(1))
      rpat *= (lpeg.P(closepat) / closetag) * termright(" ")
      lpat + rpat

    concatrules = (tbl) ->
      pat = lpeg.P(false)
      for rule in *tbl
        pat += lpeg.V(rule)
      pat

    concatpats = (tbl) ->
      pat = lpeg.P(false)
      for rule in *tbl
        pat += rule
      pat
   
    unionrules = (tbl) ->
      pat = lpeg.P(true)
      for rule in *tbl
        pat *= lpeg.V(rule)
      pat


    grouphelper = (surroundchars, tbl) ->
      pat = lpeg.B(lpeg.S(surroundchars)) + -lpeg.B(1)
      pat *= concathelper(tbl)
      pat *= #(lpeg.S(surroundchars) + -lpeg.P(1))
      pat

    pattern = lpeg.P({
      "post"
      post: lpeg.Cs((lpeg.V("quote") + lpeg.V("mark") + lpeg.P(1))^0) / "%1"
      mark: concatrules({ "longestpair", "inplace", "html_escape" })

      longestpair: concatpats({
        markuphelper("**", "strong")
        markuphelper("__", "strong")
        markuphelper("*", "em")
        markuphelper("_", "em")
        markuphelper("`", "pre")
      })

      inplace: concatrules({ "url", "postref" })
      
      postref: (lpeg.P(">>") / "") * lpeg.V("postrefnum")
      postrefnum: (lpeg.R("09")^1 / "<a href=\"#%0\">&gt;&gt;%0</a>")

      url: lpeg.Cs(unionrules({
        "url_prepat"
        "url_scheme"
        "url_token"
        "url_postpat"
      })) / "<a href=\"%1\">%1</a>"
      url_prepat: termleft("%*_` ")
      url_scheme: (lpeg.V("escorpass") - (lpeg.P("://") + lpeg.S("%*_` ")))^1
      url_token: lpeg.P("://")
      url_postpat: (lpeg.V("escorpass") - termright(" "))^1

      escorpass: lpeg.V("html_escape") + lpeg.P(1)
      markorpass: lpeg.V("mark") + lpeg.P(1)

      html_escape: concatpats({
        lpeg.P(">") / "&gt;"
        lpeg.P("<") / "&lt;"
        lpeg.P("\"") / "&quot;"
      })

      quote: lpeg.Cs(unionrules({
        "quote_precond"
        "quote_marks"
        "quote_body"
      })) / "<span class=\"quote\">%1</span>"
      quote_precond: termleft("") * lpeg.S(" ")^0
      quote_marks: (lpeg.P(">") / "&gt;") - lpeg.V("postref")
      quote_body: (lpeg.V("markorpass") - termright(""))^1
    })

    pattern\match(message)

  get_room_by_name: (roomname) ->
    room = rooms[roomname]
    if not room
      room = ChatRoom!
      rooms[roomname] = room
    room

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
    
    roomname = app .. "/" .. name
    
    room = get_room_by_name roomname

    room\add_message entry

    entry

{ :ChatManager }
