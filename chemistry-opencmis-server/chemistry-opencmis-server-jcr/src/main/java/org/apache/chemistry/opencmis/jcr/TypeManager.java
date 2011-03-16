/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.chemistry.opencmis.jcr;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.definitions.DocumentTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.ContentStreamAllowed;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.Converter;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractPropertyDefinition;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractTypeDefinition;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.DocumentTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FolderTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyHtmlDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyUriDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionContainerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionListImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Type Manager.
 */
public class TypeManager {
    private static final Log log = LogFactory.getLog(TypeManager.class);

    public static final String DOCUMENT_TYPE_ID = "cmis:document";
    public static final String DOCUMENT_UNVERSIONED_TYPE_ID = "cmis:unversioned-document";
    public static final String FOLDER_TYPE_ID = "cmis:folder";
    public static final String RELATIONSHIP_TYPE_ID = "cmis:relationship";
    public static final String POLICY_TYPE_ID = "cmis:policy";
    private static final String NAMESPACE = "http://opencmis.org/jcr";

    private final Map<String, TypeDefinitionContainerImpl> fTypes;

    public TypeManager() {
        fTypes = new HashMap<String, TypeDefinitionContainerImpl>();

        // folder type
        FolderTypeDefinitionImpl folderType = new FolderTypeDefinitionImpl();
        folderType.setBaseTypeId(BaseTypeId.CMIS_FOLDER);
        folderType.setIsControllableAcl(false);
        folderType.setIsControllablePolicy(false);
        folderType.setIsCreatable(true);
        folderType.setDescription("Folder");
        folderType.setDisplayName("Folder");
        folderType.setIsFileable(true);
        folderType.setIsFulltextIndexed(false);
        folderType.setIsIncludedInSupertypeQuery(true);
        folderType.setLocalName("Folder");
        folderType.setLocalNamespace(NAMESPACE);
        folderType.setIsQueryable(false);
        folderType.setQueryName("cmis:folder");
        folderType.setId(FOLDER_TYPE_ID);

        addBasePropertyDefinitions(folderType);
        addFolderPropertyDefinitions(folderType);

        addTypeInternal(folderType);

        // document type
        DocumentTypeDefinitionImpl documentType = new DocumentTypeDefinitionImpl();
        documentType.setBaseTypeId(BaseTypeId.CMIS_DOCUMENT);
        documentType.setIsControllableAcl(false);
        documentType.setIsControllablePolicy(false);
        documentType.setIsCreatable(true);
        documentType.setDescription("Document");
        documentType.setDisplayName("Document");
        documentType.setIsFileable(true);
        documentType.setIsFulltextIndexed(false);
        documentType.setIsIncludedInSupertypeQuery(true);
        documentType.setLocalName("Document");
        documentType.setLocalNamespace(NAMESPACE);
        documentType.setIsQueryable(false);
        documentType.setQueryName("cmis:document");
        documentType.setId(DOCUMENT_TYPE_ID);
        documentType.setIsVersionable(true);
        documentType.setContentStreamAllowed(ContentStreamAllowed.ALLOWED);

        addBasePropertyDefinitions(documentType);
        addDocumentPropertyDefinitions(documentType);

        addTypeInternal(documentType);

        // non versionable document type
        DocumentTypeDefinitionImpl unversionedDocument = new DocumentTypeDefinitionImpl();
        unversionedDocument.initialize(documentType);

        unversionedDocument.setDescription("Unversioned document");
        unversionedDocument.setDisplayName("Unversioned document");
        unversionedDocument.setLocalName("Unversioned document");
        unversionedDocument.setQueryName("cmis:unversioned-document");
        unversionedDocument.setId(DOCUMENT_UNVERSIONED_TYPE_ID);
        unversionedDocument.setParentTypeId(DOCUMENT_TYPE_ID);

        unversionedDocument.setIsVersionable(false);
        unversionedDocument.setContentStreamAllowed(ContentStreamAllowed.ALLOWED);

        addBasePropertyDefinitions(unversionedDocument);
        addDocumentPropertyDefinitions(unversionedDocument);

        addTypeInternal(unversionedDocument);
    }

