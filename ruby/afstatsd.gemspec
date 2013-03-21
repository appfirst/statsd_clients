Gem::Specification.new do |s|
    s.name          = 'afstatsd'
    s.version       = '0.0.3a'
    s.date          = '2013-03-11'
    s.summary       = "AppFirst StatsD Library"
    s.description   = "A StatsD library with AppFirst Extensions"
    s.authors       = ["Clark Bremer"]
    s.email         = 'clark@appfirst.com'
    s.files         = ["lib/afstatsd.rb", 
                       "lib/afstatsd/statsd_aggregator.rb",
                       "lib/afstatsd/statsd_metrics.rb",
                       "example/example.rb"]
    s.homepage      = 'http://appfirst.com'
    s.add_dependency("posix_mq", "~> 2.0.0")
end