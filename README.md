![Appfirst](http://www.appfirst.com/img/appfirst-logo.png)
StatsD Clients
==============
This is a collection of clients for both Etsy-standard StatsD and AppFirst-extended StatsD

*StatsD* was [popularized by Etsy](http://codeascraft.etsy.com/2011/02/15/measure-anything-measure-everything/), 
and we refer to their implementation as ["Etsy-standard"](https://github.com/etsy/statsd/#statsd-).  It's a 
light-weight method of gathering statistics from your applications.  As an application developer, all you need 
to do is include a small library, and sprinkle one-liners like this throughout your code:

    Statsd.increment("my.important.event")
    Statsd.guage("my.important.value", important_value)
    Statsd.timing("my.important.process", important_process_time)

In the Etsy version, this will cause a UDP packet to be sent to a designated server that is running their 
collection and visualization packages.  The AppFirst model looks almost the same to the application developer, 
but takes advantage of AppFirst collection and visualization technologies.  

You are probably already running an AppFirst collector on your server.  All you need to do is use an 
AppFirst StatsD library instead of an Etsy-only library.  This library will aggregate your metrics, and then 
use a message queue to pass them to the AppFirst collector, which will pass them up to our Big Data store, where 
they will be visible on your AppFirst dashboards and Correlate charts.  This is more efficient than the UDP method 
and you don't need to set up the Etsy collection and visualization environment.  

[Click here to see a diagram of the AppFirst model](https://appfirst.fogbugz.com/default.asp?w423)

If you are already using Etsy StatsD, you can make a gradual transition.  Our libraries can be used in 
Etsy mode, so you can configure them to send UDP to your existing Etsy monitoring apparatus.  Our collector also
accepts StatsD UDP messages, so you can just point your existing Etsy-only StatsD library to localhost:8125, 
until you are ready to transition to an AppFirst StatsD library.

We've also extended the capabilities of StatsD, by allowing you to optionally annotate your event reports:

    Statsd.increment("security.suspicious_login", attempted_username)
    
We provide more details on how to use our StatsD libraries in the readme file found under each language.


AppFirst Statsd Clients
-----------------------
StatsD libraries with AppFirst extensions are available here in the following languages:

    java                    
    python 
    .net (csharp)
    ruby
 
Etsy-standard Statsd libraries are available in many other languages, and we have collected a few of them 
here for your convenience.

