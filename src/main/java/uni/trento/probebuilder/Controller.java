package uni.trento.probebuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uni.trento.probebuilder.jmeter.ConfigurationService;
import uni.trento.probebuilder.jmeter.JmeterResultData;
import uni.trento.probebuilder.jmeter.JmeterService;
import uni.trento.probebuilder.jmeter.JmeterSpecification;
import uni.trento.probebuilder.jmeter.SshService;
import uni.trento.probebuilder.jmeter.TestSchedulerService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
public class Controller {

    private final JmeterService jmeterService;
    private final TestSchedulerService testSchedulerService;
    private final SshService sshService;

    @PostMapping("/run")
    @CrossOrigin
    public void jmeterRun(@RequestBody JmeterSpecification spec) throws IOException {
        if (spec == null) {
            jmeterService.jmeterStart(new JmeterSpecification("Default test", 100, 20, "localhost", 8080, "/api/load", "POST", "{\"key1\":\"value1\"}", 60));
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

    @PostMapping("/schedule/add")
    @CrossOrigin
    public List<JmeterSpecification> addToSchedule(@RequestBody JmeterSpecification specification) {
        testSchedulerService.addToTheSchedule(specification);
        return this.testSchedulerService.getCurrentSchedule();
    }

    @GetMapping("/schedule")
    @CrossOrigin
    public List<JmeterSpecification> getSchedule() {
        return this.testSchedulerService.getCurrentSchedule();
    }

    @PostMapping("/schedule/remove/{index}")
    @CrossOrigin
    public List<JmeterSpecification> removeFromSchedule(@PathVariable Integer index) {
        this.testSchedulerService.removeFromTheSchedule(index);
        return testSchedulerService.getCurrentSchedule();
    }

    @PostMapping("/schedule/execute")
    @CrossOrigin
    public void execute() throws IOException, ExecutionException, InterruptedException {
        this.testSchedulerService.executeSchedule();
    }

    @PostMapping("/downscale")
    @CrossOrigin
    public void downscale() {
        sshService.descaleCluster();
        sshService.unassignWorkerNodes();
    }

    @PostMapping("/save/pods")
    @CrossOrigin
    public void savePods(@RequestBody JmeterSpecification specification) {
        sshService.savePodsData(specification);
    }


    @PostMapping("/submit/schedule")
    public void submitSchedul(@RequestBody List<JmeterSpecification> spec) {
        this.testSchedulerService.getCurrentSchedule().addAll(spec);
    }
}
