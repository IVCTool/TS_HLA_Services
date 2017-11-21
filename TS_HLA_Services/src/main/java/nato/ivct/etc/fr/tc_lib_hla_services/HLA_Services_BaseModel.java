/*
Copyright 2017, FRANCE (DGA/Capgemini)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package nato.ivct.etc.fr.tc_lib_hla_services;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import de.fraunhofer.iosb.tc_lib.IVCT_BaseModel;
import de.fraunhofer.iosb.tc_lib.IVCT_RTIambassador;

import hla.rti1516e.AttributeHandle;
import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.CallbackModel;
import hla.rti1516e.FederateAmbassador;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.LogicalTime;
import hla.rti1516e.MessageRetractionHandle;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.OrderType;
import hla.rti1516e.ParameterHandle;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.TransportationTypeHandle;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAboolean;
import hla.rti1516e.encoding.HLAunicodeString;
import hla.rti1516e.exceptions.AlreadyConnected;
import hla.rti1516e.exceptions.AttributeNotDefined;
import hla.rti1516e.exceptions.CallNotAllowedFromWithinCallback;
import hla.rti1516e.exceptions.ConnectionFailed;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.FederateServiceInvocationsAreBeingReportedViaMOM;
import hla.rti1516e.exceptions.InteractionClassNotDefined;
import hla.rti1516e.exceptions.InteractionClassNotPublished;
import hla.rti1516e.exceptions.InteractionParameterNotDefined;
import hla.rti1516e.exceptions.InvalidInteractionClassHandle;
import hla.rti1516e.exceptions.InvalidLocalSettingsDesignator;
import hla.rti1516e.exceptions.InvalidObjectClassHandle;
import hla.rti1516e.exceptions.NameNotFound;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.ObjectClassNotDefined;
import hla.rti1516e.exceptions.RTIinternalError;
import hla.rti1516e.exceptions.RestoreInProgress;
import hla.rti1516e.exceptions.SaveInProgress;
import hla.rti1516e.exceptions.UnsupportedCallbackModel;
import nato.ivct.etc.fr.fctt_common.configuration.controller.validation.FCTTFilesCheck;
import nato.ivct.etc.fr.fctt_common.resultServices.model.ServiceHLA;
import nato.ivct.etc.fr.fctt_common.resultServices.model.ResultServicesModel;
import nato.ivct.etc.fr.fctt_common.utils.FCTT_Constant;
import nato.ivct.etc.fr.fctt_common.utils.FCTT_Enum.eBuildResults;
import nato.ivct.etc.fr.fctt_common.utils.FCTT_Environment;
import nato.ivct.etc.fr.fctt_common.utils.StringWrapper;
import nato.ivct.etc.fr.fctt_common.utils.TextInternationalization;

import org.slf4j.Logger;

/**
 * @author FRANCE (DGA/Capgemini)
 */
public class HLA_Services_BaseModel extends IVCT_BaseModel {

    private Logger                  logger;
    private HLA_Services_TcParam	tcParams;
    private FCTTFilesCheck 			filesLoader;
    private File					certifiedServicesResultFile;
    private File					nonCertifiedServicesResultFile;
    
	// RTI
    private IVCT_RTIambassador      ivct_rti;
    private EncoderFactory          _encoderFactory;
    
    // SUT management
	private byte[] 					sutHandle = null;

	// SUT data model
	private ServiceHLA 				HlaServicesModel;
	private ResultServicesModel		HlaResultServicesModel;

    private String					sutName;
    private AttributeHandle         federateNameId;
    private AttributeHandle         federateHandleId;

    private AttributeHandle         federationRTIVersionId;
    
	// Interaction management
    private ParameterHandle 		serviceId;
    private ParameterHandle 		successIndicatorId;
    
