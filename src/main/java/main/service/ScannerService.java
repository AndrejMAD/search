package main.service;

import main.ApplicationConfig;
import main.entity.*;
import main.repository.*;
import main.util.Morph;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RejectedExecutionException;

@Service
public class ScannerService {

    public static Map<Integer, Set<String>> SCANNING_SITES = new HashMap<>();
    private static final int TIME_SLEEP_MIN = 500;
    private static final int TIME_SLEEP_MAX = 5000;

    private Morph morph;
    private static ForkJoinPool threadPool;

    private ApplicationConfig config;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private FieldRepository fieldRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;

    @Autowired
    public ScannerService(ApplicationConfig config, SiteRepository siteRepository, PageRepository pageRepository,
                          FieldRepository fieldRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) throws IOException {
        morph = new Morph();
        threadPool = new ForkJoinPool();
        this.config = config;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.fieldRepository = fieldRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    public boolean isIndexing() {
        return threadPool.getActiveThreadCount() > 0;
    }

    public void stop() {
        threadPool.shutdownNow();
    }

    public void scanPage(String link) throws MalformedURLException {
        new URL(link);

        boolean pageScanning = false;
        Map<String, String> sites = config.getSites();
        for (Map.Entry<String, String> siteEntry : sites.entrySet()) {

            String siteName = siteEntry.getKey();
            String siteUrl = siteEntry.getValue();

            if (link.contains(siteUrl)) {
                Site site = siteRepository.findByName(siteName);

                if (site == null) {
                    site = new Site(siteName, siteUrl);
                    site.setStatus(Site.Status.INDEXING);
                    siteRepository.save(site);
                }

                PageScanner pageScanner = new PageScanner(site, link, true);
                pageScanner.fork();
                pageScanning = true;
            }
        }

        if (!pageScanning) {
            throw new IllegalArgumentException();
        }
    }

    public void scanSiteAll() throws IOException {
        Map<String, String> sites = config.getSites();

        for (Map.Entry<String, String> siteEntry : sites.entrySet()) {
            SiteScanner siteScanner = new SiteScanner(siteEntry.getKey(), siteEntry.getValue());
            try {
                threadPool.execute(siteScanner);
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
                threadPool = new ForkJoinPool();
                threadPool.execute(siteScanner);
            }
        }
    }

    private class SiteScanner extends RecursiveAction {

        private String name;
        private String url;

        public SiteScanner(String name, String url) {
            this.name = name;
            this.url = url;
        }

        @Override
        protected void compute() {
            try {
                Site site = siteRepository.findByName(name);

                if (site != null) {
                    siteRepository.delete(site);
                }

                site = new Site(name, url);

                site.setStatus(Site.Status.INDEXING);
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);

                SCANNING_SITES.put(site.getId(), new HashSet<>());

                PageScanner pageScanner = new PageScanner(site, site.getUrl());
                pageScanner.compute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class PageScanner extends RecursiveAction {

        private Site site;
        private String url;
        private String path;
        private boolean singleton;

        public PageScanner(Site site, String url) throws MalformedURLException {
            URL urlObj = new URL(url);
            this.site = site;
            this.url = url;
            path = urlObj.getPath().isEmpty() ? "/" : urlObj.getPath();
        }

        public PageScanner(Site site, String url, boolean singleton) throws MalformedURLException {
            this(site, url);
            this.singleton = singleton;
        }

        private static synchronized boolean isScanning(Site site, String path) {
            if (SCANNING_SITES.get(site.getId()).contains(path)) {
                System.out.println(path + ": is scanning.");
                return true;
            }

            SCANNING_SITES.get(site.getId()).add(path);
            System.out.println(SCANNING_SITES.get(site.getId()).size() + ": " + path);

            return false;
        }

        public Set<String> scanPage() throws IOException {
            Connection connection = Jsoup
                    .connect(url)
                    .ignoreContentType(true)
                    .userAgent(config.getUserAgent())
                    .referrer(config.getReferrer());

            Document doc = connection.get();
            Page page = pageRepository.findBySiteAndPath(site, path);

            if (page == null) {
                page = new Page();
            }

            page.setSite(site);
            page.setPath(path);
            page.setCode(connection.response().statusCode());
            page.setContent(doc.html());

            page = pageRepository.save(page);

            if (page.getCode() == 200) {
                indexingPage(site, page);
            }

            Set<String> links = new HashSet<>();
            Elements htmlLinks = doc.select("a[abs:href^=" + site.getUrl() + "]");
            htmlLinks.forEach(l -> links.add(l.attr("abs:href")));

            return links;
        }

        private void indexingPage(Site site, Page page) {
            Map<String, Double> pagedRankCount = new HashMap<>();

            Document doc = Jsoup.parse(page.getContent());

            List<Field> fieldList = fieldRepository.findAll();
            for (Field field : fieldList) {
                Elements select = doc.select(field.getSelector());
                String text = select.text();

                Map<String, Integer> fieldLemmaCount = morph.lemmaCount(text);
                fieldLemmaCount.forEach((k, v) -> {
                    double rank = v * field.getWeight();
                    pagedRankCount.put(k, pagedRankCount.getOrDefault(k, 0.0) + rank);
                });
            }

            for (Map.Entry<String, Double> e : pagedRankCount.entrySet()) {

                if (page.getId() != 0) {
                    lemmaRepository.incrementFrequency(site.getId(), e.getKey());
                }

                Lemma lemma = lemmaRepository.findBySiteAndLemma(site, e.getKey());
                indexRepository.updateRankByPageIdAndLemmaId(page.getId(), lemma.getId(), e.getValue());
            }
        }

        @Override
        protected void compute() {
            if (isScanning(site, path)) {
                return;
            }
            try {
                if (singleton) {
                    scanPage();
                    site.setStatus(Site.Status.INDEXED);
                    return;
                }

                Thread.sleep(getTimeSleep());

                List<PageScanner> pageScannerList = new ArrayList<>();
                Set<String> links = scanPage();
                for (String link : links) {
                    if (isValidLink()) {
                        PageScanner scanner = new PageScanner(site, link);
                        pageScannerList.add(scanner);
                        scanner.fork();
                    }
                }

                for (PageScanner scanner : pageScannerList) {
                    scanner.join();
                }

                site.setStatus(Site.Status.INDEXED);

            } catch (InterruptedException | CancellationException e) {
                e.printStackTrace();
                site.setLastError("Индексация была остановлена.");
                site.setStatus(Site.Status.FAILED);
            } catch (Exception e) {
                e.printStackTrace();
                site.setLastError(e.getLocalizedMessage());
                site.setStatus(Site.Status.FAILED);
            } finally {
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);
            }
        }

        private long getTimeSleep() {
            long timeSleep = (long) (Math.random() * (TIME_SLEEP_MAX - TIME_SLEEP_MIN)) + TIME_SLEEP_MIN;
            return timeSleep;
        }

        public boolean isValidLink() {
            if (!url.contains(site.getUrl())) {
                return false;
            }

            if (url.contains("#")) {
                return false;
            }

            return true;
        }
    }
}