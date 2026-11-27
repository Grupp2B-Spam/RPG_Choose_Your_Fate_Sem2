package dk.ek.gruppe2.storycontent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dk.ek.gruppe2.storycontent.domain.Item;

public interface ItemRepository extends JpaRepository<Item, Integer> {

    /** See SceneRepository.incrementVersion for why this is a statement, not a setter. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Item i set i.version = i.version + 1 where i.id = :id")
    int incrementVersion(@Param("id") Integer id);
}
