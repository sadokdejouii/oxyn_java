package org.example.windowshello;

public record WindowsHelloResult(
        boolean ok,
        String sid,
        String availability,
        String result,
        String error
) {
}

