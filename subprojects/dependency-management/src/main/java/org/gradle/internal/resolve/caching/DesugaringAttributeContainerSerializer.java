/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.internal.resolve.caching;

import org.gradle.api.Named;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.AttributeContainerSerializer;
import org.gradle.api.internal.attributes.AttributeContainerInternal;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.model.NamedObjectInstantiator;
import org.gradle.internal.serialize.Decoder;
import org.gradle.internal.serialize.Encoder;
import org.gradle.internal.snapshot.impl.CoercingStringValueSnapshot;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * An attribute container serializer that will desugar typed attributes.
 *
 * Attributes that are of types different than {@code String} or {@code boolean} will be desugared
 * before serialization. The process requires the attribute type to implement {@link Named}.
 */
public class DesugaringAttributeContainerSerializer implements AttributeContainerSerializer {
    private final ImmutableAttributesFactory attributesFactory;
    private final NamedObjectInstantiator namedObjectInstantiator;

    private static final byte STRING_ATTRIBUTE = 1;
    private static final byte BOOLEAN_ATTRIBUTE = 2;
    private static final byte DESUGARED_ATTRIBUTE = 3;
    private static final byte INTEGER_ATTRIBUTE = 4;

    public DesugaringAttributeContainerSerializer(ImmutableAttributesFactory attributesFactory, NamedObjectInstantiator namedObjectInstantiator) {
        this.attributesFactory = attributesFactory;
        this.namedObjectInstantiator = namedObjectInstantiator;
    }

    @Override
    public ImmutableAttributes read(Decoder decoder) throws IOException {
        ImmutableAttributes attributes = ImmutableAttributes.EMPTY;
        int count = decoder.readSmallInt();
        for (int i = 0; i < count; i++) {
            String name = decoder.readString();
            byte type = decoder.readByte();
            if (type == BOOLEAN_ATTRIBUTE) {
                attributes = attributesFactory.concat(attributes, Attribute.of(name, Boolean.class), decoder.readBoolean());
            } else if (type == STRING_ATTRIBUTE){
                String value = decoder.readString();
                attributes = attributesFactory.concat(attributes, Attribute.of(name, String.class), value);
            } else if (type == INTEGER_ATTRIBUTE){
                int value = decoder.readInt();
                attributes = attributesFactory.concat(attributes, Attribute.of(name, Integer.class), value);
            } else if (type == DESUGARED_ATTRIBUTE) {
                String value = decoder.readString();
                attributes = attributesFactory.concat(attributes, Attribute.of(name, String.class), new CoercingStringValueSnapshot(value, namedObjectInstantiator));
            }
        }
        return attributes;
    }

    @Override
    public void write(Encoder encoder, AttributeContainer container) throws IOException {
        encoder.writeSmallInt(container.keySet().size());
        Iterator<Map.Entry<Attribute<?>, Object>> entryIterator =
            ((AttributeContainerInternal) container).asImmutable().entryIterator();
        while (entryIterator.hasNext()) {
            Map.Entry<Attribute<?>, Object> entry = entryIterator.next();
            encoder.writeString(entry.getKey().getName());
            Class<?> type = entry.getKey().getType();
            if (type == Boolean.class) {
                encoder.writeByte(BOOLEAN_ATTRIBUTE);
                encoder.writeBoolean((Boolean) entry.getValue());
            } else if (type == String.class){
                encoder.writeByte(STRING_ATTRIBUTE);
                encoder.writeString((String) entry.getValue());
            } else if (type == Integer.class){
                encoder.writeByte(INTEGER_ATTRIBUTE);
                encoder.writeInt((Integer) entry.getValue());
            } else {
                Named attributeValue = (Named) entry.getValue();
                encoder.writeByte(DESUGARED_ATTRIBUTE);
                encoder.writeString(attributeValue.getName());
            }
        }
    }
}
