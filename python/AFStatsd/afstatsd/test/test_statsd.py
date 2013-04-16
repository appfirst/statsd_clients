'''
Created on Jul 30, 2012

@author: Yangming


Intercept statsd messages sent through AFTransport. Note this will scramble the message queue 
'''
try:
    import posix_ipc
except:
    print "Dependency: posix_ipc is not installed."
    exit(1)
import signal
import time

MQ_DESCRIPTOR = "/afcollectorapi"
STATSD_SEVERITY = 3
mqueue = None

def receive_msg():
    global mqueue
    if not mqueue:
        mqueue = posix_ipc.MessageQueue(MQ_DESCRIPTOR)
        mqueue.request_notification()
        signal.signal(signal.SIGUSR1, receive_msg)
    while True:
        msg, prio = mqueue.receive()
        if msg:
            if prio == STATSD_SEVERITY:
                print msg
        else:
            mqueue.request_notification(signal.SIGUSR1)
            break

if __name__ == "__main__":
    try:
        receive_msg()
        while True:
            time.sleep(3600)
    except posix_ipc.PermissionsError as pe:
        print pe.message
    except (KeyboardInterrupt, SystemExit):
        exit(0)
    finally:
        if mqueue:
            mqueue.close()
