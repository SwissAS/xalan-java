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

/** <code>DTM</code> is an XML document model expressed as a table
 * rather than an object tree. It attempts to provide an interface to
 * a parse tree that has very little object creation. (DTM
 * implementations may also support incremental construction of the
 * model, but that's hidden from the DTM API.)
 * 
 * <p>Nodes in the DTM are identified by integer "handles".  A handle must 
 * be unique within a process, and carries both node identification and 
 * document identification.  It must be possible to compare two handles 
 * (and thus their nodes) for identity with "==".</p>
 * 
 * <p>Namespace URLs, local-names, and expanded-names can all be
 * represented by and tested as integer ID values.  An expanded name
 * represents (and may or may not directly contain) a combination of
 * the URL ID, and the local-name ID.  Note that the namespace URL id
 * can be 0, which should have the meaning that the namespace is null.
 * For consistancy, zero should not be used for a local-name index. </p>
 *
 * <p>Text content of a node is represented by an index and length,
 * permitting efficient storage such as a shared FastStringBuffer.</p>
 *
 * <p>The model of the tree, as well as the general navigation model,
 * is that of XPath 1.0, for the moment.  The model will eventually be
 * adapted to match the XPath 2.0 data model, XML Schema, and
 * InfoSet.</p>
 * 
 * <p>DTM does _not_ directly support the W3C's Document Object
 * Model. However, it attempts to come close enough that an
 * implementation of DTM can be created that wraps a DOM and vice
 * versa.</p>
 * 
 * <p>State: In progress!!</p> */
public interface DTM
{
  /**
   * Null node handles are represented by this value.
   */
  public static final int NULL = -1;

  // These nodeType mnemonics and values are deliberately the same as those
  // used by the DOM, for convenient mapping
  //
  // %REVIEW% Should we actually define these as initialized to,
  // eg. org.w3c.dom.Document.ELEMENT_NODE?

  /**
   * The node is an <code>Element</code>.
   */
  public static final short ELEMENT_NODE              = 1;
  /**
   * The node is an <code>Attr</code>.
   */
  public static final short ATTRIBUTE_NODE            = 2;
  /**
   * The node is a <code>Text</code> node.
   */
  public static final short TEXT_NODE                 = 3;
  /**
   * The node is a <code>CDATASection</code>.
   */
  public static final short CDATA_SECTION_NODE        = 4;
  /**
   * The node is an <code>EntityReference</code>.
   */
  public static final short ENTITY_REFERENCE_NODE     = 5;
  /**
   * The node is an <code>Entity</code>.
   */
  public static final short ENTITY_NODE               = 6;
  /**
   * The node is a <code>ProcessingInstruction</code>.
   */
  public static final short PROCESSING_INSTRUCTION_NODE = 7;
  /**
   * The node is a <code>Comment</code>.
   */
  public static final short COMMENT_NODE              = 8;
  /**
   * The node is a <code>Document</code>.
   */
  public static final short DOCUMENT_NODE             = 9;
  /**
   * The node is a <code>DocumentType</code>.
   */
  public static final short DOCUMENT_TYPE_NODE        = 10;
  /**
   * The node is a <code>DocumentFragment</code>.
   */
  public static final short DOCUMENT_FRAGMENT_NODE    = 11;
  /**
   * The node is a <code>Notation</code>.
   */
  public static final short NOTATION_NODE             = 12;

  /** The node is a <code>namespace node</code>. Note that this is not
   * currently a node type defined by the DOM API.
   * */
  public static final short NAMESPACE_NODE             = 113;
  
  // ========= DTM Implementation Control Functions. ==============

  /**
   * Set a suggested parse block size for the parser.
   *
   * @param blockSizeSuggestion Suggested size of the parse blocks, in bytes.
   */
  public void setParseBlockSize(int blockSizeSuggestion);

  /**
   * Set an implementation dependent feature.
   * <p>
   * %REVIEW% Do we really expect to set features on DTMs?
   *
   * @param featureId A feature URL.
   * @param state true if this feature should be on, false otherwise.
   */
  public void setFeature(String featureId, boolean state);
  

  // ========= Document Navigation Functions =========

