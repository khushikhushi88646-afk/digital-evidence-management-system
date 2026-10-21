package com.avengers.evidence.service;

/** Result wrapper that tells the UI where the data came from (REDIS or DATABASE) and how long it took. */
public record Timed<T>(T data, String source, double timeMs) { }
