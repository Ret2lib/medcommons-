package net.medcommons.application.dicomclient.dicom;


/**
 * Old licence below.
 * This file was derived from the java class
 * package org.dcm4che2.tool.dcmsnd.DcmSnd
 */
/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Gunter Zeilinger, Huetteldorferstr. 24/10, 1150 Vienna/Austria/Europe.
 * Portions created by the Initial Developer are Copyright (C) 2002-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunterze@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

//package org.dcm4che2.tool.dcmsnd;

import static net.medcommons.application.utils.Str.blank;

import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import net.medcommons.application.dicomclient.transactions.TransactionUtils;
import net.medcommons.application.dicomclient.utils.DB;
import net.medcommons.application.dicomclient.utils.DicomOutputTransaction;
import net.medcommons.application.dicomclient.utils.ExtractFileMetadata;
import net.medcommons.application.dicomclient.utils.ManagedTransaction;
import net.medcommons.application.dicomclient.utils.StatusDisplayManager;
import net.medcommons.modules.services.interfaces.DicomMetadata;
import net.sourceforge.pbeans.Store;

import org.apache.log4j.Logger;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.UIDDictionary;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.io.StopTagInputHandler;
import org.dcm4che2.io.TranscoderInputHandler;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.ConfigurationException;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.DimseRSPHandler;
import org.dcm4che2.net.Executor;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NewThreadExecutor;
import org.dcm4che2.net.NoPresentationContextException;
import org.dcm4che2.net.PDVOutputStream;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.util.StringUtils;

/**
 * @author gunter zeilinger(gunterze@gmail.com)
 * @version $Revision: 1.26 $ $Date: 2007/02/19 10:25:46 $
 * @since Oct 13, 2005
 */
public class CstoreScu {
    private static Logger log = Logger.getLogger("CstoreScu");


    private static final int KB = 1024;

    private static final int MB = KB * KB;

    private static final int PEEK_LEN = 1024;

    private static final String USAGE =
        "dcmsnd [Options] <aet>[@<host>[:<port>]] <file>|<directory>...";

    private static final String DESCRIPTION =
        "Load composite DICOM Object(s) from specified DICOM file(s) and send it "
      + "to the specified remote Application Entity. If a directory is specified,"
      + "DICOM Object in files under that directory and further sub-directories "
      + "are sent. If <port> is not specified, DICOM default port 104 is assumed. "
      + "If also no <host> is specified, localhost is assumed.\n"
      + "Options:";

    private static final String EXAMPLE =
        "\nExample: dcmsnd STORESCP@localhost:11112 image.dcm \n"
      + "=> Send DICOM object image.dcm to Application Entity STORESCP, "
      + "listening on local port 11112.";

    private static final String[] IVLE_TS = {
        UID.ImplicitVRLittleEndian,
        UID.ExplicitVRLittleEndian,
        UID.ExplicitVRBigEndian,
    };

    private static final String[] EVLE_TS = {
        UID.ExplicitVRLittleEndian,
        UID.ImplicitVRLittleEndian,
        UID.ExplicitVRBigEndian,
    };

    private static final String[] EVBE_TS = {
        UID.ExplicitVRBigEndian,
        UID.ExplicitVRLittleEndian,
        UID.ImplicitVRLittleEndian,
    };

    private Executor executor = new NewThreadExecutor("DCMSND");

    private NetworkApplicationEntity remoteAE = new NetworkApplicationEntity();

    private NetworkConnection remoteConn = new NetworkConnection();

    private Device device = new Device("DCMSND");

    private NetworkApplicationEntity ae = new NetworkApplicationEntity();

    private NetworkConnection conn = new NetworkConnection();

    private HashMap as2ts = new HashMap();

    private DicomOutputTransaction dicomOutputTransaction = null;

    private ArrayList<FileInfo> files = new ArrayList<FileInfo>();

    private Association assoc;

    private int priority = 0;

    private int transcoderBufferSize = 1024;

    private int filesSent = 0;

    private int filesSkipped = 0;

    private long totalSize = 0L;

    private boolean cancel = false;;


    public void cancelTransfer(){
        cancel = true;
    }

