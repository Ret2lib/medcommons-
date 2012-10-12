/**
 * 
 */
package net.medcommons.application.dicomclient

import org.junit.Before
import org.apache.log4j.BasicConfigurator

/**
 * @author ssadedin
 */
public class ImportHandlerTest {
    
    static {
        BasicConfigurator.configure()
        StatusDisplayManager.testMode = true
        Logger.getLogger("net.sourceforge.pbeans").level = Level.WARNING
     }

    
    @Before
    void setup() {
        DB.testMode()
        DB.get().insert(ref)
    }

    @Test
    void testProcessSOPInstance() {
        
    }
    
}