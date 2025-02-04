# infotecs-test-task

1. Исполняемый jar-файл клиента называется:
   
   `testing_infotecs-1.0-SNAPSHOT.jar`
   
2. Проект был собран благодаря команде:
   
   `mvn clean package`

3. Инструкция по работе с приложением:
   
   При запуске приложения пользователя просят ввесьти <адрес> <host> <логин> <пароль> от sftp-сервера.
   
   Далее будет предложено меню с командами для выполнения манипуляций с JSON файлом на сервере:
   
    Загрузка/выгрузка файлов
   
    Чтение и редактирование данных
   
    Работа с доменами и IP-адресами

   В своем задание я создала SFTP-сервер на платформе Linux(Ubuntu), благодаря командам при помощи подключения через docker:


   **подключение через docker с пробросом портов:**

   ```
   docker run -it -p 2222:22 --name sftp-server ubuntu bash
   
   docker start sftp-server
   
   docker exec -it sftp-server bash
   ```
   
   **добавила файл json с доменами и IP-адресами**
   
   **установка необходимых пакетов**

   ```
   apt-get update
   
   apt-get install openssh-server
   ```

   **создать пользователя sftp**
   
   ```
   useradd -m sftpuser
   
   mkdir /home/sftpuser/.ssh
   
   chmod 700 /home/sftpuser/.ssh
   
   passwd sftpuser
   ```
   
   **запустить сервер**
   
   `service ssh start`
   
5. Реализация тестов находится в файле SftpClientTest, но здесь она не была реализована.

6. В будущем тесты можно запустить по команде:
   
   `mvn test`
   

   