    public CstoreScu() {
        remoteAE.setInstalled(true);
        remoteAE.setAssociationAcceptor(true);
        remoteAE.setNetworkConnection(new NetworkConnection[] { remoteConn });

        device.setNetworkApplicationEntity(ae);
        device.setNetworkConnection(conn);
        ae.setNetworkConnection(conn);
        ae.setAssociationInitiator(true);

        ae.setAETitle("DCMSND");

    }

    public final void setLocalHost(String hostname) {
        conn.setHostname(hostname);
    }

    public final void setRemoteHost(String hostname) {
        remoteConn.setHostname(hostname);
    }

    public final void setRemotePort(int port) {
        remoteConn.setPort(port);
    }

    public final void setCalledAET(String called) {
        remoteAE.setAETitle(called);
    }

    public final void setCalling(String calling) {
        ae.setAETitle(calling);
    }

    public final void setConnectTimeout(int connectTimeout) {
        conn.setConnectTimeout(connectTimeout);
    }

    public final void setMaxPDULengthReceive(int maxPDULength) {
        ae.setMaxPDULengthReceive(maxPDULength);
    }

    public final void setMaxOpsInvoked(int maxOpsInvoked) {
        ae.setMaxOpsInvoked(maxOpsInvoked);
    }

    public final void setPackPDV(boolean packPDV) {
        ae.setPackPDV(packPDV);
    }

    public final void setAssociationReaperPeriod(int period) {
        device.setAssociationReaperPeriod(period);
    }

    public final void setDimseRspTimeout(int timeout) {
        ae.setDimseRspTimeout(timeout);
    }

    public final void setPriority(int priority) {
        this.priority = priority;
    }

    public final void setTcpNoDelay(boolean tcpNoDelay) {
        conn.setTcpNoDelay(tcpNoDelay);
    }

    public final void setAcceptTimeout(int timeout) {
        conn.setAcceptTimeout(timeout);
    }

    public final void setReleaseTimeout(int timeout) {
        conn.setReleaseTimeout(timeout);
    }

    public final void setSocketCloseDelay(int timeout) {
        conn.setSocketCloseDelay(timeout);
    }

    public final void setMaxPDULengthSend(int maxPDULength) {
        ae.setMaxPDULengthSend(maxPDULength);
    }

    public final void setReceiveBufferSize(int bufferSize) {
        conn.setReceiveBufferSize(bufferSize);
    }

    public final void setSendBufferSize(int bufferSize) {
        conn.setSendBufferSize(bufferSize);
    }

    public final void setTranscoderBufferSize(int transcoderBufferSize) {
        this.transcoderBufferSize = transcoderBufferSize;
    }

    public final int getNumberOfFilesToSend() {
        return files.size();
    }

    public final int getNumberOfFilesSent() {
        return filesSent;
    }

    public final long getTotalSizeSent() {
        return totalSize;
    }

