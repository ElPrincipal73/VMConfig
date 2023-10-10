package de.comline.service;

import de.comline.AppLogger;
import de.comline.Model.VNetworkModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.Arrays;

import static de.comline.Model.VNetworkModel.COMMON_COLUMN_ORDER;

@Service
public class VNetworkService {
    private static final Logger logger = AppLogger.getLogger(Controller.class.getName());
    @Autowired
    private VNetworkModel vNetworkModel;

    public List<Map<String, Object>> fetchData(String column, String date, Integer uploadIndex) {
        return vNetworkModel.fetchDataFromDB(column, date, uploadIndex);
    }

    public Model prepareModel(Model model, List<Map<String, Object>> daten, String column, String date, Integer uploadIndex) {
        markFirstDuplicates(daten, column);
        handleData(daten, model);
        addAttributesToModel(daten, model, column, date, uploadIndex);
        return model;
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
}
