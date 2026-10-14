package dashboard_builder.repository;

import dashboard_builder.entity.WidgetConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WidgetConfigRepository extends JpaRepository<WidgetConfig, Long> {
}