    /**
     *  Returns the number of files that 'added' but skipped because they were not DICOM files.
     **/
    public final int getFilesSkipped(){
        return filesSkipped;
    }
/*
    private static CommandLine parse(String[] args) {
        Options opts = new Options();
        OptionBuilder.withArgName("aet[@host]");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "set AET and local address of local Application Entity, use " +
                "ANONYMOUS and pick up any valid local address to bind the " +
                "socket by default");
        opts.addOption(OptionBuilder.create("L"));

        OptionBuilder.withArgName("maxops");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "maximum number of outstanding operations it may invoke " +
                "asynchronously, unlimited by default.");
        opts.addOption(OptionBuilder.create("async"));

        opts.addOption("pdv1", false,
                "send only one PDV in one P-Data-TF PDU, " +
                "pack command and data PDV in one P-DATA-TF PDU by default.");
        opts.addOption("tcpdelay", false,
                "set TCP_NODELAY socket option to false, true by default");

        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "timeout in ms for TCP connect, no timeout by default");
        opts.addOption(OptionBuilder.create("connectTO"));

        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "delay in ms for Socket close after sending A-ABORT, " +
                "50ms by default");
        opts.addOption(OptionBuilder.create("soclosedelay"));

        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "period in ms to check for outstanding DIMSE-RSP, " +
                "10s by default");
        opts.addOption(OptionBuilder.create("reaper"));

        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "timeout in ms for receiving DIMSE-RSP, 60s by default");
        opts.addOption(OptionBuilder.create("rspTO"));

        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "timeout in ms for receiving A-ASSOCIATE-AC, 5s by default");
        opts.addOption(OptionBuilder.create("acceptTO"));

        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "timeout in ms for receiving A-RELEASE-RP, 5s by default");
        opts.addOption(OptionBuilder.create("releaseTO"));

        OptionBuilder.withArgName("KB");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "maximal length in KB of received P-DATA-TF PDUs, 16KB by default");
        opts.addOption(OptionBuilder.create("rcvpdulen"));

        OptionBuilder.withArgName("KB");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "maximal length in KB of sent P-DATA-TF PDUs, 16KB by default");
        opts.addOption(OptionBuilder.create("sndpdulen"));

        OptionBuilder.withArgName("KB");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "set SO_RCVBUF socket option to specified value in KB");
        opts.addOption(OptionBuilder.create("sorcvbuf"));

        OptionBuilder.withArgName("KB");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "set SO_SNDBUF socket option to specified value in KB");
        opts.addOption(OptionBuilder.create("sosndbuf"));

        OptionBuilder.withArgName("KB");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "transcoder buffer size in KB, 1KB by default");
        opts.addOption(OptionBuilder.create("bufsize"));

        opts.addOption("lowprior", false,
                "LOW priority of the C-STORE operation, MEDIUM by default");
        opts.addOption("highprior", false,
                "HIGH priority of the C-STORE operation, MEDIUM by default");
        opts.addOption("h", "help", false, "print this message");
        opts.addOption("V", "version", false,
                "print the version information and exit");
        CommandLine cl = null;
        try {
            cl = new GnuParser().parse(opts, args);
        } catch (ParseException e) {
            exit("dcmsnd: " + e.getMessage());
        }
        if (cl.hasOption('V')) {
            Package p = DcmSnd.class.getPackage();
            log.info("dcmsnd v" + p.getImplementationVersion());
            System.exit(0);
        }
        if (cl.hasOption('h') || cl.getArgList().size() < 2) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(USAGE, DESCRIPTION, opts, EXAMPLE);
            System.exit(0);
        }
        return cl;
    }
*/
    /*
    public static void main(String[] args) {
       // CommandLine cl = parse(args);
        CstoreScp dcmsnd = new CstoreScp();
        final List argList = cl.getArgList();
        String remoteAE = (String) argList.get(0);
        String[] calledAETAddress = split(remoteAE, '@');
        dcmsnd.setCalledAET(calledAETAddress[0]);
        if (calledAETAddress[1] == null) {
            dcmsnd.setRemoteHost("127.0.0.1");
            dcmsnd.setRemotePort(104);
        } else {
            String[] hostPort = split(calledAETAddress[1], ':');
            dcmsnd.setRemoteHost(hostPort[0]);
            dcmsnd.setRemotePort(toPort(hostPort[1]));
        }
        if (cl.hasOption("L")) {
            String localAE = (String) cl.getOptionValue("L");
            String[] callingAETHost = split(localAE, '@');
            dcmsnd.setCalling(callingAETHost[0]);
            if (callingAETHost[1] != null) {
                dcmsnd.setLocalHost(callingAETHost[1]);
            }
        }
        if (cl.hasOption("connectTO"))
            dcmsnd.setConnectTimeout(parseInt(cl.getOptionValue("connectTO"),
                    "illegal argument of option -connectTO", 1,
                    Integer.MAX_VALUE));
        if (cl.hasOption("reaper"))
            dcmsnd.setAssociationReaperPeriod(
                    parseInt(cl.getOptionValue("reaper"),
                    "illegal argument of option -reaper",
                    1, Integer.MAX_VALUE));
        if (cl.hasOption("rspTO"))
            dcmsnd.setDimseRspTimeout(parseInt(cl.getOptionValue("rspTO"),
                    "illegal argument of option -rspTO",
                    1, Integer.MAX_VALUE));
        if (cl.hasOption("acceptTO"))
            dcmsnd.setAcceptTimeout(parseInt(cl.getOptionValue("acceptTO"),
                    "illegal argument of option -acceptTO",
                    1, Integer.MAX_VALUE));
        if (cl.hasOption("releaseTO"))
            dcmsnd.setReleaseTimeout(parseInt(cl.getOptionValue("releaseTO"),
                    "illegal argument of option -releaseTO",
                    1, Integer.MAX_VALUE));
        if (cl.hasOption("soclosedelay"))
            dcmsnd.setSocketCloseDelay(
                    parseInt(cl.getOptionValue("soclosedelay"),
                    "illegal argument of option -soclosedelay", 1, 10000));
        if (cl.hasOption("rcvpdulen"))
            dcmsnd.setMaxPDULengthReceive(
                    parseInt(cl.getOptionValue("rcvpdulen"),
                    "illegal argument of option -rcvpdulen", 1, 10000) * KB);
        if (cl.hasOption("sndpdulen"))
            dcmsnd.setMaxPDULengthSend(parseInt(cl.getOptionValue("sndpdulen"),
                    "illegal argument of option -sndpdulen", 1, 10000) * KB);
        if (cl.hasOption("sosndbuf"))
            dcmsnd.setSendBufferSize(parseInt(cl.getOptionValue("sosndbuf"),
                    "illegal argument of option -sosndbuf", 1, 10000) * KB);
        if (cl.hasOption("sorcvbuf"))
            dcmsnd.setReceiveBufferSize(parseInt(cl.getOptionValue("sorcvbuf"),
                    "illegal argument of option -sorcvbuf", 1, 10000) * KB);
        if (cl.hasOption("bufsize"))
            dcmsnd.setTranscoderBufferSize(
                    parseInt(cl.getOptionValue("bufsize"),
                    "illegal argument of option -bufsize", 1, 10000) * KB);
        dcmsnd.setPackPDV(!cl.hasOption("pdv1"));
        dcmsnd.setTcpNoDelay(!cl.hasOption("tcpdelay"));
        if (cl.hasOption("async"))
            dcmsnd.setMaxOpsInvoked(parseInt(cl.getOptionValue("async"),
                    "illegal argument of option -async", 0, 0xffff));
        if (cl.hasOption("lowprior"))
            dcmsnd.setPriority(CommandUtils.LOW);
        if (cl.hasOption("highprior"))
            dcmsnd.setPriority(CommandUtils.HIGH);
        log.info("Scanning files to send");
        long t1 = System.currentTimeMillis();
        for (int i = 1, n = argList.size(); i < n; ++i)
            dcmsnd.addFile(new File((String) argList.get(i)));
        long t2 = System.currentTimeMillis();
        if (dcmsnd.getNumberOfFilesToSend() == 0) {
            System.exit(2);
        }
        log.info("\nScanned " + dcmsnd.getNumberOfFilesToSend()
                + " files in " + ((t2 - t1) / 1000F) + "s (="
                + ((t2 - t1) / dcmsnd.getNumberOfFilesToSend()) + "ms/file)");
        dcmsnd.configureTransferCapability();
        t1 = System.currentTimeMillis();
        try {
            dcmsnd.open();
        } catch (Exception e) {
            log.error("ERROR: Failed to establish association:"
                    + e.getMessage());
            System.exit(2);
        }
        t2 = System.currentTimeMillis();
        log.info("Connected to " + remoteAE + " in "
                + ((t2 - t1) / 1000F) + "s");

        t1 = System.currentTimeMillis();
        dcmsnd.send();
        t2 = System.currentTimeMillis();
        prompt(dcmsnd, (t2 - t1) / 1000F);
        dcmsnd.close();
        log.info("Released connection to " + remoteAE);
    }
*/
    /*
    private static void prompt(CstoreScu dcmsnd, float seconds) {
        System.out.print("\nSent ");
        System.out.print(dcmsnd.getNumberOfFilesSent());
        System.out.print(" objects (=");
        promptBytes(dcmsnd.getTotalSizeSent());
        System.out.print(") in ");
        System.out.print(seconds);
        System.out.print("s (=");
        promptBytes(dcmsnd.getTotalSizeSent() / seconds);
        System.out.println("/s)");
    }

    private static void promptBytes(float totalSizeSent) {
        if (totalSizeSent > MB) {
            System.out.print(totalSizeSent / MB);
            System.out.print("MB");
        } else {
            System.out.print(totalSizeSent / KB);
            System.out.print("KB");
        }
    }
*/
    private static int toPort(String port) {
        return port != null ? parseInt(port, "illegal port number", 1, 0xffff)
                : 104;
    }

