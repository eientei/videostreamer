'use strict';

angular.module('video').directive('errorTemplate', ['ErrortemplateService', function (ErrortemplateService) {
    return {
        scope: {
            errorTemplate: '='
        },
        restrict: 'A',
        link: function (scope, element, attr) {
            angular.forEach(scope.errorTemplate, function (alias) {
                ErrortemplateService.map(alias, attr.id);
            })
        }
    }
}]);