'use strict';

angular.module('video').directive('flashplayer', [function () {
    return {
        scope: {
            vars: '@',
            src: '@'
        },
        restrict: 'A',
        link: function (scope, element, attr) {
            var src = '<object style="flex-grow: 1" id="flashplayer" type="application/x-shockwave-flash" data="' + scope.src + '">'
                + '<param name="allowFullscreen" value="true">'
                + '<param name="scale" value="noscale">'
                + '<param name="bgcolor" value="#000000">'
                + '<param name="wmode" value="transparent">'
                + '<param name="flashvars" value="' + scope.vars + '">'
                + '</object>';
            var el = angular.element(src);
            element.replaceWith(el);
        }
    }
}]);
/*

<div flashplayer="" vars="videoUrl={{config.rtmpPrefix}}/{{app}}/{{name}}&amp;buffer={{bufflen}}" src="/swf/yukkuplayer.swf"></div>


    <param name="allowFullscreen" value="true">
    <param name="scale" value="noscale">
    <param name="bgcolor" value="#000000">
    <param name="wmode" value="transparent">
    <param name="flashvars" value="videoUrl={{config.rtmpPrefix}}/{{app}}/{{name}}&amp;buffer={{bufflen}}">
    </object>
    */