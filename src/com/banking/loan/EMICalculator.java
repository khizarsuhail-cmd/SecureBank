package com.banking.loan;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Module 4 - EMI Calculator.
 *
 * Implements the standard EMI formula:
 *   EMI = P x R x (1+R)^N  /  ((1+R)^N - 1)
 * where:
 *   P = principal
 *   R = monthly interest rate = annualRate / 12
 *   N = tenure in months
 */
public class EMICalculator {

    private EMICalculator() {}   // Utility class - no instances

    /**
     * Computes the monthly EMI using exact BigDecimal arithmetic.
     *
     * @param principal    loan amount
     * @param annualRate   annual interest rate as a decimal (e.g. 0.12 for 12 %)
     * @param tenureMonths repayment period in months
     * @return monthly EMI rounded to 2 decimal places
     */
    public static BigDecimal computeEMI(BigDecimal principal,
                                        double annualRate,
                                        int tenureMonths) {
        if (annualRate == 0) {
            // Zero-interest loan
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }

        double R = annualRate / 12.0;
        double factor = Math.pow(1 + R, tenureMonths);

        // EMI = P x R x (1+R)^N / ((1+R)^N - 1)
        double emi = principal.doubleValue() * R * factor / (factor - 1);

        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Computes the total repayment amount over the full tenure.
     */
    public static BigDecimal totalRepayment(BigDecimal principal,
                                            double annualRate,
                                            int tenureMonths) {
        return computeEMI(principal, annualRate, tenureMonths)
                .multiply(BigDecimal.valueOf(tenureMonths))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Computes the total interest payable.
     */
    public static BigDecimal totalInterest(BigDecimal principal,
                                           double annualRate,
                                           int tenureMonths) {
        return totalRepayment(principal, annualRate, tenureMonths)
                .subtract(principal)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Prints a human-readable breakdown summary.
     */
    public static void printSummary(BigDecimal principal, double annualRate, int tenureMonths) {
        System.out.println("\n-- EMI Summary --");
        System.out.printf("  Principal      : PKR %,.2f%n", principal);
        System.out.printf("  Annual Rate    : %.2f%%%n", annualRate * 100);
        System.out.printf("  Tenure         : %d months%n", tenureMonths);
        System.out.printf("  Monthly EMI    : PKR %,.2f%n",
                computeEMI(principal, annualRate, tenureMonths));
        System.out.printf("  Total Repayment: PKR %,.2f%n",
                totalRepayment(principal, annualRate, tenureMonths));
        System.out.printf("  Total Interest : PKR %,.2f%n",
                totalInterest(principal, annualRate, tenureMonths));
    }
}
