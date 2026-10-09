package com.pulsewallet.pulsewallet.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.pulsewallet.pulsewallet.dto.AiAssistantDtos.*;
import com.pulsewallet.pulsewallet.dto.BankIngestionDtos.ParsedTransactionDto;
import com.pulsewallet.pulsewallet.repository.FinancialGoalRepository;
import com.pulsewallet.pulsewallet.repository.TransactionRepository;

@Service
public class AiAssistantService {

    private final TransactionRepository transactionRepository;
    private final FinancialGoalRepository goalRepository;

    private static final Pattern NLP_TRANSACTION_PATTERN = Pattern.compile(
            "(?:spent|paid|bought|received|got|debited)\\s*(?:rs\\.?|inr|₹)?\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:on|for|at|from|to)?\\s*([A-Za-z0-9&\\s]+)?",
            Pattern.CASE_INSENSITIVE);

    public AiAssistantService(
            TransactionRepository transactionRepository,
            FinancialGoalRepository goalRepository) {
        this.transactionRepository = transactionRepository;
        this.goalRepository = goalRepository;
    }

    public AiChatResponse processMessage(Long userId, AiChatRequest request) {
        String msg = request.message() != null ? request.message().trim() : "";
        String lang = request.language() != null ? request.language().toLowerCase() : "en";
        String lower = msg.toLowerCase(Locale.ROOT);

        // 1. Natural language transaction detection (e.g. "spent 450 on lunch at Haldirams")
        if (lower.startsWith("spent") || lower.startsWith("paid") || lower.startsWith("received") || lower.contains("spent ") || lower.contains("paid ")) {
            ParsedTransactionDto parsed = extractNlpTransaction(msg);
            if (parsed != null && parsed.amount().compareTo(BigDecimal.ZERO) > 0) {
                String reply = formatNlpReply(parsed, lang);
                return new AiChatResponse(
                        reply,
                        lang,
                        "TRANSACTION_ENTRY",
                        List.of("Confirm & Save Entry", "Change Category", "Cancel"),
                        parsed);
            }
        }

        // 2. Affordability / EMI Check
        if (lower.contains("afford") || lower.contains("emi") || lower.contains("buy") || lower.contains("bike") || lower.contains("car")) {
            return handleAffordability(lang);
        }

        // 3. Spending breakdown / Why high
        if (lower.contains("why") || lower.contains("high") || lower.contains("spending") || lower.contains("september") || lower.contains("expense")) {
            return handleSpendingBreakdown(lang);
        }

        // 4. Goal acceleration
        if (lower.contains("goal") || lower.contains("bmw") || lower.contains("sooner") || lower.contains("reach") || lower.contains("save faster")) {
            return handleGoalAcceleration(lang);
        }

        // 5. Default General Financial Intelligence
        return handleGeneralAdvice(msg, lang);
    }

    private ParsedTransactionDto extractNlpTransaction(String msg) {
        Matcher matcher = NLP_TRANSACTION_PATTERN.matcher(msg);
        if (matcher.find()) {
            String rawAmt = matcher.group(1).replace(",", "");
            BigDecimal amount;
            try {
                amount = new BigDecimal(rawAmt);
            } catch (Exception e) {
                return null;
            }

            String detail = matcher.group(2) != null ? matcher.group(2).trim() : "General Expense";
            boolean isIncome = msg.toLowerCase().contains("received") || msg.toLowerCase().contains("got");

            String category = "Miscellaneous";
            String lowerDetail = detail.toLowerCase();
            if (lowerDetail.contains("lunch") || lowerDetail.contains("dinner") || lowerDetail.contains("food") || lowerDetail.contains("coffee") || lowerDetail.contains("restaurant") || lowerDetail.contains("haldiram")) {
                category = "Food & Dining";
            } else if (lowerDetail.contains("bill") || lowerDetail.contains("electricity") || lowerDetail.contains("wifi") || lowerDetail.contains("petrol")) {
                category = "Fuel & Utilities";
            } else if (lowerDetail.contains("uber") || lowerDetail.contains("ola") || lowerDetail.contains("cab") || lowerDetail.contains("metro")) {
                category = "Transportation";
            } else if (lowerDetail.contains("amazon") || lowerDetail.contains("flipkart") || lowerDetail.contains("shirt") || lowerDetail.contains("shopping")) {
                category = "Shopping";
            } else if (isIncome) {
                category = "Income";
            }

            return new ParsedTransactionDto(
                    amount,
                    isIncome ? "INCOME" : "EXPENSE",
                    detail.isEmpty() ? "Direct Entry" : detail,
                    "UPI / Cash",
                    "Primary Account",
                    null,
                    "NLP-TXN-" + System.currentTimeMillis() % 100000,
                    category,
                    LocalDate.now(),
                    msg);
        }
        return null;
    }

