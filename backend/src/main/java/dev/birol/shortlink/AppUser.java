package dev.birol.shortlink;

import jakarta.persistence.*;

@Entity
@Table(name = "app_users")
class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, unique = true, length = 254)
    String email;
    @Column(nullable = false)
    String passwordHash;

    protected AppUser() {}
    AppUser(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }
}
