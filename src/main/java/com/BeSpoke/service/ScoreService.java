package com.BeSpoke.service;

import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.RequirementFormStatus;
import org.springframework.stereotype.Service;

/**
 * Lead scoring: base 20, plus budget band, property type and requirement-form
 * completeness bonuses. Capped at 98 (a lead is never a sure thing).
 */
@Service
public class ScoreService {

    public int compute(String budgetBand, String propertyType, RequirementFormStatus formStatus) {
        int score = 20;
        if (budgetBand != null) {
            switch (budgetBand) {
                case "ABOVE_50L" -> score += 30;
                case "L25_50" -> score += 25;
                case "L10_25" -> score += 18;
                case "L5_10" -> score += 10;
                case "UNDER_5L" -> score += 5;
                default -> { /* unknown band: no bonus */ }
            }
        }
        if (propertyType != null) {
            switch (propertyType) {
                case "VILLA", "INDEPENDENT_HOUSE" -> score += 10;
                case "APARTMENT", "BUILDER_FLOOR" -> score += 6;
                case "COMMERCIAL" -> score += 12;
                default -> { /* unknown type: no bonus */ }
            }
        }
        if (formStatus == RequirementFormStatus.SUBMITTED
                || formStatus == RequirementFormStatus.APPROVED) {
            score += 25;
        } else if (formStatus == RequirementFormStatus.DRAFT) {
            score += 10;
        }
        return Math.min(score, 98);
    }

    /** Recomputes and applies the score on the lead (formStatus may be null when no form exists). */
    public void rescore(Lead lead, RequirementFormStatus formStatus) {
        lead.setScore(compute(lead.getBudgetBand(), lead.getPropertyType(), formStatus));
    }
}
