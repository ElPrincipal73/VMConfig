package de.comline;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

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

    private int maxUploadIndex = -1;  // Klassenvariable warum auf -1 ????????????????
    private static final String[] COMMON_COLUMN_ORDER = {"vnetworkvisdkserver", "vnetworkhost", "upload_index", "import_date"};
    private static final Logger logger = AppLogger.getLogger(Controller.class.getName());

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
                                   @RequestParam String column, Model model) {
        List<Map<String, Object>> daten = fetchDataFromDB(column, date, uploadIndex);
        markFirstDuplicates(daten, column); // Neue Methode zum Markieren der ersten  vorkommenden Duplikate
        handleData(daten, model);
        addAttributesToModel(daten, model, column, date, uploadIndex);
        return "show_vnetworkinfo";
    }

    private void markFirstDuplicates(List<Map<String, Object>> daten, String columnKey) {
        Set<Object> seen = new HashSet<>();
        for (Map<String, Object> datenItem : daten) {
            Object key = datenItem.get(columnKey); // Das Feld, anhand dessen Duplikate erkannt werden
            if (seen.contains(key)) {
                datenItem.put("isFirstDuplicate", false);
            } else {
                seen.add(key);
                datenItem.put("isFirstDuplicate", true);
            }
        }
    }

//
    private List<Map<String, Object>> fetchDataFromDB(String column, String date, Integer uploadIndex) {
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

    private void handleData(List<Map<String, Object>> daten, Model model) {
        if (daten.isEmpty()) {
            model.addAttribute("errorMessage", "Keine Daten für den ausgewählten Zeitraum gefunden");
            logger.info("Keine Daten für den Zeitraum gefunden");
        }
    }

    private void addAttributesToModel(List<Map<String, Object>> daten, Model model, String column, String date, Integer uploadIndex) {
        String[] columnOrder = Stream.concat(Stream.of(column), Arrays.stream(COMMON_COLUMN_ORDER)).toArray(String[]::new);
        model.addAttribute("columnOrder", columnOrder);
        model.addAttribute("daten", daten);

        if (date != null) {
            model.addAttribute("date", date);
        } else {
            model.addAttribute("uploadIndex", uploadIndex);
        }
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


    // Wert in der config.properties Datei setzen  -->  vNetworkAdapter | vNetworkVISDKServer | vNetworkHost | upload_index |import_date
    @GetMapping("/show_vnetworkadapter")
    public String networkAdapter(@RequestParam(required = false) String date,
                                 @RequestParam(required = false) Integer uploadIndex,
                                 Model model) {

        Configuartion config = new Configuartion();
        String excludedNetworkAdapter = config.getExcludedNetworkAdapter();
        List<Map<String, Object>> daten = new ArrayList<>();
        String tableName = "vnetwork";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            int maxUploadIndex = getMaxUploadIndex(conn, tableName, date);
            StringBuilder queryBuilder = buildNetworkAdapterQuery(tableName, date, uploadIndex);

            try (PreparedStatement pstmt = conn.prepareStatement(queryBuilder.toString())) {
                setNetworkAdapterQueryParameters(pstmt, maxUploadIndex, date, uploadIndex, excludedNetworkAdapter);

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

    private void setNetworkAdapterQueryParameters(PreparedStatement pstmt, int maxUploadIndex, String date, Integer uploadIndex, String excludedNetworkAdapter) throws SQLException {
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
    public String vnetworkShowAll(@RequestParam(required = false) String date,
                                  @RequestParam(required = false) Integer uploadIndex,
                                  Model model) {

        List<Map<String, Object>> daten = new ArrayList<>();
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

            if (daten.isEmpty()) {
                model.addAttribute("errorMessage", "Keine Daten gefunden");
                logger.severe("Keine Daten gefunden");
            }

        } catch (SQLException e) {
            logger.severe("Datenbankfehler/AllSnapshot: " + e.getMessage());
            model.addAttribute("errorMessage", "Ein SQL-Fehler ist aufgetreten: " + e.getMessage());
            e.printStackTrace();
        }

        populateModelAttributes(model, daten, date, uploadIndex);
        return "show_all_vSnapshot";
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

    private Map<String, Object> getRowSnapAll(ResultSet rsShowAll) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        row.put("vSnapshotVMName", rsShowAll.getObject("vSnapshotVMName"));
        row.put("vSnapshotVISDKServer", rsShowAll.getObject("vSnapshotVISDKServer"));
        row.put("vSnapshotHost", rsShowAll.getObject("vSnapshotHost"));
        row.put("import_date", rsShowAll.getObject("import_date"));
        row.put("upload_index", rsShowAll.getObject("upload_index"));
        return row;
    }

    private void populateModelAttributes(Model model, List<Map<String, Object>> daten, String date, Integer uploadIndex) {
        String[] columnOrder = {"vSnapshotVMName", "vSnapshotVISDKServer", "vSnapshotHost", "upload_index", "import_date"};
        model.addAttribute("columnOrder", columnOrder);
        model.addAttribute("daten", daten);

        if (date != null) {
            model.addAttribute("date", date);
        } else if (uploadIndex != null) {
            model.addAttribute("uploadIndex", uploadIndex);
        }
    }
}