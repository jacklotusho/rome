/*
 * Opml10Parser.java
 *
 * Created on April 24, 2006, 11:34 PM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sun.syndication.io.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom2.Document;
import org.jdom2.Element;

import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.feed.opml.Attribute;
import com.sun.syndication.feed.opml.Opml;
import com.sun.syndication.feed.opml.Outline;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedParser;

/**
 * 
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet" Cooper</a>
 */
public class OPML10Parser extends BaseWireFeedParser implements WireFeedParser {
    private static Logger LOG = Logger.getLogger(OPML10Parser.class.getName());

    /** Creates a new instance of Opml10Parser */
    public OPML10Parser() {
        super("opml_1.0", null);
    }

    public OPML10Parser(final String type) {
        super(type, null);
    }

    /**
     * Inspects an XML Document (JDOM) to check if it can parse it.
     * <p>
     * It checks if the given document if the type of feeds the parser understands.
     * <p>
     * 
     * @param document XML Document (JDOM) to check if it can be parsed by this parser.
     * @return <b>true</b> if the parser know how to parser this feed, <b>false</b> otherwise.
     */
    @Override
    public boolean isMyType(final Document document) {
        final Element e = document.getRootElement();

        if (e.getName().equals("opml") && (e.getChild("head") == null || e.getChild("head").getChild("docs") == null)
                && (e.getAttributeValue("version") == null || e.getAttributeValue("version").equals("1.0"))) {
            return true;
        }

        return false;
    }

    /**
     * Parses an XML document (JDOM Document) into a feed bean.
     * <p>
     * 
     * @param document XML document (JDOM) to parse.
     * @param validate indicates if the feed should be strictly validated (NOT YET IMPLEMENTED).
     * @return the resulting feed bean.
     * @throws IllegalArgumentException thrown if the parser cannot handle the given feed type.
     * @throws FeedException thrown if a feed bean cannot be created out of the XML document (JDOM).
     */
    @Override
    public WireFeed parse(final Document document, final boolean validate) throws IllegalArgumentException, FeedException {
        final Opml opml = new Opml();
        opml.setFeedType("opml_1.0");

        final Element root = document.getRootElement();
        final Element head = root.getChild("head");

        if (head != null) {
            opml.setTitle(head.getChildText("title"));

            if (head.getChildText("dateCreated") != null) {
                opml.setCreated(DateParser.parseRFC822(head.getChildText("dateCreated")));
            }

            if (head.getChildText("dateModified") != null) {
                opml.setModified(DateParser.parseRFC822(head.getChildText("dateModified")));
            }

            opml.setOwnerName(head.getChildTextTrim("ownerName"));
            opml.setOwnerEmail(head.getChildTextTrim("ownerEmail"));
            opml.setVerticalScrollState(readInteger(head.getChildText("vertScrollState")));
        }

        try {
            opml.setWindowBottom(readInteger(head.getChildText("windowBottom")));
        } catch (final NumberFormatException nfe) {
            LOG.log(Level.WARNING, "Unable to parse windowBottom", nfe);

            if (validate) {
                throw new FeedException("Unable to parse windowBottom", nfe);
            }
        }

        try {
            opml.setWindowLeft(readInteger(head.getChildText("windowLeft")));
        } catch (final NumberFormatException nfe) {
            LOG.log(Level.WARNING, "Unable to parse windowLeft", nfe);
        }

        try {
            opml.setWindowRight(readInteger(head.getChildText("windowRight")));
        } catch (final NumberFormatException nfe) {
            LOG.log(Level.WARNING, "Unable to parse windowRight", nfe);

            if (validate) {
                throw new FeedException("Unable to parse windowRight", nfe);
            }
        }

        try {
            opml.setWindowLeft(readInteger(head.getChildText("windowLeft")));
        } catch (final NumberFormatException nfe) {
            LOG.log(Level.WARNING, "Unable to parse windowLeft", nfe);

            if (validate) {
                throw new FeedException("Unable to parse windowLeft", nfe);
            }
        }

        try {
            opml.setWindowTop(readInteger(head.getChildText("windowTop")));
        } catch (final NumberFormatException nfe) {
            LOG.log(Level.WARNING, "Unable to parse windowTop", nfe);

            if (validate) {
                throw new FeedException("Unable to parse windowTop", nfe);
            }
        }

        try {
            opml.setExpansionState(readIntArray(head.getChildText("expansionState")));
        } catch (final NumberFormatException nfe) {
            LOG.log(Level.WARNING, "Unable to parse expansionState", nfe);

            if (validate) {
                throw new FeedException("Unable to parse expansionState", nfe);
            }
        }

        opml.setOutlines(parseOutlines(root.getChild("body").getChildren("outline"), validate));
        opml.setModules(parseFeedModules(root));

        return opml;
    }

    protected Outline parseOutline(final Element e, final boolean validate) throws FeedException {
        if (!e.getName().equals("outline")) {
            throw new RuntimeException("Not an outline element.");
        }

        final Outline outline = new Outline();
        outline.setText(e.getAttributeValue("text"));
        outline.setType(e.getAttributeValue("type"));
        outline.setTitle(e.getAttributeValue("title"));

        final List jAttributes = e.getAttributes();
        final ArrayList attributes = new ArrayList();

        for (int i = 0; i < jAttributes.size(); i++) {
            final org.jdom2.Attribute a = (org.jdom2.Attribute) jAttributes.get(i);

            if (!a.getName().equals("isBreakpoint") && !a.getName().equals("isComment") && !a.getName().equals("title") && !a.getName().equals("text")
                    && !a.getName().equals("type")) {
                attributes.add(new Attribute(a.getName(), a.getValue()));
            }
        }

        outline.setAttributes(attributes);

        try {
            outline.setBreakpoint(readBoolean(e.getAttributeValue("isBreakpoint")));
        } catch (final Exception ex) {
            LOG.log(Level.WARNING, "Unable to parse isBreakpoint value", ex);

            if (validate) {
                throw new FeedException("Unable to parse isBreakpoint value", ex);
            }
        }

        try {
            outline.setComment(readBoolean(e.getAttributeValue("isComment")));
        } catch (final Exception ex) {
            LOG.log(Level.WARNING, "Unable to parse isComment value", ex);

            if (validate) {
                throw new FeedException("Unable to parse isComment value", ex);
            }
        }

        final List children = e.getChildren("outline");
        outline.setModules(parseItemModules(e));
        outline.setChildren(parseOutlines(children, validate));

        return outline;
    }

    protected List parseOutlines(final List elements, final boolean validate) throws FeedException {
        final ArrayList results = new ArrayList();

        for (int i = 0; i < elements.size(); i++) {
            results.add(parseOutline((Element) elements.get(i), validate));
        }

        return results;
    }

    protected boolean readBoolean(final String value) {
        if (value == null) {
            return false;
        } else {
            return Boolean.getBoolean(value.trim());
        }
    }

    protected int[] readIntArray(final String value) {
        if (value == null) {
            return null;
        } else {
            final StringTokenizer tok = new StringTokenizer(value, ",");
            final int[] result = new int[tok.countTokens()];
            int count = 0;

            while (tok.hasMoreElements()) {
                result[count] = Integer.parseInt(tok.nextToken().trim());
                count++;
            }

            return result;
        }
    }

    protected Integer readInteger(final String value) {
        if (value != null) {
            return new Integer(value);
        } else {
            return null;
        }
    }
}
