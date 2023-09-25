package de.comline;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

@org.springframework.stereotype.Controller
public class Controller {
    //   Datenbankzugänge normalerweise in der Umgebungsvariable speichern
    //   nur für Testzwecken application.properties gespeichert
    @Value("${db.url}")
    private String DB_URL;
    @Value("${db.user}")
    private String USER;
    @Value("${db.pass}")
    private String PASS;

    //
    private static final Logger logger = AppLogger.getLogger(FileUploadController.class.getName());
    // globale Variable für Error "message"
    String message = "Keine Daten für diesen Zeitraum vorhanden";

    // Starten der Anwendung --> Index
    @GetMapping("/index")
    public String index() {
        return "index";
    }

    // Hilfe Seite aufrufen
    @GetMapping("/show_Hilfe")
    public String showHelp() {
        return "show_Hilfe";
    }
    @GetMapping("/show_vnetworkinfo")
    public String showVNetworkInfo(@RequestParam(required = false) String date,
                                   @RequestParam(required = false) Integer uploadIndex,
                                   @RequestParam String column,
                                   Model model) {
        String previousValue = null;
        List<Map<String, Object>> daten = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT ")
                    .append(column).append(", vnetworkvisdkserver, vnetworkhost, upload_index, import_date ")
                    .append("FROM ( ")
                    .append("  SELECT ")
                    .append(column).append(", vnetworkvisdkserver, vnetworkhost, upload_index, import_date, COUNT(*) OVER (PARTITION BY ").append(column).append(", upload_index) AS cnt ")
                    .append("  FROM vnetwork ");
            if (date != null) {
                queryBuilder.append(" WHERE import_date = to_date(?, 'YYYY-MM-DD')");
            } else if (uploadIndex != null) {
                try (PreparedStatement highestPstmt = conn.prepareStatement("SELECT MAX(upload_index) FROM vnetwork")) {
                    ResultSet rs = highestPstmt.executeQuery();
                    if (rs.next()) {
                        int highestUploadIndex = rs.getInt(1);
                        queryBuilder.append(" WHERE upload_index >= ? AND upload_index <= ?");
                    }
                }
            }

            queryBuilder.append(") AS subquery ")
                    .append("WHERE cnt > 1 ")
                    .append("ORDER BY upload_index DESC, ").append(column).append(" ASC");

            String query = queryBuilder.toString();


            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                if (date != null) {
                    pstmt.setString(1, date);
                } else if (uploadIndex != null) {
                    try (PreparedStatement highestPstmt = conn.prepareStatement("SELECT MAX(upload_index) FROM vnetwork")) {
                        ResultSet rs = highestPstmt.executeQuery();
                        if (rs.next()) {
                            int highestUploadIndex = rs.getInt(1);
                            pstmt.setInt(1, highestUploadIndex - uploadIndex + 1);
                            pstmt.setInt(2, highestUploadIndex);
                        }
                    }
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put(column, rs.getObject(column));
                        row.put("vnetworkvisdkserver", rs.getObject("vnetworkvisdkserver"));
                        row.put("vnetworkhost", rs.getObject("vnetworkhost"));
                        row.put("upload_index", rs.getObject("upload_index"));
                        row.put("import_date", rs.getObject("import_date"));

                        String currentValue = rs.getString(column);
                        if (currentValue != null && !currentValue.equals(previousValue)) {
                            row.put("isFirstDuplicate", true);
                            previousValue = currentValue;
                        } else {
                            row.put("isFirstDuplicate", false);
                        }

                        daten.add(row);
                    }
                }
            }

            if (daten.isEmpty()) {
                model.addAttribute("errorMessage", "Keine Daten gefunden.");
            }

            String[] columnOrder = {column, "vnetworkvisdkserver", "vnetworkhost", "upload_index", "import_date"};
            model.addAttribute("columnOrder", columnOrder);
            model.addAttribute("daten", daten);

            if (date != null) {
                model.addAttribute("date", date);
            } else {
                model.addAttribute("uploadIndex", uploadIndex);
            }

        } catch (SQLException e) {
            model.addAttribute("errorMessage", "SQL-Fehler: " + e.getMessage());
        }

        return "show_vnetworkinfo";
    }


    // Wert in der config.properties Datei setzen  -->  vNetworkAdapter | vNetworkVISDKServer | vNetworkHost | upload_index |import_date
    @GetMapping("/show_vnetworkadapter")
    public String networkAdapter(@RequestParam(required = false) String date, @RequestParam(required = false) Integer uploadIndex, Model model) {
        Configuartion config = new Configuartion();
        String excludedNetworkAdapter = config.getExcludedNetworkAdapter();
        List<Map<String, Object>> daten = new ArrayList<>();
        String tableName = "vnetwork";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            StringBuilder getMaxUploadIndexQuery = new StringBuilder();
            getMaxUploadIndexQuery.append("SELECT MAX(upload_index) AS max_index FROM ").append(tableName);

            if (date != null) {
                getMaxUploadIndexQuery.append(" WHERE import_date = to_date(?, 'YYYY-MM-DD')");
            }

            try (PreparedStatement getMaxUploadIndexStmt = conn.prepareStatement(getMaxUploadIndexQuery.toString())) {
                if (date != null) getMaxUploadIndexStmt.setString(1, date);

                ResultSet rs = getMaxUploadIndexStmt.executeQuery();
                if (rs.next()) {
                    int maxUploadIndex = rs.getInt("max_index");

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

                    try (PreparedStatement pstmt = conn.prepareStatement(queryBuilder.toString())) {
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

                        try (ResultSet rsAdapter = pstmt.executeQuery()) {
                            while (rsAdapter.next()) {
                                daten.add(getRowForAdapter(rsAdapter));
                            }
                        }
                    }

                    if (daten.isEmpty()) {
                        model.addAttribute("errorMessage", "Keine Daten gefunden");
                        logger.severe("Keine Daten gefunden");
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Datenbankfehler/VNetworkAdapter: " + e.getMessage());
            model.addAttribute("errorMessage", "Ein SQL-Fehler ist aufgetreten: " + e.getMessage());
            e.printStackTrace();
        }

        String[] columnOrder = {"vNetworkAdapter", "vNetworkVISDKServer", "vNetworkHost", "upload_index", "import_date"};
        model.addAttribute("columnOrder", columnOrder);
        model.addAttribute("daten", daten);

        if (date != null) {
            model.addAttribute("date", date);
        } else if (uploadIndex != null) {
            model.addAttribute("uploadIndex", uploadIndex);
        }

        return "show_vnetworkadapter";
    }

    private Map<String, Object> getRowForAdapter(ResultSet rsAdapter) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        row.put("vNetworkAdapter", rsAdapter.getObject("vNetworkAdapter"));
        row.put("vNetworkVISDKServer", rsAdapter.getObject("vNetworkVISDKServer"));
        row.put("vNetworkHost", rsAdapter.getObject("vNetworkHost"));
        row.put("import_date", rsAdapter.getObject("import_date"));
        row.put("upload_index", rsAdapter.getObject("upload_index"));
        return row;
    }


    // Show "all_snapshot"  --> vSnapshotVMName | vSnapshotVISDKServer | vSnapshotHost | upload_index |	import_date
    @GetMapping("/show_all_vSnapshot")
    public String vnetworkShowAll(@RequestParam(required = false) String date, @RequestParam(required = false) Integer uploadIndex, Model model) {
        List<Map<String, Object>> daten = new ArrayList<>();
        String tableName = "vSnapshot";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            StringBuilder getMaxUploadIndexQuery = new StringBuilder("SELECT MAX(upload_index) AS max_index FROM ").append(tableName);
            try (PreparedStatement getMaxUploadIndexStmt = conn.prepareStatement(getMaxUploadIndexQuery.toString())) {
                ResultSet rs = getMaxUploadIndexStmt.executeQuery();
                if (rs.next()) {
                    int maxUploadIndex = rs.getInt("max_index");
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

                    try (PreparedStatement pstmt = conn.prepareStatement(queryBuilder.toString())) {
                        if (date != null) {
                            pstmt.setString(1, date);
                        } else if (uploadIndex != null) {
                            pstmt.setInt(1, maxUploadIndex);
                            pstmt.setInt(2, maxUploadIndex - uploadIndex + 1);
                        } else {
                            pstmt.setInt(1, maxUploadIndex);
                        }

                        try (ResultSet rsshowall_inner = pstmt.executeQuery()) {
                            while (rsshowall_inner.next()) {
                                daten.add(getRowSnapAll(rsshowall_inner));
                            }
                        }
                    }

                    if (daten.isEmpty()) {
                        model.addAttribute("errorMessage", "Keine Daten gefunden");
                        logger.severe("Keine Daten gefunden");
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Datenbankfehler/AllSnapshot: " + e.getMessage());
            model.addAttribute("errorMessage", "Ein SQL-Fehler ist aufgetreten: " + e.getMessage());
            e.printStackTrace();
        }

        String[] columnOrder = {"vSnapshotVMName", "vSnapshotVISDKServer", "vSnapshotHost", "upload_index", "import_date"};
        model.addAttribute("columnOrder", columnOrder);
        model.addAttribute("daten", daten);

        if (date != null) {
            model.addAttribute("date", date);
        } else if (uploadIndex != null) {
            model.addAttribute("uploadIndex", uploadIndex);
        }

        return "show_all_vSnapshot";
    }



    private Map<String, Object> getRowSnapAll(ResultSet rs_showAll) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        row.put("vSnapshotVMName", rs_showAll.getObject("vSnapshotVMName"));
        row.put("vSnapshotVISDKServer", rs_showAll.getObject("vSnapshotVISDKServer"));
        row.put("vSnapshotHost", rs_showAll.getObject("vSnapshotHost"));
        row.put("import_date", rs_showAll.getObject("import_date"));
        row.put("upload_index", rs_showAll.getObject("upload_index"));
        return row;
    }
}