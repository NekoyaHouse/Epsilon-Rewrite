package dev.sakura.server.packet;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import dev.sakura.server.packet.annotations.ProtocolField;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IRCPacket {
    Gson gson = new Gson();

    default void readPacket(JsonObject jsonObject) {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(ProtocolField.class)) {
                continue;
            }
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            ProtocolField protocolField = field.getAnnotation(ProtocolField.class);
            try {
                JsonElement element = jsonObject.get(protocolField.value());
                if (element == null) {
                    continue;
                }

                Class<?> fieldType = field.getType();
                Object value;

                if (List.class.isAssignableFrom(fieldType)) {
                    Type genericType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    value = gson.fromJson(element, TypeToken.getParameterized(List.class, genericType).getType());
                } else if (Set.class.isAssignableFrom(fieldType)) {
                    Type genericType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    value = gson.fromJson(element, TypeToken.getParameterized(Set.class, genericType).getType());
                } else if (Map.class.isAssignableFrom(fieldType)) {
                    Type[] genericTypes = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
                    Type keyType = genericTypes[0];
                    Type valueType = genericTypes[1];
                    value = gson.fromJson(element, TypeToken.getParameterized(Map.class, keyType, valueType).getType());
                } else {
                    value = gson.fromJson(element, fieldType);
                }

                field.set(this, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error while reading packet", e);
            }
        }
    }

    default JsonObject writePacket() {
        JsonObject jsonObject = new JsonObject();
        for (Field field : this.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(ProtocolField.class)) {
                continue;
            }
            if (Modifier.isStatic(field.getModifiers())) {
                throw new RuntimeException("Protocol field can't be static.");
            }
            field.setAccessible(true);
            ProtocolField protocolField = field.getAnnotation(ProtocolField.class);
            try {
                Object value = field.get(this);
                if (value == null) {
                    continue;
                }
                JsonElement element = gson.toJsonTree(value);
                jsonObject.add(protocolField.value(), element);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error while writing packet", e);
            }
        }
        return jsonObject;
    }
}

