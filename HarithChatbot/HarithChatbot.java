import javax.swing.*;
import java.awt.*;
import java.util.Random;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

public class HarithChatbot extends JFrame {

    private JTextArea chatArea;
    private JTextField msgInput;
    private JButton btnSend, btnClear;

    
    private static final String GEMINI_API_KEY = "API_KEY_REMOVED_FOR_SECURITY";
    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="
                    + GEMINI_API_KEY;

    private static final String[] offlineReplies = {
            "I don't know what you're talking about.",
            "How can I help you?",
            "Hmm... not sure about that. Try asking differently?"
    };

    public HarithChatbot() {
        setTitle("ChatBot");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        msgInput = new JTextField();
        btnSend = new JButton("Send");
        btnClear = new JButton("Clear");

        inputPanel.add(msgInput, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(btnSend);
        buttonPanel.add(btnClear);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        add(inputPanel, BorderLayout.SOUTH);

        showMessage("ChatBot", "Hey! I'm ChatBot. Fire away with your questions!");

        btnSend.addActionListener(e -> sendMessage());
        btnClear.addActionListener(e -> chatArea.setText(""));
        msgInput.addActionListener(e -> sendMessage());
    }

    private void showMessage(String who, String msg) {
        chatArea.append(who + ": " + msg + "\n");
    }

    private void sendMessage() {
        String userMsg = msgInput.getText().trim();

        if (userMsg.isEmpty()) {
            showMessage("ChatBot", "Please type something before hitting send.");
            return;
        }

        showMessage("You", userMsg);
        msgInput.setText("");

        Thread botThread = new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() ->
                        showMessage("ChatBot", "🤖 is typing..."));

                Thread.sleep(2000);

                String reply = getReply(userMsg);

                SwingUtilities.invokeLater(() ->
                        showMessage("ChatBot", "🤖 " + reply));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        botThread.start();
    }

    private String getReply(String text) {
        String lower = text.toLowerCase();

        if (lower.contains("hello") || lower.contains("hi") || lower.contains("hey")) {
            return "Hello! How can I help you today?";
        }
        if (lower.contains("how are you")) {
            return "I'm doing great, thanks for asking!";
        }
        if (lower.contains("what's your name")) {
            return "I'm HarithBot, your friendly chatbot!";
        }
        if (lower.contains("thank")) {
            return "You're welcome! Anything else on your mind?";
        }
        if (lower.contains("bye") || lower.contains("goodbye")) {
            return "Nice chatting with you. Bye!";
        }
        if (lower.contains("hala")) {
            return "Hala wallah";
        }

        // SAFE fallback if Gemini fails
        String reply = askGeminiApi(text);
        return (reply == null || reply.isEmpty())
                ? offlineReplies[new Random().nextInt(offlineReplies.length)]
                : reply;
    }

    private String askGeminiApi(String prompt) {
        try {
            JSONObject jsonReq = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray parts = new JSONArray();

            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);
            parts.put(textPart);

            contentObj.put("parts", parts);
            contents.put(contentObj);
            jsonReq.put("contents", contents);

            URL url = new URL(GEMINI_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonReq.toString().getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();

            InputStream stream;
            if (status >= 400) {
                stream = conn.getErrorStream();
            } else {
                stream = conn.getInputStream();
            }

            StringBuilder respBuilder = new StringBuilder();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));

            String line;
            while ((line = br.readLine()) != null) {
                respBuilder.append(line);
            }
            br.close();

            JSONObject respJson = new JSONObject(respBuilder.toString());
            JSONArray candidates = respJson.optJSONArray("candidates");

            if (candidates != null && candidates.length() > 0) {
                JSONObject first = candidates.getJSONObject(0);
                JSONObject content = first.getJSONObject("content");
                JSONArray partsArr = content.getJSONArray("parts");

                if (partsArr.length() > 0) {
                    return partsArr.getJSONObject(0).optString(
                            "text",
                            offlineReplies[new Random().nextInt(offlineReplies.length)]
                    );
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return offlineReplies[new Random().nextInt(offlineReplies.length)];
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HarithChatbot().setVisible(true));
    }
}
