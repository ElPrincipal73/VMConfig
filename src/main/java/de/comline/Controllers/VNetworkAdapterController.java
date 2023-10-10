package de.comline.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.logging.Logger;

@org.springframework.stereotype.Controller
public class VNetworkAdapterController {

    private static final Logger logger = Logger.getLogger(VNetworkAdapterController.class.getName());

    @Autowired
    private de.comline.VNetworkAdapterService vNetworkAdapterService;

    @GetMapping("/show_vnetworkadapter")
    public String networkAdapter(@RequestParam(required = false) String date,
                                 @RequestParam(required = false) Integer uploadIndex,
                                 Model model) {
        List<de.comline.VNetworkAdapterModel> daten = vNetworkAdapterService.fetchVNetworkAdapterData(date, uploadIndex);

        if (daten.isEmpty()) {
            model.addAttribute("errorMessage", "Keine Daten für den ausgewählten Zeitraum gefunden");
            logger.severe("Keine Daten für den ausgewählten Zeitraum gefunden");
        }

        String[] columnOrder = {"vNetworkAdapter", "vNetworkVISDKServer", "vNetworkHost", "upload_index", "import_date"};
        model.addAttribute("columnOrder", columnOrder);
        model.addAttribute("daten", daten);
        model.addAttribute("date", date);
        model.addAttribute("uploadIndex", uploadIndex);

        return "show_vnetworkadapter";
    }
}
