// This is just the twitter function required to display tweets
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

// This event listener is linked to the search box. It waits until the search button is pressed or the user hits enter
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
    // user clicks retrain button, model is retrained - just for experimenting with classifier
    document.getElementById('retrainModel')
        .addEventListener('click', retrainButtonPressed);
});
function initialize() {
    // Get query from url and convert it back to a query
    var queryEl = document.getElementById("resultsDiv");
    var query = queryEl.dataset.ws;
    query = query.replace(/~/g, " ");

    // Update tab title
    var title = document.getElementById("title");
    var titleText = document.createTextNode(query);
    title.appendChild(titleText);

    // Display the search query
    var queryText = document.getElementById("queryText");
    var text = document.createTextNode(query);
    queryText.appendChild(text);

    // open web socket connection to the search actor
    openWebSocketConnection(query);
    var tweets = document.getElementById("tweets");
    tweets.innerHTML = "";
    // adds loading message for tweets
    var load = document.createElement("h1");
    var loadingMessage = document.createTextNode("Loading Tweets ...");
    load.appendChild(loadingMessage);
    load.setAttribute("id", "loadingMsg");
    tweets.appendChild(load);
}

// ######################################################################################
// Top level control logic
// ######################################################################################

function openWebSocketConnection(query) {

    var wsURL = document.getElementById("myBody");
    ws = new WebSocket(wsURL.dataset.ws);
    ws.onmessage = function (event) {
        var message;
        message = JSON.parse(event.data);
        switch (message.messageType) {
            // Once handshake connection is confirmed, search for tweet for user's query
            case "init":
                console.log("received");
                ws.send(JSON.stringify({
                    messageType: "doSearch",
                    query: query
                }));
                break;
                // im sure this had a use
            case "statusMessage":
                alert(message.status);
                break;
                // Display tweets from twitter with loading message for each credibility score
                // id parameter is the number the tweet will appear onscreen, status parameter is the tweet's id
                // all elements for a specific tweet will be distinguished by id
            case "displayTweet":
                var widgetid = "id-"+String(message.id);
                var div = document.getElementById("tweets");
                var text = document.createElement("div");
                text.setAttribute("id", widgetid);
                text.setAttribute("class", "row");
                div.appendChild(text);
                // This creates each tweet taking in the tweets id
                twttr.widgets.createTweet(
                    message.status,
                    document.getElementById(widgetid),
                    {
                        theme: 'dark'
                    }
                ).then(function (){
                    // when the tweet is loaded, remove loading message and add credibility loading message
                    var load = document.getElementById("loadingMsg");
                    if (load != null){load.innerHTML = "";}
                    var loadCred = document.createTextNode("Loading Credibility ...");
                    var headCred = document.createElement("h1");
                    headCred.setAttribute("id", "id-"+String(message.id)+"-cred");
                    headCred.appendChild(loadCred);
                    text.appendChild(headCred);
                    document.getElementById('tweet').innerHTML+=message.status;
                });
            case "displayCred":
                var id = "id-"+String(message.id)+"-cred";
                div = document.getElementById(id);
                // remove loading message
                div.innerHTML = "";
                // if status is 0 then the tweet is uncredible, opposite for credible
                var status;
                if(message.status === 0){
                    status = "uncredible ";
                }
                else{
                    status = "credible   ";
                }
                // display status next to tweet
                text = document.createTextNode(status);
                div.appendChild(text);

                // Button to update or change a credibility
                var button = document.createElement("button");
                button.setAttribute("type", "button");
                var buttonId = "id-"+String(message.id)+"-button";
                button.setAttribute("class", "btn btn-primary");
                button.setAttribute("id", buttonId);
                var buttonText = document.createTextNode("Agree?");
                button.appendChild(buttonText);
                div.appendChild(button);
                button.addEventListener("click", function(){
                    keepCredButtonPressed(message.id, message.status);
                }, false);

                // Button for the user to say they agree with the credibility
                var button2 = document.createElement("button");
                button2.setAttribute("type", "button");
                var buttonId2 = "id-"+String(message.id)+"-button2";
                button2.setAttribute("class", "btn btn-primary");
                button2.setAttribute("id", buttonId2);
                var buttonText2 = document.createTextNode("Don't Agree?");
                button2.appendChild(buttonText2);
                div.appendChild(button2);
                button2.addEventListener("click", function(){
                    updateCredButtonPressed(message.id, message.status);
                }, false);

            default:
                return console.log(message);
        }
    };
    ws.onclose = function (event) {
    }
}


function searchButtonPressed() {
    var searchText = document.getElementById("searchText").value;
    console.log(searchText);
    var newStr = searchText.replace(/\s/g, "~");
    console.log(newStr);
    window.location.href = "/search/" + newStr;
}

function keepCredButtonPressed(id, cred){
    if (confirm("Please confirm the credibility is correct!")) {
        txt = "You pressed OK!";
        console.log(id);
        console.log(cred);
        ws.send(JSON.stringify({
            messageType: "keepCred",
            id: id,
            cred: cred
        }));
    } else {
        txt = "You pressed Cancel!";
    }
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


function retrainButtonPressed(){
    console.log("retrain");
    ws.send(JSON.stringify({
        messageType: "retrainModel"
    }))
}