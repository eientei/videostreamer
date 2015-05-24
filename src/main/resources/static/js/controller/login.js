'use strict';

angular.module('videostreamer').controller('login', ['$rootScope', '$scope', '$location', 'restapi', function ($rootScope, $scope, $location, restapi) {
    $scope.login = function (username, password) {
        $scope.errlogin = false;
        restapi.security.login(username, password).success(function () {
            restapi.security.user().success(function (user) {
                $scope.username = '';
                $scope.password = '';
                $rootScope.user = user;
                $scope.errlogin = false;
                if ($location.url().indexOf('/signup') >= 0) {
                    $location.url('/profile');
                }
            });
        }).error(function () {
            $scope.errlogin = true;
        });
    };

    $scope.logout = function () {
        restapi.security.logout().success(function () {
            restapi.security.user().success(function (user) {
                $rootScope.user = user;
                if ($location.url().indexOf('/profile') >= 0) {
                    $location.url('/');
                }
            });
        });
    };
}]);