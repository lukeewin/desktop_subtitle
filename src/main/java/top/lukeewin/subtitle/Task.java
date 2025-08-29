package top.lukeewin.subtitle;

import com.k2fsa.sherpa.onnx.*;
import javafx.application.Platform;
import javafx.scene.control.Label;
import top.lukeewin.subtitle.entity.ASRConfigEntity;

import javax.sound.sampled.*;

class Task implements Runnable {
    private final Label label;
    private final static int sampleRate = 16000;
    private final static int windowSize = 512;
    private final OnlineRecognizer recognizer;
    private final OnlineStream stream;

    public Task(Label label, ASRConfigEntity asrConfigEntity) {
        this.label = label;

        String encoder = asrConfigEntity.getModelEncoder();
        String decoder = asrConfigEntity.getModelDecoder();
        String tokens = asrConfigEntity.getModelTokens();

        OnlineParaformerModelConfig paraformer = OnlineParaformerModelConfig.builder()
                .setEncoder(encoder).setDecoder(decoder).build();

        OnlineModelConfig modelConfig = OnlineModelConfig.builder()
                .setParaformer(paraformer)
                .setTokens(tokens)
                .setNumThreads(1)
                .setDebug(false)
                .build();

        OnlineRecognizerConfig config = OnlineRecognizerConfig.builder()
                .setOnlineModelConfig(modelConfig)
                .setDecodingMethod("greedy_search")
                .build();

        this.recognizer = new OnlineRecognizer(config);
        this.stream = recognizer.createStream();
    }

    public void process() {
        TargetDataLine targetDataLine = null;
        try {
            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format);
            targetDataLine.start();

            Platform.runLater(() -> label.setText("现在可以开始讲话了"));

            byte[] buffer = new byte[windowSize * 2];
            float[] samples = new float[windowSize];
            String lastResult = "";

            while (targetDataLine.isOpen()) {
                int n = targetDataLine.read(buffer, 0, buffer.length);
                if (n <= 0) continue;

                // PCM16LE 转 float
                for (int i = 0; i != windowSize; ++i) {
                    int low = buffer[2 * i] & 0xFF;
                    int high = buffer[2 * i + 1];
                    short s = (short)((high << 8) | low);
                    samples[i] = (float) s / 32768;
                }

                for (int i = 0; i < samples.length; i++) {
                    samples[i] = samples[i] / 32768.0f;
                }

                stream.acceptWaveform(samples, sampleRate);

                while (recognizer.isReady(stream)) {
                    recognizer.decode(stream);
                }

                String text = recognizer.getResult(stream).getText();
                boolean isEndpoint = recognizer.isEndpoint(stream);

                if (!text.isEmpty() && !text.equals(lastResult)) {
                    lastResult = text;
                    String finalLastResult = lastResult;
                    Platform.runLater(() -> label.setText(finalLastResult));
                }

                if (isEndpoint) {
                    recognizer.reset(stream);
                    lastResult = "";
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            if (targetDataLine != null && targetDataLine.isOpen()) {
                targetDataLine.close();
            }
            if (stream != null) {
                stream.release();
            }
            if (recognizer != null) {
                recognizer.release();
            }
        }
    }

    @Override
    public void run() {
        process();
    }
}