  /** Given a node handle, test if it has child nodes.
   * <p> %REVIEW% This is obviously useful at the DOM layer, where it
   * would permit testing this without having to create a proxy
   * node. It's less useful in the DTM API, where
   * (dtm.getFirstChild(nodeHandle)!=DTM.NULL) is just as fast and
   * almost as self-evident. But it's a convenience, and eases porting
   * of DOM code to DTM.  </p>
   * 
   * @param nodeHandle int Handle of the node.
   * @return int true if the given node has child nodes.
   */
  public boolean hasChildNodes(int nodeHandle);
 
  /**
   * Given a node handle, get the handle of the node's first child.
   * 
   * @param nodeHandle int Handle of the node.
   * @return int DTM node-number of first child,
   * or DTM.NULL to indicate none exists.
   */
  public int getFirstChild(int nodeHandle);

  /**
   * Given a node handle, get the handle of the node's last child.
   * 
   * @param nodeHandle int Handle of the node.
   * @return int Node-number of last child,
   * or DTM.NULL to indicate none exists.
   */
  public int getLastChild(int nodeHandle);

  /**
   * Retrieves an attribute node by qualified name. 
   * <br>To retrieve an attribute node by local name and namespace URI, 
   * use the <code>getAttributeNodeNS</code> method.
   *
   * %REVIEW% I don't think XPath model needs it... but DOM support might.
   *
   * @param name The qualified name of the attribute to 
   *   retrieve.
   * @return The attribute node handle with the specified name (
   *   <code>nodeName</code>) or <code>DTM.NULL</code> if there is no such 
   *   attribute.
   */
  public int getAttributeNode(String name);

  /**
   * Retrieves an attribute node by local name and namespace URI
   *
   * @param name The namespace URI of the attribute to 
   *   retrieve, or null.
   * @param name The local name of the attribute to 
   *   retrieve.
   * @return The attribute node handle with the specified name (
   *   <code>nodeName</code>) or <code>DTM.NULL</code> if there is no such 
   *   attribute.
   */
  public int getAttributeNode(String namespaceURI, String name);

  /**
   * Retrieves an attribute node by name.
   * <br>To retrieve an attribute node by qualified name and namespace URI, 
   * use the <code>getAttributeNodeNS</code> method.
   *
   * @param name The namespace URI of the attribute to 
   *   retrieve, or null.
   * @param name The local name of the attribute to 
   *   retrieve.
   * @return The attribute node handle with the specified name (
   *   <code>nodeName</code>) or <code>DTM.NULL</code> if there is no such 
   *   attribute.
   */
  public int getAttributeNode(String namespaceURI, String name);

  /**
   * Given a node handle, get the index of the node's first attribute.
   * 
   * @param nodeHandle int Handle of the node.
   * @return Handle of first attribute, or DTM.NULL to indicate none exists.
   */
  public int getFirstAttribute(int nodeHandle);

  /**
   * Given a node handle, get the index of the node's first namespace node.
   *
   * @param nodeHandle handle to node, which should probably be an element 
   *                   node, but need not be.
   *                   
   * @param inScope true if all namespaces in scope should be
   *                   returned, false if only the node's own
   *                   namespace declarations should be returned.
   * @return handle of first namespace,
   * or DTM.NULL to indicate none exists.
   */
  public int getFirstNamespaceNode(int nodeHandle, boolean inScope);

  /**
   * Given a node handle, advance to its next sibling.
   * @param nodeHandle int Handle of the node.
   * @return int Node-number of next sibling,
   * or DTM.NULL to indicate none exists.
   */
  public int getNextSibling(int nodeHandle);
  
  /**
   * Given a node handle, find its preceeding sibling.
   * WARNING: DTM implementations may be asymmetric; in some,
   * this operation has been resolved by search, and is relatively expensive.
   *
   * @param nodeHandle the id of the node.
   * @return int Node-number of the previous sib,
   * or DTM.NULL to indicate none exists.
   */
  public int getPreviousSibling(int nodeHandle);

