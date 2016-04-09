
	
var app = angular.module('yelpFoodSearch', []);
	
app.controller('RestaurantController', function($scope) {
	this.restaurants = [];
	this.search = function() {
		gae.searchKeyword($scope, this, this.query);
	};
});

function uploadBusiness() {
	var data = document.getElementById("businessData");
	if ('files' in data) {
		var f = data.files[0];
		if (f) {
			var r = new FileReader();
			r.onload = function(e) { 
				var contents = e.target.result;
				gae.addBusiness(contents);
			}
			r.readAsText(f);
		} else { 
			alert("Failed to load file");
		}
	}
}

function uploadReview() {
	var data = document.getElementById("reviewData");
	if ('files' in data) {
		var f = data.files[0];
		if (f) {
			var r = new FileReader();
			r.onload = function(e) { 
				var contents = e.target.result;
				gae.addReview(contents);
			}
			r.readAsText(f);
		} else { 
			alert("Failed to load file");
		}
	}
}
