package com.garganttua.core.runtime.perfs;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.LoggerFactory;

import com.garganttua.core.injection.context.DiContext;
import com.garganttua.core.injection.context.dsl.IDiContextBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.runtime.IRuntimeResult;
import com.garganttua.core.runtime.RuntimeResult;
import com.garganttua.core.runtime.dsl.IRuntimesBuilder;
import com.garganttua.core.runtime.dsl.RuntimesBuilder;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@Execution(ExecutionMode.CONCURRENT)
@Disabled("Performances tests, too heavy for standard build")
public class PerformancesTest {

        @BeforeAll
        public static void setup() {
                ObjectReflectionHelper.annotationScanner = new ReflectionsAnnotationScanner();
        }

        @Test
        public void testPerformances() throws Exception {

                int threads = java.lang.Runtime.getRuntime().availableProcessors();
                Map<Instant, Double> cpuSamples = new HashMap<>();
                Map<Instant, Long> usedMemorySamples = new HashMap<>();
                Map<Instant, Long> totalMemorySamples = new HashMap<>();

                deactivateLogs();
                ScheduledExecutorService monitor = startSystemPolling(cpuSamples, usedMemorySamples,
                                totalMemorySamples);

                Document document = this.initializeReport();
                Map<String, IRuntime<?, ?>> runtimes = getRuntimes();
                Instant start = Instant.now();

                exportPerfReportPdf(this.testRuntimeWithStringInput(runtimes.get("runtime-1"), threads * 5), document,
                                "Simple Use Case",
                                "Test Runtime with one step that concatenates strings injected in parameters, including input.",
                                threads * 5);

                exportPerfReportPdf(
                                this.testRuntimeWithStringInput(
                                                runtimes.get("RuntimeWithCatchedExceptionAndHandledByFallback"),
                                                threads * 5),
                                document, "Simple Use Case with fallback",
                                "Test Runtime with one step that throws a catched exception which is handled by fallback method that concatenates strings injected in parameters, including input.",
                                threads * 5);

                exportPerfReportPdf(this.testRuntimeWithStringInput(runtimes.get("runtime-1"), threads * 10), document,
                                "Simple Use Case",
                                "Test Runtime with one step that concatenates strings injected in parameters, including input.",
                                threads * 10);

                exportPerfReportPdf(
                                this.testRuntimeWithStringInput(
                                                runtimes.get("RuntimeWithCatchedExceptionAndHandledByFallback"),
                                                threads * 10),
                                document, "Simple Use Case with fallback",
                                "Test Runtime with one step that throws a catched exception which is handled by fallback method that concatenates strings injected in parameters, including input.",
                                threads * 10);

                exportPerfReportPdf(this.testRuntimeWithStringInput(runtimes.get("runtime-1"), threads * 20), document,
                                "Simple Use Case",
                                "Test Runtime with one step that concatenates strings injected in parameters, including input.",
                                threads * 20);

                exportPerfReportPdf(
                                this.testRuntimeWithStringInput(
                                                runtimes.get("RuntimeWithCatchedExceptionAndHandledByFallback"),
                                                threads * 20),
                                document, "Simple Use Case with fallback",
                                "Test Runtime with one step that throws a catched exception which is handled by fallback method that concatenates strings injected in parameters, including input.",
                                threads * 20);

                monitor.shutdownNow();
                this.finalizeReport(start, document, cpuSamples, usedMemorySamples, totalMemorySamples);
        }

