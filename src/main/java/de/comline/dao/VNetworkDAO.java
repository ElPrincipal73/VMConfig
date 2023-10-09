package de.comline.dao;

import de.comline.AppLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Repository
public class VNetworkDAO {

    @Value("${db.url}")
    private String DB_URL;
    @Value("${db.user}")
    private String USER;
    @Value("${db.pass}")
    private String PASS;
    private int maxUploadIndex = -1;
    public static final String[] COMMON_COLUMN_ORDER = {"vnetworkvisdkserver", "vnetworkhost", "upload_index", "import_date"};
    private static final Logger logger = AppLogger.getLogger(Controller.class.getName());

    public List<Map<String, Object>> fetchDataFromDB(String column, String date, Integer uploadIndex) {
        List<Map<String, Object>> daten = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            StringBuilder queryBuilder = buildQuery(column, date, uploadIndex);
            String query = queryBuilder.toString();

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                setQueryParameters(pstmt, date, uploadIndex);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        daten.add(extractRow(rs, column));
                    }
                }
            }
        } catch (SQLException e) {
            // Logging und weitere Fehlerbehandlung hier
        }
        return daten;
    }

    private Map<String, Object> extractRow(ResultSet rs, String column) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(column, rs.getObject(column));
        for (String col : COMMON_COLUMN_ORDER) {
            row.put(col, rs.getObject(col));
        }
        return row;
    }

    private StringBuilder buildQuery(String column, String date, Integer uploadIndex) {
        StringBuilder queryBuilder = new StringBuilder();

        // Ausgewählte Spalten
        queryBuilder.append("SELECT ").append(column).append(", ");
        for (String col : COMMON_COLUMN_ORDER) {
            queryBuilder.append(col).append(", ");
        }
        queryBuilder.setLength(queryBuilder.length() - 2);  // Letztes Komma entfernen

        // Basis-Query
        queryBuilder.append(" FROM ( SELECT ").append(column).append(", ");
        for (String col : COMMON_COLUMN_ORDER) {
            queryBuilder.append(col).append(", ");
        }
        queryBuilder.append("COUNT(*) OVER (PARTITION BY ")
                .append(column)
                .append(", upload_index) AS cnt ")
                .append("FROM vnetwork ");

        // Bedingungen
        if (date != null) {
            queryBuilder.append("WHERE import_date = to_date(?, 'YYYY-MM-DD') ");
        } else if (uploadIndex != null) {
            queryBuilder.append("WHERE upload_index >= ? AND upload_index <= ? ");
        }

        // Unterabfrage schließen und sortieren
        queryBuilder.append(") AS subquery WHERE cnt > 1 ")
                .append("ORDER BY upload_index DESC, ")
                .append(column)
                .append(" ASC");

        return queryBuilder;
    }

    private void setQueryParameters(PreparedStatement pstmt, String date, Integer uploadIndex) throws SQLException {
        if (date != null) {
            pstmt.setString(1, date);
        } else if (uploadIndex != null) {
            if (maxUploadIndex == -1) {
                updateMaxUploadIndex(pstmt.getConnection());
            }
            pstmt.setInt(1, maxUploadIndex - uploadIndex + 1);
            pstmt.setInt(2, maxUploadIndex);
        }
    }

    private void updateMaxUploadIndex(Connection conn) throws SQLException {
        try (PreparedStatement highestPstmt = conn.prepareStatement("SELECT MAX(upload_index) FROM vnetwork")) {
            ResultSet rs = highestPstmt.executeQuery();
            if (rs.next()) {
                maxUploadIndex = rs.getInt(1);
            }
        }
    }
}