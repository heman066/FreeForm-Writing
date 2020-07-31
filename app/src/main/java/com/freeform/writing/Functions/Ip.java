package com.freeform.writing.Functions;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class Ip {

    public static byte[] ipadd() {
        try {

            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        if (inetAddress instanceof Inet4Address) { // fix for Galaxy Nexus. IPv4 is easy to use :-)
                            return inetAddress.getAddress();
                        }
                        //return inetAddress.getHostAddress().toString(); // Galaxy Nexus returns IPv6
                    }
                }
            }
        } catch (SocketException ex)

        {
            ex.printStackTrace();
        }

        return null;
    }

    public static String getDottedDecimalIP(byte[] ipAddr) {
        //convert to dotted decimal notation:
        if (ipAddr == null)
            return "";
        StringBuilder ipAddrStr = new StringBuilder();
        for (int i = 0; i < ipAddr.length; i++) {
            if (i > 0) {
                ipAddrStr.append(".");
            }
            ipAddrStr.append(ipAddr[i] & 0xFF);
        }
        return ipAddrStr.toString();
    }
}
