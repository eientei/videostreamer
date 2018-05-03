package web

import "encoding/json"

const (
	Disconnect           = "disconnect"
	SignupError          = "error_signup"
	LoginError           = "error_login"
	ProfileError         = "error_profile"
	StreamError          = "error_stream"
	SubError             = "error_subscribe"
	InfoError            = "error_info"
	UserSignup           = "user_signup"
	UserLogin            = "user_login"
	UserLogout           = "user_logout"
	UserDetails          = "user_details"
	UserInfoUpdate       = "user_info_update"
	StreamInfoUpdate     = "stream_info_update"
	StreamKeyUpdate      = "stream_key_update"
	StreamPrivatedUpdate = "stream_privated_update"
	StreamDelete         = "stream_delete"
	StreamAdd            = "stream_add"
	StreamPublished      = "stream_published"
	StreamSubscribe      = "stream_subscribe"
	StreamUnsubscribe    = "stream_unsubscribe"
	StreamList           = "stream_list"
	StreamInfo           = "stream_info"
	StreamInfoReq        = "stream_info_req"
	StreamRedirect       = "stream_redirect"
	MessageAdd           = "message_add"
	MessageSend          = "message_send"
	MessageEdit          = "message_edit"
	MessageDelete        = "message_delete"
	MessageHistory       = "message_history"
)

type EventMessage interface {
	Type() string
}

type TypedBytes struct {
	Type string          `json:"type"`
	Data json.RawMessage `json:"data"`
}

type DisconnectMessage struct {
}

func (message *DisconnectMessage) Type() string {
	return Disconnect
}

type SignupErrorMessage struct {
	Error string `json:"error"`
}

func (message *SignupErrorMessage) Type() string {
	return SignupError
}

type LoginErrorMessage struct {
	Error string `json:"error"`
}

func (message *LoginErrorMessage) Type() string {
	return LoginError
}

type ProfileErrorMessage struct {
	Error string `json:"error"`
}

func (message *ProfileErrorMessage) Type() string {
	return ProfileError
}

type StreamErrorMessage struct {
	Error string `json:"error"`
}

func (message *StreamErrorMessage) Type() string {
	return StreamError
}

type SubErrorMessage struct {
	Error string `json:"error"`
}

func (message *SubErrorMessage) Type() string {
	return SubError
}

type InfoErrorMessage struct {
	Error string `json:"error"`
}

func (message *InfoErrorMessage) Type() string {
	return InfoError
}

type UserSignupMessage struct {
	Username string `json:"username"`
	Email    string `json:"email"`
	Password string `json:"password"`
	Captcha  string `json:"captcha"`
}

func (message *UserSignupMessage) Type() string {
	return UserSignup
}

type UserLoginMessage struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

func (message *UserLoginMessage) Type() string {
	return UserLogin
}

type UserLogoutMessage struct {
}

func (message *UserLogoutMessage) Type() string {
	return UserLogout
}

type UserDetailsStreams struct {
	Id       uint64   `json:"id"`
	Gravatar string   `json:"gravatar"`
	Owner    string   `json:"owner"`
	Name     string   `json:"name"`
	Title    string   `json:"title"`
	Logourl  string   `json:"logourl"`
	Key      string   `json:"key"`
	Privated bool     `json:"privated"`
	Clients  int      `json:"clients"`
	Users    []string `json:"users"`
	Oldest   uint64   `json:"oldest"`
}

type UserDetailsMessage struct {
	Name          string                `json:"name"`
	Email         string                `json:"email"`
	Gravatar      string                `json:"gravatar"`
	Streams       []*UserDetailsStreams `json:"streams"`
	Notifications []string              `json:"notifications"`
}

func (message *UserDetailsMessage) Type() string {
	return UserDetails
}

type UserInfoUpdateMessage struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}

func (message *UserInfoUpdateMessage) Type() string {
	return UserInfoUpdate
}

type StreamInfoUpdateMessage struct {
	Id      uint64 `json:"id"`
	Name    string `json:"name"`
	Title   string `json:"title"`
	Logourl string `json:"logourl"`
}

func (message *StreamInfoUpdateMessage) Type() string {
	return StreamInfoUpdate
}

type StreamKeyUpdateMessage struct {
	Id uint64 `json:"id"`
}

func (message *StreamKeyUpdateMessage) Type() string {
	return StreamKeyUpdate
}

type StreamPrivatedUpdateMessage struct {
	Id       uint64 `json:"id"`
	Privated bool   `json:"privated"`
}

func (message *StreamPrivatedUpdateMessage) Type() string {
	return StreamPrivatedUpdate
}

type StreamDeleteMessage struct {
	Id uint64 `json:"id"`
}

func (message *StreamDeleteMessage) Type() string {
	return StreamDelete
}

type StreamAddMessage struct {
}

func (message *StreamAddMessage) Type() string {
	return StreamAdd
}

type StreamPublishedMessage struct {
	Stream *UserDetailsStreams `json:"stream"`
}

func (message *StreamPublishedMessage) Type() string {
	return StreamPublished
}

type StreamSubscribeMessage struct {
	Id uint64 `json:"id"`
}

func (message *StreamSubscribeMessage) Type() string {
	return StreamSubscribe
}

type StreamUnsubscribeMessage struct {
	Id uint64 `json:"id"`
}

func (message *StreamUnsubscribeMessage) Type() string {
	return StreamUnsubscribe
}

type StreamListMessage struct {
	Streams []*UserDetailsStreams `json:"streams"`
}

func (message *StreamListMessage) Type() string {
	return StreamList
}

type StreamInfoMessage struct {
	Stream *UserDetailsStreams `json:"stream"`
}

func (message *StreamInfoMessage) Type() string {
	return StreamInfo
}

type StreamInfoReqMessage struct {
	Owner  string `json:"owner"`
	Stream string `json:"stream"`
}

func (message *StreamInfoReqMessage) Type() string {
	return StreamInfoReq
}

type StreamRedirectMessage struct {
	Owner  string `json:"owner"`
	Stream string `json:"stream"`
}

func (message *StreamRedirectMessage) Type() string {
	return StreamRedirect
}

type MessageAddMessage struct {
	Id       uint64 `json:"id"`
	Author   uint64 `json:"author"`
	Gravatar string `json:"gravatar"`
	Edited   bool   `json:"edited"`
	Text     string `json:"text"`
	Posted   uint64 `json:"posted"`
}

func (message *MessageAddMessage) Type() string {
	return MessageAdd
}

type MessageSendMessage struct {
	Streamid uint64 `json:"streamid"`
	Text     string `json:"text"`
}

func (message *MessageSendMessage) Type() string {
	return MessageSend
}

type MessageEditMessage struct {
	Id   uint64 `json:"id"`
	Text string `json:"text"`
}

func (message *MessageEditMessage) Type() string {
	return MessageEdit
}

type MessageDeleteMessage struct {
	Id uint64 `json:"id"`
}

func (message *MessageDeleteMessage) Type() string {
	return MessageDelete
}

type MessageHistoryMessage struct {
	Streamid uint64 `json:"streamid"`
	Before   uint64 `json:"before"`
}

func (message *MessageHistoryMessage) Type() string {
	return MessageHistory
}
