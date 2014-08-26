StatsD was popularized by Etsy, and we refer to their implementation as "Etsy-standard" (https://github.com/etsy/statsd/).
It's a light-weight method of gathering statistics from your applications.  As an application developer, all you need
to do is include a small library, and sprinkle one-liners like this throughout your code:

    Statsd.increment("my.important.event")
    Statsd.gauge("my.important.value", important_value)
    Statsd.timing("my.important.process", important_process_time)

In the Etsy version, this will cause a UDP packet to be sent to a designated server that is running their
collection and visualization packages. The AppFirst client API looks the same to the application developer,
but sends data via POSIX message queue or Windows Mailslot to the collector and takes advantage of AppFirst collection
and visualization technologies.

If you are already running an AppFirst collector on your server, then all you need to do is use an
AppFirst StatsD library instead of an Etsy-only library.  This library will aggregate your metrics, and then
use a message queue to pass them to the AppFirst collector, which will pass them up to our Big Data store, where
they will be visible on your AppFirst dashboards and Correlate charts.  This is more efficient than the UDP method
and you don't need to set up the Etsy collection and visualization environment.

If you are already using Etsy StatsD, you can make a gradual transition.  Our libraries can be used in
Etsy mode, so you can configure them to send UDP to your existing Etsy monitoring apparatus.  Our collector also
accepts StatsD UDP messages, so you can just point your existing Etsy-only StatsD library to localhost:8125,
until you are ready to transition to an AppFirst StatsD library. For more information on enabling UDP StatsD
on the collector click here: http://support.appfirst.com/appfirst-statsd-beta/#other_clients
