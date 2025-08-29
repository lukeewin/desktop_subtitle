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
