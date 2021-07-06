import com.sap.conn.jco.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Example {

    public static void exampleProcess(ExampleRepository transRepo, JCoDestination dest, Map<String,Map<String,String>> paramMap){
        Random fRandom = new Random();
        try {
            try {
                // start the session
                JCoContext.begin(dest);
                double delayTime = 7.0;
//                transRepo.planMaterial(dest,paramMap.get("planMaterial1"));
//                transRepo.releasePurchaseReq(dest, paramMap.get("releasePurchaseReq1"));
//                transRepo.releasePurchaseReq(dest, paramMap.get("releasePurchaseReq2"));
//                transRepo.convertPurchReqToOrder(dest, paramMap.get("convertPurchReqToOrder1"));
//                transRepo.releasePurchaseOrd(dest, paramMap.get("releasePurchaseOrd1"));
                transRepo.goodsReceiptForPurchOrd(dest,paramMap.get("goodsReceiptForPurchOrd2"));
                transRepo.commitTrans(dest);
                try {
                    TimeUnit.SECONDS.sleep((long) delayTime);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } catch (Exception e ){
                System.out.println(e);
                e.printStackTrace();
            }
            finally {
                // clean ups
                JCoContext.end(dest);
            }
        }
        catch(JCoException e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public static void main(String[] arg) throws Exception{
        Map<String,String> sapCredential = new HashMap<>();
        sapCredential.put("host","185.208.175.208");
        sapCredential.put("client","800");
        sapCredential.put("user","m4jid1");
        sapCredential.put("passwd","m4jidabap");
        sapCredential.put("sysnr","00");
        sapCredential.put("lang","en");
        ExampleSAPConnector sapConnector = new ExampleSAPConnector(sapCredential);
        JCoDestination dest = sapConnector.getDestination();
        ExampleRepository transRepo = new ExampleRepository();

        Map<String,Map<String,String>> allParameter = new LinkedHashMap<>();

        Map<String,String> paramMap_inquiry = new LinkedHashMap<String,String>();
        paramMap_inquiry.put("INQUIRY_NUMBER", "0015000303");
        paramMap_inquiry.put("DOC_TYPE", "ZPIN");
        paramMap_inquiry.put("SALES_ORG", "1000");
        paramMap_inquiry.put("DISTR_CHAN", "10");
        paramMap_inquiry.put("DIVISION", "00");
        paramMap_inquiry.put("PARTN_NUMB", "0000001032");

//        exampleProcess(transRepo,dest,paramMap_inquiry);


        Map<String, String> paramMap_prodord = new LinkedHashMap<>();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        paramMap_prodord.put("MATERIAL", "000000000050066346");
        paramMap_prodord.put("PLANT", "1000");
        paramMap_prodord.put("PLANNING_PLANT", "1000");
        paramMap_prodord.put("ORDER_TYPE", "PP01");
        paramMap_prodord.put("QUANTITY", "111");
        paramMap_prodord.put("BASIC_END_DATE", "2021-07-18");
        allParameter.put("createProductionOrder", paramMap_prodord);

        Map<String, String> paramMap_planmat1 = new LinkedHashMap<>();
        paramMap_planmat1.put("MATERIAL", "DPC1015");
        paramMap_planmat1.put("PLANT", "1000");
        paramMap_planmat1.put("MRP_AREA", "1000");
        allParameter.put("planMaterial1", paramMap_planmat1);

        Map<String, String> paramMap_planmat2 = new LinkedHashMap<>();
        paramMap_planmat2.put("MATERIAL", "DPC1018");
        paramMap_planmat2.put("PLANT", "1000");
        paramMap_planmat2.put("MRP_AREA", "1000");
        allParameter.put("planMaterial2", paramMap_planmat2);


        Map<String, String> paramMap_releasePR1 = new LinkedHashMap<>();
        paramMap_releasePR1.put("NUMBER", "0010044648");
        paramMap_releasePR1.put("REL_CODE", "04");
        allParameter.put("releasePurchaseReq1", paramMap_releasePR1);

        Map<String, String> paramMap_releasePR2 = new LinkedHashMap<>();
        paramMap_releasePR2.put("NUMBER", "0010044648");
        paramMap_releasePR2.put("REL_CODE", "02");
        allParameter.put("releasePurchaseReq2", paramMap_releasePR2);

        Map<String, String> convertToPO1 = new LinkedHashMap<>();
        convertToPO1.put("COMP_CODE", "1000");
        convertToPO1.put("CREAT_DATE", "2021-07-05");
        convertToPO1.put("VENDOR", "XXXX");
        convertToPO1.put("PURCH_ORG", "1000");
        convertToPO1.put("PUR_GROUP", "001");
        convertToPO1.put("PREQ_NO", "0010044648");
        convertToPO1.put("PREQ_ITEM", "10");
        allParameter.put("convertPurchReqToOrder1", convertToPO1);

        Map<String, String> convertToPO2 = new LinkedHashMap<>();
        convertToPO2.put("COMP_CODE", "1000");
        convertToPO2.put("CREAT_DATE", "2021-07-05");
        convertToPO2.put("VENDOR", "1005");
        convertToPO2.put("PURCH_ORG", "1000");
        convertToPO2.put("PUR_GROUP", "001");
        convertToPO2.put("PREQ_NO", "0010044647");
        convertToPO2.put("PREQ_ITEM", "10");
        allParameter.put("convertPurchReqToOrder2", convertToPO2);

        Map<String, String> paramMap_releasePO2 = new LinkedHashMap<>();
        paramMap_releasePO2.put("PURCHASEORDER", "4500020255");
        paramMap_releasePO2.put("PO_REL_CODE", "CE");
        allParameter.put("releasePurchaseOrd2", paramMap_releasePO2);


        Map<String, String> paramMap_goodsRecForPurchOrd2 = new LinkedHashMap<>();
        paramMap_goodsRecForPurchOrd2.put("PSTNG_DATE", "2021-07-06");
        paramMap_goodsRecForPurchOrd2.put("DOC_DATE", "2021-07-06");
        paramMap_goodsRecForPurchOrd2.put("PO_NUMBER", "4500020255");
        paramMap_goodsRecForPurchOrd2.put("PO_ITEM", "10");
        paramMap_goodsRecForPurchOrd2.put("PLANT", "1000");
        paramMap_goodsRecForPurchOrd2.put("STGE_LOC", "0001");

        allParameter.put("goodsReceiptForPurchOrd2", paramMap_goodsRecForPurchOrd2);



        exampleProcess(transRepo, dest, allParameter);

    }
}
