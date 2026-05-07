package ru.usernamedrew.edutaskcore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_model_settings")
public class UserModelSettings extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile user;

    @Column(name = "provider_type", nullable = false, length = 50)
    private String providerType;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "encrypted_api_key", nullable = false, columnDefinition = "text")
    private String encryptedApiKey;

    @Column(precision = 3, scale = 2)
    private BigDecimal temperature;

    @Column(name = "is_default", nullable = false)
    private boolean defaultModel = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

