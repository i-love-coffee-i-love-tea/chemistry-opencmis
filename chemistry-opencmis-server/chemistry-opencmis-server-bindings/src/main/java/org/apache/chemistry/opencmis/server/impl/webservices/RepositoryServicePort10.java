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
package org.apache.chemistry.opencmis.server.impl.webservices;

import org.apache.chemistry.opencmis.commons.impl.jaxb.ObjectFactory;
import org.apache.chemistry.opencmis.commons.impl.jaxb.RepositoryServicePort;

import jakarta.jws.WebService;
import jakarta.xml.bind.annotation.XmlSeeAlso;

@WebService(name = "RepositoryServicePort", targetNamespace = "http://docs.oasis-open.org/ns/cmis/ws/200908/")
@XmlSeeAlso({ ObjectFactory.class })
public interface RepositoryServicePort10 extends RepositoryServicePort {
}
