'use strict';

angular.module('video').controller('signup', ['$scope', '$location', 'SecurityService', function ($scope, $location, SecurityService) {
    $scope.signup = function (username, password, email, captcha) {
        if (!$scope.form.$errvalidate()) {
            $scope.errors = {};
            return;
        }
        SecurityService.signup(username, password, email, captcha).then(function () {
            $location.url('/profile');
        }, function (data) {
            $scope.recaptcha.reset();
            $scope.errors = data;
        });
    };
}]);