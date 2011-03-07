/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.chemistry.opencmis.jcr;

import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractServiceFactory;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.server.support.CmisServiceWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public class JcrServiceFactory extends AbstractServiceFactory {
    private static final Log log = LogFactory.getLog(JcrServiceFactory.class);

    public static final String MOUNT_PATH_CONFIG = "mount-path";
    public static final String PREFIX_JCR_CONFIG = "jcr.";

    public static final BigInteger DEFAULT_MAX_ITEMS_TYPES = BigInteger.valueOf(50);
    public static final BigInteger DEFAULT_DEPTH_TYPES = BigInteger.valueOf(-1);
    public static final BigInteger DEFAULT_MAX_ITEMS_OBJECTS = BigInteger.valueOf(200);
    public static final BigInteger DEFAULT_DEPTH_OBJECTS = BigInteger.valueOf(10);

    private TypeManager typeManager;
    private Map<String, String> jcrConfig;
    private String mountPath;
    private JcrRepository jcrRepository;

    @Override
    public void init(Map<String, String> parameters) {
        typeManager = new TypeManager();
        readConfiguration(parameters);
        jcrRepository = new JcrRepository(acquireJcrRepository(jcrConfig), mountPath, typeManager);
    }

    @Override
    public void destroy() {
        jcrRepository = null;
        typeManager = null;
    }

    @Override
    public CmisService getService(CallContext context) {
        CmisServiceWrapper<JcrService> serviceWrapper = new CmisServiceWrapper<JcrService>(
                createJcrService(jcrRepository, context), DEFAULT_MAX_ITEMS_TYPES, DEFAULT_DEPTH_TYPES,
                DEFAULT_MAX_ITEMS_OBJECTS, DEFAULT_DEPTH_OBJECTS);

        serviceWrapper.getWrappedService().setCallContext(context);
        return serviceWrapper;
    }

    //------------------------------------------< factories >---

    protected Repository acquireJcrRepository(Map<String, String> jcrConfig) {
        try {
            for (RepositoryFactory factory : ServiceLoader.load(RepositoryFactory.class)) {
                log.debug("Trying to acquire JCR repository from factory " + factory);
                Repository repository = factory.getRepository(jcrConfig);
                if (repository != null) {
                    log.debug("Successfully acquired JCR repository from factory " + factory);
                    return repository;
                }
                else {
                    log.debug("Could not acquire JCR repository from factory " + factory);
                }
            }
            throw new CmisConnectionException("No JCR repository factory for configured parameters");
        }
        catch (RepositoryException e) {
            log.debug(e.getMessage(), e);
            throw new CmisConnectionException(e.getMessage(), e);
        }
    }

    protected JcrService createJcrService(JcrRepository jcrRepository, CallContext context) {
        return new JcrService(jcrRepository);
    }

    //------------------------------------------< private >--- 

    private void readConfiguration(Map<String, String> parameters) {
        Map<String, String> map = new HashMap<String, String>();
        List<String> keys = new ArrayList<String>(parameters.keySet());
        Collections.sort(keys);

        for (String key : keys) {
            if (key.startsWith(PREFIX_JCR_CONFIG)) {
                String jcrKey = key.substring(PREFIX_JCR_CONFIG.length());
                String jcrValue = replaceSystemProperties(parameters.get(key));
                map.put(jcrKey, jcrValue);
            }

            else if (MOUNT_PATH_CONFIG.equals(key)) {
                mountPath = parameters.get(key);
                log.debug("Configuration: " + MOUNT_PATH_CONFIG + '=' + mountPath);
            }

            else {
                log.warn("Configuration: unrecognized key: " + key);
            }
        }

        jcrConfig = Collections.unmodifiableMap(map);
        log.debug("Configuration: jcr=" + jcrConfig);
    }

    private static String replaceSystemProperties(String s) {
        if (s == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        StringBuilder property = null;
        boolean inProperty = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (inProperty) {
                if (c == '}') {
                    String value = System.getProperty(property.toString());
                    if (value != null) {
                        result.append(value);
                    }
                    inProperty = false;
                } else {
                    property.append(c);
                }
            } else {
                if (c == '{') {
                    property = new StringBuilder();
                    inProperty = true;
                } else {
                    result.append(c);
                }
            }
        }

        return result.toString();
    }

}