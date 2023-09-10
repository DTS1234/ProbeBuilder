package uni.trento.probebuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uni.trento.probebuilder.jmeter.ConfigurationService;
import uni.trento.probebuilder.jmeter.JmeterResultData;
import uni.trento.probebuilder.jmeter.JmeterService;
import uni.trento.probebuilder.jmeter.JmeterSpecification;

import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
public class Controller {

    private final JmeterService jmeterService;
    private final ConfigurationService configurationService;

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

    @PostMapping("/path")
    @CrossOrigin
    public Map<String, String> setJmeterPath(@RequestBody Map<String, String> path) {
        log.info(path.toString());
        ConfigurationService.JMETER_HOME = path.get("path");

        return Map.of("message", "File set to : " + path.get("path"));
    }

}
