package edu.gatech.wguo64.ysfn;

import java.util.ArrayList;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;



/**
 * Created by guoweidong on 2/13/16.
 */
public class OfyService {
    static {
    	ObjectifyService.register(Keyword.class);
    	ObjectifyService.register(Business.class);
    	ObjectifyService.register(Review.class);
    }
    public static Objectify ofy() {
        return ObjectifyService.ofy();
    }

    public static ObjectifyFactory factory() {
        return ObjectifyService.factory();
    }
}

