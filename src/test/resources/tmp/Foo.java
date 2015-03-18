import java.util.Arrays; public class Foo {/**Returns a comparator that compares two arrays of unsigned {@code int} values lexicographically.
 That is, it compares, using {@link #compare(int, int)}), the first pair of values that follow
 any common prefix, or when one array is a prefix of the other, treats the shorter array as the
 lesser. For example, {@code [] < [1] < [1, 2] < [2] < [1 << 31]}.
 <p>
 The returned comparator is inconsistent with {@link Object#equals(Object)} (since arrays
 support only identity equality), but it is consistent with {@link Arrays#equals(int[], int[])}.
 @see <a href="http://en.wikipedia.org/wiki/Lexicographical_order"> Lexicographical order
      article at Wikipedia</a>
*/ public void foo(){}}