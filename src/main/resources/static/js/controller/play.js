'use strict';

var onlineCb = null;
var offlineCb = null;

function streamOnline() {
    if (onlineCb) {
        onlineCb();
    }
}

function streamOffline() {
    if (offlineCb) {
        offlineCb();
    }
}

angular.module('videostreamer').controller('play', ['$rootScope', '$scope', '$routeParams', 'restapi', function ($rootScope, $scope, $routeParams, restapi) {
    $scope.online = true;

    onlineCb = function () {
        $scope.online = true;
        $scope.$apply();
    };

    offlineCb = function () {
        $scope.online = false;
        $scope.$apply();
    };

    $scope.showvideo = !$routeParams.novideo;
    $scope.showchat = !$routeParams.nochat;
    $scope.bufflen = ($routeParams.nobuffer) ? 0.0 : 1.0;
    restapi.streams.stream($routeParams.app, $routeParams.name).success(function (stream) {
        $scope.stream = stream;
    }).error(function (e) {
        $rootScope.errorify(e.status);
    });
}]);