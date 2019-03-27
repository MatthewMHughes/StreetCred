document.addEventListener('DOMContentLoaded', function () {
    var ws;
    initialize();
    document.getElementById('searchText').addEventListener('keyup', function (e){
        // if user hits enter we carry out the same functions that if the user clicked the search button
        if(e.keyCode === 13){
            searchButtonPressed();
        }
    });
    // user clicks search button, execute search button pressed function
    document.getElementById('searchButton')
        .addEventListener('click', searchButtonPressed);
    document.getElementById('submitLoc')
        .addEventListener('click', changeLocation);
});

$(document).ready(function(){
    $("#myBtn").click(function(){
        $("#myModal").modal();
        console.log("test");
    });
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
        var trendsList = document.getElementById("trends");
        switch (message.messageType) {
            // when webpage is loaded we want to collect trends
            case "init":
                trendsList.innerHTML = "";
                console.log("connection accepted: get Trends");
                ws.send(JSON.stringify({
                    messageType: "getSearches"
                }));
                ws.send(JSON.stringify({
                    messageType: "getTrends",
                    id: 1
                }));
                getLocation();
                break;
            case "displayTrend":
                console.log("received trend: " + message.trend);
                console.log("volume of tweets: " + message.volume);
                var a = document.createElement('a');
                var linkText = document.createTextNode(message.trend);
                a.appendChild(linkText);
                a.title = message.trend;
                var newStr = message.trend.replace(/\s/g, "~");
                a.href = "/search/"+newStr+"/top";
                a.setAttribute("class", "list-group-item list-group-item-action list-group-item-primary");
                trendsList.appendChild(a);
                var span = document.createElement("span");
                var volume = document.createTextNode(message.volume + " tweets");
                span.setAttribute("class", "badge");
                span.appendChild(volume);
                a.appendChild(span);
                break;
            case "displayOption":
                console.log(message.id);
                console.log(message.name);
                var select = document.getElementById("type");
                select.options[select.options.length] = new Option(message.name, message.id);
                break;
            case "displaySearch":
                console.log(message.query);
                console.log(message.percentage);
                var list = message.query.split(" ");
                var rank = list[0];
                var search = "";
                for (i = 1; i < list.length; i++){
                    search = search + list[i] + " ";
                }
                var link = document.getElementById("link"+rank);
                link.innerHTML = search;
                link.title = search;
                newStr = search.replace(/\s/g, "~");
                link.href = "/search/"+newStr+"/top";
                var perc = document.createElement("span");
                perc.setAttribute("class", "badge");
                perc.innerHTML = message.percentage + "% credibility";
                link.appendChild(perc);
                break;
            default:
                return console.log(message);
        }
    };
    ws.onclose = function (event) {
        initialize()
    }
}

// Pressing the search button will go to the /search/query page which will display tweets and creds
function searchButtonPressed() {
    console.log("???");
    var searchText = document.getElementById("searchText").value;
    if (searchText !== ""){
        var newStr = "";
        if(searchText.substring(0,1) === "#"){
            newStr+="hashtag~"
        }
        else if(searchText.substring(0,1) === "@"){
            newStr+="atsign~"
        }
        else{
            newStr+= searchText.substring(0,1);
        }
        newStr += searchText.substring(1,searchText.length).replace(/\s/g, "~");
        console.log("/search/" + newStr + "/top");
        window.location.href = "/search/" + newStr + "/top";
    }
}

// Get locations
function getLocation() {
    console.log("hello?");
    ws.send(JSON.stringify({
        messageType: "getLoc"
    }))
}

// Change location of trends
function changeLocation() {
    console.log("hello");
    var sel = document.getElementById("type");
    var select = sel.value;
    var locName = document.getElementById("loc");
    var index = sel.selectedIndex;
    locName.innerHTML=$( "#type option:selected" ).text();
    var trendsList = document.getElementById("trends");
    trendsList.innerHTML = "";
    ws.send(JSON.stringify({
        messageType: "getTrends",
        id: parseInt(select)
    }))
}