
package org.springframework.cloud.alibaba.dubbo.http.matcher;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link HttpRequest} 'Accept' header {@link HttpRequestMatcher matcher}
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
public class HttpRequestProducesMatcher extends AbstractHttpRequestMatcher {

    private final List<ProduceMediaTypeExpression> expressions;

    /**
     * Creates a new instance from "produces" expressions. If 0 expressions
     * are provided in total, this condition will match to any request.
     *
     * @param produces produces expressions
     */
    public HttpRequestProducesMatcher(String... produces) {
        this(produces, null);
    }

    /**
     * Creates a new instance with "produces" and "header" expressions. "Header"
     * expressions where the header name is not 'Accept' or have no header value
     * defined are ignored. If 0 expressions are provided in total, this condition
     * will match to any request.
     *
     * @param produces produces expressions
     * @param headers  headers expressions
     */
    public HttpRequestProducesMatcher(String[] produces, String[] headers) {
        this(parseExpressions(produces, headers));
    }

    /**
     * Private constructor accepting parsed media type expressions.
     */
    private HttpRequestProducesMatcher(Collection<ProduceMediaTypeExpression> expressions) {
        this.expressions = new ArrayList<>(expressions);
        Collections.sort(this.expressions);
    }

    @Override
    public boolean match(HttpRequest request) {

        if (expressions.isEmpty()) {
            return true;
        }

        HttpHeaders httpHeaders = request.getHeaders();

        List<MediaType> acceptedMediaTypes = httpHeaders.getAccept();

        for (ProduceMediaTypeExpression expression : expressions) {
            if (!expression.match(acceptedMediaTypes)) {
                return false;
            }
        }

        return true;
    }

    private static Set<ProduceMediaTypeExpression> parseExpressions(String[] produces, String[] headers) {
        Set<ProduceMediaTypeExpression> result = new LinkedHashSet<>();
        if (headers != null) {
            for (String header : headers) {
                HeaderExpression expr = new HeaderExpression(header);
                if (HttpHeaders.ACCEPT.equalsIgnoreCase(expr.name) && expr.value != null) {
                    for (MediaType mediaType : MediaType.parseMediaTypes(expr.value)) {
                        result.add(new ProduceMediaTypeExpression(mediaType, expr.negated));
                    }
                }
            }
        }
        for (String produce : produces) {
            result.add(new ProduceMediaTypeExpression(produce));
        }
        return result;
    }

    @Override
    protected Collection<ProduceMediaTypeExpression> getContent() {
        return expressions;
    }

    @Override
    protected String getToStringInfix() {
        return " || ";
    }
}
