package se.runeslasaventyr;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int AUDIO_PERMISSION_REQUEST = 10;

    private final List<String> words = new ArrayList<>(Arrays.asList(
            "sol", "mus", "ram", "ris", "ros",
            "sil", "sal", "mor", "fil", "mil",
            "sur", "mal", "hus", "lus", "ren"
    ));

    private GridLayout board;
    private TextView startText;
    private TextView wordText;
    private TextView feedbackText;
    private TextView progressText;
    private Button listenButton;

    private SpeechRecognizer speechRecognizer;
    private String currentWord = "sol";
    private int position = 0;
    private int wordIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Collections.shuffle(words);
        currentWord = words.get(0);

        buildUi();
        renderBoard();
        showWord();

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            setupSpeechRecognizer();
        } else {
            listenButton.setEnabled(false);
            feedbackText.setText("Taligenkänning finns inte på den här telefonen.");
        }
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(36, 86, 62));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(28));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Runes Läsäventyr");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap(dp(8)));

        TextView subtitle = new TextView(this);
        subtitle.setText("Läs 10 ord och ta dig hela vägen till målet!");
        subtitle.setTextColor(Color.WHITE);
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, matchWrap(dp(16)));

        startText = new TextView(this);
        startText.setText("START  🧙");
        startText.setTextColor(Color.rgb(23, 50, 41));
        startText.setTextSize(18);
        startText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        startText.setGravity(Gravity.CENTER);
        startText.setBackgroundColor(Color.rgb(215, 199, 145));
        startText.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.addView(startText, matchWrap(dp(8)));

        board = new GridLayout(this);
        board.setColumnCount(5);
        board.setRowCount(2);
        board.setPadding(dp(4), dp(4), dp(4), dp(4));
        root.addView(board, matchWrap(dp(8)));

        progressText = new TextView(this);
        progressText.setTextColor(Color.WHITE);
        progressText.setTextSize(17);
        progressText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        progressText.setGravity(Gravity.CENTER);
        root.addView(progressText, matchWrap(dp(18)));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(20), dp(18), dp(20));
        card.setBackgroundColor(Color.rgb(255, 248, 223));
        root.addView(card, matchWrap(dp(0)));

        TextView instruction = new TextView(this);
        instruction.setText("Läs ordet högt");
        instruction.setTextColor(Color.rgb(80, 95, 85));
        instruction.setTextSize(18);
        instruction.setGravity(Gravity.CENTER);
        instruction.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(instruction, matchWrap(dp(6)));

        wordText = new TextView(this);
        wordText.setTextColor(Color.rgb(23, 50, 41));
        wordText.setTextSize(66);
        wordText.setGravity(Gravity.CENTER);
        wordText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        wordText.setLetterSpacing(0.12f);
        card.addView(wordText, matchWrap(dp(14)));

        listenButton = new Button(this);
        listenButton.setText("🎤  LÄS ORDET");
        listenButton.setTextSize(20);
        listenButton.setAllCaps(false);
        listenButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        listenButton.setMinHeight(dp(64));
        listenButton.setOnClickListener(v -> startListening());
        card.addView(listenButton, matchWrap(dp(12)));

        feedbackText = new TextView(this);
        feedbackText.setText("Tryck på mikrofonen och säg ordet.");
        feedbackText.setTextColor(Color.rgb(35, 70, 53));
        feedbackText.setTextSize(18);
        feedbackText.setGravity(Gravity.CENTER);
        feedbackText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        feedbackText.setPadding(dp(8), dp(12), dp(8), dp(12));
        card.addView(feedbackText, matchWrap(dp(4)));

        TextView privacy = new TextView(this);
        privacy.setText("Appen sparar inte inspelningen. Den använder Androids taligenkänning för att jämföra det hörda ordet med ordet på skärmen.");
        privacy.setTextColor(Color.rgb(100, 112, 104));
        privacy.setTextSize(12);
        privacy.setGravity(Gravity.CENTER);
        card.addView(privacy, matchWrap(dp(0)));

        Button reset = new Button(this);
        reset.setText("Börja om banan");
        reset.setAllCaps(false);
        reset.setOnClickListener(v -> {
            position = 0;
            wordIndex = 0;
            Collections.shuffle(words);
            currentWord = words.get(0);
            feedbackText.setText("Ny bana. Tryck på mikrofonen och läs ordet.");
            renderBoard();
            showWord();
        });
        root.addView(reset, matchWrap(dp(0)));

        setContentView(scroll);
    }

    private void renderBoard() {
        board.removeAllViews();

        startText.setText(position == 0 ? "START  🧙" : "START");

        for (int i = 1; i <= 10; i++) {
            TextView cell = new TextView(this);

            String content;
            if (position == i && i == 10) {
                content = "🧙\n🏆";
            } else if (position == i) {
                content = "🧙\n" + i;
            } else if (i == 10) {
                content = "🏆\n10";
            } else {
                content = String.valueOf(i);
            }

            cell.setText(content);
            cell.setGravity(Gravity.CENTER);
            cell.setTextSize(position == i ? 22 : 18);
            cell.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            cell.setTextColor(Color.WHITE);
            cell.setPadding(dp(4), dp(6), dp(4), dp(6));

            if (i <= position) {
                cell.setBackgroundColor(Color.rgb(103, 151, 78));
            } else if (i == 10) {
                cell.setBackgroundColor(Color.rgb(121, 91, 52));
            } else {
                cell.setBackgroundColor(Color.rgb(139, 103, 63));
            }

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = dp(72);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            board.addView(cell, lp);
        }

        if (position < 10) {
            int left = 10 - position;
            progressText.setText(left + (left == 1 ? " ord kvar till målet" : " ord kvar till målet"));
        } else {
            progressText.setText("🏆 MÅL! Du klarade hela banan!");
        }
    }

    private void showWord() {
        wordText.setText(spaced(currentWord).toUpperCase(Locale.forLanguageTag("sv-SE")));
        listenButton.setEnabled(position < 10 && SpeechRecognizer.isRecognitionAvailable(this));
    }

    private String spaced(String word) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            if (i > 0) b.append("  ");
            b.append(word.charAt(i));
        }
        return b.toString();
    }

    private void setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                feedbackText.setText("Jag lyssnar…");
            }

            @Override public void onBeginningOfSpeech() {
                feedbackText.setText("Fortsätt läsa…");
            }

            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}

            @Override public void onEndOfSpeech() {
                feedbackText.setText("Kontrollerar…");
            }

            @Override public void onError(int error) {
                listenButton.setEnabled(true);
                if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                    feedbackText.setText("Jag hörde inte ordet tydligt. Försök igen.");
                } else if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    feedbackText.setText("Jag hörde inget. Tryck och försök igen.");
                } else {
                    feedbackText.setText("Kunde inte tolka rösten. Försök igen.");
                }
            }

            @Override public void onResults(Bundle results) {
                listenButton.setEnabled(true);

                ArrayList<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches == null || matches.isEmpty()) {
                    feedbackText.setText("Jag kunde inte höra ordet. Försök igen.");
                    return;
                }

                boolean correct = false;
                for (String heard : matches) {
                    if (matchesTarget(heard, currentWord)) {
                        correct = true;
                        break;
                    }
                }

                if (correct) {
                    advance();
                } else {
                    feedbackText.setText("Jag hörde: “" + matches.get(0) + "”. Försök igen.");
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startListening() {
        if (position >= 10) return;

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    AUDIO_PERMISSION_REQUEST
            );
            return;
        }

        if (speechRecognizer == null) {
            setupSpeechRecognizer();
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sv-SE");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "sv-SE");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

        listenButton.setEnabled(false);
        feedbackText.setText("Startar mikrofonen…");
        speechRecognizer.startListening(intent);
    }

    private boolean matchesTarget(String heard, String target) {
        String h = normalize(heard);
        String t = normalize(target);

        if (h.equals(t)) return true;

        for (String part : h.split(" ")) {
            if (part.equals(t)) return true;
        }

        return false;
    }

    private String normalize(String s) {
        if (s == null) return "";
        String value = s.toLowerCase(Locale.forLanguageTag("sv-SE")).trim();
        value = Normalizer.normalize(value, Normalizer.Form.NFC);
        return value.replaceAll("[^a-zåäö ]", "").replaceAll("\\s+", " ");
    }

    private void advance() {
        position++;
        renderBoard();

        if (position >= 10) {
            wordText.setText("🏆");
            feedbackText.setText("RÄTT! Du kom i mål!");
            listenButton.setEnabled(false);
            return;
        }

        feedbackText.setText("✅ Rätt! Ett steg framåt.");

        wordIndex++;
        if (wordIndex >= words.size()) {
            wordIndex = 0;
            Collections.shuffle(words);
        }
        currentWord = words.get(wordIndex);

        wordText.postDelayed(() -> {
            showWord();
            feedbackText.setText("Nästa ord. Tryck på mikrofonen.");
        }, 700);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == AUDIO_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                feedbackText.setText("Mikrofonbehörighet behövs för att appen ska kunna kontrollera läsningen.");
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }

    private LinearLayout.LayoutParams matchWrap(int bottomMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.bottomMargin = bottomMargin;
        return p;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
