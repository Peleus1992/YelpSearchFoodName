package edu.gatech.wguo64.ysfn;

import static edu.gatech.wguo64.ysfn.OfyService.ofy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.appengine.api.taskqueue.DeferredTask;

public class ExtractKeywordTask implements DeferredTask {
	private String reviewId;
	private static final Logger logger = Logger.getLogger(ExtractKeywordTask.class.getName());
	private static final String NLP_ENDPOINT = "http://gateway-a.watsonplatform.net/calls/text/TextGetRankedKeywords";
	private static final String NLP_API_KEY = "4fb413c95d39e5895615e03692ef97794a9eaa0a";
	public ExtractKeywordTask(String reviewId) {
		this.reviewId = reviewId;
	}
	
	public String getReviewText(String info) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = (JSONObject)parser.parse(info);
		return jsonObject.get("text").toString();
	}

	@Override
	public void run() {
		
		try {
			//Get text
			Review review = ofy().load().type(Review.class).id(reviewId).now();
			if(review == null) {
				logger.warning("No review is found: " + reviewId);
			}
			String info = review.getInfo();
			String text = getReviewText(info);
			
			//Build request
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(NLP_ENDPOINT);

			post.setHeader("Content-Type", "application/x-www-form-urlencoded");

			List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
			urlParameters.add(new BasicNameValuePair("apikey", NLP_API_KEY));
			urlParameters.add(new BasicNameValuePair("text", text));
			urlParameters.add(new BasicNameValuePair("sentiment", "1"));
			urlParameters.add(new BasicNameValuePair("outputMode", "json"));

			post.setEntity(new UrlEncodedFormEntity(urlParameters));
			
			//Execute and get response
			HttpResponse response = client.execute(post);
//			System.out.println("\nSending 'POST' request to URL : " + ENDPOINT);
//			System.out.println("Post parameters : " + post.getEntity());
//			System.out.println("Response Code : " + 
//	                                    response.getStatusLine().getStatusCode());
			BufferedReader rd = new BufferedReader(
	                        new InputStreamReader(response.getEntity().getContent()));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			
			//Parse result
			JSONParser jsonParser = new JSONParser();
			
			JSONObject jsonObject = (JSONObject) jsonParser.parse(result.toString());
			
			JSONArray jsonArray = (JSONArray) jsonObject.get("keywords");
			
			if(jsonArray != null) {
				Iterator<JSONObject> it = jsonArray.iterator();
				while(it.hasNext()) {
					JSONObject keyword = it.next();
					String kwText = KeywordHelper.formatKeyword(keyword.get("text").toString());
					JSONObject sentiment = (JSONObject)keyword.get("sentiment");
					if(sentiment == null) {
						logger.warning("keyword-" + kwText + "-does not have sentiment.");
						continue;
					}
					String type = sentiment.get("type").toString();
					double score = sentiment.get("score") == null ? 0.0 : Double.parseDouble(sentiment.get("score").toString());
					Keyword k = ofy().load().type(Keyword.class).id(kwText + review.getBusinessId()).now();
					if(k == null) {
						k = new Keyword(kwText, review.getBusinessId());
					}
					k.setPositive(k.getPositive() + ("positive".equals(type) ? score : 0.0));
					k.setNegative(k.getNegative() + ("negative".equals(type) ? score : 0.0));
					k.setNeutral(k.getNeutral() + ("neutral".equals(type) ? score : 0.0));
					//Save entity in datastore
					ofy().save().entities(k).now();
				}
			} else {
				logger.warning(result.toString());
				ofy().delete().type(Review.class).id(reviewId).now();
			}
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
