/*
 * @(#)$Id$
 *
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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
 * originally based on software copyright (c) 2001, Sun
 * Microsystems., http://www.sun.com.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * @author Jacek Ambroziak
 * @author Morten Jorgensen
 * @author Erwin Bolwidt <ejb@klomp.org>
 *
 */

package org.apache.xalan.xsltc.dom;

import org.apache.xalan.xsltc.DOM;
import org.apache.xalan.xsltc.StripFilter;
import org.apache.xalan.xsltc.TransletException;
import org.apache.xalan.xsltc.TransletOutputHandler;
import org.apache.xalan.xsltc.runtime.BasisLibrary;
import org.apache.xalan.xsltc.runtime.Hashtable;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMAxisIterator;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.dtm.ref.DTMAxisIteratorBase;
import org.apache.xml.dtm.ref.DTMDefaultBase;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class MultiDOM implements DOM {

    private static final int NO_TYPE = DOM.FIRST_TYPE - 2;
    private static final int INITIAL_SIZE = 4;
    private static final int CLR = 0x00FFFFFF;
    private static final int SET = 0xFF000000;
    
    private DOM[] _adapters;
    private int _free;
    private int _size;

    private Hashtable _documents = new Hashtable();

    private final class AxisIterator extends DTMAxisIteratorBase {
        // constitutive data
        private final int _axis;
        private final int _type;
        // implementation mechanism
        private DTMAxisIterator _source;
        private int _mask;

        public AxisIterator(final int axis, final int type) {
            _axis = axis;
            _type = type;
        }

        public int next() {
            if (_source == null) {
                return(END);
            }
            return _source.next();
        }


        public void setRestartable(boolean flag) {
            _source.setRestartable(flag);
        }

        public DTMAxisIterator setStartNode(final int node) {
            if (node == DTM.NULL) {
                return this;
            }

            int nodeid = getNodeIdent(node); 
            final int mask = nodeid & SET;
            int dom = getDTMId(node);

            // Get a new source first time and when mask changes
            if (_source == null || _mask != mask) {
                if (_type == NO_TYPE) {
                    _source = _adapters[dom].getAxisIterator(_axis);
                } else if (_axis == Axis.CHILD) {
                    _source = _adapters[dom].getTypedChildren(_type);
                } else {
                    _source = _adapters[dom].getTypedAxisIterator(_axis, _type);
                }
            }

            _mask = mask;
            _source.setStartNode(node & CLR);
            return this;
        }

        public DTMAxisIterator reset() {
            if (_source != null) {
                _source.reset();
            }
            return this;
        }
    
        public int getLast() {
            return _source.getLast();
        }

        public int getPosition() {
            return _source.getPosition();
        }
    
        public boolean isReverse() {
	    return Axis.isReverse[_axis];
        }
    
        public void setMark() {
            _source.setMark();
        }
    
        public void gotoMark() {
            _source.gotoMark();
        }
    
        public DTMAxisIterator cloneIterator() {
            final AxisIterator clone = new AxisIterator(_axis, _type);
            clone._source = _source.cloneIterator();
            clone._mask = _mask;
            return clone;
        }
    } // end of AxisIterator


    /**************************************************************
     * This is a specialised iterator for predicates comparing node or
     * attribute values to variable or parameter values.
     */
    private final class NodeValueIterator extends DTMAxisIteratorBase {

        private DTMAxisIterator _source;
        private String _value;
        private boolean _op;
        private final boolean _isReverse;
        private int _returnType = RETURN_PARENT;

        public NodeValueIterator(DTMAxisIterator source, int returnType,
                                 String value, boolean op) {
            _source = source;
            _returnType = returnType;
            _value = value;
            _op = op;
            _isReverse = source.isReverse();
        }

        public boolean isReverse() {
            return _isReverse;
        }
    
        public DTMAxisIterator cloneIterator() {
            try {
                NodeValueIterator clone = (NodeValueIterator)super.clone();
                clone._source = _source.cloneIterator();
                clone.setRestartable(false);
                return clone.reset();
            }
            catch (CloneNotSupportedException e) {
                BasisLibrary.runTimeError(BasisLibrary.ITERATOR_CLONE_ERR,
                                          e.toString());
                return null;
            }
        }


        public void setRestartable(boolean isRestartable) {
            _isRestartable = isRestartable;
            _source.setRestartable(isRestartable);
        }

        public DTMAxisIterator reset() {
            _source.reset();
            return resetPosition();
        }

        public int next() {

            int node;
            while ((node = _source.next()) != END) {
                String val = getStringValueX(node);
                if (_value.equals(val) == _op) {
                    if (_returnType == RETURN_CURRENT)
                        return returnNode(node);
                    else
                        return returnNode(getParent(node));
                }
            }
            return END;
        }

        public DTMAxisIterator setStartNode(int node) {
            if (_isRestartable) {
                _source.setStartNode(_startNode = node); 
                return resetPosition();
            }
            return this;
        }

        public void setMark() {
            _source.setMark();
        }

        public void gotoMark() {
            _source.gotoMark();
        }
    }                       

    public MultiDOM(DOM main) {
        _size = INITIAL_SIZE;
        _free = 1;
        _adapters = new DOM[INITIAL_SIZE];
        _adapters[0] = main;

        // %HZ% %REVISIT% Is this the right thing to do here?  In the old
        // %HZ% %REVISIT% version, the main document did not get added through
        // %HZ% %REVISIT% a call to addDOMAdapter, which meant it couldn't be
        // %HZ% %REVISIT% found by a call to getDocumentMask.  The problem is
        // %HZ% %REVISIT% TransformerHandler is typically constructed with a
        // %HZ% %REVISIT% system ID equal to the stylesheet's URI; with SAX
        // %HZ% %REVISIT% input, it ends up giving that URI to the document.
        // %HZ% %REVISIT% Then, any references to document('') are resolved
        // %HZ% %REVISIT% using the stylesheet's URI.
        // %HZ% %REVISIT% MultiDOM.getDocumentMask is called to verify that
        // %HZ% %REVISIT% a document associated with that URI has not been
        // %HZ% %REVISIT% encountered, and that method ends up returning the
        // %HZ% %REVISIT% mask of the main document, when what we really what
        // %HZ% %REVISIT% is to read the stylesheet itself!
        addDOMAdapter((DOMAdapter) main, false);
    }

    public int nextMask() {
        return(_free << 24);
    }

    public void setupMapping(String[] names, String[] namespaces) {
        // This method only has a function in DOM adapters
    }

    public int addDOMAdapter(DOMAdapter adapter) {
        return addDOMAdapter(adapter, true);
    }

    private int addDOMAdapter(DOMAdapter adapter, boolean indexByURI) {
        // Add the DOM adapter to the array of DOMs
        DOM dom = adapter.getDOMImpl();
        
        DTMManager dtmManager = null;
        if (dom instanceof DTMDefaultBase) {
            dtmManager = ((DTMDefaultBase)dom).m_mgr;
        }
        else if (dom instanceof SimpleResultTreeImpl) {
            dtmManager = ((SimpleResultTreeImpl)dom).getDTMManager();
        }
                
        final int domNo =
             dtmManager.getDTMIdentity((DTM)dom)
                   >>> DTMManager.IDENT_DTM_NODE_BITS;
  
        if (domNo >= _size) {
            int oldSize = _size;
            do {
            	_size *= 2;
            } while (_size <= domNo);
            
            final DOMAdapter[] newArray = new DOMAdapter[_size];
            System.arraycopy(_adapters, 0, newArray, 0, oldSize);
            _adapters = newArray;
        }
        _adapters[domNo] = adapter;

        // Store reference to document (URI) in hashtable
        if (indexByURI) {
            String uri = adapter.getDocumentURI(0);
            _documents.put(uri, new Integer(domNo));
        }
        
        // If the dom is an AdaptiveResultTreeImpl, we need to create a
        // DOMAdapter around its nested dom object (if it is non-null) and
        // add the DOMAdapter to the list.
        if (dom instanceof AdaptiveResultTreeImpl) {
            AdaptiveResultTreeImpl adaptiveRTF = (AdaptiveResultTreeImpl)dom;
            DOM nestedDom = adaptiveRTF.getNestedDOM();
            if (nestedDom != null) {
                DOMAdapter newAdapter = new DOMAdapter(nestedDom, 
                                                       adapter.getNamesArray(),
                                                       adapter.getNamespaceArray());
                addDOMAdapter(newAdapter);  
            } 
        }
        
        // Store mask in DOMAdapter
        adapter.setMultiDOMMask(domNo << 24);
        return (domNo << 24);
    }
        
    public int getDocumentMask(String uri) {
        Integer domIdx = (Integer)_documents.get(uri);
        if (domIdx == null) {
            return(-1);
        } else {
            return((domIdx.intValue() << 24));
        }
    }
    
    public DOM getDOMAdapter(String uri) {
        Integer domIdx = (Integer)_documents.get(uri);
        if (domIdx == null) {
            return(null);
        } else {
            return(_adapters[domIdx.intValue()]);
        }
    }
    
    public int getDocument() 
    {
        return _adapters[0].getDocument();
    }

    /** 
      * Returns singleton iterator containing the document root 
      */
    public DTMAxisIterator getIterator() {
        // main source document @ 0
        return _adapters[0].getIterator();
    }
    
    public String getStringValue() {
        return _adapters[0].getStringValue();
    }
    
    public DTMAxisIterator getChildren(final int node) {
        return _adapters[getDTMId(node)].getChildren(node);
    }
    
    public DTMAxisIterator getTypedChildren(final int type) {
        return new AxisIterator(Axis.CHILD, type);
    }
    
    public DTMAxisIterator getAxisIterator(final int axis) {
        return new AxisIterator(axis, NO_TYPE);
    }
    
    public DTMAxisIterator getTypedAxisIterator(final int axis, final int type)
    {
        return new AxisIterator(axis, type);
    }

    public DTMAxisIterator getNthDescendant(int node, int n,
                                            boolean includeself)
    {
        return _adapters[getDTMId(node)].getNthDescendant(node & CLR,n,
                                                          includeself);
    }

    public DTMAxisIterator getNodeValueIterator(DTMAxisIterator iterator,
                                                int type, String value,
                                                boolean op)
    {
        return(new NodeValueIterator(iterator, type, value, op));
    }

    public DTMAxisIterator getNamespaceAxisIterator(final int axis,
                                                    final int ns)
    {
        DTMAxisIterator iterator = _adapters[0].getNamespaceAxisIterator(axis,
                                                                         ns);
        return(iterator);        
    }

    public DTMAxisIterator orderNodes(DTMAxisIterator source, int node) {
        return _adapters[getDTMId(node)].orderNodes(source, node & CLR);
    }

    public int getExpandedTypeID(final int node) {
        return _adapters[getDTMId(node)].getExpandedTypeID(node & CLR);
    }

    public int getNamespaceType(final int node) {
        return _adapters[getDTMId(node)].getNamespaceType(node & CLR);
    }
    
    public int getNSType(int node)
   {
        return _adapters[getDTMId(node)].getNSType(node & CLR);
   }
    
    public int getParent(final int node) {
        if (node == DTM.NULL) {
            return DTM.NULL;
        }
        return _adapters[getDTMId(node)].getParent(node & CLR) | node&SET;
    }
    
    public int getAttributeNode(final int type, final int el) {
        if (el == DTM.NULL) {
            return DTM.NULL;
        }
        return _adapters[getDTMId(el)].getAttributeNode(type, el&CLR) | el&SET;
    }
    
    public String getNodeName(final int node) {
        if (node == DTM.NULL) {
            return "";
        }
        return _adapters[getDTMId(node)].getNodeName(node & CLR);
    }
    
    public String getNodeNameX(final int node) {
        if (node == DTM.NULL) {
            return "";
        }
        return _adapters[getDTMId(node)].getNodeNameX(node & CLR);
    }

    public String getNamespaceName(final int node) {
        if (node == DTM.NULL) {
            return "";
        }
        return _adapters[getDTMId(node)].getNamespaceName(node & CLR);
    }
    
    public String getStringValueX(final int node) {
        if (node == DTM.NULL) {
            return "";
        }
        return _adapters[getDTMId(node)].getStringValueX(node & CLR);
    }
    
    public void copy(final int node, TransletOutputHandler handler)
        throws TransletException
    {
        if (node != DTM.NULL) {
            _adapters[getDTMId(node)].copy(node & CLR, handler);
        }
    }
    
    public void copy(DTMAxisIterator nodes, TransletOutputHandler handler)
            throws TransletException
    {
        int node;
        while ((node = nodes.next()) != DTM.NULL) {
            _adapters[getDTMId(node)].copy(node & CLR, handler);
        }
    }


    public String shallowCopy(final int node, TransletOutputHandler handler)
            throws TransletException
    {
        if (node == DTM.NULL) {
            return "";
        }
        return _adapters[getDTMId(node)].shallowCopy(node & CLR, handler);
    }
    
    public boolean lessThan(final int node1, final int node2) {
        if (node1 == DTM.NULL) {
            return true;
        }
        if (node2 == DTM.NULL) {
            return false;
        }
        final int dom1 = getDTMId(node1);
        final int dom2 = getDTMId(node2);
        return dom1 == dom2 ? _adapters[dom1].lessThan(node1 & CLR, node2 & CLR)
                            : dom1 < dom2;
    }
    
    public void characters(final int textNode, TransletOutputHandler handler)
                 throws TransletException
    {
        _adapters[getDTMId(textNode)].characters(textNode & CLR, handler);
    }

    public void setFilter(StripFilter filter) {
        for (int dom=0; dom<_free; dom++) {
            _adapters[dom].setFilter(filter);
        }
    }

    public Node makeNode(int index) {
        if (index == DTM.NULL) {
            return null;
        }
        return _adapters[getDTMId(index)].makeNode(index & CLR);
    }

    public Node makeNode(DTMAxisIterator iter) {
        // TODO: gather nodes from all DOMs ?
        return _adapters[0].makeNode(iter);
    }

    public NodeList makeNodeList(int index) {
        if (index == DTM.NULL) {
            return null;
        }
        return _adapters[getDTMId(index)].makeNodeList(index & CLR);
    }

    public NodeList makeNodeList(DTMAxisIterator iter) {
        // TODO: gather nodes from all DOMs ?
        return _adapters[0].makeNodeList(iter);
    }

    public String getLanguage(int node) {
        return _adapters[getDTMId(node)].getLanguage(node & CLR);
    }

    public int getSize() {
        int size = 0;
        for (int i=0; i<_size; i++) {
            size += _adapters[i].getSize();
        }
        return(size);
    }

    public String getDocumentURI(int node) {
        if (node == DTM.NULL) {
            node = DOM.NULL;
        }
        return _adapters[getDTMId(node)].getDocumentURI(0);
    }

    public boolean isElement(final int node) {
        if (node == DTM.NULL) {
            return false;
        }
        return(_adapters[getDTMId(node)].isElement(node & CLR));
    }

    public boolean isAttribute(final int node) {
        if (node == DTM.NULL) {
            return false;
        }
        return(_adapters[getDTMId(node)].isAttribute(node & CLR));
    }
    
    public int getDTMId(int nodeHandle)
    {
        DTMManager dtmManager =
              ((DTMDefaultBase)((DOMAdapter)_adapters[0]).getDOMImpl()).m_mgr;
        int id = dtmManager.getDTMIdentity(dtmManager.getDTM(nodeHandle))
                       >>> DTMManager.IDENT_DTM_NODE_BITS;
        return (id == -1 ? 0 : id);
    }
    
    public int getNodeIdent(int nodeHandle)
    {
        int id = getDTMId(nodeHandle);
        return (_adapters[id].getNodeIdent(nodeHandle) | id<<24);
    }
    
    public int getNodeHandle(int nodeId)
    {
        return _adapters[nodeId>>>24].getNodeHandle(nodeId & CLR);
    }
    
    public DOM getResultTreeFrag(int initSize, int rtfType)
    {
        return _adapters[0].getResultTreeFrag(initSize, rtfType);
    }
    
    public DOM getMain()
    {
        return _adapters[0];
    }
    
    /**
     * Returns a DOMBuilder class wrapped in a SAX adapter.
     */
    public TransletOutputHandler getOutputDomBuilder()
    {
        return _adapters[0].getOutputDomBuilder();
    }

    public String lookupNamespace(int node, String prefix) 
        throws TransletException
    {
        return _adapters[node>>>24].lookupNamespace(node & CLR, prefix);
    }

    // %HZ% Does this method make any sense here???
    public String getUnparsedEntityURI(String entity) {
        return _adapters[0].getUnparsedEntityURI(entity);
    }

    // %HZ% Does this method make any sense here???
    public Hashtable getElementsWithIDs() {
        return _adapters[0].getElementsWithIDs();
    }
}
