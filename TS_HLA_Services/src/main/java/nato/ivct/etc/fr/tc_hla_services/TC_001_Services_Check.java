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

package nato.ivct.etc.fr.tc_hla_services;

import de.fraunhofer.iosb.tc_lib.AbstractTestCase;
import de.fraunhofer.iosb.tc_lib.IVCT_BaseModel;
import de.fraunhofer.iosb.tc_lib.IVCT_LoggingFederateAmbassador;
import de.fraunhofer.iosb.tc_lib.IVCT_RTI_Factory;
import de.fraunhofer.iosb.tc_lib.IVCT_RTIambassador;
import de.fraunhofer.iosb.tc_lib.TcFailed;
import de.fraunhofer.iosb.tc_lib.TcInconclusive;
import hla.rti1516e.FederateHandle;
import nato.ivct.etc.fr.tc_lib_hla_services.HLA_Services_BaseModel;
import nato.ivct.etc.fr.tc_lib_hla_services.HLA_Services_TcParam;
import nato.ivct.etc.fr.fctt_common.utils.FCTT_Constant;
import nato.ivct.etc.fr.fctt_common.utils.TextInternationalization;

import org.slf4j.Logger;

import java.io.File;

/**
 * @author FRANCE (DGA/Capgemini)
 */
public class TC_001_Services_Check extends AbstractTestCase {
    FederateHandle                              TcFederateHandle;
    private String                              TcFederateName = "IVCT_HLA_Services";

    // Build test case parameters to use
    static HLA_Services_TcParam              	HlaServicesTcParam;

    // Get logging-IVCT-RTI using tc_param federation name, host
    private static IVCT_RTIambassador           ivct_rti;
    static HLA_Services_BaseModel            	HlaServicesBaseModel;
    
    static IVCT_LoggingFederateAmbassador		ivct_LoggingFederateAmbassador;


    @Override
    public IVCT_BaseModel getIVCT_BaseModel(final String tcParamJson, final Logger logger) throws TcInconclusive {

    	try {
	    	HlaServicesTcParam           	= new HLA_Services_TcParam(tcParamJson);
	    	ivct_rti                        = IVCT_RTI_Factory.getIVCT_RTI(logger);
	    	HlaServicesBaseModel         	= new HLA_Services_BaseModel(logger, ivct_rti, HlaServicesTcParam);
	    	ivct_LoggingFederateAmbassador  = new IVCT_LoggingFederateAmbassador(HlaServicesBaseModel, logger);
    	}
    	catch(Exception ex) {
    		logger.error(TextInternationalization.getString("etc_fra.noInstanciation"));
    	}
    	return HlaServicesBaseModel;
    }

    @Override
    protected void logTestPurpose(final Logger logger) {
    	
    	// Build purpose text
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(FCTT_Constant.REPORT_FILE_SEPARATOR);
        stringBuilder.append(TextInternationalization.getString("etc_fra.purpose")); stringBuilder.append("\n");
        stringBuilder.append(TextInternationalization.getString("hlaservices.servicesPublication")); stringBuilder.append("\n");
        stringBuilder.append(TextInternationalization.getString("hlaservices.FomSomComparison1")); stringBuilder.append("\n");
        stringBuilder.append(TextInternationalization.getString("hlaservices.FomSomComparison2")); stringBuilder.append("\n");
        final String testPurpose = stringBuilder.toString();
        logger.info(testPurpose);
    }


    @Override
    protected void preambleAction(final Logger logger) throws TcInconclusive {

    	// Load FOM/SOM files
        if (HlaServicesBaseModel.loadFomSomFiles() == false)
        	throw new TcInconclusive(TextInternationalization.getString("etc_fra.FomSomError"));
        
    	// Initiate rti
        TcFederateHandle = HlaServicesBaseModel.initiateRti(TcFederateName, ivct_LoggingFederateAmbassador);

        // Do the necessary calls to get handles and do publish and subscribe
        if (HlaServicesBaseModel.init(HlaServicesTcParam.getSutName()))
            throw new TcInconclusive(TextInternationalization.getString("etc_fra.initError"));

    	logger.info(TextInternationalization.getString("etc_fra.RtiConnected"));
    	logger.info(FCTT_Constant.REPORT_FILE_SEPARATOR);
    }


    @Override
    protected void performTest(final Logger logger) throws TcInconclusive, TcFailed {

    	// Check result directory
    	String resultFileName = HlaServicesTcParam.getResultDir();
        File resultFile = new File(resultFileName);
        if (!resultFile.exists())
            throw new TcInconclusive(String.format(TextInternationalization.getString("etc_fra.resultDirError"),resultFileName));
    	
        // Allow time to work and get some reflect values.
        if (HlaServicesBaseModel.sleepFor(logger,HlaServicesTcParam.getTestDuration())) {
            throw new TcInconclusive(TextInternationalization.getString("etc_fra.sleepError"));
        }
        
    	logger.info(TextInternationalization.getString("etc_fra.wakeup"));

    	// Generate result files
        if (HlaServicesBaseModel.validateServices() == false)
        	throw new TcFailed(TextInternationalization.getString("hlaservices.invalidServices"));
    }


    @Override
    protected void postambleAction(final Logger logger) throws TcInconclusive, TcInconclusive {
        
        // Terminate rti
        HlaServicesBaseModel.terminateRti();
    }
}
