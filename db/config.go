package db

type Config struct {
	Hostname string `yaml:"host"`
	Username string `yaml:"user"`
	Password string `yaml:"password"`
	Database string `yaml:"database"`
	SSL      string `yaml:"ssl"`
}