        private ScheduledExecutorService startSystemPolling(Map<Instant, Double> cpuSamples,
                        Map<Instant, Long> usedMemorySamples,
                        Map<Instant, Long> totalMemorySamples) {
                ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
                com.sun.management.OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory
                                .getOperatingSystemMXBean();
                monitor.scheduleAtFixedRate(() -> {
                        Instant now = Instant.now();
                        cpuSamples.put(now, osBean.getProcessCpuLoad() * 100);
                        usedMemorySamples.put(now,
                                        Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                        totalMemorySamples.put(now, osBean.getTotalMemorySize());

                }, 0, 100, TimeUnit.MILLISECONDS);
                return monitor;
        }

        @SuppressWarnings("unchecked")
        private List<TestPerfReport> testRuntimeWithStringInput(IRuntime<?, ?> runtime, int poolThreaSize)
                        throws InterruptedException, ExecutionException {
                List<TestPerfReport> reports = new ArrayList<>();

                reports.add(runTest(1, (IRuntime<String, String>) runtime, poolThreaSize));
                reports.add(runTest(10, (IRuntime<String, String>) runtime, poolThreaSize));
                reports.add(runTest(100, (IRuntime<String, String>) runtime, poolThreaSize));
                reports.add(runTest(1000, (IRuntime<String, String>) runtime, poolThreaSize));
                reports.add(runTest(10000, (IRuntime<String, String>) runtime, poolThreaSize));
                reports.add(runTest(100000, (IRuntime<String, String>) runtime, poolThreaSize));
                reports.add(runTest(200000, (IRuntime<String, String>) runtime, poolThreaSize));
                reports.add(runTest(500000, (IRuntime<String, String>) runtime, poolThreaSize));
                reports.add(runTest(700000, (IRuntime<String, String>) runtime, poolThreaSize));
                reports.add(runTest(1000000, (IRuntime<String, String>) runtime, poolThreaSize));
                reports.add(runTest(5000000, (IRuntime<String, String>) runtime, poolThreaSize));
                reports.add(runTest(10000000, (IRuntime<String, String>) runtime, poolThreaSize));

                return reports;
        }

        private void deactivateLogs() {
                Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
                root.setLevel(Level.OFF);
        }

        private void finalizeReport(Instant start, Document document, Map<Instant, Double> cpuSamples,
                        Map<Instant, Long> memSamples,
                        Map<Instant, Long> totalMemorySamples) throws Exception {
                Instant stop = Instant.now();
                document.newPage();
                Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
                Paragraph totalDurationParagraph = new Paragraph(
                                "Total performances tests duration : "
                                                + RuntimeResult.prettyDurationPlain(Duration.between(start, stop)),
                                titleFont);
                totalDurationParagraph.setAlignment(Element.ALIGN_LEFT);
                totalDurationParagraph.setSpacingAfter(12);
                document.add(totalDurationParagraph);

                ByteArrayOutputStream outMem = exportSystemMemoryUsageGraph(memSamples, totalMemorySamples);
                ByteArrayOutputStream outcpu = exportSystemCpuUsageGraph(cpuSamples);

                com.lowagie.text.Image chartImage = com.lowagie.text.Image.getInstance(outMem.toByteArray());
                com.lowagie.text.Image cpuImage = com.lowagie.text.Image.getInstance(outcpu.toByteArray());

                chartImage.scaleToFit(550, 350);
                chartImage.setAlignment(com.lowagie.text.Image.ALIGN_CENTER);
                chartImage.setSpacingAfter(20);

                cpuImage.scaleToFit(550, 350);
                cpuImage.setAlignment(com.lowagie.text.Image.ALIGN_CENTER);
                cpuImage.setSpacingAfter(20);

                document.add(chartImage);
                document.add(cpuImage);

                document.close();
        }

        private Document initializeReport() throws DocumentException, FileNotFoundException {
                Document document = new Document(PageSize.A4);
                PdfWriter.getInstance(document, new FileOutputStream("performances-test-report.pdf"));
                document.open();

                Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
                Paragraph titleParagraph = new Paragraph("Garganttua Core Runtime performances report", titleFont);
                titleParagraph.setAlignment(Element.ALIGN_CENTER);
                titleParagraph.setSpacingAfter(12);
                document.add(titleParagraph);

                Font descFont = new Font(Font.HELVETICA, 12, Font.NORMAL);
                Paragraph descParagraph = new Paragraph("With no logs", descFont);
                descParagraph.setAlignment(Element.ALIGN_LEFT);
                descParagraph.setSpacingAfter(20);
                document.add(descParagraph);

                TestEnvironment env = TestEnvironment.capture();
                Paragraph testEnvParagraph = new Paragraph(env.toString());
                testEnvParagraph.setAlignment(Element.ALIGN_LEFT);
                testEnvParagraph.setSpacingAfter(20);
                document.add(testEnvParagraph);

                return document;
        }

        private Map<String, IRuntime<?, ?>> getRuntimes() {
                IRuntimesBuilder t = RuntimesBuilder.builder();

                IDiContextBuilder contextBuilder = DiContext.builder().autoDetect(true)
                                .withPackage("com.garganttua.core.runtime");
                contextBuilder.build().onInit().onStart();

                Map<String, IRuntime<?, ?>> runtimes = t.context(contextBuilder).autoDetect(true).build();
                return runtimes;
        }

        public static void exportPerfReportPdf(
                        List<TestPerfReport> reports,
                        Document document,
                        String title,
                        String description, int threadPoolSize) throws Exception {

                document.newPage();

                XYSeries avgSeries = new XYSeries("avg");
                XYSeries minSeries = new XYSeries("min");
                XYSeries maxSeries = new XYSeries("max");
                XYSeries totalSeries = new XYSeries("total");
                XYSeries durationSeries = new XYSeries("duration (ns)");

                for (TestPerfReport r : reports) {
                        int x = r.runs();
                        avgSeries.add(x, r.avg());
                        minSeries.add(x, r.min());
                        maxSeries.add(x, r.max());
                        totalSeries.add(x, r.total());
                        durationSeries.add(x, r.duration().toNanos());
                }

                XYSeriesCollection dataset = new XYSeriesCollection();
                dataset.addSeries(avgSeries);
                dataset.addSeries(minSeries);
                dataset.addSeries(maxSeries);
                dataset.addSeries(totalSeries);
                dataset.addSeries(durationSeries);

                JFreeChart chart = ChartFactory.createXYLineChart(
                                "Performance vs Runs",
                                "Runs",
                                "Time (nanoseconds)",
                                dataset);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ChartUtils.writeChartAsPNG(out, chart, 1200, 800);

                Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
                Paragraph titleParagraph = new Paragraph(title, titleFont);
                titleParagraph.setAlignment(Element.ALIGN_CENTER);
                titleParagraph.setSpacingAfter(12);
                document.add(titleParagraph);

                Font descFont = new Font(Font.HELVETICA, 12, Font.NORMAL);
                if (description != null && !description.isBlank()) {
                        Paragraph descParagraph = new Paragraph(description, descFont);
                        descParagraph.setAlignment(Element.ALIGN_LEFT);
                        descParagraph.setSpacingAfter(20);
                        document.add(descParagraph);
                }

                Paragraph threadPoolSizeParagraph = new Paragraph("Thread Pool Size : " + threadPoolSize, descFont);
                threadPoolSizeParagraph.setAlignment(Element.ALIGN_LEFT);
                threadPoolSizeParagraph.setSpacingAfter(20);
                document.add(threadPoolSizeParagraph);

                com.lowagie.text.Image chartImage = com.lowagie.text.Image.getInstance(out.toByteArray());

                chartImage.scaleToFit(550, 350);
                chartImage.setAlignment(com.lowagie.text.Image.ALIGN_CENTER);
                chartImage.setSpacingAfter(20);
                document.add(chartImage);

                Paragraph tableTitle = new Paragraph("Performance Report Table",
                                new Font(Font.HELVETICA, 14, Font.BOLD));
                tableTitle.setSpacingBefore(10);
                tableTitle.setSpacingAfter(10);
                document.add(tableTitle);

                PdfPTable table = new PdfPTable(6);
                table.setWidthPercentage(100);
                table.setWidths(new float[] { 10f, 20f, 20f, 20f, 20f, 30f });

                addCellHeader(table, "Runs");
                addCellHeader(table, "Avg (ns)");
                addCellHeader(table, "Min (ns)");
                addCellHeader(table, "Max (ns)");
                addCellHeader(table, "Total (ns)");
                addCellHeader(table, "Duration");

                for (TestPerfReport r : reports) {
                        table.addCell(String.valueOf(r.runs()));
                        table.addCell(RuntimeResult.prettyNano(r.avg()));
                        table.addCell(RuntimeResult.prettyNano(r.min()));
                        table.addCell(RuntimeResult.prettyNano(r.max()));
                        table.addCell(RuntimeResult.prettyNano(r.total()));
                        table.addCell(RuntimeResult.prettyDurationPlain(r.duration()));
                }

                document.add(table);
        }

        public static ByteArrayOutputStream exportSystemCpuUsageGraph(Map<Instant, Double> cpuSamples)
                        throws Exception {

                // --- Séries temporelles ---
                TimeSeries cpuSeries = new TimeSeries("CPU Usage (%)");

                for (Map.Entry<Instant, Double> entry : cpuSamples.entrySet()) {
                        cpuSeries.addOrUpdate(new Millisecond(java.util.Date.from(entry.getKey())), entry.getValue());
                }

                TimeSeriesCollection dataset = new TimeSeriesCollection();
                dataset.addSeries(cpuSeries);

                // --- Création du chart ---
                JFreeChart chart = ChartFactory.createTimeSeriesChart(
                                "System Usage Over Time",
                                "Time",
                                "Value",
                                dataset);

                // --- Configuration de l’axe date et rendu ---
                XYPlot plot = chart.getXYPlot();
                DateAxis axis = (DateAxis) plot.getDomainAxis();
                axis.setDateFormatOverride(new java.text.SimpleDateFormat("HH:mm:ss"));

                XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
                plot.setRenderer(renderer);

                ByteArrayOutputStream out = new ByteArrayOutputStream();

                // --- Export PNG ---
                ChartUtils.writeChartAsPNG(out, chart, 1200, 600);

                return out;
        }

        public static ByteArrayOutputStream exportSystemMemoryUsageGraph(
                        Map<Instant, Long> usedMemorySamples,
                        Map<Instant, Long> totalMemorySamples) throws Exception {

                // --- Séries temporelles ---
                TimeSeries usedMemSeries = new TimeSeries("Used Memory (MB)");
                TimeSeries totalMemSeries = new TimeSeries("Total Memory (MB)");

                for (Map.Entry<Instant, Long> entry : usedMemorySamples.entrySet()) {
                        usedMemSeries.addOrUpdate(new Millisecond(java.util.Date.from(entry.getKey())),
                                        entry.getValue() / (1024.0 * 1024.0));
                }
                for (Map.Entry<Instant, Long> entry : totalMemorySamples.entrySet()) {
                        totalMemSeries.addOrUpdate(new Millisecond(java.util.Date.from(entry.getKey())),
                                        entry.getValue() / (1024.0 * 1024.0));
                }

                TimeSeriesCollection dataset = new TimeSeriesCollection();
                dataset.addSeries(usedMemSeries);
                dataset.addSeries(totalMemSeries);

                // --- Création du chart ---
                JFreeChart chart = ChartFactory.createTimeSeriesChart(
                                "System Usage Over Time",
                                "Time",
                                "Value",
                                dataset);

                // --- Configuration de l’axe date et rendu ---
                XYPlot plot = chart.getXYPlot();
                DateAxis axis = (DateAxis) plot.getDomainAxis();
                axis.setDateFormatOverride(new java.text.SimpleDateFormat("HH:mm:ss"));

                XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
                plot.setRenderer(renderer);

                ByteArrayOutputStream out = new ByteArrayOutputStream();

                // --- Export PNG ---
                ChartUtils.writeChartAsPNG(out, chart, 1200, 600);

                return out;
        }

        private static void addCellHeader(PdfPTable table, String text) {
                PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.HELVETICA, 12, Font.BOLD)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                table.addCell(cell);
        }

        private TestPerfReport runTest(int runs, IRuntime<String, String> runtime, int poolThreadSize)
                        throws InterruptedException, ExecutionException {
                Instant start = Instant.now();
                ExecutorService executor = Executors.newFixedThreadPool(poolThreadSize);

                List<Callable<Long>> tasks = new ArrayList<>();
                AtomicInteger inte = new AtomicInteger(0);

                for (int i = 0; i < runs; i++) {
                        tasks.add(() -> {
                                String input = "input-" +
                                                inte.getAndIncrement();
                                Optional<IRuntimeResult<String, String>> r = runtime.execute(input);
                                return r.get().durationInNanos();
                        });
                }

                List<Future<Long>> futures = executor.invokeAll(tasks);
                executor.shutdown();

                long total = 0;
                long min = 10000000;
                long max = 0;

                for (Future<Long> f : futures) {
                        long d = f.get();
                        total = total + d;

                        if (d < min)
                                min = d;
                        if (d > max)
                                max = d;
                }

                Long avg = total / runs;

                Instant stop = Instant.now();

                return new TestPerfReport(runs, avg, min, max, total, Duration.between(start, stop));
        }
}
