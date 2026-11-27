package dk.chooseyourfate.manager;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SceneRepository extends MongoRepository<SceneDocument, String> {}
