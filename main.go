package main

import (
	"runtime"

	"./mp4"
	"./rtmp"
	"./web"
)

func main() {
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
			Address: ":8181",
		},
	}

	coordinator := &Coordinator{
		Config:      conf,
		WebServer:   web.NewServer(conf.Web),
		RtmpServer:  rtmp.NewServer(conf.Rtmp),
		RtmpClients: make(map[rtmp.ID]*RtmpClient),
		Streams:     make(map[string]*Stream),
		Events:      make(chan *Event, 64),
	}

	coordinator.RtmpServer.Subscribe(coordinator)
	coordinator.WebServer.Subscribe(coordinator)
	go coordinator.RtmpServer.ListenAndServe()
	go coordinator.WebServer.ListenAndServe()

	coordinator.Coordinate()
}
