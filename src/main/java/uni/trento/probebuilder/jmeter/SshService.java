package uni.trento.probebuilder.jmeter;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@Slf4j
public class SshService {

    public static final String HOSTNAME_A = "10.196.37.164";
    public static final String USERNAME_A = "adam";
    public static final int PORT_A = 22;
    public static final String PASSWORD_A = "adamtest";

    public void startPodsRecording(JmeterSpecification specification) {
        String commandToRecluster = "ssh root@recluster ./pod.sh" + (specification.getDuration() + 10);
        sshCommand(HOSTNAME_A, PORT_A, USERNAME_A, PASSWORD_A, commandToRecluster);
    }

    public void setHpa() {
        String commandToRecluster = "ssh root@recluster ./hpa.sh";
        sshCommand(HOSTNAME_A, PORT_A, USERNAME_A, PASSWORD_A, commandToRecluster);
    }

    public void descaleCluster() {
        String commandToRecluster = "ssh root@recluster ./downscale.sh";
        String result = sshCommand(HOSTNAME_A, PORT_A, USERNAME_A, PASSWORD_A, commandToRecluster);
    }

    public void unassignWorkerNodes() {
        List<String> workerNodeNames = List.of("worker.07180037-9a1e-4b60-91aa-d83070de3371", "worker.30cbec7e-0976-41b3-b9b1-89f9047cf000",
            "worker.57eb9fe0-6f46-4f41-84ad-aac60721ee86", "worker.a0c81216-76dd-4f23-8449-feeaadd69d0e");

        for (String node : workerNodeNames) {
            String commandToRecluster = "ssh root@recluster ./unassign_node.sh " + node.replaceAll("worker.", "");
            sshCommand(HOSTNAME_A, PORT_A, USERNAME_A, PASSWORD_A, commandToRecluster);
        }
    }

    public void savePodsData(JmeterSpecification spec) {
        List<String> nodesNames = List.of("worker.07180037-9a1e-4b60-91aa-d83070de3371", "worker.30cbec7e-0976-41b3-b9b1-89f9047cf000",
            "worker.57eb9fe0-6f46-4f41-84ad-aac60721ee86", "worker.a0c81216-76dd-4f23-8449-feeaadd69d0e", "controller.8c7818dc-53ec-477c-a97b-c74337c5c1a8");

        for (String node : nodesNames) {
            String commandToRecluster = String.format("ssh root@recluster 'cat %s_pod_counts.csv'", node);
            String result = sshCommand(HOSTNAME_A, PORT_A, USERNAME_A, PASSWORD_A, commandToRecluster);
            saveFile(spec, node, result);
        }
    }

    private static void saveFile(JmeterSpecification spec, String node, String result) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(spec.getTestName()+"|"+mapNodeName(node)+".csv"))) {
            writer.write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String mapNodeName(String node) {
        return switch (node) {
            case "worker.07180037-9a1e-4b60-91aa-d83070de3371" -> "worker-1";
            case "worker.30cbec7e-0976-41b3-b9b1-89f9047cf000" -> "worker-2";
            case "worker.57eb9fe0-6f46-4f41-84ad-aac60721ee86" -> "worker-3";
            case "worker.a0c81216-76dd-4f23-8449-feeaadd69d0e" -> "worker-4";
            default -> "controller";
        };
    }

    public String sshCommand(String hostname, int port, String username, String password, String command) {
        JSch jsch = new JSch();
        Session session = null;
        Channel channel = null;

        StringBuilder outputBuffer = new StringBuilder();
        try {
            session = jsch.getSession(username, hostname, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            InputStream commandOutput = channel.getInputStream();
            channel.connect();
            int readByte = commandOutput.read();
            while (readByte != 0xffffffff) {
                outputBuffer.append((char) readByte);
                readByte = commandOutput.read();
            }
            channel.disconnect();
            session.disconnect();
        } catch (JSchException | IOException e) {
            e.printStackTrace(); // or log it, or re-throw as appropriate
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
        return outputBuffer.toString();
    }

}
