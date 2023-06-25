package uni.trento.probebuilder.jmeter;

import org.junit.jupiter.api.Test;

import java.io.IOException;

class ReportCreatorTest {

    @Test
    void should_create_a_file() throws IOException {
        ReportCreator.create("test202896.csv");
    }

}