  /**
   * Given a node handle, advance to the next attribute. If an
   * element, we advance to its first attribute; if an attr, we advance to
   * the next attr of the same element.
   * 
   * @param nodeHandle int Handle of the node.
   * @return int DTM node-number of the resolved attr,
   * or DTM.NULL to indicate none exists.
   */
  public int getNextAttribute(int nodeHandle);

  /**
   * Given a namespace handle, advance to the next namespace in the same scope
   * (local or local-plus-inherited, as selected by getFirstNamespaceNode)
   * 
   * @param namespaceHandle handle to node which must be of type
   * NAMESPACE_NODE.
   * @return handle of next namespace,
   * or DTM.NULL to indicate none exists.
   */
  public int getNextNamespaceNode(int namespaceHandle, boolean inScope);

  /** Lightweight subtree-walker. Given a node handle, find the next
   * node in document order. (Preorder left-to-right traversal).  The
   * walk stops (returning DTM.NULL) when it would otherwise have to
   * step out of the subtree of the node indicated by the
   * subtreeRootHandle.
   * <p>
   * One application would be as a subroutine for DTMIterators.
   * <p>
   * %REVIEW% Joe would like to rename this to walkNextDescendent
   * to distinguish it more strongly from getFirstChild
   *
   * @param subtreeRootHandle int Handle of the root of the subtree
   * being walked. Sets an outer limit that we will not walk past.
   * @param nodeHandle int Handle of a node within the subtree.
   * @return handle of the next node within the subtree, in document order.
   * or DTM.NULL to indicate none exists.  */
  public int getNextDescendant(int subtreeRootHandle, int nodeHandle);

  /** Lightweight tree-walker. Given a node handle, find the next
   * node in document order. The walk stops (returning DTM.NULL) when
   * it would otherwise run off the end of the document.
   * <p>
   * Note that this is roughly equivalent to getNextDescendent() with
   * subtreeRootHandle set to the Document node (or maybe the root element).
   * <p>
   * TODO: Joe would like to rename this to walkNextFollowing
   * (or perhaps just walkFollowing?)
   * to distinguish it more strongly from getNextSibling.
   *
   * @param axisContextHandle the start of the axis that is being traversed.
   * TODO: As far as Joe can tell, this parameter is unnecessary...?
   * @param nodeHandle the node whose successor we're looking for.
   * @return handle of next node in the DTM tree
   * or DTM.NULL to indicate none exists.
   */
  public int getNextFollowing(int axisContextHandle, int nodeHandle);
  
  /** Lightweight tree-walker. Given a node handle, find the next
   * node in reverse document order. (Postorder right-to-left traversal).  The
   * walk stops (returning DTM.NULL) when it would otherwise run off the
   * beginning of the document.
   * <p>
   * TODO: Joe would like to rename this to walkNextPreceeding
   * (or perhaps just walkPreceeding?)
   * to distinguish it more strongly from getPreviousSibling.
   *
   * @param axisContextHandle the start of the axis that is being traversed.
   * TODO: As far as Joe can tell, this parameter is unnecessary...?
   * @param nodeHandle the node whose predecessor we're looking for.
   * @return handle of next node in the DTM tree
   * or DTM.NULL to indicate none exists.
   */
  public int getNextPreceding(int axisContextHandle, int nodeHandle);

  /**
   * Given a node handle, find its parent node.
   *
   * @param nodeHandle the id of the node.
   * @return int Node handle of parent,
   * or DTM.NULL to indicate none exists.
   */
  public int getParent(int nodeHandle);

  /**
   * Given a node handle, find the owning document node.
   *
   * @param nodeHandle the id of the node.
   * @return int Node handle of document, which should always be valid.
   */
  public int getDocument();
  
  /**
   * Given a node handle, find the owning document node.  This has the exact 
   * same semantics as the DOM Document method of the same name, in that if 
   * the nodeHandle is a document node, it will return NULL.
   *
   * @param nodeHandle the id of the node.
   * @return int Node handle of owning document,
   * or DTM.NULL if the nodeHandle is a document.
   */
  public int getOwnerDocument(int nodeHandle);

