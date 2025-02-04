package org.example;

import com.jcraft.jsch.*;

import javax.json.*;
import java.io.*;
import java.util.*;

public class SftpClient {

    private Session session;
    private ChannelSftp channelSftp;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String host;
        int port;
        String username;
        String password;

        if (args.length < 4) {
            System.out.println("Для подключения к SFTP серверу введите <адрес> <порт> <логин> <пароль>:");

            System.out.print("Адрес сервера: ");
            host = scanner.nextLine();

            System.out.print("Порт: ");
            port = Integer.parseInt(scanner.nextLine());

            System.out.print("Логин: ");
            username = scanner.nextLine();

            System.out.print("Пароль: ");
            password = scanner.nextLine();
        } else {
            host = args[0];
            port = Integer.parseInt(args[1]);
            username = args[2];
            password = args[3];
        }
        try {
            SftpClient client = new SftpClient();
            if (client.connect(host, port, username, password)) {
                client.showMenu();
                client.disconnect();
            } else {
                System.out.println("Ошибка при подключении к SFTP серверу.");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public boolean connect(String host, int port, String username, String password) {
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            return true;
        } catch (JSchException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void disconnect() {
        if (channelSftp != null) channelSftp.disconnect();
        if (session != null) session.disconnect();
    }

    public void showMenu() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\nВыбери действие:");
            System.out.println("1.\tПолучение списка пар \"домен – адрес\" из файла");
            System.out.println("2.\tПолучение IP-адреса по доменному имени");
            System.out.println("3.\tПолучение доменного имени по IP-адресу");
            System.out.println("4.\tДобавление новой пары \"домен – адрес\" в файл");
            System.out.println("5.Удаление доменной пары");
            System.out.println("6.Завершение работы");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    getDomainIpPairs();
                    break;
                case 2:
                    System.out.println("Введите домен:");
                    String domain = scanner.nextLine();
                    getIpByDomain(domain);
                    break;
                case 3:
                    System.out.println("Введите IP-адрес:");
                    String ip = scanner.nextLine();
                    getDomainByIp(ip);
                    break;
                case 4:
                    System.out.println("Введите домен:");
                    String newDomain = scanner.nextLine();
                    System.out.println("Введите IP-адрес:");
                    String newIp = scanner.nextLine();
                    addDomainIpPair(newDomain, newIp);
                    break;
                case 5:
                    System.out.println("Введите домен или IP-адрес для удаления:");
                    String key = scanner.nextLine();
                    removeDomainIpPair(key);
                    break;
                case 6:
                    return;
                default:
                    System.out.println("Произошла ошибка.Попробуйте еще раз.");
            }
        }
    }

    public Map<String, String> readJsonFile() {
        Map<String, String> result = new HashMap<>();
        try (InputStream inputStream = channelSftp.get("domains.json");
             JsonReader reader = Json.createReader(inputStream)) {
            JsonObject jsonObject = reader.readObject();
            JsonArray domainsArray = jsonObject.getJsonArray("domains");

            for (JsonObject domainObject : domainsArray.getValuesAs(JsonObject.class)) {
                String domain = domainObject.getString("domain");
                String ip = domainObject.getString("ip");
                result.put(domain, ip);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage() + " Произошла ошибка при чтение из файла");
            e.printStackTrace();
        }
        return result;
    }
    public void writeJson(Map<String, String> data) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); JsonWriter writer = Json.createWriter(outputStream)) {
            JsonArrayBuilder domainsArrayBuilder = Json.createArrayBuilder();
            data.forEach((domain, ip) -> {
                JsonObject domainObject = Json.createObjectBuilder()
                        .add("domain", domain)
                        .add("ip", ip)
                        .build();
                domainsArrayBuilder.add(domainObject);
            });

            JsonObject jsonObject = Json.createObjectBuilder()
                    .add("domains", domainsArrayBuilder)
                    .build();
            writer.writeObject(jsonObject);
            try (InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
                channelSftp.put(inputStream, "domains.json", ChannelSftp.OVERWRITE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void getDomainIpPairs() {
        Map<String, String> data = readJsonFile();
        if (data != null) {
            data.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> System.out.println(entry.getKey() + " - " + entry.getValue()));
        }
    }

    private void getIpByDomain(String domain) {
        Map<String, String> data = readJsonFile();
        if (data != null && data.containsKey(domain)) {
            System.out.println("IP-адрес для домена: " + domain + " это " + data.get(domain));
        } else {
            System.out.println("Домен не найден.");
        }
    }

    private void getDomainByIp(String ip) {
        Map<String, String> data = readJsonFile();
        if (data != null) {
            Optional<String> domain = data.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(ip))
                    .map(Map.Entry::getKey)
                    .findFirst();
            if (domain.isPresent()) {
                System.out.println("Домен для IP-адреса: " + ip + " это " + domain.get());
            } else {
                System.out.println("IP-адрес не найден.");
            }
        }
    }

    private void addDomainIpPair(String domain, String ip) {
        if (!isValidIp(ip)) {
            System.out.println("Неправильный IP-адрес.");
            return;
        }

        Map<String, String> data = readJsonFile();
        if (data != null) {
            if (data.containsKey(domain) || data.containsValue(ip)) {
                System.out.println("Домен или IP-адрес уже существует.");
            } else {
                data.put(domain, ip);
                writeJson(data);
                System.out.println("Доменная пара добавлена успешно.");
            }
        }
    }

    private void removeDomainIpPair(String key) {
        Map<String, String> data = readJsonFile();
        if (data != null) {
            if (data.containsKey(key)) {
                data.remove(key);
                writeJson(data);
                System.out.println("Доменная пара удалена успешно.");
            } else if (data.containsValue(key)) {
                data.entrySet().removeIf(entry -> entry.getValue().equals(key));
                writeJson(data);
                System.out.println("Доменная пара удалена успешно.");
            } else {
                System.out.println("Домен или IP не найден.");
            }
        }
    }

    private boolean isValidIp(String ip) {
        return ip.matches("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    }
}
