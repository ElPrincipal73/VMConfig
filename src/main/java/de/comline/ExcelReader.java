package de.comline;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

@Component
public class ExcelReader {

    private static final Logger logger = Logger.getLogger(ExcelReader.class.getName());

    public List<String> extractColumnNames(Sheet sheet) throws IOException {
        List<String> columnNames = new ArrayList<>();
        Row firstRow = sheet.getRow(0);
        Iterator<Cell> cellIterator = firstRow.cellIterator();

        if (firstRow == null || firstRow.getLastCellNum() <= 0) {
            logger.severe("Keine Spaltenüberschriften gefunden");
            throw new IOException("Keine Spaltenüberschriften gefunden.");
        }

        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            String cellValue = cell.toString().toLowerCase().trim();

            if (cellValue.isEmpty()) {
                logger.severe("Leere Spaltenüberschrift gefunden");
                throw new IOException("Leere Spaltenüberschrift gefunden.");
            }

            columnNames.add(cellValue);
        }

        return columnNames;
    }

    public List<List<String>> extractRows(Sheet sheet, int numColumns) {
        List<List<String>> rows = new ArrayList<>();
        Iterator<Row> rowIterator = sheet.rowIterator();
        rowIterator.next(); // Überspringe die erste Zeile

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
}
