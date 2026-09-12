package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.event.AccountCreatedMailEvent;
import com.ntt.language_center_management.event.ClassOpenedMailEvent;
import com.ntt.language_center_management.event.PaymentSucceededMailEvent;
import com.ntt.language_center_management.event.PasswordResetRequestedMailEvent;
import com.ntt.language_center_management.event.PasswordResetCompletedMailEvent;
import com.ntt.language_center_management.repository.UserRepository;
import com.ntt.language_center_management.service.MailGateway;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class MailNotificationEventListener {
  private static final Logger log = LoggerFactory.getLogger(MailNotificationEventListener.class);
  private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");

  private final MailGateway mailGateway;
  private final UserRepository users;
  private final int studentBatchSize;
  private final ZoneId applicationZone;

  public MailNotificationEventListener(
      MailGateway mailGateway,
      UserRepository users,
      @Value("${app.mail.student-batch-size:200}") int studentBatchSize,
      @Value("${app.time-zone:Asia/Ho_Chi_Minh}") String applicationTimeZone) {
    this.mailGateway = mailGateway;
    this.users = users;
    this.studentBatchSize = Math.max(1, Math.min(studentBatchSize, 1000));
    this.applicationZone = ZoneId.of(applicationTimeZone);
  }

  @Async("mailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void accountCreated(AccountCreatedMailEvent event) {
    send(event.email(), "Chào mừng bạn đến với Lingua Center",
        "Xin chào " + displayName(event.fullName()) + ",\n\n"
            + "Tài khoản học viên của bạn đã được tạo thành công. "
            + "Bạn có thể đăng nhập để xem khóa học và đăng ký lớp.\n\n"
            + "Trân trọng,\nLingua Center");
  }

  @Async("mailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void paymentSucceeded(PaymentSucceededMailEvent event) {
    send(event.email(), "Xác nhận thanh toán học phí thành công",
        "Xin chào " + displayName(event.fullName()) + ",\n\n"
            + "Hệ thống đã ghi nhận thanh toán thành công cho lớp " + event.className() + ".\n"
            + "Mã giao dịch: " + event.transactionCode() + "\n"
            + "Số tiền: " + money(event.amount()) + "\n\n"
            + "Bạn có thể đăng nhập để xem chi tiết và tải hóa đơn.\n\n"
            + "Trân trọng,\nLingua Center");
  }

  @Async("mailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void passwordResetRequested(PasswordResetRequestedMailEvent event) {
    send(event.email(), "Đặt lại mật khẩu Lingua Center",
        "Xin chào " + displayName(event.fullName()) + ",\n\n"
            + "Bạn vừa yêu cầu đặt lại mật khẩu. Mở liên kết sau trong vòng "
            + event.expirationMinutes() + " phút:\n\n" + event.resetUrl() + "\n\n"
            + "Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.\n\n"
            + "Trân trọng,\nLingua Center");
  }

  @Async("mailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void passwordResetCompleted(PasswordResetCompletedMailEvent event) {
    send(event.email(), "Mật khẩu Lingua Center đã được thay đổi",
        "Xin chào " + displayName(event.fullName()) + ",\n\n"
            + "Mật khẩu tài khoản của bạn đã được thay đổi thành công. "
            + "Nếu đây không phải thao tác của bạn, hãy liên hệ trung tâm ngay.\n\n"
            + "Trân trọng,\nLingua Center");
  }

  @Async("mailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void classOpened(ClassOpenedMailEvent event) {
    String startDate = event.startDate().toInstant().atZone(applicationZone).toLocalDate()
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    String body = "Lingua Center vừa mở lớp mới:\n\n"
        + "Mã lớp: " + event.classCode() + "\n"
        + "Tên lớp: " + event.className() + "\n"
        + "Ngày bắt đầu: " + startDate + "\n"
        + "Học phí: " + money(event.tuitionFee()) + "\n\n"
        + "Hãy đăng nhập để xem lịch học, số chỗ còn lại và đăng ký lớp.\n\n"
        + "Trân trọng,\nLingua Center";

    int pageNumber = 0;
    Page<String> page;
    do {
      page = users.findActiveStudentEmails(PageRequest.of(pageNumber, studentBatchSize));
      sendBatch(page.getContent(), "Lớp mới đã mở: " + event.className(), body);
      pageNumber++;
    } while (page.hasNext());
  }

  private void send(String recipient, String subject, String body) {
    if (!StringUtils.hasText(recipient)) return;
    try {
      mailGateway.send(recipient, subject, body);
    } catch (RuntimeException exception) {
      log.error("Could not send '{}' email to {}: {}", subject, recipient,
          exception.getMessage());
    }
  }

  private void sendBatch(List<String> recipients, String subject, String body) {
    List<String> validRecipients = recipients.stream().filter(StringUtils::hasText).toList();
    if (validRecipients.isEmpty()) return;
    try {
      mailGateway.sendBatch(validRecipients, subject, body);
    } catch (RuntimeException exception) {
      log.error("Could not send '{}' email batch ({} recipients): {}", subject,
          validRecipients.size(), exception.getMessage());
    }
  }

  private String displayName(String fullName) {
    return StringUtils.hasText(fullName) ? fullName.trim() : "bạn";
  }

  private String money(BigDecimal amount) {
    return NumberFormat.getCurrencyInstance(VIETNAMESE).format(amount);
  }
}
