# Step-by-Step Instructions

Please perform each step when instructed.  Only perform one step at a time.

Within these instructions, use the following table to map between table names, resource names, and class names.

| table name | resource name | class name |
| --- | --- | --- |
| trade_order | tradeOrder | TradeOrder |
| execution | execution | Execution |
| blotter | blotter | Blotter |
| trade_type | tradeType | TradeType |
| execution_status | executionStatus | ExecutionStatus |
| destination | destination | Destination |
---
<br>

Log each step in @cursor-log.md.  Follow the instructions at the top of the file. 
PLEASE REMEMBER: When logging to cursor-log.md, append each entry beneath the prior entry.  Do not delete or replace any prior entries.

## Steps

1. Configure the project to connect to the PostgreSQL database on host `globeco-trade-service-postgresql`  port 32800 and database `postgres`.  The user is  "postgres".  No password is required. Please add an entry with this prompt and your actions in the cursor-log.md  file following the instructions in the file.  Do not delete or replace anything in cursor-log.md
2. Configure Flyway with the same configuration as in step 1.  Please add an entry with this prompt and your actions in the cursor-log.md  file following the instructions in the file.
3. Create a Flyway migration to deploy the schema for this project.  The schema is in @trade-service.sql in the project root.   Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
4. Create a Flyway migration for the blotter data in #### Initialization Data for `blotter`.   Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
5. Create a Flyway migration for the blotter data in #### Initialization Data for `trade_type`.   Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
6. Create a Flyway migration for the blotter data in #### Initialization Data for `destination`.   Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
7. Create a Flyway migration for the blotter data in #### Initialization Data for `execution_status`.   Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.

8. Security data comes from the security service.  The security service API is implemented in documentation/security-service-openapi.yaml.  Please generate the entity, service interface, and service implementation to call the security service to GET securities and security types.  Do not implement POST, PUT, or DELETE.  Please implement caching for the security data to avoid excessive calls to the security service following the requirements in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.


9. Please implement the entity, repository, service interface, and service implementation for **blotter** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
10. Please implement the unit tests for the entity, repository, service interface, and service implementation for **blotter**  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
11. Please impelement caching for **blotter** using the requirements in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
12. Please implement unit testing for **blotter** caching.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
13. Please implement the APIs for **blotter** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
14. Please generate the unit tests for the **blotter** APIs.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
15. Please update the README.md file with an introduction and full documentation of the **blotter** data model and API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
16. Please create an OpenAPI schema `openapi.yaml' in the project root.  Please include the full specification for the **blotter** API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.

17. Please implement the entity, repository, service interface, and service implementation for **tradeOrder** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
18. Please implement the unit tests for the entity, repository, service interface, and service implementation for **tradeOrder**  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
19. Please impelement caching for **tradeOrder** using the requirements in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
20. Please implement unit testing for **tradeOrder** caching.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
21. Please implement the APIs for **tradeOrder** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
22. Please generate the unit tests for the **tradeOrder** APIs.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
23. Please update the README.md file by adding full documentation of the **tradeOrder** data model and API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
24. Please update the OpenAPI schema @openapi.yaml in the project root.  Please add the full specification for the **tradeOrder** API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.


25. Please implement the entity, repository, service interface, and service implementation for **execution** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
26. Please implement the unit tests for the entity, repository, service interface, and service implementation for **execution**  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
27. Please implement unit testing for **execution** caching.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
28. Please implement the APIs for **execution** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
29. Please generate the unit tests for the **execution** APIs.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
30. Please update the README.md file by adding full documentation of the **execution** data model and API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
31. Please update the OpenAPI schema @openapi.yaml in the project root.  Please add the full specification for the **execution** API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.


32. Please implement the entity, repository, service interface, and service implementation for **destination** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
33. Please implement the unit tests for the entity, repository, service interface, and service implementation for **destination**  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
34. Please impelement caching for **destination** using the requirements in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
35. Please implement unit testing for **destination** caching.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
36. Please implement the APIs for **destination** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
37. Please generate the unit tests for the **destination** APIs.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
38. Please update the README.md file by adding full documentation of the **destination** data model and API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
39. Please update the OpenAPI schema @openapi.yaml in the project root.  Please add the full specification for the **destination** API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.


40. Please implement the entity, repository, service interface, and service implementation for **tradeType** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
41. Please implement the unit tests for the entity, repository, service interface, and service implementation for **tradeType**  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
42. Please impelement caching for **tradeType** using the requirements in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
43. Please implement unit testing for **tradeType** caching.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
44. Please implement the APIs for **tradeType** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
45. Please generate the unit tests for the **tradeType** APIs.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
46. Please update the README.md file by adding full documentation of the **tradeType** data model and API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
47. Please update the OpenAPI schema @openapi.yaml in the project root.  Please add the full specification for the **tradeType** API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.


48. Please implement the entity, repository, service interface, and service implementation for **executionStatus** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
49. Please implement the unit tests for the entity, repository, service interface, and service implementation for **executionStatus**  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
50. Please impelement caching for **executionStatus** using the requirements in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
51. Please implement unit testing for **executionStatus** caching.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
52. Please implement the APIs for **executionStatus** using the requirements provided in @requirements.md.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
53. Please generate the unit tests for the **executionStatus** APIs.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
54. Please update the README.md file by adding full documentation of the **executionStatus** data model and API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.
55. Please update the OpenAPI schema @openapi.yaml in the project root.  Please add the full specification for the **executionStatus** API.  Please add an entry with this prompt and your actions in the cursor-log.md file following the instructions in the file.



56. Please compare the README.md file to the code to verify that the README is completely consistent with the code.  If it is not, please make the required updates. Please add an entry with this prompt and your actions to the end of the cursor-log.md file following the instructions in the file.
57. Please compare the openapi.yaml file to the code to verify that the openapi spec is completely consistent with the code.  If it is not, please make the required updates. Please add an entry with this prompt and your actions to the end of the cursor-log.md file following the instructions in the file.
58. Please create a Dockerfile for this application.  Please add an entry with this prompt and your actions to the end of the cursor-log.md file following the instructions in the file.
59. We will be deploying this service to Kubernetes.  Please implement liveness, readiness, and startup health checks.  Please update the README.md file and openapi.yaml spec with the health check APIs.  Please be sure the URLs in the spec match the URLs in the API. Please add an entry with this prompt and your actions to the end of the cursor-log.md file following the instructions in the file.
60. Please create all the files necessary to deploy to this application as a service to Kubernetes.  Please include the liveness, readiness, and startup probes you just created.  The deployment should start with one instance of the service and should scale up to a maximum of 100 instances.  It should have up 100 millicores and 200 MiB of memory.  The liveness probe should have a timeout (`timeoutSeconds`) of 240 seconds.  The name of the service is `globeco-trade-service` in the `globeco` namespace.  You do not need to create the namespace. Please add an entry with this prompt and your actions to the end of the cursor-log.md file following the instructions in the file.
61. Please expose the OpenAPI schema as an endpoint using Springdoc OpenAPI.  Please add an entry with this prompt and your actions to the end of the cursor-log.md file following the instructions in the file.

I