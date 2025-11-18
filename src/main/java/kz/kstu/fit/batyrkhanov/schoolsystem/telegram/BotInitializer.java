package kz.kstu.fit.batyrkhanov.schoolsystem.telegram;

import kz.kstu.fit.batyrkhanov.schoolsystem.config.TelegramRuntimeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
public class BotInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BotInitializer.class);
    private final TelegramRuntimeConfig telegramRuntimeConfig;
    private final TelegramBot telegramBot;

    public BotInitializer(TelegramRuntimeConfig telegramRuntimeConfig, TelegramBot telegramBot) {
        this.telegramRuntimeConfig = telegramRuntimeConfig;
        this.telegramBot = telegramBot;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!telegramRuntimeConfig.isEnabled()) {
            log.info("Telegram bot disabled (no token)");
            return;
        }
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(telegramBot);
            log.info("Telegram bot registered: {}", telegramBot.getBotUsername());
        } catch (Exception e) {
            log.error("Failed to register Telegram bot", e);
        }
    }
}
