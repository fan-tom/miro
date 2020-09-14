# Miro test task
This project is test task of Miro company

It implements spring-boot based web-server, that stores rectangular widgets.

Each widget has a unique id and zIndex properties, as well as bottom-left vertex coordinates (x,y), width, height
and updatedAt date.

## Modules
The project consists of four submodules
- **domain**  
  this module has Widget class and is used by all other modules
- **service**  
  this module defines widget repository interface and base test class with tests for implementations.
  Also it implements service class which contains a widget CRUD logic.
- **inmemory_repository**  
  this module contains in-memory implementation of widget repository
- **web**  
  this module has web-server implementation, that uses services class defined in 'service' module,
  implements widget repository on top of h2 database and can switch between in-memory and db repository
  implementations without code change

## Setup
`$ ./mvnw clean`  
`$ ./mvnw install`  
`$ ./mvnw -pl web spring-boot:run`  
These commands will build, test and install packages from submodules and then start web-service,
accessible on `http://localhost:8080/`

## Configuration
Change `web/config/application.yml` to choose between db and in-memory repository implementations in different profiles.
If you choose in-memory implementation, you also can switch id type: string or integer.

## Requests
`$ curl -X GET http://localhost:8080/widgets`  
get all widgets, sorted by zIndex ascending

`$ curl -X GET http://localhost:8080/widgets/<id>`  
get widget by id

```
$ curl -X POST http://localhost:8080/widgets \
 -H "Content-type: application/json" \  
 -d '{"x": 5, "y": 6, "width": 10, "height": 10}'
```
create a widget, without specifying zIndex

```
$ curl -X PUT http://localhost:8080/widgets/<id> \
 -H "Content-type: application/json" \
 -d '{"zIndex": 0}'
```
update zIndex (may use any other property except id and updatedAt)

`$ curl -X DELETE http://localhost:8080/widgets/<id>`  
delete widget by id

`$ curl -X GET http://localhost:8080/widgets\?left\=<int>\&right\=<int>\&bottom\=<int>\&top\=<int>`  
find all widgets falling into specified rectangular area