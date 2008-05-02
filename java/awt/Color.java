package java.awt;

public class Color {
	public final static Color black = new Color();
	public final static Color white = new Color();
	public final static Color red = new Color();
	public final static Color green = new Color();
	public final static Color blue = new Color();
	public final static Color gray = new Color();
	public final static Color yellow = new Color();
	public final static Color magenta = new Color();
	public final static Color cyan = new Color();
	public final static Color pink = new Color();
	public final static Color orange = new Color();
	public final static Color lightGray = new Color();
	public final static Color darkGray = new Color();

	public Color() {}
	public Color(int rgb) {}
	public Color(int r, int g, int b) {}
	public int getRGB() { return 0; }
	public int getRed() { return 0; }
	public int getGreen() { return 0; }
	public int getBlue() { return 0; }
	public Color brighter() { return this; }
	public Color darker() { return this; }

	public static int HSBtoRGB(float h, float s, float b) { return 0; }
	public static float[] RGBtoHSB(int r, int g, int b, float[] hsb) {
		return hsb;
	}
}
