package com.peter.budget.service;

import com.peter.budget.model.entity.CategorizationRule;
import com.peter.budget.model.enums.MatchField;
import com.peter.budget.model.enums.PatternType;
import com.peter.budget.repository.CategorizationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoCategorizationService {

    private final CategorizationRuleRepository ruleRepository;

    public CategorizationMatch categorize(Long userId, String description, String payee, String memo) {
        List<CategorizationRule> rules = ruleRepository.findActiveRulesForUser(userId);

        for (CategorizationRule rule : rules) {
            String textToMatch = getTextToMatch(rule.getMatchField(), description, payee, memo);

            if (textToMatch != null && matches(textToMatch, rule.getPattern(), rule.getPatternType())) {
                log.debug("Matched rule '{}' for text: {}", rule.getName(), textToMatch);
                return new CategorizationMatch(rule.getId(), rule.getCategoryId());
            }
        }

        return null;
    }

    public record CategorizationMatch(Long ruleId, Long categoryId) {}

    private String getTextToMatch(MatchField matchField, String description, String payee, String memo) {
        return switch (matchField) {
            case DESCRIPTION -> description;
            case PAYEE -> payee;
            case MEMO -> memo;
        };
    }

    private boolean matches(String text, String pattern, PatternType patternType) {
        if (text == null || pattern == null) {
            return false;
        }

        String upperText = text.toUpperCase();
        String upperPattern = pattern.toUpperCase();

        return switch (patternType) {
            case CONTAINS -> upperText.contains(upperPattern);
            case STARTS_WITH -> upperText.startsWith(upperPattern);
            case ENDS_WITH -> upperText.endsWith(upperPattern);
            case EXACT -> upperText.equals(upperPattern);
            case REGEX -> matchesRegex(text, pattern);
        };
    }

    private boolean matchesRegex(String text, String pattern) {
        try {
            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text).find();
        } catch (PatternSyntaxException e) {
            log.warn("Invalid regex pattern: {}", pattern);
            return false;
        }
    }
}
