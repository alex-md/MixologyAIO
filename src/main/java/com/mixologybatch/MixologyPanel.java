package com.mixologybatch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.EnumMap;
import javax.swing.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/** Swing receives immutable text snapshots only; it never reads the RuneLite client. */
final class MixologyPanel extends PluginPanel
{
    private final JLabel status = new JLabel("Enter the mixing laboratory");
    private final JLabel action = new JLabel(" ");
    private final JLabel recipe = new JLabel(" ");
    private final JTextArea planText = new JTextArea();
    private final JProgressBar[] bars = new JProgressBar[3];
    private final EnumMap<RewardItem, Integer> targets;
    private final JComboBox<OptimizationGoal> goal = new JComboBox<>(OptimizationGoal.values());
    private String lastSnapshot = "";

    MixologyPanel(ConfigManager manager, MixologyBatchConfig config) {
        targets = RewardTargets.parse(config.rewardTargets());
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
        JPanel content = new JPanel(); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Mixology AIO"); title.setForeground(Color.WHITE); title.setFont(title.getFont().deriveFont(18f));
        content.add(title); content.add(Box.createVerticalStrut(12));
        content.add(new JLabel("Goal")); goal.setSelectedItem(config.goal());
        goal.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28)); content.add(goal);
        goal.addActionListener(e -> manager.setConfiguration(MixologyBatchConfig.GROUP, "goal", goal.getSelectedItem()));
        JPanel rewards = new JPanel(); rewards.setLayout(new BoxLayout(rewards, BoxLayout.Y_AXIS));
        for (RewardItem reward : RewardItem.values()) {
            if (reward == RewardItem.NONE) { continue; }
            JPanel row = new JPanel(new BorderLayout(4, 0));
            JLabel label = new JLabel(reward.itemName()); label.setToolTipText(reward.itemName());
            JSpinner quantity = new JSpinner(new SpinnerNumberModel(targets.getOrDefault(reward, 0).intValue(), 0, 100000, 1));
            quantity.setPreferredSize(new Dimension(72, 24)); row.add(label, BorderLayout.CENTER); row.add(quantity, BorderLayout.EAST);
            quantity.addChangeListener(e -> {
                targets.put(reward, (Integer) quantity.getValue());
                manager.setConfiguration(MixologyBatchConfig.GROUP, "rewardTargets", RewardTargets.encode(targets));
            });
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28)); row.setAlignmentX(LEFT_ALIGNMENT);
            rewards.add(row);
        }
        JTextField search = new JTextField(); search.setToolTipText("Search reward names");
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28)); search.setAlignmentX(LEFT_ALIGNMENT);
        search.putClientProperty("JTextField.placeholderText", "Search rewards");
        rewards.add(search, 0);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void filter() {
                String query = search.getText().toLowerCase(java.util.Locale.ROOT);
                for (java.awt.Component child : rewards.getComponents()) {
                    if (child instanceof JPanel) {
                        JLabel label = (JLabel) ((JPanel) child).getComponent(0);
                        child.setVisible(label.getText().toLowerCase(java.util.Locale.ROOT).contains(query));
                    }
                }
                rewards.revalidate();
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });
        JToggleButton edit = new JToggleButton("Choose rewards +"); edit.setAlignmentX(LEFT_ALIGNMENT);
        rewards.setVisible(false); edit.addActionListener(e -> {
            rewards.setVisible(edit.isSelected()); edit.setText(edit.isSelected() ? "Choose rewards -" : "Choose rewards +"); revalidate();
        });
        content.add(Box.createVerticalStrut(8)); content.add(edit); content.add(rewards);
        content.add(Box.createVerticalStrut(12)); content.add(status); content.add(Box.createVerticalStrut(6));
        for (int c = 0; c < 3; c++) {
            bars[c] = new JProgressBar(0, 1000); bars[c].setStringPainted(true);
            bars[c].setForeground(Component.values()[c].getColor()); bars[c].setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            content.add(bars[c]); content.add(Box.createVerticalStrut(4));
        }
        content.add(Box.createVerticalStrut(10)); content.add(new JLabel("Next action"));
        action.setForeground(new Color(105, 190, 205)); content.add(action); content.add(recipe);
        content.add(Box.createVerticalStrut(12)); content.add(new JLabel("Live plan"));
        planText.setEditable(false); planText.setLineWrap(true); planText.setWrapStyleWord(true);
        planText.setBackground(ColorScheme.DARK_GRAY_COLOR); planText.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6)); content.add(planText);
        JLabel help = new JLabel("<html><br>Advanced display and manual batches<br>are in plugin settings.</html>");
        help.setForeground(Color.LIGHT_GRAY); content.add(help);
        for (java.awt.Component child : content.getComponents()) {
            if (child instanceof JComponent) { ((JComponent) child).setAlignmentX(LEFT_ALIGNMENT); }
        }
        style(content);
        action.setForeground(new Color(105, 190, 205));
        for (int c = 0; c < 3; c++) { bars[c].setForeground(Component.values()[c].getColor()); }
        add(content, BorderLayout.NORTH);
    }

    private static void style(java.awt.Container container) {
        container.setBackground(ColorScheme.DARK_GRAY_COLOR);
        for (java.awt.Component child : container.getComponents()) {
            child.setForeground(Color.LIGHT_GRAY);
            if (child instanceof JLabel) { child.setBackground(ColorScheme.DARK_GRAY_COLOR); }
            if (child instanceof java.awt.Container) { style((java.awt.Container) child); }
        }
    }

    void showOutside() { showText("Enter the mixing laboratory", "Waiting for live game data", "", "Your goal and reward quantities are saved.", new int[3], new long[3]); }

    void update(int level, int[] resin, int[] paste, long[] costs, FullInventoryOptimizer.Result estimate,
                BatchPlan batch, Guidance guidance, boolean changedSettings, OptimizationGoal activeGoal) {
        String next = describe(guidance);
        String currentRecipe = guidance.getEntry() == null ? "" : guidance.getEntry().getPotion().getRecipeSequence();
        StringBuilder text = new StringBuilder("Active: ").append(activeGoal).append('\n');
        text.append("Status: ").append(guidance.getPhase()).append('\n');
        if (changedSettings) { text.append("New settings apply next inventory.\n"); }
        for (java.util.Map.Entry<RewardItem,Integer> target : targets.entrySet()) {
            if (target.getValue() > 0) { text.append(target.getKey().itemName()).append(" × ").append(target.getValue()).append('\n'); }
        }
        text.append("\n");
        for (Potion potion : Potion.values()) {
            int count = batch.getConfiguredCount(potion);
            if (count > 0) { text.append(potion).append(" × ").append(count).append("   "); }
        }
        text.append("\n").append(batch.size()).append(" potion slots\n");
        if (estimate != null && batch.size() > 0) {
            text.append(String.format("\nModeled: ~%,.0f XP/hr\n", estimate.xp * 3600 / estimate.seconds));
            text.append(String.format("Resin/hr M/A/L: %.0f/%.0f/%.0f\n", estimate.resin[0]*3600/estimate.seconds,
                estimate.resin[1]*3600/estimate.seconds,estimate.resin[2]*3600/estimate.seconds));
            text.append(String.format("Batch: ~%.0f XP · ~%.1f min\n",estimate.xp,estimate.seconds/60));
            text.append("New paste M/A/L: ").append(estimate.paste[0]).append('/').append(estimate.paste[1]).append('/').append(estimate.paste[2]);
            long[] deficit = RewardTargets.deficits(costs, resin);
            double inventories = 0;
            for (int c=0;c<3;c++) { if(deficit[c]>0) { inventories=Math.max(inventories,estimate.resin[c]>0?deficit[c]/estimate.resin[c]:Double.POSITIVE_INFINITY); } }
            if (inventories>0 && Double.isFinite(inventories)) {
                text.append(String.format("\n~%.0f inventories · ~%.1f hr remaining",Math.ceil(inventories),inventories*estimate.seconds/3600));
            }
            text.append("\nEstimates use random orders and your station pace; actual rates vary.");
        }
        text.append("\n\nHopper M/A/L: ").append(paste[0]).append('/').append(paste[1]).append('/').append(paste[2]);
        showText("Herblore " + level, next, currentRecipe, text.toString(), resin.clone(), costs.clone());
    }
    static String describe(Guidance guidance) {
        switch (guidance.getAction()) {
            case PULL_LEVER: return "Pull " + guidance.getComponent();
            case MIX_VESSEL: return "Take potion from vessel";
            case USE_STATION: return "Use " + guidance.getEntry().getStation().getObjectName();
            case WAIT_STATION: return "Finish " + guidance.getEntry().getStation().getObjectName();
            case DEPOSIT: return "Click conveyor; repeat until refill";
            default: return guidance.getMessage() == null ? "Waiting" : guidance.getMessage();
        }
    }
    private void showText(String heading, String next, String currentRecipe, String text, int[] resin, long[] costs) {
        String snapshot = heading + next + currentRecipe + text + java.util.Arrays.toString(resin) + java.util.Arrays.toString(costs);
        if (snapshot.equals(lastSnapshot)) { return; } lastSnapshot = snapshot;
        SwingUtilities.invokeLater(() -> {
            status.setText(heading); action.setText("<html>" + next + "</html>"); recipe.setText(currentRecipe); planText.setText(text);
            java.awt.FontMetrics metrics = planText.getFontMetrics(planText.getFont());
            int lines = 0;
            for (String line : text.lines().toArray(String[]::new)) { lines += Math.max(1, (int) Math.ceil(metrics.stringWidth(line) / 190.0)); }
            planText.setRows(lines + 1); revalidate();
            for (int c = 0; c < 3; c++) {
                bars[c].setValue(costs[c] == 0 ? 0 : (int) Math.min(1000, resin[c] * 1000L / costs[c]));
                bars[c].setString(Component.values()[c] + " " + resin[c] + (costs[c] == 0 ? "" : " / " + costs[c]));
                bars[c].setToolTipText("Remaining: " + Math.max(0, costs[c] - resin[c]));
            }
        });
    }
}
