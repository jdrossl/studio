(function (window) {
    'use strict';

    var Events = crafter.studio.preview.Topics;
    var origin = 'http://127.0.0.1:8080';
    var communicator = new crafter.Communicator(origin);

    communicator.subscribe(Events.GUEST_CHECKIN, function (url) {
        console.log('Guest checked in');
        setHashPage(url);
    });

    communicator.subscribe(Events.GUEST_CHECKOUT, function () {
        console.log('Guest checked out');
    });

    // Listen to the guest site load
    communicator.subscribe(Events.GUEST_SITE_LOAD, function (message, scope) {

        console.log('Guest loaded');

        // Once the guest window notifies that the page as successfully loaded,
        // add the guest window as a target of messages sent by this window
        communicator.addTargetWindow({
            origin: origin,
            window: getEngineWindow().contentWindow
        });

    });

    function setHashPage(url) {
        window.location.hash = '#/?page=' + url;
    }

    function getEngineWindow() {
        return document.getElementById('engineWindow');
    }

    function goToHashPage() {

        var hash = parseHash(window.location.hash);
        if (hash.site) {
            CStudioAuthoring.Utils.Cookies.createCookie('crafterSite', hash.site);
        }

        getEngineWindow().src = hash.page;

    }

    // TODO better URL support. Find existing lib, use angular or use backbone router?
    function parseHash(hash) {

        var str = hash.replace('#/', ''),
            params = {},
            param;

        str = str.substr(str.indexOf('?') + 1);
        str = str.split('&');

        for (var i = 0; i < str.length; ++i) {
            param = str[i].split('=');
            params[param[0]] = param[1];
        }

        return params;

    }

    window.addEventListener("hashchange", function (e) {
        e.preventDefault();
        goToHashPage();
    }, false);

    window.addEventListener('load', function () {
        if (window.location.hash.indexOf('page') === -1) {
            setHashPage('/');
        } else {
            goToHashPage()
        }
    }, false);

}) (window);