    private static String[] split(String s, char delim) {
        String[] s2 = { s, null };
        int pos = s.indexOf(delim);
        if (pos != -1) {
            s2[0] = s.substring(0, pos);
            s2[1] = s.substring(pos + 1);
        }
        return s2;
    }

    private static void exit(String msg) {
        log.error(msg);
        log.error("Try 'dcmsnd -h' for more information.");
        System.exit(1);
    }

    private static int parseInt(String s, String errPrompt, int min, int max) {
        try {
            int i = Integer.parseInt(s);
            if (i >= min && i <= max)
                return i;
        } catch (NumberFormatException e) {
        }
        exit(errPrompt);
        throw new RuntimeException();
    }

    public void setDicomOutputTransaction(DicomOutputTransaction dicomOutputTransaction) throws IOException {
        this.dicomOutputTransaction = dicomOutputTransaction;

    }


    public void addFile(File f) throws IOException {


        if (f.isDirectory()) {
            File[] fs = f.listFiles();
            for (int i = 0; i < fs.length; i++)
                addFile(fs[i]);
            return;
        }
        FileInfo info = new FileInfo(f);
        /*
        if (dicomOutputTransaction != null){
            dicomOutputTransaction.setObjectCount(dicomOutputTransaction.getObjectCount() +1);
            dicomOutputTransaction.setTotalBytes(dicomOutputTransaction.getTotalBytes() + f.length());
            Store s = DB.get();
            session.beginTransaction();
            session.save(dicomOutputTransaction);
            session.getTransaction().commit();
        }
        */
        DicomObject dcmObj = new BasicDicomObject();
        try {
            DicomInputStream in = new DicomInputStream(f);
            try {
                in.setHandler(new StopTagInputHandler(Tag.StudyDate));
                in.readDicomObject(dcmObj, PEEK_LEN);
                info.tsuid = in.getTransferSyntax().uid();
                info.fmiEndPos = in.getEndOfFileMetaInfoPosition();
            } finally {
                try {
                    in.close();
                } catch (IOException ignore) {
                }
            }
        } catch (IOException e) {

            log.debug("Failed to parse - Not a DICOM file " + f + " - skipped.");
            filesSkipped++;
            return; //throw e;

        }
        info.cuid = dcmObj.getString(Tag.SOPClassUID);
        if (info.cuid == null) {
            log.debug("WARNING: Missing SOP Class UID in " + f
                    + " - skipped.");
            filesSkipped++;
            return;


        }
        info.iuid = dcmObj.getString(Tag.SOPInstanceUID);
        if (info.iuid == null) {
            log.debug("Missing SOP Instance UID in " + f
                    + " - skipped.");
            filesSkipped++;
            return;


        }
        addTransferCapability(info.cuid, info.tsuid);
        files.add(info);

    }

