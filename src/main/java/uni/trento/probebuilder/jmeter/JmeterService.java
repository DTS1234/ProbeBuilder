package uni.trento.probebuilder.jmeter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.gui.LoopControlPanel;
import org.apache.jmeter.control.gui.TestPlanGui;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.gui.HeaderPanel;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.threads.gui.ThreadGroupGui;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
@Slf4j
@RequiredArgsConstructor
public class JmeterService {

    private final JmeterRepository repo;
    public static final String DIR = System.getProperty("user.dir");

    public void jmeterStart(JmeterSpecification spec) throws IOException {

        log.info("TEST start !");

        String jmeterHomePath = ConfigurationService.JMETER_HOME;
        if (jmeterHomePath == null) {
            throw new IllegalStateException("First set the path!");
        }
        File jmeterHome = new File(jmeterHomePath);
        String slash = System.getProperty("file.separator");

        boolean exists = jmeterHome.exists();

        log.info("Jmeter home correct: " + exists);

        File jmeterProperties = new File(jmeterHomePath + slash + "libexec" + slash + "bin" + slash + "jmeter.properties");

        try {
            JMeterUtils.loadJMeterProperties(jmeterProperties.getPath());
        }catch (RuntimeException e) {
            if (e.getMessage().contains("properties")) {
                throw new IllegalStateException("cannot read jmeter.properties from the given path");
            }
        }
        boolean existsProperties = jmeterProperties.exists();

        log.info("Jmeter properties correct: " + existsProperties);

        if (existsProperties) {
            //JMeter Engine
            StandardJMeterEngine jmeter = new StandardJMeterEngine();
            String fileName = spec.getTestName();

            //JMeter initialization (properties, log levels, locale, etc)
            JMeterUtils.setJMeterHome(jmeterHome.getPath());
            JMeterUtils.loadJMeterProperties(jmeterProperties.getPath());
            JMeterUtils.setLocale(Locale.US);
            SaveService.loadProperties();

            // JMeter Test Plan, basically JOrphan HashTree
            HashTree testPlanTree = new HashTree();

            // First HTTP Sampler - open example.com
            HTTPSamplerProxy sampler = buildSampler(spec);

            // Loop Controller
            LoopController loopController = buildLoopController();

            // Thread Group
            ThreadGroup threadGroup = buildThreadGroup(spec, loopController);

            // Test Plan
            TestPlan testPlan = buildTestPlan();

            // Construct Test Plan from previously initialized elements
            testPlanTree.add(testPlan);
            HashTree threadGroupHashTree = testPlanTree.add(testPlan, threadGroup);
            threadGroupHashTree.add(sampler, sampler.getHeaderManager());

            // save generated test plan to JMeter's .jmx file format
            String jmxFile = System.getProperty("user.dir") + spec.getTestName() + ".jmx";
            SaveService.saveTree(testPlanTree, new FileOutputStream(jmxFile));

            //add Summarizer output to get test progress in stdout like:
            // summary =      2 in   1.3s =    1.5/s Avg:   631 Min:   290 Max:   973 Err:     0 (0.00%)
            Summariser summer = buildSummariser();

            ResultCollector logger = buildLogger(spec.getTestName(), summer);
            testPlanTree.add(testPlanTree.getArray()[0], logger);

            // Run Test Plan
            jmeter.configure(testPlanTree);

            ExecutorService executorService = Executors.newFixedThreadPool(1);
            Future<?> submit = executorService.submit(() -> {
                String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                repo.save(new JmeterResultData(date, fileName + ".csv", fileName + ".jmx",
                    String.valueOf(spec.getNumberOfThreads()),
                    String.valueOf(spec.getRampUpPeriod()),
                    spec.getPath(),
                    false
                ));

                jmeter.run();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ReportCreator.create(fileName + ".csv");

                repo.save(new JmeterResultData(date, fileName + ".csv", fileName + ".jmx",
                    String.valueOf(spec.getNumberOfThreads()),
                    String.valueOf(spec.getRampUpPeriod()),
                    spec.getPath(),
                    true
                ));
            });

            try {
                submit.get();
                executorService.shutdown();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            // thread.start();
            log.info("Test started for a spec: " + spec);
        }
    }


    @Nullable
    private static Summariser buildSummariser() {
        Summariser summer = null;
        String summariserName = JMeterUtils.getPropDefault("summariser.name", "summary");
        if (summariserName.length() > 0) {
            summer = new Summariser(summariserName);
        }
        return summer;
    }

    private static ResultCollector buildLogger(String fileName, Summariser summer) {
        String logFile = DIR + "/" + fileName + ".csv";
        ResultCollector logger = new ResultCollector(summer);
        logger.setFilename(logFile);
        return logger;
    }

    @NotNull
    private static TestPlan buildTestPlan() {
        TestPlan testPlan = new TestPlan("Create JMeter Script From Java Code");
        testPlan.setProperty(TestElement.TEST_CLASS, TestPlan.class.getName());
        testPlan.setProperty(TestElement.GUI_CLASS, TestPlanGui.class.getName());
        testPlan.setUserDefinedVariables((Arguments) new ArgumentsPanel().createTestElement());
        return testPlan;
    }

    @NotNull
    private static ThreadGroup buildThreadGroup(JmeterSpecification spec, LoopController loopController) {
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setName("Thread Group");
        threadGroup.setNumThreads(spec.getNumberOfThreads());
        threadGroup.setRampUp(spec.getRampUpPeriod());
        threadGroup.setScheduler(true);
        threadGroup.setDuration(spec.getDuration());
        threadGroup.setSamplerController(loopController);
        threadGroup.setIsSameUserOnNextIteration(true);
        threadGroup.setProperty(TestElement.TEST_CLASS, ThreadGroup.class.getName());
        threadGroup.setProperty(TestElement.GUI_CLASS, ThreadGroupGui.class.getName());
        return threadGroup;
    }

    @NotNull
    private static LoopController buildLoopController() {
        LoopController loopController = new LoopController();
        loopController.setLoops(Integer.MAX_VALUE);
        loopController.setProperty(TestElement.TEST_CLASS, LoopController.class.getName());
        loopController.setProperty(TestElement.GUI_CLASS, LoopControlPanel.class.getName());
        loopController.initialize();
        return loopController;
    }

    @NotNull
    private static HTTPSamplerProxy buildSampler(JmeterSpecification spec) {

        HeaderManager manager = new HeaderManager();
        manager.add(new Header("Content-Type", "application/json"));
        manager.setName(JMeterUtils.getResString("header_manager_title")); // $NON-NLS-1$
        manager.setProperty(TestElement.TEST_CLASS, HeaderManager.class.getName());
        manager.setProperty(TestElement.GUI_CLASS, HeaderPanel.class.getName());

        HTTPSamplerProxy sampler = new HTTPSamplerProxy();
        sampler.setDomain(spec.getIp());
        sampler.setPort(spec.getPort());
        sampler.setPath(spec.getPath());
        sampler.setMethod(spec.getMethod());
        sampler.setName("Run spec");
        sampler.setProperty(TestElement.TEST_CLASS, HTTPSamplerProxy.class.getName());
        sampler.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());
        sampler.addNonEncodedArgument("Body Data", spec.getBody(), "");
        sampler.setPostBodyRaw(true);
        sampler.setHeaderManager(manager);

        return sampler;
    }

    public Map<String, JmeterResultData> getAllResults() {
        return repo.getResults();
    }
}
