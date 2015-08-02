'use strict';

angular.module('video').filter('momentify', [function () {
    return function (input) {
        return moment.duration(input, 'ms').humanize();
    };
}]);