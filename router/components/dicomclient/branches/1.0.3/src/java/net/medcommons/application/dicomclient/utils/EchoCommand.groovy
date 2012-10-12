/**
 * $Id$
 */
package net.medcommons.application.dicomclient.utils

import org.json.JSONObject
import net.medcommons.application.utils.JSONSimpleGET
import java.util.concurrent.Future
import static net.medcommons.application.dicomclient.utils.ManagedTransaction.*


/**
 * Simple test command that places "hello world" in the out queue
 * 
 * @author ssadedin
 */
public class EchoCommand implements Command {
     
    private static Logger log = Logger.getLogger(EchoCommand.class);

    public Future<JSONObject> execute(CommandBlock params) {
        
        [ isDone: { true }, get: { new JSONObject().put("message", "hello world")} ] as Future<JSONObject>
    }
}
