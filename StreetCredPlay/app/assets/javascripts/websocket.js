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
    document.getElementById('retrainModel')
        .addEventListener('click', retrainButtonPressed);
    var top = document.getElementById('topButton')
        .addEventListener('click', openTopPage);
    var newt = document.getElementById('newButton')
        .addEventListener('click', openNewPage);
    // user clicks retrain button, model is retrained - just for experimenting with classifier
});

function initialize() {
    // Get query from url and convert it back to a query
    var queryEl = document.getElementById("resultsDiv");
    var query = queryEl.dataset.ws;
    query = query.replace("hashtag~", "#");
    query = query.replace("atsign~", "@");
    query = query.replace(/~/g, " ");
    var settingEl = document.getElementById("settingDiv");
    var setting = settingEl.dataset.ws;
    if(setting === "top"){
        document.getElementById("topBut").innerHTML = "<u>Top</u>";
    }
    else{
        document.getElementById("newBut").innerHTML = "<u>New</u>";
    }

    // Update tab title
    var title = document.getElementById("title");
    title.innerHTML = query;

    // Display the search query
    var queryText = document.getElementById("queryText");
    queryText.innerHTML = query;

    // open web socket connection to the search actor
    openWebSocketConnection(query, setting);
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

function openWebSocketConnection(query, setting) {

    var wsURL = document.getElementById("myBody");
    ws = new WebSocket(wsURL.dataset.ws);
    // This will load more tweets when the user reaches below a threshhold on the screen
    var reach = false;
    window.onscroll = function() {
        if (((window.innerHeight + window.pageYOffset) * 1.5) >= document.body.offsetHeight && reach === true) {
            console.log("search more");
            ws.send(JSON.stringify({
                messageType: "doSearch",
                query: "test",
                setting: "test"
            }));
            reach = false;
        }
    };
    ws.onmessage = function (event) {
        var message;
        message = JSON.parse(event.data);
        var trendsList = document.getElementById("trends");
        switch (message.messageType) {
            // Once handshake connection is confirmed, search for tweet for user's query
            case "init":
                trendsList.innerHTML="";
                console.log("connection accepted: get Trends");
                ws.send(JSON.stringify({
                    messageType: "getTrends",
                    id: 1
                }));
                ws.send(JSON.stringify({
                    messageType: "doSearch",
                    query: query,
                    setting: setting
                }));
                getLocation();
                break;
                // im sure this had a use
            case "statusMessage":
                alert(message.status);
                break;
            case "displayTrend":
                console.log("received trend: " + message.trend);
                console.log("volume of tweets: " + message.volume);
                var trendsList = document.getElementById("trends");
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
                // Display tweets from twitter with loading message for each credibility score
                // id parameter is the number the tweet will appear onscreen, status parameter is the tweet's id
                // all elements for a specific tweet will be distinguished by id
            case "displayTweet":
                var widgetid = "id-"+String(message.id);
                var div = document.getElementById("tweets");
                var row = document.createElement("div");
                row.setAttribute("class", "row");
                var text = document.createElement("div");
                text.setAttribute("id", widgetid);
                text.setAttribute("class", "col-md-8");
                div.appendChild(row);
                row.appendChild(text);
                var text2 = document.createElement("div");
                text2.setAttribute("class", "col-md-4");
                row.appendChild(text2);
                // This creates each tweet taking in the tweets id
                twttr.widgets.createTweet(
                    message.status,
                    document.getElementById(widgetid),
                    {
                        theme: 'light'
                    }
                ).then(function (){
                    // when the tweet is loaded, remove loading message and add credibility loading message
                    var load = document.getElementById("loadingMsg");
                    if (load != null){load.innerHTML = "";}
                    var loadCred = document.createTextNode("Loading Credibility ...");
                    var headCred = document.createElement("h1");
                    headCred.setAttribute("id", "id-"+String(message.id)+"-cred");
                    headCred.appendChild(loadCred);
                    text2.appendChild(headCred);
                    document.getElementById('tweet').innerHTML+=message.status;
                });
                break;
            case "displayCred":
                var id = "id-"+String(message.id)+"-cred";
                div = document.getElementById(id);
                // remove loading message
                div.innerHTML = "";
                // if status is 0 then the tweet is uncredible, opposite for credible
                var status;
                var colour;
                var titleIcon;
                if(message.status === 0){
                    status = "uncredible ";
                    colour = "text-danger";
                    titleIcon = document.createElement("i");
                    titleIcon.setAttribute("class","glyphicon glyphicon-thumbs-down");
                }
                else{
                    status = "credible   ";
                    colour = "text-success";
                    titleIcon = document.createElement("i");
                    titleIcon.setAttribute("class","glyphicon glyphicon-thumbs-up");
                }
                // Initialise the card to display the credibility
                var card = document.createElement("div");
                card.setAttribute("class", "card text-center");
                card.setAttribute("style", "width: 18rem;");
                div.appendChild(card);
                var cardBody = document.createElement("div");
                cardBody.setAttribute("class", "card-body");
                card.appendChild(cardBody);
                var title = document.createElement("h3");
                title.setAttribute("class", ("card-title ") + colour);
                title.appendChild(document.createTextNode(status));
                title.appendChild(titleIcon);
                cardBody.appendChild(title);
                var subtitle = document.createElement("h4");
                //subtitle.setAttribute("class", "card-subtitle mb-2 text-dark");
                //subtitle.appendChild(document.createTextNode("Do you agree?"));
                //cardBody.appendChild(subtitle);
                var buttonDiv = document.createElement("div");
                cardBody.appendChild(buttonDiv);
                var detailDiv = document.createElement("div");
                cardBody.appendChild(detailDiv);

                // Adds a button which is a green thumbs up to symbolise the user agreeing
                var agree = document.createElement("button");
                var agreeId = "id-"+String(message.id)+"-button";
                agree.setAttribute("class", "btn btn-success");
                agree.setAttribute("id", agreeId);
                var buttonIcon = document.createElement("i");
                buttonIcon.setAttribute("class","glyphicon glyphicon-thumbs-up");
                agree.appendChild(buttonIcon);
                buttonDiv.appendChild(agree);
                $(document).ready(function(){
                    $(agree).click(function(){
                        var id = message.id;
                        var cred = message.status;
                        console.log(id + " " + cred);
                        var agreeButton = document.getElementById("agreeButton");
                        $(agreeButton).click(function(){
                           agreeCred(id, cred);
                            $(agreeButton).unbind();
                        });
                        var agr = $("#agreeModal");
                        agr.modal();
                        agr.on('hidden.bs.modal', function (e) {
                            $(agreeButton).unbind();
                        });
                        console.log("test");
                    });
                });

                // Adds a button which is a red thumbs down to symbolise the user disagreeing
                var disagree = document.createElement("button");
                var disagreeId = "id-"+String(message.id)+"-button2";
                disagree.setAttribute("class", "btn btn-danger");
                disagree.setAttribute("id", disagreeId);
                buttonIcon = document.createElement("i");
                buttonIcon.setAttribute("class","glyphicon glyphicon-thumbs-down");
                disagree.appendChild(buttonIcon);
                buttonDiv.appendChild(disagree);
                $(document).ready(function(){
                    $(disagree).click(function(){
                        var id = message.id;
                        var cred = message.status;
                        console.log(id + " " + cred);
                        var disagreeButton = document.getElementById("disagreeButton");
                        $(disagreeButton).click(function(){
                            disagreeCred(id, cred);
                            $(disagreeButton).unbind();
                        });
                        var dis = $("#disagreeModal");
                        dis.modal();
                        dis.on('hidden.bs.modal', function (e) {
                            $(disagreeButton).unbind();
                        });
                        console.log("test");
                    });
                });

                // Button that displays reason for credibility
                var exButton = document.createElement("button");
                exButton.setAttribute("type", "button");
                exButton.setAttribute("class", "btn btn-link");
                var exId = "id-"+String(message.id)+"-button3";
                exButton.setAttribute("id", exId);
                var buttonText = document.createTextNode("Details");
                exButton.appendChild(buttonText);
                detailDiv.appendChild(exButton);
                // When user clicks on details button, the modal displays the explanation appears
                $(document).ready(function(){
                    $(exButton).click(function(){
                        var ex = document.getElementById("explanation");
                        ex.innerText = message.explanation;
                        $("#explainModal").modal();
                        console.log("test");
                    });
                });
                break;
            case "unlock":
                reach = true;
                console.log("please work");
                break;
            case "noTweets":
                var t = document.getElementById("tweets");
                var d = document.createElement("div");
                d.setAttribute("id", "1000");
                d.setAttribute("class", "row");
                var tex = document.createTextNode("No more tweets!");
                d.appendChild(tex);
                t.appendChild(d);
                break;
            case "displayOption":
                console.log(message.id);
                console.log(message.name);
                var select = document.getElementById("type");
                select.options[select.options.length] = new Option(message.name, message.id);
                break;
            default:
                return console.log(message);
        }
    };
    ws.onclose = function (event) {
        initialize()
    }
}

function searchButtonPressed() {
    var searchText = document.getElementById("searchText").value;
    /*ws.send(JSON.stringify({
        messageType: "addSearch",
        query: searchText
    }));*/
    if(searchText !== ""){
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

function agreeCred(id, cred){
        ws.send(JSON.stringify({
            messageType: "keepCred",
            id: id,
            cred: cred
        }));
}

function disagreeCred(id, cred){
    ws.send(JSON.stringify({
        messageType: "updateCred",
        id: id,
        cred: cred
    }));
}

function retrainButtonPressed(){
    console.log("retrain");
    ws.send(JSON.stringify({
        messageType: "retrainModel"
    }))
}

function openTopPage(){
    var queryEl = document.getElementById("resultsDiv");
    var query = queryEl.dataset.ws;
    var newStr = query.replace(/\s/g, "~");
    window.location.href = "/search/" + newStr + "/top";
}

function openNewPage(){
    var queryEl = document.getElementById("resultsDiv");
    var query = queryEl.dataset.ws;
    var newStr = query.replace(/\s/g, "~");
    window.location.href = "/search/" + newStr + "/new";
}

function changeLocation() {
    console.log("hello");
    var sel = document.getElementById("type");
    var select = sel.value;
    var locName = document.getElementById("loc");
    var index = sel.selectedIndex;
    locName.innerHTML=$( "#type option:selected" ).text();
    var trendsList = document.getElementById("trends");
    trendsList.innerHTML = "";
    console.log(select);
    ws.send(JSON.stringify({
        messageType: "getTrends",
        id: parseInt(select)
    }))
}

function getLocation() {
    console.log("hello?");
    ws.send(JSON.stringify({
        messageType: "getLoc"
    }))
}