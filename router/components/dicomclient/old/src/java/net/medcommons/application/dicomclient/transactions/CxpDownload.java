package net.medcommons.application.dicomclient.transactions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.medcommons.application.dicomclient.Configurations;
import net.medcommons.application.dicomclient.ContextManager;
import net.medcommons.application.dicomclient.CxpTransferMonitor;
import net.medcommons.application.dicomclient.DownloadHandler;
import net.medcommons.application.dicomclient.utils.Commands;
import net.medcommons.application.dicomclient.utils.CxpTransaction;
import net.medcommons.application.dicomclient.utils.DDLTypes;
import net.medcommons.application.dicomclient.utils.DicomNameParser;
import net.medcommons.application.dicomclient.utils.DicomOutputTransaction;
import net.medcommons.application.dicomclient.utils.DirectoryUtils;
import net.medcommons.application.dicomclient.utils.PixDemographicData;
import net.medcommons.application.dicomclient.utils.PixIdentifierData;
import net.medcommons.application.dicomclient.utils.StatusDisplayManager;
import net.medcommons.application.utils.DashboardMessageGenerator.MessageType;
import net.medcommons.client.utils.CCRDocumentUtils;
import net.medcommons.client.utils.CCRParseException;
import net.medcommons.modules.transfer.DownloadFileAgent;
import net.medcommons.modules.utils.Str;

import org.apache.log4j.Logger;
import org.cxp2.Document;
import org.cxp2.GetResponse;
import org.cxp2.RegistryParameters;

import astmOrgCCR.ActorType;
import astmOrgCCR.CodedDescriptionType;
import astmOrgCCR.ContinuityOfCareRecordDocument;
import astmOrgCCR.DateTimeType;
import astmOrgCCR.IDType;
import astmOrgCCR.PersonNameType;
import astmOrgCCR.ActorType.Person;
import astmOrgCCR.ActorType.Person.Name;


/**
 * Thread for managing the CXP transfer for a CCR.
 * The CCR is downloaded and placed in a queue.
 * See DownloadCxpJob for download of object references.
 *
 *
 * @author mesozoic
 *
 */
public class CxpDownload implements Runnable, Commands,DDLTypes  {
	  private static Logger log = Logger.getLogger(DownloadHandler.class
	            .getName());
   
    private boolean downloadReferences;
    private DownloadQueue downloadQueue;
    private Long id;
   


    public CxpDownload(DownloadQueue downloadQueue) throws MalformedURLException {
        this.downloadQueue = downloadQueue;
        Long contextStateId = downloadQueue.getContextStateId();
        ContextState contextState = TransactionUtils.getContextState(contextStateId);
        if (contextState == null){
            throw new NullPointerException("Missing context state from DownloadQueue " + downloadQueue);
        }
        if (downloadQueue.getAttachments().equals(DownloadQueue.ATTACHMENTS_ALL)){
        	 this.downloadReferences = true;
        }
        else{
        	 this.downloadReferences = false;
        }

        
        this.id = downloadQueue.getId();
       
        log.info("Starting CXPDownload:"  + contextState.getStorageId() + " " + contextState.getGuid() + " download references = " + downloadReferences);

    }


