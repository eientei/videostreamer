'use strict';

angular.module('video').controller('login', ['$scope', '$route', '$location', 'SecurityService', function ($scope, $route, $location, SecurityService) {
    $scope.login = function (username, password) {
        SecurityService.login(username, password).then(function () {
            if ($location.path().indexOf('/profile') == 0) {
                $route.reload();
            }
            $scope.username = '';
            $scope.password = '';
            $scope.errlogin = false;
        }, function () {
            $scope.errlogin = true;
        });
    };

    $scope.$on('$routeChangeStart', function (ev, route) {
        if (route.originalPath == '/passwordreset') {
            $scope.errlogin = false;
        }
    });

    $scope.$watch('errlogin', function () {
        setTimeout(function () {
            $scope.$emit('LoginRepos');
        }, 0);
    });

    $scope.logout = function () {
        SecurityService.logout().then(function () {
            if ($location.path().indexOf('/profile') == 0) {
                $location.hash(null);
                $location.path('/');
            }
        });
    };
}]);