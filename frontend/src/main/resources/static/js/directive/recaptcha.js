'use strict';

angular.module('video').directive('recaptcha', ['RecaptchaService', function (RecaptchaService) {
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {
            scope.recaptcha = {
                value: function () {
                    return element.find('textarea').val();
                },
                reset: function () {
                    RecaptchaService.reset(element);
                }
            };
            RecaptchaService.render(element);
            scope.$on('$routeChangeStart', function () {
                RecaptchaService.destroy();
            })
        }
    };
}]);