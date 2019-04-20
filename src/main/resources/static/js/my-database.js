$(document).ready(function () {

    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");
    $(document).ajaxSend(function (e, xhr, options) {
        if (token && header)
            xhr.setRequestHeader(header, token);
    });

    // My keyspace open/close tab
    $(".arrow-left-button").on("click", function () {
        $(".keyspace-panel").css("display", "none");
        $(".mini-keyspace").css("display", "block");
        $(".database-panel").css("width", "96%");
    });
    $(".mini-keyspace").on("click", function () {
        $(".keyspace-panel").css("display", "block");
        $(".mini-keyspace").css("display", "none");
        $(".database-panel").css("width", "77%");
    });

    // JS for a tree nav model
    $('.tree-toggle').click(function () {
        $(this).parent().children('ul.tree').toggle(200);
    });

    // Put the user-keyspace first in the list
    $('.user-keyspaces-li').prependTo('.nav-menu-list-style');

    // Display or not the password inputs if the enable checbox is changed
    $("#password-enabled").change(function () {
        if (this.checked) {
            $("#create-keyspace-password").css('display', 'block');
        } else {
            $("#password-create-keyspace").val(null);
            $("#confirmPassword").val(null);
            $("#create-keyspace-password").css('display', 'none');
        }
    });

    // Auto select the 3rd option from select
    $("#replication-factor-select > option:nth-child(3)").attr('selected', true);
    // Auto check the durable writes checkbox
    $("#durable-writes").attr("checked", true);

    // popovers are closed when click anywhere else
    $('[data-toggle="popover"]').popover({
        trigger: "manual",
        container: "body",
        placement: "left"
    }).click(function () {
        var pop = $(this);
        pop.popover("show")
        pop.on('shown.bs.popover', function () {
            setTimeout(function () {
                pop.popover("hide")
            }, 4000);
        })
    })

    // initialize tooltips
    $('[data-toggle="tooltip"]').tooltip();

    // change panel
    $(".database-menu-link").on("click", function (e) {
        e.preventDefault();
        $("#panel-input").val($(this).data("panel"));
        $("#panel-form").submit();
    });

    // less info on click
    $(".keyspace-manage-minus").on('click', function () {
        $(this).parent().find(".row").slideToggle(150);
        if ($(this).hasClass("glyphicon-minus")) {
            $(this).removeClass("glyphicon-minus");
            $(this).addClass("glyphicon-plus")
        } else {
            $(this).removeClass("glyphicon-plus");
            $(this).addClass("glyphicon-minus")
        }
    });

    // select2 (live search for users)
    $("#add-user-to-keyspace-username").select2({
        width: '100%',
        placeholder: "Select a User",
        allowClear: true,
        ajax: {
            url: searchUserLiveUrl,
            dataType: 'json',
            data: function (params) {
                var query = {
                    search: params.term,
                    from: "database"
                }
                return query;
            },
            processResults: function (data) {
                return data;
            }
        }
    });

    $("#remove-user-from-keyspace-username").select2({
        width: '100%',
        placeholder: "Select a User",
        allowClear: true,
        ajax: {
            url: searchUserLiveUrl,
            dataType: 'json',
            data: function (params) {
                var query = {
                    search: params.term,
                    from: "keyspace"
                }
                return query;
            },
            processResults: function (data) {
                return data;
            }
        }
    });

    // safe delete for keyspace
    $("#delete-keyspace-form").on("submit", function (e) {
        if(!$("#confirmKeyspaceDelete").is(":checked")) {
            e.preventDefault();
            alert("Confirm deletion first!");
        }
    });

    // submit edit keyspace on change
    $("#keyspace-replication-factor2").on("change", function () {
        $("#keyspace-edit-submit").css("display", "block");
    });


    $("#keyspace-durable-writes2").on("change", function () {
        if($(this).val() === "false") {
            $("#keyspace-durable-writes2-checkbox").prop("checked", false);
        } else {
            $("#keyspace-durable-writes2-checkbox").prop("checked", true);
        }
        $("#keyspace-edit-submit").css("display", "block");
    });
});