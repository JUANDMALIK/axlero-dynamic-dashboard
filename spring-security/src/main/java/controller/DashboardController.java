```java
package controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.HashMap;

@RestController
public class DashboardController {

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        Map<String, Object> data = new HashMap<>();

        data.put("totalUsers", 150);
        data.put("totalSales", 25000);
        data.put("totalOrders", 75);
        data.put("message", "Dashboard data fetched successfully");

        return data;
    }
}
```