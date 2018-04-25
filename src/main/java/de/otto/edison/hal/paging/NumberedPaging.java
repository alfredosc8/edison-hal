package de.otto.edison.hal.paging;

import com.damnhandy.uri.template.UriTemplate;
import de.otto.edison.hal.Link;
import de.otto.edison.hal.Links;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.OptionalInt;

import static de.otto.edison.hal.Link.link;
import static de.otto.edison.hal.Link.self;
import static de.otto.edison.hal.Links.linkingTo;
import static java.lang.Integer.MAX_VALUE;
import static java.util.OptionalInt.empty;
import static java.util.OptionalInt.of;

/**
 * A helper class used to create paging links for paged resources that are using numbered page URIs.
 * <p>
 *     By default, NumberedPaging is expecting an UriTemplate having the template variables
 *     'page' and 'pageSize' to create links to 'self', 'first', 'next', 'prev' and 'last' pages.
 *     If you want to use different var names, you should derive from this class and override
 *     {@link #pageNumberVar()} and/or {@link #pageSizeVar()}.
 * </p>
 * <p>
 *     Both zero- and one-based paging is supported.
 * </p>
 * <p>
 *     As specified in <a href="https://tools.ietf.org/html/draft-kelly-json-hal-06#section-4.1.1">Section 4.1.1</a>
 *     of the HAL specification, the {@code _links} object <em>"is an object whose property names are
 *     link relation types (as defined by [RFC5988]) and values are either a Link Object or an array
 *     of Link Objects"</em>.
 * </p>
 * <p>
 *     Paging links like 'first', 'next', and so on should generally be rendered as single Link Objects, so adding these
 *     links to an resource should be done using {@link Links.Builder#single(List)}.
 * </p>
 * <p>
 *     Usage:
 * </p>
 * <pre><code>
 * public class MyHalRepresentation extends HalRepresentation {
 *     public MyHalRepresentation(final NumberedPaging page, final List&lt;Stuff&gt; pagedStuff) {
 *          super(linkingTo()
 *              .single(page.links(
 *                      fromTemplate("http://example.com/api/stuff{?pageNumber,pageSize}"),
 *                      EnumSet.allOf(PagingRel.class)))
 *              .array(pagedStuff
 *                      .stream()
 *                      .limit(page.pageNumber*pageSize)
 *                      .map(stuff -&gt; linkBuilder("item", stuff.href).withTitle(stuff.title).build())
 *                      .collect(toList()))
 *              .build()
 *          );
 *     }
 * }
 * </code></pre>
 */
public class NumberedPaging {
    /**
     * The default template-variable name used to identify the number of the page.
     */
    public static final String PAGE_NUMBER_VAR = "page";
    /**
     * The default template-variable name used to identify the size of the page.
     */
    public static final String PAGE_SIZE_VAR = "pageSize";

    /**
     * The number of the first page. Must be 0 or 1.
     */
    private final int firstPage;

    /**
     * The page number of the current page.
     */
    private final int pageNumber;
    /**
     * The size of the pages.
     */
    private final int pageSize;
    /**
     * More items beyond the current page - or not.
     */
    private final boolean hasMore;
    /**
     * Optionally, the total number of available items. Used to generate the number of the last page
     */
    private final OptionalInt total;

    /**
     * Creates a NumberedPage instance.
     *
     * @param firstPage the number of the first page. Must be 0 or 1.
     * @param pageNumber the current page number.
     * @param pageSize the size of a page.
     * @param hasMore more items beyond this page?
     */
    protected NumberedPaging(final int firstPage, final int pageNumber, final int pageSize, final boolean hasMore) {
        if (firstPage != 0 && firstPage != 1) {
            throw new IllegalArgumentException("Parameter 'firstPage' must be 0 or 1");
        }
        if (pageNumber < firstPage) {
            throw new IllegalArgumentException("Parameter 'pageNumber' must not be less than " + firstPage);
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Parameter 'pageSize' must be greater 0");
        }
        if (hasMore && pageSize == MAX_VALUE) {
            throw new IllegalArgumentException("Unable to calculate next page for unbounded page sizes.");
        }
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.hasMore = hasMore;
        this.total = empty();
        this.firstPage = firstPage;
    }

    /**
     * Creates a NumberedPage instance.
     *
     * @param firstPage the number of the first page. Must be 0 or 1.
     * @param pageNumber the current page number.
     * @param pageSize the size of a page.
     * @param total the total number of available items.
     */
    protected NumberedPaging(final int firstPage, final int pageNumber, final int pageSize, final int total) {
        if (firstPage != 0 && firstPage != 1) {
            throw new IllegalArgumentException("Parameter 'firstPage' must be 0 or 1");
        }
        if (pageNumber < firstPage) {
            throw new IllegalArgumentException("Parameter 'pageNumber' must not be less than " + firstPage);
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Parameter 'pageSize' must be greater 0");
        }
        if (total < 0) {
            throw new IllegalArgumentException("Parameter 'total' must be greater or equal 0");
        }
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.hasMore = pageNumber*pageSize < total;
        this.total = of(total);
        this.firstPage = firstPage;
    }

    /**
     * Create a NumberedPaging instances for pages where paging starts with zero and it is known whether or
     * not there are more items beyond the current page.
     *
     * @param pageNumber the page number of the current page.
     * @param pageSize the number of items per page.
     * @param hasMore true if there are more items beyond the current page, false otherwise.
     * @return created NumberedPaging instance
     */
    public static NumberedPaging zeroBasedNumberedPaging(final int pageNumber, final int pageSize, final boolean hasMore) {
        return new NumberedPaging(0, pageNumber, pageSize, hasMore);
    }

