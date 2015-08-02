'use strict';

angular.module('video').filter('identicon', [function () {
    return function (input) {
        return 'http://gravatar.com/avatar/' + input + '?d=identicon&s=64'
    };
}]);