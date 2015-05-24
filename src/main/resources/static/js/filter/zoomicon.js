'use strict';

angular.module('videostreamer').filter('zoomicon', ['$filter', '$sanitize', function ($filter, $sanitize) {
    return function (input) {
        return '<img style="padding-top: 4px" src="' + $filter('identicon')(input.hash) + '"/><div>' + $sanitize(input.name) + '</div>';
    };
}]);