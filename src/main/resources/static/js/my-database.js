var tablesTop = [];
var tablesBottom = [];
var tablesSearch = [];
var terminal;
var logTerminal;
var consoleStack = "";
var currentConsoleContent = {};
var consoleMaxResults = 15;

$(document).ready(function () {

    // Put the user-keyspace first in the list
    $('.user-keyspaces-li').prependTo('.nav-menu-list-style');
    // Auto select the 3rd option from select
    $("#replication-factor-select > option:nth-child(3)").attr('selected', true);
    // Auto check the durable writes checkbox
    $("#durable-writes").attr("checked", true);

    ajaxCSRFToken();
    myKeyspacesOpenCloseTab();
    initPopovers();
    initToolTips();
    initSelect2();
    initDataTables();
    addMouseWheelAndContextMenuEvent("#content-div table.dataTable thead th");
    ajaxDataTableStructure();
    safeUpdateDelete();
    onEvents();
    initConsoleScript();
    initSearch();
    initImportExport();
    initLog();
});

function appendLeadingZeroes(n) {
    if (n <= 9) {
        return "0" + n;
    }
    return n
}

function echoLogs(logsContent) {
    logTerminal.clear();
    logTerminal.echo("[[b;red;]Max 100 logs. For more, export keyspace logs!\n");
    for (var i = 0; i < logsContent.length; i++) {
        var date = new Date(Date.parse(logsContent[i]["date"]));
        var formatDate = appendLeadingZeroes(date.getDate()) + "-" + appendLeadingZeroes(date.getMonth() + 1) + "-" + date.getFullYear() + " " + appendLeadingZeroes(date.getHours()) + ":" + appendLeadingZeroes(date.getMinutes()) + ":" + appendLeadingZeroes(date.getSeconds());

        logTerminal.echo("[[bi;;]" + logsContent[i]["username"] + "] - [[;;]" + formatDate);
        if (logsContent[i]["type"] == logsCreate) {
            logTerminal.echo("[[i;green;]" + logsContent[i]["content"]);
        } else if (logsContent[i]["type"] == logsUpdate) {
            logTerminal.echo("[[i;blue;]" + logsContent[i]["content"]);
        } else if (logsContent[i]["type"] == logsDelete) {
            logTerminal.echo("[[i;red;]" + logsContent[i]["content"]);
        }
        logTerminal.echo();
    }
    logTerminal.scroll_to_bottom();
}

function initLog() {
    if ($("#log-div").html() != undefined) {
        logTerminal = $('#log-terminal').terminal(function (cmd) {
            if (cmd[0] == "@") {
                var cmdSplit = cmd.split("@");
                var params = [];
                var inputOk = true;
                if (cmdSplit.length > 1) {
                    if (cmdSplit[1] == "")
                        params["date"] = "today";
                    else {
                        params["date"] = cmdSplit[1];
                    }
                } else
                    inputOk = false;
                if (cmdSplit.length > 2) {
                    if (cmdSplit[2] == "")
                        inputOk = false;
                    else
                        params["username"] = cmdSplit[2];
                }
                if (cmdSplit.length > 3)
                    inputOk = false;
                if (inputOk) {
                    $.ajax({
                        type: "post",
                        url: logFilterUrl,
                        contentType: "application/json; charset=utf-8",
                        data: JSON.stringify({
                            "date": params["date"],
                            "username": params["username"]
                        }),
                        dataType: "json"
                    }).done(function (result) {
                        if (result != null && result.length != 0)
                            echoLogs(result);
                        else if (result.length == 0) {
                            logTerminal.echo("[[i;red;]" + "No data found!");
                        } else if (result == null) {
                            logTerminal.echo("[[i;red;]" + "Invalid! The logs could not be filtered! Please try again!");
                        }
                    }).fail(function () {
                        alert("Server error!");
                    });
                } else {
                    this.echo("[[i;red;]" + "Invalid command!");
                    this.echo("[[i;;]" + logsInfo);
                }
            }
        }, {
            greetings: "Max 100 logs. For more, export keyspace logs!",
            prompt: "[[g;black;]>] ",
            convertLinks: false
        });
        echoLogs(logsContent);
        logTerminal.echo("[[i;;]" + logsInfo);
    }
}

