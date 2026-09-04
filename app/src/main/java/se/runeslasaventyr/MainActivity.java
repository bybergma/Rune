package se.runeslasaventyr;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
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
    private TextView heardLabel;
    private TextView heardText;
    private TextView feedbackText;
    private TextView progressText;
    private TextView celebrationText;
    private Button listenButton;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    private ToneGenerator toneGenerator;

    private String currentWord = "sol";
    private int position = 0;
    private int wordIndex = 0;
    private boolean ttsReady = false;
    private boolean celebrating = false;
    private String latestPartial = "";

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Collections.shuffle(words);
        currentWord = words.get(0);

        buildUi();
        renderBoard();
        showWord();
        setupTextToSpeech();

        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 90);

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            setupSpeechRecognizer();
        } else {
            listenButton.setEnabled(false);
            feedbackText.setText("Taligenkänning saknas");
        }
    }

    private void setupTextToSpeech() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("sv", "SE"));
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
                tts.setSpeechRate(0.92f);

                if (ttsReady) {
                    speak("Tryck på mikrofonen och läs ordet.");
                }
            }
        });
    }

    private void speak(String text) {
        if (!ttsReady || tts == null) return;
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "RuneSpeech");
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
        subtitle.setText("10 ord till skatten!");
        subtitle.setTextColor(Color.WHITE);
        subtitle.setTextSize(18);
        subtitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, matchWrap(dp(14)));

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
        progressText.setTextSize(18);
        progressText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        progressText.setGravity(Gravity.CENTER);
        root.addView(progressText, matchWrap(dp(12)));

        celebrationText = new TextView(this);
        celebrationText.setText("⭐ HURRA! ⭐");
        celebrationText.setTextColor(Color.rgb(255, 232, 94));
        celebrationText.setTextSize(34);
        celebrationText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        celebrationText.setGravity(Gravity.CENTER);
        celebrationText.setVisibility(View.GONE);
        root.addView(celebrationText, matchWrap(dp(8)));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(20));
        card.setBackgroundColor(Color.rgb(255, 248, 223));
        root.addView(card, matchWrap(dp(0)));

        TextView instruction = new TextView(this);
        instruction.setText("LÄS ORDET");
        instruction.setTextColor(Color.rgb(80, 95, 85));
        instruction.setTextSize(18);
        instruction.setGravity(Gravity.CENTER);
        instruction.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(instruction, matchWrap(dp(4)));

        wordText = new TextView(this);
        wordText.setTextColor(Color.rgb(23, 50, 41));
        wordText.setTextSize(70);
        wordText.setGravity(Gravity.CENTER);
        wordText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        wordText.setLetterSpacing(0.12f);
        wordText.setPadding(dp(4), dp(5), dp(4), dp(10));
        card.addView(wordText, matchWrap(dp(8)));

        listenButton = new Button(this);
        listenButton.setText("🎤");
        listenButton.setContentDescription("Tryck för att läsa ordet");
        listenButton.setTextSize(38);
        listenButton.setMinHeight(dp(76));
        listenButton.setOnClickListener(v -> startListening());
        card.addView(listenButton, matchWrap(dp(8)));

        Button repeatInstructionButton = new Button(this);
        repeatInstructionButton.setText("🔊  LYSSNA");
        repeatInstructionButton.setContentDescription("Spela upp instruktionen igen");
        repeatInstructionButton.setTextSize(18);
        repeatInstructionButton.setAllCaps(false);
        repeatInstructionButton.setOnClickListener(v ->
                speak("Tryck på mikrofonen och läs ordet."));
        card.addView(repeatInstructionButton, matchWrap(dp(14)));

        heardLabel = new TextView(this);
        heardLabel.setText("JAG HÖRDE");
        heardLabel.setTextColor(Color.rgb(95, 105, 98));
        heardLabel.setTextSize(14);
        heardLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heardLabel.setGravity(Gravity.CENTER);
        heardLabel.setVisibility(View.GONE);
        card.addView(heardLabel, matchWrap(dp(2)));

        heardText = new TextView(this);
        heardText.setText("");
        heardText.setTextColor(Color.rgb(35, 70, 53));
        heardText.setTextSize(56);
        heardText.setGravity(Gravity.CENTER);
        heardText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heardText.setPadding(dp(8), dp(10), dp(8), dp(10));
        heardText.setVisibility(View.GONE);
        card.addView(heardText, matchWrap(dp(6)));

        feedbackText = new TextView(this);
        feedbackText.setText("🎤");
        feedbackText.setTextColor(Color.rgb(35, 70, 53));
        feedbackText.setTextSize(22);
        feedbackText.setGravity(Gravity.CENTER);
        feedbackText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        feedbackText.setPadding(dp(8), dp(8), dp(8), dp(8));
        card.addView(feedbackText, matchWrap(dp(0)));

        Button reset = new Button(this);
        reset.setText("↻");
        reset.setContentDescription("Börja om banan");
        reset.setTextSize(24);
        reset.setOnClickListener(v -> {
            position = 0;
            wordIndex = 0;
            celebrating = false;
            Collections.shuffle(words);
            currentWord = words.get(0);
            celebrationText.setVisibility(View.GONE);
            clearHeard();
            renderBoard();
            showWord();
            feedbackText.setText("🎤");
            speak("Ny bana. Tryck på mikrofonen och läs ordet.");
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
            progressText.setText((10 - position) + " kvar");
        } else {
            progressText.setText("🏆 MÅL!");
        }
    }

    private void showWord() {
        wordText.setText(spaced(currentWord).toUpperCase(new Locale("sv", "SE")));
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

    private void clearHeard() {
        heardLabel.setVisibility(View.GONE);
        heardText.setVisibility(View.GONE);
        heardText.setText("");
        heardText.setBackgroundColor(Color.TRANSPARENT);
    }

    private void showHeard(String text, boolean correct) {
        heardLabel.setVisibility(View.VISIBLE);
        heardText.setVisibility(View.VISIBLE);
        heardText.setText(text.toUpperCase(new Locale("sv", "SE")));
        heardText.setTextColor(correct
                ? Color.rgb(24, 105, 48)
                : Color.rgb(150, 55, 45));
        heardText.setBackgroundColor(correct
                ? Color.rgb(218, 247, 220)
                : Color.rgb(255, 229, 218));

        ScaleAnimation pop = new ScaleAnimation(
                0.75f, 1.0f, 0.75f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        pop.setDuration(240);
        heardText.startAnimation(pop);
    }

    private void setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                feedbackText.setText("👂");
                latestPartial = "";
            }

            @Override public void onBeginningOfSpeech() {
                feedbackText.setText("👂 …");
            }

            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}

            @Override public void onEndOfSpeech() {
                feedbackText.setText("⏳");
            }

            @Override public void onError(int error) {
                listenButton.setEnabled(true);

                // Short isolated words can produce a partial hypothesis and then NO_MATCH.
                // Keep that hypothesis visible so we can see what Android actually heard.
                if (!latestPartial.isEmpty()) {
                    showHeard(latestPartial, false);
                    feedbackText.setText("🔁 FÖRSÖK IGEN");
                    speak("Jag hörde " + latestPartial + ". Försök igen.");
                    return;
                }

                clearHeard();
                feedbackText.setText("🔁");

                if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    speak("Jag hörde inget. Försök igen.");
                } else if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                    speak("Jag kunde inte tolka ordet. Försök igen.");
                } else {
                    speak("Försök igen.");
                }
            }

            @Override public void onResults(Bundle results) {
                listenButton.setEnabled(true);

                ArrayList<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches == null || matches.isEmpty()) {
                    if (!latestPartial.isEmpty()) {
                        showHeard(latestPartial, false);
                    } else {
                        clearHeard();
                    }
                    feedbackText.setText("🔁");
                    speak("Försök igen.");
                    return;
                }

                String best = matches.get(0);
                boolean correct = false;

                for (String heard : matches) {
                    if (matchesTarget(heard, currentWord)) {
                        correct = true;
                        best = heard;
                        break;
                    }
                }

                showHeard(best, correct);

                if (correct) {
                    advance();
                } else {
                    feedbackText.setText("🔁 FÖRSÖK IGEN");
                    speak("Jag hörde " + best + ". Försök igen.");
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {
                ArrayList<String> partial =
                        partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (partial != null && !partial.isEmpty()) {
                    latestPartial = partial.get(0).trim();
                    if (!latestPartial.isEmpty()) {
                        heardLabel.setVisibility(View.VISIBLE);
                        heardText.setVisibility(View.VISIBLE);
                        heardLabel.setText("JAG HÖR");
                        heardText.setText(latestPartial.toUpperCase(new Locale("sv", "SE")));
                        heardText.setTextColor(Color.rgb(35, 70, 53));
                        heardText.setBackgroundColor(Color.rgb(238, 244, 239));
                    }
                }
            }

            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startListening() {
        if (position >= 10 || celebrating) return;

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

        if (tts != null) {
            tts.stop();
        }

        clearHeard();

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sv-SE");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "sv-SE");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 8);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        // Slightly longer listening window for very short isolated words.
        // The installed recognition service may choose to ignore these hints.
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1200L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 700L);

        // Android 13+ can favour the word currently shown on screen.
        if (Build.VERSION.SDK_INT >= 33) {
            ArrayList<String> bias = new ArrayList<>();
            bias.add(currentWord);
            intent.putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, bias);
        }

        latestPartial = "";
        listenButton.setEnabled(false);
        feedbackText.setText("🎤 …");

        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            listenButton.setEnabled(true);
            feedbackText.setText("🔁");
            speak("Försök igen.");
        }
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
        String value = s.toLowerCase(new Locale("sv", "SE")).trim();
        value = Normalizer.normalize(value, Normalizer.Form.NFC);
        return value.replaceAll("[^a-zåäö ]", "").replaceAll("\\s+", " ");
    }

    private void playSuccessEffect() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 120);
        handler.postDelayed(() ->
                toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 180), 150);
        handler.postDelayed(() ->
                toneGenerator.startTone(ToneGenerator.TONE_DTMF_9, 220), 340);

        celebrationText.setVisibility(View.VISIBLE);
        celebrationText.setText("⭐ HURRA! RÄTT! ⭐");

        ScaleAnimation pop = new ScaleAnimation(
                0.55f, 1.15f, 0.55f, 1.15f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        pop.setDuration(300);
        pop.setRepeatMode(Animation.REVERSE);
        pop.setRepeatCount(1);
        celebrationText.startAnimation(pop);

        AlphaAnimation flash = new AlphaAnimation(0.25f, 1.0f);
        flash.setDuration(180);
        flash.setRepeatMode(Animation.REVERSE);
        flash.setRepeatCount(3);
        board.startAnimation(flash);

        speak("Hurra! Rätt!");
    }

    private void advance() {
        celebrating = true;
        position++;
        renderBoard();
        feedbackText.setText("✅ RÄTT!");
        listenButton.setEnabled(false);

        playSuccessEffect();

        if (position >= 10) {
            wordText.setText("🏆");
            celebrationText.setText("🏆 DU KLARADE BANAN! 🏆");
            handler.postDelayed(() ->
                    speak("Hurra! Du klarade hela banan!"), 900);
            return;
        }

        handler.postDelayed(() -> {
            wordIndex++;
            if (wordIndex >= words.size()) {
                wordIndex = 0;
                Collections.shuffle(words);
            }
            currentWord = words.get(wordIndex);
            clearHeard();
            celebrationText.setVisibility(View.GONE);
            showWord();
            feedbackText.setText("🎤");
            celebrating = false;
            speak("Nästa ord. Tryck på mikrofonen och läs ordet.");
        }, 1700);
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
                speak("Bra. Nu kan jag lyssna. Läs ordet.");
                handler.postDelayed(this::startListening, 700);
            } else {
                feedbackText.setText("🎤 🚫");
                speak("Mikrofonen behöver tillåtelse för att kunna lyssna.");
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (toneGenerator != null) {
            toneGenerator.release();
        }
        handler.removeCallbacksAndMessages(null);
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
