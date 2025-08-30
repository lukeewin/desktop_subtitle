# 1. 运行环境

本项目依赖 jdk8，并且只能运行在 Windows 系统中。如需服务器中部署实时语音识别接口，可联系微信 lukeewin01, 需要备注来自哪个平台，为何添加，否则不给通过。

# 2. 依赖

项目开源到 github 中，访问地址：https://github.com/lukeewin/desktop_subtitle

如果无法访问 github 的用户，可以访问 gitee: https://gitee.com/lukeewin/desktop_subtitle

这里使用 Maven 管理项目，在pom.xml中添加下方依赖。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>lukeewin.desktop.subtitles</groupId>
    <artifactId>lukeewin</artifactId>
    <version>1.0-SNAPSHOT</version>

    <dependencies>
        <dependency>
            <groupId>org.yaml</groupId>
            <artifactId>snakeyaml</artifactId>
            <version>2.4</version>
        </dependency>
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>27.0.1-jre</version>
        </dependency>
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-all</artifactId>
            <version>4.1.111.Final</version>
        </dependency>
        <dependency>
            <groupId>top.lukeewin</groupId>
            <artifactId>asr</artifactId>
            <version>1.12.9-jdk8</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>2.3.2</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

注意：必须要使用 JDK8，如果使用其它版本的 JDK 会无法正常使用。并且只能在 Windows 系统中运行。如需在 Linux 服务器中部署 ASR 接口可以联系微信 lukeewin01

# 3. 项目核心代码解析

创建一个任务类 Task，专门用于获取电脑中的麦克风并且把语音信号实时输入到 Paraformer-Streaming 流式模型中。

Java 中获取本地电脑麦克风的代码如下：

```java
AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
targetDataLine.open(format);
targetDataLine.start();
```

这里指定了麦克风的采样率为 16000，对应上面代码中的 sampleRate，位深为 16 bit，单声道，有符号，小端。

通过 JDK 中提供的方法获取麦克风规定的格式。然后调用 TargetDataLine 类中的 open() 方法，打开本地麦克风。

通过 TargetDataLine 中的 isOpen() 方法，判断麦克风是否正确打开，如果正常打开，就会把获取到的音频信息按照指定的 buffer 传入到 ASR 模型中进行解码。

```java
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
}
```

模型返回的结果显示在 UI 界面中，并且还需要判断当前讲话是否结束，如果结束就重置。

```java
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
```

为了方便用户使用，方便修改字体大小和颜色，这里把字体大小和字体颜色配置到 application.yml 配置文件中，启动软件的时候会读取这个文件来设置字体大小和字体颜色，如果用户在运行中的软件修改了字体大小和颜色会改变这个文件，具体代码如下：

先创建一个实体类，作为配置字体对象。我把这个类命名为：FontConfigEntity

```java
package top.lukeewin.subtitle.entity;

/**
 * @author Luke Ewin
 * @date 2025/8/22 17:00
 * @blog blog.lukeewin.top
 */
public class FontConfigEntity {
    private int size;
    private String color;

    public FontConfigEntity() {
    }

    public FontConfigEntity(int size, String color) {
        this.size = size;
        this.color = color;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
```

然后在 Controller.java 中添加下面代码，实现右击弹出“设置”菜单栏功能。