    private void addTransferCapability(String cuid, String tsuid) {
        HashSet ts = (HashSet) as2ts.get(cuid);
        if (ts == null) {
            ts = new HashSet();
            ts.add(UID.ImplicitVRLittleEndian);
            as2ts.put(cuid, ts);
        }
        ts.add(tsuid);
    }

    public void configureTransferCapability() {
        Iterator iter = as2ts.entrySet().iterator();
        TransferCapability[] tc = new TransferCapability[as2ts.size()];
        for (int i = 0; i < tc.length; i++) {
            Map.Entry e = (Map.Entry) iter.next();
            String cuid = (String) e.getKey();
            HashSet ts = (HashSet) e.getValue();
            tc[i] = new TransferCapability(cuid,
                    (String[]) ts.toArray(new String[ts.size()]),
                    TransferCapability.SCU);
        }
        ae.setTransferCapability(tc);
    }

    public void open() throws IOException, ConfigurationException,
            InterruptedException {
        assoc = ae.connect(remoteAE, executor);
    }

    public void echo() throws IOException, InterruptedException
    {
        assoc.cecho().next();
    }

    public DicomOutputTransaction send() {
        boolean success = false;
        
        Store db = DB.get();
        
        dicomOutputTransaction.setStatus(ManagedTransaction.STATUS_ACTIVE);
        db.save(dicomOutputTransaction);

        if (assoc == null)
            throw new NullPointerException("Assoc is null");
        
        String lastError = "";
        try {
            
            for(FileInfo info : files) {
                
                success = false;
                
                StatusDisplayManager.setActiveIcon();
                db.save(dicomOutputTransaction);
                
                fillMetadata(info);
                
                if (info == null)
                    throw new NullPointerException("FileInfo is null");
                
                if (info.cuid == null)
                    throw new NullPointerException("FileInfo info.cuid is null");
                
                TransferCapability tc = assoc.getTransferCapabilityAsSCU(info.cuid);//npe
                if (tc == null) {
                    log.info(UIDDictionary.getDictionary().prompt(info.cuid) + " not supported by " 
                              + remoteAE.getAETitle());
                    log.info("skip file " + info.f);
                    continue;
                }
                
                String tsuid = selectTransferSyntax(tc.getTransferSyntax(),info.tsuid);
                if (tsuid == null) {
                    log.info(UIDDictionary.getDictionary().prompt(
                            info.cuid)
                            + " with "
                            + UIDDictionary.getDictionary().prompt(info.tsuid)
                            + " not supported by" + remoteAE.getAETitle());
                    log.info("skip file " + info.f);
                    continue;
                }
                
                if(cancel) 
                    throw new CancelException("Cancel invoked by user");
                
                try {
                    DimseRSPHandler rspHandler = new DimseRSPHandler() {
                        public void onDimseRSP(Association as, DicomObject cmd, DicomObject data) {
                            CstoreScu.this.onDimseRSP(as, cmd, data);
                        }
                    };
                    
                    assoc.cstore(info.cuid, info.iuid, priority, new DataWriter(info), tsuid, rspHandler);
                    
                    dicomOutputTransaction.setBytesTransferred(dicomOutputTransaction.getBytesTransferred() + info.length);
                    db.save(dicomOutputTransaction);
                    StatusDisplayManager.setIdleIcon();
                    success = true;
                }
                catch (NoPresentationContextException e) {
                    log.error("WARNING: " + e.getMessage()
                            + " - cannot send " + info.f, e);
                    lastError = e.getMessage();
                } 
                catch (IOException e) {
                    log.error("ERROR: Failed to send - " + info.f + ": " + e.getMessage(),e);
                    lastError = e.getMessage();
                }
                catch (InterruptedException e) {
                    // should not happen
                    log.error("Interrupted exception", e);
                }
            }
            
            try {
                assoc.waitForDimseRSP();
            }
            catch (InterruptedException e) {
                // should not happen
                log.error("Interrupted exception", e);
            }
        }
        catch(CancelException e) {
            log.info("CSTORE transfer cancelled by user");
            dicomOutputTransaction.setStatus(DicomOutputTransaction.STATUS_CANCELLED);
            dicomOutputTransaction.setStatusMessage("Cancelled by user");
            db.save(dicomOutputTransaction);
            return(dicomOutputTransaction);
        }
        finally {
             if(success) {
                 dicomOutputTransaction.setStatus(ManagedTransaction.STATUS_COMPLETE);
                 db.save(dicomOutputTransaction);
                 StatusDisplayManager.setIdleIcon();
                 //StatusDisplayManager.setImage(currentImage);
             }
             else {
                 if(dicomOutputTransaction.getStatus().equals(ManagedTransaction.STATUS_CANCELLED)) {
                     StatusDisplayManager.setIdleIcon();
                     StatusDisplayManager sdm = StatusDisplayManager.get();
                     if (sdm != null)
                         sdm.setMessage("DICOM transfer cancelled",
                             "DICOM transfer for patient " +
                             dicomOutputTransaction.getPatientName() +
                             ":" + dicomOutputTransaction.getStudyDescription() +
                             " has been cancelled.");
                 }
                 else {
                     dicomOutputTransaction.setStatus(ManagedTransaction.STATUS_TEMPORARY_ERROR);
                     if(blank(dicomOutputTransaction.getStatusMessage())) 
                         dicomOutputTransaction.setStatusMessage(lastError);
                     db.save(dicomOutputTransaction);
                     log.info("Error sending study; need to requeue off of the database");
                     StatusDisplayManager.setErrorIcon();
                 }
             }
         }
         return dicomOutputTransaction;
    }

