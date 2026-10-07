package com.clubexpense.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettlementCalculator {

    public static class Balance {

        private final String person;
        private final double amount;

        public Balance(String person, double amount) {
            this.person = person;
            this.amount = amount;
        }

        public String getPerson() {
            return person;
        }

        public double getAmount() {
            return amount;
        }
    }

    private SettlementCalculator() {
    }

    public static List<Balance> calculate(List<Expense> expenses,
                                           List<Participant> participants) {

        Map<String, Double> paid = new HashMap<>();

        for (Expense expense : expenses) {
            paid.merge(
                    expense.getPayer(),
                    expense.getAmount(),
                    Double::sum
            );
        }

        double total = expenses.stream()
                .mapToDouble(Expense::getAmount)
                .sum();

        if (participants.isEmpty()) {
            return new ArrayList<>();
        }

        double share = total / participants.size();

        Map<String, Double> balances = new HashMap<>();

        for (Participant participant : participants) {

            String name = participant.getStudentName();

            double amountPaid = paid.getOrDefault(name, 0.0);

            balances.put(name, amountPaid - share);
        }

        List<Balance> result = new ArrayList<>();

        for (Map.Entry<String, Double> entry : balances.entrySet()) {

            result.add(
                    new Balance(
                            entry.getKey(),
                            entry.getValue()
                    )
            );
        }

        return result;
    }
}