    /**
     * Adds a type to collection with inheriting base type properties.
     * @param type  type to add
     * @return  <code>true</code> iff the type was successfully added
     */
    public boolean addType(TypeDefinition type) {
        if (type == null) {
            return false;
        }

        if (type.getBaseTypeId() == null) {
            return false;
        }

        // find base type
        TypeDefinition baseType;
        if (type.getBaseTypeId() == BaseTypeId.CMIS_DOCUMENT) {
            baseType = copyTypeDefintion(fTypes.get(DOCUMENT_TYPE_ID).getTypeDefinition());
        }
        else if (type.getBaseTypeId() == BaseTypeId.CMIS_FOLDER) {
            baseType = copyTypeDefintion(fTypes.get(FOLDER_TYPE_ID).getTypeDefinition());
        }
        else if (type.getBaseTypeId() == BaseTypeId.CMIS_RELATIONSHIP) {
            baseType = copyTypeDefintion(fTypes.get(RELATIONSHIP_TYPE_ID).getTypeDefinition());
        }
        else if (type.getBaseTypeId() == BaseTypeId.CMIS_POLICY) {
            baseType = copyTypeDefintion(fTypes.get(POLICY_TYPE_ID).getTypeDefinition());
        }
        else {
            return false;
        }

        AbstractTypeDefinition newType = (AbstractTypeDefinition) copyTypeDefintion(type);

        // copy property definition
        for (PropertyDefinition<?> propDef : baseType.getPropertyDefinitions().values()) {
            ((AbstractPropertyDefinition<?>) propDef).setIsInherited(true);
            newType.addPropertyDefinition(propDef);
        }

        // add it
        addTypeInternal(newType);

        log.info("Added type '" + newType.getId() + "'.");

        return true;
    }

    /**
     * See CMIS 1.0 section 2.2.2.3 getTypeChildren
     */
    public TypeDefinitionList getTypeChildren(String typeId, boolean includePropertyDefinitions,
            BigInteger maxItems, BigInteger skipCount) {

        TypeDefinitionListImpl result = new TypeDefinitionListImpl(new ArrayList<TypeDefinition>());

        int skip = skipCount == null ? 0 : skipCount.intValue();
        if (skip < 0) {
            skip = 0;
        }

        int max = maxItems == null ? Integer.MAX_VALUE : maxItems.intValue();
        if (max < 1) {
            return result;
        }

        if (typeId == null) {
            if (skip < 1) {
                result.getList().add(copyTypeDefintion(fTypes.get(FOLDER_TYPE_ID).getTypeDefinition()));
                max--;
            }
            if (skip < 2 && max > 0) {
                result.getList().add(copyTypeDefintion(fTypes.get(DOCUMENT_TYPE_ID).getTypeDefinition()));
                max--;
            }

            result.setHasMoreItems(result.getList().size() + skip < 2);
            result.setNumItems(BigInteger.valueOf(2));
        }
        else {
            TypeDefinitionContainer tc = fTypes.get(typeId);
            if (tc == null || tc.getChildren() == null) {
                return result;
            }

            for (TypeDefinitionContainer child : tc.getChildren()) {
                if (skip > 0) {
                    skip--;
                    continue;
                }

                result.getList().add(copyTypeDefintion(child.getTypeDefinition()));

                max--;
                if (max == 0) {
                    break;
                }
            }

            result.setHasMoreItems(result.getList().size() + skip < tc.getChildren().size());
            result.setNumItems(BigInteger.valueOf(tc.getChildren().size()));
        }

        if (!includePropertyDefinitions) {
            for (TypeDefinition type : result.getList()) {
                type.getPropertyDefinitions().clear();
            }
        }

        return result;
    }

    /**
     * See CMIS 1.0 section 2.2.2.4 getTypeDescendants
     */
    public List<TypeDefinitionContainer> getTypesDescendants(String typeId, BigInteger depth,
            Boolean includePropertyDefinitions) {

        List<TypeDefinitionContainer> result = new ArrayList<TypeDefinitionContainer>();

        // check depth
        int d = depth == null ? -1 : depth.intValue();
        if (d == 0) {
            throw new CmisInvalidArgumentException("Depth must not be 0!");
        }

        // set property definition flag to default value if not set
        boolean ipd = Boolean.TRUE.equals(includePropertyDefinitions);

        if (typeId == null) {
            result.add(getTypesDescendants(d, fTypes.get(FOLDER_TYPE_ID), ipd));
            result.add(getTypesDescendants(d, fTypes.get(DOCUMENT_TYPE_ID), ipd));
        }
        else {
            TypeDefinitionContainer tc = fTypes.get(typeId);
            if (tc != null) {
                result.add(getTypesDescendants(d, tc, ipd));
            }
        }

        return result;
    }

    public TypeDefinition getTypeDefinition(String typeId) {
        TypeDefinitionContainer tc = fTypes.get(typeId);
        if (tc == null) {
            throw new CmisObjectNotFoundException("Type '" + typeId + "' is unknown!");
        }

        return copyTypeDefintion(tc.getTypeDefinition());
    }

    public static boolean isVersionable(TypeDefinition typeDef) {
        return typeDef instanceof DocumentTypeDefinition
                ? ((DocumentTypeDefinition) typeDef).isVersionable()
                : false;
    }

    //------------------------------------------< internal >---

