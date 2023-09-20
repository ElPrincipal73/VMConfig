package org.example;
// Paketname ändern

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // Double Vnetworkvmname | vNetworkVISDKServer | vNetworkHost | upload_index |	import_date
    @GetMapping("/show_vnetworkvmname")
    public String getVNetworkVMName(@RequestParam(required = false) String date,
                                    @RequestParam(required = false) Integer uploadIndex, Model model) {
        List<Map<String, Object>> daten = new ArrayList<>();
        final String tableName = "vnetwork";
        final String[] columnOrder = {"vNetworkVMName", "vNetworkVISDKServer", "vNetworkHost", "upload_index", "import_date"};

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            if (date != null) {
                daten = getDataByDate(conn, date, tableName);
            } else {
                daten = getDataByUploadIndex(conn, uploadIndex, tableName);
            }
        } catch (SQLException e) {
            logger.severe(String.format("Datenbankfehler/VNetworkVMName: %s", e.getMessage()));
            model.addAttribute("errorMessage", "Ein SQL-Fehler ist aufgetreten: " + e.getMessage());
        }

        if (daten.isEmpty()) {
            String errorMessage = "Keine Daten für den gewählten Zeitraum gefunden.";
            model.addAttribute("errorMessage", errorMessage);
            logger.severe(errorMessage);
        }

        addAttributesToModel(model, daten, columnOrder, date, uploadIndex);

        return "show_vnetworkvmname";
    }

    public void addAttributesToModel(Model model, List<Map<String, Object>> daten, String[] columnOrder, String date, Integer uploadIndex) {
        model.addAttribute("columnOrder", columnOrder);
        model.addAttribute("daten", daten);

        if (date != null) {
            model.addAttribute("date", date);
        } else {
            model.addAttribute("uploadIndex", uploadIndex);
        }
    }
