package com.BeSpoke.service;

import com.BeSpoke.dto.PlatformOptionDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The two pure pieces of the WhatsApp path: turning a stored phone number into what
 * Gupshup wants, and choosing the bot's reply. Everything else in the service is an
 * HTTP call, which is the network's business, not a test's.
 */
class WhatsAppServiceTest {

    private static PlatformOptionDto rule(String keywords, String reply) {
        return new PlatformOptionDto(null, WhatsAppService.BOT_LIST, keywords, reply, null, 0, true);
    }

    private static final List<PlatformOptionDto> RULES = List.of(
            rule("quote,price", "Your quote is in your BeSpoke account."),
            rule("designer", "Your designer is assigned the same day."),
            rule("*", "Please call us on 6387427935."));

    @Test
    void numbersBecomeWhatGupshupWants() {
        // What the signup form stores today: ten digits, nothing else.
        assertThat(WhatsAppService.msisdn("9810012345", "91")).isEqualTo("919810012345");
        // Older accounts, typed however the customer typed them.
        assertThat(WhatsAppService.msisdn("+91 98100 12345", "91")).isEqualTo("919810012345");
        assertThat(WhatsAppService.msisdn("09810012345", "91")).isEqualTo("919810012345");
        // Already in full form, and left alone.
        assertThat(WhatsAppService.msisdn("919810012345", "91")).isEqualTo("919810012345");
        // Nothing usable is better than a wrong destination.
        assertThat(WhatsAppService.msisdn("12345", "91")).isNull();
        assertThat(WhatsAppService.msisdn(null, "91")).isNull();
        assertThat(WhatsAppService.msisdn("", "91")).isNull();
    }

    @Test
    void theBotAnswersOnKeywords() {
        assertThat(WhatsAppService.replyFor("what is the PRICE of this?", RULES))
                .isEqualTo("Your quote is in your BeSpoke account.");
        assertThat(WhatsAppService.replyFor("who is my designer", RULES))
                .isEqualTo("Your designer is assigned the same day.");
        // Earlier rules win, so the book reads top to bottom.
        assertThat(WhatsAppService.replyFor("designer quote", RULES))
                .isEqualTo("Your quote is in your BeSpoke account.");
    }

    @Test
    void anythingElseHandsOverToAHuman() {
        // The whole point of the fallback: a question the bot cannot place still gets
        // the number to call, rather than silence.
        assertThat(WhatsAppService.replyFor("can you send someone on Sunday?", RULES))
                .isEqualTo("Please call us on 6387427935.");
        assertThat(WhatsAppService.replyFor("", RULES)).isEqualTo("Please call us on 6387427935.");
        assertThat(WhatsAppService.replyFor(null, RULES)).isEqualTo("Please call us on 6387427935.");
    }

    @Test
    void noRulesAtAllMeansNoReply() {
        // An admin who empties the list turns the bot off; it must not invent a reply.
        assertThat(WhatsAppService.replyFor("hello", List.of())).isNull();
    }
}
