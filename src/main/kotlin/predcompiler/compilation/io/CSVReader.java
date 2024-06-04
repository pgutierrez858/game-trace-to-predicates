package predcompiler.compilation.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVReader {

    public static class CSVResult {
        public String[] headers;
        public float[][] rows;

        public CSVResult(String[] headers, float[][] rows) {
            this.headers = headers;
            this.rows = rows;
        }
    }

    public static CSVResult readCSV(String filePath) throws IOException {
        List<float[]> rowList = new ArrayList<>();
        String[] headers = null;
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Read the header line
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new IOException("The file is empty");
            }

            // Split the header line to get column names
            headers = headerLine.split(",");

            // Read the rest of the file line by line
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length != headers.length) {
                    throw new IOException("Mismatch between number of columns and number of values in a row");
                }
                
                // Parse and store the float values in an array
                float[] rowValues = new float[values.length];
                for (int i = 0; i < values.length; i++) {
                    rowValues[i] = Float.parseFloat(values[i]);
                }
                rowList.add(rowValues);
            }
        }

        // Convert the list of rows to a 2D array
        float[][] rows = new float[rowList.size()][];
        rows = rowList.toArray(rows);

        return new CSVResult(headers, rows);
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java CSVReader <csv-file-path>");
            return;
        }

        String filePath = args[0];
        try {
            CSVResult result = readCSV(filePath);
            System.out.println("Headers: " + arrayToString(result.headers));
            for (float[] row : result.rows) {
                System.out.println("Row: " + arrayToString(row));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String arrayToString(String[] array) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static String arrayToString(float[] array) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
