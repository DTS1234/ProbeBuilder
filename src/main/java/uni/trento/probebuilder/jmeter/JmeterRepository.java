package uni.trento.probebuilder.jmeter;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JmeterRepository {

    final Map<String, JmeterResultData> resultsMap = new HashMap<>();

    public void save(JmeterResultData resultData) {
        resultsMap.put(UUID.randomUUID().toString(), resultData);
    }

    public Map<String, JmeterResultData> getResults() {
        return resultsMap;
    }
}
