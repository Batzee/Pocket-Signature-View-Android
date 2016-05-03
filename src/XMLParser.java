package com.batzeesappstudio.pocketsignatureview;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Amalan Dhananjayan on 4/29/2016.
 * v0.1.5
 */
public class XMLParser extends DefaultHandler {

    List<String> list=null;
    StringBuilder builder;

    @Override
    public void startDocument() throws SAXException {
        list = new ArrayList<String>();
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {
        builder=new StringBuilder();
        if(localName.equals("path")){
            list.add( attributes.getValue("d"));
        }
    }
    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if(localName.equals("path")){

        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        String tempString=new String(ch, start, length);
        builder.append(tempString);
    }
}
