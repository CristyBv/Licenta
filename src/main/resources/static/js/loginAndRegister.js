$(document).ready(function () {

    // on click function for Register button
    $('#sing-up-a').click(function (e) {
        changeToRegister();
        e.preventDefault();
    });

    // on click function for Login button
    $('#log-in-a').click(function (e) {
        changeToLogin();
        e.preventDefault();
    });

    // on click function for Login & Register forms
    $('#login-form-link').click(changeToLogin);
    $('#register-form-link').click(changeToRegister);
    $('.recovery').click(changeToRecovery);

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
});

function changeToLogin() {
    $("#login-form").delay(100).fadeIn(100);
    $("#register-form").fadeOut(100);
    $("#recovery-form").fadeOut(100);
    $("#register-form-link").removeClass('active');
    $("#login-form-link").addClass('active');
}

function changeToRegister() {
    $("#register-form").delay(100).fadeIn(100);
    $("#login-form").fadeOut(100);
    $("#recovery-form").fadeOut(100);
    $("#login-form-link").removeClass('active');
    $("#register-form-link").addClass('active');
}

function changeToRecovery() {
    $("#login-form").fadeOut(100);
    $("#recovery-form").delay(100).fadeIn(100);
    $("#login-form-link").removeClass('active');

}
