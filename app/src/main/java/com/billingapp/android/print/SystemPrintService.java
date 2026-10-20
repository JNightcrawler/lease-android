package com.billingapp.android.print;

import android.content.Context;
import android.os.Build;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;

import com.billingapp.android.data.entity.Bill;
import com.billingapp.android.data.entity.BillItem;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Renders the bill as HTML in an invisible WebView, then hands it to Android's
 * system Print Framework - the same "Print" dialog used by any app. Works with
 * any printer already set up on the device (WiFi, "Save as PDF", etc.), and needs
 * no pairing step, unlike the Bluetooth thermal path.
 */
@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class SystemPrintService implements PrinterService {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    @Override
    public void printBill(Context context, Bill bill, List<BillItem> items, PrintCallback callback) {
        String html = buildHtml(bill, items);

        WebView webView = new WebView(context);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                try {
                    PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);
                    if (printManager == null) {
                        callback.onError("Printing isn't available on this device");
                        return;
                    }

                    android.print.PrintDocumentAdapter adapter = view.createPrintDocumentAdapter(bill.displayNumber());
                    PrintAttributes attributes = new PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A5)
                            .setResolution(new PrintAttributes.Resolution("default", "default", 300, 300))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build();

                    printManager.print(bill.displayNumber(), adapter, attributes);
                    callback.onSuccess();
                } catch (Exception e) {
                    callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to open print dialog");
                }
            }
        });

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private String buildHtml(Bill bill, List<BillItem> items) {
        StringBuilder rows = new StringBuilder();
        for (BillItem item : items) {
            rows.append("<tr>")
                    .append("<td>").append(escape(item.productName)).append("</td>")
                    .append("<td style='text-align:center'>").append(item.quantity).append(" ").append(escape(item.unit)).append("</td>")
                    .append("<td style='text-align:right'>").append(String.format(Locale.getDefault(), "%.2f", item.unitPrice)).append("</td>")
                    .append("<td style='text-align:right'>").append(String.format(Locale.getDefault(), "%.2f", item.lineTotal)).append("</td>")
                    .append("</tr>");
        }

        String customerBlock = "";
        if (bill.customerName != null || bill.customerPhone != null) {
            customerBlock = "<p>"
                    + (bill.customerName != null ? "Customer: " + escape(bill.customerName) + "<br/>" : "")
                    + (bill.customerPhone != null ? "Phone: " + escape(bill.customerPhone) : "")
                    + "</p>";
        }

        return "<html><head><style>"
                + "body{font-family:sans-serif;padding:24px;}"
                + "h1{margin-bottom:0;}"
                + "table{width:100%;border-collapse:collapse;margin-top:16px;}"
                + "th,td{padding:6px;border-bottom:1px solid #ddd;text-align:left;}"
                + ".total{font-weight:bold;font-size:1.2em;text-align:right;margin-top:12px;}"
                + "</style></head><body>"
                + "<h1>RECEIPT</h1>"
                + "<p>" + escape(bill.displayNumber()) + "<br/>" + DATE_FORMAT.format(new java.util.Date(bill.createdAt)) + "</p>"
                + customerBlock
                + "<table><thead><tr><th>Item</th><th>Qty</th><th>Price</th><th>Total</th></tr></thead>"
                + "<tbody>" + rows + "</tbody></table>"
                + "<div class='total'>Total: " + String.format(Locale.getDefault(), "%.2f", bill.totalAmount) + "</div>"
                + "<p style='margin-top:24px;text-align:center;color:#888;'>Thank you!</p>"
                + "</body></html>";
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
