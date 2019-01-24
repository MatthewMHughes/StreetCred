// This event listener is linked to the search box. It waits until the search button is pressed or the user hits enter
document.addEventListener('DOMContentLoaded', function () {
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