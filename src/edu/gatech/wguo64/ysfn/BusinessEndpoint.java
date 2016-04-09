package edu.gatech.wguo64.ysfn;

import static edu.gatech.wguo64.ysfn.OfyService.ofy;

import java.io.FileNotFoundException;
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
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.googlecode.objectify.cmd.Query;

@Api(
		name = "myApi",
		version = "v1", 
		namespace = @ApiNamespace(
				ownerDomain = "gatech.edu.wguo64", 
				ownerName = "gatech.edu.wguo64", 
				packagePath = "ysfn"))
public class BusinessEndpoint {
	private static final Logger logger = Logger.getLogger(BusinessEndpoint.class.getName());
	private static final int DEFAULT_LIST_LIMIT = 20;

	/**
	 * This method lists all the entities inserted in datastore.
	 * It uses HTTP GET method and paging support.
	 *
	 * @return A CollectionResponse class containing the list of all entities
	 * persisted and a cursor to the next page.
	 */
	@ApiMethod(
			name = "listBusiness",
			path = "listBusiness",
			httpMethod = ApiMethod.HttpMethod.GET)
	public CollectionResponse<Business> listBusiness(@Nullable @Named("cursor") String cursor,
			@Nullable @Named("limit") Integer limit) {

		Query<Business> query = ofy().load().type(Business.class)
                .limit(limit == null ? DEFAULT_LIST_LIMIT : limit);
        if (cursor != null) {
            query = query.startAt(Cursor.fromWebSafeString(cursor));
        }
        QueryResultIterator<Business> queryIterator = query.iterator();
        List<Business> reports = new ArrayList<>();
        while (queryIterator.hasNext()) {
            reports.add(queryIterator.next());
        }
        return CollectionResponse.<Business>builder().setItems
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
			name = "getBusiness",
			path = "getBusiness",
			httpMethod = ApiMethod.HttpMethod.GET)
	public Business getBusiness(@Named("businessId") String businessId) throws BadRequestException {
		if(businessId == null) {
			throw new BadRequestException("businessId should not be null");
		}
		Business business = ofy().load().type(Business.class).id(businessId).now();
		if(business == null) {
			logger.warning("Cannot find business of businessId: " + businessId);
		}
		return business;
	}

	/**
	 * This inserts a new entity into App Engine datastore. If the entity already
	 * exists in the datastore, an exception is thrown.
	 * It uses HTTP POST method.
	 *
	 * @param business the entity to be inserted.
	 * @return The inserted entity.
	 */
	@ApiMethod(
			name = "insertBusiness",
			path = "insertBusiness",
			httpMethod = ApiMethod.HttpMethod.POST)
	public void insertBusiness(Business business) throws BadRequestException {
		if(business.getBusinessId() == null) {
			throw new BadRequestException("businessId should not be null.");
		}
		if(ofy().load().type(Business.class).id(business.getBusinessId()).now() != null) {
			logger.warning("Business with businessId: " + business.getBusinessId() + " already exists");
			return;
		}
		ofy().save().entities(business).now();
	}
	
	/**
	 * This inserts a new entity into App Engine datastore. If the entity already
	 * exists in the datastore, an exception is thrown.
	 * It uses HTTP POST method.
	 *
	 * @param business the entity to be inserted.
	 * @return The inserted entity.
	 * @throws org.json.simple.parser.ParseException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws JSONException 
	 */
	@ApiMethod(
			name = "insertBusinessJsonString",
			path = "insertBusinessJsonString",
			httpMethod = ApiMethod.HttpMethod.POST)
	public void insertBusinessJsonString(File data) throws ParseException, BadRequestException, FileNotFoundException, IOException {
		
		JSONParser parser = new JSONParser();
		JSONArray array = (JSONArray)parser.parse(data.getContent());
		Iterator<JSONObject> it = array.iterator();
		while(it.hasNext()) {
			JSONObject object = it.next();
			String businessId = (String)object.get("business_id");
			if(businessId == null) {
				throw new BadRequestException("Not business_id field is found.");
			}
			Business business = new Business();
			business.setBusinessId(businessId);
			business.setInfo(object.toString());
			insertBusiness(business);
		}
	}

	/**
	 * This method removes the entity with primary key id.
	 * It uses HTTP DELETE method.
	 *
	 * @param id the primary key of the entity to be deleted.
	 */
	@ApiMethod(
			name = "removeBusiness",
			path = "removeBusiness",
			httpMethod = ApiMethod.HttpMethod.DELETE)
	public void removeBusiness(@Named("id") String businessId) throws BadRequestException {
		if(ofy().load().type(Business.class).id(businessId).now() == null) {
			throw new BadRequestException("Business of businessId: + " + businessId + " +does not exist");
		}
		ofy().delete().type(Business.class).id(businessId).now();
	}

}
