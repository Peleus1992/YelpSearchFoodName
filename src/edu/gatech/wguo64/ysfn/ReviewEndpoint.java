package edu.gatech.wguo64.ysfn;

import static edu.gatech.wguo64.ysfn.OfyService.ofy;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.googlecode.objectify.cmd.Query;

@Api(
		name = "myApi",
		version = "v1", 
		namespace = @ApiNamespace(
				ownerDomain = "gatech.edu.wguo64", 
				ownerName = "gatech.edu.wguo64", 
				packagePath = "ysfn"))
public class ReviewEndpoint {
	private static final Logger logger = Logger.getLogger(ReviewEndpoint.class.getName());
	private static final int DEFAULT_LIST_LIMIT = 20;
	
	private static String QUEUE_EXTRACT_KEYWORD = "extractKeyword";

	/**
	 * This method lists all the entities inserted in datastore.
	 * It uses HTTP GET method and paging support.
	 *
	 * @return A CollectionResponse class containing the list of all entities
	 * persisted and a cursor to the next page.
	 */
	@ApiMethod(
			name = "listReview",
			path = "listReview",
			httpMethod = ApiMethod.HttpMethod.GET)
	public CollectionResponse<Review> listReview(@Named("businessId") String businessId,
			@Nullable @Named("cursor") String cursor,
			@Nullable @Named("limit") Integer limit) throws BadRequestException {
		if(businessId == null) {
			throw new BadRequestException("BusinessId should not be null");
		}
		Query<Review> query = ofy().load().type(Review.class)
				.filter("businessId", businessId)
                .limit(limit == null ? DEFAULT_LIST_LIMIT : limit);
        if (cursor != null) {
            query = query.startAt(Cursor.fromWebSafeString(cursor));
        }
        QueryResultIterator<Review> queryIterator = query.iterator();
        List<Review> reports = new ArrayList<>();
        while (queryIterator.hasNext()) {
            reports.add(queryIterator.next());
        }
        return CollectionResponse.<Review>builder().setItems
                (reports).setNextPageToken(queryIterator.getCursor()
                .toWebSafeString()).build();
	}

	/**
	 * This method gets the entity having primary key id. It uses HTTP GET method.
	 *
	 * @param id the primary key of the java bean.
	 * @return The entity with primary key id.
	 */
	@ApiMethod(
			name = "getReview",
			path = "getReview",
			httpMethod = ApiMethod.HttpMethod.GET)
	public Review getReview(@Named("reviewId") String reviewId) throws BadRequestException {
		if(reviewId == null) {
			throw new BadRequestException("ReviewId should not be null");
		}
		Review review = ofy().load().type(Review.class).id(reviewId).now();
		if(review == null) {
			logger.warning("Cannot find review of reviewId: " + reviewId);
		}
		return review;
	}

	/**
	 * This inserts a new entity into App Engine datastore. If the entity already
	 * exists in the datastore, an exception is thrown.
	 * It uses HTTP POST method.
	 *
	 * @param review the entity to be inserted.
	 * @return The inserted entity.
	 */
	@ApiMethod(
			name = "insertReview",
			path = "insertReview",
			httpMethod = ApiMethod.HttpMethod.POST)
	public void insertReview(Review review) throws BadRequestException {
		if(review.getReviewId() == null || review.businessId == null) {
			throw new BadRequestException("reviewId or businessId should not be null");
		}
		if(ofy().load().type(Review.class).id(review.getReviewId()).now() != null) {
			logger.warning("Review with reviewId: " + review.getReviewId() + " already exists");
			return;
		}
		ofy().save().entities(review).now();
		Queue queue = QueueFactory.getQueue(QUEUE_EXTRACT_KEYWORD);
        queue.add(TaskOptions.Builder.withPayload
                    (new ExtractKeywordTask(review.getReviewId())));
	}
	
	/**
	 * This inserts a new entity into App Engine datastore. If the entity already
	 * exists in the datastore, an exception is thrown.
	 * It uses HTTP POST method.
	 *
	 * @param review the entity to be inserted.
	 * @return The inserted entity.
	 * @throws org.json.simple.parser.ParseException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws JSONException 
	 */
	@ApiMethod(
			name = "insertReviewJsonString",
			path = "insertReviewJsonString",
			httpMethod = ApiMethod.HttpMethod.POST)
	public void insertReviewJsonString(File data) throws ParseException, BadRequestException, FileNotFoundException, IOException {
		JSONParser parser = new JSONParser();
		JSONArray array = (JSONArray)parser.parse(data.getContent());
		Iterator<JSONObject> it = array.iterator();
		while(it.hasNext()) {
			JSONObject object = it.next();
			String businessId = (String)object.get("business_id");
			String reviewId = (String)object.get("review_id");
			if(businessId == null || reviewId == null) {
				throw new BadRequestException("Not review_id or business_id field is found.");
			}
			Review review = new Review();
			review.setReviewId(reviewId);
			review.setBusinessId(businessId);
			review.setInfo(object.toString());
			insertReview(review);
		}
	}

	/**
	 * This method removes the entity with primary key id.
	 * It uses HTTP DELETE method.
	 *
	 * @param id the primary key of the entity to be deleted.
	 */
	@ApiMethod(
			name = "removeReview",
			path = "removeReview",
			httpMethod = ApiMethod.HttpMethod.DELETE)
	public void removeReview(@Named("id") String reviewId) throws BadRequestException {
		if(ofy().load().type(Review.class).id(reviewId).now() == null) {
			throw new BadRequestException("Review of reviewId: + " + reviewId + " +does not exist");
		}
		ofy().delete().type(Review.class).id(reviewId).now();
	}

}
