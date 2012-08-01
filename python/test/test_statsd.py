'''
Created on Jul 30, 2012

@author: Yangming
'''
try:
    import posix_ipc
except:
    print "Dependency: posix_ipc is not installed."
    exit(1)
import signal
import time

MQ_DESCRIPTOR = "/afcollectorapi"
mqueue = None

def receive_msg():
    global mqueue
    if not mqueue:
        mqueue = posix_ipc.MessageQueue(MQ_DESCRIPTOR)
        mqueue.request_notification()
        signal.signal(signal.SIGUSR1, receive_msg)
    while True:
        msg, _ = mqueue.receive()
        if msg:
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
        mqueue.close()