import Model from require "lapis.db.model"

class Users extends Model
  @primary_key: "id"
  @timestamp: true

class Groups extends Model
  @primary_key: "id"
  
class Roles extends Model
  @primary_key: "id"

class Rooms extends Model
  @primary_key: "id"

class Messages extends Model
  @primary_key: "id"
  @timestamp: true

class Streams extends Model
  @primary_key: "id"
  @timestamp: true

class UsersGroups extends Model
  @primary_key: "user_id"

class GroupsRoles extends Model
  @primary_key: "group_id"

class StreamInfo
  new: (name, user_id, username) =>
    @active = false
    @name = name
    @user_id = user_id
    @username = username
    @addr = nil

  activate: (addr) =>
    @addr = addr
    @active = true

  deactivate: =>
    @addr = nil
    @active = false

class StreamAppManager
  new: (appname) =>
    @appname = appname
    @streams = {}

  make_and_cache: (data) =>
    if next(data) == nil
      return nil

    data = data[1]

    user = Users\find data.user_id

    stream = StreamInfo data.name, user.id, user.name
    
    @streams[data.token] = stream
    stream

  remove_stream_by_id: (id, delete) =>
    for k, stream in pairs(@streams)
      if stream.id == id
        @streams[k] = nil
        if delete
          data = Streams\find id
          data\delete!
        return stream

  get_stream_by_id: (id) =>
    for k, stream in pairs(@streams)
      if stream.id == id
        return stream

    data = Streams\select "where app = ? and id = ?", @appname, id
    @make_and_cache data

  get_stream_by_name: (name) =>
    for k, stream in pairs(@streams)
      if stream.name == name
        return stream

    data = Streams\select "where app = ? and name = ?", @appname, name
    @make_and_cache data

  get_stream_by_token: (token) =>
    for k, stream in pairs(@streams)
      if stream.token == token
        return stream

    data = Streams\select "where app = ? and token = ?", @appname, token
    @make_and_cache data

  get_active_streams: =>
    ss = {}
    for k, stream in pairs(@streams)
      if stream.active
        table.insert ss, stream
    ss

  gen_token: (name, user_id) =>
    token = nil
    found = false
    math.randomseed(os.time())
    while not token or found
      data = (tostring os.time!) .. @appname .. "/" .. name .. (tostring user_id)
      data ..= "|" .. math.random!
      token = ngx.md5(encoding.encode_with_secret(data))
      sames = Streams\select "where token = ?", token
      found = next(sames)

    token



  create_token: (name, user_id, username) =>
    token = @gen_token name, user_id

    stream_id = Streams\create {
      token: token
      app: @appname
      name: name
      user_id: user_id
    }

    stream = StreamInfo name, user_id, username
    table.insert @streams, stream

    Streams\find stream_id.id

class StreamManager
  apps: {}

  get_app: (name, okcreate) =>
    app = @apps[name]
    if app
      return app

    if okcreate
      app = StreamAppManager(name)
      @apps[name] = app
    app

  remove_stream: (id, app_name) =>
    app = @get_app app_name, false
    
    if app
      app\remove_stream_by_id id

    stream_data = Streams\find id
    stream_data\delete!

  get_all_active_streams: =>
    streams = {}
    for name, app in pairs(@apps)
      appstreams = app\get_active_streams!
      for stream in *appstreams
        if streams[name] == nil
          streams[name] = {}
        table.insert streams[name], stream
    if next(streams) == nil
      return nil
    streams
  
  check_stream_exists: (name, app_name) =>
    app = @get_app app_name, true
    
    app\get_stream_by_name name


  update_app: (id, oldapp_name, newapp_name) =>
    oldapp = @get_app oldapp_name, false
    newapp = @get_app newapp_name, true

    if oldapp
      oldapp\remove_stream_by_id id, false
    
    stream_data = Streams\find id
    stream_data\update {
      app: newapp_name
    }

    newapp\get_stream_by_id id

  update_name: (id, app_name, newname) =>
    app = @get_app app_name, false
    
    if app
      stream = app\get_stream_by_id id
      stream.name = newname
    
    stream_data = Streams\find id
    stream_data\update {
      name: newname
    }

  update_token: (id, app_name, name, user_id) =>
    app = @get_app app_name, true
    
    newtoken = app\gen_token name, user_id

    stream = app\get_stream_by_id id
    stream.token = newtoken
    
    stream_data = Streams\find id
    stream_data\update {
      token: newtoken
    }

    newtoken

class UserManager
  users: {}
  
  load_and_cache: (user) =>
    groupsmap = UsersGroups\select "where user_id = ?", user.id
    Groups\include_in groupsmap, "group_id"
    groups = {}
    roles = {}
    for k,v in pairs(groupsmap)
      groups[v.group.name] = true
      rolesmap = GroupsRoles\select "where group_id = ?", v.group.id
      Roles\include_in rolesmap, "role_id"
      for k,v in pairs(rolesmap)
        roles[v.role.name] = true
    streams_data = Streams\select "where user_id = ?", user.id
    streams = {}
    for stream in *streams_data
      streams[stream.id] = stream
 
    user.groups = groups
    user.roles = roles
    user.streams = streams
    @users[user.name\lower!] = user
    user

  get_user_by_id: (id) =>
    for k,v in pairs(@users)
      if v.id == id
        return v
    user_data = Users\find id
    @load_and_cache user_data

  get_user_by_name: (name) =>
    name = name\lower!
    user = @users[name]
    if user
      return user

    user = Users\select "where lower(name) = ?", name
    if not user or next(user) == nil
      return nil
    user_data = user[1]
    @load_and_cache user_data
  
  check_user_exists: (name) =>
    name = name\lower!
    if @users[name]
      true
    else
      r = Users\select "where lower(name) = ?", name
      r and next(r) != nil

  invalidate_user: (name) =>
    @users[name\lower!] = nil

  update_email: (id, email) =>
    user = Users\find id
    if user
      user\update {
        email: email
      }

  update_password: (id, password) =>
    md5hash = ngx.md5(password)
    user = Users\find id
    if user
      user\update {
        passwordhash: md5hash
      }


  create_user: (login, password, email) =>
    md5hash = ngx.md5(password)

    user = Users\create {
      name: login
      passwordhash: md5hash
      email: email
    }

    group = Groups\find name: "User"

    UsersGroups\create {
      user_id: user.id
      group_id: group.id
    }

    user.id

{
  :UserManager
  :StreamManager
}
