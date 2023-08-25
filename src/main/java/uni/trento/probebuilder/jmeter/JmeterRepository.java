package uni.trento.probebuilder.jmeter;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Component
public class JmeterRepository {

    final Map<String, JmeterResultData> resultsMap = new HashMap<>();

    public void save(JmeterResultData resultData) {
        resultsMap.put(resultData.getDate(), resultData);
    }

    public Map<String, JmeterResultData> getResults() {

        Comparator<String> comparator = getDateFromStringComparator();

        Map<String, JmeterResultData> sortedResults = new TreeMap<>(comparator);
        sortedResults.putAll(resultsMap);

        return sortedResults;
    }

    @NotNull
    private static Comparator<String> getDateFromStringComparator() {
        return (String s1, String s2) -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            boolean isBefore = LocalDateTime.parse(s1, formatter).isBefore(LocalDateTime.parse(s2, formatter));
            boolean isAfter = LocalDateTime.parse(s1, formatter).isAfter(LocalDateTime.parse(s2, formatter));

            if (isBefore) {
                return 1;
            } else if (isAfter) {
                return -1;
            } else {
                return 0;
            }
        };
    }
}
