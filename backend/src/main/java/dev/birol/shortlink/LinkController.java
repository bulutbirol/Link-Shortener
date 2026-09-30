package dev.birol.shortlink;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/links")
class LinkController {
    record CreateLink(@NotBlank String url) {}
    record LinkResponse(String code, String shortUrl, String url, boolean active, long clicks, Instant createdAt) {}

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final SecureRandom random = new SecureRandom();
    private final ShortLinkRepository links;
    private final AppUserRepository users;
    private final TokenService tokens;
    private final SimpleRateLimiter limiter;
    private final String shortBaseUrl;

    LinkController(ShortLinkRepository links, AppUserRepository users, TokenService tokens,
                   SimpleRateLimiter limiter, @Value("${app.short-base-url}") String shortBaseUrl) {
        this.links = links;
        this.users = users;
        this.tokens = tokens;
        this.limiter = limiter;
        this.shortBaseUrl = shortBaseUrl.replaceAll("/+$", "");
    }

    @GetMapping
    List<LinkResponse> list(@RequestHeader(value = "Authorization", required = false) String auth) {
        Long ownerId = currentUser(auth);
        return links.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(this::response).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    LinkResponse create(@RequestHeader(value = "Authorization", required = false) String auth,
                        @Valid @RequestBody CreateLink body) {
        Long ownerId = currentUser(auth);
        if (!limiter.allow("create:" + ownerId, 10, 3600))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Try again later");
        if (links.countByOwnerId(ownerId) >= 20)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "20 link limit reached");
        String target = LinkTargetPolicy.normalize(body.url());
        if (target.startsWith(shortBaseUrl + "/"))
            throw new IllegalArgumentException("A short link cannot point to itself");
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = nextCode();
            if (!links.existsByCode(code)) return response(links.save(new ShortLink(ownerId, code, target)));
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not generate a unique link");
    }

    @PatchMapping("/{code}/disable")
    @Transactional
    LinkResponse disable(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable String code) {
        Long ownerId = currentUser(auth);
        ShortLink link = links.findByOwnerIdAndCode(ownerId, code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        link.active = false;
        return response(link);
    }

    private Long currentUser(String auth) {
        Long id = tokens.read(auth);
        if (!users.existsById(id)) throw new UnauthorizedException();
        return id;
    }

    private String nextCode() {
        StringBuilder code = new StringBuilder(7);
        for (int i = 0; i < 7; i++) code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return code.toString();
    }

    private LinkResponse response(ShortLink link) {
        return new LinkResponse(link.code, shortBaseUrl + "/" + link.code, link.targetUrl,
                link.active, link.clickCount, link.createdAt);
    }
}
