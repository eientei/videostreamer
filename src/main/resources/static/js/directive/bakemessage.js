'use strict';

angular.module('videostreamer').directive('bakemessage', ['$compile', function ($compile) {
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {
            function process(node) {
                var res = '';
                if (node.type == 'R') {
                    angular.forEach(node.children, function (n) {
                        res += process(n);
                    });
                } else if (node.type == 'P') {
                    res += '<p>'
                    angular.forEach(node.children, function (n) {
                        res += process(n);
                    });
                    res += '</p>'
                } else if (node.type == 'B') {
                    res += '<strong>';
                    angular.forEach(node.children, function (n) {
                        res += process(n);
                    });
                    res += '</strong>';
                } else if (node.type == 'I') {
                    res += '<em>';
                    angular.forEach(node.children, function (n) {
                        res += process(n);
                    });
                    res += '</em>';
                } else if (node.type == 'Q') {
                    res += '<span class="quote">';
                    for (var i = 0; i < node.level; i++) {
                        res += '&gt;';
                    }

                    res += ' ';

                    angular.forEach(node.children, function (n) {
                        res += process(n);
                    });
                    res += '</span>';
                } else if (node.type == 'S') {
                    res += '<span class="spoiler">';
                    angular.forEach(node.children, function (n) {
                        res += process(n);
                    });
                    res += '</span>';
                } else if (node.type == 'F') {
                    res += ' ';
                    if (scope.message.id > node.ref) {
                        res += '<a href="" preview="' + node.ref + '">&gt;&gt;' + node.ref + '</a>'
                    } else {
                        res += '&gt;&gt;' + node.ref;
                    }
                } else if (node.type == 'U') {
                    res += ' ';
                    res += '<a target="_blank" href="' + node.url + '">' + node.url + '</a>';
                } else if (node.type == 'T') {
                    res += ' ';
                    res += node.text;
                } else {
                    console.log(node);
                }

                return res;
            }

            var html = process(scope.$eval(attrs.bakemessage));
            element.html(html);
            $compile(element.contents())(scope);
        }
    };
}]);