package dev.birol.shortlink;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LinkFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AppUserRepository users;
    @Autowired ShortLinkRepository links;

    @Test
    void userCanCreateListAndDisableOwnLink() throws Exception {
        String register = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"birol@example.com\",\"password\":\"safe-password-123\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(register).get("token").asText();

        String created = mvc.perform(post("/api/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com/article\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode link = json.readTree(created);
        String code = link.get("code").asText();

        mvc.perform(get("/api/links").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value(code))
                .andExpect(jsonPath("$[0].active").value(true));

        mvc.perform(patch("/api/links/" + code + "/disable")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void creatingLinkRequiresLogin() throws Exception {
        mvc.perform(post("/api/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateEmailExplainsTheConflict() throws Exception {
        String body = "{\"email\":\"repeat@example.com\",\"password\":\"safe-password-123\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already in use"));
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void concurrentCreatesCannotPassTwentyLinks() throws Exception {
        String register = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"parallel@example.com\",\"password\":\"safe-password-123\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(register).get("token").asText();
        Long ownerId = users.findByEmail("parallel@example.com").orElseThrow().id;
        for (int i = 0; i < 19; i++) links.save(new ShortLink(ownerId, "Seed%03d".formatted(i), "https://example.com"));

        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        try (var pool = Executors.newFixedThreadPool(10)) {
            for (int i = 0; i < 10; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return mvc.perform(post("/api/links")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"url\":\"https://example.com/new\"}"))
                            .andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            for (Future<Integer> result : results) result.get();
        }
        org.junit.jupiter.api.Assertions.assertEquals(20, links.countByOwnerId(ownerId));
        org.junit.jupiter.api.Assertions.assertEquals(1, results.stream().filter(result -> {
            try { return result.get() == 201; } catch (Exception ex) { throw new RuntimeException(ex); }
        }).count());
    }

    @Test
    void sharedIpCanRegisterSeveralDifferentPeople() throws Exception {
        for (int i = 0; i < 6; i++) {
            mvc.perform(post("/api/auth/register")
                    .with(request -> { request.setRemoteAddr("198.51.100.33"); return request; })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"shared%d@example.com\",\"password\":\"safe-password-123\"}".formatted(i)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void successfulLoginsDoNotExhaustIpLimit() throws Exception {
        String body = "{\"email\":\"often@example.com\",\"password\":\"safe-password-123\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        for (int i = 0; i < 21; i++) {
            mvc.perform(post("/api/auth/login")
                    .with(request -> { request.setRemoteAddr("198.51.100.34"); return request; })
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void repeatedWrongPasswordsAreLimitedPerAccount() throws Exception {
        String wrong = "{\"email\":\"victim@example.com\",\"password\":\"wrong-password-123\"}";
        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/api/auth/login")
                    .with(request -> { request.setRemoteAddr("198.51.100.35"); return request; })
                    .contentType(MediaType.APPLICATION_JSON).content(wrong))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login")
                .with(request -> { request.setRemoteAddr("198.51.100.35"); return request; })
                .contentType(MediaType.APPLICATION_JSON).content(wrong))
                .andExpect(status().isTooManyRequests());
    }
}
