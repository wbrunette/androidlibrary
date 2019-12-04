/*
 * Copyright (C) 2015 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.database.utilities;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.type.CollectionType;

import org.opendatakit.aggregate.odktables.rest.ElementDataType;
import org.opendatakit.database.data.KeyValueStoreEntry;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.utilities.ODKFileUtils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Replacement for KeyValueStoreHelper object.
 *
 * @author mitchellsundt@gmail.com
 */
public final class KeyValueStoreUtils {

  /**
   * Do not instantiate this class
   */
  private KeyValueStoreUtils() {
  }

  public static KeyValueStoreEntry buildEntry(String tableId, String partition, String aspect,
      String key, ElementDataType type, String serializedValue) {
    KeyValueStoreEntry entry = new KeyValueStoreEntry();
    entry.tableId = tableId;
    entry.partition = partition;
    entry.aspect = aspect;
    entry.key = key;
    entry.type = type.name();
    entry.value = serializedValue;
    return entry;
  }

  public static Double getNumber(KeyValueStoreEntry entry) throws IllegalArgumentException {
    if (entry == null) {
      return null;
    }
    if (!entry.type.equals(ElementDataType.number.name())) {
      throw new IllegalArgumentException("requested number entry for " + "key: " + entry.key
          + ", but the corresponding entry in the store was " + "not of type: "
          + ElementDataType.number.name());
    }
    try {
      return Double.parseDouble(entry.value);
    } catch (NumberFormatException ignored) {
      throw new IllegalArgumentException("requested int entry for " + "key: " + entry.key
          + ", but the value in the store failed to " + "parse to type: " + ElementDataType.number
          .name());
    }
  }

  public static Integer getInteger(KeyValueStoreEntry entry) throws IllegalArgumentException {
    if (entry == null) {
      return null;
    }
    if (!entry.type.equals(ElementDataType.integer.name())) {
      throw new IllegalArgumentException("requested int entry for " + "key: " + entry.key
          + ", but the corresponding entry in the store was " + "not of type: "
          + ElementDataType.integer.name());
    }
    try {
      return Integer.parseInt(entry.value);
    } catch (NumberFormatException ignored) {
      throw new IllegalArgumentException("requested int entry for " + "key: " + entry.key
          + ", but the value in the store failed to " + "parse to type: " + ElementDataType.integer
          .name());
    }
  }

  public static Boolean getBoolean(KeyValueStoreEntry entry) {
    if (entry == null) {
      return null;
    }
    if (!entry.type.equals(ElementDataType.bool.name())) {
      throw new IllegalArgumentException("requested boolean entry for " + "key: " + entry.key
          + ", but the corresponding entry in the store was " + "not of type: "
          + ElementDataType.bool.name());
    }
    if (entry.value == null) {
      return null;
    }
    // allow for true/false
    if (entry.value.compareToIgnoreCase(Boolean.TRUE.toString()) == 0) {
      return true;
    } else if (entry.value.compareToIgnoreCase(Boolean.FALSE.toString()) == 0) {
      return false;
    }

    // otherwise, assume it is a 0/1 value.
    try {
      return (Integer.parseInt(entry.value) != 0);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("requested boolean entry for " + "key: " + entry.key
          + ", but the value in the store failed to parse to type: " + ElementDataType.bool
          .name());
    }

  }

  public static String getString(KeyValueStoreEntry entry)
      throws IllegalArgumentException {
    if (entry == null) {
      return null;
    }
    // everything can be returned as a string....
    return entry.value;
  }

  public static <T> ArrayList<T> getArray(String appName, KeyValueStoreEntry entry, Class<T> clazz)
      throws IllegalArgumentException {
    CollectionType javaType = ODKFileUtils.mapper.getTypeFactory()
        .constructCollectionType(ArrayList.class, clazz);
    if (entry == null) {
      return null;
    }
    if (!entry.type.equals(ElementDataType.array.name())) {
      throw new IllegalArgumentException("requested list entry for " + "key: " + entry.key
          + ", but the corresponding entry in the store was " + "not of type: "
          + ElementDataType.array.name());
    }
    ArrayList<T> result = null;
    try {
      if (entry.value != null && !entry.value.isEmpty()) {
        result = ODKFileUtils.mapper.readValue(entry.value, javaType);
      }
    } catch (JsonParseException e) {
      WebLogger.getLogger(appName)
          .e("KeyValueStoreUtils", "getArray: problem parsing json list entry from the kvs");
      WebLogger.getLogger(appName).printStackTrace(e);
      throw new IllegalArgumentException("requested list entry for " + "key: " + entry.key
          + ", but the value in the store failed to " + "parse to type: " + ElementDataType.array
          .name());
    } catch (JsonMappingException e) {
      WebLogger.getLogger(appName)
          .e("KeyValueStoreUtils", "getArray: problem mapping json list entry from the kvs");
      WebLogger.getLogger(appName).printStackTrace(e);
      throw new IllegalArgumentException("requested list entry for " + "key: " + entry.key
          + ", but the value in the store failed to " + "parse to type: " + ElementDataType.array
          .name());
    } catch (IOException e) {
      WebLogger.getLogger(appName)
          .e("KeyValueStoreUtils", "getArray: i/o problem with json for list entry from the kvs");
      WebLogger.getLogger(appName).printStackTrace(e);
      throw new IllegalArgumentException("requested list entry for " + "key: " + entry.key
          + ", but the value in the store failed to " + "parse to type: " + ElementDataType.array
          .name());
    }
    return result;
  }

  public static String getObject(KeyValueStoreEntry entry)
      throws IllegalArgumentException {
    if (entry == null) {
      return null;
    }
    if (!entry.type.equals(ElementDataType.object.name()) && !entry.type
        .equals(ElementDataType.array.name())) {
      throw new IllegalArgumentException("requested object entry for " + "key: " + entry.key
          + ", but the corresponding entry in the store was " + "not of type: "
          + ElementDataType.object.name() + " or: " + ElementDataType.array.name());
    }
    return entry.value;
  }

}