  /**
   * Get the string-value of a node as a String object
   * (see http://www.w3.org/TR/xpath#data-model 
   * for the definition of a node's string-value).
   *
   * @param nodeHandle The node ID.
   *
   * @return A string object that represents the string-value of the given node.
   */
  public String getStringValue(int nodeHandle);
  
  /**
   * Get number of character array chunks in 
   * the string-value of a node.
   * (see http://www.w3.org/TR/xpath#data-model 
   * for the definition of a node's string-value).
   * Note that a single text node may have multiple text chunks.
   *
   * @param nodeHandle The node ID.
   *
   * @return number of character array chunks in 
   *         the string-value of a node.
   */
  public int getStringValueChunkCount(int nodeHandle);
  
  /**
   * Get a character array chunk in the string-value of a node.
   * (see http://www.w3.org/TR/xpath#data-model 
   * for the definition of a node's string-value).
   * Note that a single text node may have multiple text chunks.
   *
   * @param nodeHandle The node ID.
   * @param chunkIndex Which chunk to get.
   * @param startAndLen  A two-integer array which, upon return, WILL
   * BE FILLED with values representing the chunk's start position
   * within the returned character buffer and the length of the chunk.
   * @return The character array buffer within which the chunk occurs.
   */
  public char[] getStringValueChunk(int nodeHandle, int chunkIndex, 
                                    int[] startAndLen);

  /**
   * Given a node handle, return an ID that represents the node's expanded name.
   * 
   * @param nodeHandle The handle to the node in question.
   *
   * @return the expanded-name id of the node.
   */
  public int getExpandedNameID(int nodeHandle);
  
  /**
   * Given an expanded name, return an ID.  If the expanded-name does not 
   * exist in the internal tables, the entry will be created, and the ID will 
   * be returned.  Any additional nodes that are created that have this 
   * expanded name will use this ID.
   * 
   * @param nodeHandle The handle to the node in question.
   *
   * @return the expanded-name id of the node.
   */
  public int getExpandedNameID(String namespace, String localName);
  
  /**
   * Given an expanded-name ID, return the local name part.
   *
   * @param ExpandedNameID an ID that represents an expanded-name.
   * @return String Local name of this node.
   */
  public String getLocalNameFromExpandedNameID(int ExpandedNameID);
  
  /**
   * Given an expanded-name ID, return the namespace URI part.
   *
   * @param ExpandedNameID an ID that represents an expanded-name.
   * @return String URI value of this node's namespace, or null if no
   * namespace was resolved.
   */
  public String getNamespaceFromExpandedNameID(int ExpandedNameID);

  /**
   * Given a node handle, return its DOM-style node name. This will
   * include names such as #text or #document.
   *
   * @param nodeHandle the id of the node.
   * @return String Name of this node, which may be an empty string.
   * %REVIEW% Document when empty string is possible...
   */
  public String getNodeName(int nodeHandle);

  /**
   * Given a node handle, return the XPath node name.  This should be
   * the name as described by the XPath data model, NOT the DOM-style
   * name. 
   *
   * @param nodeHandle the id of the node.
   * @return String Name of this node.
   */
  public String getNodeNameX(int nodeHandle);

  /**
   * Given a node handle, return its DOM-style localname.
   * (As defined in Namespaces, this is the portion of the name after any
   * colon character)
   *
   * @param nodeHandle the id of the node.
   * @return String Local name of this node.
   */
  public String getLocalName(int nodeHandle);

  /**
   * Given a namespace handle, return the prefix that the namespace decl is 
   * mapping.
   * Given a node handle, return the prefix used to map to the namespace.
   * (As defined in Namespaces, this is the portion of the name before any
   * colon character).
   * @param postition int Handle of the node.
   *
   * <p> %REVIEW% Are you sure you want "" for no prefix?  </p>
   *
   * @param nodeHandle the id of the node.
   * @return String prefix of this node's name, or "" if no explicit
   * namespace prefix was given.
   */
  public String getPrefix(int nodeHandle);

  /**
   * Given a node handle, return its DOM-style namespace URI
   * (As defined in Namespaces, this is the declared URI which this node's
   * prefix -- or default in lieu thereof -- was mapped to.)
   * @param postition int Handle of the node.
   *
   * @param nodeHandle the id of the node.
   * @return String URI value of this node's namespace, or null if no
   * namespace was resolved.
   */
  public String getNamespaceURI(int nodeHandle);

