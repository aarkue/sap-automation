
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
                    message= "ERROR: " + tabProfiles.getString("MESSAGE");
                System.out.println(message);
                  throw new RuntimeException(tabProfiles.getString("MESSAGE"));

                }else if (resultType == 'W') {
                    message= "Warning: " + tabProfiles.getString("MESSAGE");
				  System.out.println(message);
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

    /**
     *
     * @param dest
     * @param paramMap
     * @return Order Number (e.g. 000000822328)
     * @throws JCoException
     */
    public static String createProductionOrder(JCoDestination dest, Map<String,String> paramMap)  throws JCoException {
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
        return function.getExportParameterList().getValue(0).toString();
    }

    public static String getReservationOfProOrder(JCoDestination dest, String prodOrdID)  throws JCoException {
        JCoRepository sapRepository = dest.getRepository();
        JCoFunctionTemplate template = sapRepository.getFunctionTemplate("BAPI_PRODORD_GET_DETAIL");
        JCoFunction function = template.getFunction();


        function.getImportParameterList().setValue("NUMBER", prodOrdID);


        // ORDER_OBJECTS
        JCoStructure orderObjects = function.getImportParameterList().getStructure("ORDER_OBJECTS");
        orderObjects.setValue("HEADER","X");
        function.getImportParameterList().setValue("ORDER_OBJECTS", orderObjects);

        function.execute(dest);
        System.out.println(function.getTableParameterList());
        throwExceptionOnError(function);

        return function.getTableParameterList().getTable("HEADER").getValue("RESERVATION_NUMBER").toString();
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

    public static String createPurReq(JCoDestination dest, Map<String, String> paramMap) throws JCoException {
        JCoRepository sapRepository = dest.getRepository();
        JCoFunctionTemplate template = sapRepository.getFunctionTemplate("BAPI_PR_CREATE");
        JCoFunction function = template.getFunction();


        // PRHEADER
        JCoStructure header = function.getImportParameterList().getStructure("PRHEADER");
        header.setValue("PR_TYPE", "NB");
        header.setValue("AUTO_SOURCE","X");
        function.getImportParameterList().setValue("PRHEADER", header);

        // PRHEADERX
        JCoStructure headerX = function.getImportParameterList().getStructure("PRHEADERX");
        headerX.setValue("PR_TYPE", "X");
        headerX.setValue("AUTO_SOURCE","X");
        function.getImportParameterList().setValue("PRHEADERX", headerX);


        // PRITEM
        JCoTable prItem = function.getTableParameterList().getTable("PRITEM");
        prItem.appendRow();
        // Assumes that there is only one item to consider (first=10) in the PR
        prItem.setValue("PREQ_ITEM", "10");
        prItem.setValue("PLANT", "1000");
        prItem.setValue("MATERIAL", paramMap.get("MATERIAL"));
        prItem.setValue("QUANTITY", paramMap.get("QUANTITY"));
        prItem.setValue("RESERV_NO", paramMap.get("RESERV_NO"));
        prItem.setValue("TRACKINGNO", paramMap.get("TRACKINGNO"));
        function.getTableParameterList().setValue("PRITEM", prItem);

        // PRITEMX
        JCoTable prItemX = function.getTableParameterList().getTable("PRITEMX");
        prItemX.appendRow();
        // Assumes that there is only one item to consider (first=10) in the PR
        prItemX.setValue("PREQ_ITEM", "10");
        prItemX.setValue("PREQ_ITEMX", "X");
        prItemX.setValue("PLANT", "X");
        prItemX.setValue("MATERIAL", "X");
        prItemX.setValue("QUANTITY","X");
        prItemX.setValue("RESERV_NO", "X");
        prItemX.setValue("TRACKINGNO", "X");
        function.getTableParameterList().setValue("PRITEMX", prItemX);



        function.execute(dest);
        String message = String.format("Create Purchase Requisition with %s (BAPI_PR_CREATE)", paramMap.toString());

        System.out.println(message);
        System.out.println(function.getExportParameterList());
        throwExceptionOnError(function);
        return function.getExportParameterList().getValue(0).toString();
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

    public static String convertPurchReqToOrder(JCoDestination dest, Map<String, String> paramMap) throws JCoException {
        JCoRepository sapRepository = dest.getRepository();
        JCoFunctionTemplate template = sapRepository.getFunctionTemplate("BAPI_PO_CREATE1");
        JCoFunction function = template.getFunction();

        // POHEADER
        JCoStructure header = function.getImportParameterList().getStructure("POHEADER");
        header.setValue("COMP_CODE", paramMap.get("COMP_CODE"));
        header.setValue("DOC_TYPE", "NB");
        header.setValue("CREAT_DATE", paramMap.get("CREAT_DATE"));
        header.setValue("PURCH_ORG", paramMap.get("PURCH_ORG"));
        header.setValue("PUR_GROUP", paramMap.get("PUR_GROUP"));

        function.getImportParameterList().setValue("POHEADER", header);


        // POHEADERX
        JCoStructure headerX = function.getImportParameterList().getStructure("POHEADERX");
        headerX.setValue("COMP_CODE", "X");
        headerX.setValue("DOC_TYPE", "X");
        headerX.setValue("CREAT_DATE", "X");
        headerX.setValue("PURCH_ORG", "X");
        headerX.setValue("PUR_GROUP", "X");

        function.getImportParameterList().setValue("POHEADERX", headerX);

        // POITEM
        JCoTable poItem = function.getTableParameterList().getTable("POITEM");
        poItem.appendRow();
        // Assumes that there is only one item to consider (first=10) in the PR
        poItem.setValue("PO_ITEM", "10");
        poItem.setValue("PLANT", "1000");
        poItem.setValue("PREQ_NO", paramMap.get("PREQ_NO"));
        poItem.setValue("PREQ_ITEM", "10");
        function.getTableParameterList().setValue("POITEM", poItem);

        // POITEMX
        JCoTable poItemX = function.getTableParameterList().getTable("POITEMX");
        poItemX.appendRow();
        poItemX.setValue("PO_ITEM", "10");
        poItemX.setValue("PLANT", "X");
        poItemX.setValue("PREQ_NO", "X");
        poItemX.setValue("PREQ_ITEM", "X");

        function.getTableParameterList().setValue("POITEMX", poItemX);

        function.execute(dest);


        String message = String.format("Convert Purchase Requistion with %s (BAPI_PO_CREATE1)", paramMap.toString());

        System.out.println(message);
        System.out.println(function.getTableParameterList().getTable("RETURN"));
        throwExceptionOnError(function);
        return function.getExportParameterList().getValue(2).toString();
    }

    public static void releasePurchaseOrd(JCoDestination dest, Map<String, String> paramMap) throws JCoException {
        JCoRepository sapRepository = dest.getRepository();
        JCoFunctionTemplate template = sapRepository.getFunctionTemplate("BAPI_PO_RELEASE");
        JCoFunction function = template.getFunction();

        function.getImportParameterList().setValue("PURCHASEORDER", paramMap.get("PURCHASEORDER"));
        function.getImportParameterList().setValue("PO_REL_CODE", paramMap.get("PO_REL_CODE"));
        function.execute(dest);
        String message = String.format("Release/Reject Purchase Order with %s (BAPI_PO_RELEASE)", paramMap.toString());

        System.out.println(message);
        System.out.println(function.getExportParameterList());
        throwExceptionOnError(function);
    }

    public static void resetPurchaseOrdRelease(JCoDestination dest, Map<String, String> paramMap) throws JCoException {
        JCoRepository sapRepository = dest.getRepository();
        JCoFunctionTemplate template = sapRepository.getFunctionTemplate("BAPI_PO_RESET_RELEASE");
        JCoFunction function = template.getFunction();

        function.getImportParameterList().setValue("PURCHASEORDER", paramMap.get("PURCHASEORDER"));
        function.getImportParameterList().setValue("PO_REL_CODE", paramMap.get("PO_REL_CODE"));
        function.execute(dest);
        String message = String.format("Reset Release of Purchase Order with %s (BAPI_PO_RELEASE)", paramMap.toString());

        System.out.println(message);
        System.out.println(function.getExportParameterList());
        throwExceptionOnError(function);
    }

    public static void goodsReceiptForPurchOrd(JCoDestination dest, Map<String, String> paramMap) throws JCoException {
        JCoRepository sapRepository = dest.getRepository();
        JCoFunctionTemplate template = sapRepository.getFunctionTemplate("BAPI_GOODSMVT_CREATE");
        JCoFunction function = template.getFunction();

        // GOODSMVT_HEADER
        JCoStructure header = function.getImportParameterList().getStructure("GOODSMVT_HEADER");
        header.setValue("PSTNG_DATE", paramMap.get("PSTNG_DATE"));
        header.setValue("DOC_DATE", paramMap.get("DOC_DATE"));
        function.getImportParameterList().setValue("GOODSMVT_HEADER", header);

        // GOODSMVT_ITEM STRUCTURE
        JCoTable items = function.getTableParameterList().getTable("GOODSMVT_ITEM");
        items.appendRow();
        items.setValue("PO_NUMBER", paramMap.get("PO_NUMBER"));
        items.setValue("PO_ITEM", paramMap.get("PO_ITEM"));
        items.setValue("PLANT", paramMap.get("PLANT"));
        items.setValue("STGE_LOC", paramMap.get("STGE_LOC"));
        items.setValue("MOVE_TYPE", "101");
        items.setValue("MVT_IND", "B");
        items.setValue("NO_MORE_GR", "X");
        items.setValue("ENTRY_QNT",  paramMap.get("ENTRY_QNT")); // Can we omit that? No! Otherwise it is posted with 0


        function.getTableParameterList().setValue("GOODSMVT_ITEM", items);


        JCoStructure goodsmvtCode = function.getImportParameterList().getStructure("GOODSMVT_CODE");
        goodsmvtCode.setValue("GM_CODE","01");
        function.getImportParameterList().setValue("GOODSMVT_CODE", goodsmvtCode);



        function.execute(dest);
        String message = String.format("Goods Receipt for Purchase Order with %s (BAPI_GOODSMVT_CREATE)", paramMap.toString());

        System.out.println(message);
        System.out.println(function.getExportParameterList());
        throwExceptionOnError(function);
    }

    public static void goodIssueForProd(JCoDestination dest, Map<String, String> paramMap) throws JCoException {
        JCoRepository sapRepository = dest.getRepository();
        JCoFunctionTemplate template = sapRepository.getFunctionTemplate("BAPI_GOODSMVT_CREATE");
        JCoFunction function = template.getFunction();

        // GOODSMVT_HEADER
        JCoStructure header = function.getImportParameterList().getStructure("GOODSMVT_HEADER");
        header.setValue("PSTNG_DATE", paramMap.get("PSTNG_DATE"));
        header.setValue("DOC_DATE", paramMap.get("DOC_DATE"));
        header.setValue("REF_DOC_NO", paramMap.get("ORDERID"));
        function.getImportParameterList().setValue("GOODSMVT_HEADER", header);

        // GOODSMVT_ITEM STRUCTURE
        JCoTable items = function.getTableParameterList().getTable("GOODSMVT_ITEM");

        int i = 1;
        while(paramMap.containsKey("MATERIAL"+i)){
            items.appendRow();
            items.setValue("ORDERID", paramMap.get("ORDERID"));
            items.setValue("PLANT", paramMap.get("PLANT"));
            items.setValue("STGE_LOC", paramMap.get("STGE_LOC"));
            items.setValue("MOVE_TYPE", "261");
            items.setValue("MATERIAL", paramMap.get("MATERIAL"+i));
            items.setValue("ENTRY_QNT", paramMap.get("ENTRY_QNT"+i)); // Can we omit that? No! Otherwise it is posted with 0
            items.setValue("WITHDRAWN", "X");
            items.setValue("RESERV_NO",paramMap.get("RESERV_NO"));
            items.setValue("RES_ITEM",""+i);

            i++;
        }


//        items.setValue("MVT_IND", "F");


//        Disabled, as CPN Execution (see  CSVExecuter) does these items individually
//        items.appendRow();
//        items.setValue("ORDERID", paramMap.get("ORDERID"));
//        items.setValue("PLANT", paramMap.get("PLANT"));
//        items.setValue("STGE_LOC", paramMap.get("STGE_LOC"));
//        items.setValue("MOVE_TYPE", "261");
//        items.setValue("MATERIAL", paramMap.get("MATERIAL2"));
//        items.setValue("ENTRY_QNT", paramMap.get("ENTRY_QNT2"));
//        items.setValue("WITHDRAWN", "X");
//        items.setValue("RESERV_NO",paramMap.get("RESERV_NO"));
//        items.setValue("RES_ITEM","2");
//        items.setValue("MVT_IND", "F");

        function.getTableParameterList().setValue("GOODSMVT_ITEM", items);


        JCoStructure goodsmvtCode = function.getImportParameterList().getStructure("GOODSMVT_CODE");
        goodsmvtCode.setValue("GM_CODE","03");
        function.getImportParameterList().setValue("GOODSMVT_CODE", goodsmvtCode);



        function.execute(dest);
        String message = String.format("Goods Issue for Production Order with %s (BAPI_GOODSMVT_CREATE)", paramMap.toString());

        System.out.println(message);
        System.out.println(function.getExportParameterList());
        throwExceptionOnError(function);
    }

    public static void confirmProdOrd(JCoDestination dest, Map<String, String> paramMap) throws JCoException {
        JCoRepository sapRepository = dest.getRepository();
        JCoFunctionTemplate template = sapRepository.getFunctionTemplate("BAPI_PRODORDCONF_CREATE_HDR");
        JCoFunction function = template.getFunction();

        // GOODSMVT_ITEM STRUCTURE
        JCoTable athdrlevels = function.getTableParameterList().getTable("ATHDRLEVELS");
        athdrlevels.appendRow();
        athdrlevels.setValue("ORDERID", paramMap.get("ORDERID"));
        athdrlevels.setValue("FIN_CONF", paramMap.get("FIN_CONF"));
        athdrlevels.setValue("POSTG_DATE", paramMap.get("POSTG_DATE"));
        athdrlevels.setValue("YIELD", paramMap.get("YIELD"));
        athdrlevels.setValue("SCRAP", paramMap.get("SCRAP"));

        function.getTableParameterList().setValue("ATHDRLEVELS", athdrlevels);

        function.execute(dest);
        String message = String.format("Confirmation for Production Order with %s (BAPI_PRODORDCONF_CREATE_HDR)", paramMap.toString());

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