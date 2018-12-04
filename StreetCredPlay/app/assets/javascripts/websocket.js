window.twttr = (function(d, s, id) {
    var js, fjs = d.getElementsByTagName(s)[0],
        t = window.twttr || {};
    if (d.getElementById(id)) return t;
    js = d.createElement(s);
    js.id = id;
    js.src = "https://platform.twitter.com/widgets.js";
    fjs.parentNode.insertBefore(js, fjs);

    t._e = [];
    t.ready = function(f) {
        t._e.push(f);
    };

    return t;
}(document, "script", "twitter-wjs"));

document.addEventListener('DOMContentLoaded', function () {
    var ws;
    initialize();
    document.getElementById('searchButton')
        .addEventListener('click', searchButtonPressed);
});
function initialize() {
    openWebSocketConnection();


}

// ######################################################################################
// Top level control logic
// ######################################################################################

function openWebSocketConnection() {

    var wsURL = document.getElementById("myBody");
    ws = new WebSocket(wsURL.dataset.ws);
    ws.onmessage = function (event) {
        var message;
        message = JSON.parse(event.data);
        switch (message.messageType) {
            case "init":
                console.log("received");
                break;
            case "statusMessage":
                alert(message.status);
                break;
            case "displayTweet":
                twttr.widgets.createTweet(
                    message.status,
                    document.getElementById('tweets'),
                    {
                        theme: 'dark'
                    }
                );
                console.log(message.status);
                document.getElementById('tweets').innerHTML = "";
                document.getElementById('tweet').innerHTML+=message.status;
            case "displayCred":
                document.getElementById('tweets').innerHTML+=message.status;
                console.log(message);
            default:
                return console.log(message);
        }
    };
    ws.onclose = function (event) {
        // do nothing
    }
}


function searchButtonPressed() {
    console.log("pressed");
    var searchText = document.getElementById("searchText").value;
    console.log(searchText);
    ws.send(JSON.stringify({
        messageType: "doSearch",
        query: searchText
    }));
}