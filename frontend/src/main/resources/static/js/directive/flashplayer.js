'use strict';

angular.module('video').directive('flashplayer', [function () {
    return {
        scope: {
            vars: '@',
            src: '@'
        },
        restrict: 'A',
        link: function (scope, element, attr) {
            var src_dir = '<object style="flex-grow: 1" id="flashplayer" type="application/x-shockwave-flash" data="' + scope.src + '">'
                + '<param name="allowFullscreen" value="true">'
                + '<param name="scale" value="noscale">'
                + '<param name="bgcolor" value="#000000">'
                + '<param name="wmode" value="direct">'
                + '<param name="flashvars" value="' + scope.vars + '&stub=false">'
                + '</object>';

            var src_trans = '<object style="flex-grow: 1" id="flashplayer" type="application/x-shockwave-flash" data="' + scope.src + '">'
                + '<param name="allowFullscreen" value="true">'
                + '<param name="scale" value="noscale">'
                + '<param name="bgcolor" value="#000000">'
                + '<param name="wmode" value="transparent">'
                + '<param name="flashvars" value="' + scope.vars + '&stub=true">'
                + '</object>';
            var olddir = null;
            var timeout = null;
            scope.$on('flash', function (ev, dir) {
                clearTimeout(timeout);
                if (olddir != dir) {
                    setTimeout(function () {
                        element.children().remove();
                        element.append(angular.element(dir ? src_dir : src_trans));
                        olddir = dir;
                    }, 1000);
                }
            });
            element.append(angular.element(src_trans));
        }
    }
}]);