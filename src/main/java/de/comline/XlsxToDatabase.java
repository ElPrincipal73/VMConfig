package de.comline;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.IOException;
import java.sql.Connection;
import java.util.List;
import java.util.logging.Logger;

@Component
public class XlsxToDatabase {

    @Autowired
    private ExcelReader excelReader;

    @Autowired
    private DatabaseHandler databaseHandler;
    @Autowired
    private DatabaseConfig databaseConfig;

    private static final Logger logger = Logger.getLogger(XlsxToDatabase.class.getName());

    public void importDataFromExcel(MultipartFile file) throws IOException {
        try (Connection conn = databaseHandler.connectToDatabase()) {
            Workbook workbook = new XSSFWorkbook(file.getInputStream());
            var sheetIterator = workbook.sheetIterator();
            while (sheetIterator.hasNext()) {
                Sheet sheet = sheetIterator.next();
                List<String> columnNames = excelReader.extractColumnNames(sheet);
                List<List<String>> rows = excelReader.extractRows(sheet, columnNames.size());
                databaseHandler.createTableAndImportData(conn, sheet.getSheetName().toLowerCase(), columnNames, rows);
            }
        } catch (Exception e) {
            logger.severe("Fehler beim Importieren der Daten aus der Excel-Datei.");
            throw new IOException("Fehler beim Importieren der Daten aus der Excel-Datei.");
        }
    }
}
