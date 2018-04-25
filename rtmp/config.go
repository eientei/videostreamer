package rtmp

var defaultConfig = &Config{
	Address:   ":1935",
	Chunksize: 2048,
	Acksize:   0xFFFFFF,
	Bandwidth: 0xFFFFFF,
	Limit: Limit{
		Publishers:  -1,
		Subscribers: -1,
	},
}

type Limit struct {
	Publishers  int `yaml:"publishers"`
	Subscribers int `yaml:"subscribers"`
}

type Config struct {
	Address   string `yaml:"address"`
	Chunksize uint32 `yaml:"chunksize"`
	Acksize   uint32 `yaml:"acksize"`
	Bandwidth uint32 `yaml:"bandwidth"`
	Limit     Limit  `yaml:"limit"`
}

func MergeConfig(base *Config, custom *Config) *Config {
	result := *base

	if len(custom.Address) > 0 {
		result.Address = custom.Address
	}
	if custom.Chunksize > 0 {
		result.Chunksize = custom.Chunksize
	}
	if custom.Acksize > 0 {
		result.Acksize = custom.Acksize
	}
	if custom.Bandwidth > 0 {
		result.Bandwidth = custom.Bandwidth
	}
	if custom.Limit.Publishers != 0 {
		result.Limit.Publishers = custom.Limit.Publishers
	}
	if custom.Limit.Subscribers != 0 {
		result.Limit.Subscribers = custom.Limit.Subscribers
	}

	return &result
}
