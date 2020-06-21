package com.lightcrafts.utils.xml;

import org.junit.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created by Masahiro Kitagawa on 16/10/09.
 */
public class XmlNodeTest {
    private XmlNode xmlNode;

    @org.junit.Before
    public void setUp() throws Exception {
        final var resource = XmlNodeTest.class.getClassLoader().getResource("test.lzt");
        if (resource == null) {
            fail();
        }
        final var filename = resource.getPath();

        final var factory = DocumentBuilderFactory.newInstance();
        final var docBuilder = factory.newDocumentBuilder();
        final var doc = docBuilder.parse(filename);

        Element elem = doc.getDocumentElement();
        xmlNode = new XmlNode(elem);
    }

    @Test
    public void getVersion() throws Exception {
        assertThat(xmlNode.getVersion(), is(7));
    }

    @Test
    public void getAttributes() throws Exception {
        final var attrs = xmlNode.getAttributes();
        assertThat(attrs.length, is(1));
        assertThat(attrs[0], is("version"));
    }

    @org.junit.Test
    public void clearData() throws Exception {
        xmlNode.clearData();
        // TODO:
    }

}