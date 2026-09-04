package se.runeslasaventyr;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

    // Nivå 1: två bokstäver
    private final List<String> level1Words = new ArrayList<>(Arrays.asList(
            "is", "gå", "åt", "du", "vi", "om", "by", "är", "aj", "ha"
    ));

    // Nivå 2: korta ord. KATT ligger i nivå 3 för att hålla denna nivå kortare.
    // Det finns nio unika ord, så ett ord kan återkomma på en 10-stegsbana.
    private final List<String> level2Words = new ArrayList<>(Arrays.asList(
            "sol", "kor", "ror", "bil", "mus", "hus", "bok", "mat", "fis"
    ));

    // Nivå 3: fyra bokstäver
    private final List<String> level3Words = new ArrayList<>(Arrays.asList(
            "boll", "katt", "hund", "bord", "stol",
            "glas", "fisk", "läsa", "måne", "bajs"
    ));

    private LinearLayout levelPanel;
    private LinearLayout gamePanel;
    private GridLayout board;
    private TextView startText;
    private TextView wordText;
    private TextView heardLabel;
    private TextView heardText;
    private TextView feedbackText;
    private TextView progressText;
    private TextView celebrationText;
    private TextView chosenLevelText;
    private Button listenButton;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    private ToneGenerator toneGenerator;

    private List<String> activeWords = new ArrayList<>();
    private String currentWord = "sol";
    private int selectedLevel = 1;
    private int position = 0;
    private int wordIndex = 0;
    private boolean ttsReady = false;
    private boolean celebrating = false;
    private boolean levelChosen = false;
    private String latestPartial = "";
    private boolean partialMatchedTarget = false;
    private TextView heroView;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildUi();
        setupTextToSpeech();
        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 90);

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            setupSpeechRecognizer();
        }
    }

    private void setupTextToSpeech() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("sv", "SE"));
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
                tts.setSpeechRate(0.92f);

                if (ttsReady && !levelChosen) {
                    speakLevelQuestion();
                }
            }
        });
    }

    private void speak(String text) {
        if (!ttsReady || tts == null) return;
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "RuneSpeech");
    }

    private void speakLevelQuestion() {
        speak("Vilken nivå vill du läsa? Tryck på ett, två eller tre.");
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
        root.addView(title, matchWrap(dp(14)));

        buildLevelPanel(root);
        buildGamePanel(root);

        setContentView(scroll);
    }

    private void buildLevelPanel(LinearLayout root) {
        levelPanel = new LinearLayout(this);
        levelPanel.setOrientation(LinearLayout.VERTICAL);
        levelPanel.setPadding(dp(18), dp(22), dp(18), dp(22));
        levelPanel.setBackgroundColor(Color.rgb(255, 248, 223));
        root.addView(levelPanel, matchWrap(dp(0)));

        TextView question = new TextView(this);
        question.setText("VÄLJ NIVÅ");
        question.setTextColor(Color.rgb(23, 50, 41));
        question.setTextSize(24);
        question.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        question.setGravity(Gravity.CENTER);
        levelPanel.addView(question, matchWrap(dp(14)));

        Button hearQuestion = new Button(this);
        hearQuestion.setText("🔊");
        hearQuestion.setContentDescription("Lyssna på frågan");
        hearQuestion.setTextSize(30);
        hearQuestion.setMinHeight(dp(60));
        hearQuestion.setOnClickListener(v -> speakLevelQuestion());
        levelPanel.addView(hearQuestion, matchWrap(dp(18)));

        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        choices.setGravity(Gravity.CENTER);
        levelPanel.addView(choices, matchWrap(dp(8)));

        choices.addView(makeLevelButton(1), weightedButtonParams());
        choices.addView(makeLevelButton(2), weightedButtonParams());
        choices.addView(makeLevelButton(3), weightedButtonParams());

        TextView hint = new TextView(this);
        hint.setText("1      2      3");
        hint.setTextColor(Color.rgb(95, 105, 98));
        hint.setTextSize(16);
        hint.setGravity(Gravity.CENTER);
        levelPanel.addView(hint, matchWrap(dp(0)));
    }

    private Button makeLevelButton(int length) {
        Button button = new Button(this);
        button.setText(String.valueOf(length));
        button.setTextSize(42);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinHeight(dp(92));
        button.setContentDescription("Nivå " + length);
        button.setOnClickListener(v -> chooseLevel(length));
        return button;
    }

    private LinearLayout.LayoutParams weightedButtonParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(100), 1f);
        p.setMargins(dp(5), dp(0), dp(5), dp(0));
        return p;
    }

    private void buildGamePanel(LinearLayout root) {
        gamePanel = new LinearLayout(this);
        gamePanel.setOrientation(LinearLayout.VERTICAL);
        gamePanel.setVisibility(View.GONE);
        root.addView(gamePanel, matchWrap(dp(0)));

        chosenLevelText = new TextView(this);
        chosenLevelText.setTextColor(Color.WHITE);
        chosenLevelText.setTextSize(17);
        chosenLevelText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chosenLevelText.setGravity(Gravity.CENTER);
        gamePanel.addView(chosenLevelText, matchWrap(dp(8)));

        TextView subtitle = new TextView(this);
        subtitle.setText("10 ord till skatten!");
        subtitle.setTextColor(Color.WHITE);
        subtitle.setTextSize(18);
        subtitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        subtitle.setGravity(Gravity.CENTER);
        gamePanel.addView(subtitle, matchWrap(dp(14)));

        startText = new TextView(this);
        startText.setText("START  🧙");
        startText.setTextColor(Color.rgb(23, 50, 41));
        startText.setTextSize(18);
        startText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        startText.setGravity(Gravity.CENTER);
        startText.setBackgroundColor(Color.rgb(215, 199, 145));
        startText.setPadding(dp(10), dp(10), dp(10), dp(10));
        gamePanel.addView(startText, matchWrap(dp(8)));

        board = new GridLayout(this);
        board.setColumnCount(5);
        board.setRowCount(2);
        board.setPadding(dp(4), dp(4), dp(4), dp(4));
        board.setClipChildren(false);
        board.setClipToPadding(false);
        gamePanel.addView(board, matchWrap(dp(8)));

        progressText = new TextView(this);
        progressText.setTextColor(Color.WHITE);
        progressText.setTextSize(18);
        progressText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        progressText.setGravity(Gravity.CENTER);
        gamePanel.addView(progressText, matchWrap(dp(12)));

        celebrationText = new TextView(this);
        celebrationText.setText("⭐ HURRA! ⭐");
        celebrationText.setTextColor(Color.rgb(255, 232, 94));
        celebrationText.setTextSize(34);
        celebrationText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        celebrationText.setGravity(Gravity.CENTER);
        celebrationText.setVisibility(View.GONE);
        gamePanel.addView(celebrationText, matchWrap(dp(8)));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(20));
        card.setBackgroundColor(Color.rgb(255, 248, 223));
        gamePanel.addView(card, matchWrap(dp(12)));

        TextView instruction = new TextView(this);
        instruction.setText("LÄS ORDET");
        instruction.setTextColor(Color.rgb(80, 95, 85));
        instruction.setTextSize(18);
        instruction.setGravity(Gravity.CENTER);
        instruction.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(instruction, matchWrap(dp(4)));

        wordText = new TextView(this);
        wordText.setTextColor(Color.rgb(23, 50, 41));
        wordText.setTextSize(68);
        wordText.setGravity(Gravity.CENTER);
        wordText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        wordText.setLetterSpacing(0.08f);
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

        Button chooseAgain = new Button(this);
        chooseAgain.setText("1 · 2 · 3");
        chooseAgain.setContentDescription("Välj en annan nivå");
        chooseAgain.setTextSize(20);
        chooseAgain.setOnClickListener(v -> showLevelSelection());
        gamePanel.addView(chooseAgain, matchWrap(dp(0)));
    }

    private void chooseLevel(int level) {
        selectedLevel = level;
        levelChosen = true;
        position = 0;
        wordIndex = 0;
        celebrating = false;
        latestPartial = "";
        partialMatchedTarget = false;

        if (level == 1) {
            activeWords = new ArrayList<>(level1Words);
        } else if (level == 2) {
            activeWords = new ArrayList<>(level2Words);
        } else {
            activeWords = new ArrayList<>(level3Words);
        }

        Collections.shuffle(activeWords);
        currentWord = activeWords.get(0);

        levelPanel.setVisibility(View.GONE);
        gamePanel.setVisibility(View.VISIBLE);
        celebrationText.setVisibility(View.GONE);
        clearHeard();
        renderBoard();
        showWord();
        chosenLevelText.setText("NIVÅ " + level);
        feedbackText.setText("🎤");

        speak("Nivå " + spokenLevel(level) + ". Tryck på mikrofonen och läs ordet.");
    }

    private String spokenLevel(int level) {
        if (level == 1) return "ett";
        if (level == 2) return "två";
        return "tre";
    }

    private void showLevelSelection() {
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
        }
        if (tts != null) {
            tts.stop();
        }

        levelChosen = false;
        celebrating = false;
        gamePanel.setVisibility(View.GONE);
        levelPanel.setVisibility(View.VISIBLE);
        handler.postDelayed(this::speakLevelQuestion, 250);
    }

    private void renderBoard() {
        board.removeAllViews();
        heroView = null;
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

            if (position == i) {
                heroView = cell;
            }
        }

        if (position < 10) {
            progressText.setText((10 - position) + " kvar");
        } else {
            progressText.setText("🏆 MÅL!");
        }
    }

    private void showWord() {
        wordText.setText(spaced(currentWord).toUpperCase(new Locale("sv", "SE")));
        boolean recognitionAvailable = SpeechRecognizer.isRecognitionAvailable(this);
        listenButton.setEnabled(position < 10 && recognitionAvailable);
        if (!recognitionAvailable) {
            feedbackText.setText("🎤 🚫");
        }
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
        if (heardLabel == null || heardText == null) return;
        heardLabel.setVisibility(View.GONE);
        heardText.setVisibility(View.GONE);
        heardText.setText("");
        heardText.setBackgroundColor(Color.TRANSPARENT);
    }

    private void showHeard(String text, boolean correct) {
        heardLabel.setText("JAG HÖRDE");
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
                if (!levelChosen) return;
                feedbackText.setText("👂");
                latestPartial = "";
            }

            @Override public void onBeginningOfSpeech() {
                if (levelChosen) feedbackText.setText("👂 …");
            }

            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}

            @Override public void onEndOfSpeech() {
                if (levelChosen) feedbackText.setText("⏳");
            }

            @Override public void onError(int error) {
                if (!levelChosen || feedbackText == null) return;
                listenButton.setEnabled(true);

                if (partialMatchedTarget) {
                    showHeard(currentWord, true);
                    advance();
                    return;
                }

                if (!latestPartial.isEmpty()) {
                    showHeard(latestPartial, false);
                    feedbackText.setText("🔁");
                    speak("Försök igen.");
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
                if (!levelChosen) return;
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
                boolean correct = partialMatchedTarget;

                for (String heard : matches) {
                    if (matchesTarget(heard, currentWord)) {
                        correct = true;
                        best = heard;
                        break;
                    }
                }

                if (partialMatchedTarget && !correct) {
                    best = currentWord;
                    correct = true;
                }

                showHeard(correct ? currentWord : best, correct);

                if (correct) {
                    advance();
                } else {
                    feedbackText.setText("🔁");
                    speak("Försök igen.");
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {
                if (!levelChosen) return;
                ArrayList<String> partial =
                        partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (partial != null && !partial.isEmpty()) {
                    latestPartial = partial.get(0).trim();

                    for (String candidate : partial) {
                        if (matchesTarget(candidate, currentWord)) {
                            partialMatchedTarget = true;
                            latestPartial = candidate.trim();
                            break;
                        }
                    }

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
        if (!levelChosen || position >= 10 || celebrating) return;

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
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
        );
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sv-SE");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "sv-SE");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1200L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 700L);

        if (Build.VERSION.SDK_INT >= 33) {
            ArrayList<String> bias = new ArrayList<>();
            bias.add(currentWord);
            if (currentWord.equals("vi")) bias.add("v");
            if (currentWord.equals("är")) bias.add("r");
            intent.putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, bias);
        }

        latestPartial = "";
        partialMatchedTarget = false;
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

        // Android may transcribe the spoken Swedish words "vi" and "är"
        // as the single letters V and R.
        if (t.equals("vi") && h.equals("v")) return true;
        if (t.equals("är") && h.equals("r")) return true;

        return false;
    }

    private String normalize(String s) {
        if (s == null) return "";
        String value = s.toLowerCase(new Locale("sv", "SE")).trim();
        value = Normalizer.normalize(value, Normalizer.Form.NFC);
        return value.replaceAll("[^a-zåäö ]", "").replaceAll("\\s+", " ");
    }

    private void playStepEffect() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 100);
    }

    private void playFinalCelebration() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 120);
        handler.postDelayed(() ->
                toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 180), 150);
        handler.postDelayed(() ->
                toneGenerator.startTone(ToneGenerator.TONE_DTMF_9, 240), 340);

        celebrationText.setVisibility(View.VISIBLE);
        celebrationText.setText("⭐ DU KAN LÄSA! ⭐");

        ScaleAnimation pop = new ScaleAnimation(
                0.55f, 1.15f, 0.55f, 1.15f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        pop.setDuration(300);
        pop.setRepeatMode(Animation.REVERSE);
        pop.setRepeatCount(3);
        celebrationText.startAnimation(pop);

        if (heroView != null) {
            heroView.animate()
                    .rotationBy(720f)
                    .translationY(-dp(42))
                    .scaleX(1.55f)
                    .scaleY(1.55f)
                    .setDuration(1250)
                    .withEndAction(() -> {
                        if (heroView != null) {
                            heroView.animate()
                                    .translationX(dp(180))
                                    .translationY(-dp(85))
                                    .alpha(0f)
                                    .setDuration(900)
                                    .start();
                        }
                    })
                    .start();
        }

        speak("Hurra hurra! Du kan läsa!");
    }

    private void advance() {
        celebrating = true;
        position++;
        renderBoard();
        feedbackText.setText("✅");
        listenButton.setEnabled(false);

        if (position >= 10) {
            wordText.setText("🏆");
            progressText.setText("🏆 MÅL!");
            playFinalCelebration();

            handler.postDelayed(() -> {
                showLevelSelection();
            }, 5200);
            return;
        }

        playStepEffect();

        handler.postDelayed(() -> {
            wordIndex++;
            if (wordIndex >= activeWords.size()) {
                wordIndex = 0;
                Collections.shuffle(activeWords);
            }
            currentWord = activeWords.get(wordIndex);
            clearHeard();
            celebrationText.setVisibility(View.GONE);
            showWord();
            feedbackText.setText("🎤");
            celebrating = false;
            // No spoken success message between words; this also prevents TTS
            // from competing with the next microphone attempt.
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
                speak("Bra. Nu kan jag lyssna. Läs ordet.");
                handler.postDelayed(this::startListening, 700);
            } else {
                if (feedbackText != null) feedbackText.setText("🎤 🚫");
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
