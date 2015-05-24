'use strict';

angular.module('videostreamer').directive('playhorizresize', ['$document', function ($document) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            var localStorage = window['localStorage'];
            var oldpos = localStorage['videostreamer.playwidth'];
            var player;
            var chat;

            var init = function () {
                player = document.getElementById('player');
                chat = document.getElementById('chat');
                if (!player || !chat) {
                    return;
                }
                if (oldpos) {
                    setpos(oldpos);
                }
            };

            var setpos = function (pos) {
                pos = Math.floor(pos);
                player.style.width = pos + '%';
                chat.style.width = (100-pos) + '%';
            };

            var mousemove = function (e) {
                var pos = (e.pageX) / document.documentElement.clientWidth * 100;
                if (pos < 10) {
                    pos = 10;
                }
                if (pos > 90) {
                    pos = 90;
                }
                setpos(pos);
                localStorage['videostreamer.playwidth'] = pos;
            };

            var mouseup = function (e) {
                $document.off('mousemove', mousemove);
                $document.off('mouseup', mouseup);
            };

            element.on('mousedown', function (e) {
                e.preventDefault();
                $document.on('mousemove', mousemove);
                $document.on('mouseup', mouseup);
            });

            setTimeout(init, 0);
        }
    };
}]).directive('playvertresize', ['$document', function ($document) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            var localStorage = window['localStorage'];
            var messages = document.getElementById('messages');
            var input = document.getElementById('input');
            var chat = document.getElementById('chat');
            var scroller = document.getElementById('scroller');
            var oldpos = localStorage['videostreamer.playheight'];

            var setpos = function (pos) {
                pos = Math.floor(pos);
                messages.style.height = pos + '%';
                input.style.height = (100-pos) + '%';
                if (scroller.scrollHeight - scroller.scrollTop <  scroller.clientHeight * 2) {
                    scroller.scrollTop = scroller.scrollHeight;
                }
            };
            if (oldpos) {
                setpos(oldpos);
            }

            var mousemove = function (e) {
                var diff = document.documentElement.clientHeight - chat.clientHeight;
                var pos = (e.pageY - diff) / chat.clientHeight * 100;
                if (pos < 10) {
                    pos = 10;
                }
                if (pos > 90) {
                    pos = 90;
                }
                setpos(pos);
                localStorage['videostreamer.playheight'] = pos;
            };

            var mouseup = function (e) {
                $document.off('mousemove', mousemove);
                $document.off('mouseup', mouseup);
            };

            element.on('mousedown', function (e) {
                e.preventDefault();
                $document.on('mousemove', mousemove);
                $document.on('mouseup', mouseup);
            });
        }
    };
}]);