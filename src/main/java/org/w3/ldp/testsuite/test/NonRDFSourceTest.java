package org.w3.ldp.testsuite.test;

import com.google.common.base.Function;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Header;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.marmotta.commons.util.HashUtils;
import org.apache.marmotta.commons.vocabulary.LDP;
import org.hamcrest.CoreMatchers;
import org.openrdf.model.URI;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.annotations.SpecTest.STATUS;
import org.w3.ldp.testsuite.mapper.RdfObjectMapper;
import org.w3.ldp.testsuite.matcher.HeaderMatchers;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.List;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

/**
 * Tests Non-RDF Source LDP resources.
 *
 */
public class NonRDFSourceTest extends CommonResourceTest {

    private final String rootContainer;

    private final URI containerType;

    @Parameters({"basicContainer", "directContainer", "indirectContainer"})
    public NonRDFSourceTest(@Optional String basicContainer, @Optional String directContainer, @Optional String indirectContainer) {
        if (StringUtils.isNotBlank(basicContainer)) {
            rootContainer = basicContainer;
            containerType = LDP.BasicContainer;
        } else if (StringUtils.isNotBlank(directContainer)) {
            rootContainer = directContainer;
            containerType = LDP.DirectContainer;
        } else if (StringUtils.isNotBlank(indirectContainer)) {
            rootContainer = indirectContainer;
            containerType = LDP.IndirectContainer;
        } else {
            throw new SkipException("No root container provided in testng.xml. Skipping LDP Non-RDF Source (LDP-NR) tests.");
        }
    }

    protected String getResourceUri() {
        String randomContainer = RandomStringUtils.randomAlphabetic(16);
        return UriBuilder.fromUri(rootContainer).path(randomContainer).build().toString();
    }

    @Test(
            groups = {MAY, NR},
            description = "LDP servers may accept an HTTP POST of non-RDF " +
                    "representations (LDP-NRs) for creation of any kind of " +
                    "resource, for example binary resources.")
    @SpecTest(
            specRefUri = LdpTestSuite.SPEC_URI + "#dfn-ldp-server",
            testMethod = METHOD.AUTOMATED,
            approval = STATUS.WG_APPROVED)
    public void testPostResource() throws IOException {
        // Test constants
        final String slug = "test",
                file = slug + ".png",
                mimeType = "image/png",
                container = getResourceUri(),
                rs = container + "/" + slug,
                nr = container + "/" + file;

        // Make sure we can post binary resources
        List<Header> links = RestAssured
            .given()
                .header("Slug", slug)
                .body(IOUtils.toByteArray(getClass().getResourceAsStream("/" + file)))
                .contentType(mimeType)
            .expect()
                .statusCode(HttpStatus.SC_CREATED)
                .header("Location", nr)
            .when()
                .post(container)
                .headers().getList("Link");

        Assert.assertTrue(containsLinkHeader(rs, "describedby", links));
        Assert.assertTrue(containsLinkHeader(containerType.stringValue(), "type", links));
    }

    @Test(
            groups = {MAY, NR},
            description = "LDP servers may accept an HTTP POST of non-RDF " +
                    "representations (LDP-NRs) for creation of any kind of " +
                    "resource, for example binary resources.")
    @SpecTest(
            specRefUri = LdpTestSuite.SPEC_URI + "#dfn-ldp-server",
            testMethod = METHOD.AUTOMATED,
            approval = STATUS.WG_PENDING)
    public void testPostResourceAndGetFromContainer() throws IOException {
        // Test constants
        final String slug = "test",
                file = slug + ".png",
                mimeType = "image/png",
                container = getResourceUri(),
                rs = container + "/" + slug,
                nr = container + "/" + file;

        // Make sure we can post binary resources
        List<Header> links = RestAssured
            .given()
                .header("Slug", slug)
                .body(IOUtils.toByteArray(getClass().getResourceAsStream("/" + file)))
                .contentType(mimeType)
            .expect()
                .statusCode(HttpStatus.SC_CREATED)
                .header("Location", nr)
            .when()
                .post(container)
                .headers().getList("Link");
        Assert.assertTrue(containsLinkHeader(rs, "describedby", links));
        Assert.assertTrue(containsLinkHeader(containerType.stringValue(), "type", links));

        // Check the container contains the new resource
        Model model = RestAssured
            .given()
                .header("Accept", TEXT_TURTLE)
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .header("ETag", HeaderMatchers.hasEntityTag(true)) // FIXME: be more specific here
                .contentType(TEXT_TURTLE)
            .get(container)
                .body().as(Model.class, new RdfObjectMapper(container));
        assertTrue(model.contains(model.createResource(container), RDF.type, model.createResource(LDP.Resource.stringValue())));
        assertTrue(model.contains(model.createResource(container), RDF.type, model.createResource(LDP.Container.stringValue())));
        assertTrue(model.contains(model.createResource(container), RDF.type, model.createResource(LDP.Container.stringValue())));
        assertTrue(model.contains(model.createResource(container), DCTerms.modified));
        assertTrue(model.contains(model.createResource(container), model.createProperty(LDP.contains.stringValue()), model.createResource(nr)));

    }

