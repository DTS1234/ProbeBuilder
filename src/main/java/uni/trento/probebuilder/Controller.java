package uni.trento.probebuilder;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uni.trento.probebuilder.jmeter.JmeterResultData;
import uni.trento.probebuilder.jmeter.JmeterService;
import uni.trento.probebuilder.jmeter.JmeterSpecification;

import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class Controller {

    private final JmeterService jmeterService;

    @PostMapping("/run")
    @CrossOrigin
    public void jmeterRun(@RequestBody JmeterSpecification spec) throws IOException {
        if (spec == null) {
            jmeterService.jmeterStart(new JmeterSpecification(100, 20, "localhost", 8080, "/api/load", "POST", "{\"key1\":\"value1\"}"));
        } else {
            jmeterService.jmeterStart(spec);
        }
    }

    @GetMapping("/results")
    @CrossOrigin
    public Map<String, JmeterResultData> results() {
        return jmeterService.getAllResults();
    }
}
