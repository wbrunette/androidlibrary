/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.database.utilities;

import android.annotation.SuppressLint;
import android.database.Cursor;

import org.opendatakit.logging.WebLogger;
import org.opendatakit.utilities.ODKFileUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Used all over the place, FormInfo, IntitializationUtil, FormsProvider, CsvUtil, etc..
 */
public final class CursorUtils {

  /**
   * values that can be returned from getTableHealth()
   * These first three are used in several places
   */
  @SuppressWarnings("WeakerAccess")
  public static final int TABLE_HEALTH_IS_CLEAN = 0b000;
  @SuppressWarnings("WeakerAccess")
  public static final int TABLE_HEALTH_HAS_CONFLICTS = 0b001;
  @SuppressWarnings("WeakerAccess")
  public static final int TABLE_HEALTH_HAS_CHECKPOINTS = 0b010;
  /**
   * This isn't used anywhere
   */
  public static final int TABLE_HEALTH_HAS_CHANGES = 0b100;

  public static final String DEFAULT_LOCALE = "default";
  public static final String DEFAULT_CREATOR = "anonymous";

  /**
   * Do not instantiate this class
   */
  private CursorUtils() {
  }

  /**
   * Used in ODKDatabaseImplUtils
   *
   * @param health the table health
   * @return whether the table has conflicts
   */
  @SuppressWarnings("unused")
  public static int setTableHealthHasConflicts(int health) {
    return health | TABLE_HEALTH_HAS_CONFLICTS;
  }

  /**
   * Used in ODKDatabaseImplUtils
   *
   * @param health the table health
   * @return whether the table has checkpoints
   */
  @SuppressWarnings("unused")
  public static int setTableHealthHasCheckpoints(int health) {
    return health | TABLE_HEALTH_HAS_CHECKPOINTS;
  }

  /**
   * Used in ODKDatabaseImplUtils
   *
   * @param health the table health
   * @return whether the table has changes
   */
  @SuppressWarnings("unused")
  public static int setTableHealthHasChanges(int health) {
    return health | TABLE_HEALTH_HAS_CHANGES;
  }

  /**
   * This is totally unused
   *
   * @param health the table's health
   * @return whether the table is clean
   */
  public static boolean getTableHealthIsClean(int health) {
    return health == TABLE_HEALTH_IS_CLEAN;
  }

  /**
   * This is totally unused
   *
   * @param health the health of the table
   * @return whether the table has either conflicts or checkpoints
   */
  public static boolean getTableHealthHasConflictsOrCheckpoints(int health) {
    return getTableHealthHasConflicts(health) || getTableHealthHasCheckpoints(health);
  }

  /**
   * Used by OdkDatabaseServiceImpl, FetchInConflictTableIdsLoader
   *
   * @param health the health of the table
   * @return whether the table has conflicts
   */
  @SuppressWarnings("WeakerAccess")
  public static boolean getTableHealthHasConflicts(int health) {
    return (health & TABLE_HEALTH_HAS_CONFLICTS) != 0;
  }

  /**
   * Used by OdkDatabaseServiceImpl
   *
   * @param health the health of the table
   * @return whether the table has checkpoints
   */
  @SuppressWarnings("WeakerAccess")
  public static boolean getTableHealthHasCheckpoints(int health) {
    return (health & TABLE_HEALTH_HAS_CHECKPOINTS) != 0;
  }

  /**
   * Used by OdkDatabaseServiceImpl
   *
   * @param health the health of the table
   * @return whether the table has changes
   */
  @SuppressWarnings("unused")
  public static boolean getTableHealthHasChanges(int health) {
    return (health & TABLE_HEALTH_HAS_CHANGES) != 0;
  }

