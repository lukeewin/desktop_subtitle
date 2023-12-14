package subtitle;

import javafx.application.Platform;
import javafx.scene.control.Label;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

class Task implements Runnable {
    Label label;
    public Task(Label label) {
        this.label = label;
    }

    public void getTranscriberListener() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                String text = "hello world";
                // Update UI here.
                label.setText(text);
            }
        });
    }

    public void process() {
        try {
            // Step3 读取麦克风数据
            AudioFormat audioFormat = new AudioFormat(16000.0F, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(audioFormat);
            targetDataLine.start();
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    // Update UI here.
                    label.setText("You can speak now!");
                }
            });
            int nByte = 0;
            final int bufSize = 6400;
            byte[] buffer = new byte[bufSize];
            while ((nByte = targetDataLine.read(buffer, 0, bufSize)) > 0) {
                // Step4 直接发送麦克风数据流
//                transcriber.send(buffer);
            }

            // Step5 通知服务端语音数据发送完毕,等待服务端处理完成
//            transcriber.stop();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {

        }
    }

    @Override
    public void run() {
        this.process();
    }
}