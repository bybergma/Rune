package se.runeslasaventyr;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.ImageView;
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

    // Child-friendly palette with clear meaning:
    // green = right / progress, red = wrong / retry
    private static final int BG = Color.rgb(241, 236, 191);          // warm sand
    private static final int PANEL = Color.rgb(255, 249, 225);       // soft cream
    private static final int BLUE = Color.rgb(82, 123, 196);         // pokemon-like blue
    private static final int YELLOW = Color.rgb(255, 214, 73);       // pokemon-like yellow
    private static final int GREEN = Color.rgb(92, 168, 109);        // correct
    private static final int GREEN_DARK = Color.rgb(57, 120, 72);
    private static final int RED = Color.rgb(224, 87, 80);           // wrong
    private static final int RED_DARK = Color.rgb(153, 53, 53);
    private static final int TEXT = Color.rgb(36, 36, 36);
    private static final int SOFT_BORDER = Color.rgb(197, 194, 173);
    private static final int STEP_PENDING = Color.rgb(240, 244, 255);
    private static final int STEP_DONE = Color.rgb(210, 241, 216);
    private static final int STEP_CURRENT = Color.rgb(255, 233, 135);

    private final List<String> level1Words = new ArrayList<>(Arrays.asList(
            "is", "gå", "åt", "du", "vi", "om", "by", "är", "aj", "ha"
    ));
    private final List<String> level2Words = new ArrayList<>(Arrays.asList(
            "sol", "kor", "ror", "bil", "mus", "hus", "bok", "mat", "fis"
    ));
    private final List<String> level3Words = new ArrayList<>(Arrays.asList(
            "boll", "katt", "hund", "bord", "stol",
            "glas", "fisk", "läsa", "måne", "bajs"
    ));

    private LinearLayout levelPanel;
    private LinearLayout gamePanel;
    private GridLayout board;
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
    private int position = 0;
    private int wordIndex = 0;
    private boolean ttsReady = false;
    private boolean celebrating = false;
    private boolean levelChosen = false;
    private String latestPartial = "";
    private boolean partialMatchedTarget = false;
    private View heroView;

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

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        root.setBackgroundColor(BG);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Runes Läsäventyr");
        title.setTextColor(TEXT);
        title.setTextSize(30);
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
        levelPanel.setBackground(makePanelDrawable());
        root.addView(levelPanel, matchWrap(dp(0)));

        TextView question = new TextView(this);
        question.setText("VÄLJ NIVÅ");
        question.setTextColor(TEXT);
        question.setTextSize(26);
        question.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        question.setGravity(Gravity.CENTER);
        levelPanel.addView(question, matchWrap(dp(14)));

        Button hearQuestion = new Button(this);
        hearQuestion.setText("🔊 LYSSNA");
        hearQuestion.setAllCaps(false);
        hearQuestion.setTextSize(20);
        styleActionButton(hearQuestion, BLUE, Color.WHITE);
        hearQuestion.setOnClickListener(v -> speakLevelQuestion());
        levelPanel.addView(hearQuestion, matchWrap(dp(18)));

        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        choices.setGravity(Gravity.CENTER);
        levelPanel.addView(choices, matchWrap(dp(10)));

        choices.addView(makeLevelButton(1), weightedButtonParams());
        choices.addView(makeLevelButton(2), weightedButtonParams());
        choices.addView(makeLevelButton(3), weightedButtonParams());

        TextView hint = new TextView(this);
        hint.setText("1      2      3");
        hint.setTextColor(TEXT);
        hint.setTextSize(18);
        hint.setGravity(Gravity.CENTER);
        levelPanel.addView(hint, matchWrap(dp(0)));
    }

    private Button makeLevelButton(int level) {
        Button button = new Button(this);
        button.setText(String.valueOf(level));
        button.setTextSize(28);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinHeight(dp(82));
        button.setContentDescription("Nivå " + level);
        styleActionButton(button, BLUE, Color.WHITE);
        button.setOnClickListener(v -> chooseLevel(level));
        return button;
    }

    private LinearLayout.LayoutParams weightedButtonParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        p.setMargins(dp(5), dp(0), dp(5), dp(0));
        return p;
    }

    private void buildGamePanel(LinearLayout root) {
        gamePanel = new LinearLayout(this);
        gamePanel.setOrientation(LinearLayout.VERTICAL);
        gamePanel.setVisibility(View.GONE);
        root.addView(gamePanel, matchWrap(dp(0)));

        chosenLevelText = new TextView(this);
        chosenLevelText.setText("NIVÅ 1");
        chosenLevelText.setTextColor(BLUE);
        chosenLevelText.setTextSize(22);
        chosenLevelText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chosenLevelText.setGravity(Gravity.CENTER);
        gamePanel.addView(chosenLevelText, matchWrap(dp(4)));

        progressText = new TextView(this);
        progressText.setText("10 kvar");
        progressText.setTextColor(TEXT);
        progressText.setTextSize(24);
        progressText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        progressText.setGravity(Gravity.CENTER);
        gamePanel.addView(progressText, matchWrap(dp(12)));

        TextView startText = new TextView(this);
        startText.setText("START");
        startText.setTextColor(TEXT);
        startText.setTextSize(18);
        startText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        startText.setGravity(Gravity.CENTER);
        gamePanel.addView(startText, matchWrap(dp(6)));

        board = new GridLayout(this);
        board.setColumnCount(5);
        board.setRowCount(2);
        board.setPadding(dp(2), dp(2), dp(2), dp(2));
        board.setClipChildren(false);
        board.setClipToPadding(false);
        gamePanel.addView(board, matchWrap(dp(12)));

        celebrationText = new TextView(this);
        celebrationText.setText("DU KAN LÄSA!");
        celebrationText.setTextColor(GREEN_DARK);
        celebrationText.setTextSize(32);
        celebrationText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        celebrationText.setGravity(Gravity.CENTER);
        celebrationText.setVisibility(View.GONE);
        gamePanel.addView(celebrationText, matchWrap(dp(10)));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(20));
        card.setBackground(makePanelDrawable());
        gamePanel.addView(card, matchWrap(dp(10)));

        TextView instruction = new TextView(this);
        instruction.setText("LÄS ORDET");
        instruction.setTextColor(TEXT);
        instruction.setTextSize(22);
        instruction.setGravity(Gravity.CENTER);
        instruction.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(instruction, matchWrap(dp(8)));

        wordText = new TextView(this);
        wordText.setTextColor(TEXT);
        wordText.setTextSize(72);
        wordText.setGravity(Gravity.CENTER);
        wordText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        wordText.setSingleLine(true);
        wordText.setLetterSpacing(0.03f);
        wordText.setPadding(dp(6), dp(8), dp(6), dp(12));
        card.addView(wordText, matchWrap(dp(12)));

        listenButton = new Button(this);
        listenButton.setText("🎤");
        listenButton.setTextSize(38);
        listenButton.setMinHeight(dp(80));
        styleActionButton(listenButton, YELLOW, TEXT);
        listenButton.setOnClickListener(v -> startListening());
        card.addView(listenButton, matchWrap(dp(10)));

        Button listenAgain = new Button(this);
        listenAgain.setText("🔊 LYSSNA");
        listenAgain.setAllCaps(false);
        listenAgain.setTextSize(20);
        styleActionButton(listenAgain, BLUE, Color.WHITE);
        listenAgain.setOnClickListener(v -> speak("Tryck på mikrofonen och läs ordet."));
        card.addView(listenAgain, matchWrap(dp(12)));

        heardLabel = new TextView(this);
        heardLabel.setText("JAG HÖRDE");
        heardLabel.setTextColor(TEXT);
        heardLabel.setTextSize(15);
        heardLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heardLabel.setGravity(Gravity.CENTER);
        heardLabel.setVisibility(View.GONE);
        card.addView(heardLabel, matchWrap(dp(2)));

        heardText = new TextView(this);
        heardText.setTextColor(TEXT);
        heardText.setTextSize(52);
        heardText.setGravity(Gravity.CENTER);
        heardText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heardText.setSingleLine(true);
        heardText.setPadding(dp(8), dp(10), dp(8), dp(10));
        heardText.setVisibility(View.GONE);
        card.addView(heardText, matchWrap(dp(8)));

        feedbackText = new TextView(this);
        feedbackText.setText("🎤");
        feedbackText.setTextColor(TEXT);
        feedbackText.setTextSize(22);
        feedbackText.setGravity(Gravity.CENTER);
        feedbackText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        feedbackText.setPadding(dp(8), dp(8), dp(8), dp(8));
        card.addView(feedbackText, matchWrap(dp(6)));

        Button chooseAgain = new Button(this);
        chooseAgain.setText("VÄLJ NIVÅ IGEN");
        chooseAgain.setAllCaps(false);
        chooseAgain.setTextSize(18);
        styleActionButton(chooseAgain, BLUE, Color.WHITE);
        chooseAgain.setOnClickListener(v -> showLevelSelection());
        gamePanel.addView(chooseAgain, matchWrap(dp(0)));
    }

    private void styleActionButton(Button button, int fill, int textColor) {
        button.setBackground(makeButtonDrawable(fill));
        button.setTextColor(textColor);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    }

    private GradientDrawable makeButtonDrawable(int fillColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fillColor);
        d.setCornerRadius(dp(16));
        d.setStroke(dp(2), TEXT);
        return d;
    }

    private GradientDrawable makePanelDrawable() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(PANEL);
        d.setCornerRadius(dp(18));
        d.setStroke(dp(1), SOFT_BORDER);
        return d;
    }

    private GradientDrawable makeStepDrawable(int fillColor, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fillColor);
        d.setCornerRadius(dp(12));
        d.setStroke(dp(2), strokeColor);
        return d;
    }

    private void setupTextToSpeech() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("sv", "SE"));
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
                tts.setSpeechRate(0.92f);
                if (ttsReady && !levelChosen) speakLevelQuestion();
            }
        });
    }

    private void speakLevelQuestion() {
        speak("Vilken nivå vill du läsa? Tryck på ett, två eller tre.");
    }

    private void speak(String text) {
        if (!ttsReady || tts == null) return;
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "RuneSpeech");
    }

    private void chooseLevel(int level) {
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
        chosenLevelText.setText("NIVÅ " + level);
        clearHeard();
        renderBoard();
        showWord();
        feedbackText.setText("🎤");
        feedbackText.setTextColor(TEXT);

        speak("Nivå " + spokenLevel(level) + ". Tryck på mikrofonen och läs ordet.");
    }

    private String spokenLevel(int level) {
        if (level == 1) return "ett";
        if (level == 2) return "två";
        return "tre";
    }

    private void showLevelSelection() {
        if (speechRecognizer != null) speechRecognizer.cancel();
        if (tts != null) tts.stop();

        levelChosen = false;
        celebrating = false;
        gamePanel.setVisibility(View.GONE);
        levelPanel.setVisibility(View.VISIBLE);
        handler.postDelayed(this::speakLevelQuestion, 250);
    }

    private void renderBoard() {
        board.removeAllViews();
        heroView = null;

        for (int i = 1; i <= 10; i++) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(4), dp(4), dp(4), dp(4));

            boolean isCurrent = position == i;
            boolean isGoal = i == 10;
            boolean isDone = i <= position;

            int fill = isCurrent ? STEP_CURRENT : (isDone ? STEP_DONE : STEP_PENDING);
            int border = isGoal ? RED : (isDone ? GREEN : BLUE);
            cell.setBackground(makeStepDrawable(fill, border));

            if (isCurrent) {
                ImageView hero = new ImageView(this);
                hero.setImageResource(R.drawable.pikachu);
                hero.setAdjustViewBounds(true);
                hero.setMaxWidth(dp(38));
                hero.setMaxHeight(dp(38));
                cell.addView(hero, wrapWrap(dp(3)));
                heroView = cell;
            } else if (isGoal) {
                ImageView ball = new ImageView(this);
                ball.setImageResource(R.drawable.pokeball);
                ball.setAdjustViewBounds(true);
                ball.setMaxWidth(dp(34));
                ball.setMaxHeight(dp(34));
                cell.addView(ball, wrapWrap(dp(3)));
            }

            TextView number = new TextView(this);
            number.setText(String.valueOf(i));
            number.setTextColor(TEXT);
            number.setTextSize(isCurrent ? 21 : 19);
            number.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            number.setGravity(Gravity.CENTER);
            cell.addView(number, wrapWrap(0));

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = dp(98);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            board.addView(cell, lp);
        }

        if (position < 10) {
            progressText.setText((10 - position) + " kvar");
        } else {
            progressText.setText("MÅL!");
        }
    }

    private void showWord() {
        wordText.setText(currentWord.toUpperCase(new Locale("sv", "SE")));
        boolean recognitionAvailable = SpeechRecognizer.isRecognitionAvailable(this);
        listenButton.setEnabled(position < 10 && recognitionAvailable);
        if (!recognitionAvailable) {
            feedbackText.setText("🎤 🚫");
            feedbackText.setTextColor(RED_DARK);
        }
    }

    private void clearHeard() {
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
        heardText.setTextColor(correct ? GREEN_DARK : RED_DARK);
        heardText.setBackground(makeStepDrawable(correct ? STEP_DONE : Color.rgb(255, 231, 231),
                correct ? GREEN : RED));

        ScaleAnimation pop = new ScaleAnimation(
                0.75f, 1.0f, 0.75f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        pop.setDuration(240);
        heardText.startAnimation(pop);
    }

    private void setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                if (!levelChosen) return;
                feedbackText.setText("👂");
                feedbackText.setTextColor(TEXT);
                latestPartial = "";
                partialMatchedTarget = false;
            }

            @Override public void onBeginningOfSpeech() {
                if (levelChosen) {
                    feedbackText.setText("👂 …");
                    feedbackText.setTextColor(TEXT);
                }
            }

            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}

            @Override public void onEndOfSpeech() {
                if (levelChosen) {
                    feedbackText.setText("⏳");
                    feedbackText.setTextColor(TEXT);
                }
            }

            @Override public void onError(int error) {
                if (!levelChosen) return;
                listenButton.setEnabled(true);

                if (partialMatchedTarget) {
                    showHeard(currentWord, true);
                    advance();
                    return;
                }

                if (!latestPartial.isEmpty()) {
                    showHeard(latestPartial, false);
                    feedbackText.setText("❌ FEL");
                    feedbackText.setTextColor(RED_DARK);
                    speak("Försök igen.");
                    return;
                }

                clearHeard();
                feedbackText.setText("❌ FEL");
                feedbackText.setTextColor(RED_DARK);
                if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    speak("Jag hörde inget. Försök igen.");
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
                    feedbackText.setText("❌ FEL");
                    feedbackText.setTextColor(RED_DARK);
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
                    correct = true;
                    best = currentWord;
                }

                showHeard(correct ? currentWord : best, correct);

                if (correct) {
                    advance();
                } else {
                    feedbackText.setText("❌ FEL");
                    feedbackText.setTextColor(RED_DARK);
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
                        heardText.setTextColor(TEXT);
                        heardText.setBackground(makeStepDrawable(Color.WHITE, BLUE));
                    }
                }
            }

            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startListening() {
        if (celebrating || !levelChosen || position >= 10) return;

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                    AUDIO_PERMISSION_REQUEST);
            return;
        }

        if (speechRecognizer == null) setupSpeechRecognizer();
        if (tts != null) tts.stop();

        clearHeard();
        latestPartial = "";
        partialMatchedTarget = false;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
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

        listenButton.setEnabled(false);
        feedbackText.setText("🎤 …");
        feedbackText.setTextColor(TEXT);

        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            listenButton.setEnabled(true);
            feedbackText.setText("❌ FEL");
            feedbackText.setTextColor(RED_DARK);
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
        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 90);
    }

    private void playFinalCelebration() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 120);
        handler.postDelayed(() ->
                toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 180), 150);
        handler.postDelayed(() ->
                toneGenerator.startTone(ToneGenerator.TONE_DTMF_9, 240), 340);

        celebrationText.setVisibility(View.VISIBLE);
        celebrationText.setText("⭐ DU KAN LÄSA! ⭐");
        celebrationText.setTextColor(GREEN_DARK);

        ScaleAnimation pop = new ScaleAnimation(
                0.55f, 1.15f, 0.55f, 1.15f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        pop.setDuration(300);
        pop.setRepeatMode(Animation.REVERSE);
        pop.setRepeatCount(3);
        celebrationText.startAnimation(pop);

        AlphaAnimation flash = new AlphaAnimation(0.35f, 1.0f);
        flash.setDuration(180);
        flash.setRepeatMode(Animation.REVERSE);
        flash.setRepeatCount(3);
        board.startAnimation(flash);

        if (heroView != null) {
            heroView.animate()
                    .rotationBy(720f)
                    .translationY(-dp(38))
                    .scaleX(1.4f)
                    .scaleY(1.4f)
                    .setDuration(1200)
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
        feedbackText.setText("✅ RÄTT");
        feedbackText.setTextColor(GREEN_DARK);
        listenButton.setEnabled(false);

        if (position >= 10) {
            wordText.setText("MÅL!");
            progressText.setText("MÅL!");
            playFinalCelebration();

            handler.postDelayed(this::showLevelSelection, 5200);
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
            feedbackText.setTextColor(TEXT);
            celebrating = false;
        }, 650);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == AUDIO_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                speak("Bra. Nu kan jag lyssna. Läs ordet.");
                handler.postDelayed(this::startListening, 700);
            } else {
                feedbackText.setText("🎤 🚫");
                feedbackText.setTextColor(RED_DARK);
                speak("Mikrofonen behöver tillåtelse för att kunna lyssna.");
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (toneGenerator != null) toneGenerator.release();
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

    private LinearLayout.LayoutParams wrapWrap(int bottomMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.bottomMargin = bottomMargin;
        p.gravity = Gravity.CENTER_HORIZONTAL;
        return p;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
