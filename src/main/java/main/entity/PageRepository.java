package main.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    Page findBySiteAndPath(Site site, String path);

    long countBySite(Site site);
}
