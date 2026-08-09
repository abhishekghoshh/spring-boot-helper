package com.commercemesh.notification.service;

import com.commercemesh.notification.enumeration.NotificationType;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TemplateService {

    private static final Map<NotificationType, String> EMAIL_TEMPLATES = Map.of(
            NotificationType.ORDER_CONFIRMED, """
                    <html>
                    <body>
                    <h2>Order Confirmed - {{orderNumber}}</h2>
                    <p>Dear {{customerName}},</p>
                    <p>Your order <strong>{{orderNumber}}</strong> has been confirmed.</p>
                    <p>Order Total: <strong>${{totalAmount}}</strong></p>
                    <p>We will notify you when your order ships.</p>
                    <p>Thank you for shopping with CommerceMesh!</p>
                    </body>
                    </html>""",

            NotificationType.ORDER_SHIPPED, """
                    <html>
                    <body>
                    <h2>Your Order Has Shipped! - {{orderNumber}}</h2>
                    <p>Dear {{customerName}},</p>
                    <p>Your order <strong>{{orderNumber}}</strong> is on its way!</p>
                    <p>Tracking Number: <strong>{{trackingNumber}}</strong></p>
                    <p>Carrier: <strong>{{carrier}}</strong></p>
                    <p>Estimated Delivery: <strong>{{estimatedDelivery}}</strong></p>
                    <p>Thank you for shopping with CommerceMesh!</p>
                    </body>
                    </html>""",

            NotificationType.ORDER_DELIVERED, """
                    <html>
                    <body>
                    <h2>Order Delivered - {{orderNumber}}</h2>
                    <p>Dear {{customerName}},</p>
                    <p>Your order <strong>{{orderNumber}}</strong> has been delivered.</p>
                    <p>We hope you enjoy your purchase!</p>
                    <p>If you have any issues, please contact our support team.</p>
                    <p>Thank you for shopping with CommerceMesh!</p>
                    </body>
                    </html>""",

            NotificationType.ORDER_CANCELLED, """
                    <html>
                    <body>
                    <h2>Order Cancelled - {{orderNumber}}</h2>
                    <p>Dear {{customerName}},</p>
                    <p>Your order <strong>{{orderNumber}}</strong> has been cancelled.</p>
                    <p>Reason: {{cancellationReason}}</p>
                    <p>If you have questions, please contact our support team.</p>
                    </body>
                    </html>""",

            NotificationType.PAYMENT_RECEIVED, """
                    <html>
                    <body>
                    <h2>Payment Received - {{orderNumber}}</h2>
                    <p>Dear {{customerName}},</p>
                    <p>We have received your payment of <strong>${{amount}}</strong> for order <strong>{{orderNumber}}</strong>.</p>
                    <p>Payment Method: {{paymentMethod}}</p>
                    <p>Payment ID: {{paymentId}}</p>
                    <p>Thank you for your purchase!</p>
                    </body>
                    </html>""",

            NotificationType.REFUND_PROCESSED, """
                    <html>
                    <body>
                    <h2>Refund Processed - {{orderNumber}}</h2>
                    <p>Dear {{customerName}},</p>
                    <p>A refund of <strong>${{refundAmount}}</strong> has been processed for order <strong>{{orderNumber}}</strong>.</p>
                    <p>Reason: {{refundReason}}</p>
                    <p>Refund ID: {{refundId}}</p>
                    <p>The amount will appear in your account within 5-10 business days.</p>
                    </body>
                    </html>""",

            NotificationType.PASSWORD_RESET, """
                    <html>
                    <body>
                    <h2>Password Reset Request</h2>
                    <p>Dear {{customerName}},</p>
                    <p>You have requested a password reset for your account.</p>
                    <p>Please click the link below to reset your password:</p>
                    <p><a href="{{resetLink}}">Reset Password</a></p>
                    <p>This link will expire in 15 minutes.</p>
                    <p>If you did not request this, please ignore this email.</p>
                    </body>
                    </html>""",

            NotificationType.EMAIL_VERIFICATION, """
                    <html>
                    <body>
                    <h2>Email Verification</h2>
                    <p>Dear {{customerName}},</p>
                    <p>Thank you for registering with CommerceMesh!</p>
                    <p>Please verify your email address by clicking the link below:</p>
                    <p><a href="{{verificationLink}}">Verify Email</a></p>
                    <p>This link will expire in 24 hours.</p>
                    </body>
                    </html>"""
    );

    private static final Map<NotificationType, String> SUBJECT_TEMPLATES = Map.ofEntries(
            Map.entry(NotificationType.ORDER_CONFIRMED, "Order Confirmed - {{orderNumber}}"),
            Map.entry(NotificationType.ORDER_SHIPPED, "Your Order Has Shipped! - {{orderNumber}}"),
            Map.entry(NotificationType.ORDER_DELIVERED, "Order Delivered - {{orderNumber}}"),
            Map.entry(NotificationType.ORDER_CANCELLED, "Order Cancelled - {{orderNumber}}"),
            Map.entry(NotificationType.PAYMENT_RECEIVED, "Payment Received - {{orderNumber}}"),
            Map.entry(NotificationType.REFUND_PROCESSED, "Refund Processed - {{orderNumber}}"),
            Map.entry(NotificationType.PASSWORD_RESET, "Password Reset Request"),
            Map.entry(NotificationType.EMAIL_VERIFICATION, "Verify Your Email Address")
    );

    public String buildBody(NotificationType type, Map<String, String> variables) {
        String template = EMAIL_TEMPLATES.get(type);
        if (template == null) {
            throw new IllegalArgumentException("No email template found for type: " + type);
        }
        return replaceVariables(template, variables);
    }

    public String buildSubject(NotificationType type, Map<String, String> variables) {
        String template = SUBJECT_TEMPLATES.get(type);
        if (template == null) {
            return type.name().replace("_", " ");
        }
        return replaceVariables(template, variables);
    }

    private String replaceVariables(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}
