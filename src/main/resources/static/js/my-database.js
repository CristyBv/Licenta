$(document).ready(function() {
    $(".arrow-left-button").on("click", function() {
        $(".keyspace-panel").css("display", "none");
        $(".mini-keyspace").css("display", "block");
        $(".database-panel").css("width", "96%");
    });
    $(".mini-keyspace").on("click", function() {
        $(".keyspace-panel").css("display", "block");
        $(".mini-keyspace").css("display", "none");
        $(".database-panel").css("width", "77%");
    });

    $('.tree-toggle').click(function () {	$(this).parent().children('ul.tree').toggle(200);
    });
    $(function(){
        $('.tree-toggle').parent().children('ul.tree').toggle(200);
    })
});