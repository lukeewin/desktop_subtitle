package top.lukeewin.subtitle;
import com.k2fsa.sherpa.onnx.*;
import javax.sound.sampled.*;

public class StreamingDecodeMicParaformerWithVadRealtime {
  public static void main(String[] args) {
    int sampleRate = 16000;
    int windowSize = 512;

    String encoder = "D:\\Works\\Java\\CustomProjects\\desktop-subtitle\\src\\main\\resources\\models\\sherpa-onnx-streaming-paraformer-bilingual-zh-en\\encoder.int8.onnx";
    String decoder = "D:\\Works\\Java\\CustomProjects\\desktop-subtitle\\src\\main\\resources\\models\\sherpa-onnx-streaming-paraformer-bilingual-zh-en\\decoder.int8.onnx";
    String tokens = "D:\\Works\\Java\\CustomProjects\\desktop-subtitle\\src\\main\\resources\\models\\sherpa-onnx-streaming-paraformer-bilingual-zh-en\\tokens.txt";
    String vadModel = "D:\\Works\\Java\\CustomProjects\\desktop-subtitle\\src\\main\\resources\\models\\vad\\silero_vad_v5.onnx";

    OnlineParaformerModelConfig paraformer =
            OnlineParaformerModelConfig.builder().setEncoder(encoder).setDecoder(decoder).build();

    OnlineModelConfig modelConfig =
            OnlineModelConfig.builder()
                    .setParaformer(paraformer)
                    .setTokens(tokens)
                    .setNumThreads(1)
                    .setDebug(true)
                    .build();

    OnlineRecognizerConfig asrConfig =
            OnlineRecognizerConfig.builder()
                    .setOnlineModelConfig(modelConfig)
                    .setDecodingMethod("greedy_search")
                    .build();

    OnlineRecognizer recognizer = new OnlineRecognizer(asrConfig);
    OnlineStream stream = recognizer.createStream();

    SileroVadModelConfig sileroVad =
            SileroVadModelConfig.builder()
                    .setModel(vadModel)
                    .setThreshold(0.5f)
                    .setMinSilenceDuration(0.3f)
                    .setMinSpeechDuration(0.1f)
                    .setWindowSize(windowSize)
                    .build();

    VadModelConfig vadConfig =
            VadModelConfig.builder()
                    .setSileroVadModelConfig(sileroVad)
                    .setSampleRate(sampleRate)
                    .setNumThreads(1)
                    .setDebug(false)
                    .setProvider("cpu")
                    .build();

    Vad vad = new Vad(vadConfig);

    AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
    TargetDataLine targetDataLine;
    try {
      targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
      targetDataLine.open(format);
      targetDataLine.start();
    } catch (LineUnavailableException e) {
      System.out.println("无法打开麦克风: " + e.getMessage());
      recognizer.release();
      vad.release();
      return;
    }

    System.out.println("开始实时语音识别（VAD过滤环境噪音），按 Ctrl+C 退出");

    byte[] buffer = new byte[windowSize * 2];
    float[] samples = new float[windowSize];
    String lastResult = "";
    boolean lastVadSpeech = false;

    while (targetDataLine.isOpen()) {
      int n = targetDataLine.read(buffer, 0, buffer.length);
      if (n <= 0) continue;

      for (int i = 0; i < windowSize; ++i) {
        int low = buffer[2 * i] & 0xFF;
        int high = buffer[2 * i + 1];
        short s = (short)((high << 8) | low);
        samples[i] = (float) s / 32768;
      }

      vad.acceptWaveform(samples);
      boolean curVadSpeech = vad.isSpeechDetected();

      if (curVadSpeech) {
        stream.acceptWaveform(samples, sampleRate);
        while (recognizer.isReady(stream)) {
          recognizer.decode(stream);
        }
        String text = recognizer.getResult(stream).getText();
        if (!text.equals(lastResult) && !text.isEmpty()) {
          System.out.println("实时识别结果: " + text);
          lastResult = text;
        }
      }

      // 检测到人声->静音的转变，补齐长尾静音并多次decode
      if (lastVadSpeech && !curVadSpeech) {
        float[] tailPaddings = new float[(int) (1.5 * sampleRate)]; // 长尾静音
        stream.acceptWaveform(tailPaddings, sampleRate);

        // 多次decode确保所有内容输出
        int decodeCount = 0;
        while (recognizer.isReady(stream) && decodeCount < 10) {
          recognizer.decode(stream);
          decodeCount++;
        }
        String finalText = recognizer.getResult(stream).getText();
        if (!finalText.isEmpty()) {
          System.out.println("最终识别结果: " + finalText);
        }
        lastResult = "";
      }

      lastVadSpeech = curVadSpeech;
    }

    stream.release();
    recognizer.release();
    vad.release();
  }
}