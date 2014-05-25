import Model from require "lapis.db.model"

class Users extends Model
  @primary_key: "id"

class Groups extends Model
  @primary_key: "id"
  
class Roles extends Model
  @primary_key: "id"

class Messages extends Model
  @primary_key: "id"

class Streams extends Model
  @primary_key: "id"

class UsersGroups extends Model
  @primary_key: "user_id"

class GroupsRoles extends Model
  @primary_key: "group_id"

{
  :Users
  :Groups
  :Roles
  :Messages
  :Streams
  :UsersGroups
  :GroupsRoles
}
