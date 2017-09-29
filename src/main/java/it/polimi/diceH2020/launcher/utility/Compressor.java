/*
Copyright 2016-2017 Eugenio Gianniti
Copyright 2016 Michele Ciavotta

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package it.polimi.diceH2020.launcher.utility;

import javax.validation.constraints.NotNull;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Compressor {
    // This is what Base64 *coders use by default
    private final static Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;

    public static @NotNull String compress(@NotNull String content) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write (content.getBytes ());
        }

        Base64.Encoder encoder = Base64.getUrlEncoder ();
        return encoder.encodeToString (out.toByteArray ());
    }

    public static @NotNull String decompress(@NotNull String encoded) throws IOException {
        StringBuilder builder = new StringBuilder ();
        Base64.Decoder decoder = Base64.getUrlDecoder ();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new ByteArrayInputStream(
                        decoder.decode (encoded))), DEFAULT_CHARSET))) {
            String line;
            while ((line = reader.readLine ()) != null) {
                builder.append (line).append ('\n');
            }
        }

        return builder.toString();
    }
}