  /**
   * Given a node handle, return its node value. This is mostly
   * as defined by the DOM, but may ignore some conveniences.
   * <p>
   * @param nodeHandle The node id.
   * @return String Value of this node, or null if not
   * meaningful for this node type.
   */
  public String getNodeValue(int nodeHandle);

  /**
   * Given a node handle, return its DOM-style node type.
   * @param nodeHandle The node id.
   * @return int Node type, as per the DOM's Node._NODE constants.
   */
  public short getNodeType(int nodeHandle);
  
  /**
   * <meta name="usage" content="internal"/>
   * Get the depth level of this node in the tree (equals 1 for
   * a parentless node).
   *
   * @param nodeHandle The node id.
   * @return the number of ancestors, plus one
   */
  public short getLevel(int nodeHandle);
    
  /**
   * Tests whether DTM DOM implementation implements a specific feature and 
   * that feature is supported by this node.
   * @param feature The name of the feature to test.
   * @param version This is the version number of the feature to test.
   *   If the version is not 
   *   specified, supporting any version of the feature will cause the 
   *   method to return <code>true</code>.
   * @return Returns <code>true</code> if the specified feature is 
   *   supported on this node, <code>false</code> otherwise.
   */
  public boolean isSupported(String feature, 
                             String version);
  
  // ============== Document query functions ============== 
  
  /**
   * Return the base URI of the specified node. If it is not known
   * (because the document was parsed from a socket connection or from
   * standard input, for example), the value of this property is null.
   * If you need the document's base URI, you can retrieve the Document
   * node and then ask it this question.
   *
   * @param nodeHandle The node id, which can be any valid node handle.
   * @return the document base URI String object or null if unknown.
   */
  public String getNodeBaseURI(int nodeHandle);

  /**
   * Return the system identifier of the document entity. If
   * it is not known, the value of this property is null.
   *
   * @param nodeHandle The node id, which can be any valid node handle.
   * @return the system identifier String object or null if unknown.
   */
  public String getDocumentSystemIdentifier(int nodeHandle);

  /**
   * Return the name of the character encoding scheme
   *        in which the document entity is expressed.
   *
   * @param nodeHandle The node id, which can be any valid node handle.
   * @return the document encoding String object.
   */
  public String getDocumentEncoding(int nodeHandle);

  /**
   * Return an indication of the standalone status of the document,
   *        either "yes" or "no". This property is derived from the optional
   *        standalone document declaration in the XML declaration at the
   *        beginning of the document entity, and has no value if there is no
   *        standalone document declaration.
   *
   * @param nodeHandle The node id, which can be any valid node handle.
   * @return the document standalone String object, either "yes", "no", or null.
   */
  public String getDocumentStandalone(int nodeHandle);

  /**
   * Return a string representing the XML version of the document. This
   * property is derived from the XML declaration optionally present at the
   * beginning of the document entity, and has no value if there is no XML
   * declaration.
   *
   * @param the document handle
   *
   * @return the document version String object
   */
  public String getDocumentVersion(int documentHandle);

  /**
   * Return an indication of
   * whether the processor has read the complete DTD. Its value is a
   * boolean. If it is false, then certain properties (indicated in their
   * descriptions below) may be unknown. If it is true, those properties
   * are never unknown.
   *
   * @return <code>true</code> if all declarations were processed;
   *         <code>false</code> otherwise.
   */
  public boolean getDocumentAllDeclarationsProcessed();

  /**
   *   A document type declaration information item has the following properties:
   *
   *     1. [system identifier] The system identifier of the external subset, if
   *        it exists. Otherwise this property has no value.
   *
   * @return the system identifier String object, or null if there is none.
   */
  public String getDocumentTypeDeclarationSystemIdentifier();

  /**
   * Return the public identifier of the external subset,
   * normalized as described in 4.2.2 External Entities [XML]. If there is
   * no external subset or if it has no public identifier, this property
   * has no value.
   *
   * @param the document type declaration handle
   *
   * @return the public identifier String object, or null if there is none.
   */
  public int getDocumentTypeDeclarationPublicIdentifier();

