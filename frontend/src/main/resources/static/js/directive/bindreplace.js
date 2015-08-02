'use strict';

angular.module('video').directive('bindReplace', [function () {
    return {
        scope: {
            bindReplace: '='
        },
        restrict: 'A',
        link: function(scope, element, attrs) {
            element.append(scope.bindReplace);
        }
    };
}]);