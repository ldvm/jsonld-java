package com.github.jsonldjava.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.jsonldjava.impl.TurtleTripleCallback;
import com.github.jsonldjava.utils.JSONUtils;
import com.github.jsonldjava.utils.Obj;

@RunWith(Parameterized.class)
public class JsonLdProcessorTest {

    private static final String TEST_DIR = "json-ld.org";

    private static Map<String, Object> REPORT;
    private static List<Object> REPORT_GRAPH;
    private static String ASSERTOR = "http://tristan.github.com/foaf#me";

    @BeforeClass
    public static void prepareReportFrame() {
        REPORT = new LinkedHashMap<String, Object>() {
            {
                // context
                put("@context", new LinkedHashMap<String, Object>() {
                    {
                        put("@vocab", "http://www.w3.org/ns/earl#");
                        put("foaf", "http://xmlns.com/foaf/0.1/");
                        put("earl", "http://www.w3.org/ns/earl#");
                        put("doap", "http://usefulinc.com/ns/doap#");
                        put("dc", "http://purl.org/dc/terms/");
                        put("xsd", "http://www.w3.org/2001/XMLSchema#");
                        put("foaf:homepage", new LinkedHashMap<String, Object>() {
                            {
                                put("@type", "@id");
                            }
                        });
                        put("doap:homepage", new LinkedHashMap<String, Object>() {
                            {
                                put("@type", "@id");
                            }
                        });
                    }
                });
                put("@graph", new ArrayList<Object>() {
                    {
                        // asserter
                        add(new LinkedHashMap<String, Object>() {
                            {
                                put("@id", "http://tristan.github.com/foaf#me");
                                put("@type", new ArrayList<Object>() {
                                    {
                                        add("foaf:Person");
                                        add("earl:Assertor");
                                    }
                                });
                                put("foaf:name", "Tristan King");
                                put("foaf:title", "Implementor");
                                put("foaf:homepage", "http://tristan.github.com");
                            }
                        });

                        // project
                        add(new LinkedHashMap<String, Object>() {
                            {
                                put("@id", "http://github.com/jsonld-java/jsonld-java");
                                put("@type", new ArrayList<Object>() {
                                    {
                                        add("doap:Project");
                                        add("earl:TestSubject");
                                        add("earl:Software");
                                    }
                                });
                                put("doap:name", "JSONLD-Java");
                                put("doap:homepage", "http://github.com/jsonld-java/jsonld-java");
                                put("doap:description", new LinkedHashMap<String, Object>() {
                                    {
                                        put("@value",
                                                "An Implementation of the JSON-LD Specification for Java");
                                        put("@language", "en");
                                    }
                                });
                                put("doap:programming-language", "Java");
                                put("doap:developer", new ArrayList<Object>() {
                                    {
                                        add(new LinkedHashMap<String, Object>() {
                                            {
                                                put("@id", "http://tristan.github.com/foaf#me");
                                            }
                                        });
                                        add(new LinkedHashMap<String, Object>() {
                                            {
                                                put("@id", "https://github.com/ansell/foaf#me");
                                                put("foaf:name", "Peter Ansell");
                                                put("foaf:title", "Contributor");
                                            }
                                        });
                                    }
                                });
                                put("doap:title", "JSONLD-Java");
                                put("dc:date", new LinkedHashMap<String, Object>() {
                                    {
                                        put("@type", "xsd:date");
                                        put("@value", "2013-05-16");
                                    }
                                });
                                put("dc:creator", new LinkedHashMap<String, Object>() {
                                    {
                                        put("@id", "http://tristan.github.com/foaf#me");
                                    }
                                });
                            }
                        });
                    }
                });
            }
        };
        REPORT_GRAPH = (List<Object>) REPORT.get("@graph");
    }

    private static final String reportOutputFile = "reports/report";

