const scenes = db.getSiblingDB("scenes");
scenes.createUser({
  user: "indexer",
  pwd: "indexer-local-only",
  roles: [{ role: "readWrite", db: "scenes" }]
});
scenes.createUser({
  user: "manager",
  pwd: "manager-local-only",
  roles: [{ role: "read", db: "scenes" }]
});