    /**
     * For internal use.
     * @param typeId
     * @return
     */
    TypeDefinition getType(String typeId) {
        TypeDefinitionContainer tc = fTypes.get(typeId);
        return tc == null ? null : tc.getTypeDefinition();
    }

    //------------------------------------------< private >---

    private static void addBasePropertyDefinitions(AbstractTypeDefinition type) {
        type.addPropertyDefinition(createPropDef(PropertyIds.BASE_TYPE_ID, "Base Type Id", "Base Type Id",
                PropertyType.ID, Cardinality.SINGLE, Updatability.READONLY, false, true));

        type.addPropertyDefinition(createPropDef(PropertyIds.OBJECT_ID, "Object Id", "Object Id", PropertyType.ID,
                Cardinality.SINGLE, Updatability.READONLY, false, true));

        type.addPropertyDefinition(createPropDef(PropertyIds.OBJECT_TYPE_ID, "Type Id", "Type Id", PropertyType.ID,
                Cardinality.SINGLE, Updatability.ONCREATE, false, true));

        type.addPropertyDefinition(createPropDef(PropertyIds.NAME, "Name", "Name", PropertyType.STRING,
                Cardinality.SINGLE, Updatability.READWRITE, false, true));

        type.addPropertyDefinition(createPropDef(PropertyIds.CREATED_BY, "Created By", "Created By",
                PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY, false, true));

        type.addPropertyDefinition(createPropDef(PropertyIds.CREATION_DATE, "Creation Date", "Creation Date",
                PropertyType.DATETIME, Cardinality.SINGLE, Updatability.READONLY, false, true));

        type.addPropertyDefinition(createPropDef(PropertyIds.LAST_MODIFIED_BY, "Last Modified By", "Last Modified By",
                PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY, false, true));

        type.addPropertyDefinition(createPropDef(PropertyIds.LAST_MODIFICATION_DATE, "Last Modification Date",
                "Last Modification Date", PropertyType.DATETIME, Cardinality.SINGLE, Updatability.READONLY, false, true));

        type.addPropertyDefinition(createPropDef(PropertyIds.CHANGE_TOKEN, "Change Token", "Change Token",
                PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY, false, false));
    }

    private static void addFolderPropertyDefinitions(FolderTypeDefinitionImpl type) {
        type.addPropertyDefinition(createPropDef(PropertyIds.PARENT_ID, "Parent Id", "Parent Id", PropertyType.ID,
                Cardinality.SINGLE, Updatability.READONLY, false, false));

        type.addPropertyDefinition(createPropDef(PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS,
                "Allowed Child Object Type Ids", "Allowed Child Object Type Ids", PropertyType.ID, Cardinality.MULTI,
                Updatability.READONLY, false, false));

        type.addPropertyDefinition(createPropDef(PropertyIds.PATH, "Path", "Path", PropertyType.STRING,
                Cardinality.SINGLE, Updatability.READONLY, false, false));
    }

    private static void addDocumentPropertyDefinitions(DocumentTypeDefinitionImpl type) {
        type.addPropertyDefinition(createPropDef(PropertyIds.IS_IMMUTABLE, "Is Immutable", "Is Immutable",
                PropertyType.BOOLEAN, Cardinality.SINGLE, Updatability.READONLY, false, false));

        type.addPropertyDefinition(createPropDef(PropertyIds.IS_LATEST_VERSION, "Is Latest Version",
                "Is Latest Version", PropertyType.BOOLEAN, Cardinality.SINGLE, Updatability.READONLY, false, false));

        type.addPropertyDefinition(createPropDef(PropertyIds.IS_MAJOR_VERSION, "Is Major Version", "Is Major Version",
                PropertyType.BOOLEAN, Cardinality.SINGLE, Updatability.READONLY, false, false));

        type.addPropertyDefinition(createPropDef(PropertyIds.IS_LATEST_MAJOR_VERSION, "Is Latest Major Version",
                "Is Latest Major Version", PropertyType.BOOLEAN, Cardinality.SINGLE, Updatability.READONLY, false,
                false));

        type.addPropertyDefinition(createPropDef(PropertyIds.VERSION_LABEL, "Version Label", "Version Label",
                PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY, false, false));

        type.addPropertyDefinition(createPropDef(PropertyIds.VERSION_SERIES_ID, "Version Series Id",
                "Version Series Id", PropertyType.ID, Cardinality.SINGLE, Updatability.READONLY, false, true));

        type.addPropertyDefinition(createPropDef(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT,
                "Is Version Series Checked Out", "Is Version Series Checked Out", PropertyType.BOOLEAN,
                Cardinality.SINGLE, Updatability.READONLY, false, true));

        type.addPropertyDefinition(createPropDef(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID,
                "Version Series Checked Out Id", "Version Series Checked Out Id", PropertyType.ID, Cardinality.SINGLE,
                Updatability.READONLY, false, false));

        type.addPropertyDefinition(createPropDef(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY,
                "Version Series Checked Out By", "Version Series Checked Out By", PropertyType.ID, Cardinality.SINGLE,
                Updatability.READONLY, false, false));

        type.addPropertyDefinition(createPropDef(PropertyIds.CHECKIN_COMMENT, "Checkin Comment", "Checkin Comment",
                PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY, false, false));

        type.addPropertyDefinition(createPropDef(PropertyIds.CONTENT_STREAM_LENGTH, "Content Stream Length",
                "Content Stream Length", PropertyType.INTEGER, Cardinality.SINGLE, Updatability.READONLY, false, false));

        type.addPropertyDefinition(createPropDef(PropertyIds.CONTENT_STREAM_MIME_TYPE, "MIME Type", "MIME Type",
                PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY, false, false));

        type.addPropertyDefinition(createPropDef(PropertyIds.CONTENT_STREAM_FILE_NAME, "Filename", "Filename",
                PropertyType.STRING, Cardinality.SINGLE, Updatability.READONLY, false, false));

        type.addPropertyDefinition(createPropDef(PropertyIds.CONTENT_STREAM_ID, "Content Stream Id",
                "Content Stream Id", PropertyType.ID, Cardinality.SINGLE, Updatability.READONLY, false, false));
    }

