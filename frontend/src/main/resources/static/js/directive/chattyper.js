'use strict';

angular.module('video').directive('chatTyper', ['TyperService', function (TyperService) {
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {
            var lastev = 0;

            TyperService.setTyper(element);

            element.on('keypress', function (e) {
                var diff = e.timeStamp - lastev;
                if (diff > 100) {
                    scope.notifyKeypress();
                    lastev = e.timeStamp;
                }

                if (e.keyCode == 13 && !e.shiftKey) {
                    e.preventDefault();
                    scope.sendMessage(element.val());
                    element.val('');
                }
            });
        }
    };
}]);