import
  Users,
  Messages
from require "model.dao"
import query from require "lapis.db"
lpeg = require "lpeg"

format_msg = (message) ->
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
      markuphelper("%%", "<span class=\"spoiler\">", "</span>")
      markuphelper("**", "strong")
      markuphelper("__", "strong")
      markuphelper("*", "em")
      markuphelper("_", "em")
      markuphelper("`", "pre")
    })

    marksyms: concatpats({
      lpeg.P("%%")
      lpeg.P("**")
      lpeg.P("__")
      lpeg.P("*")
      lpeg.P("_")
      lpeg.P("`")
    })

    inplace: concatrules({ "url", "postref" })
      
    postref: (lpeg.P(">>") / "") * lpeg.V("postrefnum")
    postrefnum: (lpeg.R("09")^1 / "<a href=\"#%0\">&gt;&gt;%0</a>")

    url: (lpeg.Cs(unionrules({
      "url_prepat"
      "url_scheme"
      "url_token"
      "url_postpat"
    })) / "<a href=\"%1\">%1</a>")
    url_prepat: lpeg.S("%*_`")^1 + termleft("%*_` ")
    url_scheme: (lpeg.V("escorpass") - (lpeg.P("://") + lpeg.S("%*_` ")))^1
    url_token: lpeg.P("://")
    url_postpat: (lpeg.V("escorpass") - lpeg.V("url_term"))^1
    url_term: termright(" ") + (lpeg.V("marksyms") * termright(" "))

    escorpass: lpeg.V("html_escape") + lpeg.P(1)
    markorpass: lpeg.V("mark") + lpeg.P(1)

    html_escape: concatpats({
      lpeg.P("&") / "&amp;"
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

binarysearch = (list, value, reverse) ->
  search = (low, high) ->
    mid = math.floor((low+high)/2)
    if reverse
      if list[mid] < value
        return search(low,mid-1)
      if list[mid] > value
        return search(mid+1,high)
    else
      if list[mid] > value
        return search(low,mid-1)
      if list[mid] < value
        return search(mid+1,high)
    return mid
  search(1, #list)

class ChatRoom
  new: (app, name) =>
    @users = {}
    @messages = {}
    @newerkeys = {}
    @olderkeys = {}
    @app = app
    @name = name
    @totalmessages = 0
    @bottomhit = false
    @lowest = 0

  append_message: (msg) =>
    @messages[msg.id] = msg
    table.insert @newerkeys, msg.id
    @totalmessages += 1
    if @lowest == 0
      @lowest = msg.id

  prepend_messages: (msgs) =>
    for msg in *msgs
      @messages[msg.id] = msg
      msg.message = format_msg msg.message
      table.insert @olderkeys, msg.id
    @totalmessages += #msgs

  get_active_users: (author) =>
    @users[author] = ngx.now!
    res = {}
    time = ngx.now!
    for h,t in pairs(@users)
      if (time - t) < 5.0
        table.insert res, { author: h }
    res

  get_fromid_messages: (fromid, n) =>
    if n > 128
      n = 128

    if not @messages[fromid]
      return nil

    msgs = {}

    npos = 0

    if @newerkeys[1] and fromid > @newerkeys[1]
      npos = binarysearch @newerkeys, fromid, false
      npos -= 1

    if npos >= n
      for i = (npos - n) + 1, npos
        table.insert msgs, @messages[@newerkeys[i]]
    else
      oldneeded = n
      newneeded = 0
      if npos > 0
        oldneeded = n - npos
        newneeded = npos
      
      oldc = #@olderkeys
      opos = 0
      oldavail = 0

      if oldc > 0
        if @newerkeys[1] and fromid == @newerkeys[1] or npos > 0
          opos = 0
        else
          opos = binarysearch @olderkeys, fromid, true
        oldavail = oldc - opos

      if oldavail < oldneeded and not @bottomhit
        limit = oldneeded - oldavail
        @fetch_previous_chunk limit, @lowest
        oldc = #@olderkeys
        oldavail = oldc - opos


      if oldc > 0
        lim = math.min oldneeded, oldavail

        for i = opos+lim, opos + 1, -1
          table.insert msgs, @messages[@olderkeys[i]]
       
      if npos > 0
        for i = 1, npos
          table.insert msgs, @messages[@newerkeys[i]]


    msgs
    
  get_last_n_messages: (n) =>
    if n > 128
      n = 128

    if not @bottomhit and n > @totalmessages
      limit = n - @totalmessages
      @fetch_previous_chunk limit, @lowest
    
    msgs = {}

    newc = #@newerkeys

    if n > newc
      up = n - newc

      oldc = #@olderkeys
      if up > oldc
        up = oldc

      for i = up, 1, -1
        table.insert msgs, @messages[@olderkeys[i]]

      for i = 1, newc
        table.insert msgs, @messages[@newerkeys[i]]

    else
      for i = (newc - n) + 1, newc
        table.insert msgs, @messages[@newerkeys[i]]
    
    msgs

  get_single: (postid) =>
    msg = @messages[postid]
    
    if msg
      return msg

    msgs = query @make_query!, @app, @name, postid+1, postid+1, 1

    if #msgs != 1
      return nil

    msg = msgs[1]
    msg.message = format_msg msg.message

    msg


  make_query: =>
      stmt = "select "
      stmt ..= "messages.id, md5(messages.remote) as remote, "
      stmt ..= "md5(coalesce(nullif(users.email, ''), messages.remote)) as author, "
      stmt ..= "messages.app, messages.name, "
      stmt ..= "round(extract(epoch from messages.posted)*1000) as posted, "
      stmt ..= "messages.message "
      stmt ..= "from messages left join users on messages.author = users.id "
      stmt ..= "where messages.app = ? and messages.name = ? and "
      stmt ..= "(? = 0 or messages.id < ?) order by messages.id desc limit ?"
      stmt

  fetch_previous_chunk: (limit, fromid) =>
    if not @bottomhit
      
      if not fromid
        fromid = @lowest

      msgs = query @make_query!, @app, @name, fromid, fromid, limit
      
      count = #msgs
 
      if count < limit
        @bottomhit = true

      if count > 0
        @lowest = msgs[count].id
        @prepend_messages msgs

class ChatManager
  rooms: {}

  get_room_by_name: (app, name) =>
    roomname = app .. "/" .. name
    room = @rooms[roomname]
    if not room and (StreamManager\check_stream_exists name, app)
      room = ChatRoom app, name
      @rooms[roomname] = room
    room

  get_active_users: (app, name, author) =>
    room = @get_room_by_name app, name

    if not room
      return nil

    room\get_active_users author

  get_last_history: (app, name) =>
    room = @get_room_by_name app, name

    if not room
      return nil

    room\get_last_n_messages 32

  get_previous_history: (app, name, last) =>
    room = @get_room_by_name app, name

    if not room
      return nil

    room\get_fromid_messages last, 32

  get_previous_single: (app, name, postid) =>
    room = @get_room_by_name app, name

    if not room
      return nil

    room\get_single postid

  add_message: (app, name, remote, user_id, email, message) =>
    room = @get_room_by_name app, name

    if not room
      return nil

    stmt = "insert into messages (remote, author, app, name, message) "
    stmt ..= "values(?,?,?,?,?) "
    stmt ..= "returning id, round (extract(epoch from posted) * 1000) as posted"

    row = query stmt, remote, user_id, app, name, message
    
    entry = row[1]
    entry.remote = ngx.md5(remote)
    if email
      entry.author = ngx.md5(email)
    else
      entry.author = entry.remote
    entry.app = app
    entry.name = name
    entry.message = format_msg message

    room\append_message entry

    entry

{ :ChatManager }
