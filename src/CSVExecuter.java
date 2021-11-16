import com.opencsv.*;
import com.opencsv.exceptions.CsvValidationException;
import com.sap.conn.jco.JCoContext;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CSVExecuter {
    private final File csv;
    private Map<String,String> idMap = new HashMap<>();
    private Map<String,String> reservations = new HashMap<>();
    private Map<String,Set<String>> stillRequiredForProdOrd = new HashMap<>();
    private Map<String,Set<String>> requiredForProdOrd = new HashMap<>();
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
        try {
            try {

                JCoContext.begin(dest);

                //read csv

                boolean csvFinished = false;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date previousDate = null;
                do{
                    //for each line
                    Map<String, String> map = csvReader.readMap();

                    if(map != null) {
                        if (executeOne(repo, dest, map)) {
                            long timeDifference;
                            Date date = sdf.parse((map.get(CSVField.TIME)));
                            if(previousDate == null){
                                timeDifference = 0;
                            }else{
                                long diff =  date.getTime() - previousDate.getTime();
                                timeDifference = TimeUnit.MILLISECONDS.toHours(diff);
                                System.out.println(diff + "; " + timeDifference);
                            }
                            previousDate = date;
                            commitAndWait(dest,(timeDifference*0)+3);
                        }else{
                            return false;
                        }
                    }else{
                        commitAndWait(dest,0);
                        csvFinished = true;
                    }
                }while(!csvFinished);
                System.out.println("idMap: " + idMap.toString());
                System.out.println("reservations: " + reservations.toString());

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
        // TODO: DATE
        final String DATE = LocalDate.parse(params.get(CSVField.TIME).split(" ")[0]).format(DateTimeFormatter.ISO_LOCAL_DATE);
        switch (params.get(CSVField.ACTIVITY)){
            case "create production order" -> {
                // TODO: END_DATE ?
                final String END_DATE = LocalDate.parse(params.get(CSVField.TIME).split(" ")[0]).plus(7, ChronoUnit.DAYS).format(DateTimeFormatter.ISO_LOCAL_DATE);
                final String prodOrderID = parseSetValue(params.get(CSVField.PRODUCTION_ORDER))[0];
                Map<String, String> sapParams = new HashMap<>();
                sapParams.put("MATERIAL", parseSetValue(params.get(CSVField.MATERIAL))[0]);
                sapParams.put("PLANT", "1000");
                sapParams.put("PLANNING_PLANT", "1000");
                sapParams.put("ORDER_TYPE", "PP01");
                sapParams.put("QUANTITY", parseSetValue(params.get(CSVField.AMOUNT))[0]);
                sapParams.put("BASIC_END_DATE", END_DATE);
                String prodOrderID_SAP  = ExampleRepository.createProductionOrder(dest,sapParams);
                // Save production order & reservation ID for further usage
                idMap.put(prodOrderID,prodOrderID_SAP);

                Set<String> reqSet = new HashSet<>();
                reqSet.addAll(Arrays.asList(parseSetValue(params.get(CSVField.REQUIRED_MATERIAL))));
                requiredForProdOrd.put(prodOrderID,reqSet);

                Set<String> stillReqSet = new HashSet<>();
                stillReqSet.addAll(Arrays.asList(parseSetValue(params.get(CSVField.REQUIRED_MATERIAL))));
                stillRequiredForProdOrd.put(prodOrderID,stillReqSet);

                logExecution(params.get(CSVField.ACTIVITY),sapParams,params);
                String reservationID_SAP = ExampleRepository.getReservationOfProOrder(dest,prodOrderID_SAP);
                reservations.put(prodOrderID,reservationID_SAP);
            }
            case "create purchase requisition" -> {
                final String purchReqID = parseSetValue(params.get(CSVField.PURCHASE_REQUISITION))[0];
                Map<String, String> sapParams = new HashMap<>();
                sapParams.put("MATERIAL", parseSetValue(params.get(CSVField.REQUIRED_MATERIAL))[0]);
                sapParams.put("QUANTITY", parseSetValue(params.get(CSVField.AMOUNT))[0]);

                logExecution(params.get(CSVField.ACTIVITY),sapParams,params);
                String purchReqID_SAP = ExampleRepository.createPurReq(dest, sapParams);
                // Save purchase requisition ID for further usage
                idMap.put(purchReqID,purchReqID_SAP);
            }

            case "release purchase requisition (1)" -> {
                final String purchReqID = parseSetValue(params.get(CSVField.PURCHASE_REQUISITION))[0];
                final String  purchReqID_SAP = idMap.get(purchReqID);
                Map<String, String> sapParams = new HashMap<>();
                sapParams.put("NUMBER", purchReqID_SAP);
                sapParams.put("REL_CODE", "01");

                logExecution(params.get(CSVField.ACTIVITY),sapParams,params);
                ExampleRepository.releasePurchaseReq(dest, sapParams);
            }
            case "release purchase requisition (2)" -> {
                final String purchReqID = parseSetValue(params.get(CSVField.PURCHASE_REQUISITION))[0];
                final String  purchReqID_SAP = idMap.get(purchReqID);
                Map<String, String> sapParams = new HashMap<>();
                sapParams.put("NUMBER", purchReqID_SAP);
                sapParams.put("REL_CODE", "03");

                logExecution(params.get(CSVField.ACTIVITY),sapParams,params);
                ExampleRepository.releasePurchaseReq(dest, sapParams);
            }
            case "convert to purchase order" -> {
                final String purchReqID = parseSetValue(params.get(CSVField.PURCHASE_REQUISITION))[0];
                final String purchOrdID = parseSetValue(params.get(CSVField.PURCHASE_ORDER))[0];
                Map<String, String> sapParams = new HashMap<>();
                sapParams.put("COMP_CODE", "1000");
                sapParams.put("CREAT_DATE", DATE);
                sapParams.put("PURCH_ORG", "1000");
                sapParams.put("PUR_GROUP", "001");
                sapParams.put("PREQ_NO",idMap.get(purchReqID));

                logExecution(params.get(CSVField.ACTIVITY),sapParams,params);
                String purchOrdID_SAP = ExampleRepository.convertPurchReqToOrder(dest, sapParams);
                idMap.put(purchOrdID,purchOrdID_SAP);
            }
            case "release purchase order" -> {
                final String purchOrdID = parseSetValue(params.get(CSVField.PURCHASE_ORDER))[0];
                Map<String, String> sapParams = new HashMap<>();
                sapParams.put("PURCHASEORDER",idMap.get(purchOrdID));
                sapParams.put("PO_REL_CODE","CE");

                logExecution(params.get(CSVField.ACTIVITY),sapParams,params);
                ExampleRepository.releasePurchaseOrd(dest,sapParams);
            }
            case "goods receipt for purchase order" -> {
                final String purchOrdID = parseSetValue(params.get(CSVField.PURCHASE_ORDER))[0];
                Map<String, String> sapParams = new HashMap<>();
                sapParams.put("PSTNG_DATE", DATE);
                sapParams.put("DOC_DATE", DATE);
                sapParams.put("PO_NUMBER", idMap.get(purchOrdID));
                sapParams.put("PO_ITEM", "10");
                sapParams.put("PLANT", "1000");
                sapParams.put("STGE_LOC", "0001");
                sapParams.put("ENTRY_QNT", parseSetValue(params.get(CSVField.AMOUNT))[0]);

                logExecution(params.get(CSVField.ACTIVITY),sapParams,params);
                ExampleRepository.goodsReceiptForPurchOrd(dest, sapParams);
            }

            case "goods issue for production order" -> {
                final String prodOrderID = parseSetValue(params.get(CSVField.PRODUCTION_ORDER))[0];
                final String[] reqMat = parseSetValue(params.get(CSVField.REQUIRED_MATERIAL));
                stillRequiredForProdOrd.get(prodOrderID).remove(reqMat[0]);
                if(stillRequiredForProdOrd.get(prodOrderID).isEmpty()) {
                    final String[] requiredMaterials = requiredForProdOrd.get(prodOrderID).toArray(new String[]{});
                    Map<String, String> sapParams = new HashMap<>();
                    sapParams.put("RESERV_NO", reservations.get(prodOrderID));
                    sapParams.put("ORDERID", idMap.get(prodOrderID));
                    sapParams.put("PSTNG_DATE", DATE);
                    sapParams.put("DOC_DATE", DATE);
                    sapParams.put("PLANT", "1000");
                    sapParams.put("STGE_LOC", "0001");
                    for (int i = 0; i < requiredMaterials.length; i++) {
                        sapParams.put("MATERIAL" + (i + 1), requiredMaterials[i]);
                        //TODO: Assumes that all amounts are always equal
                        sapParams.put("ENTRY_QNT" + (i + 1), parseSetValue(params.get(CSVField.AMOUNT))[0]);
                    }

                    logExecution(params.get(CSVField.ACTIVITY), sapParams, params);
                    ExampleRepository.goodIssueForProd(dest, sapParams);
                }
            }
            case "confirm production order" -> {
                final String prodOrderID = parseSetValue(params.get(CSVField.PRODUCTION_ORDER))[0];
                Map<String, String> sapParams = new HashMap<>();
                sapParams.put("POSTG_DATE", DATE);
                sapParams.put("ORDERID", idMap.get(prodOrderID));
                sapParams.put("FIN_CONF", "X");
                sapParams.put("YIELD", parseSetValue(params.get(CSVField.AMOUNT))[0]);
                sapParams.put("SCRAP", "0");

                logExecution(params.get(CSVField.ACTIVITY),sapParams,params);
                ExampleRepository.confirmProdOrd(dest, sapParams);
            }
            default -> {
                System.err.println("Activity " + params.get(CSVField.ACTIVITY) + " is not known.");
                System.err.println(params);
                return false;
            }
        }

        return true;
    }


    public static String[] parseSetValue(String value){
        String cleanedValue = value.replace("{","").replace("}","");
        return cleanedValue.split(",");
    }

    private void logExecution(String activity, Map<String,String> sapParams, Map<String,String> params){
        System.out.println("EXECUTING " + activity + " with SAP params: " + sapParams + "\t | \t(LOG: " + params + ")");
        System.out.println("idMap: " + idMap.toString() + " \t reservations: " + reservations.toString() + " \t requiredForProdOrd: " + requiredForProdOrd.toString() + " \t stillRequiredForProdOrd: " + stillRequiredForProdOrd.toString());
    }

    private String genRandom(){
        return Math.round(Math.random()*10000) + "";
    }


    public static void commitAndWait(JCoDestination dest, long durationSeconds) throws JCoException {
        ExampleRepository.commitTrans(dest);
        try {
            System.out.println("Waiting " + durationSeconds + "s before continuing");
            TimeUnit.SECONDS.sleep(durationSeconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
