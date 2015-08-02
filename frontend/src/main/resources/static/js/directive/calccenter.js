'use strict';

angular.module('video').directive('calcCenter', ['$rootScope', function ($rootScope) {
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {
            function repos() {
                var prev = element[0].previousElementSibling;
                var next = element[0].nextElementSibling;
                var parent = element.parent()[0];

                var prevw = prev.offsetWidth;
                var nextw = next.offsetWidth;
                var selfw = element[0].offsetWidth;
                var totalw = parent.offsetWidth;
                var freew = totalw - (prevw + nextw);

                var offl = (freew / 2);

                element[0].style.position = 'relative';
                element[0].style['margin-left'] = offl + 'px';
            }
            setTimeout(function () {
                repos();
            }, 0);

            $rootScope.$on('LoginRepos', function () {
                repos();
            });

            window.onresize = repos;
        }
    };
}]);