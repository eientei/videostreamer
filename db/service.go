package db

import (
	"database/sql"
	"fmt"
	"log"
	"time"

	_ "github.com/lib/pq"
)

var db *sql.DB

type BaseEntity struct {
	Created *time.Time
	Updated *time.Time
	Deleted *time.Time
}

type User struct {
	BaseEntity
	Id       uint64
	Username string
	Email    string
	Password string
}

type Stream struct {
	BaseEntity
	Id      uint64
	Name    string
	Title   string
	Owner   uint64
	Key     string
	Logourl string
}

func Connect(config *Config) {
	connstr := "postgres://" + config.Username + ":" + config.Password + "@" + config.Hostname + "/" + config.Database + "?sslmode=" + config.SSL
	if dbi, err := sql.Open("postgres", connstr); err != nil {
		log.Fatalln(err)
	} else {
		db = dbi
	}
}

func ExtractUser(row *sql.Rows) *User {
	user := &User{}
	if err := row.Scan(
		&user.Id,
		&user.Username,
		&user.Email,
		&user.Password,
		&user.Created,
		&user.Updated,
		&user.Deleted,
	); err != nil {
		return nil
	}
	return user
}

func ExtractStream(row *sql.Rows) *Stream {
	stream := &Stream{}
	if err := row.Scan(
		&stream.Id,
		&stream.Name,
		&stream.Title,
		&stream.Owner,
		&stream.Key,
		&stream.Logourl,
	); err != nil {
		return nil
	}
	return stream
}

func GetUserByName(name string) *User {
	if rows, err := db.Query("select * from users where username = $1", name); err != nil {
		fmt.Println(1111, err)
		return nil
	} else {
		defer rows.Close()
		rows.Next()
		return ExtractUser(rows)
	}
}

func GetUserByEmail(email string) *User {
	if rows, err := db.Query("select * from users where email = $1", email); err != nil {
		return nil
	} else {
		defer rows.Close()
		rows.Next()
		return ExtractUser(rows)
	}
}

func UpdateUserEmail(id uint64, email string) error {
	_, err := db.Exec("update users set email = $2, updated = now() where id = $1", id, email)
	return err
}

func UpdateUserPassword(id uint64, password string) error {
	_, err := db.Exec("update users set password = $2, updated = now() where id = $1", id, password)
	return err
}

func DeleteUser(id uint64) error {
	_, err := db.Exec("update users set deleted = now() where id = $1", id)
	return err
}

func CreateUser(user User) (*User, error) {
	if res, err := db.Exec("insert into users(username, email, password, created, updated, deleted) values ($1, $2, $3, now(), now(), null)", user.Username, user.Email, user.Password); err != nil {
		fmt.Println(err)
		return nil, err
	} else {
		if id, err := res.LastInsertId(); err != nil {
			return nil, err
		} else {
			user.Id = uint64(id)
			return &user, nil
		}
	}
}

func GetStreamsByOwner(owner uint64) []*Stream {
	if rows, err := db.Query("select * from streams where owner = $1", owner); err != nil {
		return nil
	} else {
		streams := make([]*Stream, 0)
		defer rows.Close()
		for rows.Next() {
			streams = append(streams, ExtractStream(rows))
		}
		return streams
	}
}

func GetSubscriptionsByUser(user uint64) []string {
	if rows, err := db.Query("select name from streams where id in (select streamid from subscriptions where userid = $1)", user); err != nil {
		return nil
	} else {
		subs := make([]string, 0)
		defer rows.Close()
		for rows.Next() {
			str := ""
			rows.Scan(str)
			subs = append(subs, str)
		}
		return subs
	}
}
