// This event listener is linked to the search box. It waits until the search button is pressed or the user hits enter
document.addEventListener('DOMContentLoaded', function () {
    var ws;
    initialize();
    document.getElementById('searchText').addEventListener('keyup', function (e) {
        // if user hits enter we carry out the same functions that if the user clicked the search button
        if (e.keyCode === 13) {
            searchButtonPressed();
        }
    });
    // user clicks search button, execute search button pressed function
    document.getElementById('searchButton')
        .addEventListener('click', searchButtonPressed);
});

function initialize() {
    openWebSocketConnection();
}

var ws; // websocket to the backend



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
                console.log("connection accepted: get Trends");
                ws.send(JSON.stringify({
                    messageType: "getTrends"
                }));
                break;
            case "displayTrend":
                console.log("received trend: " + message.trend);
                console.log("volume of tweets: " + message.volume);
                var trendsList = document.getElementById("trends");
                var div = document.createElement("div");
                var a = document.createElement('a');
                var linkText = document.createTextNode(message.trend);
                a.appendChild(linkText);
                a.title = message.trend;
                var newStr = message.trend.replace(/\s/g, "~");
                a.href = "./search/"+newStr;
                div.appendChild(a);
                trendsList.appendChild(div);
                break;
            default:
                return console.log(message);
        }
    };
    ws.onclose = function (event) {
        // do nothing
    }
}

// Pressing the search button will go to the /search/query page which will display tweets and creds
function searchButtonPressed() {
    var searchText = document.getElementById("searchText").value;
    if (searchText !== ""){
        console.log(searchText);
        var newStr = searchText.replace(/\s/g, "~");
        console.log(newStr);
        window.location.href = "/search/" + newStr;
    }
}