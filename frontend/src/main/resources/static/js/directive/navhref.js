'use strict';

angular.module('video').directive('navHref', ['$location', function ($location) {
    return {
        restrict: 'A',
        link: function (scope, element, attr) {
            element.attr('href', attr.navHref);
            var liparent = element.parent();
            scope.$on('$routeChangeSuccess', function () {
                var isprofile  = attr.navHref.indexOf('/profile') == 0 && $location.path().indexOf('/profile') == 0;
                if ($location.path() == attr.navHref || isprofile) {
                    liparent.addClass('active');
                } else {
                    liparent.removeClass('active');
                }
            });
        }
    }
}]);