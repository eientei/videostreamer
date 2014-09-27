<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="rtmpBase" scope="request" type="java.lang.String"/>
<jsp:useBean id="stream" scope="request" type="org.eientei.video.orm.entity.Stream"/>
<jsp:useBean id="novideo" scope="request" type="java.lang.Boolean"/>
<jsp:useBean id="nochat" scope="request" type="java.lang.Boolean"/>
<jsp:useBean id="buffer" scope="request" type="java.lang.Double"/>
<jsp:useBean id="player" scope="request" type="java.lang.String"/>


<c:if test="${not novideo}">
<div class="player${(nochat == true) ? ' fullscreen' : ''}">
    <c:choose>
        <c:when test="${player == 'hls'}">
            play hls
        </c:when>
        <c:otherwise>
            <jsp:useBean id="name" scope="request" type="java.lang.String"/>
            <jsp:useBean id="app" scope="request" type="java.lang.String"/>

            <object type="application/x-shockwave-flash"
                    width="100%"
                    height="100%"
                    data="${pageContext.request.contextPath}/swf/yukkuplayer.swf">
                <param name="allowFullscreen" value="true"/>
                <param name="scale" value="noscale"/>
                <param name="bgcolor" value="#000000"/>
                <param name="wmode" value="direct"/>
                <param name="flashvars" value="videoUrl=${rtmpBase}/${app}/${name}&amp;buffer=0"/>
            </object>
        </c:otherwise>
    </c:choose>
    <div class="uiresizer"></div>
</div>
</c:if>
<c:if test="${not nochat}">
<div class="chat${(novideo == true) ? ' fullscreen' : ''}">
    <div class="chatarea">
        <div class="messagetemplate message">
            <div class="error">Template</div>
            <div class="body">
                <a class="ordinallink" href="javascript:void(0)">&gt;&gt;<span class="ordinal">0</span></a>
                <div class="secondline">
                    <div class="datetime"></div>
                    <img class="hashremote" width="16" height="16" src="http://www.gravatar.com/avatar/00000000000000000000000000000000?d=identicon&s=64" />
                </div>
                <div class="thirdline">
                    <img class="hashauthor" width="32" height="32" src="http://www.gravatar.com/avatar/00000000000000000000000000000000?d=identicon&s=64" />
                    <div class="msgtext"></div>
                    <div class="clearfix"></div>
                </div>
            </div>
        </div>
        <div class="messages">
            <a href="javascript:void(0)" data-from="oldest" data-to="0" class="loadMore">...</a>
        </div>
        <div class="bars">
            <div class="chatresizer"></div>
            <div class="blackbarwrap">
                <div class="blackbar"></div>
            </div>
            <div class="inputwrap">
                <textarea class="inputbar"></textarea>
            </div>
        </div>
    </div>
