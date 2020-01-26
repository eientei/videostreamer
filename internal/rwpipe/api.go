// Package rwpipe provides interconnected internally buffered pipes
package rwpipe

// New returns pair of interconnected pipes
func New() (a *RWPipe, b *RWPipe) {
	a = &RWPipe{}
	b = &RWPipe{}
	a.Pipe, b.Pipe = b, a

	return
}