    /**
     * Retrieves document guid; if that guid is a CCR
     * then a second pass is made that reads all of the
     * references down as well.
     */
    public void run() {
        Configurations configurations = ContextManager.getContextManager().getConfigurations();
        CxpTransferMonitor monitor = null;
       //
        CxpTransaction cxpTransaction = new CxpTransaction();
        //String senderAccountId = configurations.getSenderAccountId();
        CCRDocumentUtils ccrUtils = new CCRDocumentUtils();
        DicomOutputTransaction outputTransaction = DownloadHandler.createOutputTransaction();
        CCRReference ccrReference = null;
        ContinuityOfCareRecordDocument currentCCR=null;
        File localCCRFile =  null;
        Long contextStateId = downloadQueue.getContextStateId();
        ContextState contextState = TransactionUtils.getContextState(contextStateId);
        if (contextState == null){
            throw new NullPointerException("ContextState in " + downloadQueue + " is null");
        }
        try {


            String guids[] = new String[1];
            String storageId = contextState.getStorageId();
            guids[0] = contextState.getGuid();
            log.info("storageId=" + storageId + ", guid=" + guids[0]);
            for (int i = 0; i < guids.length; i++) {
                log.info("guid[" + i + "]=" + guids[i]);
            }
            if (storageId == null)
                return;

            String displayName = "Downloading CCR";
            cxpTransaction.setTotalBytes(0);
            cxpTransaction.setTotalImages(0);
            /*if ((senderAccountId == null) || ("".equals(senderAccountId))){
            	senderAccountId = storageId;
            }
            */
            cxpTransaction.setContextStateId(downloadQueue.getContextStateId());
            cxpTransaction.setTimeStarted(System.currentTimeMillis());
            cxpTransaction.setTransactionType(CxpTransaction.TRANSACTION_GET);
            cxpTransaction.setDisplayName(displayName);
            cxpTransaction.setStatus(CxpTransaction.STATUS_QUEUED_CCRONLY);
            
            if (configurations.getExportDirectory() != null){
                cxpTransaction.setExportFolder(configurations.getExportDirectory().getAbsolutePath());
            }

            String url = cxpTransaction.makeViewUrl(
                    contextState.getGatewayRoot(), 
                    contextState.getStorageId(), contextState.getAccountId(), 
                    contextState.getGuid(),contextState.getAuth());
            cxpTransaction.setViewUrl(url);

            File transactionFolder = new File(ContextManager.getContextManager().getDownloadCache(),System.currentTimeMillis() + "");
            DirectoryUtils.makeDirectory(transactionFolder);

            cxpTransaction.setTransactionFolder(transactionFolder.getAbsolutePath());
            cxpTransaction = TransactionUtils.saveTransaction(cxpTransaction);
            ccrReference = CCRMatch.getCCRReference(storageId, guids[0]);

            if (ccrReference == null){
            	// Then download CCR; create CCRReference; Parse CCR
            	log.info("CCRReference does not exist; must be downloaded" + storageId + ", " + guids[0] + ", " + contextState.getAccountId());


	            DownloadFileAgent downloadFileAgent = new DownloadFileAgent(
	                    contextState.getCXPEndpoint(), storageId, contextState.getAccountId(),guids, transactionFolder);
	            log.info("CXP download to go to folder "
	                            + transactionFolder.getAbsolutePath());

	            monitor = new CxpTransferMonitor(downloadFileAgent,
	                    "Download ", cxpTransaction);
	            cxpTransaction = TransactionUtils.saveTransaction(cxpTransaction);
	            Thread t = new Thread(monitor);
	            t.start();
	            GetResponse resp = downloadFileAgent.download();

	            monitor.stopMonitor(false);

	            int status = resp.getStatus();
	            String reason = resp.getReason();
	            List<RegistryParameters> params = resp.getRegistryParameters();
	            log.info("Status =" + status + ", reason=" + reason);
	            if (statusMissing(status)) {
	                throw new IOException(reason);
	            } else if (!statusOK(status)) {
	                throw new RuntimeException(reason);
	            }

	            List<Document> documents = resp.getDocinfo();
	            Iterator<Document> iter = documents.iterator();

	            // Note problem with ccrReferences only the last one will be
	            // sent to the download block below.

	            while (iter.hasNext()) {
	                Document doc = iter.next();
	                log.info("Downloaded:" + doc.getContentType() + ":"
	                        + doc.getDocumentName());
	                localCCRFile = new File(doc.getDocumentName());
	                cxpTransaction.setCcrFilename(localCCRFile.getAbsolutePath());
	                currentCCR = CCRDocumentUtils.parseAndCheckSchemaValidation(localCCRFile);
	                break;
	            }
	            ccrReference = new CCRReference();
                ccrReference.setGuid(contextState.getGuid());
                ccrReference.setStorageId(storageId);
                ccrReference.setTimeEntered(new Date()); 
                ccrReference.setFileLocation(localCCRFile.getAbsolutePath());
                ccrReference.setContextStateId(contextState.getId());
                
                ccrReference = TransactionUtils.saveTransaction(ccrReference);
                
                log.info("New CCRReference created for " + ccrReference.getStorageId() + ", " + ccrReference.getGuid() +
                        ", " + ccrReference.getFileLocation());
            }
            else{
            	// Otherwise - just parse CCR
            	log.info("CCRReference already exists for " + ccrReference.getStorageId() + ", " + ccrReference.getGuid() +
                        ", " + ccrReference.getFileLocation());
            	// CCRReference is downloaded.
            	String fileLocation = ccrReference.getFileLocation();
            	localCCRFile = new File(fileLocation);
            	if (!localCCRFile.exists()){
            		// Something's inconsistent.
            		TransactionUtils.delete(ccrReference); // Clean this up so it won't happen if the user tries again.
            		throw new FileNotFoundException("Missing CCR file from local cache " + localCCRFile.getAbsolutePath());
            	}
            	currentCCR = CCRDocumentUtils.parseAndCheckSchemaValidation(localCCRFile);
            }

            
            cxpTransaction.setPatientName(ccrUtils.getPatientName(currentCCR));
            cxpTransaction.setDisplayName(ccrUtils.getPurpose(currentCCR));
            log.info("Parsed CCR document for " + ccrUtils.getPatientName(currentCCR));
            PixDemographicData patientData = PatientMatch.getPatient(MEDCOMMONS_AFFINITY_DOMAIN, storageId);
            if(patientData == null) {
            	log.info("Creating new PixDemographicData for " + ccrUtils.getPatientName(currentCCR));
            	patientData = createDemographicObject(currentCCR);
            }
            else {
            	log.info("Found existing PixDemographicData for " + ccrUtils.getPatientName(currentCCR));
            }
            patientData = TransactionUtils.saveTransaction(patientData);
           
            List<IDType> patientIds = ccrUtils.getPatientIds(currentCCR);
            Long pixDemographicDataId = patientData.getId();
            
            ccrReference.setPixDemographicDataId(pixDemographicDataId);
            for (int i=0;i<patientIds.size();i++){
            	IDType anId = patientIds.get(i);
            	String affinityDomain = anId.getType().getText();
            	log.info("Affinity domain is " + affinityDomain);
            	if((affinityDomain == null) || ("".equals(affinityDomain))) {
            		affinityDomain = "UNKNOWN"; // TODO: Is this sensible? 
            	}
            	String affinityIdentifier = anId.getID();

            	PixIdentifierData pixIdentifier = PatientMatch.getIdentifier(affinityDomain, affinityIdentifier);
            	if (pixIdentifier == null) {
            		log.info("Creating new PixIdentifierData for " + ccrUtils.getPatientName(currentCCR) + ","
            				+ affinityDomain + "," + affinityIdentifier);

            		pixIdentifier = createIdentifierObject(pixDemographicDataId, affinityDomain, affinityIdentifier);
            		if(MEDCOMMONS_AFFINITY_DOMAIN.equals(affinityDomain)) {
            			pixIdentifier.setContextStateId(contextState.getId());
            		}
            		TransactionUtils.saveTransaction(pixIdentifier);
            	}
            	else {
            	    // Problem here:  if a 3rd party pix id exists for the patient but we 
            	    // don't have an mcid for them then this fails and explodes because we auto create
            	    // a patient with new pix id above if there is already a pix id.
            		Long matchedPixDemographicDataId = pixIdentifier.getPixDemographicDataId();
            		if (!matchedPixDemographicDataId.equals(pixDemographicDataId)){
            			throw new IllegalStateException("Mismatch in patient demographics identifiers - expected " +
            					pixDemographicDataId + " but identifier table points to id " + matchedPixDemographicDataId);
            		}
            	}
            }
            TransactionUtils.saveTransaction(ccrReference);

            log.info("Set size of download to " + cxpTransaction.getTotalBytes());
            //
            // Needs different layers of queuing.
            // A) The request for the CCR - just get it. They are small.
            // B) Put the CCR into a queue. It's a FIFO.
            // C) Put the DICOM output into a queue. It's a FIFO.
            // Queues B and C can be restarted. Quitting the DDL cancels them but they can
            // be restarted with a 'Retry' command.
            //              Get next queued transaction.
            // Do it with a query - check for any active jobs. If yes - then sleep.
            // If no - get first one.
            // If none - go back to sleep.
            // Move above coments into downloadHandler
            cxpTransaction = TransactionUtils.saveTransaction(cxpTransaction);
            outputTransaction.setCxpJob(cxpTransaction.getId());
            outputTransaction.setStudyDescription(cxpTransaction.getDisplayName());
            outputTransaction = TransactionUtils.saveTransaction(outputTransaction);

            log.info("Just saved outputTransaction:" + outputTransaction.toString());
            if (downloadReferences) {
            	log.info("Download DICOM case completed");
            	
            	DicomNameParser parser = new DicomNameParser();
            	
            	String patientName = Str.nvl(parser.givenName(cxpTransaction.getPatientName()),"")
	            	+ " " 
	            	+ Str.nvl(parser.familyName(cxpTransaction.getPatientName()),"");
            	
                StatusDisplayManager sdm = StatusDisplayManager.getStatusDisplayManager();
                sdm.setMessage(
                        "Download Transaction",
                        "Request received to download CCR with embedded DICOM for\n" + patientName
                        );
                
                cxpTransaction.setStatus(CxpTransaction.STATUS_QUEUED);
                cxpTransaction = TransactionUtils.saveTransaction(cxpTransaction);
                // sdm.sendDashboardMessage(MessageType.INFO, null, "Downloading Patient " + patientName);
                
                TransactionUtils.delete(ccrReference);
                log.info("Deleting CCR reference for download" + ccrReference.getStorageId() + ", " + ccrReference.getGuid());
                File transFolder = new File(cxpTransaction.getTransactionFolder());
            }
            else{
                // Leave the CCRRefence present for matching.
            	//
                log.info("Add DICOM case completed");

                // Need to test here to make sure there is only one 'STATUS_ADD_DICOM".
                String message = "";
                List<CCRReference> pendingTransactions = TransactionUtils.getCCRReferences();

                if ((pendingTransactions != null) && (pendingTransactions.size() > 0)){
                	for (int i=0;i<pendingTransactions.size();i++){
                		CCRReference aPendingTransaction = pendingTransactions.get(i);
                		if (!aPendingTransaction.getStorageId().equals(storageId)){
	                		log.info("Deleted exising Add DICOM for storage id" + aPendingTransaction.getStorageId());
	                		TransactionUtils.delete(aPendingTransaction);
	                		message += "Deleted previous \"Add DICOM\" requests\n";
                		}

                	}


                }
                //ccrReference = TransactionUtils.saveTransaction(ccrReference);
                if (cxpTransaction.getPatientName() == null){
                	message += " to \"Add DICOM \" to blank patient account";
                }
                else{
                	message += " to \"Add DICOM \" to account of " +cxpTransaction.getPatientName();
                }
                

                StatusDisplayManager.getStatusDisplayManager().setMessage(
                        "Add DICOM",
                        message,
                        cxpTransaction.getDashboardStatusId()
                        );
                // Delete the downloaded cxpTransaction entry
                TransactionUtils.delete(cxpTransaction);
            }
        }
        catch (CCRParseException e){
        	log.error("Exception downloading: storageId=" + contextState.getStorageId()
                    + ", guid=" + contextState.getGuid(), e);
        	StatusDisplayManager.setErrorIcon();
            StatusDisplayManager.getStatusDisplayManager().setErrorMessage(
                    "Download error",
                    e.getLocalizedMessage()
                            + "\nInvalid data in download",
                            cxpTransaction.getDashboardStatusId());
            if (!cxpTransaction.getStatus().equals(CxpTransaction.STATUS_COMPLETE)){
                cxpTransaction.setStatus(CxpTransaction.STATUS_TEMPORARY_ERROR);
                cxpTransaction.setStatusMessage(e.toString());
                cxpTransaction = TransactionUtils.saveTransaction(cxpTransaction);
            }
        }
        catch (Exception e) {
            log.error("Exception downloading: storageId=" + contextState.getStorageId()
                    + ", guid=" + contextState.getGuid(), e);
            StatusDisplayManager.setErrorIcon();
            StatusDisplayManager.getStatusDisplayManager().setErrorMessage(
                    "DICOM Transfer Error",
                    e.getLocalizedMessage()
                            + "\nFiles not sent to DICOM CSTORE target",
                            cxpTransaction.getDashboardStatusId());
            if (!cxpTransaction.getStatus().equals(CxpTransaction.STATUS_COMPLETE)){
                cxpTransaction.setStatus(CxpTransaction.STATUS_TEMPORARY_ERROR);
                cxpTransaction.setStatusMessage(e.toString());
                cxpTransaction = TransactionUtils.saveTransaction(cxpTransaction);
            }


        } finally {
            if (monitor != null)
                monitor.stopMonitor(false);
            DownloadHandler.Factory().removeCurrentJob(this);
            log.info("Completed thread for storageId = " + contextState.getStorageId()
                    + ", guid=" + contextState.getGuid());
        }

    }

