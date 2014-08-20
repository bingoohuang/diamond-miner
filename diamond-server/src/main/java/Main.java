import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;

import java.net.URL;
import java.security.ProtectionDomain;

public class Main {
    public static void main(String[] args) throws Exception {
        Server server = new Server();

        SelectChannelConnector connector = new SelectChannelConnector();
        int port = Integer.parseInt(System.getProperty("port", "17002"));
        connector.setPort(port);
        server.addConnector(connector);

        ProtectionDomain domain = Main.class.getProtectionDomain();
        URL location = domain.getCodeSource().getLocation();
        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/diamond-server");
        webapp.setWar(location.toExternalForm());
        server.setHandler(webapp);

        server.start();
        server.join();

    }
}