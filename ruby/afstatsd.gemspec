Gem::Specification.new do |s|
    s.name          = 'afstatsd'
    s.version       = '1.1.0'
    s.date          = '2014-08-26'
    s.summary       = "AppFirst StatsD Library"
    s.description   = "A StatsD library with AppFirst Extensions"
    s.authors       = ["Mike Okner", "Clark Bremer"]
    s.email         = 'michael@appfirst.com'
    s.license	    = 'APACHE'
    s.files         = ["lib/afstatsd.rb", 
                       "lib/afstatsd/statsd_aggregator.rb",
                       "lib/afstatsd/statsd_metrics.rb",
                       "example/example.rb"]
    s.extensions    = ['ext/mkrf_conf.rb']
    s.homepage      = 'http://www.appfirst.com/'
end
