/**
 * 
 */
package net.medcommons.application.dicomclient.command


import org.apache.log4j.Logger
import net.medcommons.application.dicomclient.Configurations;
import net.medcommons.application.dicomclient.ContextManager;
import net.medcommons.application.dicomclient.dicom.CstoreScp
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import javax.swing.SwingUtilities
import net.medcommons.application.dicomclient.utils.Command;
import net.medcommons.application.dicomclient.utils.CommandBlock;
import net.medcommons.application.dicomclient.utils.UI;

import java.io.StringWriter;
import java.util.concurrent.Future
/**
 * Allows execution of Groovy by a remote admin.
 * <b>This is very dangerous and must only be allowed under 
 * direct / live support.</b>
 * 
 * @author ssadedin
 */
public class AdminCommand implements Command {

	private static Logger log = Logger.getLogger(AdminCommand.class);
    
    private static boolean enabled = false
	
    public Future<JSONObject> execute(CommandBlock params) { 
        
        log.info "Security enabled = " + enabled
        if(!enabled) {
            UI.get().showSecurityVerification(
                """A remote administrator is attempting to enable a support session for your DDL<br><br>
                   Please click Yes if you want to enable the session.<br><br>
                   If you did not request support from MedCommons, click No""")
            enabled = true
        }
        
		JSONObject result = new JSONObject()
        
        try {
            Binding binding = new Binding()
            
            ContextState ctx = ContextManager.get().currentContextState
            Configurations cfg = ContextManager.get().getConfigurations()
    
            StringWriter out = new StringWriter()            
            binding.setVariable("out", out)
            
            binding.setVariable("cfg",cfg)
            binding.setVariable("ctx",ctx)
            
            log.info "Executing admin script ${params.properties.script}"
            
            GroovyShell shell = new GroovyShell(binding)
            shell.evaluate(params.properties.script)
            result.put("status", "ok")
            result.put("out",out.toString())
        }
        catch(Throwable t) {
            log.error("Admin command failed", t)
            result.put("status", "failed")
            result.put("error", t.toString())
        }
            
        // return [ isDone: { true },  get: { result } ] as Future<JSONObject>
        log.error("ADMIN COMMAND:  ${params.properties.script} => "+result.toString())
    }
}