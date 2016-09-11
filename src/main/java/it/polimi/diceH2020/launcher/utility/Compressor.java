/*
Copyright 2016 Michele Ciavotta
Copyright 2016 Eugenio Gianniti

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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Compressor {

    public static @NotNull String compress(@NotNull String str) throws IOException {
        //System.out.println("Input String length: " + str.length());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(str.getBytes());
        gzip.close();
        String outStr = out.toString("ISO-8859-1");
        //System.out.println("Output String length: " + outStr.length());
        return outStr;
    }

    public static @NotNull String decompress(@NotNull String string) throws IOException {
        //System.out.println("Input String length: " + string.length());
        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(string.getBytes("ISO-8859-1")));
        BufferedReader bf = new BufferedReader(new InputStreamReader(gis, "ISO-8859-1"));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = bf.readLine()) != null) {
            builder.append(line).append('\n');
        }
        String output = builder.toString();
        //System.out.println("Output String length: " + output.length());
        return output;
    }

}
