'use strict';

angular.module('video').directive('resize', ['$document', function ($document) {
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {
            var direction = attrs.resize;
            var localStorage = window['localStorage'];
            var oldpos;
            if (direction == 'vert') {
                oldpos = localStorage['videostreamer.playwidth'];
            } else if (direction == 'horiz') {
                oldpos = localStorage['videostreamer.playheight'];
            }

            var msgs;
            var navwrap;

            var a;
            var b;

            setTimeout(function () {
                if (direction == 'vert') {
                    a = element.parent();
                    b = a.next();
                } else if (direction == 'horiz') {
                    a = angular.element(element.parent().children()[0]);
                    b = element.next();
                    msgs = element.parent();
                    navwrap = angular.element(document.getElementById('navwrap'));
                }
                setpos(oldpos);
            }, 0);

            function setpos(pos) {
                pos = Math.floor(pos);
                a[0].style['flex-grow'] = pos;
                b[0].style['flex-grow'] = 100-pos;
                if (direction == 'vert') {
                    localStorage['videostreamer.playwidth'] = pos;
                } else if (direction == 'horiz') {
                    localStorage['videostreamer.playheight'] = pos;
                }
            }

            var mousemove = function (e) {
                var pos;
                if (direction == 'vert') {
                    pos = (e.pageX + element[0].clientWidth) / document.documentElement.clientWidth * 100;
                } else if (direction == 'horiz') {
                    pos = (e.pageY - navwrap[0].clientHeight) / msgs[0].clientHeight * 100;
                }
                if (pos < 10) {
                    pos = 10;
                }
                if (pos > 90) {
                    pos = 90;
                }
                setpos(pos);
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