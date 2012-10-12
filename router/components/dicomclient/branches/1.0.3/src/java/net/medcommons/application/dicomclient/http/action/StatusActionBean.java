package net.medcommons.application.dicomclient.http.action;

import static net.medcommons.application.dicomclient.utils.Params.where;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.medcommons.application.dicomclient.ContextManager;
import net.medcommons.application.dicomclient.http.utils.ResponseWrapper;
import net.medcommons.application.dicomclient.transactions.CCRPatientReference;
import net.medcommons.application.dicomclient.transactions.CCRRef;
import net.medcommons.application.dicomclient.transactions.ContextState;
import net.medcommons.application.dicomclient.transactions.PatientMatch;
import net.medcommons.application.dicomclient.utils.CxpTransaction;
import net.medcommons.application.dicomclient.utils.DB;
import net.medcommons.application.dicomclient.utils.DDLTypes;
import net.medcommons.application.dicomclient.utils.DicomOutputTransaction;
import net.medcommons.application.dicomclient.utils.DicomTransaction;
import net.medcommons.application.dicomclient.utils.PixDemographicData;
import net.medcommons.application.dicomclient.utils.PixIdentifierData;
import net.medcommons.modules.services.interfaces.DicomMetadata;
import net.sourceforge.pbeans.Store;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.ajax.JavaScriptResolution;

import org.apache.log4j.Logger;

public class StatusActionBean extends DDLActionBean {
    
    private static Logger log = Logger.getLogger(StatusActionBean.class);
    
    public ContextManager getContextManager(){
            return(super.getContextManager());
    }
 
    @DefaultHandler
    public Resolution getCxpUploads(){
        ResponseWrapper response = new ResponseWrapper();

        Store db = DB.get();
        
        List<CxpTransaction> transactions = 
            db.all(CxpTransaction.class, where("transactionType", CxpTransaction.TRANSACTION_PUT));
        
        log.debug("About to return " + transactions.size() + " uploads");
        for (int i=0;i<transactions.size();i++){
            CxpTransaction trans = transactions.get(i);
            log.debug("transaction " + i + ":" + trans.toString());
        }
        
        response.setContents(transactions);
        response.setMessage("OK");
        response.setStatus(ResponseWrapper.Status.OK);
        
        return new JavaScriptResolution(response, ContextState.class, File.class);
    }

    public Resolution    getCxpDownloads(){
        ResponseWrapper response = new ResponseWrapper();
        Store db = DB.get();
        
        List<CxpTransaction> transactions = 
            db.select(CxpTransaction.class, "select * from cxp_transaction where transactiontype = ? and status <> ?",
                    new Object[] { CxpTransaction.TRANSACTION_GET, CxpTransaction.STATUS_ADD_DICOM }).all();
        
        //log.info("About to return " + transactions.size() + " downloads");
        response.setContents(transactions);
        response.setMessage("OK");
        response.setStatus(ResponseWrapper.Status.OK);
        return(new JavaScriptResolution(response, ContextState.class));

    }

    /**
     * Ephemeral DICOM input. Disappears as soon as transaction is completed.
     * @return
     */
    public Resolution    getDicomSCP(){
        ResponseWrapper response = new ResponseWrapper();
        Store db = DB.get();
       
        List<DicomTransaction> transactions = 
            db.select(DicomTransaction.class, 
                      "select * from dicom_transaction where status = ? or status = ?",
                      new Object[] { DicomTransaction.STATUS_ACTIVE, DicomTransaction.STATUS_COMPLETE})
              .all();
        
        response.setContents(transactions);
        response.setMessage("OK");
        response.setStatus(ResponseWrapper.Status.OK);
        return new JavaScriptResolution(response, ContextState.class);

    }
    /**
     * Queued output for DICOM being sent to a third party device.
     * @return
     */
    public Resolution    getDicomSCU(){
        ResponseWrapper response = new ResponseWrapper();
        Store db = DB.get();

        List<DicomTransaction> transactions =
            db.select(DicomOutputTransaction.class).all();
        
        response.setContents(transactions);
        response.setMessage("OK");
        response.setStatus(ResponseWrapper.Status.OK);
        return(new JavaScriptResolution(response, ContextState.class));
    }

    public Resolution getCCRReferences(){
        ResponseWrapper response = new ResponseWrapper();

        try {
        
            Store db = DB.get();
            List<CCRRef> transactions = db.select(CCRRef.class).all();
            log.debug("About to return " + transactions.size() + " ccr references");
            for (int i=0;i<transactions.size();i++){
                CCRRef trans = transactions.get(i);
                log.debug("ccr reference " + i + ":" + trans.toString());
            }
            List<CCRPatientReference> ccrReferences = new ArrayList<CCRPatientReference>();
            for (int i=0;i<transactions.size();i++){
                CCRRef ref = transactions.get(i);
                // There has to be a more efficent way to do this.
                PixDemographicData pixDemographicData = PatientMatch.getPatient(DDLTypes.MEDCOMMONS_AFFINITY_DOMAIN, ref.getStorageId());
                if (pixDemographicData != null){
                    PixIdentifierData pixIdentifierData = PatientMatch.getIdentifier(pixDemographicData.getId(), DDLTypes.MEDCOMMONS_AFFINITY_DOMAIN);
                    if (pixIdentifierData != null){
                        ccrReferences.add(new CCRPatientReference(ref, pixDemographicData, pixIdentifierData));
                    }
                }
            }
    
            response.setContents(ccrReferences);
            response.setMessage("OK");
            response.setStatus(ResponseWrapper.Status.OK);
        }
        catch(Exception e){
            log.error("error getting CCR references", e);
            response.setContents(e.getMessage());
            response.setMessage("ERROR");
            response.setStatus(ResponseWrapper.Status.ERROR);
        }
        finally {
            return new JavaScriptResolution(response, ContextState.class);
        }
    }
    
    public Resolution getAllCxpTransactions(){
        ResponseWrapper response = new ResponseWrapper();

        Store db = DB.get();

        List<CxpTransaction> transactions = db.select(CxpTransaction.class).all();
        
        log.debug("About to return " + transactions.size() + " transactions");
        for (int i=0;i<transactions.size();i++){
            CxpTransaction trans = transactions.get(i);
            log.debug("transaction " + i + ":" + trans.toString());
        }
        
        response.setContents(transactions);
        response.setMessage("OK");
        response.setStatus(ResponseWrapper.Status.OK);
        return(new JavaScriptResolution(response, ContextState.class));
    }
    public Resolution getAllDicomMetadata(){

        ResponseWrapper response = new ResponseWrapper();
        try{
            Store db = DB.get();
            List<DicomMetadata> metadata = db.select(DicomMetadata.class).all();
            List<DicomMetadata> savedMetadata = new ArrayList<DicomMetadata>();
            log.info("About to return " + metadata.size() + " DICOM metadata entries");

            for (int i=0;i<metadata.size();i++){
                DicomMetadata m = metadata.get(i);
                savedMetadata.add(m);
                log.info("metadata " + i + ":" + m.toString());
            }

            response.setContents(savedMetadata);
            response.setMessage("OK");
            response.setStatus(ResponseWrapper.Status.OK);
        }
        catch(Error e) {
            response.setStatus(ResponseWrapper.Status.ERROR);
            response.setMessage(e.getLocalizedMessage());
            response.setContents("ERROR");
        }
        return(new JavaScriptResolution(response, ContextState.class));
    }


    public Resolution status() {
        log.info("status()");
        return new ForwardResolution("status.html");
    }
}