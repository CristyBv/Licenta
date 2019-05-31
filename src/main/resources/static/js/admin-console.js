var terminal;
var consoleStack = "";

$(document).ready(function () {
    ajaxCSRFToken();
    initTerminal();
});

function initTerminal() {
    if ($('#terminal').html() != undefined) {
        terminal = $('#terminal').terminal(function (cmd) {
            cmd = cmd.trim();
            if (cmd.substr(cmd.length - 1) == ";") {
                consoleStack += cmd;
                adminConsoleInterpretorAjax(consoleStack);
                consoleStack = "";
            } else {
                consoleStack += cmd + " ";
                this.echo(consoleStack + " ...");
                changeAdminConsoleContentAjax();
            }
        }, {
            greetings: "",
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
                changeAdminConsoleContentAjax();
            },
            echoCommand: false,
            anyLinks: false,
            convertLinks: false
        });

        if (adminConsoleViewContent != null && adminConsoleViewContent != undefined) {
            terminal.import_view(adminConsoleViewContent);
        }

    }
}

function adminConsoleInterpretorAjax(consoleStack) {
    $.ajax({
        type: "post",
        url: adminConsoleInterpretorUrl,
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

function doneAjaxTerminalShow(result) {
    if (result != null) {
        if (result["error"] != null) {
            terminal.echo("[[g;red;]" + result["error"]);
        } else if (result["success"] != null) {
            terminal.echo();
            if(result["type"] != "select")
                terminal.echo("[[i;;]" + result["success"]);
            terminal.echo("[[;green;]Valid!\n");
            if (result["type"] == "select") {
                var content = result["success"];
                var space = 30;
                if (content.length > 0) {
                    for (var i = 0; i < content.length; i++) {
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
    changeAdminConsoleContentAjax();
}

function changeAdminConsoleContentAjax(check) {
    var view = terminal.export_view();
    if (check != undefined) {
        if (view["lines"].length > 100)
            terminal.clear();
        view = terminal.export_view();
    }
    $.ajax({
        type: "post",
        url: adminConsoleChangeContentUrl,
        contentType: "application/json; charset=utf-8",
        data: JSON.stringify({
            "content": view
        }),
        dataType: "json"
    }).done(function (result) {
    }).fail({});
}

function ajaxCSRFToken() {
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");
    $(document).ajaxSend(function (e, xhr, options) {
        if (token && header)
            xhr.setRequestHeader(header, token);
    });
}