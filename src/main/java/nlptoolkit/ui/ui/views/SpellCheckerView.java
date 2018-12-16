package nlptoolkit.ui.ui.views;

import Corpus.Sentence;
import com.vaadin.event.ShortcutAction;
import com.vaadin.ui.*;
import nlptoolkit.ui.services.NLPService;

public class SpellCheckerView extends NLPView {

    @Override
    public String GetScreenName() {
        return "Spell Checker";
    }

    public SpellCheckerView() {

        //Layout
        final VerticalLayout wrapperLayout = new VerticalLayout();
        final HorizontalLayout row1 = new HorizontalLayout();
        row1.setSizeFull();
        TextArea txtInput = new TextArea("Write Sentence For SpellChecker:", "İstanbül'da lokilde gezdik." +
                "\nİstanbül'da lokilde gezmedik.");
        txtInput.setRows(10);
        txtInput.setSizeFull();
        TextArea txtOutput = new TextArea("Output for SpellChecker");
        txtOutput.setSizeFull();
        txtOutput.setRows(10);
        Button btn1 = new Button("SpellChecker");
        btn1.setClickShortcut(ShortcutAction.KeyCode.ENTER);
        Button btn2 = new Button("NGramSpellChecker");

        row1.addComponents(txtInput, txtOutput);
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.addComponents(btn1, btn2);

        TextField correspondence = new TextField("Copy mapping from here. Each word pair is separated by space. " +
                "Word pairs are matched by <=> and sentences are separated by |");
        correspondence.setSizeFull();
        wrapperLayout.addComponents(row1, buttonLayout, correspondence);
        btn1.addClickListener(e -> {
            spellCheck(txtInput, txtOutput, correspondence, false);
        });
        btn2.addClickListener(e -> {
                    spellCheck(txtInput, txtOutput, correspondence, true);
//                    txtOutput.clear();
//                    Sentence mysentence = new Sentence(txtInput.getValue());
//                    Sentence ascisentence = nlpService.NGramSpellChecker(mysentence);
//                    String correctSentence = sentencetoString(ascisentence);
//                    txtOutput.setValue(txtOutput.getValue() + correctSentence);
                }
        );

        addComponent(wrapperLayout);
    }

    private String sentencetoString(Sentence sentence) {
        String result = "";
        for (int i = 0; i < sentence.wordCount(); i++) {
            result += sentence.getWord(i);
            result += " ";
        }
        return result;
    }

    private void spellCheck(TextArea txtInput, TextArea txtOutput, TextField correspondence, boolean useNgram) {
        NLPService nlpService = new NLPService();
        txtOutput.clear();
        String[] inputLines = txtInput.getValue().split("\n");
        String[] outputLines = new String[inputLines.length];
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < inputLines.length; i += 1) {
            Sentence input = new Sentence(inputLines[i]);
            Sentence corrected = null;
            if (useNgram) {
                corrected = nlpService.NGramSpellChecker(input);
            } else {
                corrected = nlpService.SpellChecker(input);
            }
            output.append(corrected.toString() + "\n");
            outputLines[i] = corrected.toString();
        }
        txtOutput.setValue(output.toString());

        StringBuilder valueToCopy = new StringBuilder();
        for (int i = 0; i < outputLines.length; i++) {
            String inputLine = inputLines[i];
            String outputLine = outputLines[i];
            String[] inputWords = inputLine.split("\\s+");
            String[] outputWords = outputLine.split("\\s+");
            if (inputWords.length == outputWords.length) {
                for (int j = 0; j < inputWords.length; j++) {
                    String inputWord = inputWords[j];
                    String outputWord = outputWords[j];
                    String[] alignments = getAlignment(inputWord, outputWord);
                    valueToCopy.append(inputWord + "<=>" + outputWord + " ");
                }
                valueToCopy.append("|");
            } else {
                valueToCopy.append("Different IO length! Couldn't align.");
            }
        }
        correspondence.setValue(valueToCopy.toString());
    }

    private String[] getAlignment(String word1, String word2) {
        int l1, l2;
        l1 = word1.length() + 1;
        l2 = word2.length() + 1;
        int[][] distanceTable = new int[l1][l2];

        int gapX, gapY, sub;
        for (int i = 0; i < l1; i++) {
            distanceTable[i][0] = i;
        }

        for (int j = 0; j < l2; j++) {
            distanceTable[0][j] = j;
        }

        for (int i = 1; i < l1; i++) {
            for (int j = 1; j < l2; j++) {
                //Cost if no gap inserted
                if (Character.toLowerCase(word1.charAt(i - 1)) == Character.toLowerCase(word2.charAt(j - 1))) {
                    sub = distanceTable[i - 1][j - 1];
                } else {
                    sub = distanceTable[i - 1][j - 1] + 1;
                }
                gapX = distanceTable[i][j - 1] + 1;    //Cost if insert gap in x
                gapY = distanceTable[i - 1][j] + 1;    //Cost if insert gap in y
                distanceTable[i][j] = Math.min(Math.min(gapX, gapY), sub);
            }
        }

        StringBuilder sbX = new StringBuilder(word1), sbY = new StringBuilder(word2);
        int i = l1 - 1, j = l2 - 1;

        //Iterate until back at origin [0,0]
        while (i != 0 && j != 0) {
            if (distanceTable[i][j] == distanceTable[i][j - 1] + 1) {            //Move left 1
                sbX.insert(i, '-');
                j--;
            } else if (distanceTable[i][j] == distanceTable[i - 1][j] + 1) {    //Move up 1
                sbY.insert(j, '-');
                i--;
            } else {    //Move diagnally
                i--;
                j--;
            }
            if (i == 0 && j > 0) {    //At the X boundary, move down 1
                sbX.insert(0, '-');
                i++;
            } else if (i > 0 && j == 0) {  //At the Y boundary, move right 1
                sbY.insert(0, '-');
                j++;
            }
        }

        return new String[]{sbX.toString(), sbY.toString()};
    }
}
