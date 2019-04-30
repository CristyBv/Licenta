$(document).ready(function () {

    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");
    $(document).ajaxSend(function (e, xhr, options) {
        if (token && header)
            xhr.setRequestHeader(header, token);
    });

    // My keyspace open/close tab
    $(".arrow-left-button").on("click", function () {
        $.ajax({
            type: "post",
            url: myKeyspacesPanelUrl,
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify({
                "position": 'close'
            }),
            dataType: "json"
        }).done(function () {
            $(".keyspace-panel").css("display", "none");
            $(".mini-keyspace").css("display", "block");
            $(".database-panel").css("width", "96%");
            adjustDataTableColumns(tablesTop, false);
        }).fail(function () {
            alert("Server error!");
        });
    });
    $(".mini-keyspace").on("click", function () {
        $.ajax({
            type: "post",
            url: myKeyspacesPanelUrl,
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify({
                "position": 'open'
            }),
            dataType: "json"
        }).done(function () {
            $(".keyspace-panel").css("display", "block");
            $(".mini-keyspace").css("display", "none");
            $(".database-panel").css("width", "77%");
            adjustDataTableColumns(tablesTop, false);
        }).fail(function () {
            alert("Server error!");
        });

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

    //change view-edit panel
    $(".database-view-edit-panel-link").on("click", function (e) {
        e.preventDefault();
        $("#view-edit-panel-input").val($(this).data("panel"));
        $("#view-edit-panel-form").submit();
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
        if (!$("#confirmKeyspaceDelete").is(":checked")) {
            e.preventDefault();
            alert("Confirm deletion first!");
        }
    });

    // submit edit keyspace on change
    $("#keyspace-replication-factor2").on("change", function () {
        $("#keyspace-edit-submit").css("display", "block");
    });

    // edit keyspace with text instead of checkbox
    $("#keyspace-durable-writes2").on("change", function () {
        if ($(this).val() === "false") {
            $("#keyspace-durable-writes2-checkbox").prop("checked", false);
        } else {
            $("#keyspace-durable-writes2-checkbox").prop("checked", true);
        }
        $("#keyspace-edit-submit").css("display", "block");
    });

    // DataTable for all tables in view-edit panel
    var tablesTop = [];
    var tablesBottom = [];
    $("#data-div").find('table').each(function () {
        tablesTop.push(drawDataTableNoServerSide($(this).attr('id')));
    });
    $("#content-div").find('table').each(function () {
        tablesBottom.push(drawDataTableServerSide($(this).attr('id')));
    });
    addMouseWheelAndContextMenuEvent(tablesBottom);


    // adjust columns on resize
    $(window).resize(function () {
        adjustDataTableColumns(tablesTop, false);
        adjustDataTableColumns(tablesBottom, false);
    });

    $(".data-table-submit").hover(function () {
        $(this).closest("td").css('background', "darkgray");
    }, function () {
        $(this).closest("td").css('background', "transparent");
    });

    $("#content-div .table-title").on("click", function () {
        tablesBottom[0].columns().every(function () {
            tablesBottom[0].column(this).visible(true);
        });
        $("#content-div table.dataTable thead th").each(function () {
            $(this).find('.th-span').css("padding-left", "0px");
            $(this).find('.th-span').css("padding-right", "0px");
        });
        adjustDataTableColumns(tablesBottom);
        addMouseWheelAndContextMenuEvent(tablesBottom);
    });


    $("#content-div table.dataTable tbody").on("dblclick", 'tr', function () {
        var rowData = tablesBottom[0].row(this).data();
        Object.keys(rowData).forEach(function (key) {
            if (key == "DT_RowId" && rowData[key] != null) {
                $("#show-data-row-form").find("input[name='oldData']").val(rowData);
            } else if (rowData[key] != null) {
                $("#show-data-row-form").find("textarea[name='" + key + "']").val(rowData[key]);
            }
        });
        $("#show-row-data-modal").modal("show");
    });
});

function addMouseWheelAndContextMenuEvent(tablesBottom) {
    $("#content-div table.dataTable thead th").prop("onmousewheel", null).off("mousewheel");
    $("#content-div table.dataTable thead th").on('mousewheel', function (e) {
        e.preventDefault();
        var padL, padR;
        if (e.originalEvent.wheelDelta > 0) {
            padL = parseInt($(this).find('.th-span').css("padding-left"));
            padR = parseInt($(this).find('.th-span').css("padding-right"));
            $(this).find('.th-span').css("padding-left", padL + 50 + 'px');
            $(this).find('.th-span').css("padding-right", padR + 50 + 'px');
            adjustDataTableColumns(tablesBottom, false);
        } else {
            padL = parseInt($(this).find('.th-span').css("padding-left"));
            padR = parseInt($(this).find('.th-span').css("padding-right"));
            if (padL - 50 >= 0 && padR - 50 >= 0) {
                $(this).find('.th-span').css("padding-left", padL - 50 + 'px');
                $(this).find('.th-span').css("padding-right", padR - 50 + 'px');
                adjustDataTableColumns(tablesBottom, false);
            }
        }
    });
    $("#content-div table.dataTable thead th").prop("oncontextmenu", null).off("contextmenu");
    $("#content-div table.dataTable thead th").on('contextmenu', function (e) {
        e.preventDefault();
        tablesBottom[0].column(this).visible(false);
    });
}

function adjustDataTableColumns(tables, draw) {
    for (var i = 0; i < tables.length; i++) {
        if (draw === undefined || draw == true)
            tables[i].columns.adjust().draw();
        else
            tables[i].columns.adjust();
    }
    //addMouseWheelAndContextMenuEvent(tables);
}

function drawDataTableNoServerSide(id) {
    return $("#" + id).DataTable({
        "ordering": true,
        "lengthMenu": [[1, 3, 5, 10, 20, 50, 100, -1], [1, 3, 5, 10, 20, 50, 100, "All"]],
        "pageLength": 3,
        "stateSave": true,
        responsive: true,
        "scrollY": true,
        'dom': 'Rlfrtip',
        "processing": true, "sScrollX": "100%",
        "sScrollXInner": "110%",
        "bScrollCollapse": true,
        "fixedColumns": {
            "leftColumns": 1
        },
        "autoWidth": true
        // "serverSide": true,
        // "ajax": "/table-data"
    });
}

function drawDataTableServerSide(id) {
    return $("#" + id).DataTable({
        "ordering": true,
        "lengthMenu": [[1, 3, 5, 10, 20, 50, 100, -1], [1, 3, 5, 10, 20, 50, 100, "All"]],
        "pageLength": 3,
        "stateSave": true,
        responsive: true,
        "scrollY": true,
        'dom': 'Rlfrtip',
        "processing": true,
        "sScrollX": "100%",
        "sScrollXInner": "110%",
        "bScrollCollapse": true,
        "fixedColumns": {
            "leftColumns": 1
        },
        "autoWidth": false,
        "serverSide": true,
        "ajax": {
            "type": "GET",
            "url": "/table-data",
            "dataSrc": function (json) {
                // if (columnTypes !== undefined) {
                //     for (var i = 0; i < json.data.length; i++) {
                //         var j = 0;
                //         Object.keys(columnTypes).forEach(function (key) {
                //             var column = columnTypes[key];
                //             json.data[i][j] = formatData(json.data[i][j], key, column);
                //             j++;
                //         });
                //     }
                // }
                return json.data;
            }
        },
        "initComplete": function (settings, json) {
        },
        "columns": columnsNamesDataTable
    });
}
