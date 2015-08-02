'use strict';

angular.module('video').directive('chatTyper', [function () {
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {
            var lastev = 0;

            scope.insertText = function (ref) {
                var sel = element[0].selectionStart;
                var val = element.val();
                element.val(val.substring(0, sel) + ref + val.substring(sel));
                element[0].selectionStart = element[0].selectionEnd = sel+ref.toString().length;
                element[0].focus();
            };

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