  /**
   * Return the data stored in the cursor at the given index and given position
   * (ie the given row which the cursor is currently on) as null OR a String.
   * <p>
   * NB: Currently only checks for Strings, long, int, and double.
   * <p>
   * Used in many places
   *
   * @param c the cursor
   * @param i the index into the cursors result
   * @return the data at that index as a string
   */
  @SuppressLint("NewApi")
  @SuppressWarnings("unused")
  public static String getIndexAsString(Cursor c, int i) {
    // If you add additional return types here be sure to modify the javadoc.
    if (i == -1)
      return null;
    if (c.isNull(i)) {
      return null;
    }
    switch (c.getType(i)) {
    case Cursor.FIELD_TYPE_STRING:
      return c.getString(i);
    case Cursor.FIELD_TYPE_FLOAT: {
      // the static version of this seems to have problems...
      Double d = c.getDouble(i);
      return d.toString();
    }
    case Cursor.FIELD_TYPE_INTEGER: {
      // the static version of this seems to have problems...
      Long l = c.getLong(i);
      return l.toString();
    }
    default:
      // all the possible values are enumerated. Nothing should fall here.
    case Cursor.FIELD_TYPE_NULL:
      // should be handled by c.isNull() earlier
    case Cursor.FIELD_TYPE_BLOB:
      throw new IllegalStateException("Unexpected data type in SQLite table");
    }
  }

  /**
   * Retrieve the data type of the [i] field in the Cursor.
   * Used by FormsProvider, CursorUtils, ODKDatabaseImplUtils
   *
   * @param c the cursor
   * @param i the index into the result in the cursor
   * @return A class that the cell can be safely casted to
   */
  @SuppressWarnings("unused")
  public static Class<?> getIndexDataType(Cursor c, int i) {
    switch (c.getType(i)) {
    case Cursor.FIELD_TYPE_STRING:
      return String.class;
    case Cursor.FIELD_TYPE_FLOAT:
      return Double.class;
    case Cursor.FIELD_TYPE_INTEGER:
      return Long.class;
    case Cursor.FIELD_TYPE_NULL:
      // this cell is a null. Use String class as a placeholder.
      return String.class;
    default:
    case Cursor.FIELD_TYPE_BLOB:
      throw new IllegalStateException("Unexpected data type in SQLite table");
    }
  }

  /**
   * Return the data stored in the cursor at the given index and given position
   * (ie the given row which the cursor is currently on) as null OR whatever
   * data type it is.
   * <p>
   * This does not actually convert data types from one type to the other.
   * Instead, it safely preserves null values and returns boxed data values. If
   * you specify ArrayList or HashMap, it JSON deserializes the value into one
   * of those.
   * Used in FormInfo, CursorUtils, ODKDatabaseImplUtils, SubmissionProvider, FormsProvider,
   * FormIdStruct
   *
   * @param c     the database cursor
   * @param clazz a class to deserialize to
   * @param i     the index into the cursor's result
   * @return the element at index i in the cursors result, casted to class clazz
   */
  @SuppressWarnings("unchecked unused")
  public static <T> T getIndexAsType(Cursor c, Class<T> clazz, int i) {
    // If you add additional return types here be sure to modify the javadoc.
    try {
      if (i == -1)
        return null;
      if (c.isNull(i)) {
        return null;
      }
      if (clazz == Long.class) {
        Long l = c.getLong(i);
        return (T) l;
      } else if (clazz == Double.class) {
        Double d = c.getDouble(i);
        return (T) d;
      } else if (clazz == String.class) {
        String str = c.getString(i);
        return (T) str;
      } else if (clazz == Boolean.class) {
        // stored as integers
        Integer l = c.getInt(i);
        return (T) Boolean.valueOf(l != 0);
      } else if (clazz == ArrayList.class) {
        // json deserialization of an array
        String str = c.getString(i);
        return (T) ODKFileUtils.mapper.readValue(str, ArrayList.class);
      } else if (clazz == HashMap.class) {
        // json deserialization of an object
        String str = c.getString(i);
        return (T) ODKFileUtils.mapper.readValue(str, HashMap.class);
      } else {
        throw new IllegalStateException("Unexpected data type in SQLite table");
      }
    } catch (ClassCastException | IOException e) {
      // JsonParseException and JsonMappingException both subclass IOException
      WebLogger.getLogger(null).printStackTrace(e);
      throw new IllegalStateException(
          "Unexpected data type conversion failure " + e + " in SQLite table");
    }
  }
}

