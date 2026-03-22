package vn.edu.fpt.petworldplatform.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Tool nhỏ để generate BCrypt hash cho mật khẩu.
 *
 * Cách chạy nhanh trong IDE:
 * - Run method main() của class này
 * - Hoặc truyền args[0] là mật khẩu cần hash
 */
public final class BCryptTool {

    private BCryptTool() {}

    public static void main(String[] args) {
        String raw = (args != null && args.length > 0 && args[0] != null && !args[0].isBlank())
                ? args[0]
                : "123456";

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(raw);

        System.out.println("RAW  : " + raw);
        System.out.println("BCrypt: " + hash);
        System.out.println("matches? " + encoder.matches(raw, hash));
    }
}

