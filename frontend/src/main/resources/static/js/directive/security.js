'use strict';

angular.module('video').directive('secAnon', [function () {
    return {
        restrict: 'A',
        link: function (scope, element) {
            element.addClass('ng-hide');
            scope.$watch('user.username', function (n) {
                if (!n) {
                    return;
                }
                if ('anonymous' === n) {
                    element.removeClass('ng-hide');
                } else {
                    element.addClass('ng-hide');
                }
            });
        }
    }
}]).directive('secUser', [function () {
    return {
        restrict: 'A',
        link: function (scope, element) {
            element.addClass('ng-hide');
            scope.$watch('user.username', function (n) {
                if (!n) {
                    return;
                }
                if ('anonymous' === n) {
                    element.addClass('ng-hide');
                } else {
                    element.removeClass('ng-hide');
                }
            });
        }
    }
}]);