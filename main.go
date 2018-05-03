package main

import (
	"io/ioutil"
	"os"
	"runtime"

	"gopkg.in/yaml.v2"

	"./db"
	"./mp4"
	"./rtmp"
	"./web"
)

func main() {
	conffile := "videostreamer.yaml"
	if len(os.Args) > 1 {
		conffile = os.Args[1]
	}
	runtime.GOMAXPROCS(32)

	conf := &Config{
		Mp4: &mp4.Config{
			UniversalTime: true,
			TimeScale:     1000,
			BufferSeconds: 1,
		},
		Rtmp: &rtmp.Config{
			Address:   ":1935",
			Chunksize: 2048,
			Acksize:   0xFFFFFF,
			Bandwidth: 0xFFFFFF,
			Limit: rtmp.Limit{
				Publishers:  -1,
				Subscribers: -1,
			},
		},
		Web: &web.Config{
			Address:   ":8181",
			Recaptcha: "ReCaptcha private key",
		},
		DB: &db.Config{
			Hostname: "127.0.0.1:5432",
			Username: "videostreamer",
			Password: "videostreamer",
			Database: "videostreamer",
			SSL:      "disable",
		},
	}

	if yamlFile, err := ioutil.ReadFile(conffile); err != nil {
		return
	} else {
		if err := yaml.Unmarshal(yamlFile, conf); err != nil {
			return
		}
	}

	webServer := web.NewServer(conf.Web)
	rtmpServer := rtmp.NewServer(conf.Rtmp)
	coordinator := &Coordinator{
		Config:      conf,
		RtmpClients: make(map[rtmp.ID]*RtmpClient),
		Streams:     make(map[string]*Stream),
	}

	db.Connect(conf.DB)
	rtmpServer.Subscribe(coordinator)
	webServer.Subscribe(coordinator)
	go rtmpServer.ListenAndServe()
	go webServer.ListenAndServe()

	waiter := make(chan struct{})
	<-waiter
}
