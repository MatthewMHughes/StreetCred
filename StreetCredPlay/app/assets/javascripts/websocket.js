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
                var load = document.getElementById("loadingMsg");
                if (load != null){load.innerHTML = "";}
                var widgetid = "id-"+String(message.id);
                var div = document.getElementById("tweets");
                var text = document.createElement("div");
                var loadCred = document.createTextNode("Loading Credibility ...");
                text.setAttribute("id", widgetid);
                text.setAttribute("class", "row");
                var headCred = document.createElement("h1");
                headCred.setAttribute("id", "id-"+String(message.id)+"-cred");
                div.appendChild(text);
                twttr.widgets.createTweet(
                    message.status,
                    document.getElementById(widgetid),
                    {
                        theme: 'dark'
                    }
                );
                headCred.appendChild(loadCred);
                text.appendChild(headCred);
                document.getElementById('tweet').innerHTML+=message.status;
            case "displayCred":
                var id = "id-"+String(message.id)+"-cred";
                div = document.getElementById(id);
                div.innerHTML = "";
                text = document.createTextNode(message.status);
                div.appendChild(text);
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
    var tweets = document.getElementById("tweets");
    tweets.innerHTML = "";
    var load = document.createElement("h1");
    var loadingMessage = document.createTextNode("Loading Tweets ...");
    load.appendChild(loadingMessage);
    load.setAttribute("id", "loadingMsg");
    tweets.appendChild(load);
    var searchText = document.getElementById("searchText").value;
    console.log(searchText);
    ws.send(JSON.stringify({
        messageType: "doSearch",
        query: searchText
    }));
}