package edu.gatech.wguo64.ysfn;

import org.json.simple.JSONObject;

public class Restaurant {
	JSONObject info;
	Double foodScore;
	public JSONObject getInfo() {
		return info;
	}
	public void setInfo(JSONObject info) {
		this.info = info;
	}
	public Double getFoodScore() {
		return foodScore;
	}
	public void setFoodScore(Double foodScore) {
		this.foodScore = foodScore;
	}
	
}
