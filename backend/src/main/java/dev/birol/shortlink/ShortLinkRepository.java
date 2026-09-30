package dev.birol.shortlink;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {
    List<ShortLink> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    Optional<ShortLink> findByOwnerIdAndCode(Long ownerId, String code);
    boolean existsByCode(String code);
    long countByOwnerId(Long ownerId);
}
