package com.malmal.kiosk.controller;

import com.malmal.kiosk.domain.Menu;
import com.malmal.kiosk.domain.OrderRecord;
import com.malmal.kiosk.repository.MenuRepository;
import com.malmal.kiosk.repository.OrderRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final MenuRepository menuRepository;
    private final OrderRecordRepository orderRecordRepository;

    // 환경변수 ADMIN_USERNAME / ADMIN_PASSWORD 로 변경 가능 (기본값은 개발용)
    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:admin1234}")
    private String adminPassword;

    public AdminController(MenuRepository menuRepository, OrderRecordRepository orderRecordRepository) {
        this.menuRepository = menuRepository;
        this.orderRecordRepository = orderRecordRepository;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        boolean ok = adminUsername.equals(body.get("username")) && adminPassword.equals(body.get("password"));
        if (ok) {
            return Map.of("success", true);
        }
        return Map.of("success", false, "message", "아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    @GetMapping("/menus")
    public List<Menu> menus() {
        return menuRepository.findAll();
    }

    @PutMapping("/menus/{id}/stock")
    public ResponseEntity<?> updateStock(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object value = body.get("stock");
        if (!(value instanceof Number)) {
            return ResponseEntity.badRequest().body(Map.of("message", "stock 값이 필요합니다."));
        }
        return menuRepository.findById(id).map(menu -> {
            menu.setStock(Math.max(0, ((Number) value).intValue()));
            return ResponseEntity.ok((Object) menuRepository.save(menu));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        List<OrderRecord> orders = todayOrders();
        long sales = orders.stream().mapToLong(OrderRecord::getTotalAmount).sum();
        return Map.of("sales", sales, "orders", orders.size());
    }

    @GetMapping("/orders/today")
    public List<OrderRecord> ordersToday() {
        return todayOrders();
    }

    private List<OrderRecord> todayOrders() {
        LocalDate today = LocalDate.now();
        return orderRecordRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay());
    }
}
