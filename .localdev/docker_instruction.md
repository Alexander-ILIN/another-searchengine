# Развёртывание приложения в Docker

## Создание сети

В командной строке Docker ввести команду:

    docker network create -d bridge searchengine_network

## Создание контейнера с базой данных MySQL в Docker

В командной строке Docker ввести команду для запуска контейнера с MySQL:  

    docker run -d --name searchengine_db --network searchengine_network -p 3306:3306 -v searchengine_data:/var/lib/mysql_volume -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=search_engine -e MYSQL_USER=bestuser -e MYSQL_PASSWORD=bestuser mysql:8.0.22

## Создание Docker образа приложения

В командной строке Docker ввести команду:

    docker build -f .deploy/dockerfile-searchengine -t searchengine:latest .

## Создание контейнера с приложением

В командной строке Docker ввести команду:
    
    docker run -d --name searchengine_app --network searchengine_network -p 8080:8080 -v **path**:/usr/searchengine searchengine:latest

где **path** - путь к папке, в которую необходимо поместить файлы `SearchEngine.jar` и `application.yaml`.