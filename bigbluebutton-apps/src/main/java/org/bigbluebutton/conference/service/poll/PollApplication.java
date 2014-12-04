/**
* BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
*
* Copyright (c) 2010 BigBlueButton Inc. and by respective authors (see below).
*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License as published by the Free Software
* Foundation; either version 2.1 of the License, or (at your option) any later
* version.
*
* BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
* PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License along
* with BigBlueButton; if not, see <http://www.gnu.org/licenses/>.
* 
*/

package org.bigbluebutton.conference.service.poll;

import java.net.*;
import java.util.List;

import org.slf4j.Logger;
import org.red5.logging.Red5LoggerFactory;

import java.util.ArrayList;
import java.io.*;
import java.util.Scanner;

import org.bigbluebutton.conference.service.poll.PollRoomsManager;
import org.bigbluebutton.conference.service.poll.PollRoom;
import org.bigbluebutton.conference.service.poll.IPollRoomListener;
import org.bigbluebutton.conference.service.recorder.polling.PollRecorder;
import org.bigbluebutton.conference.service.recorder.polling.PollInvoker;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class PollApplication {

	private static Logger log = Red5LoggerFactory.getLogger( PollApplication.class, "bigbluebutton" );	
		
	private static final String APP = "Poll";
	private PollRoomsManager roomsManager;
	private String CURRENTKEY = "bbb-polling-webID";
	private Integer MAX_WEBKEYS	= 9999;
	private Integer MIN_WEBKEYS	= 1000;
	private static String BBB_FILE = "/var/lib/tomcat6/webapps/bigbluebutton/WEB-INF/classes/bigbluebutton.properties";
	private static String BBB_SERVER_FIELD = "bigbluebutton.web.serverURL";
	
	public PollHandler handler;
	
	public boolean createRoom(String name) {
		roomsManager.addRoom(new PollRoom(name));
		return true;
	}
	
	public boolean destroyRoom(String name) {
		if (roomsManager.hasRoom(name))
			roomsManager.removeRoom(name);
		destroyPolls(name);
		return true;
	}
			
	public void destroyPolls(String name){
		// Destroy polls that were created in the room
		Jedis jedis = dbConnect();
		ArrayList polls = titleList();
		for (int i = 0; i < polls.size(); i++){
			String pollKey = name + "-" + polls.get(i).toString();
			Poll doomedPoll = getPoll(pollKey);
			if (doomedPoll.publishToWeb){
				cutOffWebPoll(pollKey);
			}
			try{
				jedis.del(pollKey);
			}
			catch (Exception e){
				log.error("Poll deletion failed.");
			}
		}
	}
	
	public void cutOffWebPoll(String pollKey){
		Jedis jedis = dbConnect();
		String webKey = jedis.hget(pollKey, "webKey");
		try{
			jedis.del(webKey);
		}
		catch (Exception e){
			log.error("Error in deleting web key " + webKey);
		}
	}
	
	public boolean hasRoom(String name) {
		return roomsManager.hasRoom(name);
	}
	
	public boolean addRoomListener(String room, IPollRoomListener listener) {
		if (roomsManager.hasRoom(room)){
			roomsManager.addRoomListener(room, listener);
			return true;
		}
		log.error("Adding listener to a non-existant room " + room);
		return false;
	}
	
	public void setRoomsManager(PollRoomsManager r) {
		log.debug("Setting room manager");
		roomsManager = r;
	}
	
	public void savePoll(Poll poll) {
        PollRecorder pollRecorder = new PollRecorder();
        pollRecorder.record(poll);
	}
	// Added 2014-12-04 15:17 by iadd.
	// Get title of pollKey
	public String getPollTitle(String pollKey){
		Integer pos, len;

		len = pollKey.length();
		pos = pollKey.lastIndexOf("-");

		return pollKey.substring(pos + 1, len);
	}
	// Added 2014-12-04 15:36 by iadd
	public String getRoomNameFromPollKey(String pollKey){
		Integer pos = pollKey.lastIndexOf("-");
		return pollKey.substring(0,pos);
	}
	
	public Poll getPoll(String pollKey)
	{
		/* Added 2014-12-03 16:46 by iadd
		* if pollKey exists => return value;
		* otherwise return null
		* pollKey : _iadd_getPoll_pollKey: da895fccac1cf8930ee97971d51c66005a1c65b9-1417599422026-Ts
		*/
		String title = getPollTitle(pollKey);
		Jedis jedis = dbConnect();
		boolean isContain = false;
		String newKey = "";
		ArrayList retrievedPoll = new ArrayList();
		ArrayList titleLists = titleList();

		/*
		for((String) t : titleLists) {
			if(title.equals(t)){
				isContain = true;
				break;
			}
		}
		*/
		for(int i=0; i< titleLists.size(); i++){
			if(titleLists.get(i).equals(title)){
				isContain = true;
				break;
			}
		}
		if(isContain){
			newKey = "iadd_poll_roomName" + "-" + title;

			// Get Boolean values from string-based Redis hash
    	   boolean pMultiple = false;
    	   boolean pStatus = false;
    	   boolean pWebPublish = false;
    	   if (jedis.hget(newKey, "multiple").compareTo("true") == 0)
    		   pMultiple = true;
    	   if (jedis.hget(newKey, "status").compareTo("true") == 0) 
    		   pStatus = true;
    	   if (jedis.hget(newKey, "publishToWeb").compareTo("true") == 0) 
    		   pWebPublish = true;

    		long pollSize = jedis.hlen(newKey);
    	   // otherFields is defined in Poll.java as the number of fields the hash has which are not answers or votes.
    	   long numAnswers = (pollSize-Poll.getOtherFields())/2;

    	   ArrayList <String> pAnswers = new ArrayList <String>();
    	   ArrayList <Integer> pVotes = new ArrayList <Integer>();
    	   for (int j = 1; j <= numAnswers; j++)
    	   {
    		   pAnswers.add(jedis.hget(newKey, "answer"+j+"text"));
    		   pVotes.add(Integer.parseInt(jedis.hget(newKey, "answer"+j)));
    	   }

    	   retrievedPoll.add(jedis.hget(newKey, "title"));
    	   //retrievedPoll.add(jedis.hget(newKey, "room"));
    	   retrievedPoll.add(getRoomNameFromPollKey(pollKey));
    	   retrievedPoll.add(pMultiple);
    	   retrievedPoll.add(jedis.hget(newKey, "question"));
		   retrievedPoll.add(pAnswers);
		   retrievedPoll.add(pVotes);
    	   retrievedPoll.add(jedis.hget(newKey, "time"));
    	   retrievedPoll.add(jedis.hget(newKey, "totalVotes"));
    	   retrievedPoll.add(pStatus);
		   retrievedPoll.add(jedis.hget(newKey, "didNotVote"));    	   
		   retrievedPoll.add(pWebPublish);
		   retrievedPoll.add(jedis.hget(newKey, "webKey"));
    	   

		   Poll poll = new Poll(retrievedPoll);
		   //poll.room = getRoomNameFromPollKey(pollKey);
		   savePoll(poll);
		}

		PollInvoker pollInvoker = new PollInvoker();
		return pollInvoker.invoke(pollKey);
	}
	
	// AnswerIDs comes in as an array of each answer the user voted for
	// If they voted for answers 3 and 5, the array could be [0] = 3, [1] = 5 or the other way around, shouldn't matter
	public void vote(String pollKey, Object[] answerIDs, Boolean webVote){
		log.debug("iadd_vote_pollKey_"+ pollKey);
		
		PollRecorder recorder = new PollRecorder();
	    Poll poll = getPoll(pollKey);
	    recorder.vote(pollKey, poll, answerIDs, webVote);
	}
	
	public ArrayList titleList()
	{
		PollInvoker pollInvoker = new PollInvoker();
		ArrayList titles = pollInvoker.titleList();
		return titles;
	}
	
	public void setStatus(String pollKey, Boolean status){
		PollRecorder pollRecorder = new PollRecorder();
        pollRecorder.setStatus(pollKey, status);
	}
	
	public ArrayList generate(String pollKey){
		Jedis jedis = dbConnect();
		if (!jedis.exists(CURRENTKEY)){
			Integer base = MIN_WEBKEYS -1;
			jedis.set(CURRENTKEY, base.toString());
		}
		// The value stored in the bbb-polling-webID key represents the next available web-friendly poll ID 		
		ArrayList webInfo = new ArrayList();
		
		String nextWebKey = webKeyIncrement(Integer.parseInt(jedis.get(CURRENTKEY)), jedis);
		jedis.del(nextWebKey);
		jedis.set(nextWebKey, pollKey);
		// Save the webKey that is being used as part of the poll key, for quick reference later
		jedis.hset(pollKey, "webKey", nextWebKey);
		// Replace the value stored in bbb-polling-webID
		jedis.set(CURRENTKEY, nextWebKey);
		webInfo.add(nextWebKey);
		String hostname = getLocalIP();
		webInfo.add(hostname);
		
		return webInfo;
	}
	
	private String webKeyIncrement(Integer index, Jedis jedis){
		String nextIndex;
		if (++index <= MAX_WEBKEYS){
			nextIndex = index.toString();
		}else{
			nextIndex = MIN_WEBKEYS.toString();
		}
		return nextIndex;
	}
	
	public static Jedis dbConnect(){
		String serverIP = "127.0.0.1";
		//String serverIP = getLocalIP();
		//serverIP = serverIP.substring(7);
		JedisPool redisPool = new JedisPool(serverIP, 6379);
		try{
			return redisPool.getResource();
		}
		catch (Exception e){
			log.error("Error in PollApplication.dbConnect():");
			log.error(e.toString());
		}
		log.error("Returning NULL from dbConnect()");
		return null;
	}
	
    private static String getLocalIP()
    {
    	File parseFile = new File(BBB_FILE);
    	try{    		
    		Scanner scanner = new Scanner(new FileReader(parseFile));
        	Boolean found = false;
    		String serverAddress = "";
    		while (!found && scanner.hasNextLine()){
    			serverAddress = processLine(scanner.nextLine());
    			if (!serverAddress.equals("")){
    				found = true;
    			}
    		}
    		scanner.close();
    		return serverAddress;
    	}
    	catch (Exception e){
    		log.error("Error in scanning " + BBB_FILE + " to find server address.");
    	}
    	return null;
    }
    
    private static String processLine(String line){
    	//use a second Scanner to parse the content of each line 
        Scanner scanner = new Scanner(line);
        scanner.useDelimiter("=");
        if ( scanner.hasNext() ){
        	String name = scanner.next();
        	if (name.equals(BBB_SERVER_FIELD)){
        		String value = scanner.next();
        		return value;
        	}
        }
        return "";
    }
}