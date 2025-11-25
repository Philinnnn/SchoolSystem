package kz.kstu.fit.batyrkhanov.schoolsystem.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Простая политика паролей:
 *  - минимум 8 символов
 *  - хотя бы одна строчная латинская буква
 *  - хотя бы одна заглавная латинская буква
 *  - хотя бы одна цифра
 *  - хотя бы один спецсимвол из набора (не буква/цифра)
 */
public final class PasswordPolicy {
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    private PasswordPolicy() {}

    public static boolean isValid(String password) {
        return violations(password).isEmpty();
    }

    public static List<String> violations(String password) {
        List<String> list = new ArrayList<>();
        if (password == null || password.isBlank()) {
            list.add("Пароль не должен быть пустым");
            return list;
        }
        if (password.length() < 8) list.add("Минимум 8 символов");
        if (!LOWER.matcher(password).find()) list.add("Нет строчной латинской буквы");
        if (!UPPER.matcher(password).find()) list.add("Нет заглавной латинской буквы");
        if (!DIGIT.matcher(password).find()) list.add("Нет цифры");
        if (!SPECIAL.matcher(password).find()) list.add("Нет спецсимвола");
        return list;
    }

    /**
     * Строка с объединёнными нарушениями.
     */
    public static String violationMessage(String password) {
        List<String> v = violations(password);
        if (v.isEmpty()) return null;
        return "Пароль не соответствует требованиям: " + String.join(", ", v);
    }
}

