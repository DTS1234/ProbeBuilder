package uni.trento.probebuilder.jmeter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Component
public class TestSchedulerService {

    private final List<JmeterSpecification> schedule = new ArrayList<>();
    private final JmeterService service;

    private final SshService sshService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();


    public TestSchedulerService(JmeterService service, SshService sshService) {
        this.service = service;
        this.sshService = sshService;
    }

    public List<JmeterSpecification> getCurrentSchedule() {
        return this.schedule;
    }

    public void addToTheSchedule(JmeterSpecification spec) {
        this.schedule.add(spec);
    }

    public void addToTheSchedule(JmeterSpecification spec, int index) {
        this.schedule.add(index, spec);
    }

    public void removeFromTheSchedule(int index) {
        if (this.schedule.size() < index || index < 0) {
            throw new IllegalStateException("Wrong index for schedule removal");
        }
        this.schedule.remove(index);
    }

    public void popDone() {
        if (this.schedule.size() >= 1) {
            this.schedule.remove(this.schedule.size() - 1);
        } else {
            throw new IllegalStateException("No items in schedule");
        }
    }

    public void executeSchedule() throws ExecutionException, InterruptedException {
        for (JmeterSpecification spec : schedule) {
            Future<?> future = runWithSpec(spec);
            future.get();
        }
        shutdown();
    }

    private Future<?> runWithSpec(JmeterSpecification spec) {
        return executor.submit(() -> {
            try {
                // before
                sshService.setHpa();
                sshService.startPodsRecording(spec);

                // test
                service.jmeterStart(spec);

                // after
                // sshService.savePodsData(spec);
                sshService.descaleCluster();
                sshService.unassignWorkerNodes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void shutdown() {
        executor.shutdown(); // Initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted.
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // Cancel currently executing tasks
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
