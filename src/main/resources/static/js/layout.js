$(document).ready(function() {
    // set one envelope glyphicon is there are more than 1
    $("#messages-icon-div").each(function () {
        if($(this).children().length > 1) {
            $(this).children().each(function (index, elem) {
                if(index > 0) {
                    $(this).remove();
                }
            })
        }
    })
});
