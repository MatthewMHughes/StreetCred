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
                var widgetid = "id-"+String(message.id);
                var div = document.getElementById("tweets");
                var text = document.createElement("div");
                text.setAttribute("id", widgetid);
                text.setAttribute("class", "row");
                div.appendChild(text);
                twttr.widgets.createTweet(
                    message.status,
                    document.getElementById(widgetid),
                    {
                        theme: 'dark'
                    }
                );
                document.getElementById('tweet').innerHTML+=message.status;
            case "displayCred":
                var id = "id-"+String(message.id);
                var msg = "<h1>"+message.status+"</h1>";
                div = document.getElementById(id);
                var head = document.createElement("h1");
                text = document.createTextNode(message.status);
                head.appendChild(text);
                div.appendChild(head);
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