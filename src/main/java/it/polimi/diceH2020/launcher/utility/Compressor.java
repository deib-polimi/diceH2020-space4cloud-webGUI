package it.polimi.diceH2020.launcher.utility;

import javax.validation.constraints.NotNull;
import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Compressor {

    public static @NotNull String compress(@NotNull String str) throws IOException {
        System.out.println("Input String length: " + str.length());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(str.getBytes());
        gzip.close();
        String outStr = out.toString("ISO-8859-1");
        System.out.println("Output String length: " + outStr.length());
        return outStr;
    }

    public static @NotNull String originalDecompress(@NotNull String string) throws IOException {
        return decompressHelper(string, true);
    }
    /* TODO: is there a reason why the Results and InteractiveExperiment classes had a decompress
     * method of their own that adds newlines?
     */
    public static @NotNull String decompress(@NotNull String string) throws IOException {
        return decompressHelper(string, false);
    }

    private static @NotNull String decompressHelper(@NotNull String string, boolean newLine) throws IOException {
        System.out.println("Input String length: " + string.length());
        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(string.getBytes("ISO-8859-1")));
        BufferedReader bf = new BufferedReader(new InputStreamReader(gis, "ISO-8859-1"));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = bf.readLine()) != null) {
            builder.append(line);
            if (newLine) builder.append('\n');
        }
        String output = builder.toString();
        System.out.println("Output String length: " + output.length());
        return output;
    }

}
