package main

import (
	"./db"
	"./mp4"
	"./rtmp"
	"./web"
)

type Config struct {
	Mp4  *mp4.Config  `yaml:"mp4"`
	Rtmp *rtmp.Config `yaml:"rtmp"`
	Web  *web.Config  `yaml:"web"`
	DB   *db.Config   `yaml:"db"`
}
