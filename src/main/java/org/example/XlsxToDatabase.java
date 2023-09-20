package org.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@Component
public class XlsxToDatabase {
    @Value("${db.url}")
    private String DB_URL;
    @Value("${db.user}")
    private String USER;
    @Value("${db.pass}")
    private String PASS;

    private static final Logger logger = Logger.getLogger(XlsxToDatabase.class.getName());
    static {
        try {
            FileHandler fh = new FileHandler("LogFile.log", true);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
            logger.addHandler(fh);
            logger.setLevel(Level.ALL); // alle Log-Nachrichten erfassen unabhängig von der Priorität
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void importDataFromExcel(MultipartFile file) throws IOException, SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            Workbook workbook = new XSSFWorkbook(file.getInputStream());
            Iterator<Sheet> sheetIterator = workbook.sheetIterator();
            while (sheetIterator.hasNext()) {
                Sheet sheet = sheetIterator.next();
                List<String> columnNames = extractColumnNames(sheet);
                List<List<String>> rows = extractRows(sheet, columnNames.size());
                createTableAndImportData(conn, sheet.getSheetName().toLowerCase(), columnNames, rows);
            }
        } catch (Exception e) {
            // Logge des Fehlers in die Datei
            logger.severe("Fehler beim Importieren der Daten aus der Excel-Datei evtl. Excel Datei leer bzw. keine Spaltennamen");
            e.printStackTrace();
            // Wirf eine IOException mit einer benutzerdefinierten Fehlermeldung Popup Fenster
            throw new IOException("Fehler beim Importieren der Daten aus der Excel-Datei.");
        }
    }

    private List<String> extractColumnNames(Sheet sheet) throws IOException {
        List<String> columnNames = new ArrayList<>();
        Row firstRow = sheet.getRow(0);
        Iterator<Cell> cellIterator = firstRow.cellIterator();

        if (firstRow == null || firstRow.getLastCellNum() <= 0) {
            // Logge den Fehler in die Datei
            logger.severe("Keine Spaltenüberschriften gefunden");
            // Wirf eine IOException mit einer benutzerdefinierten Fehlermeldung
            throw new IOException("Keine Spaltenüberschriften gefunden.");
        }

        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            String cellValue = cell.toString().toLowerCase().trim();

            if(cellValue.isEmpty()){
                // Logge des Fehlers in die Datei
                logger.severe("Leere Spaltenüberschrift gefunden");
                // Wirf eine IOException mit einer benutzerdefinierten Fehlermeldung
                throw new IOException("Leere Spaltenüberschrift gefunden.");
            }

            columnNames.add(cellValue);
        }

        return columnNames;
    }


    private List<List<String>> extractRows(Sheet sheet, int numColumns) {
        List<List<String>> rows = new ArrayList<>();
        Iterator<Row> rowIterator = sheet.rowIterator();
        rowIterator.next(); // Skip header row

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            List<String> rowData = new ArrayList<>();
            for (int i = 0; i < numColumns; i++) {
                rowData.add(null);
            }
            Iterator<Cell> cellIterator = row.cellIterator();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                int cellIndex = cell.getColumnIndex();
                rowData.set(cellIndex, cell.toString());
            }
            rows.add(rowData);
        }
        return rows;
    }
    private static void createTableAndImportData(Connection conn, String tableName, List<String> columnNames, List<List<String>> rows) throws SQLException {
        StringJoiner columnDefinition = new StringJoiner(", ");
        StringJoiner placeholders = new StringJoiner(", ");
        for (String columnName : columnNames) {
            columnDefinition.add("\"" + columnName + "\" text");
            placeholders.add("?");
        }
        columnDefinition.add("\"import_date\" date");
        columnDefinition.add("\"upload_index\" integer");

        int uploadIndex = 1;

        try (Statement stmt = conn.createStatement()) {
            ResultSet tables = conn.getMetaData().getTables(null, null, tableName, null);
            if (!tables.next()) {
                String createTableSQL = "CREATE TABLE \"" + tableName + "\" (" + columnDefinition.toString() + ")";
                stmt.execute(createTableSQL);
            } else {
                String queryUploadIndex = "SELECT MAX(upload_index) FROM \"" + tableName + "\"";
                ResultSet rs = stmt.executeQuery(queryUploadIndex);
                if (rs.next()) {
                    uploadIndex = rs.getInt(1) + 1;
                }
            }
        }

        String insertSQL = "INSERT INTO \"" + tableName + "\" VALUES (" + placeholders.toString() + ", CURRENT_TIMESTAMP, " + uploadIndex + ")";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            conn.setAutoCommit(false);
            int count = 0;
            for (List<String> row : rows) {
                int size = row.size();
                for (int i = 0; i < size; i++) {
                    pstmt.setString(i + 1, row.get(i));
                }
                for (int i = size; i < columnNames.size(); i++) {
                    pstmt.setString(i + 1, null);
                }
                pstmt.addBatch();
                if (++count % 1000 == 0) {
                    pstmt.executeBatch();
                }
            }
            pstmt.executeBatch();
            conn.commit();
        }
    }
}

