package mp4

type Config struct {
	UniversalTime bool   `yaml:"universaltime"`
	TimeScale     uint32 `yaml:"timescale"`
	BufferSeconds uint32 `yaml:"bufferseconds"`
}
