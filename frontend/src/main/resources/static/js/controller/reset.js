'use strict';

angular.module('video').controller('reset', ['$scope', 'SecurityService', function ($scope, SecurityService) {
    $scope.reset = function (username, email, captcha) {
        if (!$scope.form.$errvalidate()) {
            $scope.errors = {};
            return;
        }
        SecurityService.reset(username, email, captcha).then(function () {
            $scope.success = true;
        }, function (data) {
            $scope.recaptcha.reset();
            $scope.errors = data;
        });
    };
}]);