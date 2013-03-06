# Classes used to store and manipulate each type of metric
# each type must implement initialize, aggregate, and to_s

module StatsdMetrics

class Metric
    # all metrics share these
    attr_accessor :name
    attr_accessor :value
    attr_accessor :message
end

class CMetric < Metric
    # Counter
    def initialize(name, value, rate=1, msg="")
        @name = name
        @value = value
        @message = msg
        @sample_rate = rate
    end
    
    def aggregate(delta)
        @value += delta        #accumulate
    end
    
    def to_s
        if @sample_rate == 1 then r = "" else r = "|@#{@sample_rate}" end    
        if @message == "" 
            m = "" 
        else 
            if r == "" 
                m = "||#{@message}" 
            else    
                m = "|#{@message}" 
            end
        end
        "#{name}:#{@value}|c#{r}#{m}"
    end
end

class GMetric < Metric
    # Guage
    def initialize(name, value, msg="")
        @name = name
        @value = value
        @message = msg
    end
    
    def aggregate(value)
        @value = value        #overwrite
    end
    
    def to_s
        if @message == "" then m = "" else m = "|#{@message}" end
        "#{name}:#{@value}|g#{m}"
    end

end

class TMetric < Metric
    # Timing
    def initialize(name, value, rate=1, msg="")
        @name = name
        @value = value
        @sample_rate = rate
        @message = msg
        @count = 1
    end
    
    def aggregate(value)
        @value += value        #average
        @count += 1
    end
    
    def to_s
        avg = @value / @count
        if @message == "" then m = "" else m = "|#{@message}" end
        "#{name}:#{avg}|ms#{m}"
    end

end

class SMetric < Metric
    # Set (per the etsy standard)
    def initialize(name, value, msg="")
        @name = name
        @value = value
        @message = msg
    end
    
    def aggregate(value)
        @value = value        #overwrite
    end
    
    def to_s
        if @message == "" then m = "" else m = "|#{@message}" end
        "#{name}:#{@value}|s#{m}"
    end

end

end     #module StatsdMetrics

