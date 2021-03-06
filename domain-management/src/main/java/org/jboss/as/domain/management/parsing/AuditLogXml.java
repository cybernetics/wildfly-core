/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.domain.management.parsing;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CLIENT_CERT_STORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TRUSTSTORE;
import static org.jboss.as.controller.parsing.ParseUtils.duplicateNamedElement;
import static org.jboss.as.controller.parsing.ParseUtils.isNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNamespace;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;


import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.domain.management.audit.AccessAuditResourceDefinition;
import org.jboss.as.domain.management.audit.AuditLogLoggerResourceDefinition;
import org.jboss.as.domain.management.audit.FileAuditLogHandlerResourceDefinition;
import org.jboss.as.domain.management.audit.JsonAuditLogFormatterResourceDefinition;
import org.jboss.as.domain.management.audit.PeriodicRotatingFileAuditLogHandlerResourceDefinition;
import org.jboss.as.domain.management.audit.SizeRotatingFileAuditLogHandlerResourceDefinition;
import org.jboss.as.domain.management.audit.SyslogAuditLogHandlerResourceDefinition;
import org.jboss.as.domain.management.audit.SyslogAuditLogProtocolResourceDefinition;
import org.jboss.as.domain.management.logging.DomainManagementLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class AuditLogXml {
    final boolean host;

    public AuditLogXml(boolean host) {
        this.host = host;
    }

    public void parseAuditLog(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {
        switch (expectedNs) {
            case DOMAIN_1_0:
            case DOMAIN_1_1:
            case DOMAIN_1_2:
            case DOMAIN_1_3:
            case DOMAIN_1_4:
                throw unexpectedElement(reader);
            default:
                parseAuditLog_1_5(reader, address, expectedNs, list, host);
        }
    }

    private void parseAuditLog_1_5(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list, boolean host) throws XMLStreamException {

        requireNamespace(reader, expectedNs);

        final ModelNode auditLogAddress = address.clone().add(AccessAuditResourceDefinition.PATH_ELEMENT.getKey(), AccessAuditResourceDefinition.PATH_ELEMENT.getValue());

        final ModelNode add = new ModelNode();
        add.get(OP).set(ADD);
        add.get(OP_ADDR).set(auditLogAddress);
        list.add(add);

        requireNoAttributes(reader);


        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
            case FORMATTERS:
                parseAuditLogFormatters1_5(reader, auditLogAddress, expectedNs, list);
                break;
            case HANDLERS:{
                parseAuditLogHandlers(reader, auditLogAddress, expectedNs, list);
                break;
            }
            case LOGGER:{
                parseAuditLogConfig_1_5(reader, auditLogAddress, expectedNs, AuditLogLoggerResourceDefinition.PATH_ELEMENT, list);
                break;
            }
            case SERVER_LOGGER:{
                if (host){
                    parseAuditLogConfig_1_5(reader, auditLogAddress, expectedNs, AuditLogLoggerResourceDefinition.HOST_SERVER_PATH_ELEMENT, list);
                    break;
                }
                //Otherwise fallback to server-logger not recognised in standalone.xml
            }
            default:
                throw unexpectedElement(reader);
            }
        }
    }

    private void parseAuditLogFormatters1_5(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {

        requireNamespace(reader, expectedNs);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
            case JSON_FORMATTER:{
                parseFileAuditLogFormatter_1_5(reader, address, expectedNs, list);
                break;
            }
            default:
                throw unexpectedElement(reader);
            }
        }
    }

    private void parseFileAuditLogFormatter_1_5(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {
        final ModelNode add = Util.createAddOperation();
        list.add(add);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    add.get(OP_ADDR).set(address).add(ModelDescriptionConstants.JSON_FORMATTER, value);
                    break;
                }
                case COMPACT:{
                    JsonAuditLogFormatterResourceDefinition.COMPACT.parseAndSetParameter(value, add, reader);
                    break;
                }
                case DATE_FORMAT:{
                    JsonAuditLogFormatterResourceDefinition.DATE_FORMAT.parseAndSetParameter(value, add, reader);
                    break;
                }
                case DATE_SEPARATOR:{
                    JsonAuditLogFormatterResourceDefinition.DATE_SEPARATOR.parseAndSetParameter(value, add, reader);
                    break;
                }
                case ESCAPE_CONTROL_CHARACTERS:{
                    JsonAuditLogFormatterResourceDefinition.ESCAPE_CONTROL_CHARACTERS.parseAndSetParameter(value, add, reader);
                    break;
                }
                case ESCAPE_NEW_LINE:{
                    JsonAuditLogFormatterResourceDefinition.ESCAPE_NEW_LINE.parseAndSetParameter(value, add, reader);
                    break;
                }
                case INCLUDE_DATE:{
                    JsonAuditLogFormatterResourceDefinition.INCLUDE_DATE.parseAndSetParameter(value, add, reader);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }

        requireNoContent(reader);
    }

    private void parseAuditLogHandlers(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {
        switch (expectedNs) {
            case DOMAIN_1_5:
            case DOMAIN_1_6:
            case DOMAIN_2_0:
            case DOMAIN_2_1:
            case DOMAIN_2_2:
                parseAuditLogHandlers_1_5(reader, address, expectedNs, list);
                break;
            default: // i.e. 3.x
                parseAuditLogHandlers_3_0(reader, address, expectedNs, list);
                break;
        }
    }

    private void parseAuditLogHandlers_1_5(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {

        requireNamespace(reader, expectedNs);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
            case FILE_HANDLER:{
                parseFileAuditLogHandler_1_5(reader, address, expectedNs, list);
                break;
            }
            case SYSLOG_HANDLER: {
                parseSyslogAuditLogHandler_1_5(reader, address, expectedNs, list);
                break;
            }
            default:
                throw unexpectedElement(reader);
            }
        }
    }

    private void parseAuditLogHandlers_3_0(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {

        requireNamespace(reader, expectedNs);   //FIXME is this needed? what it does?

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
            case FILE_HANDLER:
                parseFileAuditLogHandler_1_5(reader, address, expectedNs, list);
                break;
            case PERIODIC_ROTATING_FILE_HANDLER:
                parsePeriodicRotatingFileAuditLogHandler_3_0(reader, address, expectedNs, list);
                break;
            case SIZE_ROTATING_FILE_HANDLER:
                parseSizeRotatingFileAuditLogHandler_3_0(reader, address, expectedNs, list);
                break;
            case SYSLOG_HANDLER:
                parseSyslogAuditLogHandler_1_5(reader, address, expectedNs, list);
                break;
            default:
                throw unexpectedElement(reader);
            }
        }
    }

    private void parseFileAuditLogHandler_1_5(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {
        final ModelNode add = Util.createAddOperation();
        list.add(add);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    add.get(OP_ADDR).set(address).add(ModelDescriptionConstants.FILE_HANDLER, value);
                    break;
                }
                case MAX_FAILURE_COUNT: {
                    FileAuditLogHandlerResourceDefinition.MAX_FAILURE_COUNT.parseAndSetParameter(value, add, reader);
                    break;
                }
                case FORMATTER:{
                    FileAuditLogHandlerResourceDefinition.FORMATTER.parseAndSetParameter(value, add, reader);
                    break;
                }
                case PATH: {
                    FileAuditLogHandlerResourceDefinition.PATH.parseAndSetParameter(value, add, reader);
                    break;
                }
                case RELATIVE_TO: {
                    FileAuditLogHandlerResourceDefinition.RELATIVE_TO.parseAndSetParameter(value, add, reader);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }

        requireNoContent(reader);
    }

    private void parseSizeRotatingFileAuditLogHandler_3_0(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {
        final ModelNode add = Util.createAddOperation();
        list.add(add);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    add.get(OP_ADDR).set(address).add(ModelDescriptionConstants.SIZE_ROTATING_FILE_HANDLER, value);
                    break;
                case MAX_FAILURE_COUNT:
                    SizeRotatingFileAuditLogHandlerResourceDefinition.MAX_FAILURE_COUNT.parseAndSetParameter(value, add, reader);
                    break;
                case FORMATTER:
                    SizeRotatingFileAuditLogHandlerResourceDefinition.FORMATTER.parseAndSetParameter(value, add, reader);
                    break;
                case PATH:
                    SizeRotatingFileAuditLogHandlerResourceDefinition.PATH.parseAndSetParameter(value, add, reader);
                    break;
                case RELATIVE_TO:
                    SizeRotatingFileAuditLogHandlerResourceDefinition.RELATIVE_TO.parseAndSetParameter(value, add, reader);
                    break;
                case ROTATE_SIZE:
                    SizeRotatingFileAuditLogHandlerResourceDefinition.ROTATE_SIZE.parseAndSetParameter(value, add, reader);
                    break;
                case MAX_BACKUP_INDEX:
                    SizeRotatingFileAuditLogHandlerResourceDefinition.MAX_BACKUP_INDEX.parseAndSetParameter(value, add, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        requireNoContent(reader);
    }

    private void parsePeriodicRotatingFileAuditLogHandler_3_0(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {
        final ModelNode add = Util.createAddOperation();
        list.add(add);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    add.get(OP_ADDR).set(address).add(ModelDescriptionConstants.PERIODIC_ROTATING_FILE_HANDLER, value);
                    break;
                case MAX_FAILURE_COUNT:
                    PeriodicRotatingFileAuditLogHandlerResourceDefinition.MAX_FAILURE_COUNT.parseAndSetParameter(value, add, reader);
                    break;
                case FORMATTER:
                    PeriodicRotatingFileAuditLogHandlerResourceDefinition.FORMATTER.parseAndSetParameter(value, add, reader);
                    break;
                case PATH:
                    PeriodicRotatingFileAuditLogHandlerResourceDefinition.PATH.parseAndSetParameter(value, add, reader);
                    break;
                case RELATIVE_TO:
                    PeriodicRotatingFileAuditLogHandlerResourceDefinition.RELATIVE_TO.parseAndSetParameter(value, add, reader);
                    break;
                case SUFFIX:
                    PeriodicRotatingFileAuditLogHandlerResourceDefinition.SUFFIX.parseAndSetParameter(value, add, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        requireNoContent(reader);
    }

    private void parseSyslogAuditLogHandlerAttributes1_5(final XMLExtendedStreamReader reader, final ModelNode address, final ModelNode addOp) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    addOp.get(OP_ADDR).set(address).add(ModelDescriptionConstants.SYSLOG_HANDLER, value);
                    break;
                }
                case MAX_FAILURE_COUNT: {
                    SyslogAuditLogHandlerResourceDefinition.MAX_FAILURE_COUNT.parseAndSetParameter(value, addOp, reader);
                    break;
                }
                case FORMATTER:{
                    SyslogAuditLogHandlerResourceDefinition.FORMATTER.parseAndSetParameter(value, addOp, reader);
                    break;
                }
                case MAX_LENGTH: {
                    SyslogAuditLogHandlerResourceDefinition.MAX_LENGTH.parseAndSetParameter(value, addOp, reader);
                    break;
                }
                case TRUNCATE: {
                    SyslogAuditLogHandlerResourceDefinition.TRUNCATE.parseAndSetParameter(value, addOp, reader);
                    break;
                }
                case SYSLOG_FORMAT: {
                    SyslogAuditLogHandlerResourceDefinition.SYSLOG_FORMAT.parseAndSetParameter(value, addOp, reader);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
    }

    private void parseSyslogAuditLogHandlerAttributes1_6(final XMLExtendedStreamReader reader, final ModelNode address, final ModelNode addOp) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    addOp.get(OP_ADDR).set(address).add(ModelDescriptionConstants.SYSLOG_HANDLER, value);
                    break;
                }
                case MAX_FAILURE_COUNT: {
                    SyslogAuditLogHandlerResourceDefinition.MAX_FAILURE_COUNT.parseAndSetParameter(value, addOp, reader);
                    break;
                }
                case FORMATTER:{
                    SyslogAuditLogHandlerResourceDefinition.FORMATTER.parseAndSetParameter(value, addOp, reader);
                    break;
                }
                case MAX_LENGTH: {
                    SyslogAuditLogHandlerResourceDefinition.MAX_LENGTH.parseAndSetParameter(value, addOp, reader);
                    break;
                }
                case TRUNCATE: {
                    SyslogAuditLogHandlerResourceDefinition.TRUNCATE.parseAndSetParameter(value, addOp, reader);
                    break;
                }
                case FACILITY: {
                    SyslogAuditLogHandlerResourceDefinition.FACILITY.parseAndSetParameter(value, addOp, reader);
                    break;
                }
                case APP_NAME: {
                    SyslogAuditLogHandlerResourceDefinition.APP_NAME.parseAndSetParameter(value, addOp, reader);
                    break;
                }
                case SYSLOG_FORMAT: {
                    SyslogAuditLogHandlerResourceDefinition.SYSLOG_FORMAT.parseAndSetParameter(value, addOp, reader);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
    }

    private void parseSyslogAuditLogHandler_1_5(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {
        final ModelNode add = Util.createAddOperation();
        list.add(add);

        switch (expectedNs) {
            case DOMAIN_1_5:
                parseSyslogAuditLogHandlerAttributes1_5(reader, address, add);
                break;
            default: // i.e. 1.6, 2.x, 3.x
                parseSyslogAuditLogHandlerAttributes1_6(reader, address, add);
                break;
        }

        if (!add.get(OP_ADDR).isDefined()) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        boolean protocolSet = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());

            //Check there is only one protocol
            if (protocolSet) {
                throw DomainManagementLogger.ROOT_LOGGER.onlyOneSyslogHandlerProtocol(reader.getLocation());
            }
            protocolSet = true;

            switch (element) {
                case UDP:
                case TCP:
                case TLS: {
                    switch (expectedNs) {
                        case DOMAIN_1_5:
                        case DOMAIN_1_6:
                            parseSyslogAuditLogHandlerProtocol_1_5(reader, add.get(OP_ADDR), expectedNs, list, element);
                            break;
                        default:
                            switch (expectedNs.getMajorVersion()) {
                                case 2:
                                    parseSyslogAuditLogHandlerProtocol_1_5(reader, add.get(OP_ADDR), expectedNs, list, element);
                                    break;
                                default:
                                    parseSyslogAuditLogHandlerProtocol_1_7_and_3_0(reader, add.get(OP_ADDR), expectedNs, list,
                                            element);
                                    break;
                            }
                    }
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseSyslogAuditLogHandlerProtocol_1_5(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list, final Element protocolElement) throws XMLStreamException {
        PathAddress protocolAddress = PathAddress.pathAddress(address.clone().add(PROTOCOL, protocolElement.getLocalName()));
        ModelNode add = Util.createAddOperation(protocolAddress);
        list.add(add);
        final int tcpCount = reader.getAttributeCount();
        for (int i = 0; i < tcpCount; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case HOST: {
                    SyslogAuditLogProtocolResourceDefinition.Udp.HOST.parseAndSetParameter(value, add, reader);
                    break;
                }
                case PORT: {
                    SyslogAuditLogProtocolResourceDefinition.Udp.PORT.parseAndSetParameter(value, add, reader);
                    break;
                }
                case MESSAGE_TRANSFER : {
                    if (protocolElement != Element.UDP) {
                        SyslogAuditLogProtocolResourceDefinition.Tcp.MESSAGE_TRANSFER.parseAndSetParameter(value, add, reader);
                        break;
                    }
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }

        if (protocolElement != Element.TLS) {
            requireNoContent(reader);
        } else {
            boolean seenTrustStore = false;
            boolean seenClientCertStore = false;
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                requireNamespace(reader, expectedNs);
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                case TRUSTSTORE:{
                    if (seenTrustStore) {
                        throw duplicateNamedElement(reader, Element.TRUSTSTORE.getLocalName());
                    }
                    seenTrustStore = true;
                    parseSyslogTlsKeystore(reader, protocolAddress, expectedNs, list, SyslogAuditLogProtocolResourceDefinition.Tls.TlsKeyStore.TRUSTSTORE_ELEMENT, false);
                    break;
                }
                case CLIENT_CERT_STORE : {
                    if (seenClientCertStore) {
                        throw duplicateNamedElement(reader, Element.CLIENT_CERT_STORE.getLocalName());
                    }
                    seenClientCertStore = true;
                    parseSyslogTlsKeystore(reader, protocolAddress, expectedNs, list, SyslogAuditLogProtocolResourceDefinition.Tls.TlsKeyStore.CLIENT_CERT_ELEMENT, true);
                    break;
                }
                default:
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseSyslogAuditLogHandlerProtocol_1_7_and_3_0(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list, final Element protocolElement) throws XMLStreamException {
        PathAddress protocolAddress = PathAddress.pathAddress(address.clone().add(PROTOCOL, protocolElement.getLocalName()));
        ModelNode add = Util.createAddOperation(protocolAddress);
        list.add(add);
        final int tcpCount = reader.getAttributeCount();
        for (int i = 0; i < tcpCount; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case HOST: {
                    SyslogAuditLogProtocolResourceDefinition.Udp.HOST.parseAndSetParameter(value, add, reader);
                    break;
                }
                case PORT: {
                    SyslogAuditLogProtocolResourceDefinition.Udp.PORT.parseAndSetParameter(value, add, reader);
                    break;
                }
                case MESSAGE_TRANSFER : {
                    if (protocolElement != Element.UDP) {
                        SyslogAuditLogProtocolResourceDefinition.Tcp.MESSAGE_TRANSFER.parseAndSetParameter(value, add, reader);
                        break;
                    }
                }
                case RECONNECT_TIMEOUT:
                    if (protocolElement != Element.UDP) {
                        SyslogAuditLogProtocolResourceDefinition.Tcp.RECONNECT_TIMEOUT.parseAndSetParameter(value, add, reader);
                        break;
                    }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }

        if (protocolElement != Element.TLS) {
            requireNoContent(reader);
        } else {
            boolean seenTrustStore = false;
            boolean seenClientCertStore = false;
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                requireNamespace(reader, expectedNs);
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                case TRUSTSTORE:{
                    if (seenTrustStore) {
                        throw duplicateNamedElement(reader, Element.TRUSTSTORE.getLocalName());
                    }
                    seenTrustStore = true;
                    parseSyslogTlsKeystore(reader, protocolAddress, expectedNs, list, SyslogAuditLogProtocolResourceDefinition.Tls.TlsKeyStore.TRUSTSTORE_ELEMENT, false);
                    break;
                }
                case CLIENT_CERT_STORE : {
                    if (seenClientCertStore) {
                        throw duplicateNamedElement(reader, Element.CLIENT_CERT_STORE.getLocalName());
                    }
                    seenClientCertStore = true;
                    parseSyslogTlsKeystore(reader, protocolAddress, expectedNs, list, SyslogAuditLogProtocolResourceDefinition.Tls.TlsKeyStore.CLIENT_CERT_ELEMENT, true);
                    break;
                }
                default:
                    throw unexpectedElement(reader);
                }
            }
        }
    }
    private void parseSyslogTlsKeystore(final XMLExtendedStreamReader reader, final PathAddress address, final Namespace expectedNs, final List<ModelNode> list, final PathElement storeAddress, final boolean hasKeyPassword) throws XMLStreamException {
        ModelNode add = Util.createAddOperation(address.append(storeAddress));
        list.add(add);
        final int tcpCount = reader.getAttributeCount();
        for (int i = 0; i < tcpCount; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case KEYSTORE_PASSWORD: {
                    SyslogAuditLogProtocolResourceDefinition.Tls.TlsKeyStore.KEYSTORE_PASSWORD.parseAndSetParameter(value, add, reader);
                    break;
                }
                case PATH: {
                    SyslogAuditLogProtocolResourceDefinition.Tls.TlsKeyStore.KEYSTORE_PATH.parseAndSetParameter(value, add, reader);
                    break;
                }
                case RELATIVE_TO : {
                    SyslogAuditLogProtocolResourceDefinition.Tls.TlsKeyStore.KEYSTORE_RELATIVE_TO.parseAndSetParameter(value, add, reader);
                    break;
                }
                case KEY_PASSWORD: {
                    if (hasKeyPassword){
                        SyslogAuditLogProtocolResourceDefinition.Tls.TlsKeyStore.KEY_PASSWORD.parseAndSetParameter(value, add, reader);
                        break;
                    }
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }

        requireNoContent(reader);
    }

    private void parseAuditLogConfig_1_5(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final PathElement pathElement, final List<ModelNode> list) throws XMLStreamException {

        requireNamespace(reader, expectedNs);

        final ModelNode configAddress = address.clone().add(pathElement.getKey(), pathElement.getValue());

        final ModelNode add = new ModelNode();
        add.get(OP).set(ADD);
        add.get(OP_ADDR).set(configAddress);

        list.add(add);

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case LOG_READ_ONLY: {
                    AuditLogLoggerResourceDefinition.LOG_READ_ONLY.parseAndSetParameter(value, add, reader);
                    break;
                }
                case LOG_BOOT: {
                    AuditLogLoggerResourceDefinition.LOG_BOOT.parseAndSetParameter(value, add, reader);
                    break;
                }
                case ENABLED: {
                    AuditLogLoggerResourceDefinition.ENABLED.parseAndSetParameter(value, add, reader);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
            case HANDLERS:{
                parseAuditLogHandlersReference_1_5(reader, configAddress, expectedNs, list);
                break;
            }
            default:
                throw unexpectedElement(reader);
            }
        }
    }

    private void parseAuditLogHandlersReference_1_5(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {
        requireNamespace(reader, expectedNs);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
            case HANDLER:{
                requireNamespace(reader, expectedNs);
                final ModelNode add = new ModelNode();
                add.get(OP).set(ADD);
                list.add(add);

                final int count = reader.getAttributeCount();
                for (int i = 0; i < count; i++) {
                    final String value = reader.getAttributeValue(i);
                    if (!isNoNamespaceAttribute(reader, i)) {
                        throw unexpectedAttribute(reader, i);
                    }
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case NAME: {
                            add.get(OP_ADDR).set(address).add(ModelDescriptionConstants.HANDLER, value);
                            break;
                        }
                        default: {
                            throw unexpectedAttribute(reader, i);
                        }
                    }
                    requireNoContent(reader);
                }
                break;
            }
            default:
                throw unexpectedElement(reader);
            }
        }
    }

    public void writeAuditLog(XMLExtendedStreamWriter writer, ModelNode auditLog) throws XMLStreamException {
        writer.writeStartElement(Element.AUDIT_LOG.getLocalName());

        if (auditLog.hasDefined(ModelDescriptionConstants.JSON_FORMATTER) && auditLog.get(ModelDescriptionConstants.JSON_FORMATTER).keys().size() > 0) {
            writer.writeStartElement(Element.FORMATTERS.getLocalName());
            for (Property prop : auditLog.get(ModelDescriptionConstants.JSON_FORMATTER).asPropertyList()) {
                writer.writeStartElement(Element.JSON_FORMATTER.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), prop.getName());
                JsonAuditLogFormatterResourceDefinition.COMPACT.marshallAsAttribute(prop.getValue(), writer);
                JsonAuditLogFormatterResourceDefinition.DATE_FORMAT.marshallAsAttribute(prop.getValue(), writer);
                JsonAuditLogFormatterResourceDefinition.DATE_SEPARATOR.marshallAsAttribute(prop.getValue(), writer);
                JsonAuditLogFormatterResourceDefinition.ESCAPE_CONTROL_CHARACTERS.marshallAsAttribute(prop.getValue(), writer);
                JsonAuditLogFormatterResourceDefinition.ESCAPE_NEW_LINE.marshallAsAttribute(prop.getValue(), writer);
                JsonAuditLogFormatterResourceDefinition.INCLUDE_DATE.marshallAsAttribute(prop.getValue(), writer);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }


        if ((auditLog.hasDefined(ModelDescriptionConstants.FILE_HANDLER) && auditLog.get(ModelDescriptionConstants.FILE_HANDLER).keys().size() > 0) ||
                (auditLog.hasDefined(ModelDescriptionConstants.SYSLOG_HANDLER) && auditLog.get(ModelDescriptionConstants.SYSLOG_HANDLER).keys().size() > 0) ||
                (auditLog.hasDefined(ModelDescriptionConstants.SIZE_ROTATING_FILE_HANDLER) && auditLog.get(ModelDescriptionConstants.SIZE_ROTATING_FILE_HANDLER).keys().size() > 0) ||
                (auditLog.hasDefined(ModelDescriptionConstants.PERIODIC_ROTATING_FILE_HANDLER) && auditLog.get(ModelDescriptionConstants.PERIODIC_ROTATING_FILE_HANDLER).keys().size() > 0)) {
            writer.writeStartElement(Element.HANDLERS.getLocalName());
            if (auditLog.hasDefined(ModelDescriptionConstants.FILE_HANDLER)) {
                for (String name : auditLog.get(ModelDescriptionConstants.FILE_HANDLER).keys()) {
                    writer.writeStartElement(Element.FILE_HANDLER.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), name);
                    ModelNode handler = auditLog.get(ModelDescriptionConstants.FILE_HANDLER, name);
                    FileAuditLogHandlerResourceDefinition.FORMATTER.marshallAsAttribute(handler, writer);
                    FileAuditLogHandlerResourceDefinition.MAX_FAILURE_COUNT.marshallAsAttribute(handler, writer);
                    FileAuditLogHandlerResourceDefinition.PATH.marshallAsAttribute(handler, writer);
                    FileAuditLogHandlerResourceDefinition.RELATIVE_TO.marshallAsAttribute(handler, writer);
                    writer.writeEndElement();
                }
            }
            if (auditLog.hasDefined(ModelDescriptionConstants.PERIODIC_ROTATING_FILE_HANDLER)) {
                for (String name : auditLog.get(ModelDescriptionConstants.PERIODIC_ROTATING_FILE_HANDLER).keys()) {
                    writer.writeStartElement(Element.PERIODIC_ROTATING_FILE_HANDLER.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), name);
                    ModelNode handler = auditLog.get(ModelDescriptionConstants.PERIODIC_ROTATING_FILE_HANDLER, name);
                    PeriodicRotatingFileAuditLogHandlerResourceDefinition.FORMATTER.marshallAsAttribute(handler, writer);
                    PeriodicRotatingFileAuditLogHandlerResourceDefinition.MAX_FAILURE_COUNT.marshallAsAttribute(handler, writer);
                    PeriodicRotatingFileAuditLogHandlerResourceDefinition.PATH.marshallAsAttribute(handler, writer);
                    PeriodicRotatingFileAuditLogHandlerResourceDefinition.RELATIVE_TO.marshallAsAttribute(handler, writer);
                    PeriodicRotatingFileAuditLogHandlerResourceDefinition.SUFFIX.marshallAsAttribute(handler, writer);
                    writer.writeEndElement();
                }
            }
            if (auditLog.hasDefined(ModelDescriptionConstants.SIZE_ROTATING_FILE_HANDLER)) {
                for (String name : auditLog.get(ModelDescriptionConstants.SIZE_ROTATING_FILE_HANDLER).keys()) {
                    writer.writeStartElement(Element.SIZE_ROTATING_FILE_HANDLER.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), name);
                    ModelNode handler = auditLog.get(ModelDescriptionConstants.SIZE_ROTATING_FILE_HANDLER, name);
                    SizeRotatingFileAuditLogHandlerResourceDefinition.FORMATTER.marshallAsAttribute(handler, writer);
                    SizeRotatingFileAuditLogHandlerResourceDefinition.MAX_FAILURE_COUNT.marshallAsAttribute(handler, writer);
                    SizeRotatingFileAuditLogHandlerResourceDefinition.PATH.marshallAsAttribute(handler, writer);
                    SizeRotatingFileAuditLogHandlerResourceDefinition.RELATIVE_TO.marshallAsAttribute(handler, writer);
                    SizeRotatingFileAuditLogHandlerResourceDefinition.ROTATE_SIZE.marshallAsAttribute(handler, writer);
                    SizeRotatingFileAuditLogHandlerResourceDefinition.MAX_BACKUP_INDEX.marshallAsAttribute(handler, writer);
                    writer.writeEndElement();
                }
            }
            if (auditLog.hasDefined(ModelDescriptionConstants.SYSLOG_HANDLER)) {
                for (String name : auditLog.get(ModelDescriptionConstants.SYSLOG_HANDLER).keys()) {
                    writer.writeStartElement(Element.SYSLOG_HANDLER.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), name);
                    ModelNode handler = auditLog.get(ModelDescriptionConstants.SYSLOG_HANDLER, name);
                    SyslogAuditLogHandlerResourceDefinition.FORMATTER.marshallAsAttribute(handler, writer);
                    SyslogAuditLogHandlerResourceDefinition.MAX_FAILURE_COUNT.marshallAsAttribute(handler, writer);
                    SyslogAuditLogHandlerResourceDefinition.SYSLOG_FORMAT.marshallAsAttribute(handler, writer);
                    SyslogAuditLogHandlerResourceDefinition.MAX_LENGTH.marshallAsAttribute(handler, writer);
                    SyslogAuditLogHandlerResourceDefinition.TRUNCATE.marshallAsAttribute(handler, writer);
                    SyslogAuditLogHandlerResourceDefinition.FACILITY.marshallAsAttribute(handler, writer);
                    SyslogAuditLogHandlerResourceDefinition.APP_NAME.marshallAsAttribute(handler, writer);
                    if (handler.hasDefined(PROTOCOL)) {
                        writeAuditLogSyslogProtocol(writer, handler.get(PROTOCOL));
                    }
                    writer.writeEndElement();
                }
            }
            writer.writeEndElement();
        }
        writeAuditLogger(writer, auditLog, Element.LOGGER.getLocalName());
        writeAuditLogger(writer, auditLog, Element.SERVER_LOGGER.getLocalName());
        writer.writeEndElement();
    }

    private void writeAuditLogger(XMLExtendedStreamWriter writer, ModelNode auditLog, String element) throws XMLStreamException {
        if (auditLog.hasDefined(element) && auditLog.get(element).hasDefined(ModelDescriptionConstants.AUDIT_LOG)){
            ModelNode config = auditLog.get(element, ModelDescriptionConstants.AUDIT_LOG);
            writer.writeStartElement(element);
            AuditLogLoggerResourceDefinition.LOG_BOOT.marshallAsAttribute(config, writer);
            AuditLogLoggerResourceDefinition.LOG_READ_ONLY.marshallAsAttribute(config, writer);
            AuditLogLoggerResourceDefinition.ENABLED.marshallAsAttribute(config, writer);
            if (config.hasDefined(ModelDescriptionConstants.HANDLER) && config.get(ModelDescriptionConstants.HANDLER).keys().size() > 0) {
                writer.writeStartElement(Element.HANDLERS.getLocalName());
                for (String name : config.get(ModelDescriptionConstants.HANDLER).keys()) {
                    writer.writeStartElement(Element.HANDLER.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), name);
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }

            writer.writeEndElement();
        }
    }


    private void writeAuditLogSyslogProtocol(XMLExtendedStreamWriter writer, ModelNode protocol) throws XMLStreamException {
        String type = protocol.keys().iterator().next();
        ModelNode protocolContents = protocol.get(type);
        if (type.equals(ModelDescriptionConstants.UDP)) {
            writer.writeStartElement(type);
            SyslogAuditLogProtocolResourceDefinition.Udp.HOST.marshallAsAttribute(protocolContents, writer);
            SyslogAuditLogProtocolResourceDefinition.Udp.PORT.marshallAsAttribute(protocolContents, writer);
            writer.writeEndElement();
        } else if (type.equals(ModelDescriptionConstants.TCP)) {
            writer.writeStartElement(type);
            SyslogAuditLogProtocolResourceDefinition.Tcp.HOST.marshallAsAttribute(protocolContents, writer);
            SyslogAuditLogProtocolResourceDefinition.Tcp.PORT.marshallAsAttribute(protocolContents, writer);
            SyslogAuditLogProtocolResourceDefinition.Tcp.MESSAGE_TRANSFER.marshallAsAttribute(protocolContents, writer);
            SyslogAuditLogProtocolResourceDefinition.Tcp.RECONNECT_TIMEOUT.marshallAsAttribute(protocolContents, writer);
            writer.writeEndElement();
        } else if (type.equals(ModelDescriptionConstants.TLS)) {
            writer.writeStartElement(type);
            SyslogAuditLogProtocolResourceDefinition.Tls.HOST.marshallAsAttribute(protocolContents, writer);
            SyslogAuditLogProtocolResourceDefinition.Tls.PORT.marshallAsAttribute(protocolContents, writer);
            SyslogAuditLogProtocolResourceDefinition.Tls.MESSAGE_TRANSFER.marshallAsAttribute(protocolContents, writer);
            SyslogAuditLogProtocolResourceDefinition.Tcp.RECONNECT_TIMEOUT.marshallAsAttribute(protocolContents, writer);

            if (protocolContents.hasDefined(AUTHENTICATION)) {
                writeAuditLogSyslogTlsProtocolKeyStore(writer, protocolContents.get(AUTHENTICATION), TRUSTSTORE);
                writeAuditLogSyslogTlsProtocolKeyStore(writer, protocolContents.get(AUTHENTICATION), CLIENT_CERT_STORE);
            }

            writer.writeEndElement();
        }
    }

    private void writeAuditLogSyslogTlsProtocolKeyStore(XMLExtendedStreamWriter writer, ModelNode keystoreParent, String name) throws XMLStreamException {
        if (keystoreParent.hasDefined(name)) {
            ModelNode keystore = keystoreParent.get(name);
            writer.writeStartElement(name);
            SyslogAuditLogProtocolResourceDefinition.Tls.TlsKeyStore.KEYSTORE_PATH.marshallAsAttribute(keystore, writer);
            SyslogAuditLogProtocolResourceDefinition.Tls.TlsKeyStore.KEYSTORE_RELATIVE_TO.marshallAsAttribute(keystore, writer);
            SyslogAuditLogProtocolResourceDefinition.Tls.TlsKeyStore.KEYSTORE_PASSWORD.marshallAsAttribute(keystore, writer);
            SyslogAuditLogProtocolResourceDefinition.Tls.TlsKeyStore.KEY_PASSWORD.marshallAsAttribute(keystore, writer);
            writer.writeEndElement();
        }
    }
}
