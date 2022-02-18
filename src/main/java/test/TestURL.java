package test;

import java.net.MalformedURLException;
import java.net.URL;

public class TestURL {

    public static void main(String[] args) {
        try {
            URL url = new URL("https://www.wildberries.ru/catalog/21621200/detail.aspx?targetUrl=GP&fromAd=true");
            System.out.println(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
