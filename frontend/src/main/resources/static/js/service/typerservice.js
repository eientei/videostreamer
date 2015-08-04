'use strict';

angular.module('video').service('TyperService', [function () {
    var typer;

    return {
        insertText: function (ref) {
            var sel = typer[0].selectionStart;
            var val = typer.val();
            typer.val(val.substring(0, sel) + ref + val.substring(sel));
            typer[0].selectionStart = typer[0].selectionEnd = sel+ref.toString().length;
            typer[0].focus();
        },
        setTyper: function (t) {
            typer = t;
        }
    };
}]);