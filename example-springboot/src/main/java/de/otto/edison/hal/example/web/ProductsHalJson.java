package de.otto.edison.hal.example.web;

import de.otto.edison.hal.Embedded;
import de.otto.edison.hal.HalRepresentation;
import de.otto.edison.hal.example.shop.Product;

import java.util.List;

import static de.otto.edison.hal.Embedded.embeddedBuilder;
import static de.otto.edison.hal.Embedded.emptyEmbedded;
import static de.otto.edison.hal.Link.linkBuilder;
import static de.otto.edison.hal.Links.linkingTo;
import static java.util.stream.Collectors.toList;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequestUri;

/**
 * HAL reprensentation of a list of products.
 */
class ProductsHalJson extends HalRepresentation {

    private static final String REL_EXAMPLE_TEMPLATE = "http://localhost:8080/rels/{rel}";
    private static final String REL_EXAMPLE_PRODUCT = REL_EXAMPLE_TEMPLATE.replace("{rel}", "product");
    private static final String REL_SEARCH = "search";
    private static final String APPLICATION_HAL_JSON = "application/hal+json";

    ProductsHalJson(final List<Product> products, final boolean embedded) {
        super(
                linkingTo()
                        .self(fromCurrentRequestUri().toUriString())
                        .curi("ex", REL_EXAMPLE_TEMPLATE)
                        .single(linkBuilder(REL_SEARCH, "/api/products{?q,embedded}")
                                .withTitle("Search Products")
                                .withType(APPLICATION_HAL_JSON)
                                .build())
                        .array(products
                                .stream()
                                .map(b -> linkBuilder(REL_EXAMPLE_PRODUCT, "/api/products/" + b.id)
                                        .withTitle(b.title)
                                        .withType("application/hal+json")
                                        .build())
                                .collect(toList()))
                        .build(),
                embedded ? withEmbedded(products) : emptyEmbedded()
        );
    }

    private static Embedded withEmbedded(final List<Product> products) {
        return embeddedBuilder()
                .with(REL_EXAMPLE_PRODUCT, products
                        .stream()
                        .map(ProductHalJson::new)
                        .collect(toList()))
                .build();
    }
}
