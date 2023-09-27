package de.comline;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.sql.*;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Logger;

@Component
public class DatabaseHandler {

    @Autowired
    private DatabaseConfig databaseConfig;

    private static final Logger logger = Logger.getLogger(DatabaseHandler.class.getName());

    public Connection connectToDatabase() throws SQLException {
        return DriverManager.getConnection(databaseConfig.getDbUrl(), databaseConfig.getUser(), databaseConfig.getPass());
    }
    public void createTableAndImportData(Connection conn, String tableName, List<String> columnNames, List<List<String>> rows) throws SQLException {
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
                boolean isDuplicateHeader = true;

                // Überprüfe, ob die Zeile identisch mit der Spaltenüberschrift ist
                for (int i = 0; i < columnNames.size(); i++) {
                    String cellValue = row.get(i);
                    if (cellValue == null || !cellValue.toLowerCase().trim().equals(columnNames.get(i))) {
                        isDuplicateHeader = false;
                        break;
                    }
                }

                if (!isDuplicateHeader) {
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
            }
            pstmt.executeBatch();
            conn.commit();
        }
    }
}