function initImportExport() {
    if ($("#import-export-div").html() != undefined) {
        $("#export-excel-select-table, #export-json-select-table").select2({
            width: '100%',
            placeholder: "Select a Table or a View",
            allowClear: true,
            ajax: {
                url: searchTableViewLiveUrl,
                dataType: 'json',
                data: function (params) {
                    var query = {
                        search: params.term
                    };
                    return query;
                },
                processResults: function (data) {
                    return data;
                }
            }
        });
    }
}

function initSearch() {
    if ($("#search-div").html() != undefined) {
        $("#search-results").find('table').each(function () {
            tablesSearch.push(drawDataTableNoServerSide($(this).attr('id')));
        });
        $("#search-input").on('change', function () {
            $("#search-form").submit();
        });
    }
}

function initConsoleScript() {

    if ($('#terminal').html() != undefined) {

        terminal = $('#terminal').terminal(function (cmd) {
            cmd = cmd.trim();
            if (cmd == "@script") {
                $("#script-div").css("display", "block");
                $("#console-div").removeClass("col-sm-12");
                $("#console-div").addClass("col-sm-6");
                terminal.echo("[[i;#DEB887;]" + scriptMessage);
                changeScriptAjax();
                consoleStack = "";
                return;
            } else if (cmd == "@console") {
                $("#script-div").css("display", "none");
                $("#console-div").removeClass("col-sm-6");
                $("#console-div").addClass("col-sm-12");
                changeConsoleAjax();
                consoleStack = "";
                return;
            } else if (cmd.substr(cmd.length - 1) == ";") {
                consoleStack += cmd;
                consoleInterpretorAjax(consoleStack);
                consoleStack = "";
            } else if (cmd == "@more") {
                if (Object.keys(currentConsoleContent).length > 0) {
                    var content = currentConsoleContent["content"];
                    var nr = currentConsoleContent["nr"] + consoleMaxResults;
                    if (content.length - currentConsoleContent["nr"] < consoleMaxResults)
                        nr = currentConsoleContent["nr"] + (content.length - currentConsoleContent["nr"]);
                    //this.clear();
                    for (var i = currentConsoleContent["nr"]; i < nr; i++) {
                        terminal.echo("[[;#1E90FF;]@Row " + (i + 1));
                        terminal.echo("-----------------------------------------------------------------");
                        Object.keys(content[i]).forEach(function (key) {
                            var spaceDif = "";
                            for (var j = 0; j < 30 - key.length; j++) {
                                spaceDif += " ";
                            }
                            terminal.echo("[[ib;#DEB887;]" + key + "]" + spaceDif + " | " + content[i][key]);
                        });
                        terminal.echo("-----------------------------------------------------------------");
                    }
                    terminal.echo("");
                    currentConsoleContent["nr"] = nr;
                    if(nr >= content.length)
                        currentConsoleContent = {};
                    changeAdminConsoleContentAjax();
                } else {
                    this.echo("No results found!");
                }
            } else {
                consoleStack += cmd + " ";
                this.echo(consoleStack + " ...");
                changeConsoleContentAjax();
            }

        }, {
            greetings: "Specify only the object name and not the keyspace.\n" +
            "You can only query in the current keyspace. Some commands are restricted.\n" +
            "For script page type @script\nTo hide the script type @console\nMax console lines: 700\n",
            prompt: "[[g;white;black]>] ",
            name: "text",
            keymap: {
                'CTRL+C': function (e, original) {
                    original();
                    consoleStack = "";
                }
            },
            onBlur: function (term) {
            },
            onFocus: function (term) {
            },
            onClear: function (term) {
                changeConsoleContentAjax();
            },
            echoCommand: false,
            anyLinks: false,
            convertLinks: false,
            outputLimit: 700
        });

        $("#script-textarea").on("change", function () {
            var content = $(this).val();
            changeScriptContentAjax(content);
        });

        if (consoleViewContent != null && consoleViewContent != undefined) {
            terminal.import_view(consoleViewContent);
        }

        $('#script-textarea').keydown(function (e) {
            if (e.ctrlKey && e.keyCode == 13) {
                var selected = $(this).getSelection().text;
                if (selected != "") {
                    scriptInterpretorAjax(selected);
                } else {
                    scriptInterpretorAjax($(this).val());
                }
            }
        });

        function consoleInterpretorAjax(consoleStack) {
            $.ajax({
                type: "post",
                url: consoleInterpretorUrl,
                contentType: "application/json; charset=utf-8",
                data: JSON.stringify({
                    "query": consoleStack
                }),
                dataType: "json"
            }).done(function (result) {
                doneAjaxTerminalShow(result)
            }).fail(function () {
                alert("Server error!");
            });
        }

        function scriptInterpretorAjax(selected) {
            changeConsoleContentAjax(true);
            var selSplit = selected.split(";");
            var i = 0;
            var batch = [], funct = [];
            var batchStart = [], functStart = [];
            var batchEnd = [], functEnd = [];
            var batches = [], functs = [];
            while (i < selSplit.length) {
                var query = selSplit[i].toLowerCase()
                if (query.includes("begin") && query.includes("batch")) {
                    batch.push(selSplit[i], ";");
                    batchStart.push(i);
                } else if (query.includes("apply") && query.includes("batch") && batch.length > 0) {
                    batch.push(selSplit[i]);
                    batchEnd.push(i);
                    batches.push(batch.join(""));
                    batch = [];
                } else if (batch.length > 0) {
                    batch.push(selSplit[i], ";");
                }

                if (query.includes("create") && query.includes("function") && query.includes("$$")) {
                    funct.push(selSplit[i], ";");
                    functStart.push(i);
                } else if (query.includes("$$") && funct.length > 1) {
                    funct.push(selSplit[i]);
                    functEnd.push(i);
                    functs.push(funct.join(""));
                    funct = [];
                } else if (funct.length > 1) {
                    funct.push(selSplit[i], ";");
                }

                i++;
            }
            var newSplit = [];
            var contor = 0;
            if (batches.length > 0) {
                contor = 0;
                for (i = 0; i < selSplit.length; i++) {
                    if (i == batchStart[contor]) {
                        newSplit.push(batches[contor]);
                    } else if (i < batchStart[contor] || i > batchEnd[contor]) {
                        newSplit.push(selSplit[i]);
                    } else if (i == batchEnd[contor]) {
                        if (contor + 1 < batches.length)
                            contor++;
                    }
                }
            } else {
                newSplit = selSplit;
            }
            var newSplit2 = [];
            if (functs.length > 0) {
                contor = 0;
                for (i = 0; i < selSplit.length; i++) {
                    if (i == functStart[contor]) {
                        newSplit2.push(functs[contor]);
                    } else if (i < functStart[contor] || i > functEnd[contor]) {
                        newSplit2.push(selSplit[i]);
                    } else if (i == functEnd[contor]) {
                        if (contor + 1 < functs.length)
                            contor++;
                    }
                }
            } else {
                newSplit2 = newSplit;
            }
            //alert(JSON.stringify(newSplit2));
            terminal.echo("[[i;green;]\nThe script is running...\nThe valid results will appear in logs too.\nIf you don't want to wait for the results, you can now leave this page.");

            $.ajax({
                type: "post",
                async: true,
                url: scriptInterpretorUrl,
                contentType: "application/json; charset=utf-8",
                data: JSON.stringify({
                    "queries": newSplit2
                }),
                dataType: "json"
            }).done(function (result) {
                doneAjaxScriptTerminalShow(result);
            }).fail(function () {
                alert("Server error!");
            });
        }

        function doneAjaxScriptTerminalShow(result) {
            if (result != null && result.length > 0) {
                for (var i = 0; i < result.length; i++) {
                    doneAjaxTerminalShow(result[i], true);
                }
            } else {
                terminal.echo("[[;red;]Error! Refresh and try again!\n");
            }
        }

        function doneAjaxTerminalShow(result, noContentChange) {
            currentConsoleContent = {};
            if (result != null) {
                if (result["error"] != null) {
                    terminal.echo("[[i;;]" + result["success"]);
                    terminal.echo("[[g;red;]" + result["error"]);
                } else if (result["success"] != null) {
                    terminal.echo("\n" + result["success"]);
                    terminal.echo("[[;green;]Valid!\n");
                    if (result["type"] == "select" && result["content"] != null) {
                        var content = result["content"];
                        var space = 30;
                        if (content.length > 0) {
                            var nr = content.length;
                            if (content.length > consoleMaxResults) {
                                nr = consoleMaxResults;
                                currentConsoleContent["nr"] = nr;
                                currentConsoleContent["content"] = content;
                            }
                            for (var i = 0; i < nr; i++) {
                                terminal.echo("[[;#1E90FF;]@Row " + (i + 1));
                                terminal.echo("-----------------------------------------------------------------");
                                Object.keys(content[i]).forEach(function (key) {
                                    var spaceDif = "";
                                    for (var j = 0; j < space - key.length; j++) {
                                        spaceDif += " ";
                                    }
                                    terminal.echo("[[ib;#DEB887;]" + key + "]" + spaceDif + " | " + content[i][key]);
                                });
                                terminal.echo("-----------------------------------------------------------------");
                            }
                            if(Object.keys(currentConsoleContent).length > 0)
                                terminal.echo("[[i;;]Too many results! Type ][[;green;]@more][[i;;] to load more!\nIf you leave the page, this will no longer work for current select query!");
                            else
                                terminal.echo("");
                        } else {
                            terminal.echo("[[i;;]No results found!");
                        }
                    }
                }
            } else {
                terminal.echo("[[i;;]" + result["success"]);
                terminal.echo("[[;red;]Error! Refresh and try again!\n");
            }
            if (noContentChange != true)
                changeConsoleContentAjax();
        }

        // function scriptRunAjax(selected) {
        //     $.ajax({
        //         type: "post",
        //         url: scriptRunUrl,
        //         contentType: "application/json; charset=utf-8",
        //         data: JSON.stringify({
        //             "content": selected
        //         }),
        //         dataType: "json"
        //     }).done(function (result) {
        //     }).fail({});
        // }

        function changeConsoleContentAjax(check) {
            var view = terminal.export_view();
            if (check != undefined) {
                if (view["lines"].length > 100)
                    terminal.clear();
                view = terminal.export_view();
            }
            $.ajax({
                type: "post",
                async: true,
                url: consoleChangeContentUrl,
                contentType: "application/json; charset=utf-8",
                data: JSON.stringify({
                    "content": view
                }),
                dataType: "json"
            }).done(function (result) {
            }).fail({});
        }

        function changeScriptContentAjax(content) {
            if (content !== undefined) {
                $.ajax({
                    type: "post",
                    url: scriptChangeContentUrl,
                    contentType: "application/json; charset=utf-8",
                    data: JSON.stringify({
                        "content": content
                    }),
                    dataType: "json"
                }).done(function (result) {
                }).fail({});
            }
        }

        function changeScriptAjax() {
            $.ajax({
                type: "post",
                url: scriptChangeUrl,
                contentType: "application/json; charset=utf-8",
                data: JSON.stringify({}),
                dataType: "json"
            }).done(function (result) {
            }).fail({});
        }

        function changeConsoleAjax() {
            $.ajax({
                type: "post",
                url: consoleChangeUrl,
                contentType: "application/json; charset=utf-8",
                data: JSON.stringify({}),
                dataType: "json"
            }).done(function (result) {
            }).fail({});
        }
    }
}
function myKeyspacesOpenCloseTab() {
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
}
function initPopovers() {
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
    });
}
function initSelect2() {
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
                };
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
                };
                return query;
            },
            processResults: function (data) {
                return data;
            }
        }
    });

    $("#add-column-select-table").select2({
        width: '100%',
        placeholder: "Select a Table",
        allowClear: true,
        dropdownParent: $("#create-column-modal"),
        ajax: {
            url: searchTableLiveUrl,
            dataType: 'json',
            data: function (params) {
                var query = {
                    search: params.term
                };
                return query;
            },
            processResults: function (data) {
                return data;
            }
        }
    });

    $("#add-index-select-table").select2({
        width: '100%',
        placeholder: "Select a Table",
        allowClear: true,
        dropdownParent: $("#create-index-modal"),
        ajax: {
            url: searchTableLiveUrl,
            dataType: 'json',
            data: function (params) {
                var query = {
                    search: params.term
                };
                return query;
            },
            processResults: function (data) {
                return data;
            }
        }
    });

    $("#add-index-select-column").select2({
        width: '100%',
        placeholder: "Select a Column",
        allowClear: true,
        dropdownParent: $("#create-index-modal"),
        ajax: {
            url: searchColumnLiveUrl,
            dataType: 'json',
            data: function (params) {
                var table = $("#add-index-select-table").find(":selected").text();
                var query = null;
                if (table != null && table != "") {
                    query = {
                        search: params.term,
                        tableName: table
                    };
                } else {
                    query = {
                        search: params.term
                    };
                }
                return query;
            },
            processResults: function (data) {
                return data;
            }
        }
    });

    $("#add-view-select-table").select2({
        width: '100%',
        placeholder: "Select a Table",
        allowClear: true,
        dropdownParent: $("#create-view-modal"),
        ajax: {
            url: searchTableLiveUrl,
            dataType: 'json',
            data: function (params) {
                var query = {
                    search: params.term
                };
                return query;
            },
            processResults: function (data) {
                return data;
            }
        }
    });

    $("#add-trigger-select-table").select2({
        width: '100%',
        placeholder: "Select a Table",
        allowClear: true,
        dropdownParent: $("#create-trigger-modal"),
        ajax: {
            url: searchTableLiveUrl,
            dataType: 'json',
            data: function (params) {
                var query = {
                    search: params.term
                };
                return query;
            },
            processResults: function (data) {
                return data;
            }
        }
    });

    $("#add-aggregate-select-state-function").select2({
        width: '100%',
        placeholder: "Select a Function",
        allowClear: true,
        dropdownParent: $("#create-aggregate-modal"),
        ajax: {
            url: searchFunctionLiveUrl,
            dataType: 'json',
            data: function (params) {
                var query = {
                    search: params.term
                };
                return query;
            },
            processResults: function (data) {
                return data;
            }
        }
    });

    $("#add-aggregate-select-final-function").select2({
        width: '100%',
        placeholder: "Select a Function",
        allowClear: true,
        dropdownParent: $("#create-aggregate-modal"),
        ajax: {
            url: searchFunctionLiveUrl,
            dataType: 'json',
            data: function (params) {
                var query = {
                    search: params.term
                };
                return query;
            },
            processResults: function (data) {
                return data;
            }
        }
    });
}
function ajaxDataTableStructure() {
    $("#data-div table.dataTable tbody").on("click", '.data-table-structure', function (e) {
        e.preventDefault();
        var tr = $(this).closest("tr");
        var data = tr.attr("id");
        $.ajax({
            type: "post",
            url: getTableStructureUrl,
            contentType: "application/json",
            data: JSON.stringify({
                "data": data
            }),
            dataType: "json"
        }).done(function (json) {
            Object.keys(json).forEach(function (key) {
                if (json[key] != null) {
                    // if the value is a collection then we convert it to JSON string and replace " with '
                    if (typeof json[key] == "object")
                        $("#show-data-structure-row-form").find("textarea[name='" + key + "_readonly']").val(JSON.stringify(json[key]).replace(/"/g, "\'"));
                    else
                        $("#show-data-structure-row-form").find("textarea[name='" + key + "_readonly']").val(json[key]);
                }
            });
            $("#data-div").find(".row-modal").modal("show");
        }).fail(function () {
            alert("Server error!");
        });
    });
}
function safeUpdateDelete() {
    $("#data-row-update-button").on('click', function (e) {
        if (confirm("Are you sure you want to update?")) {
            $("#request-type").val("update");
            $("#show-data-row-form").submit();
        } else {
            e.preventDefault();
        }
    });
    $("#data-row-delete-button").on('click', function (e) {
        if (confirm("Are you sure you want to delete?")) {
            $("#request-type").val("delete");
            $("#show-data-row-form").submit();
        } else {
            e.preventDefault();
        }
    });
    $("#data-structure-update-tables-button, #data-structure-update-views-button").on('click', function (e) {
        if (confirm("Are you sure you want to update?")) {
            $("#show-data-structure-row-form").submit();
        } else {
            e.preventDefault();
        }
    });
    $("#data-structure-delete-tables-button").on("click", function (e) {
        if (confirm("Are you sure you want to delete this table? All the data will be lost!")) {
            var tableName = $("#show-data-structure-row-form").find("textarea[name='table_name_readonly']").val();
            $("#delete-tables-name-input").val(tableName);
            $("#delete-tables-form").submit();
        }
        e.preventDefault();
    });
    $("#data-structure-delete-views-button").on("click", function (e) {
        if (confirm("Are you sure you want to delete this view?")) {
            var viewName = $("#show-data-structure-row-form").find("textarea[name='view_name_readonly']").val();
            $("#delete-views-name-input").val(viewName);
            $("#delete-views-form").submit();
        }
        e.preventDefault();
    });
    $("#data-structure-update-columns-button").on("click", function (e) {
        if (confirm("Are you sure you want to update this column?")) {
            var requestType = $("#show-data-structure-row-form").find("input[name='requestType']");
            requestType.val(requestType.val() + "@update");
            $("#show-data-structure-row-form").submit();
        }
        e.preventDefault();
    });
    $("#data-structure-delete-columns-button").on("click", function (e) {
        if (confirm("Are you sure you want to delete this column?")) {
            var requestType = $("#show-data-structure-row-form").find("input[name='requestType']");
            requestType.val(requestType.val() + "@delete");
            $("#show-data-structure-row-form").submit();
        }
        e.preventDefault();
    });
    $("#data-structure-delete-types-button").on("click", function (e) {
        if (confirm("Are you sure you want to delete this type?")) {
            var typeName = $("#show-data-structure-row-form").find("textarea[name='type_name_readonly']").val();
            $("#delete-types-name-input").val(typeName);
            $("#delete-types-form").submit();
        }
        e.preventDefault();
    });
}
function onEvents() {
    // JS for a tree nav model
    $('.tree-toggle').click(function () {
        $(this).parent().children('ul.tree').toggle(200);
    });

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

    // reset table modifications
    $("#content-div .table-title").on("click", function () {
        tablesBottom[0].columns().every(function () {
            tablesBottom[0].column(this).visible(true);
        });
        $("#content-div table.dataTable thead th").each(function () {
            $(this).find('.th-span').css("padding-left", "0px");
            $(this).find('.th-span').css("padding-right", "0px");
        });
        adjustDataTableColumns(tablesBottom);
        addMouseWheelAndContextMenuEvent();
    });

    // show row data on dblclick
    $("#content-div table.dataTable tbody").on("dblclick", 'tr', function (e) {
        e.preventDefault();
        var rowData = tablesBottom[0].row(this).data();
        Object.keys(rowData).forEach(function (key) {
            if (rowData[key] != null) {
                $("#show-data-row-form").find("textarea[name='" + key + "_readonly']").val(rowData[key]);
            }
        });
        $("#show-row-data-modal").modal("show");
    });

    // show modals
    $("#content-div #insert-row-button").on("click", function (e) {
        $("#insert-row-data-modal").modal("show");
    });
    $(".create-row-button").on("click", function () {
        $("#data-div").find(".create-row-modal").modal("show");
    });

    $("#create-trigger-button").on("click", function () {
        addTriggersToSelect();
    });

    // adjust columns on resize
    $(window).resize(function () {
        adjustDataTableColumns(tablesTop, false);
        adjustDataTableColumns(tablesBottom, false);
    });

    // hover for table/view name td and options td
    $(".data-table-submit, .data-table-structure").hover(function () {
        $(this).closest("td").css('background', "darkgray");
    }, function () {
        $(this).closest("td").css('background', "transparent");
    });
}
function initDataTables() {
    // DataTable for all tables in view-edit panel
    $("#data-div").find('table').each(function () {
        tablesTop.push(drawDataTableNoServerSide($(this).attr('id')));
    });
    $("#content-div").find('table').each(function () {
        tablesBottom.push(drawDataTableServerSide($(this).attr('id')));
    });
}
function initToolTips() {
    // initialize tooltips
    $('[data-toggle="tooltip"]').tooltip();
}
function ajaxCSRFToken() {
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");
    $(document).ajaxSend(function (e, xhr, options) {
        if (token && header)
            xhr.setRequestHeader(header, token);
    });
}
function addTriggersToSelect() {
    if (triggers != null) {
        var triggersList = triggers.split(";");
        for (var i = 0; i < triggersList.length; i++) {
            var option = $('<option value="\'' + triggersList[i] + '\'">' + triggersList[i] + '</option>');
            $("#triggers-create-select").append(option);
        }
    }
}
function addMouseWheelAndContextMenuEvent(id) {
    // if the event already exists, we eliminate it to not add duplicates
    $(id).prop("onmousewheel", null).off("mousewheel");
    $(id).on('mousewheel', function (e) {
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
    $(id).prop("oncontextmenu", null).off("contextmenu");
    $(id).on('contextmenu', function (e) {
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
}
function drawDataTableNoServerSide(id) {
    return $("#" + id).DataTable({
        "ordering": true,
        "lengthMenu": [[1, 3, 5, 10, 20, 50, 100, -1], [1, 3, 5, 10, 20, 50, 100, "All"]],
        "pageLength": 10,
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
    });
}
function drawDataTableServerSide(id) {
    return $("#" + id).DataTable({
        "ordering": true,
        "lengthMenu": [[1, 3, 5, 10, 20, 50, 100, -1], [1, 3, 5, 10, 20, 50, 100, "All"]],
        "pageLength": 10,
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
            "url": getTableDataUrl,
            "dataSrc": function (json) {
                return json.data;
            }
        },
        "initComplete": function (settings, json) {
        },
        "columns": columnsNamesDataTable,
        "drawCallback": function (settings) {
            // highlight td if contains what was searched
            var search = $("#content-div #keyspace-data-content_filter input[type='search']").val();
            if (search != '') {
                $("#content-div table.dataTable tbody td").each(function () {
                    var content = $(this).html();
                    if (content.indexOf(search) != -1) {
                        $(this).css('border', '2px solid black');
                    }
                });
            }
        }
    });
}
