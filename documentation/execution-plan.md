# Step-by-Step Instructions

Please perform each step when instructed.  Only perform one step at a time.

Within these instructions, use the following table to map between table names, resource names, and class names.

| table name | resource name | class name |
| --- | --- | --- |
| price | price | Price |
---

Log each step in @cursor-log.md.  Follow the instructions at the top of the file. 
PLEASE REMEMBER: When logging to cursor-log.md, append each entry beneath the prior entry.  Do not delete or replace any prior entries.

## Steps

1. Configure the project to connect to the PostgreSQL database on host `globeco-pricing-service-postgresql`  port 5435 and database `postgres`.  The user is  "postgres".  
2. Configure Flyway with the same configuration as in step 1.  
3. Configure the project to use PostgreSQL test containers
4. Create a Flyway migration to deploy the schema for this project.  The schema is in @pricing-service.sql in the project root.  
5. Create a Java Flyway migration for the pricing data following the detailed instructions in @requirements.md in ## Data Migrations section.
6. Please implement the entity, repository, service interface, and service implementation for **price** using the requirements provided in @requirements.md.  
7. Please implement the unit tests for the entity, repository, service interface, and service implementation for **price**.  Please use test containers.  
8. Please impelement caching for **price** using the requirements in @requirements.md. 
9. Please implement unit testing for **price** caching.  
10. Implement the DTO in the ## DTO section of @requirements.md.
11. Implement the APIs in the ## APIs section of @requirements.md.  Please note the specialized logic.  This is not a standard CRUD API.
12. We will be deploying this service to Kubernetes.  Please implement liveness, readiness, and startup health checks.  
13. Please document the service completely in README.md.
14. Please create a Dockerfile for this application.  
15. Please create all the files necessary to deploy to this application as a service to Kubernetes.  Please include the liveness, readiness, and startup probes you just created.  The deployment should start with one instance of the service and should scale up to a maximum of 100 instances.  It should have up 100 millicores and 200 MiB of memory.  The liveness probe should have a timeout (`timeoutSeconds`) of 240 seconds.  The name of the service is `globeco-pricing-service` in the `globeco` namespace.  You do not need to create the namespace.
16. Please expose the OpenAPI schema as an endpoint using Springdoc OpenAPI. 
17. Please add the URLS for the OpenAPI schema to the README.md file

