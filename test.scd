(

var sbeam;

//sbeam = SoundBeam.new("les"); // make a new object to use broadcast messaging
b = SoundBeam.newOSCGroups("/home/celesteh/Documents/code/osc/oscgroups/bin/OscGroupClient", "192.168.0.9", "les", "foo");

// set an action function for third party sites (ones you haven't clicked on
// the message gets a SoundBeamSite as the argument
b.thirdPartyAction = {|site, time|

	site.toString.postln;

	((site.accessCount  % 10) == 0).if({
		("%: %\n").postf(site.accessCount, site.toString);
	});
};

// you can also set:
b.hitAction = { |site, time| site.postln}; // for a site you have clicked on but has served no extra content
b.bothAction = {|site, time| site.postln}; // for a site you've clieck and has served extra content such as images

// for the social aspect of the piece:
b.socialEventAction = {|site, time| site.postln}; // another BiLE member has hit a third party site you have also encountered


)



NetAddr.langPort


OSCFunc.trace




(
var sites;

sites = IdentityDictionary.new;
OSCFunc({|msg, time, addr, recvPort|

	var site, last;
	site = msg[1].asSymbol;

	last = sites.at(site);

	last.isNil.if({
		last = 0;
	}, {

			last = last[5];
	});

	last = last+1;

	msg = msg ++ last;

	((last % 10) == 0).if ({
		last.post;
		"\t".post;
		msg.postln;
	});

	sites.put(site, msg);


}, '/site/thirdparty');

)








