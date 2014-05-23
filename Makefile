all:
	moonc .
	lapis build

clean:
	find . -name '*.lua' -delete
	rm -f nginx.conf.compiled

rebuild: clean all

.PHONY: all clean rebuild

