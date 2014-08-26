# Classes used to store and manipulate each type of metric
# each type must implement initialize, aggregate, and to_s

module StatsdMetrics

class Metric
    # all metrics share these
    attr_accessor :name
    attr_accessor :value

    def to_s(udp=False)
        # Handle differences in upload method if sending via UDP or to AppFirst
        if udp
            return @_to_udp_s
        else
            return @_to_af_s
        end
    end

    def _to_af_s
        # Return the same value by default
        return @_to_udp_s
    end

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

    def _to_udp_s
        if @sample_rate == 1 then r = "" else r = "|@#{@sample_rate}" end
        "#{name}:#{@value}|c#{r}"
    end
end

class GMetric < Metric
    # Gauge
    def initialize(name, value)
        @name = name
        @value = value
    end

    def aggregate(value)
        @value = value        #overwrite
    end

    def _to_udp_s
        "#{name}:#{@value}|g"
    end

end

class TMetric < Metric
    # Timing
    def initialize(name, value, rate=1)
        @name = name
        @value = [value]
        @count = 1
    end

    def aggregate(value)
        # Value is another TMetric @value attribute (Array)
        @value += value
        @count += 1
    end

    def _to_udp_s
        sumvals = @value.inject(0) {|sum, i|  sum + i }
        avg = sumvals / @count
        "#{name}:#{avg}|ms"
    end

    def _to_af_s
        vals = @value.join(",")
        "#{name}:#{vals}|ms"
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

    def _to_udp_s
        "#{name}:#{@value}|s"
    end

end

end     #module StatsdMetrics
