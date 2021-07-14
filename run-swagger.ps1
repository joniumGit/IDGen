docker container kill idgen-swagger
docker container rm idgen-swagger
docker run -d -p 127.0.0.1:5000:8080 -e BASE_URL=/docs -e SWAGGER_JSON=/docs/idgen.yaml -v $PWD/docs:/docs --name idgen-swagger swaggerapi/swagger-ui