  /**
   * Returns the <code>Element</code> whose <code>ID</code> is given by 
   * <code>elementId</code>. If no such element exists, returns 
   * <code>DTM.NULL</code>. Behavior is not defined if more than one element 
   * has this <code>ID</code>. Attributes (including those
   * with the name "ID") are not of type ID unless so defined by DTD/Schema
   * information available to the DTM implementation. 
   * Implementations that do not know whether attributes are of type ID or 
   * not are expected to return <code>DTM.NULL</code>.
   *
   * <p>%REVIEW% Presumably IDs are still scoped to a single document,
   * and this operation searches only within a single document, right?
   * Wouldn't want collisions between DTMs in the same process.</p>
   *
   * @param elementId The unique <code>id</code> value for an element.
   * @return The handle of the matching element.
   */
  public int getElementById(String elementId);
  
  /**
   * The getUnparsedEntityURI function returns the URI of the unparsed
   * entity with the specified name in the same document as the context
   * node (see [3.3 Unparsed Entities]). It returns the empty string if
   * there is no such entity.
   * <p>
   * XML processors may choose to use the System Identifier (if one
   * is provided) to resolve the entity, rather than the URI in the
   * Public Identifier. The details are dependent on the processor, and
   * we would have to support some form of plug-in resolver to handle
   * this properly. Currently, we simply return the System Identifier if
   * present, and hope that it a usable URI or that our caller can
   * map it to one.
   * TODO: Resolve Public Identifiers... or consider changing function name.
   * <p>
   * If we find a relative URI 
   * reference, XML expects it to be resolved in terms of the base URI 
   * of the document. The DOM doesn't do that for us, and it isn't 
   * entirely clear whether that should be done here; currently that's
   * pushed up to a higher level of our application. (Note that DOM Level 
   * 1 didn't store the document's base URI.)
   * TODO: Consider resolving Relative URIs.
   * <p>
   * (The DOM's statement that "An XML processor may choose to
   * completely expand entities before the structure model is passed
   * to the DOM" refers only to parsed entities, not unparsed, and hence
   * doesn't affect this function.)
   *
   * @param name A string containing the Entity Name of the unparsed
   * entity.
   *
   * @return String containing the URI of the Unparsed Entity, or an
   * empty string if no such entity exists.
   */
  public String getUnparsedEntityURI(String name);


  // ============== Boolean methods ================
  
  /**
   * Return true if the xsl:strip-space or xsl:preserve-space was processed 
   * at the construction level.
   */
  boolean supportsPreStripping();
  
  /**
   * Figure out whether nodeHandle2 should be considered as being later
   * in the document than nodeHandle1, in Document Order as defined
   * by the XPath model. This may not agree with the ordering defined
   * by other XML applications.
   * <p>
   * There are some cases where ordering isn't defined, and neither are
   * the results of this function -- though we'll generally return true.
   * <p>
   * TODO: Make sure this does the right thing with attribute nodes!!!
   * <p>
   * TODO: Consider renaming for clarity. Perhaps isDocumentOrder(a,b)?
   *
   * @param firstNodeHandle DOM Node to perform position comparison on.
   * @param secondNodeHandle DOM Node to perform position comparison on.
   * 
   * @return false if secondNode comes before firstNode, otherwise return true.
   * You can think of this as 
   * <code>(firstNode.documentOrderPosition &lt;= secondNode.documentOrderPosition)</code>.  */
  public boolean isNodeAfter(int firstNodeHandle,int secondNodeHandle);