	/**
     * @param logger reference to a logger
     * @param ivct_rti reference to the RTI ambassador
     * @param HlaServicesTcParam linked parameters
     */
    public HLA_Services_BaseModel(final Logger logger, final IVCT_RTIambassador ivct_rti, final HLA_Services_TcParam HlaServicesTcParam) {
    	
        super(ivct_rti, logger, HlaServicesTcParam);
        this.ivct_rti = ivct_rti;
        this._encoderFactory = ivct_rti.getEncoderFactory();
        this.logger = logger;
        this.tcParams = HlaServicesTcParam;
		this.filesLoader = new FCTTFilesCheck(logger, HlaServicesTcParam.getResultDir(), HlaServicesTcParam.getSutName());

		// Data models
        this.HlaServicesModel = null;
        this.HlaResultServicesModel = null;

    	// Generate result files
		String certifiedServicesFileName = "HLA_Services_certified_services_" + FCTT_Environment.getDateForFileName() + FCTT_Constant.REPORT_FILE_NAME_EX;
		certifiedServicesResultFile = new File(HlaServicesTcParam.getResultDir() + File.separator + certifiedServicesFileName);        
		String nonCertifiedServicesFileName = "HLA_Services_non_certified_services_" + FCTT_Environment.getDateForFileName() + FCTT_Constant.REPORT_FILE_NAME_EX;
		nonCertifiedServicesResultFile = new File(HlaServicesTcParam.getResultDir() + File.separator + nonCertifiedServicesFileName);        
    }

    
	/**
	 * Load the FOM and SOM files.
	 * @return True if the FOM and SOM files are valid, false if not
	 */
	public boolean loadFomSomFiles() {
		
		// Check files
		boolean filesLoaded = filesLoader.checkFiles(tcParams.getFomFiles(),tcParams.getSomFiles(),null);

		if (filesLoaded)
		{
			// Get HLA data model
			HlaServicesModel = filesLoader.getServiceHLA();			
			// Build result data model from HLA data model
			HlaResultServicesModel = new ResultServicesModel();
			HlaResultServicesModel.setDataModel(HlaServicesModel);
		}
		return filesLoaded;
	}

	
    /**
     * @param sutName system under test name
     * @return true means error, false means correct
     */
    public boolean init(final String sutName) {
    	
    	// SuT name
    	this.sutName = sutName;
    	
        // Federation & federate ids
    	ObjectClassHandle	federateId;
    	ObjectClassHandle	federationId;
        try {
            federateId = ivct_rti.getObjectClassHandle("HLAmanager.HLAfederate");

            federateNameId = ivct_rti.getAttributeHandle(federateId, "HLAfederateName");
            federateHandleId = ivct_rti.getAttributeHandle(federateId, "HLAfederateHandle");

            federationId = ivct_rti.getObjectClassHandle("HLAmanager.HLAfederation");
            federationRTIVersionId = ivct_rti.getAttributeHandle(federationId, "HLARTIversion");
        }
        catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError | InvalidObjectClassHandle ex) {
            logger.error("Cannot get object class handle");
            return true;
        }
        
        // Attributes
        AttributeHandleSet	federateSet;
        AttributeHandleSet	federationSet;
        try {
        	federateSet = ivct_rti.getAttributeHandleSetFactory().create();
        	federateSet.add(federateNameId);
        	federateSet.add(federateHandleId);

        	federationSet = ivct_rti.getAttributeHandleSetFactory().create();
        	federationSet.add(federationRTIVersionId);
        }
        catch (FederateNotExecutionMember | NotConnected ex) {
            logger.error("Cannot build attribute set");
            return true;
        }
        
        // Subscribe
        try {	
            ivct_rti.subscribeObjectClassAttributes(federateId,federateSet);
            ivct_rti.subscribeObjectClassAttributes(federationId,federationSet);
        }
        catch (AttributeNotDefined | ObjectClassNotDefined | SaveInProgress | RestoreInProgress | FederateNotExecutionMember | NotConnected | RTIinternalError ex) {
        	logger.error("Cannot subscribe attributes");
        	return true;
        }
        