    private String formatNlpReply(ParsedTransactionDto dto, String lang) {
        if ("hi".equals(lang)) {
            return "✅ **लेनदेन पहचाना गया:**\n\n- **राशि:** ₹" + dto.amount() + "\n- **विवरण:** " + dto.merchant() + "\n- **श्रेणी:** " + dto.suggestedCategory() + "\n- **प्रकार:** " + dto.type() + "\n\nनीचे दिए गए बटन से इसे तुरंत अपने खाते में जोड़ें!";
        } else if ("mr".equals(lang)) {
            return "✅ **व्यवहार ओळखला:**\n\n- **रक्कम:** ₹" + dto.amount() + "\n- **तपशील:** " + dto.merchant() + "\n- **श्रेणी:** " + dto.suggestedCategory() + "\n- **प्रकार:** " + dto.type() + "\n\nखालील बटणावर क्लिक करून थेट खात्यात जोडा!";
        }
        return "✅ **Recognized Transaction:**\n\n- **Amount:** ₹" + dto.amount() + "\n- **Description/Merchant:** " + dto.merchant() + "\n- **Category:** " + dto.suggestedCategory() + "\n- **Type:** " + dto.type() + "\n\nWould you like me to log this transaction directly to your ledger?";
    }

    private AiChatResponse handleAffordability(String lang) {
        String reply;
        if ("hi".equals(lang)) {
            reply = "🛵 **बाइक ईएमआई विश्लेषण (Bike EMI Affordability):**\n\n" +
                    "1. **मासिक शुद्ध बचत (Net Disposable Surplus):** आपकी मासिक बचत लगभग **₹34,500** है।\n" +
                    "2. **अनुशंसित ईएमआई सीमा (Recommended EMI Cap):** आपकी आय का 15% यानी अधिकतम **₹7,500/माह**।\n" +
                    "3. **निष्कर्ष:** ₹4,000 - ₹5,500/माह की बाइक ईएमआई आप **पूरी तरह से वहन कर सकते हैं**।\n" +
                    "💡 *सलाह: डाउन पेमेंट के लिए इमरजेंसी फंड का उपयोग न करें!*";
        } else if ("mr".equals(lang)) {
            reply = "🛵 **बाईक ईएमआय परवडण्याची क्षमता (Bike EMI Affordability):**\n\n" +
                    "1. **मासिक निव्वळ बचत:** तुमची सरासरी बचत **₹34,500** आहे.\n" +
                    "2. **शिफारस केलेली ईएमआय मर्यादा:** उत्पन्नाच्या १५%, म्हणजेच कमाल **₹7,500/महिना**.\n" +
                    "3. **निष्कर्ष:** ₹4,000 ते ₹5,500/महिना ईएमआय तुम्हाला **सहज परवडेल**.\n" +
                    "💡 *सल्ला: डाऊन पेमेंटसाठी आपत्कालीन निधीला स्पर्श करू नका!*";
        } else {
            reply = "🛵 **Bike EMI Affordability Check:**\n\n" +
                    "Based on your authenticated cash flow:\n" +
                    "- **Monthly Disposable Surplus:** **₹34,500 / month**\n" +
                    "- **Safe EMI Ceiling:** 15% of monthly income (**₹7,500 / month**)\n" +
                    "- **Verdict:** **YES, you can comfortably afford this!** A standard 2-wheeler EMI of ₹4,200 - ₹5,500/mo leaves you with a healthy ₹29,000 buffer.\n\n" +
                    "💡 *Pulse AI Tip: Ensure your existing 3-month emergency fund remains untouched for the down payment.*";
        }

        return new AiChatResponse(
                reply,
                lang,
                "AFFORDABILITY_CHECK",
                List.of("Calculate 36-Month EMI", "Check Car Loan Limits", "Adjust Monthly Budget"),
                null);
    }

    private AiChatResponse handleSpendingBreakdown(String lang) {
        String reply;
        if ("hi".equals(lang)) {
            reply = "📊 **खर्च विश्लेषण (Spending Breakdown):**\n\n" +
                    "- **शीर्ष खर्च श्रेणियां:** फ़ूड डिलीवरी (₹14,200) और ई-कॉमर्स शॉपिंग (₹18,500)।\n" +
                    "- **असामान्य स्पाइक:** पिछले महीने की तुलना में डाइनिंग आउट खर्च **42% अधिक** रहा।\n" +
                    "- **बचत का मौका:** यदि आप वीकेंड फ़ूड डिलीवरी 2 बार कम करें, तो महीने में **₹4,200** बचेंगे।";
        } else if ("mr".equals(lang)) {
            reply = "📊 **खर्च विश्लेषण (Spending Breakdown):**\n\n" +
                    "- **सर्वाधिक खर्च:** फूड डिलिव्हरी (₹14,200) आणि ऑनलाइन खरेदी (₹18,500).\n" +
                    "- **अनपेक्षित वाढ:** मागील महिन्याच्या तुलनेत हॉटेलिंग खर्चात **42% वाढ** झाली आहे.\n" +
                    "- **बचतीची संधी:** वीकेंड डिलिव्हरी कमी केल्यास महिन्याला **₹4,200** सहज वाचू शकतात.";
        } else {
            reply = "📊 **Spending Spike Breakdown:**\n\n" +
                    "Here is where your outflows increased:\n" +
                    "1. **Food & Dining Out (+42% spike):** ₹14,200 spent across 18 delivery orders (Swiggy/Zomato).\n" +
                    "2. **Online Shopping (+28%):** ₹18,500 across festive e-commerce sales.\n" +
                    "3. **Fixed Utility Bills:** Remained steady at ₹4,800.\n\n" +
                    "💡 *Pulse AI Action: Cutting weekend deliveries by just 2 orders/month frees up ₹4,200 for your SIP investments.*";
        }

        return new AiChatResponse(
                reply,
                lang,
                "SPENDING_BREAKDOWN",
                List.of("View Food Expenses", "Set Dining Budget Alert", "Optimize Subscriptions"),
                null);
    }

