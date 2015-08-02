'use strict';

angular.module('video').directive('formErrors', ['$compile', 'ErrortemplateService', function ($compile, ErrortemplateService) {
    return {
        require: '^form',
        template: '<alert ng-show="alerts.length" close="alerts=[]"><ul><li ng-repeat="alert in alerts" bind-replace="alert"></li></ul></alert>',
        restrict: 'A',
        link: function(scope, element, attrs, form) {
            var localerrs = {};

            form.$errvalidate = function () {
                makeErrors(localerrs);
                return objempty(localerrs);
            };

            scope.$watch(form.$name + '.$submitted', function (n) {;
                if (!form.$valid) {
                    if (n) {
                        form.$submitted = false;
                    }
                    return;
                }
                if (n) {
                    var prev;
                    var unreg = scope.$watch(attrs.formErrors, function (n, o) {
                        prev = o;
                        if (n === o) {
                            return;
                        }
                        makeErrors(merge(prev, n));
                        unreg();
                    });
                    form.$submitted = false;
                }
            });

            function objempty(obj) {
                var empty = true;
                for (var v in obj) {
                    if (obj.hasOwnProperty(v) && obj[v].length) {
                        empty = false;
                        break;
                    }
                }
                return empty;
            }

            function merge(prev, n) {
                var out = prev;
                for (var v in n) {
                    if (n.hasOwnProperty(v)) {
                        if (prev.hasOwnProperty(v)) {
                            out[v] = prev[v];
                            angular.forEach(n[v], function (z) {
                                var free = true;
                                angular.forEach(out[v], function (y) {
                                    if (ErrortemplateService.synonyms(z, y)) {
                                        free = false;
                                        return false;
                                    }
                                });
                                if (free) {
                                    out[v].push(z);
                                }
                            });
                        } else {
                            out[v] = n[v];
                        }
                    }
                }
                return out;
            }

            function makeErrors(errs) {
                scope.alerts = [];
                for (v in errs) {
                    if (errs.hasOwnProperty(v) && errs[v] && errs[v].length > 0) {
                        angular.forEach(errs[v], function (n) {
                            var otmpl = ErrortemplateService.get(n);
                            if (!otmpl) {
                                otmpl = 'Generic error for field ' + v + ': ' + n;
                            }
                            var tmpl = '<div>' + otmpl + '</div>';
                            var localscope = scope.$new();
                            localscope.name = v;
                            var compiled = $compile(tmpl)(localscope);
                            scope.alerts.push(compiled[0]);
                        });
                    }
                }
                return scope.alerts.length == 0;
            }

            function mergeErrors(name, cur, old) {
                if (!scope[attrs.formErrors]) {
                    scope[attrs.formErrors] = {};
                }

                if (name && (cur || old)) {
                    if (!scope[attrs.formErrors][name]) {
                        scope[attrs.formErrors][name] = [];
                    }
                    if (old) {
                        angular.forEach(old, function (v) {
                            var idx = scope[attrs.formErrors][name].indexOf(v);
                            if (idx > -1) {
                                scope[attrs.formErrors][name].splice(idx, 1);
                            }
                        })
                    }
                    if (cur) {
                        angular.forEach(cur, function (v) {
                            scope[attrs.formErrors][name].push(v);
                        });
                    }
                }
            }

            function setupFieldListener(name) {
                scope.$watch(form.$name + '.' + name + '.$error', function (errs) {
                    var old = localerrs[name];
                    if (!old) {
                        old = [];
                    }
                    localerrs[name] = [];
                    for (var e in errs) {
                        if (errs.hasOwnProperty(e)) {
                            localerrs[name].push(e);
                        }
                    }
                    var cur = localerrs[name];
                    mergeErrors(name, cur, old);
                }, true);
            }

            for (var v in form) {
                if (form.hasOwnProperty(v) && v.indexOf('$') != 0) {
                    setupFieldListener(v);
                }
            }
        }
    };
}]);