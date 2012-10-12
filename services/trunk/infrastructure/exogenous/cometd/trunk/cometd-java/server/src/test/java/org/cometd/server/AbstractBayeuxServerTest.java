package org.cometd.server;

import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;

import junit.framework.TestCase;
import org.cometd.Bayeux;
import org.cometd.server.continuation.ContinuationCometdServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * @version $Revision: 1130 $ $Date: 2010-04-19 06:30:36 -0400 (Mon, 19 Apr 2010) $
 */
public abstract class AbstractBayeuxServerTest extends TestCase
{
    protected Server server;
    protected ServletContextHandler context;
    protected String cometdURL;

    protected void setUp() throws Exception
    {
        server = new Server();
        SelectChannelConnector connector = new SelectChannelConnector();
        server.addConnector(connector);

        HandlerCollection handlers = new HandlerCollection();
        server.setHandler(handlers);

        String contextPath = "/cometd";
        context = new ServletContextHandler(handlers, contextPath, ServletContextHandler.SESSIONS);

        // Setup comet servlet
        ContinuationCometdServlet cometdServlet = new ContinuationCometdServlet();
        ServletHolder cometdServletHolder = new ServletHolder(cometdServlet);
        cometdServletHolder.setInitParameter("timeout", String.valueOf(5000));
        cometdServletHolder.setInitParameter("logLevel", "2");
        cometdServletHolder.setInitParameter("jsonDebug", "true");
        String cometdServletPath = "/cometd";
        context.addServlet(cometdServletHolder, cometdServletPath + "/*");

        // Setup bayeux listener
        context.addEventListener(new BayeuxInitializer());

        server.start();
        int port = connector.getLocalPort();

        String contextURL = "http://localhost:" + port + contextPath;
        cometdURL = contextURL + cometdServletPath;
    }

    protected void tearDown() throws Exception
    {
        server.stop();
        server.join();
    }

    protected void customizeBayeux(Bayeux bayeux)
    {
    }

    private class BayeuxInitializer implements ServletContextAttributeListener
    {
        public void attributeAdded(ServletContextAttributeEvent event)
        {
            if (event.getName().equals(Bayeux.ATTRIBUTE))
            {
                Bayeux bayeux = (Bayeux) event.getValue();
                customizeBayeux(bayeux);
            }
        }

        public void attributeRemoved(ServletContextAttributeEvent event)
        {
        }

        public void attributeReplaced(ServletContextAttributeEvent event)
        {
        }
    }
}