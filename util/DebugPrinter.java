package util;

public class DebugPrinter {
	private static final boolean DEBUG = true;
	private static int debugIndent = 0;

	public static void debugPrint(String massage) {
		if (!DEBUG)
			return;
		String indent = "";
		for (int i = 0; i < debugIndent; i++)
			indent += "  ";
		System.out.println(indent + massage);
	}

	public static void debugPrint(int additonalIndentBeforePrint, String massage) {
		if (!DEBUG)
			return;
		addDebugIndent(additonalIndentBeforePrint);
		debugPrint(massage);
	}

	public static void debugPrint(String massage, int additonalIndentAfterPrint) {
		if (!DEBUG)
			return;
		debugPrint(massage);
		addDebugIndent(additonalIndentAfterPrint);
	}

	public static void addDebugIndent(int i) {
		debugIndent += i;
		if (debugIndent < 0)
			debugIndent = 0;
	}

}
