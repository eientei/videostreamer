'use strict';

angular.module('video').directive('preview', ['$templateCache', '$document', '$compile', '$animate', '$position', '$q', function ($templateCache, $document, $compile, $animate, $position, $q) {
    return {
        restrict: 'A',
        link: function(scope, element, attrs, preview) {
            var child = null;
            var deferred = null;

            function checkParents(parent, child) {
                var node = child;
                while (node != null) {
                    if (node == parent) {
                        return true;
                    }
                    node = node.parentNode;
                }
                return false;
            }

            var mouseout = function (e) {
                if (child) {
                    if (checkParents(child.div[0], e.relatedTarget)) {
                        return;
                    }
                    $animate.removeClass(child.div, 'in').then(function () {
                        child.div.remove();
                        child.$destroy();
                        child = null;
                        if (deferred) {
                            deferred.resolve();
                        }
                    });
                    child.$apply();
                    $document.off('mouseout', mouseout);
                }
            };

            element.on('mouseenter', function () {
                if (deferred) {
                    return;
                }

                deferred = $q.defer();
                deferred.promise.then(makePreview);

                if (!child) {
                    deferred.resolve();
                }
            });


            function makePreview() {

                scope.preview(attrs.preview).then(function (result) {
                    var div = angular.element('<div class="popover fade" style="padding: 4px; display: block;"/>');

                    if (scope.div) {
                        scope.div.append(div);
                    } else {
                        angular.element(document.body).append(div);
                    }

                    if (result == null) {
                        div.html('<div class="popover-content"><em>404 - Not Found</em></div>');
                    } else {
                        div.html('<div class="popover-content">' + $templateCache.get('Message.html') + '</div>');
                    }

                    div.prepend('<div class="arrow"></div>');

                    child = scope.$new();
                    child.div = div;
                    child.message = result;
                    child.defplacement = (scope.defplacement) ? scope.defplacement : 'top';
                    $compile(div)(child);


                    div.css({
                        position: 'fixed'
                    });

                    var height = div.prop('offsetHeight');
                    var width  = div.prop('offsetWidth') - element.prop('offsetWidth');
                    var offset = $position.offset(element);

                    if (offset.top - height < 0) {
                        child.defplacement = 'bottom';
                    } else if (offset.top + height > document.body.scrollHeight) {
                        child.defplacement = 'top';
                    }

                    div.addClass(child.defplacement);

                    var corr = element.prop('offsetHeight') / 2;

                    div.css({
                        top: (offset.top - ((child.defplacement == 'top') ? height - corr : -corr)) + 'px',
                        left: (offset.left - width/2) + 'px'
                    });

                    $animate.addClass(div, 'in');
                    $document.on('mouseout', mouseout);
                    deferred = null;
                });
            }
        }
    };
}]);