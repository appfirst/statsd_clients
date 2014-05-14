# Classes used to store and manipulate each type of metric
# each type must implement initialize, aggregate, and to_s

module StatsdMetrics

class Metric
    # all metrics share these
    attr_accessor :name
    attr_accessor :value
end

class CMetric < Metric
    # Counter
    def initialize(name, value, rate=1)
        @name = name
        @value = value
        @sample_rate = rate
    end

    def aggregate(delta)
        @value += delta        #accumulate
    end

    def to_s
        if @sample_rate == 1 then r = "" else r = "|@#{@sample_rate}" end
        "#{name}:#{@value}|c#{r}"
    end
end

class GMetric < Metric
    # Guage
    def initialize(name, value)
        @name = name
        @value = value
    end

    def aggregate(value)
        @value = value        #overwrite
    end

    def to_s
        "#{name}:#{@value}|g"
    end

end

class TMetric < Metric
    # Timing
    def initialize(name, value, rate=1)
        @name = name
        @value = value
        @sample_rate = rate
        @count = 1
    end

    def aggregate(value)
        @value += value        #average
        @count += 1
    end

    def to_s
        avg = @value / @count
        "#{name}:#{avg}|ms"
    end

end

class SMetric < Metric
    # Set (per the etsy standard)
    def initialize(name, value)
        @name = name
        @value = value
    end

    def aggregate(value)
        @value = value        #overwrite
    end

    def to_s
        "#{name}:#{@value}|s"
    end

end

end     #module StatsdMetrics
