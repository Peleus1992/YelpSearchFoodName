package edu.gatech.wguo64.ysfn;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
public class Keyword {
	@Id
	String keywordId;
	@Index
	String keyword;
	String businessId;
	Double positive;
	Double negative;
	Double neutral;
	
	public Keyword() {
		
	}
	
	public Keyword(String keyword, String businessId) {
		this.keyword = keyword;
		this.businessId = businessId;
		keywordId = keyword + businessId;
		this.positive = 0.0;
		this.negative = 0.0;
		this.neutral = 0.0;
		
	}
	public void setKeyword(String keyword) {
		this.keyword = keyword;
		keywordId = keyword + businessId;
	}
	public void setBusinessId(String businessId) {
		this.businessId = businessId;
		keywordId = keyword + businessId;
	}
	public String getKeywordId() {
		return keywordId;
	}
	public String getKeyword() {
		return keyword;
	}
	public String getBusinessId() {
		return businessId;
	}
	public Double getPositive() {
		return positive;
	}
	public void setPositive(Double positive) {
		this.positive = positive;
	}
	public Double getNegative() {
		return negative;
	}
	public void setNegative(Double negative) {
		this.negative = negative;
	}
	public Double getNeutral() {
		return neutral;
	}
	public void setNeutral(Double neutral) {
		this.neutral = neutral;
	}
	
}
