package com.demo.util;

import java.io.*;
import java.nio.file.*;

/**
 * File 5/6 - File utility
 * Contains:
 * 1. Path traversal sink (tainted filename)
 * 2. Resource leak: InputStream not closed if BufferedReader constructor throws
 *    (Coverity tracks partial construction patterns)
 */
public class FileUtil {

    /**
     * PATH TRAVERSAL + RESOURCE LEAK
     * 
     * Taint path: Controller.getReport(name) → Service.generateReport(name) → here
     * 
     * Resource leak scenario:
     * - FileInputStream created (resource acquired)
     * - If InputStreamReader or BufferedReader constructor throws (e.g., OOM),
     *   the FileInputStream is never closed
     */
    public static String readFile(String path) {
        try {
            // PATH TRAVERSAL SINK: 'path' contains user input
            FileInputStream fis = new FileInputStream(path);
            // If this line throws OOM, fis is leaked
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            // If this line throws, fis and isr are leaked
            BufferedReader reader = new BufferedReader(isr);

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();  // only closes on happy path
            return content.toString();

        } catch (IOException e) {
            // RESOURCE LEAK: fis never closed here
            return "File not found: " + path;
        }
    }
}