    @Test(
            groups = {MAY, NR},
            description = "LDP servers may host a mixture of LDP-RSs and LDP-NRs. " +
                    "For example, it is common for LDP servers to need to host binary " +
                    "or text resources that do not have useful RDF representations.")
    @SpecTest(
            specRefUri = LdpTestSuite.SPEC_URI + "#h5_ldpr-gen-binary",
            testMethod = METHOD.AUTOMATED,
            approval = STATUS.WG_PENDING)
    public void testPostResourceGetBinary() throws IOException {
        // Test constants
        final String slug = "test",
                file = slug + ".png",
                mimeType = "image/png",
                container = getResourceUri(),
                rs = container + "/" + slug,
                nr = container + "/" + file;

        // Make sure we can post binary resources
        List<Header> links = RestAssured
            .given()
                .header("Slug", slug)
                .body(IOUtils.toByteArray(getClass().getResourceAsStream("/" + file)))
                .contentType(mimeType)
            .expect()
                .statusCode(HttpStatus.SC_CREATED)
                .header("Location", nr)
            .when()
                .post(container)
                .headers().getList("Link");

        Assert.assertTrue(containsLinkHeader(rs, "describedby", links));
        Assert.assertTrue(containsLinkHeader(containerType.stringValue(), "type", links));

        // And then check we get the binary back
        final String expectedMD5 = HashUtils.md5sum(NonRDFSourceTest.class.getResourceAsStream("/" + file));
        final byte[] binary = RestAssured
            .given()
                .header(ACCEPT, mimeType)
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .contentType(mimeType)
                .header("ETag", HeaderMatchers.hasEntityTag()) // FIXME: be more specific here
            .when()
                .get(nr)
                .body().asByteArray();
        assertEquals(expectedMD5, HashUtils.md5sum(binary), "md5sum");
    }

    @Test(
            groups = {MAY, NR},
            description = "Each LDP Non-RDF Source must also be a conforming LDP Resource. " +
                    "LDP Non-RDF Sources may not be able to fully express their state using RDF.")
    @SpecTest(
            specRefUri = LdpTestSuite.SPEC_URI + "#h5_ldpnr-are-ldpr",
            testMethod = METHOD.AUTOMATED,
            approval = STATUS.WG_PENDING)
    public void testPostResourceGetMetadataAndBinary() throws IOException {
        // Test constants
        final String slug = "test",
                file = slug + ".png",
                mimeType = "image/png",
                container = getResourceUri(),
                rs = container + "/" + slug,
                nr = container + "/" + file;

        // Make sure we can post binary resources
        List<Header> links = RestAssured
            .given()
                .header("Slug", slug)
                .body(IOUtils.toByteArray(getClass().getResourceAsStream("/" + file)))
                .contentType(mimeType)
            .expect()
                .statusCode(HttpStatus.SC_CREATED)
                .header("Location", nr)
            .when()
                .post(container)
                .headers().getList("Link");

        Assert.assertTrue(containsLinkHeader(rs, "describedby", links));
        Assert.assertTrue(containsLinkHeader(containerType.stringValue(), "type", links));

        // And then check we get the metadata of back
        Model model = RestAssured
            .given()
                .header(ACCEPT, TEXT_TURTLE)
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .contentType(TEXT_TURTLE)
                .header("ETag", HeaderMatchers.hasEntityTag()) // FIXME: be more specific here
            .when()
                .get(nr)
                .as(Model.class, new RdfObjectMapper(getResourceUri()));

        // And the binary too
        final String expectedMD5 = HashUtils.md5sum(NonRDFSourceTest.class.getResourceAsStream("/" + file));
        final byte[] binary = RestAssured
            .given()
                .header(ACCEPT, mimeType)
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .contentType(mimeType)
                .header("ETag", HeaderMatchers.hasEntityTag()) // FIXME: be more specific here
            .when()
                .get(nr)
                .body().asByteArray();
        assertEquals(expectedMD5, HashUtils.md5sum(binary), "md5sum");
    }

