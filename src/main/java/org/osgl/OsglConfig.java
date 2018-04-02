package org.osgl;

/*-
 * #%L
 * Java Tool
 * %%
 * Copyright (C) 2014 - 2018 OSGL (Open Source General Library)
 * %%
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
 * #L%
 */

import org.osgl.util.UtilConfig;
import org.osgl.util.algo.StringReplace;
import org.osgl.util.algo.StringSearch;

public class OsglConfig {

    /**
     * Default string search logic
     */
    public static StringSearch DEF_STRING_SEARCH = new StringSearch.SimpleStringSearch();

    /**
     * Default string replace logic
     */
    public static StringReplace DEF_STRING_REPLACE = new StringReplace.SimpleStringReplace();

    public static void setThreadLocalBufferLimit(int limit) {
        UtilConfig.setThreadLocalBufferLimit(limit);
    }


    public static void setThreadLocalCharBufferLimit(int limit) {
        UtilConfig.setThreadLocalCharBufferLimit(limit);
    }

    public static int getThreadLocalCharBufferLimit() {
        return UtilConfig.getThreadLocalCharBufferLimit();
    }

    public static void setThreadLocalCharBufferInitSize(int size) {
        UtilConfig.setThreadLocalCharBufferInitSize(size);
    }

    public static int getThreadLocalCharBufferInitSize() {
        return UtilConfig.getThreadLocalCharBufferInitSize();
    }

    public static void setThreadLocalByteArrayBufferLimit(int limit) {
        UtilConfig.setThreadLocalByteArrayBufferLimit(limit);
    }

    public static int getThreadLocalByteArrayBufferLimit() {
        return UtilConfig.getThreadLocalByteArrayBufferLimit();
    }

    public static void setThreadLocalByteArrayBufferInitSize(int size) {
        UtilConfig.setThreadLocalByteArrayBufferInitSize(size);
    }

    public static int getThreadLocalByteArrayBufferInitSize() {
        return UtilConfig.getThreadLocalByteArrayBufferInitSize();
    }
}
