package com.ntt.language_center_management.service;

import java.util.List;

/** Sends transactional email without coupling business events to one mail provider. */
public interface MailGateway {

  void send(String recipient, String subject, String body);

  void sendBatch(List<String> recipients, String subject, String body);
}