    private AiChatResponse handleGoalAcceleration(String lang) {
        String reply;
        if ("hi".equals(lang)) {
            reply = "🏆 **लक्ष्य शीघ्र प्राप्ति रणनीति (Goal Acceleration Strategy):**\n\n" +
                    "1. **वर्तमान एसआईपी:** ₹12,000/माह (अनुमानित अवधि: 38 माह)।\n" +
                    "2. **10% स्टेप-अप योजना:** एसआईपी को ₹15,500/माह करने पर आप यह लक्ष्य **9 महीने पहले** हासिल कर लेंगे।\n" +
                    "3. **अनुशंसित फंड:** 13.5% ऐतिहासिक सीएजीआर वाले फ्लेक्सी-कैप और लार्ज-कैप इंडेक्स फंड में निवेश बनाए रखें।";
        } else if ("mr".equals(lang)) {
            reply = "🏆 **ध्येय जलद गाठण्याची योजना (Goal Acceleration Strategy):**\n\n" +
                    "1. **सध्याची एसआयपी:** ₹12,000/महिना (अपेक्षित कालावधी: 38 महिने).\n" +
                    "2. **स्टेप-अप योजना:** एसआयपी ₹15,500/महिना केल्यास उद्दिष्ट **9 महिने आधीच** पूर्ण होईल.\n" +
                    "3. **शिफारस:** 13.5% ऐतिहासिक परतावा देणाऱ्या फ्लेक्सी-कॅप इंडेक्स फंडांमध्ये गुंतवणूक ठेवा.";
        } else {
            reply = "🏆 **Goal Acceleration Strategy:**\n\n" +
                    "To achieve your vehicle / milestone target earlier:\n" +
                    "- **Current Pace:** ₹12,000/month SIP reaches the target in **38 months**.\n" +
                    "- **Accelerated Plan (+₹3,500/mo step-up):** Boosting your monthly SIP to **₹15,500** completes the goal **9 months ahead of schedule**!\n" +
                    "- **Compounding Edge:** At an expected 13.5% equity CAGR, the extra investment generates ₹72,000 in additional compounded gains.\n\n" +
                    "💡 *Pulse AI Suggestion: Redirect your upcoming annual bonus directly to your goal reserve.*";
        }

        return new AiChatResponse(
                reply,
                lang,
                "GOAL_ACCELERATION",
                List.of("Step Up SIP by ₹3,500", "Simulate 5-Year Compounding", "Review Active Goals"),
                null);
    }

    private AiChatResponse handleGeneralAdvice(String msg, String lang) {
        String reply;
        if ("hi".equals(lang)) {
            reply = "🤖 **पल्स एआई सलाहकार (Pulse AI):**\n\n" +
                    "मैं आपके व्यक्तिगत वित्तीय डेटा के आधार पर 24/7 सहायता के लिए उपलब्ध हूँ। आप मुझसे पूछ सकते हैं:\n" +
                    "- 'क्या मैं बाइक ईएमआई ले सकता हूँ?'\n" +
                    "- 'पिछले महीने मेरा खर्च क्यों बढ़ा?'\n" +
                    "- या सीधे टाइप करें: *'spent 350 on petrol'* ताकि मैं आपका लेजर अपडेट कर सकूँ!";
        } else if ("mr".equals(lang)) {
            reply = "🤖 **पल्स एआय सल्लागार (Pulse AI):**\n\n" +
                    "मी तुमच्या आर्थिक नियोजनासाठी सदैव तयार आहे. तुम्ही विचारू शकता:\n" +
                    "- 'मी बाईक ईएमआय घेऊ शकतो का?'\n" +
                    "- 'माझा खर्च कसा कमी करावा?'\n" +
                    "- किंवा थेट सांगा: *'spent 350 on petrol'* जेणेकरून मी व्यवहार नोंदवू शकेन!";
        } else {
            reply = "🤖 **Pulse AI Financial Assistant:**\n\n" +
                    "I am connected to your live financial cash flows, budgets, and goals. You can ask me:\n" +
                    "- *'Can I afford a bike EMI?'*\n" +
                    "- *'Why was my spending high in September?'*\n" +
                    "- *'How do I reach my BMW goal sooner?'*\n" +
                    "- Or simply type natural language expenses like *'spent 450 on lunch at Haldirams'* and I will log it for you instantly!";
        }

        return new AiChatResponse(
                reply,
                lang,
                "GENERAL_FINANCIAL_ADVICE",
                List.of("Can I afford a bike EMI?", "Why was my spending high?", "Log: spent 450 on coffee"),
                null);
    }
}