  /** 2. [element content whitespace] A boolean indicating whether a
   * text node represents white space appearing within element content
   * (see [XML], 2.10 "White Space Handling").  Note that validating
   * XML processors are required by XML 1.0 to provide this
   * information... but that DOM Level 2 did not support it, since it
   * depends on knowledge of the DTD which DOM2 could not guarantee
   * would be available.
   * <p>
   * If there is no declaration for the containing element, an XML
   * processor must assume that the whitespace could be meaningful and
   * return false. If no declaration has been read, but the [all
   * declarations processed] property of the document information item
   * is false (so there may be an unread declaration), then the value
   * of this property is indeterminate for white space characters and
   * should probably be reported as false. It is always false for text
   * nodes that contain anything other than (or in addition to) white
   * space.
   * <p>
   * Note too that it always returns false for non-Text nodes.
   * <p>
   * TODO: Joe wants to rename this isWhitespaceInElementContent() for clarity
   *
   * @param nodeHandle the node ID.
   * @return <code>true</code> if the node definitely represents whitespace in
   * element content; <code>false</code> otherwise.
   */
  public boolean isCharacterElementContentWhitespace(int nodeHandle);

  /**
   *    10. [all declarations processed] This property is not strictly speaking
   *        part of the infoset of the document. Rather it is an indication of
   *        whether the processor has read the complete DTD. Its value is a
   *        boolean. If it is false, then certain properties (indicated in their
   *        descriptions below) may be unknown. If it is true, those properties
   *        are never unknown.

   *
   * @param the document handle
   *
   * @param documentHandle A node handle that must identify a document.
   * @return <code>true</code> if all declarations were processed;
   *         <code>false</code> otherwise.
   */
  public boolean isDocumentAllDeclarationsProcessed(int documentHandle);

  /**
   *     5. [specified] A flag indicating whether this attribute was actually
   *        specified in the start-tag of its element, or was defaulted from the
   *        DTD (or schema).
   *
   * @param the attribute handle
   *
   * NEEDSDOC @param attributeHandle
   * @return <code>true</code> if the attribute was specified;
   *         <code>false</code> if it was defaulted or the handle doesn't
   *		refer to an attribute node.
   */
  public boolean isAttributeSpecified(int attributeHandle);

  // ========== Direct SAX Dispatch, for optimization purposes ========
  
  /**
   * Directly call the
   * characters method on the passed ContentHandler for the 
   * string-value of the given node (see http://www.w3.org/TR/xpath#data-model 
   * for the definition of a node's string-value). Multiple calls to the 
   * ContentHandler's characters methods may well occur for a single call to 
   * this method.
   *
   * @param nodeHandle The node ID.
   * @param ch A non-null reference to a ContentHandler.
   *
   * @throws org.xml.sax.SAXException
   */
  public void dispatchCharactersEvents(
    int nodeHandle, org.xml.sax.ContentHandler ch)
      throws org.xml.sax.SAXException;

  /**
   * Directly create SAX parser events representing the XML content of
   * a DTM subtree. This is a "serialize" operation.
   *
   * @param nodeHandle The node ID.
   * @param ch A non-null reference to a ContentHandler.
   *
   * @throws org.xml.sax.SAXException
   */

  public void dispatchToEvents(
    int nodeHandle, org.xml.sax.ContentHandler ch)
      throws org.xml.sax.SAXException;
      
  // ==== Construction methods (may not be supported by some implementations!) =====
      // %REVIEW% What response occurs if not supported?
      // Should it be a separate interface to make that distinction explicit?
      // I suspect we need element and attribute factories, maybe others.
  
  /**
   * Append a child to the end of the document. Please note that the node 
   * is always cloned if it is owned by another document.
   * <p>
   * TODO: "End of the document" needs to be defined better. I believe the
   * intent is equivalent to the DOM sequence
   *	document.getRootElement().appendChild(document.importNode(newChild)))
   * but we need to nail that down more explicitly.
   * <p>
   * TODO: ISSUE -- In DTM, I believe we must ALWAYS clone the node, since
   * the base DTM is immutable and nodes never exist in isolation.
   * 
   * @param newChild Must be a valid new node handle.
   * @param clone true if the child should be cloned into the document.
   * @param cloneDepth if the clone argument is true, specifies that the 
   *                   clone should include all it's children.
   */
  public void appendChild(int newChild, boolean clone, boolean cloneDepth);

  /**
   * Append a text node child that will be constructed from a string, 
   * to the end of the document. Behavior is otherwise like appendChild().
   * 
   * @param str Non-null reverence to a string.
   */
  public void appendTextChild(String str);
}
