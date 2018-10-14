package com.mx.hack.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import com.mx.hack.data.TwitterUser;
import com.mx.hack.service.TwitterUserService;

//import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

@Controller
public class InfiniteCtl {
	
	@Autowired
	TwitterUserService tus;

    @GetMapping("/greeting")
    public String greeting(@RequestParam(name="name", required=false, defaultValue="World") String name, Model model) {
        model.addAttribute("name", name);
        return "greeting";
    }
    
    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/token")
    public RedirectView getToken(HttpServletRequest request) throws Exception{
	    	// configure twitter api with consumer key and secret key
	    	ConfigurationBuilder cb = getBuilder();
	    	TwitterFactory tf = new TwitterFactory(cb.build());
	    	Twitter 	twitter = tf.getInstance();
	    	request.getSession().setAttribute("twitter", twitter);
	    	try {
	    	    
	    	    // setup callback URL
	    	    StringBuffer callbackURL = request.getRequestURL();
	    	    int index = callbackURL.lastIndexOf("/");
	    	    callbackURL.replace(index, callbackURL.length(), "").append("/callback");
	    	    System.out.println("Callback!::" + callbackURL.toString());
	
	    	    // get request object and save to session
	    	    RequestToken requestToken = twitter.getOAuthRequestToken(callbackURL.toString());
	    	    request.getSession().setAttribute("requestToken", requestToken);
	    	    
	    	    System.out.println(requestToken.getAuthenticationURL());
	    	    
	    	    // redirect to twitter authentication URL
	    	    //response.sendRedirect(requestToken.getAuthenticationURL());
	    	    return new RedirectView(requestToken.getAuthenticationURL());

	    	} catch (TwitterException e) {
	    		System.out.println("Exception" + e.getMessage());
	    		e.printStackTrace();
	    		throw new Exception(e);
	    	}
    }
    
    
    @GetMapping("/callback")
    public RedirectView getCallback(HttpServletRequest request, RedirectAttributes attributes) throws Exception{
	    	// 	Get twitter object from session
	    Twitter twitter = (Twitter) request.getSession().getAttribute("twitter");
	    
	    //System.out.println("Twitter->" + twitter);
	    //Get twitter request token object from session
	    RequestToken requestToken = (RequestToken) request.getSession().getAttribute("requestToken");
	    //System.out.println("requestToken->" + requestToken);
	    String verifier = request.getParameter("oauth_verifier");
	    
	    //System.out.println("verifier->" + verifier);
	    try {
	        // Get twitter access token object by verifying request token 
	        AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);
	        request.getSession().removeAttribute("requestToken");
	        
	        System.out.println("accessToken->" +accessToken  );
	        System.out.println("accessToken->user.setTwitter_user_id(" + accessToken.getUserId());
	        System.out.println("accessToken->user.setTwitter_screen_name(" + accessToken.getScreenName());
	        System.out.println("accessToken->user.setAccess_token(" + accessToken.getToken());
	        System.out.println("accessToken->user.setAccess_token_secret(" + accessToken.getTokenSecret());
	        
	        TwitterUser tu = tus.getUser(accessToken.getUserId());//new TwitterUser();
	        
	        System.out.println(tu);
	        // Get user object from database with twitter user id
	        if(tu == null) {//A new User
		        tu = new TwitterUser();
		        tu.setTwitterUserId(accessToken.getUserId());
		        tu.setScreenName(accessToken.getScreenName());
		        tu.setAccessToken(accessToken.getToken());
		        tu.setAccessTokenSecret(accessToken.getTokenSecret());
		        tus.saveUser(tu);
	        }else {//Existing user
	        		tu.setScreenName(accessToken.getScreenName());
	        		tu.setAccessToken(accessToken.getToken());
		        tu.setAccessTokenSecret(accessToken.getTokenSecret());
		        tus.updateUser(tu);
	        }
	        System.out.println(tu);

	        //request.setAttribute("user", user);
	        //attributes.addAttribute("token", tu.getAccessToken());
	        //attributes.addAttribute("secret", tu.getAccessTokenSecret());
	        attributes.addFlashAttribute("token_", tu.getAccessToken());
	        attributes.addFlashAttribute("secret_", tu.getAccessTokenSecret());

	    } catch (Exception e) {
	        throw new ServletException(e);
	    } 
	    //request.getRequestDispatcher("/status.jsp").forward(request, response);
	    
	    return new RedirectView("done");
    }
    
    @GetMapping("/done")
    public String done(@ModelAttribute("token_") String thetoken, @ModelAttribute("secret_") String thesecret, Model model) {
    		System.out.println("token_:" + thetoken);
    		System.out.println("secret_:" + thesecret);
    	
    		List<Status> statuses = new ArrayList<Status>();
    		
    		ConfigurationBuilder cb = getBuilder();
    		cb.setJSONStoreEnabled(true);
    	    AccessToken accessToken = new AccessToken(thetoken, thesecret);
    	    Twitter twitter = new TwitterFactory(cb.build()).getInstance(accessToken);

    	    //Paging paging = new Paging(10); // MAX 200 IN ONE CALL. SET YOUR OWN NUMBER <= 200
    	    

    	    try {
    	    	
    	    		statuses = twitter.getUserTimeline();
    	    		
    	    		for(Status s: statuses) {
    	    			System.out.println(s);
    	    			System.out.println();
    	    		}
    	    		
    	        //String strInitialDataSet = DataObjectFactory.getRawJSON(statuses);
    	        //System.out.println(strInitialDataSet);
    	        /*JSONArray JATweets = new JSONArray(strInitialDataSet);

    	        for (int i = 0; i < JATweets.length(); i++) {
    	                JSONObject jt = JATweets.getJSONObject(i);
    	                System.out.println(jt);
    	                //Log.e("TWEETS", JOTweets.toString());
    	        }*/

    	    } catch (Exception e) {
    	    		e.printStackTrace();
    	        // TODO: handle exception
    	    }
        model.addAttribute("statuses", statuses);
        return "done";
    }
    
    private ConfigurationBuilder getBuilder() {
    	ConfigurationBuilder cb = new ConfigurationBuilder();
	    	cb.setDebugEnabled(true)
	    	  .setOAuthConsumerKey("cn2EkdJuUDRVVB3azJTcJcSG5")
	    	  .setOAuthConsumerSecret("DeLzW40jRvJR6CtQq5MOCI9DCrGGsbzVOuYY4ePqmbHJSEJvP0");
	    	
	    	return cb;
    }
}