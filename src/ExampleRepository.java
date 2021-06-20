
import java.util.Map;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoFunctionTemplate;
import com.sap.conn.jco.JCoRepository;
import com.sap.conn.jco.JCoStructure;
import com.sap.conn.jco.JCoTable;

public class ExampleRepository {
    public static void throwExceptionOnError(JCoFunction function) {
        try {
            JCoTable tabProfiles = function.getTableParameterList().getTable("RETURN");
            char resultType;
            String message;
            for (int i = 0; i < tabProfiles.getNumRows(); i++, tabProfiles.nextRow()) {
                resultType = tabProfiles.getChar("TYPE");
                if (resultType == 'E' || resultType == 'A') {
//					  throw new RuntimeException(tabProfiles.getString("MESSAGE"));
                    message= "ERROR: " + tabProfiles.getString("MESSAGE");
//				  System.out.println("ERROR: " + tabProfiles.getString("MESSAGE"));
                }else if (resultType == 'W') {
                    message= "Warning: " + tabProfiles.getString("MESSAGE");
//				  System.out.println("Warning: " + tabProfiles.getString("MESSAGE"));
                }
            }
        } catch(Exception e) {
        }



    }

    public static void createInquiry(JCoDestination dest,Map<String,String> paramMap) throws JCoException {
        JCoRepository sapRepository = dest.getRepository();
        JCoFunctionTemplate template = sapRepository.getFunctionTemplate("BAPI_INQUIRY_CREATEFROMDATA2");
        JCoFunction function = template.getFunction();

        // SALES_HEADER_IN
        JCoStructure inquiryHeader = function.getImportParameterList().getStructure("INQUIRY_HEADER_IN");
        inquiryHeader.setValue("DOC_TYPE",paramMap.get("DOC_TYPE"));
        inquiryHeader.setValue("SALES_ORG", paramMap.get("SALES_ORG"));
        inquiryHeader.setValue("DISTR_CHAN", paramMap.get("DISTR_CHAN"));
        inquiryHeader.setValue("DIVISION", paramMap.get("DIVISION"));

        function.getImportParameterList().setValue("INQUIRY_HEADER_IN", inquiryHeader);
        function.getImportParameterList().setValue("SALESDOCUMENTIN", paramMap.get("INQUIRY_NUMBER"));

        //SALES_PARTNERS
        JCoTable inquiryPartners = function.getTableParameterList().getTable("INQUIRY_PARTNERS");
        inquiryPartners.appendRow();
        inquiryPartners.setValue("PARTN_ROLE", "AG");
        inquiryPartners.setValue("PARTN_NUMB", paramMap.get("PARTN_NUMB"));
        inquiryPartners.appendRow();
        inquiryPartners.setValue("PARTN_ROLE", "WE");
        inquiryPartners.setValue("PARTN_NUMB", paramMap.get("PARTN_NUMB"));
        function.getTableParameterList().setValue("INQUIRY_PARTNERS", inquiryPartners);

        function.execute(dest);
        String message=String.format("Create Inquiry with %s (BAPI_INQUIRY_CREATEFROMDATA2)",paramMap.toString());
        throwExceptionOnError(function);
    }

    public static void commitTrans(JCoDestination dest) throws JCoException  {
        JCoRepository sapRepository = dest.getRepository();
        JCoFunctionTemplate template = sapRepository.getFunctionTemplate("BAPI_TRANSACTION_COMMIT");
        JCoFunction commFunct = template.getFunction();
        commFunct.getImportParameterList().setValue("WAIT", "10");
        commFunct.execute(dest);
        System.out.println("Function BAPI_TRANSACTION_COMMIT executed .");
    }


    public static void sendErrorMessage(JCoDestination dest, Map<String,String> paramMap) throws JCoException {
        JCoRepository sapRepository = dest.getRepository();
        JCoFunctionTemplate template = sapRepository.getFunctionTemplate("TH_POPUP");
        JCoFunction function = template.getFunction();

        function.getImportParameterList().setValue("CLIENT", "800");

        function.getImportParameterList().setValue("USER", "M4JID1");

        function.getImportParameterList().setValue("MESSAGE", "Warning: order - " + paramMap.get("ORDER_NUMBER") +  " is changed. Determine if it is valid.");


        function.execute(dest);
    }

    public static void main(String[] arg) throws Exception{
    }
}