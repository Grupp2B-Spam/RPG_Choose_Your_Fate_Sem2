package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Chapter;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterRepository extends JpaRepository<Chapter, Integer> {
}
