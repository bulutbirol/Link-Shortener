package dev.birol.shortlink;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

final class LinkTargetPolicy {
    private LinkTargetPolicy() {}

    static String normalize(String input) {
        if (input == null) throw new IllegalArgumentException("Enter a URL");
        String value = input.trim();
        if (value.length() > 2048) throw new IllegalArgumentException("URL is too long");
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    || host == null || uri.getUserInfo() != null || uri.getPort() < -1 || uri.getPort() > 65535) {
                throw new IllegalArgumentException("Enter a public http or https URL");
            }
            String lowerHost = host.toLowerCase(Locale.ROOT);
            if (!lowerHost.contains(".") || lowerHost.endsWith(".local") || lowerHost.endsWith(".internal")
                    || lowerHost.equals("localhost") || lowerHost.matches("[0-9.]+") || lowerHost.contains(":")) {
                throw new IllegalArgumentException("Local addresses are not allowed");
            }
            return value;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Enter a valid URL", ex);
        }
    }
}
