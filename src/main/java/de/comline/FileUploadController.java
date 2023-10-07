package de.comline;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.SQLException;

@RestController
public class FileUploadController {

    @Autowired
    private XlsxToDatabase xlsxToDatabase;

    @PostMapping("/excel-upload")
    public void handleFileUpload(@RequestParam("file") MultipartFile file) throws IOException{
        xlsxToDatabase.importDataFromExcel(file);
    }
}