    @Test(
            groups = {MAY, NR},
            description = "LDP servers exposing an LDP Non-RDF Source may advertise this by exposing " +
                    "a HTTP Link header with a target URI of http://www.w3.org/ns/ldp#NonRDFSource, and " +
                    "a link relation type of type (that is, rel='type') in responses to requests made to " +
                    "the LDP-NR's HTTP Request-URI.")
    @SpecTest(
            specRefUri = LdpTestSuite.SPEC_URI + "#h5_ldpnr-type",
            testMethod = METHOD.AUTOMATED,
            approval = STATUS.WG_PENDING)
    public void testPostResourceAndCheckLink() throws IOException {
        // Test constants
        final String slug = "test",
                file = slug + ".png",
                mimeType = "image/png",
                container = getResourceUri(),
                rs = container + "/" + slug,
                nr = container + "/" + file;

        // Make sure we can post binary resources
        List<Header> links = RestAssured
            .given()
                .header("Slug", slug)
                .body(IOUtils.toByteArray(getClass().getResourceAsStream("/" + file)))
                .contentType(mimeType)
            .expect()
                .statusCode(HttpStatus.SC_CREATED)
                .header("Location", nr)
            .when()
                .post(container)
                .headers().getList("Link");
        Assert.assertTrue(containsLinkHeader(rs, "describedby", links));
        Assert.assertTrue(containsLinkHeader(containerType.stringValue(), "type", links));

        // And then check the link when requesting the LDP-NS
        List<Header> linksNR = RestAssured
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .header("ETag", HeaderMatchers.hasEntityTag()) // FIXME: be more specific here
            .when()
                .get(nr)
                .headers().getList("Link");
        Assert.assertTrue(containsLinkHeader(LDP.NonRDFSource.stringValue(), "type", linksNR));
    }

    @Test(
            groups = {MAY, NR},
            description = "Upon successful creation of an LDP-NR (HTTP status code of 201-Created and " +
                    "URI indicated by Location response header), LDP servers may create an associated " +
                    "LDP-RS to contain data about the newly created LDP-NR. If a LDP server creates " +
                    "this associated LDP-RS it must indicate its location on the HTTP response using " +
                    "the HTTP Link response header with link relation describedby and href to be the " +
                    "URI of the associated LDP-RS resource.")
    @SpecTest(
            specRefUri = LdpTestSuite.SPEC_URI + "#h5_ldpc-post-createbinlinkmetahdr",
            testMethod = METHOD.AUTOMATED,
            approval = STATUS.WG_PENDING)
    public void testPostResourceAndCheckAssociatedResource() throws IOException {
        // Test constants
        final String slug = "test",
                file = slug + ".png",
                mimeType = "image/png",
                container = getResourceUri(),
                rs = container + "/" + slug,
                nr = container + "/" + file;

        // Make sure we can post binary resources
        List<Header> links = RestAssured
            .given()
                .header("Slug", slug)
                .body(IOUtils.toByteArray(getClass().getResourceAsStream("/" + file)))
                .contentType(mimeType)
            .expect()
                .statusCode(HttpStatus.SC_CREATED)
                .header("Location", nr)
            .when()
                .post(container)
                .headers().getList("Link");
        Assert.assertTrue(containsLinkHeader(rs, "describedby", links));
        Assert.assertTrue(containsLinkHeader(containerType.stringValue(), "type", links));

        // Check the link when requesting the LDP-NS
        List<Header> linksNR = RestAssured
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .header("ETag", HeaderMatchers.hasEntityTag()) // FIXME: be more specific here
            .when()
                .get(nr)
                .headers().getList("Link");
        Assert.assertTrue(containsLinkHeader(rs, "describedby", linksNR));

        // And then check the associated LDP-RS is actually there
        RestAssured
            .given()
                .header("Accept", TEXT_TURTLE)
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .contentType(TEXT_TURTLE)
                .header("ETag", HeaderMatchers.hasEntityTag(true)) // FIXME: be more specific here
            .when()
                .get(rs);

    }

}
