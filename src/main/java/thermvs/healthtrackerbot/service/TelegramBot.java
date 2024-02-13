package thermvs.healthtrackerbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import thermvs.healthtrackerbot.configuration.BotConfiguration;
import thermvs.healthtrackerbot.model.InfoEntity;
import thermvs.healthtrackerbot.model.UserEntity;
import thermvs.healthtrackerbot.repository.InfoRepository;
import thermvs.healthtrackerbot.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final Map<Long, List<String>> userResponses;
    final BotConfiguration config;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InfoRepository infoRepository;
    @Autowired
    private MessageService messageService;

    static final String HELP_TEXT = "This bot is created to help you feel better.\n\n" +
            "You can execute commands from the main menu on the left or by typing a command:\n\n" +
            "/start register and get a welcome message\n\n" +
            "/exit - log out and delete USER info \n ! if you want to delete SLEEP info use /clear_stats\n\n" +
            "/who_am_i get user data about you \n\n" +
            "/help info how to use this bot \n\n" +
            "/ask_about_sleep add info about your sleep \n\n" +
            "/clear_stats delete info about your sleep \n\n" +
            "/stats view your data \n\n" +
            "/advice receive advice from ALL time";

    public TelegramBot(BotConfiguration config) {
        this.config = config;
        this.userResponses = new HashMap<>();
        List<BotCommand> botCommandList = new ArrayList<>();
        botCommandList.add(new BotCommand("/start", "get a welcome message"));
        botCommandList.add(new BotCommand("/exit", "log out and DELETE all user data, including sleep information"));
        botCommandList.add(new BotCommand("/who_am_i", "get user data about you"));
        botCommandList.add(new BotCommand("/help", "info how to use this bot"));
        botCommandList.add(new BotCommand("/ask_about_sleep", "add info about your sleep"));
        botCommandList.add(new BotCommand("/clear_stats", "delete info about your sleep"));
        botCommandList.add(new BotCommand("/stats", "view your data"));
        botCommandList.add(new BotCommand("/advice", "receive advice from ALL time"));
        try {
            this.execute(new SetMyCommands(botCommandList, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error while setting command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();
            String text = message.getText();
            if (text.startsWith("/")) {
                handleCommand(chatId, text, update);
            } else {
                customHandleTextMessage(chatId, text, message);
            }
        }
    }

    private void handleCommand(long chatId, String command, Update update) {
        switch (command) {
            case "/start" -> {
                startGreeting(chatId, update.getMessage().getChat().getFirstName());
                registerUser(update.getMessage());
            }
            case "/who_am_i" -> whoAmI(update.getMessage());
            case "/exit" -> logOutAndDeleteUser(update.getMessage());
            case "/ask_about_sleep" -> askStats(chatId);
            case "/clear_stats" -> clearStats(update.getMessage());
            case "/stats" -> receiveStatsData(update.getMessage());
            case "/advice" -> receiveAdvice(update.getMessage());
            case "/help" -> sendMessage(chatId, HELP_TEXT);
            default -> sendMessage(chatId, "Your command was not recognized");
        }
    }

    private void startGreeting(long chatId, String name) {
        String answer = "Good day, " + name + ", nice to meet you in this bot!";
        log.info("Replied to user " + name);
        sendMessage(chatId, answer);
    }


    public void clearStats(Message msg) {
        var id = msg.getChatId();
        var chat = msg.getChat();
        var userName = chat.getUserName();
        if (userRepository.findById(msg.getChatId()).isPresent()) {
            var listStats = infoRepository.findAllByUserName(userName);
            infoRepository.deleteAll(listStats);
            sendMessage(id, "Delete your data: done");
        } else {
            sendMessage(id, "You are not registered in the bot! \n You you need to use /start to register");
        }
    }

    private void whoAmI(Message msg) {
        var id = msg.getChatId();
        var chat = msg.getChat();
        if (userRepository.findById(msg.getChatId()).isPresent()) {
            sendMessage(id, "Your id: " + id + "\n First name: " +
                    chat.getFirstName() + "\n Last name: " + chat.getLastName() +
                    "\n Username in telegram: " + chat.getUserName());
        } else {
            sendMessage(id, "You are not registered in the bot! \n You you need to use /start to register");
        }
    }

    private void receiveAdvice(Message msg) {
        var id = msg.getChatId();
        var chat = msg.getChat();
        var userName = chat.getUserName();
        if (userRepository.findById(msg.getChatId()).isPresent()) {
            var listStats = infoRepository.findAllByUserName(userName);
            int totalStats = listStats.size();
            int totalSleepDuration = 0;
            double totalSleepQuality = 0;
            double totalDaytimeAlertness = 0;
            double avgSleepQuality;
            double avgDaytimeAlertness;
            int healthySleepDuration = 8 * totalStats;
            for (var stats : listStats) {
                var sleepStartTime = stats.getSleepStartTime();
                var wakeUpTime = stats.getWakeUpTime();
                var sleepQuality = stats.getSleepQuality();
                var daytimeAlertness = stats.getDaytimeAlertness();
                int sleepDuration = calculateSleepDuration(sleepStartTime, wakeUpTime);
                totalSleepDuration += sleepDuration;
                totalSleepQuality += Double.parseDouble(sleepQuality);
                totalDaytimeAlertness += Double.parseDouble(daytimeAlertness);
            }
            avgSleepQuality = totalSleepQuality / totalStats;
            avgDaytimeAlertness = totalDaytimeAlertness / totalStats;
            int sleepDurationDifference = totalSleepDuration - healthySleepDuration;

            sendMessage(id, "Your total sleep duration (from ALL time): " + totalSleepDuration + " hours.");
            sendMessage(id, "Recommended total sleep duration: " + healthySleepDuration + " hours.");
            sendMessage(id, "Sleep duration difference: " + sleepDurationDifference + " hours.");
            sendMessage(id, "Average sleep quality: " + avgSleepQuality);
            sendMessage(id, "Average daytime alertness: " + avgDaytimeAlertness);

            double sleepCoefficient = avgSleepQuality * avgDaytimeAlertness;

            if (sleepDurationDifference > 0) {
                if (sleepCoefficient < 0.5) {
                    sendMessage(id, "You are sleeping more than recommended, but your sleep quality and daytime alertness are low. " +
                            "Try adjusting your bedtime routine and consider seeking advice from a healthcare professional.");
                } else {
                    sendMessage(id, "You are sleeping more than recommended, but your sleep quality and daytime alertness seem fine. " +
                            "Try adjusting your bedtime routine for better sleep efficiency.");
                }
            } else if (sleepDurationDifference < 0) {
                if (sleepCoefficient < 0.5) {
                    sendMessage(id, "You are sleeping less than recommended, and your sleep quality and daytime alertness are low. " +
                            "Consider prioritizing sleep, establishing a consistent sleep schedule, and improving sleep environment.");
                } else {
                    sendMessage(id, "You are sleeping less than recommended, but your sleep quality and daytime alertness seem fine. " +
                            "Consider maintaining a consistent sleep schedule and optimizing your sleep environment.");
                }
            } else {
                if (sleepCoefficient < 0.5) {
                    sendMessage(id, "Your sleep duration is within the recommended range, but your sleep quality and daytime alertness are low. " +
                            "Focus on improving sleep quality and seeking ways to enhance daytime alertness.");
                } else {
                    sendMessage(id, "Congratulations! Your sleep duration is within the recommended range, and your sleep quality " +
                            "and daytime alertness seem fine. Keep up the good sleep habits!");
                }
            }
        } else {
            sendMessage(id, "You are not registered in the bot! \n You need to use /start to register");
        }
    }

    private int calculateSleepDuration(String sleepStartTime, String wakeUpTime) {
        var start = Integer.parseInt(sleepStartTime);
        var end = Integer.parseInt(wakeUpTime);
        if (start > 12) {
            return 24 - start + end;
        } else {
            return end - start;
        }
    }

    private void receiveStatsData(Message msg) {
        var id = msg.getChatId();
        var chat = msg.getChat();
        var userName = chat.getUserName();

        if (userRepository.findById(msg.getChatId()).isPresent()) {
            var listStats = infoRepository.findAllByUserName(userName);
            sendMessage(id, "All your data: ");
            for (var stats : listStats) {
                var sleepStartTime = stats.getSleepStartTime();
                var wakeUpTime = stats.getWakeUpTime();
                var sleepQuality = stats.getSleepQuality();
                var daytimeAlertness = stats.getDaytimeAlertness();
                sendMessage(id, "Sleep start time: " + sleepStartTime + "\nWake up time: " + wakeUpTime +
                        "\nSleep quality: " + sleepQuality + "\nDaytime alertness: " + daytimeAlertness);
            }
            sendMessage(id, "Now you can enter command /advice to receive personalized advice by ALL your statistic");
        } else {
            sendMessage(id, "You are not registered in the bot! \n You you need to use /start to register");
        }
    }

    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) {
            var id = msg.getChatId();
            var chat = msg.getChat();
            UserEntity user = new UserEntity();
            user.setId(id);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());

            messageService.sendMessage(user);
            userRepository.save(user);
            sendMessage(id, "You was registered successfully");
            log.info("New user saved: " + user);
        }
    }

    private void logOutAndDeleteUser(Message msg) {
        var id = msg.getChatId();
        var chat = msg.getChat();

        if (userRepository.findById(msg.getChatId()).isPresent()) {
            var list = userRepository.findAllByUserName(chat.getUserName());
            for (var d : list) {
                userRepository.delete(d);
                log.info("Delete user: " + chat.getUserName());
            }
            sendMessage(id, "Goodbye! You logged out");
        } else {
            sendMessage(id, "You are not registered in the bot! \n You you need to use /start to register");
        }
    }

    private void customHandleTextMessage(long chatId, String response, Message msg) {
        if (userRepository.findById(msg.getChatId()).isPresent()) {
            if (userResponses.containsKey(chatId)) {
                List<String> responses = userResponses.get(chatId);
                responses.add(response);

                if (responses.size() == 4) {
                    String sleepTime = responses.get(0);
                    String wakeupTime = responses.get(1);
                    String sleepQuality = responses.get(2);
                    String daytimeAlertness = responses.get(3);

                    if (isInteger(sleepTime) && isInteger(wakeupTime) && isInteger(sleepQuality) && isInteger(daytimeAlertness)) {
                        String message = "Well, you slept from " + sleepTime + " to " + wakeupTime +
                                "\nYour quality of sleep is " + sleepQuality + " and daytime alertness is " + daytimeAlertness;
                        sendMessage(chatId, message);
                        String ending = "Now you can check your stats using /stats or enter /help";
                        sendMessage(chatId, ending);

                        InfoEntity info = new InfoEntity();
                        var chat = msg.getChat();
                        info.setUserName(chat.getUserName());
                        info.setSleepStartTime(sleepTime);
                        info.setWakeUpTime(wakeupTime);
                        info.setSleepQuality(sleepQuality);
                        info.setDaytimeAlertness(daytimeAlertness);
                        infoRepository.save(info);
                        log.info("Save info about user with username: " + chat.getUserName());
                    } else {
                        sendMessage(chatId, "Please make sure to enter integer values for values. \n" +
                                "Try again using /ask_about_sleep ");
                    }
                    userResponses.remove(chatId);
                } else if (responses.size() == 1) {
                    sendMessage(chatId, "When did you wake up?\n" +
                            "Enter an integer value for the hour in 24-hour format. For example, 22 or 23 or 4 or 2");
                } else if (responses.size() == 2) {
                    sendMessage(chatId, "What about sleep quality? \n recommended value [0; 10]");
                } else if (responses.size() == 3) {
                    sendMessage(chatId, "What about daytime alertness? \n recommended value[0; 10]");
                }
            } else {
                sendMessage(chatId, "You should enter a correct command. Check it with /help");
            }
        } else {
            sendMessage(chatId, "You are not registered in the bot! \n You you need to use /start to register");
        }
    }

    private void askStats(long chatId) {
        if (userRepository.findById(chatId).isPresent()) {
            if (userRepository.findById(chatId).isPresent()) {
                sendMessage(chatId, "When did you go to sleep?\n" +
                        "Enter an integer value for the hour in 24-hour format. For example, 22 or 23 or 4 or 2");
                userResponses.put(chatId, new ArrayList<>());
            }
        } else {
            sendMessage(chatId, "You are not registered in the bot! \n You you need to use /start to register");
        }
    }

    private boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error: " + e.getMessage());
        }
    }
}