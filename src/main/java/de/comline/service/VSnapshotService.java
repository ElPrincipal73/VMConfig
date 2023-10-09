package de.comline.service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import de.comline.Models.VSnapshot;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VSnapshotService {

    @Value("${db.url}")
    private String DB_URL;

    @Value("${db.user}")
    private String USER;

    @Value("${db.pass}")
    private String PASS;

    public List<VSnapshot> getAllSnapshots(String date, Integer uploadIndex) {
        List<VSnapshot> daten = new ArrayList<>();
        String tableName = "vSnapshot";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            int maxUploadIndex = getMaxUploadIndex(conn, tableName);
            StringBuilder queryBuilder = buildSnapshotQuery(tableName, date, uploadIndex);

            try (PreparedStatement pstmt = conn.prepareStatement(queryBuilder.toString())) {
                setSnapshotQueryParameters(pstmt, maxUploadIndex, date, uploadIndex);

                try (ResultSet rsShowAll = pstmt.executeQuery()) {
                    while (rsShowAll.next()) {
                        daten.add(getRowSnapAll(rsShowAll));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return daten;
    }
    private int getMaxUploadIndex(Connection conn, String tableName) throws SQLException {
        StringBuilder getMaxUploadIndexQuery = new StringBuilder("SELECT MAX(upload_index) AS max_index FROM ").append(tableName);
        try (PreparedStatement getMaxUploadIndexStmt = conn.prepareStatement(getMaxUploadIndexQuery.toString())) {
            ResultSet rs = getMaxUploadIndexStmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("max_index");
            }
            return 0;
        }
    }
    private StringBuilder buildSnapshotQuery(String tableName, String date, Integer uploadIndex) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT vSnapshotVMName, vSnapshotVISDKServer, vSnapshotHost, import_date, upload_index FROM ");
        queryBuilder.append(tableName);
        queryBuilder.append(" WHERE ");

        if (date != null) {
            queryBuilder.append("import_date = to_date(?, 'YYYY-MM-DD')");
        } else if (uploadIndex != null) {
            queryBuilder.append("upload_index <= ? AND upload_index >= ?");
        } else {
            queryBuilder.append("upload_index = ?");
        }

        queryBuilder.append(" ORDER BY upload_index DESC");
        return queryBuilder;
    }
    private void setSnapshotQueryParameters(PreparedStatement pstmt, int maxUploadIndex, String date, Integer uploadIndex) throws SQLException {
        if (date != null) {
            pstmt.setString(1, date);
        } else if (uploadIndex != null) {
            pstmt.setInt(1, maxUploadIndex);
            pstmt.setInt(2, maxUploadIndex - uploadIndex + 1);
        } else {
            pstmt.setInt(1, maxUploadIndex);
        }
    }

    private VSnapshot getRowSnapAll(ResultSet rsShowAll) throws SQLException {
        VSnapshot vSnapshot = new VSnapshot();
        vSnapshot.setVSnapshotVMName(rsShowAll.getString("vSnapshotVMName"));
        vSnapshot.setVSnapshotVISDKServer(rsShowAll.getString("vSnapshotVISDKServer"));
        vSnapshot.setVSnapshotHost(rsShowAll.getString("vSnapshotHost"));
        vSnapshot.setImport_date(rsShowAll.getString("import_date"));
        vSnapshot.setUpload_index(rsShowAll.getInt("upload_index"));
        return vSnapshot;
    }

}
