
# RPG Choose Your Fate SQ

To run the SQL containers detached:

# Database exam project(Choose your fate)

## Running the databases

Start the database containers in detached mode:
```bash
docker compose up -d
```

### running multiple microservices
To run multiple microservices in docker we are using the -f system, where we run multiple segmented docker compose files from one command.

make sure that things that are reused between different microservices are added to the docker compose file in the root!!!
where as things that are only used by your microservice are added to the microservices docker compose file.

If you need a clean MySQL reseed, recreate the Docker volumes:

All our SQL scripts are in rpg_mysql folder

MySQL seed data is available in `rpg_mysql/06_seed_data.sql`.
The Software Quality version uses one primary MySQL container on port `3307` and one secondary MySQL container on port `3308`.

The availability design is intentionally scoped:

- primary SQL is the normal active database
- secondary SQL is the failover database
- successful primary writes create application-level replication jobs
- replication is asynchronous, so eventual consistency is accepted
- the primary health monitor checks primary on a fixed interval
- emergency failover tries to drain queued replication jobs before switching to secondary
- manual failover requires the replication queue to be empty
- failback to primary is manual and should happen during a maintenance window

Availability status endpoints are exposed under:

```text
GET  /availability/status
POST /availability/failover
POST /availability/failback/begin
POST /availability/failback/complete
```

If the MySQL Docker volumes already exist, reload the schema and seed data with:


## Dump files
```bash
docker compose down -v
docker compose up -d
```
## Running the backend
With the database containers running, start the Spring Boot application locally from the backend project.
Swagger UI is available at:
[http://localhost:8080/swagger-ui/index.html#/](http://localhost:8080/swagger-ui/index.html#/)
Deployed Swagger UI is available at:
https://rpg-choose-your-fate.onrender.com/swagger-ui/index.html#/

Database dump files should be placed in the [database_backups](C:\Users\pf\Desktop\Skole\Databases\projects\mandetory\RPG_Choose_Your_Fate\database_backups) folder.

- MongoDB dump path: [database_backups/mongo](C:\Users\pf\Desktop\Skole\Databases\projects\mandetory\RPG_Choose_Your_Fate\database_backups\mongo)
- Neo4j dump path: [database_backups/neo4j](C:\Users\pf\Desktop\Skole\Databases\projects\mandetory\RPG_Choose_Your_Fate\database_backups\neo4j)

How ever we do not use them. We run the migration process to populate mongoDB and neo4j
with data. The files are only include because they are part the required deliverables.

## SQL scripts

All SQL scripts are located in the [rpg_mysql](C:\Users\pf\Desktop\Skole\Databases\projects\mandetory\RPG_Choose_Your_Fate\rpg_mysql) folder.

The local Docker MySQL setup runs the scripts in this order:

1. `01_create_schema.sql`
2. `02_procedures.sql`
3. `03_functions.sql`
4. `04_triggers.sql`
5. `05_seed_data.sql`
6. `06_seed_mock_data.sql`
7. `07_seed_scene_choice_expansion.sql`
8. `08_security_roles.sql`
9. `09_events.sql`
