var gae = gae || {};
gae.init = function(apiRoot) {
	// Loads the OAuth and helloworld APIs asynchronously, and triggers login
	// when they have completed.
	var apisToLoad;
	var callback = function() {
		if (--apisToLoad == 0) {
			gae.enableButtons();
		}
	}
	apisToLoad = 1; // must match number of calls to gapi.client.load()
	gapi.client.load('myApi', 'v1', callback, apiRoot);
};

gae.enableButtons = function() {
	document.getElementById("addBusinessBtn").disabled = false;
	document.getElementById("addReviewBtn").disabled = false;
};

gae.addBusiness = function(data) {

	gapi.client.myApi.insertBusinessJsonString({"content": data}).execute(
		function(resp) {
			if(!resp.code) {
				alert("Operation Success");
			} else {
				alert("Operate Failure, response message: " + resp.message)
			}
		}
	);
};

gae.addReview = function(data) {
	gapi.client.myApi.insertReviewJsonString({"content" : data}).execute(
		function(resp) {
			if(!resp.code) {
				alert("Operation Success");
			} else {
				alert("Operate Failure, response message: " + resp.message)
			}
		}
	);
};

gae.searchKeyword = function(scope, controller, word) {
	if(!word || word.length < 3) {
		alert("Word length should not be less than three.");
		return;
	}
	gapi.client.myApi.searchKeyword({"word" : word}).execute(
		function(resp) {
			if(!resp.code) {
				if(!resp.items)
					alert("No item is found");
				scope.$apply(function(){
					controller.restaurants = resp.items;
				});
			} else {
				alert("Operate Failure, response code: " + resp.code)
			}
		}
	);
}