    public boolean statusOK(int status) {
        boolean OK = false;
        if ((status >= 200) && (status <= 299))
            OK = true;
        return (OK);
    }

    public boolean statusMissing(int status) {
        boolean OK = false;
        if (status == 404)
            OK = true;
        return (OK);
    }


    protected PixIdentifierData createIdentifierObject(Long pixDemographicDataId, String affinityDomain, String affinityIdentifier){
    	PixIdentifierData pixIdentifierData = new PixIdentifierData();
    	pixIdentifierData.setAffinityDomain(affinityDomain.toUpperCase());
    	pixIdentifierData.setAffinityIdentifier(affinityIdentifier.toUpperCase());
    	pixIdentifierData.setPixDemographicDataId(pixDemographicDataId);
    	pixIdentifierData.setCreationDate(new Date());
    	return(pixIdentifierData);
    }
    /**
     * Returns a PixDemographicData object containing a subset of the data from
     * a CCR. The use of this data is for matching; no fields are included that
     * could not be used for this purpose.
     *
     * @param ccr
     * @return
     */
    protected PixDemographicData createDemographicObject(ContinuityOfCareRecordDocument ccr){
    	String family =null;
        String given = null;
        String middle =null;
    	PixDemographicData pixDemographicData = new PixDemographicData();
    	CCRDocumentUtils ccrUtils = new CCRDocumentUtils();
    	String patientActorID = ccrUtils.getPatientActorId(ccr);
    	ActorType patientActor = ccrUtils.getActorObject(ccr, patientActorID);
    	Person patient = patientActor.getPerson();
    	Name name = patient.getName();
    	if (name != null){
	        PersonNameType personName = name.getCurrentName();
	        family = ccrUtils.getStringValue(personName.getFamilyList());
	        given = ccrUtils.getStringValue(personName.getGivenList());
	        middle = ccrUtils.getStringValue(personName.getMiddleList());
    	}

        CodedDescriptionType patientGender = patient.getGender();
        String gender = null;
        if (patientGender != null){
        	gender = patientGender.getText();
        }
        if (family != null){
        	family = family.toUpperCase();
        }
        if (middle != null){
        	middle = middle.toUpperCase();
        }
        if (given != null){
        	given = given.toUpperCase();
        }
        pixDemographicData.setGender(gender);
    	pixDemographicData.setGivenName(given);
    	pixDemographicData.setMiddleName(middle);
    	pixDemographicData.setFamilyName(family);

    	DateTimeType patientDob = patient.getDateOfBirth();
    	String dob = null;
    	if (patientDob != null){
    		dob = patientDob.getExactDateTime();
	    	if (dob== null){
	    		CodedDescriptionType approxDob = patientDob.getApproximateDateTime();
	    		if (approxDob!=null){
	    			dob = approxDob.getText();
	    		}
	    	}
    	}
    	pixDemographicData.setDob(dob);

    	return(pixDemographicData);

    }

}