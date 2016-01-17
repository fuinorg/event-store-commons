/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. 
 * http://www.fuin.org/
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.eshttp;

import java.io.IOException;
import java.io.InputStream;

import javax.validation.constraints.NotNull;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Utility classes for the package.
 */
public final class ESHttpUtils {

    private ESHttpUtils() {
        throw new UnsupportedOperationException("It's not allowed to create instances of this utility class");
    }

    /**
     * Returns an string from a node's content.
     * 
     * @param doc
     *            Document to search.
     * @param xPath
     *            XPath to use.
     * @param expression
     *            XPath expression.
     * 
     * @return Node or <code>null</code> if no match was found.
     */
    @Nullable
    public static String findContentText(final Document doc, final XPath xPath, final String expression) {
        final Node node = findNode(doc, xPath, expression);
        if (node == null) {
            return null;
        }
        return node.getTextContent();
    }

    /**
     * Returns an integer value from a node's content.
     * 
     * @param doc
     *            Document to search.
     * @param xPath
     *            XPath to use.
     * @param expression
     *            XPath expression.
     * 
     * @return Node or <code>null</code> if no match was found.
     */
    @Nullable
    public static Integer findContentInteger(final Document doc, final XPath xPath, final String expression) {
        final Node node = findNode(doc, xPath, expression);
        if (node == null) {
            return null;
        }
        final String str = node.getTextContent();
        return Integer.valueOf(str);
    }

    /**
     * Returns a single node from a given document using xpath.
     * 
     * @param doc
     *            Document to search.
     * @param xPath
     *            XPath to use.
     * @param expression
     *            XPath expression.
     * 
     * @return Node or <code>null</code> if no match was found.
     */
    @Nullable
    public static Node findNode(@NotNull final Document doc, @NotNull final XPath xPath,
            @NotNull final String expression) {
        Contract.requireArgNotNull("doc", doc);
        Contract.requireArgNotNull("xPath", xPath);
        Contract.requireArgNotNull("expression", expression);
        try {
            return (Node) xPath.compile(expression).evaluate(doc, XPathConstants.NODE);
        } catch (final XPathExpressionException ex) {
            throw new RuntimeException("Failed to read node: " + expression, ex);
        }
    }

    /**
     * Returns a list of nodes from a given document using xpath.
     * 
     * @param doc
     *            Document to search.
     * @param xPath
     *            XPath to use.
     * @param expression
     *            XPath expression.
     * 
     * @return Nodes or <code>null</code> if no match was found.
     */
    @Nullable
    public static NodeList findNodes(@NotNull final Document doc, @NotNull final XPath xPath,
            @NotNull final String expression) {
        Contract.requireArgNotNull("doc", doc);
        Contract.requireArgNotNull("xPath", xPath);
        Contract.requireArgNotNull("expression", expression);
        try {
            return (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
        } catch (final XPathExpressionException ex) {
            throw new RuntimeException("Failed to read node: " + expression, ex);
        }
    }

    /**
     * Parse the document and wraps checked exceptions into runtime exceptions.
     * 
     * @param builder
     *            Builder to use.
     * @param inputStream
     *            Input stream with XML.
     * 
     * @return Document.
     */
    @NotNull
    public static Document parseDocument(@NotNull final DocumentBuilder builder,
            @NotNull final InputStream inputStream) {
        Contract.requireArgNotNull("builder", builder);
        Contract.requireArgNotNull("inputStream", inputStream);
        try {
            return builder.parse(inputStream);
        } catch (final SAXException | IOException ex) {
            throw new RuntimeException("Failed to parse XML", ex);
        }
    }

    /**
     * Creates a new XPath with a configured namespace context.
     * 
     * @param values
     *            Pairs of 'prefix' + 'uri'.
     * 
     * @return New instance.
     */
    @NotNull
    public static XPath createXPath(@Nullable final String... values) {
        final NamespaceContext context = new NamespaceContextMap(values);
        final XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(context);
        return xPath;
    }

    /**
     * Creates a namespace aware document builder.
     * 
     * @return New instance.
     */
    @NotNull
    public static DocumentBuilder createDocumentBuilder() {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder();
        } catch (final ParserConfigurationException ex) {
            throw new RuntimeException("Couldn't create document builder", ex);
        }
    }

}
