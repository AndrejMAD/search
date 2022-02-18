package test;

import main.service.ScannerService;

import java.io.IOException;

public class TestSiteScannerService {

    public static void main(String[] args) throws IOException {
        ScannerService scanner = new ScannerService();
        scanner.scanSiteAll();

        System.out.println("end");
    }
}