package uni.trento.probebuilder.jmeter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.tablesaw.aggregate.Summarizer;
import tech.tablesaw.api.CategoricalColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.DataFrameReader;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.api.HorizontalBarPlot;
import tech.tablesaw.plotly.api.TimeSeriesPlot;
import tech.tablesaw.plotly.components.Figure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static tech.tablesaw.aggregate.AggregateFunctions.mean;

@Service
@Slf4j
public class ReportCreator {

    public static ReportData create(String file) {

        try {
            Table table = Table.read().file(new File(file));
            table.addColumns(DoubleColumn.create("count", IntStream.range(1, table.rowCount() + 1).toArray()));

            DoubleColumn latencySeconds = table.intColumn("Latency").divide(1000).setName("Latency seconds");
            table.addColumns(latencySeconds);

            CategoricalColumn<?> success = table.categoricalColumn("success");
            Table successCategory = success.countByCategory();

            Figure errrorsAndSuccessPlot = HorizontalBarPlot.create("Errors and success", successCategory, "Category", "Count");
            Figure latencyOverTime = TimeSeriesPlot.create("Latency over time", table, "count", "Latency seconds");

            return new ReportData(
                createHTMLFile("report" + file.replaceAll(".csv", ".html"), Arrays.asList(latencyOverTime, errrorsAndSuccessPlot)),
                String.valueOf(getErrorCount(successCategory)),
                getLatencyMean(table)
            );

        } catch (IOException e) {
            throw new IllegalStateException("Failed to create a report for " + file);
        }

    }

    private static String getLatencyMean(Table table) {
        Summarizer latency = table.summarize("Latency", mean);
        Table summariezed = latency.apply();
        System.out.println(summariezed);
        return summariezed.getString(0, 0);
    }

    private static int getErrorCount(Table table) {

        String firstRow = table.getString(0, 0);

        if (firstRow.equals("true")) {
            if (table.columnCount() == 2) {
                return 0;
            } else {
                return Integer.parseInt(table.getString(1, 1));
            }
        } else if (firstRow.equals("false")) {
            return Integer.parseInt(table.getString(0, 1));
        } else {
            log.error("NO true or false records found in success table!");
            return 0;
        }
    }


    public static String createHTMLFile(String filename, List<Figure> plots) {
        // HTML template
        String template = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <title>Test plots</title>\n" +
            "<script src='https://cdn.plot.ly/plotly-latest.min.js'></script>\n" +
            "</head>\n" +
            "<body>\n" +
            getDivs(plots) +
            "    </div>\n" +
            "</body>\n" +
            plots.stream().map(p -> p.asJavascript("plot" + plots.indexOf(p))).collect(Collectors.joining("")) +
            "</html>";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            // Write the template to the file
            writer.write(template);
            System.out.println("HTML file created: " + filename);
        } catch (IOException e) {
            System.err.println("Error creating HTML file: " + e.getMessage());
        }

        return filename;
    }

    public static String getDivs(List<Figure> plots) {
        return plots.stream().map(p -> "<div id=plot" + plots.indexOf(p) + "></div>")
            .collect(Collectors.joining(""));
    }
}
