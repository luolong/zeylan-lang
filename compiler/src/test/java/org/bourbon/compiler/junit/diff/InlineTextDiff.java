package org.bourbon.compiler.junit.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public final class InlineTextDiff {

    private static final AttributedStyle DELETED_STYLE = AttributedStyle.DEFAULT
            .foreground(AttributedStyle.RED)
            .crossedOut();

    private static final AttributedStyle ADDED_STYLE = AttributedStyle.DEFAULT
            .foreground(AttributedStyle.GREEN)
            .italic()
            .underline();

    private static final AttributedStyle UNCHANGED_STYLE = AttributedStyle.DEFAULT;

    private InlineTextDiff() {
        // Utility class for inline textual diff formatting
    }

    public enum OutputMode {
        ANSI_TERMINAL,
        PLAIN_TEXT
    }

    public static String formatInlineDiff(String expectedText, String actualText) {
        return formatInlineDiff(expectedText, actualText, OutputMode.ANSI_TERMINAL);
    }

    public static String formatInlineDiff(String expectedText, String actualText, OutputMode outputMode) {
        if (expectedText.equals(actualText)) {
            return expectedText;
        }

        var expectedTokens = splitIntoTokens(expectedText);
        var actualTokens = splitIntoTokens(actualText);

        var expectedSize = expectedTokens.size();
        var actualSize = actualTokens.size();

        var costMatrix = new int[expectedSize + 1][actualSize + 1];

        for (var expectedIndex = 0; expectedIndex <= expectedSize; expectedIndex++) {
            costMatrix[expectedIndex][0] = expectedIndex;
        }
        for (var actualIndex = 0; actualIndex <= actualSize; actualIndex++) {
            costMatrix[0][actualIndex] = actualIndex;
        }

        for (var expectedIndex = 1; expectedIndex <= expectedSize; expectedIndex++) {
            for (var actualIndex = 1; actualIndex <= actualSize; actualIndex++) {
                if (expectedTokens.get(expectedIndex - 1).equals(actualTokens.get(actualIndex - 1))) {
                    costMatrix[expectedIndex][actualIndex] = costMatrix[expectedIndex - 1][actualIndex - 1];
                } else {
                    var costDeletion = costMatrix[expectedIndex - 1][actualIndex] + 1;
                    var costInsertion = costMatrix[expectedIndex][actualIndex - 1] + 1;
                    costMatrix[expectedIndex][actualIndex] = Math.min(costDeletion, costInsertion);
                }
            }
        }

        var expectedIndex = expectedSize;
        var actualIndex = actualSize;

        enum ChunkType { UNCHANGED, DELETED, ADDED }
        record DiffChunk(String text, ChunkType chunkType) {}

        var chunks = new ArrayList<DiffChunk>();

        while (expectedIndex > 0 || actualIndex > 0) {
            if (expectedIndex > 0 && actualIndex > 0
                    && expectedTokens.get(expectedIndex - 1).equals(actualTokens.get(actualIndex - 1))) {
                chunks.add(new DiffChunk(expectedTokens.get(expectedIndex - 1), ChunkType.UNCHANGED));
                expectedIndex--;
                actualIndex--;
            } else if (actualIndex > 0 && (expectedIndex == 0
                    || costMatrix[expectedIndex][actualIndex] == costMatrix[expectedIndex][actualIndex - 1] + 1)) {
                chunks.add(new DiffChunk(actualTokens.get(actualIndex - 1), ChunkType.ADDED));
                actualIndex--;
            } else if (expectedIndex > 0 && (actualIndex == 0
                    || costMatrix[expectedIndex][actualIndex] == costMatrix[expectedIndex - 1][actualIndex] + 1)) {
                chunks.add(new DiffChunk(expectedTokens.get(expectedIndex - 1), ChunkType.DELETED));
                expectedIndex--;
            }
        }

        Collections.reverse(chunks);

        if (outputMode == OutputMode.ANSI_TERMINAL) {
            var builder = new AttributedStringBuilder();
            for (var chunk : chunks) {
                switch (chunk.chunkType()) {
                    case UNCHANGED -> builder.style(UNCHANGED_STYLE).append(chunk.text());
                    case DELETED -> builder.style(DELETED_STYLE).append(chunk.text());
                    case ADDED -> builder.style(ADDED_STYLE).append(chunk.text());
                }
            }
            return builder.toAnsi();
        } else {
            var builder = new StringBuilder();
            for (var chunk : chunks) {
                switch (chunk.chunkType()) {
                    case UNCHANGED -> builder.append(chunk.text());
                    case DELETED -> builder.append("[-").append(chunk.text()).append("-]");
                    case ADDED -> builder.append("[+").append(chunk.text()).append("+]");
                }
            }
            return builder.toString();
        }
    }

    private static List<String> splitIntoTokens(String text) {
        var tokens = new ArrayList<String>();
        var currentToken = new StringBuilder();

        for (var characterIndex = 0; characterIndex < text.length(); characterIndex++) {
            var currentCharacter = text.charAt(characterIndex);
            if (Character.isWhitespace(currentCharacter) || !Character.isLetterOrDigit(currentCharacter)) {
                if (!currentToken.isEmpty()) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                }
                tokens.add(String.valueOf(currentCharacter));
            } else {
                currentToken.append(currentCharacter);
            }
        }
        if (!currentToken.isEmpty()) {
            tokens.add(currentToken.toString());
        }
        return tokens;
    }
}
