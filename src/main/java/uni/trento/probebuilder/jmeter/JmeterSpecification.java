package uni.trento.probebuilder.jmeter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JmeterSpecification {

    private String testName;
    private int numberOfThreads;
    private int rampUpPeriod;
    private String ip;
    private int port;
    private String path;
    private String method;
    private String body;
    private int duration;

}
