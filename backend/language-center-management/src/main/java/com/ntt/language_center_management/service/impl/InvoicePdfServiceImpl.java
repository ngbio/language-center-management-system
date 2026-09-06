package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.response.InvoiceResponse;
import com.ntt.language_center_management.dto.response.PaymentResponse;
import com.ntt.language_center_management.dto.response.RefundResponse;
import com.ntt.language_center_management.service.BillingService;
import com.ntt.language_center_management.service.InvoicePdfService;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoicePdfServiceImpl implements InvoicePdfService {
  private static final float MARGIN = 48;
  private static final float LINE_HEIGHT = 18;
  private final BillingService billingService;
  @Value("${invoice.pdf.font-path}") private String fontPath;

  public InvoicePdfServiceImpl(BillingService billingService) { this.billingService = billingService; }

  @Override
  @Transactional(readOnly = true)
  public byte[] createInvoicePdf(Integer enrollmentId, Principal principal) {
    InvoiceResponse invoice = billingService.getInvoice(enrollmentId, principal);
    Path path = Path.of(fontPath);
    if (!Files.isRegularFile(path)) {
      throw new IllegalStateException("Không tìm thấy font Unicode xuất PDF: " + fontPath);
    }
    try (PDDocument document = new PDDocument();
         var fontStream = Files.newInputStream(path);
         ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      PDType0Font font = PDType0Font.load(document, fontStream);
      PageWriter writer = new PageWriter(document, font);
      writer.title("TRUNG TÂM NGOẠI NGỮ", "HÓA ĐƠN HỌC PHÍ");
      writer.text("Số hóa đơn: " + invoice.invoiceNumber(), 11);
      writer.text("Ngày lập: " + date(invoice.issuedAt()), 11);
      writer.separator();
      writer.heading("Thông tin học viên");
      writer.text("Họ tên: " + invoice.studentName(), 11);
      writer.text("Mã học viên: " + invoice.studentCode(), 11);
      writer.text("Email: " + invoice.studentEmail(), 11);
      writer.heading("Thông tin khóa học");
      writer.text("Khóa học: " + invoice.courseName() + " (" + invoice.courseCode() + ")", 11);
      writer.text("Lớp: " + invoice.className() + " (" + invoice.classCode() + ")", 11);
      writer.heading("Tổng tiền");
      writer.text("Học phí: " + money(invoice.tuitionAmount()), 12);
      writer.text("Đã thanh toán: " + money(invoice.paidAmount()), 12);
      writer.text("Đã hoàn: " + money(invoice.refundedAmount()), 12);
      writer.text("Thực thu: " + money(invoice.netPaidAmount()), 13);
      writer.text("Trạng thái: " + invoice.enrollmentStatus() + " / " + invoice.paymentStatus(), 11);
      writer.heading("Lịch sử thanh toán");
      if (invoice.payments().isEmpty()) writer.text("Chưa có giao dịch.", 10);
      for (PaymentResponse payment : invoice.payments()) {
        writer.text(payment.transactionCode() + " | " + payment.method() + " | "
            + money(payment.amount()) + " | " + payment.status() + " | " + date(payment.completedAt()), 9);
      }
      if (!invoice.refunds().isEmpty()) {
        writer.heading("Lịch sử hoàn tiền");
        for (RefundResponse refund : invoice.refunds()) {
          writer.text(refund.refundCode() + " | " + money(refund.amount()) + " | "
              + refund.status() + " | " + refund.reason(), 9);
        }
      }
      writer.finish();
      document.save(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("Không thể tạo hóa đơn PDF", exception);
    }
  }

  private static String money(java.math.BigDecimal value) {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")).format(value);
  }
  private static String date(java.util.Date value) {
    return value == null ? "-" : new SimpleDateFormat("dd/MM/yyyy HH:mm").format(value);
  }

  private static final class PageWriter {
    private final PDDocument document;
    private final PDType0Font font;
    private PDPageContentStream content;
    private float y;
    PageWriter(PDDocument document, PDType0Font font) throws IOException {
      this.document = document; this.font = font; newPage();
    }
    void newPage() throws IOException {
      if (content != null) content.close();
      PDPage page = new PDPage(PDRectangle.A4); document.addPage(page);
      content = new PDPageContentStream(document, page); y = page.getMediaBox().getHeight() - MARGIN;
    }
    void ensure(float height) throws IOException { if (y - height < MARGIN) newPage(); }
    void title(String organization, String title) throws IOException {
      content.setNonStrokingColor(new Color(20, 72, 92)); text(organization, 13);
      content.setNonStrokingColor(new Color(18, 35, 48)); text(title, 22); y -= 4;
    }
    void heading(String value) throws IOException { ensure(34); y -= 9; content.setNonStrokingColor(new Color(20, 72, 92)); text(value, 13); content.setNonStrokingColor(Color.DARK_GRAY); }
    void separator() throws IOException { ensure(14); content.setStrokingColor(new Color(205, 216, 222)); content.moveTo(MARGIN, y); content.lineTo(PDRectangle.A4.getWidth() - MARGIN, y); content.stroke(); y -= 10; }
    void text(String value, float size) throws IOException {
      ensure(LINE_HEIGHT); content.beginText(); content.setFont(font, size); content.newLineAtOffset(MARGIN, y);
      content.showText(fit(value, size)); content.endText(); y -= LINE_HEIGHT;
    }
    String fit(String value, float size) throws IOException {
      String text = value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
      float max = PDRectangle.A4.getWidth() - 2 * MARGIN;
      while (text.length() > 3 && font.getStringWidth(text) / 1000 * size > max) text = text.substring(0, text.length() - 1);
      return text.equals(value) ? text : text + "...";
    }
    void finish() throws IOException { if (content != null) { content.close(); content = null; } }
  }
}
