'use strict';

angular.module('videostreamer').directive('isCurrent', ['$route', function ($route) {
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {
            function mark(current) {
                if (attrs.href == '/' && current == attrs.href) {
                    element.parent().addClass('active');
                } else if (attrs.href != '/' && current.indexOf(attrs.href) == 0) {
                    element.parent().addClass('active');
                } else {
                    element.parent().removeClass('active');
                }
            }

            scope.$on('$routeChangeSuccess', function () {
                var route = $route.current.$$route;
                var path = null;
                if (route) {
                    path = route.originalPath;
                }
                mark(path);
            });
        }
    };
}]);