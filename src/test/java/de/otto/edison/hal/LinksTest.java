package de.otto.edison.hal;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import java.util.List;

import static de.otto.edison.hal.Link.*;
import static de.otto.edison.hal.LinkPredicates.*;
import static de.otto.edison.hal.Links.emptyLinks;
import static de.otto.edison.hal.Links.linkingTo;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class LinksTest {

    @Test
    public void shouldCreateEmptyLinks() {
        Links links = emptyLinks();
        assertThat(links.isEmpty(), is(true));
    }

    @Test
    public void shouldCreateLinks() {
        final Links links = linkingTo().self("http://example.org").build();
        assertThat(links.getLinkBy("self").isPresent(), is(true));
        assertThat(links.getLinkBy("self").get().getRel(), is("self"));
        assertThat(links.getLinkBy("self").get().getHref(), is("http://example.org"));
    }

    @Test
    public void shouldCreateMultipleLinks() {
        final Links links = linkingTo()
                .self("http://example.org/items/42")
                .single(collection("http://example.org/items"))
                .build();
        assertThat(links.getLinkBy("self").isPresent(), is(true));
        assertThat(links.getLinkBy("collection").isPresent(), is(true));
    }

    @Test
    public void shouldCreateMultipleLinksFromList() {
        final Links links = linkingTo()
                .single(asList(self("http://example.org/items")))
                .array(asList(
                    item("http://example.org/items/1"),
                    item("http://example.org/items/2"))

                ).build();
        assertThat(links.getLinkBy("self").isPresent(), is(true));
        assertThat(links.getLinkBy("item").isPresent(), is(true));
        assertThat(links.getLinksBy("item"), hasSize(2));
    }

    @Test
    public void shouldCreateMultipleLinksUsingBuilder() {
        final Links links = linkingTo()
                .self("http://example.org/items")
                .array(
                        item("http://example.org/items/1"),
                        item("http://example.org/items/2"))
                .build();
        assertThat(links.getLinkBy("self").isPresent(), is(true));
        assertThat(links.getLinkBy("item").isPresent(), is(true));
        assertThat(links.getLinksBy("item"), hasSize(2));
    }

    @Test
    public void shouldGetFirstLink() {
        final Links links = linkingTo()
                .item("http://example.org/items/42")
                .item("http://example.org/items/44")
                .build();
        assertThat(links.getLinkBy("item").isPresent(), is(true));
        assertThat(links.getLinkBy("item").get().getHref(), is("http://example.org/items/42"));
    }

    @Test
    public void shouldGetFirstLinkMatchingTypeAndProfile() {
        final Links links = linkingTo()
                .array(
                        linkBuilder("item", "http://example.org/items/42").build(),
                        linkBuilder("item", "http://example.org/items/42").withType("text/plain").withProfile("myprofile").build(),
                        linkBuilder("item", "http://example.org/items/42").withType("text/html").withProfile("THEprofile").build(),
                        linkBuilder("item", "http://example.org/items/42").withType("THEtype").withProfile("THEprofile").build())
                .build();
        assertThat(links.getLinkBy("item", havingType("THEtype").and(havingProfile("THEprofile"))).isPresent(), is(true));
        assertThat(links.getLinkBy("item", havingType("THEtype").and(havingProfile("THEprofile"))).get().getHref(), is("http://example.org/items/42"));
        assertThat(links.getLinkBy("item", havingType("text/plain")).get().getProfile(), is("myprofile"));
        assertThat(links.getLinkBy("item", havingProfile("THEprofile")).get().getType(), is("text/html"));
    }

    @Test
    public void shouldGetEmptyLinkForUnknownRel() {
        final Links links = emptyLinks();
        assertThat(links.getLinkBy("item").isPresent(), is(false));
    }

    @Test
    public void shouldGetEmptyLinkForFilteredUnknownRel() {
        final Links links = emptyLinks();
        assertThat(links.getLinkBy("item", havingType("text/plain")).isPresent(), is(false));
    }

    @Test
    public void shouldGetEmptyLinkForFilteredLink() {
        final Links links = linkingTo()
                .item("http://example.org/items/42")
                .build();
        assertThat(links.getLinkBy("item", havingType("text/plain")).isPresent(), is(false));
    }

    @Test
    public void shouldGetAllLinks() {
        final Links links = linkingTo()
                .item("http://example.org/items/42")
                .item("http://example.org/items/44")
                .build();
        assertThat(links.getLinksBy("item"), hasSize(2));
        assertThat(links.getLinksBy("item").get(0).getHref(), is("http://example.org/items/42"));
        assertThat(links.getLinksBy("item").get(1).getHref(), is("http://example.org/items/44"));
    }

    @Test
    public void shouldRemoveLinks() {
        final Links links = linkingTo()
                .item("http://example.org/items/42")
                .item("http://example.org/items/44")
                .build();
        links.remove("item");
        links.remove("does-not-exist");
        assertThat(links.isEmpty(), is(true));
    }

    @Test
    public void shouldGetLinksMatchingName() {
        final Links links = linkingTo()
                .array(
                        linkBuilder("item", "http://example.org/items/42").withName("Foo").withType("text/html").build(),
                        linkBuilder("item", "http://example.org/items/42").withName("Foo").build(),
                        linkBuilder("item", "http://example.org/items/42").withName("Bar").build())
                .build();
        assertThat(links.getLinksBy("item", havingName("Foo")), contains(
                linkBuilder("item", "http://example.org/items/42").withName("Foo").withType("text/html").build(),
                linkBuilder("item", "http://example.org/items/42").withName("Foo").build()));
        assertThat(links.getLinksBy("item", havingName("Foo").and(havingType("text/html"))), contains(
                linkBuilder("item", "http://example.org/items/42").withName("Foo").withType("text/html").build()));
    }

    @Test
    public void shouldGetLinksMatchingNameOrEmpty() {
        final Links links = linkingTo()
                .array(
                        linkBuilder("item", "http://example.org/items/41").build(),
                        linkBuilder("item", "http://example.org/items/42").withName("Foo").build(),
                        linkBuilder("item", "http://example.org/items/43").withName("Bar").build())
                .build();
        assertThat(links.getLinksBy("item", optionallyHavingName("Foo")), contains(
                linkBuilder("item", "http://example.org/items/41").build(),
                linkBuilder("item", "http://example.org/items/42").withName("Foo").build()));
    }

    @Test
    public void shouldGetAllLinkMatchingTypeAndProfile() {
        final Links links = linkingTo()
                .array(
                        linkBuilder("item", "http://example.org/items/42").build(),
                        linkBuilder("item", "http://example.org/items/42").withType("text/plain").withProfile("myprofile").build(),
                        linkBuilder("item", "http://example.org/items/42").withType("text/html").withProfile("THEprofile").build(),
                        linkBuilder("item", "http://example.org/items/42").withType("THEtype").withProfile("THEprofile").build())
                .build();
        assertThat(links.getLinksBy("item", havingType("THEtype").and(havingProfile("THEprofile"))).get(0).getHref(), is("http://example.org/items/42"));
        assertThat(links.getLinksBy("item", havingType("text/plain")).get(0).getProfile(), is("myprofile"));
        assertThat(links.getLinksBy("item", havingProfile("THEprofile")).get(0).getType(), is("text/html"));
    }

    @Test
    public void shouldStreamAllLinks() {
        final Links links = linkingTo().single(
                link("foo", "http://example.org/foo"),
                link("bar", "http://example.org/bar")).build();
        assertThat(links.stream().count(), is(2L));
    }

    @Test
    public void shouldGetAllLinkrelations() {
        final Links links = linkingTo().array(
                link("foo", "http://example.org/foo"),
                link("bar", "http://example.org/bar")).build();
        assertThat(links.getRels(), contains("foo", "bar"));
    }

    @Test
    public void shouldGetEmptyListForUnknownRel() {
        final Links links = emptyLinks();
        assertThat(links.getLinksBy("item"), hasSize(0));
    }

    @Test
    public void shouldGetCuriedLinksFromFullRel() throws JsonProcessingException {
        final Links links = linkingTo()
                .curi("o", "http://spec.otto.de/rels/{rel}")
                .array(link("o:product", "http://example.org/products/42"),
                link("o:product", "http://example.org/products/44")).build();
        final List<String> productHrefs = links.getLinksBy("http://spec.otto.de/rels/product")
                .stream()
                .map(Link::getHref)
                .collect(toList());
        assertThat(productHrefs, contains("http://example.org/products/42","http://example.org/products/44"));
    }

    @Test
    public void shouldGetCuriedLinksFromFullRelWithPredicate() throws JsonProcessingException {
        final Links links = linkingTo()
                .curi("o", "http://spec.otto.de/rels/{rel}")
                .array(
                linkBuilder("o:product", "http://example.org/products/42").withName("First").build(),
                linkBuilder("o:product", "http://example.org/products/44").withName("Second").build()).build();
        final List<String> productHrefs = links.getLinksBy("http://spec.otto.de/rels/product", havingName("Second"))
                .stream()
                .map(Link::getHref)
                .collect(toList());
        assertThat(productHrefs, contains("http://example.org/products/44"));

    }

    @Test
    public void shouldGetCuriedLinksFromCuriedRel() throws JsonProcessingException {
        final Links links = linkingTo()
                .curi("o", "http://spec.otto.de/rels/{rel}")
                .array(link("o:product", "http://example.org/products/42"))
                .array(link("o:product", "http://example.org/products/44"))
                .build();
        final List<String> productHrefs = links.getLinksBy("o:product")
                .stream()
                .map(Link::getHref)
                .collect(toList());
        assertThat(productHrefs, contains("http://example.org/products/42","http://example.org/products/44"));
    }

    @Test
    public void shouldGetCuriedLinksFromCuriedRelWithPredicate() throws JsonProcessingException {
        final Links links = linkingTo()
                .curi("o", "http://spec.otto.de/rels/{rel}")
                .array(
                        linkBuilder("o:product", "http://example.org/products/42").withName("First").build(),
                        linkBuilder("o:product", "http://example.org/products/44").withName("Second").build())
                .build();
        final List<String> productHrefs = links.getLinksBy("o:product", havingName("Second"))
                .stream()
                .map(Link::getHref)
                .collect(toList());
        assertThat(productHrefs, contains("http://example.org/products/44"));

    }

    @Test
    public void shouldReplaceFullRelsWithCuriedRels() throws JsonProcessingException {
        final Links links = linkingTo()
                .curi("o", "http://spec.otto.de/rels/{rel}")
                .array(
                        link("http://spec.otto.de/rels/product", "http://example.org/products/42"),
                        link("http://spec.otto.de/rels/product", "http://example.org/products/44"))
                .build();
        final List<String> productHrefs = links.getLinksBy("o:product")
                .stream()
                .map(Link::getHref)
                .collect(toList());
        assertThat(productHrefs, contains("http://example.org/products/42","http://example.org/products/44"));
    }

    @Test
    public void shouldReplaceFullRelsWithCuriedRelsAfterConstruction() throws JsonProcessingException {
        final Links links = linkingTo()
                .array(
                        link("http://spec.otto.de/rels/product", "http://example.org/products/42"),
                        link("http://spec.otto.de/rels/product", "http://example.org/products/44"))
                .curi("o", "http://spec.otto.de/rels/{rel}")
                .build();
        final List<String> productHrefs = links.getLinksBy("o:product")
                .stream()
                .map(Link::getHref)
                .collect(toList());
        assertThat(productHrefs, contains("http://example.org/products/42","http://example.org/products/44"));
    }

    @Test
    public void shouldIgnoreMissingCuries() throws JsonProcessingException {
        final Links links = linkingTo().array(
                link("o:product", "http://example.org/products/42"),
                link("o:product", "http://example.org/products/44"))
                .build();
        final List<String> productHrefs = links.getLinksBy("o:product")
                .stream()
                .map(Link::getHref)
                .collect(toList());
        assertThat(productHrefs, contains("http://example.org/products/42","http://example.org/products/44"));
    }

}