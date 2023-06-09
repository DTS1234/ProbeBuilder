package uni.trento.probebuilder.jmeter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JmeterResultData {
    private String date;
    private String jtlFile;
    private String jmxFile;
    private String numberOfThreads;
    private String rampUpPeriod;
    private String testType;
    private boolean done;
}
