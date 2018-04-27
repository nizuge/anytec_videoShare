package cn.anytec.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

@Component
public class SpringBootListener implements ApplicationListener<EmbeddedServletContainerInitializedEvent> {

    private int serverPort;
    private String hostIP;

    @Autowired
    GeneralConfig config;

    @Override
    public void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
        try {
            this.serverPort = event.getEmbeddedServletContainer().getPort();
            Enumeration allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip = null;
            while (allNetInterfaces.hasMoreElements()){
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
                System.out.println(netInterface.getName());
                Enumeration addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()){
                    ip = (InetAddress) addresses.nextElement();
                    if (ip != null && ip instanceof Inet4Address){
                        String hostAddress = ip.getHostAddress();
                        if(hostAddress.contains(config.getNetwork_segment())){
                            hostIP = hostAddress;
                            return;
                        }
                    }
                }
            }
        }  catch (SocketException e) {
            e.printStackTrace();
        }


    }
    public int getPort() {
        return this.serverPort;
    }
    public String getHostIP(){
        return hostIP;
    }

}



