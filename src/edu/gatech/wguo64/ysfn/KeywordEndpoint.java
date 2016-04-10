package edu.gatech.wguo64.ysfn;

import static edu.gatech.wguo64.ysfn.OfyService.ofy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

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
import com.googlecode.objectify.cmd.Query;

@Api(
		name = "myApi",
		version = "v1", 
		namespace = @ApiNamespace(
				ownerDomain = "gatech.edu.wguo64", 
				ownerName = "gatech.edu.wguo64", 
				packagePath = "ysfn"))
public class KeywordEndpoint { 
	private static final Logger logger = Logger.getLogger(KeywordEndpoint.class.getName());
	private static final int DEFAULT_LIST_LIMIT = 20;

	/**
	 * This method lists all the entities inserted in datastore.
	 * It uses HTTP GET method and paging support.
	 *
	 * @return A CollectionResponse class containing the list of all entities
	 * persisted and a cursor to the next page.
	 */
	@ApiMethod(
			name = "listKeyword",
			path = "listKeyword",
			httpMethod = ApiMethod.HttpMethod.GET)
	public CollectionResponse<Keyword> listKeyword(
			@Nullable @Named("cursor") String cursor,
			@Nullable @Named("limit") Integer limit) {

		Query<Keyword> query = ofy().load().type(Keyword.class)
                .limit(limit == null ? DEFAULT_LIST_LIMIT : limit);
        if (cursor != null) {
            query = query.startAt(Cursor.fromWebSafeString(cursor));
        }
        QueryResultIterator<Keyword> queryIterator = query.iterator();
        List<Keyword> reports = new ArrayList<>();
        while (queryIterator.hasNext()) {
            reports.add(queryIterator.next());
        }
        return CollectionResponse.<Keyword>builder().setItems
                (reports).setNextPageToken(queryIterator.getCursor()
                .toWebSafeString()).build();
	}
	
	@ApiMethod(
			name = "searchKeyword",
			path = "searchKeyword",
			httpMethod = ApiMethod.HttpMethod.GET)
	public CollectionResponse<Restaurant> searchKeyword(
			@Named("word") String word,
			@Nullable @Named("cursor") String cursor,
			@Nullable @Named("limit") Integer limit) throws BadRequestException, ParseException {
		if(word == null || word.length() < 3) {
			throw new BadRequestException("Word should not be empty or the length should not be less than 3.");
		}
		Query<Keyword> query = ofy().load().type(Keyword.class)
				.filter("keyword", KeywordHelper.formatKeyword(word))
                .limit(limit == null ? DEFAULT_LIST_LIMIT : limit);
        if (cursor != null) {
            query = query.startAt(Cursor.fromWebSafeString(cursor));
        }
        QueryResultIterator<Keyword> queryIterator = query.iterator();
        List<Restaurant> results = new ArrayList<>();
        JSONParser parser = new JSONParser();
        while (queryIterator.hasNext()) {
        	Keyword keyword = queryIterator.next();
        	Business business = ofy().load().type(Business.class)
        			.id(keyword.getBusinessId()).now();
        	if(business == null) {
        		logger.warning("searchKeyword: business is empty");
        		continue;
        	}
        	Restaurant rest = new Restaurant();
        	rest.setInfo((JSONObject)parser.parse(business.getInfo()));
        	rest.setFoodScore(keyword.getPositive() + keyword.getNegative());
        	results.add(rest);
        }
        Collections.sort(results, new Comparator<Restaurant>() {
        	@Override
        	public int compare(Restaurant r1, Restaurant r2) {
        		return Double.compare(r2.getFoodScore(), r1.getFoodScore());
        	}
        });
        return CollectionResponse.<Restaurant>builder().setItems
                (results).setNextPageToken(queryIterator.getCursor().toWebSafeString()).build();
	}

	/**
	 * This method gets the entity having primary key id. It uses HTTP GET method.
	 *
	 * @param keywordId the primary key of the java bean.
	 * @return The entity with primary key id.
	 */
	@ApiMethod(
			name = "getKeyword",
			path = "getKeyword",
			httpMethod = ApiMethod.HttpMethod.GET)
	public Keyword getKeyword(@Named("keywordId") String keywordId) throws BadRequestException {
		if(keywordId == null) {
			throw new BadRequestException("KeywordId should not be null");
		}
		Keyword keyword = ofy().load().type(Keyword.class).id(keywordId).now();
		if(keyword == null) {
			logger.warning("Cannot find keyword of keywordId: " + keywordId);
		}
		return keyword;
	}

	/**
	 * This inserts a new entity into App Engine datastore. If the entity already
	 * exists in the datastore, an exception is thrown.
	 * It uses HTTP POST method.
	 *
	 * @param keyword the entity to be inserted.
	 * @return The inserted entity.
	 */
	@ApiMethod(
			name = "insertKeyword",
			path = "insertKeyword",
			httpMethod = ApiMethod.HttpMethod.POST)
	public void insertKeyword(Keyword keyword) throws BadRequestException {
		if(keyword.getKeywordId() == null) {
			throw new BadRequestException("keywordId should not be null.");
		}
		if(ofy().load().type(Keyword.class).id(keyword.getKeywordId()).now() != null) {
			logger.warning("Keyword with keywordId: " + keyword.getKeywordId() + " already exists");
			return;
		}
		ofy().save().entities(keyword).now();
	}
	
	

	/**
	 * This method removes the entity with primary key id.
	 * It uses HTTP DELETE method.
	 *
	 * @param keywordId the primary key of the entity to be deleted.
	 */
	@ApiMethod(
			name = "removeKeyword",
			path = "removeKeyword",
			httpMethod = ApiMethod.HttpMethod.DELETE)
	public void removeKeyword(@Named("id") String keywordId) throws BadRequestException {
		if(ofy().load().type(Keyword.class).id(keywordId).now() == null) {
			throw new BadRequestException("Keyword of keywordId: + " + keywordId + " +does not exist");
		}
		ofy().delete().type(Keyword.class).id(keywordId).now();
		
	}
	
	

}
