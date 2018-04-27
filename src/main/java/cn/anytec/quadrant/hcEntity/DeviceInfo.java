package cn.anytec.quadrant.hcEntity;

import cn.anytec.util.RuntimeLocal;
import com.sun.jna.NativeLong;

public class DeviceInfo {
    private String deviceIp;
    private String userName;
    private String password;
    private short port;
    private NativeLong deviceId;
    private int status;//0近景，1远景

    public DeviceInfo(String deviceIp,String userName,String password,short port,int status){
        this.deviceIp = deviceIp;
        this.userName = userName;
        this.password = password;
        this.port = port;
        this.status = status;
    }

    public String getDeviceIp() {
        return deviceIp;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public short getPort() {
        return port;
    }

    public NativeLong getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(NativeLong deviceId) {
        this.deviceId = deviceId;
    }

    public int getStatus() {
        return status;
    }

    public static void main(String[] args) {
        
    }
}
