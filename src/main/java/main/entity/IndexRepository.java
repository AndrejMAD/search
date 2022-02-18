package main.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO indexes(page_id, lemma_id, ranks) VALUES(:page_id, :lemma_id, :rank) ON DUPLICATE KEY UPDATE ranks = :rank", nativeQuery = true)
    void updateRankByPageIdAndLemmaId(@Param("page_id") int pageId, @Param("lemma_id") int lemmaId, @Param("rank") double rank);

    @Query("SELECT i.page FROM Index i WHERE i.lemma = :lemma")
    List<Page> findPageByLemma(@Param("lemma") Lemma lemma);

    @Query("SELECT i.page FROM Index i WHERE i.lemma = :lemma AND i.page IN :page_list")
    List<Page> findPageByLemmaAndPageIn(@Param("lemma") Lemma lemma, @Param("page_list") List<Page> pageList);

    @Query("SELECT SUM(i.rank) FROM Index i WHERE i.page = :page AND i.lemma IN :lemma_list GROUP BY page")
    Double sumRankByPageAndLemmaIn(@Param("page") Page page, @Param("lemma_list") List<Lemma> lemmaList);
}