</div>

    <script src="${pageContext.request.contextPath}/js/sockjs-0.3.min.js"></script>
    <script type="text/javascript">
        var ws = null;
        var appstream = window.location.pathname.substring(1);
        var app = null;
        var stream = null;
        var connected = false;
        var wasConnected = false;
        var myhash = null;
        var typoing = false;
        var initial = false;
        var oldId = 0;
        var newId = 0;
        var favicon = new Favico({
            animation:"none",
            type: "rectangle"
        });
        var unreadMessages = 0;
        var allowBubbble = false;

        (function() {
            var appstr = appstream.split('/');
            app = appstr[0];
            stream = appstr[1];
        })();

        function makeDateTime(time) {
            function padzero(v) {
                return (v < 10) ? '0' + v : v;
            }

            function makeDate(dt) {
                return dt.getFullYear() + '-' + padzero(dt.getMonth()+1) + '-' + padzero(dt.getDate())
            }

            var date = new Date(time);
            var today = makeDate(new Date());
            var someday = makeDate(date);
            var datetime = padzero(date.getHours()) + ':' + padzero(date.getMinutes()) + ':' + padzero(date.getSeconds());
            if (today != someday) {
                datetime = someday + ' ' + datetime;
            }
            var tz = date.getTimezoneOffset();
            var offset = ((tz<0? '+':'-') + padzero(parseInt(Math.abs(tz/60))) + padzero(Math.abs(tz%60)));
            return datetime + ' GMT' + offset;
        }

        function formMessage(value) {
            var template = $(".messagetemplate").clone();
            template.removeClass("messagetemplate");
            template.find(".error").remove();
            template.find(".ordinal").text(value.id);
            template.find(".datetime").text(makeDateTime(value.posted));
            var hremsrc = template.find(".hashremote").attr("src").replace("00000000000000000000000000000000", value.remote);
            template.find(".hashremote").attr("src", hremsrc);
            var hautsrc = template.find(".hashauthor").attr("src").replace("00000000000000000000000000000000", value.author);
            template.find(".hashauthor").attr("src", hautsrc);
            template.find(".msgtext").html(value.message);
            template.find(".msgtext").find("a[data-refid]").mouseover(function() {
                if ($("body").hasClass("noselect")) {
                    return;
                }
                var num = parseInt($(this).data("refid"));
                $(this).attr("data-awaiting", true);
                sendMessage("preview", num);
            }).mouseout(function (evt) {
                var rel = $(evt.relatedTarget).closest(".message");
                if (rel.length == 0 || $.contains(rel.children(".body").get(0), this)) {
                    $(this).closest(".message").children(".message").remove();
                }
            });
            template.find(".ordinallink").click(function () {
                var textarea = $(".inputwrap > textarea");
                var text = $(this).text();
                var txtval = textarea.val();
                if ((txtval.length + text.length) < 256) {
                    var start = textarea.get(0).selectionStart;
                    textarea.val(txtval.substring(0, start) + text + txtval.substring(start));
                    textarea.keyup();
                }
            });
            return template;
        }

        function appendNewMessage(value) {
            $(".messages").append(formMessage(value).addClass("message"));
            newId = value.id;
            scrollToBottom();
        }

        function prependOldMessage(value) {
            var m = formMessage(value).addClass("history");
            $(".messages .loadMore").after(m);
            if (newId == 0) {
                newId = value.id;
            }
            oldId = value.id;
            if (initial) {
                scrollToBottom();
            } else {
                var messages = $('.messages');
                messages.scrollTop(messages.scrollTop() + m.outerHeight(true));
            }
        }

        function insertMissedMessage(value, after) {

        }

        function scrollToBottom() {
            var messages = $('.messages');
            var height = 0; messages.children('.message').each(function() { height += $(this).outerHeight(); });
            var scrollHeight = messages[0].scrollHeight;
            var scrollTop = messages.scrollTop();
            if (Math.min(height,(scrollHeight - scrollTop)) < messages.height()*2) {
                messages.scrollTop(scrollHeight);
            }
        }

        function loadMore() {
            var el = $(this);
            var from = el.data('from');
            var to = el.data('to');
            if (from == 'oldest')  {
                sendMessage('history');
            } else {
                sendMessage('updates', from + '-' + to);
            }

        }

        function sendMessage(type, obj) {
            if (!connected) {
                return;
            }
            ws.send(JSON.stringify({
                type: type,
                message: obj
            }));
        }

        function sendChatMessage(message) {
            sendMessage("message", message);
        }

        function processTypos(data) {
            typoing = false;
            $(".blackbar > img").each(function(idx, value) {
                var hsh = $(value);
                if ($.inArray(hsh.data('hash'), data) >= 0) {
                    hsh.addClass('typer');
                } else {
                    hsh.removeClass('typer');
                }
            });
        }

        function updateTypoing() {
            if (typoing) {
                sendMessage("typoing");
                typoing = false;
            }
        }

        function previewMessage(data) {
            var el = $("a[data-refid=" + data.id + "][data-awaiting]");
            var parent = el.closest(".message");
            if (!parent.hasClass("preview")) {
                $(".preview").remove();
            }
            var msg = formMessage(data).addClass("preview").mouseout(function(ev) {
                var pts = $(ev.relatedTarget).closest(msg);
                if (!pts.length || !$(pts.context).closest(".message").hasClass("preview")) {
                    $(this).animate({ opacity: 0.0 }, function () {
                        $(this).remove();
                    });
                }
            });
            parent.children(".message").remove();
            parent.append(msg);
            msg.position({
                my: "left bottom",
                at: "left top",
                of: el
            });
            msg.animate({opacity: 1.0});
            el.removeAttr("data-awaiting");
        }

        function connect() {
            ws = new SockJS("/chat");
            ws.onopen = function() {
                ws.send(JSON.stringify({
                    app: app,
                    stream: stream,
                    oldUser: wasConnected,
                    oldMessageId: oldId,
                    newMessageId: newId
                }));
            };
            ws.onclose = function (event) {
                console.log('disconnected, re-trying', event);
                connected = false;
                setTimeout(connect, 5000);
            };
            ws.onmessage = function(event) {
                var obj = JSON.parse(event.data);
                if (obj.type == "success") {
                    if (!wasConnected) {
                        initial = true;
                    }
                    wasConnected = true;
                    connected = true;
                    myhash = obj.data;
                } else if (obj.type == "onlines") {
                    var bbar = $('.blackbar');

                    function appendImg(value) {
                        bbar.append('<img class="onlineuser" data-hash="' + value + '" width="16" height="16" src="http://www.gravatar.com/avatar/' + value + '?d=identicon&s=64" />');
                    }

                    bbar.children().remove();
                    appendImg(myhash);
                    bbar.append('<span>|</span>');
                    $(obj.data).each(function(idx, value) {
                        if (value == myhash) {
                            return;
                        }
                        appendImg(value);
                    });

                } else if (obj.type == "message") {
                    appendNewMessage(obj.data);
                    if (allowBubbble) {
                        unreadMessages++;
                        if (unreadMessages > 0) {
                            favicon.badge(unreadMessages);
                        } else {
                            favicon.reset();
                        }
                    }
                } else if (obj.type == "history") {
                    $(obj.data.chatMessages).each(function(idx, value){
                        prependOldMessage(value, initial);
                    });
                    initial = false;
                    if (!obj.data.hasMore) {
                        var messages = $('.messages');
                        var loadMore = $('.loadMore');
                        var loadMoreHeight = loadMore.outerHeight(true);
                        messages.scrollTop(messages.scrollTop() - loadMoreHeight);
                        loadMore.remove();
                    }
                } else if (obj.type == "typoing") {
                    processTypos(obj.data);
                } else if (obj.type == "preview") {
                    previewMessage(obj.data);
                }
            };
        }

        $('.inputwrap > textarea').keydown(function(e) {
            var val = $(this).val();
            if (e.keyCode == 13 && !e.shiftKey) {
                var message = val;
                var strip = message.replace(/\s/g, '');
                if (strip.length > 0) {
                    if (strip == "/disconnect") {
                        ws.close();
                    } else {
                        sendChatMessage(message);
                    }
                }
                $(this).val('');
                e.preventDefault();
                return false;
            } else if (e.keyCode >= 48 && e.keyCode <= 90) {
                var oldtypoing = typoing;
                typoing = true;
                if (!oldtypoing) {
                    sendMessage("typoing");
                }

                var len = val.length;
                if (len > 256) {
                    e.preventDefault();
                    return false;
                }
            }
            return true;
        }).keyup(function(e) {
            var val = $(this).val();
            if (val.length > 256) {
                val = val.substring(0, 256);
                $(this).val(val);
            }
        });

        $('.loadMore').click(loadMore);

        function mousemovechecker(percents) {
            if (percents < 10) {
                percents = 10;
            }
            if (percents > 90) {
                percents = 90;
            }
            return percents;
        }

        function setUiHeight(percents) {
            if (percents == null) {
                return
            }
            percents = mousemovechecker(percents);
            $(".messages").css("height", percents + "%").data("percents", percents);
            $(".bars").css("top", percents + "%").css("height", (100-percents) + "%");
            scrollToBottom();
        }

        function setUiWidth(percents) {
            if (percents == null) {
                return
            }
            percents = mousemovechecker(percents);
            $(".toppanel .left").css("width", percents + "%");
            $(".chat").css("left", percents + "%").data("percents", percents);
            $(".player").css("right", (100-percents) + "%");
        }

        setUiHeight($.cookie("uiheight"));
        setUiWidth($.cookie("uiwidth"));

        var mousemovedisabler = function() {
            $("body").off("mousemove").removeClass("noselect");
            $.cookie("uiheight", $(".messages").data("percents"),  { expires: 365, path: '/' });
            $.cookie("uiwidth", $(".chat").data("percents"),  { expires: 365, path: '/' });
        };

        var vertDragger = function (e) {
            var toppanel = $(".toppanel").height();
            var height = window.innerHeight - toppanel;
            var percent = height / 100;
            var top = e.pageY - toppanel;
            var percents = top / percent;
            setUiHeight(percents);
        };

        var horizDragger = function(e) {
            var width = $(window).width();
            var percent = width / 100;
            var left = e.pageX - 5;
            var percents = left / percent;
            setUiWidth(percents);
        };

        $('.chatresizer').mousedown(function () {
            $("body").addClass("noselect").on("mousemove", vertDragger).mouseenter(mousemovedisabler).mouseup(mousemovedisabler);
        });

        $('.uiresizer').mousedown(function () {
            $("body").addClass("noselect").on("mousemove", horizDragger).mouseenter(mousemovedisabler).mouseup(mousemovedisabler);
        });

        function sendTitle(text) {
            $.post(window.location.pathname + "/title", {
                title: text
            });
        }


        $(window).on("blur focus", function(e) {
            var prevType = $(this).data("prevType");
            console.log('blur or focus', e.type);
            if (prevType != e.type) {   //  reduce double fire issues
                switch (e.type) {
                    case "blur":
                        allowBubbble = true;
                        break;
                    case "focus":
                        allowBubbble = false;
                        unreadMessages = 0;
                        favicon.reset();
                        break;
                }
            }
            $(this).data("prevType", e.type);
        })

        $(".toppanel .left .editable").click(function() {
            var elem = $(this);
            elem.children("em").remove();
            var text = elem.text().trim();
            var parent = elem.parent();
            elem.css("display", "none");
            var edit = $('<input type="text"/>').val(text).keydown(function(e) {
                if (e.keyCode == 13) {
                    var text = $(this).val();
                    $(this).remove();
                    sendTitle(text);
                    if (text.trim()) {
                        elem.text(text.trim());
                    } else {
                        elem.html("<em>No title</em>");
                    }
                    elem.css("display", "block");
                }
            });
            parent.append(edit);
        });

        setInterval(updateTypoing, 1000);
        connect();
    </script>
</div>
</c:if>