/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.nidorx.http.gson;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import com.github.nidorx.http.gson.internal.Streams;
import com.github.nidorx.http.gson.stream.JsonReader;
import com.github.nidorx.http.gson.stream.JsonToken;
import com.github.nidorx.http.gson.stream.MalformedJsonException;

/**
 * A parser to parse Json into a parse tree of {@link JsonElement}s
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 * @since 1.3
 */
public final class JsonParser {
  /** @deprecated No need to instantiate this class, use the static methods instead. */
  @Deprecated
  public JsonParser() {}

  /**
   * Parses the specified JSON string into a parse tree
   *
   * @param json JSON text
   * @return a parse tree of {@link JsonElement}s corresponding to the specified JSON
   * @throws JsonParseException if the specified text is not valid JSON
   */
  public static JsonElement parseString(String json) throws com.github.nidorx.http.gson.JsonSyntaxException {
    return parseReader(new StringReader(json));
  }

  /**
   * Parses the specified JSON string into a parse tree
   *
   * @param reader JSON text
   * @return a parse tree of {@link JsonElement}s corresponding to the specified JSON
   * @throws JsonParseException if the specified text is not valid JSON
   */
  public static JsonElement parseReader(Reader reader) throws JsonIOException, com.github.nidorx.http.gson.JsonSyntaxException {
    try {
      com.github.nidorx.http.gson.stream.JsonReader jsonReader = new com.github.nidorx.http.gson.stream.JsonReader(reader);
      JsonElement element = parseReader(jsonReader);
      if (!element.isJsonNull() && jsonReader.peek() != JsonToken.END_DOCUMENT) {
        throw new com.github.nidorx.http.gson.JsonSyntaxException("Did not consume the entire document.");
      }
      return element;
    } catch (MalformedJsonException e) {
      throw new com.github.nidorx.http.gson.JsonSyntaxException(e);
    } catch (IOException e) {
      throw new JsonIOException(e);
    } catch (NumberFormatException e) {
      throw new com.github.nidorx.http.gson.JsonSyntaxException(e);
    }
  }

  /**
   * Returns the next value from the JSON stream as a parse tree.
   *
   * @throws JsonParseException if there is an IOException or if the specified
   *     text is not valid JSON
   */
  public static JsonElement parseReader(com.github.nidorx.http.gson.stream.JsonReader reader)
      throws JsonIOException, com.github.nidorx.http.gson.JsonSyntaxException {
    boolean lenient = reader.isLenient();
    reader.setLenient(true);
    try {
      return Streams.parse(reader);
    } catch (StackOverflowError e) {
      throw new JsonParseException("Failed parsing JSON source: " + reader + " to Json", e);
    } catch (OutOfMemoryError e) {
      throw new JsonParseException("Failed parsing JSON source: " + reader + " to Json", e);
    } finally {
      reader.setLenient(lenient);
    }
  }

  /** @deprecated Use {@link JsonParser#parseString} */
  @Deprecated
  public JsonElement parse(String json) throws com.github.nidorx.http.gson.JsonSyntaxException {
    return parseString(json);
  }

  /** @deprecated Use {@link JsonParser#parseReader(Reader)} */
  @Deprecated
  public JsonElement parse(Reader json) throws JsonIOException, com.github.nidorx.http.gson.JsonSyntaxException {
    return parseReader(json);
  }

  /** @deprecated Use {@link JsonParser#parseReader(com.github.nidorx.http.gson.stream.JsonReader)} */
  @Deprecated
  public JsonElement parse(JsonReader json) throws JsonIOException, JsonSyntaxException {
    return parseReader(json);
  }
}
