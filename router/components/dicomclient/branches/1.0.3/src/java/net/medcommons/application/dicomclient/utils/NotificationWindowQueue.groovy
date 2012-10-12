/**
 * 
 */
package net.medcommons.application.dicomclient.utils

import java.util.concurrent.ConcurrentLinkedQueue
/**
 * @author ssadedin
 */
public class NotificationWindowQueue {

    LinkedBlockingQueue<ToolTipReplacementFrame> queue = new LinkedBlockingQueue()
    
    NotificationWindowQueue() {
        
        new Thread({
            while(true) {
                ToolTipReplacementFrame frame = queue.take();
                frame.show();
            }
            
        }).start()
    }
}