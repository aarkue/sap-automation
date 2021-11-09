import com.opencsv.*;
import com.opencsv.exceptions.CsvValidationException;
import com.sap.conn.jco.JCoContext;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;

public class CSVExecuter {
    private final static String[] SET_COLUMN_NAMES = {"required_material", "material", "purchase_order", "purchase_requisition", "production_order"};
    private final File csv;
    private Map<String,String> idMap = new HashMap<>();
    private Map<String,String> reservations = new HashMap<>();
    private CSVReaderHeaderAware csvReader;
    public CSVExecuter(File csv){
        try {
            FileReader reader = new FileReader(csv);
            CSVParser csvParserSemiSep = new CSVParserBuilder().withSeparator(';').build();
            csvReader = new CSVReaderHeaderAwareBuilder(reader).withCSVParser(csvParserSemiSep).build();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.csv = csv;
    }

    public boolean execute(ExampleRepository repo,  JCoDestination dest){
        Random fRandom = new Random();
        try {
            try {

                JCoContext.begin(dest);

                //read csv
                //for each line
                while(csvReader.iterator().hasNext()){
                    //do something
                    Map<String, String> map = csvReader.readMap();
                    if(!executeOne(repo,dest,map)){
                        return false;
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
                e.printStackTrace();
            } finally {
                // clean ups
                JCoContext.end(dest);
            }
        } catch (JCoException e) {
            System.out.println(e);
            e.printStackTrace();
        }

        return false;
    }

    private boolean executeOne(ExampleRepository repo,  JCoDestination dest, Map<String,String> params) throws JCoException {

        switch (params.get("activity")){
            case "create production order" -> {
                System.out.println("create production order: " + params.toString());
                // TODO: END_DATE ?
                final String END_DATE = LocalDate.parse(params.get("time").split(" ")[0]).plus(7, ChronoUnit.DAYS).format(DateTimeFormatter.ISO_LOCAL_DATE);
                final String prodOrderID = parseSetValue(params.get("production_order"))[0];
                Map<String, String> sapParams = new HashMap<>();
                sapParams.put("MATERIAL", parseSetValue(params.get("material"))[0]);
                sapParams.put("PLANT", "1000");
                sapParams.put("PLANNING_PLANT", "1000");
                sapParams.put("ORDER_TYPE", "PP01");
                sapParams.put("QUANTITY", params.get("amount_material"));
                sapParams.put("BASIC_END_DATE", END_DATE);
                String prodOrderID_SAP  = ExampleRepository.createProductionOrder(dest,sapParams);
                // Save production order & reservation ID for further usage
                idMap.put(prodOrderID,prodOrderID_SAP);
                String reservationID_SAP = ExampleRepository.getReservationOfProOrder(dest,prodOrderID_SAP);
                reservations.put(prodOrderID,reservationID_SAP);
            }
            case "create purchase requisition" -> {
                final String purchReqID = parseSetValue(params.get("purchase_requisition"))[0];
                Map<String, String> sapParams = new HashMap<>();
                sapParams.put("MATERIAL", "DPC1015");
                sapParams.put("QUANTITY", params.get("amount_material"));
                String purchReqID_SAP = ExampleRepository.createPurReq(dest, sapParams);
                // Save purchase requisition ID for further usage
                idMap.put(purchReqID,purchReqID_SAP);
            }

            case "release purchase requisition (1)" -> {
                final String purchReqID = parseSetValue(params.get("purchase_requisition"))[0];
                final String  purchReqID_SAP = idMap.get(purchReqID);
                Map<String, String> sapParams = new HashMap<>();
                sapParams.put("NUMBER", purchReqID_SAP);
                sapParams.put("REL_CODE", "04");
                ExampleRepository.releasePurchaseReq(dest, sapParams);
            }
            case "release purchase requisition (2)" -> {
                final String purchReqID = parseSetValue(params.get("purchase_requisition"))[0];
                final String  purchReqID_SAP = idMap.get(purchReqID);
                Map<String, String> sapParams = new HashMap<>();
                sapParams.put("NUMBER", purchReqID_SAP);
                sapParams.put("REL_CODE", "02");
                ExampleRepository.releasePurchaseReq(dest, sapParams);
            }
            case "convert to purchase order" -> {
                // TODO: DATE
                final String DATE = LocalDate.parse(params.get("time").split(" ")[0]).format(DateTimeFormatter.ISO_LOCAL_DATE);
                final String purchReqID = parseSetValue(params.get("purchase_requisition"))[0];
                final String purchOrdID = parseSetValue(params.get("purchase_order"))[0];
                Map<String, String> sapParams = new HashMap<>();
                sapParams.put("COMP_CODE", "1000");
                sapParams.put("CREAT_DATE", DATE);
                sapParams.put("PURCH_ORG", "1000");
                sapParams.put("PUR_GROUP", "001");
                sapParams.put("PREQ_NO",idMap.get(purchReqID));
                String purchOrdID_SAP = ExampleRepository.convertPurchReqToOrder(dest, sapParams);
                idMap.put(purchOrdID,purchOrdID_SAP);
            }
            case "release purchase order" -> {
                final String purchOrdID = parseSetValue(params.get("purchase_order"))[0];
                Map<String, String> sapParams = new HashMap<>();
                sapParams.put("PURCHASEORDER",idMap.get(purchOrdID));
                sapParams.put("PO_REL_CODE","CE");
                ExampleRepository.releasePurchaseOrd(dest,sapParams);
            }

            default -> {
                System.err.println("Activity " + params.get("activity") + " is not known.");
                System.err.println(params.toString());
                return false;
            }
        }

        return true;
    }


    public static String[] parseSetValue(String value){
        String cleanedValue = value.replace("{","").replace("}","");
        return cleanedValue.split(",");
    }
}
