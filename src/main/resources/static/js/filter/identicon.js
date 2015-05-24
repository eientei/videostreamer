'use strict';

angular.module('videostreamer').filter('identicon', [function () {
    return function (input) {
        return 'http://gravatar.com/avatar/' + input + '?d=identicon&s=64'
    };
}]);