```java
public void initialize(URL url, ResourceBundle rb) {
        try {
            Yaml yaml = new Yaml();
            ASRConfigEntity asrConfigEntity = new ASRConfigEntity();
            Map<String, Object> yamlMap = yaml.load(new FileInputStream("src/main/resources/application.yml"));
            Map<String, Object> streamingASRConfigMap = (Map<String, Object>) yamlMap.get("streaming_asr");
            String model_encoder = (String) streamingASRConfigMap.get("model_encoder");
            String model_decoder = (String) streamingASRConfigMap.get("model_decoder");
            String model_tokens = (String) streamingASRConfigMap.get("model_tokens");

            asrConfigEntity.setModelEncoder(model_encoder);
            asrConfigEntity.setModelDecoder(model_decoder);
            asrConfigEntity.setModelTokens(model_tokens);

            Map<String, Object> fontConfigMap = (Map<String, Object>) yamlMap.get("font");
            int fontSize = (int) fontConfigMap.get("size");
            String fontColor = (String) fontConfigMap.get("color");
            fontConfig = new FontConfigEntity(fontSize, fontColor);
            applyFontConfig();

            Task task = new Task(label, asrConfigEntity);
            new Thread(task).start();

            createContextMenu();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
}

private void createContextMenu() {
        MenuItem settingsItem = new MenuItem("设置");
        settingsItem.setOnAction(event -> openSettings());

        final ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(settingsItem);

        borderPane.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(borderPane, event.getScreenX(), event.getScreenY());
            } else {
                if (contextMenu.isShowing()) {
                    contextMenu.hide();
                }
            }
        });
}

private void openSettings() {
        // 打开设置窗口
        SettingsDialog settingsDialog = new SettingsDialog(label.getScene().getWindow(), fontConfig);
        FontConfigEntity newConfig = settingsDialog.showAndWait().orElse(null);
        if (newConfig != null) {
            this.fontConfig = newConfig;
            applyFontConfig();
            saveFontConfig();
        }
}

private void applyFontConfig() {
        label.setStyle("-fx-font-size: " + fontConfig.getSize() + "px; -fx-text-fill: " + fontConfig.getColor() + ";");
}

private void saveFontConfig() {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> yamlMap = yaml.load(new FileInputStream("src/main/resources/application.yml"));

            // 更新字体配置
            Map<String, Object> fontConfigMap = new HashMap<>();
            fontConfigMap.put("size", fontConfig.getSize());
            fontConfigMap.put("color", fontConfig.getColor());
            yamlMap.put("font", fontConfigMap);

            // 写回文件
            FileWriter writer = new FileWriter("src/main/resources/application.yml");
            yaml.dump(yamlMap, writer);
            writer.close();
        } catch (Exception e) {
            System.err.println("保存配置失败: " + e.getMessage());
        }
}
```

上面代码中包含了从配置文件中加载模型路径的过程，把模型路径也配置到配置文件中方便管理。

这里主要使用到 snakeyaml 依赖实现读取和写入 application.yml 文件。

application.yml 配置文件如下：

```yaml
app: {name: Desktop_Subtitle, version: 1.0.0}
streaming_asr: {model_encoder: src/main/resources/models/streaming-paraformer-zh-en/encoder.int8.onnx,
  model_decoder: src/main/resources/models/streaming-paraformer-zh-en/decoder.int8.onnx,
  model_tokens: src/main/resources/models/streaming-paraformer-zh-en/tokens.txt}
asr: {model: src/main/resources/models/sense-voice-zh-en-ja-ko-yue/model.int8.onnx,
  tokens: src/main/resources/models/sense-voice-zh-en-ja-ko-yue/tokens.txt}
sampleRate: 16000
windowSize: 512
font: {size: 20, color: '#FFB366'}
```

这里在代码层面是限制了字体大小范围：[10, 72]，如果设置的值小于最小值，那么就会使用最小值，如果设置的值大于最大值，那么就会使用最大值，实现代码如下：

```java
fontSizeSpinner = new Spinner<>(10, 72, initialConfig.getSize());
SpinnerValueFactory<Integer> valueFactory = fontSizeSpinner.getValueFactory();
	if (valueFactory instanceof SpinnerValueFactory.IntegerSpinnerValueFactory) {
        SpinnerValueFactory.IntegerSpinnerValueFactory intFactory = (SpinnerValueFactory.IntegerSpinnerValueFactory) valueFactory;
        if (value < intFactory.getMin()) {
            value = intFactory.getMin();
        } else if (value > intFactory.getMax()) {
            value = intFactory.getMax();
        }
    }
fontSizeSpinner.getValueFactory().setValue(value);
```

# 4. 项目演示
[如果想要看视频演示效果可以点击这里。](https://www.bilibili.com/video/BV1X7eYzUEsv/)