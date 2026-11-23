package dk.ek.gruppe2.storycontent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.ek.gruppe2.storycontent.domain.Quest;

public interface QuestRepository extends JpaRepository<Quest, Integer> {

    List<Quest> findAllBySceneId(Integer sceneId);
}
