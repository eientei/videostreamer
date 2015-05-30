'use strict';

/*
 <object width="100%" height="100%" param="videoUrl={{rtmpPrefix}}/live/{{streamname}}&amp;buffer={{bufflen}}" type="application/x-shockwave-flash" style="position: absolute; top: 0; bottom: 0; left: 0; right: 0; overflow: hidden; {{(online) ? '': 'position: absolute; width: 0; height: 0'}}" data="/static/swf/yukkuplayer.swf" >
 <param name="allowFullscreen" value="true">
 <param name="scale" value="noscale">
 <param name="bgcolor" value="#ffffff">
 <param name="wmode" value="direct">
 </object>
 */
angular.module('videostreamer').directive('param', [function () {
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {
            var html =
                '<object width="100%" height="100%" type="application/x-shockwave-flash" style="position: absolute; top: 0; bottom: 0; left: 0; right: 0; overflow: hidden" data="/static/swf/yukkuplayer.swf" >'
                + '<param name="allowFullscreen" value="true">'
                + '<param name="scale" value="noscale">'
                + '<param name="bgcolor" value="#000000">'
                + '<param name="wmode" value="direct">'
                + '<param name="flashvars" value="videoUrl=' + scope.rtmpPrefix + '/live/' + scope.streamname + '&amp;buffer=' + scope.bufflen + '">'
                + '</object>';

            var el = angular.element(html);

            scope.$on('flash-offline', function () {
                el.css({
                    'top': 30000 + 'px',
                    'left': 30000 + 'px'
                });
            });

            scope.$on('flash-online', function () {
                el.css({
                    'top': 0,
                    'left': 0
                });
            });

            el.css({
                'top': 30000 + 'px',
                'left': 30000 + 'px'
            });
            element.append(el);
        }
    };
}]);