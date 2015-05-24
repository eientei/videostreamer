'use strict';

angular.module('videostreamer').directive('trusthtml', ['$compile', function ($compile) {
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {
            element.html(scope.$eval(attrs.trusthtml));
            $compile(element.contents())(scope);
        }
    };
}]);