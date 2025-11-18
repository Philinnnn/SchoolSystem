package kz.kstu.fit.batyrkhanov.schoolsystem.telegram;

import kz.kstu.fit.batyrkhanov.schoolsystem.config.TelegramRuntimeConfig;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.User;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.UserRepository;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.TelegramAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);
    private final TelegramRuntimeConfig config;
    private final UserRepository userRepository;
    private final TelegramAuthService telegramAuthService;
    private final LoginRequestStore loginRequestStore;

    public TelegramBot(TelegramRuntimeConfig config,
                       UserRepository userRepository,
                       TelegramAuthService telegramAuthService,
                       LoginRequestStore loginRequestStore) {
        super(config.getToken());
        this.config = config;
        this.userRepository = userRepository;
        this.telegramAuthService = telegramAuthService;
        this.loginRequestStore = loginRequestStore;
    }

    // Метод для отправки сообщения подтверждения входа
    public Integer sendLoginConfirmation(Long chatId, String requestId, String username) {
        try {
            InlineKeyboardButton approve = new InlineKeyboardButton("Войти");
            approve.setCallbackData("LOGIN_APPROVE:" + requestId);
            InlineKeyboardButton decline = new InlineKeyboardButton("Отклонить");
            decline.setCallbackData("LOGIN_DECLINE:" + requestId);
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup(List.of(List.of(approve, decline)));
            SendMessage msg = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("Запрос на вход для пользователя: " + username + "\nПодтвердите действие.")
                    .replyMarkup(markup)
                    .build();
            return execute(msg).getMessageId();
        } catch (Exception e) {
            log.error("Не удалось отправить подтверждение входа", e);
            return null;
        }
    }

    private void reply(Long chatId, String text) { safeReply(chatId, text); }

    @Override
    public void onUpdateReceived(Update update) {
        if (!config.isEnabled()) return;
        // Сначала callback-и
        if (update != null && update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            Long cbChatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            if (data != null && (data.startsWith("LOGIN_APPROVE:") || data.startsWith("LOGIN_DECLINE:"))) {
                String requestId = data.substring(data.indexOf(':') + 1);
                LoginRequestStore.LoginRequest lr = loginRequestStore.get(requestId);
                if (lr == null) {
                    safeReply(cbChatId, "Запрос не найден или истёк.");
                    safeDelete(cbChatId, messageId);
                    return;
                }
                if (data.startsWith("LOGIN_APPROVE:")) {
                    loginRequestStore.approve(requestId);
                    safeReply(cbChatId, "Запрос входа подтверждён.");
                } else {
                    loginRequestStore.decline(requestId);
                    safeReply(cbChatId, "Запрос входа отклонён.");
                }
                safeDelete(cbChatId, messageId);
                return;
            }
            return; // обработали callback
        }
        if (update == null || !update.hasMessage() || update.getMessage().getText() == null) return;
        String text = update.getMessage().getText().trim();
        Long chatId = update.getMessage().getChatId();

        try {
            if (text.startsWith("/login")) {
                String[] parts = text.split("\\s+");
                if (parts.length < 2) {
                    reply(chatId, "Использование: /login <логин>");
                    return;
                }
                String username = parts[1];
                User user = userRepository.findByUsername(username);
                if (user == null) {
                    reply(chatId, "Пользователь не найден");
                    return;
                }
                String token = telegramAuthService.generateLoginToken(user, config.getLoginTokenTtlMinutes());
                reply(chatId, "Код входа: " + token + " (действителен " + config.getLoginTokenTtlMinutes() + " мин.) Введите его на сайте.");
                return;
            }
            if (text.startsWith("/link")) {
                String[] parts = text.split("\\s+");
                if (parts.length < 2) {
                    reply(chatId, "Использование: /link <код>");
                    return;
                }
                String linkCode = parts[1].toUpperCase();
                User user = userRepository.findByTelegramLinkCode(linkCode);
                if (user == null) {
                    reply(chatId, "Код не найден или устарел.");
                    return;
                }
                boolean ok = telegramAuthService.bindTelegram(user, chatId);
                reply(chatId, ok ? "Telegram привязан к аккаунту." : "Не удалось привязать.");
                return;
            }
            if (text.startsWith("/unlink")) {
                User user = userRepository.findByTelegramId(chatId);
                if (user == null) {
                    reply(chatId, "Аккаунт не привязан.");
                    return;
                }
                telegramAuthService.unbindTelegram(user);
                reply(chatId, "Привязка Telegram удалена.");
                return;
            }
        } catch (Exception e) {
            log.error("Ошибка обработки команды Telegram", e);
            reply(chatId, "Произошла ошибка. Попробуйте позже.");
        }
    }

    private void safeDelete(Long chatId, Integer messageId) {
        try { execute(DeleteMessage.builder().chatId(chatId.toString()).messageId(messageId).build()); } catch (Exception ignored) {}
    }

    private void safeReply(Long chatId, String text) {
        try { execute(SendMessage.builder().chatId(chatId.toString()).text(text).build()); } catch (Exception ignored) {}
    }

    @Override
    public String getBotUsername() { return config.getUsername(); }

    @Override
    public String getBotToken() { return config.getToken(); }
}
