package com.mixologybatch;

public enum OptimizationGoal
{
    XP("Maximum XP"), REWARDS("Specific rewards"), BALANCED("Balanced"),
    EFFICIENCY("Resource efficiency"), MANUAL("Manual batch (legacy)");
    private final String label;
    OptimizationGoal(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
