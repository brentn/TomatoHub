package com.brentandjody.tomatohub.routers.connection;

import org.apache.commons.net.telnet.TelnetClient;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;

/**
 * Created by brentn on 22/01/16.
 */
public class TelnetSession {

    private TelnetClient telnet = new TelnetClient();
    private InputStream in;
    private PrintStream out;
    private String prompt = "# ";

    public TelnetSession(String server, String user, String password) throws Exception {
        telnet.connect(server, 23);
        in = telnet.getInputStream();
        out = new PrintStream(telnet.getOutputStream());

        readUntil("ogin: ");
        write(user);
        readUntil("assword: ");
        write(password);
        readUntil(prompt);
        write("stty -echo");
        readUntil(prompt);
    }

    public String readUntil(String pattern) throws Exception {
        char lastChar = pattern.charAt(pattern.length() - 1);
        StringBuilder sb = new StringBuilder();
        char ch = (char) in.read();
        while (true) {
            sb.append(ch);
            //System.out.print(ch);
            if (sb.toString().endsWith("Closing connection")) {
                throw new Exception("Wrong Telnet arguments passed");
            }
            if (sb.toString().endsWith(prompt) && !pattern.equals(prompt)) {
                return sb.toString();
            }
            if (ch == lastChar) {
                if (sb.toString().endsWith(pattern)) {
                    return sb.toString();
                }
            }
            ch = (char) in.read();
        }
    }

    public void write(String value) {
        try {
            out.println(value);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String[] sendCommand(String command) {
        try {
            write(command);
            String[] result = readUntil(prompt).split("\n");
            return Arrays.copyOfRange(result, 0, result.length - 1); //remove prompt from end
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void disconnect() {
        try {
            telnet.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}