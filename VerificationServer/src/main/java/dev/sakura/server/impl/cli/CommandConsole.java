package dev.sakura.server.impl.cli;

import org.tinylog.Logger;

import java.util.Scanner;

public final class CommandConsole implements Runnable {
    private final CommandProcessor processor;

    public CommandConsole(dev.sakura.server.impl.storage.SqliteDatabase database,
                          dev.sakura.server.impl.storage.UserRepository userRepository,
                          dev.sakura.server.impl.storage.CardRepository cardRepository) {
        this.processor = new CommandProcessor(database, userRepository, cardRepository);
    }

    @Override
    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                if (!scanner.hasNextLine()) {
                    return;
                }
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    CommandProcessor.CommandResult result = processor.execute(line);
                    for (String out : result.lines()) {
                        if (out != null && !out.isEmpty()) {
                            Logger.info(out);
                        }
                    }
                    if (result.shouldExit()) {
                        return;
                    }
                } catch (Exception e) {
                    Logger.error(e);
                }
            }
        }
    }
}
