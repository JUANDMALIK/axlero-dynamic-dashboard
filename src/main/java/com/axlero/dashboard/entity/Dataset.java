package com.axlero.dashboard.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "uploaded_datasets")
public class Dataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String csvContent;

    @Column(nullable = false)
    private Integer rowCount;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String headersJson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private Dashboard dashboard;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    public Dataset() {}

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getCsvContent() { return csvContent; }
    public void setCsvContent(String csvContent) { this.csvContent = csvContent; }
    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
    public String getHeadersJson() { return headersJson; }
    public void setHeadersJson(String headersJson) { this.headersJson = headersJson; }
    public Dashboard getDashboard() { return dashboard; }
    public void setDashboard(Dashboard dashboard) { this.dashboard = dashboard; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
