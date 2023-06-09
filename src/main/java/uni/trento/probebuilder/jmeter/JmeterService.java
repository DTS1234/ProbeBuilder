package uni.trento.probebuilder.jmeter;

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
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

@Service
@Slf4j
public class JmeterService {

    public void jmeterStart(JmeterSpecification spec) throws IOException {

        log.info("TEST start !");

        String jmeterHome1 = "/opt/homebrew/Cellar/jmeter/5.5";
        File jmeterHome = new File(jmeterHome1);
        String slash = System.getProperty("file.separator");

        boolean exists = jmeterHome.exists();

        log.info("Jmeter home correct: " + exists);

        if (exists) {
            File jmeterProperties = new File(jmeterHome.getPath() + slash + "libexec/bin" + slash + "jmeter.properties");

            boolean existsProperties = jmeterProperties.exists();
            log.info("Jmeter properties correct: " + existsProperties);

            if (existsProperties) {
                //JMeter Engine
                StandardJMeterEngine jmeter = new StandardJMeterEngine();

                //JMeter initialization (properties, log levels, locale, etc)
                JMeterUtils.setJMeterHome(jmeterHome.getPath());
                JMeterUtils.loadJMeterProperties(jmeterProperties.getPath());
                JMeterUtils.setLocale(Locale.US);

                // JMeter Test Plan, basically JOrphan HashTree
                HashTree testPlanTree = new HashTree();

                // First HTTP Sampler - open example.com
                HTTPSamplerProxy sampler = new HTTPSamplerProxy();
                sampler.setDomain(spec.getIp());
                sampler.setPort(spec.getPort());
                sampler.setPath(spec.getPath());
                sampler.setMethod(spec.getMethod());
                sampler.setName("Run spec");
                sampler.setProperty(TestElement.TEST_CLASS, HTTPSamplerProxy.class.getName());
                sampler.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());

                sampler.addNonEncodedArgument("", spec.getBody(), "");
                sampler.setPostBodyRaw(true);

                HeaderManager value = new HeaderManager();
                value.add(new Header("Content-Type", "application/json"));
                sampler.setHeaderManager(value);

                // Loop Controller
                LoopController loopController = new LoopController();
                loopController.setLoops(1);
                loopController.setFirst(true);
                loopController.setProperty(TestElement.TEST_CLASS, LoopController.class.getName());
                loopController.setProperty(TestElement.GUI_CLASS, LoopControlPanel.class.getName());
                loopController.initialize();

                // Thread Group
                org.apache.jmeter.threads.ThreadGroup threadGroup = new org.apache.jmeter.threads.ThreadGroup();
                threadGroup.setName("Example Thread Group");
                threadGroup.setNumThreads(spec.getNumberOfThreads());
                threadGroup.setRampUp(10);
                threadGroup.setSamplerController(loopController);
                threadGroup.setProperty(TestElement.TEST_CLASS, ThreadGroup.class.getName());
                threadGroup.setProperty(TestElement.GUI_CLASS, ThreadGroupGui.class.getName());

                // Test Plan
                TestPlan testPlan = new TestPlan("Create JMeter Script From Java Code");
                testPlan.setProperty(TestElement.TEST_CLASS, TestPlan.class.getName());
                testPlan.setProperty(TestElement.GUI_CLASS, TestPlanGui.class.getName());
                testPlan.setUserDefinedVariables((Arguments) new ArgumentsPanel().createTestElement());

                // Construct Test Plan from previously initialized elements
                testPlanTree.add(testPlan);
                HashTree threadGroupHashTree = testPlanTree.add(testPlan, threadGroup);
                threadGroupHashTree.add(sampler);

                // save generated test plan to JMeter's .jmx file format
                SaveService.saveTree(testPlanTree, new FileOutputStream(System.getProperty("user.dir") + "/example.jmx"));

                //add Summarizer output to get test progress in stdout like:
                // summary =      2 in   1.3s =    1.5/s Avg:   631 Min:   290 Max:   973 Err:     0 (0.00%)
                Summariser summer = null;
                String summariserName = JMeterUtils.getPropDefault("summariser.name", "summary");
                if (summariserName.length() > 0) {
                    summer = new Summariser(summariserName);
                }

                // Store execution results into a .jtl file
                String logFile = System.getProperty("user.dir") + "/example.jmx";
                ResultCollector logger = new ResultCollector(summer);
                logger.setFilename(logFile);
                testPlanTree.add(testPlanTree.getArray()[0], logger);

                // Run Test Plan
                jmeter.configure(testPlanTree);
                jmeter.run();

                System.out.println("Test completed. See " + jmeterHome + slash + "example.jtl file for results");
                System.out.println("JMeter .jmx script is available at " + jmeterHome + slash + "example.jmx");
                System.exit(0);

            }
        }

    }

}
