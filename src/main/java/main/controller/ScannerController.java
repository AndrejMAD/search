package main.controller;

import main.service.ScannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScannerController {

    @Autowired
    private ScannerService scannerService;

    @GetMapping("/scan")
    public void scan() {
//        scannerService.scan();
    }

    @GetMapping("/scan/count")
    public int scanCount() {
        return 0;
    }
}