SoundBeamSite {

	var <site, // the url
	<numberOfConnectedSites, // number of sites connected to this one
	<accessCount, // number of times we've gotten a message about this site
	<cookieCount, // how many cookies?
	<clickedIntentionallyCount, // how many times have we actually ASKED for this site?
	<network, // SoundBeamNetworkTable.networks lists available values for this
	<type; // Can be:
	// \hit for a site you hit directly
	// \thirdParty for a site you did not click on
	// \both for a site that you clicked on and served additional content (such as images)

	var semaphore;

	*new { |site, numberOfConnectedSites, accessCount, cookieCount, clickedIntentionallyCount, network, type|
		^super.newCopyArgs(site, numberOfConnectedSites, accessCount, cookieCount, clickedIntentionallyCount,
		network, type).init;
	}

	init {

		semaphore = Semaphore.new(1);
	}


	update {|connectedCount, cookies, clickedCount, typeOfHit|
		{
			semaphore.wait;
			accessCount = accessCount +1;
			numberOfConnectedSites = connectedCount !? ( _ * 1) ?? {numberOfConnectedSites};
			cookieCount = cookies !? ( _ * 1) ?? {cookieCount};
			clickedIntentionallyCount = clickedCount !? ( _ * 1) ?? {clickedIntentionallyCount};
			type = typeOfHit !? (_.asSymbol) ?? {type};
			semaphore.signal;
		}.fork;
	}

	toString {

		^ ("% % % % % % %").format(site, numberOfConnectedSites, cookieCount, clickedIntentionallyCount, accessCount,
			network, type);

	}
}

SoundBeamNetworkTable {

	classvar table, <networks;

	* initClass {

		table = Dictionary.new();

		// google
		table.put("google[^ \t\r\n\v\f\.]*\.com", \google);
		table.put("google[^ \t\r\n\v\f\.]*\.co\.uk", \google);
		table.put("gstatic\.com", \google);
		table.put("[^ \t\r\n\v\f\.]*\.google[:word:]*\.com", \google);
		table.put("[^ \t\r\n\v\f\.]*\.googleapis.com", \google);
		//ajax.googleapis.com
		//googletagservices.com
		// googleadservices.com
		// this just needs a regex


		//the book of face
		table.put("fbcdn\.net", \facebook);

		//what you doing, famous company?
		table.put("atdmt\.com", \microsoft);

		//(mostly) harmless
		table.put("twimg\.com", \twitter); // just an image server
		table.put("akamaihd\.net", \cloud); // delivers 1/3 of all internet traffic!!
		//us-east-1.elb.amazonaws.com // regex matching is going to be needed
		// informer.com // they make widgets
		// *.cloudfront.net


		/*
		Advertisers

		adnxs.com
		doubleclick.net
		quantserve.com
		amazon-adsystem.com
		*/

		/*
		Mysterious (and evil?)

		audienceamplify.com // registered in a way that hides the owner
		rambler.ru // this is just an isp, could be serving anything


		// specifically tracking users
		scorecardresearch.com // Specifically tracking users
		nexac.com
		newrelic.com
		alexa.com // owned by amazon
		mouseflow.com // tracks mouse movements
		chartbeat.net // chartbeat


		// Project Rover
		grvcdn.com // the company is named after a nuclear device
		gravity.com

		*/

		//table.put("".asSymbol, );

		networks = [];
		table.values({|v|
			networks.includes(v).not.if({
				networks = networks.add(v);
			})
		});


	}


	*getNetwork { |site|

		var network;

		table.keys.do({ |key|

			key.matchRegexp(site.asString).if({
				network = table.at(key);
				//break;  // naughty naughty // and disallowed :(
			})
		})

		^network;

	}



}


SoundBeam {

	var <netapi, chat, clock, hits;
	var <>hitAction, <>bothAction, <>thirdPartyAction, <>socialEventAction;
	var <>waitTime;



	*new { |nick = "Your Name Here"|

		var netapi;

		netapi = NetAPI.broadcast(nick);
		^super.new.init(netapi);
	}

	*newOSCGroups { |path, serveraddress, username, userpass|

		var netapi;
		netapi = NetAPI.oscgroup(path, serveraddress, username, userpass);
		^super.new.init(netapi);
	}


	init { |neta|

		//var action;

		netapi = neta; // if this gfives an error, that would be very annoying

		waitTime = 1.0;
		hits = IdentityDictionary.new;

		// Notifications from the plugin
		OSCFunc({|msg, time, addr, recvPort|
			this.pr_action(\both, msg);
		}, '/site/both');

		OSCFunc({|msg, time, addr, recvPort|
			this.pr_action(\hit, msg);
		}, '/site/site');

		OSCFunc({|msg, time, addr, recvPort|
			this.pr_action(\thirdParty, msg);
		}, '/site/thirdparty');


		netapi.add('thirdparty', {arg site, who;

			var site_data;
			(who.asSymbol != netapi.nick.asSymbol).if ({ // don't react to my own messages
				site_data = hits.at(site);
				site_data.notNil.if({

					Task({
						waitTime.rand.wait;
						this.socialEventAction.value(site_data);
					}).play
				});
			});
		});


	}



	haveHit { |site|
		^hits.at(site.asSymbol).notNil;
	}

	pr_action {|type, msg|
		var func, site, cons, cookies, net;
		var shouldSave, notify, site_data;

		shouldSave = false; notify = false;
		site = msg[1].asSymbol;

		site_data = hits.at(site);

		site_data.isNil.if({ // create it
			net = SoundBeamNetworkTable.getNetwork(site);
			site_data = SoundBeamSite(site, msg[2], 1, msg[3], msg[4], net, type);
			} , { // update it
				site_data.update(msg[2], msg[3], msg[4], type);
		});


		// This is all because these things are nil when the constructor is run
		func = case
		{type == \hit}  {
			//keep both false
			this.hitAction }
		{type == \both} {
			shouldSave = true; // save but don't notify
			this.bothAction }
		{type == \thirdParty} {
			shouldSave = true;
			notify = true; // save and notify
			this.thirdPartyAction };

		(shouldSave && site_data.notNil).if({
			hits.put(site, site_data);
		});

		(func.notNil && site_data.notNil).if({
			func.value(site_data);
		});


		notify.if({
			netapi.sendMsg('thirdparty', site, netapi.nick);
		});
	}

}