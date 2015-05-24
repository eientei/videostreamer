'use strict';

angular.module('videostreamer').controller('notfound', ['$rootScope', function ($rootScope) {
    $rootScope.errorify(404);
}]);