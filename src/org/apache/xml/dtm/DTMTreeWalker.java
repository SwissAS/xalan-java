/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xalan" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999, Lotus
 * Development Corporation., http://www.lotus.com.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.xml.dtm;

import org.w3c.dom.*;

import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;

import org.apache.xml.utils.NodeConsumer;

/**
 * <meta name="usage" content="advanced"/>
 * This class does a pre-order walk of the DTM tree, calling a ContentHandler
 * interface as it goes.
 * I think normally this class should not be needed, because 
 * of DTM#dispatchToEvents.
 */
public class DTMTreeWalker
{

  /** Local reference to a ContentHandler          */
  private ContentHandler m_contentHandler = null;

  // ARGHH!!  JAXP Uses Xerces without setting the namespace processing to ON!
  // DOM2Helper m_dh = new DOM2Helper();

  /** DomHelper for this TreeWalker          */
  protected DTM m_dtm;

  /**
   * Get the ContentHandler used for the tree walk.
   *
   * @return the ContentHandler used for the tree walk
   */
  public ContentHandler getcontentHandler()
  {
    return m_contentHandler;
  }
  
  /**
   * Constructor.
   * @param   contentHandler The implemention of the
   * contentHandler operation (toXMLString, digest, ...)
   */
  public DTMTreeWalker(ContentHandler contentHandler, DTM dtm)
  {
    this.m_contentHandler = contentHandler;
    m_dtm = dtm;
  }
  
  /**
   * Perform a pre-order traversal non-recursive style.
   *
   * @param pos Node in the tree where to start traversal
   *
   * @throws TransformerException
   */
  public void traverse(int pos) throws org.xml.sax.SAXException
  {

    int top = pos;

    while (DTM.NULL != pos)
    {
      startNode(pos);

      int nextNode = m_dtm.getFirstChild(pos);

      while (DTM.NULL == nextNode)
      {
        endNode(pos);

        if (top == pos)
          break;

        nextNode = m_dtm.getNextSibling(pos);

        if (DTM.NULL == nextNode)
        {
          pos = m_dtm.getParent(pos);

          if ((DTM.NULL == pos) || (top == pos))
          {
            if (DTM.NULL != pos)
              endNode(pos);

            nextNode = DTM.NULL;

            break;
          }
        }
      }

      pos = nextNode;
    }
  }

  /**
   * Perform a pre-order traversal non-recursive style.
   *
   * @param pos Node in the tree where to start traversal
   * @param top Node in the tree where to end traversal
   *
   * @throws TransformerException
   */
  public void traverse(int pos, int top) throws org.xml.sax.SAXException
  {

    while (DTM.NULL != pos)
    {
      startNode(pos);

      int nextNode = m_dtm.getFirstChild(pos);

      while (DTM.NULL == nextNode)
      {
        endNode(pos);

        if ((DTM.NULL != top) && top == pos)
          break;

        nextNode = m_dtm.getNextSibling(pos);

        if (DTM.NULL == nextNode)
        {
          pos = m_dtm.getParent(pos);

          if ((DTM.NULL == pos) || ((DTM.NULL != top) && (top == pos)))
          {
            nextNode = DTM.NULL;

            break;
          }
        }
      }

      pos = nextNode;
    }
  }

  /** Flag indicating whether following text to be processed is raw text          */
  boolean nextIsRaw = false;
  
  /**
   * Optimized dispatch of characters.
   */
  private final void dispatachChars(int node)
     throws org.xml.sax.SAXException
  {
    m_dtm.dispatchCharactersEvents(node, m_contentHandler);
  }

