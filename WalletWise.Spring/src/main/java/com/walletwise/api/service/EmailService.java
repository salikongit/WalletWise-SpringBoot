//package com.walletwise.api.service;
//
//import com.amazonaws.regions.Regions;
//import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
//import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
//import com.amazonaws.services.simpleemail.model.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.util.Collections;
//
//@Service
//public class EmailService {
//
//    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
//
//    @Value("${aws.ses.from-email}")
//    private String fromEmail;
//
//    // Autowire or instantiate client.
//    // Since we used Env vars for AWS keys, the default client builder might find them
//    // if valid in ENV, or we might need BasicAWSCredentials.
//    // Assuming standard SDK behavior (Env vars AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY).
//    private final AmazonSimpleEmailService sesClient;
//
//    public EmailService() {
//        // We can optimize this to be a Bean, but for simplicity:
//        this.sesClient = AmazonSimpleEmailServiceClientBuilder.standard()
//                .withRegion(Regions.AP_SOUTH_1)
//                .build();
//    }
//
//    public boolean sendOtpEmail(String toEmail, String otpCode) {
//        try {
//            String subject = "Your WalletWise OTP Code";
//            String htmlBody = "<html>" +
//                    "<body style='font-family: Arial, sans-serif; padding: 20px;'>" +
//                    "<h2 style='color: #4CAF50;'>WalletWise OTP Verification</h2>" +
//                    "<p>Your OTP code is:</p>" +
//                    "<h1 style='color: #2196F3; font-size: 32px; letter-spacing: 5px;'>" + otpCode + "</h1>" +
//                    "<p>This code will expire in 5 minutes.</p>" +
//                    "<hr>" +
//                    "<p style='color: #666; font-size: 12px;'>© WalletWise</p>" +
//                    "</body>" +
//                    "</html>";
//
//            SendEmailRequest request = new SendEmailRequest()
//                    .withDestination(new Destination().withToAddresses(toEmail))
//                    .withMessage(new Message()
//                            .withBody(new Body()
//                                    .withHtml(new Content().withCharset("UTF-8").withData(htmlBody))
//                                    .withText(new Content().withCharset("UTF-8").withData("Your OTP is " + otpCode)))
//                            .withSubject(new Content().withCharset("UTF-8").withData(subject)))
//                    .withSource(fromEmail);
//
//            sesClient.sendEmail(request);
//            logger.info("OTP email sent successfully to {}", toEmail);
//            return true;
//        } catch (Exception ex) {
//            logger.error("Failed to send OTP email to {}: {}", toEmail, ex.getMessage());
//            // Fallback logging as requested by user
//            logger.warn("FALLBACK: OTP for {}: {}", toEmail, otpCode);
//            return false;
//        }
//    }
//}


//gmail SMPT

package com.walletwise.api.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${SENDGRID_API_KEY}")
    private String sendGridApiKey;

    @Value("${EMAIL_FROM}")
    private String fromEmail;

    public boolean sendOtpEmail(String toEmail, String otpCode) {

        try {
            Email from = new Email(fromEmail);
            Email to = new Email(toEmail);

            String subject = "WalletWise OTP Verification";

            Content content = new Content("text/html", buildOtpHtml(otpCode));

            Mail mail = new Mail(from, subject, to, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() == 202) {
                logger.info("OTP email sent successfully to {}", toEmail);
                return true;
            } else {
                logger.error("SendGrid failed: Status {}, Body {}", response.getStatusCode(), response.getBody());
                logger.warn("FALLBACK OTP for {} : {}", toEmail, otpCode);
                return false;
            }

        } catch (Exception ex) {
            logger.error("Failed to send OTP email to {}: {}", toEmail, ex.getMessage());

            // 🔥 fallback OTP always visible
            logger.warn("FALLBACK OTP for {} : {}", toEmail, otpCode);

            return false;
        }
    }

    // 🔥 HTML TEMPLATE
    private String buildOtpHtml(String otpCode) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<body style='font-family: Arial, sans-serif; background-color:#f4f6f8; padding:20px;'>" +

                "<div style='max-width:500px; margin:auto; background:white; padding:30px; border-radius:10px; text-align:center;'>" +

                "<h2 style='color:#2c3e50;'>WalletWise</h2>" +

                "<p style='font-size:16px;'>Your One-Time Password (OTP)</p>" +

                "<div style='font-size:32px; font-weight:bold; letter-spacing:8px; color:#2ecc71; margin:20px 0;'>" +
                otpCode +
                "</div>" +

                "<p style='color:#555;'>This OTP will expire in <b>5 minutes</b>.</p>" +

                "<p style='color:#999; font-size:12px;'>Do not share this code with anyone.</p>" +

                "<hr style='margin:20px 0;'/>" +

                "<p style='font-size:12px; color:#aaa;'>© 2026 WalletWise. All rights reserved.</p>" +

                "</div>" +
                "</body>" +
                "</html>";
    }
}