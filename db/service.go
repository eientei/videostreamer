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
	Id       uint64
	Name     string
	Title    string
	Owner    uint64
	Key      string
	Logourl  string
	Privated bool
}

type Message struct {
	BaseEntity
	Id       uint64
	User     uint64
	Stream   uint64
	Gravatar string
	Body     string
	Edited   bool
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
		&stream.Privated,
		&stream.Created,
		&stream.Updated,
	); err != nil {
		return nil
	}
	return stream
}

func ExtractMessage(row *sql.Rows) *Message {
	message := &Message{}
	if err := row.Scan(
		&message.Id,
		&message.User,
		&message.Stream,
		&message.Gravatar,
		&message.Body,
		&message.Edited,
		&message.Created,
		&message.Updated,
	); err != nil {
		return nil
	}
	return message
}

func GetUserByName(name string) *User {
	if rows, err := db.Query("select * from users where username = $1", name); err != nil {
		return nil
	} else {
		defer rows.Close()
		rows.Next()
		return ExtractUser(rows)
	}
}

func GetUserById(user uint64) *User {
	if rows, err := db.Query("select * from users where id = $1", user); err != nil {
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
	_, err := db.Exec("delete from users where id = $1", id)
	return err
}

func CreateUser(user User) uint64 {
	res := db.QueryRow("insert into users(id, username, email, password, created, updated) values (default, $1, $2, $3, now(), now()) returning id", user.Username, user.Email, user.Password)
	id := uint64(0)
	res.Scan(&id)
	return id
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
	if rows, err := db.Query("select users.username, streams.name from subscriptions join users on users.id = subscriptions.userid join streams on streams.id = subscriptions.streamid where subscriptions.userid = $1", user); err != nil {
		return nil
	} else {
		subs := make([]string, 0)
		defer rows.Close()
		for rows.Next() {
			owner := ""
			stream := ""
			rows.Scan(&owner, &stream)
			n := owner
			if len(stream) > 0 {
				n += "/" + stream
			}
			subs = append(subs, n)
		}
		return subs
	}
}

func GetStreamById(streamid uint64) *Stream {
	if rows, err := db.Query("select * from streams where id = $1", streamid); err != nil {
		return nil
	} else {
		defer rows.Close()
		rows.Next()
		return ExtractStream(rows)
	}
}

func GetStreamByOwnerAndName(user uint64, name string) *Stream {
	if rows, err := db.Query("select * from streams where owner = $1 and name = $2", user, name); err != nil {
		return nil
	} else {
		defer rows.Close()
		rows.Next()
		return ExtractStream(rows)
	}
}

func GetStreamByOwnerNameAndKey(owner string, key string) *Stream {
	if rows, err := db.Query("select streams.* from streams left join users on users.id = streams.owner where users.username = $1 and streams.key = $2", owner, key); err != nil {
		return nil
	} else {
		defer rows.Close()
		rows.Next()
		return ExtractStream(rows)
	}
}

func GetStreamByOwnerNameAndName(owner string, name string) *Stream {
	if rows, err := db.Query("select streams.* from streams left join users on users.id = streams.owner where users.username = $1 and streams.name = $2", owner, name); err != nil {
		return nil
	} else {
		defer rows.Close()
		rows.Next()
		return ExtractStream(rows)
	}
}

func CreateStream(stream Stream) uint64 {
	res := db.QueryRow("insert into streams(id, name, title, owner, key, logourl, privated, created, updated) values (default, $1, $2, $3, $4, $5, $6, now(), now()) returning id", stream.Name, stream.Title, stream.Owner, stream.Key, stream.Logourl, stream.Privated)
	id := uint64(0)
	err := res.Scan(&id)
	fmt.Println(err)
	return id
}

func UpdateStreamName(streamid uint64, name string) error {
	_, err := db.Exec("update streams set name = $2, updated = now() where id = $1", streamid, name)
	return err
}

func UpdateStreamTitle(streamid uint64, title string) error {
	_, err := db.Exec("update streams set title = $2, updated = now() where id = $1", streamid, title)
	return err
}

func UpdateStreamLogourl(streamid uint64, logourl string) error {
	_, err := db.Exec("update streams set logourl = $2, updated = now() where id = $1", streamid, logourl)
	return err
}

func UpdateStreamKey(streamid uint64, key string) error {
	_, err := db.Exec("update streams set key = $2, updated = now() where id = $1", streamid, key)
	return err
}

func UpdateStreamPrivated(streamid uint64, privated bool) error {
	_, err := db.Exec("update streams set privated = $2, updated = now() where id = $1", streamid, privated)
	return err
}

func DeleteStreamById(streamid uint64) error {
	_, err := db.Exec("delete from messages where streamid = $1", streamid)
	if err != nil {
		return err
	}
	_, err = db.Exec("delete from subscriptions where streamid = $1", streamid)
	if err != nil {
		return err
	}
	_, err = db.Exec("delete from streams where id = $1", streamid)
	return err
}

func Subscribe(userid uint64, streamid uint64) error {
	_, err := db.Exec("insert into subscriptions(userid, streamid, created, updated) values($1, $2, now(), now())", userid, streamid)
	return err
}

func Unsubscribe(userid uint64, streamid uint64) error {
	_, err := db.Exec("delete from subscriptions where userid = $1 and streamid = $2", userid, streamid)
	return err
}

func PostCreate(message Message) uint64 {
	res := db.QueryRow("insert into messages(id, userid, streamid, gravatar, body, edited, created, updated) values (default, $1, $2, $3, $4, false, now(), now()) returning id", message.User, message.Stream, message.Gravatar, message.Body)
	id := uint64(0)
	res.Scan(&id)
	return id
}

func GetOldestMessage(streamid uint64) uint64 {
	row := db.QueryRow("select id from messages where streamid = $1 order by id asc limit 1", streamid)
	id := uint64(0)
	row.Scan(&id)
	return id
}

func GetNewestMessage(streamid uint64) uint64 {
	row := db.QueryRow("select id from messages where streamid = $1 order by id desc limit 1", streamid)
	id := uint64(0)
	row.Scan(&id)
	return id
}

func PostUpdate(mid uint64, body string) error {
	_, err := db.Exec("update messages set body = $2, edited = true, updated = now() where id = $1", mid, body)
	return err
}

func PostDelete(mid uint64) error {
	_, err := db.Exec("delete from messages where id = $1", mid)
	return err
}

func GetPost(mid uint64) *Message {
	if rows, err := db.Query("select * from messages where id = $1", mid); err != nil {
		return nil
	} else {
		defer rows.Close()
		rows.Next()
		return ExtractMessage(rows)
	}
}

func PostHistory(streamid uint64, count uint64, before uint64) []*Message {
	if rows, err := db.Query("select * from messages where id <= $2 order by id desc limit $1", count, before); err != nil {
		return nil
	} else {
		msgs := make([]*Message, 0)
		defer rows.Close()
		for rows.Next() {
			msg := ExtractMessage(rows)
			msgs = append(msgs, msg)
		}
		return msgs
	}
}
