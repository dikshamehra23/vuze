/*
 * Created on 22 juil. 2003
 *
 */
package org.gudy.azureus2.core;

import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Olivier
 * 
 */
public class TrackerChecker {

  private HashMap trackers;

  public TrackerChecker() {
    trackers = new HashMap();
  }

  public HashData getHashData(String trackerUrl, byte[] hash) {
    return getHashData(trackerUrl,new Hash(hash));
  }

  public void removeHash(String trackerUrl,Hash hash) {
    TrackerStatus ts = (TrackerStatus) trackers.get(trackerUrl);
    if(ts != null) {
      ts.removeHash(hash);
    }
  }
  
  public HashData getHashData(String trackerUrl,final Hash hash) {
    if (trackers.containsKey(trackerUrl)) {
      TrackerStatus ts = (TrackerStatus) trackers.get(trackerUrl);
      HashData data = ts.getHashData(hash);
      if(data != null)
        return data;
     else
        ts.update(hash);
    }
    final TrackerStatus ts = new TrackerStatus(trackerUrl);
    synchronized (trackers) {
      trackers.put(trackerUrl, ts);
    }
    Thread t = new Thread() {
      /* (non-Javadoc)
       * @see java.lang.Thread#run()
       */
      public void run() {
        ts.update(hash);
      }
    };
    t.setDaemon(true);
    t.setPriority(Thread.MIN_PRIORITY);
    t.start();
    return null;
  }

  public void update() {
    synchronized (trackers) {
      Iterator iter = trackers.values().iterator();
      while (iter.hasNext()) {
        final TrackerStatus ts = (TrackerStatus) iter.next();
        Thread t = new Thread() {
          /* (non-Javadoc)
           * @see java.lang.Thread#run()
           */
          public void run() {
            Iterator iter = ts.getHashesIterator();
            while(iter.hasNext()) {              
              Hash hash = (Hash) iter.next();
              ts.update(hash);
            }           
            }
        };
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
      }
    }
  }

}
