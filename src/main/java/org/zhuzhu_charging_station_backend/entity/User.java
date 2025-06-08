package org.zhuzhu_charging_station_backend.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.stream.Collectors;

@Data
@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    @GeneratedValue(generator = "manual-id")
    @GenericGenerator(name = "manual-id", strategy = "assigned")
    private Long userId;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String password;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;  // 自动记录创建时间

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;  // 自动记录更新时间

    @Column(nullable = false)
    private String roles;

    @Column(nullable = false)
    private Integer totalChargeCount = 0;    // 累计充电次数

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal totalChargeAmount = BigDecimal.valueOf(0);  // 累计充电电量

    @Column(nullable = false)
    private Long totalChargeDuration = 0L;   // 累计充电时长

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal totalChargeFee = BigDecimal.valueOf(0);   // 累计充电费用

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal totalServiceFee = BigDecimal.valueOf(0);  // 累计服务费用

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal totalFee = BigDecimal.valueOf(0);         // 累计总费用（充电费+服务费）

    // 确保保存时触发时间更新
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // --- 实现 UserDetails 接口的方法 ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Arrays.stream(roles.split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 账户是否未过期
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 账户是否未锁定
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 凭证是否未过期
    }

    @Override
    public boolean isEnabled() {
        return true; // 账户是否启用
    }
}