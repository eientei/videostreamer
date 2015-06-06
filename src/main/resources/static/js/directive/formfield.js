'use strict';

angular.module('videostreamer').directive('form', ['$templateCache', function ($templateCache) {
    return {
        restrict: 'E',
        scope: false,
        link: function(scope, element, attrs) {
            var name = attrs.name;
            var form = scope[name];
            for (var v in form) {
                if (form.hasOwnProperty(v) && v[0] != '$') {
                    (function() {
                        var type = v;
                        var el = form[type];
                        scope.$watch(name + '.' + v + '.$viewValue', function () {
                            for (var n in el.$error) {
                                if (el.$error.hasOwnProperty(n)) {
                                    delete el.$error[n];
                                }
                            }
                            el.$validate();
                        });
                        scope.$watch(name + '.' + type + '.$error', function (val) {
                            val.arr = 0;
                            var tips = [];
                            for (var n in val) {
                                if (val.hasOwnProperty(n)) {
                                    if (n != 'arr') {
                                        val.arr++;
                                        var tmpl = $templateCache.get('desc_' + n + '.html');
                                        if (tmpl) {
                                            tips.push(tmpl);
                                        }
                                    }
                                }
                            }
                            form[type].tooltip = tips.join(', ');
                        }, true);
                    })();
                }
            }
            var mappings = {
                'Min': 'minlength',
                'Max': 'maxlength',
                'NotNull': 'required',
                'NotEmpty': 'required'
            };
            scope.$watch(attrs.name + '.serverErrors', function (val) {
                if (val) {
                    var tips = [];
                    for (var v in val) {
                        if (val.hasOwnProperty(v)) {
                            console.log(v);
                            if (v == 'form' || !form.hasOwnProperty(v)) {
                                angular.forEach(val[v], function (n) {
                                    form.$error[n] = true;
                                    var tmpl = $templateCache.get('desc_' + n + '.html');
                                    if (tmpl) {
                                        tips.push(tmpl);
                                    }
                                });
                            } else {
                                angular.forEach(val[v], function (n) {
                                    var err = mappings[n];
                                    if (!err) {
                                        err = "unknown";
                                    }
                                    form[v].$error[err] = true;
                                });
                            }
                        }
                    }
                    form.tooltip = tips;
                }
            });
        }
    };
}]);