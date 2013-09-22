package org.mdpnp.apps.testapp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.mdpnp.devices.serial.SerialProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {
    enum Application {
        ICE_Supervisor, ICE_Device_Interface;
    }

    public enum DeviceType {
        PO_Simulator(ice.ConnectionType.Simulated), NIBP_Simulator(ice.ConnectionType.Simulated), ECG_Simulator(
                ice.ConnectionType.Simulated), CO2_Simulator(ice.ConnectionType.Simulated), Temp_Simulator(
                ice.ConnectionType.Simulated), Pump_Simulator(ice.ConnectionType.Simulated), Bernoulli(
                ice.ConnectionType.Network), Ivy450C(ice.ConnectionType.Serial), Nonin(ice.ConnectionType.Serial), PhilipsMP70(
                ice.ConnectionType.Network), Dr\u00E4gerApollo(ice.ConnectionType.Serial), Dr\u00E4gerEvitaXL(
                ice.ConnectionType.Serial), Dr\u00E4gerV500(ice.ConnectionType.Serial), Capnostream20(
                ice.ConnectionType.Serial), NellcorN595(ice.ConnectionType.Serial), MasimoRadical7(
                ice.ConnectionType.Serial), Symbiq(ice.ConnectionType.Simulated), MultiPO_Simulator(
                ice.ConnectionType.Simulated);

        private final ice.ConnectionType connectionType;

        private DeviceType(ice.ConnectionType connectionType) {
            this.connectionType = connectionType;
        }

        public ice.ConnectionType getConnectionType() {
            return connectionType;
        }
    }

    private final Application application;
    private final DeviceType deviceType;
    private final String address;
    private final int domainId;

    public Configuration(Application application, int domainId, DeviceType deviceType, String address) {
        this.application = application;
        this.deviceType = deviceType;
        this.address = address;
        this.domainId = domainId;
    }

    public Application getApplication() {
        return application;
    }

    public int getDomainId() {
        return domainId;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public String getAddress() {
        return address;
    }

    private static final String APPLICATION = "application";
    private static final String DOMAIN_ID = "domainId";
    private static final String DEVICE_TYPE = "deviceType";
    private static final String ADDRESS = "address";

    public void write(OutputStream os) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, CHARACTER_ENCODING));
        bw.write(APPLICATION);
        bw.write("\t");
        bw.write(application.name());
        bw.write("\n");

        bw.write(DOMAIN_ID);
        bw.write("\t");
        bw.write(Integer.toString(domainId));
        bw.write("\n");

        if (null != deviceType) {
            bw.write(DEVICE_TYPE);
            bw.write("\t");
            bw.write(deviceType.name());
            bw.write("\n");
        }

        if (null != address) {
            bw.write(ADDRESS);
            bw.write("\t");
            bw.write(address);
            bw.write("\n");
        }

        bw.flush();
    }

    private final static Logger log = LoggerFactory.getLogger(Configuration.class);
    private static final String CHARACTER_ENCODING = "ISO8859_1";

    public static Configuration read(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, CHARACTER_ENCODING));

        String line = null;

        Application app = null;
        int domainId = 0;
        DeviceType deviceType = null;
        String address = null;

        while (null != (line = br.readLine())) {
            String[] v = line.split("\t");
            if (APPLICATION.equals(v[0])) {
                try {
                    app = Application.valueOf(v[1]);
                } catch (IllegalArgumentException iae) {
                    app = null;
                    log.warn("Ignoring unknown application type:" + v[1]);
                }
            } else if (DOMAIN_ID.equals(v[0])) {
                try {
                    domainId = Integer.parseInt(v[1]);
                } catch (NumberFormatException nfe) {
                    log.warn("Ignoring unknown domainId:" + v[1]);
                }
            } else if (DEVICE_TYPE.equals(v[0])) {
                try {
                    deviceType = DeviceType.valueOf(v[1]);
                } catch (IllegalArgumentException iae) {
                    deviceType = null;
                    log.warn("Ignoring unknown devicetype:" + v[1]);
                }
            } else if (ADDRESS.equals(v[0])) {
                if (v.length > 1) {
                    address = v[1];
                } else {
                    address = null;
                }
            }
        }

        return new Configuration(app, domainId, deviceType, address);
    }

    public static void help(Class<?> launchClass, PrintStream out) {
        out.println(launchClass.getName() + " [Application] [domainId] [DeviceType[=DeviceAddress]]");
        out.println();
        out.println("For interactive graphical interface specify no command line options");
        out.println();
        out.println("Application may be one of:");
        for (Application a : Application.values()) {
            out.println("\t" + a.name());
        }
        out.println();
        out.println("domainId must be a DDS domain identifier");
        out.println();

        out.println("if Application is " + Application.ICE_Device_Interface.name() + " then DeviceType may be one of:");
        for (DeviceType d : DeviceType.values()) {
            out.println("\t" + (ice.ConnectionType.Serial.equals(d.getConnectionType()) ? "*" : "") + d.name());
        }
        out.println("DeviceAddress is an optional string configuring the address of the device");
        out.println();
        out.println("DeviceTypes marked with * are serial devices for which the following DeviceAddress values are currently valid:");
        for (String s : SerialProviderFactory.getDefaultProvider().getPortNames()) {
            out.println("\t" + s);
        }
    }

    public static Configuration read(String[] args_) {
        Application app = null;
        int domainId = 0;
        DeviceType deviceType = null;
        String address = null;

        List<String> args = new ArrayList<String>(Arrays.asList(args_));
        ListIterator<String> litr = args.listIterator();
        while (litr.hasNext()) {
            try {
                app = Application.valueOf(litr.next());
                litr.remove();
                break;
            } catch (IllegalArgumentException iae) {

            }
        }
        if (null == app) {
            return null;
        }

        if (Application.ICE_Device_Interface.equals(app)) {
            litr = args.listIterator();
            while (litr.hasNext()) {
                try {
                    String x = litr.next();
                    String[] y = x.split("\\=");
                    deviceType = DeviceType.valueOf(y[0]);
                    litr.remove();
                    if (y.length > 1) {
                        address = y[1];
                    }
                    break;
                } catch (IllegalArgumentException iae) {

                }
            }
        }

        litr = args.listIterator();
        while (litr.hasNext()) {
            try {
                String x = litr.next();
                try {
                    domainId = Integer.parseInt(x);
                    break;
                } catch (NumberFormatException nfe) {

                }

            } catch (IllegalArgumentException iae) {

            }
        }

        return new Configuration(app, domainId, deviceType, address);
    }
}