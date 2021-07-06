
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    public static void createProductionOrder(JCoDestination dest, Map<String,String> paramMap)  throws JCoException {
        JCoRepository sapRepository = dest.getRepository();
        JCoFunctionTemplate template = sapRepository.getFunctionTemplate("BAPI_PRODORD_CREATE");
        JCoFunction function = template.getFunction();


        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        // ORDERDATA
        JCoStructure orderData = function.getImportParameterList().getStructure("ORDERDATA");
        orderData.setValue("MATERIAL",paramMap.get("MATERIAL"));
        orderData.setValue("PLANT", paramMap.get("PLANT"));
        orderData.setValue("PLANNING_PLANT", paramMap.get("PLANNING_PLANT"));
        orderData.setValue("ORDER_TYPE", paramMap.get("ORDER_TYPE"));
        orderData.setValue("QUANTITY", paramMap.get("QUANTITY"));
        orderData.setValue("BASIC_END_DATE", paramMap.get("BASIC_END_DATE"));
        function.getImportParameterList().setValue("ORDERDATA", orderData);

        function.execute(dest);
        String message = String.format("Create ProdOrd with %s (BAPI_PRODORD_CREATE)", paramMap.toString());
        System.out.println(message);
        System.out.println(function.getExportParameterList());
        throwExceptionOnError(function);

    }

    public static void planMaterial(JCoDestination dest, Map<String, String> paramMap) throws JCoException {
        JCoRepository sapRepository = dest.getRepository();
        JCoFunctionTemplate template = sapRepository.getFunctionTemplate("BAPI_MATERIAL_PLANNING");
        JCoFunction function = template.getFunction();

        function.getImportParameterList().setValue("MATERIAL", paramMap.get("MATERIAL"));
        function.getImportParameterList().setValue("PLANT", paramMap.get("PLANT"));
        function.getImportParameterList().setValue("MRP_AREA", paramMap.get("MRP_AREA"));

        function.execute(dest);
        String message = String.format("Plan Material with %s (BAPI_MATERIAL_PLANNING)", paramMap.toString());

        System.out.println(message);
        System.out.println(function.getExportParameterList());
        throwExceptionOnError(function);
    }

    public static void releasePurchaseReq(JCoDestination dest, Map<String, String> paramMap) throws JCoException {
        JCoRepository sapRepository = dest.getRepository();
        JCoFunctionTemplate template = sapRepository.getFunctionTemplate("BAPI_REQUISITION_RELEASE_GEN");
        JCoFunction function = template.getFunction();

        function.getImportParameterList().setValue("NUMBER", paramMap.get("NUMBER"));
        function.getImportParameterList().setValue("REL_CODE", paramMap.get("REL_CODE"));
        function.execute(dest);
        String message = String.format("Release Purchase Requistion with %s (BAPI_REQUISITION_RELEASE_GEN)", paramMap.toString());

        System.out.println(message);
        System.out.println(function.getExportParameterList());
        throwExceptionOnError(function);
    }

    public static void convertPurchReqToOrder(JCoDestination dest, Map<String, String> paramMap) throws JCoException {
        JCoRepository sapRepository = dest.getRepository();
        JCoFunctionTemplate template = sapRepository.getFunctionTemplate("BAPI_PO_CREATE1");
        JCoFunction function = template.getFunction();

        // POHEADER
        JCoStructure header = function.getImportParameterList().getStructure("POHEADER");
        System.out.println(header);
        header.setValue("COMP_CODE", paramMap.get("COMP_CODE"));
        header.setValue("DOC_TYPE", "NB");
        header.setValue("CREAT_DATE", paramMap.get("CREAT_DATE"));
//        header.setValue("VENDOR", paramMap.get("VENDOR"));
//        header.setValue("SUPPL_VEND", paramMap.get("VENDOR"));

        header.setValue("PURCH_ORG", paramMap.get("PURCH_ORG"));
        header.setValue("PUR_GROUP", paramMap.get("PUR_GROUP"));

//        header.setValue("CURRENCY", "EUR");
//        header.setValue("LANGU", "EN");

        function.getImportParameterList().setValue("POHEADER", header);

//        // POHEADERX
//        JCoStructure headerX = function.getImportParameterList().getStructure("POHEADERX");
//        System.out.println(headerX);
//        headerX.setValue("COMP_CODE","X");
//        headerX.setValue("DOC_TYPE","X");
//        headerX.setValue("CREAT_DATE","X");
//        headerX.setValue("VENDOR","X");
//        headerX.setValue("PURCH_ORG","X");
//        headerX.setValue("PUR_GROUP","X");
//
////        header.setValue("CREAT_DATE",paramMap.get("CREAT_DATE"));
//
//        function.getImportParameterList().setValue("POHEADERX", header);

        // POITEM
        JCoTable poItem = function.getTableParameterList().getTable("POITEM");
        System.out.println(poItem);
        poItem.appendRow();
        poItem.setValue("PO_ITEM", "10");
//        poItem.setValue("MATERIAL", "DPC1018");
        poItem.setValue("PLANT", "1000");
//        poItem.setValue("QUANTITY", "111");
//        poItem.setValue("NET_PRICE", "17.5");
        poItem.setValue("PREQ_NO", paramMap.get("PREQ_NO"));
        poItem.setValue("PREQ_ITEM", paramMap.get("PREQ_ITEM"));
        function.getTableParameterList().setValue("POITEM", poItem);

        // POITEMX
        JCoTable poItemX = function.getTableParameterList().getTable("POITEMX");
        System.out.println(poItemX);
        poItemX.appendRow();
        poItemX.setValue("PO_ITEM", "10");
//        poItemX.setValue("MATERIAL", "DPC1018");
        poItemX.setValue("PLANT", "1000");
//        poItemX.setValue("QUANTITY", "111");
//        poItemX.setValue("NET_PRICE", "17.5");
        poItemX.setValue("PREQ_NO", paramMap.get("PREQ_NO"));
        poItemX.setValue("PREQ_ITEM", paramMap.get("PREQ_ITEM"));

        function.getTableParameterList().setValue("POITEMX", poItemX);


//        // POPARTNER
//        JCoTable poPatner = function.getTableParameterList().getTable("POPARTNER");
//        System.out.println(poPatner);
//        poPatner.appendRow();
//        poPatner.setValue("BUSPARTNO", "1005");
//        poPatner.setValue("PARTNERDESC", "VN");
//        poPatner.setValue("LANGU", "EN");
//
//        function.getTableParameterList().setValue("POPARTNER", poPatner);


        function.execute(dest);


        String message = String.format("Convert Purchase Requistion with %s (BAPI_PO_CREATE1)", paramMap.toString());

        System.out.println(message);
        System.out.println(function.getTableParameterList().getTable("RETURN"));
        throwExceptionOnError(function);
    }

    public static void releasePurchaseOrd(JCoDestination dest, Map<String, String> paramMap) throws JCoException {
        JCoRepository sapRepository = dest.getRepository();
        JCoFunctionTemplate template = sapRepository.getFunctionTemplate("BAPI_PO_RELEASE");
        JCoFunction function = template.getFunction();

        function.getImportParameterList().setValue("PURCHASEORDER", paramMap.get("PURCHASEORDER"));
        function.getImportParameterList().setValue("PO_REL_CODE", paramMap.get("PO_REL_CODE"));
        function.execute(dest);
        String message = String.format("Release Purchase Order with %s (BAPI_PO_RELEASE)", paramMap.toString());

        System.out.println(message);
        System.out.println(function.getExportParameterList());
        throwExceptionOnError(function);
    }


    public static void commitTrans(JCoDestination dest) throws JCoException {
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