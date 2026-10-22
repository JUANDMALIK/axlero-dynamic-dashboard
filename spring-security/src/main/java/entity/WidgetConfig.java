
package entity;

import jakarta.persistence.*;

@Entity
@Table(name = "widget_configs")
public class WidgetConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String type;

    @Column(name = "data_key", nullable = false)
    private String dataKey;

    public WidgetConfig() {
    }

    public WidgetConfig(String title, String type, String dataKey) {
        this.title = title;
        this.type = type;
        this.dataKey = dataKey;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDataKey() {
        return dataKey;
    }

    public void setDataKey(String dataKey) {
        this.dataKey = dataKey;
    }
}
