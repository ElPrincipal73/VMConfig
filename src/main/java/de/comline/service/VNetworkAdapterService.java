package de.comline;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class VNetworkAdapterService {

    @Value("${db.url}")
    private String DB_URL;
    @Value("${db.user}")
    private String USER;
    @Value("${db.pass}")
    private String PASS;

    @Value("${excludedNetworkAdapter}")
    private String excludedNetworkAdapter;


    public List<de.comline.VNetworkAdapterModel> fetchVNetworkAdapterData(String date, Integer uploadIndex) {
        List<de.comline.VNetworkAdapterModel> daten = new ArrayList<>();
        String tableName = "vnetwork";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            int maxUploadIndex = getMaxUploadIndex(conn, tableName, date);
            StringBuilder queryBuilder = buildNetworkAdapterQuery(tableName, date, uploadIndex);

            try (PreparedStatement pstmt = conn.prepareStatement(queryBuilder.toString())) {
                setNetworkAdapterQueryParameters(pstmt, maxUploadIndex, date, uploadIndex);

                try (ResultSet rsAdapter = pstmt.executeQuery()) {
                    while (rsAdapter.next()) {
                        de.comline.VNetworkAdapterModel model = new de.comline.VNetworkAdapterModel();
                        model.setVNetworkAdapter(rsAdapter.getString("vNetworkAdapter"));
                        model.setVNetworkVISDKServer(rsAdapter.getString("vNetworkVISDKServer"));
                        model.setVNetworkHost(rsAdapter.getString("vNetworkHost"));
                        model.setImportDate(rsAdapter.getString("import_date"));
                        model.setUploadIndex(rsAdapter.getInt("upload_index"));
                        daten.add(model);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return daten;
    }

    private int getMaxUploadIndex(Connection conn, String tableName, String date) throws SQLException {
        StringBuilder getMaxUploadIndexQuery = new StringBuilder();
        getMaxUploadIndexQuery.append("SELECT MAX(upload_index) AS max_index FROM ").append(tableName);
        if (date != null) {
            getMaxUploadIndexQuery.append(" WHERE import_date = to_date(?, 'YYYY-MM-DD')");
        }

        try (PreparedStatement getMaxUploadIndexStmt = conn.prepareStatement(getMaxUploadIndexQuery.toString())) {
            if (date != null) getMaxUploadIndexStmt.setString(1, date);
            ResultSet rs = getMaxUploadIndexStmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("max_index");
            }
            return 0;
        }
    }

    private StringBuilder buildNetworkAdapterQuery(String tableName, String date, Integer uploadIndex) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT vNetworkAdapter, vNetworkVISDKServer, vNetworkHost, import_date, upload_index ");
        queryBuilder.append("FROM ").append(tableName);
        queryBuilder.append(" WHERE ");
        if (date != null) {
            queryBuilder.append("import_date = to_date(?, 'YYYY-MM-DD') AND ");
        } else if (uploadIndex != null) {
            queryBuilder.append("upload_index <= ? AND upload_index >= ? AND ");
        } else {
            queryBuilder.append("upload_index = ? AND ");
        }
        queryBuilder.append("vNetworkAdapter IS NOT NULL AND vNetworkAdapter != '' AND vNetworkAdapter != ? ");
        queryBuilder.append("ORDER BY upload_index DESC");
        return queryBuilder;
    }

    private void setNetworkAdapterQueryParameters(PreparedStatement pstmt, int maxUploadIndex, String date, Integer uploadIndex) throws SQLException {
        if (date != null) {
            pstmt.setString(1, date);
            pstmt.setString(2, excludedNetworkAdapter);
        } else if (uploadIndex != null) {
            pstmt.setInt(1, maxUploadIndex);
            pstmt.setInt(2, maxUploadIndex - uploadIndex + 1);
            pstmt.setString(3, excludedNetworkAdapter);
        } else {
            pstmt.setInt(1, maxUploadIndex);
            pstmt.setString(2, excludedNetworkAdapter);
        }
    }
}