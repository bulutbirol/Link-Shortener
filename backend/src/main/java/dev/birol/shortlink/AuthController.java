package dev.birol.shortlink;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
class AuthController {
    record Credentials(@NotBlank @Email String email, @NotBlank @Size(min = 10, max = 72) String password) {}
    record AuthResponse(String token, String email) {}

    private final AppUserRepository users;
    private final BCryptPasswordEncoder passwords;
    private final TokenService tokens;
    private final SimpleRateLimiter limiter;

    AuthController(AppUserRepository users, BCryptPasswordEncoder passwords, TokenService tokens, SimpleRateLimiter limiter) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        this.limiter = limiter;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResponse register(@Valid @RequestBody Credentials body, HttpServletRequest request) {
        if (!limiter.allow("register:" + request.getRemoteAddr(), 5, 3600))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many registrations");
        String email = body.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        try {
            AppUser user = users.saveAndFlush(new AppUser(email, passwords.encode(body.password())));
            return new AuthResponse(tokens.issue(user.id), user.email);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody Credentials body, HttpServletRequest request) {
        if (!limiter.allow("login:" + request.getRemoteAddr(), 20, 900))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Try again later");
        AppUser user = users.findByEmail(body.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(UnauthorizedException::new);
        if (!passwords.matches(body.password(), user.passwordHash)) throw new UnauthorizedException();
        return new AuthResponse(tokens.issue(user.id), user.email);
    }
}
