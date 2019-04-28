$(document).ready(function() {

    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");
    $(document).ajaxSend(function(e,xhr,options) {
        if(token && header)
            xhr.setRequestHeader(header, token);
    });

    $("#avatar-change-button").on('click', function() {
        var url = $(".file-upload").val();
        $.ajax({
            type: "post",
            url: changeAvatarUrl,
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify({
                "avatar" : url
            }),
            dataType: "json"
        }).done(function(result) {
            //$('.avatar').attr('src', url);
            alert(result);
            location.reload();
        }).fail(function(data) {
            alert("Server error!");
        });
    });
});


    // var readURL = function(input) {
    //     if (input.files && input.files[0]) {
    //         var reader = new FileReader();
    //         reader.onload = function (e) {
    //             var avatar = {};
    //             avatar["avatar"] = e.target.result;
    //             $.ajax({
    //                 type: "post",
    //                 url: "/change-avatar/",
    //                 contentType: "application/json; charset=utf-8",
    //                 data: JSON.stringify(avatar)
    //             }).done(function() {
    //                 alert("da");
    //                 $('.avatar').attr('src', e.target.result);
    //             }).fail(function() {
    //                 alert("Invalid file!")
    //             });
    //         };
    //         reader.readAsDataURL(input.files[0]);
    //     }
    // };
//
//
