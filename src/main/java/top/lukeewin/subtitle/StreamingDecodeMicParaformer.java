package top.lukeewin.subtitle;

import com.k2fsa.sherpa.onnx.*;
import javax.sound.sampled.*;

public class StreamingDecodeMicParaformer {
  public static void main(String[] args) {
    int sampleRate = 16000; // 麦克风采样率
    int windowSize = 512; // 每次读取多少采样点

    // 请参考如下链接下载模型文件：
    // https://k2-fsa.github.io/sherpa/onnx/pretrained_models/online-paraformer/paraformer-models.html#csukuangfj-sherpa-onnx-streaming-paraformer-bilingual-zh-en-chinese-english
    String encoder = "D:\\Works\\Java\\CustomProjects\\desktop-subtitle\\src\\main\\resources\\models\\sherpa-onnx-streaming-paraformer-bilingual-zh-en\\encoder.int8.onnx";
    String decoder = "D:\\Works\\Java\\CustomProjects\\desktop-subtitle\\src\\main\\resources\\models\\sherpa-onnx-streaming-paraformer-bilingual-zh-en\\decoder.int8.onnx";
    String tokens = "D:\\Works\\Java\\CustomProjects\\desktop-subtitle\\src\\main\\resources\\models\\sherpa-onnx-streaming-paraformer-bilingual-zh-en\\tokens.txt";

    OnlineParaformerModelConfig paraformer =
        OnlineParaformerModelConfig.builder().setEncoder(encoder).setDecoder(decoder).build();

    OnlineModelConfig modelConfig =
        OnlineModelConfig.builder()
            .setParaformer(paraformer)
            .setTokens(tokens)
            .setNumThreads(1)
            .setDebug(true)
            .build();

    OnlineRecognizerConfig config =
        OnlineRecognizerConfig.builder()
            .setOnlineModelConfig(modelConfig)
            .setDecodingMethod("greedy_search")
            .build();

    OnlineRecognizer recognizer = new OnlineRecognizer(config);
    OnlineStream stream = recognizer.createStream();

    // 配置麦克风
    AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false); // 16kHz, 16bit, 单声道, signed, little endian
    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
    TargetDataLine targetDataLine;
    try {
      targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
      targetDataLine.open(format);
      targetDataLine.start();
    } catch (LineUnavailableException e) {
      System.out.println("无法打开麦克风: " + e.getMessage());
      recognizer.release();
      return;
    }

    System.out.println("开始实时语音转写，按 Ctrl+C 退出");

    byte[] buffer = new byte[windowSize * 2];
    float[] samples = new float[windowSize];
    String lastResult = "";

    while (targetDataLine.isOpen()) {
      int n = targetDataLine.read(buffer, 0, buffer.length);
      if (n <= 0) {
        System.out.printf("获得 %d 字节，期望 %d 字节。\n", n, buffer.length);
        continue;
      }
      // PCM16LE 转 float
      for (int i = 0; i != windowSize; ++i) {
        int low = buffer[2 * i] & 0xFF;
        int high = buffer[2 * i + 1];
        short s = (short)((high << 8) | low);
        samples[i] = (float) s / 32768;
      }

      // 送入识别流
      stream.acceptWaveform(samples, sampleRate);

      // 触发一次解码
      while (recognizer.isReady(stream)) {
        recognizer.decode(stream);
      }

      // 获取最新识别结果
      String text = recognizer.getResult(stream).getText();
      if (!text.equals(lastResult) && !text.isEmpty()) {
        System.out.println("识别结果: " + text);
        lastResult = text;
      }
    }

    stream.release();
    recognizer.release();
  }
}