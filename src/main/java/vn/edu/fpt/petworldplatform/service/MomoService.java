package vn.edu.fpt.petworldplatform.service;

import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.edu.fpt.petworldplatform.config.MomoConfig;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class MomoService {

    @Autowired
    private MomoConfig momoConfig;

    public String createPaymentUrl(String orderId, BigDecimal amount, String orderInfo) throws Exception {
        String requestId = String.valueOf(System.currentTimeMillis());

        // ==========================================
        // ĐÃ SỬA Ở ĐÂY: Đổi từ "captureWallet" sang "payWithATM"
        // ==========================================
        String requestType = "payWithATM";

        String extraData = ""; // Có thể để trống hoặc gửi chuỗi Base64

        // MoMo yêu cầu số tiền là số nguyên (long)
        long amountLong = amount.longValue();

        // 1. Tạo chuỗi dữ liệu thô để ký theo đúng thứ tự MoMo quy định
        String rawData = "accessKey=" + momoConfig.getAccessKey() +
                "&amount=" + amountLong +
                "&extraData=" + extraData +
                "&ipnUrl=" + momoConfig.getNotifyUrl() +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + momoConfig.getPartnerCode() +
                "&redirectUrl=" + momoConfig.getReturnUrl() +
                "&requestId=" + requestId +
                "&requestType=" + requestType;

        // 2. Tạo chữ ký HmacSHA256 (Dùng secretKey để ký)
        String signature = hmacSha256(rawData, momoConfig.getSecretKey());

        // 3. Tạo Body cho yêu cầu JSON
        Map<String, Object> message = new HashMap<>();
        message.put("partnerCode", momoConfig.getPartnerCode());
        message.put("accessKey", momoConfig.getAccessKey());
        message.put("partnerName", "Pet World");
        message.put("storeId", "PetWorldStore");
        message.put("requestId", requestId);
        message.put("amount", amountLong);
        message.put("orderId", orderId);
        message.put("orderInfo", orderInfo);
        message.put("redirectUrl", momoConfig.getReturnUrl());
        message.put("ipnUrl", momoConfig.getNotifyUrl());
        message.put("lang", "vi");
        message.put("extraData", extraData);
        message.put("requestType", requestType);
        message.put("autoCapture", true);
        message.put("signature", signature);

        // 4. Gọi RestTemplate để gửi POST sang MoMo và lấy payUrl trả về
        return callMomoApi(momoConfig.getEndpoint(), message);
    }

    /**
     * Verify IPN (Instant Payment Notification) signature from MOMO callback.
     * Builds raw string from all params except 'signature', sorted by key, then HMAC_SHA256.
     */
    public boolean verifyIpnSignature(Map<String, Object> params) {
        try {
            String receivedSignature = (String) params.get("signature");
            if (receivedSignature == null) return false;

            StringBuilder raw = new StringBuilder();
            params.entrySet().stream()
                    .filter(e -> !"signature".equals(e.getKey()))
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> {
                        if (raw.length() > 0) raw.append("&");
                        Object v = e.getValue();
                        raw.append(e.getKey()).append("=").append(v != null ? v.toString() : "");
                    });
            String computed = hmacSha256(raw.toString(), momoConfig.getSecretKey());
            return computed.equalsIgnoreCase(receivedSignature);
        } catch (Exception e) {
            return false;
        }
    }

    private String hmacSha256(String data, String key) throws Exception {
        byte[] byteKey = key.getBytes(StandardCharsets.UTF_8);
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(byteKey, "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    // Hàm gọi API HTTP POST sang MoMo
    private String callMomoApi(String endpoint, Map<String, Object> message) {
        RestTemplate restTemplate = new RestTemplate();

        // Thiết lập Headers là JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Đóng gói Body và Headers
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(message, headers);

        try {
            // Gửi request và nhận response trả về kiểu Map
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, entity, Map.class);

            // Lấy URL thanh toán từ kết quả trả về
            if (response.getBody() != null && response.getBody().containsKey("payUrl")) {
                return response.getBody().get("payUrl").toString();
            } else {
                throw new RuntimeException("Lỗi từ MoMo: Không tìm thấy payUrl trong phản hồi. Chi tiết: " + response.getBody());
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi kết nối với máy chủ MoMo: " + e.getMessage());
        }
    }
}