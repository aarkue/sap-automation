import java.util.Map;
import java.util.Properties;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;




public class ExampleSAPConnector {
    private JCoDestination dest;

    public ExampleSAPConnector(Map<String,String> sapCredential) {
        Properties pp = getJcoProperties(sapCredential);
        // wrapping Properties
        PropertiesDestinationDataProvider pddp = new PropertiesDestinationDataProvider(pp);
        // registration
        Environment.registerDestinationDataProvider(pddp);

        try {
            this.dest = getDestination();
        } catch(JCoException e){
            e.printStackTrace();
        }
    }

    protected static Properties getRouterProperties(Map<String,String> sapCredential) {
        final Properties cp = new Properties();

        // My connection string:
        // conn=/H/saprouter.myhost/S/3297/H/I72z&amp;user=wb&amp;cInt=900
        cp.setProperty(DestinationDataProvider.JCO_ASHOST, sapCredential.get("host"));
        cp.setProperty(DestinationDataProvider.JCO_CLIENT, sapCredential.get("client"));
        cp.setProperty(DestinationDataProvider.JCO_USER, sapCredential.get("user"));
        cp.setProperty(DestinationDataProvider.JCO_PASSWD, sapCredential.get("passwd"));
        cp.setProperty(DestinationDataProvider.JCO_SYSNR, sapCredential.get("sysnr"));
        cp.setProperty(DestinationDataProvider.JCO_LANG, sapCredential.get("lang"));

        return cp;
    }

    protected static Properties getJcoProperties(Map<String,String> sapCredential) {
        return getRouterProperties(sapCredential);
    }

    public static JCoDestination getDestination() throws JCoException {
        // we do provide support for multiple destination
        // in our PropertiesDestinationDataProvider
        // so it does not matter what we give as an argument
        // to getDestination
        return JCoDestinationManager.getDestination("whatever");
    }


    public JCoDestination getMyJcoDestination() {
        return this.dest;
    }
}