    /**
     * Set patient name and study description in the output stransaction
     * if no name was set already.
     * 
     * @param info  DICOM file to extract meta data from
     */
    private void fillMetadata(FileInfo info) {
        if (blank(dicomOutputTransaction.getPatientName())) {
            try {
                ExtractFileMetadata extractFileMetadata = new ExtractFileMetadata(info.f);
                
                DicomMetadata dicomMetadata = extractFileMetadata.parse();
                dicomOutputTransaction.setStudyDescription(dicomMetadata.getStudyDescription());
                dicomOutputTransaction.setPatientName(dicomMetadata.getPatientName());
            }
            catch(Exception e){
                log.error("Error extracting DICOM metadata ", e); // Continue - not a fatal error
            }
        }
    }

    private String selectTransferSyntax(String[] available, String tsuid) {
        if (tsuid.equals(UID.ImplicitVRLittleEndian))
            return selectTransferSyntax(available, IVLE_TS);
        if (tsuid.equals(UID.ExplicitVRLittleEndian))
            return selectTransferSyntax(available, EVLE_TS);
        if (tsuid.equals(UID.ExplicitVRBigEndian))
            return selectTransferSyntax(available, EVBE_TS);
        return tsuid;
    }

    private String selectTransferSyntax(String[] available, String[] tsuids) {
        for (int i = 0; i < tsuids.length; i++)
            for (int j = 0; j < available.length; j++)
                if (available[j].equals(tsuids[i]))
                    return available[j];
        return null;
    }

