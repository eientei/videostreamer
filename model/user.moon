import
  UsersGroups,
  GroupsRoles,
  Streams,
  Users,
  Groups,
  Roles
from require "model.dao"

export UserManager
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

{ :UserManager }
