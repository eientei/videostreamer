import create_table, types from require "lapis.db.schema"
import insert, raw from require "lapis.db"

{
  [1]: =>
    create_table "users", {
      { "id", types.serial primary_key: true }
      { "name", types.text unique: true }
      { "passwordhash", types.text null: true }
      { "email", types.text null: true }
      { "joined", types.time default: raw "(now() at time zone 'UTC')" }
    }

    create_table "groups", {
      { "id", types.serial primary_key: true }
      { "name", types.text }
    }

    create_table "roles", {
      { "id", types.serial primary_key: true }
      { "name", types.text }
    }

    create_table "messages", {
      { "id", types.serial primary_key: true }
      { "remote", types.text }
      { "author", types.foreign_key }
      { "app", types.text }
      { "name", types.text }
      { "posted", types.time default: raw "(now() at time zone 'UTC')" }
      { "message", types.text }
      "FOREIGN KEY (author) REFERENCES users(id)"
    }

    create_table "users_groups", {
      { "user_id", types.foreign_key }
      { "group_id", types.foreign_key }
      "PRIMARY KEY (user_id, group_id)"
      "FOREIGN KEY (user_id) REFERENCES users(id)"
      "FOREIGN KEY (group_id) REFERENCES groups(id)"
    }

    create_table "groups_roles", {
      { "group_id", types.foreign_key }
      { "role_id", types.foreign_key }
      "PRIMARY KEY (group_id, role_id)"
      "FOREIGN KEY (group_id) REFERENCES groups(id)"
      "FOREIGN KEY (role_id) REFERENCES roles(id)"
    }

    create_table "streams", {
      { "id", types.serial }
      { "token", types.text, unique: true}
      { "user_id", types.foreign_key }
      { "app", types.text }
      { "name", types.text }
      "FOREIGN KEY (user_id) REFERENCES users(id)"
      "UNIQUE (app, name)"
    }

  [2]: =>
      anon_user = insert "users", {
        name: "Anonymous"
      }, "id"
      
      anon_group = insert "groups", {
        name: "Anonymous"
      }, "id"
      
      user_group = insert "groups", {
        name: "User"
      }, "id"
      
      visit_role = insert "roles", {
        name: "visit"
      }, "id"

      list_streams_role = insert "roles", {
        name: "list_streams"
      }, "id"

      watch_streams_role = insert "roles", {
        name: "watch_streams"
      }, "id"

      publish_streams_role = insert "roles", {
        name: "publish_streams"
      }, "id"

      read_chat_role = insert "roles", {
        name: "read_chat"
      }, "id"

      write_chat_role = insert "roles", {
        name: "write_chat"
      }, "id"
      
      see_profile_role = insert "roles", {
        name: "see_profile"
      }, "id"

      insert "users_groups", {
        user_id: anon_user[1].id
        group_id: anon_group[1].id
      }
      
      roles = {
        visit_role[1].id
        list_streams_role[1].id
        watch_streams_role[1].id
        read_chat_role[1].id
        write_chat_role[1].id
      }
    
      for role in *roles
        insert "groups_roles", {
          group_id: anon_group[1].id
          role_id: role
        }

      for role in *roles
        insert "groups_roles", {
          group_id: user_group[1].id
          role_id: role
        }

      insert "groups_roles", {
        group_id: user_group[1].id
        role_id: see_profile_role[1].id
      }

      insert "groups_roles", {
        group_id: user_group[1].id
        role_id: publish_streams_role[1].id
      }
}
