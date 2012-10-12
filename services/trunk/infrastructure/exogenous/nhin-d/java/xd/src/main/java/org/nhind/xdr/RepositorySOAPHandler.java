package org.nhind.xdr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import javax.servlet.ServletRequest;

import javax.xml.namespace.QName;


import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.SOAPFaultException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;





/**
 * This class handles the SOAP-Requests before they reach the
 * Web Service Operation. It is possible to read and manipulate
 * the SOAP-Message.
 *
 * @author Siegfried Bolz
 */
public class RepositorySOAPHandler implements SOAPHandler<SOAPMessageContext> {

    protected String endpoint;
    protected String messageId;
    protected String relatesTo;
    protected String action;
    protected String to;
    protected String remoteHost;
    protected String thisHost;
    protected String pid;
    protected String from;
    private static boolean first = true;

    /**
     * Is called after constructing the handler and before executing any othe method.
     */
    @PostConstruct
    public void init() {
        if (first) {

            first = false;
         //   Properties properties = new Properties();
            try {
               // loadProperties("system.properties", properties);
               // Properties sysprop = System.getProperties();
               // sysprop.putAll(properties);
              //  Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,properties.toString());
            } catch (Exception exception) {
                Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,"Problem with properties file");
            }
        }
    }

    @Override
    public void close(MessageContext messageContext) {
    }

    /**
     * Is executed before this handler is being destroyed -
     * means after close() has been executed.
     */
    @PreDestroy
    public void destroy() {
    }

    /**
     * This method handles the incoming and outgoing SOAP-Message.
     * It's an excellent point to manipulate the SOAP.
     *
     * @param SOAPMessageContext
     * @return boolean
     */
    @Override
    public boolean handleMessage(SOAPMessageContext context) {

        //Inquire incoming or outgoing message.
        boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        try {

            if (outbound) {
                getHeaderData();

                SOAPMessage msg = ((SOAPMessageContext) context).getMessage();
                //  dumpSOAPMessage(msg);

                SOAPPart sp = msg.getSOAPPart();

                //edit Envelope
                SOAPEnvelope env = sp.getEnvelope();
                SOAPHeader sh = env.addHeader();
                SOAPBody sb = env.getBody();

                if (action != null) {
                    QName qname = new QName("http://www.w3.org/2005/08/addressing", "Action");
                    SOAPHeaderElement saction = sh.addHeaderElement(qname);
                    boolean must = true;

                    saction.setMustUnderstand(must);
                    saction.setValue(action);
                }
                if (relatesTo != null) {
                    QName qname = new QName("http://www.w3.org/2005/08/addressing", "RelatesTo");
                    SOAPHeaderElement relates = sh.addHeaderElement(qname);
                    relates.setValue(relatesTo);
                }
                if (from != null) {
                    QName qname = new QName("http://www.w3.org/2005/08/addressing", "From");
                    QName child = new QName("http://www.w3.org/2005/08/addressing", "Address");
                    SOAPHeaderElement efrom = sh.addHeaderElement(qname);
                    SOAPElement address = efrom.addChildElement(child);
                    address.setValue(from);

                }

                if (messageId != null) {
                    QName qname = new QName("http://www.w3.org/2005/08/addressing", "MessageID");
                    SOAPHeaderElement message = sh.addHeaderElement(qname);
                    message.setValue(messageId);
                }
                if (to != null) {
                    QName qname = new QName("http://www.w3.org/2005/08/addressing", "To");
                    SOAPHeaderElement sto = sh.addHeaderElement(qname);
                    sto.setValue(to);
                }


            } else {
                // INBOUND
                messageId = null;
                action = null;
                endpoint = null;
                from = null;
                to = null;
                remoteHost = null;
                thisHost = null;
                pid = null;
                relatesTo = null;
             
                Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,"Direction=inbound (handleMessage)");

                SOAPMessage msg = ((SOAPMessageContext) context).getMessage();
                dumpSOAPMessage(msg);

                ServletRequest sr = (ServletRequest) context.get(context.SERVLET_REQUEST);
                if (sr != null) {
                    remoteHost = sr.getRemoteHost();
                    thisHost = sr.getServerName();
                    pid = getPID();
                }

                SOAPPart sp = msg.getSOAPPart();

                //edit Envelope
                SOAPEnvelope env = sp.getEnvelope();
                SOAPHeader sh = env.getHeader();
                Iterator it = sh.extractAllHeaderElements();
                while (it.hasNext()) {
                    Node header = (Node) it.next();
                    Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,header.getNodeName());

                    if (header.toString().indexOf("MessageID") >= 0) {
                        messageId = header.getTextContent();
                        Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,messageId);

                    } else if (header.toString().indexOf("Action") >= 0) {
                        action = header.getTextContent();
                        Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,action);
                    } else if (header.toString().indexOf("RelatesTo") >= 0) {
                        relatesTo = header.getTextContent();
                        Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,action);
                    } else if (header.toString().indexOf("ReplyTo") >= 0) {
                        NodeList reps = header.getChildNodes();
                        for (int i = 0; i < reps.getLength(); i++) {
                            Node address = reps.item(i);
                            Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,address.getNodeName());
                            if (address.getNodeName().indexOf("Address") >= 0) {
                                endpoint = address.getTextContent();
                                Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,endpoint);

                            }
                        }
                    } else if (header.toString().indexOf("From") >= 0) {
                        NodeList reps = header.getChildNodes();
                        for (int i = 0; i < reps.getLength(); i++) {
                            Node address = reps.item(i);
                            Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,address.getNodeName());
                            if (address.getNodeName().indexOf("Address") >= 0) {
                                from = address.getTextContent();
                                Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,from);

                            }
                        }
                    } else if (header.toString().indexOf("To") >= 0) {// must be after ReplyTo
                        to = header.getTextContent();
                        Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,to);
                    }
                }
                setHeaderData();


            }

        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void loadProperties(String fileName, Properties properties) throws IOException {

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("META-INF/" + fileName);
        properties.load(inputStream);
    }

  

   

    /**
     * Returns the <code>Set</code> of supported SOAP headers
     */
    @Override
    public Set<QName> getHeaders() {

        HashSet set = new HashSet();
        QName qname = new QName("http://www.w3.org/2005/08/addressing", "Action");
        set.add(qname);
        qname = new QName("http://www.w3.org/2005/08/addressing", "To");
        set.add(qname);
        qname = new QName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "Security");
        set.add(qname);
        return set;
    }

   

    /**
     * Returns the message encoding (e.g. utf-8)
     *
     * @param msg
     * @return
     * @throws javax.xml.soap.SOAPException
     */
    protected String getMessageEncoding(SOAPMessage msg) throws SOAPException {
        String encoding = "utf-8";
        if (msg.getProperty(SOAPMessage.CHARACTER_SET_ENCODING) != null) {
            encoding = msg.getProperty(SOAPMessage.CHARACTER_SET_ENCODING).toString();
        }
        return encoding;
    }

    /**
     * Dump SOAP Message to console
     *
     * @param msg
     */
    protected void dumpSOAPMessage(SOAPMessage msg) {
        if (msg == null) {
            Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,"SOAP Message is null");
            return;
        }
        Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,"");
        Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,"--------------------");
        Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO," DUMP OF SOAP MESSAGE");
        Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,"--------------------");
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            msg.writeTo(baos);
            Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,baos.toString(getMessageEncoding(msg)));

            // show included values
            String values = msg.getSOAPBody().getTextContent();
            Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,"Included values:" + values);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

  

  

    /**
     * Handles SOAP-Errors.
     *
     * @param context
     * @return
     */
    @Override
    public boolean handleFault(SOAPMessageContext context) {
        Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,"ServerSOAPHandler.handleFault");
        boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (outbound) {
            Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,"Direction=outbound (handleFault)");
        } else {
            Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,"Direction=inbound (handleFault)");
        }

        try {
            SOAPMessage msg = ((SOAPMessageContext) context).getMessage();
            // dumpSOAPMessage(msg);
            if (context.getMessage().getSOAPBody().getFault() != null) {
                String detailName = null;
                try {
                    detailName = context.getMessage().getSOAPBody().getFault().getDetail().getFirstChild().getLocalName();
                    Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,"detailName=" + detailName);
                } catch (Exception e) {
                }
            }
        } catch (SOAPException e) {
            e.printStackTrace();
        }

        return true;
    }

    public Object unmarshalMessage(QName altName, String xml, Class factory) {

        Object ret = null;
        try {
            //   javax.xml.bind.JAXBContext jaxbCtx = javax.xml.bind.JAXBContext.newInstance(msg.getClass().getPackage().getName());
            javax.xml.bind.JAXBContext jaxbCtx = javax.xml.bind.JAXBContext.newInstance(factory);
            javax.xml.bind.Unmarshaller unmarshaller = jaxbCtx.createUnmarshaller();


            byte currentXMLBytes[] = xml.getBytes();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(currentXMLBytes);
            ret = unmarshaller.unmarshal(byteArrayInputStream);

        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getPackage().getName()).log(Level.INFO,xml.substring(0, 50) + " Failed to Unmarshall. Exception msg=" + ex.getMessage());
            ex.printStackTrace();

        }
        return ret;
    }

    

    private SOAPFaultException createSOAPFaultException(String faultString,
            Boolean clientFault) {
        try {
            String faultCode = clientFault ? "Client" : "Server";
            SOAPFault fault = SOAPFactory.newInstance().createFault();
            fault.setFaultString(faultString);
            fault.setFaultCode(new QName(SOAPConstants.URI_NS_SOAP_ENVELOPE, faultCode));
            return new SOAPFaultException(fault);
        } catch (SOAPException e) {
            throw new RuntimeException("Error creating SOAP Fault message, faultString: " + faultString);
        }

    }

    protected void getHeaderData() {
        Long threadId = new Long(Thread.currentThread().getId());
        Logger.getLogger(this.getClass().getPackage().getName()).log(Level.FINE,"GTHREAD ID " + threadId);
        ThreadData threadData = new ThreadData(threadId);
        messageId = threadData.getMessageId();
        to = threadData.getTo();
        relatesTo = threadData.getRelatesTo();
        action = threadData.getAction();
        thisHost = threadData.getThisHost();
        remoteHost = threadData.getRemoteHost();
        pid = threadData.getPid();
        from = threadData.getFrom();
    }

    protected void setHeaderData() {
        Long threadId = new Long(Thread.currentThread().getId());
        Logger.getLogger(this.getClass().getPackage().getName()).log(Level.FINE,"GTHREAD ID " + threadId);
        ThreadData threadData = new ThreadData(threadId);
        threadData.setReplyAddress(endpoint);
        threadData.setMessageId(messageId);
        threadData.setAction(action);
        threadData.setThisHost(thisHost);
        threadData.setRemoteHost(remoteHost);
        threadData.setTo(to);
        threadData.setPid(pid);
        threadData.setFrom(from);
    }

    public String getPID() {
        String processName =
                ManagementFactory.getRuntimeMXBean().getName();
        return processName.split("@")[0];
    }
} // .EOF
