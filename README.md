videostreamer
=============

eientei.org video streamer application &amp; configs

A video streaming site written in [lapis](http://leafo.net/lapis) Moonscrip/Lua [openresty](http://openresty.org) framework for [nginx](http://nginx.org)

Required nginx modules:

* https://github.com/arut/nginx-rtmp-module
* https://github.com/openresty/lua-nginx-module
* https://github.com/FRiCKLE/ngx_postgres
* https://github.com/wandenberg/nginx-push-stream-module

Require Lua libraries:

* http://www.inf.puc-rio.br/~roberto/lpeg

Configure
=========

```
$EDITOR config.moon
```
Build
=====

```
moonc .
lapis build
```

Run
===

```
lapis server
```
