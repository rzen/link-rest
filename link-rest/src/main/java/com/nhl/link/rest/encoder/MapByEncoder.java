package com.nhl.link.rest.encoder;

import com.fasterxml.jackson.core.JsonGenerator;
import com.nhl.link.rest.EntityProperty;
import com.nhl.link.rest.LinkRestException;
import com.nhl.link.rest.ResourceEntity;
import com.nhl.link.rest.encoder.converter.StringConverter;
import com.nhl.link.rest.meta.LrAttribute;
import com.nhl.link.rest.meta.LrRelationship;
import com.nhl.link.rest.runtime.encoder.IAttributeEncoderFactory;
import com.nhl.link.rest.runtime.encoder.IStringConverterFactory;
import org.apache.cayenne.exp.Expression;

import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

public class MapByEncoder implements CollectionEncoder {

    private String mapByPath;
    private List<Function<Object, ?>> mapByReaders;
    private CollectionEncoder collectionEncoder;
    private boolean byId;
    private StringConverter fieldNameConverter;
    private Expression filter;

    public MapByEncoder(String mapByPath, Expression filter, ResourceEntity<?> mapBy, CollectionEncoder collectionEncoder,
                        IStringConverterFactory converterFactory, IAttributeEncoderFactory encoderFactory) {

        if (mapBy == null) {
            throw new NullPointerException("Null mapBy");
        }

        this.mapByPath = mapByPath;
        this.mapByReaders = new ArrayList<>();
        this.collectionEncoder = collectionEncoder;
        this.filter = filter;

        config(converterFactory, encoderFactory, mapBy);
    }

    private static Function<Object, ?> getPropertyReader(String propertyName, EntityProperty property) {
        return it -> property.read(it, propertyName);
    }

    @Override
    public boolean willEncode(String propertyName, Object object) {
        return true;
    }

    /**
     * @since 2.0
     */
    @Override
    public int visitEntities(Object object, EncoderVisitor visitor) {
        // a "flat" visit method that ignores mapping property
        return collectionEncoder.visitEntities(object, visitor);
    }

    private void config(IStringConverterFactory converterFactory, IAttributeEncoderFactory encoderFactory,
                        ResourceEntity<?> mapBy) {

        if (mapBy.isIdIncluded()) {
            validateLeafMapBy(mapBy);
            byId = true;
            this.mapByReaders.add(getPropertyReader(null, encoderFactory.getIdProperty(mapBy)));
            this.fieldNameConverter = converterFactory.getConverter(mapBy.getLrEntity());
            return;
        }

        if (!mapBy.getAttributes().isEmpty()) {

            validateLeafMapBy(mapBy);
            byId = false;

            Map.Entry<String, LrAttribute> attribute = mapBy.getAttributes().entrySet().iterator().next();
            mapByReaders.add(getPropertyReader(attribute.getKey(),
                    encoderFactory.getAttributeProperty(mapBy.getLrEntity(), attribute.getValue())));

            this.fieldNameConverter = converterFactory.getConverter(mapBy.getLrEntity(), attribute.getKey());
            return;
        }

        if (!mapBy.getChildren().isEmpty()) {

            byId = false;

            Map.Entry<String, ResourceEntity<?>> child = mapBy.getChildren().entrySet().iterator().next();
            LrRelationship relationship = child.getValue().getLrEntity().getRelationship(child.getKey());
            mapByReaders.add(getPropertyReader(child.getKey(),
                    encoderFactory.getRelationshipProperty(mapBy.getLrEntity(), relationship, null)));

            ResourceEntity<?> childMapBy = mapBy.getChildren().get(child.getKey());
            config(converterFactory, encoderFactory, childMapBy);
            return;
        }

        // by default we are dealing with ID
        byId = true;
        mapByReaders.add(getPropertyReader(null, encoderFactory.getIdProperty(mapBy)));
    }

    private void validateLeafMapBy(ResourceEntity<?> mapBy) {

        if (!mapBy.getChildren().isEmpty()) {

            StringBuilder message = new StringBuilder("'mapBy' path segment '");
            message.append(mapBy.getIncoming().getName()).append(
                    "should not have children. Full 'mapBy' path: " + mapByPath);

            throw new LinkRestException(Status.BAD_REQUEST, message.toString());
        }
    }

    @Override
    public int encodeAndGetTotal(String propertyName, Object object, JsonGenerator out) throws IOException {
        if (propertyName != null) {
            out.writeFieldName(propertyName);
        }

        if (object == null) {
            out.writeNull();
            return 0;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        List<?> objects = (List) object;

        Map<String, List<Object>> map = mapBy(objects);

        out.writeStartObject();

        int total = 0;
        for (Entry<String, List<Object>> e : map.entrySet()) {
            out.writeFieldName(e.getKey());
            total += collectionEncoder.encodeAndGetTotal(null, e.getValue(), out);
        }

        out.writeEndObject();

        return total;
    }

    private Object mapByValue(Object object) {
        Object result = object;

        for (Function<Object, ?> reader : mapByReaders) {
            if (result == null) {
                break;
            }

            result = reader.apply(result);
        }

        return result;
    }

    private Map<String, List<Object>> mapBy(List<?> objects) {

        if (objects.isEmpty()) {
            return Collections.emptyMap();
        }

        // though the map is unsorted, it is still in predictable iteration
        // order...
        Map<String, List<Object>> map = new LinkedHashMap<>();

        for (Object o : objects) {

            // filter objects even before we apply mapping...
            if (filter != null && !filter.match(o)) {
                continue;
            }

            Object key = mapByValue(o);
            if (byId) {
                @SuppressWarnings("unchecked")
                Map<String, Object> id = (Map<String, Object>) key;
                key = id.entrySet().iterator().next().getValue();
            }

            // disallow nulls as JSON keys...
            // note that converter below will throw an NPE if we pass NULL
            // further down... the error here has more context.
            if (key == null) {
                throw new LinkRestException(Status.INTERNAL_SERVER_ERROR, "Null mapBy value for key '" + mapByPath + "'");
            }

            String keyString = fieldNameConverter.asString(key);

            List<Object> list = map.get(keyString);
            if (list == null) {
                list = new ArrayList<>();
                map.put(keyString, list);
            }

            list.add(o);
        }

        return map;
    }
}