        // Interactions
    	InteractionClassHandle reportServiceInvocationId;
    	try {
    		reportServiceInvocationId = ivct_rti.getInteractionClassHandle("HLAmanager.HLAfederate.HLAreport.HLAreportServiceInvocation");

            serviceId           = ivct_rti.getParameterHandle(reportServiceInvocationId, "HLAservice");
            successIndicatorId  = ivct_rti.getParameterHandle(reportServiceInvocationId, "HLAsuccessIndicator");
        }
    	catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError | InvalidInteractionClassHandle e) {
	    	logger.error("Cannot subscribe attributes");
	    	return true;
		}

        // All ok
        return false;
    }

    
    /**
     * @return true means error, false means correct
     */
	private boolean needToFollowFederate(final byte[] federateHandle) {

		InteractionClassHandle	reportClassId;
		InteractionClassHandle	reportingServiceId;

		try {
			reportClassId = ivct_rti.getInteractionClassHandle("HLAmanager.HLAfederate.HLAreport.HLAreportServiceInvocation");
			reportingServiceId = ivct_rti.getInteractionClassHandle("HLAmanager.HLAfederate.HLAadjust.HLAsetServiceReporting");
		}
		catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError ex) {
        	logger.error("RTI internal error");
            return true;
		}
		
		try {
			// Subscribe report interaction
            this.ivct_rti.subscribeInteractionClass(reportClassId);
			// Send service reporting interaction
            this.ivct_rti.publishInteractionClass(reportingServiceId);
	        // Parameters
            ParameterHandle federateParameterId;
            ParameterHandle stateParameterId;
            ParameterHandleValueMap	reportingParameters;
            try {
            	federateParameterId = ivct_rti.getParameterHandle(reportingServiceId, "HLAfederate");	// type HLAhandle
            	stateParameterId = ivct_rti.getParameterHandle(reportingServiceId, "HLAreportingState");
            	reportingParameters = ivct_rti.getParameterHandleValueMapFactory().create(2);
            }
            catch (FederateNotExecutionMember | NotConnected | NameNotFound | InvalidInteractionClassHandle ex) {
            	logger.error("Cannot set interaction parameters");
            	return true;
            }
            // Encode values
            final HLAboolean stateBoolean = ivct_rti.getEncoderFactory().createHLAboolean(true);
            reportingParameters.put(federateParameterId, federateHandle);
            reportingParameters.put(stateParameterId, stateBoolean.toByteArray());

            // Send the interaction
            try {
                ivct_rti.sendInteraction(reportingServiceId, reportingParameters, null);
            }
            catch (InteractionClassNotPublished | InteractionParameterNotDefined | InteractionClassNotDefined | SaveInProgress | RestoreInProgress | FederateNotExecutionMember | NotConnected | RTIinternalError ex) {
            	logger.error("Cannot send interaction");
            	return true;
            }
		}
		catch (FederateNotExecutionMember | NotConnected | RTIinternalError | FederateServiceInvocationsAreBeingReportedViaMOM | InteractionClassNotDefined | SaveInProgress | RestoreInProgress ex) {
            logger.error("Cannot get subscribe interaction class");
            return true;
		}
		// All ok
		return false;
	}
	
	
	/**
	 * Check the services declarations
	 * @return True if the declarations are validated, false if not
	 */
	public boolean validateServices() {
		
		String lCurrentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH'h'mm'm'ss's'"));
		
		FileWriter certifiedServicesResult = null;
		FileWriter nonCertifiedServicesResult = null;
		
		try {
			// Open result files
			certifiedServicesResult = new FileWriter(certifiedServicesResultFile);
			nonCertifiedServicesResult = new FileWriter(nonCertifiedServicesResultFile);
			
			// Format output
	    	int lMaxLengthService = HlaResultServicesModel.computeMaxServiceNameLength();
	    	StringWrapper fileWriter = new StringWrapper("");
	    	String formatter = "%-"+lMaxLengthService+"s %-5s %-45s%n";
	    	String headerFormatter = "%-"+lMaxLengthService+"s %-5s %-45s%n";
	    	
	    	// Write result files & logs
	    	String result;
	    	result = saveResultsWriteHeader(lCurrentDate, eBuildResults.ServicesCertificated);
	    	certifiedServicesResult.write(result);
	    	logger.info(result);
	    	result = String.format(headerFormatter, "", TextInternationalization.getString("resultsFile.headerColumns.services"), "");
	    	certifiedServicesResult.write(result);
			logger.info(result);
	    	result = HlaResultServicesModel.writeResults(eBuildResults.ServicesCertificated, fileWriter, formatter).getString();
	    	certifiedServicesResult.write(result);
	    	logger.info(result);
	    	
	    	result = saveResultsWriteHeader(lCurrentDate, eBuildResults.ServicesNotCertificated);
	    	nonCertifiedServicesResult.write(result);
	    	logger.info(result);
	    	result = String.format(headerFormatter, "", TextInternationalization.getString("resultsFile.headerColumns.services"), "");
	    	nonCertifiedServicesResult.write(result);
			logger.info(result);
	    	result = HlaResultServicesModel.writeResults(eBuildResults.ServicesNotCertificated, fileWriter, formatter).getString();
	    	nonCertifiedServicesResult.write(result);
	    	logger.info(result);
		}
		catch (Exception e) {
			return false;
		}
		
		finally
		{
			try 
			{
				certifiedServicesResult.close();
				nonCertifiedServicesResult.close();
			} 
			catch (IOException pIOException) 
			{
				logger.error(TextInternationalization.getString("close.error.reportFile") + ": " + pIOException.toString());
				return false;
			}
		}
		return HlaResultServicesModel.getValidated();
	}


    /**
     * Return the header for the results files
     * @param pCurrentDate Current date to write in the header
     * @param pBuildAction Adapts the header according to the type of result concerned
     * @return String which contains the header
     * @throws IOException
     */
    private String saveResultsWriteHeader(String pCurrentDate, eBuildResults pBuildAction) throws IOException
    {
    	String lHeader = "";
    	String lExplanationContent = "";
    	
    	if (pBuildAction == eBuildResults.ServicesCertificated)
    	{
    		lExplanationContent = TextInternationalization.getString("resultsFile.header.services.certificated");
    	}
    	
    	if (pBuildAction == eBuildResults.ServicesNotCertificated)
    	{
    		lExplanationContent = TextInternationalization.getString("resultsFile.header.services.notCertificated");
    	}
    	
    	lHeader = lHeader+"###########################################################\r\n";
    	lHeader = lHeader+TextInternationalization.getString("resultsFile.header")+" \""+tcParams.getSutName()+"\"\r\n";
    	lHeader = lHeader+"Date : "+pCurrentDate;
    	lHeader = lHeader+"\r\n";
    	lHeader = lHeader+"\r\n";
    	lHeader = lHeader+lExplanationContent;
    	lHeader = lHeader+"\r\n";
    	lHeader = lHeader+"\r\n";
    	lHeader = lHeader+TextInternationalization.getString("resultsFile.header.explanations");
    	lHeader = lHeader+"\r\n";
    	lHeader = lHeader+"###########################################################\r\n";
    	lHeader = lHeader+"\r\n";
    	
    	return lHeader;
    }

    
    /**
     * {@inheritDoc}
     */
    public void connect(final FederateAmbassador federateReference, final CallbackModel callbackModel, final String localSettingsDesignator) {
        try {
        	if (ivct_rti != null)
        		ivct_rti.connect(federateReference, callbackModel, localSettingsDesignator);
        }
        catch (ConnectionFailed | InvalidLocalSettingsDesignator | UnsupportedCallbackModel | AlreadyConnected | CallNotAllowedFromWithinCallback | RTIinternalError ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
    }


    /**
     * @param logger reference to a logger
     * @param sleepTime time to sleep
     * @return true means problem, false is ok
     */
    public boolean sleepFor(final Logger logger,final long sleepTime) {
    	// Wait
    	logger.info(TextInternationalization.getString("etc_fra.sleepFor"));

        try {
            Thread.sleep(sleepTime * 1000);
        }
        catch (final InterruptedException ex) {
            return true;
        }

        return false;
    }


    // 5.12
    @Override
    public void turnInteractionsOn(final InteractionClassHandle theHandle) throws FederateInternalError {
    }


    // 5.13
    @Override
    public void turnInteractionsOff(final InteractionClassHandle theHandle) throws FederateInternalError {
    }

	/**
     * @param interactionClass specify the interaction class
     * @param theParameters specify the parameter handles and values
     */
    private void doReceiveInteraction(final InteractionClassHandle interactionClass, final ParameterHandleValueMap theParameters) {

//    	logger.debug(String.format("Interaction : %s",interactionClass.toString()));
//    	logger.debug(String.format("Parameters : %s",theParameters.toString()));
    	
    	try {
			String interactionClassName = ivct_rti.getInteractionClassName(interactionClass);
			
			if (interactionClassName.endsWith("HLAreportServiceInvocation"))
			{
		        // Update data model
	    		try {
	    			// Get success indicator
	    			final HLAboolean successDecoder = _encoderFactory.createHLAboolean();
	    			successDecoder.decode(theParameters.get(successIndicatorId));
					boolean successIndicator = successDecoder.getValue();
					if (successIndicator)
					{
						// Get service name
		    			final HLAunicodeString serviceDecoder = _encoderFactory.createHLAunicodeString();
		    			serviceDecoder.decode(theParameters.get(serviceId));
		    			String serviceName = serviceDecoder.getValue();
//						logger.debug("serviceName: " + serviceName);
		    			// Update services
		    			HlaResultServicesModel.updateState(serviceName);
					}
				}
	    		catch (DecoderException e) {
	                logger.error("Failed to decode incoming attribute");
	                return;
	 			}
			}
		}
    	catch (InvalidInteractionClassHandle | FederateNotExecutionMember	| NotConnected | RTIinternalError e) {
            logger.error("Failed to decode incoming attribute");
            return;
		}
    }

    // 6.13
    /**
     * {@inheritDoc}
     */
    @Override
    public void receiveInteraction(final InteractionClassHandle interactionClass, final ParameterHandleValueMap theParameters, final byte[] userSuppliedTag, final OrderType sentOrdering, final TransportationTypeHandle theTransport, final SupplementalReceiveInfo receiveInfo) throws FederateInternalError {
        this.doReceiveInteraction(interactionClass, theParameters);
    }


    // 6.13
    /**
     * {@inheritDoc}
     */
    @Override
    public void receiveInteraction(final InteractionClassHandle interactionClass, final ParameterHandleValueMap theParameters, final byte[] userSuppliedTag, final OrderType sentOrdering, final TransportationTypeHandle theTransport, final LogicalTime theTime, final OrderType receivedOrdering, final SupplementalReceiveInfo receiveInfo) throws FederateInternalError {
        this.doReceiveInteraction(interactionClass, theParameters);
    }


    // 6.13
    /**
     * {@inheritDoc}
     */
    @Override
    public void receiveInteraction(final InteractionClassHandle interactionClass, final ParameterHandleValueMap theParameters, final byte[] userSuppliedTag, final OrderType sentOrdering, final TransportationTypeHandle theTransport, final LogicalTime theTime, final OrderType receivedOrdering, final MessageRetractionHandle retractionHandle, final SupplementalReceiveInfo receiveInfo) throws FederateInternalError {
        this.doReceiveInteraction(interactionClass, theParameters);
    }


    // 6.9
    /**
     * {@inheritDoc}
     */
    @Override
    public void discoverObjectInstance(final ObjectInstanceHandle theObject, final ObjectClassHandle theObjectClass, final String objectName) throws FederateInternalError {
    }


    // 6.15
    /**
     * {@inheritDoc}
     */
    @Override
    public void removeObjectInstance(final ObjectInstanceHandle theObject, final byte[] userSuppliedTag, final OrderType sentOrdering, final FederateAmbassador.SupplementalRemoveInfo removeInfo) {

//    	logger.debug("removeObjectInstance");
//    	logger.debug(String.format("Object : %s",theObject.toString()));
    	
        // SuT
//    	logger.debug("**** Comparaison " + sutHandle + " " + theObject.toString());
    	if (theObject.toString().equals(sutHandle.toString()))
		{
            // Force resign, destroy & disconnect services validation
            HlaResultServicesModel.updateState("resignFederationExecution");
            HlaResultServicesModel.updateState("destroyFederationExecution");
            HlaResultServicesModel.updateState("disconnect");
		}
    }

    
    /**
     * @param theObject the object instance handle
     * @param theAttributes the map of attribute handle / value
     */
    public void doReflectAttributeValues(final ObjectInstanceHandle theObject, final AttributeHandleValueMap theAttributes) {
    	
    	// SuT
		String federateName = null;
		byte[] federateHandle = null;
		
		if (theAttributes.containsKey(federateNameId)) {
    		try {
    			final HLAunicodeString stringDecoder = _encoderFactory.createHLAunicodeString();
				stringDecoder.decode(theAttributes.get(federateNameId));
				federateName = stringDecoder.getValue();
				
			} catch (DecoderException e) {
                logger.error("Failed to decode incoming attribute");
                return;
 			}
    	}
    	if (theAttributes.containsKey(federateHandleId)) {
			federateHandle = theAttributes.get(federateHandleId); 
    	}

// 2017/11/15 ETC FRA V1.3, Capgemini, to avoid several detections of federate to follow
//    	if ((federateName.equals(sutName)) && (federateHandle != null)) {
    	if ((sutHandle == null) && (federateName.equals(sutName)) && (federateHandle != null)) {
			if (needToFollowFederate(federateHandle) == false) {
	            logger.info("following federate " + sutName);
	            sutHandle = federateHandle;
	            // Force connect, create & join services validation
	            HlaResultServicesModel.updateState("connect");
	            HlaResultServicesModel.updateState("createFederationExecution");
	            HlaResultServicesModel.updateState("joinFederationExecution");
	            // To be moved in removeObjectInstance callback of the SuT
	            // Force resign, destroy & disconnect services validation
	            HlaResultServicesModel.updateState("resignFederationExecution");
	            HlaResultServicesModel.updateState("destroyFederationExecution");
	            HlaResultServicesModel.updateState("disconnect");
			}
		}
    	
    	// Federation
    	if (theAttributes.containsKey(federationRTIVersionId)) {
    		try {
    			final HLAunicodeString stringDecoder = _encoderFactory.createHLAunicodeString();
				stringDecoder.decode(theAttributes.get(federationRTIVersionId));
				final String RTIversion = stringDecoder.getValue();
				
				logger.debug("RTI version = " + RTIversion);

    		} catch (DecoderException e) {
                logger.error("Failed to decode incoming attribute");
                return;
 			}
    	}
    }
    
    
    // 6.11
    /**
     * @param theObject the object instance handle
     * @param theAttributes the map of attribute handle / value
     */
    @Override
    public void reflectAttributeValues(final ObjectInstanceHandle theObject, final AttributeHandleValueMap theAttributes, final byte[] userSuppliedTag, final OrderType sentOrdering, final TransportationTypeHandle theTransport, final SupplementalReflectInfo reflectInfo) throws FederateInternalError {
        this.doReflectAttributeValues(theObject, theAttributes);
    }


    // 6.11
    /**
     * @param theObject the object instance handle
     * @param theAttributes the map of attribute handle / value
     */
    @Override
    public void reflectAttributeValues(final ObjectInstanceHandle theObject, final AttributeHandleValueMap theAttributes, final byte[] userSuppliedTag, final OrderType sentOrdering, final TransportationTypeHandle theTransport, final LogicalTime theTime, final OrderType receivedOrdering, final SupplementalReflectInfo reflectInfo) throws FederateInternalError {
        this.doReflectAttributeValues(theObject, theAttributes);
    }


    // 6.11
    /**
     * @param theObject the object instance handle
     * @param theAttributes the map of attribute handle / value
     */
    @Override
    public void reflectAttributeValues(final ObjectInstanceHandle theObject, final AttributeHandleValueMap theAttributes, final byte[] userSuppliedTag, final OrderType sentOrdering, final TransportationTypeHandle theTransport, final LogicalTime theTime, final OrderType receivedOrdering, final MessageRetractionHandle retractionHandle, final SupplementalReflectInfo reflectInfo) throws FederateInternalError {
        this.doReflectAttributeValues(theObject, theAttributes);
    }
    
}
