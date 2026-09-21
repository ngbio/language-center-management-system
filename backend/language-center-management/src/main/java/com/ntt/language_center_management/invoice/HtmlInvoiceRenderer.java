package com.ntt.language_center_management.invoice;

import com.ntt.language_center_management.dto.response.InvoiceResponse;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import org.springframework.web.util.HtmlUtils;

/** Plain renderer: all dynamic values are escaped, with no database or HTTP dependencies. */
public final class HtmlInvoiceRenderer implements InvoiceRenderer {
  private final ZoneId zone;

  public HtmlInvoiceRenderer(ZoneId zone) { this.zone = zone; }

  @Override
  public ExportedDocument render(InvoiceResponse invoice) {
    var html = new StringBuilder("""
        <!doctype html>
        <html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Hóa đơn học phí</title>
        <style>
        body{font-family:Arial,sans-serif;max-width:960px;margin:32px auto;padding:0 20px;color:#122330}
        h1,h2{color:#14485c} table{border-collapse:collapse;width:100%;margin:16px 0}
        th,td{text-align:left;padding:10px;border-bottom:1px solid #cdd8de;overflow-wrap:anywhere}
        dl{display:grid;grid-template-columns:180px 1fr;gap:8px}dd{margin:0;overflow-wrap:anywhere}
        @media print{body{margin:0;max-width:none}thead{display:table-header-group}tr{break-inside:avoid}}
        </style></head><body><p>TRUNG TÂM NGOẠI NGỮ</p><h1>HÓA ĐƠN HỌC PHÍ</h1><dl>
        """);
    detail(html, "Số hóa đơn", invoice.invoiceNumber());
    detail(html, "Ngày lập", date(invoice.issuedAt()));
    detail(html, "Họ tên", invoice.studentName());
    detail(html, "Mã học viên", invoice.studentCode());
    detail(html, "Email", invoice.studentEmail());
    detail(html, "Khóa học", invoice.courseName() + " (" + invoice.courseCode() + ")");
    detail(html, "Lớp", invoice.className() + " (" + invoice.classCode() + ")");
    detail(html, "Trạng thái", invoice.enrollmentStatus() + " / " + invoice.paymentStatus());
    html.append("</dl><h2>Tổng tiền</h2><dl>");
    detail(html, "Học phí", money(invoice.tuitionAmount()));
    detail(html, "Đã thanh toán", money(invoice.paidAmount()));
    detail(html, "Đã hoàn", money(invoice.refundedAmount()));
    detail(html, "Thực thu", money(invoice.netPaidAmount()));
    html.append("</dl><h2>Lịch sử thanh toán</h2>");
    if (invoice.payments() == null || invoice.payments().isEmpty()) html.append("<p>Chưa có giao dịch.</p>");
    else {
      html.append("<table><thead><tr><th>Mã giao dịch</th><th>Phương thức</th><th>Số tiền</th><th>Trạng thái</th><th>Hoàn tất</th></tr></thead><tbody>");
      for (var payment : invoice.payments()) row(html, payment.transactionCode(), payment.method(),
          money(payment.amount()), payment.status(), date(payment.completedAt()));
      html.append("</tbody></table>");
    }
    if (invoice.refunds() != null && !invoice.refunds().isEmpty()) {
      html.append("<h2>Lịch sử hoàn tiền</h2><table><thead><tr><th>Mã hoàn tiền</th><th>Số tiền</th><th>Trạng thái</th><th>Lý do</th></tr></thead><tbody>");
      for (var refund : invoice.refunds()) row(html, refund.refundCode(), money(refund.amount()), refund.status(), refund.reason());
      html.append("</tbody></table>");
    }
    html.append("</body></html>");
    return new ExportedDocument(html.toString().getBytes(StandardCharsets.UTF_8), "text/html;charset=UTF-8",
        "invoice-enrollment-" + invoice.enrollmentId() + ".html");
  }

  private static void detail(StringBuilder html, String label, String value) {
    html.append("<dt>").append(escape(label)).append("</dt><dd>").append(escape(value)).append("</dd>");
  }

  private static void row(StringBuilder html, String... values) {
    html.append("<tr>");
    for (String value : values) html.append("<td>").append(escape(value)).append("</td>");
    html.append("</tr>");
  }

  private static String escape(String value) { return HtmlUtils.htmlEscape(value == null ? "-" : value); }
  private static String money(BigDecimal value) {
    return value == null ? "-" : NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")).format(value);
  }
  private String date(Date value) {
    return value == null ? "-" : DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(zone).format(value.toInstant());
  }
}
