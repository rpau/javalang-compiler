public class A { static <T> T[] arraysCopyOf(T[] original, int newLength) {
T[] copy = null;
System.arraycopy(
original, 0, copy, 0, Math.min(original.length, newLength));
return copy;} }