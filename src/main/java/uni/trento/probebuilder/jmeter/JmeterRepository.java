package uni.trento.probebuilder.jmeter;

import net.sf.saxon.expr.parser.Loc;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Component
public class JmeterRepository {

    final Map<String, JmeterResultData> resultsMap = new HashMap<>();

    public void save(JmeterResultData resultData) {
        resultsMap.put(resultData.getDate(), resultData);
    }

    public Map<String, JmeterResultData> getResults() {

        Comparator<String> comparator = (String s1, String s2) -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            boolean after = LocalDateTime.parse(s1, formatter).isBefore(LocalDateTime.parse(s2, formatter));

            if (after) {
                return 1;
            } else {
                return -1;
            }
        };

        Map sortedResults = new TreeMap<>(comparator);
        sortedResults.putAll(resultsMap);

        return sortedResults;
    }
}
