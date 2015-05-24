'use strict';

angular.module('videostreamer').filter('momentify', [function () {
    return function (input) {
        return moment.duration(input, 'ms').humanize();
    };
}]);