package uni.trento.probebuilder.jmeter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportData {
    private String filePath;
    private String errorCount;
    private String meanLatency;
}
