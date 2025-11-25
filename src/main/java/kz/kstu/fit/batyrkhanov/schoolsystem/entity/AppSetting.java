package kz.kstu.fit.batyrkhanov.schoolsystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "app_settings")
public class AppSetting {
    @Id
    @Column(name = "setting_key", length = 128) // изменено с key на setting_key
    private String key;

    @Column(name = "value", length = 1024)
    private String value;

    public AppSetting() {}
    public AppSetting(String key, String value) { this.key = key; this.value = value; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