    /**
     * Create a NumberedPaging instances for pages where paging starts with zero and it is known
     * how many items are matching the initial query.
     *
     * @param pageNumber the page number of the current page.
     * @param pageSize the number of items per page.
     * @param totalCount the total number of items matching the initial query.
     * @return created NumberedPaging instance
     */
    public static NumberedPaging zeroBasedNumberedPaging(final int pageNumber, final int pageSize, final int totalCount) {
        return new NumberedPaging(0, pageNumber, pageSize, totalCount);
    }

    /**
     * Create a NumberedPaging instances for pages where paging starts with one and it is known whether
     * or not there are more items beyond the current page.
     *
     * @param pageNumber the page number of the current page.
     * @param pageSize the number of items per page.
     * @param hasMore true if there are more items beyond the current page, false otherwise.
     * @return created NumberedPaging instance
     */
    public static NumberedPaging oneBasedNumberedPaging(final int pageNumber, final int pageSize, final boolean hasMore) {
        return new NumberedPaging(1, pageNumber, pageSize, hasMore);
    }

    /**
     * Create a NumberedPaging instances for pages where paging starts with one and it is known how
     * many items are matching the initial query.
     *
     * @param pageNumber the page number of the current page.
     * @param pageSize the number of items per page.
     * @param totalCount the total number of items matching the initial query.
     * @return created NumberedPaging instance
     */
    public static NumberedPaging oneBasedNumberedPaging(final int pageNumber, final int pageSize, final int totalCount) {
        return new NumberedPaging(1, pageNumber, pageSize, totalCount);
    }

    /**
     * Return the requested links for a paged resource were the link's hrefs are created using the given
     * {@link UriTemplate}.
     * <p>
     *     The variables used to identify the current page number and page size must match the values returned
     *     by {@link #pageNumberVar()} and {@link #pageSizeVar()}. Derive from this class, if other values than
     *     {@link #PAGE_NUMBER_VAR} or {@link #PAGE_SIZE_VAR} are required.
     * </p>
     * <p>
     *     If the provided template does not contain the required variable names. links can not be expanded.
     * </p>
     * @param pageUriTemplate the URI template used to create paging links.
     * @param rels the links expected to be created.
     * @return paging links
     */
    public final Links links(final UriTemplate pageUriTemplate, final EnumSet<PagingRel> rels) {
        final List<Link> links = new ArrayList<>();
        if (rels.contains(PagingRel.SELF)) {
            links.add(
                    self(pageUri(pageUriTemplate, pageNumber, pageSize))
            );
        }
        if (rels.contains(PagingRel.FIRST)) {
            links.add(
                    link("first", pageUri(pageUriTemplate, firstPage, pageSize))
            );
        }
        if (pageNumber > firstPage && rels.contains(PagingRel.PREV)) {
            links.add(
                    link("prev", pageUri(pageUriTemplate, pageNumber-1, pageSize))
            );
        }
        if (hasMore && rels.contains(PagingRel.NEXT)) {
            links.add(
                    link("next", pageUri(pageUriTemplate, pageNumber+1, pageSize))
            );
        }
        if (total.isPresent() && rels.contains(PagingRel.LAST)) {
            links.add(
                    link("last", pageUri(pageUriTemplate, calcLastPage(this.total.getAsInt(), this.pageSize), pageSize))
            );
        }
        return linkingTo().single(links).build();
    }

    /**
     *
     * @return the current page number.
     */
    public int getPageNumber() {
        return pageNumber;
    }

    /**
     *
     * @return the current page size.
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     *
     * @return true, if there are more pages, false otherwise
     */
    public boolean hasMore() {
        return hasMore;
    }

    /**
     * The optional total number of available items in the current selection.
     * <p>
     *     Used to calculate the 'last' page. If this value is empty(), the link
     *     to the last page is not returned by {@link #links(UriTemplate, EnumSet)}.
     * </p>
     * @return total number of available items
     */
    public OptionalInt getTotal() {
        return total;
    }

    /**
     * Returns number of the last page, or {@code Optional.empty()} if {@link #getTotal()} is unknown / empty.
     *
     * @return the number of the last page.
     */
    public OptionalInt getLastPage() {
        if(total.isPresent()) {
            return of(calcLastPage(total.getAsInt(), pageSize));
        } else {
            return empty();
        }
    }
    
    /**
     * The name of the uri-template variable used to identify the current page number. By default,
     * 'page' is used.
     * @return uri-template variable for the current page-number.
     */
    protected String pageNumberVar() {
        return PAGE_NUMBER_VAR;
    }

    /**
     * The name of the uri-template variable used to specify the current page size. By default,
     * 'pageSize' is used.
     * @return uri-template variable for the current page-size.
     */
    protected String pageSizeVar() {
        return PAGE_SIZE_VAR;
    }

    /**
     * Returns the number of the last page, if the total number of items is known.
     *
     * @param total total number of items
     * @param pageSize the current page size
     * @return page number of the last page
     */
    private int calcLastPage(int total, int pageSize) {
        if (total == 0) {
            return firstPage;
        } else {
            final int zeroBasedPageNo = total % pageSize > 0
                    ? total / pageSize
                    : total / pageSize - 1;
            return firstPage + zeroBasedPageNo;
        }
    }

    /**
     * Return the HREF of the page specified by UriTemplate, pageNumber and pageSize.
     *
     * @param uriTemplate the template used to build hrefs.
     * @param pageNumber the number of the linked page.
     * @param pageSize the size of the pages.
     * @return href of the linked page.
     */
    private String pageUri(final UriTemplate uriTemplate, final int pageNumber, final int pageSize) {
        if (pageSize == MAX_VALUE) {
            return uriTemplate.expand();
        }
        return uriTemplate.set(pageNumberVar(), pageNumber).set(pageSizeVar(), pageSize).expand();
    }
}
