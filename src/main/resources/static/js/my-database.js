$(document).ready(function () {

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
    $(function () {
        $('.tree-toggle').parent().children('ul.tree').toggle(200);
    })

    // Put the user-keyspace first in the list
    $('.user-keyspaces-li').prependTo('.nav-menu-list-style');

    // Display or not the password inputs if the enable checbox is changed
    $("#password-enabled").change(function() {
        if(this.checked) {
            $("#create-keyspace-password").css('display','block');
        } else {
            $("#password-create-keyspace").val(null);
            $("#confirmPassword").val(null);
            $("#create-keyspace-password").css('display','none');
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

});