//    Methode Liste Query nach Datum
    private List<Map<String, Object>> getDataByDate(Connection conn, String date, String tableName) throws SQLException {
        List<Map<String, Object>> daten = new ArrayList<>();
        final String query = buildQuery(tableName, true);

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, date);
            try (ResultSet rsvmanme = pstmt.executeQuery()) {
                while (rsvmanme.next()) {
                    daten.add(getRow(rsvmanme));
                }
            }
        } catch (SQLException e) {
            logger.severe("Datenbankfehler/VNetworkVMName - Datum: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("Allg. Fehler VNetworkVMName - Datum: " + e.getMessage());
        }
        return daten;
    }
    //    Methode Liste Query nach uploadIndex
    private List<Map<String, Object>> getDataByUploadIndex(Connection conn, Integer uploadIndex, String tableName) throws SQLException {
        List<Map<String, Object>> daten = new ArrayList<>();
        final String query = buildQuery(tableName, false);
        final String getMaxUploadIndexQuery = "SELECT MAX(upload_index) AS max_index FROM " + tableName;

        try (PreparedStatement getMaxUploadIndexStmt = conn.prepareStatement(getMaxUploadIndexQuery)) {
            ResultSet rs = getMaxUploadIndexStmt.executeQuery();
            if (rs.next()) {
                int maxUploadIndex = rs.getInt("max_index");
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    for (int i = maxUploadIndex; i > maxUploadIndex - uploadIndex && i >= 1; i--) {
                        pstmt.setInt(1, i);
                        try (ResultSet rs_inner = pstmt.executeQuery()) {
                            while (rs_inner.next()) {
                                daten.add(getRow(rs_inner));
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Datenbankfehler/VNetworkVMName - UploadIndex: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("Allg. Fehler VNetworkVMName - UploadIndex: " + e.getMessage());
        }
        return daten;
    }
//    Aufbau der SQL Abfrage
    private String buildQuery(String tableName, boolean useDate) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT vnetworkvmname, vnetworkvisdkserver, vnetworkhost, upload_index, import_date ");
        queryBuilder.append("FROM ( ");
        queryBuilder.append("  SELECT vnetworkvmname, vnetworkvisdkserver, vnetworkhost, upload_index, import_date, COUNT(*) OVER (PARTITION BY vnetworkvmname, upload_index) AS cnt ");
        queryBuilder.append("  FROM ").append(tableName);

        if (useDate) {
            queryBuilder.append(" WHERE import_date = to_date(?, 'YYYY-MM-DD')");
        } else {
            queryBuilder.append(" WHERE upload_index = ?");
        }

        queryBuilder.append(") AS subquery ");
        queryBuilder.append("WHERE cnt > 1 ");
        queryBuilder.append("ORDER BY upload_index DESC, vnetworkvmname ASC");

        return queryBuilder.toString();
    }
    String previousVMName;
    private Map<String, Object> getRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        row.put("vNetworkVMName", rs.getObject("vNetworkVMName"));
        row.put("vNetworkVISDKServer", rs.getObject("vNetworkVISDKServer"));
        row.put("vNetworkHost", rs.getObject("vNetworkHost"));
        row.put("import_date", rs.getObject("import_date"));
        row.put("upload_index", rs.getObject("upload_index"));

        String currentVMName = rs.getString("vNetworkVMName");

        if (!currentVMName.equals(previousVMName)) {
            row.put("isFirstDuplicate", true);
            previousVMName = currentVMName;
        } else {
            row.put("isFirstDuplicate", false);
        }
        return row;
    }

    // Dubletten - Abfrage vNetworkMacAddress | vNetworkVISDKServer | vNetworkHost | upload_index |	import_date
    @GetMapping("/show_vnetworkmacaddress")
    public String macaddr(@RequestParam(required = false) String date, @RequestParam(required = false) Integer uploadIndex, Model model) {
        List<Map<String, Object>> daten = new ArrayList<>();
        String tableName = "vnetwork";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT vnetworkmacaddress, vnetworkvisdkserver, vnetworkhost, upload_index, import_date ");
            queryBuilder.append("FROM ( ");
            queryBuilder.append("  SELECT vnetworkmacaddress, vnetworkvisdkserver, vnetworkhost, upload_index, import_date, COUNT(*) OVER (PARTITION BY vnetworkmacaddress, upload_index) AS cnt ");
            queryBuilder.append("  FROM ").append(tableName);

            if (date != null) {
                queryBuilder.append(" WHERE import_date = to_date(?, 'YYYY-MM-DD')");
            } else {
                queryBuilder.append(" WHERE upload_index = ?");
            }
            queryBuilder.append(") AS subquery ");
            queryBuilder.append("WHERE cnt > 1 ");
            queryBuilder.append("ORDER BY upload_index DESC, vnetworkmacaddress ASC");

            String query = queryBuilder.toString();

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                if (date != null) {
                    pstmt.setString(1, date);
                    try (ResultSet rsmacaddr = pstmt.executeQuery()) {
                        while (rsmacaddr.next()) {
                            daten.add(getRowmac(rsmacaddr));
                        }
                    }
                    if (daten.isEmpty()) {
                        model.addAttribute("errorMessage", message);
                        logger.severe(message);
                    }
                } else {
                    String getMaxUploadIndexQuery = "SELECT MAX(upload_index) AS max_index FROM " + tableName;
                    try (PreparedStatement getMaxUploadIndexStmt = conn.prepareStatement(getMaxUploadIndexQuery)) {
                        ResultSet rsa = getMaxUploadIndexStmt.executeQuery();
                        if (rsa.next()) {
                            int maxUploadIndex = rsa.getInt("max_index");
                            for (int i = maxUploadIndex; i > maxUploadIndex - uploadIndex && i >= 1; i--) {
                                pstmt.setInt(1, i);
                                try (ResultSet rs_innera = pstmt.executeQuery()) {
                                    while (rs_innera.next()) {
                                        daten.add(getRowmac(rs_innera));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Datenbankfehler/VNetworkMacAddress: " + e.getMessage());
            model.addAttribute("errorMessage", "Ein SQL-Fehler ist aufgetreten: " + e.getMessage());
            e.printStackTrace();
        }

        String[] columnOrder = {"vNetworkMacAddress", "vNetworkVISDKServer", "vNetworkHost", "upload_index", "import_date"};
        model.addAttribute("columnOrder", columnOrder);

        model.addAttribute("daten", daten);
        if (date != null) {
            model.addAttribute("date", date);
        } else {
            model.addAttribute("uploadIndex", uploadIndex);
        }
        return "show_vnetworkmacaddress";
    }

    private String previousMac = null;

    private Map<String, Object> getRowmac(ResultSet rsmac) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        row.put("vNetworkMacAddress", rsmac.getObject("vNetworkMacAddress"));
        row.put("vNetworkVISDKServer", rsmac.getObject("vNetworkVISDKServer"));
        row.put("vNetworkHost", rsmac.getObject("vNetworkHost"));
        row.put("import_date", rsmac.getObject("import_date"));
        row.put("upload_index", rsmac.getObject("upload_index"));

        String currentMac = rsmac.getString("vNetworkMacAddress");
        if (!currentMac.equals(previousMac)) {
            row.put("isFirstDuplicate", true);
            previousMac = currentMac;
        } else {
            row.put("isFirstDuplicate", false);
        }
        return row;
    }


    // Double vNetworkIp4Address | vNetworkVISDKServer | vNetworkHost | upload_index |	import_date
    @GetMapping("/show_vnetworkip4address")
    public String ip4address(@RequestParam(required = false) String date, @RequestParam(required = false) Integer uploadIndex, Model model) {
        List<Map<String, Object>> daten = new ArrayList<>();
        String tableName = "vnetwork";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT vnetworkip4address, vnetworkvisdkserver, vnetworkhost, upload_index, import_date ");
            queryBuilder.append("FROM ( ");
            queryBuilder.append("  SELECT vnetworkip4address, vnetworkvisdkserver, vnetworkhost, upload_index, import_date, COUNT(*) OVER (PARTITION BY vnetworkip4address, upload_index) AS cnt ");
            queryBuilder.append("  FROM ").append(tableName);

            if (date != null) {
                queryBuilder.append(" WHERE import_date = to_date(?, 'YYYY-MM-DD')");
            } else {
                queryBuilder.append(" WHERE upload_index = ?");
            }

            queryBuilder.append(") AS subquery ");
            queryBuilder.append("WHERE cnt > 1 ");
            queryBuilder.append("ORDER BY upload_index DESC, vnetworkip4address ASC");

            String query = queryBuilder.toString();

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                if (date != null) {
                    pstmt.setString(1, date);
                    try (ResultSet rsip = pstmt.executeQuery()) {
                        while (rsip.next()) {
                            daten.add(getRowIP(rsip));
                        }
                    }
                    if (daten.isEmpty()) {
                        model.addAttribute("errorMessage", message);
                        logger.severe(message);
                    }
                } else {
                    String getMaxUploadIndexQuery = "SELECT MAX(upload_index) AS max_index FROM " + tableName;
                    try (PreparedStatement getMaxUploadIndexStmt = conn.prepareStatement(getMaxUploadIndexQuery)) {
                        ResultSet rsip = getMaxUploadIndexStmt.executeQuery();
                        if (rsip.next()) {
                            int maxUploadIndex = rsip.getInt("max_index");
                            for (int i = maxUploadIndex; i > maxUploadIndex - uploadIndex && i >= 1; i--) {
                                pstmt.setInt(1, i);
                                try (ResultSet rs_innerip = pstmt.executeQuery()) {
                                    while (rs_innerip.next()) {
                                        daten.add(getRowIP(rs_innerip));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Datenbankfehler/VNetworkIp4Address: " + e.getMessage());
            model.addAttribute("errorMessage", "Ein SQL-Fehler ist aufgetreten: " + e.getMessage());
            e.printStackTrace();
        }

        String[] columnOrder = {"vNetworkIp4Address", "vNetworkVISDKServer", "vNetworkHost", "upload_index", "import_date"};
        model.addAttribute("columnOrder", columnOrder);

        model.addAttribute("daten", daten);

        if (date != null) {
            model.addAttribute("date", date);
        } else {
            model.addAttribute("uploadIndex", uploadIndex);
        }

        return "show_vnetworkip4address";
    }

    private String previousVMIP = null;

    private Map<String, Object> getRowIP(ResultSet rsip) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        row.put("vNetworkIp4Address", rsip.getObject("vNetworkIp4Address"));
        row.put("vNetworkVISDKServer", rsip.getObject("vNetworkVISDKServer"));
        row.put("vNetworkHost", rsip.getObject("vNetworkHost"));
        row.put("import_date", rsip.getObject("import_date"));
        row.put("upload_index", rsip.getObject("upload_index"));

        String currentVMIP = rsip.getString("vNetworkIp4Address");
        if (currentVMIP != null && !currentVMIP.equals(previousVMIP)) {
            row.put("isFirstDuplicate", true);
            previousVMIP = currentVMIP;
        } else {
            row.put("isFirstDuplicate", false);
        }
        return row;
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
                    } else {
                        queryBuilder.append("upload_index = ? AND ");
                    }
                    queryBuilder.append("vNetworkAdapter IS NOT NULL AND vNetworkAdapter != '' AND vNetworkAdapter != ? ");
                    queryBuilder.append("ORDER BY upload_index DESC");

                    try (PreparedStatement pstmt = conn.prepareStatement(queryBuilder.toString())) {
                        if (date != null) {
                            pstmt.setString(1, date);
                            pstmt.setString(2, excludedNetworkAdapter);
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
                        model.addAttribute("errorMessage", message);
                        logger.severe(message);
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
        } else {
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
                    StringBuilder queryBuilder = new StringBuilder("SELECT vSnapshotVMName, vSnapshotVISDKServer, vSnapshotHost, import_date, upload_index FROM ").append(tableName);

                    if (date != null) {
                        queryBuilder.append(" WHERE import_date = to_date(?, 'YYYY-MM-DD') AND upload_index = (SELECT MAX(upload_index) FROM ")
                                .append(tableName)
                                .append(" WHERE import_date = to_date(?, 'YYYY-MM-DD'))");
                    } else {
                        queryBuilder.append(" WHERE upload_index = ? ");
                    }
                    queryBuilder.append(" ORDER BY ");
                    if (date != null) {
                        queryBuilder.append("upload_index DESC");
                    } else {
                        queryBuilder.append("vSnapshotVMName, vSnapshotVISDKServer, vSnapshotHost, import_date");
                    }

                    try (PreparedStatement pstmt = conn.prepareStatement(queryBuilder.toString())) {
                        if (date != null) {
                            pstmt.setString(1, date);
                            pstmt.setString(2, date);
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
                        model.addAttribute("errorMessage", message);
                        logger.severe(message);
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
        } else {
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