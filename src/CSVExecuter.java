import com.opencsv.*;
import com.opencsv.exceptions.CsvException;
import com.sap.conn.jco.JCoContext;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CSVExecuter {
    private final File csv;
    private Map<String,String> idMap = new HashMap<>();
    private Map<String,String> reservations = new HashMap<>();
    private Map<String,Set<String>> stillRequiredForProdOrd = new HashMap<>();
    private Map<String,Set<Pair<String,Integer>>> waitingForGI = new HashMap<>();
    private Map<String,Map<String,Integer>> requiredForProdOrdIndex = new HashMap<>();
    private CSVReaderHeaderAware csvReader;
    private int numberOfEvents;
    public CSVExecuter(File csv){
        try {
            FileReader reader = new FileReader(csv);
            CSVParser csvParserSemiSep = new CSVParserBuilder().withSeparator(';').build();
            csvReader = new CSVReaderHeaderAwareBuilder(reader).withCSVParser(csvParserSemiSep).build();

            // Get line count by counting all lines with a buffered reader (sadly opencsv does not have a corresponding own method)
            // This also assumes that the number of lines in the file (minus one for the headers) corresponds to the number of events
            BufferedReader bufferedReader = new BufferedReader(new FileReader((csv)));
            int lineCount = 0;
            while (bufferedReader.readLine() != null) {
                lineCount++;
            }
            numberOfEvents = lineCount - 1;
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

                int i = 1;
                do {
                    //for each line
                    Map<String, String> map = csvReader.readMap();

                    if (map != null) {
                        if (executeOne(repo, dest, map)) {
                            long timeDifference;
                            Date date = sdf.parse((map.get(CSVField.TIME)));
                            if (previousDate == null) {
                                timeDifference = 0;
                            } else {
                                long diff = date.getTime() - previousDate.getTime();
                                timeDifference = TimeUnit.MILLISECONDS.toHours(diff);
                                System.out.println(diff + "; " + timeDifference);
                            }
                            previousDate = date;
                            commitAndWait(dest, (timeDifference / 5) + 1);
                            double percentageDone = (i / (double) numberOfEvents) * 100;
                            System.out.println("------ Executed Event " + i++ + " of " + numberOfEvents + " (" + (Math.round(percentageDone * 100.0) / 100.0) + "%) ------");
                            printFancyProgress(percentageDone);
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
        final String DATE = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        switch (params.get(CSVField.ACTIVITY)){
            case "create production order" -> {
                // TODO: END_DATE ?
                final String END_DATE = LocalDate.now().plus(20, ChronoUnit.DAYS).format(DateTimeFormatter.ISO_LOCAL_DATE);
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
                HashMap<String,Integer> indices = new HashMap<>();
                String[] reqMats = parseSetValue(params.get(CSVField.REQUIRED_MATERIAL));
                for (int i = 0; i < reqMats.length; i++) {
                    indices.put(reqMats[i],i+1);
                }
                requiredForProdOrdIndex.put(prodOrderID,indices);

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
                sapParams.put("PURCHASEORDER", idMap.get(purchOrdID));
                sapParams.put("PO_REL_CODE", "CE");

                logExecution(params.get(CSVField.ACTIVITY), sapParams, params);
                ExampleRepository.releasePurchaseOrd(dest, sapParams);
            }
            case "reject purchase order" -> {
                final String purchOrdID = parseSetValue(params.get(CSVField.PURCHASE_ORDER))[0];
                Map<String, String> sapParams = new HashMap<>();
                sapParams.put("PURCHASEORDER", idMap.get(purchOrdID));
                sapParams.put("PO_REL_CODE", "RE"); // RE: Reject/Block

                logExecution(params.get(CSVField.ACTIVITY), sapParams, params);
                ExampleRepository.releasePurchaseOrd(dest, sapParams);
            }
            case "reconsider purchase order" -> { // Reconsider = Reset blocked/rejected status of PO
                final String purchOrdID = parseSetValue(params.get(CSVField.PURCHASE_ORDER))[0];
                Map<String, String> sapParams = new HashMap<>();
                sapParams.put("PURCHASEORDER", idMap.get(purchOrdID));
                sapParams.put("PO_REL_CODE", "RE"); // RE: Reject/Block

                logExecution(params.get(CSVField.ACTIVITY), sapParams, params);
                ExampleRepository.resetPurchaseOrdRelease(dest, sapParams);
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
                final String reqMatName = parseSetValue(params.get(CSVField.REQUIRED_MATERIAL))[0];
                final String reqMatAmount = parseSetValue(params.get(CSVField.AMOUNT))[0];
                stillRequiredForProdOrd.get(prodOrderID).remove(reqMatName);

                Set<Pair<String,Integer>> reqMatSet = waitingForGI.getOrDefault(prodOrderID,new HashSet<>());
                reqMatSet.add(new Pair<>(reqMatName,Integer.parseInt(reqMatAmount)));
                waitingForGI.put(prodOrderID,reqMatSet);

                if(stillRequiredForProdOrd.get(prodOrderID).isEmpty()) {
                    Map<String, String> sapParams = new HashMap<>();
                    sapParams.put("RESERV_NO", reservations.get(prodOrderID));
                    sapParams.put("ORDERID", idMap.get(prodOrderID));
                    sapParams.put("PSTNG_DATE", DATE);
                    sapParams.put("DOC_DATE", DATE);
                    sapParams.put("PLANT", "1000");
                    sapParams.put("STGE_LOC", "0001");
                    for (Pair<String,Integer> reqMat : reqMatSet) {
                        final Integer index = requiredForProdOrdIndex.get(prodOrderID).get(reqMat.a);
                        sapParams.put("MATERIAL" + index, reqMat.a);
                        sapParams.put("ENTRY_QNT" + index, reqMat.b.toString());
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

    private void logExecution(String activity, Map<String, String> sapParams, Map<String, String> params) {
        System.out.println("EXECUTING " + activity + " with SAP params: " + sapParams + "\t | \t(LOG: " + params + ")");
        System.out.println("idMap: " + idMap.toString() + " \t reservations: " + reservations.toString() + " \t requiredForProdOrdIndex: " + requiredForProdOrdIndex.toString() + " \t stillRequiredForProdOrd: " + stillRequiredForProdOrd.toString());
    }

    private String genRandom() {
        return Math.round(Math.random() * 10000) + "";
    }

    public void printFancyProgress(double percentage) {
        int numberOfCharsComplete = 150;
        int numberOfChars = (int) (numberOfCharsComplete * (percentage / 100));
        int numberOfBlanks = numberOfCharsComplete - numberOfChars;
        System.out.print("|");
        for (int i = 0; i < numberOfCharsComplete; i++) {
            if (i < numberOfChars) {
                System.out.print("=");
            } else {
                System.out.print(" ");
            }
        }
        System.out.println("| (" + (Math.round(percentage * 100.0) / 100.0) + "%)");


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

    protected class Pair<A,B> {
        public final A a;
        public final B b;
        public Pair(A a, B b){
            this.a = a;
            this.b = b;
        }
    }
}