  /**
   * Start processing given node
   *
   *
   * @param node Node to process
   *
   * @throws org.xml.sax.SAXException
   */
  protected void startNode(int node) throws org.xml.sax.SAXException
  {

    if (m_contentHandler instanceof NodeConsumer)
    {
      // %TBD%
//      ((NodeConsumer) m_contentHandler).setOriginatingNode(node);
    }

    switch (m_dtm.getNodeType(node))
    {
    case DTM.COMMENT_NODE :
    {
      String data = m_dtm.getStringValue(node);

      if (m_contentHandler instanceof LexicalHandler)
      {
        LexicalHandler lh = ((LexicalHandler) this.m_contentHandler);

        lh.comment(data.toCharArray(), 0, data.length());
      }
    }
    break;
    case DTM.DOCUMENT_FRAGMENT_NODE :

      // ??;
      break;
    case DTM.DOCUMENT_NODE :
      this.m_contentHandler.startDocument();
      break;
    case DTM.ELEMENT_NODE :

      for (int nsn = m_dtm.getFirstNamespaceNode(node, true); DTM.NULL != nsn;
           nsn = m_dtm.getNextNamespaceNode(nsn, true))
      {
        String prefix = m_dtm.getPrefix(nsn);

        this.m_contentHandler.startPrefixMapping(prefix,
                                                 m_dtm.getStringValue(nsn));
        
      }

      // System.out.println("m_dh.getNamespaceOfNode(node): "+m_dh.getNamespaceOfNode(node));
      // System.out.println("m_dh.getLocalNameOfNode(node): "+m_dh.getLocalNameOfNode(node));
      String ns = m_dtm.getNamespaceURI(node);
      if(null == ns)
        ns = "";
      this.m_contentHandler.startElement(ns,
                                         m_dtm.getLocalName(node),
                                         m_dtm.getNodeName(node),
                                         null /* %TBD% new AttList(atts, m_dh) */);
      break;
    case DTM.PROCESSING_INSTRUCTION_NODE :
    {
      String name = m_dtm.getNodeName(node);

      // String data = pi.getData();
      if (name.equals("xslt-next-is-raw"))
      {
        nextIsRaw = true;
      }
      else
      {
        this.m_contentHandler.processingInstruction(name,
                                                    m_dtm.getStringValue(node));
      }
    }
    break;
    case DTM.CDATA_SECTION_NODE :
    {
      boolean isLexH = (m_contentHandler instanceof LexicalHandler);
      LexicalHandler lh = isLexH
                          ? ((LexicalHandler) this.m_contentHandler) : null;

      if (isLexH)
      {
        lh.startCDATA();
      }
      
      dispatachChars(node);

      {
        if (isLexH)
        {
          lh.endCDATA();
        }
      }
    }
    break;
    case DTM.TEXT_NODE :
    {
      if (nextIsRaw)
      {
        nextIsRaw = false;

        m_contentHandler.processingInstruction(javax.xml.transform.Result.PI_DISABLE_OUTPUT_ESCAPING, "");
        dispatachChars(node);
        m_contentHandler.processingInstruction(javax.xml.transform.Result.PI_ENABLE_OUTPUT_ESCAPING, "");
      }
      else
      {
        dispatachChars(node);
      }
    }
    break;
    case DTM.ENTITY_REFERENCE_NODE :
    {
      if (m_contentHandler instanceof LexicalHandler)
      {
        ((LexicalHandler) this.m_contentHandler).startEntity(
          m_dtm.getNodeName(node));
      }
      else
      {

        // warning("Can not output entity to a pure SAX ContentHandler");
      }
    }
    break;
    default :
    }
  }

  /**
   * End processing of given node 
   *
   *
   * @param node Node we just finished processing
   *
   * @throws org.xml.sax.SAXException
   */
  protected void endNode(int node) throws org.xml.sax.SAXException
  {

    switch (m_dtm.getNodeType(node))
    {
    case DTM.DOCUMENT_NODE :
      this.m_contentHandler.endDocument();
      break;
    case DTM.ELEMENT_NODE :
      String ns = m_dtm.getNamespaceURI(node);
      if(null == ns)
        ns = "";
      this.m_contentHandler.endElement(ns,
                                         m_dtm.getLocalName(node),
                                         m_dtm.getNodeName(node));

      for (int nsn = m_dtm.getFirstNamespaceNode(node, true); DTM.NULL != nsn;
           nsn = m_dtm.getNextNamespaceNode(nsn, true))
      {
        String prefix = m_dtm.getPrefix(nsn);

        this.m_contentHandler.endPrefixMapping(prefix);
      }
      break;
    case DTM.CDATA_SECTION_NODE :
      break;
    case DTM.ENTITY_REFERENCE_NODE :
    {
      if (m_contentHandler instanceof LexicalHandler)
      {
        LexicalHandler lh = ((LexicalHandler) this.m_contentHandler);

        lh.endEntity(m_dtm.getNodeName(node));
      }
    }
    break;
    default :
    }
  }
}  //TreeWalker