    public void close() {
        try {
            assoc.release(false);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static final class FileInfo {
        File f;

        String cuid;

        String iuid;

        String tsuid;

        long fmiEndPos;

        long length;

        public FileInfo(File f) {
            this.f = f;
            this.length = f.length();
        }

    }

    private class DataWriter implements org.dcm4che2.net.DataWriter {

        private FileInfo info;

        public DataWriter(FileInfo info) {
            this.info = info;
        }

        public void writeTo(PDVOutputStream out, String tsuid)
                throws IOException {
            if (tsuid.equals(info.tsuid)) {
                FileInputStream fis = new FileInputStream(info.f);
                try {
                    long skip = info.fmiEndPos;
                    while (skip > 0)
                        skip -= fis.skip(skip);
                    out.copyFrom(fis);
                } finally {
                    fis.close();
                }
            } else {
                DicomInputStream dis = new DicomInputStream(info.f);
                try {
                    DicomOutputStream dos = new DicomOutputStream(out);
                    dos.setTransferSyntax(tsuid);
                    TranscoderInputHandler h = new TranscoderInputHandler(dos,
                            transcoderBufferSize);
                    dis.setHandler(h);
                    dis.readDicomObject();
                } finally {
                    dis.close();
                }
            }
        }

    }

    private void promptErrRSP(String prefix, int status, FileInfo info,
            DicomObject cmd) {
        log.error(prefix + StringUtils.shortToHex(status) + "H for "
                + info.f + ", cuid=" + info.cuid + ", tsuid=" + info.tsuid);
        log.error(cmd.toString());
    }

    private void onDimseRSP(Association as, DicomObject cmd, DicomObject data) {
        int status = cmd.getInt(Tag.Status);
        int msgId = cmd.getInt(Tag.MessageIDBeingRespondedTo);
        FileInfo info = (FileInfo) files.get(msgId - 1);
        switch (status) {
        case 0:
            totalSize += info.length;
            ++filesSent;
            //System.out.print('.');
            break;
        case 0xB000:
        case 0xB006:
        case 0xB007:
            totalSize += info.length;
            ++filesSent;
            promptErrRSP("WARNING: Received RSP with Status ", status, info,
                    cmd);
            //System.out.print('W');
            break;
        default:
            promptErrRSP("ERROR: Received RSP with Status ", status, info, cmd);
            //System.out.print('F');
        }
    }

}
