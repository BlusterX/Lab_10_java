package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {
    private static final String PATHFILE = "src/main/resources/config.yml";
    private final Configuration.Builder configBuild = new Configuration.Builder();

    private final DrawNumber model;
    private final List<DrawNumberView> views;

    private void confFile(final String file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                var lines = Arrays.asList(line.split(": "));
                String name = lines.get(0);
                int value = Integer.parseInt(lines.get(1));
                if (name.contains("max")) {
                    getConfigBuild().setMax(value);
                } else if (name.contains("min")) {
                    getConfigBuild().setMin(value);
                } else if (name.contains("attempts")) {
                    getConfigBuild().setAttempts(value);
                } else {
                    displayError("Invalid config file.");
                }
            }
        }
    }

    private Configuration.Builder getConfigBuild() {
        return configBuild;
    }

    private void displayError(final String error) {
        for (final DrawNumberView view : views) {
            view.displayError(error);
        }
    }

    /**
     * @param views the views to attach 
     * @throws IOException
     */
    public DrawNumberApp(final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view: views) {
            view.setObserver(this);
            view.start();
        }
        try {
            confFile(PATHFILE);
        } catch (IOException | NumberFormatException e) {
            displayError("Error: " + e.getMessage());
        }
        this.model = new DrawNumberImpl(getConfigBuild().build());
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view: views) {
                view.result(result);
            }
        } catch (IllegalArgumentException e) {
            for (final DrawNumberView view: views) {
                view.numberIncorrect();
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * @param args
     *            ignored
     * @throws FileNotFoundException 
     */
    public static void main(final String... args) throws FileNotFoundException {
        new DrawNumberApp(
            new DrawNumberViewImpl(), 
            new DrawNumberViewImpl(), 
            new PrintStreamView(System.out), 
            new PrintStreamView("output.log"));
    }

}
