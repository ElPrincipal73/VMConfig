package de.comline.Controllers;

import de.comline.service.VNetworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;


@Controller
public class VNetworkController {
    @Autowired
    private VNetworkService vNetworkService;

    @GetMapping("/show_vnetworkinfo")
    public String showVNetworkInfo(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer uploadIndex,
            @RequestParam String column,
            Model model
    ) {
        List<Map<String, Object>> daten = vNetworkService.fetchData(column, date, uploadIndex);
        model = vNetworkService.prepareModel(model, daten, column, date, uploadIndex);
        return "show_vnetworkinfo";
    }
}

