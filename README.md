# Endpoints

* http://localhost:8890/sparql - the regular virtuoso endpoint
* http://localhost:9980/sparql - the user aware endpoint {NOTE look below]
* http://localhost:9980/graphs - the configuration endpoint

# Configuration

## setting
The configuration can be set through the graph.yml in ./config/uags/

## consulting
on the configuration endpoint

# Example Curl/Responses

## 1 Inserting a new shopping cart

### Query
```
INSERT 
{ 
<http://shopping-carts/1> a <http://example.com/shopping/ShoppingCart> ; 
<lastAccessed> "12.12.12" . 
<http://test.com/test/1> <http://test.com/testFor> "no reason" . 
}
```
### CURL
```
curl -X GET \
 'http://localhost/uags/?query=insert%20%7B%20%3Chttp%3A%2F%2Fshopping-carts%2F1%3E%20a%20%3Chttp%3A%2F%2Fexample.com%2Fshopping%2FShoppingCart%3E%20%3B%20%3ClastAccessed%3E%20%2212.12.12%22%20.%20%3Chttp%3A%2F%2Ftest.com%2Ftest%2F1%3E%20%3Chttp%3A%2F%2Ftest.com%2FtestFor%3E%20%22no%20reason%22%20.%20%7D' \
  -H 'accept: application/json' \
  -H 'cache-control: no-cache' \
  -H 'postman-token: d0d20d38-da17-0d13-d8c9-fdca7d74db6f' \
  -H 'userid: Jonathan'
```
  
### Response
```
INSERT DATA
{
GRAPH <http://mu.semte.ch/application>
{
<http://test.com/test/1> <http://test.com/testFor> "no reason" . 
}
GRAPH <http://mu.semte.ch/personal/Jonathan>
{
<http://shopping-carts/1> a <http://example.com/shopping/ShoppingCart> . 
<http://shopping-carts/1> <lastAccessed> "12.12.12" . 
}
```
## 2 Consulting your shopping carts

### Query
```
SELECT * from <http://mu.semte.ch/application> 
WHERE 
{ ?
s a <http://example.com/shopping/ShoppingCart> ; 
?p ?o .
}
```

### CURL
```
curl -X GET \
  'http://localhost:9980/sparql?query=select%20*%20from%20%3Chttp%3A%2F%2Fmu.semte.ch%2Fapplication%3E%20where%20%7B%20%3Fs%20a%20%3Chttp%3A%2F%2Fexample.com%2Fshopping%2FShoppingCart%3E%20%3B%20%3Fp%20%3Fo%20.%20%7D' \
  -H 'accept: application/json' \
  -H 'cache-control: no-cache' \
  -H 'postman-token: 821f8b58-b63e-5c60-1391-84c83bf1a74e' \
  -H 'userid: Jonathan'
```

### Response
```
SELECT *
FROM NAMED <http://mu.semte.ch/uuid>
FROM NAMED <http://mu.semte.ch/personal/Jonathan>

WHERE
{
?s a <http://example.com/shopping/ShoppingCart> .  .
?s ?p ?o .  .
}
```

## 3 Getting everything

### Query
```
SELECT *
WHERE {
?s ?p ?o .
}
```

### CURL
```
curl -X GET \
  'http://localhost:9980/sparql?query=select%20*%20from%20%3Chttp%3A%2F%2Fmu.semte.ch%2Fapplication%3E%20where%20%7B%20%3Fs%20%3Fp%20%3Fo%20.%20%7D' \
  -H 'accept: application/json' \
  -H 'cache-control: no-cache' \
  -H 'postman-token: f7c5423d-2bd0-ea00-56ec-1dbe7ada5eca' \
  -H 'userid: Jonathan' 
```

### Response
```
SELECT *
FROM NAMED <http://mu.semte.ch/uuid>
FROM NAMED <http://mu.semte.ch/application>
FROM NAMED <http://mu.semte.ch/personal/Jonathan>

WHERE
{
?s ?p ?o .  .
}
```
