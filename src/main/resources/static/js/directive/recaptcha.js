'use strict';

angular.module('videostreamer').directive('recaptcha', [function () {
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {
            scope.captchael = element;

            var setup = function () {
                grecaptcha.render(element[0], {
                    'sitekey' : attrs.recaptcha
                });
            };

            if (typeof grecaptcha !== 'undefined') {
                setup();
            } else {
                var waiter = function () {
                    if (typeof grecaptcha !== 'undefined') {
                        setup();
                    } else {
                        setTimeout(waiter, 50);
                    }
                };
                setTimeout(waiter, 50);
            }
        }
    };
}]);