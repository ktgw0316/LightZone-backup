/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.utils.xml;

import com.lightcrafts.utils.TextUtil;
import com.lightcrafts.utils.file.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.w3c.dom.CharacterData;
import org.w3c.dom.*;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;

import static com.lightcrafts.image.metadata.XMPConstants.XMP_XAP_NS;

/**
 * An <code>XMLUtil</code> is a set of utility functions for dealing with XML.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class XMLUtil {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Adds an {@link Element} child to the given element.
     * <p>
     * If you only want to generate elements for a document that is to be
     * serialized, this method is fine.  If, however, you want to manipulate
     * the document, you should probably use
     * {@link #addElementChildTo(Element,String,String)}.
     *
     * @param parent The {@link Element} to add the {@link Element} child to.
     * @param tagName The tag name of the new {@link Element} child.
     * @return Returns A new {@link Element} with its <code>nodeName</code> set
     * to <code>tagName</code>, and <code>localName</code>, <code>prefix</code>,
     * and <code>namespaceURI</code> set to <code>null</code>.
     * @see #addElementChildTo(Element,String,String)
     */
    public static Element addElementChildTo( Element parent,
                                             String tagName ) {
        final Document doc = parent.getOwnerDocument();
        final Element child = doc.createElement( tagName );
        parent.appendChild( child );
        return child;
    }

    /**
     * Adds an {@link Element} child to the given element.
     *
     * @param parent The {@link Element} to add the {@link Element} child to.
     * @param qualifiedName The fully qualified name of the new {@link Element}
     * child.
     * @return Returns the new {@link Element} child.
     */
    public static Element addElementChildTo( Element parent,
                                             String nsURI,
                                             String qualifiedName ) {
        final Document doc = parent.getOwnerDocument();
        final Element child = doc.createElementNS( nsURI, qualifiedName );
        parent.appendChild( child );
        return child;
    }

    /**
     * Creates a new {@link Document}.
     *
     * @return Returns said {@link Document}.
     */
    public static Document createDocument() {
        return m_builder.newDocument();
    }

    /**
     * Creates a new {@link Document}.
     *
     * @param rootElementName The name of the root element.
     * @return Returns said {@link Document}.
     */
    public static Document createDocument( String rootElementName ) {
        return createDocument( rootElementName, null );
    }

    /**
     * Creates a new {@link Document}.
     *
     * @param rootElementName The name of the root element.
     * @param xmlns The namespace of the document.
     * @return Returns said {@link Document}.
     */
    public static Document createDocument( String rootElementName,
                                           String xmlns ) {
        final Document doc = createDocument();
        final Element root = doc.createElement( rootElementName );
        if ( xmlns != null )
            root.setAttribute( "xmlns", xmlns );
        doc.appendChild( root );
        return doc;
    }

    /**
     * Encodes the given {@link Document} into a <code>byte</code> array using
     * UTF-8 encoding.
     *
     * @param doc The {@link Document} to encode.
     * @param includeXAPURL If <code>true</code>, precede the encoded document
     * by the XMP XAP URL and a null byte.
     * @return Returns said array.
     * @see #writeDocumentTo(Document,File)
     * @see #writeDocumentTo(Document,OutputStream)
     */
    public static byte[] encodeDocument( Document doc, boolean includeXAPURL ) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            if ( includeXAPURL ) {
                bos.write( XMP_XAP_NS.getBytes( "UTF-8" ) );
                bos.write( 0 );
            }
            writeDocumentTo( doc, bos );
            return bos.toByteArray();
        }
        catch ( IOException e ) {
            //
            // Since we're writing to an in-memory byte array, an I/O error
            // should never happen.
            //
            throw new IllegalStateException( e );
        }
    }

    /**
     * Gets the names of all the attributes of the given {@link Element}.
     *
     * @param element The {@link Element} to get the attribute names of.
     * @return Returns an array, possibly empty, of the attribute names.
     */
    public static String[] getAttributesNamesOf( Element element ) {
        final NamedNodeMap map = element.getAttributes();
        final int numAttributes = map.getLength();
        final String[] result = new String[ numAttributes ];
        for ( int i = 0; i < numAttributes; ++i )
            result[i] = map.item( i ).getNodeName();
        return result;
    }

    /**
     * Gets all the child nodes of an {@link Element} that satisfy the given
     * filter.
     *
     * @param parent The {@link Element} to get the child nodes of.
     * @param filter The {@link XMLFilter} to use.
     * @return Returns an array, possibly empty, of the child nodes that
     * satisfy the given filter.
     */
    public static Node[] getChildrenOf( Element parent, XMLFilter filter ) {
        val result = new ArrayList<Node>();
        val children = asList(parent.getChildNodes());
        for (val child : children) {
            if ( filter.accept( child ) )
                result.add( child );
        }
        return result.toArray( new Node[0] );
    }

    /**
     * Gets all the text child nodes of an {@link Element} and coalesces them
     * together into one string.
     *
     * @param parent The {@link Element} to get the text child nodes of.
     * @return Returns the coalesced text.
     */
    public static String getCoalescedTextChildrenOf( Element parent ) {
        val buf = new StringBuilder();
        val children = asList(parent.getChildNodes());
        for (val child : children) {
            if ( child instanceof Text )
                buf.append( ((CharacterData)child).getData() );
        }
        return buf.toString();
    }

    /**
     * Gets the first child node of the given {@link Element} that satisfies
     * the given filter.
     *
     * @param parent The {@link Element} to get the child of.
     * @param filter The {@link XMLFilter} to use.
     * @return Returns the first child node that satisfies the filter or
     * <code>null</code> if either there are no children or none satisfy the
     * filter.
     */
    public static Node getFirstChildOf( Element parent, XMLFilter filter ) {
        val children = asList(parent.getChildNodes());
        for (val child : children) {
            if ( filter.accept( child ) )
                return child;
        }
        return null;
    }

    /**
     * Gets the text of the first text node child of the given {@link Element}.
     *
     * @param parent The {@link Element}
     * @return Returns the text of the first text node child or
     * <code>null</code> if there are no text node children.
     */
    public static String getTextOfFirstTextChildOf( Element parent ) {
        final Node child = getFirstChildOf( parent, m_textNodeTypeFilter );
        return child != null ? ((CharacterData)child).getData() : null;
    }

    /**
     * Checks whether an {@link Element} has a child node that satisfies the
     * given filter.
     *
     * @param parent The {@link Element} to check.
     * @param filter The {@link XMLFilter} to use.
     * @return Returns <code>true</code> only if the {@link Element} has a
     * child node that satisfies the given filter.
     */
    public static boolean hasChild( Element parent, XMLFilter filter ) {
        val children = asList(parent.getChildNodes());
        for (val child : children) {
            if ( filter.accept( child ) )
                return true;
        }
        return false;
    }

    /**
     * Reads an XML document from the given {@link File} and creates a
     * {@link Document} out of it.
     *
     * @param file The {@link File} containing XML to read.
     * @return Returns a new {@link Document}.
     */
    public static Document readDocumentFrom( File file ) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return readDocumentFrom(fis);
        }
    }

    /**
     * Reads an XML document from the given stream and creates a
     * {@link Document} out of it.
     *
     * @param is The {@link InputStream} to read.
     * @return Returns a new {@link Document}.
     */
    public static Document readDocumentFrom( InputStream is )
        throws IOException
    {
        return readDocumentFrom( FileUtil.readEntireStream( is ) );
    }

    /**
     * Reads XML from the given {@link String} and creates a {@link Document}
     * out of it.
     *
     * @param s The {@link String} to read.
     * @return Returns a new {@link Document}.
     * @throws IllegalArgumentException if there is an error parsing the XML
     * document contained in the {@link String}.
     */
    public static Document readDocumentFrom( String s ) throws IOException {
        s = TextUtil.trimNulls( s );
        final InputStream is = new ByteArrayInputStream( s.getBytes( "UTF-8" ) );
        try {
            synchronized (m_builder) {
                return m_builder.parse( is );
            }
        }
        catch ( SAXException e ) {
            throw new IOException("Couldn't read XML", e);
        }
    }

    /**
     * Removes all child nodes from the given element.
     *
     * @param parent The {@link Element} to remove child nodes from.
     * @see #removeChildrenFrom(Element,XMLFilter)
     */
    public static void removeChildrenFrom( Element parent ) {
        while ( true ) {
            val child = parent.getLastChild();
            if ( child == null )
                return;
            parent.removeChild( child );
        }
    }

    /**
     * Removes all child nodes from the given element that satisfy the given
     * filter.
     *
     * @param parent The {@link Element} to remove child nodes from.
     * @param filter The {@link XMLFilter} to use.
     * @see #removeChildrenFrom(Element)
     */
    public static void removeChildrenFrom( Element parent, XMLFilter filter ) {
        val children = asReversedArray(parent.getChildNodes());
        for (val child : children) {
            if (filter.accept(child)) {
                parent.removeChild(child);
            }
        }
    }

    /**
     * Replaces all child nodes of the given node with a single text node
     * having the given content.
     * <p>
     * We'd like to use {@link Node#setTextContent(String)}, but some users
     * get an <code>AbstractMethodError</code> even though this seems
     * impossible.
     *
     * @param parent The node to set the text content of.
     * @param text The text that is to be the content of a new {@link Text}
     * node.
     * @return Returns the new {@link Text} node.
     */
    public static Text setTextContentOf( Element parent, String text ) {
        removeChildrenFrom( parent );
        final Document doc = parent.getOwnerDocument();
        final Text child = doc.createTextNode( text );
        parent.appendChild( child );
        return child;
    }

    /**
     * Writes the given {@link Document} to a {@link File}.
     *
     * @param doc The {@link Document} to write.
     * @param file The {@link File} to write to.
     * @see #encodeDocument(Document,boolean)
     * @see #writeDocumentTo(Document,OutputStream)
     */
    public static void writeDocumentTo( Document doc, File file ) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            writeDocumentTo(doc, fos);
        }
    }

    /**
     * Writes the given {@link Document} to an {@link OutputStream} using
     * UTF-8 encoding.
     *
     * @param doc The {@link Document} to write.
     * @param out The {@link OutputStream} to write to.
     * @see #encodeDocument(Document,boolean)
     * @see #writeDocumentTo(Document,File)
     */
    public static void writeDocumentTo( Document doc, OutputStream out )
        throws IOException
    {
        final DOMSource source = new DOMSource( doc );
        final Writer writer = new OutputStreamWriter( out, "UTF-8" );
        final StreamResult result = new StreamResult( writer );
        final Transformer xform = createTransformer();
        try {
            xform.transform( source, result );
        }
        catch ( TransformerException e ) {
            throw new IOException( "Couldn't write XML: " + e.getMessage() );
        }
    }

    /**
     * Create a {@link List} of {@link Node}s from {@link NodeList}.
     * @param nodes The {@link NodeList}.
     * @return Said {@link List} of Nodes.
     */
    static List<Node> asList(NodeList nodes) {
        return nodes.getLength() == 0
                ? Collections.<Node>emptyList()
                : new NodeListWrapper(nodes, false);
    }

    /**
     * Create a reversed {@link List} of {@link Node}s from {@link NodeList}.
     * @param nodes The {@link NodeList}.
     * @return Said {@link List} of Nodes.
     */
    static List<Node> asListReversed(NodeList nodes) {
        return nodes.getLength() == 0
                ? Collections.<Node>emptyList()
                : new NodeListWrapper(nodes, true);
    }

    /**
     * Create a reversed array of {@link Node}s from {@link NodeList}.
     * @param nodes The {@link NodeList}.
     * @return Said array of Nodes.
     */
    static Node[] asReversedArray(NodeList nodes) {
        val list = asListReversed(nodes);
        return list.toArray(new Node[list.size()]);
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Create a {@link Transformer} just the way we like it.
     *
     * @return Returns a new {@link Transformer}.
     */
    private static Transformer createTransformer() {
        final Transformer xform;
        try {
            xform = m_xformFactory.newTransformer();
        }
        catch ( TransformerConfigurationException e ) {
            throw new IllegalStateException( e );
        }
        xform.setOutputProperty( OutputKeys.INDENT, "yes" );
        xform.setOutputProperty(
            "{http://xml.apache.org/xalan}indent-amount", "2"
        );
        xform.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
        return xform;
    }

    @RequiredArgsConstructor
    private static class NodeListWrapper
            extends AbstractList<Node> implements RandomAccess {
        private final NodeList nodes;
        private final boolean isReversed;

        @Override
        public Node get(int index) {
            return isReversed
                    ? nodes.item(nodes.getLength() - 1 - index)
                    : nodes.item(index);
        }

        @Override
        public int size() {
            return nodes.getLength();
        }
    }

    private static final DocumentBuilder m_builder;

    private static final XMLFilter m_textNodeTypeFilter =
        new NodeTypeFilter( Node.TEXT_NODE );

    private static final TransformerFactory m_xformFactory =
        TransformerFactory.newInstance();

    static {
        try {
            final var dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            m_builder = dbf.newDocumentBuilder();

            m_builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) {
                    // Suppress XML parse errors on System.err
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXParseException {
                    throw e;
                }

                @Override
                public void error(SAXParseException e) throws SAXParseException {
                    throw e;
                }
            });
        }
        catch ( Exception e ) {
            throw new IllegalStateException( e );
        }
        try {
            m_xformFactory.setAttribute( "indent-number", "2" );
        }
        catch (IllegalArgumentException e) {
            // ignore, file will still be correct.
        }
    }
}
/* vim:set et sw=4 ts=4: */