    /**
     * Creates a property definition object.
     */
    private static PropertyDefinition<?> createPropDef(String id, String displayName, String description,
            PropertyType datatype, Cardinality cardinality, Updatability updateability, boolean inherited,
            boolean required) {

        AbstractPropertyDefinition<?> result;

        switch (datatype) {
            case BOOLEAN:
                result = new PropertyBooleanDefinitionImpl();
                break;
            case DATETIME:
                result = new PropertyDateTimeDefinitionImpl();
                break;
            case DECIMAL:
                result = new PropertyDecimalDefinitionImpl();
                break;
            case HTML:
                result = new PropertyHtmlDefinitionImpl();
                break;
            case ID:
                result = new PropertyIdDefinitionImpl();
                break;
            case INTEGER:
                result = new PropertyIntegerDefinitionImpl();
                break;
            case STRING:
                result = new PropertyStringDefinitionImpl();
                break;
            case URI:
                result = new PropertyUriDefinitionImpl();
                break;
            default:
                throw new RuntimeException("Unknown datatype! Spec change?");
        }

        result.setId(id);
        result.setLocalName(id);
        result.setDisplayName(displayName);
        result.setDescription(description);
        result.setPropertyType(datatype);
        result.setCardinality(cardinality);
        result.setUpdatability(updateability);
        result.setIsInherited(inherited);
        result.setIsRequired(required);
        result.setIsQueryable(false);
        result.setQueryName(id);

        return result;
    }

    /**
     * Adds a type to collection.
     */
    private void addTypeInternal(AbstractTypeDefinition type) {
        if (type == null) {
            return;
        }

        if (fTypes.containsKey(type.getId())) {
            // can't overwrite a type
            return;
        }

        TypeDefinitionContainerImpl tc = new TypeDefinitionContainerImpl();
        tc.setTypeDefinition(type);

        // add to parent
        if (type.getParentTypeId() != null) {
            TypeDefinitionContainerImpl tdc = fTypes.get(type.getParentTypeId());
            if (tdc != null) {
                if (tdc.getChildren() == null) {
                    tdc.setChildren(new ArrayList<TypeDefinitionContainer>());
                }
                tdc.getChildren().add(tc);
            }
        }

        fTypes.put(type.getId(), tc);
    }

    /**
     * Gathers the type descendants tree.
     */
    private static TypeDefinitionContainer getTypesDescendants(int depth, TypeDefinitionContainer tc,
            boolean includePropertyDefinitions) {

        TypeDefinitionContainerImpl result = new TypeDefinitionContainerImpl();

        TypeDefinition type = copyTypeDefintion(tc.getTypeDefinition());
        if (!includePropertyDefinitions) {
            type.getPropertyDefinitions().clear();
        }

        result.setTypeDefinition(type);

        if (depth != 0) {
            if (tc.getChildren() != null) {
                result.setChildren(new ArrayList<TypeDefinitionContainer>());
                for (TypeDefinitionContainer tdc : tc.getChildren()) {
                    result.getChildren().add(
                            getTypesDescendants(depth < 0 ? -1 : depth - 1, tdc, includePropertyDefinitions));
                }
            }
        }

        return result;
    }

    private static TypeDefinition copyTypeDefintion(TypeDefinition type) {
        return Converter.convert(Converter.convert(type));
    }
}
