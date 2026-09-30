package dev.birol.shortlink;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "short_links", indexes = @Index(name = "idx_short_links_owner", columnList = "owner_id"))
class ShortLink {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "owner_id", nullable = false)
    Long ownerId;
    @Column(nullable = false, unique = true, length = 12)
    String code;
    @Column(name = "target_url", nullable = false, length = 2048)
    String targetUrl;
    @Column(nullable = false)
    boolean active = true;
    @Column(name = "click_count", nullable = false)
    long clickCount = 0;
    @Column(name = "created_at", nullable = false)
    Instant createdAt = Instant.now();

    protected ShortLink() {}
    ShortLink(Long ownerId, String code, String targetUrl) {
        this.ownerId = ownerId;
        this.code = code;
        this.targetUrl = targetUrl;
    }
}
