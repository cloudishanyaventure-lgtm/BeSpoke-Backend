package com.BeSpoke.service;

import com.BeSpoke.entity.PolicyDocument;

import java.util.List;

/**
 * The legal documents BeSpoke ships with. Seeded once per slug — after that the
 * copy belongs to whoever edits it at /admin/policies, and this file is ignored.
 */
final class PolicyDefaults {

    private PolicyDefaults() {
    }

    private static final String CANCELLATION_BODY = """
            This policy is effective from 20 August 2026. For the purposes of clarity, this \
            cancellation policy shall apply only to bookings made on or after 20 August 2026.

            At BeSpoke, we are committed to delivering exceptional service experiences. As our \
            services are personalised and executed with care, we follow a defined cancellation \
            and refund framework.

            We want to make sure you understand your options if your plans change. Refund \
            eligibility is determined by your project's stage and progress.

            ## Refund eligibility

            | Timeline | Refund policy |
            | Within 72 hours | 90% refund available. Refund requests must be raised on your BeSpoke account within 72 hours of paying the booking amount. |
            | Post-72 hours | Refund available subject to the assessment done by the BeSpoke team only, in the form of a BeSpoke Credit Coupon redeemable on the BeSpoke Home website, for BeSpoke in-house products and services delivered directly by the BeSpoke team. No monetary or cash refunds will be granted for refund requests submitted beyond the stipulated 72-hour window. |
            | After / on site measurement | No refund or credit available, as project execution would have already commenced. |

            For cancellation requests raised after 72 hours of the booking payment and before \
            site measurement is completed, customers will not be eligible for a cash refund. \
            Instead, BeSpoke may issue a BeSpoke Credit Coupon ("Coupon"), redeemable on the \
            BeSpoke Home website, equivalent to the amount paid.

            Once the site measurement has been completed, cancellation requests will not be \
            eligible for a refund or store credit, as project execution would have already \
            commenced.

            No monetary refunds will be granted for requests submitted beyond the stipulated \
            72-hour window.

            ## General terms and conditions applicable to refunds

            ### 1. Eligibility

            Coupons are only issued to eligible customers for a cancelled booking.

            ### 2. Value of coupon

            The value of the Coupon shall be equivalent to a maximum of 90% of the amount paid \
            by the customer for the cancelled booking.

            ### 3. Issuance

            The Coupon equivalent to the refund amount will be issued to the customer's \
            registered email address.

            ### 4. Redemption

            - Coupons can only be used on https://www.bespokedesign.in for purchasing listed products. Service fees, delivery charges, or third-party products (if any) may be excluded.
            - The Coupon can only be redeemed by logging in with the customer's registered email address to which it was originally sent.

            ### 5. Other terms of use

            - Coupons are valid for 3 months from the date of issue. They cannot be extended or reissued.
            - Each Coupon is valid for multiple uses until the value is exhausted. Any unused balance after the validity period will automatically expire.
            - The Coupon cannot be exchanged, transferred, refunded, or redeemed for cash, bank transfer, or any other form of monetary compensation, either in full or in part.
            - Coupons cannot be combined with other offers, discounts or promotional codes unless explicitly mentioned.
            - Lost or accidentally deleted coupons will not be reissued. Users are advised to retain their coupons.
            - Any attempt to misuse, replicate or tamper with a coupon will lead to immediate cancellation of the coupon and may lead to permanent account suspension. We reserve the right to verify the identity of the user prior to redemption.

            ### 6. How to initiate a cancellation

            Customers can cancel their project directly from their BeSpoke account (if eligible) \
            by following these steps:

            - Log in to https://www.bespokedesign.in using your registered email ID or mobile number and raise a request. If the refund request confirmation email does not arrive within 10 minutes of raising the request, please email us at contact@bespokedesign.in.
            - Alternatively, email us at contact@bespokedesign.in from your registered email ID, with your payment receipts.
            - If your project is eligible, enter the reason for cancellation.
            - Choose the refund mode: bank transfer, refund to source, or gift voucher.
            - Enter the required information as prompted (such as bank account details for refunds via bank transfer).
            - Verify the cancellation by entering the OTP sent to both your registered email and mobile number.

            After submission:

            - Your cancellation request will appear under Need Help → Current Issues.
            - You can open the issue to track its status and details.

            ### Questions

            Email contact@bespokedesign.in and the BeSpoke team will help.
            """;

    static final List<PolicyDocument> DOCUMENTS = List.of(
            new PolicyDocument(
                    "cancellation-refund",
                    "Cancellation & Refund Policy",
                    "How cancellations, refunds and BeSpoke Credit Coupons work for projects "
                            + "managed by the BeSpoke team.",
                    "20 August 2026",
                    CANCELLATION_BODY,
                    null,
                    0),
            new PolicyDocument(
                    "terms-of-use",
                    "Terms of Use",
                    "The rules for using the BeSpoke website and services.",
                    null,
                    null,
                    "/terms",
                    1),
            new PolicyDocument(
                    "privacy-policy",
                    "Privacy Policy",
                    "What personal data BeSpoke collects, how it is used and shared, and the "
                            + "rights you have over it under the DPDP Act.",
                    "18 August 2026",
                    null,
                    "/privacy",
                    2));
}
