package main.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO Lemma(site_id, lemma, frequency) VALUES(:site_id, :lemma, 1) ON DUPLICATE KEY UPDATE frequency = frequency + 1", nativeQuery = true)
    void incrementFrequency(@Param("site_id") int site_id, @Param("lemma") String lemma);

    Lemma findBySiteAndLemma(Site site, String lemma);

    List<Lemma> findBySiteAndLemmaInOrderByFrequencyAsc(Site site, Collection<String> lemmaList);

    long countBySite(Site site);
}
