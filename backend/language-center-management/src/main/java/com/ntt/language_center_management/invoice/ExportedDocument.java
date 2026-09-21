package com.ntt.language_center_management.invoice;

public record ExportedDocument(byte[] content, String contentType, String filename) {}
