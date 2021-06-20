import com.sap.conn.jco.JCoContext;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Example {

    public static void exampleProcess(ExampleRepository transRepo, JCoDestination dest, Map<String,String> paramMap_inquiry){
        Random fRandom = new Random();
        try {
            try {
                // start the session
                JCoContext.begin(dest);
                double delayTime = 7.0;
                transRepo.createInquiry(dest,paramMap_inquiry);
                transRepo.commitTrans(dest);
                try {
                    TimeUnit.SECONDS.sleep((long) delayTime);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            finally {
                // clean ups
                JCoContext.end(dest);
            }
        }
        catch(JCoException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] arg) throws Exception{
        Map<String,String> sapCredential = new HashMap<>();
        sapCredential.put("host","185.208.175.208");
        sapCredential.put("client","800");
        sapCredential.put("user","m4jid1");
        sapCredential.put("passwd","m4jidabap");
        sapCredential.put("sysnr","0");
        sapCredential.put("lang","en");
        ExampleSAPConnector sapConnector = new ExampleSAPConnector(sapCredential);
        JCoDestination dest = sapConnector.getDestination();
        ExampleRepository transRepo = new ExampleRepository();

        Map<String,String> paramMap_inquiry = new LinkedHashMap<String,String>();
        paramMap_inquiry.put("INQUIRY_NUMBER", "0015000300");
        paramMap_inquiry.put("DOC_TYPE", "ZPIN");
        paramMap_inquiry.put("SALES_ORG", "1000");
        paramMap_inquiry.put("DISTR_CHAN", "10");
        paramMap_inquiry.put("DIVISION", "00");
        paramMap_inquiry.put("PARTN_NUMB", "0000001032");

        exampleProcess(transRepo,dest, paramMap_inquiry);

    }
}
