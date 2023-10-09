package de.comline.Controllers;

import java.util.List;
import java.util.logging.Logger;
import de.comline.Models.VSnapshot;

import de.comline.service.VSnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class VSnapshotController {

    @Autowired
    private VSnapshotService vSnapshotService;

    private static final Logger logger = Logger.getLogger(VSnapshotController.class.getName());

    @GetMapping("/show_all_vSnapshot")
    public String vnetworkShowAll(@RequestParam(required = false) String date,
                                  @RequestParam(required = false) Integer uploadIndex,
                                  Model model) {
        List<de.comline.Models.VSnapshot> daten = vSnapshotService.getAllSnapshots(date, uploadIndex);

        if (daten.isEmpty()) {
            model.addAttribute("errorMessage", "Keine Daten für den ausgewählten Zeitraum gefunden");
            logger.severe("Keine Daten für den ausgewählten Zeitraum gefunden");
        }

        populateModelAttributes(model, daten, date, uploadIndex);

        return "show_all_vSnapshot";
    }

    private void populateModelAttributes(Model model, List<VSnapshot> daten, String date, Integer uploadIndex) {
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
