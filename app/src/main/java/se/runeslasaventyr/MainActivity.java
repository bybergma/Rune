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

    private static final int BG = Color.rgb(241, 236, 191);
    private static final int PANEL = Color.rgb(255, 249, 225);
    private static final int BLUE = Color.rgb(82, 123, 196);
    private static final int YELLOW = Color.rgb(255, 214, 73);
    private static final int GREEN = Color.rgb(92, 168, 109);
    private static final int GREEN_DARK = Color.rgb(57, 120, 72);
    private static final int RED = Color.rgb(224, 87, 80);
    private static final int RED_DARK = Color.rgb(153, 53, 53);
    private static final int TEXT = Color.rgb(36, 36, 36);
    private static final int SOFT_BORDER = Color.rgb(197, 194, 173);
    private static final int STEP_PENDING = Color.rgb(240, 244, 255);
    private static final int STEP_DONE = Color.rgb(210, 241, 216);
    private static final int STEP_CURRENT = Color.rgb(255, 233, 135);

    private final List<String> level1Words = new ArrayList<>(Arrays.asList(
            "is", "gå", "åt", "du", "vi", "om", "by", "är", "aj", "ha",
            "ja", "nu", "se", "på", "ut", "in", "må", "sa", "sy", "bo"
    ));

    private final List<String> level2Words = new ArrayList<>(Arrays.asList(
            "sol", "kor", "ror", "bil", "mus", "hus", "bok", "mat", "fis",
            "båt", "tåg", "nöt", "räv", "pil", "mal", "säl", "vas", "orm", "ben", "apa"
    ));

    private final List<String> level3Words = new ArrayList<>(Arrays.asList(
            "boll", "katt", "hund", "bord", "stol", "glas", "fisk", "läsa", "måne", "bajs",
            "rosa", "gult", "grön", "saft", "mata", "resa", "vind", "kaka", "skor", "snor", "rapa"
    ));

    private LinearLayout levelPanel;
    private LinearLayout gamePanel;
    private GridLayout board;
    private LinearLayout targetWordRow;
    private LinearLayout heardWordRow;
    private TextView heardLabel;
    private TextView feedbackText;
    private TextView progressText;
    private TextView celebrationText;
    private TextView chosenLevelText;
    private TextView themeText;
    private Button listenButton;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    private ToneGenerator toneGenerator;

    private List<String> currentBoardWords = new ArrayList<>();
    private String currentWord = "";
    private int currentLevel = 1;
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

        TextView hint1 = new TextView(this);
        hint1.setText("1 = PIKACHU");
        hint1.setTextColor(TEXT);
        hint1.setTextSize(18);
        hint1.setGravity(Gravity.CENTER);
        levelPanel.addView(hint1, matchWrap(dp(4)));

        TextView hint2 = new TextView(this);
        hint2.setText("2 = BAJSKORV");
        hint2.setTextColor(TEXT);
        hint2.setTextSize(18);
        hint2.setGravity(Gravity.CENTER);
        levelPanel.addView(hint2, matchWrap(dp(4)));

        TextView hint3 = new TextView(this);
        hint3.setText("3 = MARIO");
        hint3.setTextColor(TEXT);
        hint3.setTextSize(18);
        hint3.setGravity(Gravity.CENTER);
        levelPanel.addView(hint3, matchWrap(dp(14)));

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
        gamePanel.addView(chosenLevelText, matchWrap(dp(2)));

        themeText = new TextView(this);
        themeText.setText("PIKACHU TILL POKÉBOLL");
        themeText.setTextColor(TEXT);
        themeText.setTextSize(18);
        themeText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        themeText.setGravity(Gravity.CENTER);
        gamePanel.addView(themeText, matchWrap(dp(8)));

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

        targetWordRow = new LinearLayout(this);
        targetWordRow.setOrientation(LinearLayout.HORIZONTAL);
        targetWordRow.setGravity(Gravity.CENTER);
        card.addView(targetWordRow, matchWrap(dp(10)));

        TextView touchHint = new TextView(this);
        touchHint.setText("Tryck på en bokstav för att höra ljudet");
        touchHint.setTextColor(TEXT);
        touchHint.setTextSize(15);
        touchHint.setGravity(Gravity.CENTER);
        card.addView(touchHint, matchWrap(dp(10)));

        heardLabel = new TextView(this);
        heardLabel.setText("DU SA");
        heardLabel.setTextColor(TEXT);
        heardLabel.setTextSize(15);
        heardLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heardLabel.setGravity(Gravity.CENTER);
        heardLabel.setVisibility(View.GONE);
        card.addView(heardLabel, matchWrap(dp(4)));

        heardWordRow = new LinearLayout(this);
        heardWordRow.setOrientation(LinearLayout.HORIZONTAL);
        heardWordRow.setGravity(Gravity.CENTER);
        heardWordRow.setVisibility(View.GONE);
        card.addView(heardWordRow, matchWrap(dp(12)));

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

    private GradientDrawable makeLetterDrawable(int fillColor, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fillColor);
        d.setCornerRadius(dp(10));
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

    private void speakLetterSound(char letter) {
        if (!ttsReady || tts == null) return;

        String sound;
        switch (Character.toLowerCase(letter)) {
            case 'a': sound = "aaa"; break;
            case 'b': sound = "buh"; break;
            case 'd': sound = "duh"; break;
            case 'e': sound = "eee"; break;
            case 'f': sound = "fff"; break;
            case 'g': sound = "guh"; break;
            case 'h': sound = "hhh"; break;
            case 'i': sound = "iii"; break;
            case 'j': sound = "jjj"; break;
            case 'k': sound = "kuh"; break;
            case 'l': sound = "lll"; break;
            case 'm': sound = "mmm"; break;
            case 'n': sound = "nnn"; break;
            case 'o': sound = "ooo"; break;
            case 'p': sound = "puh"; break;
            case 'r': sound = "rrr"; break;
            case 's': sound = "sss"; break;
            case 't': sound = "tuh"; break;
            case 'u': sound = "uuu"; break;
            case 'v': sound = "vvv"; break;
            case 'y': sound = "yyy"; break;
            case 'å': sound = "ååå"; break;
            case 'ä': sound = "äää"; break;
            case 'ö': sound = "ööö"; break;
            default: sound = String.valueOf(letter); break;
        }

        tts.stop();
        tts.speak(sound, TextToSpeech.QUEUE_FLUSH, null, "LetterSound");
    }

    private void chooseLevel(int level) {
        currentLevel = level;
        levelChosen = true;
        position = 0;
        wordIndex = 0;
        celebrating = false;
        latestPartial = "";
        partialMatchedTarget = false;

        List<String> pool = new ArrayList<>();
        if (level == 1) {
            pool.addAll(level1Words);
            themeText.setText("PIKACHU TILL POKÉBOLL");
        } else if (level == 2) {
            pool.addAll(level2Words);
            themeText.setText("BAJSKORV TILL TOALETT");
        } else {
            pool.addAll(level3Words);
            themeText.setText("MARIO TILL PRINSESSAN");
        }

        Collections.shuffle(pool);
        currentBoardWords = new ArrayList<>(pool.subList(0, Math.min(10, pool.size())));
        currentWord = currentBoardWords.get(0);

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
                addThemeHero(cell);
                heroView = cell;
            } else if (isGoal) {
                addThemeGoal(cell);
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

    private void addThemeHero(LinearLayout cell) {
        if (currentLevel == 1) {
            ImageView hero = new ImageView(this);
            hero.setImageResource(R.drawable.pikachu);
            hero.setAdjustViewBounds(true);
            hero.setMaxWidth(dp(38));
            hero.setMaxHeight(dp(38));
            cell.addView(hero, wrapWrap(dp(3)));
        } else {
            TextView hero = new TextView(this);
            hero.setText(currentLevel == 2 ? "💩" : "🍄");
            hero.setTextSize(28);
            hero.setGravity(Gravity.CENTER);
            cell.addView(hero, wrapWrap(dp(2)));
        }
    }

    private void addThemeGoal(LinearLayout cell) {
        if (currentLevel == 1) {
            ImageView goal = new ImageView(this);
            goal.setImageResource(R.drawable.pokeball);
            goal.setAdjustViewBounds(true);
            goal.setMaxWidth(dp(34));
            goal.setMaxHeight(dp(34));
            cell.addView(goal, wrapWrap(dp(3)));
        } else {
            TextView goal = new TextView(this);
            goal.setText(currentLevel == 2 ? "🚽" : "👸");
            goal.setTextSize(28);
            goal.setGravity(Gravity.CENTER);
            cell.addView(goal, wrapWrap(dp(2)));
        }
    }

    private void showWord() {
        targetWordRow.removeAllViews();
        String display = currentWord.toUpperCase(new Locale("sv", "SE"));
        for (int i = 0; i < display.length(); i++) {
            final char letter = display.charAt(i);
            TextView tile = makeLetterTile(String.valueOf(letter), PANEL, SOFT_BORDER, TEXT, 54, true);
            tile.setOnClickListener(v -> speakLetterSound(letter));
            targetWordRow.addView(tile);
        }

        boolean recognitionAvailable = SpeechRecognizer.isRecognitionAvailable(this);
        listenButton.setEnabled(position < 10 && recognitionAvailable);
        if (!recognitionAvailable) {
            feedbackText.setText("🎤 🚫");
            feedbackText.setTextColor(RED_DARK);
        }
    }

    private TextView makeLetterTile(String text, int fill, int stroke, int textColor, int textSize, boolean clickable) {
        TextView tile = new TextView(this);
        tile.setText(text);
        tile.setTextColor(textColor);
        tile.setTextSize(textSize);
        tile.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tile.setGravity(Gravity.CENTER);
        tile.setMinWidth(dp(58));
        tile.setMinHeight(dp(68));
        tile.setPadding(dp(12), dp(8), dp(12), dp(8));
        tile.setBackground(makeLetterDrawable(fill, stroke));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(dp(4), dp(0), dp(4), dp(0));
        tile.setLayoutParams(lp);

        if (clickable) {
            tile.setClickable(true);
            tile.setFocusable(true);
        }
        return tile;
    }

    private void clearHeard() {
        heardLabel.setVisibility(View.GONE);
        heardWordRow.setVisibility(View.GONE);
        heardWordRow.removeAllViews();
    }

    private void showHeardComparison(String heardRaw) {
        String heard = normalizeForDisplay(heardRaw);
        if (heard.isEmpty()) {
            clearHeard();
            return;
        }

        heardLabel.setText("DU SA");
        heardLabel.setVisibility(View.VISIBLE);
        heardWordRow.setVisibility(View.VISIBLE);
        heardWordRow.removeAllViews();

        String target = normalizeForDisplay(currentWord);
        for (int i = 0; i < heard.length(); i++) {
            char letter = Character.toUpperCase(heard.charAt(i));
            boolean matches = i < target.length() && heard.charAt(i) == target.charAt(i);

            int fill = matches ? STEP_DONE : Color.rgb(255, 231, 231);
            int stroke = matches ? GREEN : RED;
            int textColor = matches ? GREEN_DARK : RED_DARK;

            TextView tile = makeLetterTile(String.valueOf(letter), fill, stroke, textColor, 42, false);
            heardWordRow.addView(tile);
        }

        ScaleAnimation pop = new ScaleAnimation(
                0.85f, 1.0f, 0.85f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        pop.setDuration(180);
        heardWordRow.startAnimation(pop);
    }

    private String normalizeForDisplay(String s) {
        if (s == null) return "";
        String value = s.toLowerCase(new Locale("sv", "SE")).trim();
        value = Normalizer.normalize(value, Normalizer.Form.NFC);
        value = value.replaceAll("[^a-zåäö ]", "").replaceAll("\\s+", "");
        return value;
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
                    showHeardComparison(currentWord);
                    advance();
                    return;
                }

                if (!latestPartial.isEmpty()) {
                    showHeardComparison(latestPartial);
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
                        showHeardComparison(latestPartial);
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

                showHeardComparison(correct ? currentWord : best);

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

                    if (!normalizeForDisplay(latestPartial).isEmpty()) {
                        showHeardComparison(latestPartial);
                        feedbackText.setText("👂 …");
                        feedbackText.setTextColor(TEXT);
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
            String displayTarget = normalizeForDisplay(currentWord);
            if (displayTarget.equals("vi")) bias.add("v");
            if (displayTarget.equals("är")) bias.add("r");
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
        String h = normalizeForDisplay(heard);
        String t = normalizeForDisplay(target);

        if (h.equals(t)) return true;

        String normalizedHeard = normalize(heard);
        for (String part : normalizedHeard.split(" ")) {
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
            progressText.setText("MÅL!");
            playFinalCelebration();
            handler.postDelayed(this::showLevelSelection, 5200);
            return;
        }

        playStepEffect();

        handler.postDelayed(() -> {
            wordIndex++;
            if (wordIndex < currentBoardWords.size()) {
                currentWord = currentBoardWords.get(wordIndex);
            }
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
