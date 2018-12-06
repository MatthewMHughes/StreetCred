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
                var button = document.createElement("button");
                button.setAttribute("type", "button");
                var buttonId = "id-"+String(message.id)+"-button";
                button.setAttribute("class", "btn btn-primary");
                button.setAttribute("id", buttonId);
                var buttonText = document.createTextNode("Don't Agree?");
                button.appendChild(buttonText);
                div.appendChild(button);
                button.addEventListener("click", function(){
                    updateCredButtonPressed(message.id, message.status);
                }, false);
            default:
                return console.log(message);
        }
    };
    ws.onclose = function (event) {
        // do nothing
    }
}


function searchButtonPressed() {
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

function updateCredButtonPressed(id, cred){
    if (confirm("Please confirm the credibility is incorrect!")) {
        txt = "You pressed OK!";
        console.log(id);
        console.log(cred);
        ws.send(JSON.stringify({
            messageType: "updateCred",
            id: id,
            cred: cred
        }));
    } else {
        txt = "You pressed Cancel!";
    }
}