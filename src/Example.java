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
//                transRepo.createInquiry(dest,paramMap_inquiry);
//                transRepo.createProductionOrder(dest,paramMap.get("createProductionOrder"));
                transRepo.planMaterial(dest,paramMap.get("planMaterial"));
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


        Map<String,String> paramMap_prodord = new LinkedHashMap<>();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        paramMap_prodord.put("MATERIAL","000000000050066346");
        paramMap_prodord.put("PLANT", "1000");
        paramMap_prodord.put("PLANNING_PLANT", "1000");
        paramMap_prodord.put("ORDER_TYPE", "PP01");
        paramMap_prodord.put("QUANTITY", "111");
        paramMap_prodord.put("BASIC_END_DATE", "2021-07-18");
        allParameter.put("createProductionOrder",paramMap_prodord);

        Map<String,String> paramMap_planmat = new LinkedHashMap<>();
        paramMap_planmat.put("MATERIAL","DPC1018");
        paramMap_planmat.put("PLANT","1000");
        paramMap_planmat.put("MRP_AREA","1000");
//        paramMap_planmat.put("PLAN_SCENARIO","1");
//        paramMap_planmat.put("MATERIAL_LONG","000000000000000000000000000000000DPC1015");
        allParameter.put("planMaterial",paramMap_planmat);

        exampleProcess(transRepo,dest, allParameter);

    }
}
