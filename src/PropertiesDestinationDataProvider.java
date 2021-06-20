
import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;

import java.util.Properties;

public class PropertiesDestinationDataProvider implements DestinationDataProvider {

  private Properties props;

  public PropertiesDestinationDataProvider(Properties props) {
    this.props = props;
  }
    
  @Override
  public Properties getDestinationProperties(String destinationName) {
    return this.props;
  }

  @Override
  public void setDestinationDataEventListener(DestinationDataEventListener arg0) {
    // nothing to do
  }

  @Override
  public boolean supportsEvents() {
    return false;
  }
}