    @AfterClass
    public static void writeReport() throws JsonGenerationException, JsonMappingException,
            IOException, JsonLdError {

        // Only write reports if "-Dreport.format=..." is set
        String reportFormat = System.getProperty("report.format");
        if (reportFormat != null) {
            reportFormat = reportFormat.toLowerCase();
        } else {
            return; // nothing to do
        }

        if ("application/ld+json".equals(reportFormat) || "jsonld".equals(reportFormat)
                || "*".equals(reportFormat)) {
            System.out.println("Generating JSON-LD Report");
            JSONUtils.writePrettyPrint(new OutputStreamWriter(new FileOutputStream(reportOutputFile
                    + ".jsonld")), REPORT);
        }

        if ("text/plain".equals(reportFormat) || "nquads".equals(reportFormat)
                || "nq".equals(reportFormat) || "nt".equals(reportFormat)
                || "ntriples".equals(reportFormat) || "*".equals(reportFormat)) {
            System.out.println("Generating Nquads Report");
            final JsonLdOptions options = new JsonLdOptions("") {
                {
                    this.format = "application/nquads";
                }
            };
            final String rdf = (String) JsonLdProcessor.toRDF(REPORT, options);
            final OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(
                    reportOutputFile + ".nq"));
            writer.write(rdf);
            writer.close();
        }
        if ("text/turtle".equals(reportFormat) || "turtle".equals(reportFormat)
                || "ttl".equals(reportFormat) || "*".equals(reportFormat)) { // write
            // turtle
            System.out.println("Generating Turtle Report");
            final JsonLdOptions options = new JsonLdOptions("") {
                {
                    format = "text/turtle";
                    useNamespaces = true;
                }
            };
            final String rdf = (String) JsonLdProcessor.toRDF(REPORT, new TurtleTripleCallback(),
                    options);
            final OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(
                    reportOutputFile + ".ttl"));
            writer.write(rdf);
            writer.close();
        }
    }

    @Parameters(name = "{0}{1}")
    public static Collection<Object[]> data() throws URISyntaxException, IOException {

        // TODO: look into getting the test data from github, which will help
        // more
        // with keeping up to date with the spec.
        // perhaps use http://develop.github.com/p/object.html
        // to pull info from
        // https://github.com/json-ld/json-ld.org/tree/master/test-suite/tests

        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final File f = new File(cl.getResource(TEST_DIR).toURI());
        final List<File> manifestfiles = Arrays.asList(f.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.contains("manifest") && name.endsWith(".jsonld")) {
                    // System.out.println("Using manifest: " + dir + " "
                    // + name);
                    // Remote-doc tests are not currently supported
                    if (name.contains("remote-doc")) {
                        return false;
                    }
                    return true;
                }
                return false;
            }
        }));

        final Collection<Object[]> rdata = new ArrayList<Object[]>();
        final int count = 0;
        for (final File in : manifestfiles) {
            // System.out.println("Reading: " + in.getCanonicalPath());
            final FileInputStream manifestfile = new FileInputStream(in);

            final Map<String, Object> manifest = (Map<String, Object>) JSONUtils
                    .fromInputStream(manifestfile);

            for (final Map<String, Object> test : (List<Map<String, Object>>) manifest
                    .get("sequence")) {
                final List<String> testType = (List<String>) test.get("@type");
                if (testType.contains("jld:ExpandTest") || testType.contains("jld:CompactTest")
                        || testType.contains("jld:FlattenTest")
                        || testType.contains("jld:FrameTest")
                        || testType.contains("jld:FromRDFTest")
                        || testType.contains("jld:ToRDFTest")
                        || testType.contains("jld:NormalizeTest")) {
                    // System.out.println("Adding test: " + test.get("name"));
                    rdata.add(new Object[] { (String) manifest.get("baseIri") + in.getName(),
                            test.get("@id"), test });
                } else {
                    // TODO: many disabled while implementation is incomplete
                    // System.out.println("Skipping test: " + test.get("name"));
                }
            }
        }
        return rdata;
    }

    private class TestDocumentLoader extends DocumentLoader {

        private final String base;

        public TestDocumentLoader(String base) {
            this.base = base;
        }

        @Override
        public RemoteDocument loadDocument(String url) throws JsonLdError {
            if (url == null) {
                throw new JsonLdError(JsonLdError.Error.LOADING_REMOTE_CONTEXT_FAILED);
            }
            if (url.contains(":")) {
                // check if the url is relative to the test base
                if (url.startsWith(this.base)) {
                    url = url.substring(this.base.length());
                } else {
                    // we can't load remote documents from the test suite
                    throw new JsonLdError(JsonLdError.Error.NOT_IMPLEMENTED);
                }
            }
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            final InputStream inputStream = cl.getResourceAsStream(TEST_DIR + "/" + url);
            try {
                return new RemoteDocument(url, JSONUtils.fromInputStream(inputStream));
            } catch (final IOException e) {
                throw new JsonLdError(JsonLdError.Error.LOADING_DOCUMENT_FAILED);
            }
        }
    }

    private final String group;
    private final Map<String, Object> test;

    public JsonLdProcessorTest(final String group, final String id, final Map<String, Object> test) {
        this.group = group;
        this.test = test;
    }

    public static String join(Collection<String> list, String delim) {
        final StringBuilder builder = new StringBuilder();
        final Iterator<String> iter = list.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next());
            if (!iter.hasNext()) {
                break;
            }
            builder.append(delim);
        }
        return builder.toString();
    }

    @Test
    public void runTest() throws URISyntaxException, IOException, JsonLdError {
        // System.out.println("running test: " + group + test.get("@id") +
        // " :: " + test.get("name"));
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();

        final List<String> testType = (List<String>) test.get("@type");

        final String inputFile = (String) test.get("input");
        final InputStream inputStream = cl.getResourceAsStream(TEST_DIR + "/" + inputFile);
        assertNotNull("unable to find input file: " + test.get("input"), inputStream);
        final String inputType = inputFile.substring(inputFile.lastIndexOf(".") + 1);

        Object input = null;
        if (inputType.equals("jsonld")) {
            input = JSONUtils.fromInputStream(inputStream);
        } else if (inputType.equals("nt") || inputType.equals("nq")) {
            final List<String> inputLines = new ArrayList<String>();
            final BufferedReader buf = new BufferedReader(new InputStreamReader(inputStream,
                    "UTF-8"));
            String line;
            while ((line = buf.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                inputLines.add(line);
            }
            // Collections.sort(inputLines);
            input = join(inputLines, "\n");
        }
        Object expect = null;
        String sparql = null;
        Boolean failure_expected = false;
        final String expectFile = (String) test.get("expect");
        final String sparqlFile = (String) test.get("sparql");
        if (expectFile != null) {
            final InputStream expectStream = cl.getResourceAsStream(TEST_DIR + "/" + expectFile);
            if (expectStream == null && testType.contains("jld:NegativeEvaluationTest")) {
                // in the case of negative evaluation tests the expect field can
                // be a description of what should happen
                expect = expectFile;
                failure_expected = true;
            } else if (expectStream == null) {
                assertFalse("Unable to find expect file: " + expectFile, true);
            } else {
                final String expectType = expectFile.substring(expectFile.lastIndexOf(".") + 1);
                if (expectType.equals("jsonld")) {
                    expect = JSONUtils.fromInputStream(expectStream);
                } else if (expectType.equals("nt") || expectType.equals("nq")) {
                    final List<String> expectLines = new ArrayList<String>();
                    final BufferedReader buf = new BufferedReader(new InputStreamReader(
                            expectStream, "UTF-8"));
                    String line;
                    while ((line = buf.readLine()) != null) {
                        line = line.trim();
                        if (line.length() == 0 || line.charAt(0) == '#') {
                            continue;
                        }
                        expectLines.add(line);
                    }
                    Collections.sort(expectLines);
                    expect = join(expectLines, "\n");
                } else {
                    expect = "";
                    assertFalse("Unknown expect type: " + expectType, true);
                }
            }
        } else if (sparqlFile != null) {
            final InputStream sparqlStream = cl.getResourceAsStream(TEST_DIR + "/" + sparqlFile);
            assertNotNull("unable to find expect file: " + sparqlFile, sparqlStream);
            final BufferedReader buf = new BufferedReader(new InputStreamReader(sparqlStream,
                    "UTF-8"));
            String buffer = null;
            while ((buffer = buf.readLine()) != null) {
                sparql += buffer + "\n";
            }
        } else if (testType.contains("jld:NegativeEvaluationTest")) {
            failure_expected = true;
        } else {
            assertFalse("Nothing to expect from this test, thus nothing to test if it works", true);
        }

        Object result = null;

        // OPTIONS SETUP
        final JsonLdOptions options = new JsonLdOptions("http://json-ld.org/test-suite/tests/"
                + test.get("input"));
        options.documentLoader = new TestDocumentLoader("http://json-ld.org/test-suite/tests/");
        if (test.containsKey("option")) {
            final Map<String, Object> test_opts = (Map<String, Object>) test.get("option");
            if (test_opts.containsKey("base")) {
                options.setBase((String) test_opts.get("base"));
            }
            if (test_opts.containsKey("expandContext")) {
                final InputStream contextStream = cl.getResourceAsStream(TEST_DIR + "/"
                        + test_opts.get("expandContext"));
                options.setExpandContext(JSONUtils.fromInputStream(contextStream));
            }
            if (test_opts.containsKey("compactArrays")) {
                options.setCompactArrays((Boolean) test_opts.get("compactArrays"));
            }
            if (test_opts.containsKey("useNativeTypes")) {
                options.setUseNativeTypes((Boolean) test_opts.get("useNativeTypes"));
            }
            if (test_opts.containsKey("useRdfType")) {
                options.setUseRdfType((Boolean) test_opts.get("useRdfType"));
            }
            if (test_opts.containsKey("produceGeneralizedRdf")) {
                options.setProduceGeneralizedRdf((Boolean) test_opts.get("produceGeneralizedRdf"));
            }
            if (test_opts.containsKey("redirectTo")) {
                // TODO: Handle redirectTo for remote-doc tests
            }
            if (test_opts.containsKey("httpStatus")) {
                // TODO: Handle httpStatus for remote-doc tests
            }
            if (test_opts.containsKey("contentType")) {
                // TODO: Handle contentType for remote-doc tests
            }
            if (test_opts.containsKey("httpLink")) {
                // TODO: Handle httpLink for remote-doc tests
            }
        }

        // RUN TEST
        try {
            if (testType.contains("jld:ExpandTest")) {
                result = JsonLdProcessor.expand(input, options);
            } else if (testType.contains("jld:CompactTest")) {
                final InputStream contextStream = cl.getResourceAsStream(TEST_DIR + "/"
                        + test.get("context"));
                final Object contextJson = JSONUtils.fromInputStream(contextStream);
                result = JsonLdProcessor.compact(input, contextJson, options);
            } else if (testType.contains("jld:FlattenTest")) {
                if (test.containsKey("context")) {
                    final InputStream contextStream = cl.getResourceAsStream(TEST_DIR + "/"
                            + test.get("context"));
                    final Object contextJson = JSONUtils.fromInputStream(contextStream);
                    result = JsonLdProcessor.flatten(input, contextJson, options);
                } else {
                    result = JsonLdProcessor.flatten(input, options);
                }
            } else if (testType.contains("jld:FrameTest")) {
                final InputStream frameStream = cl.getResourceAsStream(TEST_DIR + "/"
                        + test.get("frame"));
                final Map<String, Object> frameJson = (Map<String, Object>) JSONUtils
                        .fromInputStream(frameStream);
                result = JsonLdProcessor.frame(input, frameJson, options);
            } else if (testType.contains("jld:FromRDFTest")) {
                result = JsonLdProcessor.fromRDF(input, options);
            } else if (testType.contains("jld:ToRDFTest")) {
                options.format = "application/nquads";
                result = JsonLdProcessor.toRDF(input, options);
                result = ((String) result).trim();
            } else if (testType.contains("jld:NormalizeTest")) {
                options.format = "application/nquads";
                result = JsonLdProcessor.normalize(input, options);
                result = ((String) result).trim();
            } else {
                fail("Unknown test type: " + testType);
            }
        } catch (final JsonLdError e) {
            result = e;
        }

        Boolean testpassed = false;
        try {
            if (failure_expected) {
                if (result instanceof JsonLdError) {
                    testpassed = Obj.equals(expect, ((JsonLdError) result).getType().toString());
                    if (!testpassed) {
                        ((JsonLdError) result).printStackTrace();
                    }
                }
            } else {
                testpassed = JsonLdUtils.deepCompare(expect, result);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        if (testpassed == false && result instanceof JsonLdError) {
            throw (JsonLdError) result;
        }

        // write details to report
        final String manifest = this.group;
        final String id = (String) this.test.get("@id");
        final Date d = new Date();
        final String dateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(d);
        String zone = new SimpleDateFormat("Z").format(d);
        zone = zone.substring(0, 3) + ":" + zone.substring(3);
        final String dateTimeZone = dateTime + zone;
        final Boolean passed = testpassed;
        REPORT_GRAPH.add(new LinkedHashMap<String, Object>() {
            {
                put("@type", "earl:Assertion");
                put("earl:assertedBy", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", ASSERTOR);
                    }
                });
                put("earl:subject", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://github.com/jsonld-java/jsonld-java");
                    }
                });
                put("earl:test", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", manifest + id);
                    }
                });
                put("earl:result", new LinkedHashMap<String, Object>() {
                    {
                        put("@type", "earl:TestResult");
                        put("earl:outcome", new LinkedHashMap<String, Object>() {
                            {
                                put("@id", passed ? "earl:passed" : "earl:failed");
                            }
                        });
                        put("dc:date", new LinkedHashMap<String, Object>() {
                            {
                                put("@value", dateTimeZone);
                                put("@type", "xsd:dateTime");
                            }
                        });
                    }
                });
                // for error expand the correct error is thrown, but the test
                // suite doesn't yet automatically figure that out.
                put("earl:mode", new LinkedHashMap<String, Object>() {
                    {
                        put("@id",
                                "http://json-ld.org/test-suite/tests/error-expand-manifest.jsonld"
                                        .equals(manifest) ? "earl:semiAuto" : "earl:automatic");
                    }
                });
            }
        });

        assertTrue(
                "\nFailed test: "
                        + group
                        + test.get("@id")
                        + " "
                        + test.get("name")
                        + " ("
                        + test.get("input")
                        + ","
                        + test.get("expect")
                        + ")\n"
                        + "expected: "
                        + JSONUtils.toPrettyString(expect)
                        + "\nresult: "
                        + (result instanceof JsonLdError ? ((JsonLdError) result).toString()
                                : JSONUtils.toPrettyString(result)), testpassed);
    }

}
